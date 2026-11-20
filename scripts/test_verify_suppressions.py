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
# See the License for the specific language governing permissions and
# limitations under the License.
"""pytest coverage for scripts/verify-suppressions.py (issue #6823 contract).

The ledger row is the suppression mechanism of record: ``justification``
must cite closure evidence (code-scanning dismissal reason/URL or a
code-rewrite reference), not merely a sink-line ``// codeql[...]`` comment
that never closes an alert. ``mkd-`` rows additionally require the
in-source ``mkd-code-review[<rule>]`` marker.
"""
from __future__ import annotations

import importlib.util
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "verify-suppressions.py"


def _load_module():  # type: ignore[no-untyped-def]
    spec = importlib.util.spec_from_file_location("verify_supp", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)  # type: ignore[union-attr]
    return module


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


HEADER = (
    "| alert_id | rule_id | file_path | line | justification | applied_on | applied_by | review_by |\n"
    "|----------|---------|-----------|------|---------------|------------|------------|-----------|\n"
)


def test_help_exits_zero_and_prints_usage() -> None:
    result = _run("--help")
    assert result.returncode == 0, result.stderr
    assert "usage:" in result.stdout.lower()


def test_missing_suppressions_exits_nonzero(tmp_path: Path) -> None:
    result = _run("--suppressions", str(tmp_path / "does-not-exist.md"))
    assert result.returncode != 0
    assert "not found" in (result.stderr + result.stdout).lower()


def test_parse_suppression_rows_extracts_columns() -> None:
    module = _load_module()
    text = (
        "# Suppressions\n\n" + HEADER + "| 1 | `java/xss` | `Foo.java` | 10 "
        "| Dismissed FP: Jackson JSON DTO return, not HTML. | 2026-01-01 | me | 2026-12-31 |\n"
    )
    rows = module._parse_suppression_rows(text)
    assert len(rows) == 1
    assert rows[0]["alert_id"] == "1"
    assert rows[0]["rule"] == "java/xss"
    assert rows[0]["justification"].startswith("Dismissed FP")


def test_parse_suppression_rows_accepts_freeform_ids_and_skips_header() -> None:
    module = _load_module()
    text = (
        "# Suppressions\n\n" + HEADER
        + "| PR-1337-login | `java/unvalidated-url-redirection` | `A.java` | — "
        "| Runtime fix via PSRedirectValidation. Tests: `FooTest`. | 2026-01-01 | me | 2026-12-31 |\n"
        + "| mkd-6325 | `paths.hardcoded_sep` | `B.java` | joins "
        "| Adjudicated intentional (mkd-code-review). | 2026-01-01 | me | 2026-12-31 |\n"
        + "| 595,624 | `java/xss` | `.github/codeql/codeql-config.yml` | — "
        "| paths-ignore for vendored bundles. | 2026-01-01 | me | 2026-12-31 |\n"
    )
    rows = module._parse_suppression_rows(text)
    assert [row["alert_id"] for row in rows] == ["PR-1337-login", "mkd-6325", "595,624"]


def test_dismissal_row_passes_without_codeql_anchor(tmp_path: Path) -> None:
    """A dismissal reason/URL satisfies the row with no source comment (issue #6823)."""
    module = _load_module()
    java = tmp_path / "Foo.java"
    java.write_text("package x;\npublic class Foo {}\n", encoding="utf-8")
    row = {
        "alert_id": "9999",
        "rule": "java/xss",
        "path": "`Foo.java`",
        "line": "10",
        "justification": "**Dismissed FP** (won't fix): Jackson JSON DTO return, not HTML body.",
    }
    assert module._check_row(row, tmp_path, {}) is None


def test_rewrite_reference_row_passes_without_codeql_anchor(tmp_path: Path) -> None:
    module = _load_module()
    java = tmp_path / "Foo.java"
    java.write_text("package x;\npublic class Foo {}\n", encoding="utf-8")
    row = {
        "alert_id": "9999",
        "rule": "java/path-injection",
        "path": "`Foo.java`",
        "line": "10",
        "justification": "Runtime fix: requireUnderBase before exists. Tests: `FooPathTest`. PR #1234.",
    }
    assert module._check_row(row, tmp_path, {}) is None


