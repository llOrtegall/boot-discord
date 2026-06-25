import { CachedSong } from './CachedSong.ts';
import { CachedSongList } from './CachedSongList.ts';

export interface SongCacheRepository {
  getByVideoId: (videoId: string) => Promise<CachedSong | null>;
  save: (song: CachedSong) => Promise<CachedSong>;
  getAll: () => Promise<CachedSongList>;
  deleteExpired: (ttlMs: number, now: number) => Promise<CachedSongList>;
}
