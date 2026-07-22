#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/verify-distribution-archive.sh.

Purpose
-------
Rebuild ``modules/perc-distribution-tree`` (and ``modules/perc-packages``) and
assert none of the files listed in the obsolete-removal inventory
(``tmp/gh-codeql-alerts/removed-files.txt``) appear in the resulting JARs or
``.ppkg`` installer archive.

Usage
-----
::

    python3 scripts/verify-distribution-archive.py
        [--removed-files <path>]
        [--distribution-jar <path>]

Behavioral Notes
----------------
- The bash version invokes Maven (``mvn``) directly to avoid ``mvn-env.sh``'s
  cross-filesystem ``mv`` issues on Windows. The Python port does the same
  (FR-008: subprocess.run with argv list, shell=False).
- ``unzip -l`` is replaced by ``zipfile.ZipFile.namelist()`` (stdlib only).
- The script honors ``JAVA_HOME_21`` / ``MAVEN`` overrides (matching the bash
  original) via environment variables; tests pass ``--skip-mvn`` to bypass the
  Maven invocation and exercise only the archive scan.
"""
from __future__ import annotations

import argparse
import logging
import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify_distribution_archive.py",
        description="Rebuild distribution JARs and assert removed files are absent.",
    )
    parser.add_argument(
        "--removed-files",
        default="tmp/gh-codeql-alerts/removed-files.txt",
        help="Removed-files inventory",
    )
    parser.add_argument(
        "--distribution-jar",
        default="modules/perc-distribution-tree/target/perc-distribution-tree-*.jar",
        help="Distribution-jar glob (overrides the build)",
    )
    parser.add_argument(
        "--skip-mvn",
        action="store_true",
        help="Skip the Maven rebuild (test-only; exercises only the archive scan)",
    )
    return parser


def _read_removed_list(path: Path) -> list[str]:
    """Read the removed-files inventory, skipping blank / comment lines."""
    if not path.is_file():
        raise FileNotFoundError(path)
    out: list[str] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        out.append(stripped)
    return out


def _jar_contains(jar_path: Path, name_substr: str) -> bool:
    """Return True if any entry in ``jar_path`` matches ``name_substr``."""
    try:
        with zipfile.ZipFile(jar_path, "r") as zf:
            return any(name_substr in entry.filename for entry in zf.infolist())
    except (OSError, zipfile.BadZipFile):
        return False


def _scan_archives(archive_paths: list[Path], removed_basenames: list[str]) -> dict[str, list[str]]:
    """Return a map of ``removed_basename -> [archives that contain it]``."""
    found: dict[str, list[str]] = {b: [] for b in removed_basenames}
    for archive in archive_paths:
        for basename in removed_basenames:
            if _jar_contains(archive, basename):
                found[basename].append(str(archive))
    return found


def _discover_distribution_archives(dist_glob: str) -> list[Path]:
    """Resolve the distribution-jar glob against the repo root."""
    base = REPO_ROOT
    matches = sorted(base.glob(dist_glob))
    # Also pick up any sibling perc-packages JARs.
    matches.extend(sorted((base / "modules" / "perc-packages" / "target").glob("*.jar")))
    return matches


def _discover_ppkg_archives() -> list[Path]:
    """Find any ``.ppkg`` archives anywhere in the working tree.

    Excludes ``.git`` and ``node_modules`` to keep the search bounded.
    """
    candidates: list[Path] = []
    for path in REPO_ROOT.rglob("*.ppkg"):
        rel = path.relative_to(REPO_ROOT)
        parts = rel.parts
        if any(part in (".git", "node_modules", "target") for part in parts):
            continue
        candidates.append(path)
    return candidates


def _build_distribution() -> int:
    """Invoke the Maven build (matches bash ``mvn_cmd`` honoring JAVA_HOME_21)."""
    java_home = os.environ.get("JAVA_HOME_21", "")
    mvn = os.environ.get("MAVEN", "mvn")
    if java_home and Path(java_home).is_dir():
        env = {**os.environ, "JAVA_HOME": java_home}
    else:
        env = os.environ.copy()
    cmd = [mvn, "-Djava.io.tmpdir=tmp", "-pl", "modules/perc-distribution-tree", "-am",
           "-DskipTests", "clean", "package"]
    result = subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=1800,
        env=env,
    )
    return result.returncode


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    removed_path = Path(args.removed_files)
    try:
        removed = _read_removed_list(removed_path)
    except FileNotFoundError:
        print(f"FAIL: removed-files inventory not found: {removed_path}", file=sys.stderr)
        return 1

    if not args.skip_mvn:
        print("==> clean package of modules/perc-distribution-tree (with deps)")
        rc = _build_distribution()
        if rc != 0:
            print(f"FAIL: Maven build returned {rc}", file=sys.stderr)
            return 1

    removed_basenames = [Path(name).name for name in removed if name]
    jar_paths = _discover_distribution_archives(args.distribution_jar)
    ppkg_paths = _discover_ppkg_archives()
    all_archives = jar_paths + ppkg_paths

    found = _scan_archives(all_archives, removed_basenames)
    fail = False
    for basename, hits in found.items():
        if hits:
            print(f"  FAIL: {basename} still present in: {hits}", file=sys.stderr)
            fail = True

    if fail:
        print("verify-distribution-archive: FAIL", file=sys.stderr)
        return 1
    print("verify-distribution-archive: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
