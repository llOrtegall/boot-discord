import type { SongCacheRepository } from '../../domain/SongCacheRepository.ts';
import { CachedSongList } from '../../domain/CachedSongList.ts';
import * as CachedSongMother from './CachedSongMother.ts';

export function create(overrides?: Partial<SongCacheRepository>): jest.Mocked<SongCacheRepository> {
  return {
    getByVideoId: jest.fn().mockResolvedValue(null),
    save: jest.fn().mockImplementation((song) => Promise.resolve(song)),
    getAll: jest.fn().mockResolvedValue(CachedSongList.create([CachedSongMother.create()])),
    deleteExpired: jest.fn().mockResolvedValue(CachedSongList.create([])),
    ...overrides,
  } as jest.Mocked<SongCacheRepository>;
}
