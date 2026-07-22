#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/verify-pr-review-resolution.py.

Exercises the triage-row → PR-number parser (``_pr_numbers_from_triage``) and
the CLI surface. The actual ``gh pr view`` call is gated behind
``@pytest.mark.network`` (and the absence of ``gh`` on the test host).
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "verify-pr-review-resolution.py"


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


def test_missing_gh_exits_two(monkeypatch: pytest.MonkeyPatch, tmp_path: Path) -> None:
    """When ``gh`` is not on PATH (we strip PATH down to ``/nonexistent``),
    the script exits 2."""
    triage = tmp_path / "triage.md"
    triage.write_text(
        "# Triage\n\n| # | alert_id | rule_id | severity | file_path | module_owner | disposition | target_action | target_milestone | linked_pr | notes |\n"
        "| 1 | 1 | `x` | low | `Foo.java` | `WebUI/` | valid | fix | 8.2 | 1234 | — |\n",
        encoding="utf-8",
    )
    result = subprocess.run(
        [sys.executable, str(SCRIPT), "--triage", str(triage)],
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
        env={"PATH": "/nonexistent"},
    )
    # The script will exit 2 because gh is missing on the scrubbed PATH.
    assert result.returncode == 2


def test_pr_numbers_from_triage_extracts_unique_numbers(tmp_path: Path) -> None:
    import importlib.util
    import re

    spec = importlib.util.spec_from_file_location("verify_prr", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    text = (
        "# Triage\n\n"
        "| # | alert_id | rule_id | severity | file_path | module_owner | disposition | target_action | target_milestone | linked_pr | notes |\n"
        "| 1 | 1 | `x` | low | `A.java` | `WebUI/` | valid | fix | 8.2 | 1234 | — |\n"
        "| 2 | 2 | `y` | low | `B.java` | `WebUI/` | valid | fix | 8.2 | 1234 | — |\n"
        "| 3 | 3 | `z` | low | `C.java` | `WebUI/` | valid | fix | 8.2 | 5678 | — |\n"
        "| 4 | 4 | `w` | low | `D.java` | `WebUI/` | obsolete | remove | 8.2 | — | — |\n"
    )
    nums = module._pr_numbers_from_triage(text)
    assert nums == ["1234", "5678"]


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
