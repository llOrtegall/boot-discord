export class SongDuration {
  private readonly value: number;

  static create(value: number): SongDuration {
    SongDuration.ensureIsValid(value);
    return new SongDuration(value);
  }

  static fromPrimitive(value: any): SongDuration {
    return SongDuration.create(Number(value));
  }

  static ensureIsValid(value: number): void {
    if (typeof value !== 'number' || value < 0 || !isFinite(value)) {
      throw new Error(`[SongDuration] Invalid duration: ${value}`);
    }
  }

  private constructor(value: number) {
    this.value = value;
  }

  getValue(): number {
    return this.value;
  }
  toPrimitive(): number {
    return this.value;
  }
  equals(other: SongDuration): boolean {
    return this.value === other.value;
  }

  toFormatted(): string {
    const minutes = Math.floor(this.value / 60);
    const seconds = this.value % 60;
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  }
}
