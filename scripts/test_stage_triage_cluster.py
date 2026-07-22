#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/stage-triage-cluster.py.

Exercises the row-staging pure function with a small in-memory triage table.
The actual file-IO path is tested in a separate test using ``tmp_path``.
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "stage-triage-cluster.py"


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


def test_missing_cluster_name_exits_nonzero(tmp_path: Path) -> None:
    """``--cluster-name`` is required. Use ``tmp_path`` so the path is
    cross-platform (Windows lacks ``/dev/null``)."""
    result = _run("--triage", str(tmp_path / "does-not-exist.md"))
    assert result.returncode != 0
    combined = (result.stderr + result.stdout).lower()
    assert "cluster-name" in combined or "required" in combined


def test_unknown_cluster_in_path_mode_exits_nonzero(tmp_path: Path) -> None:
    triage = tmp_path / "triage.md"
    triage.write_text(
        "# Triage\n\n| # | alert_id | rule_id | severity | file_path | module_owner | disposition | target_action | target_milestone | linked_pr | notes |\n"
        "|---|----------|---------|----------|-----------|--------------|-------------|---------------|------------------|-----------|-------|\n"
        "| 1 | 1 | `x` | low | `Foo.java` | `WebUI/` | valid | fix | 8.2 | — | — |\n",
        encoding="utf-8",
    )
    result = _run(
        "--cluster-name",
        "T999-no-such-cluster",
        "--mode",
        "path",
        "--triage",
        str(triage),
    )
    assert result.returncode != 0
    combined = (result.stderr + result.stdout).lower()
    assert "no path matchers" in combined or "t999" in combined


def test_stage_rows_writes_staged_marker(tmp_path: Path) -> None:
    """The T037 path-mode matcher is ``PSProxyQueryResource.java``. Use a
    triage row whose file_path contains that substring; expect ``T037-staged``
    to appear in the linked_pr column after staging."""
    triage = tmp_path / "triage.md"
    triage.write_text(
        "# Triage\n\n| # | alert_id | rule_id | severity | file_path | module_owner | disposition | target_action | target_milestone | linked_pr | notes |\n"
        "|---|----------|---------|----------|-----------|--------------|-------------|---------------|------------------|-----------|-------|\n"
        "| 1 | 1 | `x` | low | `extensions-main/src/main/java/com/percussion/extensions/general/PSProxyQueryResource.java` | `modules/extensions-main/` | valid | fix | 8.2 | — | — |\n",
        encoding="utf-8",
    )
    result = _run(
        "--cluster-name",
        "T037",
        "--mode",
        "path",
        "--triage",
        str(triage),
    )
    assert result.returncode == 0, result.stderr
    body = triage.read_text(encoding="utf-8")
    assert "T037-staged" in body


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
