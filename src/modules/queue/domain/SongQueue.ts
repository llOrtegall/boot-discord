import { Song } from './Song.ts';

export class SongQueue {
  private readonly songs: Song[];

  static create(songs: Song[] | null): SongQueue {
    return new SongQueue(songs);
  }

  static fromPrimitive(songs: any[] | null): SongQueue {
    if (songs === null) return SongQueue.create(null);
    return SongQueue.create(songs.map(Song.fromPrimitive));
  }

  private constructor(songs: Song[] | null) {
    this.songs = songs ?? [];
  }

  getAll(): Song[] {
    return [...this.songs];
  }
  getFirst(): Song | null {
    return this.songs[0] ?? null;
  }
  isEmpty(): boolean {
    return this.songs.length === 0;
  }
  count(): number {
    return this.songs.length;
  }

  add(song: Song): SongQueue {
    return SongQueue.create([...this.songs, song]);
  }

  shift(): { song: Song | null; queue: SongQueue } {
    if (this.songs.length === 0) return { song: null, queue: this };
    const [first, ...rest] = this.songs;
    return { song: first ?? null, queue: SongQueue.create(rest) };
  }

  remove(id: string): SongQueue {
    return SongQueue.create(this.songs.filter((s) => s.getId() !== id));
  }

  clear(): SongQueue {
    return SongQueue.create([]);
  }

  equals(other: SongQueue): boolean {
    if (this.songs.length !== other.songs.length) return false;
    return this.songs.every((s, i) => s.equals(other.songs[i]!));
  }

  toPrimitive(): object[] {
    return this.songs.map((s) => s.toPrimitive());
  }
}
