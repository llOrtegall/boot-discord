# ---- deps: install production node_modules ----
FROM oven/bun:1 AS deps
WORKDIR /app
ENV HUSKY=0
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile --production

# ---- runtime ----
FROM oven/bun:1 AS runtime
WORKDIR /app

# ffmpeg: audio fallback / remux. curl + ca-certificates: fetch yt-dlp.
RUN apt-get update \
  && apt-get install -y --no-install-recommends ffmpeg ca-certificates curl \
  && rm -rf /var/lib/apt/lists/*

# yt-dlp standalone build (self-contained, no python). In a bun-owned dir so the
# entrypoint can run `yt-dlp -U` at runtime.
RUN mkdir -p /app/bin \
  && curl -fsSL https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux \
     -o /app/bin/yt-dlp \
  && chmod 0755 /app/bin/yt-dlp
ENV PATH="/app/bin:${PATH}"

COPY --from=deps /app/node_modules ./node_modules
COPY package.json bun.lock tsconfig.json ./
COPY src ./src
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh

RUN chmod +x /usr/local/bin/entrypoint.sh \
  && mkdir -p /app/.cache/audio \
  && chown -R bun:bun /app

ENV NODE_ENV=production \
    CACHE_DB_PATH=/app/.cache/songs.db \
    CACHE_AUDIO_DIR=/app/.cache/audio \
    CACHE_TTL_DAYS=7

USER bun
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["bun", "run", "src/index.ts"]
