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
import { logger } from '../../../shared/logger.ts';

const SCOPE = 'voice';

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
    player.on('stateChange', (oldState, newState) => {
      logger.info(SCOPE, `player ${oldState.status} -> ${newState.status} (guild ${guildId})`);
    });
    player.on('error', (err) => {
      logger.error(SCOPE, `audio player error (guild ${guildId})`, err);
    });
    player.on(AudioPlayerStatus.Idle, () => {
      this.onIdleCallback?.(guildId).catch((err) =>
        logger.error(SCOPE, `onIdle callback failed (guild ${guildId})`, err),
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
    logger.info(SCOPE, `play "${song.getTitle()}" from ${filePath} (guild ${guildId})`);

    const { stream, type } = await demuxProbe(createReadStream(filePath));
    logger.info(SCOPE, `stream type=${type} (guild ${guildId})`);
    const resource = createAudioResource(stream, { inputType: type });

    const connection = this.getConnection(guildId);
    if (!connection) {
      logger.error(SCOPE, `no voice connection for guild ${guildId}; cannot play`);
      return;
    }

    try {
      await entersState(connection, VoiceConnectionStatus.Ready, 15_000);
    } catch (err) {
      logger.error(SCOPE, `connection not ready within 15s (guild ${guildId})`, err);
      return;
    }

    connection.subscribe(player);
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
    const guildId = channel.guild.id;
    const existing = this.getConnection(guildId);
    if (existing) return existing;

    logger.info(SCOPE, `joining channel ${channel.id} (guild ${guildId})`);
    const connection = joinVoiceChannel({
      channelId: channel.id,
      guildId,
      adapterCreator: channel.guild.voiceAdapterCreator as unknown as DiscordGatewayAdapterCreator,
    });

    connection.on('stateChange', (oldState, newState) => {
      logger.info(SCOPE, `connection ${oldState.status} -> ${newState.status} (guild ${guildId})`);
    });
    connection.on('error', (err) => {
      logger.error(SCOPE, `connection error (guild ${guildId})`, err);
    });

    return connection;
  }

  getAudioPlayer(guildId: string): AudioPlayer | undefined {
    return this.audioPlayers.get(guildId);
  }
}
