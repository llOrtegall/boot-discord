import { getOrDownloadSong } from '../../application/getOrDownloadSong.ts';
import * as SongCacheRepositoryMother from '../helpers/SongCacheRepositoryMother.ts';
import * as SongSourceMother from '../helpers/SongSourceMother.ts';
import * as AudioFileStoreMother from '../helpers/AudioFileStoreMother.ts';
import * as CachedSongMother from '../helpers/CachedSongMother.ts';
import {
  VALID_QUERY,
  VALID_VIDEO_ID,
  VALID_SOURCE_URL,
  VALID_FILE_PATH,
  NOW,
} from '../fixtures/values.ts';

describe('getOrDownloadSong', () => {
  describe('Basic Behaviour', () => {
    it('downloads and caches when not previously cached', async () => {
      // Arrange
      const songCacheRepository = SongCacheRepositoryMother.create();
      const songSource = SongSourceMother.create();
      const audioFileStore = AudioFileStoreMother.create();

      // Act
      const result = await getOrDownloadSong({
        query: VALID_QUERY,
        now: NOW,
        songCacheRepository,
        songSource,
        audioFileStore,
      });

      // Assert
      expect(songSource.download).toHaveBeenCalledWith(VALID_VIDEO_ID, VALID_SOURCE_URL);
      expect(songCacheRepository.save).toHaveBeenCalledTimes(1);
      expect(result.getVideoId().getValue()).toBe(VALID_VIDEO_ID);
      expect(result.getFilePath().getValue()).toBe(VALID_FILE_PATH);
      expect(result.getLastPlayedAt()).toBe(NOW);
    });
  });

  describe('Edge Cases', () => {
    it('reuses cached file and refreshes lastPlayedAt without downloading', async () => {
      // Arrange
      const existing = CachedSongMother.create({
        videoId: VALID_VIDEO_ID,
        filePath: VALID_FILE_PATH,
        lastPlayedAt: 1,
      });
      const songCacheRepository = SongCacheRepositoryMother.create({
        getByVideoId: jest.fn().mockResolvedValue(existing),
      });
      const songSource = SongSourceMother.create();
      const audioFileStore = AudioFileStoreMother.create({
        exists: jest.fn().mockResolvedValue(true),
      });

      // Act
      const result = await getOrDownloadSong({
        query: VALID_QUERY,
        now: NOW,
        songCacheRepository,
        songSource,
        audioFileStore,
      });

      // Assert
      expect(songSource.download).not.toHaveBeenCalled();
      expect(result.getLastPlayedAt()).toBe(NOW);
      expect(songCacheRepository.save).toHaveBeenCalledTimes(1);
    });

    it('re-downloads when cache row exists but the file is gone', async () => {
      // Arrange
      const existing = CachedSongMother.create({ videoId: VALID_VIDEO_ID });
      const songCacheRepository = SongCacheRepositoryMother.create({
        getByVideoId: jest.fn().mockResolvedValue(existing),
      });
      const songSource = SongSourceMother.create();
      const audioFileStore = AudioFileStoreMother.create({
        exists: jest.fn().mockResolvedValue(false),
      });

      // Act
      await getOrDownloadSong({
        query: VALID_QUERY,
        now: NOW,
        songCacheRepository,
        songSource,
        audioFileStore,
      });

      // Assert
      expect(songSource.download).toHaveBeenCalledTimes(1);
    });
  });

  describe('Error Scenarios', () => {
    it('throws when the source finds no results', async () => {
      // Arrange
      const songCacheRepository = SongCacheRepositoryMother.create();
      const songSource = SongSourceMother.create({
        resolve: jest.fn().mockResolvedValue(null),
      });
      const audioFileStore = AudioFileStoreMother.create();

      // Act & Assert
      await expect(
        getOrDownloadSong({
          query: VALID_QUERY,
          now: NOW,
          songCacheRepository,
          songSource,
          audioFileStore,
        }),
      ).rejects.toThrow('[getOrDownloadSong] No results found');
      expect(songSource.download).not.toHaveBeenCalled();
    });

    it('throws when query is blank', async () => {
      // Arrange
      const songCacheRepository = SongCacheRepositoryMother.create();
      const songSource = SongSourceMother.create();
      const audioFileStore = AudioFileStoreMother.create();

      // Act & Assert
      await expect(
        getOrDownloadSong({
          query: '   ',
          now: NOW,
          songCacheRepository,
          songSource,
          audioFileStore,
        }),
      ).rejects.toThrow('[getOrDownloadSong] query is required');
      expect(songSource.resolve).not.toHaveBeenCalled();
    });
  });
});
