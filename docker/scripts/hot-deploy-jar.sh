#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
hot-deploy-jar.sh --jar <path-to-built-jar> [--container <name>] [--target cms|dts|both|<absolute-path>] [--restart]

Examples:
  ./docker/scripts/hot-deploy-jar.sh --jar modules/utils/target/utils-8.2.0-SNAPSHOT.jar --target both --restart
  ./docker/scripts/hot-deploy-jar.sh --jar modules/perc-system/target/perc-system-8.2.0-SNAPSHOT.jar --target /opt/Percussion/jetty/base/lib

Notes:
  - This script copies a built module jar into a running container for fast validation.
  - Default container: percussion-cms-dts
  - Default target: both (CMS and DTS lib dirs)
  - If --restart is set, container restart is used to pick up jar changes.
EOF
}

CONTAINER_NAME="percussion-cms-dts"
JAR_PATH=""
TARGET="both"
RESTART_CONTAINER="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --jar)
      JAR_PATH="$2"
      shift 2
      ;;
    --container)
      CONTAINER_NAME="$2"
      shift 2
      ;;
    --target)
      TARGET="$2"
      shift 2
      ;;
    --restart)
      RESTART_CONTAINER="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "$JAR_PATH" ]]; then
  echo "ERROR: --jar is required" >&2
  usage >&2
  exit 1
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "ERROR: jar not found: $JAR_PATH" >&2
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -Fxq "$CONTAINER_NAME"; then
  echo "ERROR: container is not running: $CONTAINER_NAME" >&2
  exit 1
fi

JAR_BASENAME="$(basename "$JAR_PATH")"
TS="$(date +%Y%m%d%H%M%S)"

deploy_to_path() {
  local target_dir="$1"
  local target_jar="${target_dir}/${JAR_BASENAME}"
  local backup_jar="${target_jar}.bak.${TS}"

  echo "Deploying ${JAR_BASENAME} -> ${CONTAINER_NAME}:${target_dir}"
  docker exec "$CONTAINER_NAME" bash -lc "mkdir -p '$target_dir'"
  docker exec "$CONTAINER_NAME" bash -lc "if [ -f '$target_jar' ]; then cp '$target_jar' '$backup_jar'; fi"
  docker cp "$JAR_PATH" "${CONTAINER_NAME}:${target_jar}"
}

case "$TARGET" in
  cms)
    deploy_to_path "/opt/Percussion/jetty/base/lib"
    ;;
  dts)
    deploy_to_path "/opt/Percussion/Deployment/Server/lib"
    ;;
  both)
    deploy_to_path "/opt/Percussion/jetty/base/lib"
    deploy_to_path "/opt/Percussion/Deployment/Server/lib"
    ;;
  /*)
    deploy_to_path "$TARGET"
    ;;
  *)
    echo "ERROR: unsupported --target value: $TARGET" >&2
    echo "Use cms|dts|both or an absolute container path." >&2
    exit 1
    ;;
esac

if [[ "$RESTART_CONTAINER" == "true" ]]; then
  echo "Restarting container ${CONTAINER_NAME} to apply update"
  docker restart "$CONTAINER_NAME" >/dev/null
fi

echo "Hot deploy complete."
