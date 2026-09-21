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
"""Unit tests for scripts/typesafe-prescreen.py.

Run with the project test runner:
    bash scripts/run-python-tests.sh        # Linux / macOS
    scripts\\run-python-tests.cmd            # Windows
or directly:
    python3 -m pytest scripts/test_typesafe_prescreen.py -v

No live TypeSafe calls happen in these tests: --answers / monkeypatched
ts_ask are used throughout, and the no-key path exercises the fail-open
fallback.
"""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent


def _load_module():
    path = SCRIPT_DIR / "typesafe-prescreen.py"
    name = "typesafe_prescreen"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(mod)
    return mod


ps = _load_module()


def noul(value: float) -> dict:
    return {"type": "noul", "noul": value}


def inventory(*numbers: int) -> list[dict]:
    return [
        {"number": n, "title": f"issue {n}", "labels": [], "assignees": [], "author": "bot"}
        for n in numbers
    ]


def rule_issue(number: int, label: str) -> dict:
    return {
        "number": number,
        "title": f"issue {number}",
        "labels": [label],
        "assignees": [],
        "author": "bot",
    }


def answers_for(numbers, *, skip=0.5, pr=0.5, close=0.2) -> dict:
    """Build a TypeSafe ``/systemone`` response shape for the given issue numbers."""
    answers = {}
    for n in numbers:
        answers[f"q_{n}_skip"] = noul(skip)
        answers[f"q_{n}_prsized"] = noul(pr)
        answers[f"q_{n}_close"] = noul(close)
    return {
        "model": "jev-latest",
        "answers": answers,
        "usage": {"input_tokens": 1_000_000, "output_tokens": 0},
    }


# ---------------------------------------------------------------- rules layer

def test_normalize_issue_accepts_gh_and_dict_shapes():
    raw = {
        "number": 7,
        "title": "t",
        "labels": [{"name": "bug"}],
        "assignees": [{"login": "alice"}],
        "author": {"login": "bob"},
    }
    out = ps.normalize_issue(raw)
    assert out["number"] == 7
    assert out["labels"] == ["bug"]
    assert out["assignees"] == ["alice"]
    assert out["author"] == "bob"


def test_normalize_issue_tolerates_missing_fields():
    out = ps.normalize_issue({"number": 3})
    assert out["labels"] == []
    assert out["assignees"] == []
    assert out["author"] == "unknown"


def test_deterministic_rules_labels():
    cases = {
        "not safe for agents": "not_safe_agents",
        "in progress": "in_progress",
        "in-progress": "in_progress",
        "qa task": "human_qa",
        "migrated": "migrated",
    }
    for label, kind in cases.items():
        rec = ps.deterministic_rules(rule_issue(1, label))
        assert rec["rule_skip"] is True, label
        assert rec["rule_kind"] == kind, label
        assert rec["rule_close_candidate"] == (kind == "migrated"), label


def test_deterministic_rules_assignees():
    rec = ps.deterministic_rules(
        {"number": 1, "labels": [], "assignees": ["carol"], "title": "t", "author": "x"}
    )
    assert rec["rule_skip"] is True
    assert rec["rule_kind"] == "assigned"


def test_deterministic_rules_not_safe_takes_priority():
    rec = ps.deterministic_rules(
        {"number": 1, "labels": ["qa task", "not safe for agents"], "assignees": [],
         "title": "t", "author": "x"}
    )
    assert rec["rule_kind"] == "not_safe_agents"


def test_deterministic_rules_none():
    rec = ps.deterministic_rules(
        {"number": 1, "labels": ["bug"], "assignees": [], "title": "t", "author": "x"}
    )
    assert rec["rule_skip"] is False
    assert rec["rule_kind"] is None
    assert rec["rule_close_candidate"] is False


# ------------------------------------------------------------- ts_ask paths

def test_ts_ask_private_answer_cache():
    payload = {"answers": {}}
    assert ps.ts_ask({"state": {}}, {}, "m", answers_cache=payload) is payload


def test_ts_ask_requires_key(monkeypatch):
    monkeypatch.delenv("TYPESAFE_API_KEY", raising=False)
    with pytest.raises(RuntimeError):
        ps.ts_ask({"state": {}}, {"q_1_skip": {}}, "m")


# --------------------------------------------------------- merging/thresholds

