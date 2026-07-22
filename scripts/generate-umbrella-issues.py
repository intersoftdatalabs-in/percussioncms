#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/generate-umbrella-issues.sh.

Purpose
-------
Read ``docs/ai-generated/tasks/gh-codeql-alerts/triage.md`` and emit one
"umbrella" Markdown file per top-5 module owner, grouping alert IDs by
disposition. Each umbrella lists totals and a per-cluster PR plan; the data
drives the 004-zero-code-scanning-alerts workflow.

Usage
-----
::

    python3 scripts/generate-umbrella-issues.py --input <triage.md>
                                               [--output-dir <dir>]
                                               [--dry-run]

Behavioral Notes
----------------
- The bash version hard-coded the top-5 module list (``WebUI/``, ``system/``,
  ``projects/sitemanage/``, ``modules/perc-packages/``,
  ``modules/perc-common-ui-bundle/``). The port preserves that list as the
  default but also accepts ``--module-owner`` overrides in the contract.
- ``awk -F'|'`` parsing is replaced by a Python Markdown-row parser that
  handles backticks / parenthetical disposition labels (e.g.
  ``obsolete (candidate)``) without regex hacks.
- Output filenames use ``Path.stem`` on the module path (``WebUI/`` →
  ``WebUI_``) to avoid path-separator collisions on Windows.
