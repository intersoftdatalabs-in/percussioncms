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
"""TypeSafe pre-screen for the `night-issue-prs` workflow.

Purpose
-------
A cheap (~$0.002, ~1s on 40+ issues) judgement layer run by the Preflight
scout *before* the expensive Triage / Work agents. It computes per-issue:

* ``rule_skip`` / ``rule_kind``   — deterministic GitHub-label rules
  (``not safe for agents``, ``in progress``, ``qa task``, ``migrated``,
  assignee present). These are authoritative and never depend on the model.
* ``rule_close_candidate``        — the ``migrated`` label marks a reconcile
  close candidate.
* ``model.skip_safe`` etc.        — one batched TypeSafe (jev) call with
  three *Noul* questions per issue that is not already rule-skipped:
  ``skip_safe`` (soak / customer-env / gated / human-sign-off that the
  nightly work agent cannot complete), ``pr_sized`` (single-PR sized) and
  ``close`` (recommend reconcile closing). The model **only adds** hints; it
  never overrides a deterministic rule.

Fail-open
---------
If ``TYPESAFE_API_KEY`` is missing, the API errors, or the network is down,
the script still writes ``--out`` with the deterministic layer populated and
sets ``status: "fallback_rule_only"``. Downstream phases must treat the file
as a screening aid, never as a gate.

Usage
-----
::

    python3 scripts/typesafe-prescreen.py \
        --inventory scratch/issues-raw.json \
        --out scratch/prescreen.json

    # Windows
    scripts\\typesafe-prescreen.cmd --inventory scratch\\issues-raw.json --out scratch\\prescreen.json

    # Reuse a previously saved TypeSafe /systemone response (tests / no live call)
    python3 scripts/typesafe-prescreen.py --inventory i.json --out p.json --answers saved-response.json

Exit codes
----------
0     wrote --out (normal or fallback_rule_only)
2     usage error (bad args / unreadable inventory / bad answers file)
1     unexpected internal error

Portability
-----------
Stdlib-only at runtime; no ``shell=True``; ``Path`` APIs; no hardcoded path
separators; works on Windows / Linux / macOS.
"""
from __future__ import annotations

import argparse
import json
import logging
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

LOGGER = logging.getLogger("typesafe-prescreen")

API_URL = "https://api.typesafe.ai/v1/systemone"
DEFAULT_MODEL = "jev-latest"
DEFAULT_THRESHOLD = 0.85
DEFAULT_CLOSE_THRESHOLD = 0.85
INPUT_PRICE_PER_MT = 0.042  # $ per 1M input tokens (jev, 2026-09)
OUTPUT_PRICE_PER_MT = 0.00  # $ per 1M output tokens

# Label names (case-insensitive) with hard-baked semantics.
RULE_KINDS = (
    # highest severity first: prescreen only reports the first match
    ("not_safe_agents", "not safe for agents"),
    ("in_progress", "in progress"),
    ("in_progress", "in-progress"),
    ("human_qa", "qa task"),
    ("migrated", "migrated"),
)

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


def _assignee_logins(assignees: object) -> list[str]:
    if not assignees:
        return []
    logins = []
    for asg in assignees:
        if isinstance(asg, str):
            logins.append(asg)
        elif isinstance(asg, dict) and isinstance(asg.get("login"), str):
            logins.append(asg["login"])
    return logins


def normalize_issue(raw: dict) -> dict:
    """Normalize a raw ``gh issue list`` object (or a hand-built dict) into the
    compact shape the questions and output use. Never fails on missing keys."""
    labels = _label_names(raw.get("labels"))
    assignees = _assignee_logins(raw.get("assignees"))
    author = raw.get("author")
    if isinstance(author, dict):
        author = author.get("login", "unknown")
    return {
        "number": int(raw["number"]),
        "title": str(raw.get("title", "")),
        "labels": labels,
        "assignees": assignees,
        "author": str(author or "unknown"),
    }


