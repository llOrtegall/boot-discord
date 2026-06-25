import { mkdirSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import type { SongSource, ResolvedSong } from '../domain/SongSource.ts';

export class YtDlpSongSource implements SongSource {
  private readonly audioDir: string;
  private readonly binary: string;

  constructor(audioDir: string, binary = 'yt-dlp') {
    this.audioDir = audioDir;
    this.binary = binary;
    mkdirSync(audioDir, { recursive: true });
  }

  async resolve(query: string): Promise<ResolvedSong | null> {
    const target = this.isUrl(query) ? query : `ytsearch1:${query}`;
    const stdout = await this.run([
      this.binary,
      target,
      '--dump-single-json',
      '--no-playlist',
      '--no-warnings',
      '--skip-download',
    ]);

    const json = JSON.parse(stdout);
    const entry = Array.isArray(json.entries) ? json.entries[0] : json;
    if (!entry || !entry.id) return null;

    return {
      videoId: String(entry.id),
      title: entry.title ? String(entry.title) : 'Unknown',
      durationSec: Math.round(Number(entry.duration ?? 0)),
      sourceUrl:
        entry.webpage_url ?? entry.original_url ?? `https://www.youtube.com/watch?v=${entry.id}`,
    };
  }

  async download(videoId: string, sourceUrl: string): Promise<string> {
    const outTemplate = join(this.audioDir, `${videoId}.%(ext)s`);
    await this.run([
      this.binary,
      sourceUrl,
      '-f',
      'bestaudio[ext=webm]/bestaudio',
      '-o',
      outTemplate,
      '--no-playlist',
      '--no-warnings',
      '--no-part',
    ]);

    const file = readdirSync(this.audioDir).find((f) => f.startsWith(`${videoId}.`));
    if (!file) throw new Error(`[YtDlpSongSource] downloaded file not found for ${videoId}`);
    return join(this.audioDir, file);
  }

  private isUrl(query: string): boolean {
    return /^https?:\/\//i.test(query.trim());
  }

  private async run(args: string[]): Promise<string> {
    const proc = Bun.spawn(args, { stdout: 'pipe', stderr: 'pipe' });
    const stdout = await new Response(proc.stdout).text();
    const exitCode = await proc.exited;
    if (exitCode !== 0) {
      const stderr = await new Response(proc.stderr).text();
      throw new Error(`[YtDlpSongSource] ${this.binary} exited ${exitCode}: ${stderr.trim()}`);
    }
    return stdout;
  }
}
