#!/bin/bash
# GH-1989: exercise CMS or DTS Linux service install/uninstall inside a
# user+mount namespace. Does not register units on the host systemd.
#
# Usage:
#   linux-service-namespace-soak.sh cms <installRoot> <evidenceDir>
#   linux-service-namespace-soak.sh dts <installRoot> <evidenceDir>
#
# Linux only. Windows: linux-service-namespace-soak.bat (documents N/A).
# Refuses host uid 0 so a mistaken sudo cannot write the real /etc.

set -u

if [ "${PERCUSSION_SOAK_NS:-}" != "1" ]; then
    if [ "$(id -u)" = "0" ]; then
        echo "Refusing soak as host root (would touch real /etc)." >&2
        exit 2
    fi
    if ! command -v unshare >/dev/null 2>&1; then
        echo "unshare is required for the Linux service namespace soak." >&2
        exit 2
    fi
    exec unshare --user --map-root-user --mount env PERCUSSION_SOAK_NS=1 "$0" "$@"
fi

# Host root can set PERCUSSION_SOAK_NS=1 and skip the outer refuse. A full host
# uid map (0 0 4294967295) means mounts hit the caller's real /etc. unshare
# --map-root-user writes a one-uid map (0 <hostuid> 1) before this check.
uid_map="$(awk 'NR==1 { print $1, $2, $3 }' /proc/self/uid_map 2>/dev/null || true)"
if [ "$uid_map" = "0 0 4294967295" ]; then
    echo "Refusing soak: host uid map would mount over the real /etc." >&2
    exit 2
fi
if [ "$(id -u)" != "0" ]; then
    echo "Namespace did not map uid 0; aborting before any /etc write." >&2
    exit 2
fi

MODE="${1:-}"
INSTALL_ROOT="${2:-}"
EVIDENCE="${3:-}"
if [ "$MODE" != "cms" ] && [ "$MODE" != "dts" ]; then
    echo "Usage: $0 cms|dts <installRoot> <evidenceDir>" >&2
    exit 2
fi
if [ ! -d "$INSTALL_ROOT" ] || [ ! -d "$EVIDENCE" ]; then
    echo "install root and evidence dir must exist" >&2
    exit 2
fi

fail() {
    echo "SOAK FAIL: $*" >&2
    exit 1
}

STAGE="$(mktemp -d)"
cp /etc/passwd /etc/group "$STAGE/" 2>/dev/null || true
[ -f /etc/nsswitch.conf ] && cp /etc/nsswitch.conf "$STAGE/" || true

mount -t tmpfs tmpfs /etc || fail "tmpfs mount on /etc failed"
mount -t tmpfs tmpfs /run || fail "tmpfs mount on /run failed"
mount -t tmpfs tmpfs /var || fail "tmpfs mount on /var failed"

[ -f "$STAGE/passwd" ] && cp "$STAGE/passwd" /etc/passwd
[ -f "$STAGE/group" ] && cp "$STAGE/group" /etc/group
[ -f "$STAGE/nsswitch.conf" ] && cp "$STAGE/nsswitch.conf" /etc/nsswitch.conf
mkdir -p /etc/init.d /etc/default /etc/systemd/system/multi-user.target.wants \
    /etc/rc2.d /etc/rc0.d /run/systemd/system /var/run
rm -rf "$STAGE"

BIN="$(mktemp -d)"
SYSLOG="$EVIDENCE/systemctl.log"
CHKLOG="$EVIDENCE/chkconfig.log"
: > "$SYSLOG"
: > "$CHKLOG"

cat > "$BIN/systemctl" <<'EOF'
#!/bin/bash
echo "$*" >> "${SOAK_SYSLOG:?}"
cmd="$1"
shift || true
case "$cmd" in
    daemon-reload|reset-failed|start|stop|status) exit 0 ;;
    is-active) exit 3 ;;
    enable)
        unit="$1"
        base="${unit%.service}"
        mkdir -p /etc/systemd/system/multi-user.target.wants
        ln -sfn "../${base}.service" "/etc/systemd/system/multi-user.target.wants/${base}.service"
        exit 0
        ;;
    disable)
        # disable --now NAME.service
        unit=""
        for arg in "$@"; do
            case "$arg" in
                *.service) unit="$arg" ;;
            esac
        done
        base="${unit%.service}"
        rm -f "/etc/systemd/system/multi-user.target.wants/${base}.service"
        exit 0
        ;;
    list-unit-files)
        unit="$1"
        if [ -f "/etc/systemd/system/${unit}" ]; then
            echo "${unit} enabled"
        fi
        exit 0
        ;;
    *) exit 0 ;;
esac
EOF

cat > "$BIN/chkconfig" <<'EOF'
#!/bin/bash
echo "$*" >> "${SOAK_CHKLOG:?}"
exit 0
EOF

cat > "$BIN/service" <<'EOF'
#!/bin/bash
echo "not running"
exit 3
EOF

chmod 755 "$BIN/systemctl" "$BIN/chkconfig" "$BIN/service"
export SOAK_SYSLOG="$SYSLOG" SOAK_CHKLOG="$CHKLOG"
export PATH="$BIN:$PATH"

# Exit 0 when any registration path remains (caller treats that as failure).
leftovers() {
    local name="$1"
    local found=1
    for p in \
        "/etc/init.d/${name}" \
        "/etc/default/${name}" \
        "/etc/systemd/system/${name}.service" \
        "/etc/systemd/system/multi-user.target.wants/${name}.service" \
        "/etc/rc2.d/S99${name}" \
        "/etc/rc0.d/K99${name}"
    do
        if [ -e "$p" ] || [ -L "$p" ]; then
            echo "$p"
            found=0
        fi
    done
    return "$found"
}

