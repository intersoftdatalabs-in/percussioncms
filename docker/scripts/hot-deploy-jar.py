#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Hot-deploy a built module jar into a running cms-dts docker container.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Replaces ``docker/scripts/hot-deploy-jar.sh``. The script copies a built
module jar into a running container for fast validation. The original
.sh used ``docker exec ... bash -lc ...`` for each command; the Python
port uses ``subprocess.run([...], shell=False)`` (FR-008; root AGENTS.md
"subprocess.run([...], shell=False)" rule).

A ``--dry-run`` flag prints every docker invocation that would be
performed without touching the host filesystem or running docker.
This gates pytest and lets operators review the deploy plan before
running it for real.

## Behavioral Notes (FR-009b)

- The original ``.sh`` used ``basename "$JAR_PATH"`` + ``date +%Y%m%d%H%M%S``
  for timestamped backup names (e.g. ``foo.jar.bak.20260722120000``).
  Python preserves this format (``%Y%m%d%H%M%S``) so existing backup
  naming conventions are unchanged.
- The original ``.sh`` used ``bash -lc "..."`` for in-container
  commands. The Python port passes argv lists to ``docker exec``
  without a shell so the same command runs identically on
  Windows + Unix. The shell-specific ``.bashrc`` / ``/etc/profile``
  side effects of ``bash -l`` are not preserved — this is a known
  deviation, called out so operators who relied on ``~/.bashrc``
  exports in their container know to set them in ``docker-compose.yml``
  instead.
- Path discovery uses ``pathlib.Path``; the cross-platform
  ``docker cp host:container`` accepts forward-slash or backslash
  paths on Windows so ``Path.as_posix()`` is used in argv.
- All external invocations use ``subprocess.run([...], shell=False,
  check=False)`` so a failed docker command surfaces as a non-zero
  exit code without raising.

Exit codes:

  0  deploy complete
  1  invocation / argument error
  2  container not running
  3  jar not found
  4  unsupported --target value
  5  docker cp / docker exec failed
