#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Install Percussion Delivery Tier Suite from a distribution JAR.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Replaces ``modules/ai-shared-develop/src/main/resources/skills/
percussioncms-dev/scripts/install-dts.sh``.

## Behavioral Notes (FR-009b)

- The original ``.sh`` invoked ``java -jar ${JAR_PATH} ${INSTALL_DIR}``
  and verified via ``Deployment/Server`` existence. The Python port
  preserves both via ``subprocess.run([...], shell=False)`` (FR-008) +
  ``Path.is_dir()``.
- ``ln -sfn ${JAVA_HOME} ${INSTALL_DIR}/JRE`` symlink step uses
  ``Path.symlink_to`` for portability.
- Path discovery uses ``pathlib.Path``; no hardcoded separators.

Exit codes:

  0  install verified
  1  invocation error
  2  JAVA_HOME unset / JAR not found
  3  java invocation failed
  4  JRE symlink creation failed
  5  post-install verification failed (Deployment/Server missing)
"""

from __future__ import annotations

import argparse
import logging
import os
import subprocess
import sys
from pathlib import Path
from typing import Iterable, Optional

LOG = logging.getLogger("install-dts")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_PREREQ_MISSING = 2
EXIT_JAVA_FAILED = 3
EXIT_JRE_SYMLINK_FAILED = 4
EXIT_VERIFY_FAILED = 5

DEFAULT_INSTALL_DIR = os.path.expanduser("~/percussiondts-install")
DEFAULT_JAR_BASENAME = "delivery-tier-distribution.jar"
JRE_SYMLINK_NAME = "JRE"


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="install-dts.py",
        description=(
            "Install Percussion Delivery Tier Suite from a "
            "distribution JAR."
        ),
    )
    p.add_argument(
        "--jar",
        type=Path,
        default=None,
        help=(
            "Path to delivery-tier-distribution.jar "
            "(default: deliverytiersuite/delivery-tier-suite/"
            f"delivery-tier-distribution/target/{DEFAULT_JAR_BASENAME} "
            "resolved from repo root)."
        ),
    )
    p.add_argument(
        "--install-dir",
        type=Path,
        default=Path(os.environ.get("DTS_INSTALL_DIR", DEFAULT_INSTALL_DIR)),
        help=(
            f"Installation directory (default: {DEFAULT_INSTALL_DIR}; "
            "env DTS_INSTALL_DIR overrides)."
        ),
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
    if jar_arg is not None:
        return jar_arg.resolve()
    repo_root = script_path.resolve().parents[3]
    return (
        repo_root
        / "deliverytiersuite"
        / "delivery-tier-suite"
        / "delivery-tier-distribution"
        / "target"
        / DEFAULT_JAR_BASENAME
    ).resolve()


def _resolve_java_home() -> Optional[str]:
    return os.environ.get("JAVA_HOME") or os.environ.get("JAVA_HOME_21")


def _run_java(argv: Iterable[str], *, dry_run: bool) -> int:
    cmd = list(argv)
    if dry_run:
        LOG.info("DRY-RUN: %s", " ".join(cmd))
        return EXIT_OK
    LOG.info("Running: %s", " ".join(cmd))
    completed = subprocess.run(cmd, shell=False, check=False)
    if completed.returncode != 0:
        return EXIT_JAVA_FAILED
    return EXIT_OK


def install(
    *,
    script_path: Path,
    jar: Path,
    install_dir: Path,
    dry_run: bool,
) -> int:
    if not jar.is_file():
        LOG.error(
            "ERROR: DTS distribution JAR not found at %s. "
            "Run `./mvn-env.sh clean install` first.",
            jar,
        )
        return EXIT_PREREQ_MISSING

    java_home = _resolve_java_home()
    if not java_home:
        LOG.error("ERROR: JAVA_HOME is not set. Set JAVA_HOME to a JDK 21 installation.")
        return EXIT_PREREQ_MISSING
    java_home_path = Path(java_home)

    LOG.info("Installing Percussion DTS...")
    LOG.info("  JAR:         %s", jar)
    LOG.info("  Install Dir: %s", install_dir)
    LOG.info("  JAVA_HOME:   %s", java_home_path)

    install_dir.mkdir(parents=True, exist_ok=True)

    rc = _run_java(
        ["java", "-jar", str(jar), str(install_dir)],
        dry_run=dry_run,
    )
    if rc != EXIT_OK:
        return rc

    jre_link = install_dir / JRE_SYMLINK_NAME
    if not dry_run and not jre_link.is_symlink():
        LOG.info("Creating JRE symlink: %s -> %s", jre_link, java_home_path)
        try:
            if jre_link.exists() or jre_link.is_symlink():
                jre_link.unlink()
            jre_link.symlink_to(java_home_path, target_is_directory=True)
        except (OSError, NotImplementedError) as exc:
            LOG.error(
                "ERROR: cannot create JRE symlink at %s (%s).",
                jre_link, exc,
            )
            return EXIT_JRE_SYMLINK_FAILED

    if not (install_dir / "Deployment" / "Server").is_dir():
        LOG.warning(
            "WARNING: DTS install may have failed — Deployment/Server missing."
        )
        return EXIT_VERIFY_FAILED

    LOG.info("DTS Installation successful. Start with: cd %s && ./TomcatStartup.sh", install_dir)
    return EXIT_OK


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    return install(
        script_path=Path(__file__).resolve(),
        jar=_resolve_jar(Path(__file__).resolve(), args.jar),
        install_dir=args.install_dir.resolve(),
        dry_run=args.dry_run,
    )


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())