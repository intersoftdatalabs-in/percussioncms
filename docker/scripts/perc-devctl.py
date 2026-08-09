#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform consolidated docker dev/test stack control.

Replaces ``docker/scripts/perc-devctl.sh`` (390 lines of bash). The
script is the operator-facing entry point for the cms-dts dev/test
docker compose stack and exposes every subcommand the original
``.sh`` provided: ``install``, ``up``, ``down``, ``status``, ``verify``,
``it-verify``, ``deploy-jar``, ``verify-fix``, ``logs-path``,
``inspect-install``, ``show-generated-passwords``.

QA mode (H2-in-Docker, no host install — issue #1827 / #1927) adds:

* ``qa-up`` — one-shot CMS on H2 via matrix cell (``--keep``), health wait
* ``qa-health`` — poll published CMS URL until ready (clear timeout);
  fail-fast when Jetty logs show Rhythmyx ApplicationContext failure
  (``Failed startup of context`` / ``BeanCurrentlyInCreationException``)
  even if HTTP still answers (#2462 / #2423). RESULT lines include
  ``HEALTH:healthy|unhealthy|starting|none`` from ``docker inspect``
  (matrix cell HEALTHCHECK — #2481 residual #2537)
* ``qa-down`` — destroy the QA cell (frees ports; no multi-GB orphans)
* ``qa-preflight`` — rebuild-chain preflight; detect a stale WebUI WAR vs
  a freshly built sitemanage SNAPSHOT (#2486)

Compose ``verify`` / ``verify-fix`` / ``deploy-jar --verify`` apply the same
Rhythmyx context log scan against the cms-dts container (#2480 companion to
#2462): HTTP + docker health alone is not enough when Spring context is dead.
Both ``qa-health`` (matrix cell) and ``verify`` (cms-dts) print ``HEALTH:``
from ``_docker_health`` so host log-scan and inspect status share one RESULT.

Each subcommand logs its full output to a timestamped file under
``docker/logs/`` and emits a single ``RESULT:OK STEP:<step> LOG:<path>``
or ``RESULT:FAIL STEP:<step> LOG:<path>`` line on stdout so agent
workflows can parse the result without parsing free-form output.

## Behavioral Notes (FR-009b)

- The original ``.sh`` used bash ``set -euo pipefail`` for early-fail.
  The Python port raises explicit exceptions and surfaces non-zero
  exit codes via ``subprocess.run([...], check=False)`` so failures
  are caught at the right granularity.
- The original ``.sh`` shell-built-in ``bash -lc "$cmd"`` for
  ``run_logged`` (which forces login-shell sourcing of ``.bashrc`` /
  ``/etc/profile``). The Python port replaces this with
  ``subprocess.run([...], shell=False)`` — the OS resolves the
  executable via PATH lookup, so the same effect is achieved without
  a shell. Operators who relied on ``.bashrc`` exports in their
  subshells need to set them in ``docker-compose.yml`` instead.
- All external invocations use ``subprocess.run([...], shell=False,
  check=False)`` (FR-008).
- Path discovery uses ``pathlib.Path``; the cross-platform
  ``docker compose`` invocation is built as an argv list (no shell
  quoting hazards).
- The dry-run gating for pytest uses ``--dry-run`` on every
  subcommand; the script logs the planned compose / curl / docker
  command and returns ``RESULT:OK STEP:<step> LOG:`` without invoking
  the external program.

Exit codes (per subcommand):

  0  success (or dry-run)
  1  invocation error / subcommand not found
  2  docker / curl / mvn invocation failed (mirrors the original
     ``.sh`` exit codes where ``RESULT:FAIL`` lines were followed by
     a non-zero exit code)
"""

from __future__ import annotations

import argparse
import logging
import os
import shlex
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, List, Optional, Sequence

# Sibling freeport helpers (stdlib only). Ensure scripts dir is importable
# when this file is loaded via importlib (unit tests) or as a script.
_SCRIPTS_DIR = Path(__file__).resolve().parent
if str(_SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS_DIR))
from perc_host_ports import find_free_port, is_port_free, resolve_host_port  # noqa: E402
from rhythmyx_ready import (  # noqa: E402
    DETAIL_CONTEXT_FAILED,
    assess_rhythmyx_ready,
    find_rhythmyx_context_failure,
)

LOG = logging.getLogger("perc-devctl")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_SUBPROCESS_FAILED = 2

DEFAULT_CONTAINER = "percussion-cms-dts"
DEFAULT_ENV_FILE = ".env.compose"
ENV_FILE_FALLBACK = ".env.compose.example"
DEFAULT_COMPOSE_FILE = "docker-compose.yml"

VERIFY_TIMEOUT_SECONDS_DEFAULT = 300
VERIFY_INTERVAL_SECONDS_DEFAULT = 5
VERIFY_FIX_TIMEOUT_SECONDS_DEFAULT = 240

# Preferred host ports when free and no env override (single-worktree baseline).
# Multi-worktree: set CMS_PORT / DTS_PORT / QA_CMS_HOST_PORT (or leave unset
# so freeport allocates) — see resolve_host_port / docker/README.md.
PREFERRED_VERIFY_CMS_HOST_PORT = 9992
PREFERRED_VERIFY_DTS_HOST_PORT = 9980
PREFERRED_QA_CMS_HOST_PORT = 9993
# Compose DB host publishes (docker-compose.yml ${*_PORT:-…}); #2004.
PREFERRED_MYSQL_HOST_PORT = 3306
PREFERRED_POSTGRES_HOST_PORT = 5433
PREFERRED_MSSQL_HOST_PORT = 1433

VERIFY_CMS_PATH = "/Rhythmyx/rest/folders/by-path/Assets"
VERIFY_DTS_PATH = "/"

# ---------------------------------------------------------------------------
# QA mode — H2 matrix cell for Playwright (no host install). Host port is
# resolved at runtime (env override or freeport); preferred baseline 9993
# matches matrix-install-smoke.py when that port is free (#2001).
# ---------------------------------------------------------------------------
QA_CMS_PRODUCT = "cms"
QA_CMS_DB = "h2"
QA_CMS_CELL_ID = f"{QA_CMS_PRODUCT}-{QA_CMS_DB}"
QA_CMS_CONTAINER = f"perc-matrix-{QA_CMS_CELL_ID}"
# Probe URL path for ``qa-health`` (#2482). The matrix-recommended primary
# is ``/Rhythmyx/rest/mimetypes`` (Spring-managed ``MimeTypeResource.ping()``
# — returns 404 when the Rhythmyx Spring ApplicationContext is dead, instead
# of the legacy ``/Rhythmyx/login`` 200 from the JSP renderer).
QA_CMS_PROBE_PATH = "/Rhythmyx/rest/mimetypes"
QA_ADMIN_USERNAME = "Admin"
QA_INSTALL_ROOT = "/opt/Percussion"
QA_PASSWORDS_REL = "var/config/generated/passwords"
# Matrix silent install can take several minutes on first run.
QA_PROBE_TIMEOUT_SECONDS_DEFAULT = 900
QA_PROBE_INTERVAL_SECONDS_DEFAULT = 5
# Tail size for docker logs when scanning for Rhythmyx context failure (#2462).
QA_LOG_SCAN_TAIL_LINES = 800
# Env var that overrides the QA probe path. Same name as the in-image
# ``rhythmyx_healthcheck.py`` (``RHYTHMYX_HEALTH_PATH``) so host, Docker,
# and matrix cells can be configured from one place. ``#2482`` matrix.
QA_CMS_PROBE_PATH_ENV = "RHYTHMYX_HEALTH_PATH"


# Freeport primitives live in perc_host_ports.py (shared with matrix) — #2001/#2005.


def resolve_verify_cms_url() -> str:
    """CMS probe URL for ``verify`` / ``verify-fix``.

    Override with ``VERIFY_CMS_URL``, else build from ``CMS_PORT`` (compose)
    / freeport with preferred host port :data:`PREFERRED_VERIFY_CMS_HOST_PORT`.
    """
    explicit = os.environ.get("VERIFY_CMS_URL", "").strip()
    if explicit:
        return explicit
    port = resolve_host_port(
        "CMS_PORT", preferred=PREFERRED_VERIFY_CMS_HOST_PORT
    )
    return f"http://localhost:{port}{VERIFY_CMS_PATH}"


def resolve_verify_dts_url() -> str:
    """DTS probe URL for ``verify`` / ``verify-fix``.

    Override with ``VERIFY_DTS_URL``, else build from ``DTS_PORT`` / freeport
    with preferred host port :data:`PREFERRED_VERIFY_DTS_HOST_PORT`.
    """
    explicit = os.environ.get("VERIFY_DTS_URL", "").strip()
    if explicit:
        return explicit
    port = resolve_host_port(
        "DTS_PORT", preferred=PREFERRED_VERIFY_DTS_HOST_PORT
    )
    return f"http://localhost:{port}{VERIFY_DTS_PATH}"


def resolve_qa_cms_host_port() -> int:
    """Host port for the QA CMS matrix cell publish mapping.

    Override with ``QA_CMS_HOST_PORT`` or ``CMS_HOST_PORT``; else preferred
    9993 when free, else freeport. Exports nothing by itself — callers that
    start the cell should write the chosen port into the environment for
    child processes and operator discovery.
    """
    return resolve_host_port(
        "QA_CMS_HOST_PORT",
        "CMS_HOST_PORT",
        preferred=PREFERRED_QA_CMS_HOST_PORT,
    )


def qa_cms_base_url(port: int) -> str:
    """Base URL for the QA CMS cell on the given host port."""
    return f"http://127.0.0.1:{port}"


def qa_cms_probe_url(port: int) -> str:
    """Health probe URL for the QA CMS cell.

    Honors :data:`QA_CMS_PROBE_PATH_ENV` (``RHYTHMYX_HEALTH_PATH``) so the
    QA cell, in-image ``rhythmyx_healthcheck.py`` (#2481), and any
    external orchestrator can be configured from the same env var. The
    default path is the matrix-recommended primary (#2482); see
    :data:`PROBE_URL_MATRIX` in :mod:`rhythmyx_ready`.
    """
    env_override = os.environ.get(QA_CMS_PROBE_PATH_ENV, "").strip()
    path = env_override or QA_CMS_PROBE_PATH
    return f"{qa_cms_base_url(port)}{path}"


def ensure_compose_db_host_ports() -> dict[str, int]:
    """Resolve compose DB host ports and pin them in ``os.environ`` (#2004).

    Compose maps ``${MYSQL_PORT:-3306}:3306``, ``${POSTGRES_PORT:-5433}:5432``,
    and ``${MSSQL_PORT:-1433}:1433``. Process-env pins override ``.env.compose``
    defaults so concurrent worktrees do not fail with address-already-in-use.
    Returns mapping of env key → resolved host port.
    """
    specs = (
        ("MYSQL_PORT", PREFERRED_MYSQL_HOST_PORT),
        ("POSTGRES_PORT", PREFERRED_POSTGRES_HOST_PORT),
        ("MSSQL_PORT", PREFERRED_MSSQL_HOST_PORT),
    )
    resolved: dict[str, int] = {}
    for env_key, preferred in specs:
        port = resolve_host_port(env_key, preferred=preferred)
        os.environ[env_key] = str(port)
        resolved[env_key] = port
    return resolved


def ensure_compose_host_ports() -> tuple[int, int]:
    """Resolve CMS/DTS (and DB) host ports for compose and pin ``os.environ``.

    Compose already maps ``${CMS_PORT:-9992}:9992`` and
    ``${DTS_PORT:-9980}:9980``. Pinning the resolved values into the process
    environment makes ``docker compose`` and later ``verify`` in the same
    session use the same published ports (critical for multi-worktree freeport).

    Also pins ``MYSQL_PORT`` / ``POSTGRES_PORT`` / ``MSSQL_PORT`` via
    :func:`ensure_compose_db_host_ports` so full compose stacks share the
    freeport contract (#2004).
    """
    cms = resolve_host_port("CMS_PORT", preferred=PREFERRED_VERIFY_CMS_HOST_PORT)
    dts = resolve_host_port("DTS_PORT", preferred=PREFERRED_VERIFY_DTS_HOST_PORT)
    os.environ["CMS_PORT"] = str(cms)
    os.environ["DTS_PORT"] = str(dts)
    ensure_compose_db_host_ports()
    return cms, dts


def ensure_qa_cms_host_port() -> int:
    """Resolve QA CMS host port and pin it for child processes / discovery."""
    port = resolve_qa_cms_host_port()
    os.environ["QA_CMS_HOST_PORT"] = str(port)
    # matrix-install-smoke reads CMS_HOST_PORT / QA_CMS_HOST_PORT for docker -p (#2005).
    os.environ.setdefault("CMS_HOST_PORT", str(port))
    return port


def _build_arg_parser() -> argparse.ArgumentParser:
    """Build the top-level arg parser with the canonical subcommand
    set. Subcommand-specific flags live in ``_build_<sub>_parser``.
    """
    p = argparse.ArgumentParser(
        prog="perc-devctl.py",
        description=(
            "Cross-platform docker dev/test stack control. Each "
            "subcommand writes full output to a timestamped file under "
            "docker/logs/ and emits a single RESULT:OK/FAIL line on "
            "stdout for agent workflows."
        ),
    )
    p.add_argument(
        "--repo-root",
        type=Path,
        default=None,
        help="Repo root (default: parent of this script's grandparent).",
    )
    p.add_argument(
        "--env-file",
        default=None,
        help=(
            f"docker compose env file (default: {DEFAULT_ENV_FILE} if it "
            f"exists, else {ENV_FILE_FALLBACK})."
        ),
    )
    p.add_argument(
        "--compose-file",
        default=DEFAULT_COMPOSE_FILE,
        help=(
            f"docker compose file "
            f"(default: {DEFAULT_COMPOSE_FILE})."
        ),
    )
    sub = p.add_subparsers(dest="command", required=True)

    # install
    pi = sub.add_parser(
        "install",
        help="Run the host-side installer (scripts/install-cms-dev.py).",
    )
    pi.add_argument("--reset", action="store_true")
    pi.add_argument("--no-bootstrap", action="store_true")
    pi.add_argument("--install-root")
    pi.add_argument("--dry-run", action="store_true")

    # up
    pu = sub.add_parser("up", help="Start mysql + cms-dts compose stack.")
    pu.add_argument("--build", action="store_true")
    pu.add_argument("--dry-run", action="store_true")

    # down
    pd = sub.add_parser("down", help="Stop compose stack.")
    pd.add_argument("--volumes", action="store_true")
    pd.add_argument("--dry-run", action="store_true")

    # status
    ps = sub.add_parser("status", help="Print concise stack status.")
    ps.add_argument("--dry-run", action="store_true")

    # verify
    pv = sub.add_parser("verify", help="Verify running stack health.")
    pv.add_argument("--timeout-seconds", type=int, default=VERIFY_TIMEOUT_SECONDS_DEFAULT)
    pv.add_argument("--interval-seconds", type=int, default=VERIFY_INTERVAL_SECONDS_DEFAULT)
    pv.add_argument("--dry-run", action="store_true")

    # it-verify
    pit = sub.add_parser(
        "it-verify",
        help="Run Maven integration verification with compose profile.",
    )
    pit.add_argument("--dry-run", action="store_true")

    # deploy-jar
    pdj = sub.add_parser("deploy-jar", help="Hot deploy a built jar.")
    pdj.add_argument("--jar", required=True)
    pdj.add_argument("--target", default="both")
    pdj.add_argument("--restart", action="store_true")
    pdj.add_argument("--verify", action="store_true")
    pdj.add_argument("--dry-run", action="store_true")

    # verify-fix
    pvf = sub.add_parser(
        "verify-fix",
        help="Deploy jar and run verification as one operation.",
    )
    pvf.add_argument("--jar", required=True)
    pvf.add_argument("--target", default="both")
    pvf.add_argument("--restart", action="store_true")
    pvf.add_argument("--no-restart", action="store_true")
    pvf.add_argument(
        "--timeout-seconds",
        type=int,
        default=VERIFY_FIX_TIMEOUT_SECONDS_DEFAULT,
    )
    pvf.add_argument("--dry-run", action="store_true")

    # logs-path
    sub.add_parser("logs-path", help="Print the logs directory path.")

    # inspect-install
    sub.add_parser(
        "inspect-install",
        help="Capture effective CMS and DTS database configuration.",
    )

    # show-generated-passwords
    sub.add_parser(
        "show-generated-passwords",
        help="Capture generated passwords file from running container.",
    )

    # --- QA mode (H2 Docker, no host install) — #1827 slice 1 / #1927 ---
    pqu = sub.add_parser(
        "qa-up",
        help=(
            "Start CMS on H2 in Docker for QA/Playwright (no host install). "
            "Waits for health; prints TEST_CMS_URL and admin creds hint."
        ),
    )
    pqu.add_argument(
        "--timeout-seconds",
        type=int,
        default=QA_PROBE_TIMEOUT_SECONDS_DEFAULT,
        help=(
            f"Seconds to wait for CMS ready during install/start "
            f"(default: {QA_PROBE_TIMEOUT_SECONDS_DEFAULT})."
        ),
    )
    pqu.add_argument(
        "--skip-image-build",
        action="store_true",
        help="Pass --skip-image-build to matrix-install-smoke (reuse local image).",
    )
    pqu.add_argument("--dry-run", action="store_true")

    # --- Rebuild-chain preflight — #2486 / #2532 ---
    pqp = sub.add_parser(
        "qa-preflight",
        help=(
            "Detect a stale WebUI WAR vs a freshly built sitemanage "
            "SNAPSHOT before qa-up (#2486 / #2532). Default content-hash "
            "(SHA-256 m2 jar vs WAR zip entry); --no-content-hash for "
            "mtime-only. Exits non-zero in --strict mode when stale."
        ),
    )
    pqp.add_argument("--strict", action="store_true")
    pqp.add_argument(
        "--no-content-hash",
        action="store_true",
        help="Mtime-only comparison (disable default SHA-256 content hash).",
    )
    pqp.add_argument("--dry-run", action="store_true")

    pqh = sub.add_parser(
        "qa-health",
        help=(
            "Poll QA CMS health URL until ready or timeout "
            "(default: freeport/env-resolved QA CMS probe URL)."
        ),
    )
    pqh.add_argument(
        "--timeout-seconds",
        type=int,
        default=QA_PROBE_TIMEOUT_SECONDS_DEFAULT,
        help=f"Seconds to wait (default: {QA_PROBE_TIMEOUT_SECONDS_DEFAULT}).",
    )
    pqh.add_argument(
        "--interval-seconds",
        type=int,
        default=QA_PROBE_INTERVAL_SECONDS_DEFAULT,
        help=f"Poll interval seconds (default: {QA_PROBE_INTERVAL_SECONDS_DEFAULT}).",
    )
    pqh.add_argument(
        "--url",
        default=None,
        help=(
            "Probe URL (default: from QA_CMS_HOST_PORT / CMS_HOST_PORT env, "
            f"preferred {PREFERRED_QA_CMS_HOST_PORT} when free, else freeport)."
        ),
    )
    pqh.add_argument("--dry-run", action="store_true")

    pqd = sub.add_parser(
        "qa-down",
        help=(
            "Tear down the H2 QA CMS cell (docker rm -f). "
            "Frees published ports; does not leave multi-GB orphans."
        ),
    )
    pqd.add_argument(
        "--container",
        default=QA_CMS_CONTAINER,
        help=f"Container name to remove (default: {QA_CMS_CONTAINER}).",
    )
    pqd.add_argument("--dry-run", action="store_true")

    return p


def _resolve_paths(args: argparse.Namespace) -> tuple[Path, Path, Path]:
    """Return (repo_root, env_file, compose_file) resolved from CLI
    args. ``env_file`` falls back to ``.env.compose.example`` when
    ``.env.compose`` is absent (matches the original ``.sh``).
    """
    repo_root = (
        args.repo_root.resolve()
        if args.repo_root
        else Path(__file__).resolve().parents[2]
    )
    env_file = (
        Path(args.env_file).resolve()
        if args.env_file
        else (repo_root / DEFAULT_ENV_FILE).resolve()
    )
    if not env_file.is_file():
        env_file = (repo_root / ENV_FILE_FALLBACK).resolve()
    compose_file = (Path(args.compose_file) if Path(args.compose_file).is_absolute()
                    else (repo_root / args.compose_file)).resolve()
    return repo_root, env_file, compose_file


def _log_dir(repo_root: Path) -> Path:
    """Return the docker/logs directory (created if absent)."""
    d = repo_root / "docker" / "logs"
    d.mkdir(parents=True, exist_ok=True)
    return d


def _ts() -> str:
    return datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")


def _new_log_file(log_dir: Path, prefix: str) -> Path:
    return log_dir / f"{prefix}-{_ts()}.log"


def _run_logged(
    label: str,
    argv: Sequence[str],
    *,
    log_dir: Path,
    cwd: Optional[Path] = None,
    dry_run: bool,
) -> tuple[int, Path]:
    """Run a subprocess and capture full output to ``docker/logs/<label>-<ts>.log``.
    Emits a single ``RESULT:OK`` / ``RESULT:FAIL`` line on stdout for
    agent consumption (matches the original ``.sh`` ``run_logged``).

    Returns ``(exit_code, log_file_path)`` so callers (e.g.
    ``cmd_verify_fix``) can include the log path in their own
    ``RESULT:FAIL`` lines. The agent-workflow contract is
    ``RESULT:OK/FAIL STEP:<label> LOG:<path>``; downstream subcommands
    MUST include the path so retry/loop diagnostics stay reachable.

    Cross-platform note: the log file is opened inside a ``with``
    block in BOTH the dry-run and real-run paths so the OS file
    handle is released deterministically. On Windows, leaving an
    unclosed ``open("w")`` handle causes ``PermissionError
    [WinError 32]`` when a later code path tries to open a file in
    the same directory (the OS keeps the handle locked until
    Python's garbage collector runs, which is unpredictable across
    tests). The ``with`` block fixes this on Windows without
    affecting the POSIX behavior.
    """
    log_file = _new_log_file(log_dir, label)
    if dry_run:
        LOG.info("DRY-RUN: %s (cwd=%s)", " ".join(argv), cwd)
        with log_file.open("w", encoding="utf-8") as f:
            f.write(f"DRY-RUN: {' '.join(argv)}\n")
            if cwd:
                f.write(f"cwd={cwd}\n")
        print(f"RESULT:OK STEP:{label} LOG:{log_file}")
        return EXIT_OK, log_file
    LOG.info("Running: %s (cwd=%s)", " ".join(argv), cwd)
    with log_file.open("w", encoding="utf-8") as f:
        completed = subprocess.run(
            list(argv),
            cwd=str(cwd) if cwd else None,
            shell=False,
            check=False,
            stdout=f,
            stderr=subprocess.STDOUT,
        )
    if completed.returncode != 0:
        print(f"RESULT:FAIL STEP:{label} LOG:{log_file}")
        return EXIT_SUBPROCESS_FAILED, log_file
    print(f"RESULT:OK STEP:{label} LOG:{log_file}")
    return EXIT_OK, log_file


def _docker_compose(env_file: Path, compose_file: Path, *args: str) -> List[str]:
    """Build a ``docker compose`` argv list."""
    return [
        "docker", "compose",
        f"--env-file={env_file}",
        f"-f", str(compose_file),
        *args,
    ]


# ---------------------------------------------------------------------------
# Subcommand implementations
# ---------------------------------------------------------------------------


def cmd_install(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    install_argv = [str(repo_root / "scripts" / "install-cms-dev.py")]
    if args.reset:
        install_argv.append("--reset")
    if args.no_bootstrap:
        install_argv.append("--no-bootstrap")
    if args.install_root:
        install_argv.extend(["--install-root", args.install_root])
    rc, _log_path = _run_logged(
        "install",
        install_argv,
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    return rc


def cmd_up(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    cms_port, dts_port = ensure_compose_host_ports()
    db_ports = {
        key: int(os.environ[key])
        for key in ("MYSQL_PORT", "POSTGRES_PORT", "MSSQL_PORT")
        if os.environ.get(key)
    }
    compose_argv = _docker_compose(env_file, compose_file, "up", "-d")
    if args.build:
        compose_argv.append("--build")
    rc, _log_path = _run_logged(
        "up",
        compose_argv,
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    # Agent/operator discovery: published host ports for this worktree cell.
    print(f"CMS_PORT={cms_port}")
    print(f"DTS_PORT={dts_port}")
    for key in ("MYSQL_PORT", "POSTGRES_PORT", "MSSQL_PORT"):
        if key in db_ports:
            print(f"{key}={db_ports[key]}")
    print(f"VERIFY_CMS_URL={resolve_verify_cms_url()}")
    print(f"VERIFY_DTS_URL={resolve_verify_dts_url()}")
    return rc


def cmd_down(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    compose_argv = _docker_compose(env_file, compose_file, "down")
    if args.volumes:
        compose_argv.append("-v")
    rc, _log_path = _run_logged(
        "down",
        compose_argv,
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    return rc


def cmd_status(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    rc, _log_path = _run_logged(
        "status",
        _docker_compose(env_file, compose_file, "ps", "--format", "json"),
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    return rc


def _curl_status(url: str, *, timeout: float) -> int:
    """Return the HTTP status code for ``url``, or 0 on network error."""
    try:
        with urllib.request.urlopen(url, timeout=timeout) as r:
            return r.status
    except urllib.error.HTTPError as e:
        return e.code
    except (urllib.error.URLError, TimeoutError, ConnectionError):
        return 0


def _docker_health(container_name: str) -> str:
    """Return docker ``Health.Status`` for ``container_name``.

    Values typically seen by operators / RESULT lines (#2481 / #2537):

    * ``healthy`` / ``unhealthy`` / ``starting`` — when the image has a HEALTHCHECK
    * ``none`` — container exists but has no ``.State.Health`` block
    * ``unknown`` — docker missing, inspect failed, or empty output
    """
    if not container_name:
        return "unknown"
    completed = subprocess.run(
        [
            "docker",
            "inspect",
            "-f",
            # Prefer explicit none over docker template error when Health is absent
            # (same pattern as matrix-install-smoke wait-for-health).
            "{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}",
            container_name,
        ],
        shell=False,
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        return "unknown"
    return (completed.stdout or "").strip() or "unknown"


def _docker_logs_tail(
    container_name: str,
    *,
    tail: int = QA_LOG_SCAN_TAIL_LINES,
    timeout: float = 30.0,
) -> str:
    """Return recent ``docker logs`` text for ``container_name``, or empty.

    Used by ``qa-health`` and compose ``verify`` / ``_verify_inline`` to
    fail-fast when Jetty reports Rhythmyx context startup failure while the
    HTTP port may still answer (#2462 / #2480).
    """
    if not container_name:
        return ""
    try:
        completed = subprocess.run(
            [
                "docker",
                "logs",
                "--tail",
                str(max(1, int(tail))),
                container_name,
            ],
            shell=False,
            check=False,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
    except (OSError, subprocess.TimeoutExpired):
        return ""
    if completed.returncode != 0:
        return ""
    # docker logs may put stream content on stderr depending on version.
    out = completed.stdout or ""
    err = completed.stderr or ""
    if out and err:
        return out + "\n" + err
    return out or err


def cmd_verify(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    """Poll CMS/DTS HTTP + docker health until ready or timeout / context fail.

    Ready means CMS and DTS probe HTTP codes are in the verify set, the
    cms-dts container health is ``healthy``, **and** recent ``docker logs``
    for that container do **not** contain Rhythmyx ApplicationContext
    failure markers (see :mod:`rhythmyx_ready`).

    Context-failure markers cause **immediate** FAIL even when HTTP answers
    and docker reports healthy (Jetty up, Spring webapp dead — #2480 / #2462).
    """
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    timeout = args.timeout_seconds
    interval = args.interval_seconds
    max_checks = max(1, timeout // interval) if interval > 0 else 1
    log_file = _new_log_file(log_dir, "verify")
    cms_url = resolve_verify_cms_url()
    dts_url = resolve_verify_dts_url()
    container = DEFAULT_CONTAINER

    if args.dry_run:
        LOG.info(
            "DRY-RUN: verify plan: %d checks x %ds interval against %s + %s "
            "(+ Rhythmyx context log scan on %s)",
            max_checks, interval, cms_url, dts_url, container,
        )
        with log_file.open("w", encoding="utf-8") as f:
            f.write(
                f"DRY-RUN: verify max_checks={max_checks} interval={interval}\n"
                f"endpoints={cms_url} {dts_url}\n"
                f"container={container}\n"
                f"log_scan=rhythmyx_context_fail_markers\n"
            )
        print(f"RESULT:OK STEP:verify CMS_HTTP:200 DTS_HTTP:200 HEALTH:healthy LOG:{log_file}")
        return EXIT_OK

    last_cms = 0
    last_dts = 0
    last_health = "unknown"
    last_detail = "not_checked"
    for check in range(1, max_checks + 1):
        last_cms = _curl_status(cms_url, timeout=5.0)
        last_dts = _curl_status(dts_url, timeout=5.0)
        last_health = _docker_health(container)
        logs = _docker_logs_tail(container)
        # require_http=False: HTTP readiness is still gated by the verify
        # code set below; this assessor is for context-fail markers only.
        _ready_ctx, last_detail = assess_rhythmyx_ready(
            last_cms, logs, require_http=False,
        )

        # Fail-fast when Spring/Jetty already reported a dead Rhythmyx context.
        if DETAIL_CONTEXT_FAILED in last_detail:
            match = find_rhythmyx_context_failure(logs) or "unknown"
            with log_file.open("w", encoding="utf-8") as f:
                f.write("verify failed\n")
                f.write(f"cms_http={last_cms}\n")
                f.write(f"dts_http={last_dts}\n")
                f.write(f"container_health={last_health}\n")
                f.write(f"check={check}\n")
                f.write(f"container={container}\n")
                f.write(f"detail={last_detail}\n")
                f.write(f"match={match}\n")
                f.write(f"cms_url={cms_url}\n")
                f.write(f"dts_url={dts_url}\n")
                f.write(
                    "hint: Rhythmyx ApplicationContext failed; Jetty HTTP / "
                    "docker health may still look green. Inspect docker logs "
                    "for Spring cycle / bean errors (parent #2423 / #2480). "
                    "Do not treat port-up as ready.\n"
                )
            print(
                f"RESULT:FAIL STEP:verify DETAIL:{DETAIL_CONTEXT_FAILED} "
                f"MATCH:{match} CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
                f"HEALTH:{last_health} CONTAINER:{container} LOG:{log_file}"
            )
            return EXIT_SUBPROCESS_FAILED

        if (
            last_cms in (200, 401, 403)
            and last_dts in (200, 401, 403)
            and last_health == "healthy"
        ):
            with log_file.open("w", encoding="utf-8") as f:
                f.write("verify success\n")
                f.write(f"cms_http={last_cms}\n")
                f.write(f"dts_http={last_dts}\n")
                f.write(f"container_health={last_health}\n")
                f.write(f"cms_url={cms_url}\n")
                f.write(f"dts_url={dts_url}\n")
                f.write("rhythmyx_context=ok\n")
            print(
                f"RESULT:OK STEP:verify CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
                f"HEALTH:{last_health} LOG:{log_file}"
            )
            return EXIT_OK
        time.sleep(interval)

    # Timeout path: re-scan logs once more so DETAIL prefers context failure.
    final_logs = _docker_logs_tail(container)
    final_match = find_rhythmyx_context_failure(final_logs)
    with log_file.open("w", encoding="utf-8") as f:
        f.write("verify failed\n")
        f.write(f"timeout_seconds={timeout}\n")
        f.write(f"interval_seconds={interval}\n")
        f.write(f"cms_http={last_cms}\n")
        f.write(f"dts_http={last_dts}\n")
        f.write(f"container_health={last_health}\n")
        f.write(f"container={container}\n")
        f.write(f"last_detail={last_detail}\n")
        if final_match:
            f.write(f"match={final_match}\n")
            f.write(
                "hint: Rhythmyx context failure markers found in docker logs; "
                "see parent #2423 / #2480. HTTP-only / health-only ready is "
                "insufficient.\n"
            )
        f.write("--- compose ps\n")
        # capture compose ps in a child log to avoid clobbering
        compose_ps_log = _new_log_file(log_dir, "verify-compose-ps")
        with compose_ps_log.open("w") as ps_f:
            subprocess.run(
                _docker_compose(env_file, compose_file, "ps"),
                shell=False,
                check=False,
                stdout=ps_f,
                stderr=subprocess.STDOUT,
            )
        f.write(f"see {compose_ps_log}\n")
    if final_match:
        print(
            f"RESULT:FAIL STEP:verify DETAIL:{DETAIL_CONTEXT_FAILED} "
            f"MATCH:{final_match} CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
            f"HEALTH:{last_health} CONTAINER:{container} LOG:{log_file}"
        )
    else:
        print(
            f"RESULT:FAIL STEP:verify DETAIL:timeout after {timeout}s "
            f"(cms_http={last_cms} dts_http={last_dts} health={last_health}) "
            f"LOG:{log_file}"
        )
    return EXIT_SUBPROCESS_FAILED


def cmd_it_verify(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    # Windows: Maven wrapper is mvnw.cmd; Unix: mvnw (shell script).
    mvnw = repo_root / ("mvnw.cmd" if sys.platform.startswith("win") else "mvnw")
    rc, _log_path = _run_logged(
        "it-verify",
        [
            str(mvnw),
            "-P", "integration-test,docker-compose",
            "verify",
        ],
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    return rc


def _deploy_jar_argv(
    repo_root: Path,
    jar_path: str,
    target: str,
    restart: bool,
) -> List[str]:
    """Build the argv list for ``docker/scripts/hot-deploy-jar.py``."""
    argv = [
        "python3",
        str(repo_root / "docker" / "scripts" / "hot-deploy-jar.py"),
        "--jar", jar_path,
        "--target", target,
    ]
    if restart:
        argv.append("--restart")
    return argv


def cmd_deploy_jar(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    rc, _log_path = _run_logged(
        "deploy-jar",
        _deploy_jar_argv(repo_root, args.jar, args.target, args.restart),
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    if rc != EXIT_OK or not args.verify:
        return rc
    # ``--verify`` triggers a follow-up health check (3 minutes by default).
    rc_verify, _log_path = _verify_inline(
        repo_root, env_file, compose_file, log_dir,
        timeout_seconds=180, interval_seconds=5, dry_run=args.dry_run,
    )
    return rc_verify


def cmd_verify_fix(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    restart_flag = args.restart and not args.no_restart
    # Phase 1 — deploy
    deploy_argv = _deploy_jar_argv(repo_root, args.jar, args.target, restart_flag)
    rc_deploy, deploy_log = _run_logged(
        "deploy-jar",
        deploy_argv,
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    if rc_deploy != EXIT_OK:
        # Include the deploy log path so the agent-workflow contract
        # ``RESULT:OK/FAIL STEP:<label> LOG:<path>`` is honored. Without
        # this, downstream retry/loop tooling can't find the diagnostics
        # (kilo-code-bot review thread 3631740695).
        print(f"RESULT:FAIL STEP:verify-fix PHASE:deploy LOG:{deploy_log}")
        return EXIT_SUBPROCESS_FAILED
    # Phase 2 — verify
    rc_verify, verify_log = _verify_inline(
        repo_root, env_file, compose_file, log_dir,
        timeout_seconds=args.timeout_seconds, interval_seconds=5,
        dry_run=args.dry_run,
    )
    if rc_verify != EXIT_OK:
        # Same contract — always include the verify log path
        # (kilo-code-bot review thread 3631740700).
        print(f"RESULT:FAIL STEP:verify-fix PHASE:verify LOG:{verify_log}")
        return EXIT_SUBPROCESS_FAILED
    print(f"RESULT:OK STEP:verify-fix LOG:{verify_log}")
    return EXIT_OK


def _verify_inline(
    repo_root: Path,
    env_file: Path,
    compose_file: Path,
    log_dir: Path,
    *,
    timeout_seconds: int,
    interval_seconds: int,
    dry_run: bool,
) -> tuple[int, Path]:
    """Inline verify used by ``deploy-jar --verify`` and ``verify-fix``.

    Mirrors ``cmd_verify`` (including Rhythmyx context log fail-fast —
    #2480) but bypasses the wrapped ``verify`` parser argument. Returns
    ``(exit_code, log_file_path)`` so callers (e.g. ``cmd_verify_fix``)
    can include the log path in their own ``RESULT:FAIL`` lines.
    """
    max_checks = max(1, timeout_seconds // interval_seconds) if interval_seconds > 0 else 1
    log_file = _new_log_file(log_dir, "verify")

    cms_url = resolve_verify_cms_url()
    dts_url = resolve_verify_dts_url()
    container = DEFAULT_CONTAINER

    if dry_run:
        with log_file.open("w", encoding="utf-8") as f:
            f.write(
                f"DRY-RUN: verify-inline max_checks={max_checks} interval={interval_seconds}\n"
                f"endpoints={cms_url} {dts_url}\n"
                f"container={container}\n"
                f"log_scan=rhythmyx_context_fail_markers\n"
            )
        return EXIT_OK, log_file

    last_cms = 0
    last_dts = 0
    last_health = "unknown"
    for _ in range(1, max_checks + 1):
        last_cms = _curl_status(cms_url, timeout=5.0)
        last_dts = _curl_status(dts_url, timeout=5.0)
        last_health = _docker_health(container)
        logs = _docker_logs_tail(container)
        _ready_ctx, detail = assess_rhythmyx_ready(
            last_cms, logs, require_http=False,
        )
        if DETAIL_CONTEXT_FAILED in detail:
            match = find_rhythmyx_context_failure(logs) or "unknown"
            with log_file.open("w", encoding="utf-8") as f:
                f.write("verify failed\n")
                f.write(f"cms_http={last_cms}\n")
                f.write(f"dts_http={last_dts}\n")
                f.write(f"container_health={last_health}\n")
                f.write(f"container={container}\n")
                f.write(f"detail={detail}\n")
                f.write(f"match={match}\n")
                f.write(
                    "hint: Rhythmyx ApplicationContext failed during "
                    "verify-inline (#2480 / #2423).\n"
                )
            print(
                f"RESULT:FAIL STEP:verify DETAIL:{DETAIL_CONTEXT_FAILED} "
                f"MATCH:{match} CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
                f"HEALTH:{last_health} CONTAINER:{container} LOG:{log_file}"
            )
            return EXIT_SUBPROCESS_FAILED, log_file
        if (
            last_cms in (200, 401, 403)
            and last_dts in (200, 401, 403)
            and last_health == "healthy"
        ):
            with log_file.open("w", encoding="utf-8") as f:
                f.write("verify success\n")
                f.write(f"cms_http={last_cms}\n")
                f.write(f"dts_http={last_dts}\n")
                f.write(f"container_health={last_health}\n")
                f.write("rhythmyx_context=ok\n")
            print(
                f"RESULT:OK STEP:verify CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
                f"HEALTH:{last_health} LOG:{log_file}"
            )
            return EXIT_OK, log_file
        time.sleep(interval_seconds)

    final_logs = _docker_logs_tail(container)
    final_match = find_rhythmyx_context_failure(final_logs)
    with log_file.open("w", encoding="utf-8") as f:
        f.write("verify failed\n")
        f.write(f"cms_http={last_cms}\n")
        f.write(f"dts_http={last_dts}\n")
        f.write(f"container_health={last_health}\n")
        if final_match:
            f.write(f"match={final_match}\n")
    if final_match:
        print(
            f"RESULT:FAIL STEP:verify DETAIL:{DETAIL_CONTEXT_FAILED} "
            f"MATCH:{final_match} CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
            f"HEALTH:{last_health} CONTAINER:{container} LOG:{log_file}"
        )
    else:
        print(f"RESULT:FAIL STEP:verify LOG:{log_file}")
    return EXIT_SUBPROCESS_FAILED, log_file


def cmd_logs_path(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, _, _ = paths
    log_dir = _log_dir(repo_root)
    print(f"RESULT:OK STEP:logs-path LOG_DIR:{log_dir}")
    return EXIT_OK


# Note: the original ``.sh`` had a ``_docker_exec_capture`` helper that
# wrapped ``docker exec container bash -lc '<script>'``. With the refactor
# to ``_run_logged`` returning ``(rc, log_path)`` tuples (kilo-code-bot
# review thread #7 on PR #1468), that wrapper would need its signature
# updated; but the wrapper has no remaining callers in ``_DISPATCH`` so
# the simpler fix is to delete it. Direct ``docker exec`` invocations live
# in ``cmd_inspect_install`` and ``cmd_show_generated_passwords`` inline.


_INSPECT_SCRIPT = """set -euo pipefail
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
"""


_SHOW_PASSWORDS_SCRIPT = """set -euo pipefail
install_root=\"${PERC_INSTALL_ROOT:-/opt/Percussion}\"
pwd_file=\"$install_root/var/config/generated/passwords\"
if [ ! -f \"$pwd_file\" ]; then
  echo \"generated_passwords_missing=$pwd_file\"
  exit 1
fi
echo \"generated_passwords_file=$pwd_file\"
cat \"$pwd_file\"
"""


def cmd_inspect_install(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, _, _ = paths
    log_dir = _log_dir(repo_root)
    rc, _log_path = _run_logged(
        "inspect-install",
        ["docker", "exec", DEFAULT_CONTAINER, "bash", "-lc", _INSPECT_SCRIPT],
        log_dir=log_dir,
        dry_run=getattr(args, "dry_run", False),
    )
    return rc


def cmd_show_generated_passwords(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, _, _ = paths
    log_dir = _log_dir(repo_root)
    rc, _log_path = _run_logged(
        "show-generated-passwords",
        ["docker", "exec", DEFAULT_CONTAINER, "bash", "-lc", _SHOW_PASSWORDS_SCRIPT],
        log_dir=log_dir,
        dry_run=getattr(args, "dry_run", False),
    )
    return rc


# ---------------------------------------------------------------------------
# QA mode — H2-in-Docker one-shot (issue #1827 slice 1 / #1927)
# ---------------------------------------------------------------------------


def _qa_matrix_up_argv(
    repo_root: Path,
    *,
    probe_timeout: int,
    skip_image_build: bool,
    dry_run: bool,
) -> List[str]:
    """Build argv for matrix-install-smoke CMS+H2 with ``--keep`` (pure).

    Uses ``sys.executable`` so Windows/Linux/macOS all invoke the same
    interpreter that is running perc-devctl (no hardcoded ``python3``).
    """
    script = repo_root / "docker" / "scripts" / "matrix-install-smoke.py"
    argv: List[str] = [
        sys.executable,
        str(script),
        "--repo-root",
        str(repo_root),
        "--product",
        QA_CMS_PRODUCT,
        "--db",
        QA_CMS_DB,
        "--keep",
        "--probe-timeout",
        str(probe_timeout),
    ]
    if skip_image_build:
        argv.append("--skip-image-build")
    if dry_run:
        argv.append("--dry-run")
    return argv


def _qa_destroy_argv(container_name: str) -> List[str]:
    """``docker rm -f`` argv for the QA cell (pure; unit-tested)."""
    return ["docker", "rm", "-f", container_name]


def _qa_print_endpoint_banner(
    host_port: int,
    *,
    admin_password_line: Optional[str] = None,
) -> None:
    """Emit agent-parseable endpoint + credential guidance after qa-up.

    Does not emit RESULT: lines — callers use ``_run_logged`` for that
    contract so agent parsers see a single OK/FAIL per step.
    ``host_port`` is the resolved published host port (env or freeport).
    """
    base = qa_cms_base_url(host_port)
    print(f"QA_CMS_HOST_PORT={host_port}")
    print(f"QA_CMS_URL:{base}")
    print(f"TEST_CMS_URL={base}")
    print(f"TEST_DB_TYPE={QA_CMS_DB}")
    print(f"TEST_PRODUCT={QA_CMS_PRODUCT}")
    print(f"QA_CONTAINER:{QA_CMS_CONTAINER}")
    print(f"ADMIN_USERNAME={QA_ADMIN_USERNAME}")
    if admin_password_line:
        print(admin_password_line)
    else:
        # URL path inside container always uses '/'; not a host filesystem path.
        pwd_path = f"{QA_INSTALL_ROOT}/{QA_PASSWORDS_REL}"
        print(
            "ADMIN_PASSWORD: fetch with "
            f"docker exec {QA_CMS_CONTAINER} cat {pwd_path} "
            f"(look for {QA_ADMIN_USERNAME}=…)"
        )


def _qa_fetch_admin_password(container_name: str) -> Optional[str]:
    """Best-effort read of Admin=… from generated passwords in the QA cell.

    Returns a line ``ADMIN_PASSWORD=<value>`` or None if unavailable.
    Never raises; callers treat missing passwords as non-fatal (URL is enough
    for health; Playwright login may need env set separately).
    """
    pwd_path = f"{QA_INSTALL_ROOT}/{QA_PASSWORDS_REL}"
    try:
        completed = subprocess.run(
            ["docker", "exec", container_name, "cat", pwd_path],
            shell=False,
            check=False,
            capture_output=True,
            text=True,
            timeout=30,
        )
    except (OSError, subprocess.TimeoutExpired):
        return None
    if completed.returncode != 0:
        return None
    text = completed.stdout or ""
    for line in text.splitlines():
        line = line.strip()
        if line.startswith(f"{QA_ADMIN_USERNAME}="):
            value = line.split("=", 1)[1].strip()
            if value:
                return f"ADMIN_PASSWORD={value}"
    return None


def cmd_qa_up(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    """Bring up CMS on H2 in Docker and wait until the cell is ready.

    Delegates install/start/health-wait to ``matrix-install-smoke.py`` with
    ``--product cms --db h2 --keep``. CMS wait requires docker
    ``Health.Status=healthy`` (fail-fast when already unhealthy — #2535 /
    #2481), host HTTP probe, and host ``rhythmyx_ready`` log scan. On success
    prints ``TEST_CMS_URL`` and admin credential guidance for Playwright /
    agents.

    Host port is resolved via :func:`ensure_qa_cms_host_port` (env override
    or freeport) and exported as ``QA_CMS_HOST_PORT`` / ``CMS_HOST_PORT`` so
    matrix-install-smoke docker ``-p`` and operators / Playwright agree (#2005).
    """
    repo_root, _env_file, _compose_file = paths
    log_dir = _log_dir(repo_root)
    dry_run = bool(args.dry_run)
    probe_timeout = int(args.timeout_seconds)
    skip_image_build = bool(args.skip_image_build)
    host_port = ensure_qa_cms_host_port()

    matrix_argv = _qa_matrix_up_argv(
        repo_root,
        probe_timeout=probe_timeout,
        skip_image_build=skip_image_build,
        dry_run=dry_run,
    )
    # Use a dedicated label so logs are easy to find for QA mode.
    # _run_logged emits RESULT:OK/FAIL STEP:qa-up LOG:<path>.
    rc, log_file = _run_logged(
        "qa-up",
        matrix_argv,
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=dry_run,
    )
    if rc != EXIT_OK:
        # Extra detail line for agent timeout diagnosis (RESULT already printed).
        print(
            f"QA_DETAIL:matrix-install-smoke failed "
            f"timeout_seconds={probe_timeout} "
            f"QA_CMS_HOST_PORT={host_port} LOG:{log_file}"
        )
        return rc

    if dry_run:
        _qa_print_endpoint_banner(host_port)
        return EXIT_OK

    admin_line = _qa_fetch_admin_password(QA_CMS_CONTAINER)
    _qa_print_endpoint_banner(host_port, admin_password_line=admin_line)
    return EXIT_OK


def cmd_qa_health(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    """Poll the QA CMS probe URL until ready or a clear timeout / context fail.

    Ready means:

    * HTTP status on the probe URL is in the ready set (200/302/401/403), **and**
    * Recent ``docker logs`` for the QA cell do **not** contain Rhythmyx
      ApplicationContext failure markers (see :mod:`rhythmyx_ready`), **and**
    * Docker ``Health.Status`` for the QA cell is ``healthy`` (matrix HEALTHCHECK
      after #2481 — same gate as compose ``verify`` for cms-dts).

    RESULT lines always include ``HEALTH:<status>`` so operators see host
    log-scan **and** inspect health in one line (#2537 residual of #2481).

    Context-failure markers cause **immediate** FAIL even when HTTP answers
    (Jetty up, Spring webapp dead — #2462 residual of #2423).
    """
    repo_root, _env_file, _compose_file = paths
    log_dir = _log_dir(repo_root)
    timeout = int(args.timeout_seconds)
    interval = int(args.interval_seconds)
    host_port = resolve_qa_cms_host_port()
    url = args.url or qa_cms_probe_url(host_port)
    container = QA_CMS_CONTAINER
    max_checks = max(1, timeout // interval) if interval > 0 else 1
    log_file = _new_log_file(log_dir, "qa-health")

    if args.dry_run:
        LOG.info(
            "DRY-RUN: qa-health plan: %d checks x %ds interval against %s "
            "(+ Rhythmyx context log scan + docker Health.Status on %s)",
            max_checks,
            interval,
            url,
            container,
        )
        with log_file.open("w", encoding="utf-8") as f:
            f.write(
                f"DRY-RUN: qa-health max_checks={max_checks} interval={interval}\n"
                f"url={url}\n"
                f"container={container}\n"
                f"log_scan=rhythmyx_context_fail_markers\n"
                f"docker_health=inspect Health.Status\n"
            )
        print(
            f"RESULT:OK STEP:qa-health HTTP:200 URL:{url} "
            f"HEALTH:healthy CONTAINER:{container} LOG:{log_file}"
        )
        return EXIT_OK

    last_code = 0
    last_health = "unknown"
    last_detail = "not_checked"
    for check in range(1, max_checks + 1):
        last_code = _curl_status(url, timeout=5.0)
        last_health = _docker_health(container)
        logs = _docker_logs_tail(container)
        ready, last_detail = assess_rhythmyx_ready(last_code, logs)

        # Fail-fast when Spring/Jetty already reported a dead Rhythmyx context.
        if DETAIL_CONTEXT_FAILED in last_detail:
            match = find_rhythmyx_context_failure(logs) or "unknown"
            with log_file.open("w", encoding="utf-8") as f:
                f.write("qa-health failed\n")
                f.write(f"url={url}\n")
                f.write(f"last_http={last_code}\n")
                f.write(f"container_health={last_health}\n")
                f.write(f"check={check}\n")
                f.write(f"container={container}\n")
                f.write(f"detail={last_detail}\n")
                f.write(f"match={match}\n")
                f.write(
                    "hint: Rhythmyx ApplicationContext failed; Jetty HTTP may "
                    "still bind. Inspect docker logs for Spring cycle / bean "
                    "errors (parent #2423). Do not treat port-up as ready.\n"
                )
            print(
                f"RESULT:FAIL STEP:qa-health DETAIL:{DETAIL_CONTEXT_FAILED} "
                f"MATCH:{match} HTTP:{last_code} HEALTH:{last_health} "
                f"URL:{url} CONTAINER:{container} LOG:{log_file}"
            )
            return EXIT_SUBPROCESS_FAILED

        # HTTP + clean logs + docker healthy (parity with compose verify).
        if ready and last_health == "healthy":
            with log_file.open("w", encoding="utf-8") as f:
                f.write("qa-health success\n")
                f.write(f"url={url}\n")
                f.write(f"http={last_code}\n")
                f.write(f"container_health={last_health}\n")
                f.write(f"check={check}\n")
                f.write(f"container={container}\n")
                f.write("rhythmyx_context=ok\n")
            print(
                f"RESULT:OK STEP:qa-health HTTP:{last_code} HEALTH:{last_health} "
                f"URL:{url} CONTAINER:{container} LOG:{log_file}"
            )
            return EXIT_OK

        time.sleep(interval)

    # Timeout path: re-scan logs once more so DETAIL prefers context failure.
    final_logs = _docker_logs_tail(container)
    final_match = find_rhythmyx_context_failure(final_logs)
    with log_file.open("w", encoding="utf-8") as f:
        f.write("qa-health failed\n")
        f.write(f"url={url}\n")
        f.write(f"last_http={last_code}\n")
        f.write(f"container_health={last_health}\n")
        f.write(f"timeout_seconds={timeout}\n")
        f.write(f"interval_seconds={interval}\n")
        f.write(f"container={container}\n")
        f.write(f"last_detail={last_detail}\n")
        if final_match:
            f.write(f"match={final_match}\n")
            f.write(
                "hint: Rhythmyx context failure markers found in docker logs; "
                "see parent #2423. HTTP-only ready is insufficient.\n"
            )
        else:
            f.write(
                "hint: run qa-up first; ensure installer jar is built "
                "(modules/perc-distribution-tree package); if Jetty is up but "
                "login fails, scan docker logs for Failed startup of context; "
                "also check docker inspect Health.Status on the QA cell\n"
            )
    if final_match:
        print(
            f"RESULT:FAIL STEP:qa-health DETAIL:{DETAIL_CONTEXT_FAILED} "
            f"MATCH:{final_match} HTTP:{last_code} HEALTH:{last_health} "
            f"URL:{url} CONTAINER:{container} LOG:{log_file}"
        )
    else:
        print(
            f"RESULT:FAIL STEP:qa-health DETAIL:timeout after {timeout}s "
            f"(last_http={last_code} health={last_health}) URL:{url} "
            f"CONTAINER:{container} LOG:{log_file}"
        )
    return EXIT_SUBPROCESS_FAILED


def cmd_qa_down(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    """Destroy the H2 QA CMS cell so ports/disk are freed by default.

    Uses ``docker rm -f`` on the matrix cell. Install lives inside the
    container filesystem (no named multi-GB volume by default), so removing
    the container frees the port and disk used by the cell.
    """
    repo_root, _env_file, _compose_file = paths
    log_dir = _log_dir(repo_root)
    container = args.container or QA_CMS_CONTAINER
    dry_run = bool(args.dry_run)
    rc, log_file = _run_logged(
        "qa-down",
        _qa_destroy_argv(container),
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=dry_run,
    )
    # RESULT:OK/FAIL already emitted by _run_logged; add agent-parseable detail.
    print(f"QA_CONTAINER:{container}")
    if rc == EXIT_OK:
        print("QA_DETAIL:removed (ports/disk freed)")
    return rc


def cmd_qa_preflight(
    args: argparse.Namespace, paths: tuple[Path, Path, Path]
) -> int:
    """Run the rebuild-chain preflight (#2486 / #2532).

    Detects a stale WebUI WAR vs a freshly built sitemanage SNAPSHOT
    so the operator / agent does not launch a container that
    silently ships an outdated ``sitemanage-*.jar`` inside the WAR.
    Default uses SHA-256 content hash (mtime-resistant); pass
    ``--no-content-hash`` for legacy mtime-only. Delegates to
    ``docker/scripts/qa_preflight.py``.
    """
    import qa_preflight

    repo_root, _env_file, _compose_file = paths
    log_dir = _log_dir(repo_root)
    log_file = log_dir / f"qa-preflight-{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}.log"
    if args.dry_run:
        print("RESULT:OK STEP:qa-preflight LOG:")
        print("PREFLIGHT: dry-run — skipping filesystem checks")
        return EXIT_OK
    argv = [
        "--repo-root", str(repo_root),
        "--log-file", str(log_file),
        "--strict" if args.strict else "--no-strict",
    ]
    if getattr(args, "no_content_hash", False):
        argv.append("--no-content-hash")
    rc = qa_preflight.main(argv)
    # Mirror the rest of perc-devctl: emit a single RESULT line for agents.
    if rc == 0:
        print(f"RESULT:OK STEP:qa-preflight LOG:{log_file}")
    else:
        print(f"RESULT:FAIL STEP:qa-preflight LOG:{log_file}")
    return rc


# ---------------------------------------------------------------------------
# Dispatch
# ---------------------------------------------------------------------------


_DISPATCH = {
    "install": cmd_install,
    "up": cmd_up,
    "down": cmd_down,
    "status": cmd_status,
    "verify": cmd_verify,
    "it-verify": cmd_it_verify,
    "deploy-jar": cmd_deploy_jar,
    "verify-fix": cmd_verify_fix,
    "logs-path": cmd_logs_path,
    "inspect-install": cmd_inspect_install,
    "show-generated-passwords": cmd_show_generated_passwords,
    "qa-up": cmd_qa_up,
    "qa-health": cmd_qa_health,
    "qa-down": cmd_qa_down,
    "qa-preflight": cmd_qa_preflight,
}


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    paths = _resolve_paths(args)
    handler = _DISPATCH.get(args.command)
    if handler is None:
        LOG.error("ERROR: unknown subcommand: %s", args.command)
        return EXIT_INVOCATION
    return handler(args, paths)


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())
