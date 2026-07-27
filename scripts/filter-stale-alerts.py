#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/filter-stale-alerts.sh.

Purpose
-------
Scan ``<alerts.md>`` and emit ``<stale-out.md>`` containing rows whose
``most_recent_instance.location.path`` is no longer in ``git ls-files`` on the
current branch. The CodeQL dashboard sometimes caches alerts for deleted
files; this script filters them out of the open-alert count for release
readiness (per spec 004 T007b).

Usage
-----
::

    python3 scripts/filter-stale-alerts.py <alerts.md> <stale-out.md>

Behavioral Notes
----------------
- bash ``awk`` extraction of alert tuples + ``comm -23`` set-difference is
  replaced by a Python parser that uses ``set`` arithmetic on the path set.
- ``git ls-files`` is invoked via ``subprocess.run(..., shell=False)``.
- Line-ending normalization: the script preserves the host OS line endings on
  output (FR-007: no forced conversion).
"""
from __future__ import annotations

import argparse
import logging
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

ALERT_HEADER_RE = re.compile(r"^- \*\*Alert #(?P<id>[^*]+)\*\*")
LOCATION_RE = re.compile(r"\*\*Location:\*\*\s+(?P<path>[^:\s]+)(?::\d+)?")
RULE_RE = re.compile(r"`(?P<rule>[^`]+)`")


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="filter_stale_alerts.py",
        description="Emit stale (deleted-file) alerts to a separate Markdown file.",
    )
    parser.add_argument(
        "--input",
        required=True,
        help="Input alerts markdown file",
    )
    parser.add_argument(
        "--stale-output",
        default=None,
        help="Stale-rows output path (default: <input>.stale.md)",
    )
    return parser


def _git(args: list[str]) -> tuple[int, str]:
    result = subprocess.run(
        ["git", *args],
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
    )
    return (result.returncode, (result.stdout or "").strip())


def _parse_alerts(text: str) -> list[tuple[str, str, str]]:
    """Return ``[(alert_id, rule_id, path), ...]`` from an alerts.md payload."""
    tuples: list[tuple[str, str, str]] = []
    current_id = ""
    current_rule = ""
    for line in text.splitlines():
        m_id = ALERT_HEADER_RE.search(line)
        if m_id:
            current_id = m_id.group("id").strip().strip("*")
            m_rule = RULE_RE.search(line)
            current_rule = m_rule.group("rule") if m_rule else "<unknown>"
            continue
        m_loc = LOCATION_RE.search(line)
        if m_loc:
            path = m_loc.group("path").lstrip("./")
            if path and path != "<no-path>":
                tuples.append((current_id, current_rule, path))
    return tuples


def _tracked_paths() -> set[str]:
    rc, out = _git(["ls-files"])
    if rc != 0:
        LOGGER.warning("git ls-files failed (rc=%d); treating empty tracked set", rc)
        return set()
    return {line for line in out.splitlines() if line}


def _current_branch() -> str:
    rc, out = _git(["rev-parse", "--abbrev-ref", "HEAD"])
    if rc != 0 or not out:
        return "unknown"
    return out


def _origin_url() -> str:
    rc, out = _git(["config", "--get", "remote.origin.url"])
    if rc != 0 or not out:
        return "unknown"
    return out


def write_stale(
    tuples: list[tuple[str, str, str]],
    stale: list[tuple[str, str, str]],
    stale_out: Path,
    branch: str,
    repo_url: str,
) -> int:
    """Write the stale-rows Markdown file. Returns the count of emitted rows."""
    stale_out.parent.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    header = [
        "# Stale Scanner-Cache Alerts",
        "",
        f"**Repository**: {repo_url}",
        f"**Branch**: {branch}",
        f"**Generated**: {timestamp} (UTC)",
        "",
        "These alerts reference a file path that is no longer present in this",
        "branch (per `git ls-files`). The CodeQL dashboard can cache alerts for",
        "deleted files; these rows are EXCLUDED from the open-alert count for",
        "release readiness. See",
        "`specs/004-zero-code-scanning-alerts/contracts/README.md` C1.",
        "",
        "| alert_id | rule_id | path | last_seen_branch |",
        "|----------|---------|------|------------------|",
    ]
    rows = [f"| {aid} | `{rid}` | `{path}` | {branch} |" for aid, rid, path in stale]
    stale_out.write_text("\n".join(header + rows) + "\n", encoding="utf-8")
    return len(stale)


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    alerts_path = Path(args.input)
    if not alerts_path.is_file():
        LOGGER.error("alerts file not found: %s", alerts_path)
        return 2
    stale_out = Path(args.stale_output) if args.stale_output else alerts_path.with_suffix(
        alerts_path.suffix + ".stale.md"
    )

    text = alerts_path.read_text(encoding="utf-8")
    tuples = _parse_alerts(text)
    tracked = _tracked_paths()
    stale_set = sorted({path for _, _, path in tuples if path not in tracked})
    stale = [(aid, rid, path) for aid, rid, path in tuples if path in stale_set]
    branch = _current_branch()
    repo_url = _origin_url()
    count = write_stale(tuples, stale, stale_out, branch, repo_url)
    print(f"stale-alerts: {count}  (file: {stale_out})", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
