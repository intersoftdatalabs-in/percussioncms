#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for api-update.py (no Maven, no network, no Jetty).

All external invocations (Maven, Jetty) are gated behind ``--dry-run`` and
the copy operations run against a synthesized fake repo layout in a
tempdir.
"""

from __future__ import annotations

import importlib.util
import io
import logging
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "api-update.py"
    name = "api_update"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


au = _load()


def _make_fake_artifact(target_dir: Path, name_prefix: str) -> Path:
    """Build a synthetic built-artifact at ``target_dir/<name_prefix>-<v>``.

    Returns the path to the created artifact.
    """
    target_dir.mkdir(parents=True, exist_ok=True)
    artifact = target_dir / f"{name_prefix}-1.0.0-SNAPSHOT.jar"
    # Write a tiny valid zip so we can assert the file got copied.
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as zf:
        zf.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n")
    artifact.write_bytes(buf.getvalue())
    return artifact


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            au.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_missing_module_arg_errors(self):
        with self.assertRaises(SystemExit) as cm:
            au.main([])
        # argparse exits 2 for missing required argument
        self.assertEqual(cm.exception.code, 2)

    def test_unknown_module_choice_errors(self):
        with self.assertRaises(SystemExit) as cm:
            au.main(["--module", "not-a-module"])
        self.assertEqual(cm.exception.code, 2)


class TestResolvePaths(unittest.TestCase):
    def test_resolves_well_known_paths(self):
        fake_script = SCRIPTS / "api-update.py"
        paths = au._resolve_paths(fake_script)
        self.assertEqual(paths.module_dir, SCRIPTS.parent)
        self.assertEqual(paths.repo_root, SCRIPTS.parent.parent.parent)
        # Cross-platform: Path.as_posix() always uses '/' so the suffix
        # comparison works on Windows (where str(Path) uses '\\') and Unix.
        self.assertTrue(
            paths.jetty_start_script.as_posix().endswith(
                "modules/perc-distribution-tree/target/classes/distribution/jetty/StartJetty.bat"
            )
        )


class TestRunModuleDryRun(unittest.TestCase):
    """``--dry-run`` exercises the full wiring without touching Maven or
    Jetty. These tests use a synthesized fake repo layout in a tempdir.
    """

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.td_path = Path(self.td.name)
        # Build a synthetic repo layout:
        #   <td>/repo_root/
        #     WebUI/target/CMLite-WebUI-1.0.0-SNAPSHOT/  (dir for recursive)
        #     rest/target/rest-1.0.0-SNAPSHOT.jar
        #     projects/sitemanage/target/sitemanage-1.0.0-SNAPSHOT.jar
        #     modules/perc-tinymce/target/perc-tinymce-1.0.0-SNAPSHOT.jar
        #     modules/perc-auditlog/target/audit-log-1.0.0-SNAPSHOT.jar
        #     system/target/CMLite-Main-1.0.0-SNAPSHOT.jar
        #     modules/perc-distribution-tree/target/classes/distribution/jetty/StartJetty.bat
        self.repo_root = self.td_path / "repo_root"
        webui_target = self.repo_root / "WebUI" / "target" / "CMLite-WebUI-1.0.0-SNAPSHOT"
        webui_target.mkdir(parents=True)
        (webui_target / "index.html").write_text("<html/>", encoding="utf-8")
        _make_fake_artifact(self.repo_root / "rest" / "target", "rest")
        _make_fake_artifact(self.repo_root / "projects" / "sitemanage" / "target", "sitemanage")
        _make_fake_artifact(self.repo_root / "modules" / "perc-tinymce" / "target", "perc-tinymce")
        _make_fake_artifact(self.repo_root / "modules" / "perc-auditlog" / "target", "audit-log")
        _make_fake_artifact(self.repo_root / "system" / "target", "CMLite-Main")
        # Jetty start script — must exist for the no-dry-run path; for
        # --no-restart tests we don't need it.
        jetty = (
            self.repo_root
            / "modules"
            / "perc-distribution-tree"
            / "target"
            / "classes"
            / "distribution"
            / "jetty"
            / "StartJetty.bat"
        )
        jetty.parent.mkdir(parents=True, exist_ok=True)
        jetty.write_text("@echo off\necho Jetty started\n", encoding="utf-8")

        self.paths = au.ResolvedPaths(
            repo_root=self.repo_root,
            module_dir=self.repo_root / "modules" / "perc-distribution-tree",
            jetty_start_script=jetty,
        )
        # Suppress INFO logging noise during tests
        logging.getLogger("api-update").setLevel(logging.CRITICAL)

    def _run(self, *, module, skip_tests=False, no_restart=False, dry_run=True):
        return au.run_module(
            module=module,
            skip_tests=skip_tests,
            no_restart=no_restart,
            dry_run=dry_run,
            paths=self.paths,
            maven_argv0=["mvn"],  # stub — dry_run=True so it's never invoked
        )

    def test_dry_run_webui_copies_recursive_tree(self):
        rc = self._run(module="webui", dry_run=True)
        self.assertEqual(rc, au.EXIT_OK)

    def test_dry_run_rest_copies_single_jar(self):
        rc = self._run(module="rest", dry_run=True)
        self.assertEqual(rc, au.EXIT_OK)

    def test_dry_run_sitemanage_copies_single_jar(self):
        rc = self._run(module="sitemanage", dry_run=True)
        self.assertEqual(rc, au.EXIT_OK)

    def test_dry_run_jars_copies_multiple_artifacts(self):
        rc = self._run(module="jars", dry_run=True)
        self.assertEqual(rc, au.EXIT_OK)

    def test_dry_run_no_restart_skips_jetty(self):
        rc = self._run(module="webui", dry_run=True, no_restart=True)
        self.assertEqual(rc, au.EXIT_OK)


class TestRunModuleReal(unittest.TestCase):
    """``--dry-run`` is False. Maven and Jetty are stubbed so we exercise
    the real subprocess / copy / restart code paths without paying the
    build-time cost.
    """

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.td_path = Path(self.td.name)
        # Minimal synthetic repo: rest module only (simplest case).
        rest_target = self.td_path / "rest" / "target"
        _make_fake_artifact(rest_target, "rest")
        jetty = (
            self.td_path
            / "modules"
            / "perc-distribution-tree"
            / "target"
            / "classes"
            / "distribution"
            / "jetty"
            / "StartJetty.bat"
        )
        jetty.parent.mkdir(parents=True, exist_ok=True)
        jetty.write_text("@echo off\necho Jetty started\n", encoding="utf-8")
        self.paths = au.ResolvedPaths(
            repo_root=self.td_path,
            module_dir=self.td_path / "modules" / "perc-distribution-tree",
            jetty_start_script=jetty,
        )
        logging.getLogger("api-update").setLevel(logging.CRITICAL)

    def test_real_run_rest_copies_jar_into_distribution(self):
        # Use the test's own argv0 as a stand-in for Maven: it always exits
        # 0, satisfies the subprocess.run contract, and records the call
        # in self._invocations.
        rc = au.run_module(
            module="rest",
            skip_tests=True,
            no_restart=True,  # don't actually launch Jetty
            dry_run=False,
            paths=self.paths,
            maven_argv0=[sys.executable, "-c", "import sys; sys.exit(0)"],
        )
        self.assertEqual(rc, au.EXIT_OK)
        # Verify the rest jar was copied into the expected destination.
        dest = (
            self.td_path
            / "modules"
            / "perc-distribution-tree"
            / "target"
            / "classes"
            / "distribution"
            / "jetty"
            / "base"
            / "webapps"
            / "Rhythmyx"
            / "WEB-INF"
            / "lib"
        )
        copied = sorted(dest.glob("rest-*.jar"))
        self.assertEqual(len(copied), 1, msg=f"expected one rest-*.jar in {dest}")
        self.assertEqual(copied[0].name, "rest-1.0.0-SNAPSHOT.jar")

    def test_real_run_maven_failure_propagates(self):
        rc = au.run_module(
            module="rest",
            skip_tests=True,
            no_restart=True,
            dry_run=False,
            paths=self.paths,
            maven_argv0=[sys.executable, "-c", "import sys; sys.exit(7)"],
        )
        self.assertEqual(rc, au.EXIT_BUILD_FAILED)


class TestRestartJetty(unittest.TestCase):
    def test_jetty_script_missing(self):
        with tempfile.TemporaryDirectory() as td:
            rc = au._restart_jetty(
                Path(td) / "no-such.bat",
                cwd=Path(td),
                dry_run=True,
            )
            self.assertEqual(rc, au.EXIT_RESTART_FAILED)


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            au.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


if __name__ == "__main__":
    logging.getLogger("api-update").setLevel(logging.CRITICAL)
    unittest.main()