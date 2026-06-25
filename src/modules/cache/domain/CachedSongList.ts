import { CachedSong } from './CachedSong.ts';

export class CachedSongList {
  private readonly songs: CachedSong[];

  static create(songs: CachedSong[] | null): CachedSongList {
    return new CachedSongList(songs);
  }

  static fromPrimitive(songs: any[] | null): CachedSongList {
    if (songs === null) return CachedSongList.create(null);
    return CachedSongList.create(songs.map(CachedSong.fromPrimitive));
  }

  private constructor(songs: CachedSong[] | null) {
    this.songs = songs ?? [];
  }

  getAll(): CachedSong[] {
    return [...this.songs];
  }
  getByVideoId(videoId: string): CachedSong | null {
    return this.songs.find((s) => s.getVideoId().getValue() === videoId) ?? null;
  }
  isEmpty(): boolean {
    return this.songs.length === 0;
  }
  count(): number {
    return this.songs.length;
  }

  toPrimitive(): object[] {
    return this.songs.map((s) => s.toPrimitive());
  }
}
