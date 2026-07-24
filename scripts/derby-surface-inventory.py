#!/usr/bin/env python3
"""Generate a dispositionable inventory of Apache Derby surface area in the repo.

Feature #548 / specs/548-derby-embedded-migration tasks T004–T005, QC-001.

Cross-platform Python 3.9+. Prefer:
  python3 scripts/derby-surface-inventory.py
Windows:
  scripts\\derby-surface-inventory.bat
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import defaultdict
from datetime import date
from pathlib import Path

# Search tokens (case-sensitive variants handled via flags where needed)
PATTERNS: list[tuple[str, re.Pattern[str]]] = [
    ("derby", re.compile(r"derby", re.IGNORECASE)),
    ("DERBY_const", re.compile(r"\bDERBY\b")),
    ("sqlDerby", re.compile(r"sqlDerby", re.IGNORECASE)),
    ("NetworkServer", re.compile(r"NetworkServer", re.IGNORECASE)),
    ("isDerby", re.compile(r"isDerby", re.IGNORECASE)),
    ("derby.system", re.compile(r"derby\.system", re.IGNORECASE)),
    ("dbms_derby", re.compile(r'dbms\s*=\s*["\']derby["\']', re.IGNORECASE)),
    ("port_1527", re.compile(r"\b1527\b")),
    ("EmbeddedDriver", re.compile(r"org\.apache\.derby\.jdbc\.EmbeddedDriver")),
    ("ClientDriver", re.compile(r"org\.apache\.derby\.jdbc\.ClientDriver")),
    ("derbydata", re.compile(r"derbydata", re.IGNORECASE)),
    ("p13n", re.compile(r"p13n", re.IGNORECASE)),
    ("Repository.zip", re.compile(r"Repository\.zip", re.IGNORECASE)),
    ("drda", re.compile(r"drda", re.IGNORECASE)),
    ("DerbyDialect", re.compile(r"DerbyDialect")),
]

SKIP_DIR_NAMES = {
    ".git",
    "target",
    "node_modules",
    ".idea",
    ".vscode",
    "__pycache__",
    ".grok",
    "tmp",
    "install_root",
    "dev-data",
}

# Binary / noise extensions
SKIP_SUFFIXES = {
    ".class",
    ".jar",
    ".war",
    ".zip",
    ".png",
    ".jpg",
    ".jpeg",
    ".gif",
    ".ico",
    ".pdf",
    ".woff",
    ".woff2",
    ".ttf",
    ".eot",
    ".so",
    ".dll",
    ".exe",
    ".bin",
    ".map",
    ".min.js",
}

MAX_FILE_BYTES = 2_000_000


def repo_root_from_script() -> Path:
    return Path(__file__).resolve().parent.parent


def should_skip_dir(name: str) -> bool:
    return name in SKIP_DIR_NAMES or name.startswith(".")


def iter_files(root: Path) -> list[Path]:
    files: list[Path] = []
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        # Skip if any parent is a skip dir
        try:
            rel = path.relative_to(root)
        except ValueError:
            continue
        if any(should_skip_dir(p) for p in rel.parts[:-1]):
            continue
        if path.suffix.lower() in SKIP_SUFFIXES:
            continue
        if path.name.endswith(".min.js"):
            continue
        files.append(path)
    return files


def scan_file(path: Path, root: Path) -> list[dict]:
    hits: list[dict] = []
    try:
        if path.stat().st_size > MAX_FILE_BYTES:
            return hits
        text = path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return hits

    rel = path.relative_to(root).as_posix()
    lines = text.splitlines()
    for i, line in enumerate(lines, start=1):
        matched_labels = [label for label, pat in PATTERNS if pat.search(line)]
        if not matched_labels:
            continue
        # Skip pure "p13n" hits that have no derby-related context on line
        # unless path suggests derby/p13n-ds packaging
        if matched_labels == ["p13n"] and "derby" not in line.lower() and "derby" not in rel.lower():
            if "p13n" not in rel.lower() or "derby" not in rel.lower():
                # keep p13n path hits only if path has derby or p13n-ds with derby nearby
                if "p13n" in rel.lower() and "derby" not in rel.lower():
                    # still record path-level p13n modules once per file via separate pass
                    continue
        snippet = line.strip()
        if len(snippet) > 160:
            snippet = snippet[:157] + "..."
        hits.append(
            {
                "path": rel,
                "line": i,
                "patterns": matched_labels,
                "snippet": snippet,
            }
        )
    return hits


def suggest_disposition(rel_path: str, patterns: list[str]) -> str:
    """Heuristic default disposition for triage; humans refine in the checklist."""
    p = rel_path.lower()
    if "test" in p or "/src/test/" in p:
        if "sqlderby" in ",".join(patterns).lower() or "networkserver" in ",".join(patterns).lower():
            return "migration-only"
        return "port"
    if "changelog" in p or "liquibase" in p or "dbms_derby" in patterns:
        return "port"
    if "install" in p and ("sqlderby" in ",".join(patterns).lower() or "1527" in patterns or "networkserver" in ",".join(patterns).lower()):
        return "port"
    if "psx_archiveinfo" in p or ".ppkg" in p:
        return "docs-only"
    if "p13n" in p:
        return "unknown"
    if patterns == ["p13n"]:
        return "unknown"
    if "derbynet" in p or "networkserver" in ",".join(patterns).lower():
        return "migration-only"
    if "isderby" in ",".join(patterns).lower():
        return "generalize"
    if "dialect" in p.lower() or "DerbyDialect" in patterns:
        return "port"
    return "unknown"


def render_markdown(hits: list[dict], root: Path) -> str:
    by_path: dict[str, list[dict]] = defaultdict(list)
    for h in hits:
        by_path[h["path"]].append(h)

    # Collapse to one inventory row per path with union of patterns
    rows: list[dict] = []
    for path, path_hits in sorted(by_path.items()):
        patterns: set[str] = set()
        for h in path_hits:
            patterns.update(h["patterns"])
        # Prefer first non-p13n-only line for sample
        sample = path_hits[0]
        for h in path_hits:
            if h["patterns"] != ["p13n"]:
                sample = h
                break
        disp = suggest_disposition(path, sorted(patterns))
        rows.append(
            {
                "path": path,
                "line": sample["line"],
                "patterns": sorted(patterns),
                "snippet": sample["snippet"],
                "disposition": disp,
                "hit_count": len(path_hits),
            }
        )

    today = date.today().isoformat()
    unknown_n = sum(1 for r in rows if r["disposition"] == "unknown")
    lines = [
        "# Derby surface inventory (#548)",
        "",
        f"**Generated**: {today}  ",
        f"**Script**: `scripts/derby-surface-inventory.py`  ",
        f"**Repo root**: `{root}`  ",
        f"**Files with hits**: {len(rows)}  ",
        f"**Heuristic unknown disposition**: {unknown_n} (must reach zero before GA — QC-001)",
        "",
        "## Disposition legend",
        "",
        "| Disposition | Meaning |",
        "|-------------|---------|",
        "| port | Change for H2/new default runtime |",
        "| sqlH2 | Add/replace install SQL dialect branch |",
        "| generalize | Broaden Derby-only API/predicate |",
        "| migration-only | Keep for Derby→new upgrade window only |",
        "| docs-only | Docs / archive metadata stamp |",
        "| unknown | Needs human triage |",
        "",
        "Heuristics are **starting points** — edit Disposition column as you triage.",
        "",
        "## Inventory",
        "",
        "| Path | Sample line | Patterns | Hits | Disposition | Notes |",
        "|------|-------------|----------|------|-------------|-------|",
    ]
    for r in rows:
        pat = ", ".join(r["patterns"])
        snip = r["snippet"].replace("|", "\\|")
        lines.append(
            f"| `{r['path']}` | {r['line']}: `{snip}` | {pat} | {r['hit_count']} | {r['disposition']} | |"
        )

    lines.extend(
        [
            "",
            "## Next steps (T005)",
            "",
            "1. Triage every `unknown` row.",
            "2. Prioritize install (`installRepository.xml`, NetworkServer, 1527), runtime (`isDerby*`, dialects), Liquibase `dbms=derby`, Jetty `derby.drda`, DTS scripts, p13n.",
            "3. Freeze QC-001 when unknown count is 0 at GA.",
            "",
        ]
    )
    return "\n".join(lines) + "\n"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Inventory Derby surface for #548")
    parser.add_argument(
        "--root",
        type=Path,
        default=None,
        help="Repo root (default: parent of scripts/)",
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=None,
        help="Output markdown path",
    )
    args = parser.parse_args(argv)

    root = (args.root or repo_root_from_script()).resolve()
    out = args.out or (
        root / "specs" / "548-derby-embedded-migration" / "checklists" / "derby-surface-inventory.md"
    )

    files = iter_files(root)
    all_hits: list[dict] = []
    for f in files:
        all_hits.extend(scan_file(f, root))

    # Ensure p13n-ds derby resource paths appear even if only filename matched weakly
    for f in files:
        rel = f.relative_to(root).as_posix()
        if "p13n" in rel.lower() and "derby" in rel.lower():
            if not any(h["path"] == rel for h in all_hits):
                all_hits.append(
                    {
                        "path": rel,
                        "line": 1,
                        "patterns": ["p13n", "derby"],
                        "snippet": "(path-level hit)",
                    }
                )

    md = render_markdown(all_hits, root)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(md, encoding="utf-8")
    print(f"Wrote {out} ({len(all_hits)} line hits, see file for path count)", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
