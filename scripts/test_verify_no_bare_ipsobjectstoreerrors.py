#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/verify-no-bare-ipsobjectstoreerrors.py.

Proves:
* PASS on the real monorepo (allow-listed residuals only)
* FAIL when a deliberate new bare production call-site is introduced
  (negative probes use ``tmp_path`` only — never dirties the real tree)
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
SCRIPT = SCRIPT_DIR / "verify-no-bare-ipsobjectstoreerrors.py"


def _run(*args: str, cwd: Path | None = None) -> subprocess.CompletedProcess:
    cmd = [sys.executable, str(SCRIPT), *args]
    return subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=90,
        capture_output=True,
        text=True,
        cwd=str(cwd) if cwd else None,
    )


def _init_fake_git_repo(fake_root: Path) -> None:
    """Minimal git repo so ``git grep`` works under a temp root."""
    fake_root.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["git", "init"],
        shell=False,
        check=True,
        cwd=str(fake_root),
        capture_output=True,
    )
    subprocess.run(
        ["git", "config", "user.email", "gate-test@example.com"],
        shell=False,
        check=True,
        cwd=str(fake_root),
        capture_output=True,
    )
    subprocess.run(
        ["git", "config", "user.name", "ipsobjectstoreerrors-gate-test"],
        shell=False,
        check=True,
        cwd=str(fake_root),
        capture_output=True,
    )


def _write_and_add(fake_root: Path, rel: str, content: str) -> None:
    path = fake_root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    subprocess.run(
        ["git", "add", "--", rel.replace("\\", "/")],
        shell=False,
        check=True,
        cwd=str(fake_root),
        capture_output=True,
    )


def test_help_exits_zero_and_prints_usage() -> None:
    result = _run("--help")
    assert result.returncode == 0, result.stderr
    assert "usage:" in result.stdout.lower()


def test_list_allowlist_exits_zero() -> None:
    result = _run("--list-allowlist")
    assert result.returncode == 0, result.stderr
    combined = result.stdout + result.stderr
    assert "IPSObjectStoreErrors.java" in combined
    assert "deployer/src/main/java/" in combined


def test_clean_repo_passes() -> None:
    """Allow-listed tree on current main must pass the freeze gate."""
    result = _run("--repo-root", str(REPO_ROOT))
    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS" in result.stdout


def test_deliberate_new_bare_production_site_fails(tmp_path: Path) -> None:
    """Tracked production Java with bare IPSObjectStoreErrors must fail.

    Acceptance for #3143: gate fails on a deliberate new bare production usage.
    Uses ``tmp_path`` only so interrupted runs never leave the monorepo dirty.
    """
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)

    # Interface alone is allow-listed in the real script, but this fake tree
    # has a brand-new production throw site outside the residual prefixes.
    _write_and_add(
        fake_root,
        "system/src/main/java/com/percussion/example/BareIpsProbe.java",
        (
            "package com.percussion.example;\n"
            "import com.percussion.design.objectstore.IPSObjectStoreErrors;\n"
            "import com.percussion.design.objectstore.PSUnknownNodeTypeException;\n"
            "public class BareIpsProbe {\n"
            "  void boom() throws PSUnknownNodeTypeException {\n"
            "    throw new PSUnknownNodeTypeException(\n"
            "        IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE, new Object[] {});\n"
            "  }\n"
            "}\n"
        ),
    )

    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 1, result.stdout + result.stderr
    combined = result.stdout + result.stderr
    assert "FAIL" in combined
    assert "BareIpsProbe.java" in combined
    assert "IPSObjectStoreErrors" in combined


def test_allowlisted_interface_alone_passes(tmp_path: Path) -> None:
    """Only the interface definition under the always-allow path must pass."""
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    _write_and_add(
        fake_root,
        "modules/utils/src/main/java/com/percussion/design/objectstore/"
        "IPSObjectStoreErrors.java",
        (
            "package com.percussion.design.objectstore;\n"
            "public interface IPSObjectStoreErrors {\n"
            "  int XML_ELEMENT_WRONG_TYPE = 2012;\n"
            "}\n"
        ),
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS" in result.stdout


def test_comment_only_mention_passes(tmp_path: Path) -> None:
    """Historical comment/javadoc mentions must not trip the gate."""
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    _write_and_add(
        fake_root,
        "system/src/main/java/com/percussion/example/CommentOnly.java",
        (
            "package com.percussion.example;\n"
            "/** Formerly used {@code IPSObjectStoreErrors} ints. */\n"
            "public class CommentOnly {\n"
            "  // IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE was here\n"
            "}\n"
        ),
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS" in result.stdout


def test_test_source_bare_use_passes(tmp_path: Path) -> None:
    """Test sources may still reference legacy ints for parity assertions."""
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    _write_and_add(
        fake_root,
        "system/src/test/java/com/percussion/example/ParityTest.java",
        (
            "package com.percussion.example;\n"
            "import com.percussion.design.objectstore.IPSObjectStoreErrors;\n"
            "import org.junit.jupiter.api.Test;\n"
            "import static org.junit.jupiter.api.Assertions.assertEquals;\n"
            "class ParityTest {\n"
            "  @Test void code() {\n"
            "    assertEquals(2012, IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE);\n"
            "  }\n"
            "}\n"
        ),
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS" in result.stdout


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
