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

Snapshot `rtk gain -f json` (global + optional project) and optional delta
for overnight night-issue-prs token-savings metrics.

Does not merge, does not call GitHub, does not print secrets.
"""
from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


def _repo_root() -> Path:
    return Path(__file__).resolve().parent.parent


def _run_rtk_gain(*, project: bool) -> dict[str, Any]:
    rtk = shutil.which("rtk")
    if not rtk:
        return {"ok": False, "error": "rtk_not_on_path"}
    cmd = [rtk, "gain", "-f", "json"]
    if project:
        cmd.insert(2, "-p")
    try:
        proc = subprocess.run(
            cmd,
            cwd=str(_repo_root()),
            capture_output=True,
            text=True,
            check=False,
            timeout=60,
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        return {"ok": False, "error": str(exc)}
    if proc.returncode != 0:
        err = (proc.stderr or proc.stdout or "").strip()[:500]
        return {"ok": False, "error": err or f"exit_{proc.returncode}"}
    try:
        payload = json.loads(proc.stdout or "{}")
    except json.JSONDecodeError as exc:
        return {"ok": False, "error": f"json: {exc}"}
    payload["ok"] = True
    return payload


def _summary(payload: dict[str, Any]) -> dict[str, Any]:
    s = payload.get("summary")
    if not isinstance(s, dict):
        s = {}
    return {
        "total_commands": int(s.get("total_commands") or 0),
        "total_saved": int(s.get("total_saved") or 0),
        "total_input": int(s.get("total_input") or 0),
        "total_output": int(s.get("total_output") or 0),
        "avg_savings_pct": float(s.get("avg_savings_pct") or 0.0),
        "ok": bool(payload.get("ok")),
        "error": payload.get("error") or "",
    }


def _delta(before: dict[str, Any], after: dict[str, Any]) -> dict[str, Any]:
    b = _summary(before)
    a = _summary(after)
    return {
        "commands": a["total_commands"] - b["total_commands"],
        "tokens_saved": a["total_saved"] - b["total_saved"],
        "input_chars": a["total_input"] - b["total_input"],
        "output_chars": a["total_output"] - b["total_output"],
        "avg_savings_pct_after": a["avg_savings_pct"],
        "ok": True,
        "before_gain_ok": b["ok"],
        "after_gain_ok": a["ok"],
    }


def _load(path: Path) -> dict[str, Any]:
    if not path.is_file():
        return {}
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}
    return data if isinstance(data, dict) else {}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Snapshot rtk gain JSON and optional overnight delta."
    )
    parser.add_argument(
        "--snapshot",
        type=Path,
        required=True,
        help="Write this rtk gain snapshot (JSON).",
    )
    parser.add_argument(
        "--delta-from",
        type=Path,
        default=None,
        help="Previous snapshot; when set, also write --out delta.",
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=None,
        help="Delta JSON path (default: snapshot sibling rtk-metrics.json).",
    )
    parser.add_argument(
        "--no-project",
        action="store_true",
        help="Skip rtk gain -p (cwd project filter).",
    )
    args = parser.parse_args(argv)

    snap_path = args.snapshot
    if not snap_path.is_absolute():
        snap_path = _repo_root() / snap_path
    snap_path.parent.mkdir(parents=True, exist_ok=True)

    global_gain = _run_rtk_gain(project=False)
    project_gain = (
        {"ok": False, "error": "skipped"}
        if args.no_project
        else _run_rtk_gain(project=True)
    )
    snapshot = {
        "captured_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "global": global_gain,
        "project": project_gain,
    }
    snap_path.write_text(
        json.dumps(snapshot, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )

    if args.delta_from is None:
        print(json.dumps({"snapshot": str(snap_path), "delta": None}, indent=2))
        return 0

    before_path = args.delta_from
    if not before_path.is_absolute():
        before_path = _repo_root() / before_path
    before = _load(before_path)
    out_path = args.out
    if out_path is None:
        out_path = snap_path.with_name("rtk-metrics.json")
    if not out_path.is_absolute():
        out_path = _repo_root() / out_path
    out_path.parent.mkdir(parents=True, exist_ok=True)

    metrics = {
        "captured_at": snapshot["captured_at"],
        "before_path": str(before_path),
        "after_path": str(snap_path),
        "global_delta": _delta(before.get("global") or {}, global_gain),
        "project_delta": _delta(before.get("project") or {}, project_gain),
        "note": (
            "tokens_saved is RTK-elided command output (chars/tokens per rtk gain), "
            "not Grok billed tokens. Compare night-issue-prs run tokens_used in "
            "workflow state for billed usage; this file is CLI-output savings."
        ),
    }
    out_path.write_text(
        json.dumps(metrics, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(metrics, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())
