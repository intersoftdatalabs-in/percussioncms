#!/usr/bin/env bash
# CI / local smoke for product-docs Virtual Site build (issue #2704).
#
# Runs scripts/build-cms-docs.sh then fails if the default versioned index
# HTML is missing (or no index.html under the output root).
#
# Usage (from repo root, after system classpath is available):
#   scripts/ci-smoke-product-docs.sh [siteRoot] [outputRoot]
#
# Windows parity: scripts\ci-smoke-product-docs.bat
#
# Exit codes:
#   0 — build succeeded and at least one index.html was emitted
#   1 — build failed (non-zero from build-cms-docs) or index HTML missing
#
# Failure artifacts: output root is recreated empty (not left deleted) and a
# .ci-smoke-meta.txt marker is always written so CI upload-artifact on
# failure() has a path + file even when the docs build emits nothing.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SITE_ROOT="${1:-${REPO_ROOT}/product-docs}"
OUT_ROOT="${2:-${REPO_ROOT}/tmp/product-docs-site}"

# Fresh output so stale HTML from prior runs cannot mask a failed emit.
# Recreate the directory immediately so workflow failure artifacts always have
# a path (rm -rf alone leaves nothing for actions/upload-artifact).
rm -rf "${OUT_ROOT}"
mkdir -p "${OUT_ROOT}"
{
  echo "status=running"
  echo "siteRoot=${SITE_ROOT}"
  echo "outRoot=${OUT_ROOT}"
} > "${OUT_ROOT}/.ci-smoke-meta.txt"

echo "ci-smoke-product-docs: building siteRoot=${SITE_ROOT} → outRoot=${OUT_ROOT}"

set +e
bash "${SCRIPT_DIR}/build-cms-docs.sh" "${SITE_ROOT}" "${OUT_ROOT}"
BUILD_RC=$?
set -e

if [[ "${BUILD_RC}" -ne 0 ]]; then
  {
    echo "status=build_failed"
    echo "buildExit=${BUILD_RC}"
  } >> "${OUT_ROOT}/.ci-smoke-meta.txt"
  echo "ci-smoke-product-docs: FAIL — build-cms-docs.sh exited ${BUILD_RC}" >&2
  exit 1
fi

# Prefer default product version path (product-docs/_config.yaml → 8.2).
DEFAULT_INDEX="${OUT_ROOT}/8.2/index.html"
if [[ -f "${DEFAULT_INDEX}" ]]; then
  echo "status=ok" >> "${OUT_ROOT}/.ci-smoke-meta.txt"
  echo "ci-smoke-product-docs: OK — found ${DEFAULT_INDEX}"
  exit 0
fi

# Fallback: any index.html under the output tree (future multi-version layouts).
# Portable: find (no hardcoded OS separators beyond URL-style path join above).
FOUND="$(find "${OUT_ROOT}" -type f -name 'index.html' 2>/dev/null | head -n 1 || true)"
if [[ -n "${FOUND}" ]]; then
  echo "status=ok" >> "${OUT_ROOT}/.ci-smoke-meta.txt"
  echo "ci-smoke-product-docs: OK — found index HTML at ${FOUND}"
  exit 0
fi

{
  echo "status=missing_index"
  echo "expectedDefault=${DEFAULT_INDEX}"
} >> "${OUT_ROOT}/.ci-smoke-meta.txt"
echo "ci-smoke-product-docs: FAIL — no index.html under ${OUT_ROOT}" >&2
echo "  Expected at least ${DEFAULT_INDEX} (or any **/index.html)." >&2
echo "  Broken Markdown/frontmatter/_config.yaml or Virtual Site build bugs" >&2
echo "  cause this failure; fix content or system virtualsite package and re-run." >&2
exit 1
