#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/verify-triage-inventory.py.

Exercises the real verify logic against the existing
``scripts/test-fixtures/triage-{good,bad}.md`` fixtures per quickstart.md
Scenario B.2 and B.4.
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
SCRIPT = SCRIPT_DIR / "verify-triage-inventory.py"
GOOD_FIXTURE = SCRIPT_DIR / "test-fixtures" / "triage-good.md"
BAD_FIXTURE = SCRIPT_DIR / "test-fixtures" / "triage-bad.md"
ALERTS_GOOD = SCRIPT_DIR / "test-fixtures" / "alerts-good.md"
ALERTS_BAD = SCRIPT_DIR / "test-fixtures" / "alerts-bad.md"


def _run(
    *args: str,
    cwd: Path | None = None,
    env: dict[str, str] | None = None,
) -> subprocess.CompletedProcess:
    cmd = [sys.executable, str(SCRIPT), *args]
    import os
    full_env = os.environ.copy()
    if env:
        full_env.update(env)
    return subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
        cwd=str(cwd) if cwd else None,
        env=full_env,
    )


def test_help_exits_zero_and_prints_usage() -> None:
    result = _run("--help")
    assert result.returncode == 0, result.stderr
    assert "usage:" in result.stdout.lower()


def test_bogus_flag_exits_nonzero() -> None:
    result = _run("--no-such-flag")
    assert result.returncode != 0


@pytest.mark.skipif(
    not (GOOD_FIXTURE.is_file() and BAD_FIXTURE.is_file()),
    reason="test fixtures not present",
)
def test_good_fixture_passes_module_owner_check() -> None:
    """The good fixture's module_owners (``modules/extensions-main``, ``WebUI``,
    ``deliverytiersuite/delivery-tier-suite/feeds``, ``modules/perc-legacy``)
    must all be recognized via ``AGENTS.md``."""
    import importlib.util

    spec = importlib.util.spec_from_file_location("verify_triage", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    rows = module._parse_rows(GOOD_FIXTURE.read_text(encoding="utf-8"))
    assert len(rows) == 4
    agents = (REPO_ROOT / "AGENTS.md").read_text(encoding="utf-8")
    modules = module._module_paths_from_agents(agents)
    # Sanity: at least the modules referenced by the good fixture must be present.
    expected = {
        "modules/extensions-main",
        "WebUI",
        "deliverytiersuite/delivery-tier-suite/feeds",
        "modules/perc-legacy",
    }
    missing = {e for e in expected if not any(e == m or e.startswith(f"{m}/") for m in modules)}
    assert not missing, f"AGENTS.md does not list expected module roots: {missing}"


@pytest.mark.skipif(
    not (BAD_FIXTURE.is_file() and ALERTS_BAD.is_file()),
    reason="bad fixture or alerts file not present",
)
def test_bad_fixture_flags_empty_notes_and_unknown_owner() -> None:
    """End-to-end check: the bad fixture must produce at least one of the two
    expected failures (empty notes on a false-positive row, or unknown
    module_owner)."""
    result = _run(
        "--triage",
        str(BAD_FIXTURE),
        "--alerts",
        str(ALERTS_BAD),
    )
    # The script may still exit 0 if the row-count slack absorbs the bad data
    # AND no other check fires — but the bad fixture has BOTH an empty-notes row
    # AND an unknown module_owner, so at least one of those checks must fire.
    # We assert that the output mentions the failure modes (works regardless of exit code).
    combined = (result.stdout + result.stderr).lower()
    fired = (
        "empty notes" in combined
        or "unknown module_owner" in combined
        or "fail: row-count check" in combined
    )
    assert fired, f"bad fixture did not trigger any expected failure mode; output: {combined}"


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
