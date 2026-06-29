#!/bin/sh
set -e

echo "[entrypoint] refreshing yt-dlp..."
yt-dlp -U 2>/dev/null || echo "[entrypoint] yt-dlp self-update skipped"
yt-dlp --version 2>/dev/null || echo "[entrypoint] WARNING: yt-dlp not available"

# yt-dlp rewrites the cookie jar on exit, so the file must be writable. The
# host cookies are bind-mounted read-only; copy them onto the writable cache
# volume and repoint yt-dlp there. Source = YT_DLP_COOKIES or /app/cookies.txt.
COOKIE_RW=/app/.cache/cookies.txt
COOKIE_SRC="${YT_DLP_COOKIES:-/app/cookies.txt}"
if [ -f "$COOKIE_SRC" ] && [ "$COOKIE_SRC" != "$COOKIE_RW" ]; then
  if cp "$COOKIE_SRC" "$COOKIE_RW"; then
    export YT_DLP_COOKIES="$COOKIE_RW"
    echo "[entrypoint] staged cookies $COOKIE_SRC -> $COOKIE_RW"
  else
    echo "[entrypoint] WARNING: could not stage cookies from $COOKIE_SRC"
  fi
fi

exec "$@"
