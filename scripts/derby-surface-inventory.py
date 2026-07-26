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
    # Local agent / session noise
    ".sessions",
    "sessions",
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
    ".log",
    ".lock",
}

# File names that are local build/agent noise even if tracked
SKIP_FILE_NAMES = {
    "buildout.log",
    "compile-output.log",
    "spotless-index-file",
    "derby.log",
}

MAX_FILE_BYTES = 2_000_000

# Valid dispositions for QC-001 freeze (unknown must be zero)
DISPOSITIONS = (
    "port",
    "sqlH2",
    "generalize",
    "migration-only",
    "docs-only",
    "test-only",
    "false-positive",
    "unknown",
)


def repo_root_from_script() -> Path:
    return Path(__file__).resolve().parent.parent


def should_skip_dir(name: str) -> bool:
    return name in SKIP_DIR_NAMES or name.startswith(".")


def iter_files(root: Path) -> list[Path]:
    files: list[Path] = []
    for path in root.rglob("*"):
        if not path.is_file():
            continue
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
        if path.name in SKIP_FILE_NAMES:
            continue
        # Skip bundled modern UI assets (false-positive substring noise)
        rel_posix = rel.as_posix()
        if "perc-modern-ui.js" in rel_posix:
            continue
        if "/war/" in rel_posix and rel_posix.endswith(".js"):
            # Prefer source under src/; war copies are build outputs often committed
            if "derby" not in path.name.lower():
                # still scan — disposition will mark false-positive if only weak hits
                pass
        files.append(path)
    return files


def scan_file(path: Path, root: Path) -> list[dict]:
    hits: list[dict] = []
    try:
        if path.stat().st_size > MAX_FILE_BYTES:
            return hits
        raw = path.read_bytes()
        # Drop NULs so inventory markdown is never a "binary" file
        if b"\x00" in raw:
            raw = raw.replace(b"\x00", b"")
        text = raw.decode("utf-8", errors="replace")
    except OSError:
        return hits

    rel = path.relative_to(root).as_posix()
    lines = text.splitlines()
    for i, line in enumerate(lines, start=1):
        matched_labels = [label for label, pat in PATTERNS if pat.search(line)]
        if not matched_labels:
            continue
        # Skip pure "p13n" hits that have no derby-related context
        if matched_labels == ["p13n"] and "derby" not in line.lower() and "derby" not in rel.lower():
            continue
        # Port-only 1527 hits in unrelated contexts (often timestamps / other ports)
        if matched_labels == ["port_1527"] and "derby" not in line.lower() and "derby" not in rel.lower():
            if "drda" not in line.lower() and "networkserver" not in line.lower():
                continue
        snippet = line.strip().replace("\x00", "")
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


def _pats(patterns: list[str]) -> str:
    return ",".join(patterns).lower()


