#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/verify-triage-inventory.sh.

Purpose
-------
Enforce contracts/C1 (triage.md format and content) for spec
004-zero-code-scanning-alerts:
  (a) row count == open-alert count (alerts.md minus stale-cache rows)
  (b) every false-positive / accepted-risk row has non-empty notes
  (c) every module_owner is a path listed under ``./AGENTS.md``

Usage
-----
::

    python3 scripts/verify-triage-inventory.py
        [--triage <path>]
        [--alerts <path>]
        [--stale <path>]
        [--strict]

Exit codes
----------
- ``0`` all rules satisfied (warnings only)
- ``1`` rule violation
- ``2`` IO error (missing file, etc.)

Behavioral Notes
----------------
- bash ``awk -F'|'`` parsing is replaced by a Python row parser that handles
  backticks / ``(candidate)`` decoration identically.
- ``set -e`` is replaced by explicit ``sys.exit(code)`` propagation; warnings
  are collected and printed at the end.
- ``--strict`` upgrades warnings to failures (the bash version uses
  ``TRIAGE_SLACK`` for the same purpose; the port keeps the env var as well).
"""
from __future__ import annotations

import argparse
import logging
import os
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

ROW_RE = re.compile(r"^\|\s*(\d+)\s*\|")


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify_triage_inventory.py",
        description="Verify the CodeQL triage inventory against contracts/C1.",
    )
    parser.add_argument(
        "--triage",
        default="docs/ai-generated/tasks/gh-codeql-alerts/triage.md",
        help="Triage markdown path",
    )
    parser.add_argument(
        "--alerts",
        default="docs/ai-generated/tasks/gh-codeql-alerts/alerts.md",
        help="Alerts markdown path",
    )
    parser.add_argument(
        "--stale",
        default="docs/ai-generated/tasks/gh-codeql-alerts/alerts-stale-cache.md",
        help="Stale cache markdown path",
    )
    parser.add_argument(
        "--agents",
        default="AGENTS.md",
        help="AGENTS.md path (for module_owner validation)",
    )
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Treat row-count slack warnings as failures",
    )
    return parser


def _parse_rows(text: str) -> list[dict[str, str]]:
    """Parse a triage.md into a list of row dicts (matches the bash awk semantic)."""
    rows: list[dict[str, str]] = []
    for line in text.splitlines():
        if not ROW_RE.match(line):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 11:
            continue
        disposition = cells[6].strip("`")
        disposition = re.sub(r"\s*\(candidate\)", "", disposition).strip()
        rows.append(
            {
                "alert_id": cells[1].strip("`"),
                "rule_id": cells[2].strip("`"),
                "severity": cells[3].strip("`"),
                "file_path": cells[4].strip("`"),
                "module_owner": cells[5].strip("`"),
                "disposition": disposition,
                "target_action": cells[7].strip("`"),
                "target_milestone": cells[8].strip("`"),
                "linked_pr": cells[9].strip("`"),
                "notes": cells[10].strip("`"),
            }
        )
    return rows


def _count_open_alerts(text: str) -> int:
    """Count ``- **Alert #...`` lines in alerts.md."""
    return sum(1 for line in text.splitlines() if line.startswith("- **Alert #"))


def _count_stale(text: str) -> int:
    """Count rows in the stale-cache Markdown table (skip header + separator)."""
    return sum(1 for line in text.splitlines() if ROW_RE.match(line))


def _module_paths_from_agents(agents_text: str) -> set[str]:
    """Extract ``./<path>/`` module roots from ``AGENTS.md`` Module List section.

    Mirrors the bash version's awk that scans the ``## Module List`` section.
    """
    in_list = False
    modules: set[str] = set()
    for line in agents_text.splitlines():
        if line.startswith("## Module List"):
            in_list = True
            continue
        if in_list and line.startswith("## "):
            in_list = False
        if not in_list:
            continue
        m = re.search(r"`\.\/([^`]+)`", line)
        if m:
            path = m.group(1).rstrip("/")
            modules.add(path)
    return modules


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    triage_path = Path(args.triage)
    alerts_path = Path(args.alerts)
    stale_path = Path(args.stale)
    agents_path = Path(args.agents)

    for label, p in (
        ("triage", triage_path),
        ("alerts", alerts_path),
        ("agents", agents_path),
    ):
        if not p.is_file():
            print(f"FAIL: {label} file not found: {p}", file=sys.stderr)
            return 2

    triage_rows = _parse_rows(triage_path.read_text(encoding="utf-8"))
    open_alerts = _count_open_alerts(alerts_path.read_text(encoding="utf-8"))
    stale_rows = (
        _count_stale(stale_path.read_text(encoding="utf-8"))
        if stale_path.is_file()
        else 0
    )
    ready_to_close = sum(1 for r in triage_rows if r["linked_pr"] not in ("", "—"))

    effective_open = open_alerts - stale_rows
    expected = effective_open + ready_to_close
    delta = len(triage_rows) - expected

    fail = False

    # ---- (a) row-count check ----
    if delta == 0:
        print(
            f"OK: row-count check ({len(triage_rows)} == {expected}; "
            f"{effective_open} open + {ready_to_close} ready-to-close)"
        )
    else:
        slack = int(os.environ.get("TRIAGE_SLACK", "0"))
        abs_delta = abs(delta)
        if abs_delta <= slack and not args.strict:
            print(
                f"WARN: row-count off by {delta} (within slack={slack}): "
                f"{len(triage_rows)} vs {expected}"
            )
        else:
            print("FAIL: row-count check")
            print(f"  triage.md rows:           {len(triage_rows)}")
            print(f"  open alerts:              {open_alerts}")
            print(f"  stale-cache rows:         {stale_rows}")
            print(f"  effective open alerts:    {effective_open}")
            print(f"  ready-to-close rows:      {ready_to_close}")
            print(f"  expected total:           {expected}")
            print(f"  delta:                    {delta}")
            print(f"  slack (TRIAGE_SLACK):     {slack}")
            fail = True

    # ---- (b) false-positive/accepted-risk notes ----
    bad_notes = [
        r
        for r in triage_rows
        if r["disposition"] in ("false-positive", "accepted-risk") and not r["notes"]
    ]
    if bad_notes:
        print("FAIL: false-positive/accepted-risk rows with empty notes:")
        for r in bad_notes:
            print(f"  alert {r['alert_id']} (path={r['file_path']})")
        fail = True
    else:
        print("OK: notes check (all false-positive/accepted-risk rows have notes)")

    # ---- (c) module_owner check ----
    modules = _module_paths_from_agents(agents_path.read_text(encoding="utf-8"))
    if not modules:
        print(f"WARN: could not extract module list from {agents_path}; skipping owner check", file=sys.stderr)
    else:
        bad_owners: list[str] = []
        for r in triage_rows:
            owner = r["module_owner"].rstrip("/")
            if not owner:
                continue
            matched = any(
                owner == m or owner.startswith(f"{m}/") for m in modules
            )
            if not matched:
                bad_owners.append(f"  alert {r['alert_id']} -> unknown module_owner: {r['module_owner']}")
        if bad_owners:
            print("FAIL: unknown module_owner values:")
            for line in bad_owners:
                print(line)
            fail = True
        else:
            print("OK: module_owner check (all owners found in AGENTS.md)")

    if fail:
        print("verify-triage-inventory: FAIL", file=sys.stderr)
        return 1
    print("verify-triage-inventory: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
