#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/verify-suppressions.py.

Exercises the row parser and the file-suppression checker against an in-memory
suppressions.md + a tiny Java file containing the expected ``// codeql[...]``
comment. The ``missing-triage`` and help paths cover the CLI surface.
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "verify-suppressions.py"


def _run(*args: str) -> subprocess.CompletedProcess:
    cmd = [sys.executable, str(SCRIPT), *args]
    return subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=60,
        capture_output=True,
        text=True,
    )


def test_help_exits_zero_and_prints_usage() -> None:
    result = _run("--help")
    assert result.returncode == 0, result.stderr
    assert "usage:" in result.stdout.lower()


def test_missing_suppressions_exits_nonzero(tmp_path: Path) -> None:
    result = _run("--suppressions", str(tmp_path / "does-not-exist.md"))
    assert result.returncode != 0
    assert "not found" in (result.stderr + result.stdout).lower()


def test_parse_suppression_rows_extracts_columns(tmp_path: Path) -> None:
    import importlib.util

    spec = importlib.util.spec_from_file_location("verify_supp", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    text = (
        "# Suppressions\n\n"
        "| # | alert_id | rule_id | file_path | line | justification | applied_on | applied_by | review_by |\n"
        "|---|----------|---------|-----------|------|---------------|------------|------------|-----------|\n"
        "| 1 | 1 | `java/xss` | `Foo.java` | 10 | Legacy XSS in dev only | 2026-01-01 | me | 2026-12-31 |\n"
    )
    rows = module._parse_suppression_rows(text)
    assert len(rows) == 1
    assert rows[0]["rule"] == "java/xss"
    assert rows[0]["path"] == "Foo.java"


def test_check_file_suppression_with_real_file(tmp_path: Path) -> None:
    import importlib.util

    spec = importlib.util.spec_from_file_location("verify_supp", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    java = tmp_path / "Foo.java"
    java.write_text(
        "package x;\n"
        "public class Foo {\n"
        "  void bar() {\n"
        "    // codeql[java/xss] Legacy XSS in dev only — accepted per spec 004.\n"
        "    String s = \"<script>\";\n"
        "  }\n"
        "}\n",
        encoding="utf-8",
    )
    err = module._check_file_suppression(java, "java/xss", "Legacy XSS in dev only")
    assert err is None


def test_check_file_suppression_fails_on_missing_anchor(tmp_path: Path) -> None:
    import importlib.util

    spec = importlib.util.spec_from_file_location("verify_supp", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]

    java = tmp_path / "Foo.java"
    java.write_text("package x;\npublic class Foo {}\n", encoding="utf-8")
    err = module._check_file_suppression(java, "java/xss", "anything")
    assert err is not None
    assert "no // codeql[java/xss]" in err


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
