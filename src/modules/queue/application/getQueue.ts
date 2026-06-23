import { SongQueue } from '../domain/SongQueue.ts';
import type { QueueRepository } from '../domain/QueueRepository.ts';

interface GetQueueProps {
  guildId: string;
  queueRepository: QueueRepository;
}

export async function getQueue({ guildId, queueRepository }: GetQueueProps): Promise<SongQueue> {
  return queueRepository.getByGuildId(guildId);
}
