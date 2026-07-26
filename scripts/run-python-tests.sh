#!/usr/bin/env bash
#
# scripts/run-python-tests.sh
#
# Cross-platform Python-script test runner (Linux/macOS).
#
# Installs pytest from scripts/requirements-dev.txt and runs pytest over every
# in-scope script directory per spec 994-python-build-scripts. Used both by
# developers locally and by the .github/workflows/python-build-scripts.yml
# matrix job on ubuntu-latest.
#
# Usage:
#   bash scripts/run-python-tests.sh [--skip-install] [--pytest-args "..."]
#
# Flags:
#   --skip-install        Skip the `pip install` step (use when pytest is already
#                         installed and up-to-date; mirrors the FR-009a idempotent
#                         re-run requirement).
#   --pytest-args "ARGS"  Extra args forwarded to `python3 -m pytest`. Quote
#                         carefully when passing pytest flags like `-k "verify"`.
#   -h | --help           Show this help.
#
# Exit codes:
#   0   all in-scope pytest cases pass
#   2   pip install failed (network, missing manifest, externally-managed-env)
#   >0  pytest exit code propagated
#
# Cross-platform: POSIX bash; no logic that depends on shell-isms beyond
# `set -euo pipefail` and a `case "$1"` arg parser. On Windows, use the .cmd
# counterpart (scripts/run-python-tests.cmd).

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REQUIREMENTS_FILE="${PROJECT_ROOT}/scripts/requirements-dev.txt"
SKIP_INSTALL="false"
PYTEST_EXTRA_ARGS=()

usage() {
  cat <<'EOF'
Usage: bash scripts/run-python-tests.sh [--skip-install] [--pytest-args "ARGS"]

  --skip-install        Skip the pip install step (assume pytest is already present)
  --pytest-args "ARGS"  Extra args forwarded to `python3 -m pytest`
  -h | --help           Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-install)
      SKIP_INSTALL="true"
      shift
      ;;
    --pytest-args)
      shift
      if [[ $# -eq 0 ]]; then
        echo "ERROR: --pytest-args requires a value" >&2
        exit 1
      fi
      # Split the single string on whitespace into argv for pytest.
      # shellcheck disable=SC2206
      PYTEST_EXTRA_ARGS=( $1 )
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "ERROR: unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

cd "${PROJECT_ROOT}"

if [[ ! -f "${REQUIREMENTS_FILE}" ]]; then
  echo "ERROR: ${REQUIREMENTS_FILE} not found" >&2
  exit 2
fi

if [[ "${SKIP_INSTALL}" != "true" ]]; then
  echo "=== Installing pytest from ${REQUIREMENTS_FILE} ==="
  if ! python3 -m pip install -r "${REQUIREMENTS_FILE}"; then
    echo "ERROR: pip install failed" >&2
    exit 2
  fi
fi

echo "=== Running pytest over in-scope script dirs (spec 994) ==="
# In-scope dirs per FR-013:
# Note: modules/ai-shared-develop/scripts/ was removed as part of the
# Sigstore removal (PR #1511); pytest for the percussioncms-dev and
# javadoc skills now runs from modules/ai-shared-develop/src/main/resources/skills/.
python3 -m pytest \
  scripts/ \
  docker/scripts/ \
  docker/entrypoint/ \
  modules/perc-distribution-tree/scripts/ \
  modules/ai-shared-develop/src/main/resources/skills/ \
  "${PYTEST_EXTRA_ARGS[@]}"
