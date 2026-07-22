#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/resolve-conflicts.sh.

Purpose
-------
Scan the working tree for unresolved git merge conflicts (``git diff --name-only
--diff-filter=U``) and stage them as ``--ours`` (i.e. the current branch's
version). Files that were deleted on ``ours`` are removed with ``git rm --force``
or ``git add -u`` as a fallback (mirrors the bash original's exact semantics).

Usage
-----
::

    python3 scripts/resolve-conflicts.py [--strategy ours|theirs|manual] [--dry-run]

Behavioral Notes
----------------
- The bash original only supported ``--ours`` semantics; the port adds
  ``--theirs`` for symmetry and ``--manual`` to print the list of conflicted
  files without modifying anything (the recommended dry-run).
- ``mapfile -d '' ... < <(...)`` is replaced by ``subprocess.run(..., text=True,
  capture_output=True)`` + ``splitlines()``; ``git diff -z`` returns NUL-separated
  filenames which we split on ``\0`` directly (matches bash ``-z`` behavior).
- ``try``/``finally`` replaces ``trap 'rm -f "$tmp"' EXIT`` for cleanup.

Exit codes
----------
- ``0`` all conflicted files resolved (or none present)
- ``1`` strategy execution failures or files that could not be auto-resolved
- ``2`` argument errors
"""
from __future__ import annotations

import argparse
import logging
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="resolve-conflicts.py",
        description="Resolve git merge conflicts by accepting ours or theirs.",
    )
    parser.add_argument(
        "--strategy",
        choices=("ours", "theirs", "manual"),
        default="manual",
        help="Conflict resolution strategy (default: manual = list only)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print what would be done without modifying the working tree",
    )
    return parser


def _git(args: list[str]) -> tuple[int, str]:
    """Run ``git <args>`` and return ``(returncode, stdout)``."""
    result = subprocess.run(
        ["git", *args],
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
    )
    return (result.returncode, result.stdout or "")


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    # Must be inside a git working tree.
    rc, _ = _git(["rev-parse", "--is-inside-work-tree"])
    if rc != 0:
        LOGGER.error("This script must be run inside a Git repository.")
        return 1

    LOGGER.info("Scanning index for unresolved merge conflicts...")
    rc, raw = _git(["diff", "--name-only", "--diff-filter=U", "-z"])
    if rc != 0:
        LOGGER.error("git diff --name-only failed (rc=%d).", rc)
        return 1

    # ``git diff -z`` emits NUL-terminated names. Filter empties to be safe.
    conflicted = [name for name in raw.split("\0") if name]
    if not conflicted:
        LOGGER.info("No unresolved merge conflicts found.")
        return 0

    LOGGER.info("Found %d conflicted file(s).", len(conflicted))

    if args.strategy == "manual" or args.dry_run:
        for f in conflicted:
            print(f)
        if args.dry_run:
            LOGGER.info("--dry-run set; no changes made.")
        else:
            LOGGER.info(
                "Manual strategy: list only. Re-run with --strategy ours|theirs to auto-resolve."
            )
        return 0 if args.dry_run else 1

    checkout_flag = "--ours" if args.strategy == "ours" else "--theirs"

    resolved = 0
    failed = 0
    for f in conflicted:
        LOGGER.info("Processing: %s", f)
        # Stage 2 is "ours"; stage 3 is "theirs" in git's index terminology.
        ours_stage = "2"
        theirs_stage = "3"
        side = ours_stage if args.strategy == "ours" else theirs_stage
        # If the chosen side does not exist in the index, the file was deleted
        # on that side; fall through to a deletion-stage path.
        rc_show, _ = _git(["show", f":{side}:{f}"])
        if rc_show == 0:
            rc_co, _ = _git(["checkout", checkout_flag, "--", f])
            rc_add, _ = _git(["add", "--", f])
            if rc_co == 0 and rc_add == 0:
                resolved += 1
            else:
                LOGGER.warning("Failed to resolve with %s: %s", checkout_flag, f)
                failed += 1
        else:
            rc_rm, _ = _git(["rm", "--force", "--", f])
            if rc_rm != 0:
                rc_add_u, _ = _git(["add", "-u", "--", f])
                if rc_add_u != 0:
                    LOGGER.warning("Failed to stage deletion: %s", f)
                    failed += 1
                    continue
            resolved += 1

    # Confirm remaining conflicts.
    rc_remain, remain_raw = _git(["diff", "--name-only", "--diff-filter=U"])
    remaining = sum(1 for name in remain_raw.splitlines() if name)

    LOGGER.info("Resolved: %d", resolved)
    LOGGER.info("Failed: %d", failed)
    LOGGER.info("Remaining unresolved conflicts: %d", remaining)

    if failed or remaining:
        LOGGER.error("Some conflicts could not be resolved automatically. Review with: git status")
        return 1
    LOGGER.info("Done. All current merge conflicts were resolved using %s.", args.strategy)
    LOGGER.info("Review with: git status")
    return 0


if __name__ == "__main__":
    sys.exit(main())
