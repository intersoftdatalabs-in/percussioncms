#!/usr/bin/env bash
# hot-deploy-local.sh - Build selected CMS modules and copy outputs into a local install.
#
# Typical usage:
#   ./scripts/hot-deploy-local.sh --install-dir /home/nate/installs/cms-8.2-dev --modules system,rest
#   ./scripts/hot-deploy-local.sh --install-dir /home/nate/installs/cms-8.2-dev --modules webui
#
# Notes:
# - This script targets local installs, not Docker containers.
# - For jar modules, artifacts are copied to jetty/base/webapps/Rhythmyx/WEB-INF/lib.
# - For webui, the built WAR is unzipped into jetty/base/webapps/Rhythmyx.
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  ./scripts/hot-deploy-local.sh --install-dir <path> [options]

Options:
  --install-dir <path>   CMS install directory (contains jetty/base). Required.
  --modules <list>       Comma-separated modules: system,rest,sitemanage,webui
                         Default: system
  --skip-build           Skip Maven build and deploy existing artifacts from target/.
  --restart              Restart local Jetty after deploy (StopJetty.sh + StartJetty.sh).
  --with-tests           Run tests during Maven build (default is -DskipTests).
  --help                 Show this help.

Examples:
  ./scripts/hot-deploy-local.sh --install-dir /home/nate/installs/cms-8.2-dev --modules system
  ./scripts/hot-deploy-local.sh --install-dir /home/nate/installs/cms-8.2-dev --modules rest,sitemanage
  ./scripts/hot-deploy-local.sh --install-dir /home/nate/installs/cms-8.2-dev --modules webui --restart
EOF
}

PROJECT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
INSTALL_DIR=""
MODULES_CSV="system"
SKIP_BUILD=0
RESTART=0
RUN_TESTS=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --install-dir)
      INSTALL_DIR="$2"
      shift 2
      ;;
    --modules)
      MODULES_CSV="$2"
      shift 2
      ;;
    --skip-build)
      SKIP_BUILD=1
      shift
      ;;
    --restart)
      RESTART=1
      shift
      ;;
    --with-tests)
      RUN_TESTS=1
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$INSTALL_DIR" ]]; then
  echo "ERROR: --install-dir is required." >&2
  usage
  exit 1
fi

JETTY_BASE_DIR="$INSTALL_DIR/jetty/base"
WEBAPP_DIR="$JETTY_BASE_DIR/webapps/Rhythmyx"
LIB_DIR="$WEBAPP_DIR/WEB-INF/lib"
WEB_RESOURCES_JS_DIR="$INSTALL_DIR/web_resources/cm/common/js"

if [[ ! -d "$JETTY_BASE_DIR" ]]; then
  echo "ERROR: Jetty base directory not found: $JETTY_BASE_DIR" >&2
  exit 1
fi
if [[ ! -d "$LIB_DIR" ]]; then
  echo "ERROR: CMS lib directory not found: $LIB_DIR" >&2
  exit 1
fi

