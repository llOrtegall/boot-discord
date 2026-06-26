import { SqliteSongCacheRepository } from '../../infrastructure/SqliteSongCacheRepository.ts';
import * as CachedSongMother from '../helpers/CachedSongMother.ts';
import { TTL_7_DAYS_MS, DAY_MS, NOW } from '../fixtures/values.ts';

describe('SqliteSongCacheRepository (integration)', () => {
  let repository: SqliteSongCacheRepository;

  beforeEach(() => {
    repository = new SqliteSongCacheRepository(':memory:');
  });

  describe('Basic Behaviour', () => {
    it('saves and retrieves a song by video id', async () => {
      // Arrange
      const song = CachedSongMother.create({ videoId: 'abc12345678' });

      // Act
      await repository.save(song);
      const found = await repository.getByVideoId('abc12345678');

      // Assert
      expect(found).not.toBeNull();
      expect(found!.equals(song)).toBe(true);
      expect(found!.getTitle()).toBe(song.getTitle());
      expect(found!.getFilePath().getValue()).toBe(song.getFilePath().getValue());
    });

    it('upserts on conflicting video id', async () => {
      // Arrange
      const first = CachedSongMother.create({ videoId: 'dup12345678', lastPlayedAt: 1 });
      const updated = first.touch(NOW);

      // Act
      await repository.save(first);
      await repository.save(updated);
      const found = await repository.getByVideoId('dup12345678');
      const all = await repository.getAll();

      // Assert
      expect(all.count()).toBe(1);
      expect(found!.getLastPlayedAt()).toBe(NOW);
    });
  });

  describe('Edge Cases', () => {
    it('returns null for a missing video id', async () => {
      // Act
      const found = await repository.getByVideoId('does-not-exist');

      // Assert
      expect(found).toBeNull();
    });

    it('deletes only songs older than the ttl and returns them', async () => {
      // Arrange
      const fresh = CachedSongMother.create({ videoId: 'fresh111111', lastPlayedAt: NOW });
      const stale = CachedSongMother.create({
        videoId: 'stale111111',
        lastPlayedAt: NOW - 8 * DAY_MS,
      });
      await repository.save(fresh);
      await repository.save(stale);

      // Act
      const deleted = await repository.deleteExpired(TTL_7_DAYS_MS, NOW);
      const remaining = await repository.getAll();

      // Assert
      expect(deleted.count()).toBe(1);
      expect(deleted.getByVideoId('stale111111')).not.toBeNull();
      expect(remaining.count()).toBe(1);
      expect(remaining.getByVideoId('fresh111111')).not.toBeNull();
    });
  });
});
