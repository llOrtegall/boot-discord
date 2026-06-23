import { addSong } from './addSong.ts';
import { getQueue } from './getQueue.ts';
import { removeSong } from './removeSong.ts';
import { clearQueue } from './clearQueue.ts';
import { shiftQueue } from './shiftQueue.ts';
import { InMemoryQueueRepository } from '../infrastructure/InMemoryQueueRepository.ts';

export const queueRepository = new InMemoryQueueRepository();

export const QueueFactory = {
  addSong: (guildId: string, title: string, url: string, duration: number, requestedBy: string) =>
    addSong({ guildId, title, url, duration, requestedBy, queueRepository }),

  getQueue: (guildId: string) => getQueue({ guildId, queueRepository }),

  removeSong: (guildId: string, songId: string) => removeSong({ guildId, songId, queueRepository }),

  clearQueue: (guildId: string) => clearQueue({ guildId, queueRepository }),

  shiftQueue: (guildId: string) => shiftQueue({ guildId, queueRepository }),
};
