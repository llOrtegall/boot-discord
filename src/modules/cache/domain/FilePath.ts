export class FilePath {
  private readonly value: string;

  static create(value: string): FilePath {
    FilePath.ensureIsValid(value);
    return new FilePath(value);
  }

  static fromPrimitive(value: any): FilePath {
    return FilePath.create(String(value));
  }

  static ensureIsValid(value: string): void {
    if (!value || typeof value !== 'string' || value.trim().length === 0) {
      throw new Error(`[FilePath] Invalid file path: ${value}`);
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
  equals(other: FilePath): boolean {
    return this.value === other.value;
  }
}
