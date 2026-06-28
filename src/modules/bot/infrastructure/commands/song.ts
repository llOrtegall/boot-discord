import { ChatInputCommandInteraction, GuildMember, SlashCommandBuilder } from 'discord.js';
import { CacheFactory } from '../../../cache/application/factory.ts';
import { QueueFactory } from '../../../queue/application/factory.ts';
import { PlayerFactory, playerRepository } from '../../../player/application/factory.ts';
import {
  buildControls,
  buildNowPlayingEmbed,
  buildQueuedEmbed,
} from '../components/playerControls.ts';

export const data = new SlashCommandBuilder()
  .setName('song')
  .setDescription('Search, download and play a song')
  .addStringOption((opt) =>
    opt.setName('query').setDescription('Song title or YouTube URL').setRequired(true),
  );

export async function execute(interaction: ChatInputCommandInteraction): Promise<void> {
  await interaction.deferReply();

  const member = interaction.member as GuildMember;
  const voiceChannel = member.voice.channel;

  if (!voiceChannel) {
    await interaction.editReply('You must be in a voice channel.');
    return;
  }

  const query = interaction.options.getString('query', true);
  const guildId = interaction.guildId!;

  let cached;
  try {
    cached = await CacheFactory.getOrDownloadSong(query);
  } catch (err: any) {
    const message: string = err?.message ?? '';
    await interaction.editReply(
      message.includes('No results')
        ? `No results found for: **${query}**`
        : `Could not fetch the song. ${message}`.trim(),
    );
    return;
  }

  playerRepository.joinChannel(voiceChannel);

  const queue = await QueueFactory.addSong(
    guildId,
    cached.getTitle(),
    cached.getFilePath().getValue(),
    cached.getDurationSec(),
    member.user.id,
  );

  const player = await playerRepository.getByGuildId(guildId);
  const isActive = !!player && (player.getState().isPlaying() || player.getState().isPaused());

  const requestedById = member.user.id;

  if (isActive) {
    await interaction.editReply({
      embeds: [buildQueuedEmbed(cached.getTitle(), queue.count(), requestedById)],
    });
  } else {
    await PlayerFactory.playNext(guildId, voiceChannel.id);
    await interaction.editReply({
      embeds: [buildNowPlayingEmbed(cached.getTitle(), cached.getDurationSec(), requestedById)],
      components: [buildControls()],
    });
  }
}
