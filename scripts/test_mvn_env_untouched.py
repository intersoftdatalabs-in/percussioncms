#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""US1 regression sentinel for spec 994-python-build-scripts.

Per Clarification Q2, ``mvn-env.sh`` and ``mvn-env.bat`` are EXPLICITLY EXCLUDED
from the migration to Python because they already work cross-platform. This
pytest module enforces that exclusion: it fails if either file is deleted from
the working tree, if its size drops below the pre-spec baseline, or if the
expected pre-spec content snippet is missing.

Tested: SC-004 ("mvn-env.sh and mvn-env.bat continue to exist on development
and continue to behave exactly as before").

This test is self-contained — no network, no Maven, no Docker. It runs on
Linux and Windows identically. Keep it that way.

Behavioral Notes
----------------
- Pre-spec content snippets are matched as raw byte substrings; the test does
  not re-parse the shell. If ``mvn-env.sh`` is rewritten for unrelated reasons
  (e.g. JDK 25 support) but still preserves the matched snippets, the test
  passes.
- The "minimum size" guard is sized for the current ``mvn-env.sh`` (~1.4 KiB,
  46 lines) and ``mvn-env.bat`` (~0.9 KiB, 33 lines) as observed on the
  development branch on 2026-07-21. If a future change adds substantial new
  content, bump the constants below.
- Snippet fingerprints updated to the actual current file content (2026-07-21);
  see git log for any earlier shape.
"""

from __future__ import annotations

import re
from pathlib import Path

import pytest

REPO_ROOT = Path(__file__).resolve().parent.parent

MVN_ENV_SH = REPO_ROOT / "mvn-env.sh"
MVN_ENV_BAT = REPO_ROOT / "mvn-env.bat"

# Pre-spec content fingerprints taken from the actual files on 2026-07-21.
EXPECTED_SH_SNIPPETS = (
    "#!/bin/bash",
    "# Environment setup script for Linux/macOS",
    "Sets JAVA_HOME to JAVA_HOME_21 for JDK 21 compatibility",
    'if [[ -z "${JAVA_HOME_21}" ]]; then',
    'export JAVA_HOME="${JAVA_HOME_21}"',
    'exec "$SCRIPT_DIR/mvnw" -Djava.io.tmpdir="$TMP_DIR" "$@"',
)

EXPECTED_BAT_SNIPPETS = (
    "@echo off",
    "REM Environment setup script for Windows",
    "Sets JAVA_HOME to JAVA_HOME_21 for JDK 21 compatibility",
    'if "%JAVA_HOME_21%"=="" (',
    "set JAVA_HOME=%JAVA_HOME_21%",
    'call "%SCRIPT_DIR%mvnw.cmd" -Djava.io.tmpdir="%TMP_DIR%" %*',
)

# Minimum byte sizes — guards against silent truncation / accidental rewrite.
MIN_SH_BYTES = 1200
MIN_BAT_BYTES = 700


def _read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def test_mvn_env_sh_exists() -> None:
    assert MVN_ENV_SH.is_file(), (
        "mvn-env.sh must not be deleted by spec 994 "
        "(per Clarification Q2 — file already cross-platform)."
    )


def test_mvn_env_bat_exists() -> None:
    assert MVN_ENV_BAT.is_file(), (
        "mvn-env.bat must not be deleted by spec 994 "
        "(per Clarification Q2 — file already cross-platform)."
    )


def test_mvn_env_sh_size_floor() -> None:
    size = MVN_ENV_SH.stat().st_size
    assert size >= MIN_SH_BYTES, (
        f"mvn-env.sh shrank to {size} bytes (floor: {MIN_SH_BYTES}). "
        f"Either the file was truncated or the floor needs to be bumped "
        f"after a deliberate content addition."
    )


def test_mvn_env_bat_size_floor() -> None:
    size = MVN_ENV_BAT.stat().st_size
    assert size >= MIN_BAT_BYTES, (
        f"mvn-env.bat shrank to {size} bytes (floor: {MIN_BAT_BYTES}). "
        f"Either the file was truncated or the floor needs to be bumped "
        f"after a deliberate content addition."
    )


@pytest.mark.parametrize("snippet", EXPECTED_SH_SNIPPETS)
def test_mvn_env_sh_contains_expected_snippet(snippet: str) -> None:
    content = _read_text(MVN_ENV_SH)
    assert snippet in content, (
        f"mvn-env.sh missing expected pre-spec snippet: {snippet!r}. "
        f"This sentinel enforces Clarification Q2 (do not rewrite the file)."
    )


@pytest.mark.parametrize("snippet", EXPECTED_BAT_SNIPPETS)
def test_mvn_env_bat_contains_expected_snippet(snippet: str) -> None:
    content = _read_text(MVN_ENV_BAT)
    assert snippet in content, (
        f"mvn-env.bat missing expected pre-spec snippet: {snippet!r}. "
        f"This sentinel enforces Clarification Q2 (do not rewrite the file)."
    )


def test_mvn_env_sh_bash_block_balance() -> None:
    """Lightweight balance check — count ``if``/``fi`` open/close pairs.

    Not a full bash parse (would require running ``bash -n``); a coarse
    tripwire for accidental corruption that preserves all expected snippets.
    """
    content = _read_text(MVN_ENV_SH)
    if_count = len(re.findall(r"^\s*if\b", content, flags=re.MULTILINE))
    fi_count = len(re.findall(r"^\s*fi\b", content, flags=re.MULTILINE))
    assert if_count == fi_count, (
        f"mvn-env.sh has unbalanced if/fi: {if_count} if vs {fi_count} fi"
    )


def test_mvn_env_bat_if_open_close_balance() -> None:
    """Coarse balance check on ``if (...) (`` open and matching ``)`` close.

    Counts top-level block opens (lines matching ``if ... (``) and standalone
    close-paren lines. Heuristic; the actual control flow is parsed by Windows
    cmd.exe at runtime.
    """
    content = _read_text(MVN_ENV_BAT)
    opens = len(re.findall(r"^\s*if\b.*\(\s*$", content, flags=re.MULTILINE))
    closes = len(re.findall(r"^\s*\)\s*$", content, flags=re.MULTILINE))
    assert opens == closes, (
        f"mvn-env.bat has unbalanced if/close: {opens} if-blocks vs {closes} ) lines"
    )
