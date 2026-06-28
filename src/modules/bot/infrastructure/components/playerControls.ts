import { ActionRowBuilder, ButtonBuilder, ButtonStyle, EmbedBuilder } from 'discord.js';

export const PLAYER_BUTTON = {
  prev: 'player:prev',
  playpause: 'player:playpause',
  skip: 'player:skip',
  stop: 'player:stop',
} as const;

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

export function buildNowPlayingEmbed(
  title: string,
  durationSec: number,
  requestedById: string,
): EmbedBuilder {
  return new EmbedBuilder()
    .setColor(0x5865f2)
    .setTitle('🎵 ¡Ahora suena!')
    .setDescription(`**${title}**`)
    .addFields(
      { name: 'Duración', value: formatDuration(durationSec), inline: true },
      { name: 'Pedido por', value: `<@${requestedById}>`, inline: true },
    );
}

export function buildQueuedEmbed(
  title: string,
  position: number,
  requestedById: string,
): EmbedBuilder {
  return new EmbedBuilder()
    .setColor(0x57f287)
    .setTitle('✅ Añadido a la cola')
    .setDescription(`**${title}**`)
    .addFields(
      { name: 'Posición', value: `${position}`, inline: true },
      { name: 'Pedido por', value: `<@${requestedById}>`, inline: true },
    );
}

export function buildControls(): ActionRowBuilder<ButtonBuilder> {
  return new ActionRowBuilder<ButtonBuilder>().addComponents(
    new ButtonBuilder()
      .setCustomId(PLAYER_BUTTON.prev)
      .setEmoji('⏮️')
      .setStyle(ButtonStyle.Secondary),
    new ButtonBuilder()
      .setCustomId(PLAYER_BUTTON.playpause)
      .setEmoji('⏯️')
      .setStyle(ButtonStyle.Primary),
    new ButtonBuilder()
      .setCustomId(PLAYER_BUTTON.skip)
      .setEmoji('⏭️')
      .setStyle(ButtonStyle.Secondary),
    new ButtonBuilder().setCustomId(PLAYER_BUTTON.stop).setEmoji('⏹️').setStyle(ButtonStyle.Danger),
  );
}
