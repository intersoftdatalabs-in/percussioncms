"""Summary report generator.

Ported from ``scripts/release-audit/lib/report.sh``. Emits a 7-section
Markdown summary for posting to a GitHub issue.
"""
from __future__ import annotations

import json
import logging
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import backlog
import common

LOGGER = logging.getLogger("release_audit.report")


def _top10_backlog(candidates: list[dict[str, Any]]) -> list[tuple[int, dict[str, Any]]]:
    """Return ``[(idx_1based, candidate), ...]`` for the top-10 by priority + date."""
    sorted_candidates = sorted(
        candidates,
        key=lambda c: (
            {"P0": 0, "P1": 1, "P2": 2, "P3": 3}[backlog._priority_for(c)],
            c.get("mergedAt", ""),
        ),
    )
    return list(enumerate(sorted_candidates[:10], start=1))


def run_report_phase(repo_root: Path, output_dir: Path) -> None:
    """Generate ``v8.1.7-to-8.2-migration-report.md``."""
    inventory_path = output_dir / "inventory.json"
    verdicts_path = output_dir / "verdicts.json"
    excluded_path = output_dir / "dependabot-excluded.json"
    report_path = output_dir / "v8.1.7-to-8.2-migration-report.md"
    config_path = output_dir / "_audit_config.json"

    if not inventory_path.is_file() or not verdicts_path.is_file():
        common.log_warn("inventory or verdicts missing; skipping report phase")
        return

    inventory = common.read_json(inventory_path)
    verdicts = common.read_json(verdicts_path)
    excluded = common.read_json(excluded_path) if excluded_path.is_file() else []
    config = common.read_json(config_path) if config_path.is_file() else {}

    from_tag = config.get("fromTag", "?")
    to_tag = config.get("toTag", "?")
    target_branch = config.get("targetBranch", "development")
    run_ts = config.get("runTimestamp") or datetime.now(timezone.utc).isoformat()

    total_inv = len(inventory) if isinstance(inventory, list) else 0
    total_excl = len(excluded) if isinstance(excluded, list) else 0

    verdict_dist = Counter(
        v.get("verdict", "?") for v in verdicts if isinstance(verdicts, list)
    ) if isinstance(verdicts, list) else Counter()

    p0_count = sum(
        1
        for v in verdicts
        if isinstance(v, dict) and v.get("verdict") == "needs-migration" and v.get("securityFlag")
    )
    sec_heur = sum(1 for v in verdicts if isinstance(v, dict) and v.get("securityFlag"))
    empty_modules = sum(
        1
        for pr in inventory
        if isinstance(pr, dict) and not (pr.get("modulePaths") or [])
    )

    candidates = [
        c for c in verdicts if isinstance(c, dict) and c.get("verdict") == "needs-migration"
    ]
    inv_by_number = {pr.get("number"): pr for pr in inventory if isinstance(pr, dict)}
    top10: list[tuple[int, dict[str, Any]]] = []
    for idx, v in _top10_backlog(candidates):
        pr = inv_by_number.get(v.get("prNumber"), {})
        merged = {**v, **pr}
        top10.append((idx, merged))

    lines: list[str] = [
        f"# v8.1.7 → {target_branch} Migration Report",
        "",
        f"**Tag range**: `{from_tag}..{to_tag}`  ",
        f"**Target branch**: `{target_branch}`  ",
        f"**Run timestamp**: {run_ts}",
        "",
        "## TL;DR",
        "",
        f"- **Inventory**: {total_inv} non-dependabot PRs (after excluding {total_excl} dependabot PRs)",
        f"- **Verdict distribution**: {' '.join(f'{k}={v}' for k, v in sorted(verdict_dist.items()))}",
        f"- **P0 (security) backlog items**: {p0_count}",
        "- **Actionable backlog**: see [`migration-backlog.md`](migration-backlog.md)",
        "",
        "## Verdict Distribution",
        "",
        "| Verdict | Count |",
        "|--------|-------|",
    ]
    for verdict, count in sorted(verdict_dist.items()):
        lines.append(f"| {verdict} | {count} |")
    lines.append("")

    lines.append("## Top 10 Backlog Items (by priority)\n")
    for idx, c in top10:
        n = c.get("prNumber") or c.get("number")
        title = (c.get("title") or "")[:80]
        prio = backlog._priority_for(c)
        lines.append(
            f"{idx}. [#{n}](https://github.com/intersoftdatalabs-in/percussioncms/pull/{n}) — {title} _({prio})_"
        )
    lines.append("")

    lines.append("## Exclusions\n")
    lines.append(f"Excluded {total_excl} dependabot PRs (dependency updates, not in scope per FR-002).\n")

    lines.append("## Open Questions / Data Gaps\n")
    lines.append(
        f"- {sec_heur} PRs flagged `securityFlag == true` via filename heuristic; "
        "per-component dependency version comparison (FR-006a) is a follow-up — current verdicts "
        "treat them as `needs-migration` if dev is missing the patched version."
    )
    lines.append(
        f"- {empty_modules} PRs without files-changed data have empty `modulePaths`; "
        "their priority defaults to P3."
    )
    lines.append(
        "- Verdict heuristic uses commit-message tokens; manual review recommended for ambiguous "
        "cases (verdict != `already-present` AND verdict != `conflicts-with-newer-design`).\n"
    )

    lines.append("## Next Steps\n")
    lines.append("1. Review this report and [`migration-backlog.md`](migration-backlog.md).")
    lines.append("2. For each P0 item: assign a porter, open a porting PR per spec US4.")
    lines.append(
        "3. Per Constitution Principle IX, when review comments arrive on porting PRs, "
        "reply inline AND resolve each thread (see root `AGENTS.md`)."
    )
    lines.append("4. Re-run this audit after each v8.x release is tagged.\n")

    common.write_atomic(report_path, "\n".join(lines))
    common.log_info(f"report written: {report_path}")
