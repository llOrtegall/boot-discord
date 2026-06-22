import { Song } from './Song.ts';
import { SongQueue } from './SongQueue.ts';

export interface QueueRepository {
  getByGuildId: (guildId: string) => Promise<SongQueue>;
  save: (guildId: string, queue: SongQueue) => Promise<void>;
  addSong: (guildId: string, song: Song) => Promise<SongQueue>;
  removeSong: (guildId: string, songId: string) => Promise<SongQueue>;
  shift: (guildId: string) => Promise<{ song: Song | null; queue: SongQueue }>;
  clear: (guildId: string) => Promise<void>;
}
