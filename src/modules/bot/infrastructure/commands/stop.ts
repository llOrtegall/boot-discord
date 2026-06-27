import { ChatInputCommandInteraction, SlashCommandBuilder } from 'discord.js';
import { PlayerFactory } from '../../../player/application/factory.ts';

export const data = new SlashCommandBuilder()
  .setName('stop')
  .setDescription('Stop playback and clear the queue');

export async function execute(interaction: ChatInputCommandInteraction): Promise<void> {
  try {
    await PlayerFactory.stopPlayer(interaction.guildId!);
    await interaction.reply('Stopped and queue cleared.');
  } catch (err: any) {
    await interaction.reply(err.message ?? 'Could not stop.');
  }
}
