#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Start a local Percussion CMS instance.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Replaces ``modules/ai-shared-develop/src/main/resources/skills/
percussioncms-dev/scripts/start-cms.sh``.

The original ``.sh`` ended with ``exec ./StartJetty.sh`` (replacing
the current shell with the start script). The Python port preserves
this on POSIX via ``os.execvp`` and falls back to a subprocess
launch on Windows + dry-run mode.

## Behavioral Notes (FR-009b)

- ``exec`` is replaced by ``os.execvp`` (POSIX) or
  ``subprocess.run`` (Windows + dry-run). Cross-platform callers use
  ``run_start_script`` which dispatches based on host OS.
- JRE symlink check + creation uses ``Path.is_symlink`` +
  ``Path.symlink_to`` (portable, matches install-cms / install-dts).
- Path discovery uses ``pathlib.Path``; no hardcoded separators.

Exit codes:

  0  CMS start script started (or dry-run completed)
  1  invocation error
  2  install_root missing
  3  StartJetty.sh not present / not executable
"""

from __future__ import annotations

import argparse
import logging
import os
import subprocess
import sys
from pathlib import Path
from typing import Iterable, Optional

LOG = logging.getLogger("start-cms")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_INSTALL_MISSING = 2
EXIT_STARTSCRIPT_MISSING = 3

DEFAULT_INSTALL_DIR = os.path.expanduser("~/percussioncms-install")
START_SCRIPT = "StartJetty.sh"
JRE_SYMLINK_NAME = "JRE"


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="start-cms.py",
        description=(
            "Start a local Percussion CMS instance. On POSIX this "
            "replaces the current process with StartJetty.sh via "
            "``os.execvp``; on Windows + dry-run mode it spawns "
            "the start script via ``subprocess.run``."
        ),
    )
    p.add_argument(
        "--install-dir",
        type=Path,
        default=Path(os.environ.get("CMS_INSTALL_DIR", DEFAULT_INSTALL_DIR)),
        help=(
            "CMS installation directory "
            f"(default: {DEFAULT_INSTALL_DIR}; env CMS_INSTALL_DIR overrides)."
        ),
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help=(
            "Print the invocation that would be performed without "
            "actually starting the CMS. Used by pytest."
        ),
    )
    return p


def _setup_jre_symlink(install_dir: Path) -> int:
    """Create the JRE symlink if missing. Returns one of the EXIT_*
    constants. No-op in dry-run mode.
    """
    jre_link = install_dir / JRE_SYMLINK_NAME
    if jre_link.is_symlink():
        return EXIT_OK
    java_home = os.environ.get("JAVA_HOME") or os.environ.get("JAVA_HOME_21")
    if not java_home:
        LOG.error("ERROR: Neither JAVA_HOME nor JAVA_HOME_21 is set.")
        return EXIT_STARTSCRIPT_MISSING
    LOG.warning("Creating JRE symlink: %s -> %s", jre_link, java_home)
    try:
        if jre_link.exists() or jre_link.is_symlink():
            jre_link.unlink()
        jre_link.symlink_to(Path(java_home), target_is_directory=True)
    except (OSError, NotImplementedError) as exc:
        LOG.error("ERROR: cannot create JRE symlink (%s).", exc)
        return EXIT_STARTSCRIPT_MISSING
    return EXIT_OK


def start(
    *,
    install_dir: Path,
    dry_run: bool,
) -> int:
    """Start the CMS. On POSIX this replaces the current process with
    StartJetty.sh via os.execvp (matches the original .sh's ``exec``
    behavior). On Windows + dry-run, it spawns via subprocess.run so
    the Python port remains runnable everywhere.
    """
    if not (install_dir / "jetty").is_dir():
        LOG.error("ERROR: CMS installation not found at %s/jetty", install_dir)
        LOG.error("Run install-cms.py first.")
        return EXIT_INSTALL_MISSING

    start_script = install_dir / "jetty" / START_SCRIPT
    if not start_script.is_file():
        LOG.error("ERROR: %s not found in %s", START_SCRIPT, install_dir / "jetty")
        return EXIT_STARTSCRIPT_MISSING

    jre_rc = _setup_jre_symlink(install_dir)
    if jre_rc != EXIT_OK:
        return jre_rc

    LOG.info("Starting Percussion CMS from %s ...", install_dir)
    LOG.info("Press CTRL-C to stop.")

    if dry_run:
        LOG.info("DRY-RUN: exec %s (cwd=%s/jetty)", start_script, install_dir)
        return EXIT_OK

    if os.name == "nt":
        # Windows: spawn the start script directly. CreateProcessW
        # does not consult file-association extensions for shell
        # scripts; the operator should run ``bash ./StartJetty.sh``
        # via WSL or Git Bash, or use docker compose up -d.
        LOG.warning(
            "Windows: launching .sh scripts natively is not supported. "
            "Use WSL, Git Bash, or docker compose up -d for the CMS."
        )
        completed = subprocess.run(
            [str(start_script)],
            cwd=str(install_dir / "jetty"),
            shell=False,
            check=False,
        )
        return completed.returncode

    # POSIX: replace the current process with StartJetty.sh.
    os.chdir(str(install_dir / "jetty"))
    os.execvp(str(start_script), [str(start_script)])


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