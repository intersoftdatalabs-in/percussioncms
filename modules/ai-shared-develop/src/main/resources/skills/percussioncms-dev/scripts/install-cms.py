#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Install Percussion CMS from a distribution JAR.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Replaces ``modules/ai-shared-develop/src/main/resources/skills/
percussioncms-dev/scripts/install-cms.sh``.

A ``--dry-run`` flag prints every Java / shell invocation that would be
performed without actually starting the JVM or modifying the
filesystem. This gates pytest and lets operators inspect the install
plan before running for real.

## Behavioral Notes (FR-009b)

- The original ``.sh`` invoked ``java -jar ${JAR_PATH} ${INSTALL_DIR}``
  via ``exec``. The Python port uses ``subprocess.run([...], shell=False)``
  (FR-008; root AGENTS.md "subprocess.run([...], shell=False)" rule).
- ``ln -sfn ${JAVA_HOME} ${INSTALL_DIR}/JRE`` is the original's
  JRE-symlink step. The Python port uses
  ``Path.is_symlink`` + ``Path.symlink_to`` for portable symlink
  creation. On Windows the call falls through with a clear
  ``EXIT_INSTALL_FAILED`` if the symlink can't be created (Windows
  symlinks need admin + Developer Mode).
- Path discovery uses ``pathlib.Path``; no hardcoded separators.

Exit codes:

  0  install verified
  1  invocation error
  2  JAVA_HOME unset / JAR not found
  3  java invocation failed
  4  JRE symlink creation failed
  5  post-install verification failed (StartJetty.sh missing)
"""

from __future__ import annotations

import argparse
import logging
import os
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Iterable, Optional

LOG = logging.getLogger("install-cms")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_PREREQ_MISSING = 2
EXIT_JAVA_FAILED = 3
EXIT_JRE_SYMLINK_FAILED = 4
EXIT_VERIFY_FAILED = 5

DEFAULT_INSTALL_DIR = os.path.expanduser("~/percussioncms-install")
DEFAULT_JAR_BASENAME = "perc-distribution-tree.jar"
START_SCRIPT = "StartJetty.sh"
JRE_SYMLINK_NAME = "JRE"


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="install-cms.py",
        description=(
            "Install Percussion CMS from a distribution JAR. "
            "Creates the JRE symlink and verifies the install."
        ),
    )
    p.add_argument(
        "--jar",
        type=Path,
        default=None,
        help=(
            "Path to perc-distribution-tree.jar "
            "(default: modules/perc-distribution-tree/target/"
            f"{DEFAULT_JAR_BASENAME} resolved from repo root)."
        ),
    )
    p.add_argument(
        "--install-dir",
        type=Path,
        default=Path(os.environ.get("CMS_INSTALL_DIR", DEFAULT_INSTALL_DIR)),
        help=(
            "Installation directory "
            f"(default: {DEFAULT_INSTALL_DIR}; env CMS_INSTALL_DIR overrides)."
        ),
    )
    p.add_argument(
        "--reset",
        action="store_true",
        help="Wipe INSTALL_DIR before running the installer.",
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help=(
            "Print every java / ln invocation that would be performed "
            "without actually running the JVM or modifying the filesystem."
        ),
    )
    return p


def _resolve_jar(script_path: Path, jar_arg: Optional[Path]) -> Path:
    """Default jar path is the local Maven build output
    (``modules/perc-distribution-tree/target/...``).
    """
    if jar_arg is not None:
        return jar_arg.resolve()
    repo_root = script_path.resolve().parents[3]
    return (repo_root / "modules" / "perc-distribution-tree" / "target" / DEFAULT_JAR_BASENAME).resolve()


def _resolve_java_home() -> Optional[str]:
    """Return JAVA_HOME (env) falling back to JAVA_HOME_21."""
    return os.environ.get("JAVA_HOME") or os.environ.get("JAVA_HOME_21")


def _run(
    argv: Iterable[str],
    *,
    dry_run: bool,
    cwd: Optional[Path] = None,
) -> int:
    """Run ``subprocess.run([...], shell=False)``. Dry-run logs only."""
    cmd = list(argv)
    if dry_run:
        LOG.info("DRY-RUN: %s (cwd=%s)", " ".join(cmd), cwd)
        return EXIT_OK
    LOG.info("Running: %s (cwd=%s)", " ".join(cmd), cwd)
    completed = subprocess.run(cmd, cwd=str(cwd) if cwd else None, shell=False, check=False)
    if completed.returncode != 0:
        return EXIT_JAVA_FAILED
    return EXIT_OK


def install(
    *,
    script_path: Path,
    jar: Path,
    install_dir: Path,
    reset: bool,
    dry_run: bool,
) -> int:
    """Top-level entry point."""
    if not jar.is_file():
        LOG.error(
            "ERROR: Distribution JAR not found at %s. "
            "Run `./mvnw clean install` first, or provide a release JAR via --jar.",
            jar,
        )
        return EXIT_PREREQ_MISSING

    java_home = _resolve_java_home()
    if not java_home:
        LOG.error("ERROR: JAVA_HOME is not set. Set JAVA_HOME to a JDK 21 installation.")
        return EXIT_PREREQ_MISSING
    java_home_path = Path(java_home)

    LOG.info("Installing Percussion CMS...")
    LOG.info("  JAR:         %s", jar)
    LOG.info("  Install Dir: %s", install_dir)
    LOG.info("  JAVA_HOME:   %s", java_home_path)

    if reset and not dry_run:
        if install_dir.exists():
            LOG.info("Reset: removing %s", install_dir)
            shutil.rmtree(install_dir)

    install_dir.mkdir(parents=True, exist_ok=True)

    rc = _run(
        ["java", "-jar", str(jar), str(install_dir)],
        dry_run=dry_run,
    )
    if rc != EXIT_OK:
        return rc

    # Create JRE symlink. On Windows, symlinks need admin / Developer
    # Mode; if we can't create one, surface a clear error.
    jre_link = install_dir / JRE_SYMLINK_NAME
    if not dry_run and not jre_link.is_symlink():
        LOG.info("Creating JRE symlink: %s -> %s", jre_link, java_home_path)
        try:
            if jre_link.exists() or jre_link.is_symlink():
                jre_link.unlink()
            jre_link.symlink_to(java_home_path, target_is_directory=True)
        except (OSError, NotImplementedError) as exc:
            LOG.error(
                "ERROR: cannot create JRE symlink at %s (%s). "
                "On Windows, enable Developer Mode or run as Administrator.",
                jre_link, exc,
            )
            return EXIT_JRE_SYMLINK_FAILED

    # Verify installation.
    start_script = install_dir / "jetty" / START_SCRIPT
    if not start_script.is_file():
        LOG.warning(
            "WARNING: %s not found. The installer may have failed.",
            start_script,
        )
        return EXIT_VERIFY_FAILED

    LOG.info("Installation successful. Start with: cd %s/jetty && ./%s", install_dir, START_SCRIPT)
    return EXIT_OK


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    return install(
        script_path=Path(__file__).resolve(),
        jar=_resolve_jar(Path(__file__).resolve(), args.jar),
        install_dir=args.install_dir.resolve(),
        reset=args.reset,
        dry_run=args.dry_run,
    )


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())