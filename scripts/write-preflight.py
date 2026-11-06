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
"""Build a canonical `scratch/preflight.json` from files (no live gh).

Used by the `night-issue-prs` host when the `preflight` sub-agent is skipped
(decision-front `signals_complete`). The host must NOT hand-roll
`preflight.json` with `jq --arg` embedding issue bodies: large backlogs
exceed `ARG_MAX` (`Argument list too long`) and truncate the file to 0
bytes, blocking every downstream phase.

## Behavioral Notes

- Cross-platform Python 3.9+, stdlib only, `pathlib` paths, no `shell`.
- Offline: reads `scratch/issues-raw.json` (+ optional `prescreen.json`,
  `decision-front.json`, `alerts.json`, `maintainers.txt`); never calls `gh`.
- Fail-open: every optional input may be missing. A missing alert feed omits
  `open_alert_count` (unknown, NOT zero) per the preflight hard rules.
- `issues_compact` is an ARRAY of flag objects per `.opencode/agents/
  preflight.md` Phase 2G — never a flat string. `reconcile` reads this shape.
- Body-based flags cannot be evaluated offline: `Destructive` is always
  `false` with a note, so Triage must still apply its safety matrix.
  `LargeI18n` is a conservative label/title heuristic (see `_is_large_i18n`),
  documented as an approximation of the Preflight inventory-flags table.
- Stale `In Progress` cleanup is NOT performed here; counts are carried over
  from `decision-front.json` (`issues_stale` / `issues_cleared`) when present.

## Usage

    python3 scripts/write-preflight.py \
      --inventory scratch/issues-raw.json \
      --prescreen scratch/prescreen.json \
      --decision-front scratch/decision-front.json \
      --alerts scratch/alerts.json \
      --maintainers scratch/maintainers.txt \
      --repo intersoftdatalabs-in/percussioncms \
      --out scratch/preflight.json
"""
from __future__ import annotations

import argparse
import json
import logging
import sys
from pathlib import Path

LOGGER = logging.getLogger(__name__)

NOT_SAFE_LABEL = "not safe for agents"
IN_PROGRESS_LABELS = frozenset({"in progress", "in-progress"})
I18N_HINTS = ("tmx", "locale", "locales", "translation", "translat", "i18n")


def _labels(issue: dict) -> list[str]:
    labels = issue.get("labels") or []
    names: list[str] = []
    for label in labels:
        if isinstance(label, str):
            names.append(label)
        elif isinstance(label, dict) and label.get("name"):
            names.append(str(label["name"]))
    return names


def _norm(label: str) -> str:
    return label.strip().casefold()


def _is_large_i18n(title: str, labels: list[str]) -> bool:
    """Conservative approximation of the Preflight LargeI18n flag.

    True only when an i18n-ish label is present AND the title mentions a
    bulk-matrix hint (TMX / locale / translation). The full preflight
    sub-agent applies the authoritative table when it runs.
    """
    lowered = f"{title}".casefold()
    has_i18n_label = any("i18n" in _norm(label) for label in labels)
    has_bulk_hint = any(hint in lowered for hint in I18N_HINTS)
    return bool(has_i18n_label and has_bulk_hint)


def _load_json(path: str | None) -> object | None:
    if not path:
        return None
    file = Path(path)
    if not file.is_file():
        LOGGER.warning("optional input missing: %s", path)
        return None
    try:
        return json.loads(file.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError) as exc:
        LOGGER.warning("unreadable input %s: %s", path, exc)
        return None


def _load_maintainers(path: str | None, extra: str) -> list[str]:
    logins: list[str] = []
    if path:
        file = Path(path)
        if file.is_file():
            try:
                logins.extend(
                    line.strip()
                    for line in file.read_text(encoding="utf-8").splitlines()
                    if line.strip()
                )
            except OSError as exc:
                LOGGER.warning("unreadable maintainers file %s: %s", path, exc)
        else:
            LOGGER.warning("optional input missing: %s", path)
    if extra:
        logins.extend(item.strip() for item in extra.split(",") if item.strip())
    seen: set[str] = set()
    unique: list[str] = []
    for login in logins:
        key = login.casefold()
        if key not in seen:
            seen.add(key)
            unique.append(login)
    return unique


def _prescreen_by_number(prescreen: object | None) -> dict[int, dict]:
    if not isinstance(prescreen, dict):
        return {}
    issues = prescreen.get("issues")
    if not isinstance(issues, list):
        return {}
    table: dict[int, dict] = {}
    for entry in issues:
        if isinstance(entry, dict) and isinstance(entry.get("number"), int):
            table[entry["number"]] = entry
    return table


def _as_int(value: object, default: int = 0) -> int:
    try:
        return int(value)  # type: ignore[arg-type]
    except (TypeError, ValueError):
        return default


def _stale_cleanup(signals: dict) -> dict:
    cleared: list = signals.get("issues_cleared_list") or []
    if not isinstance(cleared, list):
        cleared = []
    kept: list = []
    if isinstance(signals.get("issues_kept"), str) and signals["issues_kept"]:
        kept = [signals["issues_kept"]]
    return {
        "enabled": True,
        "cleared": cleared,
        "kept": kept,
        "capped": False,
        "note": "stale cleanup not performed in the decision-front path; "
        "counts carried from decision-front.json when present",
    }


def _alert_count(alerts: object | None, signals: dict) -> int | None:
    if isinstance(alerts, list):
        return len(alerts)
    if isinstance(alerts, dict) and isinstance(alerts.get("open_alerts"), list):
        return len(alerts["open_alerts"])
    if isinstance(signals.get("open_alert_count"), int):
        return signals["open_alert_count"]
    return None


