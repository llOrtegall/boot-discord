import { createBot } from './modules/bot/infrastructure/BotRouter.ts';
import { logger } from './shared/logger.ts';

const token = process.env['DISCORD_TOKEN'];
const clientId = process.env['DISCORD_CLIENT_ID'];

if (!token) throw new Error('[index] DISCORD_TOKEN env var is required');
if (!clientId) throw new Error('[index] DISCORD_CLIENT_ID env var is required');

logger.info('index', 'starting bot');
const client = createBot(token, clientId);

try {
  await client.login(token);
} catch (err) {
  logger.error('index', 'login failed', err);
  process.exit(1);
}
