#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for install-cms.py (no java, no filesystem writes).

``--dry-run`` exercises the wiring; real-mode tests inject stubs for
``subprocess.run`` and ``Path.is_file``.
"""

from __future__ import annotations

import importlib.util
import logging
import sys
import tempfile
import unittest
import unittest.mock
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "install-cms.py"
    name = "install_cms"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


ic = _load()


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            ic.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


class TestDryRun(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.jar = Path(self.td.name) / "fake.jar"
        self.jar.write_bytes(b"x")
        self.install_dir = Path(self.td.name) / "install"
        # Pre-create the verify target so dry-run passes the
        # post-install verification step (which checks for
        # StartJetty.sh without running the installer in dry-run mode).
        (self.install_dir / "jetty").mkdir(parents=True, exist_ok=True)
        (self.install_dir / "jetty" / "StartJetty.sh").write_text("#!/bin/sh\n")
        logging.getLogger("install-cms").setLevel(logging.CRITICAL)

    def test_dry_run_default(self):
        rc = ic.install(
            script_path=Path(ic.__file__).resolve() if hasattr(ic, "__file__") else Path(__file__),
            jar=self.jar,
            install_dir=self.install_dir,
            reset=False,
            dry_run=True,
        )
        self.assertEqual(rc, ic.EXIT_OK)


class TestRealRun(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.jar = Path(self.td.name) / "fake.jar"
        self.jar.write_bytes(b"x")
        self.install_dir = Path(self.td.name) / "install"
        # Lay down the post-install verification file.
        (self.install_dir / "jetty").mkdir(parents=True, exist_ok=True)
        (self.install_dir / "jetty" / "StartJetty.sh").write_text("#!/bin/sh\n")
        logging.getLogger("install-cms").setLevel(logging.CRITICAL)

    def _install(self, **kwargs):
        kwargs.setdefault("reset", False)
        return ic.install(
            script_path=Path(__file__).resolve(),
            jar=self.jar,
            install_dir=self.install_dir,
            dry_run=False,
            **kwargs,
        )

    def test_missing_jar_returns_prereq(self):
        rc = ic.install(
            script_path=Path(__file__).resolve(),
            jar=Path(self.td.name) / "no-such.jar",
            install_dir=self.install_dir,
            reset=False,
            dry_run=False,
        )
        self.assertEqual(rc, ic.EXIT_PREREQ_MISSING)

    def test_java_invocation_success(self):
        with unittest.mock.patch.object(ic, "_run") as mock_run:
            mock_run.return_value = ic.EXIT_OK
            rc = self._install()
        self.assertEqual(rc, ic.EXIT_OK)

    def test_java_invocation_failure_propagates(self):
        with unittest.mock.patch.object(ic, "_run") as mock_run:
            mock_run.return_value = ic.EXIT_JAVA_FAILED
            rc = self._install()
        self.assertEqual(rc, ic.EXIT_JAVA_FAILED)

    def test_missing_start_script_after_install_returns_verify_failed(self):
        """If the installer's java step succeeds but the verify
        script isn't there, EXIT_VERIFY_FAILED."""
        (self.install_dir / "jetty" / "StartJetty.sh").unlink()
        with unittest.mock.patch.object(ic, "_run") as mock_run:
            mock_run.return_value = ic.EXIT_OK
            rc = self._install()
        self.assertEqual(rc, ic.EXIT_VERIFY_FAILED)

    def test_reset_wipes_install_dir(self):
        # Pre-populate install_dir with a junk file; reset should remove it.
        junk = self.install_dir / "junk.txt"
        junk.write_text("delete me")
        with unittest.mock.patch.object(ic, "_run") as mock_run:
            mock_run.return_value = ic.EXIT_OK
            ic.install(
                script_path=Path(__file__).resolve(),
                jar=self.jar,
                install_dir=self.install_dir,
                reset=True,
                dry_run=False,
            )
        self.assertFalse(junk.exists())


class TestResolveJavaHome(unittest.TestCase):
    def test_java_home_from_env(self):
        with unittest.mock.patch.dict(
            "os.environ", {"JAVA_HOME": "/opt/jdk21"}, clear=True
        ):
            self.assertEqual(ic._resolve_java_home(), "/opt/jdk21")

    def test_java_home_21_fallback(self):
        with unittest.mock.patch.dict(
            "os.environ", {"JAVA_HOME_21": "/opt/jdk21-21"}, clear=True
        ):
            self.assertEqual(ic._resolve_java_home(), "/opt/jdk21-21")

    def test_unset_returns_none(self):
        with unittest.mock.patch.dict(
            "os.environ", {}, clear=True
        ):
            self.assertIsNone(ic._resolve_java_home())


class TestResolveJar(unittest.TestCase):
    def test_explicit_jar_arg_wins(self):
        explicit = Path("/tmp/explicit.jar")
        resolved = ic._resolve_jar(Path(__file__).resolve(), explicit)
        self.assertEqual(resolved, explicit.resolve())

    def test_default_resolves_to_maven_target(self):
        # When no --jar is given, the helper walks up from the script to
        # find the repo root + the Maven output path.
        fake_script = Path("/tmp/repo/modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/scripts/install-cms.py")
        resolved = ic._resolve_jar(fake_script, None)
        self.assertTrue(
            str(resolved).endswith(
                "modules/perc-distribution-tree/target/perc-distribution-tree.jar"
            )
        )


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            ic.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


if __name__ == "__main__":
    logging.getLogger("install-cms").setLevel(logging.CRITICAL)
    unittest.main()