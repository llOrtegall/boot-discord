import {
  Client,
  Collection,
  Events,
  GatewayIntentBits,
  MessageFlags,
  REST,
  Routes,
  ChatInputCommandInteraction,
} from 'discord.js';
import * as songCmd from './commands/song.ts';
import * as pauseCmd from './commands/pause.ts';
import * as resumeCmd from './commands/resume.ts';
import * as skipCmd from './commands/skip.ts';
import * as stopCmd from './commands/stop.ts';
import * as queueCmd from './commands/queue.ts';
import { PlayerFactory, playerRepository } from '../../player/application/factory.ts';
import { CacheFactory } from '../../cache/application/factory.ts';

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
    console.log(`Bot ready as ${readyClient.user.tag}`);
    await registerCommands(
      token,
      clientId,
      readyClient.guilds.cache.map((g) => g.id),
    );
    startEvictionJob();
  });

  client.on(Events.InteractionCreate, async (interaction) => {
    if (!interaction.isChatInputCommand()) return;
    const command = commandMap.get(interaction.commandName);
    if (!command) return;

    try {
      await command.execute(interaction);
    } catch (err) {
      console.error(`[BotRouter] Command ${interaction.commandName} failed:`, err);
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

  console.log(`Slash commands registered for ${guildIds.length} guild(s)`);
}

function startEvictionJob(): void {
  const run = () => {
    CacheFactory.evictExpiredSongs()
      .then((count) => {
        if (count > 0) console.log(`[BotRouter] Evicted ${count} expired cached song(s)`);
      })
      .catch((err) => console.error('[BotRouter] Eviction job failed:', err));
  };

  run();
  setInterval(run, EVICTION_INTERVAL_MS);
}
