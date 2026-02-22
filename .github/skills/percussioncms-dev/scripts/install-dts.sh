#!/usr/bin/env bash
# install-dts.sh — Install Percussion Delivery Tier Suite from a distribution JAR.
#
# Usage:
#   ./install-dts.sh [--jar PATH] [--install-dir DIR]
#
# Options:
#   --jar          Path to delivery-tier-distribution.jar
#                  (default: deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution.jar)
#   --install-dir  Installation directory (default: ~/percussiondts-install)
#
# Environment:
#   JAVA_HOME      Must point to JDK 21
#   PROJECT_ROOT   Root of the percussioncms Git workspace (auto-detected if unset)
#
set -euo pipefail

INSTALL_DIR="${HOME}/percussiondts-install"
JAR_PATH=""

# Parse options
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
DEFAULT_JAR_PATH="${PROJECT_ROOT}/deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution.jar"
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
  echo "ERROR: DTS distribution JAR not found at ${JAR_PATH}" >&2
  echo "Run './mvn-env.sh -P with-dts clean install' to build the project first, or provide a release JAR via --jar." >&2
  exit 1
fi

echo "Installing Percussion DTS..."
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
if [[ -d "${INSTALL_DIR}/Deployment/Server" ]]; then
  echo ""
  echo "DTS Installation successful!"
  echo "Start the DTS with: cd ${INSTALL_DIR} && ./TomcatStartup.sh"
else
  echo ""
  echo "WARNING: DTS installation may have failed — Deployment/Server directory not found."
  echo "Check ${INSTALL_DIR} for details."
fi
