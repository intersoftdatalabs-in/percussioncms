#!/bin/bash
# Install or uninstall Percussion CMS Jetty as a Linux service.
# GH-962: prefer native systemd unit; keep init.d as fallback.
#
# Usage:
#   install-jetty-service.sh [ServiceName] install [--systemd|--initd]
#   install-jetty-service.sh [ServiceName] uninstall
#   install-jetty-service.sh [ServiceName] cleanupJBoss

SERVICE_NAME=PercussionCMS
FORCE_SYSTEMD=false
FORCE_INITD=false

if [ "$(id -u)" != "0" ]; then
    echo "This script must be run with sudo or as root" 1>&2
    exit 1
fi

function usage() {
    echo "Usage: $0 [ service name default : PercussionCMS ] {install | uninstall | cleanupJBoss } [--systemd | --initd]"
    echo "  --systemd  Require systemd (fail if not available)"
    echo "  --initd    Force classic SysV/init.d registration (no native unit)"
    exit 1
}

# Parse: optional service name, then action, then optional flags
if [ $# -lt 1 ]; then
    usage
fi

if [ "$1" != "install" ] && [ "$1" != "uninstall" ] && [ "$1" != "cleanupJBoss" ]; then
    SERVICE_NAME="$1"
    shift
fi

ACTION="$1"
shift || true

while [ $# -gt 0 ]; do
    case "$1" in
        --systemd) FORCE_SYSTEMD=true ;;
        --initd) FORCE_INITD=true ;;
        *) usage ;;
    esac
    shift
done

if [ "$ACTION" == "uninstall" ]; then
    uninstall=true
elif [ "$ACTION" == "install" ]; then
    uninstall=false
elif [ "$ACTION" == "cleanupJBoss" ]; then
    cleanupJBoss=true
    uninstall=false
else
    usage
fi

if [ "$FORCE_SYSTEMD" = "true" ] && [ "$FORCE_INITD" = "true" ]; then
    echo "Cannot combine --systemd and --initd" 1>&2
    exit 1
fi

# Service names are substituted into unit files; restrict to safe identifier chars.
function validate_service_name() {
    case "$1" in
        '' | *[!A-Za-z0-9_-]* | -* | _*)
            echo "Invalid service name '$1' (use letters, digits, underscore, hyphen; must start with alnum)" 1>&2
            exit 1
            ;;
    esac
}

validate_service_name "$SERVICE_NAME"

