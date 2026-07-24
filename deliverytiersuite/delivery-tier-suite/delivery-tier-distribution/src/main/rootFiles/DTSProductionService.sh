#!/bin/bash
# Install or uninstall Percussion Production DTS as a Linux service.
# GH-962: prefer native systemd; keep init.d as fallback.
#
# Usage:
#   DTSProductionService.sh [ServiceName] install [--systemd|--initd]
#   DTSProductionService.sh [ServiceName] uninstall

echo "Script To Install Production DTS Linux Service."

SERVICE_NAME=PercussionProductionDTS
FORCE_SYSTEMD=false
FORCE_INITD=false
VARIANT=production
PID_BASENAME=PercussionProductionDTS.pid
RUN_PARENT=/var/run/PercussionProductionService
CATALINA_MARKER=catalina

if [ "$(id -u)" != "0" ]; then
	echo "This script must be run with sudo or as root" 1>&2
	exit 1
fi

function usage() {
	echo "Usage: $0 [ Service name default : PercussionProductionDTS ] {install | uninstall} [--systemd | --initd]"
	exit 1
}

if [ $# -lt 1 ]; then
	usage
fi

if [ "$1" != "install" ] && [ "$1" != "uninstall" ]; then
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
else
	usage
fi

if [ "$FORCE_SYSTEMD" = "true" ] && [ "$FORCE_INITD" = "true" ]; then
	echo "Cannot combine --systemd and --initd" 1>&2
	exit 1
fi

function validate_service_name() {
	case "$1" in
		'' | *[!A-Za-z0-9_-]* | -* | _*)
			echo "Invalid service name '$1' (use letters, digits, underscore, hyphen; must start with alnum)" 1>&2
			exit 1
			;;
	esac
}

validate_service_name "$SERVICE_NAME"

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
		printf '%s\n' "$line"
	done < "$template" > "$dest"
}

function is_systemd_available() {
	[ -d /run/systemd/system ] && command -v systemctl >/dev/null 2>&1
}

function use_systemd_install() {
	if [ "$FORCE_INITD" = "true" ]; then
		return 1
	fi
	if [ "$FORCE_SYSTEMD" = "true" ]; then
		if ! is_systemd_available; then
			echo "systemd required (--systemd) but not available" 1>&2
			exit 1
		fi
		return 0
	fi
	is_systemd_available
}

function abspath() {
	if [ -d "$1" ]; then
		(cd "$1" && pwd)
	elif [ -f "$1" ]; then
		if [[ $1 = /* ]]; then
			echo "$1"
		elif [[ $1 == */* ]]; then
			echo "$(cd "${1%/*}" && pwd)/${1##*/}"
		else
			echo "$(pwd)/$1"
		fi
	fi
}

