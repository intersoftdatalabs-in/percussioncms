#!/bin/bash

# Generate a random session ID
SESSION_ID=$(openssl rand -hex 6)
REPO_ROOT=$(git rev-parse --show-toplevel)
TMPDIR="$REPO_ROOT/.tmp/agent-$SESSION_ID"
mkdir -p "$TMPDIR" &> /dev/null

echo "$TMPDIR"
