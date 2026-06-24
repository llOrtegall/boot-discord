import { PlayerState } from './PlayerState.ts';
import { Song } from '../../queue/domain/Song.ts';

export class Player {
  private readonly guildId: string;
  private readonly channelId: string;
  private readonly state: PlayerState;
  private readonly currentSong: Song | null;

  static create(
    guildId: string,
    channelId: string,
    state: PlayerState,
    currentSong: Song | null,
  ): Player {
    Player.ensureIsValid(guildId, channelId, state);
    return new Player(guildId, channelId, state, currentSong);
  }

  static fromPrimitive(data: any): Player {
    if (!data) throw new Error('[Player] data must be provided');
    return Player.create(
      String(data.guildId),
      String(data.channelId),
      PlayerState.fromPrimitive(data.state),
      data.currentSong ? Song.fromPrimitive(data.currentSong) : null,
    );
  }

  static ensureIsValid(guildId: string, channelId: string, state: PlayerState): void {
    if (!guildId || typeof guildId !== 'string') throw new Error('[Player] invalid guildId');
    if (!channelId || typeof channelId !== 'string') throw new Error('[Player] invalid channelId');
    if (!(state instanceof PlayerState)) throw new Error('[Player] invalid state');
  }

  private constructor(
    guildId: string,
    channelId: string,
    state: PlayerState,
    currentSong: Song | null,
  ) {
    this.guildId = guildId;
    this.channelId = channelId;
    this.state = state;
    this.currentSong = currentSong;
  }

  getGuildId(): string {
    return this.guildId;
  }
  getChannelId(): string {
    return this.channelId;
  }
  getState(): PlayerState {
    return this.state;
  }
  getCurrentSong(): Song | null {
    return this.currentSong;
  }

  play(song: Song): Player {
    return Player.create(this.guildId, this.channelId, PlayerState.create('playing'), song);
  }

  pause(): Player {
    return Player.create(
      this.guildId,
      this.channelId,
      PlayerState.create('paused'),
      this.currentSong,
    );
  }

  resume(): Player {
    return Player.create(
      this.guildId,
      this.channelId,
      PlayerState.create('playing'),
      this.currentSong,
    );
  }

  stop(): Player {
    return Player.create(this.guildId, this.channelId, PlayerState.create('idle'), null);
  }

  equals(other: Player): boolean {
    return this.guildId === other.guildId;
  }

  toPrimitive(): object {
    return {
      guildId: this.guildId,
      channelId: this.channelId,
      state: this.state.toPrimitive(),
      currentSong: this.currentSong?.toPrimitive() ?? null,
    };
  }
}
