#!/usr/bin/env bash
# Build product-docs Virtual Site to static HTML via Maven exec:java (system module).
# Usage: scripts/build-cms-docs.sh [siteRoot] [outputRoot]
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SITE_ROOT="${1:-${REPO_ROOT}/product-docs}"
OUT_ROOT="${2:-${REPO_ROOT}/tmp/product-docs-site}"

cd "${REPO_ROOT}/system"
exec ../mvnw -q -DskipTests compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.classpathScope=compile \
  -Dexec.mainClass=com.percussion.services.virtualsite.PSVirtualSiteBuildMain \
  -Dexec.args="${SITE_ROOT} ${OUT_ROOT} product-docs"
