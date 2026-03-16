#!/bin/bash
# Script to verify AI resource signatures. Designed to be called by a git pre-commit hook.

set -e

# Get repo root relative to this script
# Assuming script is in modules/ai-shared-develop/scripts/
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
SIGN_DIR="$REPO_ROOT/modules/ai-shared-develop/src/main/resources"

# Function to run the verifier
run_verifier() {
    local files=("$@")
    "$REPO_ROOT/mvn-env.sh" -pl modules/ai-shared-develop exec:java \
        -Dexec.mainClass="com.percussion.ai.signing.ResourceVerifier" \
        -Dexec.args="${files[*]}"
}

echo "Verifying AI resource signatures..."

FILES_TO_VERIFY=()

# Collect tracked AI resources in ai-shared-develop
while IFS= read -r file; do
    if [ -f "$file" ]; then
        FILES_TO_VERIFY+=("$file")
    fi
done < <(find "$SIGN_DIR/skills" "$SIGN_DIR/instructions" "$SIGN_DIR/prompts" -type f ! -name "*.sha256" ! -name "*.sha256.sig" ! -name "*.sigstore.json" 2>/dev/null || true)

# Collect resources in ai-shared-release
RELEASE_SIGN_DIR="$REPO_ROOT/modules/ai-shared-release/src/main/resources"
while IFS= read -r file; do
    if [ -f "$file" ]; then
        FILES_TO_VERIFY+=("$file")
    fi
done < <(find "$RELEASE_SIGN_DIR/skills" -type f ! -name "*.sha256" ! -name "*.sha256.sig" ! -name "*.sigstore.json" 2>/dev/null || true)

# AGENTS.md in root
if [ -f "$REPO_ROOT/AGENTS.md" ]; then
    FILES_TO_VERIFY+=("$REPO_ROOT/AGENTS.md")
fi

# Module-level AGENTS.md
while IFS= read -r file; do
    if [ -f "$file" ]; then
        FILES_TO_VERIFY+=("$file")
    fi
done < <(find "$REPO_ROOT/modules" -name "AGENTS.md" 2>/dev/null || true)

# AGENTS.local.md (Local protection, even though not committed)
while IFS= read -r file; do
    if [ -f "$file" ]; then
        FILES_TO_VERIFY+=("$file")
    fi
done < <(find "$REPO_ROOT" -name "AGENTS.local.md" 2>/dev/null || true)

if [ ${#FILES_TO_VERIFY[@]} -gt 0 ]; then
    echo "Found ${#FILES_TO_VERIFY[@]} files to verify."
    # Build first to ensure verifier is up to date
    echo "Ensuring ResourceVerifier is built..."
    "$REPO_ROOT/mvn-env.sh" -pl modules/ai-shared-develop clean compile > /dev/null
    
    run_verifier "${FILES_TO_VERIFY[@]}"
else
    echo "No AI resources found to verify."
fi

echo "Signature verification successful."
