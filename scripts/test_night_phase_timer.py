#!/usr/bin/env python3
"""Tests for night-phase-timer.py (issue #6785).

Per scripts/ conventions: colocated pytest module, tmp_path filesystems,
stdlib only. Run via:

    python3 -m pytest scripts/test_night_phase_timer.py -v
"""
from __future__ import annotations

import importlib.util
import json
import os
import subprocess
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT_PATH = SCRIPT_DIR / "night-phase-timer.py"


def _load_module():
    spec = importlib.util.spec_from_file_location("night_phase_timer", SCRIPT_PATH)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _run_cli(*argv: str, env_extra: dict | None = None) -> subprocess.CompletedProcess:
    env = dict(os.environ)
    if env_extra:
        env.update(env_extra)
    return subprocess.run(
        [sys.executable, str(SCRIPT_PATH), *argv],
        capture_output=True,
        text=True,
        env=env,
        timeout=60,
    )


def test_start_end_roundtrip_records_elapsed(tmp_path):
    state = tmp_path / "timings.json"
    assert _run_cli("start", "Work", "--state", str(state),
                     "--at", "2026-11-20T00:00:00Z").returncode == 0
    assert _run_cli("end", "Work", "--state", str(state),
                    "--at", "2026-11-20T01:30:00Z").returncode == 0
    data = json.loads(state.read_text(encoding="utf-8"))
    assert data["phases"]["Work"]["elapsed_sec"] == 5400.0


def test_end_without_start_exits_two(tmp_path):
    state = tmp_path / "timings.json"
    completed = _run_cli("end", "Work", "--state", str(state))
    assert completed.returncode == 2
    assert "no recorded start" in completed.stderr


def test_repeated_start_resets_phase(tmp_path):
    state = tmp_path / "timings.json"
    _run_cli("start", "Work", "--state", str(state), "--at", "2026-11-20T00:00:00Z")
    _run_cli("start", "Work", "--state", str(state), "--at", "2026-11-20T02:00:00Z")
    _run_cli("end", "Work", "--state", str(state), "--at", "2026-11-20T03:00:00Z")
    data = json.loads(state.read_text(encoding="utf-8"))
    assert data["phases"]["Work"]["elapsed_sec"] == 3600.0


def test_report_markdown_table(tmp_path):
    mod = _load_module()
    state = {
        "version": 1,
        "order": ["Triage", "Work"],
        "phases": {
            "Triage": {
                "start": "2026-11-20T00:00:00Z",
                "end": "2026-11-20T00:10:00Z",
                "elapsed_sec": 600.0,
            },
            "Work": {"start": "2026-11-20T00:10:00Z"},
        },
    }
    md = mod.render_markdown(state, "2026-11-20T01:10:00Z")
    assert "| Phase | Started (UTC) | Ended (UTC) | Elapsed |" in md
    assert "| Triage | 2026-11-20T00:00:00Z | 2026-11-20T00:10:00Z | 0:10:00 |" in md
    assert "| Work | 2026-11-20T00:10:00Z | (running) | 1:00:00 so far |" in md
    assert "Total wall-clock: 1:10:00" in md


def test_format_elapsed_units():
    mod = _load_module()
    assert mod.format_elapsed(45) == "0:00:45"
    assert mod.format_elapsed(600) == "0:10:00"
    assert mod.format_elapsed(5400) == "1:30:00"
    assert mod.format_elapsed(23400) == "6:30:00"


def test_state_env_default(tmp_path, monkeypatch):
    mod = _load_module()
    monkeypatch.chdir(tmp_path)
    monkeypatch.delenv(mod.STATE_ENV, raising=False)
    assert mod.default_state_path() == tmp_path / mod.DEFAULT_STATE_NAME
    custom = tmp_path / "custom.json"
    monkeypatch.setenv(mod.STATE_ENV, str(custom))
    assert mod.default_state_path() == custom


def test_parse_iso_accepts_z_suffix():
    mod = _load_module()
    assert mod.parse_iso("2026-11-20T00:00:00Z").utcoffset() is not None
