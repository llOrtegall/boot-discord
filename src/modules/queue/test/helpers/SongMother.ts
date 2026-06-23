import { faker } from '@faker-js/faker';
import { Song } from '../../domain/Song.ts';
import { SongDuration } from '../../domain/SongDuration.ts';
import { SongUrl } from '../../domain/SongUrl.ts';

export function create(
  overrides?: Partial<{
    id: string;
    title: string;
    url: string;
    duration: number;
    requestedBy: string;
  }>,
): Song {
  return Song.create(
    overrides?.id ?? faker.string.uuid(),
    overrides?.title ?? faker.music.songName(),
    SongUrl.create(
      overrides?.url ?? `https://www.youtube.com/watch?v=${faker.string.alphanumeric(11)}`,
    ),
    SongDuration.create(overrides?.duration ?? faker.number.int({ min: 60, max: 600 })),
    overrides?.requestedBy ?? faker.internet.username(),
  );
}

export function createInvalid() {
  return { id: '', title: '', url: '', duration: -1, requestedBy: '' };
}
