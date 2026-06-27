import { ChatInputCommandInteraction, EmbedBuilder, SlashCommandBuilder } from 'discord.js';
import { QueueFactory } from '../../../queue/application/factory.ts';

export const data = new SlashCommandBuilder()
  .setName('queue')
  .setDescription('Show the current song queue');

export async function execute(interaction: ChatInputCommandInteraction): Promise<void> {
  const queue = await QueueFactory.getQueue(interaction.guildId!);

  if (queue.isEmpty()) {
    await interaction.reply('Queue is empty.');
    return;
  }

  const embed = new EmbedBuilder()
    .setTitle('Song Queue')
    .setColor(0x5865f2)
    .setDescription(
      queue
        .getAll()
        .map(
          (song, i) =>
            `**${i + 1}.** ${song.getTitle()} (${song.getDuration().toFormatted()}) — <@${song.getRequestedBy()}>`,
        )
        .join('\n'),
    )
    .setFooter({ text: `${queue.count()} song(s) in queue` });

  await interaction.reply({ embeds: [embed] });
}
