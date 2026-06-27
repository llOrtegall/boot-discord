import { ChatInputCommandInteraction, SlashCommandBuilder } from 'discord.js';
import { PlayerFactory } from '../../../player/application/factory.ts';

export const data = new SlashCommandBuilder()
  .setName('pause')
  .setDescription('Pause the current song');

export async function execute(interaction: ChatInputCommandInteraction): Promise<void> {
  try {
    await PlayerFactory.pausePlayer(interaction.guildId!);
    await interaction.reply('Paused.');
  } catch (err: any) {
    await interaction.reply(err.message ?? 'Could not pause.');
  }
}
