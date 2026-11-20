#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Copyright (c) 2026 Intersoft Data Labs, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#
# See the License for the specific language governing permissions and
# limitations under the License.
"""pytest coverage for scripts/verify-codeql-drained.py.

Exercises the pure verdict function ``evaluate`` (no network) plus the CLI
surface (``--help``, missing ``--pr-number``, missing ``gh``). All GitHub
I/O stays behind ``_run_gh_json`` and is never touched by these tests, so
the suite is H2-independent and needs no QA cell.
"""
from __future__ import annotations

import importlib.util
import subprocess
import sys
from pathlib import Path
from unittest import mock

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "verify-codeql-drained.py"


def _load():
    spec = importlib.util.spec_from_file_location("verify_codeql_drained", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]
    return module


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


def _check(name, status="completed", conclusion="success"):
    return {"name": name, "status": status, "conclusion": conclusion}


def test_help_exits_zero_and_prints_usage() -> None:
    result = _run("--help")
    assert result.returncode == 0, result.stderr
    assert "usage:" in result.stdout.lower()


def test_missing_pr_number_exits_two() -> None:
    result = _run()
    assert result.returncode == 2


def test_missing_gh_exits_two() -> None:
    module = _load()
    with mock.patch.object(module.shutil, "which", return_value=None):
        assert module.main(["--pr-number", "6770"]) == 2


def test_is_codeql_check_matches_known_names() -> None:
    module = _load()
    matchers = module.DEFAULT_CODEQL_MATCHERS
    assert module.is_codeql_check("CodeQL", matchers)
    assert module.is_codeql_check("Analyze (javascript-typescript)", matchers)
    assert module.is_codeql_check("Analyze (java-kotlin)", matchers)
    assert not module.is_codeql_check("H2 qa-up + Playwright (live)", matchers)
    assert not module.is_codeql_check("QA wiring (dry-run / unit / list)", matchers)


def test_evaluate_drained_when_green_and_no_alerts() -> None:
    module = _load()
    verdict = module.evaluate(
        files=["a/b.spec.js"],
        check_runs=[_check("CodeQL"), _check("Analyze (javascript-typescript)")],
        alerts=[],
    )
    assert verdict["drained"] is True
    assert verdict["reasons"] == []


def test_evaluate_not_drained_on_failing_codeql_check() -> None:
    module = _load()
    verdict = module.evaluate(
        files=["a/b.spec.js"],
        check_runs=[_check("CodeQL", conclusion="failure")],
        alerts=[],
    )
    assert verdict["drained"] is False
    assert any("failure" in r for r in verdict["reasons"])


def test_evaluate_not_drained_on_pending_codeql_check() -> None:
    """A still-running CodeQL check is not green yet — not drained (issue #6784)."""
    module = _load()
    verdict = module.evaluate(
        files=["a/b.spec.js"],
        check_runs=[_check("CodeQL", status="in_progress", conclusion=None)],
        alerts=[],
    )
    assert verdict["drained"] is False
    assert any("not green yet" in r for r in verdict["reasons"])


def test_evaluate_not_drained_on_open_alert_at_pr_file() -> None:
    """Alert #2101 pattern: open alert at a touched file blocks drained."""
    module = _load()
    verdict = module.evaluate(
        files=["modules/perc-qa-automation/frontend/tests/editor-codemirror-bundle.spec.js"],
        check_runs=[_check("CodeQL")],
        alerts=[
            {
                "number": 2101,
                "rule": "js/incomplete-url-substring-sanitization",
                "path": "modules/perc-qa-automation/frontend/tests/editor-codemirror-bundle.spec.js",
            }
        ],
    )
    assert verdict["drained"] is False
    assert any("#2101" in r for r in verdict["reasons"])


def test_evaluate_ignores_alerts_at_untouched_files() -> None:
    module = _load()
    verdict = module.evaluate(
        files=["a/b.spec.js"],
        check_runs=[_check("CodeQL")],
        alerts=[{"number": 7, "rule": "java/xss", "path": "elsewhere/Foo.java"}],
    )
    assert verdict["drained"] is True


def test_evaluate_ignores_non_codeql_failures() -> None:
    """Other CI red does not gate a CodeQL thread (pr-follow-up: CI alone is not a blocker)."""
    module = _load()
    verdict = module.evaluate(
        files=["a/b.spec.js"],
        check_runs=[
            _check("CodeQL"),
            _check("H2 qa-up + Playwright (live)", conclusion="failure"),
        ],
        alerts=[],
    )
    assert verdict["drained"] is True


def test_evaluate_skipped_language_pack_is_ok() -> None:
    """A skipped Analyze leg (language not in the PR) counts as green."""
    module = _load()
    verdict = module.evaluate(
        files=["a/b.spec.js"],
        check_runs=[
            _check("CodeQL"),
            _check("Analyze (java-kotlin)", conclusion="skipped"),
        ],
        alerts=[],
    )
    assert verdict["drained"] is True


def test_alert_is_open_prefers_top_level_state() -> None:
    module = _load()
    assert module._alert_is_open({"state": "open"}) is True
    assert module._alert_is_open({"state": "fixed"}) is False
    assert module._alert_is_open({"state": "dismissed"}) is False


def test_alert_is_open_falls_back_to_instance_state() -> None:
    """PR-analysis-only alerts (e.g. #2101) can carry a null top-level state."""
    module = _load()
    assert (
        module._alert_is_open(
            {"state": None, "most_recent_instance": {"state": "open"}}
        )
        is True
    )
    assert (
        module._alert_is_open(
            {"state": None, "most_recent_instance": {"state": "fixed"}}
        )
        is False
    )


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
