import { Player } from '../domain/Player.ts';
import type { PlayerRepository } from '../domain/PlayerRepository.ts';
import type { QueueRepository } from '../../queue/domain/QueueRepository.ts';
import { playNext } from './playNext.ts';

interface SkipSongProps {
  guildId: string;
  channelId: string;
  playerRepository: PlayerRepository;
  queueRepository: QueueRepository;
}

export async function skipSong({
  guildId,
  channelId,
  playerRepository,
  queueRepository,
}: SkipSongProps): Promise<Player | null> {
  const player = await playerRepository.getByGuildId(guildId);
  if (!player) throw new Error(`[skipSong] No player found for guild: ${guildId}`);
  if (player.getState().isIdle()) throw new Error('[skipSong] Nothing is playing');

  return playNext({ guildId, channelId, playerRepository, queueRepository });
}
