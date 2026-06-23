import { Song } from '../domain/Song.ts';
import { SongDuration } from '../domain/SongDuration.ts';
import { SongUrl } from '../domain/SongUrl.ts';
import { SongQueue } from '../domain/SongQueue.ts';
import type { QueueRepository } from '../domain/QueueRepository.ts';
import { randomUUID } from 'crypto';

interface AddSongProps {
  guildId: string;
  title: string;
  url: string;
  duration: number;
  requestedBy: string;
  queueRepository: QueueRepository;
}

export async function addSong({
  guildId,
  title,
  url,
  duration,
  requestedBy,
  queueRepository,
}: AddSongProps): Promise<SongQueue> {
  const song = Song.create(
    randomUUID(),
    title,
    SongUrl.create(url),
    SongDuration.create(duration),
    requestedBy,
  );

  return queueRepository.addSong(guildId, song);
}
