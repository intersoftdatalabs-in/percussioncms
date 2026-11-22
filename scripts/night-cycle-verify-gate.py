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
"""Night cycle-verify turn-closure gate (issue #7019).

Background: cycle ``stonemonkey-20261121T235545Z`` stalled because the
``cycle-verify`` sub-agent ended its turn while Maven builds were still
running in the background (``nohup ... &`` + "resume on the background
completion notice"). Sub-agent sessions get no such notice, so the parent
went idle: no fresh ``scratch/cycle-verify.json``, no Report, stale queue
claims. The ``.opencode/agents/cycle-verify.md`` §8 "Complete the turn"
rule existed but nothing enforced it.

This script is the enforceable half of that rule (pure Python, no
platform-specific code, so no ``.cmd`` counterpart is needed):

* ``check`` — run by the night-worker host right after the ``cycle-verify``
  task returns. Requires BOTH the ``CYCLE_VERIFY_DONE ...`` stdout line
  (proof the turn finished its gates in-turn) AND a fresh
  ``scratch/cycle-verify.json`` whose ``cycle_id`` equals the current cycle
  and whose mtime is later than the dispatch instant (stale files from
  earlier cycles are rejected). Non-zero exit = re-dispatch once with an
  explicit "previous attempt returned while builds were still running"
  note; still failing = record ``cycle_verify: incomplete`` and run
  Report anyway instead of going idle.
* ``wait`` — foreground blocking wait on a sentinel file, for use INSIDE
  the ``cycle-verify`` turn instead of detaching (``&``, ``nohup``,
  ``setsid``). The long chain ends with ``touch <sentinel>`` (or the
  chain step that produces the deliverable creates it); the agent blocks
  on this command with a long tool timeout instead of ending its turn.
  Timeout expiry is concrete blocked evidence, not a silent stall.

Usage (from the night worktree)::

    python3 scripts/night-cycle-verify-gate.py check \\
        --work-dir "$NIGHT_WORKTREE_PATH" --cycle-id "$NIGHT_CYCLE_ID" \\
        --dispatched-at 2026-11-22T03:10:20Z \\
        --output-file /tmp/cv-task-stdout.txt

    python3 scripts/night-cycle-verify-gate.py wait \\
        --sentinel scratch/cv-maven-chain.done --timeout 7200 --poll 15

Tests: ``python3 -m pytest scripts/test_night_cycle_verify_gate.py -v``
"""
from __future__ import annotations

import argparse
import datetime as _dt
import json
import os
import re
import sys
import time
from pathlib import Path

DONE_RE = re.compile(
    r"^CYCLE_VERIFY_DONE\s+tip=\S+\s+modules=\S*\s+status=(passed|failed|failures_filed|partial|skipped)\b",
    re.MULTILINE,
)

# Exit codes: 0 pass, 1 gate failure (actionable), 2 usage error.
EXIT_PASS = 0
EXIT_FAIL = 1
EXIT_USAGE = 2


def parse_instant(text: str) -> _dt.datetime:
    """Parse an ISO-8601 instant; trailing ``Z`` means UTC (GH style)."""
    candidate = text.strip()
    if candidate.endswith("Z"):
        candidate = candidate[:-1] + "+00:00"
    parsed = _dt.datetime.fromisoformat(candidate)
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=_dt.timezone.utc)
    return parsed


