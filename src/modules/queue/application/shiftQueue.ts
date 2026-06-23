import { Song } from '../domain/Song.ts';
import { SongQueue } from '../domain/SongQueue.ts';
import type { QueueRepository } from '../domain/QueueRepository.ts';

interface ShiftQueueProps {
  guildId: string;
  queueRepository: QueueRepository;
}

export async function shiftQueue({
  guildId,
  queueRepository,
}: ShiftQueueProps): Promise<{ song: Song | null; queue: SongQueue }> {
  return queueRepository.shift(guildId);
}
