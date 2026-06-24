import { Player } from './Player.ts';
import { Song } from '../../queue/domain/Song.ts';

export interface PlayerRepository {
  getByGuildId: (guildId: string) => Promise<Player | null>;
  save: (player: Player) => Promise<void>;
  playSong: (guildId: string, song: Song) => Promise<void>;
  pause: (guildId: string) => Promise<void>;
  resume: (guildId: string) => Promise<void>;
  stop: (guildId: string) => Promise<void>;
  destroy: (guildId: string) => Promise<void>;
}
