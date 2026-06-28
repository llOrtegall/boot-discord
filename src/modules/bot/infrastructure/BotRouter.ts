import {
  Client,
  Collection,
  Events,
  GatewayIntentBits,
  MessageFlags,
  REST,
  Routes,
  ChatInputCommandInteraction,
  type ButtonInteraction,
} from 'discord.js';
import { PLAYER_BUTTON } from './components/playerControls.ts';
import * as songCmd from './commands/song.ts';
import * as pauseCmd from './commands/pause.ts';
import * as resumeCmd from './commands/resume.ts';
import * as skipCmd from './commands/skip.ts';
import * as stopCmd from './commands/stop.ts';
import * as queueCmd from './commands/queue.ts';
import { PlayerFactory, playerRepository } from '../../player/application/factory.ts';
import { CacheFactory } from '../../cache/application/factory.ts';
import { logger } from '../../../shared/logger.ts';

const SCOPE = 'bot';
const EVICTION_INTERVAL_MS = 24 * 60 * 60 * 1000;

type Command = {
  data: { name: string; toJSON: () => object };
  execute: (interaction: ChatInputCommandInteraction) => Promise<void>;
};

const commands: Command[] = [songCmd, pauseCmd, resumeCmd, skipCmd, stopCmd, queueCmd];

export function createBot(token: string, clientId: string): Client {
  const client = new Client({
    intents: [
      GatewayIntentBits.Guilds,
      GatewayIntentBits.GuildVoiceStates,
      GatewayIntentBits.GuildMessages,
    ],
  });

  const commandMap = new Collection<string, Command>();
  for (const cmd of commands) {
    commandMap.set(cmd.data.name, cmd);
  }

  playerRepository.onIdle(async (guildId) => {
    const player = await playerRepository.getByGuildId(guildId);
    if (!player) return;
    await PlayerFactory.playNext(guildId, player.getChannelId());
  });

  client.once(Events.ClientReady, async (readyClient) => {
    logger.info(SCOPE, `ready as ${readyClient.user.tag}`);
    await registerCommands(
      token,
      clientId,
      readyClient.guilds.cache.map((g) => g.id),
    );
    startEvictionJob();
  });

  client.on(Events.InteractionCreate, async (interaction) => {
    if (interaction.isButton() && interaction.customId.startsWith('player:')) {
      await handlePlayerButton(interaction);
      return;
    }

    if (!interaction.isChatInputCommand()) return;
    const command = commandMap.get(interaction.commandName);
    if (!command) return;

    logger.info(
      SCOPE,
      `/${interaction.commandName} by ${interaction.user.tag} (guild ${interaction.guildId})`,
    );
    try {
      await command.execute(interaction);
    } catch (err) {
      logger.error(SCOPE, `command /${interaction.commandName} failed`, err);
      const reply =
        interaction.replied || interaction.deferred
          ? interaction.editReply('An error occurred.')
          : interaction.reply({ content: 'An error occurred.', flags: MessageFlags.Ephemeral });
      await reply;
    }
  });

  return client;
}

async function registerCommands(
  token: string,
  clientId: string,
  guildIds: string[],
): Promise<void> {
  const rest = new REST().setToken(token);
  const body = commands.map((c) => c.data.toJSON());

  for (const guildId of guildIds) {
    await rest.put(Routes.applicationGuildCommands(clientId, guildId), { body });
  }

  logger.info(SCOPE, `slash commands registered for ${guildIds.length} guild(s)`);
}

function startEvictionJob(): void {
  const run = () => {
    CacheFactory.evictExpiredSongs()
      .then((count) => {
        if (count > 0) logger.info(SCOPE, `evicted ${count} expired cached song(s)`);
      })
      .catch((err) => logger.error(SCOPE, 'eviction job failed', err));
  };

  run();
  setInterval(run, EVICTION_INTERVAL_MS);
}

async function handlePlayerButton(interaction: ButtonInteraction): Promise<void> {
  const guildId = interaction.guildId;
  if (!guildId) return;

  logger.info(
    SCOPE,
    `button ${interaction.customId} by ${interaction.user.tag} (guild ${guildId})`,
  );

  const player = await playerRepository.getByGuildId(guildId);
  if (!player) {
    await interaction.reply({
      content: 'No hay reproducción activa.',
      flags: MessageFlags.Ephemeral,
    });
    return;
  }
  const channelId = player.getChannelId();

  try {
    switch (interaction.customId) {
      case PLAYER_BUTTON.prev: {
        const prev = await PlayerFactory.playPrevious(guildId, channelId);
        await interaction.reply({
          content: prev
            ? `⏮️ Anterior: **${prev.getCurrentSong()?.getTitle()}**`
            : 'No hay canción anterior.',
          flags: MessageFlags.Ephemeral,
        });
        break;
      }
      case PLAYER_BUTTON.skip: {
        const next = await PlayerFactory.skipSong(guildId, channelId);
        await interaction.reply({
          content: next
            ? `⏭️ Ahora suena: **${next.getCurrentSong()?.getTitle()}**`
            : '⏭️ Cola vacía.',
          flags: MessageFlags.Ephemeral,
        });
        break;
      }
      case PLAYER_BUTTON.playpause: {
        if (player.getState().isPlaying()) {
          await PlayerFactory.pausePlayer(guildId);
          await interaction.reply({ content: '⏸️ Pausado.', flags: MessageFlags.Ephemeral });
        } else if (player.getState().isPaused()) {
          await PlayerFactory.resumePlayer(guildId);
          await interaction.reply({ content: '▶️ Reanudado.', flags: MessageFlags.Ephemeral });
        } else {
          await interaction.reply({ content: 'Nada sonando.', flags: MessageFlags.Ephemeral });
        }
        break;
      }
      case PLAYER_BUTTON.stop: {
        await PlayerFactory.stopPlayer(guildId);
        await interaction.reply({
          content: '⏹️ Detenido y cola limpiada.',
          flags: MessageFlags.Ephemeral,
        });
        break;
      }
    }
  } catch (err) {
    logger.error(SCOPE, `button ${interaction.customId} failed`, err);
    const msg = err instanceof Error ? err.message : 'Error.';
    if (interaction.replied || interaction.deferred) {
      await interaction.followUp({ content: msg, flags: MessageFlags.Ephemeral });
    } else {
      await interaction.reply({ content: msg, flags: MessageFlags.Ephemeral });
    }
  }
}
