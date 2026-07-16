#!/usr/bin/env sh
# Cross-platform entry (Unix/macOS/Git Bash): harvest Kilo/GitHub review comments
# into Erlang pattern memory. Delegates to Python for Windows portability.
set -eu

ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
cd "$ROOT"

if command -v python3 >/dev/null 2>&1; then
  PY=python3
elif command -v python >/dev/null 2>&1; then
  PY=python
else
  echo "erlang-harvest: python3/python not found on PATH" >&2
  exit 1
fi

exec "$PY" "$ROOT/scripts/erlang-harvest-review-patterns.py" "$@"
