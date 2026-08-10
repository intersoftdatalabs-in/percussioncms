#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/verify-no-cadf-legacy-auditlog.py.

Exercises PASS on a clean tree and FAIL when a tracked production path
reintroduces ``com.ibm.cadf`` (probe file cleaned in finally).
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
SCRIPT = SCRIPT_DIR / "verify-no-cadf-legacy-auditlog.py"


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


def test_clean_repo_passes() -> None:
    """CADF module and legacy package are gone; gate must pass."""
    result = _run("--repo-root", str(REPO_ROOT))
    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS" in result.stdout


def test_stray_cadf_reference_fails() -> None:
    """Introduce a tracked Java file with ``com.ibm.cadf``, then clean up."""
    probe = (
        REPO_ROOT
        / "modules"
        / "perc-auditlog"
        / "src"
        / "main"
        / "java"
        / "com"
        / "intsof"
        / "percussioncms"
        / "auditlog"
        / "CadfGateProbe.java"
    )
    probe.parent.mkdir(parents=True, exist_ok=True)
    rel = str(probe.relative_to(REPO_ROOT)).replace("\\", "/")
    try:
        probe.write_text(
            "// probe for verify-no-cadf-legacy-auditlog: import com.ibm.cadf.model.Event;\n",
            encoding="utf-8",
        )
        subprocess.run(
            ["git", "add", "--", rel],
            shell=False,
            check=False,
            cwd=str(REPO_ROOT),
        )
        result = _run("--repo-root", str(REPO_ROOT))
        assert result.returncode == 1, result.stdout + result.stderr
        combined = result.stdout + result.stderr
        assert "com.ibm.cadf" in combined
        assert "FAIL" in combined
    finally:
        subprocess.run(
            ["git", "reset", "--", rel],
            shell=False,
            check=False,
            cwd=str(REPO_ROOT),
        )
        probe.unlink(missing_ok=True)


def test_reintroduced_module_dir_fails(tmp_path: Path) -> None:
    """A recreated modules/jcadf-master directory must fail the gate."""
    # Use a temporary fake repo root with only the forbidden directory so we do
    # not recreate jcadf under the real monorepo (would break parallel work).
    fake_root = tmp_path / "repo"
    fake_root.mkdir()
    (fake_root / "modules" / "jcadf-master").mkdir(parents=True)
    # Initialize a minimal git repo so git grep does not error hard.
    subprocess.run(
        ["git", "init"],
        shell=False,
        check=True,
        cwd=str(fake_root),
        capture_output=True,
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 1, result.stdout + result.stderr
    assert "jcadf-master" in (result.stdout + result.stderr)


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
