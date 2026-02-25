#!/usr/bin/env bash
# start-dts.sh — Start a local Percussion Delivery Tier Suite instance.
#
# Usage:
#   ./start-dts.sh [INSTALL_DIR]
#
# Arguments:
#   INSTALL_DIR   The DTS installation directory (default: ~/percussiondts-install)
#
# Environment:
#   DTS_INSTALL_DIR  Alternative to the positional argument
#
set -euo pipefail

INSTALL_DIR="${1:-${DTS_INSTALL_DIR:-${HOME}/percussiondts-install}}"

# Validate installation
if [[ ! -d "${INSTALL_DIR}/Deployment/Server" ]]; then
  echo "ERROR: DTS installation not found at ${INSTALL_DIR}" >&2
  echo "Expected ${INSTALL_DIR}/Deployment/Server to exist." >&2
  echo "Run install-dts.sh first." >&2
  exit 1
fi

# Check JRE
if [[ ! -d "${INSTALL_DIR}/JRE" ]]; then
  echo "WARNING: JRE directory not found at ${INSTALL_DIR}/JRE"
  echo "Creating symlink from JAVA_HOME..."
  if [[ -n "${JAVA_HOME:-}" ]]; then
    ln -sfn "${JAVA_HOME}" "${INSTALL_DIR}/JRE"
  elif [[ -n "${JAVA_HOME_21:-}" ]]; then
    ln -sfn "${JAVA_HOME_21}" "${INSTALL_DIR}/JRE"
  else
    echo "ERROR: Neither JAVA_HOME nor JAVA_HOME_21 is set." >&2
    exit 1
  fi
fi

# Determine which startup script to use
STARTUP_SCRIPT=""
if [[ -f "${INSTALL_DIR}/TomcatStartup.sh" ]]; then
  STARTUP_SCRIPT="${INSTALL_DIR}/TomcatStartup.sh"
elif [[ -f "${INSTALL_DIR}/startup.sh" ]]; then
  STARTUP_SCRIPT="${INSTALL_DIR}/startup.sh"
else
  echo "ERROR: No startup script found in ${INSTALL_DIR}" >&2
  echo "Expected TomcatStartup.sh or startup.sh" >&2
  exit 1
fi

echo "Starting Percussion DTS from ${INSTALL_DIR}..."
echo "Using startup script: ${STARTUP_SCRIPT}"
echo "Press CTRL-C to stop."
echo ""

cd "${INSTALL_DIR}"
chmod +x *.sh 2>/dev/null || true
exec "${STARTUP_SCRIPT}"
