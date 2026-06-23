import { SongQueue } from '../domain/SongQueue.ts';
import type { QueueRepository } from '../domain/QueueRepository.ts';

interface RemoveSongProps {
  guildId: string;
  songId: string;
  queueRepository: QueueRepository;
}

export async function removeSong({
  guildId,
  songId,
  queueRepository,
}: RemoveSongProps): Promise<SongQueue> {
  const queue = await queueRepository.getByGuildId(guildId);

  const song = queue.getAll().find((s) => s.getId() === songId);
  if (!song) throw new Error(`[removeSong] Song not found: ${songId}`);

  return queueRepository.removeSong(guildId, songId);
}
