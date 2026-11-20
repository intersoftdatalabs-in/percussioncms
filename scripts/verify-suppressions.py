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
"""Suppression-ledger verifier for ``suppressions.md`` (issue #6823).

Purpose
-------
For every row in ``suppressions.md``, require the ``justification`` to cite
closure evidence — a code-scanning dismissal reason/URL or a code-rewrite
reference — instead of a sink-line ``// codeql[...]`` comment. A sink-line
``// codeql[<rule-id>]`` comment alone never closes a GitHub code-scanning
alert (issues #6784 / #6823: on PR #6770 a same-line comment was added yet
alert #2101 stayed open and the CodeQL check kept failing). Closure requires
either (a) a code rewrite that removes the flagged pattern (the alert
auto-resolves as ``fixed`` on rescan) or (b) dismissal of the alert through
code scanning with a reason. The ledger row is therefore the suppression
mechanism of record; a ``// codeql[...]`` anchor — where one exists — is
documentation only and is NOT required.

Row contract (checked per row)
------------------------------
1. ``justification`` cites closure evidence: a dismissal marker
   (``dismis...`` — "Dismissed FP", "dismiss reason", ...), a code-rewrite
   reference (runtime/structural fix, sanitizer/guard/barrier, model pack,
   path query-filter / paths-ignore, PR/issue link, tests, accepted-risk
   record), or — for local-analyzer-only rows — an ``adjudicated`` /
   ``mkd-code-review`` intentional-FP record.
2. Every concretely cited source file must still exist (stale-row
   detection). Combined ``config + sink`` cells check each side;
   ``...``-elided and prose-only cells are descriptive and skipped.
3. ``mkd-`` rows (local analyzer only, no CodeQL alert ID) must carry the
   in-source ``mkd-code-review[<rule>]`` marker — the one inline mechanism
   the local analyzer honors.
4. Rows anchored at ``.github/codeql/codeql-config.yml`` pass when that
   config exists (the path-exclusion mechanism lives there).

The disposition ladder is unchanged: runtime fix → model pack → sink-line
comment → path query-filters → dismiss last. The ladder steps are
documentation, not closure; only the rewrite or the dismissal closes.

Usage
-----
::

    python3 scripts/verify-suppressions.py [--suppressions <path>] [--source-root <path>]

Behavioral Notes
----------------
- Portable path handling throughout: ``pathlib.Path`` joins, no hardcoded
  separators, no shell. ``git ls-files`` builds the basename index for
  shorthand (extension-only, no directory) citations; when git is
  unavailable only exact relative paths are resolved.
- Exit codes: ``0`` every row cites closure evidence and every cited file
  exists; ``1`` otherwise (reason printed per row).
"""
from __future__ import annotations

import argparse
import logging
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

CONFIG_PATH = ".github/codeql/codeql-config.yml"

#: Closure evidence a ``justification`` must cite (issue #6823). A row whose
#: justification merely notes that a ``// codeql[...]`` comment was sprinkled
#: — with no dismissal, rewrite, barrier, filter, test, or adjudication
#: reference — fails the check.
EVIDENCE_RE = re.compile(
    r"dismis"  # dismissal through code scanning ("Dismissed FP", "dismiss reason", ...)
    r"|adjudicated|mkd-code-review"  # local-analyzer adjudication (mkd- rows)
    r"|PR\s*#\d+|issue\s*#\d+"  # traceability to the closing PR / issue
    r"|runtime|structural|\bfix"  # the code rewrite that removed the pattern
    r"|rewrite|refactor"
    r"|sanitiz|validat|guard|barrier|canonical|rebuild|escape|quote"  # runtime defenses
    r"|allow-list|no-throw|constant"
    r"|paths-ignore|path exclu|query-filter|model pack"  # config mechanisms
    r"|\btests?\b"  # behavioral-test evidence backing the rewrite claim
    r"|ACCEPTED-RISK|accepted.risk"  # accepted-risks.md record
    r"|\brequire",  # requireUnderBase / requireSafe... barrier entry points
    re.IGNORECASE,
)

#: Single-rule token (``area.name``) eligible for the in-source
#: ``mkd-code-review[<rule>]`` marker check. Compound cells such as
#: ``complexity.cognitive / patterns.hard_gate (tests.empty-catch)`` are
#: descriptive and skipped.
SINGLE_RULE_RE = re.compile(r"^[A-Za-z0-9_]+(?:\.[A-Za-z0-9_\-]+)+$")

#: File extensions treated as concrete source citations (bare basenames are
#: resolved through the repo index; anything else is descriptive prose).
SOURCE_EXTENSIONS = (
    ".java",
    ".js",
    ".jsx",
    ".ts",
    ".tsx",
    ".mjs",
    ".cjs",
    ".py",
    ".css",
    ".xml",
    ".jsp",
    ".tag",
    ".html",
)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify_suppressions.py",
        description="Verify CodeQL suppression ledger rows cite closure evidence.",
    )
    parser.add_argument(
        "--suppressions",
        default="docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md",
        help="Suppressions markdown path",
    )
    parser.add_argument(
        "--source-root",
        default=str(REPO_ROOT),
        help="Repo root for resolving cited file paths",
    )
    return parser


