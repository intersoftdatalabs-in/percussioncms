#!/usr/bin/env bash
#
# scripts/install-cms-dev.sh
#
# Host-side CMS installer for the docker dev/test runtime.
#
# Purpose: Run the Percussion CMS installer Java + Ant buildfile ONCE on
# the host into a single persistent install_root directory. The
# docker-compose.yml then bind-mounts that single dir into the
# cms-dts container at /opt/Percussion/, so:
#
#   * container restarts do NOT re-install (the install_root persists on
#     the host);
#   * hot-deploys (jar swaps, config edits) are local file edits in
#     install_root/, picked up by the container on next restart;
#   * the container's only job is to run StartJetty.sh.
#
# Architecture note: this script is for **development and test** use.
# Production deployment is a separate scope (different install path,
# different DB lifecycle, different container patterns).
#
# Usage:
#   ./scripts/install-cms-dev.sh [--install-root <path>] [--reset] [--no-bootstrap]
#
# Defaults:
#   --install-root ./docker/dev-data/cms-dts/install_root
#   --reset    do not honor the install marker; reinstall even if marked
#   --no-bootstrap  do NOT copy pre-seeded docker/dev-data/cms-dts/
#                    {ObjectStore,var,rxconfig,Deployment/Server/conf,jetty/base}
#                    into install_root if install_root is empty
#
# Reads DB config from .env.compose (PERC_DB_TYPE, _HOST, _PORT, _NAME,
# _USER, _PASSWORD, _SSL_*, _TRUSTSTORE_*, _KEYSTORE_*).
#
# Cross-platform: requires POSIX bash, JDK 21, docker/dev-data seed
# layout (all host-relative paths). Windows users run via WSL or
# Git Bash.
#
# Logging: writes full output to docker/logs/install-<ts>.log; prints
# `RESULT:OK STEP:install LOG:<path>` on success or
# `RESULT:FAIL STEP:install LOG:<path>` on failure. Idempotent.

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${PROJECT_ROOT}/docker/logs"
ENV_FILE="${PROJECT_ROOT}/.env.compose"
INSTALL_ROOT_DEFAULT="/opt/Percussion"
INSTALL_ROOT="${INSTALL_ROOT_DEFAULT}"
RESET="false"
BOOTSTRAP="false"
SKIP_DTS="true"

usage() {
  cat <<'EOF'
Usage: ./scripts/install-cms-dev.sh [options]

Options:
  --install-root <path>   Target install dir (default: /opt/Percussion)
                          The host path MUST equal the in-container path that
                          docker-compose.yml bind-mounts at /opt/Percussion,
                          otherwise the absolute paths the installer writes into
                          config files (rxrepository.properties etc.) will not
                          match what the container reads at runtime.
  --reset                 Reinstall even if the marker file is present
  --skip-dts              Run the CMS installer only (DTS is out of scope for
                          992-react-content-explorer story automation).
                          Default is skip-dts=true.
  -h | --help             Show this help

Reads DB config from .env.compose (PERC_DB_*).

Examples:
  sudo mkdir -p /opt/Percussion && sudo chown $USER /opt/Percussion
  ./scripts/install-cms-dev.sh
  ./scripts/install-cms-dev.sh --reset
  ./scripts/install-cms-dev.sh --install-root /home/nate/installs/cms82
EOF
}

ts() { date -u +'%Y-%m-%dT%H:%M:%SZ'; }
new_log_file() { echo "${LOG_DIR}/install-$(date +%Y%m%d-%H%M%S).log"; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --install-root) INSTALL_ROOT="$2"; shift 2 ;;
    --reset) RESET="true"; shift ;;
    --skip-dts) SKIP_DTS="true"; shift ;;
    --install-dts) SKIP_DTS="false"; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 1 ;;
  esac
done

mkdir -p "${LOG_DIR}"
LOG_FILE="$(new_log_file)"

log() { printf '[install-cms-dev] %s\n' "$*"; }

