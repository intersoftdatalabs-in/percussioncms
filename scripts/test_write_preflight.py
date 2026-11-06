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
"""Unit tests for scripts/write-preflight.py (no live gh)."""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "write-preflight.py"


def _run(args: list[str], cwd: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(SCRIPT), *args],
        cwd=cwd,
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
    )


def _inventory() -> list[dict]:
    return [
        {
            "number": 1,
            "title": "Product bug",
            "labels": ["bug", "p2"],
            "assignees": [],
            "author": "natechadwick-intsof",
            "updatedAt": "2026-11-05T00:00:00Z",
        },
        {
            "number": 2,
            "title": "Soak the release",
            "labels": ["not safe for agents", "p2"],
            "assignees": [],
            "author": "someone-else",
            "updatedAt": "2026-11-05T00:00:00Z",
        },
        {
            "number": 3,
            "title": "Claimed work",
            "labels": ["In Progress", "p3"],
            "assignees": [],
            "author": "natechadwick-intsof",
            "updatedAt": "2026-11-05T00:00:00Z",
        },
    ]


def test_canonical_shape_from_inventory_only(tmp_path: Path) -> None:
    inv = tmp_path / "issues-raw.json"
    inv.write_text(json.dumps(_inventory()), encoding="utf-8")
    out = tmp_path / "preflight.json"

    proc = _run(
        [
            "--inventory", str(inv),
            "--maintainers", "",
            "--allowed-authors", "natechadwick-intsof",
            "--out", str(out),
        ],
        cwd=tmp_path,
    )
    assert proc.returncode == 0, proc.stderr
    doc = json.loads(out.read_text(encoding="utf-8"))

    assert doc["phase"] == "preflight"
    compact = doc["issues_compact"]
    assert isinstance(compact, list) and len(compact) == 3
    by_number = {row["number"]: row for row in compact}
    assert by_number[1]["Maintainer"] is True
    assert by_number[1]["NotSafe"] is False
    assert by_number[1]["InProgress"] is False
    assert by_number[2]["NotSafe"] is True
    assert by_number[2]["Maintainer"] is False
    assert by_number[3]["InProgress"] is True
    for row in compact:
        assert row["Destructive"] is False
        assert set(row) >= {
            "number", "title", "labels", "assignees", "author",
            "NotSafe", "Destructive", "InProgress", "Maintainer", "LargeI18n",
        }
    # Alert feed missing -> key omitted (unknown, not zero).
    assert "open_alert_count" not in doc
    assert doc["stats"]["codeql_alerts"] == -1


def test_merges_prescreen_and_alerts(tmp_path: Path) -> None:
    inv = tmp_path / "issues-raw.json"
    inv.write_text(json.dumps(_inventory()), encoding="utf-8")
    prescreen = tmp_path / "prescreen.json"
    prescreen.write_text(
        json.dumps(
            {
                "issues": [
                    {"number": 1, "rule_kind": None},
                    {"number": 2, "rule_kind": "not_safe_agents"},
                ]
            }
        ),
        encoding="utf-8",
    )
    alerts = tmp_path / "alerts.json"
    alerts.write_text(
        json.dumps([{"number": 10}, {"number": 11}, {"number": 12}]),
        encoding="utf-8",
    )
    decision = tmp_path / "decision-front.json"
    decision.write_text(
        json.dumps(
            {
                "owned_pr_count": 0,
                "peer_eligible_count": 0,
                "cluster_recommended": False,
                "issues_cleared": 0,
            }
        ),
        encoding="utf-8",
    )
    out = tmp_path / "preflight.json"

    proc = _run(
        [
            "--inventory", str(inv),
            "--prescreen", str(prescreen),
            "--decision-front", str(decision),
            "--alerts", str(alerts),
            "--allowed-authors", "natechadwick-intsof",
            "--out", str(out),
        ],
        cwd=tmp_path,
    )
    assert proc.returncode == 0, proc.stderr
    doc = json.loads(out.read_text(encoding="utf-8"))
    by_number = {row["number"]: row for row in doc["issues_compact"]}
    assert by_number[2]["NotSafe"] is True
    assert doc["open_alert_count"] == 3
    assert doc["stats"]["codeql_alerts"] == 3
    assert doc["cluster_recommended"] is False


def test_rejects_non_array_inventory(tmp_path: Path) -> None:
    inv = tmp_path / "issues-raw.json"
    inv.write_text(json.dumps({"oops": True}), encoding="utf-8")
    proc = _run(
        ["--inventory", str(inv), "--out", str(tmp_path / "preflight.json")],
        cwd=tmp_path,
    )
    assert proc.returncode == 2
