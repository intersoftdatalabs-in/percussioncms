#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform consolidated docker dev/test stack control.

Replaces ``docker/scripts/perc-devctl.sh`` (390 lines of bash). The
script is the operator-facing entry point for the cms-dts dev/test
docker compose stack and exposes every subcommand the original
``.sh`` provided: ``install``, ``up``, ``down``, ``status``, ``verify``,
``it-verify``, ``deploy-jar``, ``verify-fix``, ``logs-path``,
``inspect-install``, ``show-generated-passwords``.

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
import shlex
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, List, Optional, Sequence

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

# HTTP endpoints used by `verify`.
VERIFY_CMS_URL = "http://localhost:9992/Rhythmyx/rest/folders/by-path/Assets"
VERIFY_DTS_URL = "http://localhost:9980/"


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
    """Return the docker health status string for ``container_name``,
    or 'unknown' if docker is missing / container absent.
    """
    completed = subprocess.run(
        ["docker", "inspect", "-f", "{{.State.Health.Status}}", container_name],
        shell=False,
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        return "unknown"
    return (completed.stdout or "").strip() or "unknown"


def cmd_verify(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    timeout = args.timeout_seconds
    interval = args.interval_seconds
    max_checks = max(1, timeout // interval) if interval > 0 else 1
    log_file = _new_log_file(log_dir, "verify")

    if args.dry_run:
        LOG.info(
            "DRY-RUN: verify plan: %d checks x %ds interval against %s + %s",
            max_checks, interval, VERIFY_CMS_URL, VERIFY_DTS_URL,
        )
        with log_file.open("w", encoding="utf-8") as f:
            f.write(
                f"DRY-RUN: verify max_checks={max_checks} interval={interval}\n"
                f"endpoints={VERIFY_CMS_URL} {VERIFY_DTS_URL}\n"
            )
        print(f"RESULT:OK STEP:verify CMS_HTTP:200 DTS_HTTP:200 HEALTH:healthy LOG:{log_file}")
        return EXIT_OK

    for check in range(1, max_checks + 1):
        cms_code = _curl_status(VERIFY_CMS_URL, timeout=5.0)
        dts_code = _curl_status(VERIFY_DTS_URL, timeout=5.0)
        health = _docker_health(DEFAULT_CONTAINER)
        if (
            cms_code in (200, 401, 403)
            and dts_code in (200, 401, 403)
            and health == "healthy"
        ):
            with log_file.open("w", encoding="utf-8") as f:
                f.write("verify success\n")
                f.write(f"cms_http={cms_code}\n")
                f.write(f"dts_http={dts_code}\n")
                f.write(f"container_health={health}\n")
            print(
                f"RESULT:OK STEP:verify CMS_HTTP:{cms_code} DTS_HTTP:{dts_code} "
                f"HEALTH:{health} LOG:{log_file}"
            )
            return EXIT_OK
        time.sleep(interval)

    with log_file.open("w", encoding="utf-8") as f:
        f.write("verify failed\n")
        f.write(f"timeout_seconds={timeout}\n")
        f.write(f"interval_seconds={interval}\n")
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
    print(f"RESULT:FAIL STEP:verify LOG:{log_file}")
    return EXIT_SUBPROCESS_FAILED


def cmd_it_verify(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    rc, _log_path = _run_logged(
        "it-verify",
        [
            str(repo_root / "mvnw"),
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
    Mirrors ``cmd_verify`` but bypasses the wrapped ``verify`` parser
    argument. Returns ``(exit_code, log_file_path)`` so callers
    (e.g. ``cmd_verify_fix``) can include the log path in their own
    ``RESULT:FAIL`` lines.
    """
    max_checks = max(1, timeout_seconds // interval_seconds) if interval_seconds > 0 else 1
    log_file = _new_log_file(log_dir, "verify")

    if dry_run:
        with log_file.open("w", encoding="utf-8") as f:
            f.write(
                f"DRY-RUN: verify-inline max_checks={max_checks} interval={interval_seconds}\n"
            )
        return EXIT_OK, log_file

    for _ in range(1, max_checks + 1):
        cms_code = _curl_status(VERIFY_CMS_URL, timeout=5.0)
        dts_code = _curl_status(VERIFY_DTS_URL, timeout=5.0)
        health = _docker_health(DEFAULT_CONTAINER)
        if (
            cms_code in (200, 401, 403)
            and dts_code in (200, 401, 403)
            and health == "healthy"
        ):
            with log_file.open("w", encoding="utf-8") as f:
                f.write("verify success\n")
                f.write(f"cms_http={cms_code}\n")
                f.write(f"dts_http={dts_code}\n")
                f.write(f"container_health={health}\n")
            print(
                f"RESULT:OK STEP:verify CMS_HTTP:{cms_code} DTS_HTTP:{dts_code} "
                f"HEALTH:{health} LOG:{log_file}"
            )
            return EXIT_OK, log_file
        time.sleep(interval_seconds)

    with log_file.open("w", encoding="utf-8") as f:
        f.write("verify failed\n")
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