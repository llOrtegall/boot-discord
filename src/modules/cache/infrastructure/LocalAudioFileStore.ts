import { unlink } from 'node:fs/promises';
import type { AudioFileStore } from '../domain/AudioFileStore.ts';

export class LocalAudioFileStore implements AudioFileStore {
  async exists(filePath: string): Promise<boolean> {
    return Bun.file(filePath).exists();
  }

  async delete(filePath: string): Promise<void> {
    try {
      await unlink(filePath);
    } catch (err: any) {
      if (err?.code !== 'ENOENT') console.error('[LocalAudioFileStore.delete]', err.message);
    }
  }
}
