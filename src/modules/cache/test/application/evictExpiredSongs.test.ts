import { evictExpiredSongs } from '../../application/evictExpiredSongs.ts';
import * as SongCacheRepositoryMother from '../helpers/SongCacheRepositoryMother.ts';
import * as AudioFileStoreMother from '../helpers/AudioFileStoreMother.ts';
import * as CachedSongMother from '../helpers/CachedSongMother.ts';
import { CachedSongList } from '../../domain/CachedSongList.ts';
import { TTL_7_DAYS_MS, NOW } from '../fixtures/values.ts';

describe('evictExpiredSongs', () => {
  describe('Basic Behaviour', () => {
    it('deletes expired files and returns the evicted count', async () => {
      // Arrange
      const expired = [
        CachedSongMother.create({ filePath: '/tmp/a.webm' }),
        CachedSongMother.create({ filePath: '/tmp/b.webm' }),
      ];
      const songCacheRepository = SongCacheRepositoryMother.create({
        deleteExpired: jest.fn().mockResolvedValue(CachedSongList.create(expired)),
      });
      const audioFileStore = AudioFileStoreMother.create();

      // Act
      const count = await evictExpiredSongs({
        ttlMs: TTL_7_DAYS_MS,
        now: NOW,
        songCacheRepository,
        audioFileStore,
      });

      // Assert
      expect(count).toBe(2);
      expect(songCacheRepository.deleteExpired).toHaveBeenCalledWith(TTL_7_DAYS_MS, NOW);
      expect(audioFileStore.delete).toHaveBeenCalledTimes(2);
      expect(audioFileStore.delete).toHaveBeenCalledWith('/tmp/a.webm');
      expect(audioFileStore.delete).toHaveBeenCalledWith('/tmp/b.webm');
    });
  });

  describe('Edge Cases', () => {
    it('returns 0 and deletes nothing when no song is expired', async () => {
      // Arrange
      const songCacheRepository = SongCacheRepositoryMother.create({
        deleteExpired: jest.fn().mockResolvedValue(CachedSongList.create([])),
      });
      const audioFileStore = AudioFileStoreMother.create();

      // Act
      const count = await evictExpiredSongs({
        ttlMs: TTL_7_DAYS_MS,
        now: NOW,
        songCacheRepository,
        audioFileStore,
      });

      // Assert
      expect(count).toBe(0);
      expect(audioFileStore.delete).not.toHaveBeenCalled();
    });
  });

  describe('Error Scenarios', () => {
    it('throws when ttlMs is invalid', async () => {
      // Arrange
      const songCacheRepository = SongCacheRepositoryMother.create();
      const audioFileStore = AudioFileStoreMother.create();

      // Act & Assert
      await expect(
        evictExpiredSongs({ ttlMs: -1, now: NOW, songCacheRepository, audioFileStore }),
      ).rejects.toThrow('[evictExpiredSongs] invalid ttlMs');
      expect(songCacheRepository.deleteExpired).not.toHaveBeenCalled();
    });
  });
});
