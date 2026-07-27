#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/verify-no-jqplot-vendor-refs.py.

Exercises the FAIL path by introducing a transient ``*.js`` file under the
tracked tree and ``git add``-ing it (so ``git grep`` sees it). The probe is
removed after the test via a ``try``/``finally`` cleanup so the working tree
stays clean (matches the bash self-test's ``trap cleanup EXIT`` pattern).
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
SCRIPT = SCRIPT_DIR / "verify-no-jqplot-vendor-refs.py"


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
    """Run from the repo root; the jqplot vendor lib is genuinely gone."""
    result = _run("--repo-root", str(REPO_ROOT))
    assert result.returncode == 0, result.stderr


def test_stray_reference_fails() -> None:
    """Introduce a tracked file with a ``jqplot`` reference, run the script,
    then clean up. Mirrors the bash self-test's ``trap cleanup EXIT`` pattern.
    """
    probe = REPO_ROOT / "WebUI" / "src" / "main" / "webapp" / "cm" / "plugins" / "perc_test_jqplot_probe.py"
    # Actually write the file under a real path (WebUI/...) so git grep's
    # file-extension filter doesn't exclude it; the regex itself doesn't care.
    probe_js = probe.with_suffix(".js")
    probe_js.parent.mkdir(parents=True, exist_ok=True)
    try:
        probe_js.write_text("// jqplot (self-test probe, not a real reference)\n", encoding="utf-8")
        subprocess.run(
            ["git", "add", "--", str(probe_js.relative_to(REPO_ROOT))],
            shell=False,
            check=False,
            cwd=str(REPO_ROOT),
        )
        result = _run("--repo-root", str(REPO_ROOT))
        assert result.returncode == 1, result.stdout + result.stderr
        assert "jqplot" in (result.stdout + result.stderr)
    finally:
        # Clean up: git reset, remove the file. Use shell=False argv-list per FR-008.
        subprocess.run(
            ["git", "reset", "--", str(probe_js.relative_to(REPO_ROOT))],
            shell=False,
            check=False,
            cwd=str(REPO_ROOT),
        )
        probe_js.unlink(missing_ok=True)


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
