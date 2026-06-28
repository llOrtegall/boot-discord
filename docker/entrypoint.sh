#!/bin/sh
set -e

echo "[entrypoint] refreshing yt-dlp..."
yt-dlp -U 2>/dev/null || echo "[entrypoint] yt-dlp self-update skipped"
yt-dlp --version 2>/dev/null || echo "[entrypoint] WARNING: yt-dlp not available"

exec "$@"
