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
"""Unit tests for the night-issue-prs timer entry points (MODEL passthrough).

POSIX (`night-timer-run.sh`, needs `flock`) is executed only off Windows;
Windows (`night-timer-run.cmd`) is executed only on Windows, with a static
content test covering the .cmd on every platform.
"""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
TIMER_RUN_SH = SCRIPT_DIR / "night-timer-run.sh"
TIMER_RUN_CMD = SCRIPT_DIR / "night-timer-run.cmd"

PINNED_DEFAULT = "opencode-go/muse-spark-1.3-contributor#medium"

POSIX_ONLY = pytest.mark.skipif(
    os.name == "nt",
    reason="night-timer-run.sh is POSIX-only (flock); Windows uses night-timer-run.cmd",
)
WINDOWS_ONLY = pytest.mark.skipif(
    os.name != "nt",
    reason="night-timer-run.cmd executes only on Windows",
)


def _dry_run_sh(env: dict[str, str], tmp_path: Path) -> subprocess.CompletedProcess[str]:
    env = dict(env)
    env["LOCK_FILE"] = str(tmp_path / "test-night-issue-prs.lock")
    return subprocess.run(
        ["bash", str(TIMER_RUN_SH), "--dry-run"],
        cwd=REPO_ROOT,
        shell=False,
        check=False,
        timeout=120,
        capture_output=True,
        text=True,
        env=env,
    )


@POSIX_ONLY
def test_model_env_forwarded_to_launcher(tmp_path: Path) -> None:
    env = os.environ.copy()
    env["MODEL"] = "test-provider/test-model#test-variant"
    proc = _dry_run_sh(env, tmp_path)
    assert proc.returncode == 0, proc.stderr[-2000:]
    combined = proc.stdout + proc.stderr
    assert "--model" in combined
    assert "test-provider/test-model#test-variant" in combined


@POSIX_ONLY
def test_model_defaults_to_pinned_fast_model(tmp_path: Path) -> None:
    env = os.environ.copy()
    env.pop("MODEL", None)
    proc = _dry_run_sh(env, tmp_path)
    assert proc.returncode == 0, proc.stderr[-2000:]
    combined = proc.stdout + proc.stderr
    assert "--model" in combined
    assert PINNED_DEFAULT in combined


def test_cmd_pins_model_and_forwards_it() -> None:
    """Static parity check for the Windows entry point (runs everywhere)."""
    text = TIMER_RUN_CMD.read_text(encoding="utf-8")
    assert f'set "MODEL={PINNED_DEFAULT}"' in text
    assert "--model %MODEL%" in text


@WINDOWS_ONLY
def test_cmd_dry_run_forwards_model(tmp_path: Path) -> None:
    env = os.environ.copy()
    env["MODEL"] = "test-provider/test-model#test-variant"
    proc = subprocess.run(
        ["cmd", "/c", str(TIMER_RUN_CMD), "--dry-run"],
        cwd=REPO_ROOT,
        shell=False,
        check=False,
        timeout=120,
        capture_output=True,
        text=True,
        env=env,
    )
    assert proc.returncode == 0, (proc.stdout + proc.stderr)[-2000:]
    combined = proc.stdout + proc.stderr
    assert "--model" in combined
    assert "test-provider/test-model#test-variant" in combined


if __name__ == "__main__":
    sys.exit(f"{__name__} is a pytest module; run pytest instead.")