def check_turn(
    output_text: str | None,
    json_path: Path,
    cycle_id: str,
    dispatched_at: _dt.datetime,
) -> dict:
    """Validate one cycle-verify turn. Returns a verdict dict.

    ``ok`` is True only when the DONE line is present AND the JSON file
    exists, parses, stamps the current ``cycle_id``, and is newer than
    the dispatch instant. ``reason`` names the first failing check so the
    host can re-dispatch with a precise note.
    """
    if not output_text or not DONE_RE.search(output_text):
        return {
            "ok": False,
            "reason": "missing_done_line",
            "detail": (
                "cycle-verify task output has no CYCLE_VERIFY_DONE line; "
                "the turn ended without finishing its gates in-turn "
                "(likely detached builds with &, nohup, or setsid)."
            ),
        }
    if not json_path.is_file():
        return {
            "ok": False,
            "reason": "missing_json",
            "detail": f"{json_path} was not written by this turn.",
        }
    try:
        payload = json.loads(json_path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        return {
            "ok": False,
            "reason": "bad_json",
            "detail": f"{json_path} is unreadable: {exc}.",
        }
    if not isinstance(payload, dict) or not payload.get("cycle_id"):
        return {
            "ok": False,
            "reason": "missing_cycle_id",
            "detail": (
                f"{json_path} stamps no cycle_id; a file from an earlier "
                "cycle cannot be told apart from a fresh result."
            ),
        }
    if str(payload["cycle_id"]) != cycle_id:
        return {
            "ok": False,
            "reason": "stale_cycle_id",
            "detail": (
                f"{json_path} stamps cycle_id={payload['cycle_id']!r}, "
                f"expected {cycle_id!r}: leftover from a previous cycle."
            ),
        }
    mtime = _dt.datetime.fromtimestamp(
        json_path.stat().st_mtime, tz=_dt.timezone.utc
    )
    if mtime <= dispatched_at:
        return {
            "ok": False,
            "reason": "stale_mtime",
            "detail": (
                f"{json_path} mtime {mtime.isoformat()} is not later than "
                f"dispatch {dispatched_at.isoformat()}: predates this turn."
            ),
        }
    return {
        "ok": True,
        "reason": "",
        "detail": (
            f"CYCLE_VERIFY_DONE present; {json_path.name} stamps "
            f"cycle_id={cycle_id} with mtime {mtime.isoformat()}."
        ),
        "status": str(payload.get("status", "")),
    }


def wait_for_sentinel(sentinel: Path, timeout: float, poll: float) -> dict:
    """Block (foreground) until ``sentinel`` exists. Never detach."""
    if timeout <= 0:
        return {"ok": False, "reason": "bad_timeout",
                "detail": "timeout must be > 0 seconds."}
    if poll <= 0:
        return {"ok": False, "reason": "bad_poll",
                "detail": "poll interval must be > 0 seconds."}
    deadline = time.monotonic() + timeout
    start = time.monotonic()
    while True:
        if sentinel.exists():
            elapsed = time.monotonic() - start
            return {"ok": True, "reason": "",
                    "detail": f"sentinel {sentinel} appeared.",
                    "elapsed": round(elapsed, 1)}
        if time.monotonic() >= deadline:
            return {
                "ok": False,
                "reason": "timeout",
                "detail": (
                    f"sentinel {sentinel} did not appear within "
                    f"{timeout:g}s; treat the chained step as blocked "
                    "and record evidence instead of detaching."
                ),
                "elapsed": round(time.monotonic() - start, 1),
            }
        time.sleep(min(poll, max(0.0, deadline - time.monotonic())))


def _read_output_text(args: argparse.Namespace) -> str | None:
    if args.output_file:
        try:
            return Path(args.output_file).read_text(encoding="utf-8",
                                                   errors="replace")
        except OSError as exc:
            print(f"CYCLE_VERIFY_GATE_FAIL reason=unreadable_output "
                  f"detail={exc}", flush=True)
            raise SystemExit(EXIT_USAGE)
    return args.output_text


def cmd_check(args: argparse.Namespace) -> int:
    output_text = _read_output_text(args)
    if output_text is None:
        print("CYCLE_VERIFY_GATE_FAIL reason=no_output_provided detail="
              "pass --output-file or --output-text with the cycle-verify "
              "task stdout so the DONE line can be required.",
              flush=True)
        return EXIT_USAGE
    try:
        dispatched_at = parse_instant(args.dispatched_at)
    except ValueError as exc:
        print(f"CYCLE_VERIFY_GATE_FAIL reason=bad_dispatched_at "
              f"detail={exc}", flush=True)
        return EXIT_USAGE
    json_path = Path(args.work_dir) / "scratch" / args.json_name
    verdict = check_turn(output_text, json_path, args.cycle_id,
                         dispatched_at)
    if args.verdict_out:
        try:
            Path(args.verdict_out).write_text(
                json.dumps({"cycle_id": args.cycle_id, **verdict},
                           indent=1) + os.linesep,
                encoding="utf-8",
            )
        except OSError as exc:
            print(f"CYCLE_VERIFY_GATE_FAIL reason=unwritable_verdict "
                  f"detail={exc}", flush=True)
            return EXIT_USAGE
    if verdict["ok"]:
        print(f"CYCLE_VERIFY_GATE_PASS cycle={args.cycle_id} "
              f"{verdict['detail']}", flush=True)
        return EXIT_PASS
    print(f"CYCLE_VERIFY_GATE_FAIL reason={verdict['reason']} "
          f"detail={verdict['detail']}", flush=True)
    return EXIT_FAIL


def cmd_wait(args: argparse.Namespace) -> int:
    verdict = wait_for_sentinel(Path(args.sentinel), args.timeout,
                                args.poll)
    if verdict["ok"]:
        print(f"WAIT_DONE sentinel={args.sentinel} "
              f"elapsed={verdict['elapsed']}s", flush=True)
        return EXIT_PASS
    print(f"WAIT_{verdict['reason'].upper()} sentinel={args.sentinel} "
          f"detail={verdict['detail']}", flush=True)
    return EXIT_FAIL


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Night cycle-verify turn-closure gate (#7019).")
    sub = parser.add_subparsers(dest="command", required=True)

    check = sub.add_parser(
        "check",
        help="Host gate: DONE line + fresh cycle-stamped JSON required.")
    check.add_argument("--work-dir", required=True,
                       help="Night worktree (reads scratch/<json-name>).")
    check.add_argument("--cycle-id", required=True,
                       help="Current cycle id the JSON must stamp.")
    check.add_argument("--dispatched-at", required=True,
                       help="Cycle-verify dispatch instant (ISO-8601); "
                            "the JSON mtime must be later.")
    check.add_argument("--output-file", default="",
                       help="File holding the cycle-verify task stdout.")
    check.add_argument("--output-text", default=None,
                       help="Task stdout inline (alternative).")
    check.add_argument("--json-name", default="cycle-verify.json",
                       help="Basename under scratch/ (default: "
                            "cycle-verify.json).")
    check.add_argument("--verdict-out", default="",
                       help="Optional path for a JSON verdict copy.")
    check.set_defaults(func=cmd_check)

    wait = sub.add_parser(
        "wait",
        help="Foreground sentinel wait for use inside the turn.")
    wait.add_argument("--sentinel", required=True,
                      help="File whose appearance ends the wait.")
    wait.add_argument("--timeout", type=float, default=7200.0,
                      help="Give up after this many seconds (default 7200).")
    wait.add_argument("--poll", type=float, default=15.0,
                      help="Re-check interval in seconds (default 15).")
    wait.set_defaults(func=cmd_wait)
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    return int(args.func(args))


if __name__ == "__main__":
    sys.exit(main())
