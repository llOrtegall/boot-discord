import type { SongCacheRepository } from '../domain/SongCacheRepository.ts';
import type { AudioFileStore } from '../domain/AudioFileStore.ts';

interface EvictExpiredSongsProps {
  ttlMs: number;
  now: number;
  songCacheRepository: SongCacheRepository;
  audioFileStore: AudioFileStore;
}

export async function evictExpiredSongs({
  ttlMs,
  now,
  songCacheRepository,
  audioFileStore,
}: EvictExpiredSongsProps): Promise<number> {
  if (typeof ttlMs !== 'number' || ttlMs < 0 || !isFinite(ttlMs)) {
    throw new Error('[evictExpiredSongs] invalid ttlMs');
  }

  const expired = await songCacheRepository.deleteExpired(ttlMs, now);

  for (const song of expired.getAll()) {
    await audioFileStore.delete(song.getFilePath().getValue());
  }

  return expired.count();
}
