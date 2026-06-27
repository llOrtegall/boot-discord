import { createBot } from './modules/bot/infrastructure/BotRouter.ts';

const token = process.env['DISCORD_TOKEN'];
const clientId = process.env['DISCORD_CLIENT_ID'];

if (!token) throw new Error('[index] DISCORD_TOKEN env var is required');
if (!clientId) throw new Error('[index] DISCORD_CLIENT_ID env var is required');

const client = createBot(token, clientId);
await client.login(token);
