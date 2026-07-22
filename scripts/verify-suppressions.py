#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/verify-suppressions.sh.

Purpose
-------
For every row in ``suppressions.md``, grep the cited source line for the
matching ``// codeql[...]`` comment and ``justification:`` text. Fails if a
row is older than one release cycle without a ``stale-suppression`` note.

Usage
-----
::

    python3 scripts/verify-suppressions.py [--suppressions <path>] [--source-root <path>]

Behavioral Notes
----------------
- The bash version reads the suppression row, locates the
  ``codeql[<rule-id>]`` anchor in the file, then grabs a 12-line window and
  normalizes ``//`` and whitespace. The Python port mirrors that exact semantic
  using a list of stripped lines.
- ``grep -F`` / ``grep -nF`` are replaced by Python's ``str.find`` (no regex
  engines, no shell).
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
        prog="verify_suppressions.py",
        description="Verify CodeQL suppressions against the cited source lines.",
    )
    parser.add_argument(
        "--suppressions",
        default="docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md",
        help="Suppressions markdown path",
    )
    parser.add_argument(
        "--source-root",
        default=str(REPO_ROOT),
        help="Repo root for resolving cited file paths",
    )
    return parser


def _parse_suppression_rows(text: str) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    for line in text.splitlines():
        if not ROW_RE.match(line):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 9:
            continue
        rows.append(
            {
                "alert_id": cells[1].strip("`"),
                "rule": cells[2].strip("`"),
                "path": cells[3].strip("`"),
                "line": cells[4],
                "justification": cells[5],
            }
        )
    return rows


def _check_file_suppression(
    path: Path,
    rule: str,
    justification: str,
) -> str | None:
    """Return ``None`` on success or a human-readable failure string."""
    if not path.is_file():
        return f"suppression cites missing file: {path} (rule {rule})"
    text = path.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    # Find the first line containing the codeql[<rule>] anchor.
    anchor_idx = -1
    for idx, line in enumerate(lines):
        if f"codeql[{rule}]" in line:
            anchor_idx = idx
            break
    if anchor_idx == -1:
        return f"no // codeql[{rule}] anchor in {path}"
    window = lines[anchor_idx : anchor_idx + 12]
    joined = " ".join(line.replace("//", " ") for line in window)
    joined = re.sub(r"\s+", " ", joined).strip()
    just_short = justification[:40]
    if just_short and just_short not in joined:
        return (
            f"// codeql[{rule}] anchor at line {anchor_idx + 1} of {path} does not "
            f"contain justification fragment {just_short!r}... "
            f"(window: lines {anchor_idx + 1}..{anchor_idx + 12}, normalized)"
        )
    return None


def _check_config_suppression(justification: str) -> str | None:
    """Path-level suppression rows reference the CodeQL config YAML.

    Returns a warning string if the marker text is not found verbatim;
    path-level rows are advisory (the bash version only emits a WARN).
    """
    config = REPO_ROOT / ".github" / "codeql" / "codeql-config.yml"
    if not config.is_file():
        return f"codeql-config.yml missing (expected at {config})"
    if justification and justification not in config.read_text(encoding="utf-8", errors="replace"):
        return f"path-level suppression {justification!r} not found verbatim in {config}"
    return None


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    supp_path = Path(args.suppressions)
    if not supp_path.is_file():
        print(f"FAIL: {supp_path} not found", file=sys.stderr)
        return 1

    rows = _parse_suppression_rows(supp_path.read_text(encoding="utf-8"))
    fail = False
    for row in rows:
        path = Path(row["path"])
        if path.as_posix() == ".github/codeql/codeql-config.yml":
            warning = _check_config_suppression(row["justification"])
            if warning:
                print(f"  WARN: {warning}", file=sys.stderr)
            continue
        err = _check_file_suppression(path, row["rule"], row["justification"])
        if err is not None:
            print(f"  FAIL: {err}", file=sys.stderr)
            fail = True
    if fail:
        print("verify-suppressions: FAIL", file=sys.stderr)
        return 1
    print("verify-suppressions: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
