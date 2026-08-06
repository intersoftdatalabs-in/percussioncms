#!/usr/bin/env python3
"""
Rewrite Percussion Software copyright lines to Intersoft Data Labs for files
first added to the repo on or after 2023-01-01.

Pre-2023 files are left unchanged.

Usage (from repo root):
  python scripts/retrofit-intersoft-copyright.py           # apply
  python scripts/retrofit-intersoft-copyright.py --dry-run # report only
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
CUTOFF = "2023-01-01"
SOURCE_GLOBS = [
    "*.java",
    "*.ts",
    "*.tsx",
    "*.js",
    "*.jsx",
    "*.mjs",
    "*.cjs",
    "*.kt",
    "*.cs",
    "*.groovy",
    "*.scala",
]

# Match common Apache-style copyright lines that still say Percussion Software.
PERC_COPYRIGHT = re.compile(
    r"(?P<pre>^[ \t]*(?://|#|\*|/\*\*?|<!--)?[ \t]*)"
    r"Copyright(?:\s*\([cC]\))?\s+"
    r"(?P<span>[\d\s,\-–—]+)"
    r"\s*Percussion Software,?\s*Inc\.?",
    re.MULTILINE | re.IGNORECASE,
)

SKIP_PATH_PARTS = {
    "node_modules",
    "target",
    ".git",
    "tmp",
    "dist",
    "build",
    ".kilo",
}


def git_first_adds_since(since: str) -> dict[str, str]:
    """path -> first-add date (YYYY-MM-DD) for source files added on/after since."""
    cmd = [
        "git",
        "log",
        "--diff-filter=A",
        "--name-only",
        "--pretty=format:%as",
        f"--since={since}",
        "--",
        *SOURCE_GLOBS,
    ]
    out = subprocess.check_output(cmd, cwd=REPO, text=True, errors="replace")
    path_to_date: dict[str, str] = {}
    current_date: str | None = None
    for raw in out.splitlines():
        line = raw.strip().replace("\\", "/")
        if not line:
            continue
        if re.fullmatch(r"\d{4}-\d{2}-\d{2}", line):
            current_date = line
            continue
        if current_date is None:
            continue
        if any(p in line.split("/") for p in SKIP_PATH_PARTS):
            continue
        # First appearance in reverse-chronological log is latest add; keep earliest.
        if line not in path_to_date or current_date < path_to_date[line]:
            path_to_date[line] = current_date
    return path_to_date


def year_from_date(iso: str) -> int:
    return int(iso[:4])


def replacement_for(match: re.Match[str], year: int) -> str:
    pre = match.group("pre") or ""
    # Normalize leading comment markers already in `pre`
    return f"{pre}Copyright (c) {year} Intersoft Data Labs, Inc."


def process_file(path: Path, year: int, dry_run: bool) -> bool:
    try:
        text = path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        try:
            text = path.read_text(encoding="utf-8-sig")
        except (UnicodeDecodeError, OSError):
            return False

    if "Percussion Software" not in text:
        return False

    # Only rewrite copyright lines (not prose mentions deeper in the file if not copyright)
    new_text, n = PERC_COPYRIGHT.subn(lambda m: replacement_for(m, year), text)
    if n == 0:
        return False
    if not dry_run and new_text != text:
        path.write_text(new_text, encoding="utf-8", newline="\n")
    return True


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--since", default=CUTOFF)
    args = ap.parse_args()

    print(f"Scanning first-adds since {args.since} …", flush=True)
    first_adds = git_first_adds_since(args.since)
    print(f"  {len(first_adds)} paths first-added in range", flush=True)

    by_year: dict[int, list[str]] = defaultdict(list)
    changed: list[str] = []
    missing = 0
    no_perc = 0

    for rel, date in sorted(first_adds.items()):
        path = REPO / rel
        if not path.is_file():
            missing += 1
            continue
        year = year_from_date(date)
        try:
            sample = path.read_text(encoding="utf-8", errors="replace")[:4000]
        except OSError:
            missing += 1
            continue
        if "Percussion Software" not in sample and "Percussion Software" not in path.read_text(
            encoding="utf-8", errors="replace"
        ):
            no_perc += 1
            continue
        if process_file(path, year, args.dry_run):
            changed.append(rel)
            by_year[year].append(rel)

    mode = "would change" if args.dry_run else "changed"
    print(f"\n{mode}: {len(changed)} files")
    for y in sorted(by_year):
        print(f"  year {y}: {len(by_year[y])}")
    print(f"skipped (no Percussion copyright in file): {no_perc}")
    print(f"missing paths: {missing}")

    report = REPO / "tmp" / "intersoft-copyright-retrofit-report.txt"
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(
        "\n".join(f"{year_from_date(first_adds[p])}\t{p}" for p in changed) + "\n",
        encoding="utf-8",
    )
    print(f"report: {report}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
