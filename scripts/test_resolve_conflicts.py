#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/resolve-conflicts.py.

The "happy path" for ``resolve-conflicts.py`` is the ``--strategy manual`` mode
run inside a real git repo that has no conflicts (the typical CI / day-to-day
state). The bash original exits 0 in that case; this port preserves that.

The failure path is exercised by passing a bogus strategy value (argparse
rejects it before any git call).
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "resolve-conflicts.py"
REPO_ROOT = SCRIPT_DIR.parent


def _run(*args: str, cwd: Path | None = None) -> subprocess.CompletedProcess:
    cmd = [sys.executable, str(SCRIPT), *args]
    return subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
        cwd=str(cwd or REPO_ROOT),
    )


def test_help_exits_zero_and_prints_usage() -> None:
    result = _run("--help")
    assert result.returncode == 0, result.stderr
    assert "usage:" in result.stdout.lower()


def test_bogus_strategy_exits_nonzero() -> None:
    result = _run("--strategy", "definitely-not-a-real-strategy")
    assert result.returncode != 0
    combined = (result.stderr + result.stdout).lower()
    assert "invalid choice" in combined or "definitely-not-a-real-strategy" in combined


def test_no_conflicts_in_clean_repo_exits_zero() -> None:
    """Run from the repo root (which on the test host has no unresolved
    conflicts); manual strategy lists nothing and exits 0 via the dry-run path."""
    result = _run("--strategy", "manual", "--dry-run")
    assert result.returncode == 0, result.stderr


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
