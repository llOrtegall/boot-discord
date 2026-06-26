import type { SongSource, ResolvedSong } from '../../domain/SongSource.ts';
import {
  VALID_VIDEO_ID,
  VALID_TITLE,
  VALID_DURATION_SEC,
  VALID_SOURCE_URL,
  VALID_FILE_PATH,
} from '../fixtures/values.ts';

export function resolved(overrides?: Partial<ResolvedSong>): ResolvedSong {
  return {
    videoId: overrides?.videoId ?? VALID_VIDEO_ID,
    title: overrides?.title ?? VALID_TITLE,
    durationSec: overrides?.durationSec ?? VALID_DURATION_SEC,
    sourceUrl: overrides?.sourceUrl ?? VALID_SOURCE_URL,
  };
}

export function create(overrides?: Partial<SongSource>): jest.Mocked<SongSource> {
  return {
    resolve: jest.fn().mockResolvedValue(resolved()),
    download: jest.fn().mockResolvedValue(VALID_FILE_PATH),
    ...overrides,
  } as jest.Mocked<SongSource>;
}
