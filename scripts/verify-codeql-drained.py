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
"""CodeQL "drained" gate for night-issue-prs pr-follow-up / cycle-verify.

Purpose
-------
A CodeQL review thread on an owned PR is **not drained** while the CodeQL
check is failing (or still pending) on the PR head SHA, or while an open
code-scanning alert is still located at a file the PR touches. A trailing
or same-line ``// codeql[<rule-id>]`` comment alone never closes an alert
(issue #6784: PR #6770 added one on the spec line, yet alert #2101 stayed
open and the CodeQL check kept failing). Closure requires either

(a) a code rewrite that removes the pattern (the alert auto-resolves as
    ``fixed`` on rescan), or
(b) dismissal of the alert through code scanning with a reason —

and then a re-check that the CodeQL check is green.

This script encodes that verdict so pr-follow-up / cycle-verify agents can
run it before marking a CodeQL thread ``merged_blocker_drained`` instead of
eyeballing readiness. Other (non-CodeQL) CI failures do not affect the
verdict: per the pr-follow-up contract, failing/pending CI alone is not a
merge blocker — only the CodeQL surface gates a CodeQL thread.

Usage
-----
::

    python3 scripts/verify-codeql-drained.py --pr-number 6770
    python3 scripts/verify-codeql-drained.py --pr-number 6770 --repo OWNER/REPO --json

Exit codes
----------
- ``0`` drained (CodeQL checks green on the head SHA, no open alerts at PR files)
- ``1`` not drained (reason printed; re-check after fix/dismiss + rescan)
- ``2`` prerequisite failure (``gh`` CLI missing/unauthenticated, PR not
  found, API error) — transient, not a verdict

Behavioral Notes
----------------
- All GitHub access goes through the ``gh`` CLI with list-form ``subprocess``
  calls (no shell), so the script is cross-platform (Windows: run with
  ``python scripts\\verify-codeql-drained.py``).
- H2-independent: no CMS checkout build, no QA cell, no Playwright.
- Paginated ``gh api`` output is normalized in Python (``--paginate`` merges
  array pages into one list, but object pages stay a list of responses).
"""
from __future__ import annotations

import argparse
import json
import logging
import shutil
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

DEFAULT_CODEQL_MATCHERS = ("codeql", "analyze (")

# Check-run conclusions that mean the CodeQL surface failed on the head SHA.
FAILING_CONCLUSIONS = frozenset({"failure", "timed_out", "action_required", "cancelled"})

# Conclusions that mean the run does not block a drained verdict
# (not applicable to this PR's language set, or informational).
OK_CONCLUSIONS = frozenset({"success", "skipped", "neutral"})


class GhError(RuntimeError):
    """Raised when a ``gh`` invocation fails or returns unusable output."""


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify_codeql_drained.py",
        description=(
            "Decide whether the CodeQL surface on a PR is drained: "
            "CodeQL checks green on the head SHA and no open alerts at PR files."
        ),
    )
    parser.add_argument(
        "--pr-number",
        required=True,
        type=int,
        help="PR number to check (the CodeQL thread lives on this PR).",
    )
    parser.add_argument(
        "--repo",
        default="intersoftdatalabs-in/percussioncms",
        help="owner/repo for gh CLI (default: intersoftdatalabs-in/percussioncms).",
    )
    parser.add_argument(
        "--codeql-check",
        dest="codeql_checks",
        action="append",
        default=None,
        help=(
            "Case-insensitive substring matching a CodeQL check-run name "
            "(repeatable; default: 'codeql' and 'analyze (' match "
            "'CodeQL' / 'Analyze (javascript-typescript)' / 'Analyze (java-kotlin)')."
        ),
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Emit only a machine-readable verdict object on stdout.",
    )
    return parser


