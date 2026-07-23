#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Start a local Percussion Delivery Tier Suite instance.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Replaces ``modules/ai-shared-develop/src/main/resources/skills/
percussioncms-dev/scripts/start-dts.sh``.

The original ``.sh`` resolved which of ``TomcatStartup.sh`` or
``startup.sh`` existed, set up the JRE symlink, then
``exec ${STARTUP_SCRIPT}``. The Python port preserves this fallback
chain on POSIX via ``os.execvp`` and on Windows + dry-run via
``subprocess.run``.

## Behavioral Notes (FR-009b)

- Cross-platform ``os.execvp`` (POSIX) vs ``subprocess.run`` (Windows)
  pattern matches ``start-cms.py``.
- JRE symlink check + creation uses ``Path.is_symlink`` +
  ``Path.symlink_to`` (portable).
- Path discovery uses ``pathlib.Path``; no hardcoded separators.

Exit codes:

  0  DTS start script started (or dry-run completed)
  1  invocation error
  2  install_root / Deployment/Server missing
  3  no startup script found
"""

from __future__ import annotations

import argparse
import logging
import os
import subprocess
import sys
from pathlib import Path
from typing import Iterable, Optional

LOG = logging.getLogger("start-dts")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_INSTALL_MISSING = 2
EXIT_NO_STARTSCRIPT = 3

DEFAULT_INSTALL_DIR = os.path.expanduser("~/percussiondts-install")
PRIMARY_START_SCRIPT = "TomcatStartup.sh"
FALLBACK_START_SCRIPT = "startup.sh"
JRE_SYMLINK_NAME = "JRE"


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="start-dts.py",
        description=(
            "Start a local Percussion Delivery Tier Suite instance. "
            "Picks ``TomcatStartup.sh`` if present, else falls back to "
            "``startup.sh``."
        ),
    )
    p.add_argument(
        "--install-dir",
        type=Path,
        default=Path(os.environ.get("DTS_INSTALL_DIR", DEFAULT_INSTALL_DIR)),
        help=(
            "DTS installation directory "
            f"(default: {DEFAULT_INSTALL_DIR}; env DTS_INSTALL_DIR overrides)."
        ),
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the invocation that would be performed without starting the DTS.",
    )
    return p


def _setup_jre_symlink(install_dir: Path) -> int:
    jre_link = install_dir / JRE_SYMLINK_NAME
    if jre_link.is_symlink():
        return EXIT_OK
    java_home = os.environ.get("JAVA_HOME") or os.environ.get("JAVA_HOME_21")
    if not java_home:
        LOG.error("ERROR: Neither JAVA_HOME nor JAVA_HOME_21 is set.")
        return EXIT_NO_STARTSCRIPT
    LOG.warning("Creating JRE symlink: %s -> %s", jre_link, java_home)
    try:
        if jre_link.exists() or jre_link.is_symlink():
            jre_link.unlink()
        jre_link.symlink_to(Path(java_home), target_is_directory=True)
    except (OSError, NotImplementedError) as exc:
        LOG.error("ERROR: cannot create JRE symlink (%s).", exc)
        return EXIT_NO_STARTSCRIPT
    return EXIT_OK


def start(
    *,
    install_dir: Path,
    dry_run: bool,
) -> int:
    if not (install_dir / "Deployment" / "Server").is_dir():
        LOG.error(
            "ERROR: DTS installation not found at %s (Deployment/Server missing).",
            install_dir,
        )
        LOG.error("Run install-dts.py first.")
        return EXIT_INSTALL_MISSING

    primary = install_dir / PRIMARY_START_SCRIPT
    fallback = install_dir / FALLBACK_START_SCRIPT
    if primary.is_file():
        startup_script = primary
    elif fallback.is_file():
        startup_script = fallback
    else:
        LOG.error(
            "ERROR: No startup script found in %s. Expected %s or %s.",
            install_dir, PRIMARY_START_SCRIPT, FALLBACK_START_SCRIPT,
        )
        return EXIT_NO_STARTSCRIPT

    jre_rc = _setup_jre_symlink(install_dir)
    if jre_rc != EXIT_OK:
        return jre_rc

    LOG.info("Starting Percussion DTS from %s ...", install_dir)
    LOG.info("Using startup script: %s", startup_script)
    LOG.info("Press CTRL-C to stop.")

    if dry_run:
        LOG.info("DRY-RUN: exec %s (cwd=%s)", startup_script, install_dir)
        return EXIT_OK

    if os.name == "nt":
        LOG.warning(
            "Windows: launching .sh scripts natively is not supported. "
            "Use WSL, Git Bash, or docker compose up -d for the DTS."
        )
        completed = subprocess.run(
            [str(startup_script)],
            cwd=str(install_dir),
            shell=False,
            check=False,
        )
        return completed.returncode

    os.chdir(str(install_dir))
    os.execvp(str(startup_script), [str(startup_script)])


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    return start(install_dir=args.install_dir.resolve(), dry_run=args.dry_run)


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())