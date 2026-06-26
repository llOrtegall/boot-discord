import { playNext } from './playNext.ts';
import { playPrevious } from './playPrevious.ts';
import { pausePlayer } from './pausePlayer.ts';
import { resumePlayer } from './resumePlayer.ts';
import { stopPlayer } from './stopPlayer.ts';
import { skipSong } from './skipSong.ts';
import { DiscordPlayerRepository } from '../infrastructure/DiscordPlayerRepository.ts';
import { queueRepository } from '../../queue/application/factory.ts';

export const playerRepository = new DiscordPlayerRepository();

export const PlayerFactory = {
  playNext: (guildId: string, channelId: string) =>
    playNext({ guildId, channelId, playerRepository, queueRepository }),

  playPrevious: (guildId: string, channelId: string) =>
    playPrevious({ guildId, channelId, playerRepository, queueRepository }),

  pausePlayer: (guildId: string) => pausePlayer({ guildId, playerRepository }),

  resumePlayer: (guildId: string) => resumePlayer({ guildId, playerRepository }),

  stopPlayer: (guildId: string) => stopPlayer({ guildId, playerRepository, queueRepository }),

  skipSong: (guildId: string, channelId: string) =>
    skipSong({ guildId, channelId, playerRepository, queueRepository }),
};
