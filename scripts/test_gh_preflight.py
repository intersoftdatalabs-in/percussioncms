#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/gh-preflight.py.

Tests the CLI surface (help + bad flag) without invoking ``gh`` or ``git``:
the real network-requiring path is intentionally not exercised here. The
"required tool missing" failure path is tested by pointing ``--require`` at a
definitely-absent tool name.
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "gh-preflight.py"


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


def test_bogus_flag_exits_nonzero() -> None:
    result = _run("--no-such-flag")
    assert result.returncode != 0
    combined = (result.stderr + result.stdout).lower()
    assert (
        "unrecognized" in combined
        or "no-such-flag" in combined
        or "usage" in combined
    )


def test_missing_required_tool_exits_two() -> None:
    """The bash original exits 1 when ``gh`` is missing; the contract spec
    (``contracts/cli-schemas.md``) tightens this to exit 2 for missing prereqs.
    """
    result = _run("--require", "definitely-not-on-path-xyz")
    assert result.returncode == 2
    assert "definitely-not-on-path-xyz" in (result.stderr + result.stdout)


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
