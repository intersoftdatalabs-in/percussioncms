#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/verify-distribution-archive.py.

The Maven build is gated behind ``--skip-mvn``; the default tests exercise only
the archive scan (read the inventory + check the JARs / ``.ppkg`` files). The
CLI surface (help + bad flag + missing inventory) is also covered.
"""
from __future__ import annotations

import subprocess
import sys
import zipfile
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "verify-distribution-archive.py"


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


def test_missing_removed_files_exits_nonzero(tmp_path: Path) -> None:
    result = _run(
        "--removed-files",
        str(tmp_path / "does-not-exist.txt"),
        "--skip-mvn",
    )
    assert result.returncode != 0
    assert "not found" in (result.stderr + result.stdout).lower()


def test_scan_detects_present_basename(tmp_path: Path) -> None:
    """Build a tiny JAR containing a file with a known basename; assert the
    scan flags it as present when the inventory lists that basename."""
    import importlib.util

    spec = importlib.util.spec_from_file_location("verify_dist", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    jar = tmp_path / "tiny.jar"
    with zipfile.ZipFile(jar, "w") as zf:
        zf.writestr("com/example/RemovedFoo.class", b"x")

    found = module._scan_archives([jar], ["RemovedFoo.class"])
    assert found["RemovedFoo.class"] == [str(jar)]


def test_scan_returns_empty_when_absent(tmp_path: Path) -> None:
    """Build a tiny JAR with one file; assert the scan reports the unrelated
    basename as not-present."""
    import importlib.util

    spec = importlib.util.spec_from_file_location("verify_dist", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    jar = tmp_path / "tiny.jar"
    with zipfile.ZipFile(jar, "w") as zf:
        zf.writestr("com/example/KeepMe.class", b"x")
    found = module._scan_archives([jar], ["RemovedFoo.class"])
    assert found["RemovedFoo.class"] == []


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
