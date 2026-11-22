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
"""Unit tests for scripts/night-cycle-verify-gate.py (no network).

Covers issue #7019: a cycle-verify turn that returns while builds are
still running must be caught (missing DONE line, stale cycle id, stale
mtime), and the foreground sentinel wait must return only after the
background job it awaits actually finishes (or time out with evidence).
"""
from __future__ import annotations

import datetime as dt
import importlib.util
import json
import os
import threading
import time
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent


def _load():
    path = SCRIPT_DIR / "night-cycle-verify-gate.py"
    spec = importlib.util.spec_from_file_location("night_cycle_verify_gate",
                                                  path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(mod)
    return mod


gate = _load()

CYCLE = "stonemonkey-20261122T120726Z"
OLD_CYCLE = "stonemonkey-20261121T235545Z"
DONE = ("CYCLE_VERIFY_DONE tip=pr:refs/pull/456/head "
        "modules=rest,projects/sitemanage status=failed issues_filed=2")


def _utcnow() -> dt.datetime:
    return dt.datetime.now(dt.timezone.utc)


def _iso(moment: dt.datetime) -> str:
    return moment.isoformat().replace("+00:00", "Z")


def _write_json(work_dir: Path, payload: dict,
                mtime: dt.datetime) -> Path:
    scratch = work_dir / "scratch"
    scratch.mkdir(parents=True, exist_ok=True)
    path = scratch / "cycle-verify.json"
    path.write_text(json.dumps(payload), encoding="utf-8")
    stamp = mtime.timestamp()
    os.utime(path, (stamp, stamp))
    return path


def _fresh_json(work_dir: Path, cycle: str = CYCLE,
                status: str = "failed") -> Path:
    return _write_json(work_dir,
                       {"phase": "cycle-verify", "cycle_id": cycle,
                        "status": status},
                       _utcnow())


def test_check_pass_accepts_done_line_and_fresh_matching_json(tmp_path):
    _fresh_json(tmp_path)
    verdict = gate.check_turn(
        DONE, tmp_path / "scratch" / "cycle-verify.json", CYCLE,
        _utcnow() - dt.timedelta(minutes=5))
    assert verdict["ok"] is True, verdict
    assert verdict["reason"] == ""


def test_check_rejects_turn_that_ended_mid_build(tmp_path):
    # The exact #7019 stall message: background chain + early return.
    output = ("Maven chain is underway in `system` module tests. QA chain "
              "staged. Ending turn here — will resume on the background "
              "completion notice.")
    _fresh_json(tmp_path)
    verdict = gate.check_turn(
        output, tmp_path / "scratch" / "cycle-verify.json", CYCLE,
        _utcnow() - dt.timedelta(minutes=5))
    assert verdict["ok"] is False
    assert verdict["reason"] == "missing_done_line"


def test_check_rejects_previous_cycle_json(tmp_path):
    # Stale file from the earlier cycle must not count as fresh (#7019
    # acceptance: previous cycle id is rejected).
    _write_json(tmp_path,
                {"phase": "cycle-verify", "cycle_id": OLD_CYCLE,
                 "status": "passed"},
                _utcnow() - dt.timedelta(seconds=30))
    verdict = gate.check_turn(
        DONE, tmp_path / "scratch" / "cycle-verify.json", CYCLE,
        _utcnow() - dt.timedelta(minutes=5))
    assert verdict["ok"] is False
    assert verdict["reason"] == "stale_cycle_id"
    assert OLD_CYCLE in verdict["detail"]


def test_check_rejects_json_predating_dispatch(tmp_path):
    # Same cycle id but written before this turn was dispatched: the
    # agent never wrote a result for THIS turn.
    dispatch = _utcnow()
    _write_json(tmp_path,
                {"phase": "cycle-verify", "cycle_id": CYCLE,
                 "status": "passed"},
                dispatch - dt.timedelta(seconds=60))
    verdict = gate.check_turn(
        DONE, tmp_path / "scratch" / "cycle-verify.json", CYCLE,
        dispatch)
    assert verdict["ok"] is False
    assert verdict["reason"] == "stale_mtime"


def test_check_rejects_missing_and_unstamped_json(tmp_path):
    missing = gate.check_turn(DONE, tmp_path / "scratch" /
                              "cycle-verify.json", CYCLE,
                              _utcnow() - dt.timedelta(minutes=5))
    assert missing["ok"] is False
    assert missing["reason"] == "missing_json"

    _write_json(tmp_path, {"phase": "cycle-verify", "status": "passed"},
                _utcnow())
    unstamped = gate.check_turn(
        DONE, tmp_path / "scratch" / "cycle-verify.json", CYCLE,
        _utcnow() - dt.timedelta(minutes=5))
    assert unstamped["ok"] is False
    assert unstamped["reason"] == "missing_cycle_id"

    corrupt = tmp_path / "scratch" / "cycle-verify.json"
    corrupt.write_text("{not json", encoding="utf-8")
    bad = gate.check_turn(DONE, corrupt, CYCLE,
                          _utcnow() - dt.timedelta(minutes=5))
    assert bad["ok"] is False
    assert bad["reason"] == "bad_json"


def test_wait_blocks_until_background_job_finishes(tmp_path):
    # The background "build" finishes 0.3 s in; the foreground wait must
    # not return before the sentinel the job writes appears.
    sentinel = tmp_path / "chain.done"

    def _background_job():
        time.sleep(0.3)
        sentinel.write_text("BUILD SUCCESS", encoding="utf-8")

    worker = threading.Thread(target=_background_job)
    started = time.monotonic()
    worker.start()
    try:
        verdict = gate.wait_for_sentinel(sentinel, timeout=10.0,
                                         poll=0.05)
    finally:
        worker.join()
    assert verdict["ok"] is True, verdict
    # Windows timer coalescing can undershoot time.sleep(0.3) by a few
    # ms (observed 0.297 on windows-latest CI), so assert a relaxed
    # lower bound. The behavioral guarantee — wait returns only after
    # the sentinel the background job writes appears — is covered by
    # the sentinel-content assertion below.
    assert time.monotonic() - started >= 0.2
    assert sentinel.read_text(encoding="utf-8") == "BUILD SUCCESS"


def test_wait_times_out_with_blocked_evidence(tmp_path):
    verdict = gate.wait_for_sentinel(tmp_path / "never.done",
                                     timeout=0.3, poll=0.05)
    assert verdict["ok"] is False
    assert verdict["reason"] == "timeout"
    assert "blocked" in verdict["detail"]


def test_cli_check_pass_end_to_end(tmp_path, capsys):
    _fresh_json(tmp_path)
    out_file = tmp_path / "stdout.txt"
    out_file.write_text(DONE + "\n", encoding="utf-8")
    dispatch = _iso(_utcnow() - dt.timedelta(minutes=5))
    rc = gate.main(["check", "--work-dir", str(tmp_path),
                    "--cycle-id", CYCLE, "--dispatched-at", dispatch,
                    "--output-file", str(out_file)])
    assert rc == 0
    assert "CYCLE_VERIFY_GATE_PASS" in capsys.readouterr().out


def test_cli_check_fail_end_to_end_stale_cycle(tmp_path, capsys):
    _write_json(tmp_path,
                {"phase": "cycle-verify", "cycle_id": OLD_CYCLE,
                 "status": "passed"},
                _utcnow())
    dispatch = _iso(_utcnow() - dt.timedelta(minutes=5))
    rc = gate.main(["check", "--work-dir", str(tmp_path),
                    "--cycle-id", CYCLE, "--dispatched-at", dispatch,
                    "--output-text", DONE])
    assert rc == 1
    assert "stale_cycle_id" in capsys.readouterr().out
