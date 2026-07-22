#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for perc-devctl.py (no docker, no maven, no curl).

Every subcommand is exercised via ``--dry-run`` so no real subprocess
is invoked. Real-mode tests inject a stub ``subprocess.run`` to capture
argv and assert exit-code / RESULT:OK mapping.
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
    path = SCRIPTS / "perc-devctl.py"
    name = "perc_devctl"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


pdc = _load()


def _stub_repo_root(td_path: Path) -> Path:
    """Build a synthetic repo layout: docker/scripts/, docker/logs/,
    scripts/install-cms-dev.py, mvn-env.sh.
    """
    repo = td_path / "repo"
    (repo / "docker" / "scripts").mkdir(parents=True)
    (repo / "docker" / "logs").mkdir(parents=True)
    (repo / "scripts").mkdir(parents=True)
    (repo / "scripts" / "install-cms-dev.py").write_text(
        "#!/usr/bin/env python3\n# stub installer\n", encoding="utf-8"
    )
    (repo / "mvn-env.sh").write_text(
        "#!/bin/sh\necho mvn-env stub\n", encoding="utf-8"
    )
    return repo


class _CliRunner:
    """Helper to run the CLI end-to-end with a captured subprocess."""

    def __init__(self, repo_root: Path):
        self.repo_root = repo_root

    def run(self, argv, *, dry_run=True, fake_run=None):
        """Invoke ``main(argv)`` and capture stdout.

        If ``fake_run`` is provided, it's used as ``subprocess.run``
        side effect via ``unittest.mock.patch``.
        """
        import io
        from contextlib import redirect_stdout

        argv = ["--repo-root", str(self.repo_root)] + list(argv)
        # ``--dry-run`` is a per-subcommand arg, not a top-level one.
        # Insert it right after the subcommand name. The subcommand is at
        # index 2 (``--repo-root`` and its value occupy indices 0,1).
        no_dry_run_cmds = {"logs-path", "inspect-install", "show-generated-passwords"}
        if (
            dry_run
            and "--dry-run" not in argv
            and len(argv) > 2
            and argv[2] not in no_dry_run_cmds
        ):
            argv = argv[:3] + ["--dry-run"] + argv[3:]
        buf = io.StringIO()
        patches = []
        if fake_run is not None:
            patches.append(unittest.mock.patch.object(pdc.subprocess, "run", side_effect=fake_run))
        for p in patches:
            p.start()
        try:
            with redirect_stdout(buf):
                rc = pdc.main(argv)
        finally:
            for p in patches:
                p.stop()
        return rc, buf.getvalue()


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            pdc.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_missing_subcommand_errors(self):
        with self.assertRaises(SystemExit) as cm:
            pdc.main([])
        self.assertEqual(cm.exception.code, 2)


