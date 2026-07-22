#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/verify-no-jqplot-vendor-refs.sh.

Purpose
-------
Guards the removal of the dead jqplot vendor library against regression:
fails if any tracked source file references ``jqplot`` outside the removed
directories, or if a copy of the removed directory reappears in the tree.

Usage
-----
::

    python3 scripts/verify_no_jqplot_vendor_refs.py [--repo-root <path>]

Exit codes
----------
- ``0`` clean (no reintroduced vendor dirs, no stray references)
- ``1`` at least one violation found

Behavioral Notes
----------------
- bash ``git grep -l -i "jqplot" -- "*.jsp" "*.js" "*.html" "*.xml"`` is replaced
  by ``subprocess.run(["git", "grep", "-l", "-i", "jqplot", "--", ...])``
  (FR-008: shell=False). This preserves ``git grep``'s "only tracked files"
  semantic, which is load-bearing for the test (the bash self-test relies on
  it).
- ``if [ -d "$d" ]`` directory existence checks use ``Path.is_dir()``.
"""
from __future__ import annotations

import argparse
import logging
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

REMOVED_VENDOR_DIRS = (
    "WebUI/src/main/webapp/cm/gadgets/repository/common/lib/jqplot",
    "WebUI/src/main/webapp/cm/widgets/repository/common/lib/jqplot",
    "WebUI/war/gadgets/repository/common/lib/jqplot",
)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify_no_jqplot_vendor_refs.py",
        description="Verify jqplot vendor library is gone and no stray refs remain.",
    )
    parser.add_argument(
        "--repo-root",
        default=str(REPO_ROOT),
        help="Repo root for the file checks (default: this script's repo root)",
    )
    return parser


def _git_grep(repo_root: Path) -> list[str]:
    """Return ``git grep -l -i jqplot -- <globs>`` output (one path per line)."""
    cmd = [
        "git",
        "grep",
        "-l",
        "-i",
        "jqplot",
        "--",
        "*.jsp",
        "*.js",
        "*.html",
        "*.xml",
    ]
    result = subprocess.run(
        cmd,
        shell=False,
        check=False,
        cwd=str(repo_root),
        timeout=120,
        capture_output=True,
        text=True,
    )
    if result.returncode not in (0, 1):
        # rc=1 from git grep means "no matches" — that's the success path.
        # Any other rc is a real error.
        raise RuntimeError(f"git grep failed (rc={result.returncode})")
    return [line for line in result.stdout.splitlines() if line]


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    repo_root = Path(args.repo_root)

    fail = False
    print("==> checking for reintroduced jqplot vendor directories")
    for d in REMOVED_VENDOR_DIRS:
        p = repo_root / d
        if p.is_dir():
            print(f"  FAIL: removed vendor directory reappeared: {d}", file=sys.stderr)
            fail = True

    print("==> checking for stray jqplot references in tracked JSP/JS/HTML/XML")
    try:
        matches = _git_grep(repo_root)
    except RuntimeError as exc:
        print(f"  FAIL: {exc}", file=sys.stderr)
        print("verify-no-jqplot-vendor-refs: FAIL", file=sys.stderr)
        return 1
    if matches:
        print("  FAIL: found jqplot reference(s) outside the removed vendor library:", file=sys.stderr)
        for line in matches:
            print(f"    {line}", file=sys.stderr)
        fail = True

    if fail:
        print("verify-no-jqplot-vendor-refs: FAIL", file=sys.stderr)
        return 1
    print("verify-no-jqplot-vendor-refs: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