def test_comment_only_row_fails_evidence(tmp_path: Path) -> None:
    """A justification that merely notes a // codeql comment is not closure (issue #6823)."""
    module = _load_module()
    java = tmp_path / "Foo.java"
    java.write_text(
        "package x;\npublic class Foo {\n  // codeql[java/xss] justification: sink note.\n}\n",
        encoding="utf-8",
    )
    row = {
        "alert_id": "9999",
        "rule": "java/xss",
        "path": "`Foo.java`",
        "line": "3",
        "justification": "Sink-line `// codeql[java/xss]` added on the return.",
    }
    err = module._check_row(row, tmp_path, {})
    assert err is not None
    assert "closure evidence" in err


def test_missing_cited_file_fails(tmp_path: Path) -> None:
    module = _load_module()
    row = {
        "alert_id": "8888",
        "rule": "java/ssrf",
        "path": "`Gone.java`",
        "line": "1",
        "justification": "**Dismissed FP** won't fix; runtime validateURLString barrier.",
    }
    err = module._check_row(row, tmp_path, {})
    assert err is not None
    assert "missing file" in err


def test_mkd_row_requires_in_source_marker(tmp_path: Path) -> None:
    module = _load_module()
    java = tmp_path / "Bar.java"
    java.write_text('public class Bar { String s = "/" + "x"; }\n', encoding="utf-8")  # mkd-code-review[paths.hardcoded_sep]: adjudicated fixture (issue #6823; cf. #6778) — "/" is Java fixture content, not an OS path join.
    row = {
        "alert_id": "mkd-9999",
        "rule": "paths.hardcoded_sep",
        "path": "`Bar.java`",
        "line": "1",
        "justification": "**Adjudicated intentional (mkd-code-review):** CMS logical path, not an OS path.",
    }
    err = module._check_row(row, tmp_path, {})
    assert err is not None
    assert "mkd-code-review[paths.hardcoded_sep]" in err
    java.write_text(
        'public class Bar {\n  // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #9999).\n'
        '  String s = "/" + "x";\n}\n',  # mkd-code-review[paths.hardcoded_sep]: adjudicated fixture (issue #6823; cf. #6778) — "/" is Java fixture content, not an OS path join.
        encoding="utf-8",
    )
    assert module._check_row(row, tmp_path, {}) is None


def test_multi_token_cell_resolves_each_file(tmp_path: Path) -> None:
    module = _load_module()
    (tmp_path / "A.java").write_text("public class A {}\n", encoding="utf-8")
    (tmp_path / "B.java").write_text("public class B {}\n", encoding="utf-8")
    row = {
        "alert_id": "1,2",
        "rule": "java/path-injection",
        "path": "`A.java` / `B.java`",
        "line": "1,2",
        "justification": "**Dismissed FP** thrash; runtime requireUnderBase barrier.",
    }
    assert module._check_row(row, tmp_path, {}) is None


def test_end_to_end_pass_and_fail(tmp_path: Path) -> None:
    java = tmp_path / "Foo.java"
    java.write_text("package x;\npublic class Foo {}\n", encoding="utf-8")
    good = tmp_path / "good.md"
    good.write_text(
        "# S\n\n" + HEADER + "| 1 | `java/xss` | `Foo.java` | 10 "
        "| **Dismissed FP**: JSON DTO return. | 2026-01-01 | me | 2026-12-31 |\n",
        encoding="utf-8",
    )
    result = _run("--suppressions", str(good), "--source-root", str(tmp_path))
    assert result.returncode == 0, result.stderr
    assert "PASS (1 rows)" in result.stdout
    bad = tmp_path / "bad.md"
    bad.write_text(
        "# S\n\n" + HEADER + "| 1 | `java/xss` | `Foo.java` | 10 "
        "| Sink-line comment added. | 2026-01-01 | me | 2026-12-31 |\n",
        encoding="utf-8",
    )
    result = _run("--suppressions", str(bad), "--source-root", str(tmp_path))
    assert result.returncode == 1
    assert "FAIL" in result.stderr


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
