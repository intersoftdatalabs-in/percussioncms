#!/usr/bin/env bash
# download-latest.sh — Download the latest Percussion CMS release artifacts from GitHub.
#
# Usage:
#   ./download-latest.sh [--dts] [--output-dir DIR]
#
# Options:
#   --dts          Also download the DTS distribution JAR
#   --output-dir   Directory for downloaded files (default: ./downloads)
#
# Environment:
#   GITHUB_REPO    GitHub org/repo (default: intersoftdatalabs-in/percussioncms)
#   GITHUB_TOKEN   Optional GitHub token for authenticated API calls (higher rate limits)
#
set -euo pipefail

GITHUB_REPO="${GITHUB_REPO:-intersoftdatalabs-in/percussioncms}"
OUTPUT_DIR="./downloads"
DOWNLOAD_DTS=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dts)
      DOWNLOAD_DTS=true
      shift
      ;;
    --output-dir)
      OUTPUT_DIR="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

mkdir -p "${OUTPUT_DIR}"

# Build auth header if token is available
AUTH_HEADER=""
if [[ -n "${GITHUB_TOKEN:-}" ]]; then
  AUTH_HEADER="-H \"Authorization: token ${GITHUB_TOKEN}\""
fi

echo "Fetching latest release info from ${GITHUB_REPO}..."
RELEASE_JSON=$(curl -sfL ${AUTH_HEADER} \
  "https://api.github.com/repos/${GITHUB_REPO}/releases/latest")

TAG_NAME=$(echo "${RELEASE_JSON}" | python3 -c "import sys, json; print(json.load(sys.stdin)['tag_name'])")
echo "Latest release: ${TAG_NAME}"

# Download CMS distribution JAR
CMS_JAR_URL=$(echo "${RELEASE_JSON}" | python3 -c "
import sys, json
assets = json.load(sys.stdin).get('assets', [])
for a in assets:
    if 'perc-distribution-tree' in a['name'] and a['name'].endswith('.jar'):
        print(a['browser_download_url'])
        break
else:
    print('')
")

if [[ -n "${CMS_JAR_URL}" ]]; then
  echo "Downloading CMS distribution JAR..."
  curl -fL -o "${OUTPUT_DIR}/perc-distribution-tree.jar" "${CMS_JAR_URL}"
  echo "  -> ${OUTPUT_DIR}/perc-distribution-tree.jar"
else
  echo "WARNING: CMS distribution JAR not found in release assets."
  echo "You may need to build from source: ./mvn-env.sh clean install"
fi

# Optionally download DTS distribution JAR
if [[ "${DOWNLOAD_DTS}" == "true" ]]; then
  DTS_JAR_URL=$(echo "${RELEASE_JSON}" | python3 -c "
import sys, json
assets = json.load(sys.stdin).get('assets', [])
for a in assets:
    if 'delivery-tier-distribution' in a['name'] and a['name'].endswith('.jar'):
        print(a['browser_download_url'])
        break
else:
    print('')
")

  if [[ -n "${DTS_JAR_URL}" ]]; then
    echo "Downloading DTS distribution JAR..."
    curl -fL -o "${OUTPUT_DIR}/delivery-tier-distribution.jar" "${DTS_JAR_URL}"
    echo "  -> ${OUTPUT_DIR}/delivery-tier-distribution.jar"
  else
    echo "WARNING: DTS distribution JAR not found in release assets."
    echo "You may need to build from source: ./mvn-env.sh -P with-dts clean install"
  fi
fi

echo ""
echo "Download complete. Release: ${TAG_NAME}"
echo "Files in ${OUTPUT_DIR}:"
ls -lh "${OUTPUT_DIR}"/*.jar 2>/dev/null || echo "  (no JAR files found)"
