#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/verify-no-cadf-legacy-auditlog.py.

Exercises PASS on a clean tree and FAIL when a tracked production path
reintroduces ``com.ibm.cadf`` (negative probes use ``tmp_path`` only).
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


def _init_fake_git_repo(fake_root: Path) -> None:
    """Minimal git repo so ``git grep`` works under a temp root."""
    fake_root.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["git", "init"],
        shell=False,
        check=True,
        cwd=str(fake_root),
        capture_output=True,
    )
    # Identity required on some CI hosts before git add of content.
    subprocess.run(
        ["git", "config", "user.email", "gate-test@example.com"],
        shell=False,
        check=True,
        cwd=str(fake_root),
        capture_output=True,
    )
    subprocess.run(
        ["git", "config", "user.name", "cadf-gate-test"],
        shell=False,
        check=True,
        cwd=str(fake_root),
        capture_output=True,
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


def test_stray_cadf_reference_fails(tmp_path: Path) -> None:
    """Tracked Java with ``com.ibm.cadf`` under a fake repo must fail the gate.

    Uses ``tmp_path`` only — never stages or writes under the real monorepo
    (interrupted runs must not leave the worktree dirty).
    """
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    probe = (
        fake_root
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
    probe.write_text(
        "// probe for verify-no-cadf-legacy-auditlog: import com.ibm.cadf.model.Event;\n",
        encoding="utf-8",
    )
    rel = str(probe.relative_to(fake_root)).replace("\\", "/")
    subprocess.run(
        ["git", "add", "--", rel],
        shell=False,
        check=True,
        cwd=str(fake_root),
        capture_output=True,
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 1, result.stdout + result.stderr
    combined = result.stdout + result.stderr
    assert "com.ibm.cadf" in combined
    assert "FAIL" in combined


def test_reintroduced_module_dir_fails(tmp_path: Path) -> None:
    """A recreated modules/jcadf-master directory must fail the gate."""
    # Use a temporary fake repo root with only the forbidden directory so we do
    # not recreate jcadf under the real monorepo (would break parallel work).
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    (fake_root / "modules" / "jcadf-master").mkdir(parents=True)
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 1, result.stdout + result.stderr
    assert "jcadf-master" in (result.stdout + result.stderr)


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
