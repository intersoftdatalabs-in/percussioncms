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
"""Unit tests for scripts/night-timer-run.sh (MODEL passthrough, dry-run)."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
TIMER_RUN = SCRIPT_DIR / "night-timer-run.sh"

PINNED_DEFAULT = "opencode-go/muse-spark-1.3-contributor#medium"


def _dry_run(env: dict[str, str], tmp_path: Path) -> subprocess.CompletedProcess[str]:
    env = dict(env)
    env["LOCK_FILE"] = str(tmp_path / "test-night-issue-prs.lock")
    return subprocess.run(
        ["bash", str(TIMER_RUN), "--dry-run"],
        cwd=REPO_ROOT,
        shell=False,
        check=False,
        timeout=120,
        capture_output=True,
        text=True,
        env=env,
    )


def test_model_env_forwarded_to_launcher(tmp_path: Path) -> None:
    env = os.environ.copy()
    env["MODEL"] = "test-provider/test-model#test-variant"
    proc = _dry_run(env, tmp_path)
    assert proc.returncode == 0, proc.stderr[-2000:]
    combined = proc.stdout + proc.stderr
    assert "--model" in combined
    assert "test-provider/test-model#test-variant" in combined


def test_model_defaults_to_pinned_fast_model(tmp_path: Path) -> None:
    env = os.environ.copy()
    env.pop("MODEL", None)
    proc = _dry_run(env, tmp_path)
    assert proc.returncode == 0, proc.stderr[-2000:]
    combined = proc.stdout + proc.stderr
    assert "--model" in combined
    assert PINNED_DEFAULT in combined


if __name__ == "__main__":
    sys.exit(f"{__name__} is a pytest module; run pytest instead.")
