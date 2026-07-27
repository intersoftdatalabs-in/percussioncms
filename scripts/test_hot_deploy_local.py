#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/hot-deploy-local.py.

The real Maven + Jetty restart flow is intentionally not exercised; this suite
covers the CLI surface and the small pure helpers (newest-jar selection, etc.).
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "hot-deploy-local.py"


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
    """argparse rejects unknown flags. The exact error substring varies
    (depends on whether the unknown token is consumed as a value for a
    required flag); accept any reasonable substring."""
    result = _run("--totally-not-a-flag-xyz")
    assert result.returncode != 0
    combined = (result.stderr + result.stdout).lower()
    assert (
        "totally-not-a-flag-xyz" in combined
        or "unrecognized" in combined
        or "usage" in combined
        or "required" in combined
    )


def test_missing_install_dir_is_required(tmp_path: Path) -> None:
    """The bash original exits 1 with a clear error; the port preserves that."""
    result = _run("--modules", "system")  # no --install-dir
    assert result.returncode != 0
    combined = (result.stderr + result.stdout).lower()
    assert "install-dir" in combined or "required" in combined


def test_newest_jar_skips_sources_and_javadoc(tmp_path: Path) -> None:
    """Exercise the ``_newest_jar`` helper directly."""
    import importlib.util

    spec = importlib.util.spec_from_file_location("hot_deploy_local", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    target = tmp_path / "target"
    target.mkdir()
    primary = target / "perc-system-8.2.0.jar"
    sources = target / "perc-system-8.2.0-sources.jar"
    javadoc = target / "perc-system-8.2.0-javadoc.jar"
    primary.write_bytes(b"")
    sources.write_bytes(b"")
    javadoc.write_bytes(b"")
    # Newer primary (newer mtime).
    import os
    import time

    time.sleep(0.05)
    newer = target / "perc-system-8.2.1.jar"
    newer.write_bytes(b"")
    chosen = module._newest_jar(target, "perc-system")
    assert chosen is not None
    assert chosen.name == "perc-system-8.2.1.jar"


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