def _run_gh_json(args: list[str]) -> object:
    """Run ``gh <args>`` and return the parsed JSON payload."""
    result = subprocess.run(
        ["gh", *args],
        shell=False,
        check=False,
        timeout=120,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise GhError(
            f"gh {' '.join(args[:4])}… failed (rc={result.returncode}): "
            f"{(result.stderr or '').strip()[:500]}"
        )
    try:
        return json.loads(result.stdout or "null")
    except json.JSONDecodeError as exc:
        raise GhError(f"gh returned invalid JSON: {exc}") from exc


def pr_head_sha(repo: str, pr: int) -> str:
    """Return the head SHA of a PR (raises GhError when not found)."""
    payload = _run_gh_json(
        ["pr", "view", str(pr), "--repo", repo, "--json", "headRefOid"]
    )
    sha = (payload.get("headRefOid", "") or "") if isinstance(payload, dict) else ""
    if not sha:
        raise GhError(f"PR #{pr} has no head SHA (deleted or not found?)")
    return str(sha)


def pr_files(repo: str, pr: int) -> list[str]:
    """Return the changed-file paths of a PR (raises GhError when not found)."""
    payload = _run_gh_json(
        ["pr", "view", str(pr), "--repo", repo, "--json", "files"]
    )
    if not isinstance(payload, dict) or not isinstance(payload.get("files"), list):
        raise GhError(f"PR #{pr} returned an unexpected files payload")
    return [str(entry.get("path", "")) for entry in payload["files"]
            if isinstance(entry, dict) and entry.get("path")]


def check_runs_for_sha(repo: str, sha: str) -> list[dict]:
    """Return normalized check-run dicts for a commit SHA (name/status/conclusion)."""
    payload = _run_gh_json(
        ["api", f"repos/{repo}/commits/{sha}/check-runs", "--paginate"]
    )
    runs: list[dict] = []
    pages = payload if isinstance(payload, list) else [payload]
    for page in pages:
        if isinstance(page, dict):
            for run in page.get("check_runs", []) or []:
                if isinstance(run, dict):
                    runs.append(run)
    return runs


def _alert_is_open(item: dict) -> bool:
    """True when a code-scanning alert still needs action.

    Prefers the top-level ``state`` (``open`` / ``fixed`` / ``dismissed``).
    Alerts raised only by a PR analysis (``refs/pull/<N>/merge``) can carry
    a null top-level state — e.g. alert #2101 on PR #6770 — so fall back to
    ``most_recent_instance.state`` in that case.
    """
    state = item.get("state")
    if state == "open":
        return True
    if state in ("fixed", "dismissed"):
        return False
    instance = item.get("most_recent_instance") or {}
    return instance.get("state") == "open"


def open_alerts(repo: str, pr: int) -> list[dict]:
    """Return normalized open code-scanning alerts (number/rule/path).

    Scoped to the PR ref (``refs/pull/<N>/merge``): without ``ref`` the
    endpoint returns default-branch alerts only, so PR-introduced alerts
    (e.g. #2101 on PR #6770) are invisible to the gate while pre-existing
    main alerts at touched files read as false NOT DRAINED. Filters
    client-side on ``state`` instead of the ``?state=open`` query
    parameter, which this repo's API surface rejects (HTTP 404) for some
    token scopes; the verdict is identical and deterministic.
    """
    payload = _run_gh_json(
        ["api", f"repos/{repo}/code-scanning/alerts?ref=refs/pull/{pr}/merge",
         "--paginate"]
    )
    items = payload if isinstance(payload, list) else []
    alerts: list[dict] = []
    for item in items:
        if not isinstance(item, dict) or not _alert_is_open(item):
            continue
        location = ((item.get("most_recent_instance") or {}).get("location") or {})
        alerts.append(
            {
                "number": item.get("number"),
                "rule": (item.get("rule") or {}).get("id", ""),
                "path": location.get("path", ""),
            }
        )
    return alerts


def is_codeql_check(name: str, matchers: tuple[str, ...]) -> bool:
    """True when a check-run name belongs to the CodeQL surface."""
    lowered = (name or "").lower()
    return any(m.lower() in lowered for m in matchers)


def evaluate(
    files: list[str],
    check_runs: list[dict],
    alerts: list[dict],
    matchers: tuple[str, ...] = DEFAULT_CODEQL_MATCHERS,
) -> dict:
    """Compute the drained verdict from already-fetched data.

    Returns ``{"drained": bool, "reasons": [...], "codeql_checks": [...],
    "open_alerts_at_pr_files": [...]}``. Pure function — no I/O, unit-tested.
    """
    touched = set(files)
    codeql_runs = [
        {
            "name": str(run.get("name", "")),
            "status": str(run.get("status", "")),
            "conclusion": run.get("conclusion"),
        }
        for run in check_runs
        if is_codeql_check(str(run.get("name", "")), matchers)
    ]
    reasons: list[str] = []
    for run in codeql_runs:
        conclusion = run["conclusion"]
        if run["status"] != "completed" or conclusion is None:
            reasons.append(
                f"CodeQL check {run['name']!r} is not green yet "
                f"(status={run['status']!r}) — re-check after the run completes"
            )
        elif conclusion in FAILING_CONCLUSIONS:
            reasons.append(
                f"CodeQL check {run['name']!r} concluded {conclusion!r} "
                "on the PR head SHA"
            )
        elif conclusion not in OK_CONCLUSIONS:
            reasons.append(
                f"CodeQL check {run['name']!r} has unexpected conclusion "
                f"{conclusion!r} on the PR head SHA"
            )
    # Alerts are already PR-ref scoped by open_alerts; the touched-file
    # match keeps the verdict scoped to this PR's files.
    at_files = [
        alert
        for alert in alerts
        if str(alert.get("path", "")) in touched
    ]
    for alert in at_files:
        reasons.append(
            f"CodeQL alert #{alert.get('number')} "
            f"({alert.get('rule')}) still open at {alert.get('path')} — "
            "rewrite the pattern or dismiss via code scanning, then re-check green"
        )
    return {
        "drained": not reasons,
        "reasons": reasons,
        "codeql_checks": codeql_runs,
        "open_alerts_at_pr_files": at_files,
    }


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    if shutil.which("gh") is None:
        print("FAIL: gh CLI not found", file=sys.stderr)
        return 2

    matchers = tuple(args.codeql_checks or list(DEFAULT_CODEQL_MATCHERS))
    try:
        sha = pr_head_sha(args.repo, args.pr_number)
        files = pr_files(args.repo, args.pr_number)
        runs = check_runs_for_sha(args.repo, sha)
        alerts = open_alerts(args.repo, args.pr_number)
    except GhError as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 2

    verdict = evaluate(files, runs, alerts, matchers)
    verdict_payload = {"pr": args.pr_number, "head_sha": sha, **verdict}
    if args.json:
        print(json.dumps(verdict_payload, indent=2))
    elif verdict["drained"]:
        print(
            f"verify-codeql-drained: DRAINED — PR #{args.pr_number} "
            f"({len(verdict['codeql_checks'])} CodeQL check(s) green, "
            "no open alerts at PR files)"
        )
    else:
        print(
            f"verify-codeql-drained: NOT DRAINED — PR #{args.pr_number}",
            file=sys.stderr,
        )
        for reason in verdict["reasons"]:
            print(f"  FAIL: {reason}", file=sys.stderr)
    return 0 if verdict["drained"] else 1


if __name__ == "__main__":
    sys.exit(main())
