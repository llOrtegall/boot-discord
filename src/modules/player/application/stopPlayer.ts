import type { PlayerRepository } from '../domain/PlayerRepository.ts';
import type { QueueRepository } from '../../queue/domain/QueueRepository.ts';

interface StopPlayerProps {
  guildId: string;
  playerRepository: PlayerRepository;
  queueRepository: QueueRepository;
}

export async function stopPlayer({
  guildId,
  playerRepository,
  queueRepository,
}: StopPlayerProps): Promise<void> {
  const player = await playerRepository.getByGuildId(guildId);
  if (!player) throw new Error(`[stopPlayer] No player found for guild: ${guildId}`);

  const stopped = player.stop();
  await playerRepository.save(stopped);
  await playerRepository.stop(guildId);
  await queueRepository.clear(guildId);
}
