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
"""Queue-time issue claims for night-issue-prs (2.2.13-draft, 2026-11-20).

Why: the In Progress label used to be applied only when a Work agent
*started* an issue. With serial Work that can be hours after the queue was
built, so a runner on another machine (observed 2026-11-20: #6651, our
#6840 vs mayank-intsof's #6847) saw an unlabeled issue, queued it, and
both runners implemented it. Worse, our Work step then removed the other
runner's fresh label on exit.

Mechanism (existing, shared by every runner): the ``in progress`` label.
Every runner's decision-front / triage / Work claim-check already skips
issues carrying it. This script adds an auditable claim comment with a
machine-readable marker so runners can tell *whose* claim it is:

    <!-- night-claim:v1 action=claim cycle=<id> host=<host> -->
    <!-- night-claim:v1 action=release cycle=<id> host=<host> reason=<r> [scope=all] -->

Subcommands (all write/refresh ``scratch/claims.json``):

  init            start a cycle id (releases a previous unfinished file first)
  claim           claim queue rows the moment they enter the queue; rows
                  already claimed by anyone else come back as ``conflicts``
                  (the host drops them and backfills)
  release         release named issues (dropped / deferred / failed rows)
  release-unfinished
                  Report phase: release every claim of this cycle whose
                  ``scratch/work-<N>.json`` is not ``pr_opened``
  release-closed  release every claim whose issue is now CLOSED (merge-close
                  or otherwise) and clear the label from all CLOSED issues
                  still carrying it; keeps OPEN issues untouched
  sweep-stale     clear claims older than ``--stale-hours`` (default 24)
                  with no covering open PR; never touches assigned issues
                  or the current cycle's claims

Ties: if two runners claim in the same window, the earliest claim comment
(GitHub server ``createdAt``) wins; the loser posts a release and yields.

A release removes the label only when no other runner's active claim
remains. Never prints tokens; only shells out to ``gh``.
"""
from __future__ import annotations

import argparse
import datetime as _dt
import json
import re
import socket
import subprocess
import sys
from pathlib import Path
from typing import Callable

LABEL = "in progress"
LABEL_ALIASES = {"in progress", "in-progress"}
MARKER_RE = re.compile(r"<!--\s*night-claim:v1\s+([^>]*?)\s*-->")
FIXES_RE = re.compile(r"(?:fixes|closes|resolves)\s+#(\d+)", re.I)
ISSUE_HEAD_RE = re.compile(r"issue-(\d+)")
DEFAULT_CLAIMS = "scratch/claims.json"
DEFAULT_REPO = "intersoftdatalabs-in/percussioncms"
TERMINAL = {"released", "conflict"}


def utcnow() -> _dt.datetime:
    return _dt.datetime.now(_dt.timezone.utc)


