#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/verify-no-finder-jsp-references.py.

Self-test per the contract: drives the script through its PASS case
(modern Track B shell is hard-cut; no navigation entries) and FAIL cases
(re-introduced navigation entry; <%@include variant). The carve-out for
``finder_js.jsp`` is also exercised.

The test writes scratch files under ``tmp/`` (per the bash original's
convention) and cleans them up via ``tmp_path``.
"""
from __future__ import annotations

import shutil
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
SCRIPT = SCRIPT_DIR / "verify-no-finder-jsp-references.py"
TARGET = REPO_ROOT / "WebUI" / "src" / "main" / "webapp" / "cm" / "app" / "webmgt.jsp"


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


@pytest.mark.skipif(not TARGET.is_file(), reason="target JSP not present")
def test_clean_target_passes() -> None:
    """The actual modern Track B shell is hard-cut (no navigation entries)."""
    result = _run("--target", str(TARGET))
    assert result.returncode == 0, result.stderr


def test_finder_navigation_entry_fails(tmp_path: Path) -> None:
    """Appending a navigation entry to a synthetic JSP must fail."""
    jsp = tmp_path / "webmgt.jsp"
    jsp.write_text("<%-- harmless comment --%>\n<body>x</body>\n", encoding="utf-8")
    # Now append a probe via Python rather than shell heredoc.
    with jsp.open("a", encoding="utf-8") as fp:
        fp.write(
            '<jsp:include page="includes/finder.jsp" flush="true">\n'
            '    <jsp:param name="probe" value="true"/>\n'
            "</jsp:include>\n"
        )
    result = _run("--target", str(jsp))
    assert result.returncode == 1
    assert "finder.jsp" in (result.stdout + result.stderr)


def test_at_include_variant_fails(tmp_path: Path) -> None:
    """The ``<%@include file="includes/finder.jsp">`` form is also a gate hit."""
    jsp = tmp_path / "webmgt.jsp"
    jsp.write_text("<body>x</body>\n", encoding="utf-8")
    with jsp.open("a", encoding="utf-8") as fp:
        fp.write('<%@include file="includes/finder.jsp" %>\n')
    result = _run("--target", str(jsp))
    assert result.returncode == 1


def test_jsp_comment_with_navigation_substring_does_not_fire(tmp_path: Path) -> None:
    """A navigation-entry substring inside ``<%-- ... --%>`` is a comment; the
    gate must NOT fire on it (matches the US6 cutover comment carve-out)."""
    jsp = tmp_path / "webmgt.jsp"
    jsp.write_text(
        "<%-- this is documentation; see <jsp:include page=\"includes/finder.jsp\"> --%>\n"
        "<body>x</body>\n",
        encoding="utf-8",
    )
    result = _run("--target", str(jsp))
    assert result.returncode == 0, result.stderr


def test_finder_js_jsp_shared_lib_is_carved_out(tmp_path: Path) -> None:
    """The ``finder_js.jsp`` shared-library include must NOT fire the gate."""
    jsp = tmp_path / "webmgt.jsp"
    jsp.write_text("<body>x</body>\n", encoding="utf-8")
    with jsp.open("a", encoding="utf-8") as fp:
        fp.write('<%@include file="includes/finder_js.jsp" %>\n')
    result = _run("--target", str(jsp))
    assert result.returncode == 0, result.stderr


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
