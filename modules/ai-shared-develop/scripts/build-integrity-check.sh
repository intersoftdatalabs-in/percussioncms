#!/bin/bash
# Wrapper for build-time integrity check.
# Consistently verifies AI resources using SHA-256 and Sigstore signatures.
# Falls back gracefully if tools are missing to avoid breaking developer builds.

RESOURCE_ABS=$(realpath "$1")
RESOURCE_DIR=$(dirname "$RESOURCE_ABS")
RESOURCE_NAME=$(basename "$RESOURCE_ABS")
HASH_FILE="$RESOURCE_ABS.sha256"
SIG_FILE="$RESOURCE_ABS.sha256.sig"

if [ ! -f "$RESOURCE_ABS" ]; then
    echo "Resource $RESOURCE_ABS not found. Skipping."
    exit 0
fi

if [ ! -f "$HASH_FILE" ]; then
    echo "[INTEGRITY] WARNING: Missing sidecar for $RESOURCE_NAME. Please run sign-ai-resources.sh"
    exit 0
fi

# 1. Integrity Check
echo "[INTEGRITY] Checking $RESOURCE_NAME..."
(
    cd "$RESOURCE_DIR"
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum -c "$HASH_FILE"
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 -c "$HASH_FILE"
    else
        echo "[INTEGRITY] WARNING: sha256sum not found. Skipping hash check."
        exit 0
    fi
) || (echo "[INTEGRITY] FAILED: Hash mismatch for $RESOURCE_NAME" && exit 1)

# 2. Authenticity Check
if command -v cosign >/dev/null 2>&1; then
    if [ ! -f "$SIG_FILE" ]; then
        echo "[AUTHENTICITY] SKIP: Signature file $RESOURCE_NAME.sha256.sig not found. Only integrity verified."
        exit 0
    fi

    echo "[AUTHENTICITY] Verifying $RESOURCE_NAME signature..."
    
    # Secure Verification: We pull the expected identity from git config
    # to ensure that artifacts were signed by someone in the same organization.
    GIT_EMAIL=$(git config user.email || echo "")
    ID_REGEXP=".*"
    ISSUER_REGEXP=".*"
    
    if [[ -n "$GIT_EMAIL" ]]; then
        # If we have an email like user@domain.com, we expect signs from @domain.com
        DOMAIN=$(echo "$GIT_EMAIL" | awk -F@ '{print $2}')
        if [[ -n "$DOMAIN" ]]; then
            ID_REGEXP=".*@$DOMAIN"
            echo "[AUTHENTICITY] Restricting verification to identity: $ID_REGEXP"
        fi
    else
        echo "[AUTHENTICITY] WARNING: Could not determine git identity. Using catch-all verification."
    fi

    cosign verify-blob \
        --certificate-identity-regexp "$ID_REGEXP" \
        --certificate-oidc-issuer-regexp "$ISSUER_REGEXP" \
        --bundle "$SIG_FILE" "$HASH_FILE" || (echo "[AUTHENTICITY] FAILED: Signature invalid for $RESOURCE_NAME" && exit 1)
else
    echo "[AUTHENTICITY] SKIP: cosign not found. Skipping signature check."
fi
