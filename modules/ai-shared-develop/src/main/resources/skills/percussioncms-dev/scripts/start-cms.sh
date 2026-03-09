#!/usr/bin/env bash
# start-cms.sh — Start a local Percussion CMS instance.
#
# Usage:
#   ./start-cms.sh [INSTALL_DIR]
#
# Arguments:
#   INSTALL_DIR   The CMS installation directory (default: ~/percussioncms-install)
#
# Environment:
#   CMS_INSTALL_DIR  Alternative to the positional argument
#
set -euo pipefail

INSTALL_DIR="${1:-${CMS_INSTALL_DIR:-${HOME}/percussioncms-install}}"

# Validate installation
if [[ ! -d "${INSTALL_DIR}/jetty" ]]; then
  echo "ERROR: CMS installation not found at ${INSTALL_DIR}/jetty" >&2
  echo "Run install-cms.sh first." >&2
  exit 1
fi

if [[ ! -f "${INSTALL_DIR}/jetty/StartJetty.sh" ]]; then
  echo "ERROR: StartJetty.sh not found in ${INSTALL_DIR}/jetty" >&2
  exit 1
fi

# Check JRE symlink
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

echo "Starting Percussion CMS from ${INSTALL_DIR}..."
echo "Press CTRL-C to stop."
echo ""

cd "${INSTALL_DIR}/jetty"
chmod +x *.sh 2>/dev/null || true
exec ./StartJetty.sh