def test_merge_answers_model_threshold(monkeypatch):
    answers = {
        "q_1_skip": noul(0.90),
        "q_1_prsized": noul(0.75),
        "q_1_close": noul(0.25),
        "q_2_skip": noul(0.50),
        "q_2_prsized": noul(0.20),
        "q_2_close": noul(0.20),
    }
    full = ps._merge_answers(inventory(1, 2), answers, 0.85, 0.85)
    assert full[0]["recommend_skip"] is True and full[0]["skip_source"] == "model"
    assert full[0]["model"]["skip_safe"] == 0.90
    assert full[0]["model"]["pr_sized"] == 0.75
    assert full[1]["recommend_skip"] is False and full[1]["skip_source"] is None
    assert full[1]["model"]["pr_sized"] == 0.20


def test_merge_answers_close_threshold(monkeypatch):
    answers = {
        "q_1_skip": noul(0.1), "q_1_prsized": noul(0.5), "q_1_close": noul(0.95),
        "q_2_skip": noul(0.1), "q_2_prsized": noul(0.5), "q_2_close": noul(0.84),
    }
    full = ps._merge_answers(inventory(1, 2), answers, 0.85, 0.85)
    assert full[0]["recommend_close"] is True
    assert full[1]["recommend_close"] is False


def test_merge_answers_rule_wins_over_model(monkeypatch):
    answers = {"q_1_skip": noul(0.10), "q_1_prsized": noul(0.9), "q_1_close": noul(0.9)}
    full = ps._merge_answers([rule_issue(1, "qa task")], answers, 0.85, 0.85)
    assert full[0]["recommend_skip"] is True
    assert full[0]["skip_source"] == "rule"
    assert full[0]["rule_kind"] == "human_qa"


# ------------------------------------------------------------ end-to-end main

@pytest.fixture()
def no_api_key(monkeypatch):
    monkeypatch.delenv("TYPESAFE_API_KEY", raising=False)


def write_json(path: Path, payload):
    path.write_text(json.dumps(payload, indent=2), encoding="utf-8")


def test_main_dry_run_rule_layer(tmp_path, no_api_key):
    inv = tmp_path / "inv.json"
    out = tmp_path / "prescreen.json"
    write_json(inv, inventory(1, 2, 3) + [rule_issue(4, "migrated")])
    rc = ps.main(["--inventory", str(inv), "--out", str(out), "--dry-run"])
    assert rc == 0
    result = json.loads(out.read_text(encoding="utf-8"))
    assert result["status"] == "dry_run"
    assert result["stats"]["issues_total"] == 4
    assert result["stats"]["rule_skips"] == 1
    assert result["stats"]["model_recommend_skips"] == 0
    assert result["stats"]["close_candidates"] == 1
    issues = {r["number"]: r for r in result["issues"]}
    assert issues[4]["recommend_skip"] is True
    assert issues[4]["skip_source"] == "rule"
    assert issues[1]["model"] is None


def test_main_with_saved_answers(tmp_path, no_api_key):
    inv = tmp_path / "inv.json"
    out = tmp_path / "prescreen.json"
    ans = tmp_path / "answers.json"
    write_json(inv, inventory(1, 2, 3))
    write_json(ans, answers_for([1, 2, 3], skip=0.90))
    rc = ps.main(["--inventory", str(inv), "--out", str(out), "--answers", str(ans),
                  "--threshold", "0.85", "--close-threshold", "0.85"])
    assert rc == 0
    result = json.loads(out.read_text(encoding="utf-8"))
    assert result["status"] == "ok"
    assert result["args"]["model"] == "jev-latest"
    # 1M input tokens * $0.042/MT
    assert result["cost_usd_est"] == pytest.approx(0.042)
    assert result["stats"]["model_recommend_skips"] == 3
    assert result["stats"]["recommend_skips"] == 3
    assert result["stats"]["close_candidates"] == 0


