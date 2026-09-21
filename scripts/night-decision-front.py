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
"""Deterministic decision-front for night-issue-prs (gh facts + Jev hints).

Replaces the expensive Preflight / Reconcile / Triage Grok agents when the
host can rank a queue from JSON. TypeSafe/Jev skip+close hints are optional
inputs (from typesafe-prescreen.py); they never override labels.

Fail-open: missing optional files still produce a queue from issues+PRs.

Does not call GitHub unless --live is set (tests pass fixture JSON).
Does not merge PRs. Mechanical close list is emitted; --apply-closes is
off by default (workflow agent may pass it after reading the list).
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

FIXES_RE = re.compile(r"(?:fixes|closes|resolves)\s+#(\d+)", re.I)
ISSUE_HEAD_RE = re.compile(r"issue-(\d+)")
PARENT_TITLE_RE = re.compile(r"issue\s+(\d+)\s+slice", re.I)
PARENT_BODY_RE = re.compile(r"parent:\s*#(\d+)", re.I)
PN_RE = re.compile(r"^p([1-8])$", re.I)

THRASH_HINTS = (
    "sitemanage-beans.xml",
    "paths.ts",
    "messages.ts",
    "DeveloperShell.tsx",
    "allowlists.ts",
    "developer/rest.md",
    "package.json",
    "CatalogRestJaxrsRegistrationTest.java",
)

SELF_OPERATORS = frozenset({"operator:grok", "operator:night-issue-prs"})


def _load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def _label_names(labels: object) -> list[str]:
    if not labels:
        return []
    names = []
    for lab in labels:
        if isinstance(lab, str):
            names.append(lab)
        elif isinstance(lab, dict) and isinstance(lab.get("name"), str):
            names.append(lab["name"])
    return names


def _login(obj: object) -> str:
    if obj is None:
        return ""
    if isinstance(obj, str):
        return obj
    if isinstance(obj, dict):
        return str(obj.get("login") or "")
    return ""


def _assignees(raw: object) -> list[str]:
    if not raw:
        return []
    return [a for a in (_login(x) for x in raw) if a]


def normalize_issue(raw: dict) -> dict:
    author = raw.get("author")
    return {
        "number": int(raw["number"]),
        "title": str(raw.get("title") or ""),
        "body": str(raw.get("body") or ""),
        "labels": _label_names(raw.get("labels")),
        "assignees": _assignees(raw.get("assignees")),
        "author": _login(author) or str(author or "unknown"),
        "updatedAt": str(raw.get("updatedAt") or ""),
    }


def normalize_pr(raw: dict) -> dict:
    head = raw.get("headRefName") or raw.get("head") or ""
    if isinstance(head, dict):
        head = head.get("ref") or ""
    files = []
    for f in raw.get("files") or []:
        if isinstance(f, str):
            files.append(f)
        elif isinstance(f, dict):
            p = f.get("path") or f.get("filename")
            if p:
                files.append(str(p))
    reviews = raw.get("reviews") or []
    approved = False
    for rev in reviews:
        if isinstance(rev, dict) and str(rev.get("state") or "").upper() == "APPROVED":
            approved = True
    mergeable = str(raw.get("mergeable") or raw.get("mergeStateStatus") or "")
    return {
        "number": int(raw["number"]),
        "title": str(raw.get("title") or ""),
        "body": str(raw.get("body") or ""),
        "head": str(head),
        "state": str(raw.get("state") or "OPEN").upper(),
        "author": _login(raw.get("author")),
        "labels": _label_names(raw.get("labels")),
        "files": files,
        "approved": approved,
        "mergeable": mergeable.upper(),
        "isDraft": bool(raw.get("isDraft") or raw.get("draft")),
    }


def parent_of(issue: dict) -> int:
    m = PARENT_TITLE_RE.search(issue["title"])
    if m:
        return int(m.group(1))
    m = PARENT_BODY_RE.search(issue.get("body") or "")
    if m:
        return int(m.group(1))
    return int(issue["number"])


def priority_of(issue: dict) -> str:
    for name in issue["labels"]:
        m = PN_RE.match(name.strip())
        if m:
            return "p" + m.group(1)
    return "Unset"


def is_debt(issue: dict) -> bool:
    labels = {n.lower() for n in issue["labels"]}
    if "tech-debt" in labels:
        return True
    p = priority_of(issue)
    return p in ("p7", "p8")


def is_slice_or_residual(issue: dict) -> bool:
    t = issue["title"]
    tl = t.lower()
    if "slice" in tl:
        return True
    if tl.startswith("residual:") or tl.startswith("issue "):
        return True
    return False


def is_epic_title(issue: dict) -> bool:
    t = issue["title"]
    if t.startswith("[8.2]"):
        return True
    if "epic" in t.lower() and "slice" not in t.lower():
        return True
    return False


def covering_map(prs: list[dict]) -> dict[int, int]:
    covered: dict[int, int] = {}
    for pr in prs:
        if pr["state"] not in ("OPEN", ""):
            continue
        blob = pr["title"] + "\n" + pr["body"] + "\n" + pr["head"]
        for m in FIXES_RE.finditer(blob):
            covered[int(m.group(1))] = pr["number"]
        for m in ISSUE_HEAD_RE.finditer(pr["head"]):
            covered[int(m.group(1))] = pr["number"]
    return covered


def pr_touches_thrash(pr: dict) -> bool:
    for path in pr["files"]:
        for hint in THRASH_HINTS:
            if hint in path.replace("\\", "/"):
                return True
    return False


def cluster_recommended(prs: list[dict], min_prs: int) -> bool:
    open_prs = [p for p in prs if p["state"] in ("OPEN", "")]
    if len(open_prs) < 2:
        return False
    groups: dict[str, set[int]] = {}
    dirty = 0
    for pr in open_prs:
        st = pr["mergeable"]
        if st in ("CONFLICTING", "DIRTY"):
            dirty += 1
        for path in pr["files"]:
            groups.setdefault(path, set()).add(pr["number"])
    for members in groups.values():
        if len(members) >= min_prs:
            return True
        if len(members) >= 2 and dirty >= 2:
            return True
    return False


def prescreen_index(prescreen: dict | None) -> dict[int, dict]:
    out: dict[int, dict] = {}
    if not prescreen:
        return out
    for row in prescreen.get("issues") or []:
        try:
            n = int(row["number"])
        except (KeyError, TypeError, ValueError):
            continue
        out[n] = row
    return out


def rule_skip_issue(issue: dict, prescreen_row: dict | None) -> tuple[bool, str]:
    if prescreen_row and prescreen_row.get("rule_skip"):
        return True, str(prescreen_row.get("rule_kind") or "rule")
    labels = {n.lower() for n in issue["labels"]}
    if "not safe for agents" in labels:
        return True, "not_safe_agents"
    if "in progress" in labels or "in-progress" in labels:
        return True, "in_progress"
    if "qa task" in labels:
        return True, "human_qa"
    if "migrated" in labels:
        return True, "migrated"
    if issue["assignees"]:
        return True, "assigned"
    return False, ""


def model_skip(prescreen_row: dict | None) -> bool:
    if not prescreen_row:
        return False
    return bool(prescreen_row.get("recommend_skip"))


def pn_rank(p: str) -> int:
    order = {
        "p1": 0,
        "p2": 1,
        "p3": 2,
        "p4": 3,
        "p5": 4,
        "p6": 5,
        "Unset": 6,
        "p7": 7,
        "p8": 8,
    }
    return order.get(p, 6)


def peer_eligible(pr: dict, self_model_label: str) -> bool:
    labels = {n.lower() for n in pr["labels"]}
    if pr["approved"]:
        return False
    if pr["mergeable"] in ("CONFLICTING", "DIRTY"):
        return False
    other_op = False
    other_model = False
    own_model = False
    for n in labels:
        if n.startswith("operator:") and n not in SELF_OPERATORS:
            other_op = True
        if n.startswith("model:"):
            if self_model_label and n == self_model_label.lower():
                own_model = True
            else:
                other_model = True
    if other_op or other_model:
        return True
    if own_model and not other_op:
        return False
    return False


def build_queue(
    issues: list[dict],
    prs: list[dict],
    prescreen: dict | None,
    max_issues: int,
    low_slots: int,
) -> tuple[list[dict], dict]:
    ps = prescreen_index(prescreen)
    covered = covering_map(prs)
    open_prs = [p for p in prs if p["state"] in ("OPEN", "")]
    thrash_busy = any(pr_touches_thrash(p) for p in open_prs)
    parents_covered = set()
    for issue in issues:
        n = issue["number"]
        if n in covered:
            parents_covered.add(parent_of(issue))
            parents_covered.add(n)

    candidates = []
    skipped = []
    for issue in issues:
        n = issue["number"]
        row = ps.get(n)
        skip, kind = rule_skip_issue(issue, row)
        if skip:
            skipped.append((n, kind))
            continue
        if model_skip(row):
            skipped.append((n, "prescreen_model"))
            continue
        if is_epic_title(issue) and not is_slice_or_residual(issue):
            skipped.append((n, "epic"))
            continue
        if n in covered:
            skipped.append((n, "covering_pr"))
            continue
        par = parent_of(issue)
        if par in parents_covered or par in covered:
            skipped.append((n, "sibling_covering_pr"))
            continue
        if thrash_busy and par in parents_covered:
            skipped.append((n, "hot_path_busy"))
            continue
        debt = is_debt(issue)
        candidates.append(
            {
                "issue": issue,
                "parent": par,
                "priority": priority_of(issue),
                "debt": debt,
            }
        )

    product = [c for c in candidates if not c["debt"]]
    debt_c = [c for c in candidates if c["debt"]]
    product.sort(key=lambda c: (pn_rank(c["priority"]), c["issue"]["number"]))
    debt_c.sort(key=lambda c: (pn_rank(c["priority"]), c["issue"]["number"]))

    queue = []
    used_parents: set[int] = set()
    for pool, cap in ((product, max_issues), (debt_c, low_slots)):
        for c in pool:
            if len(queue) >= max_issues:
                break
            if cap == low_slots and len([q for q in queue if q.get("_debt")]) >= low_slots:
                break
            par = c["parent"]
            if par in used_parents:
                continue
            issue = c["issue"]
            item = {
                "issue_number": issue["number"],
                "title": issue["title"],
                "disposition": "implement",
                "rationale": "decision-front leftover; one-per-parent; no covering PR",
                "work_summary": (
                    "PARENT #"
                    + str(par)
                    + ". Implement this slice as one vertical PR (REST+impl+UI+"
                    "Playwright H2+product-docs as applicable). Update parent "
                    "## Agent progress. Out of scope: sibling slices."
                ),
                "priority": c["priority"],
                "parent_issue": par,
                "modules": "",
                "slice_title": issue["title"],
                "split_plan": "",
                "_debt": c["debt"],
            }
            queue.append(item)
            used_parents.add(par)
            if cap == max_issues and not c["debt"] and len([q for q in queue if not q.get("_debt")]) >= max_issues:
                break

    for item in queue:
        item.pop("_debt", None)
    stats = {
        "candidates": len(candidates),
        "skipped": skipped[:40],
        "product_queued": sum(1 for q in queue if True),
    }
    return queue[:max_issues], stats


def build_signals(
    issues: list[dict],
    prs: list[dict],
    self_model_label: str,
    cluster_min_prs: int,
) -> dict:
    open_prs = [p for p in prs if p["state"] in ("OPEN", "")]
    blockers = []
    peer = []
    approved = []
    for pr in open_prs:
        n = pr["number"]
        if pr["mergeable"] in ("CONFLICTING", "DIRTY"):
            blockers.append(n)
        if pr["approved"]:
            approved.append(n)
        if peer_eligible(pr, self_model_label):
            peer.append(n)
    return {
        "owned_pr_count": len(open_prs),
        "blocker_pr_count": len(blockers),
        "blocker_prs": ",".join("#" + str(n) for n in blockers),
        "peer_eligible_count": len(peer),
        "peer_eligible_prs": ",".join("#" + str(n) for n in peer),
        "prs_with_approve_count": len(approved),
        "prs_with_approve": ",".join("#" + str(n) for n in approved),
        "cluster_recommended": cluster_recommended(open_prs, cluster_min_prs),
        "cycle_verify_residual_count": sum(
            1 for i in issues if i["title"].startswith("[night-issues: Cycle Verify]")
        ),
        "qa_failed_count": sum(
            1 for i in issues if "qa: failed" in {n.lower() for n in i["labels"]}
        ),
    }


def close_candidates(issues: list[dict], prescreen: dict | None) -> list[int]:
    ps = prescreen_index(prescreen)
    out = []
    for issue in issues:
        row = ps.get(issue["number"])
        if row and row.get("recommend_close"):
            out.append(issue["number"])
        labels = {n.lower() for n in issue["labels"]}
        if "migrated" in labels:
            out.append(issue["number"])
    return sorted(set(out))


def compact_inventory(issues: list[dict], max_rows: int = 40) -> str:
    lines = []
    for issue in issues[:max_rows]:
        p = priority_of(issue)
        lines.append(
            "#"
            + str(issue["number"])
            + " | "
            + issue["author"]
            + " | "
            + p
            + " | "
            + issue["title"]
        )
    return "\n".join(lines)


def run(
    issues_raw: list,
    prs_raw: list,
    prescreen: dict | None,
    max_issues: int,
    low_slots: int,
    self_model_label: str,
    cluster_min_prs: int,
) -> dict:
    issues = [normalize_issue(x) for x in issues_raw]
    prs = [normalize_pr(x) for x in prs_raw]
    queue, stats = build_queue(issues, prs, prescreen, max_issues, low_slots)
    signals = build_signals(issues, prs, self_model_label, cluster_min_prs)
    closes = close_candidates(issues, prescreen)
    implement = ",".join("#" + str(q["issue_number"]) for q in queue)
    notes = (
        "decision-front script: queued "
        + str(len(queue))
        + "/"
        + str(max_issues)
        + " one-per-parent; skipped="
        + str(len(stats["skipped"]))
        + "; cluster_recommended="
        + str(signals["cluster_recommended"]).lower()
        + ". Does not create new DESIGN_GAPS children (host may spawn Triage if slots empty)."
    )
    return {
        "status": "ok",
        "summary": (
            "decision-front issues="
            + str(len(issues))
            + " open_prs="
            + str(signals["owned_pr_count"])
            + " queued="
            + str(len(queue))
            + " close_hints="
            + str(len(closes))
        ),
        "inventory": compact_inventory(issues),
        "signals_complete": True,
        "open_alert_count": 0,
        "issues_scanned": 0,
        "issues_stale": 0,
        "issues_cleared": 0,
        "issues_kept": "",
        "issues_cleared_list": "",
        "label_name_used": "in progress",
        "blocked": "",
        "queue": queue,
        "notes": notes,
        "implement_candidates": implement,
        "issues_inspected": len(issues),
        "issues_closed": 0,
        "issues_closed_list": "",
        "qa_failed_closed": 0,
        "qa_failed_closed_list": "",
        "remaining_notes": notes,
        "close_hints": ",".join("#" + str(n) for n in closes),
        "expand_needed": len(queue) < max_issues,
        **signals,
    }


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(prog="night-decision-front.py")
    p.add_argument("--issues", required=True, help="JSON array of gh issues")
    p.add_argument("--prs", help="JSON array of open PRs (default empty)")
    p.add_argument("--prescreen", help="scratch/prescreen.json from typesafe-prescreen")
    p.add_argument("--out", required=True)
    p.add_argument("--max-issues", type=int, default=3)
    p.add_argument("--low-slots", type=int, default=0)
    p.add_argument("--self-model-label", default="model:grok-4.6")
    p.add_argument("--cluster-min-prs", type=int, default=3)
    args = p.parse_args(argv)

    issues_path = Path(args.issues)
    if not issues_path.is_file():
        print("unreadable --issues", file=sys.stderr)
        return 2
    issues_raw = _load_json(issues_path)
    if not isinstance(issues_raw, list):
        print("--issues must be a JSON array", file=sys.stderr)
        return 2
    prs_raw = []
    if args.prs:
        prs_path = Path(args.prs)
        if not prs_path.is_file():
            print("unreadable --prs", file=sys.stderr)
            return 2
        loaded = _load_json(prs_path)
        if not isinstance(loaded, list):
            print("--prs must be a JSON array", file=sys.stderr)
            return 2
        prs_raw = loaded
    prescreen = None
    if args.prescreen:
        ps_path = Path(args.prescreen)
        if ps_path.is_file():
            prescreen = _load_json(ps_path)

    result = run(
        issues_raw,
        prs_raw,
        prescreen,
        max(1, args.max_issues),
        max(0, args.low_slots),
        str(args.self_model_label).lower(),
        max(2, args.cluster_min_prs),
    )
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
