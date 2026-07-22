#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/filter-stale-alerts.py.

Exercises the alerts parser (``_parse_alerts``) and the stale-row writer
(``write_stale``) directly. The script is invoked end-to-end on a small
in-memory alerts fixture, which produces a non-empty stale file because none
of the fixture paths are tracked.
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "filter-stale-alerts.py"


def _run(*args: str, cwd: Path | None = None) -> subprocess.CompletedProcess:
    cmd = [sys.executable, str(SCRIPT), *args]
    return subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
        cwd=str(cwd) if cwd else None,
    )


def test_help_exits_zero_and_prints_usage() -> None:
    result = _run("--help")
    assert result.returncode == 0, result.stderr
    assert "usage:" in result.stdout.lower()


def test_bogus_flag_exits_nonzero() -> None:
    """argparse rejects unknown flags with a non-zero exit. The exact error
    substring varies between argparse versions (``unrecognized arguments`` on
    CPython 3.9+, ``the following arguments are required: --input`` when the
    unknown token is consumed as a positional for a required flag), so accept
    any of those substrings."""
    result = _run("--totally-not-a-flag-12345")
    assert result.returncode != 0
    combined = (result.stderr + result.stdout).lower()
    assert (
        "totally-not-a-flag-12345" in combined
        or "unrecognized" in combined
        or "usage" in combined
        or "required" in combined
    )


def test_parse_alerts_extracts_tuples() -> None:
    import importlib.util

    spec = importlib.util.spec_from_file_location("filter_alerts", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    body = (
        "# Code Scanning Alerts for o/r\n\n"
        "State filter: open\n\n"
        "- **Alert #42** — `js/xss-through-dom` (high, CodeQL)\n"
        "  - **Tool:** CodeQL\n"
        "  - **State:** open\n"
        "  - **Created:** 2026-07-21T00:00:00Z\n"
        "  - **URL:** https://gh/x\n"
        "  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTable/x.js:10\n"
        "  - **Message:** bad x\n\n"
        "- **Alert #43** — `java/sql-injection` (high, CodeQL)\n"
        "  - **Location:** projects/sitemanage/src/main/java/PSPageDaoHelper.java:42\n"
    )
    tuples = module._parse_alerts(body)
    assert tuples == [
        ("42", "js/xss-through-dom", "WebUI/src/main/webapp/cm/widgets/PercDataTable/x.js"),
        ("43", "java/sql-injection", "projects/sitemanage/src/main/java/PSPageDaoHelper.java"),
    ]


def test_end_to_end_writes_stale_file(tmp_path: Path) -> None:
    """Drive the script end-to-end against an alerts fixture that references
    paths NOT present in the working tree; expect a non-empty stale file."""
    alerts = tmp_path / "alerts.md"
    alerts.write_text(
        "# Code Scanning Alerts for o/r\n\n"
        "State filter: open\n\n"
        "- **Alert #42** — `js/xss-through-dom` (high, CodeQL)\n"
        "  - **Location:** __definitely_not_tracked_xyz.js:10\n",
        encoding="utf-8",
    )
    stale = tmp_path / "stale.md"
    result = _run("--input", str(alerts), "--stale-output", str(stale))
    assert result.returncode == 0, result.stderr
    body = stale.read_text(encoding="utf-8")
    assert "Stale Scanner-Cache Alerts" in body
    assert "__definitely_not_tracked_xyz.js" in body


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
