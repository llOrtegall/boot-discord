import { Player } from '../domain/Player.ts';
import type { PlayerRepository } from '../domain/PlayerRepository.ts';

interface ResumePlayerProps {
  guildId: string;
  playerRepository: PlayerRepository;
}

export async function resumePlayer({
  guildId,
  playerRepository,
}: ResumePlayerProps): Promise<Player> {
  const player = await playerRepository.getByGuildId(guildId);
  if (!player) throw new Error(`[resumePlayer] No player found for guild: ${guildId}`);
  if (!player.getState().isPaused()) throw new Error('[resumePlayer] Player is not paused');

  const resumed = player.resume();
  await playerRepository.save(resumed);
  await playerRepository.resume(guildId);

  return resumed;
}
