import type { QueueRepository } from '../domain/QueueRepository.ts';
import { Song } from '../domain/Song.ts';
import { SongQueue } from '../domain/SongQueue.ts';

export class InMemoryQueueRepository implements QueueRepository {
  private readonly store = new Map<string, SongQueue>();
  private readonly history = new Map<string, Song[]>();

  async getByGuildId(guildId: string): Promise<SongQueue> {
    return this.store.get(guildId) ?? SongQueue.create([]);
  }

  async save(guildId: string, queue: SongQueue): Promise<void> {
    this.store.set(guildId, queue);
  }

  async addSong(guildId: string, song: Song): Promise<SongQueue> {
    const current = await this.getByGuildId(guildId);
    const updated = current.add(song);
    this.store.set(guildId, updated);
    return updated;
  }

  async unshift(guildId: string, song: Song): Promise<SongQueue> {
    const current = await this.getByGuildId(guildId);
    const updated = current.prepend(song);
    this.store.set(guildId, updated);
    return updated;
  }

  async removeSong(guildId: string, songId: string): Promise<SongQueue> {
    const current = await this.getByGuildId(guildId);
    const updated = current.remove(songId);
    this.store.set(guildId, updated);
    return updated;
  }

  async shift(guildId: string): Promise<{ song: Song | null; queue: SongQueue }> {
    const current = await this.getByGuildId(guildId);
    const { song, queue } = current.shift();
    this.store.set(guildId, queue);
    return { song, queue };
  }

  async clear(guildId: string): Promise<void> {
    this.store.set(guildId, SongQueue.create([]));
    this.history.delete(guildId);
  }

  async pushHistory(guildId: string, song: Song): Promise<void> {
    const stack = this.history.get(guildId) ?? [];
    stack.push(song);
    this.history.set(guildId, stack);
  }

  async popHistory(guildId: string): Promise<Song | null> {
    const stack = this.history.get(guildId);
    return stack?.pop() ?? null;
  }
}
