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
"""Lint: gh JSON hygiene rule reach + no live 2>&1-into-JSON pipes (#7020).

Follow-up to #6986 / #7015. #7015 added the hygiene rule and
``scripts/gh-json.py``, but the rule only landed in
``.opencode/agents/work.md`` (plus the Grok ``work_prompt``) — the next
cycle's night-worker PRE follow-up re-hit ``JSONDecodeError`` by merging
gh stderr into a JSON parser. This test pins the fix:

- every night agent doc that runs ``gh --json`` references the shared
  rule file (``scripts/gh-json-hygiene.md`` — one source of truth, #7020
  review), and
- no agent / rule / script file keeps a *live* ``2>&1 | ... json`` pipe
  (documented anti-pattern mentions that say NEVER / dies / wrapper
  nearby are allowed — they teach the rule, they don't run it).
"""
from __future__ import annotations

import re
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
AGENT_GLOB = ".opencode/agents/*.md"
RHAI = REPO_ROOT / ".grok" / "workflows" / "night-issue-prs.rhai"

# Shared single-source-of-truth rule file (#7020 review): agent docs and
# rhai prompts reference this instead of pasting the full paragraph.
SHARED_RULE_REF = "gh-json-hygiene.md"

# A shell pipe that merges gh stderr into a JSON consumer. The negated
# character class keeps the match to one logical line (no accidental
# cross-line hits from a bare `2>&1` log redirect elsewhere in a file).
DANGEROUS_PIPE_RE = re.compile(r"2>&1\s*\|[^\n]*json", re.IGNORECASE)

# Words that prove a `2>&1 | ... json` line is a *documented* anti-pattern
# (the fix's own explanation), not a live instruction. Checked
# case-insensitively in a ±5-line window around the match.
DOCUMENTED_CUES = (
    "never",
    "dies",
    "die with",
    "anti-pattern",
    "must not",
    "do not",
    "wrapper",
    "blessed",
    "reintroduces the bug",
)

# Files scanned for live pipes: every place a night agent copies shell
# from. Agent docs, both workflow dialects, helper scripts, and CI.
SCAN_GLOBS = (
    ".opencode/agents/*.md",
    ".opencode/commands/*.md",
    ".grok/workflows/*.rhai",
    ".kilo/workflows/*.md",
    "scripts/*.py",
    ".github/workflows/*.yml",
    ".github/workflows/*.yaml",
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _iter_scan_files():
    """Yield each unique file matched by SCAN_GLOBS, in sorted order."""
    seen: set[Path] = set()
    for glob in SCAN_GLOBS:
        for path in sorted(REPO_ROOT.glob(glob)):
            if path in seen or not path.is_file():
                continue
            seen.add(path)
            yield path


def _is_live_pipe(lines: list[str], lineno: int, line: str) -> bool:
    """True when a DANGEROUS_PIPE line has no documented-cue nearby.

    ``lineno`` is 1-based; the ±5-line cue window below it is checked
    case-insensitively for DOCUMENTED_CUES.
    """
    if not DANGEROUS_PIPE_RE.search(line):
        return False
    window = "\n".join(
        lines[max(0, lineno - 6) : min(len(lines), lineno + 5)]
    ).lower()
    return not any(cue in window for cue in DOCUMENTED_CUES)


def _live_pipes_in(path: Path) -> list[str]:
    """Live `2>&1 | ... json` pipe locations (`rel:lineno`) in one file.

    NEVER treat this docstring mention as a live pipe: it only describes
    what the helper reports, so the cue window excludes it.
    """
    lines = _read(path).splitlines()
    return [
        f"{path.relative_to(REPO_ROOT)}:{lineno}"
        for lineno, line in enumerate(lines, start=1)
        if _is_live_pipe(lines, lineno, line)
    ]


def test_agent_docs_carry_shared_rule_reference() -> None:
    """Every `.opencode/agents/*.md` that runs `gh --json` references the
    shared rule file (single source of truth, #7020 review)."""
    missing = [
        str(path.relative_to(REPO_ROOT))
        for path in sorted(REPO_ROOT.glob(AGENT_GLOB))
        if "--json" in _read(path) and SHARED_RULE_REF not in _read(path)
    ]
    assert not missing, (
        "agent docs run `gh --json` without referencing the shared gh JSON "
        f"hygiene rule `scripts/{SHARED_RULE_REF}` (#7020): {missing}"
    )


def test_no_live_stderr_merge_into_json_parser() -> None:
    """No live `2>&1 | ... json` pipes in agent/rule/script files (#7020).

    Documented NEVER anti-patterns (like the strings in this very
    test) are excluded via the cue window below.
    """
    violations: list[str] = []
    for path in _iter_scan_files():
        violations.extend(_live_pipes_in(path))
    assert not violations, (
        "NEVER allow live `2>&1 | ... json` pipes: they feed the gh 2.105 "
        f"stderr hint into a JSON parser (#6986/#7020): {violations}"
    )


def test_rhai_prompts_carry_the_rule() -> None:
    """Grok work + follow-up + preflight + triage + cluster prompts (#7020).

    Prompts reference the shared rule file (single source of truth, #7020
    review) instead of pasting the full paragraph.
    """
    text = _read(RHAI)
    count = text.count(SHARED_RULE_REF)
    assert count >= 5, (
        "expected the shared hygiene rule reference in work + pr-follow-up "
        f"+ preflight + triage + cluster prompts, found {count} mention(s)"
    )
    for prompt_var in ("work_prompt", "pr_prompt_core", "pf_prompt",
                       "triage_prompt", "cl_prompt"):
        assert prompt_var in text, f"prompt variable gone: {prompt_var}"
