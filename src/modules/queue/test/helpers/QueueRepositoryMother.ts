import type { QueueRepository } from '../../domain/QueueRepository.ts';
import { SongQueue } from '../../domain/SongQueue.ts';
import * as SongMother from './SongMother.ts';

export function create(overrides?: Partial<QueueRepository>): jest.Mocked<QueueRepository> {
  const defaultQueue = SongQueue.create([SongMother.create()]);

  return {
    getByGuildId: jest.fn().mockResolvedValue(defaultQueue),
    save: jest.fn().mockResolvedValue(undefined),
    addSong: jest
      .fn()
      .mockImplementation((_guildId, song) => Promise.resolve(defaultQueue.add(song))),
    unshift: jest
      .fn()
      .mockImplementation((_guildId, song) => Promise.resolve(defaultQueue.prepend(song))),
    removeSong: jest.fn().mockResolvedValue(SongQueue.create([])),
    shift: jest.fn().mockResolvedValue({ song: SongMother.create(), queue: SongQueue.create([]) }),
    clear: jest.fn().mockResolvedValue(undefined),
    pushHistory: jest.fn().mockResolvedValue(undefined),
    popHistory: jest.fn().mockResolvedValue(null),
    ...overrides,
  } as jest.Mocked<QueueRepository>;
}
