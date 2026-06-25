import { VideoId } from './VideoId.ts';
import { FilePath } from './FilePath.ts';

export class CachedSong {
  private readonly videoId: VideoId;
  private readonly title: string;
  private readonly durationSec: number;
  private readonly sourceUrl: string;
  private readonly filePath: FilePath;
  private readonly lastPlayedAt: number;

  static create(
    videoId: VideoId,
    title: string,
    durationSec: number,
    sourceUrl: string,
    filePath: FilePath,
    lastPlayedAt: number,
  ): CachedSong {
    CachedSong.ensureIsValid(videoId, title, durationSec, sourceUrl, filePath, lastPlayedAt);
    return new CachedSong(videoId, title, durationSec, sourceUrl, filePath, lastPlayedAt);
  }

  static fromPrimitive(data: any): CachedSong {
    if (!data) throw new Error('[CachedSong] data must be provided');
    return CachedSong.create(
      VideoId.fromPrimitive(data.videoId),
      String(data.title),
      Number(data.durationSec),
      String(data.sourceUrl),
      FilePath.fromPrimitive(data.filePath),
      Number(data.lastPlayedAt),
    );
  }

  static ensureIsValid(
    videoId: VideoId,
    title: string,
    durationSec: number,
    sourceUrl: string,
    filePath: FilePath,
    lastPlayedAt: number,
  ): void {
    if (!(videoId instanceof VideoId)) throw new Error('[CachedSong] invalid videoId');
    if (!title || typeof title !== 'string') throw new Error('[CachedSong] invalid title');
    if (typeof durationSec !== 'number' || durationSec < 0 || !isFinite(durationSec)) {
      throw new Error('[CachedSong] invalid durationSec');
    }
    if (!sourceUrl || typeof sourceUrl !== 'string')
      throw new Error('[CachedSong] invalid sourceUrl');
    if (!(filePath instanceof FilePath)) throw new Error('[CachedSong] invalid filePath');
    if (typeof lastPlayedAt !== 'number' || lastPlayedAt < 0 || !isFinite(lastPlayedAt)) {
      throw new Error('[CachedSong] invalid lastPlayedAt');
    }
  }

  private constructor(
    videoId: VideoId,
    title: string,
    durationSec: number,
    sourceUrl: string,
    filePath: FilePath,
    lastPlayedAt: number,
  ) {
    this.videoId = videoId;
    this.title = title;
    this.durationSec = durationSec;
    this.sourceUrl = sourceUrl;
    this.filePath = filePath;
    this.lastPlayedAt = lastPlayedAt;
  }

  getVideoId(): VideoId {
    return this.videoId;
  }
  getTitle(): string {
    return this.title;
  }
  getDurationSec(): number {
    return this.durationSec;
  }
  getSourceUrl(): string {
    return this.sourceUrl;
  }
  getFilePath(): FilePath {
    return this.filePath;
  }
  getLastPlayedAt(): number {
    return this.lastPlayedAt;
  }

  touch(now: number): CachedSong {
    return CachedSong.create(
      this.videoId,
      this.title,
      this.durationSec,
      this.sourceUrl,
      this.filePath,
      now,
    );
  }

  isExpired(ttlMs: number, now: number): boolean {
    return now - this.lastPlayedAt > ttlMs;
  }

  equals(other: CachedSong): boolean {
    return this.videoId.equals(other.videoId);
  }

  toPrimitive(): object {
    return {
      videoId: this.videoId.toPrimitive(),
      title: this.title,
      durationSec: this.durationSec,
      sourceUrl: this.sourceUrl,
      filePath: this.filePath.toPrimitive(),
      lastPlayedAt: this.lastPlayedAt,
    };
  }
}
