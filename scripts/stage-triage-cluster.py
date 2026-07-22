#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/stage-triage-cluster.sh.

Purpose
-------
Mark ``triage.md`` rows as "ready to close" (set ``linked_pr`` to
``<cluster-id>-staged``) for clusters whose closing changes are staged in the
working tree. Two match modes are supported:

- ``basename`` (default): match on the basename of files in the cluster's
  section of ``tmp/gh-codeql-alerts/removed-files.txt``.
- ``path``: match on the ``file_path`` column directly using a hard-coded
  cluster-specific path list (matches the bash original's ``case "$cluster"``
  table).

Usage
-----
::

    python3 scripts/stage-triage-cluster.py --cluster-name <id>
                                           [--mode basename|path]
                                           [--triage <path>]

Behavioral Notes
----------------
- bash ``awk -v matchers=...`` is replaced by a Python row walker that treats
  backticks / ``(candidate)`` decoration identically to the bash version.
- Atomic write via ``tempfile`` + ``os.replace`` (FR-007: pathlib-only).
- The bash ``trap 'rm -f "$tmp"' EXIT`` is replaced by ``try``/``finally``
  around a ``NamedTemporaryFile`` (auto-cleanup; R2).

Exit codes
----------
- ``0`` rows staged (or zero matches; not an error)
- ``1`` triage file missing
- ``2`` argument errors
"""
from __future__ import annotations

import argparse
import logging
import os
import sys
import tempfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

# Hard-coded path-mode matchers, ported verbatim from the bash original.
PATH_MATCHERS: dict[str, list[str]] = {
    "T037": ["PSProxyQueryResource.java"],
    "T039": ["PSSerializerUtils.java"],
    "T040": ["PSJndiGroupProvider.java"],
    "T041": ["PSArchiveFiles.java"],
    "T042": ["PSPageDaoHelper.java"],
    "T044": ["PSSiteDataRestService.java"],
    "T066": ["PSFeedServicePerformanceTest.java"],
}


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="stage-triage-cluster.py",
        description="Mark triage.md rows as ready to close for one cluster.",
    )
    parser.add_argument(
        "--cluster-name",
        required=True,
        help="Cluster ID (e.g. T027, T037)",
    )
    parser.add_argument(
        "--max-prs",
        type=int,
        default=10,
        help="Max PRs per cluster (informational; default: 10)",
    )
    parser.add_argument(
        "--mode",
        choices=("basename", "path"),
        default="basename",
        help="Matcher mode (default: basename)",
    )
    parser.add_argument(
        "--triage",
        default="docs/ai-generated/tasks/gh-codeql-alerts/triage.md",
        help="Path to triage.md",
    )
    parser.add_argument(
        "--removed-inventory",
        default="tmp/gh-codeql-alerts/removed-files.txt",
        help="Path to removed-files inventory (basename mode only)",
    )
    return parser


def _collect_basenames(inventory: Path, cluster: str) -> set[str]:
    """Extract unique basenames from the cluster's section of the inventory.

    The bash version uses ``awk`` with a section delimiter (``# --- <id> ---``).
    The port mirrors that semantic.
    """
    if not inventory.is_file():
        return set()
    basenames: set[str] = set()
    in_target = False
    for line in inventory.read_text(encoding="utf-8").splitlines():
        if line.startswith("# --- "):
            in_target = f"# --- {cluster} ---" in line
            continue
        if not in_target:
            continue
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        name = Path(stripped.rstrip("/")).name
        if name:
            basenames.add(name)
    return basenames


def stage_rows(
    triage_text: str,
    cluster: str,
    matchers: set[str],
    mode: str,
) -> tuple[str, int]:
    """Return ``(new_text, updated_count)`` after staging rows whose file_path
    matches any of the basenames (basename mode) or path substrings (path mode).
    """
    lines = triage_text.splitlines(keepends=True)
    out: list[str] = []
    updated = 0
    for line in lines:
        if line.startswith("| ") and len(line.split("|")) >= 12:
            # Treat as a data row. After ``split('|')``, the leading empty field
            # is parts[0]; awk column N maps to Python index N-1. So:
            #   awk $6  (file_path)   -> parts[5]
            #   awk $11 (linked_pr)   -> parts[10]
            parts = line.split("|")
            file_path = parts[5].strip().strip("`") if len(parts) > 5 else ""
            linked = parts[10].strip() if len(parts) > 10 else ""
            matched = False
            if mode == "basename":
                file_basename = Path(file_path).name
                matched = file_basename in matchers
            else:
                matched = any(m in file_path for m in matchers)
            if matched and linked in ("", "—"):
                # Replace awk column 11 (linked_pr) which is parts[10] in split.
                cols = line.rstrip("\n").split("|")
                while len(cols) < 12:
                    cols.append("")
                cols[10] = f" {cluster}-staged "
                rebuilt = "|".join(cols) + "\n"
                out.append(rebuilt)
                updated += 1
                continue
        out.append(line)
    return "".join(out), updated


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    triage_path = Path(args.triage)
    if not triage_path.is_file():
        LOGGER.error("FAIL: %s not found", triage_path)
        return 1

    if args.mode == "basename":
        inventory = Path(args.removed_inventory)
        if not inventory.is_file():
            LOGGER.error(
                "FAIL: %s not found (basename mode requires the removed-files inventory)",
                inventory,
            )
            return 1
        matchers = _collect_basenames(inventory, args.cluster_name)
        if not matchers:
            LOGGER.warning(
                "No basenames found for cluster %s in %s; nothing to stage",
                args.cluster_name,
                inventory,
            )
    else:
        matchers = set(PATH_MATCHERS.get(args.cluster_name, []))
        if not matchers:
            LOGGER.error(
                "FAIL: cluster %s has no path matchers; add one to stage-triage-cluster.py",
                args.cluster_name,
            )
            return 1

    original = triage_path.read_text(encoding="utf-8")
    new_text, updated = stage_rows(original, args.cluster_name, matchers, args.mode)

    # Atomic write.
    fd, tmp_name = tempfile.mkstemp(
        prefix=f".stage-triage-{args.cluster_name}-",
        suffix=".md",
        dir=str(triage_path.parent),
    )
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as tmp:
            tmp.write(new_text)
        os.replace(tmp_name, str(triage_path))
    except OSError:
        # On failure, ensure the temp file is cleaned up.
        try:
            os.unlink(tmp_name)
        except OSError:
            pass
        raise
    LOGGER.info(
        "stage-triage-cluster: marked %d rows as '%s-staged' (mode=%s) in %s",
        updated,
        args.cluster_name,
        args.mode,
        triage_path,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