def deterministic_rules(issue: dict) -> dict:
    """Return rule-layer fields for one normalized issue. Authoritative."""
    rule_skip = False
    rule_kind = None
    rule_close = False
    labels = {name.lower() for name in issue["labels"]}
    for kind, label in RULE_KINDS:
        if label in labels:
            rule_skip = True
            rule_kind = kind
            if kind == "migrated":
                rule_close = True
            break
    if not rule_skip and issue["assignees"]:
        rule_skip = True
        rule_kind = "assigned"
    return {
        "rule_skip": rule_skip,
        "rule_kind": rule_kind,
        "rule_close_candidate": rule_close,
    }


def build_state(issues: list[dict]) -> dict:
    return {
        "issues": [
            {
                "number": it["number"],
                "title": it["title"],
                "labels": it["labels"],
                "author": it["author"],
                "assignees": it["assignees"],
            }
            for it in issues
        ]
    }


def build_questions(indexed: list[tuple[int, dict]]) -> dict:
    """One batched request; three Noul questions per indexable issue."""
    questions = {}
    for idx, it in indexed:
        n = it["number"]
        ref = "issue `issues[" + str(idx) + "]` (**#" + str(n) + "**): `issues[" + str(idx) + "].title`"
        labels = "labels: `issues[" + str(idx) + "].labels`; assignees: `issues[" + str(idx) + "].assignees`"
        questions[f"q_{n}_skip"] = {
            "type": "noul",
            "instructions": {
                "ref": ref,
                "labels": labels,
                "question": (
                    "Should the nightly issue-PR workflow SKIP this issue so its "
                    "high-cost work agent is NOT dispatched for it right now?"
                ),
                "criteria": {
                    "true": (
                        "Not actionable by the in-flight nightly agent: human QA / UAT / "
                        "sign-off task, soak or live-install verification, customer/snapshot "
                        "environment check, or gated on an external prerequisite ('when X is "
                        "supported', 'when criteria met', 'after <other issue>')."
                    ),
                    "false": (
                        "Actionable coding work a nightly agent can implement and open a PR for "
                        "in the current environment."
                    ),
                },
            },
        }
        questions[f"q_{n}_prsized"] = {
            "type": "noul",
            "instructions": {
                "ref": ref,
                "labels": labels,
                "question": (
                    "Is this issue sized so one agent can implement it end-to-end and close "
                    "it with a single code PR?"
                ),
                "criteria": {
                    "true": "A single code PR closes it (typical bugfix, bounded cleanup, pre-sliced slice).",
                    "false": (
                        "Epic / roadmap bucket needing slicing, the parent of pre-sliced work, "
                        "or a QA / UAT / soak task with no single code PR."
                    ),
                },
            },
        }
        questions[f"q_{n}_close"] = {
            "type": "noul",
            "instructions": {
                "ref": ref,
                "labels": labels,
                "question": (
                    "Is this issue already fully implemented and should be RECOMMENDED for "
                    "closing by the reconcile step?"
                ),
                "criteria": {
                    "true": "Clearly done: labeled 'migrated', or the described work is verifiably complete.",
                    "false": "The work is still outstanding; the issue should stay open.",
                },
            },
        }
    return questions


def ts_ask(state: dict, questions: dict, model: str, timeout: int = 600,
           max_retries: int = 5, answers_cache: dict | None = None):
    """POST one System One request (or load a cached response). Returns the
    TypeSafe response dict. Raises on unexpected HTTP errors; retries 429/529."""
    if answers_cache is not None:
        return answers_cache
    api_key = os.environ.get("TYPESAFE_API_KEY")
    if not api_key:
        raise RuntimeError("TYPESAFE_API_KEY is not set")
    body = json.dumps({"state": state, "model": model, "questions": questions}).encode("utf-8")
    delay = 1.0
    for attempt in range(max_retries + 1):
        req = urllib.request.Request(
            API_URL,
            data=body,
            method="POST",
            headers={"Authorization": "Bearer " + api_key, "Content-Type": "application/json"},
        )
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as err:
            if err.code in (429, 529):
                retry_after = err.headers.get("Retry-After")
                wait = float(retry_after) if retry_after else delay
                LOGGER.warning("retry %s: waiting %.1fs", err.code, wait)
                time.sleep(wait)
                delay = min(delay * 2, 30.0)
            else:
                raise
        except urllib.error.URLError as err:  # network-level
            LOGGER.warning("urlerror: %s; backing off %.1fs", err, delay)
            time.sleep(delay)
            delay = min(delay * 2, 30.0)
    raise RuntimeError("max retries exceeded")


