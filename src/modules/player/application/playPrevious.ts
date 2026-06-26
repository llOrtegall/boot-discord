import { Player } from '../domain/Player.ts';
import { PlayerState } from '../domain/PlayerState.ts';
import type { PlayerRepository } from '../domain/PlayerRepository.ts';
import type { QueueRepository } from '../../queue/domain/QueueRepository.ts';

interface PlayPreviousProps {
  guildId: string;
  channelId: string;
  playerRepository: PlayerRepository;
  queueRepository: QueueRepository;
}

export async function playPrevious({
  guildId,
  channelId,
  playerRepository,
  queueRepository,
}: PlayPreviousProps): Promise<Player | null> {
  const previous = await queueRepository.popHistory(guildId);
  if (!previous) return null;

  let player = await playerRepository.getByGuildId(guildId);

  const current = player?.getCurrentSong() ?? null;
  if (current) await queueRepository.unshift(guildId, current);

  if (!player) {
    player = Player.create(guildId, channelId, PlayerState.create('idle'), null);
  }

  const playing = player.play(previous);
  await playerRepository.save(playing);
  await playerRepository.playSong(guildId, previous);

  return playing;
}
