import { ChatInputCommandInteraction, SlashCommandBuilder } from 'discord.js';
import { PlayerFactory } from '../../../player/application/factory.ts';

export const data = new SlashCommandBuilder()
  .setName('resume')
  .setDescription('Resume the paused song');

export async function execute(interaction: ChatInputCommandInteraction): Promise<void> {
  try {
    await PlayerFactory.resumePlayer(interaction.guildId!);
    await interaction.reply('Resumed.');
  } catch (err: any) {
    await interaction.reply(err.message ?? 'Could not resume.');
  }
}