def _author_login(issue: dict) -> str:
    author = issue.get("author")
    if isinstance(author, dict):
        return str(author.get("login", ""))
    if isinstance(author, str):
        return author
    return ""


def _compact_row(
    issue: dict,
    prescreen_table: dict[int, dict],
    maintainer_keys: set[str],
    agent_safe_only: bool,
) -> dict | None:
    if not isinstance(issue, dict) or not isinstance(issue.get("number"), int):
        return None
    labels = _labels(issue)
    normed = {_norm(label) for label in labels}
    rule_kind = prescreen_table.get(issue["number"], {}).get("rule_kind")
    not_safe = NOT_SAFE_LABEL in normed or rule_kind == "not_safe_agents"
    large_i18n = _is_large_i18n(str(issue.get("title", "")), labels)
    if agent_safe_only and large_i18n:
        not_safe = True
    author_login = _author_login(issue)
    return {
        "number": issue["number"],
        "title": str(issue.get("title", "")),
        "labels": labels,
        "assignees": issue.get("assignees") or [],
        "author": author_login,
        "NotSafe": bool(not_safe),
        "Destructive": False,
        "InProgress": bool(normed & IN_PROGRESS_LABELS),
        "Maintainer": author_login.casefold() in maintainer_keys,
        "LargeI18n": bool(large_i18n),
    }


def build_preflight(
    inventory: list,
    prescreen: object | None,
    decision_front: object | None,
    alerts: object | None,
    maintainers: list[str],
    repo: str,
    base_branch: str,
    agent_safe_only: bool,
) -> dict:
    """Assemble the canonical Phase 2G preflight document."""
    prescreen_table = _prescreen_by_number(prescreen)
    maintainer_keys = {login.casefold() for login in maintainers}

    compact: list[dict] = []
    for issue in inventory:
        row = _compact_row(issue, prescreen_table, maintainer_keys, agent_safe_only)
        if row is not None:
            compact.append(row)

    signals: dict = decision_front if isinstance(decision_front, dict) else {}

    owned_count = _as_int(signals.get("owned_pr_count", 0))
    peer_count = _as_int(signals.get("peer_eligible_count", 0))
    alert_count = _alert_count(alerts, signals)
    cycle_residuals = _as_int(signals.get("cycle_verify_residual_count", 0))
    qa_failed = _as_int(signals.get("qa_failed_count", 0))

    document: dict = {
        "phase": "preflight",
        "executor": "script:write-preflight",
        "repo": repo,
        "base_branch": base_branch,
        "stale_in_progress_cleanup": _stale_cleanup(signals),
        "owned_prs": [],
        "peer_pr_eligible": [],
        "issues_compact": compact,
        "maintainer_logins": maintainers,
        "stats": {
            "issues_compact": len(compact),
            "owned_prs": owned_count,
            "peer_pr_eligible": peer_count,
            "codeql_alerts": alert_count if alert_count is not None else -1,
        },
        "notes": "Destructive is always false offline (no bodies fetched); "
        "Triage must still apply its safety matrix. LargeI18n is a "
        "label/title heuristic; the full preflight sub-agent table applies "
        "when that phase runs.",
    }
    if isinstance(signals.get("cluster_recommended"), bool):
        document["cluster_recommended"] = signals["cluster_recommended"]
    document["cycle_verify_residual_count"] = cycle_residuals
    document["qa_failed_count"] = qa_failed
    if alert_count is not None:
        document["open_alert_count"] = alert_count
    return document


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="write-preflight",
        description="Build canonical scratch/preflight.json from files.",
    )
    parser.add_argument("--inventory", required=True, help="issues-raw.json path.")
    parser.add_argument("--prescreen", default="", help="prescreen.json path.")
    parser.add_argument(
        "--decision-front", default="", help="decision-front.json path."
    )
    parser.add_argument("--alerts", default="", help="alerts.json path.")
    parser.add_argument("--maintainers", default="", help="maintainers.txt path.")
    parser.add_argument(
        "--allowed-authors",
        default="",
        help="Extra maintainer logins, comma-separated.",
    )
    parser.add_argument("--repo", default="intersoftdatalabs-in/percussioncms")
    parser.add_argument("--base-branch", default="main")
    parser.add_argument(
        "--agent-safe-only",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Fold LargeI18n into NotSafe (default: true).",
    )
    parser.add_argument("--out", required=True, help="Output preflight.json path.")
    parser.add_argument(
        "--log-level",
        default="INFO",
        choices=["DEBUG", "INFO", "WARNING", "ERROR"],
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    logging.basicConfig(
        level=getattr(logging, args.log_level),
        format="%(asctime)s %(levelname)s %(message)s",
    )

    raw = _load_json(args.inventory)
    if not isinstance(raw, list):
        LOGGER.error("inventory is not a JSON array: %s", args.inventory)
        return 2

    document = build_preflight(
        inventory=raw,
        prescreen=_load_json(args.prescreen),
        decision_front=_load_json(args.decision_front),
        alerts=_load_json(args.alerts),
        maintainers=_load_maintainers(args.maintainers, args.allowed_authors),
        repo=args.repo,
        base_branch=args.base_branch,
        agent_safe_only=bool(args.agent_safe_only),
    )

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(document, indent=2) + "\n", encoding="utf-8")
    LOGGER.info(
        "wrote %s: issues_compact=%d owned_prs=%d peer=%d alerts=%s",
        out,
        len(document["issues_compact"]),
        document["stats"]["owned_prs"],
        document["stats"]["peer_pr_eligible"],
        document.get("open_alert_count", "unknown"),
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
