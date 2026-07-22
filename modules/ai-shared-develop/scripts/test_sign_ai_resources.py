#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for sign-ai-resources.py (no Maven, no Sigstore, no network).

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
    path = SCRIPTS / "sign-ai-resources.py"
    name = "sign_ai_resources"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


sar = _load()


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            sar.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


class TestIsExcluded(unittest.TestCase):
    def test_sha256_excluded(self):
        self.assertTrue(sar._is_excluded(Path("/tmp/foo.sha256")))

    def test_sha256_sig_excluded(self):
        self.assertTrue(sar._is_excluded(Path("/tmp/foo.sha256.sig")))

    def test_sigstore_json_excluded(self):
        self.assertTrue(sar._is_excluded(Path("/tmp/foo.sigstore.json")))

    def test_normal_file_not_excluded(self):
        self.assertFalse(sar._is_excluded(Path("/tmp/SKILL.md")))


class TestCollectResources(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = Path(self.td.name)

        # Lay down a realistic layout.
        dev = self.repo_root / "modules" / "ai-shared-develop" / "src" / "main" / "resources"
        for sub in ("skills", "instructions", "prompts"):
            d = dev / sub
            d.mkdir(parents=True, exist_ok=True)
            (d / "fixture.md").write_text(f"# {sub} fixture", encoding="utf-8")
        rel = self.repo_root / "modules" / "ai-shared-release" / "src" / "main" / "resources"
        (rel / "skills").mkdir(parents=True, exist_ok=True)
        (rel / "skills" / "rel-fixture.md").write_text("# rel fixture", encoding="utf-8")
        (self.repo_root / "AGENTS.md").write_text("# root agents", encoding="utf-8")
        # Module-level AGENTS.md
        (self.repo_root / "modules" / "perc-jetty").mkdir(parents=True, exist_ok=True)
        (self.repo_root / "modules" / "perc-jetty" / "AGENTS.md").write_text(
            "# perc-jetty agents", encoding="utf-8"
        )

    def test_collects_every_category(self):
        resources = sar._collect_resources(self.repo_root)
        names = [r.name for r in resources]
        # dev fixtures
        self.assertIn("fixture.md", names)  # appears 3x but set-deduped
        # rel fixture
        self.assertIn("rel-fixture.md", names)
        # AGENTS.md files
        self.assertIn("AGENTS.md", names)  # root + module, deduped by basename only if identical paths; here we have root + perc-jetty, so 2 entries
        # Confirm root and module-level AGENTS.md are both found
        agents_paths = [r for r in resources if r.name == "AGENTS.md"]
        self.assertGreaterEqual(len(agents_paths), 2)

    def test_excludes_sha256_and_sigstore(self):
        # Add sidecar files that should be excluded.
        dev = self.repo_root / "modules" / "ai-shared-develop" / "src" / "main" / "resources" / "skills"
        (dev / "x.md").write_text("plain", encoding="utf-8")
        (dev / "x.md.sha256").write_text("abc", encoding="utf-8")
        (dev / "x.md.sha256.sig").write_text("def", encoding="utf-8")
        (dev / "x.md.sigstore.json").write_text("{}", encoding="utf-8")
        resources = sar._collect_resources(self.repo_root)
        names = [r.name for r in resources]
        self.assertIn("x.md", names)
        self.assertNotIn("x.md.sha256", names)
        self.assertNotIn("x.md.sha256.sig", names)
        self.assertNotIn("x.md.sigstore.json", names)

    def test_no_resources_returns_empty_list(self):
        empty = tempfile.TemporaryDirectory()
        self.addCleanup(empty.cleanup)
        resources = sar._collect_resources(Path(empty.name))
        self.assertEqual(resources, [])


class TestDryRun(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = Path(self.td.name)
        # One skill to find.
        dev = self.repo_root / "modules" / "ai-shared-develop" / "src" / "main" / "resources"
        (dev / "skills").mkdir(parents=True, exist_ok=True)
        (dev / "skills" / "x.md").write_text("x", encoding="utf-8")
        # Stub mvn to satisfy _resolve_maven_argv0 path (script path is the real one; we override mvn_argv0).
        logging.getLogger("sign-ai-resources").setLevel(logging.CRITICAL)

    def test_dry_run_with_resources(self):
        rc = sar.sign(
            repo_root=self.repo_root,
            mvn_argv0=["mvn"],  # stub
            dry_run=True,
            no_build=False,
        )
        self.assertEqual(rc, sar.EXIT_OK)

    def test_dry_run_with_no_resources_returns_two(self):
        empty = tempfile.TemporaryDirectory()
        self.addCleanup(empty.cleanup)
        rc = sar.sign(
            repo_root=Path(empty.name),
            mvn_argv0=["mvn"],
            dry_run=True,
            no_build=True,  # skip the build, which would otherwise succeed with stub
        )
        self.assertEqual(rc, sar.EXIT_NO_RESOURCES)


class TestRealRun(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = Path(self.td.name)
        dev = self.repo_root / "modules" / "ai-shared-develop" / "src" / "main" / "resources"
        (dev / "skills").mkdir(parents=True, exist_ok=True)
        (dev / "skills" / "x.md").write_text("x", encoding="utf-8")
        logging.getLogger("sign-ai-resources").setLevel(logging.CRITICAL)

    def test_real_run_maven_failure_propagates(self):
        with unittest.mock.patch.object(sar.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=1, stdout="", stderr=""
            )
            rc = sar.sign(
                repo_root=self.repo_root,
                mvn_argv0=["mvn"],
                dry_run=False,
                no_build=False,
            )
        # Build invocation failed -> EXIT_INVOCATION
        self.assertEqual(rc, sar.EXIT_INVOCATION)

    def test_real_run_no_build_skips_build_invocation(self):
        """With ``--no-build``, only the signer invocation runs.
        """
        with unittest.mock.patch.object(sar.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=0, stdout="", stderr=""
            )
            rc = sar.sign(
                repo_root=self.repo_root,
                mvn_argv0=["mvn"],
                dry_run=False,
                no_build=True,
            )
        self.assertEqual(rc, sar.EXIT_OK)
        # Exactly one mvn invocation (the signer call).
        self.assertEqual(mock_run.call_count, 1)
        call_args = mock_run.call_args_list[0].args[0]
        # mvn_argv0 + extra_args
        self.assertEqual(call_args[0], "mvn")
        self.assertIn("exec:java", call_args)
        self.assertIn("-Dexec.mainClass=com.percussion.ai.signing.ResourceSigner", call_args)

    def test_real_run_signer_failure_propagates(self):
        """Build succeeds; signer invocation fails; EXIT_INVOCATION.
        """
        with unittest.mock.patch.object(sar.subprocess, "run") as mock_run:
            # Build returns 0; signer returns 1.
            mock_run.side_effect = [
                subprocess.CompletedProcess(args=[], returncode=0, stdout="", stderr=""),
                subprocess.CompletedProcess(args=[], returncode=1, stdout="", stderr=""),
            ]
            rc = sar.sign(
                repo_root=self.repo_root,
                mvn_argv0=["mvn"],
                dry_run=False,
                no_build=False,
            )
        self.assertEqual(rc, sar.EXIT_INVOCATION)
        self.assertEqual(mock_run.call_count, 2)


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            sar.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


if __name__ == "__main__":
    logging.getLogger("sign-ai-resources").setLevel(logging.CRITICAL)
    unittest.main()