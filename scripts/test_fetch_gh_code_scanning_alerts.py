#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/fetch-gh-code-scanning-alerts.py.

The live ``gh api`` path requires an authenticated ``gh`` and is gated by
``@pytest.mark.network``. The Markdown formatter (``write_report``) is a pure
function and is exercised offline.
"""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "fetch-gh-code-scanning-alerts.py"


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


def test_bogus_state_exits_nonzero() -> None:
    result = _run("--state", "definitely-not-a-real-state")
    assert result.returncode != 0
    combined = (result.stderr + result.stdout).lower()
    assert (
        "invalid choice" in combined
        or "definitely-not-a-real-state" in combined
    )


def test_write_report_with_fixture(tmp_path: Path) -> None:
    """Pure-function test: write a small alerts payload and assert Markdown shape."""
    import importlib.util

    spec = importlib.util.spec_from_file_location("fetch_alerts", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    alerts = [
        {
            "number": 42,
            "rule": {"id": "js/xss-through-dom", "security_severity_level": "high"},
            "tool": {"name": "CodeQL"},
            "state": "open",
            "created_at": "2026-07-21T00:00:00Z",
            "html_url": "https://gh/x",
            "most_recent_instance": {
                "message": {"text": "x is bad"},
                "location": {"path": "WebUI/x.js", "start_line": 10},
            },
        }
    ]
    out = tmp_path / "alerts.md"
    module.write_report(alerts, "o/r", "open", out)
    body = out.read_text(encoding="utf-8")
    assert "Code Scanning Alerts for o/r" in body
    assert "Alert #42" in body
    assert "js/xss-through-dom" in body
    assert "WebUI/x.js:10" in body


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