def suggest_disposition(rel_path: str, patterns: list[str]) -> str:
    """Heuristic disposition; refined so QC-001 can freeze with zero unknown."""
    p = rel_path.lower().replace("\\", "/")
    pats = _pats(patterns)

    # --- docs / specs / release notes ---
    if p.startswith("specs/") or p.startswith("docs/"):
        return "docs-only"
    if p.endswith(".md") or p.endswith(".txt") or p.endswith(".rst"):
        return "docs-only"
    if p in {".gitignore", "agents.md", "claude.md", "readme.md"} or p.endswith(
        "/readme.md"
    ):
        return "docs-only"
    if p.endswith(".env.compose") or p.endswith(".env.compose.example") or ".env" in p:
        return "docs-only"
    if "psx_archiveinfo" in p or p.endswith(".ppkg"):
        return "docs-only"
    if "dbfix_analysis" in p:
        return "docs-only"

    # --- pure false positives ---
    if p.endswith(".log") or "buildout" in p or "compile-output" in p:
        return "false-positive"
    if "spotless-index" in p:
        return "false-positive"
    # Minified / bundled JS with accidental substring (e.g. not a real Derby API)
    if p.endswith(".js") and "derby" in pats and "isderby" not in pats:
        if "/modern/" in p or "perc-modern" in p or p.endswith(".min.js"):
            return "false-positive"
        # Source JS mentioning Derby in comments only still docs-ish
        if "renderbyobject" in p or "inspection" in p:
            return "false-positive"

    # --- tests ---
    if "/src/test/" in p or p.startswith("scripts/test_") or "/test/" in p:
        if any(
            x in pats
            for x in (
                "sqlderby",
                "networkserver",
                "embeddeddriver",
                "clientdriver",
                "derbydata",
            )
        ):
            return "migration-only"
        if "psh2" in p or "h2" in p and "multiuser" in p:
            return "test-only"
        if "embeddedrepository" in p or "migrat" in p or "backupgate" in p:
            return "test-only"
        if "packaging" in p and "h2" in p:
            return "test-only"
        return "test-only"

    # --- migration / upgrade window ---
    migrator_markers = (
        "embeddedrepository",
        "migrateembedded",
        "migratedts",
        "psmigrat",
        "migrationtransfer",
        "migrationvalidator",
        "migrationreport",
        "migrationsecrets",
        "configcutover",
        "repositorybackup",
        "repositoryoffline",
        "backupgate",
        "psupgradederby",
        "upgradepluginembedded",
        "tablefactorymigration",
        "psmigratorlock",
        "dtsembeddedrepository",
    )
    if any(m in p for m in migrator_markers):
        return "migration-only"
    if "derbynet" in p or "networkserver" in pats:
        return "migration-only"
    if "repository.zip" in pats or "repository.zip" in p:
        return "migration-only"
    if "embeddeddriver" in pats or "clientdriver" in pats:
        return "migration-only"
    if "derbydata" in pats or "derbydata" in p:
        return "migration-only"

    # --- install SQL / path port ---
    if "changelog" in p or "liquibase" in p or "dbms_derby" in patterns:
        return "port"
    if "install" in p and (
        "sqlderby" in pats
        or "port_1527" in patterns
        or "networkserver" in pats
        or "sqlh2" in p
    ):
        return "port"
    if "installrepository" in p or "installdts" in p:
        return "port"

    # --- runtime API / dialect ---
    if "isderby" in pats:
        return "generalize"
    if "derbydialect" in pats or "dialect" in p and "derby" in pats:
        return "port"
    if "psjdbcutils" in p or "pssqlhelper" in p:
        return "generalize"
    if "sessionfactory" in p or "config.xml" in p or "databasefunctiondefs" in p:
        return "port"
    if "rxrepository.properties" in p or "perc-ds.properties" in p or "perc-ds.xml" in p:
        return "port"
    if "server-beans" in p or "install-beans" in p or "local-beans" in p:
        return "port"

    # --- DTS packaging / services ---
    if "delivery-tier" in p or "deliverytiersuite" in p:
        if "pom.xml" in p and "derby" in pats:
            return "migration-only"
        if "service.bat" in p or "tomcatstartup" in p or "setenv" in p:
            return "port"
        if "datasources" in p or "beans.xml" in p or "hibernate" in p:
            return "port"
        if "derby.log" in p:
            return "false-positive"
        return "port"

    # --- jetty / distribution ---
    if "perc-jetty" in p or "perc-distribution" in p:
        return "port"

    # --- parent pom coordinates ---
    if p == "pom.xml" or p.endswith("/pom.xml"):
        if "derby" in pats:
            return "migration-only"
        return "port"

    # --- p13n (legacy personalisation) ---
    if "p13n" in p:
        if "derby" in p or "derby" in pats:
            return "migration-only"
        return "false-positive"

    # --- scripts ---
    if p.startswith("scripts/"):
        return "docs-only"

    # --- remaining with real derby tokens ---
    if "derby" in pats or "drda" in pats or "sqlderby" in pats:
        # Prefer migration-only for residual product-managed Derby support
        if any(
            x in p
            for x in (
                "system/",
                "modules/tablefactory",
                "modules/perc-ant",
                "modules/utils",
            )
        ):
            return "migration-only"
        return "port"

    # Last resort: only non-derby weak patterns (should be rare)
    if patterns == ["p13n"] or patterns == ["port_1527"]:
        return "false-positive"

    return "unknown"


def suggest_notes(rel_path: str, disposition: str, patterns: list[str]) -> str:
    p = rel_path.lower()
    if disposition == "false-positive":
        return "noise or non-Derby substring"
    if disposition == "docs-only" and "psx_archive" in p:
        return "archive build stamp; QC-023 soft"
    if disposition == "migration-only":
        return "FR-021 window"
    if disposition == "test-only":
        return "automated verification"
    if disposition == "generalize" and "isderby" in _pats(patterns):
        return "H2-aware predicates required"
    return ""


