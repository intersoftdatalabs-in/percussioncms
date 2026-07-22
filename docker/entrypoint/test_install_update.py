#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for install-update.py (no docker, no Jetty, no DTS).

``--dry-run`` exercises the full wiring without invoking subprocess;
real-mode tests inject a stub ``subprocess.run`` to capture invocations
and assert exit-code mapping.
"""

from __future__ import annotations

import importlib.util
import logging
import subprocess
import sys
import tempfile
import unittest
import unittest.mock
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "install-update.py"
    name = "install_update"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


iu = _load()


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            iu.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_unknown_service_mode_errors(self):
        with self.assertRaises(SystemExit) as cm:
            iu.main(["--service-mode", "bogus"])
        self.assertEqual(cm.exception.code, 2)


class TestDryRun(unittest.TestCase):
    """``--dry-run`` exercises the full wiring without invoking subprocess."""

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.install_root = Path(self.td.name)
        # Lay down a realistic CMS tree.
        (self.install_root / "jetty").mkdir(parents=True, exist_ok=True)
        (self.install_root / "jetty" / "StartJetty.sh").write_text(
            "#!/bin/sh\necho CMS started\n", encoding="utf-8"
        )
        (self.install_root).mkdir(parents=True, exist_ok=True)
        (self.install_root / "TomcatStartup.sh").write_text(
            "#!/bin/sh\necho DTS started\n", encoding="utf-8"
        )

        logging.getLogger("install-update").setLevel(logging.CRITICAL)

    def test_dry_run_cms_dts(self):
        rc = iu.run(
            install_root=self.install_root,
            service_mode="cms-dts",
            dry_run=True,
        )
        self.assertEqual(rc, iu.EXIT_OK)

    def test_dry_run_cms_only(self):
        rc = iu.run(
            install_root=self.install_root,
            service_mode="cms",
            dry_run=True,
        )
        self.assertEqual(rc, iu.EXIT_OK)

    def test_dry_run_dts_only(self):
        rc = iu.run(
            install_root=self.install_root,
            service_mode="dts",
            dry_run=True,
        )
        self.assertEqual(rc, iu.EXIT_OK)


class TestSanityChecks(unittest.TestCase):
    """Real mode without --dry-run — exercises the install_root sanity check."""

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.install_root = Path(self.td.name)
        logging.getLogger("install-update").setLevel(logging.CRITICAL)

    def test_missing_install_exits_one(self):
        """No StartJetty.sh -> EXIT_INSTALL_MISSING (1)."""
        rc = iu.run(
            install_root=self.install_root,
            service_mode="cms",
            dry_run=False,
        )
        self.assertEqual(rc, iu.EXIT_INSTALL_MISSING)

    def test_unsupported_service_mode_exits_three(self):
        """Bypasses the sanity check (by laying down StartJetty.sh) so we
        can hit the unsupported-mode branch."""
        (self.install_root / "jetty").mkdir(parents=True, exist_ok=True)
        (self.install_root / "jetty" / "StartJetty.sh").write_text(
            "#!/bin/sh\n", encoding="utf-8"
        )
        rc = iu.run(
            install_root=self.install_root,
            service_mode="bogus",
            dry_run=False,
        )
        self.assertEqual(rc, iu.EXIT_UNSUPPORTED_MODE)


class TestRealRun(unittest.TestCase):
    """Real mode with a stubbed subprocess.run that succeeds."""

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.install_root = Path(self.td.name)
        (self.install_root / "jetty").mkdir(parents=True, exist_ok=True)
        (self.install_root / "jetty" / "StartJetty.sh").write_text(
            "#!/bin/sh\n", encoding="utf-8"
        )
        (self.install_root).mkdir(parents=True, exist_ok=True)
        (self.install_root / "TomcatStartup.sh").write_text(
            "#!/bin/sh\n", encoding="utf-8"
        )
        logging.getLogger("install-update").setLevel(logging.CRITICAL)

    def test_real_run_cms_dts_starts_both(self):
        # Block the final os.execvp call by patching it out — we don't
        # want the test process to replace itself with `tail -F`.
        with unittest.mock.patch.object(
            iu.os, "execvp", side_effect=RuntimeError("execvp blocked")
        ) as mock_execvp, unittest.mock.patch.object(
            iu.subprocess, "run"
        ) as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=0, stdout="", stderr=""
            )
            with self.assertRaises(RuntimeError):
                iu.run(
                    install_root=self.install_root,
                    service_mode="cms-dts",
                    dry_run=False,
                )
            # Both StartJetty.sh and TomcatStartup.sh should have been
            # invoked via subprocess.run.
            cms_calls = [
                c for c in mock_run.call_args_list
                if c.args and c.args[0] and c.args[0][0].endswith("StartJetty.sh")
            ]
            dts_calls = [
                c for c in mock_run.call_args_list
                if c.args and c.args[0] and c.args[0][0].endswith("TomcatStartup.sh")
            ]
            self.assertEqual(len(cms_calls), 1)
            self.assertEqual(len(dts_calls), 1)
            # And the execvp for tail -F was attempted.
            self.assertEqual(mock_execvp.call_count, 1)

    def test_real_run_start_failure_propagates(self):
        """If StartJetty.sh fails, run() returns EXIT_START_FAILED (2)."""
        with unittest.mock.patch.object(iu.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=1, stdout="", stderr="startup failed"
            )
            rc = iu.run(
                install_root=self.install_root,
                service_mode="cms",
                dry_run=False,
            )
        self.assertEqual(rc, iu.EXIT_START_FAILED)

    def test_real_run_dts_uses_fallback_when_primary_missing(self):
        """When ``TomcatStartup.sh`` is missing but ``startup.sh`` is present,
        ``start_dts`` uses the fallback.
        """
        # Remove primary; leave fallback.
        (self.install_root / "TomcatStartup.sh").unlink()
        (self.install_root).mkdir(parents=True, exist_ok=True)
        (self.install_root / "startup.sh").write_text(
            "#!/bin/sh\n", encoding="utf-8"
        )
        with unittest.mock.patch.object(
            iu.os, "execvp", side_effect=RuntimeError("execvp blocked")
        ), unittest.mock.patch.object(iu.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=0, stdout="", stderr=""
            )
            with self.assertRaises(RuntimeError):
                iu.run(
                    install_root=self.install_root,
                    service_mode="dts",
                    dry_run=False,
                )
            dts_calls = [
                c for c in mock_run.call_args_list
                if c.args and c.args[0] and c.args[0][0].endswith("startup.sh")
            ]
            self.assertEqual(len(dts_calls), 1)


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            iu.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


if __name__ == "__main__":
    logging.getLogger("install-update").setLevel(logging.CRITICAL)
    unittest.main()