#!/usr/bin/env bash
#
# docker/entrypoint/install-update.sh
#
# Container entrypoint for the cms-dts dev/test docker compose stack.
# **Service-only mode** as of the install-on-host refactor (992-react-
# content-explorer story automation follow-up). The install runs ONCE
# on the host via scripts/install-cms-dev.sh; this entrypoint only
# starts the service.
#
# Contract with the host bind mount:
#   docker-compose.yml bind-mounts ./docker/dev-data/cms-dts/install_root/
#   into the container at /opt/Percussion/. The install lives entirely
#   on the host; the container is responsible for executing the service.
#
# If /opt/Percussion/jetty/StartJetty.sh is missing on entry, this
# script exits non-zero with a clear pointer to the host-side installer.
#
# Cross-platform: Linux/macOS containers (Jetty + bash). Windows
# containers (if any) would require a different launcher; documented
# out-of-scope for the dev/test runtime.

set -euo pipefail

log() { printf '[install-update] %s\n' "$*"; }

INSTALL_ROOT="${PERC_INSTALL_ROOT:-/opt/Percussion}"
CMS_START_SCRIPT="${INSTALL_ROOT}/jetty/StartJetty.sh"
DTS_START_SCRIPT_PRIMARY="${INSTALL_ROOT}/TomcatStartup.sh"
DTS_START_SCRIPT_FALLBACK="${INSTALL_ROOT}/startup.sh"

start_cms() {
  if [[ ! -x "${CMS_START_SCRIPT}" ]]; then
    log "ERROR: CMS start script missing or not executable: ${CMS_START_SCRIPT}"
    log "The install must run on the host first:"
    log "  ./scripts/install-cms-dev.sh"
    log "  docker compose --env-file .env.compose -f docker-compose.yml up -d"
    exit 1
  fi

  log "Starting CMS via ${CMS_START_SCRIPT}"
  (
    cd "${INSTALL_ROOT}/jetty"
    ./StartJetty.sh
  )

  log "CMS startup triggered."
}

start_dts() {
  local dts_start_script="${DTS_START_SCRIPT_PRIMARY}"
  if [[ ! -x "${dts_start_script}" ]]; then
    dts_start_script="${DTS_START_SCRIPT_FALLBACK}"
  fi
  if [[ ! -x "${dts_start_script}" ]]; then
    log "ERROR: DTS start script missing: ${DTS_START_SCRIPT_PRIMARY} (or fallback ${DTS_START_SCRIPT_FALLBACK})"
    log "The install must run on the host first:"
    log "  ./scripts/install-cms-dev.sh"
    exit 1
  fi

  log "Starting DTS via ${dts_start_script}"
  (
    cd "${INSTALL_ROOT}"
    ./TomcatStartup.sh
  )

  log "DTS startup triggered."
}

stream_logs_foreground() {
  shopt -s nullglob
  local log_files=(
    "${INSTALL_ROOT}/jetty/base/logs"/*.log
    "${INSTALL_ROOT}/Deployment/Server/logs"/*.log
  )

  if (( ${#log_files[@]} == 0 )); then
    log "No log files found yet. Keeping container alive while waiting for logs."
    exec tail -f /dev/null
  fi

  log "Streaming CMS/DTS logs to keep container in foreground."
  exec tail -F "${log_files[@]}"
}

# Sanity check that the bind-mounted install_root has a real CMS tree.
if [[ ! -x "${CMS_START_SCRIPT}" ]]; then
  log "ERROR: ${CMS_START_SCRIPT} not present. Did the host-side installer run?"
  log "  ./scripts/install-cms-dev.sh"
  exit 1
fi

case "${SERVICE_MODE:-cms-dts}" in
  cms)
    start_cms
    stream_logs_foreground
    ;;
  dts)
    start_dts
    stream_logs_foreground
    ;;
  cms-dts)
    start_cms
    start_dts
    stream_logs_foreground
    ;;
  *)
    log "ERROR: Unsupported SERVICE_MODE=${SERVICE_MODE}; expected cms|dts|cms-dts"
    exit 1
    ;;
esac