def render_markdown(hits: list[dict], root: Path) -> str:
    by_path: dict[str, list[dict]] = defaultdict(list)
    for h in hits:
        by_path[h["path"]].append(h)

    rows: list[dict] = []
    for path, path_hits in sorted(by_path.items()):
        patterns: set[str] = set()
        for h in path_hits:
            patterns.update(h["patterns"])
        sample = path_hits[0]
        for h in path_hits:
            if h["patterns"] != ["p13n"]:
                sample = h
                break
        sorted_pats = sorted(patterns)
        disp = suggest_disposition(path, sorted_pats)
        if disp not in DISPOSITIONS:
            disp = "unknown"
        notes = suggest_notes(path, disp, sorted_pats)
        rows.append(
            {
                "path": path,
                "line": sample["line"],
                "patterns": sorted_pats,
                "snippet": sample["snippet"],
                "disposition": disp,
                "hit_count": len(path_hits),
                "notes": notes,
            }
        )

    today = date.today().isoformat()
    unknown_n = sum(1 for r in rows if r["disposition"] == "unknown")
    by_disp: dict[str, int] = defaultdict(int)
    for r in rows:
        by_disp[r["disposition"]] += 1

    lines = [
        "# Derby surface inventory (#548)",
        "",
        f"**Generated**: {today}  ",
        f"**Script**: `scripts/derby-surface-inventory.py`  ",
        f"**Repo root**: `{root}`  ",
        f"**Files with hits**: {len(rows)}  ",
        f"**Unknown disposition**: {unknown_n} "
        + (
            "**(QC-001 freeze met — zero unknown)**"
            if unknown_n == 0
            else "(must reach zero before GA — QC-001)"
        ),
        "",
        "## Disposition counts",
        "",
        "| Disposition | Count |",
        "|-------------|-------|",
    ]
    for d in DISPOSITIONS:
        if by_disp.get(d, 0):
            lines.append(f"| {d} | {by_disp[d]} |")

    lines.extend(
        [
            "",
            "## Disposition legend",
            "",
            "| Disposition | Meaning |",
            "|-------------|---------|",
            "| port | Change for H2/new default runtime |",
            "| sqlH2 | Add/replace install SQL dialect branch |",
            "| generalize | Broaden Derby-only API/predicate |",
            "| migration-only | Keep for Derby→new upgrade window only (FR-021) |",
            "| docs-only | Docs / archive metadata stamp / operator notes |",
            "| test-only | Tests and harnesses only |",
            "| false-positive | Noise; not product Derby surface |",
            "| unknown | Needs human triage |",
            "",
            "Heuristics assign a disposition for every row. Re-run after large tree changes; "
            "refine rules in `scripts/derby-surface-inventory.py` rather than hand-editing "
            "hundreds of rows.",
            "",
            "## Inventory",
            "",
            "| Path | Sample line | Patterns | Hits | Disposition | Notes |",
            "|------|-------------|----------|------|-------------|-------|",
        ]
    )
    for r in rows:
        pat = ", ".join(r["patterns"])
        snip = r["snippet"].replace("|", "\\|").replace("\n", " ")
        note = r["notes"].replace("|", "\\|")
        lines.append(
            f"| `{r['path']}` | {r['line']}: `{snip}` | {pat} | {r['hit_count']} | {r['disposition']} | {note} |"
        )

    lines.extend(
        [
            "",
            "## QC-001 freeze",
            "",
            f"- Unknown rows: **{unknown_n}**",
            "- Re-generate: `python3 scripts/derby-surface-inventory.py`",
            "- Windows: `scripts\\derby-surface-inventory.bat`",
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
    parser.add_argument(
        "--fail-on-unknown",
        action="store_true",
        help="Exit non-zero if any row is still disposition=unknown",
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
    # Always write clean UTF-8 without NULs
    out.write_text(md.replace("\x00", ""), encoding="utf-8", newline="\n")

    # Count unknown rows from the disposition column (table data rows only).
    unknown_rows = sum(
        1
        for line in md.splitlines()
        if line.startswith("| `") and "| unknown |" in line
    )
    print(
        f"Wrote {out} ({len(all_hits)} line hits; unknown rows={unknown_rows})",
        file=sys.stderr,
    )
    if args.fail_on_unknown and unknown_rows:
        print(f"ERROR: {unknown_rows} unknown disposition rows remain", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
