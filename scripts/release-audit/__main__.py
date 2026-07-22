"""CLI dispatcher for the release-audit pipeline.

Usage
-----
::

    python3 scripts/release-audit/__main__.py [--from-tag TAG] [--to-tag TAG]
                                              [--target-branch BRANCH]
                                              [--output-dir DIR]
                                              [--report PATH] [--inventory PATH]
                                              [--strict] [--include-dependabot]
                                              {inventory,verdicts,backlog,report,port,all}

Behavior
--------
- With no subcommand, runs the full pipeline (collect -> classify -> enrich ->
  verdicts -> backlog -> report).
- With a subcommand, runs only that phase.
- ``--report`` and ``--inventory`` set the output paths (per the contract).
- ``--strict`` upgrades row-count warnings to failures (triage parity with
  ``verify-triage-inventory.py --strict``).

Notes
-----
- The package directory is ``scripts/release-audit/`` (per the contract) which
  contains a dash and therefore cannot be imported as ``python -m
  release_audit`` (Python identifier rules). The supported invocation is
  ``python3 scripts/release-audit/__main__.py ...`` which is portable across
  Linux, macOS, and Windows.
"""
from __future__ import annotations

import argparse
import json
import logging
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

# The package directory contains a dash; relative imports ``from . import x``
# do not work when the directory is invoked as a script. Use flat imports
# (each sibling module is in the same directory as this __main__.py).
import backlog
import common
import inventory
import port
import report
import verdicts

LOGGER = logging.getLogger("release_audit")

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_REPORT = REPO_ROOT / "docs" / "ai-generated" / "release-audit.md"
DEFAULT_INVENTORY = REPO_ROOT / "docs" / "ai-generated" / "release-inventory.md"

DEFAULT_FROM_TAG = "v8.1.6"
DEFAULT_TO_TAG = "v8.1.7"
DEFAULT_TARGET_BRANCH = "development"


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="release-audit",
        description="v8.1.x -> 8.2 migration audit pipeline (cross-platform).",
    )
    parser.add_argument(
        "--report",
        default=str(DEFAULT_REPORT),
        help="Output report path (default: docs/ai-generated/release-audit.md)",
    )
    parser.add_argument(
        "--inventory",
        default=str(DEFAULT_INVENTORY),
        help="Inventory file path (default: docs/ai-generated/release-inventory.md)",
    )
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Treat warnings as failures",
    )
    parser.add_argument(
        "--from-tag",
        default=DEFAULT_FROM_TAG,
        help="Lower bound tag (default: v8.1.6)",
    )
    parser.add_argument(
        "--to-tag",
        default=DEFAULT_TO_TAG,
        help="Upper bound tag (default: v8.1.7)",
    )
    parser.add_argument(
        "--target-branch",
        default=DEFAULT_TARGET_BRANCH,
        help="Branch to compare against (default: development)",
    )
    parser.add_argument(
        "--output-dir",
        default=None,
        help="Pipeline output directory (default: ./tmp/release-audit/<from>..<to>)",
    )
    parser.add_argument(
        "--include-dependabot",
        action="store_true",
        help="Include dependabot PRs in the inventory (flagged but not excluded)",
    )
    parser.add_argument(
        "--skip-origin-check",
        action="store_true",
        help="Skip the `origin` reachability check (test-only; not for production use)",
    )
    parser.add_argument(
        "subcommand",
        nargs="?",
        choices=("inventory", "verdicts", "backlog", "report", "port", "all"),
        default="all",
        help="Pipeline subcommand (default: all phases)",
    )
    return parser


def _resolve_output_dir(args: argparse.Namespace) -> Path:
    if args.output_dir:
        return Path(args.output_dir)
    return Path(f"./tmp/release-audit/{args.from_tag}..{args.to_tag}")


def _write_audit_config(output_dir: Path, args: argparse.Namespace, from_sha: str, to_sha: str) -> None:
    config = {
        "fromTag": args.from_tag,
        "toTag": args.to_tag,
        "fromSha": from_sha,
        "toSha": to_sha,
        "targetBranch": args.target_branch,
        "includeDependabot": args.include_dependabot,
        "outputDir": str(output_dir),
        "runTimestamp": datetime.now(timezone.utc).isoformat(),
    }
    common.write_json(output_dir / "_audit_config.json", config)


def _run_all(args: argparse.Namespace, output_dir: Path) -> int:
    if not args.skip_origin_check:
        common.require_origin(REPO_ROOT)
    from_sha = common.require_tag(REPO_ROOT, args.from_tag)
    to_sha = common.require_tag(REPO_ROOT, args.to_tag)
    _write_audit_config(output_dir, args, from_sha, to_sha)

    inventory.run_inventory_phase(
        REPO_ROOT,
        output_dir,
        args.from_tag,
        args.to_tag,
        args.include_dependabot,
    )
    verdicts.run_verdicts_phase(REPO_ROOT, output_dir, args.target_branch)
    backlog.run_backlog_phase(REPO_ROOT, output_dir)
    report.run_report_phase(REPO_ROOT, output_dir)
    common.log_info(f"Pipeline complete; outputs in {output_dir}/")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    output_dir = _resolve_output_dir(args)
    common.ensure_output_dir(output_dir)

    if args.subcommand == "all":
        return _run_all(args, output_dir)
    if args.subcommand == "inventory":
        if not args.skip_origin_check:
            common.require_origin(REPO_ROOT)
        inventory.run_inventory_phase(
            REPO_ROOT,
            output_dir,
            args.from_tag,
            args.to_tag,
            args.include_dependabot,
        )
        return 0
    if args.subcommand == "verdicts":
        verdicts.run_verdicts_phase(REPO_ROOT, output_dir, args.target_branch)
        return 0
    if args.subcommand == "backlog":
        backlog.run_backlog_phase(REPO_ROOT, output_dir)
        return 0
    if args.subcommand == "report":
        report.run_report_phase(REPO_ROOT, output_dir)
        return 0
    if args.subcommand == "port":
        common.log_info("port subcommand is interactive; see PORTING.md for the workflow")
        return 0
    parser.error(f"unknown subcommand: {args.subcommand}")
    return 2


if __name__ == "__main__":
    sys.exit(main())
