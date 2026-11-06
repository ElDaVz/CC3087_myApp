#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if command -v google-chrome-stable >/dev/null; then
  export PLAYWRIGHT_CHROME_EXECUTABLE
  PLAYWRIGHT_CHROME_EXECUTABLE="$(command -v google-chrome-stable)"
fi
exec npm run test:e2e --prefix web