# Load DB config from .env.compose. Quote-safe via `set -a`.
if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
else
  log "ERROR: ${ENV_FILE} not found. Copy .env.compose.example to .env.compose and edit secrets."
  exit 1
fi

# Resolve installer jars. Build artifacts are at conventional Maven paths.
CMS_JAR="${PROJECT_ROOT}/modules/perc-distribution-tree/target/perc-distribution-tree.jar"
DTS_JAR="${PROJECT_ROOT}/deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution.jar"

if [[ ! -f "${CMS_JAR}" ]]; then
  log "ERROR: CMS distribution jar not found: ${CMS_JAR}. Run ./mvn-env.sh clean install -DskipTests=true."
  exit 1
fi
if [[ ! -f "${DTS_JAR}" ]]; then
  log "ERROR: DTS distribution jar not found: ${DTS_JAR}. Run ./mvn-env.sh clean install -DskipTests=true."
  exit 1
fi

# Resolve DB config with sensible defaults (matches install-update.sh's
# db_config_value fallback chain). PERC_DB_TYPE defaults to derby for
# out-of-the-box experience; .env.compose.example points users at mysql.
db_config_value() {
  local primary="$1"
  local fallback="$2"
  local default_value="${3:-}"
  local value="${!primary:-}"
  if [[ -z "${value}" && -n "${fallback}" ]]; then
    value="${!fallback:-}"
  fi
  if [[ -z "${value}" ]]; then
    value="${default_value}"
  fi
  printf '%s' "${value}"
}

build_installer_db_args() {
  local -a args=()
  local db_type db_ssl_enabled db_ssl_verify db_ssl_allow_self_signed

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
  if [[ -n "${env_file}" ]]; then
    args+=("--db.config.env.file=${env_file}")
  fi

  local host port name schema user password
  host="$(db_config_value PERC_DB_HOST DB_HOST "")"
  port="$(db_config_value PERC_DB_PORT DB_PORT "")"
  name="$(db_config_value PERC_DB_NAME DB_NAME "")"
  schema="$(db_config_value PERC_DB_SCHEMA DB_SCHEMA "")"
  user="$(db_config_value PERC_DB_USER DB_USER "")"
  password="$(db_config_value PERC_DB_PASSWORD DB_PASSWORD "")"

  [[ -n "${host}"     ]] && args+=("--db.host=${host}")
  [[ -n "${port}"     ]] && args+=("--db.port=${port}")
  [[ -n "${name}"     ]] && args+=("--db.name=${name}")
  [[ -n "${schema}"   ]] && args+=("--db.schema=${schema}")
  [[ -n "${user}"     ]] && args+=("--db.user=${user}")
  [[ -n "${password}" ]] && args+=("--db.password=${password}")

  local truststore_path truststore_password keystore_path keystore_password
  truststore_path="$(db_config_value PERC_DB_SSL_TRUSTSTORE_PATH DB_SSL_TRUSTSTORE_PATH "")"
  [[ -n "${truststore_path}" ]] && args+=("--db.ssl.trustStorePath=${truststore_path}")
  truststore_password="$(db_config_value PERC_DB_SSL_TRUSTSTORE_PASSWORD DB_SSL_TRUSTSTORE_PASSWORD "")"
  [[ -n "${truststore_password}" ]] && args+=("--db.ssl.trustStorePassword=${truststore_password}")
  keystore_path="$(db_config_value PERC_DB_SSL_KEYSTORE_PATH DB_SSL_KEYSTORE_PATH "")"
  [[ -n "${keystore_path}" ]] && args+=("--db.ssl.keyStorePath=${keystore_path}")
  keystore_password="$(db_config_value PERC_DB_SSL_KEYSTORE_PASSWORD DB_SSL_KEYSTORE_PASSWORD "")"
  [[ -n "${keystore_password}" ]] && args+=("--db.ssl.keyStorePassword=${keystore_password}")

  printf '%s\n' "${args[@]}"
}

