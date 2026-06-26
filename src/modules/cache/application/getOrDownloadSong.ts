import { CachedSong } from '../domain/CachedSong.ts';
import { VideoId } from '../domain/VideoId.ts';
import { FilePath } from '../domain/FilePath.ts';
import type { SongCacheRepository } from '../domain/SongCacheRepository.ts';
import type { SongSource } from '../domain/SongSource.ts';
import type { AudioFileStore } from '../domain/AudioFileStore.ts';

interface GetOrDownloadSongProps {
  query: string;
  now: number;
  songCacheRepository: SongCacheRepository;
  songSource: SongSource;
  audioFileStore: AudioFileStore;
}

export async function getOrDownloadSong({
  query,
  now,
  songCacheRepository,
  songSource,
  audioFileStore,
}: GetOrDownloadSongProps): Promise<CachedSong> {
  if (!query || query.trim().length === 0) {
    throw new Error('[getOrDownloadSong] query is required');
  }

  const resolved = await songSource.resolve(query);
  if (!resolved) throw new Error('[getOrDownloadSong] No results found');

  const cached = await songCacheRepository.getByVideoId(resolved.videoId);
  if (cached && (await audioFileStore.exists(cached.getFilePath().getValue()))) {
    return songCacheRepository.save(cached.touch(now));
  }

  const filePath = await songSource.download(resolved.videoId, resolved.sourceUrl);
  const song = CachedSong.create(
    VideoId.create(resolved.videoId),
    resolved.title,
    resolved.durationSec,
    resolved.sourceUrl,
    FilePath.create(filePath),
    now,
  );

  return songCacheRepository.save(song);
}
