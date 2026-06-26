import type { AudioFileStore } from '../../domain/AudioFileStore.ts';

export function create(overrides?: Partial<AudioFileStore>): jest.Mocked<AudioFileStore> {
  return {
    exists: jest.fn().mockResolvedValue(true),
    delete: jest.fn().mockResolvedValue(undefined),
    ...overrides,
  } as jest.Mocked<AudioFileStore>;
}
