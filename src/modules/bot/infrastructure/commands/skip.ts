import { ChatInputCommandInteraction, GuildMember, SlashCommandBuilder } from 'discord.js';
import { PlayerFactory } from '../../../player/application/factory.ts';

export const data = new SlashCommandBuilder()
  .setName('skip')
  .setDescription('Skip the current song');

export async function execute(interaction: ChatInputCommandInteraction): Promise<void> {
  try {
    const member = interaction.member as GuildMember;
    const channelId = member.voice.channel?.id ?? '';
    const next = await PlayerFactory.skipSong(interaction.guildId!, channelId);
    if (next) {
      await interaction.reply(`Skipped. Now playing: **${next.getCurrentSong()?.getTitle()}**`);
    } else {
      await interaction.reply('Skipped. Queue is empty.');
    }
  } catch (err: any) {
    await interaction.reply(err.message ?? 'Could not skip.');
  }
}
