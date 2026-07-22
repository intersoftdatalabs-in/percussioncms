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
        # Lay down a realistic CMS tree. The start scripts must be
        # executable for the host install (matches the original
        # `[[ -x ${CMS_START_SCRIPT} ]]` guard that install-update.sh
        # relied on). chmod 0o755 = rwxr-xr-x.
        (self.install_root / "jetty").mkdir(parents=True, exist_ok=True)
        cms_start = self.install_root / "jetty" / "StartJetty.sh"
        cms_start.write_text("#!/bin/sh\necho CMS started\n", encoding="utf-8")
        cms_start.chmod(0o755)
        self.install_root.mkdir(parents=True, exist_ok=True)
        dts_start = self.install_root / "TomcatStartup.sh"
        dts_start.write_text("#!/bin/sh\necho DTS started\n", encoding="utf-8")
        dts_start.chmod(0o755)

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


class TestExecutableBitGuard(unittest.TestCase):
    """Regression tests for kilo-code-bot review threads 3631740684 +
    3631740689: the original ``install-update.sh`` used ``[[ -x ... ]]`` to
    verify the start script was both present AND executable. The first
    Python port only checked ``.is_file()`` so a present-but-not-executable
    ``StartJetty.sh`` would fail at the kernel level with a confusing
    ``Permission denied`` instead of a clear "missing executable bit"
    error. ``_is_executable`` restores the -x guard.
    """

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.install_root = Path(self.td.name)
        logging.getLogger("install-update").setLevel(logging.CRITICAL)

    def _write_non_executable(self, *parts: str) -> Path:
        p = self.install_root.joinpath(*parts)
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text("#!/bin/sh\n", encoding="utf-8")
        p.chmod(0o644)  # no execute bit
        return p

    def _write_executable(self, *parts: str) -> Path:
        p = self.install_root.joinpath(*parts)
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text("#!/bin/sh\n", encoding="utf-8")
        p.chmod(0o755)  # executable
        return p

    def test_cms_non_executable_returns_install_missing(self):
        """A StartJetty.sh with no execute bit must surface as
        EXIT_INSTALL_MISSING (matches the original [[ -x ]] guard).
        """
        if sys.platform.startswith("win"):
            self.skipTest(
                "Windows file permissions are POSIX-incompatible: "
                "os.access(X_OK) returns True even after chmod(0o644); "
                "the executable-bit guard is a Linux/macOS concept "
                "(verified by the Linux-side CI job)."
            )
        self._write_non_executable("jetty", "StartJetty.sh")
        rc = iu.run(
            install_root=self.install_root,
            service_mode="cms",
            dry_run=False,
        )
        self.assertEqual(rc, iu.EXIT_INSTALL_MISSING)

    def test_dts_primary_non_executable_falls_through_to_fallback(self):
        """When ``TomcatStartup.sh`` exists but is not executable AND
        ``startup.sh`` is executable, the DTS start uses the fallback.
        Mirrors the original ``[[ -x ${dts_start_script} ]]`` guard
        that skipped non-executable candidates.
        """
        if sys.platform.startswith("win"):
            self.skipTest(
                "Windows file permissions are POSIX-incompatible: "
                "chmod(0o644) does not strip the executable bit; the "
                "primary-vs-fallback distinction is a Linux/macOS concept."
            )
        # CMS script must exist so `run()`'s upfront sanity check passes
        # before reaching the DTS branch. Use the executable helper to
        # mark it executable.
        self._write_executable("jetty", "StartJetty.sh")
        self._write_non_executable("TomcatStartup.sh")
        # Touch install_root so _start_dts picks startup.sh as the fallback
        # (matches the production install layout).
        self.install_root.mkdir(parents=True, exist_ok=True)
        self._write_executable("startup.sh")
        with unittest.mock.patch.object(iu.os, "execvp", side_effect=RuntimeError("execvp blocked")), \
             unittest.mock.patch.object(iu.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=0, stdout="", stderr=""
            )
            with self.assertRaises(RuntimeError):
                iu.run(
                    install_root=self.install_root,
                    service_mode="dts",
                    dry_run=False,
                )
            startup_calls = [
                c for c in mock_run.call_args_list
                if c.args and c.args[0] and c.args[0][0].endswith("startup.sh")
            ]
            self.assertEqual(
                len(startup_calls), 1,
                msg="fallback startup.sh must be used when primary is not executable",
            )

    def test_dts_neither_executable_returns_install_missing(self):
        """When both primary and fallback exist but neither is
        executable, EXIT_INSTALL_MISSING (not a confusing kernel
        permission error).
        """
        if sys.platform.startswith("win"):
            self.skipTest(
                "Windows file permissions are POSIX-incompatible; "
                "the neither-executable branch is a Linux/macOS concept."
            )
        self._write_non_executable("TomcatStartup.sh")
        self._write_non_executable("startup.sh")
        rc = iu.run(
            install_root=self.install_root,
            service_mode="dts",
            dry_run=False,
        )
        self.assertEqual(rc, iu.EXIT_INSTALL_MISSING)

    def test_is_executable_helper(self):
        """Direct unit test for the _is_executable helper."""
        if sys.platform.startswith("win"):
            self.skipTest(
                "Windows file permissions are POSIX-incompatible; "
                "the _is_executable helper is a Linux/macOS concept."
            )
        exe = self._write_executable("exe.sh")
        non_exe = self._write_non_executable("non-exe.sh")
        missing = self.install_root / "no-such.sh"
        self.assertTrue(iu._is_executable(exe))
        self.assertFalse(iu._is_executable(non_exe))
        self.assertFalse(iu._is_executable(missing))


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
        cms_start = self.install_root / "jetty" / "StartJetty.sh"
        cms_start.write_text("#!/bin/sh\n", encoding="utf-8")
        cms_start.chmod(0o755)  # match original [[ -x ]] guard
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
        cms_start = self.install_root / "jetty" / "StartJetty.sh"
        cms_start.write_text("#!/bin/sh\n", encoding="utf-8")
        cms_start.chmod(0o755)  # match original install-update.sh [[ -x ]] guard
        (self.install_root).mkdir(parents=True, exist_ok=True)
        dts_start = self.install_root / "TomcatStartup.sh"
        dts_start.write_text("#!/bin/sh\n", encoding="utf-8")
        dts_start.chmod(0o755)  # match original [[ -x ]] guard
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
        startup = self.install_root / "startup.sh"
        startup.write_text("#!/bin/sh\n", encoding="utf-8")
        startup.chmod(0o755)  # match original [[ -x ]] guard on the fallback
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