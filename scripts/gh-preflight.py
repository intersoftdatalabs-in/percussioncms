#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/gh-preflight.sh.

Purpose
-------
Checks that GitHub actions target the ``intersoftdatalabs-in/percussioncms`` fork
by default:
    1. ``gh repo set-default --view`` returns ``intersoftdatalabs-in/percussioncms``.
    2. ``git remote get-url origin`` points to the intersoft fork.

In ``--fix`` mode, the script also calls ``gh repo set-default`` and
``git config remote.pushDefault origin`` to repair drift.

Usage
-----
::

    python3 scripts/gh-preflight.py [--fix] [--repo OWNER/REPO] [--require <tool> ...]

Behavioral Notes
----------------
- The bash version hard-coded ``EXPECTED_REPO``; this port keeps the same default
  but allows override via ``--repo`` (per ``contracts/cli-schemas.md``).
- ``shutil.which`` replaces ``command -v`` for portability.
- Exit code ``2`` is returned when any required tool is missing on PATH
  (per the contract).
"""
from __future__ import annotations

import argparse
import logging
import shutil
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

DEFAULT_REPO = "intersoftdatalabs-in/percussioncms"
DEFAULT_REQUIRES = ("gh", "jq")


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="gh-preflight.py",
        description="Preflight check for GitHub target repo and required tools.",
    )
    parser.add_argument(
        "--repo",
        default=DEFAULT_REPO,
        help=f"Expected owner/repo (default: {DEFAULT_REPO})",
    )
    parser.add_argument(
        "--require",
        action="append",
        default=None,
        help="Tool that must be on PATH (repeatable; default: gh, jq)",
    )
    parser.add_argument(
        "--fix",
        action="store_true",
        help="Repair drift by setting gh default repo + git remote.pushDefault",
    )
    return parser


def _run(cmd: list[str]) -> tuple[int, str]:
    """Run ``cmd`` and return ``(returncode, stdout)``. Non-zero return codes are
    surfaced; empty stdout is fine."""
    try:
        result = subprocess.run(
            cmd,
            shell=False,
            check=False,
            timeout=30,
            capture_output=True,
            text=True,
        )
    except FileNotFoundError:
        return (127, "")
    return (result.returncode, (result.stdout or "").strip())


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    required_tools = list(args.require) if args.require else list(DEFAULT_REQUIRES)
    expected_repo = args.repo

    # 1. Required tools on PATH.
    missing = [t for t in required_tools if shutil.which(t) is None]
    if missing:
        for tool in missing:
            LOGGER.error("Required tool not found on PATH: %s", tool)
        return 2

    # 2. ``gh`` default repo matches expected.
    rc, current_repo = _run(["gh", "repo", "set-default", "--view"])
    if rc != 0:
        current_repo = ""

    repo_ok = current_repo == expected_repo
    if args.fix and not repo_ok:
        _run(["gh", "repo", "set-default", expected_repo])
        rc, current_repo = _run(["gh", "repo", "set-default", "--view"])
        repo_ok = current_repo == expected_repo

    if not repo_ok:
        LOGGER.error(
            "gh default repo is %r (expected %r). Run: gh repo set-default %s",
            current_repo or "<unset>",
            expected_repo,
            expected_repo,
        )
        return 1

    # 3. ``git origin`` points at the expected fork.
    rc, origin_url = _run(["git", "remote", "get-url", "origin"])
    if rc != 0:
        origin_url = ""
    expected_origin_substr = f"github.com/{expected_repo}"
    origin_ok = expected_origin_substr in origin_url

    if not origin_ok:
        LOGGER.error(
            "git origin points to %r (expected fork %s). Run: "
            "git remote set-url origin https://github.com/%s.git",
            origin_url or "<unset>",
            expected_repo,
            expected_repo,
        )
        return 1

    # 4. ``remote.pushDefault`` is a soft warning (matches bash original).
    rc, push_default = _run(["git", "config", "--get", "remote.pushDefault"])
    if rc != 0:
        push_default = ""
    if push_default != "origin":
        LOGGER.warning(
            "remote.pushDefault is %r (recommended: origin). "
            "Run: git config remote.pushDefault origin",
            push_default or "<unset>",
        )
        if args.fix:
            _run(["git", "config", "remote.pushDefault", "origin"])

    LOGGER.info("OK: GitHub target preflight passed (%s).", expected_repo)
    return 0


if __name__ == "__main__":
    sys.exit(main())
