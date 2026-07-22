#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/verify-valid-fixes.sh.

Purpose
-------
Iterate ``triage.md`` rows whose disposition is ``valid`` and confirm each has
a non-empty ``linked_pr`` (set when the closing PR merges). Fails otherwise.
Per spec 004 T035.

Usage
-----
::

    python3 scripts/verify-valid-fixes.py [--triage <path>]

Exit codes
----------
- ``0`` all valid rows have linked_pr
- ``1`` at least one valid row lacks linked_pr
- ``2`` missing file
"""
from __future__ import annotations

import argparse
import logging
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

ROW_RE = re.compile(r"^\|\s*(\d+)\s*\|")


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify_valid_fixes.py",
        description="Verify every valid triage row has a linked_pr.",
    )
    parser.add_argument(
        "--triage",
        default="docs/ai-generated/tasks/gh-codeql-alerts/triage.md",
        help="Triage markdown path",
    )
    return parser


def _parse_valid_rows(text: str) -> list[dict[str, str]]:
    """Parse rows whose disposition is exactly ``valid``."""
    rows: list[dict[str, str]] = []
    for line in text.splitlines():
        if not ROW_RE.match(line):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 11:
            continue
        disposition = re.sub(r"\s*\(candidate\)", "", cells[6].strip("`")).strip()
        if disposition != "valid":
            continue
        rows.append(
            {
                "alert_id": cells[1].strip("`"),
                "rule_id": cells[2].strip("`"),
                "file_path": cells[4].strip("`"),
                "linked_pr": cells[9].strip("`"),
            }
        )
    return rows


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    triage_path = Path(args.triage)
    if not triage_path.is_file():
        print(f"FAIL: {triage_path} not found", file=sys.stderr)
        return 2

    rows = _parse_valid_rows(triage_path.read_text(encoding="utf-8"))
    missing = [r for r in rows if r["linked_pr"] in ("", "—")]
    if missing:
        print("FAIL: valid alerts without a linked_pr:", file=sys.stderr)
        for r in missing:
            print(
                f"  alert {r['alert_id']}  rule {r['rule_id']}  path {r['file_path']}",
                file=sys.stderr,
            )
        return 1
    print("verify-valid-fixes: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
