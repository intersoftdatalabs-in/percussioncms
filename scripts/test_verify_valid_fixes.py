#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/verify-valid-fixes.py.

The CLI surface + happy path is exercised against the good fixture (every
valid row has a linked_pr). The bad fixture has no valid rows (only obsolete /
false-positive / accepted-risk), so the happy-path assertion is trivially
satisfied — the test uses the good fixture as the source of valid rows.
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "verify-valid-fixes.py"
GOOD_FIXTURE = SCRIPT_DIR / "test-fixtures" / "triage-good.md"


def _run(*args: str) -> subprocess.CompletedProcess:
    cmd = [sys.executable, str(SCRIPT), *args]
    return subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
    )


def test_help_exits_zero_and_prints_usage() -> None:
    result = _run("--help")
    assert result.returncode == 0, result.stderr
    assert "usage:" in result.stdout.lower()


def test_missing_triage_exits_two(tmp_path: Path) -> None:
    result = _run("--triage", str(tmp_path / "does-not-exist.md"))
    assert result.returncode == 2


@pytest.mark.skipif(not GOOD_FIXTURE.is_file(), reason="good fixture missing")
def test_good_fixture_passes() -> None:
    """The good fixture's ``valid`` row has ``linked_pr = 1234``."""
    result = _run("--triage", str(GOOD_FIXTURE))
    assert result.returncode == 0, result.stderr + result.stdout


def test_valid_row_without_linked_pr_fails(tmp_path: Path) -> None:
    triage = tmp_path / "triage.md"
    triage.write_text(
        "# Triage\n\n"
        "| # | alert_id | rule_id | severity | file_path | module_owner | disposition | target_action | target_milestone | linked_pr | notes |\n"
        "|---|----------|---------|----------|-----------|--------------|-------------|---------------|------------------|-----------|-------|\n"
        "| 1 | 1 | `x` | low | `Foo.java` | `WebUI/` | valid | fix | 8.2 | — | note |\n",
        encoding="utf-8",
    )
    result = _run("--triage", str(triage))
    assert result.returncode == 1
    assert "alert 1" in (result.stdout + result.stderr)


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
