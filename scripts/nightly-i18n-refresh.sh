#!/bin/bash
set -euo pipefail

# Source shell profile to get trans, gh, mvnw, and JAVA_HOME on PATH
# Try bashrc first (Linux/WSL), then profile (macOS/Solaris)
if [ -f "$HOME/.bashrc" ]; then
    source "$HOME/.bashrc" 2>/dev/null || true
elif [ -f "$HOME/.profile" ]; then
    source "$HOME/.profile" 2>/dev/null || true
fi

# Change to repo root
cd "$(git rev-parse --show-toplevel)"

# Run the Python wrapper with all passed arguments
exec python3 scripts/nightly_i18n_refresh.py "$@"
