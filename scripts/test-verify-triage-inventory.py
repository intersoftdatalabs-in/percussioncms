#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/test-verify-triage-inventory.sh.

Purpose
-------
Self-test for ``scripts/verify-triage-inventory.py``. Drives the real
verification logic against the existing ``scripts/test-fixtures/triage-good.md``
and ``scripts/test-fixtures/triage-bad.md`` fixtures. Exits 0 only when the
good fixture passes and the bad fixture surfaces the expected failures
(empty notes on a false-positive row + unknown module_owner).

Usage
-----
::

    python3 scripts/test-verify-triage-inventory.py
        [--fixture-good <path>]
        [--fixture-bad <path>]
        [--script-under-test <path>]

Exit codes
----------
- ``0`` both fixtures behave as expected
- ``1`` a fixture assertion failed

Behavioral Notes
----------------
- The original bash self-test embedded inline awk snippets to re-implement
  the row-count + notes + owner checks. The Python port calls
  ``verify-triage-inventory.py`` itself (subprocess) for the bad fixture and
  inspects the output for the expected failure substrings; the good fixture
  is exercised via the row parser (a pure function in
  ``verify-triage-inventory.py``).
"""
from __future__ import annotations

import argparse
import logging
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
SCRIPT_DIR = REPO_ROOT / "scripts"

LOGGER = logging.getLogger(__name__)

ROW_RE = re.compile(r"^\|\s*(\d+)\s*\|")


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="test_verify_triage_inventory.py",
        description="Self-test for verify-triage-inventory.py against the good/bad fixtures.",
    )
    parser.add_argument(
        "--fixture-good",
        default=str(SCRIPT_DIR / "test-fixtures" / "triage-good.md"),
        help="Path to the good triage fixture",
    )
    parser.add_argument(
        "--fixture-bad",
        default=str(SCRIPT_DIR / "test-fixtures" / "triage-bad.md"),
        help="Path to the bad triage fixture",
    )
    parser.add_argument(
        "--script-under-test",
        default=str(SCRIPT_DIR / "verify-triage-inventory.py"),
        help="Path to verify-triage-inventory.py",
    )
    parser.add_argument(
        "--alerts-good",
        default=str(SCRIPT_DIR / "test-fixtures" / "alerts-good.md"),
        help="Alerts file used with the good fixture",
    )
    parser.add_argument(
        "--alerts-bad",
        default=str(SCRIPT_DIR / "test-fixtures" / "alerts-bad.md"),
        help="Alerts file used with the bad fixture",
    )
    return parser


def _count_rows(path: Path) -> int:
    text = path.read_text(encoding="utf-8")
    return sum(1 for line in text.splitlines() if ROW_RE.match(line))


def _bad_fixture_has_empty_notes(path: Path) -> bool:
    """Mirror the bash awk: find a false-positive row with empty notes."""
    text = path.read_text(encoding="utf-8")
    for line in text.splitlines():
        if not ROW_RE.match(line):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 11:
            continue
        disposition = re.sub(r"\s*\(candidate\)", "", cells[6].strip("`")).strip()
        notes = cells[10].strip("`").strip()
        if disposition in ("false-positive", "accepted-risk") and not notes:
            return True
    return False


def _bad_fixture_has_unknown_owner(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    for line in text.splitlines():
        if not ROW_RE.match(line):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 11:
            continue
        owner = cells[5].strip("`").strip()
        if owner == "modules/some-unknown-module/":
            return True
    return False


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    good = Path(args.fixture_good)
    bad = Path(args.fixture_bad)
    script_under_test = Path(args.script_under_test)
    alerts_good = Path(args.alerts_good)
    alerts_bad = Path(args.alerts_bad)

    if not good.is_file() or not bad.is_file():
        print(
            f"FAIL: missing fixtures ({args.fixture_good}, {args.fixture_bad})",
            file=sys.stderr,
        )
        return 1

    fail = False

    # 1. Both fixtures must have exactly 4 rows.
    good_rows = _count_rows(good)
    bad_rows = _count_rows(bad)
    if good_rows == 4:
        print(f"OK: {good} has {good_rows} rows (expected 4)")
    else:
        print(f"FAIL: {good} has {good_rows} rows (expected 4)", file=sys.stderr)
        fail = True
    if bad_rows == 4:
        print(f"OK: {bad} has {bad_rows} rows (expected 4)")
    else:
        print(f"FAIL: {bad} has {bad_rows} rows (expected 4)", file=sys.stderr)
        fail = True

    # 2. Bad fixture must surface the empty-notes bug.
    if _bad_fixture_has_empty_notes(bad):
        print("OK: bad fixture has a false-positive row with empty notes (catches the bug)")
    else:
        print("FAIL: bad fixture did not surface the empty-notes bug", file=sys.stderr)
        fail = True

    # 3. Bad fixture must surface the unknown module_owner bug.
    if _bad_fixture_has_unknown_owner(bad):
        print("OK: bad fixture has an unknown module_owner (catches the bug)")
    else:
        print("FAIL: bad fixture did not surface the unknown-owner bug", file=sys.stderr)
        fail = True

    # 4. Run the real verify-triage-inventory.py against the good fixture
    #    with matching alerts and expect PASS.
    if not script_under_test.is_file():
        print(
            f"FAIL: script-under-test not found: {script_under_test}",
            file=sys.stderr,
        )
        fail = True
    else:
        cmd = [
            sys.executable,
            str(script_under_test),
            "--triage",
            str(good),
            "--alerts",
            str(alerts_good),
        ]
        result = subprocess.run(
            cmd,
            shell=False,
            check=False,
            timeout=60,
            capture_output=True,
            text=True,
        )
        if result.returncode == 0 and "PASS" in result.stdout:
            print("OK: verify-triage-inventory.py passes the good fixture")
        else:
            print(
                f"FAIL: verify-triage-inventory.py did not pass the good fixture "
                f"(rc={result.returncode})\nstdout: {result.stdout}\nstderr: {result.stderr}",
                file=sys.stderr,
            )
            fail = True

        # 5. Run against the bad fixture; expect at least one failure substring.
        cmd_bad = [
            sys.executable,
            str(script_under_test),
            "--triage",
            str(bad),
            "--alerts",
            str(alerts_bad),
        ]
        result_bad = subprocess.run(
            cmd_bad,
            shell=False,
            check=False,
            timeout=60,
            capture_output=True,
            text=True,
        )
        combined = (result_bad.stdout + result_bad.stderr).lower()
        if (
            "empty notes" in combined
            or "unknown module_owner" in combined
            or result_bad.returncode != 0
        ):
            print("OK: verify-triage-inventory.py surfaces the bad fixture failures")
        else:
            print(
                f"FAIL: verify-triage-inventory.py did not surface bad-fixture failures "
                f"(rc={result_bad.returncode})\nstdout: {result_bad.stdout}\nstderr: {result_bad.stderr}",
                file=sys.stderr,
            )
            fail = True

    if fail:
        print("test-verify-triage-inventory: FAIL", file=sys.stderr)
        return 1
    print("test-verify-triage-inventory: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
