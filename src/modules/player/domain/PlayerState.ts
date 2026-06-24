export class PlayerState {
  private readonly value: 'idle' | 'playing' | 'paused';

  static create(value: string): PlayerState {
    PlayerState.ensureIsValid(value);
    return new PlayerState(value as PlayerState['value']);
  }

  static fromPrimitive(value: any): PlayerState {
    return PlayerState.create(String(value));
  }

  static ensureIsValid(value: string): void {
    const valid = ['idle', 'playing', 'paused'];
    if (!valid.includes(value)) {
      throw new Error(`[PlayerState] Invalid state: ${value}`);
    }
  }

  private constructor(value: PlayerState['value']) {
    this.value = value;
  }

  getValue(): PlayerState['value'] {
    return this.value;
  }
  toPrimitive(): string {
    return this.value;
  }
  equals(other: PlayerState): boolean {
    return this.value === other.value;
  }

  isIdle(): boolean {
    return this.value === 'idle';
  }
  isPlaying(): boolean {
    return this.value === 'playing';
  }
  isPaused(): boolean {
    return this.value === 'paused';
  }
}
