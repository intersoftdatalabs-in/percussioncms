#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for install-dts.py, start-cms.py, start-dts.py, and
generate-javadoc-stubs.py.

These four modules share a common testing shape: argparse help,
``--dry-run`` exercising the wiring without external tools, and
real-mode tests with stubbed subprocess.run. They are grouped in one
file to keep the four small test classes adjacent and readable.
"""

from __future__ import annotations

import importlib.util
import logging
import os
import re
import subprocess
import sys
import tempfile
import unittest
import unittest.mock
from pathlib import Path

SCRIPTS_ROOT = Path(__file__).resolve().parent

# Make the importable script modules reachable.
SCRIPT_PARENT = SCRIPTS_ROOT


def _load_from(name: str, path: Path) -> object:
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


_ID_DTS = _load_from(
    "install_dts",
    SCRIPT_PARENT / "install-dts.py",
)
_START_CMS = _load_from(
    "start_cms",
    SCRIPT_PARENT / "start-cms.py",
)
_START_DTS = _load_from(
    "start_dts",
    SCRIPT_PARENT / "start-dts.py",
)
_JAVADOC_PARENT = SCRIPTS_ROOT.parent.parent / "javadoc" / "scripts"
_GENERATE_JAVADOC_STUBS = _load_from(
    "generate_javadoc_stubs",
    _JAVADOC_PARENT / "generate-javadoc-stubs.py",
)


# ---------------------------------------------------------------------------
# install-dts
# ---------------------------------------------------------------------------


class TestInstallDtsArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            _ID_DTS.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


class TestInstallDtsDryRun(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.jar = Path(self.td.name) / "dts.jar"
        self.jar.write_bytes(b"x")
        self.install_dir = Path(self.td.name) / "install"
        logging.getLogger("install-dts").setLevel(logging.CRITICAL)

    def test_dry_run_with_verify_target_precreated(self):
        # Pre-create Deployment/Server so the verify step passes
        # without invoking the JVM.
        (self.install_dir / "Deployment" / "Server").mkdir(parents=True)
        rc = _ID_DTS.install(
            script_path=Path(__file__).resolve(),
            jar=self.jar,
            install_dir=self.install_dir,
            dry_run=True,
        )
        self.assertEqual(rc, _ID_DTS.EXIT_OK)

    def test_dry_run_verify_failed_when_deployment_missing(self):
        rc = _ID_DTS.install(
            script_path=Path(__file__).resolve(),
            jar=self.jar,
            install_dir=self.install_dir,
            dry_run=True,
        )
        self.assertEqual(rc, _ID_DTS.EXIT_VERIFY_FAILED)


class TestInstallDtsRealRun(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.jar = Path(self.td.name) / "dts.jar"
        self.jar.write_bytes(b"x")
        self.install_dir = Path(self.td.name) / "install"
        logging.getLogger("install-dts").setLevel(logging.CRITICAL)

    def test_missing_jar_returns_prereq(self):
        rc = _ID_DTS.install(
            script_path=Path(__file__).resolve(),
            jar=Path(self.td.name) / "no.jar",
            install_dir=self.install_dir,
            dry_run=False,
        )
        self.assertEqual(rc, _ID_DTS.EXIT_PREREQ_MISSING)

    def test_java_invocation_success(self):
        with unittest.mock.patch.object(_ID_DTS, "_run_java") as mock:
            mock.return_value = _ID_DTS.EXIT_OK
            (self.install_dir / "Deployment" / "Server").mkdir(parents=True)
            rc = _ID_DTS.install(
                script_path=Path(__file__).resolve(),
                jar=self.jar,
                install_dir=self.install_dir,
                dry_run=False,
            )
        self.assertEqual(rc, _ID_DTS.EXIT_OK)

    def test_java_invocation_failure_propagates(self):
        with unittest.mock.patch.object(_ID_DTS, "_run_java") as mock:
            mock.return_value = _ID_DTS.EXIT_JAVA_FAILED
            rc = _ID_DTS.install(
                script_path=Path(__file__).resolve(),
                jar=self.jar,
                install_dir=self.install_dir,
                dry_run=False,
            )
        self.assertEqual(rc, _ID_DTS.EXIT_JAVA_FAILED)


class TestInstallDtsResolve(unittest.TestCase):
    def test_explicit_jar_arg_wins(self):
        explicit = Path("/tmp/explicit.jar")
        resolved = _ID_DTS._resolve_jar(Path(__file__).resolve(), explicit)
        self.assertEqual(resolved, explicit.resolve())

    def test_default_resolves_to_maven_target(self):
        fake_script = Path("/tmp/repo/modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/scripts/install-dts.py")
        resolved = _ID_DTS._resolve_jar(fake_script, None)
        self.assertTrue(
            str(resolved).endswith(
                "deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution.jar"
            )
        )


# ---------------------------------------------------------------------------
# start-cms
# ---------------------------------------------------------------------------


class TestStartCmsArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            _START_CMS.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


class TestStartCmsDryRun(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.install_dir = Path(self.td.name)
        (self.install_dir / "jetty").mkdir(parents=True)
        (self.install_dir / "jetty" / "StartJetty.sh").write_text("#!/bin/sh\n")
        logging.getLogger("start-cms").setLevel(logging.CRITICAL)

    def test_dry_run_succeeds_when_install_present(self):
        rc = _START_CMS.start(install_dir=self.install_dir, dry_run=True)
        self.assertEqual(rc, _START_CMS.EXIT_OK)

    def test_dry_run_fails_when_install_missing(self):
        rc = _START_CMS.start(
            install_dir=Path(self.td.name) / "missing", dry_run=True,
        )
        self.assertEqual(rc, _START_CMS.EXIT_INSTALL_MISSING)

    def test_dry_run_fails_when_start_script_missing(self):
        (self.install_dir / "jetty" / "StartJetty.sh").unlink()
        rc = _START_CMS.start(install_dir=self.install_dir, dry_run=True)
        self.assertEqual(rc, _START_CMS.EXIT_STARTSCRIPT_MISSING)


class TestStartCmsJreSymlink(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.install_dir = Path(self.td.name)
        # Don't mkdir — the symlink helper operates on an existing dir.
        logging.getLogger("start-cms").setLevel(logging.CRITICAL)

    def test_existing_symlink_is_ok(self):
        link = self.install_dir / "JRE"
        link.symlink_to(self.install_dir, target_is_directory=True)
        rc = _START_CMS._setup_jre_symlink(self.install_dir)
        self.assertEqual(rc, _START_CMS.EXIT_OK)

    def test_missing_symlink_creates_with_java_home(self):
        with unittest.mock.patch.dict(
            "os.environ", {"JAVA_HOME": str(self.install_dir)}, clear=True
        ):
            rc = _START_CMS._setup_jre_symlink(self.install_dir)
        self.assertEqual(rc, _START_CMS.EXIT_OK)
        self.assertTrue((self.install_dir / "JRE").is_symlink())

    def test_missing_symlink_no_java_home_fails(self):
        with unittest.mock.patch.dict("os.environ", {}, clear=True):
            rc = _START_CMS._setup_jre_symlink(self.install_dir)
        self.assertEqual(rc, _START_CMS.EXIT_STARTSCRIPT_MISSING)


# ---------------------------------------------------------------------------
# start-dts
# ---------------------------------------------------------------------------


class TestStartDtsArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            _START_DTS.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


class TestStartDtsDryRun(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.install_dir = Path(self.td.name)
        (self.install_dir / "Deployment" / "Server").mkdir(parents=True)
        logging.getLogger("start-dts").setLevel(logging.CRITICAL)

    def test_dry_run_picks_primary(self):
        (self.install_dir / "TomcatStartup.sh").write_text("#!/bin/sh\n")
        rc = _START_DTS.start(install_dir=self.install_dir, dry_run=True)
        self.assertEqual(rc, _START_DTS.EXIT_OK)

    def test_dry_run_falls_back_to_startup(self):
        # No primary; only fallback.
        (self.install_dir / "startup.sh").write_text("#!/bin/sh\n")
        rc = _START_DTS.start(install_dir=self.install_dir, dry_run=True)
        self.assertEqual(rc, _START_DTS.EXIT_OK)

    def test_dry_run_fails_when_no_start_script(self):
        rc = _START_DTS.start(install_dir=self.install_dir, dry_run=True)
        self.assertEqual(rc, _START_DTS.EXIT_NO_STARTSCRIPT)

    def test_dry_run_fails_when_deployment_missing(self):
        rc = _START_DTS.start(
            install_dir=Path(self.td.name) / "no-deployment",
            dry_run=True,
        )
        self.assertEqual(rc, _START_DTS.EXIT_INSTALL_MISSING)


# ---------------------------------------------------------------------------
# generate-javadoc-stubs
# ---------------------------------------------------------------------------


class TestGenerateJavadocStubsArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            _GENERATE_JAVADOC_STUBS.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


class TestGenerateJavadocStubs(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        logging.getLogger("generate-javadoc-stubs").setLevel(logging.CRITICAL)

    def test_extract_methods_basic(self):
        src = (
            "public class Foo {\n"
            "    public void bar(int x, String y) { }\n"
            "    public String baz() { return null; }\n"
            "    private static List<String> quux(List<Integer> xs) { return null; }\n"
            "}\n"
        )
        methods = _GENERATE_JAVADOC_STUBS._extract_methods(src)
        # bar, baz, quux (3 methods).
        self.assertEqual(len(methods), 3)
        names = [m[1] for m in methods]
        self.assertEqual(names, ["bar", "baz", "quux"])

    def test_extract_methods_skips_non_methods(self):
        """Lines that LOOK like methods but aren't (e.g., field declarations,
        constructors without access modifier) are not matched by the regex.
        """
        src = (
            "public class Foo {\n"
            "    private int x = 5;\n"  # field, not a method
            "    Foo() { x = 6; }\n"      # constructor, no access modifier
            "    public void bar() { }\n"
            "}\n"
        )
        methods = _GENERATE_JAVADOC_STUBS._extract_methods(src)
        # Only bar should match.
        self.assertEqual([m[1] for m in methods], ["bar"])

    def test_build_stub_includes_params_and_return(self):
        methods = [("String", "bar", "int x, String y"), ("void", "baz", "")]
        stub = _GENERATE_JAVADOC_STUBS._build_stub("Foo", methods, "21")
        self.assertIn("@param x", stub)
        self.assertIn("@param y", stub)
        self.assertIn("@return", stub)  # bar returns String
        # baz returns void -> no @return for baz specifically,
        # but the @return IS present once (for bar). Confirm.
        self.assertEqual(stub.count("@return"), 1)
        self.assertIn("JDK 21", stub)

    def test_build_stub_void_returns_no_at_return(self):
        methods = [("void", "bar", "")]
        stub = _GENERATE_JAVADOC_STUBS._build_stub("Foo", methods, "17")
        self.assertNotIn("@return", stub)
        self.assertIn("JDK 17", stub)

    def test_detect_jdk_version_from_pom(self):
        # Write a fake pom.xml with <source>21</source>.
        pom = Path(self.td.name) / "pom.xml"
        pom.write_text("<project><build><source>21</source></build></project>")
        self.assertEqual(
            _GENERATE_JAVADOC_STUBS._detect_jdk_version(Path(self.td.name)),
            "21",
        )

    def test_detect_jdk_version_default_when_no_pom(self):
        self.assertEqual(
            _GENERATE_JAVADOC_STUBS._detect_jdk_version(Path(self.td.name)),
            "21",  # DEFAULT_JDK_VERSION
        )

    def test_detect_jdk_version_first_match_only(self):
        pom = Path(self.td.name) / "pom.xml"
        pom.write_text("<source>17</source><source>21</source>")
        self.assertEqual(
            _GENERATE_JAVADOC_STUBS._detect_jdk_version(Path(self.td.name)),
            "17",
        )

    def test_has_existing_javadoc(self):
        with_doc = "/** foo */\npublic class X {}\n"
        without_doc = "public class X {}\n"
        self.assertTrue(_GENERATE_JAVADOC_STUBS._has_existing_javadoc(with_doc))
        self.assertFalse(_GENERATE_JAVADOC_STUBS._has_existing_javadoc(without_doc))

    def test_run_missing_input_returns_input_missing(self):
        rc = _GENERATE_JAVADOC_STUBS.run(
            input_path=Path(self.td.name) / "no-such",
            output=None,
            jdk_version=None,
            dry_run=False,
        )
        self.assertEqual(rc, _GENERATE_JAVADOC_STUBS.EXIT_INPUT_MISSING)

    def test_run_dry_run_single_file(self):
        java = Path(self.td.name) / "Foo.java"
        java.write_text("public class Foo {\n    public void bar() {}\n}\n")
        rc = _GENERATE_JAVADOC_STUBS.run(
            input_path=java,
            output=None,
            jdk_version="21",
            dry_run=True,
        )
        self.assertEqual(rc, _GENERATE_JAVADOC_STUBS.EXIT_OK)

    def test_run_dry_run_skips_files_with_existing_javadoc(self):
        java = Path(self.td.name) / "Foo.java"
        java.write_text("/** already documented */\npublic class Foo {}\n")
        rc = _GENERATE_JAVADOC_STUBS.run(
            input_path=java,
            output=None,
            jdk_version="21",
            dry_run=True,
        )
        self.assertEqual(rc, _GENERATE_JAVADOC_STUBS.EXIT_OK)

    def test_run_real_writes_to_file(self):
        java = Path(self.td.name) / "Foo.java"
        java.write_text("public class Foo {\n    public void bar() {}\n}\n")
        rc = _GENERATE_JAVADOC_STUBS.run(
            input_path=java,
            output=None,
            jdk_version="21",
            dry_run=False,
        )
        self.assertEqual(rc, _GENERATE_JAVADOC_STUBS.EXIT_OK)
        new_content = java.read_text(encoding="utf-8")
        self.assertIn("/**", new_content)
        self.assertIn("TODO: Add description for Foo", new_content)
        # Original content is preserved after the stub.
        self.assertIn("public class Foo", new_content)
        self.assertIn("public void bar()", new_content)

    def test_run_real_writes_to_output_file(self):
        java = Path(self.td.name) / "Foo.java"
        java.write_text("public class Foo {\n    public void bar() {}\n}\n")
        out = Path(self.td.name) / "Foo-stubs.java"
        rc = _GENERATE_JAVADOC_STUBS.run(
            input_path=java,
            output=out,
            jdk_version="21",
            dry_run=False,
        )
        self.assertEqual(rc, _GENERATE_JAVADOC_STUBS.EXIT_OK)
        self.assertTrue(out.is_file())
        out_content = out.read_text(encoding="utf-8")
        self.assertIn("TODO: Add description for Foo", out_content)
        # The input file is NOT modified when --output is specified.
        self.assertNotIn("/**", java.read_text(encoding="utf-8"))

    def test_run_directory_recursive(self):
        sub = Path(self.td.name) / "sub"
        sub.mkdir()
        for i in range(3):
            java = sub / f"T{i}.java"
            java.write_text(f"public class T{i} {{}}\n")
        rc = _GENERATE_JAVADOC_STUBS.run(
            input_path=Path(self.td.name),
            output=None,
            jdk_version="21",
            dry_run=False,
        )
        self.assertEqual(rc, _GENERATE_JAVADOC_STUBS.EXIT_OK)
        for i in range(3):
            content = (sub / f"T{i}.java").read_text(encoding="utf-8")
            self.assertIn("/**", content)


if __name__ == "__main__":
    logging.getLogger("install-dts").setLevel(logging.CRITICAL)
    logging.getLogger("start-cms").setLevel(logging.CRITICAL)
    logging.getLogger("start-dts").setLevel(logging.CRITICAL)
    logging.getLogger("generate-javadoc-stubs").setLevel(logging.CRITICAL)
    unittest.main()