IFS=',' read -r -a MODULES <<< "$MODULES_CSV"
if [[ ${#MODULES[@]} -eq 0 ]]; then
  echo "ERROR: No modules were provided." >&2
  exit 1
fi

for module in "${MODULES[@]}"; do
  case "$module" in
    system|rest|sitemanage|webui)
      ;;
    *)
      echo "ERROR: Unsupported module '$module'. Allowed: system,rest,sitemanage,webui" >&2
      exit 1
      ;;
  esac
done

maven_test_flag="-DskipTests"
if [[ $RUN_TESTS -eq 1 ]]; then
  maven_test_flag=""
fi

run_maven_build() {
  local module_path="$1"
  local maven_cmd=("$PROJECT_ROOT/mvn-env.sh" -pl "$module_path" clean install)
  if [[ -n "$maven_test_flag" ]]; then
    maven_cmd+=("$maven_test_flag")
  fi

  echo "Building module path: $module_path"
  (cd "$PROJECT_ROOT" && "${maven_cmd[@]}")
}

find_primary_jar() {
  local target_dir="$1"
  local artifact_id="$2"

  ls -1t "$target_dir/${artifact_id}"-*.jar 2>/dev/null \
    | grep -Ev '(-sources|-javadoc|-tests|original)\.jar$' \
    | head -n 1
}

deploy_jar_module() {
  local module_path="$1"
  local artifact_id="$2"
  local target_dir="$PROJECT_ROOT/$module_path/target"

  if [[ $SKIP_BUILD -eq 0 ]]; then
    run_maven_build "$module_path"
  fi

  if [[ ! -d "$target_dir" ]]; then
    echo "ERROR: target directory not found for $module_path: $target_dir" >&2
    exit 1
  fi

  local jar_path
  jar_path="$(find_primary_jar "$target_dir" "$artifact_id")"
  if [[ -z "$jar_path" ]]; then
    echo "ERROR: Could not find built jar for $artifact_id in $target_dir" >&2
    exit 1
  fi

  local jar_name
  jar_name="$(basename "$jar_path")"

  # Avoid stale duplicate versions for the same module artifact.
  find "$LIB_DIR" -maxdepth 1 -type f -name "${artifact_id}-*.jar" ! -name "$jar_name" -delete

  cp -f "$jar_path" "$LIB_DIR/$jar_name"
  echo "Deployed $jar_name -> $LIB_DIR"
}

deploy_webui() {
  local module_path="WebUI"
  local target_dir="$PROJECT_ROOT/$module_path/target"

  if [[ $SKIP_BUILD -eq 0 ]]; then
    run_maven_build "$module_path"
  fi

  if [[ ! -d "$target_dir" ]]; then
    echo "ERROR: target directory not found for WebUI: $target_dir" >&2
    exit 1
  fi

  local war_path
  war_path="$(ls -1t "$target_dir"/perc-web-ui-*.war 2>/dev/null | head -n 1)"
  if [[ -z "$war_path" ]]; then
    echo "ERROR: Could not find built WAR in $target_dir" >&2
    exit 1
  fi

  echo "Unzipping $(basename "$war_path") -> $WEBAPP_DIR"
  unzip -oq "$war_path" -d "$WEBAPP_DIR"

  if [[ -d "$WEBAPP_DIR/cm/common/js" ]]; then
    mkdir -p "$WEB_RESOURCES_JS_DIR"
    cp -fR "$WEBAPP_DIR/cm/common/js/." "$WEB_RESOURCES_JS_DIR/"
    echo "Synced common JS -> $WEB_RESOURCES_JS_DIR"
  fi
}

restart_jetty() {
  local jetty_dir="$INSTALL_DIR/jetty"
  local stop_script="$jetty_dir/StopJetty.sh"
  local start_script="$jetty_dir/StartJetty.sh"
  local server_log="$JETTY_BASE_DIR/logs/server.log"

  if [[ ! -x "$start_script" ]]; then
    chmod +x "$start_script" 2>/dev/null || true
  fi
  if [[ -f "$stop_script" && ! -x "$stop_script" ]]; then
    chmod +x "$stop_script" 2>/dev/null || true
  fi

  if [[ -f "$stop_script" ]]; then
    echo "Stopping Jetty..."
    (cd "$jetty_dir" && ./StopJetty.sh) || true
    sleep 2
  fi

  if [[ -f "$server_log" ]]; then
    rm -f "$server_log"
    echo "Removed log file: $server_log"
  fi

  echo "Starting Jetty..."
  (cd "$jetty_dir" && ./StartJetty.sh)
}

echo "Project root : $PROJECT_ROOT"
echo "Install dir  : $INSTALL_DIR"
echo "Modules      : $MODULES_CSV"
echo "Skip build   : $SKIP_BUILD"
echo "Run tests    : $RUN_TESTS"
echo ""

for module in "${MODULES[@]}"; do
  case "$module" in
    system)
      deploy_jar_module "system" "perc-system"
      ;;
    rest)
      deploy_jar_module "rest" "rest"
      ;;
    sitemanage)
      deploy_jar_module "projects/sitemanage" "sitemanage"
      ;;
    webui)
      deploy_webui
      ;;
  esac
done

if [[ $RESTART -eq 1 ]]; then
  restart_jetty
else
  echo "Deploy complete. Restart Jetty to load updated classes/resources."
fi
