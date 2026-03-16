#!/bin/bash
# Script to sign AI resources (skills, prompts, instructions) using Sigstore.

set -e

# Get repo root relative to this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
SIGN_DIR="$REPO_ROOT/modules/ai-shared-develop/src/main/resources"

# Build the signer utility first
echo "Building ResourceSigner utility..."
"$REPO_ROOT/mvn-env.sh" -pl modules/ai-shared-develop clean compile

# Function to run the signer
run_signer() {
    local files=("$@")
    # Quote each file path for the exec:java argument
    # exec.args is a space-separated string of arguments
    # We join them with spaces but ensure each is treated as a single arg in the Java main
    
    "$REPO_ROOT/mvn-env.sh" -pl modules/ai-shared-develop exec:java \
        -Dexec.mainClass="com.percussion.ai.signing.ResourceSigner" \
        -Dexec.args="${files[*]}"
}

echo "Searching for AI resources to sign in $SIGN_DIR..."

FILES_TO_SIGN=()

# Collect tracked AI resources in ai-shared-develop
while IFS= read -r file; do
    FILES_TO_SIGN+=("$file")
done < <(find "$SIGN_DIR/skills" "$SIGN_DIR/instructions" "$SIGN_DIR/prompts" -type f ! -name "*.sha256" ! -name "*.sha256.sig" ! -name "*.sigstore.json" 2>/dev/null || true)

# Collect resources in ai-shared-release
RELEASE_SIGN_DIR="$REPO_ROOT/modules/ai-shared-release/src/main/resources"
while IFS= read -r file; do
    FILES_TO_SIGN+=("$file")
done < <(find "$RELEASE_SIGN_DIR/skills" -type f ! -name "*.sha256" ! -name "*.sha256.sig" ! -name "*.sigstore.json" 2>/dev/null || true)

# Add AGENTS.md in root
if [ -f "$REPO_ROOT/AGENTS.md" ]; then
    FILES_TO_SIGN+=("$REPO_ROOT/AGENTS.md")
fi

# Add module-level AGENTS.md
while IFS= read -r file; do
    FILES_TO_SIGN+=("$file")
done < <(find "$REPO_ROOT/modules" -name "AGENTS.md" 2>/dev/null || true)

# Add AGENTS.local.md (root and modules)
while IFS= read -r file; do
    FILES_TO_SIGN+=("$file")
done < <(find "$REPO_ROOT" -name "AGENTS.local.md" 2>/dev/null || true)

if [ ${#FILES_TO_SIGN[@]} -gt 0 ]; then
    echo "Found ${#FILES_TO_SIGN[@]} files to sign."
    run_signer "${FILES_TO_SIGN[@]}"
else
    echo "No AI resources found to sign."
fi

echo "All AI resources signed successfully."
