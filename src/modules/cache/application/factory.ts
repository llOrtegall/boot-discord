import { getOrDownloadSong } from './getOrDownloadSong.ts';
import { evictExpiredSongs } from './evictExpiredSongs.ts';
import { SqliteSongCacheRepository } from '../infrastructure/SqliteSongCacheRepository.ts';
import { YtDlpSongSource } from '../infrastructure/YtDlpSongSource.ts';
import { LocalAudioFileStore } from '../infrastructure/LocalAudioFileStore.ts';

const DB_PATH = process.env['CACHE_DB_PATH'] ?? './.cache/songs.db';
const AUDIO_DIR = process.env['CACHE_AUDIO_DIR'] ?? './.cache/audio';
const TTL_DAYS = Number(process.env['CACHE_TTL_DAYS'] ?? 7);
const TTL_MS = TTL_DAYS * 24 * 60 * 60 * 1000;
const YT_DLP_COOKIES = process.env['YT_DLP_COOKIES'];
const YT_DLP_EXTRACTOR_ARGS = process.env['YT_DLP_EXTRACTOR_ARGS'];

const songCacheRepository = new SqliteSongCacheRepository(DB_PATH);
const songSource = new YtDlpSongSource(AUDIO_DIR, {
  cookiesFile: YT_DLP_COOKIES,
  extractorArgs: YT_DLP_EXTRACTOR_ARGS,
});
const audioFileStore = new LocalAudioFileStore();

export const CacheFactory = {
  getOrDownloadSong: (query: string) =>
    getOrDownloadSong({ query, now: Date.now(), songCacheRepository, songSource, audioFileStore }),

  evictExpiredSongs: () =>
    evictExpiredSongs({ ttlMs: TTL_MS, now: Date.now(), songCacheRepository, audioFileStore }),
};