# Bootstrap: if install_root is empty (or missing), copy pre-seeded
# docker/dev-data/cms-dts subdirs into it. This preserves prior dev state
# (e.g. ObjectStore from an earlier installer run) while letting the new
# install add/upgrade files on top.
bootstrap_install_root() {
  local seed_base="${PROJECT_ROOT}/docker/dev-data/cms-dts"
  local needs_init="false"
  if [[ ! -d "${INSTALL_ROOT}" ]]; then
    needs_init="true"
  elif [[ -z "$(ls -A "${INSTALL_ROOT}" 2>/dev/null)" ]]; then
    needs_init="true"
  fi

  if [[ "${needs_init}" != "true" ]]; then
    log "install_root already populated (${INSTALL_ROOT}); skipping bootstrap"
    return 0
  fi

  log "Bootstrapping install_root ${INSTALL_ROOT} from ${seed_base}"
  mkdir -p "${INSTALL_ROOT}"

  for sub in ObjectStore var rxconfig Deployment/Server/conf jetty/base; do
    local src="${seed_base}/${sub}"
    local dst="${INSTALL_ROOT}/${sub}"
    if [[ -d "${src}" ]]; then
      mkdir -p "${dst}"
      # `cp -a src/. dst/` merges into existing dst rather than nesting src inside it.
      cp -a "${src}/." "${dst}/" || {
        log "ERROR: failed to seed ${sub} from ${src}"
        return 1
      }
      log "  seeded ${sub}"
    else
      log "  no seed for ${sub} (ok; install will create it)"
    fi
  done
}

# Idempotency: skip install if marker exists and --reset not passed.
MARKER_PATH="${INSTALL_ROOT}/.percussion-install-complete"
if [[ -f "${MARKER_PATH}" && "${RESET}" != "true" ]]; then
  log "Install marker present at ${MARKER_PATH}; skipping install (use --reset to force)."
  echo "RESULT:OK STEP:install ALREADY_INSTALLED LOG:${LOG_FILE}"
  exit 0
fi

if [[ "${BOOTSTRAP}" == "true" ]]; then
  bootstrap_install_root
fi

mkdir -p "${INSTALL_ROOT}"

log "Installing CMS+DTS into ${INSTALL_ROOT}"
log "  CMS_JAR=${CMS_JAR}"
log "  DTS_JAR=${DTS_JAR}"
log "  DB_TYPE=${PERC_DB_TYPE:-derby}"

INSTALLER_DB_ARGS=()
mapfile -t INSTALLER_DB_ARGS < <(build_installer_db_args)

log "  DB_ARGS=${INSTALLER_DB_ARGS[*]:-<none>}"

# Run the install. The installer Java program does:
#   1. Unpack itself to /tmp/percInstallTmp_*/
#   2. Run Ant with install.xml (deletes old files, copies new ones,
#      runs DB schema migration)
#   3. Exit with non-zero on failure.
{
  set +e
  java -jar "${CMS_JAR}" "${INSTALL_ROOT}" "${INSTALLER_DB_ARGS[@]}"
  rc_cms=$?
  if [[ "${SKIP_DTS}" != "true" ]]; then
    java -jar "${DTS_JAR}" "${INSTALL_ROOT}" "${INSTALLER_DB_ARGS[@]}"
    rc_dts=$?
  else
    rc_dts=0
    log "Skipping DTS installer (--skip-dts)"
  fi
  set -e
  if [[ ${rc_cms} -ne 0 ]]; then
    log "CMS installer exit code: ${rc_cms}"
    exit "${rc_cms}"
  fi
  if [[ ${rc_dts} -ne 0 ]]; then
    log "DTS installer exit code: ${rc_dts}"
    exit "${rc_dts}"
  fi
} >"${LOG_FILE}" 2>&1

touch "${MARKER_PATH}"
log "Install complete. Marker written to ${MARKER_PATH}"
echo "RESULT:OK STEP:install LOG:${LOG_FILE}"