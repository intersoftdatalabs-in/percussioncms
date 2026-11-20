#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

See the License for the specific language governing permissions and
limitations under the License.

Wall-clock phase timer for the night-issue-prs workflow (issue #6785).

``scratch/night-report.md`` has no per-phase timings, so regressions against
the 45m-2h cycle target are invisible. This helper records phase start/end
timestamps to a small JSON state file and renders a Markdown table the host
pastes (or appends) into the report.

## Usage

    # Start a phase (state defaults to $NIGHT_TIMINGS_PATH, else ./phase-timings.json)
    python3 scripts/night-phase-timer.py start Work \\
        --state "$(dirname "$NIGHT_REPORT_PATH")/phase-timings.json"

    # End it
    python3 scripts/night-phase-timer.py end Work \\
        --state "$(dirname "$NIGHT_REPORT_PATH")/phase-timings.json"

    # Render the Markdown table for night-report.md
    python3 scripts/night-phase-timer.py report \\
        --state "$(dirname "$NIGHT_REPORT_PATH")/phase-timings.json"

A repeated ``start`` for the same phase resets its start (retry-safe); an
``end`` with no matching ``start`` exits 2. Phases still running at
``report`` time render ``(running)`` with the elapsed time so far.

## Conventions

Cross-platform Python 3.9+ stdlib only (no third-party imports), pathlib
paths, no hardcoded separators. State is plain JSON so any host (rhai,
opencode prompt, shell) can read it without this script.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List

STATE_ENV = "NIGHT_TIMINGS_PATH"
DEFAULT_STATE_NAME = "phase-timings.json"
STATE_VERSION = 1


def utc_now_iso() -> str:
    """Current UTC time as ``YYYY-MM-DDTHH:MM:SSZ``."""
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def parse_iso(raw: str) -> datetime:
    """Parse an ISO-8601 timestamp; accepts a trailing ``Z`` (py3.9-safe)."""
    text = (raw or "").strip()
    if text.endswith("Z"):
        text = text[:-1] + "+00:00"
    parsed = datetime.fromisoformat(text)
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return parsed.astimezone(timezone.utc)


def default_state_path() -> Path:
    """State path: ``$NIGHT_TIMINGS_PATH`` else ``./phase-timings.json``."""
    env = (os.environ.get(STATE_ENV, "") or "").strip()
    if env:
        return Path(env).expanduser()
    return Path.cwd() / DEFAULT_STATE_NAME


def load_state(path: Path) -> Dict[str, Any]:
    """Load timer state; missing/corrupt files start empty (never crash)."""
    if not path.is_file():
        return {"version": STATE_VERSION, "phases": {}, "order": []}
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {"version": STATE_VERSION, "phases": {}, "order": []}
    if not isinstance(data, dict):
        return {"version": STATE_VERSION, "phases": {}, "order": []}
    phases = data.get("phases")
    if not isinstance(phases, dict):
        phases = {}
    order = data.get("order")
    if not isinstance(order, list):
        order = [name for name in phases if isinstance(name, str)]
    return {"version": STATE_VERSION, "phases": phases, "order": order}


def save_state(path: Path, state: Dict[str, Any]) -> None:
    """Write timer state (creates parent dirs)."""
    if path.parent and str(path.parent) not in ("", "."):
        path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(state, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def record_start(state: Dict[str, Any], phase: str, at: str) -> Dict[str, Any]:
    """Record (or reset) the start of ``phase``."""
    phases = state["phases"]
    entry = phases.get(phase)
    if not isinstance(entry, dict):
        entry = {}
    entry["start"] = at
    entry.pop("end", None)
    entry.pop("elapsed_sec", None)
    phases[phase] = entry
    order: List[str] = state["order"]
    if phase not in order:
        order.append(phase)
    return state


def record_end(state: Dict[str, Any], phase: str, at: str) -> Dict[str, Any]:
    """Record the end of ``phase``. Raises ``KeyError`` with no start."""
    phases = state["phases"]
    entry = phases.get(phase)
    if not isinstance(entry, dict) or not entry.get("start"):
        raise KeyError(f"phase {phase!r} has no recorded start")
    start = parse_iso(str(entry["start"]))
    end = parse_iso(at)
    entry["end"] = at
    entry["elapsed_sec"] = max(0.0, (end - start).total_seconds())
    return state


def format_elapsed(seconds: float) -> str:
    """Format seconds as ``H:MM:SS`` (hours may exceed 24)."""
    total = max(0, int(seconds))
    hours, remainder = divmod(total, 3600)
    minutes, secs = divmod(remainder, 60)
    return f"{hours}:{minutes:02d}:{secs:02d}"


def render_markdown(state: Dict[str, Any], now_iso: str) -> str:
    """Render the timings table for ``night-report.md``."""
    lines = ["## Phase timings (wall-clock, UTC)", ""]
    lines.append("| Phase | Started (UTC) | Ended (UTC) | Elapsed |")
    lines.append("| --- | --- | --- | --- |")
    total_sec = 0.0
    now = parse_iso(now_iso)
    for phase in state.get("order", []):
        entry = state["phases"].get(phase, {})
        start_raw = str(entry.get("start") or "—")
        end_raw = entry.get("end")
        elapsed = entry.get("elapsed_sec")
        if end_raw and isinstance(elapsed, (int, float)):
            total_sec += float(elapsed)
            lines.append(
                f"| {phase} | {start_raw} | {end_raw} | {format_elapsed(float(elapsed))} |"
            )
        else:
            running_sec = 0.0
            if entry.get("start"):
                running_sec = max(0.0, (now - parse_iso(start_raw)).total_seconds())
            lines.append(
                f"| {phase} | {start_raw} | (running) | {format_elapsed(running_sec)} so far |"
            )
            total_sec += running_sec
    lines.append("")
    lines.append(f"Total wall-clock: {format_elapsed(total_sec)}")
    lines.append("")
    return "\n".join(lines)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="night-phase-timer",
        description="Record wall-clock phase timings for night-issue-prs runs.",
    )
    parser.add_argument(
        "--state",
        default="",
        help=f"Timer state file (default: ${STATE_ENV} else ./phase-timings.json).",
    )
    sub = parser.add_subparsers(dest="command", required=True)

    for name in ("start", "end"):
        cmd = sub.add_parser(name, help=f"Record {name} of a phase.")
        cmd.add_argument(
            "--state",
            default="",
            help=f"Timer state file (default: ${STATE_ENV} else ./phase-timings.json).",
        )
        cmd.add_argument("phase", help="Phase name (e.g. Work, Triage, Report).")
        cmd.add_argument(
            "--at",
            default="",
            help="Timestamp override (ISO-8601, default: now UTC). Mostly for tests.",
        )

    rep = sub.add_parser("report", help="Render the Markdown timings table.")
    rep.add_argument(
        "--state",
        default="",
        help=f"Timer state file (default: ${STATE_ENV} else ./phase-timings.json).",
    )
    rep.add_argument(
        "--format",
        choices=("markdown", "text"),
        default="markdown",
        help="Output format (default: markdown).",
    )
    return parser


def resolve_state_path(args: argparse.Namespace) -> Path:
    raw = (args.state or "").strip()
    return Path(raw).expanduser() if raw else default_state_path()


def main(argv: List[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    state_path = resolve_state_path(args)
    state = load_state(state_path)

    if args.command in ("start", "end"):
        at = (args.at or "").strip() or utc_now_iso()
        try:
            parse_iso(at)
        except ValueError:
            sys.stderr.write(f"night-phase-timer: invalid --at timestamp: {at!r}\n")
            return 2
        try:
            if args.command == "start":
                record_start(state, args.phase, at)
            else:
                record_end(state, args.phase, at)
        except KeyError as exc:
            sys.stderr.write(f"night-phase-timer: {exc}\n")
            return 2
        save_state(state_path, state)
        return 0

    now_iso = utc_now_iso()
    if args.format == "text":
        for phase in state.get("order", []):
            entry = state["phases"].get(phase, {})
            elapsed = entry.get("elapsed_sec")
            status = format_elapsed(float(elapsed)) if isinstance(elapsed, (int, float)) else "running"
            sys.stdout.write(f"{phase}: {status}\n")
        return 0
    sys.stdout.write(render_markdown(state, now_iso))
    return 0


if __name__ == "__main__":
    sys.exit(main())
