#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG_DIR="${PROJECT_ROOT}/docker/logs"
ENV_FILE_DEFAULT="${PROJECT_ROOT}/.env.compose"
ENV_FILE_FALLBACK="${PROJECT_ROOT}/.env.compose.example"
COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"

mkdir -p "${LOG_DIR}"

if [[ -f "${ENV_FILE_DEFAULT}" ]]; then
  ENV_FILE="${ENV_FILE_DEFAULT}"
else
  ENV_FILE="${ENV_FILE_FALLBACK}"
fi

usage() {
  cat <<'EOF'
Usage:
  ./docker/scripts/perc-devctl.sh <command> [options]

Commands:
  install [--reset] [--no-bootstrap] [--install-root <path>]
      Run the host-side installer (scripts/install-cms-dev.sh) into
      the persistent install_root/ directory. Idempotent: skips if the
      install marker is already present unless --reset is passed.
      Run this ONCE per host, then `up`. Re-run after a CMS distribution
      version bump (e.g. 8.1.x → 8.2.x) or to apply local patches.

  up [--build]
      Start mysql + cms-dts compose stack. Assumes the install_root
      has been populated by `install` (otherwise the container will
      exit with a clear pointer to `install-cms-dev.sh`).

  down [--volumes]
      Stop compose stack. Use --volumes to remove MySQL volume.

  status
      Print concise stack status for agents.

  verify [--timeout-seconds N] [--interval-seconds N]
      Verify running stack health and endpoints.

  it-verify
      Run Maven integration verification with compose profile.

  deploy-jar --jar <path> [--target cms|dts|both|/abs/path] [--restart] [--verify]
      Hot deploy a built jar into running cms-dts container.

    verify-fix --jar <path> [--target cms|dts|both|/abs/path] [--restart] [--timeout-seconds N]
      Deploy jar and run verification as one operation with a final single-line result.

  logs-path
      Print the logs directory path.

    inspect-install
      Capture effective CMS and DTS database configuration from running container.

    show-generated-passwords
      Capture generated passwords file from running container if present.

Logging behavior:
  - Every command writes full output to a timestamped file under docker/logs.
  - Console output is concise and machine-readable for agent workflows.
EOF
}

ts() {
  date +%Y%m%d-%H%M%S
}

new_log_file() {
  local prefix="$1"
  echo "${LOG_DIR}/${prefix}-$(ts).log"
}

run_logged() {
  local label="$1"
  local cmd="$2"
  local log_file
  log_file="$(new_log_file "$label")"

  if bash -lc "$cmd" >"${log_file}" 2>&1; then
    echo "RESULT:OK STEP:${label} LOG:${log_file}"
    return 0
  fi

  echo "RESULT:FAIL STEP:${label} LOG:${log_file}"
  return 1
}

compose_cmd() {
  echo "docker compose --env-file '${ENV_FILE}' -f '${COMPOSE_FILE}' $*"
}

status_cmd() {
  compose_cmd ps --format json
}

verify_stack() {
  local timeout_seconds="${1:-300}"
  local interval_seconds="${2:-5}"
  local max_checks=$(( timeout_seconds / interval_seconds ))
  local check=1

  local verify_log
  verify_log="$(new_log_file "verify")"

  while [[ "${check}" -le "${max_checks}" ]]; do
    local cms_code
    local dts_code
    local health

    cms_code="$(curl -s -o /dev/null -w '%{http_code}' http://localhost:9992/Rhythmyx/rest/folders/by-path/Assets || true)"
    dts_code="$(curl -s -o /dev/null -w '%{http_code}' http://localhost:9980/ || true)"
    health="$(docker inspect -f '{{.State.Health.Status}}' percussion-cms-dts 2>/dev/null || echo unknown)"

    if { [[ "${cms_code}" == "200" ]] || [[ "${cms_code}" == "401" ]] || [[ "${cms_code}" == "403" ]]; } \
      && { [[ "${dts_code}" == "200" ]] || [[ "${dts_code}" == "401" ]] || [[ "${dts_code}" == "403" ]]; } \
      && [[ "${health}" == "healthy" ]]; then
      {
        echo "verify success"
        echo "cms_http=${cms_code}"
        echo "dts_http=${dts_code}"
        echo "container_health=${health}"
      } >"${verify_log}"
      echo "RESULT:OK STEP:verify CMS_HTTP:${cms_code} DTS_HTTP:${dts_code} HEALTH:${health} LOG:${verify_log}"
      return 0
    fi

    sleep "${interval_seconds}"
    check=$((check + 1))
  done

  {
    echo "verify failed"
    echo "timeout_seconds=${timeout_seconds}"
    echo "interval_seconds=${interval_seconds}"
    echo "--- compose ps"
    docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps || true
    echo "--- cms-dts logs"
    docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" logs --tail=200 cms-dts || true
    echo "--- mysql logs"
    docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" logs --tail=120 mysql || true
  } >"${verify_log}" 2>&1

  echo "RESULT:FAIL STEP:verify LOG:${verify_log}"
  return 1
}

