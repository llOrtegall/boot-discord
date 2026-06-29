import { mkdirSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import type { SongSource, ResolvedSong } from '../domain/SongSource.ts';
import { logger } from '../../../shared/logger.ts';

const SCOPE = 'ytdlp';

interface YtDlpOptions {
  binary?: string;
  /** Path to a Netscape-format cookies.txt — needed when YouTube demands login (e.g. datacenter/VPS IPs). */
  cookiesFile?: string;
  /** Raw value for `--extractor-args`, e.g. `youtube:player_client=tv`. */
  extractorArgs?: string;
}

export class YtDlpSongSource implements SongSource {
  private readonly audioDir: string;
  private readonly binary: string;
  private readonly cookiesFile?: string;
  private readonly extractorArgs?: string;

  constructor(audioDir: string, options: YtDlpOptions = {}) {
    this.audioDir = audioDir;
    this.binary = options.binary ?? 'yt-dlp';
    this.cookiesFile = options.cookiesFile?.trim() || undefined;
    this.extractorArgs = options.extractorArgs?.trim() || undefined;
    mkdirSync(audioDir, { recursive: true });
    if (this.cookiesFile) logger.info(SCOPE, `using cookies file ${this.cookiesFile}`);
    if (this.extractorArgs) logger.info(SCOPE, `using extractor-args ${this.extractorArgs}`);
  }

  /** Auth/anti-bot flags appended to every yt-dlp invocation. */
  private authArgs(): string[] {
    const args: string[] = [];
    if (this.cookiesFile) args.push('--cookies', this.cookiesFile);
    if (this.extractorArgs) args.push('--extractor-args', this.extractorArgs);
    return args;
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
      ...this.authArgs(),
    ]);

    const json = JSON.parse(stdout);
    const entry = Array.isArray(json.entries) ? json.entries[0] : json;
    if (!entry || !entry.id) {
      logger.warn(SCOPE, `no result for query "${query}"`);
      return null;
    }

    logger.info(SCOPE, `resolved "${query}" -> ${entry.id} (${entry.title ?? 'Unknown'})`);
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
    logger.info(SCOPE, `downloading ${videoId}`);
    const startedAt = Date.now();
    await this.run([
      this.binary,
      sourceUrl,
      '-f',
      // Prefer opus/webm (plays without transcode); fall back to any audio-only,
      // then to a combined format when YouTube serves no audio-only stream.
      'bestaudio[ext=webm]/bestaudio/best',
      '-o',
      outTemplate,
      '--no-playlist',
      '--no-warnings',
      '--no-part',
      ...this.authArgs(),
    ]);

    const file = readdirSync(this.audioDir).find((f) => f.startsWith(`${videoId}.`));
    if (!file) throw new Error(`[YtDlpSongSource] downloaded file not found for ${videoId}`);
    logger.info(SCOPE, `downloaded ${videoId} in ${Date.now() - startedAt}ms -> ${file}`);
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
      logger.error(SCOPE, `${this.binary} exited ${exitCode}`, stderr.trim());
      throw new Error(`[YtDlpSongSource] ${this.binary} exited ${exitCode}: ${stderr.trim()}`);
    }
    return stdout;
  }
}
