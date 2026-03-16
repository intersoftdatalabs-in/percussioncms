#!/bin/bash
# Helper script to authenticate with Sigstore OIDC once per session.
# This prevents repeated browser prompts during multimodule Maven builds.

set -e

# Check if cosign is installed
if ! command -v cosign &> /dev/null; then
    echo "Error: 'cosign' is not installed."
    echo "Please install it from https://github.com/sigstore/cosign"
    exit 1
fi

echo "[DEBUG] Running authenticate-sigstore.sh v2.3 (Java Auth Flow)"
echo "Initiating Sigstore OIDC authentication via Java utility..."

# We use a temp file to capture only the TOKEN from stdout, 
# while allowing stderr (instructions) to flow to the console.
TEMP_TOKEN_FILE=$(mktemp)

# Run the Java OidcAuthenticator utility
# We explicitly override the exec.mainClass property defined in the POM
./mvn-env.sh -pl modules/ai-shared-develop -q exec:java \
    -Dexec.mainClass="com.percussion.ai.signing.OidcAuthenticator" > "$TEMP_TOKEN_FILE"

TOKEN=$(cat "$TEMP_TOKEN_FILE" | xargs)
rm -f "$TEMP_TOKEN_FILE"

if [[ -n "$TOKEN" ]]; then
    echo "Successfully retrieved Sigstore Identity Token."
    echo ""
    echo "Applying it to your session cache (~/.sigstore-token)..."
    echo "$TOKEN" > ~/.sigstore-token
    
    # Also export it to current shell for immediate use
    export SIGSTORE_IDENTITY_TOKEN="$TOKEN"
    echo "Done. Future 'mvn-env.sh' calls in this session will see this token."
    echo ""
    echo "To use this token in your current shell immediately, run:"
    echo "export SIGSTORE_IDENTITY_TOKEN=$TOKEN"
    echo ""
    echo "NOTE: The token is valid for approximately 15 minutes."
    echo "If your build takes longer, you may need to re-authenticate."
else
    echo "Error: Failed to retrieve identity token."
    exit 1
fi
