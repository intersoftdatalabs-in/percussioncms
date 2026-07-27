#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/create-large-folder-fixture.py.

The live CMS API path requires a running CMS instance and is gated behind
``@pytest.mark.network``. The CLI surface (help + bad flag + missing creds)
is exercised offline.
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "create-large-folder-fixture.py"


def _run(*args: str, env: dict[str, str] | None = None) -> subprocess.CompletedProcess:
    cmd = [sys.executable, str(SCRIPT), *args]
    import os
    full_env = os.environ.copy()
    full_env.pop("CMS_USER", None)
    full_env.pop("CMS_PASS", None)
    if env is not None:
        full_env.update(env)
    return subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
        env=full_env,
    )


def test_help_exits_zero_and_prints_usage() -> None:
    result = _run("--help")
    assert result.returncode == 0, result.stderr
    assert "usage:" in result.stdout.lower()


def test_bogus_flag_exits_nonzero() -> None:
    result = _run("--no-such-flag")
    assert result.returncode != 0


def test_missing_credentials_exits_two() -> None:
    """The script refuses to run without CMS_USER / CMS_PASS."""
    result = _run()
    assert result.returncode == 2
    combined = (result.stdout + result.stderr).lower()
    assert "cms_user" in combined or "cms_pass" in combined or "required" in combined


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
