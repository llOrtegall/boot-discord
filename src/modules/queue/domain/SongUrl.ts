export class SongUrl {
  private readonly value: string;

  static create(value: string): SongUrl {
    SongUrl.ensureIsValid(value);
    return new SongUrl(value);
  }

  static fromPrimitive(value: any): SongUrl {
    return SongUrl.create(String(value));
  }

  static ensureIsValid(value: string): void {
    if (!value || typeof value !== 'string' || value.trim().length === 0) {
      throw new Error(`[SongUrl] Invalid url: ${value}`);
    }
  }

  private constructor(value: string) {
    this.value = value;
  }

  getValue(): string {
    return this.value;
  }
  toPrimitive(): string {
    return this.value;
  }
  equals(other: SongUrl): boolean {
    return this.value === other.value;
  }
}
