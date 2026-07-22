#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Container entrypoint for the cms-dts dev/test docker compose stack.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Replaces ``docker/entrypoint/install-update.sh``. **Service-only mode** as
of the install-on-host refactor (992-react-content-explorer story automation
follow-up): the install runs ONCE on the host via ``scripts/install-cms-dev.py``;
this entrypoint only starts the service.

Contract with the host bind mount:
  ``docker-compose.yml`` bind-mounts ``./docker/dev-data/cms-dts/install_root/``
  into the container at ``/opt/Percussion/``. The install lives entirely
  on the host; the container is responsible for executing the service.

If ``/opt/Percussion/jetty/StartJetty.sh`` is missing on entry, this
script exits non-zero with a clear pointer to the host-side installer.

## Behavioral Notes (FR-009b)

- The original ``.sh`` used ``set -euo pipefail`` for early-fail. The
  Python port raises explicit exceptions (``subprocess.CalledProcessError``
  via ``check=True``) so failures are caught at the right granularity
  and the entrypoint can decide whether to halt.
- The original ``.sh`` used ``./StartJetty.sh`` and ``./TomcatStartup.sh``
  via a subshell ``(cd ... && ./X.sh)``. The Python port uses
  ``subprocess.run([...], cwd=...)`` (R2) so the working directory is
  set without a shell.
- The original ``.sh`` used ``tail -F ${log_files[@]}`` (foreground log
  streaming). The Python port uses ``subprocess.Popen`` with
  ``stdout=DEVNULL`` for ``tail -f /dev/null`` semantics, or a list of
  tail argv for actual log files.
- Path discovery uses ``pathlib.Path``; ``INSTALL_ROOT`` env var
  defaults to ``/opt/Percussion`` (matches the original).
- Cross-platform: Linux/macOS containers (Jetty + bash). Windows
  containers (if any) would require a different launcher; documented
  out-of-scope for the dev/test runtime — same caveat as the original
  ``.sh``.

Exit codes:

  0  service started (the script then ``exec``s into ``tail`` to keep
     the container in foreground; the script's own exit is never
     observed in normal operation)
  1  install missing on host (StartJetty.sh not present in bind mount)
  2  start script failed (subprocess returned non-zero)
  3  unsupported SERVICE_MODE