run_cms() {
    local script="${INSTALL_ROOT}/jetty/service/install-jetty-service.sh"
    [ -f "$script" ] || fail "missing $script"
    local name=PercussionCMS

    : > "$SYSLOG"
    : > "$CHKLOG"
    printf 'root\n' | bash "$script" "$name" install || fail "cms systemd install"
    [ -f "/etc/systemd/system/${name}.service" ] || fail "cms unit missing"
    grep -q 'TimeoutStartSec=1800' "/etc/systemd/system/${name}.service" || fail "cms TimeoutStartSec"
    grep -q 'StandardOutput=journal' "/etc/systemd/system/${name}.service" || fail "cms journal stdout"
    [ -L "/etc/systemd/system/multi-user.target.wants/${name}.service" ] || fail "cms not enabled"
    grep -q 'daemon-reload' "$SYSLOG" || fail "cms daemon-reload"
    grep -q "enable ${name}.service" "$SYSLOG" || fail "cms enable"
    if grep -Eq '(^| )start( |$)' "$SYSLOG"; then
        fail "cms install must not systemctl start (timeout is a runtime property)"
    fi
    if grep -q "${name} on" "$CHKLOG"; then
        fail "cms systemd path dual-registered via chkconfig"
    fi
    cp "/etc/systemd/system/${name}.service" "$EVIDENCE/cms-systemd.unit"

    bash "$script" "$name" uninstall || fail "cms systemd uninstall"
    if leftovers "$name"; then
        fail "cms systemd uninstall leftovers"
    fi

    : > "$SYSLOG"
    : > "$CHKLOG"
    printf 'root\n' | bash "$script" "$name" install --initd || fail "cms --initd install"
    [ ! -f "/etc/systemd/system/${name}.service" ] || fail "cms --initd wrote a unit"
    [ -f "/etc/init.d/${name}" ] || fail "cms --initd missing helper"
    grep -q "${name} on" "$CHKLOG" || fail "cms --initd did not chkconfig on"
    if grep -q 'enable ' "$SYSLOG"; then
        fail "cms --initd enabled a systemd unit"
    fi
    cp "$CHKLOG" "$EVIDENCE/cms-initd-chkconfig.log"

    bash "$script" "$name" uninstall || fail "cms --initd uninstall"
    if leftovers "$name"; then
        fail "cms initd uninstall leftovers"
    fi

    : > "$SYSLOG"
    : > "$CHKLOG"
    printf 'root\n' | bash "$script" "$name" install || fail "cms migration reinstall"
    [ -f "/etc/systemd/system/${name}.service" ] || fail "cms migration unit missing"
    grep -q 'TimeoutStartSec=1800' "/etc/systemd/system/${name}.service" || fail "cms migration timeout"
    bash "$script" "$name" uninstall || fail "cms migration uninstall"
    if leftovers "$name"; then
        fail "cms migration leftovers"
    fi
    echo cms-ok > "$EVIDENCE/result.txt"
}

run_dts() {
    local script="${INSTALL_ROOT}/Deployment/Server/DTSProductionService.sh"
    [ -f "$script" ] || fail "missing $script"
    local name=PercussionProductionDTS

    : > "$SYSLOG"
    : > "$CHKLOG"
    bash "$script" "$name" install || fail "dts systemd install"
    [ -f "/etc/systemd/system/${name}.service" ] || fail "dts unit missing"
    grep -q 'TimeoutStartSec=1800' "/etc/systemd/system/${name}.service" || fail "dts TimeoutStartSec"
    grep -q 'StandardOutput=journal' "/etc/systemd/system/${name}.service" || fail "dts journal stdout"
    [ -L "/etc/systemd/system/multi-user.target.wants/${name}.service" ] || fail "dts not enabled"
    if grep -q "${name} on" "$CHKLOG"; then
        fail "dts systemd path dual-registered via chkconfig"
    fi
    if grep -Eq '(^| )start( |$)' "$SYSLOG"; then
        fail "dts install must not systemctl start"
    fi
    cp "/etc/systemd/system/${name}.service" "$EVIDENCE/dts-systemd.unit"

    bash "$script" "$name" uninstall || fail "dts systemd uninstall"
    if leftovers "$name"; then
        fail "dts systemd uninstall leftovers"
    fi

    : > "$SYSLOG"
    : > "$CHKLOG"
    bash "$script" "$name" install --initd || fail "dts --initd install"
    [ ! -f "/etc/systemd/system/${name}.service" ] || fail "dts --initd wrote a unit"
    [ -f "/etc/init.d/${name}" ] || fail "dts --initd missing helper"
    grep -q "${name} on" "$CHKLOG" || fail "dts --initd did not chkconfig on"
    cp "$CHKLOG" "$EVIDENCE/dts-initd-chkconfig.log"

    bash "$script" "$name" uninstall || fail "dts --initd uninstall"
    if leftovers "$name"; then
        fail "dts initd uninstall leftovers"
    fi

    bash "$script" "$name" install || fail "dts migration reinstall"
    [ -f "/etc/systemd/system/${name}.service" ] || fail "dts migration unit missing"
    bash "$script" "$name" uninstall || fail "dts migration uninstall"
    if leftovers "$name"; then
        fail "dts migration leftovers"
    fi
    echo dts-ok > "$EVIDENCE/result.txt"
}

case "$MODE" in
    cms) run_cms ;;
    dts) run_dts ;;
esac
echo "done ${MODE}"
