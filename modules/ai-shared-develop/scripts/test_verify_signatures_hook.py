#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for verify-signatures-hook.py (no Maven, no Sigstore, no network).

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
    # Load sign_ai_resources first so verify_signatures_hook can import from it.
    sar_path = SCRIPTS / "sign-ai-resources.py"
    sar_spec = importlib.util.spec_from_file_location("sign_ai_resources", sar_path)
    sar_mod = importlib.util.module_from_spec(sar_spec)
    sys.modules["sign_ai_resources"] = sar_mod
    sar_spec.loader.exec_module(sar_mod)

    path = SCRIPTS / "verify-signatures-hook.py"
    name = "verify_signatures_hook"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


vsh = _load()


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            vsh.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


class TestDryRun(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = Path(self.td.name)
        # One skill to find.
        dev = self.repo_root / "modules" / "ai-shared-develop" / "src" / "main" / "resources"
        (dev / "skills").mkdir(parents=True, exist_ok=True)
        (dev / "skills" / "x.md").write_text("x", encoding="utf-8")
        logging.getLogger("verify-signatures-hook").setLevel(logging.CRITICAL)

    def test_dry_run_with_resources(self):
        rc = vsh.verify(
            repo_root=self.repo_root,
            mvn_argv0=["mvn"],
            dry_run=True,
            no_build=False,
        )
        self.assertEqual(rc, vsh.EXIT_OK)

    def test_dry_run_no_resources_returns_two(self):
        empty = tempfile.TemporaryDirectory()
        self.addCleanup(empty.cleanup)
        rc = vsh.verify(
            repo_root=Path(empty.name),
            mvn_argv0=["mvn"],
            dry_run=True,
            no_build=True,
        )
        self.assertEqual(rc, vsh.EXIT_NO_RESOURCES)


class TestRealRun(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = Path(self.td.name)
        dev = self.repo_root / "modules" / "ai-shared-develop" / "src" / "main" / "resources"
        (dev / "skills").mkdir(parents=True, exist_ok=True)
        (dev / "skills" / "x.md").write_text("x", encoding="utf-8")
        logging.getLogger("verify-signatures-hook").setLevel(logging.CRITICAL)

    def test_real_run_build_failure_propagates(self):
        with unittest.mock.patch.object(vsh.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=1, stdout=b"", stderr=b""
            )
            rc = vsh.verify(
                repo_root=self.repo_root,
                mvn_argv0=["mvn"],
                dry_run=False,
                no_build=False,
            )
        self.assertEqual(rc, vsh.EXIT_INVOCATION)

    def test_real_run_verifier_failure_propagates(self):
        # Build (suppressed) succeeds; verifier invocation fails.
        with unittest.mock.patch.object(vsh.subprocess, "run") as mock_run:
            mock_run.side_effect = [
                subprocess.CompletedProcess(args=[], returncode=0, stdout=b"", stderr=b""),
                subprocess.CompletedProcess(args=[], returncode=1, stdout=b"", stderr=b""),
            ]
            rc = vsh.verify(
                repo_root=self.repo_root,
                mvn_argv0=["mvn"],
                dry_run=False,
                no_build=False,
            )
        self.assertEqual(rc, vsh.EXIT_INVOCATION)

    def test_real_run_no_build_skips_build_invocation(self):
        with unittest.mock.patch.object(vsh.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=0, stdout="", stderr=""
            )
            rc = vsh.verify(
                repo_root=self.repo_root,
                mvn_argv0=["mvn"],
                dry_run=False,
                no_build=True,
            )
        self.assertEqual(rc, vsh.EXIT_OK)
        # Only the verifier call (no build).
        self.assertEqual(mock_run.call_count, 1)
        call_args = mock_run.call_args_list[0].args[0]
        self.assertEqual(call_args[0], "mvn")
        self.assertIn("exec:java", call_args)
        self.assertIn("-Dexec.mainClass=com.percussion.ai.signing.ResourceVerifier", call_args)


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            vsh.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


if __name__ == "__main__":
    logging.getLogger("verify-signatures-hook").setLevel(logging.CRITICAL)
    unittest.main()