function checkForDTSService() {
	currentService=
	if ! compgen -G "/etc/default/*" >/dev/null 2>&1; then
		return 0
	fi
	while read -r line; do
		service=$(basename "${line}")
		echo "Found DTS service $service in $line"
		serviceHome=$(bash -c "source ${line} >/dev/null 2>&1 ; echo \${CATALINA_HOME}")
		if [ "$serviceHome" == "$CATALINA_HOME" ]; then
			currentService=${service}
		fi
	done < <(grep -l /etc/default/* -e 'CATALINA_HOME' 2>/dev/null || true)
}

function removeServiceFromStartup() {
	echo "Uninstalling SysV service $1 from startup"
	if command -v chkconfig >/dev/null 2>&1; then
		chkconfig "${1}" off || true
	elif command -v update-rc.d >/dev/null 2>&1; then
		update-rc.d -f "${1}" remove || true
	fi
	if [ -d "/etc/rc.d/rc2.d" ]; then
		rm -f /etc/rc.d/rc?.d/S??"${1}" /etc/rc.d/rc?.d/K??"${1}"
	fi
	if [ -d "/etc/rc2.d" ]; then
		rm -f /etc/rc?.d/S??"${1}" /etc/rc?.d/K??"${1}"
	fi
}

function removeSystemdUnit() {
	local name="$1"
	local unit="/etc/systemd/system/${name}.service"
	if [ -f "$unit" ]; then
		echo "Removing systemd unit ${name}.service"
		systemctl disable --now "${name}.service" 2>/dev/null || true
		rm -f "$unit"
		systemctl daemon-reload || true
		systemctl reset-failed "${name}.service" 2>/dev/null || true
	fi
}

function removeServiceScript() {
	echo "Removing files for ${SERVICE_NAME}"
	rm -f "/etc/init.d/${SERVICE_NAME}"
	rm -f "/etc/default/${SERVICE_NAME}"
	rm -f "/etc/systemd/system/${SERVICE_NAME}.service"
	rm -rf "${TOMCAT_RUN}"
}

function installInitScriptAndDefaults() {
	echo "Setting up pid folder ${TOMCAT_RUN} user=${RX_USER} group=${RX_GROUP}"
	mkdir -p "${TOMCAT_RUN}"
	chown -R "${RX_USER}:${RX_GROUP}" "${TOMCAT_RUN}"

	if [ ! -f "${CATALINA_HOME}/bin/catalina.sh" ]; then
		echo "Missing ${CATALINA_HOME}/bin/catalina.sh" 1>&2
		exit 1
	fi

	echo "Installing start helper to /etc/init.d/${SERVICE_NAME}"
	# Fresh file each install (avoid append corruption from re-runs)
	sed -e "s/\${PercussionProductionDTS_service}/${SERVICE_NAME}/" \
		"${CATALINA_HOME}/bin/catalina.sh" > "/etc/init.d/${SERVICE_NAME}"
	# Ensure env is set early in the helper script
	sed -i "2 a CATALINA_HOME=${CATALINA_HOME}" "/etc/init.d/${SERVICE_NAME}"
	sed -i "3 a JAVA_HOME=${JAVA_HOME}" "/etc/init.d/${SERVICE_NAME}"
	chmod 755 "/etc/init.d/${SERVICE_NAME}"

	# EnvironmentFile-safe defaults (no shell commands)
	cat > "/etc/default/${SERVICE_NAME}" <<EOF
JAVA_HOME=${JAVA_HOME}
JRE_HOME=${JAVA_HOME}
CATALINA_HOME=${CATALINA_HOME}
CATALINA_BASE=${CATALINA_HOME}
CATALINA_OUT=${CATALINA_HOME}/logs/catalina.log
CATALINA_PID=${TOMCAT_RUN}/${PID_BASENAME}
EOF

	echo "Wrote /etc/default/${SERVICE_NAME}"
	cat "/etc/default/${SERVICE_NAME}"
}

function installSystemdUnit() {
	local unit_template
	unit_template="$(dirname "$(abspath "$0")")/dts-tomcat.service.in"
	if [ ! -f "$unit_template" ]; then
		echo "Missing systemd unit template: $unit_template" 1>&2
		exit 1
	fi
	local unit_path="/etc/systemd/system/${SERVICE_NAME}.service"
	local pid_file="${TOMCAT_RUN}/${PID_BASENAME}"
	local env_file="/etc/default/${SERVICE_NAME}"
	local init_script="/etc/init.d/${SERVICE_NAME}"
	local description="Percussion Production DTS Tomcat (${SERVICE_NAME})"

	echo "Installing systemd unit ${unit_path}"
	substitute_unit_template "$unit_template" "$unit_path" \
		"$description" "$pid_file" "$env_file" "$init_script"
	chmod 644 "$unit_path"
	systemctl daemon-reload
	systemctl enable "${SERVICE_NAME}.service"
	echo "Enabled ${SERVICE_NAME}.service (not started). Start with: systemctl start ${SERVICE_NAME}"
}

function enableSysV() {
	if command -v chkconfig >/dev/null 2>&1; then
		chkconfig "${SERVICE_NAME}" off >/dev/null 2>&1 || true
		chkconfig "${SERVICE_NAME}" on
	elif command -v update-rc.d >/dev/null 2>&1; then
		update-rc.d "${SERVICE_NAME}" defaults
	elif [ -d "/etc/rc2.d" ]; then
		ln "/etc/init.d/${SERVICE_NAME}" "/etc/rc2.d/S99${SERVICE_NAME}"
		ln "/etc/init.d/${SERVICE_NAME}" "/etc/rc0.d/K99${SERVICE_NAME}"
	else
		echo "Cannot register SysV service for boot; start manually via /etc/init.d/${SERVICE_NAME}"
	fi
}

distVersion=$(cat /proc/version 2>&1 || true)
# Service script is installed at <InstallRoot>/Deployment/Server/ (see installDts.xml).
CATALINA_HOME=$(dirname "$(abspath "$0")")
EXECUTABLE="${CATALINA_HOME}/bin/catalina.sh"
# Product surface root: two levels up from Server. Holds resolve-java-home.sh and
# (after install-time selection) java.properties — not a mandatory JRE folder.
INSTALL_ROOT="$(cd "${CATALINA_HOME}/../.." && pwd)"
# Deployment parent for chown only (do not chown the whole CMS tree when co-located).
rxDir="$(cd "${CATALINA_HOME}/.." && pwd)"
RX_USER=$(ls -ld "${INSTALL_ROOT}" | awk '{print $3}')
RX_GROUP=$(ls -ld "${INSTALL_ROOT}" | awk '{print $4}')
TOMCAT_RUN=${RUN_PARENT}/${SERVICE_NAME}

if command -v service >/dev/null 2>&1; then
	serviceCmd="service ${SERVICE_NAME}"
else
	serviceCmd="/etc/init.d/${SERVICE_NAME}"
fi

if [ "$uninstall" != "true" ]; then
	if [ -f "/etc/init.d/${SERVICE_NAME}" ] || [ -f "/etc/systemd/system/${SERVICE_NAME}.service" ]; then
		echo "Service ${SERVICE_NAME} already installed. Uninstall first."
		exit 1
	fi

	checkForDTSService
	if [ -n "${currentService}" ]; then
		echo "A service at /etc/default/${currentService} already starts this CATALINA_HOME. Remove it first."
		exit 1
	fi

	echo "CATALINA_HOME=${CATALINA_HOME}"
	echo "INSTALL_ROOT=${INSTALL_ROOT}"
	echo "rxDir=${rxDir}"

	# GH-991 / issue #1340: operators pick Java at install time; the preinstall
	# writes <INSTALL_ROOT>/java.properties. Runtime and service install MUST
	# use resolve-java-home (java.properties > env > optional legacy JRE/JRE64 >
	# PATH > fail). Do NOT require <INSTALL_ROOT>/JRE and do NOT soft-fail into
	# an unvalidated JRE path. See specs/991-system-java-home/contracts/.
	RESOLVER="${INSTALL_ROOT}/resolve-java-home.sh"
	if [ ! -f "$RESOLVER" ] || [ ! -r "$RESOLVER" ]; then
		echo "Missing ${RESOLVER}. Re-run the DTS installer or copy resolve-java-home.sh next to TomcatStartup.sh." 1>&2
		exit 1
	fi
	# Source into this shell (NOT a subshell) so JAVA_HOME / JAVA / RESOLVE_SOURCE
	# propagate. Hard-fail on resolve failure — the helper already prints the
	# sources tried and required major 21+.
	# shellcheck disable=SC1090
	source "$RESOLVER" "${INSTALL_ROOT}" || exit 1
	if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/java" ]; then
		echo "resolve-java-home did not set a usable JAVA_HOME; check ${INSTALL_ROOT}/java.properties or JAVA_HOME." 1>&2
		exit 1
	fi
	echo "Service Java home resolved via ${RESOLVE_SOURCE:-unknown}: ${JAVA_HOME}"

	echo "Ensuring permissions on ${rxDir} user=${RX_USER} group=${RX_GROUP}"
	chown -R "${RX_USER}:${RX_GROUP}" "${rxDir}"

	installInitScriptAndDefaults

	if use_systemd_install; then
		echo "Detected systemd — installing native unit (SysV boot registration skipped)"
		installSystemdUnit
		echo "********"
		echo "  Start:  systemctl start ${SERVICE_NAME}"
		echo "  Stop:   systemctl stop ${SERVICE_NAME}"
		echo "  Status: systemctl status ${SERVICE_NAME}"
		echo "  Logs:   journalctl -u ${SERVICE_NAME} -n 100 --no-pager"
		echo "  Also:   ${CATALINA_HOME}/logs/"
		echo "  TimeoutStartSec=1800 — override: systemctl edit ${SERVICE_NAME}"
		echo "********"
	else
		echo "Using classic init.d registration"
		enableSysV
		echo "********"
		echo "  Start: ${serviceCmd} start"
		echo "  Stop:  ${serviceCmd} stop"
		echo "********"
	fi
else
	echo "Uninstalling ${SERVICE_NAME}"
	had_systemd=false
	had_initd=false

	if [ -f "/etc/systemd/system/${SERVICE_NAME}.service" ]; then
		had_systemd=true
		if systemctl is-active --quiet "${SERVICE_NAME}.service" 2>/dev/null; then
			systemctl stop "${SERVICE_NAME}.service" || true
		fi
		removeSystemdUnit "${SERVICE_NAME}"
	fi

	if [ -f "/etc/init.d/${SERVICE_NAME}" ]; then
		had_initd=true
		if ! grep -q "${CATALINA_MARKER}" "/etc/init.d/${SERVICE_NAME}"; then
			echo "Service at /etc/init.d/${SERVICE_NAME} is not a Percussion Production DTS service"
			exit 1
		fi
		if ${serviceCmd} status 2>/dev/null | grep -qi running; then
			${serviceCmd} stop || true
		fi
		removeServiceFromStartup "${SERVICE_NAME}"
	fi

	if [ "$had_systemd" != "true" ] && [ "$had_initd" != "true" ]; then
		echo "Service ${SERVICE_NAME} not installed"
		exit 1
	fi

	removeServiceScript
	systemctl daemon-reload 2>/dev/null || true
fi

echo "Done"
