import { Player } from '../domain/Player.ts';
import { PlayerState } from '../domain/PlayerState.ts';
import type { PlayerRepository } from '../domain/PlayerRepository.ts';
import type { QueueRepository } from '../../queue/domain/QueueRepository.ts';

interface PlayNextProps {
  guildId: string;
  channelId: string;
  playerRepository: PlayerRepository;
  queueRepository: QueueRepository;
}

export async function playNext({
  guildId,
  channelId,
  playerRepository,
  queueRepository,
}: PlayNextProps): Promise<Player | null> {
  const { song, queue: _queue } = await queueRepository.shift(guildId);

  if (!song) {
    const existing = await playerRepository.getByGuildId(guildId);
    if (existing) {
      const stopped = existing.stop();
      await playerRepository.save(stopped);
      await playerRepository.stop(guildId);
    }
    return null;
  }

  let player = await playerRepository.getByGuildId(guildId);
  if (!player) {
    player = Player.create(guildId, channelId, PlayerState.create('idle'), null);
  }

  const playing = player.play(song);
  await playerRepository.save(playing);
  await playerRepository.playSong(guildId, song);

  return playing;
}