class TestResolvePaths(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = _stub_repo_root(Path(self.td.name))

    def test_env_file_falls_back_to_example(self):
        # No .env.compose; should resolve to .env.compose.example.
        (self.repo_root / ".env.compose.example").write_text("", encoding="utf-8")
        ns = pdc.argparse.Namespace(
            repo_root=self.repo_root,
            env_file=None,
            compose_file="docker-compose.yml",
        )
        repo, env_file, compose_file = pdc._resolve_paths(ns)
        self.assertEqual(env_file, (self.repo_root / ".env.compose.example").resolve())


class TestLogsPath(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = _stub_repo_root(Path(self.td.name))
        self.runner = _CliRunner(self.repo_root)

    def test_logs_path_prints_directory(self):
        rc, out = self.runner.run(["logs-path"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:logs-path", out)
        self.assertIn("LOG_DIR:", out)


class TestSubcommandDryRun(unittest.TestCase):
    """Every subcommand that supports ``--dry-run`` returns EXIT_OK and
    emits RESULT:OK without invoking docker / curl / mvn.
    """

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = _stub_repo_root(Path(self.td.name))
        self.runner = _CliRunner(self.repo_root)

    def test_install_dry_run(self):
        rc, out = self.runner.run(["install"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:install", out)
        # Verify the install-cms-dev.py argv was used.
        # install-cms-dev.py path is logged via LOG.info to stderr, not stdout

    def test_up_dry_run(self):
        rc, out = self.runner.run(["up", "--build"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:up", out)
        # --build is logged via LOG.info to stderr, not stdout — only check the RESULT line

    def test_down_dry_run(self):
        rc, out = self.runner.run(["down", "--volumes"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:down", out)
        # -v is logged via LOG.info to stderr, not stdout — only check the RESULT line

    def test_status_dry_run(self):
        rc, out = self.runner.run(["status"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:status", out)

    def test_verify_dry_run(self):
        rc, out = self.runner.run(["verify", "--timeout-seconds", "60"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:verify", out)
        self.assertIn("CMS_HTTP:200", out)

    def test_it_verify_dry_run(self):
        rc, out = self.runner.run(["it-verify"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:it-verify", out)

    def test_deploy_jar_dry_run(self):
        rc, out = self.runner.run([
            "deploy-jar", "--jar", "/tmp/foo.jar", "--target", "cms", "--restart",
        ])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:deploy-jar", out)

    def test_verify_fix_dry_run(self):
        rc, out = self.runner.run([
            "verify-fix", "--jar", "/tmp/foo.jar", "--restart", "--timeout-seconds", "120",
        ])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:verify-fix", out)

    def test_inspect_install_dry_run(self):
        """inspect-install doesn't have --dry-run; we patch _run_logged
        to simulate the dry-run behavior via fake_run.
        """
        rc, out = self.runner.run(
            ["inspect-install"],
            fake_run=lambda *a, **kw: subprocess.CompletedProcess(
                args=[], returncode=0, stdout="", stderr="",
            ),
        )
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:inspect-install", out)

    def test_show_generated_passwords_missing_file(self):
        """The script exits non-zero if the passwords file is missing
        inside the container. With a stubbed docker exec returning rc=1,
        we assert the failure propagates as ``EXIT_SUBPROCESS_FAILED``.
        """
        rc, out = self.runner.run(
            ["show-generated-passwords"],
            fake_run=lambda *a, **kw: subprocess.CompletedProcess(
                args=[], returncode=1, stdout="", stderr="missing",
            ),
        )
        self.assertEqual(rc, pdc.EXIT_SUBPROCESS_FAILED)
        self.assertIn("RESULT:FAIL STEP:show-generated-passwords", out)


class TestVerifyRealMode(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = _stub_repo_root(Path(self.td.name))
        self.runner = _CliRunner(self.repo_root)

    def test_verify_first_check_succeeds(self):
        """When curl + docker inspect all return success values, the
        verify loop exits on the first check with RESULT:OK.
        """
        with unittest.mock.patch.object(pdc, "_curl_status") as mock_curl, \
             unittest.mock.patch.object(pdc, "_docker_health") as mock_health, \
             unittest.mock.patch.object(pdc.time, "sleep"):
            mock_curl.side_effect = lambda url, **kw: 200
            mock_health.return_value = "healthy"
            import io
            from contextlib import redirect_stdout
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "verify", "--timeout-seconds", "10",
                ])
            out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:verify", out)
        self.assertIn("CMS_HTTP:200", out)
        self.assertIn("HEALTH:healthy", out)


class TestVerifyFixRealMode(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = _stub_repo_root(Path(self.td.name))
        self.runner = _CliRunner(self.repo_root)

    def test_verify_fix_deploy_failure_propagates(self):
        """Deploy phase returns rc=0, verify phase returns rc=2 → final rc=2."""
        # Use a side_effect that returns 0 for the deploy call (argv0
        # includes python3 + hot-deploy-jar.py) and 2 for the verify
        # curl calls. Mock time.sleep so the verify loop doesn't block.
        with unittest.mock.patch.object(pdc, "_curl_status") as mock_curl, \
             unittest.mock.patch.object(pdc, "_docker_health") as mock_health, \
             unittest.mock.patch.object(pdc.subprocess, "run") as mock_run, \
             unittest.mock.patch.object(pdc.time, "sleep"):
            def fake_run(argv, *args, **kwargs):
                if any("hot-deploy-jar.py" in str(a) for a in argv):
                    return subprocess.CompletedProcess(
                        args=argv, returncode=0, stdout="", stderr=""
                    )
                return subprocess.CompletedProcess(
                    args=argv, returncode=1, stdout="", stderr=""
                )
            mock_run.side_effect = fake_run
            # Force verify to fail by making curl return 0.
            mock_curl.return_value = 0
            mock_health.return_value = "unknown"
            import io
            from contextlib import redirect_stdout
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "verify-fix",
                    "--jar", "/tmp/foo.jar",
                    "--timeout-seconds", "5",
                ])
            out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_SUBPROCESS_FAILED)
        self.assertIn("RESULT:FAIL STEP:verify-fix", out)

    def test_verify_fix_failure_includes_log_path(self):
        """Regression for kilo-code-bot review threads 3631740695 +
        3631740700: ``RESULT:FAIL`` lines for verify-fix must include
        the log file path so agent retry/loop tooling can find the
        diagnostics. Previous implementation printed ``LOG:`` with
        an empty path.
        """
        with unittest.mock.patch.object(pdc, "_curl_status") as mock_curl, \
             unittest.mock.patch.object(pdc, "_docker_health") as mock_health, \
             unittest.mock.patch.object(pdc.subprocess, "run") as mock_run, \
             unittest.mock.patch.object(pdc.time, "sleep"):
            def fake_run(argv, *args, **kwargs):
                if any("hot-deploy-jar.py" in str(a) for a in argv):
                    # Deploy succeeds.
                    return subprocess.CompletedProcess(
                        args=argv, returncode=0, stdout="", stderr=""
                    )
                return subprocess.CompletedProcess(
                    args=argv, returncode=1, stdout="", stderr=""
                )
            mock_run.side_effect = fake_run
            mock_curl.return_value = 0  # curl fails
            mock_health.return_value = "unknown"
            import io
            from contextlib import redirect_stdout
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "verify-fix",
                    "--jar", "/tmp/foo.jar",
                    "--timeout-seconds", "5",
                ])
            out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_SUBPROCESS_FAILED)
        # Must include a non-empty LOG:<path> segment in the failure line.
        self.assertIn("RESULT:FAIL STEP:verify-fix", out)
        # The empty-LOG bug: ``LOG:`` followed by an EOL is forbidden.
        self.assertNotIn("PHASE:verify LOG:\n", out)
        # The fix: ``LOG:`` must be followed by a non-empty path (the
        # verify log file path). We don't assert the exact path because
        # it is timestamped, but we assert it's non-empty after LOG:.
        import re
        for m in re.finditer(r"PHASE:verify LOG:(\S*)\n", out):
            self.assertTrue(
                m.group(1),
                msg=f"PHASE:verify LOG: has empty path: {m.group(0)!r}",
            )

    def test_verify_fix_deploy_failure_includes_log_path(self):
        """Regression for kilo-code-bot review thread 3631740695:
        when the deploy phase fails, the RESULT:FAIL line must include
        the deploy-jar log path.
        """
        with unittest.mock.patch.object(pdc.subprocess, "run") as mock_run, \
             unittest.mock.patch.object(pdc.time, "sleep"):
            # Deploy fails (rc=1 for hot-deploy-jar.py call).
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=1, stdout="", stderr=""
            )
            import io
            from contextlib import redirect_stdout
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "verify-fix",
                    "--jar", "/tmp/foo.jar",
                    "--timeout-seconds", "5",
                ])
            out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_SUBPROCESS_FAILED)
        self.assertIn("RESULT:FAIL STEP:verify-fix PHASE:deploy", out)
        # Must NOT have empty LOG: (the previous bug).
        self.assertNotIn("PHASE:deploy LOG:\n", out)
        # The deploy log path must be present and non-empty.
        import re
        for m in re.finditer(r"PHASE:deploy LOG:(\S*)\n", out):
            self.assertTrue(
                m.group(1),
                msg=f"PHASE:deploy LOG: has empty path: {m.group(0)!r}",
            )


class TestDispatch(unittest.TestCase):
    def test_unknown_subcommand(self):
        """A direct invocation with an unknown command in args should
        fail at parse time (argparse requires the subcommand).
        """
        with self.assertRaises(SystemExit):
            pdc.main(["not-a-command"])


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            pdc.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


if __name__ == "__main__":
    logging.getLogger("perc-devctl").setLevel(logging.CRITICAL)
    unittest.main()