def test_main_threshold_filters_weak_skips(tmp_path, no_api_key):
    inv = tmp_path / "inv.json"
    out = tmp_path / "prescreen.json"
    ans = tmp_path / "answers.json"
    write_json(inv, inventory(1, 2))
    # issue 1: skip_safe=0.84 (just below default threshold), issue 2: 0.86
    payload = answers_for([])
    payload["answers"]["q_1_skip"] = noul(0.84)
    payload["answers"]["q_2_skip"] = noul(0.86)
    write_json(ans, payload)
    rc = ps.main(["--inventory", str(inv), "--out", str(out), "--answers", str(ans)])
    assert rc == 0
    result = json.loads(out.read_text(encoding="utf-8"))
    issues = {r["number"]: r for r in result["issues"]}
    assert issues[1]["recommend_skip"] is False
    assert issues[2]["recommend_skip"] is True
    # raising the threshold drops the 0.86 one too
    rc = ps.main(["--inventory", str(inv), "--out", str(out), "--answers", str(ans),
                  "--threshold", "0.87"])
    assert rc == 0
    result = json.loads(out.read_text(encoding="utf-8"))
    issues = {r["number"]: r for r in result["issues"]}
    assert issues[2]["recommend_skip"] is False


def test_main_fail_open_no_key(tmp_path, no_api_key):
    inv = tmp_path / "inv.json"
    out = tmp_path / "prescreen.json"
    write_json(inv, inventory(1, 2))
    rc = ps.main(["--inventory", str(inv), "--out", str(out)])
    assert rc == 0
    result = json.loads(out.read_text(encoding="utf-8"))
    assert result["status"] == "fallback_rule_only"
    assert result["stats"]["model_recommend_skips"] == 0


def test_main_fail_open_on_api_error(tmp_path, no_api_key, monkeypatch):
    inv = tmp_path / "inv.json"
    out = tmp_path / "prescreen.json"
    write_json(inv, inventory(1, 2))

    def boom(*args, **kwargs):
        raise RuntimeError("service down")

    monkeypatch.setattr(ps, "ts_ask", boom)
    monkeypatch.setenv("TYPESAFE_API_KEY", "k")
    rc = ps.main(["--inventory", str(inv), "--out", str(out)])
    assert rc == 0
    result = json.loads(out.read_text(encoding="utf-8"))
    assert result["status"] == "fallback_rule_only"
    assert result["issues"]

    # deterministic layer still populated
    assert all(r["recommend_skip"] is False and r["skip_source"] is None for r in result["issues"])


def test_main_bad_inputs(tmp_path, no_api_key):
    bad_json = tmp_path / "bad.json"
    bad_json.write_text("{not json", encoding="utf-8")
    assert ps.main(["--inventory", str(bad_json), "--out", str(tmp_path / "o.json")]) == 2

    not_list = tmp_path / "notlist.json"
    write_json(not_list, {"not": "a list"})
    assert ps.main(["--inventory", str(not_list), "--out", str(tmp_path / "o.json")]) == 2

    missing = tmp_path / "missing.json"
    assert ps.main(["--inventory", str(missing), "--out", str(tmp_path / "o.json")]) == 2

    empty = tmp_path / "empty.json"
    write_json(empty, [])
    assert ps.main(["--inventory", str(empty), "--out", str(tmp_path / "o.json")]) == 2


def test_main_max_inventory_cap(tmp_path, no_api_key):
    inv = tmp_path / "inv.json"
    out = tmp_path / "prescreen.json"
    write_json(inv, inventory(1, 2, 3, 4, 5))
    rc = ps.main(["--inventory", str(inv), "--out", str(out), "--max-inventory", "2",
                  "--dry-run"])
    assert rc == 0
    result = json.loads(out.read_text(encoding="utf-8"))
    assert result["stats"]["issues_total"] == 2


def test_main_creates_parent_dirs(tmp_path, no_api_key):
    inv = tmp_path / "inv.json"
    out = tmp_path / "a" / "b" / "prescreen.json"
    write_json(inv, inventory(1))
    rc = ps.main(["--inventory", str(inv), "--out", str(out), "--dry-run"])
    assert rc == 0
    assert out.exists()


def test_main_inventory_downloaded_labels_shape(tmp_path, no_api_key):
    """gh issue list --json labels gives [{name:...}]; prescreen must handle it."""
    inv = tmp_path / "inv.json"
    out = tmp_path / "prescreen.json"
    raw = [
        {"number": 10, "title": "t", "labels": [{"name": "in progress"}],
         "assignees": [], "author": {"login": "bob"}}
    ]
    write_json(inv, raw)
    rc = ps.main(["--inventory", str(inv), "--out", str(out), "--dry-run"])
    assert rc == 0
    result = json.loads(out.read_text(encoding="utf-8"))
    assert result["issues"][0]["rule_kind"] == "in_progress"
    assert result["issues"][0]["author"] == "bob"