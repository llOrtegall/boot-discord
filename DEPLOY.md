# Deployment

CI/CD: GitHub Actions runs lint + typecheck + tests on every push/PR, then (on
push to `main`) builds the Docker image and pushes it to **GHCR**:

```
ghcr.io/llortegall/boot-discord:latest
ghcr.io/llortegall/boot-discord:sha-<short>
```

No secrets are required for the build — it uses the built-in `GITHUB_TOKEN`
(`packages: write`).

## Runtime requirements (baked into the image)

- Bun (base `oven/bun:1`)
- `ffmpeg` (apt)
- `yt-dlp` standalone binary, auto-updated (`yt-dlp -U`) on container start

## Manual deploy on the Hostinger VPS

Prereqs on the VPS: Docker + Docker Compose plugin installed.

1. Make the GHCR package pullable. Either:
   - In GitHub → the package page → **Package settings → Change visibility →
     Public**, or
   - Keep it private and log in on the VPS:
     ```sh
     echo <GITHUB_PAT_with_read:packages> | docker login ghcr.io -u llOrtegall --password-stdin
     ```

2. Create a working dir with `docker-compose.yml` and `.env`:
   ```sh
   mkdir -p ~/music-bot && cd ~/music-bot
   # copy docker-compose.yml here (from this repo)
   ```
   `.env`:
   ```
   DISCORD_TOKEN=your_bot_token
   DISCORD_CLIENT_ID=your_client_id
   # optional overrides:
   # CACHE_TTL_DAYS=7
   ```

3. Pull and start:
   ```sh
   docker compose pull
   docker compose up -d
   ```

4. Logs / status:
   ```sh
   docker compose logs -f
   docker compose ps
   ```

5. Update to a new release (after a push to `main` rebuilds the image):
   ```sh
   docker compose pull && docker compose up -d
   ```

## YouTube "Sign in to confirm you're not a bot"

YouTube blocks many datacenter/VPS IPs and demands login. Two knobs (set in `.env`):

- `YT_DLP_EXTRACTOR_ARGS=youtube:player_client=tv` — no account; try first, often not enough.
- `YT_DLP_COOKIES=/app/cookies.txt` — reliable. Export `cookies.txt` (Netscape format)
  from a **throwaway** logged-in YouTube account (the bundled account can be rate-limited),
  put it next to `docker-compose.yml`, and uncomment the `cookies.txt` bind mount in
  `docker-compose.yml`. Cookies expire — refresh the file when 403s return.

Both apply to every `yt-dlp` call (resolve + download). After editing `.env`/the mount,
`docker compose up -d` to recreate the container.

## Persistence

Downloaded audio + the sqlite cache live in `/app/.cache`, mounted on the named
volume `cache` (survives restarts and image updates). Remove it with
`docker compose down -v` if you want a clean cache.
