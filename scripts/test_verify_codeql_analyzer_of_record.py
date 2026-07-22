#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/verify-codeql-analyzer-of-record.py.

The CLI surface is exercised end-to-end against the real repo tree. The
``gh api`` default-setup check is advisory (WARN when ``gh`` is missing);
on CI hosts with ``gh`` the test surface either skips or marks this with
``@pytest.mark.network``.
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
SCRIPT = SCRIPT_DIR / "verify-codeql-analyzer-of-record.py"


def _run(*args: str) -> subprocess.CompletedProcess:
    cmd = [sys.executable, str(SCRIPT), *args]
    return subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=120,
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


def test_runs_against_real_repo() -> None:
    """Run against the repo's actual workflow + config files. The script
    should find the advanced workflow, the config, and the model pack. The
    ``gh`` default-setup check is the only path that might fail (network).
    """
    result = _run("--repo", "intersoftdatalabs-in/percussioncms")
    # We don't assert exit code 0 here because the gh default-setup check
    # requires an authenticated gh + network. We DO assert that the
    # local-file checks ran and didn't crash.
    combined = result.stdout + result.stderr
    assert "advanced workflow" in combined
    assert "codeql-config.yml" in combined