def _parse_suppression_rows(text: str) -> list[dict[str, str]]:
    """Parse the 8-column ledger table.

    The schema (contracts C3) is ``alert_id | rule_id | file_path | line |
    justification | applied_on | applied_by | review_by``. The first column is
    free-form (numeric IDs, comma/ranges lists, ``PR-...`` tags, ``mkd-...``
    local-analyzer rows); header and separator rows are skipped.
    """
    rows: list[dict[str, str]] = []
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped.startswith("|"):
            continue
        cells = [cell.strip() for cell in stripped.strip().strip("|").split("|")]
        if len(cells) < 8:
            continue
        first = cells[0].strip("`").lower()
        if first in ("alert_id", "#"):
            continue
        if first and set(first) <= set("-: "):
            continue
        rows.append(
            {
                "alert_id": cells[0].strip("`"),
                "rule": cells[1].strip("`"),
                # Keep the file_path cell raw (inner backticks intact): _check_row
                # extracts each `...`-quoted token. Stripping outer backticks
                # here would corrupt multi-token cells such as
                # "`A.java` / `B.java`" into "A.java` / `B.java".
                "path": cells[2],
                "line": cells[3].strip("`"),
                "justification": cells[4],
            }
        )
    return rows


def _basename_index(source_root: Path) -> dict[str, list[str]]:
    """Map tracked basenames to repo-relative paths (once, for shorthand cites).

    Falls back to an empty index when git is unavailable; callers then only
    resolve exact relative paths.
    """
    try:
        proc = subprocess.run(
            ["git", "ls-files"],
            shell=False,
            check=False,
            timeout=120,
            capture_output=True,
            text=True,
            cwd=str(source_root),
        )
    except (OSError, subprocess.SubprocessError) as exc:
        LOGGER.warning("git ls-files unavailable (%s); basename cites skipped", exc)
        return {}
    if proc.returncode != 0:
        LOGGER.warning("git ls-files failed; basename cites skipped")
        return {}
    index: dict[str, list[str]] = {}
    for entry in proc.stdout.splitlines():
        name = entry.rsplit("/", 1)[-1]
        index.setdefault(name, []).append(entry)
    return index


def _resolve_cited_file(
    part: str, source_root: Path, index: dict[str, list[str]]
) -> Path | None:
    """Resolve one ``+``-separated file_path cell part.

    Returns the resolved path, or ``None`` when the part is descriptive prose
    (no directory, no source extension) or an ``...``-elided shorthand.
    """
    if not part or "..." in part:
        return None
    lowered = part.lower()
    if "/" in part:
        candidate = source_root / Path(*part.split("/"))
        return candidate if candidate.is_file() else candidate
    if lowered.endswith(SOURCE_EXTENSIONS):
        hits = index.get(part.split()[-1], [])
        if hits:
            return source_root / Path(*hits[0].split("/"))
        return source_root / part
    return None


def _check_file_part(
    part: str, row: dict[str, str], resolved: Path
) -> str | None:
    """Check one resolved file_path cell part; ``None`` on success."""
    if not resolved.is_file():
        return (
            f"row {row['alert_id']!r} ({row['rule']}) cites missing file: "
            f"{part}"
        )
    if row["alert_id"].lower().startswith("mkd-") and SINGLE_RULE_RE.match(
        row["rule"]
    ):
        marker = f"mkd-code-review[{row['rule']}]"
        if marker not in resolved.read_text(encoding="utf-8", errors="replace"):
            return (
                f"row {row['alert_id']!r} lacks the in-source "
                f"{marker} marker in {part}"
            )
    return None


def _check_row(
    row: dict[str, str], source_root: Path, index: dict[str, list[str]]
) -> str | None:
    """Return ``None`` when the row satisfies the closure contract."""
    if not EVIDENCE_RE.search(row["justification"]):
        return (
            f"row {row['alert_id']!r} ({row['rule']}) cites no closure evidence: "
            "justification must state the code-scanning dismissal reason/URL "
            "or the code-rewrite reference (runtime fix, barrier, filter, "
            "tests, PR link), not merely a // codeql[...] comment"
        )
    parts: list[str] = []
    for chunk in row["path"].split("+"):
        spans = re.findall(r"`([^`]+)`", chunk)
        if spans:
            parts.extend(span.strip() for span in spans)
        elif chunk.strip():
            parts.append(chunk.strip().strip("`"))
    for part in parts:
        if not part:
            continue
        if part == CONFIG_PATH:
            if not (source_root / CONFIG_PATH).is_file():
                return f"row {row['alert_id']!r} cites missing {CONFIG_PATH}"
            continue
        resolved = _resolve_cited_file(part, source_root, index)
        if resolved is None:
            continue
        err = _check_file_part(part, row, resolved)
        if err is not None:
            return err
    return None


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    supp_path = Path(args.suppressions)
    if not supp_path.is_file():
        print(f"FAIL: {supp_path} not found", file=sys.stderr)
        return 1
    source_root = Path(args.source_root)

    rows = _parse_suppression_rows(supp_path.read_text(encoding="utf-8"))
    if not rows:
        print("FAIL: no suppression rows parsed", file=sys.stderr)
        return 1
    index = _basename_index(source_root)
    failures = 0
    for row in rows:
        err = _check_row(row, source_root, index)
        if err is not None:
            print(f"  FAIL: {err}", file=sys.stderr)
            failures += 1
    if failures:
        print(f"verify-suppressions: FAIL ({failures} row(s))", file=sys.stderr)
        return 1
    print(f"verify-suppressions: PASS ({len(rows)} rows)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
