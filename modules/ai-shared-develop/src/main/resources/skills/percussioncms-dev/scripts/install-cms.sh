#!/usr/bin/env bash
# install-cms.sh — Install Percussion CMS from a distribution JAR.
#
# Usage:
#   ./install-cms.sh [--jar PATH] [--install-dir DIR]
#
# Options:
#   --jar          Path to perc-distribution-tree.jar
#                  (default: modules/perc-distribution-tree/target/perc-distribution-tree.jar)
#   --install-dir  Installation directory (default: ~/percussioncms-install)
#
# Environment:
#   JAVA_HOME      Must point to JDK 21
#   PROJECT_ROOT   Root of the percussioncms Git workspace (auto-detected if unset)
#
set -euo pipefail

INSTALL_DIR="${HOME}/percussioncms-install"
JAR_PATH=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --jar)
      JAR_PATH="$2"
      shift 2
      ;;
    --install-dir)
      INSTALL_DIR="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

# Determine project root and default JAR (local build is the default)
PROJECT_ROOT="${PROJECT_ROOT:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
DEFAULT_JAR_PATH="${PROJECT_ROOT}/modules/perc-distribution-tree/target/perc-distribution-tree.jar"
JAR_PATH="${JAR_PATH:-${DEFAULT_JAR_PATH}}"

# Validate prerequisites
if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -n "${JAVA_HOME_21:-}" ]]; then
    export JAVA_HOME="${JAVA_HOME_21}"
  else
    echo "ERROR: JAVA_HOME is not set. Set JAVA_HOME to a JDK 21 installation." >&2
    exit 1
  fi
fi

if [[ ! -f "${JAR_PATH}" ]]; then
  echo "ERROR: Distribution JAR not found at ${JAR_PATH}" >&2
  echo "Run './mvn-env.sh clean install' to build the project first, or provide a release JAR via --jar." >&2
  exit 1
fi

echo "Installing Percussion CMS..."
echo "  JAR:         ${JAR_PATH}"
echo "  Install Dir: ${INSTALL_DIR}"
echo "  JAVA_HOME:   ${JAVA_HOME}"
echo ""

# Run the installer
java -jar "${JAR_PATH}" "${INSTALL_DIR}"

# Create JRE symlink
echo "Creating JRE symlink..."
ln -sfn "${JAVA_HOME}" "${INSTALL_DIR}/JRE"

# Verify installation
if [[ -f "${INSTALL_DIR}/jetty/StartJetty.sh" ]]; then
  echo ""
  echo "Installation successful!"
  echo "Start the CMS with: cd ${INSTALL_DIR}/jetty && ./StartJetty.sh"
else
  echo ""
  echo "WARNING: Installation may have failed — StartJetty.sh not found."
  echo "Check ${INSTALL_DIR} for details."
fi