def load_answers_file(path: Path) -> dict:
    """Load a previously saved TypeSafe ``/systemone`` response for reuse."""
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except OSError as err:
        raise SystemExit(f"ERROR: cannot read --answers {path}: {err}") from err
    if not isinstance(payload, dict) or not isinstance(payload.get("answers"), dict):
        raise SystemExit(
            "ERROR: --answers must be a TypeSafe /systemone response "
            "{\"answers\": {...}, \"usage\": {...}}"
        )
    return payload


def _usage_cost(usage: object) -> float:
    if not isinstance(usage, dict):
        return 0.0
    return (
        float(usage.get("input_tokens") or 0) / 1e6 * INPUT_PRICE_PER_MT
        + float(usage.get("output_tokens") or 0) / 1e6 * OUTPUT_PRICE_PER_MT
    )


def _merge_answers(issues: list[dict], answers: dict,
                   threshold: float, close_threshold: float) -> list[dict]:
    """Per-issue: deterministic rules (authoritative) + optional model scores and
    recommendations. The model never un-skips an issue the rules skipped."""
    out = []
    for it in issues:
        n = it["number"]
        rec = {
            "number": n,
            "title": it["title"],
            "labels": it["labels"],
            "assignees": it["assignees"],
            "author": it["author"],
        }
        rec.update(deterministic_rules(it))
        skip = answers.get(f"q_{n}_skip")
        ps = answers.get(f"q_{n}_prsized")
        cl = answers.get(f"q_{n}_close")
        if skip and isinstance(skip, dict) and skip.get("type") == "noul":
            rec["model"] = {
                "skip_safe": skip.get("noul"),
                "pr_sized": ps.get("noul") if ps and isinstance(ps, dict) else None,
                "close": cl.get("noul") if cl and isinstance(cl, dict) else None,
            }
        else:
            rec["model"] = None
        if rec["rule_skip"]:
            rec["recommend_skip"] = True
            rec["skip_source"] = "rule"
        elif (rec["model"] and rec["model"]["skip_safe"] is not None
              and rec["model"]["skip_safe"] >= threshold):
            rec["recommend_skip"] = True
            rec["skip_source"] = "model"
        else:
            rec["recommend_skip"] = False
            rec["skip_source"] = None
        rec["recommend_close"] = rec["rule_close_candidate"] or bool(
            rec["model"] and rec["model"]["close"] is not None
            and rec["model"]["close"] >= close_threshold
        )
        out.append(rec)
    return out


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="typesafe-prescreen.py",
        description="TypeSafe jev pre-screen for the night-issue-prs workflow (fail-open).",
    )
    parser.add_argument("--inventory", default="scratch/issues-raw.json",
                        help="path to issue inventory JSON (gh issue list shape)")
    parser.add_argument("--out", default="scratch/prescreen.json",
                        help="output prescreen.json path")
    parser.add_argument("--threshold", type=float, default=DEFAULT_THRESHOLD,
                        help="skip_safe threshold to ADD a model skip (default 0.85)")
    parser.add_argument("--close-threshold", type=float, default=DEFAULT_CLOSE_THRESHOLD,
                        help="close threshold (default 0.85)")
    parser.add_argument("--model", default=os.environ.get("TYPESAFE_MODEL", DEFAULT_MODEL),
                        help=f"TypeSafe model (default {DEFAULT_MODEL})")
    parser.add_argument("--answers", default=None,
                        help="reuse a saved TypeSafe /systemone response instead of a live call")
    parser.add_argument("--dry-run", action="store_true",
                        help="never call the API; deterministic layer only (also sets fallback status)")
    parser.add_argument("--max-inventory", type=int, default=120,
                        help="cap the number of scored issues (default 120)")
    args = parser.parse_args(argv)

    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")

    inventory_path = Path(args.inventory)
    out_path = Path(args.out)
    try:
        raw = json.loads(inventory_path.read_text(encoding="utf-8"))
    except OSError as err:
        print(f"ERROR: cannot read inventory {inventory_path}: {err}", file=sys.stderr)
        return 2
    except json.JSONDecodeError as err:
        print(f"ERROR: inventory {inventory_path} is not valid JSON: {err}", file=sys.stderr)
        return 2
    if not isinstance(raw, list):
        print("ERROR: inventory must be a JSON array of issue objects", file=sys.stderr)
        return 2

    issues = [normalize_issue(item) for item in raw][: args.max_inventory]
    if not issues:
        print("ERROR: inventory contains no issues", file=sys.stderr)
        return 2

    answers_cache = None
    if args.answers:
        answers_cache = load_answers_file(Path(args.answers))

    status = "ok"
    model_meta = None
    usage = None
    latency = None
    questions = {}
    answers = {}

    # the model only scores issues the deterministic layer did NOT already skip
    to_ask = [(idx, it) for idx, it in enumerate(issues)
              if not deterministic_rules(it)["rule_skip"]]
    if to_ask:
        if args.dry_run:
            status = "dry_run"
        else:
            state = build_state(issues)
            questions = build_questions(to_ask)
            try:
                t0 = time.monotonic()
                raw_response = ts_ask(state, questions, args.model,
                                      answers_cache=answers_cache) or {}
                latency = time.monotonic() - t0
                wanted = set(questions)
                answers = {qid: a for qid, a in (raw_response.get("answers") or {}).items()
                           if qid in wanted}
                usage = raw_response.get("usage") if isinstance(raw_response.get("usage"), dict) else None
                model_meta = {"model": raw_response.get("model")}
            except Exception as err:  # noqa: BLE001 - fail-open by design
                LOGGER.warning("TypeSafe call failed; fallback to rule-only layer: %s", err)
                status = "fallback_rule_only"
                answers = {}

    full = _merge_answers(issues, answers, args.threshold, args.close_threshold)

    rule_skips = sum(1 for r in full if r["rule_skip"])
    model_rec_skips = sum(1 for r in full if r["recommend_skip"] and r["skip_source"] == "model")
    rec_skips = sum(1 for r in full if r["recommend_skip"])
    close_candidates = sum(1 for r in full if r["recommend_close"])
    pr_sized_counts = {
        "low": sum(1 for r in full if r["model"] and r["model"]["pr_sized"] is not None and r["model"]["pr_sized"] < 0.3),
        "high": sum(1 for r in full if r["model"] and r["model"]["pr_sized"] is not None and r["model"]["pr_sized"] >= args.threshold),
    }

    result = {
        "phase": "preflight.prescreen",
        "executor": "script:typesafe-prescreen",
        "args": {
            "inventory": str(inventory_path),
            "threshold": args.threshold,
            "close_threshold": args.close_threshold,
            "model": args.model,
            "max_inventory": args.max_inventory,
        },
        "status": status,
        "model": model_meta,
        "usage": usage,
        "cost_usd_est": _usage_cost(usage) if usage else 0.0,
        "latency_s": latency,
        "issues": full,
        "stats": {
            "issues_total": len(full),
            "rule_skips": rule_skips,
            "model_recommend_skips": model_rec_skips,
            "recommend_skips": rec_skips,
            "close_candidates": close_candidates,
            "pr_sized_high_conf": pr_sized_counts["high"],
            "pr_sized_low_conf": pr_sized_counts["low"],
        },
        "notes": [
            "Deterministic label rules are authoritative; the model only ADDS semantic skip hints.",
            "recommend_skip from the model requires skip_safe >= threshold (fail-open below it).",
            "No issue is ever un-skipped by a model answer.",
        ],
    }

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(result, indent=2), encoding="utf-8")
    LOGGER.info(
        "prescreen %s: issues=%d rule_skips=%d model_rec_skips=%d close=%d cost=$%s latency=%s",
        status, len(full), rule_skips, model_rec_skips, close_candidates,
        f"{result['cost_usd_est']:.5f}", latency if latency is None else f"{latency:.2f}s",
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())