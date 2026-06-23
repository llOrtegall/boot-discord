import { addSong } from '../../application/addSong.ts';
import * as QueueRepositoryMother from '../helpers/QueueRepositoryMother.ts';
import { VALID_GUILD_ID, VALID_URL, VALID_DURATION } from '../fixtures/values.ts';

describe('addSong', () => {
  describe('Basic Behaviour', () => {
    it('adds song to queue via repository', async () => {
      // Arrange
      const queueRepository = QueueRepositoryMother.create();

      // Act
      await addSong({
        guildId: VALID_GUILD_ID,
        title: 'Never Gonna Give You Up',
        url: VALID_URL,
        duration: VALID_DURATION,
        requestedBy: 'user123',
        queueRepository,
      });

      // Assert
      expect(queueRepository.addSong).toHaveBeenCalledTimes(1);
      const [calledGuildId, calledSong] = queueRepository.addSong.mock.calls[0]!;
      expect(calledGuildId).toBe(VALID_GUILD_ID);
      expect(calledSong.getTitle()).toBe('Never Gonna Give You Up');
      expect(calledSong.getUrl().getValue()).toBe(VALID_URL);
    });
  });

  describe('Edge Cases', () => {
    it('generates unique id per song', async () => {
      // Arrange
      const queueRepository = QueueRepositoryMother.create();

      // Act
      await addSong({
        guildId: VALID_GUILD_ID,
        title: 'A',
        url: VALID_URL,
        duration: 60,
        requestedBy: 'u',
        queueRepository,
      });
      await addSong({
        guildId: VALID_GUILD_ID,
        title: 'B',
        url: VALID_URL,
        duration: 60,
        requestedBy: 'u',
        queueRepository,
      });

      // Assert
      const idA = queueRepository.addSong.mock.calls[0]![1]!.getId();
      const idB = queueRepository.addSong.mock.calls[1]![1]!.getId();
      expect(idA).not.toBe(idB);
    });
  });

  describe('Error Scenarios', () => {
    it('throws when url is empty', async () => {
      // Arrange
      const queueRepository = QueueRepositoryMother.create();

      // Act & Assert
      await expect(
        addSong({
          guildId: VALID_GUILD_ID,
          title: 'A',
          url: '',
          duration: 60,
          requestedBy: 'u',
          queueRepository,
        }),
      ).rejects.toThrow('[SongUrl] Invalid url');
    });

    it('throws when duration is negative', async () => {
      // Arrange
      const queueRepository = QueueRepositoryMother.create();

      // Act & Assert
      await expect(
        addSong({
          guildId: VALID_GUILD_ID,
          title: 'A',
          url: VALID_URL,
          duration: -1,
          requestedBy: 'u',
          queueRepository,
        }),
      ).rejects.toThrow('[SongDuration] Invalid duration');
    });
  });
});
