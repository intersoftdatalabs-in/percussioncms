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
"""Unit tests for scripts/night-decision-front.py (no live gh)."""
from __future__ import annotations

import importlib.util
import json
import tempfile
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent


def _load():
    path = SCRIPT_DIR / "night-decision-front.py"
    spec = importlib.util.spec_from_file_location("night_decision_front", path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(mod)
    return mod


df = _load()


def issue(n, title, labels=None, assignees=None, body=""):
    return {
        "number": n,
        "title": title,
        "labels": labels or ["p2", "enhancement"],
        "assignees": assignees or [],
        "author": "natechadwick-intsof",
        "body": body,
    }


def test_one_per_parent_and_skip_epic():
    issues = [
        issue(4531, "[8.2] Unified publishing"),
        issue(4648, "issue 4531 slice 11: runtime start stop"),
        issue(4649, "issue 4531 slice 12: sibling"),
        issue(4645, "issue 4532 slice 11: EditorHost save"),
        issue(4637, "issue 4530 slice 11: Explorer create folder"),
    ]
    result = df.run(issues, [], None, 3, 0, "model:grok-4.6", 3)
    nums = [q["issue_number"] for q in result["queue"]]
    assert 4531 not in nums
    assert len(nums) == 3
    assert 4648 in nums
    assert 4649 not in nums
    parents = {q["parent_issue"] for q in result["queue"]}
    assert parents == {4531, 4532, 4530}


def test_covering_pr_skips_issue_and_siblings():
    issues = [
        issue(4648, "issue 4531 slice 11: runtime"),
        issue(4649, "issue 4531 slice 12: sibling"),
        issue(4645, "issue 4532 slice 11: save"),
    ]
    prs = [
        {
            "number": 4700,
            "title": "fix 4648",
            "body": "Fixes #4648",
            "headRefName": "feat/issue-4648-runtime",
            "state": "OPEN",
            "labels": ["operator:grok"],
            "files": [],
        }
    ]
    result = df.run(issues, prs, None, 3, 0, "model:grok-4.6", 3)
    nums = [q["issue_number"] for q in result["queue"]]
    assert 4648 not in nums
    assert 4649 not in nums
    assert 4645 in nums
    assert result["owned_pr_count"] == 1


def test_prescreen_model_skip_and_assigned():
    issues = [
        issue(1989, "Live Linux soak", labels=["p2"]),
        issue(4645, "issue 4532 slice 11: save"),
        issue(1, "assigned one", assignees=["vijaya-boddipudi"]),
    ]
    prescreen = {
        "issues": [
            {"number": 1989, "rule_skip": False, "recommend_skip": True},
            {"number": 4645, "rule_skip": False, "recommend_skip": False},
        ]
    }
    result = df.run(issues, [], prescreen, 3, 0, "model:grok-4.6", 3)
    nums = [q["issue_number"] for q in result["queue"]]
    assert nums == [4645]


def test_cluster_recommended_shared_files():
    prs = [
        {"number": i, "title": "p", "body": "", "headRefName": "x", "state": "OPEN",
         "files": [{"path": "WebUI/src/main/ts/publish/messages.ts"}]}
        for i in (1, 2, 3)
    ]
    result = df.run([], prs, None, 3, 0, "model:grok-4.6", 3)
    assert result["cluster_recommended"] is True


def test_cluster_not_recommended_disjoint():
    prs = [
        {"number": 1, "title": "a", "body": "", "state": "OPEN",
         "files": [{"path": "a.ts"}]},
        {"number": 2, "title": "b", "body": "", "state": "OPEN",
         "files": [{"path": "b.ts"}]},
        {"number": 3, "title": "c", "body": "", "state": "OPEN",
         "files": [{"path": "c.ts"}]},
    ]
    result = df.run([], prs, None, 3, 0, "model:grok-4.6", 3)
    assert result["cluster_recommended"] is False
    assert result["owned_pr_count"] == 3


def test_peer_eligible_other_operator():
    prs = [
        {
            "number": 10,
            "title": "oc",
            "body": "",
            "state": "OPEN",
            "labels": ["operator:opencode", "model:minimax-m3"],
            "reviews": [],
            "mergeable": "MERGEABLE",
            "files": [],
        },
        {
            "number": 11,
            "title": "own",
            "body": "",
            "state": "OPEN",
            "labels": ["operator:grok", "model:grok-4.6"],
            "reviews": [],
            "mergeable": "MERGEABLE",
            "files": [],
        },
    ]
    result = df.run([], prs, None, 3, 0, "model:grok-4.6", 3)
    assert result["peer_eligible_count"] == 1
    assert "#10" in result["peer_eligible_prs"]


def test_cli_roundtrip():
    tmp = Path(tempfile.mkdtemp())
    issues = tmp / "issues.json"
    out = tmp / "out.json"
    issues.write_text(
        json.dumps([issue(4645, "issue 4532 slice 11: save")]),
        encoding="utf-8",
    )
    rc = df.main(
        [
            "--issues",
            str(issues),
            "--out",
            str(out),
            "--max-issues",
            "3",
        ]
    )
    assert rc == 0
    data = json.loads(out.read_text(encoding="utf-8"))
    assert data["queue"][0]["issue_number"] == 4645
    assert data["signals_complete"] is True


if __name__ == "__main__":
    test_one_per_parent_and_skip_epic()
    test_covering_pr_skips_issue_and_siblings()
    test_prescreen_model_skip_and_assigned()
    test_cluster_recommended_shared_files()
    test_cluster_not_recommended_disjoint()
    test_peer_eligible_other_operator()
    test_cli_roundtrip()
    print("ok")
