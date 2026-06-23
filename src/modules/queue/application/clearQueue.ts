import type { QueueRepository } from '../domain/QueueRepository.ts';

interface ClearQueueProps {
  guildId: string;
  queueRepository: QueueRepository;
}

export async function clearQueue({ guildId, queueRepository }: ClearQueueProps): Promise<void> {
  await queueRepository.clear(guildId);
}
