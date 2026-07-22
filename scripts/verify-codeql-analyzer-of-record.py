#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/verify-codeql-analyzer-of-record.sh.

Purpose
-------
Verify CodeQL analyzer-of-record policy for ``intersoftdatalabs-in/percussioncms``.
Fails non-zero if default setup is re-enabled or required files are missing.
Per ``docs/ai-generated/tasks/gh-codeql-alerts/codeql-pr-playbook.md``.

Usage
-----
::

    python3 scripts/verify_codeql_analyzer_of_record.py
        [--repo OWNER/REPO]
        [--workflow <path>]

Behavioral Notes
----------------
- bash ``grep -qE`` checks are replaced by Python's ``re.search`` (no shell).
- The ``gh api ... default-setup`` check is gated on ``gh`` being installed
  (matches bash behavior). When gh is absent the script emits a WARN and
  continues; the test surface asserts the script still returns 0 on the local
  workflow / config files (the gh check is the only WARN path).
"""
from __future__ import annotations

import argparse
import json
import logging
import re
import shutil
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

WORKFLOW = REPO_ROOT / ".github" / "workflows" / "codeql.yml"
CONFIG = REPO_ROOT / ".github" / "codeql" / "codeql-config.yml"
MODEL_PACK = REPO_ROOT / ".github" / "codeql" / "models" / "codeql-pack.yml"
PLAYBOOK = REPO_ROOT / "docs" / "ai-generated" / "tasks" / "gh-codeql-alerts" / "codeql-pr-playbook.md"


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify_codeql_analyzer_of_record.py",
        description="Verify CodeQL analyzer-of-record config and default-setup state.",
    )
    parser.add_argument(
        "--repo",
        default="intersoftdatalabs-in/percussioncms",
        help="owner/repo for the gh API check (default: intersoftdatalabs-in/percussioncms)",
    )
    parser.add_argument(
        "--workflow",
        default=str(WORKFLOW.relative_to(REPO_ROOT)),
        help="Path to the advanced CodeQL workflow (default: .github/workflows/codeql.yml)",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    workflow = REPO_ROOT / args.workflow
    print(f"== CodeQL analyzer-of-record check (repo={args.repo}) ==")

    err = 0

    if not workflow.is_file():
        print(f"FAIL: missing {workflow}")
        err = 1
    else:
        wf_text = workflow.read_text(encoding="utf-8")
        if re.search(r"^\s*pull_request:", wf_text, flags=re.MULTILINE):
            print("OK: advanced workflow has pull_request trigger")
        else:
            print(
                "FAIL: advanced workflow missing pull_request trigger "
                "(PR analyzer will not run with config)"
            )
            err = 1
        if "config-file: ./.github/codeql/codeql-config.yml" in wf_text:
            print("OK: advanced workflow uses codeql-config.yml")
        else:
            print("FAIL: advanced workflow not wired to codeql-config.yml")
            err = 1

    for required in (CONFIG, MODEL_PACK, PLAYBOOK):
        rel = required.relative_to(REPO_ROOT)
        if required.is_file():
            print(f"OK: present {rel}")
        else:
            print(f"FAIL: missing {rel}")
            err = 1

    if CONFIG.is_file():
        if re.search(r"^\s*packs:", CONFIG.read_text(encoding="utf-8"), flags=re.MULTILINE):
            print("OK: codeql-config.yml declares packs")
        else:
            print("FAIL: codeql-config.yml missing packs: (model pack not loaded)")
            err = 1

    if shutil.which("gh") is not None:
        cmd = [
            "gh",
            "api",
            f"repos/{args.repo}/code-scanning/default-setup",
            "--jq",
            ".state",
        ]
        result = subprocess.run(
            cmd,
            shell=False,
            check=False,
            timeout=60,
            capture_output=True,
            text=True,
        )
        state = result.stdout.strip() if result.returncode == 0 else "error"
        if state == "not-configured":
            print("OK: default CodeQL setup is not-configured")
        else:
            print(
                f"FAIL: default CodeQL setup state is {state!r} (expected not-configured)"
            )
            print("      Fix: gh api --method PATCH repos/{args.repo}/code-scanning/default-setup -f state=not-configured")
            err = 1
    else:
        print("WARN: gh not installed; skipped default-setup API check")

    if err:
        print("== RESULT: FAIL ==")
        return 1
    print("== RESULT: PASS ==")
    return 0


if __name__ == "__main__":
    sys.exit(main())