if [[ $# -lt 1 ]]; then
  usage
  exit 1
fi

COMMAND="$1"
shift

case "${COMMAND}" in
  install)
    INSTALL_ARGS=()
    while [[ $# -gt 0 ]]; do
      case "$1" in
        --reset|--no-bootstrap)
          INSTALL_ARGS+=("$1")
          shift
          ;;
        --install-root)
          INSTALL_ARGS+=("$1" "$2")
          shift 2
          ;;
        *)
          echo "Unknown install option: $1" >&2
          exit 1
          ;;
      esac
    done
    run_logged "install" "cd '${PROJECT_ROOT}' && ./scripts/install-cms-dev.sh ${INSTALL_ARGS[*]}"
    ;;

  up)
    BUILD_FLAG=""
    if [[ "${1:-}" == "--build" ]]; then
      BUILD_FLAG="--build"
    fi
    run_logged "up" "$(compose_cmd up -d ${BUILD_FLAG})"
    ;;

  down)
    VOLUMES_FLAG=""
    if [[ "${1:-}" == "--volumes" ]]; then
      VOLUMES_FLAG="-v"
    fi
    run_logged "down" "$(compose_cmd down ${VOLUMES_FLAG})"
    ;;

  status)
    run_logged "status" "$(status_cmd)"
    ;;

  verify)
    TIMEOUT_SECONDS=300
    INTERVAL_SECONDS=5

    while [[ $# -gt 0 ]]; do
      case "$1" in
        --timeout-seconds)
          TIMEOUT_SECONDS="$2"
          shift 2
          ;;
        --interval-seconds)
          INTERVAL_SECONDS="$2"
          shift 2
          ;;
        *)
          echo "Unknown verify option: $1" >&2
          exit 1
          ;;
      esac
    done

    verify_stack "${TIMEOUT_SECONDS}" "${INTERVAL_SECONDS}"
    ;;

  it-verify)
    run_logged "it-verify" "cd '${PROJECT_ROOT}' && ./mvn-env.sh -P integration-test,docker-compose verify"
    ;;

  deploy-jar)
    JAR_PATH=""
    TARGET="both"
    RESTART_FLAG=""
    RUN_VERIFY="false"

    while [[ $# -gt 0 ]]; do
      case "$1" in
        --jar)
          JAR_PATH="$2"
          shift 2
          ;;
        --target)
          TARGET="$2"
          shift 2
          ;;
        --restart)
          RESTART_FLAG="--restart"
          shift
          ;;
        --verify)
          RUN_VERIFY="true"
          shift
          ;;
        *)
          echo "Unknown deploy-jar option: $1" >&2
          exit 1
          ;;
      esac
    done

    if [[ -z "${JAR_PATH}" ]]; then
      echo "deploy-jar requires --jar <path>" >&2
      exit 1
    fi

    run_logged "deploy-jar" "cd '${PROJECT_ROOT}' && ./docker/scripts/hot-deploy-jar.sh --jar '${JAR_PATH}' --target '${TARGET}' ${RESTART_FLAG}"

    if [[ "${RUN_VERIFY}" == "true" ]]; then
      verify_stack 180 5
    fi
    ;;

  verify-fix)
    JAR_PATH=""
    TARGET="both"
    RESTART_FLAG="--restart"
    TIMEOUT_SECONDS=240

    while [[ $# -gt 0 ]]; do
      case "$1" in
        --jar)
          JAR_PATH="$2"
          shift 2
          ;;
        --target)
          TARGET="$2"
          shift 2
          ;;
        --restart)
          RESTART_FLAG="--restart"
          shift
          ;;
        --no-restart)
          RESTART_FLAG=""
          shift
          ;;
        --timeout-seconds)
          TIMEOUT_SECONDS="$2"
          shift 2
          ;;
        *)
          echo "Unknown verify-fix option: $1" >&2
          exit 1
          ;;
      esac
    done

    if [[ -z "${JAR_PATH}" ]]; then
      echo "verify-fix requires --jar <path>" >&2
      exit 1
    fi

    DEPLOY_LOG=""
    VERIFY_LOG=""

    if deploy_output=$(bash -lc "cd '${PROJECT_ROOT}' && ./docker/scripts/perc-devctl.sh deploy-jar --jar '${JAR_PATH}' --target '${TARGET}' ${RESTART_FLAG}" 2>&1); then
      DEPLOY_LOG=$(echo "$deploy_output" | awk '/RESULT:OK STEP:deploy-jar LOG:/{print $NF}' | sed 's/^LOG://')
    else
      DEPLOY_LOG=$(echo "$deploy_output" | awk '/RESULT:FAIL STEP:deploy-jar LOG:/{print $NF}' | sed 's/^LOG://')
      if [[ -z "$DEPLOY_LOG" ]]; then
        DEPLOY_LOG="${LOG_DIR}/deploy-jar-unknown.log"
      fi
      echo "RESULT:FAIL STEP:verify-fix PHASE:deploy DEPLOY_LOG:${DEPLOY_LOG}"
      exit 1
    fi

    if verify_output=$(bash -lc "cd '${PROJECT_ROOT}' && ./docker/scripts/perc-devctl.sh verify --timeout-seconds '${TIMEOUT_SECONDS}'" 2>&1); then
      VERIFY_LOG=$(echo "$verify_output" | awk '/RESULT:OK STEP:verify/{print $NF}' | sed 's/^LOG://')
      echo "RESULT:OK STEP:verify-fix DEPLOY_LOG:${DEPLOY_LOG} VERIFY_LOG:${VERIFY_LOG}"
      exit 0
    fi

    VERIFY_LOG=$(echo "$verify_output" | awk '/RESULT:FAIL STEP:verify LOG:/{print $NF}' | sed 's/^LOG://')
    if [[ -z "$VERIFY_LOG" ]]; then
      VERIFY_LOG="${LOG_DIR}/verify-unknown.log"
    fi
    echo "RESULT:FAIL STEP:verify-fix PHASE:verify DEPLOY_LOG:${DEPLOY_LOG} VERIFY_LOG:${VERIFY_LOG}"
    exit 1
    ;;

  logs-path)
    echo "RESULT:OK STEP:logs-path LOG_DIR:${LOG_DIR}"
    ;;

  inspect-install)
    run_logged "inspect-install" "docker exec percussion-cms-dts bash -lc '
      set -euo pipefail
      install_root=\"${PERC_INSTALL_ROOT:-/opt/Percussion}\"
      cms_repo=\"$install_root/rxconfig/Installer/rxrepository.properties\"
      dts_ds=\"$install_root/Deployment/Server/conf/perc/perc-datasources.properties\"
      echo \"install_root=$install_root\"
      if [ -f \"$cms_repo\" ]; then
        echo \"--- cms rxrepository.properties\"
        grep -E \"^(DB_BACKEND|DB_DRIVER_NAME|DB_DRIVER_CLASS_NAME|DB_SERVER|DB_SCHEMA|DB_NAME|UID)=\" \"$cms_repo\" || true
      else
        echo \"cms_repo_missing=$cms_repo\"
      fi
      if [ -f \"$dts_ds\" ]; then
        echo \"--- dts perc-datasources.properties\"
        grep -E \"^(db.username|db.name|db.schema|jdbcDriver|jdbcUrl|hibernate.dialect)=\" \"$dts_ds\" || true
      else
        echo \"dts_datasource_missing=$dts_ds\"
      fi
    '"
    ;;

  show-generated-passwords)
    run_logged "show-generated-passwords" "docker exec percussion-cms-dts bash -lc '
      set -euo pipefail
      install_root=\"${PERC_INSTALL_ROOT:-/opt/Percussion}\"
      pwd_file=\"$install_root/var/config/generated/passwords\"
      if [ ! -f \"$pwd_file\" ]; then
        echo \"generated_passwords_missing=$pwd_file\"
        exit 1
      fi
      echo \"generated_passwords_file=$pwd_file\"
      cat \"$pwd_file\"
    '"
    ;;

  help|-h|--help)
    usage
    ;;

  *)
    echo "Unknown command: ${COMMAND}" >&2
    usage >&2
    exit 1
    ;;
esac
