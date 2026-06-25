export class VideoId {
  private readonly value: string;

  static create(value: string): VideoId {
    VideoId.ensureIsValid(value);
    return new VideoId(value);
  }

  static fromPrimitive(value: any): VideoId {
    return VideoId.create(String(value));
  }

  static ensureIsValid(value: string): void {
    if (!value || typeof value !== 'string' || value.trim().length === 0) {
      throw new Error(`[VideoId] Invalid video id: ${value}`);
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
  equals(other: VideoId): boolean {
    return this.value === other.value;
  }
}
