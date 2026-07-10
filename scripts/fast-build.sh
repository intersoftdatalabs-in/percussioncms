#!/usr/bin/env bash
# fast-build.sh - Run a Maven build with slow, non-essential plugins skipped for quick dev iteration.
#
# This reactor binds several plugins to the default lifecycle that are expensive across a
# 29+ module build (javadoc jar generation, a custom repo-wide SHA-256 hash integrity check
# that verifies 15,000+ hashes per module, dependency bytecode analysis, enforcer rules,
# and unit tests). None of these are needed for a quick "does it compile/package" iteration
# loop, so this script skips them via their documented Maven properties.
#
# Typical usage:
#   ./scripts/fast-build.sh -pl deliverytiersuite/delivery-tier-suite/delivery-tier-distribution -am package
#   ./scripts/fast-build.sh -pl modules/perc-security-utils install
#   ./scripts/fast-build.sh install                       # fast full reactor build
#   ./scripts/fast-build.sh --with-tests -pl system install
#
# Notes:
# - Runs offline (-o) by default since this repo's dependencies are normally already
#   resolved/installed locally. Pass --online to disable.
# - Uses ./mvn-env.sh to ensure the correct JDK (per AGENTS.md) is used.
# - Any extra arguments (Maven goals, -pl, -am, profiles, etc.) are passed through to Maven
#   as-is, after the fast-build flags.
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  ./scripts/fast-build.sh [fast-build options] [-- ] <maven args...>

Fast-build options (all optional):
  --with-tests     Do not skip unit tests (default: tests are skipped).
  --online         Do not pass -o (offline) to Maven (default: offline).
  --help           Show this help.

Anything else on the command line is passed straight through to Maven, e.g.:
  -pl <module>[,<module>...]
  -am
  install | package | clean install | etc.
  -P<profile>

Skipped by default (see script header for rationale):
  -Dai.integrity.skip=true       (custom ai-build-integrity hash generate/verify mojos)
  -Dmaven.javadoc.skip=true      (attach-javadocs jar generation)
  -Denforcer.skip=true           (maven-enforcer-plugin rule checks)
  -Dmdep.analyze.skip=true       (maven-dependency-plugin analyze-only bytecode scan)
  -DskipTests=true               (surefire unit tests; use --with-tests to run them)
  -Dcheckstyle.skip=true         (defensive; not bound to the default build today)

Examples:
  ./scripts/fast-build.sh -pl deliverytiersuite/delivery-tier-suite/delivery-tier-distribution -am package
  ./scripts/fast-build.sh -pl modules/perc-security-utils install
  ./scripts/fast-build.sh --with-tests -pl system install
  ./scripts/fast-build.sh --online -pl rest install
EOF
}

PROJECT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
WITH_TESTS=0
OFFLINE=1
MAVEN_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --with-tests)
      WITH_TESTS=1
      shift
      ;;
    --online)
      OFFLINE=0
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    --)
      shift
      MAVEN_ARGS+=("$@")
      break
      ;;
    *)
      MAVEN_ARGS+=("$1")
      shift
      ;;
  esac
done

if [[ ${#MAVEN_ARGS[@]} -eq 0 ]]; then
  echo "ERROR: no Maven goal(s) provided (e.g. package, install)." >&2
  usage
  exit 1
fi

FAST_FLAGS=(
  "-Dai.integrity.skip=true"
  "-Dmaven.javadoc.skip=true"
  "-Denforcer.skip=true"
  "-Dmdep.analyze.skip=true"
  "-Dcheckstyle.skip=true"
)

if [[ $WITH_TESTS -eq 0 ]]; then
  FAST_FLAGS+=("-DskipTests=true")
fi

CMD=("$PROJECT_ROOT/mvn-env.sh")
if [[ $OFFLINE -eq 1 ]]; then
  CMD+=("-o")
fi
CMD+=("${FAST_FLAGS[@]}" "${MAVEN_ARGS[@]}")

echo "Project root : $PROJECT_ROOT"
echo "Offline      : $OFFLINE"
echo "With tests   : $WITH_TESTS"
echo "Command      : ${CMD[*]}"
echo ""

cd "$PROJECT_ROOT"
exec "${CMD[@]}"
