import { Database } from 'bun:sqlite';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import type { SongCacheRepository } from '../domain/SongCacheRepository.ts';
import { CachedSong } from '../domain/CachedSong.ts';
import { CachedSongList } from '../domain/CachedSongList.ts';

interface Row {
  video_id: string;
  title: string;
  duration_sec: number;
  source_url: string;
  file_path: string;
  last_played_at: number;
}

export class SqliteSongCacheRepository implements SongCacheRepository {
  private readonly db: Database;

  constructor(dbPath: string) {
    if (dbPath !== ':memory:') mkdirSync(dirname(dbPath), { recursive: true });
    this.db = new Database(dbPath, { create: true });
    this.migrate();
  }

  private migrate(): void {
    this.db.run(`
      CREATE TABLE IF NOT EXISTS cached_songs (
        video_id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        duration_sec INTEGER NOT NULL,
        source_url TEXT NOT NULL,
        file_path TEXT NOT NULL,
        last_played_at INTEGER NOT NULL
      )
    `);
  }

  private toDomain(row: Row): CachedSong {
    return CachedSong.fromPrimitive({
      videoId: row.video_id,
      title: row.title,
      durationSec: row.duration_sec,
      sourceUrl: row.source_url,
      filePath: row.file_path,
      lastPlayedAt: row.last_played_at,
    });
  }

  async getByVideoId(videoId: string): Promise<CachedSong | null> {
    try {
      const row = this.db
        .query('SELECT * FROM cached_songs WHERE video_id = ?')
        .get(videoId) as Row | null;
      return row ? this.toDomain(row) : null;
    } catch (err: any) {
      console.error('[SqliteSongCacheRepository.getByVideoId]', err.message);
      return null;
    }
  }

  async save(song: CachedSong): Promise<CachedSong> {
    const p = song.toPrimitive() as any;
    this.db
      .query(
        `INSERT INTO cached_songs (video_id, title, duration_sec, source_url, file_path, last_played_at)
         VALUES ($videoId, $title, $durationSec, $sourceUrl, $filePath, $lastPlayedAt)
         ON CONFLICT(video_id) DO UPDATE SET
           title = excluded.title,
           duration_sec = excluded.duration_sec,
           source_url = excluded.source_url,
           file_path = excluded.file_path,
           last_played_at = excluded.last_played_at`,
      )
      .run({
        $videoId: p.videoId,
        $title: p.title,
        $durationSec: p.durationSec,
        $sourceUrl: p.sourceUrl,
        $filePath: p.filePath,
        $lastPlayedAt: p.lastPlayedAt,
      });
    return song;
  }

  async getAll(): Promise<CachedSongList> {
    try {
      const rows = this.db.query('SELECT * FROM cached_songs').all() as Row[];
      return CachedSongList.create(rows.map((r) => this.toDomain(r)));
    } catch (err: any) {
      console.error('[SqliteSongCacheRepository.getAll]', err.message);
      return CachedSongList.create([]);
    }
  }

  async deleteExpired(ttlMs: number, now: number): Promise<CachedSongList> {
    const cutoff = now - ttlMs;
    try {
      const rows = this.db
        .query('SELECT * FROM cached_songs WHERE last_played_at < ?')
        .all(cutoff) as Row[];
      this.db.query('DELETE FROM cached_songs WHERE last_played_at < ?').run(cutoff);
      return CachedSongList.create(rows.map((r) => this.toDomain(r)));
    } catch (err: any) {
      console.error('[SqliteSongCacheRepository.deleteExpired]', err.message);
      return CachedSongList.create([]);
    }
  }
}
