#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/verify-pr-review-resolution.sh.

Purpose
-------
Per Constitution IX (NON-NEGOTIABLE) and the PR Review Comment Resolution
procedure in AGENTS.md, every closing PR for tasks T021..T072 MUST have all
its review threads resolved before merge. This script reads the GitHub PRs
identified by ``triage.md`` ``linked_pr`` and fails if any thread has
``isResolved: false``.

Usage
-----
::

    python3 scripts/verify-pr-review-resolution.py [--pr-number N ...]
                                                   [--triage <path>]
                                                   [--repo OWNER/REPO]

Exit codes
----------
- ``0`` all PRs have resolved threads (or no PRs to check)
- ``1`` at least one PR has an unresolved review thread
- ``2`` missing prerequisite (``gh`` CLI not authenticated, or missing triage file)

Behavioral Notes
----------------
- The bash version parses ``gh pr view --json reviewThreads`` output via
  ``grep -c '"isResolved":false'``. The Python port uses ``json.loads`` to
  count reliably across platforms.
- The script requires ``gh`` on PATH; if it's missing, exit code 2 (matches
  the contract).
"""
from __future__ import annotations

import argparse
import json
import logging
import shutil
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

ROW_RE = __import__("re").compile(r"^\|\s*(\d+)\s*\|")


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify_pr_review_resolution.py",
        description="Verify every linked PR has all review threads resolved.",
    )
    parser.add_argument(
        "--pr-number",
        action="append",
        type=int,
        default=None,
        help="PR number to check (repeatable; default: all linked_pr in triage.md)",
    )
    parser.add_argument(
        "--triage",
        default="docs/ai-generated/tasks/gh-codeql-alerts/triage.md",
        help="Triage markdown path",
    )
    parser.add_argument(
        "--repo",
        default="intersoftdatalabs-in/percussioncms",
        help="owner/repo for gh CLI (default: intersoftdatalabs-in/percussioncms)",
    )
    return parser


def _pr_numbers_from_triage(triage_text: str) -> list[str]:
    """Extract numeric ``linked_pr`` values from triage.md (matches bash awk)."""
    out: list[str] = []
    seen: set[str] = set()
    for line in triage_text.splitlines():
        if not ROW_RE.match(line):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 10:
            continue
        linked = cells[9].strip("`").strip()
        if linked.isdigit() and linked not in seen:
            seen.add(linked)
            out.append(linked)
    return out


def _review_threads(repo: str, pr: str) -> list[dict]:
    """Call ``gh pr view`` and return the parsed ``reviewThreads`` array."""
    cmd = [
        "gh",
        "pr",
        "view",
        pr,
        "--repo",
        repo,
        "--json",
        "reviewThreads",
        "--jq",
        ".reviewThreads // []",
    ]
    result = subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise RuntimeError(
            f"gh pr view {pr} failed (rc={result.returncode}): "
            f"{(result.stderr or '').strip()[:500]}"
        )
    payload = result.stdout.strip()
    if not payload:
        return []
    try:
        data = json.loads(payload)
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"gh returned invalid JSON: {exc}") from exc
    if not isinstance(data, list):
        return []
    return data


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    if shutil.which("gh") is None:
        print("FAIL: gh CLI not found", file=sys.stderr)
        return 2

    prs: list[str]
    if args.pr_number:
        prs = [str(n) for n in args.pr_number]
    else:
        triage_path = Path(args.triage)
        if not triage_path.is_file():
            print(f"FAIL: {triage_path} not found", file=sys.stderr)
            return 2
        prs = _pr_numbers_from_triage(triage_path.read_text(encoding="utf-8"))

    if not prs:
        print("verify-pr-review-resolution: no PRs to check (PASS by vacuous truth)")
        return 0

    fail = False
    for pr in prs:
        print(f"==> PR #{pr}")
        try:
            threads = _review_threads(args.repo, pr)
        except RuntimeError as exc:
            print(f"  FAIL: {exc}", file=sys.stderr)
            fail = True
            continue
        unresolved = sum(1 for t in threads if not t.get("isResolved", True))
        if unresolved > 0:
            print(f"  FAIL: {unresolved} unresolved review thread(s) on PR #{pr}", file=sys.stderr)
            fail = True
        else:
            print("  OK: all review threads resolved")

    if fail:
        print("verify-pr-review-resolution: FAIL", file=sys.stderr)
        return 1
    print("verify-pr-review-resolution: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
