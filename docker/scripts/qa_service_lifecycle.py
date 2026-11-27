#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""QA service-lifecycle harness (GH-7324): container install testing via systemd.

The QA matrix cell historically installed CMS/DTS and launched the product
directly (``StartJetty.sh`` / ``TomcatStartup.sh``). This harness instead:

1. Boots a systemd-as-PID-1 matrix cell (``docker/matrix/Dockerfile.systemd``,
   unprivileged with a private cgroup namespace and ``SYS_ADMIN`` —
   in-container systemd only, never the host's).
2. Runs the silent installer inside the cell via the normal install path
   (``cell-entrypoint.py --no-start``).
3. Installs the native service units via the shipped installer scripts
   (``install-jetty-service.sh`` / ``DTSProductionService.sh`` ``install
   --systemd`` — the dual-ship path from GH-962 / #1988).
4. Drives the test instance lifecycle with real ``systemctl
   start/stop/restart/status`` + ``journalctl -u`` assertions. After
   every start/restart the readiness probe pairs the serving check
   with the ``rhythmyx_ready`` server.log context-failure assessment
   (GH-7332: a 200 from the login JSP alone never passes).
5. Uninstalls the units and asserts no leftover units/links remain, then
   removes the cell container.

Host-systemd independence is structural: every service-manager invocation
is ``docker exec <cell> ...``. ``validate_container_name`` refuses any
target outside the ``perc-matrix-*-systemd`` test-cell namespace, so the
harness cannot touch host ``/etc`` no matter the flags passed. Hosts whose
default AppArmor profile denies the ENTRYPOINT cgroup remount fail closed
at ``wait-systemd``; such hosts may opt into the tailored profile at
``docker/matrix/apparmor/perc-systemd-cell`` via ``apparmor_profile``
(never ``unconfined``, never ``--privileged``).

Live run (foreground, in-cell evidence)::

    python docker/scripts/perc-devctl.py qa-service-lifecycle --product cms
"""

from __future__ import annotations

import subprocess
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, Dict, List, Optional, Sequence, Tuple

from rhythmyx_ready import (  # noqa: E402 — same scripts dir (perc-devctl sys.path)
    assess_rhythmyx_ready,
    container_server_log_path,
)

STEP_QA_SERVICE_LIFECYCLE = "qa-service-lifecycle"

SYSTEMD_IMAGE = "percussion-matrix-systemd-cell:local"
BASE_MATRIX_IMAGE = "percussion-matrix-cell:local"

PRODUCTS = ("cms", "dts")

CMS_UNIT = "PercussionCMS"
DTS_UNIT = "PercussionProductionDTS"

CMS_INSTALL_ROOT = "/opt/Percussion"
CMS_SERVICE_SCRIPT = "/opt/Percussion/jetty/service/install-jetty-service.sh"
DTS_SERVICE_SCRIPT = "/opt/Percussion/Deployment/Server/DTSProductionService.sh"

# mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #7324,
# same as #6570) — container-internal absolute Linux paths (the QA cell image
# is Linux-only); host-side joins use Path.resolve, never string concat.
CELL_ENTRYPOINT = "/usr/local/bin/cell-entrypoint.py"
CELL_INSTALLER_JAR = "/installer/installer.jar"

# In-container listen ports (same as matrix cells; no host publish needed —
# all assertions run in-container via docker exec).
CONTAINER_PORTS = {"cms": 9992, "dts": 9980}
# In-container readiness paths (mirror matrix-install-smoke probe paths).
READINESS_PATHS = {"cms": "/Rhythmyx/login", "dts": "/"}

# Bytes of the CMS server.log tail scanned for context-failure markers
# after each serving probe (GH-7332). Bounded so the evidence step stays
# small; a fresh start logs its Spring context outcome at the tail.
SERVER_LOG_TAIL_BYTES = 65536

# Host-side AppArmor profile name suggested for hosts whose default
# Docker profile denies the ENTRYPOINT cgroup remount (GH-7332). Selected
# only via the opt-in ``apparmor_profile`` parameter — never by default.
# ``unconfined`` is refused (no privileged fallbacks).
SUGGESTED_APPARMOR_PROFILE = "perc-systemd-cell"

# systemctl verbs the harness drives (lifecycle + state assertions).
LIFECYCLE_VERBS = ("start", "stop", "restart", "status")
SYSTEMCTL_VERBS = LIFECYCLE_VERBS + ("is-active", "is-enabled", "daemon-reload")

# Registration artifacts the installer scripts may write; all must be gone
# after uninstall (GH-7324 teardown acceptance).
_UNIT_PATH_TEMPLATES = (
    "/etc/systemd/system/{unit}.service",
    "/etc/systemd/system/multi-user.target.wants/{unit}.service",
    "/etc/init.d/{unit}",
    "/etc/default/{unit}",
)


class LifecycleError(RuntimeError):
    """A lifecycle step failed (command rc, unexpected unit state, leftovers)."""


def unit_for(product: str) -> str:
    """Service unit name owned by the harness for ``product``."""
    if product == "cms":
        return CMS_UNIT
    if product == "dts":
        return DTS_UNIT
    raise ValueError(f"Unknown product: {product}")


def readiness_url(product: str) -> str:
    """In-container readiness URL for ``product`` (localhost inside the cell)."""
    return f"http://localhost:{CONTAINER_PORTS[product]}{READINESS_PATHS[product]}"


def systemd_container_name(product: str, cell_suffix: Optional[str] = None) -> str:
    """Test-cell container name for the systemd lifecycle run.

    ``cell_suffix`` is the already-sanitized shard suffix (same contract as
    perc-devctl ``--cell-suffix``); ``None``/empty keeps the legacy fixed name.
    """
    base = f"perc-matrix-{product}-systemd"
    suffix = (cell_suffix or "").strip()
    return f"{base}-{suffix}" if suffix else base


def validate_container_name(container: str) -> str:
    """Refuse any docker-exec target outside the systemd test-cell namespace.

    Structural guard for the no-host-systemd acceptance: the harness only
    ever touches ``perc-matrix-*-systemd*`` containers, never the host.
    """
    if not container or not container.startswith("perc-matrix-"):
        raise LifecycleError(
            f"Refusing docker-exec target outside the QA cell namespace: {container!r}"
        )
    if "systemd" not in container:
        raise LifecycleError(
            f"Refusing non-systemd cell target for service-lifecycle run: {container!r}"
        )
    return container


def assert_safe_cell_argv(argv: Sequence[str]) -> List[str]:
    """Fail closed unless ``argv`` is a safe unprivileged cell invocation.

    Container-safety hard gate (2026-11-26 host VT takeover): the harness
    must never boot a cell with ``--privileged``, a shared/host cgroup
    or pid namespace, host ``/sys/fs/cgroup`` or ``/dev`` binds, or
    ``--device`` passthrough, and must never hand-roll a
    ``/sbin/init``/``systemd`` PID-1 ``docker run`` outside this builder.
    ``--security-opt apparmor=unconfined`` is likewise refused (GH-7332:
    no privileged fallbacks — a tailored profile must be named explicitly).
    Raises :class:`LifecycleError` on any violation; returns the argv as
    a list otherwise. A private cgroup namespace is required so the
    in-container systemd cannot touch host cgroups or host VTs.
    """
    args = [str(a) for a in argv]
    joined = " ".join(args)
    banned = [
        "--privileged",
        "--cgroupns=host",
        "--cgroupns=shared",
        "--pid=host",
        "--device",
        "/sys/fs/cgroup:/sys/fs/cgroup",
        "-v /dev",
        "--volume /dev",
        "apparmor=unconfined",
        "chvt",
        "openvt",
    ]
    for token in banned:
        if token in args or token in joined:
            raise LifecycleError(
                f"Refusing unsafe cell argv (banned {token!r}): {joined[:300]}"
            )
    if "--cgroupns=private" not in args:
        raise LifecycleError(
            "Refusing cell argv without --cgroupns=private: "
            f"{joined[:300]}"
        )
    return args


def _is_safe_profile_name(profile: str) -> bool:
    """True when ``profile`` is blank (no opt-in) or a safe AppArmor name.

    Only simple host-loaded profile names are accepted (letters,
    digits, ``-_.``) — no paths, no whitespace, and never
    ``unconfined`` (GH-7332: no privileged fallbacks).
    """
    if not profile:
        return True
    if profile != profile.strip():
        return False
    if profile == "unconfined":
        return False
    return all(ch.isalnum() or ch in "-_." for ch in profile)


def _apparmor_security_opt(profile: str) -> List[str]:
    """``--security-opt`` fragment for an opted-in AppArmor profile.

    Empty when no profile was selected (host default applies); the
    caller validates the name via :func:`_is_safe_profile_name` first.
    """
    if not profile:
        return []
    return ["--security-opt", f"apparmor={profile}"]


def docker_build_base_argv(repo_root: Path) -> List[str]:
    """Build argv for the stock matrix cell image (mirrors matrix-install-smoke)."""
    root = Path(repo_root)
    return [
        "docker",
        "build",
        "-t",
        BASE_MATRIX_IMAGE,
        "-f",
        str(root / "docker" / "matrix" / "Dockerfile"),
        str(root / "docker"),
    ]


def docker_build_systemd_argv(repo_root: Path) -> List[str]:
    """Build argv for the systemd-enabled cell image."""
    root = Path(repo_root)
    return [
        "docker",
        "build",
        "-t",
        SYSTEMD_IMAGE,
        "-f",
        str(root / "docker" / "matrix" / "Dockerfile.systemd"),
        str(root / "docker"),
    ]


def docker_run_systemd_argv(
    *,
    image: str,
    container_name: str,
    installer_jar_host: Path,
    product: str,
    network: Optional[str] = None,
    db_type: str = "h2",
    db_meta: Optional[Dict[str, str]] = None,
    apparmor_profile: Optional[str] = None,
) -> List[str]:
    """``docker run`` argv for a systemd PID-1 test cell (pure).

    Unprivileged + private cgroup namespace + SYS_ADMIN + tmpfs /run are
    required so the in-container systemd can manage units without exposing
    host ``/dev/tty*`` or host cgroups. No host ports are published:
    every assertion runs in-container via ``docker exec``. No host
    systemd is involved at any step. ``network`` is omitted when unset
    (default bridge); pass the matrix network for external-DB runs.

    ``apparmor_profile`` (GH-7332) names a host-loaded AppArmor profile
    selected via ``--security-opt`` for Docker hosts whose default
    profile denies the ENTRYPOINT cgroup remount (fail closed via
    wait-systemd otherwise). ``None``/blank keeps the host default.
    ``unconfined`` is refused — no privileged fallbacks.
    """
    validate_container_name(container_name)
    meta = dict(db_meta or {})
    profile = (apparmor_profile or "").strip()
    if profile == "unconfined" or not _is_safe_profile_name(profile):
        raise LifecycleError(
            f"Refusing unsafe apparmor profile: {apparmor_profile!r}"
        )
    argv = [
        "docker",
        "run",
        "-d",
        "--name",
        container_name,
    ]
    if network:
        argv.extend(["--network", network])
    argv.extend([
        "--cgroupns=private",
        "--cap-add=SYS_ADMIN",
    ])
    argv.extend(_apparmor_security_opt(profile))
    argv.extend([
        "--tmpfs",
        "/run",
        "--tmpfs",
        "/run/lock",
        "--tmpfs",
        "/tmp",
        "-v",
        f"{Path(installer_jar_host).resolve()}:/installer/installer.jar:ro",
        "-e",
        f"PRODUCT={product}",
        "-e",
        "INSTALLER_JAR=/installer/installer.jar",
        "-e",
        f"INSTALL_ROOT={CMS_INSTALL_ROOT}",
        "-e",
        f"DB_TYPE={db_type}",
    ])
    if db_type not in ("h2", "derby"):
        for key, flag in (
            ("container_host", "DB_HOST"),
            ("port", "DB_PORT"),
            ("name", "DB_NAME"),
            ("user", "DB_USER"),
            ("password", "DB_PASSWORD"),
        ):
            value = (meta.get(key) or "").strip()
            if value:
                argv.extend(["-e", f"{flag}={value}"])
        schema = (meta.get("schema") or "").strip()
        if schema:
            argv.extend(["-e", f"DB_SCHEMA={schema}"])
    argv.append(image)
    return assert_safe_cell_argv(argv)


def wait_systemd_exec_argv(container: str, timeout: int = 120) -> List[str]:
    """``docker exec`` argv blocking until in-container systemd is running.

    ``is-system-running --wait`` returns once the boot completes (running
    or degraded); the runner applies the wall-clock timeout.
    """
    validate_container_name(container)
    return ["docker", "exec", container, "systemctl", "is-system-running", "--wait"]


def install_exec_argv(
    container: str,
    *,
    product: str,
    install_root: str = CMS_INSTALL_ROOT,
    installer_jar: str = CELL_INSTALLER_JAR,
    db_type: str = "h2",
    demo_sites: bool = True,
) -> List[str]:
    """``docker exec`` argv running the silent installer WITHOUT direct start.

    Routes through ``cell-entrypoint.py --no-start`` (GH-7324) so the
    install itself follows the normal matrix-cell path while lifecycle
    stays with the service manager. Secrets stay inside the cell env;
    no ``--db.password`` argv is built here.
    """
    validate_container_name(container)
    argv = [
        "docker",
        "exec",
        container,
        "python3",
        CELL_ENTRYPOINT,
        "--product",
        product,
        "--installer-jar",
        installer_jar,
        "--install-root",
        install_root,
        "--db-type",
        db_type,
    ]
    argv.append("--demo-sites" if demo_sites else "--no-demo-sites")
    argv.extend(["--no-start", "--no-keep-alive"])
    return argv


def service_script_for(product: str) -> str:
    """In-container installer service script for ``product``."""
    if product == "cms":
        return CMS_SERVICE_SCRIPT
    if product == "dts":
        return DTS_SERVICE_SCRIPT
    raise ValueError(f"Unknown product: {product}")


def service_install_exec_argv(container: str, *, product: str, unit: str) -> List[str]:
    """``docker exec`` argv installing the native unit via the normal path.

    The CMS script prompts for the service user id on stdin — the runner
    feeds ``root\\n`` (in-container root, consistent with matrix cells).
    """
    validate_container_name(container)
    return [
        "docker",
        "exec",
        "-i",
        container,
        "bash",
        service_script_for(product),
        unit,
        "install",
        "--systemd",
    ]


def systemctl_exec_argv(container: str, verb: str, unit: Optional[str] = None) -> List[str]:
    """``docker exec`` argv for one in-cell ``systemctl`` invocation."""
    validate_container_name(container)
    if verb not in SYSTEMCTL_VERBS:
        raise ValueError(f"Unsupported systemctl verb: {verb}")
    argv = ["docker", "exec", container, "systemctl", verb]
    if verb == "daemon-reload":
        if unit is not None:
            raise ValueError("daemon-reload takes no unit")
        return argv
    if not unit:
        raise ValueError(f"systemctl {verb} requires a unit")
    argv.append(f"{unit}.service" if not unit.endswith(".service") else unit)
    return argv


def journalctl_exec_argv(container: str, unit: str, lines: int = 100) -> List[str]:
    """``docker exec`` argv capturing the unit journal (in-cell evidence)."""
    validate_container_name(container)
    name = unit if unit.endswith(".service") else f"{unit}.service"
    return [
        "docker",
        "exec",
        container,
        "journalctl",
        "-u",
        name,
        "-n",
        str(lines),
        "--no-pager",
    ]


def service_uninstall_exec_argv(container: str, *, product: str, unit: str) -> List[str]:
    """``docker exec`` argv removing the unit via the installer script."""
    validate_container_name(container)
    return [
        "docker",
        "exec",
        "-i",
        container,
        "bash",
        service_script_for(product),
        unit,
        "uninstall",
    ]


def server_log_tail_exec_argv(
    container: str,
    install_root: str = CMS_INSTALL_ROOT,
    *,
    tail_bytes: int = SERVER_LOG_TAIL_BYTES,
) -> List[str]:
    """``docker exec`` argv capturing the CMS server.log tail (GH-7332).

    The tail feeds :func:`assess_context_ready` after each serving
    probe: a running JVM with a dead Rhythmyx Spring context must not
    pass the lifecycle verdict on curl success alone. The log path is
    the container-internal POSIX path from ``rhythmyx_ready`` (same
    Linux-cell adjudication as the module header suppression).
    """
    validate_container_name(container)
    return [
        "docker", "exec", container,
        "tail", "-c", str(int(tail_bytes)),
        container_server_log_path(install_root),
    ]


def assess_context_ready(server_log_text: str) -> Tuple[bool, str]:
    """Reuse the ``rhythmyx_ready`` health assessment on a log tail.

    Pure; no I/O. Empty text skips the file ERROR gate per
    ``assess_rhythmyx_ready`` semantics — callers only pass text from a
    successful tail (an unreadable tail fails the probe before this
    runs), so empty here means an empty log, not a skipped assessment.
    Any Rhythmyx context-failure marker or server.log ERROR/FATAL/SEVERE
    line fails, even with HTTP 200.
    """
    return assess_rhythmyx_ready(
        200, "", server_log_text=server_log_text or "", require_http=False)


def leftover_unit_paths(unit: str) -> List[str]:
    """In-container registration paths that must be gone after uninstall."""
    return [template.format(unit=unit) for template in _UNIT_PATH_TEMPLATES]


def leftover_check_exec_argv(container: str, unit: str) -> List[str]:
    """``docker exec`` argv asserting no leftover units/links (exit 0 = clean).

    Codex P2 (#7324): ``test ! -e`` follows symlinks, so a dangling
    enablement link (target removed, link left) would pass. ``! -L``
    rejects the link itself.
    """
    validate_container_name(container)
    checks = " && ".join(
        f"test ! -e {path} -a ! -L {path}" for path in leftover_unit_paths(unit)
    )
    return ["docker", "exec", container, "bash", "-lc", checks]


def parse_active_state(output: str) -> str:
    """Normalize ``systemctl is-active`` output to a bare state token."""
    return (output or "").strip().split()[0] if (output or "").strip() else "unknown"


def is_unit_active(output: str) -> bool:
    """True only when ``systemctl is-active`` reports exactly ``active``."""
    return parse_active_state(output) == "active"


RunFn = Callable[..., subprocess.CompletedProcess]


@dataclass
class LifecycleStep:
    """One harness step: what ran and its outcome (evidence for the PR body)."""

    name: str
    argv: Sequence[str]
    returncode: int
    output_tail: str = ""


@dataclass
class LifecycleResult:
    """Full harness run: ordered steps plus the pass/fail verdict."""

    ok: bool
    product: str
    unit: str
    container: str
    steps: List[LifecycleStep] = field(default_factory=list)
    detail: str = ""


def _tail(text: str, limit: int = 2000) -> str:
    text = text or ""
    return text if len(text) <= limit else text[-limit:]


class _StepTimeout(LifecycleError):
    """Internal: a lifecycle step exceeded its wall-clock timeout."""

    def __init__(self, step_name: str, timeout: int):
        super().__init__(f"step {step_name} exceeded {timeout}s timeout")
        self.step_name = step_name
        self.timeout = timeout


def _decode_output(data) -> str:
    """Decode captured subprocess output, tolerating split multibyte chars.

    ``tail -c`` byte-truncation can split a UTF-8 sequence at the head of
    a server.log tail (Codex P2, #7332); strict decoding would raise
    ``UnicodeDecodeError`` instead of producing a readiness verdict.
    Replacement keeps the evidence readable and never affects ASCII
    marker matching. Accepts ``str`` (test fakes) and ``bytes`` (live).
    """
    if data is None:
        return ""
    if isinstance(data, str):
        return data
    return bytes(data).decode("utf-8", errors="replace")


def _run_step(
    run: RunFn,
    name: str,
    argv: Sequence[str],
    *,
    timeout: int,
    step_input: Optional[str] = None,
    output_limit: int = 2000,
) -> LifecycleStep:
    """Execute one harness step, capturing combined output for evidence.

    A wall-clock timeout is converted to a rc=124 evidence step (Codex P2,
    #7324): the sequence keeps its collected steps, the finish leg still
    writes the structured FAIL result, and teardown still runs — timeouts
    never discard diagnostic evidence. ``output_limit`` bounds the kept
    tail (log-tail steps pass a larger budget; the default preserves the
    evidence-size contract for every other step). Output is captured as
    bytes and decoded with replacement so a byte-truncated multibyte
    tail cannot raise ``UnicodeDecodeError`` (Codex P2, #7332).
    """
    kwargs = {"shell": False, "check": False, "capture_output": True,
              "text": False, "timeout": timeout}
    if step_input is not None:
        kwargs["input"] = step_input.encode("utf-8")
    try:
        completed = run(list(argv), **kwargs)
    except subprocess.TimeoutExpired as exc:
        # NB: TimeoutExpired stores partial output in .output (not
        # .stdout); .stdout is also probed for foreign run fakes.
        partial = _decode_output(
            getattr(exc, "output", None)
            or getattr(exc, "stdout", None)) + "\n" + _decode_output(
                getattr(exc, "stderr", None))
        return LifecycleStep(
            name=name, argv=list(argv), returncode=124,
            output_tail=_tail(f"TIMEOUT after {timeout}s: {partial.strip()}",
                              output_limit))
    output = _tail((_decode_output(completed.stdout) + "\n"
                    + _decode_output(completed.stderr)).strip(), output_limit)
    return LifecycleStep(name=name, argv=list(argv), returncode=completed.returncode,
                         output_tail=output)


def wait_for_systemd(
    run: RunFn,
    container: str,
    *,
    op_timeout: int,
    bus_timeout: int = 90,
    bus_interval: int = 5,
) -> List[LifecycleStep]:
    """Wait until in-container systemd answers, tolerating boot races.

    The cell boots asynchronously after ``docker run -d``: the first
    ``systemctl`` attempts can fail with ``Failed to connect to bus``
    before PID 1 creates the socket. Retry those (only those) until
    ``bus_timeout``; any other failure or an exhausted budget ends the
    wait. Returns every attempt as evidence.
    """
    validate_container_name(container)
    steps: List[LifecycleStep] = []
    deadline = time.time() + bus_timeout
    attempts = 0
    while True:
        attempts += 1
        step = _run_step(run, f"wait-systemd-attempt-{attempts}",
                         wait_systemd_exec_argv(container), timeout=op_timeout)
        final = step.returncode == 0 or "Failed to connect to bus" not in step.output_tail
        if final or time.time() >= deadline:
            step.name = "wait-systemd"
            steps.append(step)
            return steps
        steps.append(step)
        time.sleep(min(bus_interval, max(1, int(deadline - time.time()))))


def remove_cell_container(
    container: str, run: RunFn, *, timeout: int = 120
) -> LifecycleStep:
    """Remove the systemd test cell; the caller must check ``returncode``.

    (Codex P2, #7324): teardown success is asserted from this step, never
    assumed — a failed ``docker rm -f`` must not print ``cell-removed``.
    """
    validate_container_name(container)
    return _run_step(run, "remove-cell", ["docker", "rm", "-f", container],
                     timeout=timeout)


def probe_readiness(
    run: RunFn,
    container: str,
    product: str,
    *,
    label: str,
    ready_timeout: int,
    ready_interval: int,
    op_timeout: int,
) -> LifecycleStep:
    """Poll the in-container readiness URL until serving or budget exhausted.

    (Codex P2, #7324): the product must prove it serves after EVERY
    start/restart — a running-but-broken application context must not
    pass the lifecycle verdict. Returns one summary evidence step.

    (GH-7332): for ``cms``, a serving HTTP probe is necessary but not
    sufficient — the legacy ``/Rhythmyx/login`` probe renders 200 with a
    dead Spring context. After curl succeeds, the server.log tail is
    assessed with the ``rhythmyx_ready`` health check
    (:func:`assess_context_ready`); any context-failure marker or
    server.log ERROR fails the probe. Other products keep the HTTP-only
    verdict (their logs live outside server.log).
    """
    validate_container_name(container)
    probe_argv = ["docker", "exec", container, "curl", "-sf", readiness_url(product)]
    deadline = time.time() + ready_timeout
    ready_output = ""
    ready = False
    attempts = 0
    while time.time() < deadline:
        attempts += 1
        step = _run_step(run, f"{label}-attempt-{attempts}", probe_argv,
                         timeout=op_timeout)
        ready_output = step.output_tail
        if step.returncode == 0:
            ready = True
            break
        time.sleep(max(0.05, min(ready_interval, max(1, int(deadline - time.time())))))
    if not ready:
        return LifecycleStep(
            name=label, argv=probe_argv, returncode=1,
            output_tail=_tail(
                f"readiness probe never passed ({attempts} attempts): {ready_output}"))
    if product == "cms":
        tail_argv = server_log_tail_exec_argv(container)
        tail_step = _run_step(run, f"{label}-server-log", tail_argv,
                              timeout=op_timeout,
                              output_limit=SERVER_LOG_TAIL_BYTES + 1024)
        if tail_step.returncode != 0:
            # Fail closed (Codex P2, #7332): curl already proved the
            # server answers, so server.log must exist — an unreadable
            # tail means the context assessment cannot run, and the
            # known-weak login probe must not pass without it.
            return LifecycleStep(
                name=label, argv=tail_argv, returncode=1,
                output_tail=_tail(
                    "server.log tail unreadable, context not assessed: "
                    f"{tail_step.output_tail}"))
        context_ok, context_detail = assess_context_ready(tail_step.output_tail)
        if not context_ok:
            return LifecycleStep(
                name=label, argv=tail_argv, returncode=1,
                output_tail=_tail(
                    f"application context not ready: {context_detail}"))
    return LifecycleStep(
        name=label, argv=probe_argv, returncode=0, output_tail=_tail(ready_output))


def run_service_lifecycle(
    *,
    container: str,
    product: str,
    run: RunFn,
    install_timeout: int = 1800,
    op_timeout: int = 180,
    ready_timeout: int = 600,
    ready_interval: int = 15,
    journal_lines: int = 100,
) -> LifecycleResult:
    """Drive install → systemctl lifecycle → uninstall inside ``container``.

    Sequence (GH-7324 acceptance): wait for in-container systemd, silent
    install (no direct start), unit install via the normal path, enable
    assertion, ``start`` → active → readiness probe, ``stop`` → inactive,
    ``start`` → active → readiness probe, ``restart`` → active →
    readiness probe, ``status``, ``journalctl`` evidence, uninstall,
    leftover check. Every command runs as ``docker exec`` — host systemd
    is never touched. Step timeouts become rc=124 evidence (never raised),
    so the verdict + teardown always run.
    """
    validate_container_name(container)
    if product not in PRODUCTS:
        raise ValueError(f"Unknown product: {product}")
    unit = unit_for(product)
    result = LifecycleResult(ok=False, product=product, unit=unit, container=container)

    def fail(detail: str) -> LifecycleResult:
        result.detail = detail
        return result

    # 1. In-container systemd must be up before anything else runs.
    # is-system-running --wait blocks until running/degraded; the bus
    # socket may not exist yet right after docker run (boot race) —
    # wait_for_systemd retries only that case as evidence steps.
    wait_steps = wait_for_systemd(run, container, op_timeout=op_timeout)
    result.steps.extend(wait_steps)
    step = wait_steps[-1]
    if step.returncode != 0:
        return fail(f"in-container systemd never reached running state: {step.output_tail[-500:]}")

    # 2. Silent install without direct start (normal matrix-cell path).
    step = _run_step(run, "install", install_exec_argv(container, product=product),
                     timeout=install_timeout)
    result.steps.append(step)
    if step.returncode != 0:
        return fail(f"silent install failed: {step.output_tail[-500:]}")

    # 3. Native unit install via the shipped installer script (--systemd).
    step = _run_step(
        run, "install-unit",
        service_install_exec_argv(container, product=product, unit=unit),
        timeout=op_timeout, step_input="root\n",
    )
    result.steps.append(step)
    if step.returncode != 0:
        return fail(f"service unit install failed: {step.output_tail[-500:]}")

    # 4. Unit must be enabled (boots with the cell) but NOT started by install.
    step = _run_step(run, "assert-enabled",
                     systemctl_exec_argv(container, "is-enabled", unit),
                     timeout=op_timeout)
    result.steps.append(step)
    if step.returncode != 0 or parse_active_state(step.output_tail) != "enabled":
        return fail(f"unit not enabled after install: {step.output_tail[-500:]}")

    # 5. start → active.
    step = _run_step(run, "start", systemctl_exec_argv(container, "start", unit),
                     timeout=op_timeout)
    result.steps.append(step)
    if step.returncode != 0:
        return fail(f"systemctl start failed: {step.output_tail[-500:]}")
    step = _run_step(run, "assert-active-after-start",
                     systemctl_exec_argv(container, "is-active", unit),
                     timeout=op_timeout)
    result.steps.append(step)
    if not is_unit_active(step.output_tail):
        return fail(f"unit not active after start: {step.output_tail[-500:]}")

    # 6. In-container readiness probe (product actually serving).
    step = probe_readiness(run, container, product, label="readiness-probe",
                           ready_timeout=ready_timeout, ready_interval=ready_interval,
                           op_timeout=op_timeout)
    result.steps.append(step)
    if step.returncode != 0:
        return fail(step.output_tail[-500:])

    # 7. stop → inactive.
    step = _run_step(run, "stop", systemctl_exec_argv(container, "stop", unit),
                     timeout=op_timeout)
    result.steps.append(step)
    if step.returncode != 0:
        return fail(f"systemctl stop failed: {step.output_tail[-500:]}")
    step = _run_step(run, "assert-inactive-after-stop",
                     systemctl_exec_argv(container, "is-active", unit),
                     timeout=op_timeout)
    result.steps.append(step)
    # Codex P2 (#7324): require the explicit `inactive` token — a bus
    # error or `failed` state must not pass as "stopped". (A dead bus
    # would also fail the next start, but the stop assertion itself
    # must prove the state it claims.)
    if parse_active_state(step.output_tail) != "inactive":
        return fail(f"unit not inactive after stop: {step.output_tail[-500:]}")

    # 8. start again → active (stop/start cycle green).
    step = _run_step(run, "restart-cycle-start",
                     systemctl_exec_argv(container, "start", unit), timeout=op_timeout)
    result.steps.append(step)
    if step.returncode != 0:
        return fail(f"systemctl start (cycle) failed: {step.output_tail[-500:]}")
    step = _run_step(run, "assert-active-after-cycle-start",
                     systemctl_exec_argv(container, "is-active", unit),
                     timeout=op_timeout)
    result.steps.append(step)
    if not is_unit_active(step.output_tail):
        return fail(f"unit not active after cycle start: {step.output_tail[-500:]}")
    # Readiness again: the stop/start cycle must restore serving, not just activity.
    step = probe_readiness(run, container, product, label="readiness-probe-after-cycle-start",
                           ready_timeout=ready_timeout, ready_interval=ready_interval,
                           op_timeout=op_timeout)
    result.steps.append(step)
    if step.returncode != 0:
        return fail(step.output_tail[-500:])

    # 9. restart → active.
    step = _run_step(run, "restart", systemctl_exec_argv(container, "restart", unit),
                     timeout=op_timeout)
    result.steps.append(step)
    if step.returncode != 0:
        return fail(f"systemctl restart failed: {step.output_tail[-500:]}")
    step = _run_step(run, "assert-active-after-restart",
                     systemctl_exec_argv(container, "is-active", unit),
                     timeout=op_timeout)
    result.steps.append(step)
    if not is_unit_active(step.output_tail):
        return fail(f"unit not active after restart: {step.output_tail[-500:]}")
    # Readiness after restart: a running-but-broken context must not pass.
    step = probe_readiness(run, container, product, label="readiness-probe-after-restart",
                           ready_timeout=ready_timeout, ready_interval=ready_interval,
                           op_timeout=op_timeout)
    result.steps.append(step)
    if step.returncode != 0:
        return fail(step.output_tail[-500:])

    # 10. status must report the running unit (rc 0 while active).
    step = _run_step(run, "status", systemctl_exec_argv(container, "status", unit),
                     timeout=op_timeout)
    result.steps.append(step)
    if step.returncode != 0:
        return fail(f"systemctl status failed: {step.output_tail[-500:]}")

    # 11. journalctl evidence for the unit (StandardOutput=journal contract).
    # Codex P2 (#7324): journalctl prints "-- No entries --" (non-empty)
    # when the unit never logged — reject that placeholder explicitly.
    step = _run_step(run, "journalctl",
                     journalctl_exec_argv(container, unit, journal_lines),
                     timeout=op_timeout)
    result.steps.append(step)
    if (step.returncode != 0 or not step.output_tail.strip()
            or "no entries" in step.output_tail.lower()):
        return fail(f"journalctl produced no unit output: {step.output_tail[-500:]}")

    # 12. Uninstall via the normal path, then prove no leftovers.
    step = _run_step(
        run, "uninstall",
        service_uninstall_exec_argv(container, product=product, unit=unit),
        timeout=op_timeout, step_input="root\n",
    )
    result.steps.append(step)
    if step.returncode != 0:
        return fail(f"service uninstall failed: {step.output_tail[-500:]}")
    step = _run_step(run, "assert-no-leftovers",
                     leftover_check_exec_argv(container, unit), timeout=op_timeout)
    result.steps.append(step)
    if step.returncode != 0:
        return fail(f"leftover units/links after uninstall: {step.output_tail[-500:]}")

    result.ok = True
    result.detail = (
        f"systemctl start/stop/restart/status green for {unit} "
        f"in {container}; journalctl captured; uninstall left no leftovers"
    )
    return result
