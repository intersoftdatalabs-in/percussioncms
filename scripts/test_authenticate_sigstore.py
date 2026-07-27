#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/authenticate-sigstore.py.

Tests the CLI surface (happy/help/failure) without invoking Maven:
- ``--help`` exits 0 and prints the usage banner
- A bogus flag exits non-zero with a recognizable error substring
- The "cached token present" code path exits 0 when ``--cache-path`` points at
  a real file and ``--identity`` is empty (idempotent re-run, mirrors bash
  original's ``[[ -f ~/.sigstore-token ]]`` short-circuit)

The real Maven invocation path is intentionally NOT exercised here because
that requires a built ``modules/ai-shared-develop`` JAR and a reachable IdP.
"""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "authenticate-sigstore.py"


def _run(*args: str, env: dict[str, str] | None = None) -> subprocess.CompletedProcess:
    cmd = [sys.executable, str(SCRIPT), *args]
    full_env = os.environ.copy()
    # Ensure no pre-existing SIGSTORE_IDENTITY_TOKEN pollutes the cache-hit path.
    full_env.pop("SIGSTORE_IDENTITY_TOKEN", None)
    if env:
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
    # argparse prints the bad flag name into stderr.
    combined = (result.stderr + result.stdout).lower()
    assert "no-such-flag" in combined or "unrecognized" in combined or "usage" in combined


def test_cached_token_short_circuits_to_zero(tmp_path: Path) -> None:
    """When the cache file exists and no --identity is given, exit 0 without
    invoking Maven (mirrors the bash original's idempotent re-run)."""
    cache = tmp_path / "sigstore-token"
    cache.write_text("cached-token-do-not-refresh\n", encoding="utf-8")
    result = _run("--cache-path", str(cache))
    assert result.returncode == 0, result.stderr
    assert "reusing" in (result.stdout + result.stderr).lower() or "cached" in (
        result.stdout + result.stderr
    ).lower()
    # Sanity: cache file unchanged.
    assert cache.read_text(encoding="utf-8").startswith("cached-token-")


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
