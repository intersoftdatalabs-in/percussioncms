#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/install-cms-dev.py.

The Maven / java installer invocation is intentionally not exercised; this suite
covers the CLI surface (help + bad flag) plus the pure-function parts:
``build_installer_db_args`` (env-var → CLI-arg mapping) and the env-file loader
(which is pure-Python and cross-platform).
"""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "install-cms-dev.py"


def _run(*args: str, env: dict[str, str] | None = None) -> subprocess.CompletedProcess:
    cmd = [sys.executable, str(SCRIPT), *args]
    full_env = os.environ.copy()
    if env is not None:
        # Wipe PERC_DB_* and DB_* to make assertions deterministic.
        for k in list(full_env.keys()):
            if k.startswith("PERC_DB_") or k.startswith("DB_"):
                full_env.pop(k, None)
        full_env.update(env)
    return subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
        env=full_env,
    )


def test_help_exits_zero_and_prints_usage() -> None:
    result = _run("--help")
    assert result.returncode == 0, result.stderr
    assert "usage:" in result.stdout.lower()


def test_bogus_flag_exits_nonzero() -> None:
    result = _run("--no-such-flag")
    assert result.returncode != 0
    combined = (result.stderr + result.stdout).lower()
    assert "no-such-flag" in combined or "unrecognized" in combined


def test_build_installer_db_args_defaults_to_derby() -> None:
    """The pure helper defaults to derby + ssl enabled/verify=true when no env
    is set; tests import the module to exercise the function directly.
    """
    import importlib.util

    spec = importlib.util.spec_from_file_location("install_cms_dev", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    saved_env = os.environ.copy()
    try:
        for k in list(os.environ.keys()):
            if k.startswith("PERC_DB_") or k.startswith("DB_"):
                os.environ.pop(k, None)
        args = module.build_installer_db_args()
        assert "--db.type=derby" in args
        assert "--db.ssl.enabled=true" in args
        assert "--db.ssl.verify=true" in args
        assert "--db.ssl.allowSelfSigned=false" in args
        # No optional host/port/etc. when env is empty.
        assert not any(a.startswith("--db.host=") for a in args)
    finally:
        os.environ.clear()
        os.environ.update(saved_env)


def test_build_installer_db_args_respects_perc_prefix() -> None:
    import importlib.util

    spec = importlib.util.spec_from_file_location("install_cms_dev", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    saved_env = os.environ.copy()
    try:
        for k in list(os.environ.keys()):
            if k.startswith("PERC_DB_") or k.startswith("DB_"):
                os.environ.pop(k, None)
        os.environ["PERC_DB_HOST"] = "db.example"
        os.environ["PERC_DB_PORT"] = "3306"
        os.environ["PERC_DB_TYPE"] = "mysql"
        args = module.build_installer_db_args()
        assert "--db.type=mysql" in args
        assert "--db.host=db.example" in args
        assert "--db.port=3306" in args
    finally:
        os.environ.clear()
        os.environ.update(saved_env)


def test_missing_env_file_exits_one(tmp_path: Path) -> None:
    """A bogus --env-file path makes the script exit non-zero with a recognizable
    error substring (the bash original's behavior on missing .env.compose)."""
    result = _run("--env-file", str(tmp_path / "does-not-exist.env"))
    assert result.returncode != 0
    combined = (result.stderr + result.stdout).lower()
    assert "not found" in combined or "env.compose" in combined


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
