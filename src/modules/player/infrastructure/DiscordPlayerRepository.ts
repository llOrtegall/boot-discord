import {
  AudioPlayer,
  AudioPlayerStatus,
  VoiceConnection,
  VoiceConnectionStatus,
  createAudioPlayer,
  createAudioResource,
  joinVoiceChannel,
  getVoiceConnection,
  demuxProbe,
  entersState,
  NoSubscriberBehavior,
} from '@discordjs/voice';
import type { DiscordGatewayAdapterCreator } from '@discordjs/voice';
import { createReadStream } from 'node:fs';
import type { VoiceBasedChannel } from 'discord.js';
import type { PlayerRepository } from '../domain/PlayerRepository.ts';
import { Player } from '../domain/Player.ts';
import { Song } from '../../queue/domain/Song.ts';

export class DiscordPlayerRepository implements PlayerRepository {
  private readonly players = new Map<string, Player>();
  private readonly audioPlayers = new Map<string, AudioPlayer>();
  private onIdleCallback: ((guildId: string) => Promise<void>) | null = null;

  onIdle(callback: (guildId: string) => Promise<void>): void {
    this.onIdleCallback = callback;
  }

  private getOrCreateAudioPlayer(guildId: string): AudioPlayer {
    const existing = this.audioPlayers.get(guildId);
    if (existing) return existing;

    const player = createAudioPlayer({ behaviors: { noSubscriber: NoSubscriberBehavior.Pause } });
    player.on(AudioPlayerStatus.Idle, () => {
      this.onIdleCallback?.(guildId).catch((err) =>
        console.error(`[DiscordPlayerRepository] onIdle error for guild ${guildId}:`, err),
      );
    });
    this.audioPlayers.set(guildId, player);
    return player;
  }

  private getConnection(guildId: string): VoiceConnection | undefined {
    return getVoiceConnection(guildId);
  }

  async getByGuildId(guildId: string): Promise<Player | null> {
    return this.players.get(guildId) ?? null;
  }

  async save(player: Player): Promise<void> {
    this.players.set(player.getGuildId(), player);
  }

  async playSong(guildId: string, song: Song): Promise<void> {
    const player = this.getOrCreateAudioPlayer(guildId);
    const filePath = song.getUrl().getValue();
    const { stream, type } = await demuxProbe(createReadStream(filePath));
    const resource = createAudioResource(stream, { inputType: type });

    const connection = this.getConnection(guildId);
    if (connection) {
      try {
        await entersState(connection, VoiceConnectionStatus.Ready, 15_000);
      } catch (err) {
        console.error(`[DiscordPlayerRepository] connection not ready for guild ${guildId}:`, err);
      }
      connection.subscribe(player);
    }

    player.play(resource);
  }

  async pause(guildId: string): Promise<void> {
    this.audioPlayers.get(guildId)?.pause();
  }

  async resume(guildId: string): Promise<void> {
    this.audioPlayers.get(guildId)?.unpause();
  }

  async stop(guildId: string): Promise<void> {
    this.audioPlayers.get(guildId)?.stop();
  }

  async destroy(guildId: string): Promise<void> {
    this.audioPlayers.get(guildId)?.stop();
    this.audioPlayers.delete(guildId);
    this.getConnection(guildId)?.destroy();
    this.players.delete(guildId);
  }

  joinChannel(channel: VoiceBasedChannel): VoiceConnection {
    const existing = this.getConnection(channel.guild.id);
    if (existing) return existing;

    return joinVoiceChannel({
      channelId: channel.id,
      guildId: channel.guild.id,
      adapterCreator: channel.guild.voiceAdapterCreator as unknown as DiscordGatewayAdapterCreator,
    });
  }

  getAudioPlayer(guildId: string): AudioPlayer | undefined {
    return this.audioPlayers.get(guildId);
  }
}
