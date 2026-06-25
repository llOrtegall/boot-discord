export interface ResolvedSong {
  videoId: string;
  title: string;
  durationSec: number;
  sourceUrl: string;
}

export interface SongSource {
  resolve: (query: string) => Promise<ResolvedSong | null>;
  download: (videoId: string, sourceUrl: string) => Promise<string>;
}