"""

from __future__ import annotations

import argparse
import logging
import os
import subprocess
import sys
from pathlib import Path
from typing import Iterable, Optional

LOG = logging.getLogger("install-update")

EXIT_OK = 0
EXIT_INSTALL_MISSING = 1
EXIT_START_FAILED = 2
EXIT_UNSUPPORTED_MODE = 3

DEFAULT_INSTALL_ROOT = "/opt/Percussion"
DEFAULT_SERVICE_MODE = "cms-dts"

CMS_START_SCRIPT_NAME = "StartJetty.sh"
DTS_START_SCRIPT_PRIMARY = "TomcatStartup.sh"
DTS_START_SCRIPT_FALLBACK = "startup.sh"


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="install-update.py",
        description=(
            "Container entrypoint: start the CMS and/or DTS service "
            "from a host-bind-mounted install_root. The install lives "
            "on the host (see scripts/install-cms-dev.py); this script "
            "only starts the service."
        ),
    )
    p.add_argument(
        "--install-root",
        default=os.environ.get("PERC_INSTALL_ROOT", DEFAULT_INSTALL_ROOT),
        help=(
            "Container-side install root "
            "(default: $PERC_INSTALL_ROOT env var or /opt/Percussion)."
        ),
    )
    p.add_argument(
        "--service-mode",
        choices=("cms", "dts", "cms-dts"),
        default=os.environ.get("SERVICE_MODE", DEFAULT_SERVICE_MODE),
        help=(
            "Which services to start "
            "(default: $SERVICE_MODE env var or 'cms-dts')."
        ),
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help=(
            "Print every subprocess invocation that would be performed "
            "without actually starting the service. Used by pytest to "
            "exercise the wiring without a real Jetty/DTS dependency."
        ),
    )
    return p


def _start_cms(
    install_root: Path,
    *,
    dry_run: bool,
) -> int:
    """Start the CMS service by running ``StartJetty.sh`` in ``install_root/jetty``."""
    cms_start = install_root / "jetty" / CMS_START_SCRIPT_NAME
    if not cms_start.is_file():
        LOG.error(
            "ERROR: CMS start script missing: %s. "
            "Run scripts/install-cms-dev.py on the host first, then "
            "docker compose up -d.",
            cms_start,
        )
        return EXIT_INSTALL_MISSING

    if dry_run:
        LOG.info(
            "DRY-RUN: %s (cwd=%s)",
            cms_start,
            install_root / "jetty",
        )
        return EXIT_OK

    LOG.info("Starting CMS via %s", cms_start)
    completed = subprocess.run(
        [str(cms_start)],
        cwd=str(install_root / "jetty"),
        shell=False,
        check=False,
    )
    return (
        EXIT_OK
        if completed.returncode == 0
        else EXIT_START_FAILED
    )


def _start_dts(
    install_root: Path,
    *,
    dry_run: bool,
) -> int:
    """Start the DTS service by running ``TomcatStartup.sh`` (with
    ``startup.sh`` as fallback) in ``install_root``.
    """
    primary = install_root / DTS_START_SCRIPT_PRIMARY
    fallback = install_root / DTS_START_SCRIPT_FALLBACK
    if primary.is_file():
        dts_start = primary
    elif fallback.is_file():
        dts_start = fallback
    else:
        LOG.error(
            "ERROR: DTS start script missing: %s (or fallback %s). "
            "Run scripts/install-cms-dev.py on the host first.",
            primary,
            fallback,
        )
        return EXIT_INSTALL_MISSING

    if dry_run:
        LOG.info("DRY-RUN: %s (cwd=%s)", dts_start, install_root)
        return EXIT_OK

    LOG.info("Starting DTS via %s", dts_start)
    completed = subprocess.run(
        [str(dts_start)],
        cwd=str(install_root),
        shell=False,
        check=False,
    )
    return (
        EXIT_OK
        if completed.returncode == 0
        else EXIT_START_FAILED
    )


def _stream_logs_foreground(
    install_root: Path,
    *,
    dry_run: bool,
) -> int:
    """Stream CMS + DTS log files to keep the container in the foreground.
    Under ``--dry-run`` we just log the plan; in production we ``exec``
    into ``tail -F`` which replaces the current process.

    Cross-platform: ``tail -F`` is GNU tail behavior (follow by name —
    reopen the file if it's rotated/recreated). On Linux + macOS this
    works out of the box. On Windows containers the script notes that
    a different launcher is needed (matches the original ``.sh`` caveat).
    """
    log_files = sorted(
        list((install_root / "jetty/base/logs").glob("*.log"))
        + list((install_root / "Deployment/Server/logs").glob("*.log"))
    )

    if not log_files:
        LOG.info(
            "No log files found yet. Keeping container alive while waiting "
            "for logs."
        )
        if dry_run:
            LOG.info("DRY-RUN: tail -f /dev/null (foreground)")
            return EXIT_OK
        # ``tail -f /dev/null`` keeps the process alive indefinitely.
        os.execvp("tail", ["tail", "-f", "/dev/null"])

    LOG.info(
        "Streaming %d log file(s) to keep container in foreground.",
        len(log_files),
    )
    if dry_run:
        LOG.info("DRY-RUN: tail -F %s", " ".join(str(f) for f in log_files))
        return EXIT_OK
    # ``tail -F`` is the original's choice; preserve it (FR-002).
    argv = ["tail", "-F", *(str(f) for f in log_files)]
    os.execvp("tail", argv)
    # ``execvp`` doesn't return on success — defensive return for the
    # type checker + an unreachable code path in real execution.
    return EXIT_OK


def run(
    *,
    install_root: Path,
    service_mode: str,
    dry_run: bool,
) -> int:
    """Top-level entry point used by both ``main()`` and pytest tests."""
    cms_script = install_root / "jetty" / CMS_START_SCRIPT_NAME
    if not cms_script.is_file():
        LOG.error(
            "ERROR: %s not present. Did the host-side installer run? "
            "See scripts/install-cms-dev.py.",
            cms_script,
        )
        return EXIT_INSTALL_MISSING

    if service_mode == "cms":
        rc = _start_cms(install_root, dry_run=dry_run)
        if rc != EXIT_OK:
            return rc
        return _stream_logs_foreground(install_root, dry_run=dry_run)

    if service_mode == "dts":
        rc = _start_dts(install_root, dry_run=dry_run)
        if rc != EXIT_OK:
            return rc
        return _stream_logs_foreground(install_root, dry_run=dry_run)

    if service_mode == "cms-dts":
        rc = _start_cms(install_root, dry_run=dry_run)
        if rc != EXIT_OK:
            return rc
        rc = _start_dts(install_root, dry_run=dry_run)
        if rc != EXIT_OK:
            return rc
        return _stream_logs_foreground(install_root, dry_run=dry_run)

    LOG.error(
        "ERROR: Unsupported SERVICE_MODE=%r; expected cms|dts|cms-dts",
        service_mode,
    )
    return EXIT_UNSUPPORTED_MODE


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    return run(
        install_root=Path(args.install_root),
        service_mode=args.service_mode,
        dry_run=args.dry_run,
    )


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="[install-update] %(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())