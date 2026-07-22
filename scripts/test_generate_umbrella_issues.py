#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/generate-umbrella-issues.py.

Exercises the pure Markdown-row parser and the umbrella-body renderer against
the existing ``triage-good.md`` fixture. The file-write step uses ``tmp_path``.
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "generate-umbrella-issues.py"
REPO_ROOT = SCRIPT_DIR.parent
FIXTURE = REPO_ROOT / "scripts" / "test-fixtures" / "triage-good.md"


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


def test_missing_input_exits_nonzero(tmp_path: Path) -> None:
    """Missing --input fails loudly (matches bash original which checks for
    ``$triage`` file existence)."""
    result = _run("--input", str(tmp_path / "does-not-exist.md"))
    assert result.returncode != 0
    combined = (result.stderr + result.stdout).lower()
    assert "not found" in combined or "required" in combined


def test_parse_triage_against_good_fixture() -> None:
    """Pure-function test: parse the good fixture and assert row contents."""
    import importlib.util

    spec = importlib.util.spec_from_file_location("gen_umbrella", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    rows = module.parse_triage(FIXTURE.read_text(encoding="utf-8"))
    assert len(rows) == 4
    dispositions = sorted(r["disposition"] for r in rows)
    assert dispositions == sorted(
        ["valid", "obsolete", "false-positive", "accepted-risk"]
    )


def test_dry_run_writes_nothing(tmp_path: Path) -> None:
    """``--dry-run`` lists the files that would be written but creates nothing."""
    output_dir = tmp_path / "umbrellas"
    result = _run("--input", str(FIXTURE), "--output-dir", str(output_dir), "--dry-run")
    assert result.returncode == 0, result.stderr
    assert "would write" in result.stdout
    assert not output_dir.exists() or not any(output_dir.iterdir())


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
