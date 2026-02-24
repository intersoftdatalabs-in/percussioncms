#!/usr/bin/env bash
set -euo pipefail

log() {
  printf '[install-update] %s\n' "$*"
}

INSTALL_ROOT="${PERC_INSTALL_ROOT:-/opt/Percussion}"
INSTALL_MODE="${PERC_INSTALL_MODE:-install-if-missing}"
INSTALL_MARKER_NAME="${PERC_INSTALL_MARKER:-.percussion-install-complete}"
INSTALL_MARKER_PATH="${INSTALL_ROOT}/${INSTALL_MARKER_NAME}"
SERVICE_MODE="${SERVICE_MODE:-cms-dts}"
CMS_JAR="${CMS_DISTRIBUTION_JAR:-/workspace/modules/perc-distribution-tree/target/perc-distribution-tree.jar}"
DTS_JAR="${DTS_DISTRIBUTION_JAR:-/workspace/deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution.jar}"

db_config_value() {
  local primary="$1"
  local fallback="$2"
  local default_value="${3:-}"
  local value="${!primary:-}"
  if [[ -z "$value" && -n "$fallback" ]]; then
    value="${!fallback:-}"
  fi
  if [[ -z "$value" ]]; then
    value="$default_value"
  fi
  printf '%s' "$value"
}

build_installer_db_args() {
  local -a args=()
  local db_type
  local db_ssl_enabled
  local db_ssl_verify
  local db_ssl_allow_self_signed

  db_type="$(db_config_value PERC_DB_TYPE DB_TYPE derby)"
  db_ssl_enabled="$(db_config_value PERC_DB_SSL_ENABLED DB_SSL_ENABLED true)"
  db_ssl_verify="$(db_config_value PERC_DB_SSL_VERIFY DB_SSL_VERIFY true)"
  db_ssl_allow_self_signed="$(db_config_value PERC_DB_SSL_ALLOW_SELF_SIGNED DB_SSL_ALLOW_SELF_SIGNED false)"

  args+=("--db.type=${db_type}")
  args+=("--db.ssl.enabled=${db_ssl_enabled}")
  args+=("--db.ssl.verify=${db_ssl_verify}")
  args+=("--db.ssl.allowSelfSigned=${db_ssl_allow_self_signed}")

  local env_file
  env_file="$(db_config_value PERC_DB_CONFIG_ENV_FILE DB_CONFIG_ENV_FILE "")"
  if [[ -n "$env_file" ]]; then
    args+=("--db.config.env.file=${env_file}")
  fi

  local host
  host="$(db_config_value PERC_DB_HOST DB_HOST "")"
  [[ -n "$host" ]] && args+=("--db.host=${host}")

  local port
  port="$(db_config_value PERC_DB_PORT DB_PORT "")"
  [[ -n "$port" ]] && args+=("--db.port=${port}")

  local name
  name="$(db_config_value PERC_DB_NAME DB_NAME "")"
  [[ -n "$name" ]] && args+=("--db.name=${name}")

  local schema
  schema="$(db_config_value PERC_DB_SCHEMA DB_SCHEMA "")"
  [[ -n "$schema" ]] && args+=("--db.schema=${schema}")

  local user
  user="$(db_config_value PERC_DB_USER DB_USER "")"
  [[ -n "$user" ]] && args+=("--db.user=${user}")

  local password
  password="$(db_config_value PERC_DB_PASSWORD DB_PASSWORD "")"
  [[ -n "$password" ]] && args+=("--db.password=${password}")

  local truststore_path
  truststore_path="$(db_config_value PERC_DB_SSL_TRUSTSTORE_PATH DB_SSL_TRUSTSTORE_PATH "")"
  [[ -n "$truststore_path" ]] && args+=("--db.ssl.trustStorePath=${truststore_path}")

  local truststore_password
  truststore_password="$(db_config_value PERC_DB_SSL_TRUSTSTORE_PASSWORD DB_SSL_TRUSTSTORE_PASSWORD "")"
  [[ -n "$truststore_password" ]] && args+=("--db.ssl.trustStorePassword=${truststore_password}")

  local keystore_path
  keystore_path="$(db_config_value PERC_DB_SSL_KEYSTORE_PATH DB_SSL_KEYSTORE_PATH "")"
  [[ -n "$keystore_path" ]] && args+=("--db.ssl.keyStorePath=${keystore_path}")

  local keystore_password
  keystore_password="$(db_config_value PERC_DB_SSL_KEYSTORE_PASSWORD DB_SSL_KEYSTORE_PASSWORD "")"
  [[ -n "$keystore_password" ]] && args+=("--db.ssl.keyStorePassword=${keystore_password}")

  printf '%s\n' "${args[@]}"
}

install_jar_if_present() {
  local label="$1"
  local jar_path="$2"
  shift 2
  local -a installer_args=("$@")

  if [[ -f "$jar_path" ]]; then
    log "Running ${label} installer/update: ${jar_path} -> ${INSTALL_ROOT}"
    java -jar "$jar_path" "$INSTALL_ROOT" "${installer_args[@]}"
  else
    log "Skipping ${label} installer/update; jar not found: ${jar_path}"
  fi
}

start_cms() {
  local cms_start_script="${INSTALL_ROOT}/jetty/StartJetty.sh"
  if [[ ! -x "$cms_start_script" ]]; then
    log "ERROR: CMS start script missing or not executable: ${cms_start_script}"
    exit 1
  fi

  log "Starting CMS via ${cms_start_script}"
  (
    cd "${INSTALL_ROOT}/jetty"
    ./StartJetty.sh
  )

  log "CMS startup triggered."
}

start_dts() {
  local dts_start_script="${INSTALL_ROOT}/TomcatStartup.sh"
  if [[ ! -x "$dts_start_script" ]]; then
    dts_start_script="${INSTALL_ROOT}/startup.sh"
  fi

  if [[ ! -x "$dts_start_script" ]]; then
    log "ERROR: DTS start script missing or not executable: ${INSTALL_ROOT}/TomcatStartup.sh or ${INSTALL_ROOT}/startup.sh"
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

main() {
  mkdir -p "$INSTALL_ROOT"
  mapfile -t installer_db_args < <(build_installer_db_args)

  case "$INSTALL_MODE" in
    install-always)
      log "Install mode is install-always; running installer/update flow."
      install_jar_if_present "CMS" "$CMS_JAR" "${installer_db_args[@]}"
      install_jar_if_present "DTS" "$DTS_JAR" "${installer_db_args[@]}"
      touch "$INSTALL_MARKER_PATH"
      ;;
    install-if-missing)
      if [[ -f "$INSTALL_MARKER_PATH" ]]; then
        log "Install marker found at ${INSTALL_MARKER_PATH}; skipping installer/update flow."
      else
        log "Install marker missing; running installer/update flow."
        install_jar_if_present "CMS" "$CMS_JAR" "${installer_db_args[@]}"
        install_jar_if_present "DTS" "$DTS_JAR" "${installer_db_args[@]}"
        touch "$INSTALL_MARKER_PATH"
      fi
      ;;
    skip-install)
      log "Install mode is skip-install; installer/update flow skipped."
      ;;
    *)
      log "ERROR: Unsupported PERC_INSTALL_MODE=${INSTALL_MODE}; expected install-always|install-if-missing|skip-install"
      exit 1
      ;;
  esac

  case "$SERVICE_MODE" in
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
}

main "$@"