# Replace @PLACEHOLDER@ tokens without sed metacharacter issues in values.
# Args: template dest description pid_file env_file init_script
function substitute_unit_template() {
    local template="$1"
    local dest="$2"
    local _description="$3"
    local _pid_file="$4"
    local _env_file="$5"
    local _init_script="$6"
    local line
    while IFS= read -r line || [ -n "$line" ]; do
        line=${line//@SERVICE_NAME@/${SERVICE_NAME}}
        line=${line//@DESCRIPTION@/${_description}}
        line=${line//@PID_FILE@/${_pid_file}}
        line=${line//@ENV_FILE@/${_env_file}}
        line=${line//@INIT_SCRIPT@/${_init_script}}
        line=${line//@JETTY_ROOT@/${JETTY_ROOT}}
        printf '%s\n' "$line"
    done < "$template" > "$dest"
}

function is_systemd_available() {
    if [ ! -d /run/systemd/system ]; then
        return 1
    fi
    if ! command -v systemctl >/dev/null 2>&1; then
        return 1
    fi
    return 0
}

function use_systemd_install() {
    if [ "$FORCE_INITD" = "true" ]; then
        return 1
    fi
    if [ "$FORCE_SYSTEMD" = "true" ]; then
        if ! is_systemd_available; then
            echo "systemd required (--systemd) but not available on this host" 1>&2
            exit 1
        fi
        return 0
    fi
    is_systemd_available
}

function checkForJettyService() {
    currentService=
    if compgen -G "/etc/default/*" > /dev/null 2>&1; then
        while read -r line; do
            service=$(basename "${line}")
            echo "Found Jetty service $service in $line"
            serviceHome=$(bash -c "source ${line} >/dev/null 2>&1 ; echo \${JETTY_BASE}")
            if [ "$serviceHome" == "$JETTY_BASE" ]; then
                currentService=$service
            fi
        done < <(grep -l /etc/default/* -e 'JETTY_BASE' 2>/dev/null || true)
    fi
}

function checkForJbossService() {
    currentService=
    if ! compgen -G "/etc/init.d/*" > /dev/null 2>&1; then
        return 0
    fi
    while read -r line; do
        service=$(basename "${line}")
        echo "Found JBoss service $service in $line"
        serviceHome=$(grep "^SERVER_DIR=" /etc/init.d/${service} | cut -d "=" -f 2)
        echo "$serviceHome"
        if [ "$serviceHome" == "$rxDir" ]; then
            currentService=$service
        fi
    done < <(grep -l /etc/init.d/* -e 'RhythmyxD' 2>/dev/null || true)

    if [ ! -z "$currentService" ]; then
        if [ "$cleanupJBoss" != "true" ]; then
            echo "Warning Jboss startup /etc/init.d/${currentService} for this instance ${rxDir} exists use cleanupJBoss to remove"
            usage
        fi
        echo "Cleaning up JBoss init scripts"
        removeServiceFromStartup "$currentService"
        removeServiceScript "$currentService"
        exit 0
    fi
}

function removeServiceFromStartup() {
    echo "uninstalling SysV service $1 from startup"
    if command -v chkconfig >/dev/null 2>&1; then
        echo "Using command 'chkconfig ${1} off'"
        chkconfig "${1}" off || true
    elif command -v update-rc.d >/dev/null 2>&1; then
        echo "using command 'update-rc.d -f ${1} remove'"
        update-rc.d -f "${1}" remove || true
    else
        echo "Cannot find chkconfig or update-rc.d; removing rc?.d links if present"
    fi

    if [ -d "/etc/rc.d/rc2.d" ]; then
        rm -f /etc/rc.d/rc?.d/S??"${1}"
        rm -f /etc/rc.d/rc?.d/K??"${1}"
    fi
    if [ -d "/etc/rc2.d" ]; then
        rm -f /etc/rc?.d/S??"${1}"
        rm -f /etc/rc?.d/K??"${1}"
    fi
}

function removeSystemdUnit() {
    local name="$1"
    local unit="/etc/systemd/system/${name}.service"
    if [ -f "$unit" ] || systemctl list-unit-files "${name}.service" 2>/dev/null | grep -q "${name}.service"; then
        echo "Removing systemd unit ${name}.service"
        systemctl disable --now "${name}.service" 2>/dev/null || true
        rm -f "$unit"
        systemctl daemon-reload || true
        systemctl reset-failed "${name}.service" 2>/dev/null || true
    fi
}

function removeServiceScript() {
    echo "removing service files for $1"
    rm -f "/etc/init.d/${1}"
    rm -f "/etc/default/${1}"
    rm -f "/etc/systemd/system/${1}.service"
    rm -rf "$JETTY_RUN"
    rm -f "$JETTY_BASE/${SERVICE_NAME}.state"
}

function installInitScriptAndDefaults() {
    echo "setting up pid folder ${JETTY_RUN} ownership user=${RX_USER} group=${RX_GROUP}"
    mkdir -p "${JETTY_RUN}"
    mkdir -p "/var/run/rxjetty/${SERVICE_NAME}"
    chown -R "${RX_USER}:${RX_GROUP}" "/var/run/rxjetty/${SERVICE_NAME}"
    chown -R "${RX_USER}:${RX_GROUP}" "${JETTY_RUN}"
    chmod -R ugo+rw "/var/run/rxjetty/${SERVICE_NAME}"

    local template="${JETTY_DEFAULTS}/bin/rxjetty.sh"
    if [ ! -f "$template" ]; then
        echo "Missing Jetty service template: $template" 1>&2
        exit 1
    fi
    echo "Installing start helper to /etc/init.d/${SERVICE_NAME}"
    sed -e "s/\${rxjetty_service}/$SERVICE_NAME/" "$template" > "/etc/init.d/${SERVICE_NAME}"
    chmod 755 "/etc/init.d/${SERVICE_NAME}"

    # /etc/default must be shell-sourceable (no shell commands mixed into env file)
    cat > "/etc/default/${SERVICE_NAME}" <<EOF
JAVA_HOME=${JAVA_HOME}
JAVA=${JAVA_HOME}/bin/java
JETTY_HOME=${JETTY_HOME}
JETTY_BASE=${JETTY_BASE}
JETTY_DEFAULTS=${JETTY_DEFAULTS}
JETTY_CONF=${JETTY_CONF}
JETTY_START_LOG=${JETTY_BASE}/logs/start.log
JAVA_OPTIONS="-XX:+DisableAttachMechanism -Drxdeploydir=${rxDir} -Djetty_perc_defaults=${JETTY_DEFAULTS}"
JETTY_RUN=${JETTY_RUN}
JETTY_PID=${JETTY_RUN}/rxjetty.pid
JETTY_ARGS="--include-jetty-dir=${JETTY_DEFAULTS}"
JETTY_USER=${RX_USER}
EOF

    echo "Wrote /etc/default/${SERVICE_NAME}"
    cat "/etc/default/${SERVICE_NAME}"
}

function installSystemdUnit() {
    local unit_template
    unit_template="$(dirname "$(readlink -f "$0")")/percussion-cms.service.in"
    if [ ! -f "$unit_template" ]; then
        echo "Missing systemd unit template: $unit_template" 1>&2
        exit 1
    fi
    local unit_path="/etc/systemd/system/${SERVICE_NAME}.service"
    local pid_file="${JETTY_RUN}/rxjetty.pid"
    local env_file="/etc/default/${SERVICE_NAME}"
    local init_script="/etc/init.d/${SERVICE_NAME}"
    local description="Percussion CMS Jetty (${SERVICE_NAME})"

    echo "Installing systemd unit ${unit_path}"
    substitute_unit_template "$unit_template" "$unit_path" \
        "$description" "$pid_file" "$env_file" "$init_script"
    chmod 644 "$unit_path"

    systemctl daemon-reload
    systemctl enable "${SERVICE_NAME}.service"
    echo "Enabled ${SERVICE_NAME}.service (not started). Start with: systemctl start ${SERVICE_NAME}"
}

function enableSysV() {
    echo "Registering SysV/init.d service for boot"
    if command -v chkconfig >/dev/null 2>&1; then
        echo "Using 'chkconfig ${SERVICE_NAME} on'"
        chkconfig "${SERVICE_NAME}" off > /dev/null 2>&1 || true
        chkconfig "${SERVICE_NAME}" on
    elif command -v update-rc.d >/dev/null 2>&1; then
        echo "using 'update-rc.d ${SERVICE_NAME} defaults'"
        update-rc.d "${SERVICE_NAME}" defaults
    elif [ -d "/etc/rc2.d" ]; then
        echo "Fall back to symbolic linking into /etc/rcx.d folders"
        ln "/etc/init.d/${SERVICE_NAME}" "/etc/rc2.d/S99${SERVICE_NAME}"
        ln "/etc/init.d/${SERVICE_NAME}" "/etc/rc0.d/K99${SERVICE_NAME}"
    else
        echo "Cannot find chkconfig or update-rc.d or /etc/rc2.d; start manually via /etc/init.d/${SERVICE_NAME}"
        echo "${distVersion}"
    fi
}

distVersion=$(cat /proc/version 2>&1 || true)

JETTY_ROOT=$(dirname "$(dirname "$(readlink -f "$0")")")
JETTY_HOME=${JETTY_ROOT}/upstream
JETTY_BASE=${JETTY_ROOT}/base
JETTY_DEFAULTS=${JETTY_ROOT}/defaults
rxDir=$(dirname "${JETTY_ROOT}")
RX_USER=$(ls -ld "${rxDir}" | awk '{print $3}')
RX_GROUP=$(ls -ld "${rxDir}" | awk '{print $4}')
JETTY_RUN=/var/run/rxjetty/${SERVICE_NAME}

if command -v service >/dev/null 2>&1; then
    serviceCmd="service ${SERVICE_NAME}"
else
    serviceCmd="/etc/init.d/${SERVICE_NAME}"
fi

if [ "$uninstall" != "true" ]; then
    # ----- install -----
    if [ -f "/etc/init.d/${SERVICE_NAME}" ] || [ -f "/etc/systemd/system/${SERVICE_NAME}.service" ]; then
        echo "Service ${SERVICE_NAME} already installed (init.d and/or systemd unit present). Uninstall first."
        exit 1
    fi

    checkForJbossService
    checkForJettyService
    if [ ! -z "$currentService" ]; then
        echo "A service with configuration at /etc/default/${currentService} is already set up for this instance. Remove it first."
        exit 1
    fi

    echo "JETTY_ROOT=${JETTY_ROOT}"
    echo "rxDir=${rxDir}"

    # GH-991 / issue #1340: install-time selection wrote <installRoot>/java.properties.
    # Service install MUST use resolve-java-home (java.properties > env > optional
    # legacy JRE|JRE64 > PATH > fail, major 21+). Do NOT require <installRoot>/JRE
    # and do NOT soft-fail into an unvalidated JRE/JRE64 path.
    # See specs/991-system-java-home/contracts/java-home-resolution.md.
    RESOLVER="${JETTY_ROOT}/resolve-java-home.sh"
    if [ ! -f "$RESOLVER" ] || [ ! -r "$RESOLVER" ]; then
        echo "Missing ${RESOLVER}. Re-run the CMS installer or restore resolve-java-home.sh under jetty/." 1>&2
        exit 1
    fi
    # Source into this shell (NOT a subshell) so JAVA_HOME / JAVA / RESOLVE_SOURCE
    # propagate. Hard-fail on resolve failure — the helper prints sources tried.
    # shellcheck disable=SC1090
    source "$RESOLVER" "${rxDir}" || exit 1
    if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/java" ]; then
        echo "resolve-java-home did not set a usable JAVA_HOME; check ${rxDir}/java.properties or JAVA_HOME." 1>&2
        exit 1
    fi
    echo "Service Java home resolved via ${RESOLVE_SOURCE:-unknown}: ${JAVA_HOME}"

    if [ -f "${JETTY_BASE}/etc/jetty.conf" ]; then
        JETTY_CONF=${JETTY_BASE}/etc/jetty.conf
    else
        JETTY_CONF=${JETTY_DEFAULTS}/etc/jetty.conf
    fi

    echo "Please identify the user id that the percussion service should run as: <default: root>"
    read -r suppliedUser
    if [ -z "$suppliedUser" ]; then
        suppliedUser=root
    fi
    echo "Using user id: ${suppliedUser}"
    echo "Generating ${rxDir}/rx_user.id file"
    echo "SYSTEM_USER_ID=${suppliedUser}" > "${rxDir}/rx_user.id"
    RX_USER=${suppliedUser}
    RX_GROUP=${suppliedUser}
    echo "Ensuring permissions match top level ${rxDir} folder user=${RX_USER} group=${RX_GROUP}"
    chown -R "${RX_USER}:${RX_GROUP}" "${rxDir}"

    installInitScriptAndDefaults

    if [ -x "/etc/init.d/${SERVICE_NAME}" ]; then
        "/etc/init.d/${SERVICE_NAME}" check || true
    fi

    if use_systemd_install; then
        echo "Detected systemd — installing native unit (init.d helper kept for ExecStart; SysV boot registration skipped)"
        installSystemdUnit
        echo "********"
        echo "  Start:  systemctl start ${SERVICE_NAME}"
        echo "  Stop:   systemctl stop ${SERVICE_NAME}"
        echo "  Status: systemctl status ${SERVICE_NAME}"
        echo "  Logs:   journalctl -u ${SERVICE_NAME} -n 100 --no-pager"
        echo "  Also:   ${JETTY_BASE}/logs/ (application Log4j2)"
        echo "  TimeoutStartSec=1800 (30m) — override with: systemctl edit ${SERVICE_NAME}"
        echo "********"
    else
        echo "Using classic init.d registration"
        enableSysV
        echo "********"
        echo "  Start service with '${serviceCmd} start'"
        echo "  Stop service with '${serviceCmd} stop'"
        echo "  use '${serviceCmd}' without parameters to check other options"
        echo "********"
    fi

else
    # ----- uninstall -----
    echo "Checking for installed service ${SERVICE_NAME}"
    had_systemd=false
    had_initd=false

    if [ -f "/etc/systemd/system/${SERVICE_NAME}.service" ]; then
        had_systemd=true
        if systemctl is-active --quiet "${SERVICE_NAME}.service" 2>/dev/null; then
            echo "Stopping systemd unit ${SERVICE_NAME}"
            systemctl stop "${SERVICE_NAME}.service" || true
        fi
        removeSystemdUnit "${SERVICE_NAME}"
    fi

    if [ -f "/etc/init.d/${SERVICE_NAME}" ]; then
        had_initd=true
        if cat "/etc/init.d/${SERVICE_NAME}" | grep -q "jetty"; then
            echo "Found service installed to /etc/init.d/${SERVICE_NAME}"
        else
            echo "Service installed to /etc/init.d/${SERVICE_NAME} is not a Rhythmyx Jetty Service"
            exit 1
        fi
        if ${serviceCmd} status 2>/dev/null | grep -q "Jetty running"; then
            echo "Service still running, shutting down...."
            ${serviceCmd} stop || true
        fi
        removeServiceFromStartup "${SERVICE_NAME}"
    fi

    if [ "$had_systemd" != "true" ] && [ "$had_initd" != "true" ]; then
        echo "Service $SERVICE_NAME not installed"
        exit 1
    fi

    removeServiceScript "${SERVICE_NAME}"
    systemctl daemon-reload 2>/dev/null || true
fi

echo "done"
