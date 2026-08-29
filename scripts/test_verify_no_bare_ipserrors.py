#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for scripts/verify-no-bare-ipserrors.py.

Proves:
* PASS on the real monorepo (allow-listed residuals only)
* FAIL when a deliberate new bare production call-site is introduced
  (negative probes use ``tmp_path`` only — never dirties the real tree)
* Interface / typed-bridge / test / comment-only mentions are ignored
* Directory prefixes do not silently allow new files
"""
from __future__ import annotations

import importlib.util
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
SCRIPT = SCRIPT_DIR / "verify-no-bare-ipserrors.py"
ALLOWLIST = SCRIPT_DIR / "ipserrors-residual-allowlist.txt"

# Representative leftover from #3739 (deployer) — must stay exact-listed, not a
# directory prefix (a new file under that tree must fail). Sitemanage leftovers
# converted in #3846; system/services leftovers in #3847; servlet/WebDAV in #3848.
DEPLOYER_RESIDUAL = (
    "deployer/src/main/java/com/percussion/deployer/jexl/PSDeployJexlUtils.java"
)

# Representative leftover from sibling #3585 / converted in #3861. Remaining
# system/webservices SOAP/ws rows were removed from the allow-list; cms builders
# converted in #3882, cms handlers in #3883, cms.objectstore+client in #3884;
# cms.objectstore.server converted in #3900; extensions-main converted in
# #3756/#3938; com.percussion.data (+ macro/vfs) in #3939; com.percussion.security
# in #3940; com.percussion.error in #3971; design.catalog leftover call-sites in
# #3969.
# Keep an exact residual that is still frozen (system debug leftover).
SYSTEM_CMS_RESIDUAL = (
    "system/src/main/java/com/percussion/debug/PSDebugLogHandler.java"
)


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
        ["git", "config", "user.name", "ipserrors-gate-test"],
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


def _load_gate():
    spec = importlib.util.spec_from_file_location("ipserrors_gate", SCRIPT)
    assert spec is not None and spec.loader is not None
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def test_help_exits_zero_and_prints_usage() -> None:
    result = _run("--help")
    assert result.returncode == 0, result.stderr
    assert "usage:" in result.stdout.lower()


def test_list_allowlist_exits_zero() -> None:
    result = _run("--list-allowlist")
    assert result.returncode == 0, result.stderr
    combined = result.stdout + result.stderr
    assert "IPS*Errors.java" in combined
    assert "ErrorCodes.java" in combined
    assert "IPSObjectStoreErrors" in combined
    assert "#3739" in combined
    assert "#3585" in combined
    combined_posix = combined.replace("\\", "/")
    assert DEPLOYER_RESIDUAL.replace("\\", "/") in combined_posix
    assert SYSTEM_CMS_RESIDUAL.replace("\\", "/") in combined_posix
    # Prefix freeze: printed residuals are files, not directory wildcards, and an
    # unlisted probe under the same tree is not advertised as covered.
    residual_lines = [
        ln.strip()
        for ln in combined_posix.splitlines()
        if ln.strip().endswith(".java") or ln.strip().endswith("/")
    ]
    for ln in residual_lines:
        assert not ln.endswith("/"), ln
    probe = "system/services/src/com/percussion/services/assembly/impl/NewBare.java"
    assert probe not in combined_posix


def test_clean_repo_passes() -> None:
    """Allow-listed tree on current main must pass the freeze gate."""
    result = _run("--repo-root", str(REPO_ROOT))
    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS" in result.stdout


def test_deliberate_new_bare_production_site_fails(tmp_path: Path) -> None:
    """Tracked production Java with bare IPSWebserviceErrors must fail.

    Acceptance for #3586: gate fails on a deliberate new bare production usage.
    Uses ``tmp_path`` only so interrupted runs never leave the monorepo dirty.
    """
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    _write_and_add(
        fake_root,
        "system/src/main/java/com/percussion/example/BareIpsProbe.java",
        (
            "package com.percussion.example;\n"
            "import com.percussion.webservices.IPSWebserviceErrors;\n"
            "public class BareIpsProbe {\n"
            "  int c = IPSWebserviceErrors.OBJECT_NOT_FOUND;\n"
            "}\n"
        ),
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 1, result.stdout + result.stderr
    combined = result.stdout + result.stderr
    assert "FAIL" in combined
    assert "BareIpsProbe.java" in combined
    assert "IPS*Errors" in combined or "IPSWebserviceErrors" in combined


def test_allowlisted_interface_alone_passes(tmp_path: Path) -> None:
    """Only the interface definition under IPS*Errors.java must pass."""
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    _write_and_add(
        fake_root,
        "system/webservices/src/com/percussion/webservices/IPSWebserviceErrors.java",
        (
            "package com.percussion.webservices;\n"
            "public interface IPSWebserviceErrors {\n"
            "  int OBJECT_NOT_FOUND = 1;\n"
            "}\n"
        ),
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS" in result.stdout


def test_typed_errorcodes_bridge_passes(tmp_path: Path) -> None:
    """Typed *ErrorCodes.java dual-write bridges must not trip the gate."""
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    _write_and_add(
        fake_root,
        "modules/perc-auditlog/src/main/java/com/intsof/percussioncms/auditlog/"
        "codes/WebserviceErrorCodes.java",
        (
            "package com.intsof.percussioncms.auditlog.codes;\n"
            "import com.percussion.webservices.IPSWebserviceErrors;\n"
            "public enum WebserviceErrorCodes {\n"
            "  OBJECT_NOT_FOUND(IPSWebserviceErrors.OBJECT_NOT_FOUND, false);\n"
            "  WebserviceErrorCodes(int code, boolean auditable) {}\n"
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
            "/** Formerly used {@code IPSWebserviceErrors} ints. */\n"
            "public class CommentOnly {\n"
            "  // IPSSiteManageErrors.SITE_NOT_FOUND was here\n"
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
            "import com.percussion.webservices.IPSWebserviceErrors;\n"
            "import org.junit.jupiter.api.Test;\n"
            "class ParityTest {\n"
            "  @Test void code() {\n"
            "    int c = IPSWebserviceErrors.OBJECT_NOT_FOUND;\n"
            "  }\n"
            "}\n"
        ),
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS" in result.stdout


def test_repo_root_src_test_bare_use_passes(tmp_path: Path) -> None:
    """Repo-root ``src/test/`` layout (no module prefix) must still be test."""
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    _write_and_add(
        fake_root,
        "src/test/java/com/percussion/example/RootLayoutParity.java",
        (
            "package com.percussion.example;\n"
            "import com.percussion.sitemanage.error.IPSSiteManageErrors;\n"
            "public class RootLayoutParity {\n"
            "  int c = IPSSiteManageErrors.SITE_LOAD_FAILED;\n"
            "}\n"
        ),
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS" in result.stdout


def test_legacy_webservices_test_root_passes(tmp_path: Path) -> None:
    """``system/webservices/test/`` (no src/test) is a legacy test root."""
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    _write_and_add(
        fake_root,
        "system/webservices/test/com/percussion/webservices/WsParity.java",
        (
            "package com.percussion.webservices;\n"
            "import com.percussion.webservices.IPSWebserviceErrors;\n"
            "public class WsParity {\n"
            "  int c = IPSWebserviceErrors.OBJECT_NOT_FOUND;\n"
            "}\n"
        ),
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS" in result.stdout


def test_objectstore_family_not_gated_here(tmp_path: Path) -> None:
    """IPSObjectStoreErrors stays on the #3143 sibling gate."""
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    _write_and_add(
        fake_root,
        "system/src/main/java/com/percussion/example/ObjectStoreOnly.java",
        (
            "package com.percussion.example;\n"
            "import com.percussion.design.objectstore.IPSObjectStoreErrors;\n"
            "public class ObjectStoreOnly {\n"
            "  int c = IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE;\n"
            "}\n"
        ),
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS" in result.stdout


def test_norm_path_edge_cases() -> None:
    """``_norm_path`` must not corrupt ``../`` via character-class strip."""
    mod = _load_gate()
    assert mod._norm_path(r"foo\bar\Baz.java") == "foo/bar/Baz.java"
    assert mod._norm_path("./system/src/main/java/X.java") == "system/src/main/java/X.java"
    assert mod._norm_path("././deployer/src/main/java/Y.java") == "deployer/src/main/java/Y.java"
    assert mod._norm_path("../foo/bar.java") == "../foo/bar.java"
    assert mod._norm_path(r"..\foo\bar.java") == "../foo/bar.java"
    assert mod._norm_path("system/src/main/java/X.java") == "system/src/main/java/X.java"


def test_is_test_path_segment_detection() -> None:
    """``src/test`` (and it / testFixtures) as consecutive segments."""
    mod = _load_gate()
    assert mod._is_test_path("system/src/test/java/com/example/FooTest.java")
    assert mod._is_test_path("src/test/java/com/example/RootLayoutTest.java")
    assert mod._is_test_path("src/it/java/com/example/RootIt.java")
    assert mod._is_test_path("module/src/testFixtures/java/com/example/Fx.java")
    assert mod._is_test_path(r"src\test\java\com\example\WinSep.java")
    assert mod._is_test_path("system/webservices/test/com/example/Legacy.java")
    assert not mod._is_test_path("system/src/main/java/com/example/Prod.java")
    assert not mod._is_test_path("src/main/java/com/example/Prod.java")
    assert not mod._is_test_path("system/src/main/java/com/percussion/test/Util.java")
    assert mod._is_test_path("tools/FooTest.java")


def test_new_file_under_sitemanage_tree_not_silently_allowed(tmp_path: Path) -> None:
    """Exact residual list — a new sitemanage file must fail (#3584 leftover)."""
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    _write_and_add(
        fake_root,
        "projects/sitemanage/src/main/java/com/percussion/sitemanage/NewBareSite.java",
        (
            "package com.percussion.sitemanage;\n"
            "import com.percussion.sitemanage.error.IPSSiteManageErrors;\n"
            "public class NewBareSite {\n"
            "  int c = IPSSiteManageErrors.SITE_LOAD_FAILED;\n"
            "}\n"
        ),
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 1, result.stdout + result.stderr
    assert "NewBareSite.java" in (result.stdout + result.stderr)


def test_new_file_under_webservices_tree_not_silently_allowed(tmp_path: Path) -> None:
    """Exact residual list — a new webservices file must fail (#3585 leftover)."""
    fake_root = tmp_path / "repo"
    _init_fake_git_repo(fake_root)
    _write_and_add(
        fake_root,
        "system/webservices/src/com/percussion/webservices/NewBareWs.java",
        (
            "package com.percussion.webservices;\n"
            "import com.percussion.webservices.IPSWebserviceErrors;\n"
            "public class NewBareWs {\n"
            "  int c = IPSWebserviceErrors.OBJECT_NOT_FOUND;\n"
            "}\n"
        ),
    )
    result = _run("--repo-root", str(fake_root))
    assert result.returncode == 1, result.stdout + result.stderr
    assert "NewBareWs.java" in (result.stdout + result.stderr)


def test_residual_allowlist_is_exact_paths_only() -> None:
    """Committed allow-list must not use directory prefixes."""
    assert ALLOWLIST.is_file(), "missing scripts/ipserrors-residual-allowlist.txt"
    text = ALLOWLIST.read_text(encoding="utf-8")
    entries = [
        ln.strip()
        for ln in text.splitlines()
        if ln.strip() and not ln.strip().startswith("#")
    ]
    assert len(entries) > 0
    assert DEPLOYER_RESIDUAL in entries
    assert SYSTEM_CMS_RESIDUAL in entries
    for entry in entries:
        assert not entry.endswith("/"), entry
        assert "\\" not in entry, entry
        assert entry.endswith(".java"), entry
        assert not entry.startswith("modules/extensions-main/"), entry
        assert not entry.startswith("system/src/main/java/com/percussion/security/"), entry
        assert not entry.startswith("system/src/main/java/com/percussion/extension/"), entry
        assert not entry.startswith("system/src/main/java/com/percussion/design/catalog/"), entry


def test_extensions_main_converted_paths_not_allowlisted() -> None:
    """#3756 typed the leftovers; #3938 must not re-list them after #3793."""
    text = ALLOWLIST.read_text(encoding="utf-8")
    entries = [
        ln.strip()
        for ln in text.splitlines()
        if ln.strip() and not ln.strip().startswith("#")
    ]
    resurrected = [e for e in entries if e.startswith("modules/extensions-main/")]
    assert resurrected == [], resurrected


def test_system_security_converted_paths_not_allowlisted() -> None:
    """#3940 typed leftover com.percussion.security production call-sites."""
    text = ALLOWLIST.read_text(encoding="utf-8")
    entries = [
        ln.strip()
        for ln in text.splitlines()
        if ln.strip() and not ln.strip().startswith("#")
    ]
    resurrected = [
        e for e in entries if e.startswith("system/src/main/java/com/percussion/security/")
    ]
    assert resurrected == [], resurrected


def test_system_extension_converted_paths_not_allowlisted() -> None:
    """#3970 typed leftover com.percussion.extension production call-sites."""
    converted = (
        "system/src/main/java/com/percussion/extension/PSExtensionHandler.java",
        "system/src/main/java/com/percussion/extension/PSExtensionHandlerConfiguration.java",
        "system/src/main/java/com/percussion/extension/PSExtensionParams.java",
        "system/src/main/java/com/percussion/extension/PSExtensionProcessingException.java",
        "system/src/main/java/com/percussion/extension/PSJavaExtensionHandler.java",
        "system/src/main/java/com/percussion/extension/PSJavaScriptCallException.java",
        "system/src/main/java/com/percussion/extension/PSJavaScriptCompileException.java",
        "system/src/main/java/com/percussion/extension/PSJavaScriptUdfExtension.java",
        "system/src/main/java/com/percussion/extension/PSParameterMismatchException.java",
    )
    text = ALLOWLIST.read_text(encoding="utf-8")
    entries = {
        ln.strip()
        for ln in text.splitlines()
        if ln.strip() and not ln.strip().startswith("#")
    }
    resurrected = [p for p in converted if p in entries]
    assert resurrected == [], resurrected


def test_system_design_catalog_converted_paths_not_allowlisted() -> None:
    """#3969 typed leftover com.percussion.design.catalog production call-sites."""
    text = ALLOWLIST.read_text(encoding="utf-8")
    entries = [
        ln.strip()
        for ln in text.splitlines()
        if ln.strip() and not ln.strip().startswith("#")
    ]
    resurrected = [
        e
        for e in entries
        if e.startswith("system/src/main/java/com/percussion/design/catalog/")
    ]
    assert resurrected == [], resurrected


def test_empty_allowlist_fails_on_real_residuals(tmp_path: Path) -> None:
    """Without the residual file, current production leftovers must fail."""
    empty = tmp_path / "empty-allowlist.txt"
    empty.write_text("# none\n", encoding="utf-8")
    result = _run("--repo-root", str(REPO_ROOT), "--allowlist", str(empty))
    assert result.returncode == 1, result.stdout + result.stderr
    combined = result.stdout + result.stderr
    assert "FAIL" in combined
    assert (
        "PSDeployJexlUtils.java" in combined
        or "PSActiveAssemblerProcessor.java" in combined
    )


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