"""
from __future__ import annotations

import argparse
import logging
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

DEFAULT_MODULES = (
    "WebUI/",
    "system/",
    "projects/sitemanage/",
    "modules/perc-packages/",
    "modules/perc-common-ui-bundle/",
)

# Inline cluster plans per module, ported verbatim from the bash original.
CLUSTER_PLANS: dict[str, list[str]] = {
    "WebUI/": [
        "| knockout.js vendored dist | T021 | 5 files | 32+ |",
        "| twitter-bootstrap-3.0.0 | T022 | 9 files | 250+ |",
        "| jquery-migrate | T023 | 2 files | 12 |",
        "| shared-*.js + perc_utils.js | T024 | 7 files | 100+ |",
        "| PercDataTable widget | T025 | 3 files | 96+ |",
        "| third-party vendored JS | T026 | 3 files | mixed |",
        "| JS critical-severity fixes (xss/code-injection) | T038, T058, T060 | varies | TBD |",
        "| JS high/medium fixes | T044 etc. | varies | TBD |",
    ],
    "system/": [
        "| ApplicationFiles dojo/trinidad/jstree | T027 | 3+ files | 67 |",
        "| dojo vendor files in non-ApplicationFiles paths | T028 | varies | TBD |",
        "| Java critical: `java/xxe` (PSSerializerUtils) | T039 | 1 file | 2 |",
        "| Java critical: `java/ldap-injection` (PSJndiGroupProvider) | T040 | 1 file | 1 |",
        "| Java high: `java/zipslip` (PSArchiveFiles) | T041 | 1 file | 3 |",
        "| Java high: `java/insecure-trustmanager` (PSDeliveryClient) | T046 | 1 file | 2 |",
        "| Java medium: `java/stack-trace-exposure`, etc. | T055 etc. | varies | TBD |",
    ],
    "projects/sitemanage/": [
        "| test sample HTML (CM1094-SamplePage.html etc.) | T029 | 7 files | 28 |",
        "| Java high: `java/sql-injection` (PSPageDaoHelper) | T042 | 1 file | 7 |",
        "| Java high: `java/path-injection` (58 alerts) | T043 | 50+ files | 58 |",
        "| Java high: `java/xss` (PSSiteDataRestService) | T044 | 1 file | 35 |",
        "| Java medium: `java/unsafe-hostname-verification`, `java/error-message-exposure`, etc. | T053, T054 | varies | TBD |",
    ],
    "modules/perc-packages/": [
        "| Per-finding valid fixes | T042-T063 | TBD | 35 |",
    ],
    "modules/perc-common-ui-bundle/": [
        "| Per-finding valid fixes | T042-T063 | TBD | 14 |",
    ],
}


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="generate-umbrella-issues.py",
        description="Emit umbrella-issue Markdown per top module owner.",
    )
    parser.add_argument(
        "--input",
        required=True,
        help="Path to triage.md (input)",
    )
    parser.add_argument(
        "--output-dir",
        default="docs/ai-generated/tasks/gh-codeql-alerts/",
        help="Output directory for umbrella issue files",
    )
    parser.add_argument(
        "--module-owner",
        action="append",
        default=None,
        help="Module owner to emit (repeatable; default: top-5)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the file paths that would be written without writing",
    )
    return parser


_ROW_RE = re.compile(r"^\|\s*(\d+)\s*\|")


def parse_triage(text: str) -> list[dict[str, str]]:
    """Parse a triage.md into a list of row dicts with stable column names.

    Columns per ``contracts/C1``:
        #, alert_id, rule_id, severity, file_path, module_owner, disposition,
        target_action, target_milestone, linked_pr, notes
    """
    rows: list[dict[str, str]] = []
    for line in text.splitlines():
        if not _ROW_RE.match(line):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 11:
            continue
        disposition = cells[6].strip("`")
        disposition = re.sub(r"\s*\(candidate\)", "", disposition).strip()
        rows.append(
            {
                "alert_id": cells[1].strip("`"),
                "rule_id": cells[2].strip("`"),
                "severity": cells[3].strip("`"),
                "file_path": cells[4].strip("`"),
                "module_owner": cells[5].strip("`"),
                "disposition": disposition,
                "target_action": cells[7].strip("`"),
                "target_milestone": cells[8].strip("`"),
                "linked_pr": cells[9].strip("`"),
                "notes": cells[10].strip("`"),
            }
        )
    return rows


def _output_filename_for_module(module_owner: str) -> str:
    """Return a filename for the umbrella issue; safe on Windows (no slashes)."""
    safe = module_owner.replace("/", "_").strip("_")
    return f"{safe}.md"


def _build_umbrella_body(module_owner: str, rows: list[dict[str, str]]) -> str:
    """Render the Markdown body for one module's umbrella issue."""
    lines: list[str] = [
        f"# Umbrella: code-scanning alerts for `{module_owner}`",
        "",
        "Tracks the closure of every code-scanning (CodeQL) alert on",
        "`development` whose `module_owner` in",
        "[triage.md](docs/ai-generated/tasks/gh-codeql-alerts/triage.md)",
        f"is `{module_owner}` per spec `004-zero-code-scanning-alerts` (US2/US3/US4).",
        "",
        "Source of truth: `docs/ai-generated/tasks/gh-codeql-alerts/triage.md`",
        f"(filter on column 7 = `{module_owner}`).",
        "",
        "## Totals (computed by generate-umbrella-issues.py)",
        "",
        "| Disposition | Count |",
        "|-------------|-------|",
    ]
    counts: dict[str, int] = {}
    for row in rows:
        if row["module_owner"] != module_owner:
            continue
        counts[row["disposition"]] = counts.get(row["disposition"], 0) + 1
    for disposition, count in sorted(counts.items()):
        lines.append(f"| `{disposition}` | {count} |")
    lines.extend(
        [
            "",
            "## Per-cluster PR plan",
            "",
            "| Cluster | Task | File(s) | Approx alert count |",
            "|---------|------|---------|--------------------|",
        ]
    )
    lines.extend(CLUSTER_PLANS.get(module_owner, ["| (no plan; populate) | - | - | - |"]))
    lines.extend(
        [
            "",
            "## Definition of done (per cluster)",
            "",
            "1. PR merges to `development` with the standard closing checklist",
            "   per `specs/004-zero-code-scanning-alerts/contracts/README.md` C5.",
            "2. `scripts/verify-triage-inventory.py` still passes after",
            "   the `linked_pr` column is updated in `triage.md`.",
            "3. `scripts/verify-valid-fixes.py` reports 0 unlinked valid",
            "   rows for this module (T062 / SC-007).",
            "4. For US3 (valid): regression test fails on the pre-fix code and",
            "   passes on the post-fix code, with the pre-fix commit hash",
            "   recorded in the PR body (Constitution III, contracts/C5).",
            "5. For US2 (obsolete): `scripts/verify-distribution-archive.py`",
            "   confirms the removed files are absent from the rebuilt",
            "   `modules/perc-distribution-tree` + `modules/perc-packages`",
            "   JARs and the assembled `.ppkg`.",
            "6. Constitution IX: every review thread on the closing PR is",
            "   resolved via the GraphQL `resolveReviewThread` mutation",
            "   before the merge button is enabled (T078b).",
            "",
            "## Coordination",
            "",
            "- Spec: `specs/004-zero-code-scanning-alerts/spec.md`",
            "- Tasks: `specs/004-zero-code-scanning-alerts/tasks.md`",
            "- Triage: `docs/ai-generated/tasks/gh-codeql-alerts/triage.md`",
            "- Verify scripts: `scripts/verify-{triage-inventory,valid-fixes,suppressions,distribution-archive,pr-review-resolution}.py`",
            "- Stale-cache filter: `scripts/filter_stale_alerts.py` + `docs/ai-generated/tasks/gh-codeql-alerts/alerts-stale-cache.md`",
            "",
        ]
    )
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    triage_path = Path(args.input)
    if not triage_path.is_file():
        LOGGER.error("triage file not found: %s", triage_path)
        return 1

    output_dir = Path(args.output_dir)
    if not args.dry_run:
        output_dir.mkdir(parents=True, exist_ok=True)

    rows = parse_triage(triage_path.read_text(encoding="utf-8"))
    LOGGER.info("parsed %d rows from %s", len(rows), triage_path)

    modules = list(args.module_owner) if args.module_owner else list(DEFAULT_MODULES)
    written: list[str] = []
    for module in modules:
        body = _build_umbrella_body(module, rows)
        out_path = output_dir / _output_filename_for_module(module)
        if args.dry_run:
            print(f"would write {out_path}")
            continue
        out_path.write_text(body, encoding="utf-8")
        LOGGER.info("wrote %s", out_path)
        written.append(str(out_path))

    if not written:
        return 0
    print(f"Wrote {len(written)} umbrella files under {output_dir}/")
    return 0


if __name__ == "__main__":
    sys.exit(main())
