import { SongDuration } from './SongDuration.ts';
import { SongUrl } from './SongUrl.ts';

export class Song {
  private readonly id: string;
  private readonly title: string;
  private readonly url: SongUrl;
  private readonly duration: SongDuration;
  private readonly requestedBy: string;

  static create(
    id: string,
    title: string,
    url: SongUrl,
    duration: SongDuration,
    requestedBy: string,
  ): Song {
    Song.ensureIsValid(id, title, url, duration, requestedBy);
    return new Song(id, title, url, duration, requestedBy);
  }

  static fromPrimitive(data: any): Song {
    if (!data) throw new Error('[Song] data must be provided');
    return Song.create(
      String(data.id),
      String(data.title),
      SongUrl.fromPrimitive(data.url),
      SongDuration.fromPrimitive(data.duration),
      String(data.requestedBy),
    );
  }

  static ensureIsValid(
    id: string,
    title: string,
    url: SongUrl,
    duration: SongDuration,
    requestedBy: string,
  ): void {
    if (!id || typeof id !== 'string') throw new Error('[Song] invalid id');
    if (!title || typeof title !== 'string') throw new Error('[Song] invalid title');
    if (!(url instanceof SongUrl)) throw new Error('[Song] invalid url');
    if (!(duration instanceof SongDuration)) throw new Error('[Song] invalid duration');
    if (!requestedBy || typeof requestedBy !== 'string')
      throw new Error('[Song] invalid requestedBy');
  }

  private constructor(
    id: string,
    title: string,
    url: SongUrl,
    duration: SongDuration,
    requestedBy: string,
  ) {
    this.id = id;
    this.title = title;
    this.url = url;
    this.duration = duration;
    this.requestedBy = requestedBy;
  }

  getId(): string {
    return this.id;
  }
  getTitle(): string {
    return this.title;
  }
  getUrl(): SongUrl {
    return this.url;
  }
  getDuration(): SongDuration {
    return this.duration;
  }
  getRequestedBy(): string {
    return this.requestedBy;
  }

  equals(other: Song): boolean {
    return this.id === other.id;
  }

  toPrimitive(): object {
    return {
      id: this.id,
      title: this.title,
      url: this.url.toPrimitive(),
      duration: this.duration.toPrimitive(),
      requestedBy: this.requestedBy,
    };
  }
}