def iso(ts: _dt.datetime) -> str:
    return ts.astimezone(_dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def parse_ts(raw: str) -> _dt.datetime | None:
    if not raw:
        return None
    try:
        return _dt.datetime.strptime(raw, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=_dt.timezone.utc)
    except ValueError:
        try:
            return _dt.datetime.fromisoformat(raw.replace("Z", "+00:00"))
        except ValueError:
            return None


# --------------------------------------------------------------------- gh


Runner = Callable[[list[str], str | None], str]


def default_runner(args: list[str], stdin: str | None = None) -> str:
    proc = subprocess.run(
        ["gh", *args], input=stdin, capture_output=True, text=True, check=False
    )
    if proc.returncode != 0:
        raise RuntimeError("gh " + " ".join(args[:3]) + " failed: " + proc.stderr.strip()[:300])
    return proc.stdout


class Gh:
    def __init__(self, repo: str, runner: Runner = default_runner):
        self.repo = repo
        self.run = runner

    def issue(self, n: int) -> dict:
        out = self.run(
            ["issue", "view", str(n), "--repo", self.repo,
             "--json", "number,state,labels,assignees,comments,updatedAt"],
            None,
        )
        return json.loads(out)

    def add_label(self, n: int) -> None:
        self.run(["issue", "edit", str(n), "--repo", self.repo, "--add-label", LABEL], None)

    def remove_label(self, n: int, name: str = LABEL) -> None:
        self.run(["issue", "edit", str(n), "--repo", self.repo, "--remove-label", name], None)

    def comment(self, n: int, body: str) -> None:
        self.run(["issue", "comment", str(n), "--repo", self.repo, "--body-file", "-"], body)

    def labeled_open(self) -> list[dict]:
        out = self.run(
            ["issue", "list", "--repo", self.repo, "--state", "open", "--label", LABEL,
             "--limit", "200", "--json", "number,updatedAt,assignees,labels"],
            None,
        )
        return json.loads(out or "[]")

    def labeled_closed(self) -> list[dict]:
        out = self.run(
            ["issue", "list", "--repo", self.repo, "--state", "closed", "--label", LABEL,
             "--limit", "200", "--json", "number,updatedAt,assignees,labels"],
            None,
        )
        return json.loads(out or "[]")

    def open_prs(self) -> list[dict]:
        out = self.run(
            ["pr", "list", "--repo", self.repo, "--state", "open", "--limit", "300",
             "--json", "number,title,body,headRefName"],
            None,
        )
        return json.loads(out or "[]")


# ----------------------------------------------------------------- markers


def marker(action: str, cycle: str, host: str, **extra: str) -> str:
    parts = ["action=" + action, "cycle=" + cycle, "host=" + host]
    parts += [k + "=" + v for k, v in extra.items() if v]
    return "<!-- night-claim:v1 " + " ".join(parts) + " -->"


def parse_marker(body: str) -> list[dict]:
    found = []
    for m in MARKER_RE.finditer(body or ""):
        kv = {}
        for tok in m.group(1).split():
            if "=" in tok:
                k, v = tok.split("=", 1)
                kv[k] = v
        if kv.get("action") in ("claim", "release") and kv.get("cycle"):
            found.append(kv)
    return found


def active_claims(issue: dict) -> list[dict]:
    """Return active claims ``[{cycle, host, at}]`` ordered by comment createdAt."""
    active: dict[str, dict] = {}
    comments = sorted(issue.get("comments") or [], key=lambda c: c.get("createdAt") or "")
    for c in comments:
        for kv in parse_marker(c.get("body") or ""):
            cyc = kv["cycle"]
            if kv["action"] == "claim":
                active.setdefault(cyc, {"cycle": cyc, "host": kv.get("host", ""), "at": c.get("createdAt") or ""})
            elif kv.get("scope") == "all":
                active.clear()
            else:
                active.pop(cyc, None)
    return sorted(active.values(), key=lambda a: a["at"])


def labels_of(issue: dict) -> set[str]:
    out = set()
    for lab in issue.get("labels") or []:
        name = lab.get("name") if isinstance(lab, dict) else lab
        if name:
            out.add(str(name).lower())
    return out


def has_label(issue: dict) -> bool:
    return bool(labels_of(issue) & LABEL_ALIASES)


def label_name(issue: dict) -> str:
    for lab in issue.get("labels") or []:
        name = lab.get("name") if isinstance(lab, dict) else lab
        if name and str(name).lower() in LABEL_ALIASES:
            return str(name)
    return LABEL


def assignees(issue: dict) -> list[str]:
    return [a.get("login", "") if isinstance(a, dict) else str(a) for a in issue.get("assignees") or []]


def covered_issues(prs: list[dict]) -> set[int]:
    covered: set[int] = set()
    for pr in prs:
        blob = (pr.get("title") or "") + "\n" + (pr.get("body") or "")
        covered |= {int(m.group(1)) for m in FIXES_RE.finditer(blob)}
        covered |= {int(m.group(1)) for m in ISSUE_HEAD_RE.finditer(pr.get("headRefName") or "")}
    return covered


# ------------------------------------------------------------------- state


def load_state(path: Path) -> dict:
    if path.exists():
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            pass
    return {}


def save_state(path: Path, state: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(state, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    tmp.replace(path)


def new_cycle_id(host: str, now: _dt.datetime) -> str:
    safe = re.sub(r"[^A-Za-z0-9_.-]", "-", host) or "host"
    return safe + "-" + now.strftime("%Y%m%dT%H%M%SZ")


def queue_numbers(path: Path) -> list[int]:
    """Accept decision-front (``queue[].issue_number``) or triage (``slotted_for_work``)."""
    data = json.loads(path.read_text(encoding="utf-8"))
    nums: list[int] = []
    if isinstance(data, dict) and isinstance(data.get("slotted_for_work"), list):
        for v in data["slotted_for_work"]:
            if isinstance(v, int) or (isinstance(v, str) and v.isdigit()):
                nums.append(int(v))
    elif isinstance(data, dict) and isinstance(data.get("queue"), list):
        for row in data["queue"]:
            if str(row.get("disposition", "implement")) in ("implement", "split"):
                n = row.get("issue_number", row.get("number"))
                if n is not None:
                    nums.append(int(n))
    elif isinstance(data, list):
        nums = [int(v) for v in data if str(v).isdigit()]
    seen, out = set(), []
    for n in nums:
        if n not in seen:
            seen.add(n)
            out.append(n)
    return out


def parse_csv(raw: str | None) -> list[int]:
    return [int(t.strip().lstrip("#")) for t in (raw or "").split(",") if t.strip().lstrip("#").isdigit()]


# ------------------------------------------------------- stale sweep helpers


def sweep_guard_reason(row: dict, n: int, covered: set[int]) -> str | None:
    """Pre-lookup keep reason for a labeled-open sweep row, else None.

    Cheap guards that need no extra issue fetch: human-owned rows and
    rows covered by an open PR are never stale.
    """
    if assignees(row):
        return "assigned"
    if n in covered:
        return "open_pr"
    return None


def current_cycle_claimed(claims: list[dict], cycle: str) -> bool:
    """True when one of the active claims belongs to this cycle."""
    return any(a["cycle"] == cycle for a in claims)


def stale_activity(claims: list[dict], issue: dict, row: dict) -> tuple[_dt.datetime | None, str]:
    """Resolve (last activity, basis): newest claim, else updatedAt."""
    if claims:
        return parse_ts(claims[-1]["at"]), "claim"
    return parse_ts(issue.get("updatedAt") or row.get("updatedAt") or ""), "updatedAt"


# --------------------------------------------------------------- actions


class Claimer:
    def __init__(self, gh: Gh, state_path: Path, host: str | None = None,
                 now: Callable[[], _dt.datetime] = utcnow):
        self.gh = gh
        self.path = state_path
        self.host = host or socket.gethostname()
        self.now = now
        self.state = load_state(state_path)

    # -- cycle
    @property
    def cycle(self) -> str:
        return self.state.get("cycle_id", "")

    def init(self, cycle_id: str | None = None, work_dir: Path | None = None) -> dict:
        prev = self.state
        released: list[int] = []
        if prev.get("cycle_id") and prev.get("cycle_id") != cycle_id:
            released = self.release_unfinished(work_dir, reason="previous_cycle_unfinished")["released"]
            arch = self.path.with_name("claims-" + prev["cycle_id"] + ".json")
            save_state(arch, self.state)
        if cycle_id and prev.get("cycle_id") == cycle_id:
            return {"cycle_id": cycle_id, "resumed": True, "released_previous": []}
        now = self.now()
        self.state = {
            "cycle_id": cycle_id or new_cycle_id(self.host, now),
            "host": self.host,
            "repo": self.gh.repo,
            "started_at": iso(now),
            "claims": {},
        }
        save_state(self.path, self.state)
        return {"cycle_id": self.cycle, "resumed": False, "released_previous": released}

    def _require_cycle(self) -> None:
        if not self.cycle:
            self.init()

    def _record(self, n: int, **fields: str) -> None:
        rec = self.state.setdefault("claims", {}).setdefault(str(n), {})
        rec.update(fields)
        save_state(self.path, self.state)

    # -- claim
    def claim(self, numbers: list[int]) -> dict:
        self._require_cycle()
        claimed, conflicts = [], []
        for n in numbers:
            rec = self.state.get("claims", {}).get(str(n), {})
            if rec.get("state") in ("claimed", "pr_opened"):
                claimed.append(n)
                continue
            try:
                issue = self.gh.issue(n)
            except RuntimeError as exc:
                conflicts.append({"issue": n, "reason": "lookup_failed", "detail": str(exc)[:200]})
                self._record(n, state="conflict", reason="lookup_failed")
                continue
            reason, owner = self._blocked(issue)
            if reason:
                conflicts.append({"issue": n, "reason": reason, "owner": owner})
                self._record(n, state="conflict", reason=reason, owner=owner)
                continue
            mine = [a for a in active_claims(issue) if a["cycle"] == self.cycle]
            if not mine:
                if not has_label(issue):
                    self.gh.add_label(n)
                self.gh.comment(n, self._claim_body())
                issue = self.gh.issue(n)
            winner = (active_claims(issue) or [{"cycle": self.cycle}])[0]
            if winner["cycle"] != self.cycle:
                self.gh.comment(n, self._release_body("lost_race_to_" + winner["cycle"]))
                conflicts.append({"issue": n, "reason": "lost_race", "owner": winner["cycle"]})
                self._record(n, state="conflict", reason="lost_race", owner=winner["cycle"])
                continue
            self._record(n, state="claimed", at=iso(self.now()))
            claimed.append(n)
        return {"cycle_id": self.cycle, "claimed": claimed, "conflicts": conflicts}

    def _blocked(self, issue: dict) -> tuple[str, str]:
        if str(issue.get("state", "OPEN")).upper() != "OPEN":
            return "closed", ""
        who = assignees(issue)
        if who:
            return "assigned", ",".join(who)
        others = [a for a in active_claims(issue) if a["cycle"] != self.cycle]
        mine = [a for a in active_claims(issue) if a["cycle"] == self.cycle]
        if others and not mine:
            return "claimed_by_other", others[0]["cycle"] + "@" + others[0]["host"]
        if has_label(issue) and not others and not mine:
            # Label without a marker: a legacy / human / older-runner claim.
            return "claimed_by_other", "label-only"
        return "", ""

    def _claim_body(self) -> str:
        return (
            "night-issue-prs: queued and claimed by cycle `" + self.cycle + "` on `" + self.host
            + "` at " + iso(self.now()) + ". Other runners: please skip this issue while the "
            "`in progress` label is set; it is released automatically if this cycle drops it.\n\n"
            + marker("claim", self.cycle, self.host)
        )

    def _release_body(self, reason: str, scope: str = "") -> str:
        return (
            "night-issue-prs: claim released by cycle `" + self.cycle + "` on `" + self.host
            + "` (" + reason + ").\n\n" + marker("release", self.cycle, self.host, reason=reason, scope=scope)
        )

    # -- release
    def release(self, numbers: list[int], reason: str) -> dict:
        self._require_cycle()
        released, skipped = [], []
        for n in numbers:
            rec = self.state.get("claims", {}).get(str(n))
            if not rec or rec.get("state") in TERMINAL:
                skipped.append({"issue": n, "reason": "not_claimed_by_this_cycle"})
                continue
            issue = self.gh.issue(n)
            others = [a for a in active_claims(issue) if a["cycle"] != self.cycle]
            if has_label(issue) and not others:
                self.gh.remove_label(n, label_name(issue))
            self.gh.comment(n, self._release_body(reason))
            self._record(n, state="released", reason=reason, released_at=iso(self.now()))
            released.append(n)
        return {"cycle_id": self.cycle, "released": released, "skipped": skipped}

    def release_unfinished(self, work_dir: Path | None, reason: str = "not_reached") -> dict:
        todo, kept = [], []
        for key, rec in sorted(self.state.get("claims", {}).items(), key=lambda kv: int(kv[0])):
            if rec.get("state") != "claimed":
                continue
            n = int(key)
            status = work_status(work_dir, n)
            if status == "pr_opened":
                self._record(n, state="pr_opened")
                kept.append(n)
            else:
                todo.append((n, status))
        released = []
        for n, status in todo:
            released += self.release([n], reason if not status else "work_" + status)["released"]
        return {"cycle_id": self.cycle, "released": released, "kept_pr_opened": kept}

    def release_closed(self, numbers: list[int] | None = None, max_clears: int = 40) -> dict:
        """Release claims whose issues are now CLOSED and drop the label.

        Merge-closed (or otherwise closed) issues kept the ``in progress``
        label forever: ``release-unfinished`` deliberately keeps
        ``pr_opened`` claims to guard the open PR, and ``sweep-stale``
        only scans OPEN labeled issues. A claim on a closed issue is
        definitionally dead, so CLOSED issues always lose the label here —
        own claim records move to ``released`` (reason ``issue_closed``)
        and every cleared row gets a release comment so the marker history
        resolves to no active claims.

        Scans this cycle's non-terminal claims plus every CLOSED issue
        still carrying the label (the pre-fix backlog). OPEN issues are
        kept untouched.
        """
        self._require_cycle()
        own = [int(k) for k, rec in self.state.get("claims", {}).items()
               if rec.get("state") not in TERMINAL]
        if numbers:
            own = list(dict.fromkeys(own + [int(n) for n in numbers]))
        try:
            labeled = [int(r["number"]) for r in self.gh.labeled_closed()]
        except RuntimeError:
            labeled = []
        todo = list(dict.fromkeys(own + labeled))
        released, cleared, kept = [], [], []
        for n in todo:
            if len(released) + len(cleared) >= max_clears:
                kept.append({"issue": n, "reason": "cap"})
                continue
            try:
                issue = self.gh.issue(n)
            except RuntimeError:
                kept.append({"issue": n, "reason": "lookup_failed"})
                continue
            if str(issue.get("state", "OPEN")).upper() != "CLOSED":
                kept.append({"issue": n, "reason": "open"})
                continue
            rec = self.state.get("claims", {}).get(str(n))
            mine = rec is not None and rec.get("state") not in TERMINAL
            if has_label(issue):
                self.gh.remove_label(n, label_name(issue))
            self.gh.comment(n, self._release_body("issue_closed", scope="all"))
            if mine:
                self._record(n, state="released", reason="issue_closed",
                             released_at=iso(self.now()))
                released.append(n)
            else:
                cleared.append(n)
        return {"cycle_id": self.cycle, "released": released,
                "cleared": cleared, "kept": kept}

    # -- stale sweep
    def sweep_stale(self, stale_hours: int, max_clears: int = 40) -> dict:
        self._require_cycle()
        cutoff = self.now() - _dt.timedelta(hours=stale_hours)
        covered = covered_issues(self.gh.open_prs())
        cleared, kept = [], []
        for row in self.gh.labeled_open():
            n = int(row["number"])
            if len(cleared) >= max_clears:
                kept.append({"issue": n, "reason": "cap"})
                continue
            reason = sweep_guard_reason(row, n, covered)
            if reason is not None:
                kept.append({"issue": n, "reason": reason})
                continue
            issue = self.gh.issue(n)
            claims = active_claims(issue)
            if current_cycle_claimed(claims, self.cycle):
                kept.append({"issue": n, "reason": "this_cycle"})
                continue
            last, basis = stale_activity(claims, issue, row)
            if last is None or last > cutoff:
                kept.append({"issue": n, "reason": "fresh"})
                continue
            self._clear_stale(n, issue, stale_hours, basis)
            cleared.append(n)
        return {"cycle_id": self.cycle, "cleared": cleared, "kept": kept}

    def _clear_stale(self, n: int, issue: dict, stale_hours: int, basis: str) -> None:
        """Drop the label and post the stale-release comment for one issue."""
        self.gh.remove_label(n, label_name(issue))
        self.gh.comment(
            n,
            self._release_body(
                "stale_" + basis + "_over_" + str(stale_hours) + "h_no_open_pr", scope="all"
            ),
        )


def work_status(work_dir: Path | None, n: int) -> str:
    if not work_dir:
        return ""
    path = work_dir / ("work-" + str(n) + ".json")
    if not path.exists():
        return ""
    try:
        return str(json.loads(path.read_text(encoding="utf-8")).get("status") or "")
    except (OSError, ValueError):
        return "unreadable"


# ------------------------------------------------------------------- cli


def main(argv: list[str] | None = None, runner: Runner = default_runner) -> int:
    p = argparse.ArgumentParser(prog="night-queue-claim.py", description=__doc__.split("\n")[0])
    p.add_argument("--repo", default=DEFAULT_REPO)
    p.add_argument("--claims", default=DEFAULT_CLAIMS, help="state file (default scratch/claims.json)")
    p.add_argument("--host", default=None)
    sub = p.add_subparsers(dest="cmd", required=True)

    s = sub.add_parser("init")
    s.add_argument("--cycle-id", default=None, help="reuse to resume an interrupted cycle")
    s.add_argument("--work-dir", default="scratch")

    s = sub.add_parser("claim")
    s.add_argument("--queue", help="decision-front.json / triage.json")
    s.add_argument("--issues", help="comma-separated issue numbers")

    s = sub.add_parser("release")
    s.add_argument("--issues", required=True)
    s.add_argument("--reason", required=True)

    s = sub.add_parser("release-unfinished")
    s.add_argument("--work-dir", default="scratch")

    s = sub.add_parser("release-closed")
    s.add_argument("--issues", help="comma-separated issue numbers (extra to the cycle's claims)")
    s.add_argument("--max-clears", type=int, default=40)

    s = sub.add_parser("sweep-stale")
    s.add_argument("--stale-hours", type=int, default=24)
    s.add_argument("--max-clears", type=int, default=40)

    args = p.parse_args(argv)
    c = Claimer(Gh(args.repo, runner), Path(args.claims), host=args.host)
    if args.cmd == "init":
        out = c.init(args.cycle_id, Path(args.work_dir))
    elif args.cmd == "claim":
        nums = (queue_numbers(Path(args.queue)) if args.queue else []) + parse_csv(args.issues)
        out = c.claim(list(dict.fromkeys(nums)))
    elif args.cmd == "release":
        out = c.release(parse_csv(args.issues), args.reason)
    elif args.cmd == "release-unfinished":
        out = c.release_unfinished(Path(args.work_dir))
    elif args.cmd == "release-closed":
        out = c.release_closed(parse_csv(args.issues), args.max_clears)
    else:
        out = c.sweep_stale(max(1, args.stale_hours), args.max_clears)
    print(json.dumps(out, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
