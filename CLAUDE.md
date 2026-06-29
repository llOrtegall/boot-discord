# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Discord music bot. Users run slash commands; the bot resolves a query to a YouTube
video via `yt-dlp`, downloads the audio to a local on-disk cache, and streams it into a
voice channel through `@discordjs/voice`.

## Commands

```sh
bun run dev          # run with --watch (hot reload)
bun run start        # run once
bun test             # run all tests
bun test src/modules/cache/test/application/getOrDownloadSong.test.ts   # single file
bun test -t "evicts"   # single test by name
bun run typecheck    # tsc --noEmit
bun run lint         # eslint src
bun run format       # prettier --write src
```

CI (`.github/workflows/deploy.yml`) gates on `lint`, `typecheck`, and `test` — run all
three before pushing. On push to `main` it then builds and pushes a Docker image to GHCR.

## Runtime requirements

- **Bun** is the runtime — not Node. There is no build step; `src/index.ts` runs directly.
  Imports use explicit `.ts` extensions (required by `allowImportingTsExtensions`).
- **`yt-dlp`** must be a recent (2025+) standalone binary on `PATH`. Older builds fail
  against current YouTube. Invoked via `Bun.spawn`, not an npm wrapper.
- **`.env` is auto-loaded by Bun** — there is no `dotenv` dependency. Required vars:
  `DISCORD_TOKEN`, `DISCORD_CLIENT_ID`. Optional cache vars: `CACHE_DB_PATH`,
  `CACHE_AUDIO_DIR`, `CACHE_TTL_DAYS` (defaults: `./.cache/songs.db`, `./.cache/audio`, 7).
- **opusscript** is the opus encoder, deliberately, because `@discordjs/opus` needs a
  native build that fails on the dev machine. **`@noble/ciphers`** provides voice
  encryption — `@discordjs/voice` is pinned `>=0.18` because Discord removed the legacy
  `xsalsa20_poly1305` modes that older versions used. Do not swap either out without a
  reason; "voice never reaches Ready" usually means a missing/incompatible encryption lib.

## Architecture

Clean Architecture + DDD. Four modules under `src/modules/`, each split into the same
three layers:

- `domain/` — entities and value objects (e.g. `Song`, `Player`, `CachedSong`, `VideoId`)
  plus **port interfaces** (`*Repository`, `SongSource`, `AudioFileStore`). Pure, no I/O.
- `application/` — one file per use case (`addSong`, `playNext`, `getOrDownloadSong`, …),
  each a plain function taking its dependencies as props. No framework imports.
- `infrastructure/` — port implementations (`InMemoryQueueRepository`,
  `DiscordPlayerRepository`, `SqliteSongCacheRepository`, `YtDlpSongSource`,
  `LocalAudioFileStore`) and, for the `bot` module, the discord.js glue.

The modules:

- **`queue`** — the song queue per guild, plus a play-history stack (for "previous").
- **`player`** — playback state machine and voice/audio control.
- **`cache`** — resolve query → download → store, keyed by `videoId` in `bun:sqlite`,
  with TTL eviction.
- **`bot`** — discord.js client, slash-command handlers, and button interactions.

### Wiring — read this before touching the modules

Each module's `application/factory.ts` constructs **singleton** infrastructure instances
and exposes a `*Factory` object of bound use-case functions. This is the composition root;
there is no DI container.

- **The queue repository is a single shared instance.** `player/application/factory.ts`
  imports `queueRepository` from `queue/application/factory.ts` rather than constructing
  its own. This sharing is load-bearing: `song.ts` adds to the queue and `playNext` reads
  from it, so they must be the same object. Do not give a module its own second instance.
- `playerRepository` (the `DiscordPlayerRepository` singleton) is exported directly from
  its factory because `bot` needs it for `joinChannel` and `onIdle`.

### Playback flow

1. `/song <query>` (`commands/song.ts`) → `CacheFactory.getOrDownloadSong` resolves and
   downloads (or hits cache), then joins the user's voice channel.
2. The song is added to the queue. If the player is idle, `PlayerFactory.playNext` starts
   it; otherwise it just enqueues. **Start-vs-enqueue is decided by player state
   (`isPlaying() || isPaused()`), not by queue length** — queue-length checks break because
   `playNext` shifts the current song out of the queue.
3. Auto-advance: `DiscordPlayerRepository` fires an `onIdle` callback when the audio player
   goes idle; `BotRouter` registers that callback to call `playNext` for the guild.
4. `playSong` (`DiscordPlayerRepository`) waits for the voice connection to reach `Ready`
   (15s timeout) before subscribing, then `demuxProbe`s the file so opus/webm passes
   through without transcoding (low CPU; avoids opusscript on the hot path).

Player buttons (⏮ ⏯ ⏭ ⏹) use customIds `player:prev|playpause|skip|stop`, handled in
`BotRouter.handlePlayerButton`. Embeds and the control row live in
`bot/infrastructure/components/playerControls.ts`.

## Tests

Bun's test runner. Application use cases are tested against in-memory fakes built with the
**Object Mother** pattern (`test/helpers/*Mother.ts`) and shared `test/fixtures/values.ts`.
Tests live under each module's `test/` mirroring the layer layout. `@faker-js/faker`
generates values. `tsconfig.json` includes `"jest"` types for `jest.Mocked`/test globals.

## Conventions

- Use-case functions take a single props object containing their dependencies (ports), and
  return domain objects (or `null`), never discord.js types — keep discord.js inside `bot`.
- `strict` TS plus `noUncheckedIndexedAccess`, `noUnusedLocals`/`Parameters`,
  `verbatimModuleSyntax` (import interfaces with `import type`). Lint allows `any` only as
  a warning.
- Logging goes through `src/shared/logger.ts` (`logger.info/warn/error(scope, msg, …)`);
  scopes in use: `index`, `bot`, `voice`, `ytdlp`.
- **Volume control is intentionally absent** — it would force PCM re-encode through
  opusscript and worsen audio stutter.
