import { faker } from '@faker-js/faker';
import { CachedSong } from '../../domain/CachedSong.ts';
import { VideoId } from '../../domain/VideoId.ts';
import { FilePath } from '../../domain/FilePath.ts';

export function create(
  overrides?: Partial<{
    videoId: string;
    title: string;
    durationSec: number;
    sourceUrl: string;
    filePath: string;
    lastPlayedAt: number;
  }>,
): CachedSong {
  const videoId = overrides?.videoId ?? faker.string.alphanumeric(11);
  return CachedSong.create(
    VideoId.create(videoId),
    overrides?.title ?? faker.music.songName(),
    overrides?.durationSec ?? faker.number.int({ min: 60, max: 600 }),
    overrides?.sourceUrl ?? `https://www.youtube.com/watch?v=${videoId}`,
    FilePath.create(overrides?.filePath ?? `/tmp/cache/audio/${videoId}.webm`),
    overrides?.lastPlayedAt ?? faker.number.int({ min: 1, max: 1_700_000_000_000 }),
  );
}

export function createInvalid() {
  return { videoId: '', title: '', durationSec: -1, sourceUrl: '', filePath: '', lastPlayedAt: -1 };
}
