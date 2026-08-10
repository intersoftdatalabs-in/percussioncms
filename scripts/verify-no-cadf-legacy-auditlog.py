#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Grep gate: CADF / jcadf-master / legacy com.percussion.auditlog must stay gone.

Purpose
-------
Phase 2c (#2617 / #2675) removed IBM CADF (``modules/jcadf-master``,
``com.ibm.cadf:auditlogger``) and the legacy CADF-facing package
``com.percussion.auditlog`` from ``modules/perc-auditlog``. Production audit
logging is ``com.intsof.percussioncms.auditlog`` only.

This script fails if those surfaces reappear in production source or Maven
POMs. Historical docs under ``docs/`` and ``specs/`` may still mention CADF.

Usage
-----
::

    python3 scripts/verify-no-cadf-legacy-auditlog.py [--repo-root <path>]

Exit codes
----------
- ``0`` clean
- ``1`` at least one violation

Portable: Python 3.9+; uses ``git grep`` (tracked files only) with
``shell=False``.
"""
from __future__ import annotations

import argparse
import logging
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

# Directory that must not reappear as a reactor module tree.
REMOVED_MODULE_DIR = "modules/jcadf-master"

# Patterns that must not appear in production-tracked paths.
BANNED_SUBSTRINGS = (
    "com.ibm.cadf",
    "com.percussion.auditlog",
)

# git grep pathspecs (production-ish). Docs/specs intentionally omitted.
GREP_PATHSPECS = (
    "*.java",
    "**/pom.xml",
    "pom.xml",
)

# Allowlist: paths under these prefixes may still mention banned strings
# (this gate script, its tests, design notes that document the removal).
ALLOWLIST_PREFIXES = (
    "scripts/verify-no-cadf-legacy-auditlog.py",
    "scripts/test_verify_no_cadf_legacy_auditlog.py",
    "docs/ai-generated/tasks/system-audit-log/",
    "modules/perc-auditlog/README.md",
)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify-no-cadf-legacy-auditlog.py",
        description=(
            "Verify CADF/jcadf and legacy com.percussion.auditlog stay removed "
            "from production sources (#2675)."
        ),
    )
    parser.add_argument(
        "--repo-root",
        default=str(REPO_ROOT),
        help="Repo root for the checks (default: this script's repo root)",
    )
    return parser


def _is_allowlisted(path: str) -> bool:
    norm = path.replace("\\", "/")
    for prefix in ALLOWLIST_PREFIXES:
        if norm == prefix or norm.startswith(prefix):
            return True
    return False


def _git_grep(repo_root: Path, pattern: str) -> list[str]:
    """Return ``git grep -l <pattern> -- <pathspecs>`` paths (one per line)."""
    cmd = ["git", "grep", "-l", "-F", pattern, "--", *GREP_PATHSPECS]
    result = subprocess.run(
        cmd,
        shell=False,
        check=False,
        cwd=str(repo_root),
        timeout=120,
        capture_output=True,
        text=True,
    )
    # git grep: 0 = matches, 1 = no matches, other = error
    if result.returncode not in (0, 1):
        raise RuntimeError(
            f"git grep failed for {pattern!r} (rc={result.returncode}): "
            f"{result.stderr.strip()}"
        )
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    repo_root = Path(args.repo_root)
    fail = False

    print("==> checking that modules/jcadf-master is gone")
    removed = repo_root / REMOVED_MODULE_DIR
    if removed.is_dir():
        # Allow empty leftover after partial delete only if no pom/src — still fail
        # if any content exists under the historical module path.
        print(
            f"  FAIL: removed module directory reappeared: {REMOVED_MODULE_DIR}",
            file=sys.stderr,
        )
        fail = True
    else:
        print(f"  OK: {REMOVED_MODULE_DIR} absent")

    print("==> checking production sources/POMs for banned CADF / legacy packages")
    for pattern in BANNED_SUBSTRINGS:
        try:
            matches = _git_grep(repo_root, pattern)
        except RuntimeError as exc:
            print(f"  FAIL: {exc}", file=sys.stderr)
            fail = True
            continue
        offenders = [m for m in matches if not _is_allowlisted(m)]
        if offenders:
            print(
                f"  FAIL: found banned pattern {pattern!r} in production paths:",
                file=sys.stderr,
            )
            for line in offenders:
                print(f"    {line}", file=sys.stderr)
            fail = True
        else:
            print(f"  OK: no production hits for {pattern!r}")

    if fail:
        print("verify-no-cadf-legacy-auditlog: FAIL", file=sys.stderr)
        return 1
    print("verify-no-cadf-legacy-auditlog: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