"""

from __future__ import annotations

import argparse
import logging
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, Optional

LOG = logging.getLogger("hot-deploy-jar")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_CONTAINER_NOT_RUNNING = 2
EXIT_JAR_NOT_FOUND = 3
EXIT_UNSUPPORTED_TARGET = 4
EXIT_DOCKER_FAILED = 5

DEFAULT_CONTAINER = "percussion-cms-dts"
DEFAULT_TARGET = "both"

# Target name -> absolute container directory.
KNOWN_TARGETS: dict[str, str] = {
    "cms": "/opt/Percussion/jetty/base/lib",
    "dts": "/opt/Percussion/Deployment/Server/lib",
}


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="hot-deploy-jar.py",
        description=(
            "Copy a built module jar into a running cms-dts docker "
            "container for fast validation. Default container: "
            f"{DEFAULT_CONTAINER}; default target: {DEFAULT_TARGET}."
        ),
    )
    p.add_argument("--jar", required=True, type=Path, help="Path to the built jar (required).")
    p.add_argument(
        "--container",
        default=DEFAULT_CONTAINER,
        help=f"Target container name (default: {DEFAULT_CONTAINER}).",
    )
    p.add_argument(
        "--target",
        default=DEFAULT_TARGET,
        help=(
            "Where to deploy: 'cms', 'dts', 'both', or an absolute "
            "container path like '/opt/Percussion/jetty/base/lib' "
            f"(default: {DEFAULT_TARGET})."
        ),
    )
    p.add_argument(
        "--restart",
        action="store_true",
        help="Restart the container after deploy so the jar change takes effect.",
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help=(
            "Print every docker invocation that would be performed "
            "without touching the host filesystem or running docker. "
            "Used by pytest to exercise the wiring without paying the "
            "container-startup cost."
        ),
    )
    return p


def _run(argv0: Iterable[str], *, dry_run: bool) -> int:
    """Run a docker argv list via ``subprocess.run([...], shell=False)``.

    Under ``--dry-run`` the command is logged and 0 is returned. Real
    invocations propagate ``EXIT_DOCKER_FAILED`` on non-zero exit so
    callers can rely on the script-level exit codes documented above
    (``0`` / ``1`` / ``2`` / ``3`` / ``4`` / ``5``) — mapping docker's
    raw exit code (``1`` for "container not found", ``125`` for
    "container not running", etc.) into the script's exit-code
    vocabulary.
    """
    cmd = list(argv0)
    if dry_run:
        LOG.info("DRY-RUN: %s", " ".join(cmd))
        return EXIT_OK
    LOG.info("Running: %s", " ".join(cmd))
    completed = subprocess.run(cmd, shell=False, check=False)
    if completed.returncode != 0:
        return EXIT_DOCKER_FAILED
    return EXIT_OK


def _container_running(container_name: str, *, dry_run: bool) -> bool:
    """Return True if ``container_name`` is currently running. Under
    ``--dry-run`` we cannot inspect docker, so we report True and let
    downstream calls fail in dry-run if the operator wants strict
    validation.
    """
    if dry_run:
        return True
    completed = subprocess.run(
        ["docker", "ps", "--format", "{{.Names}}"],
        capture_output=True,
        text=True,
        shell=False,
        check=False,
    )
    if completed.returncode != 0:
        return False
    running = {
        line.strip()
        for line in (completed.stdout or "").splitlines()
        if line.strip()
    }
    return container_name in running


def _resolve_targets(target: str) -> list[str]:
    """Return the list of absolute container directories to deploy to.
    Raises ``ValueError`` if ``target`` is not one of the known
    shortcuts and not an absolute path.
    """
    if target in KNOWN_TARGETS:
        return [KNOWN_TARGETS[target]]
    if target == "both":
        return [KNOWN_TARGETS["cms"], KNOWN_TARGETS["dts"]]
    if target.startswith("/"):
        return [target]
    raise ValueError(
        f"unsupported --target value: {target!r}; "
        "use cms|dts|both or an absolute container path"
    )


def _deploy_to_path(
    jar_path: Path,
    container_name: str,
    target_dir: str,
    *,
    dry_run: bool,
) -> int:
    """Copy ``jar_path`` into ``target_dir`` inside ``container_name``
    with a timestamped backup of any existing jar. Returns one of the
    script exit codes.
    """
    jar_basename = jar_path.name
    target_jar = f"{target_dir}/{jar_basename}"
    ts = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
    backup_jar = f"{target_jar}.bak.{ts}"

    LOG.info("Deploying %s -> %s:%s", jar_basename, container_name, target_dir)

    # mkdir -p the target dir inside the container.
    rc = _run(
        [
            "docker", "exec", container_name,
            "mkdir", "-p", target_dir,
        ],
        dry_run=dry_run,
    )
    if rc != EXIT_OK:
        return EXIT_DOCKER_FAILED

    # If a previous jar exists, copy it aside as .bak.<TS>.
    #
    # Security: do NOT use ``docker exec ... sh -c "if [ -f '<jar>' ]..."``
    # with the jar name interpolated into the shell snippet. ``--jar`` is
    # a CLI arg (operator-controlled) and a name like ``foo'; rm -rf /; '``
    # would inject arbitrary shell commands. Use ``stat`` (external
    # command, accepts the path as a single argv element) to probe for
    # the file's existence, then ``mv`` for the rename — both pass
    # ``target_jar`` / ``backup_jar`` as argv elements, not shell-quoted
    # fragments, so a malicious name cannot break out of the docker
    # exec namespace. (kilo-code-bot review thread 3631740669.)
    if dry_run:
        LOG.info(
            "DRY-RUN: backup-if-exists: docker exec %s stat %s ; then mv %s %s",
            container_name, target_jar, target_jar, backup_jar,
        )
    else:
        check_rc = subprocess.run(
            [
                "docker", "exec", container_name,
                "stat", target_jar,
            ],
            capture_output=True,
            shell=False,
            check=False,
        )
        if check_rc.returncode == 0:
            backup_rc = _run(
                [
                    "docker", "exec", container_name,
                    "mv", target_jar, backup_jar,
                ],
                dry_run=False,
            )
            if backup_rc != EXIT_OK:
                return EXIT_DOCKER_FAILED

    # ``docker cp host container:remote`` is the actual deploy step.
    # On Windows, ``Path.as_posix()`` produces ``C:/Users/.../foo.jar``;
    # docker accepts that form on Windows + Unix hosts.
    return _run(
        [
            "docker", "cp",
            str(jar_path.resolve()),
            f"{container_name}:{target_jar}",
        ],
        dry_run=dry_run,
    )


def deploy(
    *,
    jar_path: Path,
    container_name: str,
    target: str,
    restart: bool,
    dry_run: bool,
) -> int:
    """Top-level entry point used by both ``main()`` and pytest tests."""
    if not jar_path.is_file():
        LOG.error("ERROR: jar not found: %s", jar_path)
        return EXIT_JAR_NOT_FOUND

    if not _container_running(container_name, dry_run=dry_run):
        LOG.error("ERROR: container is not running: %s", container_name)
        return EXIT_CONTAINER_NOT_RUNNING

    try:
        targets = _resolve_targets(target)
    except ValueError as exc:
        LOG.error("ERROR: %s", exc)
        return EXIT_UNSUPPORTED_TARGET

    for target_dir in targets:
        rc = _deploy_to_path(
            jar_path,
            container_name,
            target_dir,
            dry_run=dry_run,
        )
        if rc != EXIT_OK:
            return rc

    if restart:
        rc = _run(["docker", "restart", container_name], dry_run=dry_run)
        if rc != EXIT_OK:
            return EXIT_DOCKER_FAILED

    LOG.info("Hot deploy complete.")
    return EXIT_OK


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    return deploy(
        jar_path=args.jar,
        container_name=args.container,
        target=args.target,
        restart=args.restart,
        dry_run=args.dry_run,
    )


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())