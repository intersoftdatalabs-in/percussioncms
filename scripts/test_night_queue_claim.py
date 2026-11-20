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
"""Unit tests for scripts/night-queue-claim.py (fake gh, no network)."""
from __future__ import annotations

import datetime as dt
import importlib.util
import json
import tempfile
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent


def _load():
    path = SCRIPT_DIR / "night-queue-claim.py"
    spec = importlib.util.spec_from_file_location("night_queue_claim", path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(mod)
    return mod


nqc = _load()
NOW = dt.datetime(2026, 11, 20, 22, 0, 0, tzinfo=dt.timezone.utc)


class FakeGh:
    """Minimal in-memory GitHub: issues with labels/assignees/comments, open PRs."""

    def __init__(self):
        self.issues: dict[int, dict] = {}
        self.prs: list[dict] = []
        self.clock = NOW
        self.calls: list[list[str]] = []

    def add(self, n, labels=(), assignees=(), comments=(), state="OPEN", updated="2026-11-20T21:00:00Z"):
        self.issues[n] = {
            "number": n,
            "state": state,
            "labels": [{"name": x} for x in labels],
            "assignees": [{"login": x} for x in assignees],
            "comments": [{"body": b, "createdAt": t} for b, t in comments],
            "updatedAt": updated,
        }

    def tick(self):
        self.clock += dt.timedelta(seconds=1)
        return self.clock.strftime("%Y-%m-%dT%H:%M:%SZ")

    def __call__(self, args, stdin=None):
        self.calls.append(args)
        if args[:2] == ["issue", "view"]:
            return json.dumps(self.issues[int(args[2])])
        if args[:2] == ["issue", "edit"]:
            iss = self.issues[int(args[2])]
            if args[5] == "--add-label":
                iss["labels"].append({"name": args[6]})
            else:
                iss["labels"] = [l for l in iss["labels"] if l["name"].lower() != args[6].lower()]
            return ""
        if args[:2] == ["issue", "comment"]:
            self.issues[int(args[2])]["comments"].append({"body": stdin, "createdAt": self.tick()})
            return ""
        if args[:2] == ["issue", "list"]:
            rows = [i for i in self.issues.values()
                    if i["state"] == "OPEN" and nqc.has_label(i)]
            return json.dumps(rows)
        if args[:2] == ["pr", "list"]:
            return json.dumps(self.prs)
        raise AssertionError(args)


def claimer(gh, tmp, host="stonemonkey"):
    c = nqc.Claimer(nqc.Gh("o/r", gh), Path(tmp) / "claims.json", host=host, now=lambda: gh.clock)
    return c


def other_claim(cycle, at, host="mayank-box"):
    return ("claimed\n\n" + nqc.marker("claim", cycle, host), at)


def labels(gh, n):
    return [l["name"] for l in gh.issues[n]["labels"]]


def test_claim_labels_and_comments_fresh_issue():
    gh = FakeGh()
    gh.add(1)
    with tempfile.TemporaryDirectory() as tmp:
        c = claimer(gh, tmp)
        c.init()
        out = c.claim([1])
        assert out["claimed"] == [1] and out["conflicts"] == []
        assert labels(gh, 1) == ["in progress"]
        acts = nqc.active_claims(gh.issues[1])
        assert [a["cycle"] for a in acts] == [c.cycle]
        state = json.loads((Path(tmp) / "claims.json").read_text())
        assert state["claims"]["1"]["state"] == "claimed"


def test_claim_is_idempotent_on_resume():
    gh = FakeGh()
    gh.add(1)
    with tempfile.TemporaryDirectory() as tmp:
        c = claimer(gh, tmp)
        c.init()
        c.claim([1])
        n_comments = len(gh.issues[1]["comments"])
        c2 = claimer(gh, tmp)
        assert c2.claim([1])["claimed"] == [1]
        assert len(gh.issues[1]["comments"]) == n_comments


def test_claim_skips_other_runner_label_assignee_and_closed():
    gh = FakeGh()
    gh.add(2, labels=["in progress"], comments=[other_claim("mayank-1", "2026-11-20T20:00:00Z")])
    gh.add(3, labels=["in progress"])  # legacy label-only claim (older runner / human)
    gh.add(4, assignees=["mayank-intsof"])
    gh.add(5, state="CLOSED")
    with tempfile.TemporaryDirectory() as tmp:
        c = claimer(gh, tmp)
        c.init()
        out = c.claim([2, 3, 4, 5])
        assert out["claimed"] == []
        reasons = {x["issue"]: x["reason"] for x in out["conflicts"]}
        assert reasons == {2: "claimed_by_other", 3: "claimed_by_other", 4: "assigned", 5: "closed"}
        # never touch someone else's claim
        assert labels(gh, 2) == ["in progress"] and labels(gh, 3) == ["in progress"]
        assert not any(a[:2] == ["issue", "comment"] for a in gh.calls)


def test_lost_race_yields_to_earlier_claim():
    gh = FakeGh()
    gh.add(6)
    real = gh.__call__

    def racing(args, stdin=None):
        # Another runner's claim lands (earlier server time) just before ours.
        if args[:2] == ["issue", "comment"] and "action=claim" in (stdin or ""):
            gh.issues[6]["comments"].append(
                {"body": other_claim("mayank-9", "")[0], "createdAt": "2026-11-20T21:59:59Z"})
        return real(args, stdin)

    with tempfile.TemporaryDirectory() as tmp:
        c = nqc.Claimer(nqc.Gh("o/r", racing), Path(tmp) / "claims.json", host="h", now=lambda: gh.clock)
        c.init()
        out = c.claim([6])
        assert out["claimed"] == []
        assert out["conflicts"][0]["reason"] == "lost_race"
        assert [a["cycle"] for a in nqc.active_claims(gh.issues[6])] == ["mayank-9"]
        assert labels(gh, 6) == ["in progress"]  # theirs stays


def test_release_removes_label_only_when_no_other_claim():
    gh = FakeGh()
    gh.add(7)
    with tempfile.TemporaryDirectory() as tmp:
        c = claimer(gh, tmp)
        c.init()
        c.claim([7])
        out = c.release([7], "dropped_by_triage")
        assert out["released"] == [7]
        assert labels(gh, 7) == []
        assert nqc.active_claims(gh.issues[7]) == []
        # releasing something we never claimed is a no-op
        gh.add(8, labels=["in progress"], comments=[other_claim("m", "2026-11-20T20:00:00Z")])
        assert c.release([8], "x")["released"] == []
        assert labels(gh, 8) == ["in progress"]


def test_release_unfinished_keeps_pr_opened():
    gh = FakeGh()
    gh.add(10)
    gh.add(11)
    gh.add(12)
    with tempfile.TemporaryDirectory() as tmp:
        work = Path(tmp)
        (work / "work-10.json").write_text(json.dumps({"status": "pr_opened"}))
        (work / "work-11.json").write_text(json.dumps({"status": "failed"}))
        c = claimer(gh, tmp)
        c.init()
        c.claim([10, 11, 12])
        out = c.release_unfinished(work)
        assert out["kept_pr_opened"] == [10]
        assert sorted(out["released"]) == [11, 12]
        assert labels(gh, 10) == ["in progress"]
        assert labels(gh, 11) == [] and labels(gh, 12) == []
        assert "reason=work_failed" in gh.issues[11]["comments"][-1]["body"]
        assert "reason=not_reached" in gh.issues[12]["comments"][-1]["body"]


def test_init_releases_previous_unfinished_cycle():
    gh = FakeGh()
    gh.add(13)
    with tempfile.TemporaryDirectory() as tmp:
        c = claimer(gh, tmp)
        first = c.init()["cycle_id"]
        c.claim([13])
        gh.clock += dt.timedelta(hours=5)
        c2 = claimer(gh, tmp)
        out = c2.init(work_dir=Path(tmp))
        assert out["released_previous"] == [13]
        assert out["cycle_id"] != first
        assert (Path(tmp) / ("claims-" + first + ".json")).exists()
        assert labels(gh, 13) == []
        # resume with the same id keeps state
        assert c2.init(cycle_id=c2.cycle)["resumed"] is True


def test_sweep_stale():
    gh = FakeGh()
    old = "2026-11-19T10:00:00Z"
    gh.add(20, labels=["in progress"], comments=[other_claim("old", old)])           # stale claim
    gh.add(21, labels=["in progress"], comments=[other_claim("new", "2026-11-20T18:00:00Z")])  # fresh
    gh.add(22, labels=["in progress"], comments=[other_claim("old", old)])           # stale but open PR
    gh.add(23, labels=["in progress"], assignees=["mayank-intsof"], updated=old)    # human-owned
    gh.add(24, labels=["in progress"], updated=old)                                # legacy, stale
    gh.add(25, labels=["in progress"], updated="2026-11-20T12:00:00Z")             # legacy, fresh
    gh.prs = [{"number": 900, "title": "x", "body": "", "headRefName": "fix/issue-22-thing"}]
    with tempfile.TemporaryDirectory() as tmp:
        c = claimer(gh, tmp)
        c.init()
        out = c.sweep_stale(24)
        assert sorted(out["cleared"]) == [20, 24]
        kept = {k["issue"]: k["reason"] for k in out["kept"]}
        assert kept == {21: "fresh", 22: "open_pr", 23: "assigned", 25: "fresh"}
        assert nqc.active_claims(gh.issues[20]) == []
        assert labels(gh, 21) == ["in progress"]


def test_sweep_never_clears_current_cycle():
    gh = FakeGh()
    gh.add(30)
    with tempfile.TemporaryDirectory() as tmp:
        c = claimer(gh, tmp)
        c.init()
        c.claim([30])
        gh.clock += dt.timedelta(hours=30)
        out = c.sweep_stale(24)
        assert out["cleared"] == []
        assert labels(gh, 30) == ["in progress"]


def test_queue_numbers_shapes():
    with tempfile.TemporaryDirectory() as tmp:
        p = Path(tmp) / "q.json"
        p.write_text(json.dumps({"queue": [
            {"issue_number": 5, "disposition": "implement"},
            {"issue_number": 6, "disposition": "skip"},
            {"issue_number": 7, "disposition": "split"},
            {"issue_number": 5, "disposition": "implement"},
        ]}))
        assert nqc.queue_numbers(p) == [5, 7]
        p.write_text(json.dumps({"slotted_for_work": [9, "file:blocker:9:0", "10"]}))
        assert nqc.queue_numbers(p) == [9, 10]


def test_cli_claim_round_trip():
    gh = FakeGh()
    gh.add(40)
    with tempfile.TemporaryDirectory() as tmp:
        claims = str(Path(tmp) / "claims.json")
        assert nqc.main(["--repo", "o/r", "--claims", claims, "--host", "h", "init"], runner=gh) == 0
        assert nqc.main(["--repo", "o/r", "--claims", claims, "claim", "--issues", "#40"], runner=gh) == 0
        assert labels(gh, 40) == ["in progress"]
        assert nqc.main(["--repo", "o/r", "--claims", claims, "release", "--issues", "40",
                         "--reason", "deferred"], runner=gh) == 0
        assert labels(gh, 40) == []
