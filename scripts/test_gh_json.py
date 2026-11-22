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
"""Unit tests for scripts/gh-json.py (fake gh, no network).

Regression cover for issue #6986: gh 2.105.0 prints a multi-line
``gh event listen ...`` hint to stderr after ``gh pr view``; merging
streams with ``2>&1`` feeds the hint to the JSON parser. The wrapper
must keep stdout pure JSON, leave stderr unmerged, and fail loudly on
a non-zero gh exit.
"""
from __future__ import annotations

import json
import os
import stat
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
GH_JSON = SCRIPT_DIR / "gh-json.py"

HINT = (
    "hint: if you need to wait for this pull request to be reviewed, "
    "merged, or closed before continuing, use this instead of polling:\n"
    "    gh event listen --resource https://example.invalid/pr/1 "
    "--event com.github.pull_request.review.submitted.v1\n"
)
PAYLOAD = '{"number":7014}\n'


def _install_fake_gh(tmp_path: Path) -> dict[str, str]:
    """Put a fake ``gh`` first on PATH; mode via ``FAKE_GH_MODE`` env.

    Modes: ``hint`` (JSON stdout + hint stderr, exit 0),
    ``fail`` (error stderr, empty stdout, exit 1).
    """
    if os.name == "nt":
        fake = tmp_path / "gh.cmd"
        fake.write_text(
            "@echo off\n"
            'if "%FAKE_GH_MODE%"=="fail" (\n'
            "  echo gh: something went wrong 1>&2\n"
            "  exit /b 1\n"
            ")\n"
            f"echo {PAYLOAD.strip()}\n"
            f"echo {HINT.splitlines()[0]} 1>&2\n",
            encoding="utf-8",
        )
    else:
        fake = tmp_path / "gh"
        fake.write_text(
            "#!/bin/sh\n"
            'if [ "$FAKE_GH_MODE" = "fail" ]; then\n'
            "  echo 'gh: something went wrong' >&2\n"
            "  exit 1\n"
            "fi\n"
            f"printf '%s' '{PAYLOAD}'\n"
            f"printf '%s' '{HINT}' >&2\n",
            encoding="utf-8",
        )
        fake.chmod(fake.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    env = os.environ.copy()
    env["PATH"] = str(tmp_path) + os.pathsep + env.get("PATH", "")
    return env


def _run(env: dict[str, str], *args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(GH_JSON), *args],
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
        env=env,
    )


def _load_gh_json_module():
    """Import ``scripts/gh-json.py`` (hyphenated name) for unit-level checks."""
    import importlib.util

    spec = importlib.util.spec_from_file_location("gh_json_under_test", GH_JSON)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_resolve_gh_honors_pathext_cmd_shim(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    """Windows lookup must honor a ``PATHEXT`` ``gh.cmd`` shim (BLOCK, #6986).

    Regression proof for the Windows-only CI red: a ``CreateProcess``-style
    bare-``gh`` search appends ``.exe`` only, so a ``gh.cmd`` earlier on
    ``PATH`` is skipped and the real ``gh`` runs instead (exit 4 without
    ``GH_TOKEN``). ``resolve_gh`` must find the ``gh.cmd`` shim via
    ``PATHEXT``. ``nt`` is simulated so this proves the Windows code path
    on any host — the ``.cmd`` file is only resolved here, never executed;
    the real Windows CI run proves execution.
    """
    fake_cmd = tmp_path / "gh.cmd"
    fake_cmd.write_text("@echo off\n", encoding="utf-8")
    monkeypatch.setattr(os, "name", "nt")
    monkeypatch.setenv("PATH", str(tmp_path))
    monkeypatch.setenv("PATHEXT", ".COM;.EXE;.BAT;.CMD")
    gh_json = _load_gh_json_module()
    # String compare (not ``Path``): ``Path`` dispatches on the patched
    # ``os.name`` and would build a ``WindowsPath`` on a POSIX host.
    assert gh_json.resolve_gh() == str(fake_cmd)


def test_canonicalize_on_disk_restores_true_entry_case(tmp_path: Path) -> None:
    """``_canonicalize_on_disk`` maps PATHEXT case back to on-disk case.

    Windows ``shutil.which("gh")`` returns the path built with PATHEXT
    case (``gh.CMD``) rather than the on-disk name (``gh.cmd``). This
    proves the canonicalizer on any host by feeding it the upper-case
    spelling directly — no ``nt`` simulation needed.
    """
    fake_cmd = tmp_path / "gh.cmd"
    fake_cmd.write_text("@echo off\n", encoding="utf-8")
    gh_json = _load_gh_json_module()
    miscased = os.path.join(str(tmp_path), "GH.CMD")
    assert gh_json._canonicalize_on_disk(miscased) == str(fake_cmd)


def test_stdout_stays_pure_json_despite_stderr_hint(tmp_path: Path) -> None:
    """The gh 2.105 stderr hint must not leak into the JSON stream (#6986)."""
    env = _install_fake_gh(tmp_path)
    env["FAKE_GH_MODE"] = "hint"
    proc = _run(env, "pr", "view", "7014", "--json", "number")
    assert proc.returncode == 0, proc.stderr[-2000:]
    assert proc.stdout == PAYLOAD
    assert json.loads(proc.stdout) == {"number": 7014}
    assert "hint:" in proc.stderr  # still visible, just not merged


def test_nonzero_exit_fails_loudly_with_same_code(tmp_path: Path) -> None:
    """A failing gh must not surface as a silent empty stdout."""
    env = _install_fake_gh(tmp_path)
    env["FAKE_GH_MODE"] = "fail"
    proc = _run(env, "pr", "view", "7014", "--json", "number")
    assert proc.returncode == 1
    assert proc.stdout == ""
    assert "gh-json:" in proc.stderr
    assert "exit 1" in proc.stderr


def test_missing_gh_fails_loudly(tmp_path: Path) -> None:
    """No gh on PATH is exit 127 with a clear message, not a traceback."""
    env = os.environ.copy()
    env["PATH"] = str(tmp_path)  # empty dir: no gh anywhere
    proc = _run(env, "issue", "view", "1", "--json", "number")
    assert proc.returncode == 127
    assert "gh-json:" in proc.stderr
    assert "Traceback" not in proc.stderr


def test_help_usage(tmp_path: Path) -> None:
    """--help prints usage on stdout and exits 0."""
    env = _install_fake_gh(tmp_path)
    proc = _run(env, "--help")
    assert proc.returncode == 0
    assert "usage:" in proc.stdout


def test_no_args_is_usage_error() -> None:
    """Bare invocation is a usage error, never an accidental gh call."""
    proc = _run(os.environ.copy())
    assert proc.returncode == 2
    assert "usage:" in proc.stdout
