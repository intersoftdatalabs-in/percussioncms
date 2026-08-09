#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for docker/scripts/qa_rebuild_chain.py (#2533).

No real Maven: dry-run exercises planning; real-mode uses a stubbed
``subprocess.run`` that records argv/cwd and returns CompletedProcess.
"""

from __future__ import annotations

import importlib.util
import io
import subprocess
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from typing import Any, List, Optional

SCRIPTS = Path(__file__).resolve().parent

_spec = importlib.util.spec_from_file_location(
    "qa_rebuild_chain", SCRIPTS / "qa_rebuild_chain.py"
)
qa_rebuild_chain = importlib.util.module_from_spec(_spec)
assert _spec.loader is not None
sys.modules["qa_rebuild_chain"] = qa_rebuild_chain
_spec.loader.exec_module(qa_rebuild_chain)


class _FakeRun:
    """Stub subprocess.run: record calls, return configurable codes."""

    def __init__(self, returncodes: Optional[List[int]] = None) -> None:
        self.calls: List[dict[str, Any]] = []
        self.returncodes = list(returncodes) if returncodes is not None else []
        self._i = 0

    def __call__(self, argv, **kwargs):  # noqa: ANN001
        self.calls.append({"argv": list(argv), **kwargs})
        code = 0
        if self._i < len(self.returncodes):
            code = self.returncodes[self._i]
        self._i += 1
        return subprocess.CompletedProcess(argv, code)


class _RepoLayout:
    def __init__(self) -> None:
        self.td = tempfile.TemporaryDirectory()
        self.root = Path(self.td.name) / "repo"
        self.root.mkdir()
        # Module dirs the chain requires
        (self.root / "projects" / "sitemanage").mkdir(parents=True)
        (self.root / "WebUI").mkdir(parents=True)
        (self.root / "modules" / "perc-distribution-tree").mkdir(parents=True)
        (self.root / "docker" / "logs").mkdir(parents=True)
        # Wrappers — both names so tests pass on any OS without depending
        # on sys.platform for file presence.
        (self.root / "mvnw").write_text("#!/bin/sh\necho stub\n", encoding="utf-8")
        (self.root / "mvnw.cmd").write_text("@echo stub\r\n", encoding="utf-8")

    def cleanup(self) -> None:
        self.td.cleanup()


class TestPlanChain(unittest.TestCase):
    def test_full_chain_order_and_goals(self):
        steps = qa_rebuild_chain.plan_chain()
        self.assertEqual(len(steps), 3)
        self.assertEqual(steps[0].label, "qa-rebuild-sitemanage")
        self.assertEqual(steps[0].goals, ("clean", "install"))
        self.assertEqual(steps[0].extra_args, ())
        self.assertEqual(steps[1].label, "qa-rebuild-webui")
        self.assertEqual(steps[1].goals, ("package",))
        self.assertIn("-DskipTests", steps[1].extra_args)
        self.assertEqual(steps[2].label, "qa-rebuild-dist")
        self.assertEqual(steps[2].goals, ("clean", "package"))
        self.assertIn("-DskipTests", steps[2].extra_args)

    def test_skip_tests_adds_flag_to_sitemanage(self):
        steps = qa_rebuild_chain.plan_chain(skip_tests=True)
        self.assertIn("-DskipTests", steps[0].extra_args)

    def test_dist_only_single_step(self):
        steps = qa_rebuild_chain.plan_chain(dist_only=True)
        self.assertEqual(len(steps), 1)
        self.assertEqual(steps[0].label, "qa-rebuild-dist")
        self.assertEqual(steps[0].goals, ("package",))


class TestResolveMvnw(unittest.TestCase):
    def test_resolve_picks_platform_wrapper_name(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            mvnw = qa_rebuild_chain.resolve_mvnw(root)
            if sys.platform.startswith("win"):
                self.assertTrue(str(mvnw).endswith("mvnw.cmd"))
            else:
                self.assertTrue(mvnw.name == "mvnw")


class TestDryRun(unittest.TestCase):
    def setUp(self):
        self.layout = _RepoLayout()
        self.addCleanup(self.layout.cleanup)

    def test_dry_run_prints_planned_and_result_ok_no_subprocess(self):
        fake = _FakeRun()
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_rebuild_chain.run_chain(
                self.layout.root,
                dry_run=True,
                run_fn=fake,
            )
        out = buf.getvalue()
        self.assertEqual(rc, qa_rebuild_chain.EXIT_OK)
        self.assertEqual(fake.calls, [])
        self.assertIn("PLANNED STEP:qa-rebuild-sitemanage", out)
        self.assertIn("PLANNED STEP:qa-rebuild-webui", out)
        self.assertIn("PLANNED STEP:qa-rebuild-dist", out)
        self.assertIn("RESULT:OK STEP:qa-rebuild-sitemanage", out)
        self.assertIn("RESULT:OK STEP:qa-rebuild-webui", out)
        self.assertIn("RESULT:OK STEP:qa-rebuild-dist", out)
        self.assertIn("RESULT:OK STEP:qa-rebuild-chain", out)
        # Planned argv must include wrapper + goals
        self.assertIn("clean", out)
        self.assertIn("install", out)
        self.assertIn("package", out)

    def test_dry_run_dist_only(self):
        fake = _FakeRun()
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_rebuild_chain.run_chain(
                self.layout.root,
                dry_run=True,
                dist_only=True,
                run_fn=fake,
            )
        out = buf.getvalue()
        self.assertEqual(rc, qa_rebuild_chain.EXIT_OK)
        self.assertNotIn("qa-rebuild-sitemanage", out)
        self.assertIn("PLANNED STEP:qa-rebuild-dist", out)
        self.assertIn("RESULT:OK STEP:qa-rebuild-chain", out)


class TestRealModeStubbed(unittest.TestCase):
    def setUp(self):
        self.layout = _RepoLayout()
        self.addCleanup(self.layout.cleanup)

    def test_success_runs_three_steps_shell_false(self):
        fake = _FakeRun(returncodes=[0, 0, 0])
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_rebuild_chain.run_chain(
                self.layout.root,
                dry_run=False,
                skip_tests=True,
                log_dir=self.layout.root / "docker" / "logs",
                run_fn=fake,
            )
        out = buf.getvalue()
        self.assertEqual(rc, qa_rebuild_chain.EXIT_OK)
        self.assertEqual(len(fake.calls), 3)
        for call in fake.calls:
            self.assertIs(call.get("shell"), False)
            self.assertIn("cwd", call)
            argv = call["argv"]
            self.assertTrue(
                argv[0].endswith("mvnw") or argv[0].endswith("mvnw.cmd"),
                msg=f"argv0 should be mvnw wrapper, got {argv[0]!r}",
            )

        # Order: sitemanage → WebUI → dist
        cwd0 = Path(fake.calls[0]["cwd"])
        cwd1 = Path(fake.calls[1]["cwd"])
        cwd2 = Path(fake.calls[2]["cwd"])
        self.assertEqual(cwd0.name, "sitemanage")
        self.assertEqual(cwd1.name, "WebUI")
        self.assertEqual(cwd2.name, "perc-distribution-tree")

        # sitemanage with skip_tests
        self.assertIn("-DskipTests", fake.calls[0]["argv"])
        self.assertIn("install", fake.calls[0]["argv"])
        self.assertIn("package", fake.calls[1]["argv"])
        self.assertIn("package", fake.calls[2]["argv"])
        self.assertIn("RESULT:OK STEP:qa-rebuild-chain", out)

    def test_failure_stops_chain(self):
        fake = _FakeRun(returncodes=[0, 1, 0])
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_rebuild_chain.run_chain(
                self.layout.root,
                dry_run=False,
                log_dir=self.layout.root / "docker" / "logs",
                run_fn=fake,
            )
        out = buf.getvalue()
        self.assertEqual(rc, qa_rebuild_chain.EXIT_SUBPROCESS_FAILED)
        # Second step fails; third must not run
        self.assertEqual(len(fake.calls), 2)
        self.assertIn("RESULT:FAIL STEP:qa-rebuild-webui", out)
        self.assertIn("RESULT:FAIL STEP:qa-rebuild-chain", out)
        self.assertNotIn("RESULT:OK STEP:qa-rebuild-dist", out)

    def test_missing_wrapper_is_invocation_error(self):
        # Remove both wrappers
        for name in ("mvnw", "mvnw.cmd"):
            p = self.layout.root / name
            if p.exists():
                p.unlink()
        fake = _FakeRun()
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_rebuild_chain.run_chain(
                self.layout.root,
                dry_run=False,
                run_fn=fake,
            )
        self.assertEqual(rc, qa_rebuild_chain.EXIT_INVOCATION)
        self.assertEqual(fake.calls, [])
        self.assertIn("RESULT:FAIL STEP:qa-rebuild-chain", buf.getvalue())


class TestMainCli(unittest.TestCase):
    def setUp(self):
        self.layout = _RepoLayout()
        self.addCleanup(self.layout.cleanup)

    def test_main_dry_run_exit_zero(self):
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_rebuild_chain.main(
                [
                    "--repo-root",
                    str(self.layout.root),
                    "--dry-run",
                    "--skip-tests",
                ]
            )
        self.assertEqual(rc, 0)
        self.assertIn("RESULT:OK STEP:qa-rebuild-chain", buf.getvalue())


if __name__ == "__main__":
    unittest.main()
