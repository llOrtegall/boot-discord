import { Player } from '../domain/Player.ts';
import type { PlayerRepository } from '../domain/PlayerRepository.ts';

interface PausePlayerProps {
  guildId: string;
  playerRepository: PlayerRepository;
}

export async function pausePlayer({
  guildId,
  playerRepository,
}: PausePlayerProps): Promise<Player> {
  const player = await playerRepository.getByGuildId(guildId);
  if (!player) throw new Error(`[pausePlayer] No player found for guild: ${guildId}`);
  if (!player.getState().isPlaying()) throw new Error('[pausePlayer] Player is not playing');

  const paused = player.pause();
  await playerRepository.save(paused);
  await playerRepository.pause(guildId);

  return paused;
}
