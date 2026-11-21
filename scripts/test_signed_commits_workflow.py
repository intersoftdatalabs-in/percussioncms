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
# See the License for the specific language governing permissions and
# limitations under the License.
"""Structural contract tests for .github/workflows/signed-commits.yml (#6855).

The workflow wires scripts/verify-signed-commits.py into CI on PRs. These
tests pin the design decisions that make the wiring actually catch the
#6769/#6770 defect class instead of silently passing it through:

* The ``pull_request`` trigger carries NO ``paths:`` filter — the gate
  inspects commits, not files, so filtering by path would let unsigned
  commits slip through on PRs that only touch other areas.
* Checkout uses ``fetch-depth: 0`` — ``git log <base>..<head>`` cannot
  resolve its range on the default depth-1 clone.
* The live ``check`` step runs the verifier against
  ``origin/${{ github.base_ref }}`` (the PR base) and is advisory
  (``continue-on-error: true``): CI runners carry no signer keyrings, so
  signatures cannot verify to ``G`` there; the ``required_signatures``
  ruleset stays the merge blocker while the step still fails loud with the
  gate's remediation output.
* The gate's behavioral suite (both directions) runs as a HARD step in the
  same job, so every PR proves the gate logic deterministically.
* Agent-safe: the workflow references no ``secrets.*`` and keeps
  ``permissions: contents: read`` (least privilege).

Deliberately stdlib-only (text/regex over the YAML): requirements-dev.txt
pins pytest alone, and adding a YAML parser dependency just to test a
workflow file would expand the install surface for every script-test run.
"""
from __future__ import annotations

from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
WORKFLOW = REPO_ROOT / ".github" / "workflows" / "signed-commits.yml"
GATE_SCRIPT = "scripts/verify-signed-commits.py"


def _read() -> str:
    assert WORKFLOW.is_file(), f"workflow missing: {WORKFLOW}"
    return WORKFLOW.read_text(encoding="utf-8")


def _trigger_block(text: str) -> str:
    """Return the raw text between the ``on:`` trigger and the ``jobs:`` key."""
    on_idx = text.find("\non:")
    assert on_idx != -1, "workflow has no 'on:' trigger block"
    jobs_idx = text.find("\njobs:", on_idx)
    assert jobs_idx != -1, "workflow has no 'jobs:' block"
    return text[on_idx:jobs_idx]


def test_workflow_triggers_on_pull_request_without_path_filter() -> None:
    trigger = _trigger_block(_read())
    assert "pull_request" in trigger, "gate must run on pull_request"
    assert "paths:" not in trigger, (
        "pull_request trigger must NOT be path-filtered: the gate inspects "
        "commits, and a paths filter would let unsigned commits through on "
        "PRs that only touch other areas (#6769/#6770)"
    )


def test_workflow_checkout_fetches_full_history() -> None:
    text = _read()
    assert "fetch-depth: 0" in text, (
        "checkout must use fetch-depth: 0 — git log <base>..<head> cannot "
        "resolve its range on the default depth-1 clone"
    )


def test_workflow_live_check_runs_verifier_against_pr_base() -> None:
    text = _read()
    assert GATE_SCRIPT in text, f"workflow must invoke {GATE_SCRIPT}"
    assert "origin/${{ github.base_ref }}" in text, (
        "live check must run against origin/<PR base> so the range covers "
        "exactly the PR's commits"
    )


def test_workflow_live_check_is_advisory() -> None:
    text = _read()
    run_marker = f"run: python {GATE_SCRIPT}"
    run_idx = text.find(run_marker)
    assert run_idx != -1, f"workflow must have a step running `{run_marker}`"
    # The advisory marker must precede the verifier invocation (same step).
    marker_idx = text.rfind("continue-on-error: true", 0, run_idx)
    assert marker_idx != -1, (
        "live check step must set continue-on-error: true — CI runners carry "
        "no signer keyrings, so the step reports (red, with remediation) "
        "without blocking merge; required_signatures stays the blocker"
    )


def test_workflow_runs_gate_behavioral_suite_as_hard_step() -> None:
    text = _read()
    assert "test_verify_signed_commits.py" in text, (
        "workflow must run the gate's behavioral suite (both directions) as "
        "a hard step on every PR"
    )
    assert "test_signed_commits_workflow.py" in text, (
        "workflow must run this contract suite so trigger/step regressions "
        "fail the job"
    )


def test_workflow_is_agent_safe_no_secrets_least_privilege() -> None:
    text = _read()
    assert "secrets." not in text, "gate needs no secrets; must reference none"
    assert "contents: read" in text, "workflow must keep least-privilege permissions"
