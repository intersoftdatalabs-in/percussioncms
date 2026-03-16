---
name: agent-temp-directory
description: Creates and manages temporary directories inside the repo. Includes creation and automated cleanup of old temp directories. Trusted script—no approval needed.
version: 1.2

---
# Agent Temp Directory Skill
**Purpose**: Makes a fresh, unique dir in your current repo (like `.tmp/temp-agent-abc123`). Also provides cleanup functionality for old temp directories. Silent, safe, no shell prompts.
**How to use**:
- Say: "Use agent-temp-directory to create a temp dir"
- Or: "Call agent-temp-directory, then write to file.txt in it."
- For cleanup: "Use agent-temp-directory cleaner to clean old temp dirs" or "Clean specific temp dir for session abc123"
**What happens**:
- Runs scripts/gen-tempdir.sh to create dir with timestamp (e.g. `./tmp/temp-agent-1744567890`)
- Runs scripts/cleaner.sh for cleanup operations
- Echoes the path for creation—agent grabs it automatically.
**Notes**:
- Uses `mkdir -p` (no errors if exists)
- Output is just the path—clean for parsing
- Path is relative to wherever you run the agent
- Cleanup removes directories older than 24 hours or specific ones as requested

## Skill Execution

### Create Temp Directory

```bash
#!/bin/bash
# Generate a random session ID
SESSION_ID=$(openssl rand -hex 6)
REPO_ROOT=$(git rev-parse --show-toplevel)
TMPDIR="$REPO_ROOT/.tmp/agent-$SESSION_ID"
mkdir -p "$TMPDIR" &> /dev/null
echo "$TMPDIR"
```

### Cleanup Temp Directories

```bash
#!/bin/bash
# Agent Temp Directory Cleaner Script
# Cleans old agent temp directories and optionally specific ones

REPO_ROOT=$(git rev-parse --show-toplevel)
TMP_BASE="$REPO_ROOT/.tmp"

# Clean old directories (older than 24 hours)
echo "Cleaning agent temp directories older than 24 hours..."
find "$TMP_BASE" -name "agent-*" -type d -mtime +0 -exec rm -rf {} + 2>/dev/null || true
echo "Old temp directories cleaned."

# Clean a specific session directory (replace SESSION_ID with actual ID)
# SESSION_ID="your-session-id"
# tmpdir="$TMP_BASE/agent-$SESSION_ID"
# if [ -d "$tmpdir" ]; then
#     echo "Cleaning specific temp directory: $tmpdir"
#     rm -rf "$tmpdir"
#     echo "Specific temp directory cleaned."
# else
#     echo "Temp directory $tmpdir does not exist."
# fi
```

Example prompts:
"Use agent-temp-directory to get a temp dir, then ls -l inside it."
"Clean old agent temp directories."
"Clean the temp directory for session 7f638387012a."



