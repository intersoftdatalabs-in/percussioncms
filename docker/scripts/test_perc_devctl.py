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
import os
import socket
import subprocess
import sys
import tempfile
import unittest
import unittest.mock
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent

# Env keys that freeport resolution may set or read — clear between tests.
_PORT_ENV_KEYS = (
    "QA_CMS_HOST_PORT",
    "CMS_HOST_PORT",
    "DTS_HOST_PORT",
    "CMS_PORT",
    "DTS_PORT",
    "MYSQL_PORT",
    "POSTGRES_PORT",
    "MSSQL_PORT",
    "VERIFY_CMS_URL",
    "VERIFY_DTS_URL",
    # #2482 — ``qa_cms_probe_url`` honors this override so host + Docker +
    # in-image healthcheck can agree on the probe path.
    "RHYTHMYX_HEALTH_PATH",
)


def _clear_port_env() -> None:
    for key in _PORT_ENV_KEYS:
        os.environ.pop(key, None)


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
    scripts/install-cms-dev.py, mvnw.
    """
    repo = td_path / "repo"
    (repo / "docker" / "scripts").mkdir(parents=True)
    (repo / "docker" / "logs").mkdir(parents=True)
    (repo / "scripts").mkdir(parents=True)
    (repo / "scripts" / "install-cms-dev.py").write_text(
        "#!/usr/bin/env python3\n# stub installer\n", encoding="utf-8"
    )
    (repo / "mvnw").write_text(
        "#!/bin/sh\necho Maven wrapper stub\n", encoding="utf-8"
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


class TestQaPreflight(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = _stub_repo_root(Path(self.td.name))
        self.runner = _CliRunner(self.repo_root)

    def test_qa_preflight_dry_run(self):
        rc, out = self.runner.run(["qa-preflight"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-preflight", out)
        self.assertIn("dry-run", out.lower())


class TestQaRebuildChain(unittest.TestCase):
    """#2533: perc-devctl qa-rebuild-chain dry-run + dispatch."""

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = _stub_repo_root(Path(self.td.name))
        # Module dirs required only for real runs; dry-run still plans them.
        (self.repo_root / "projects" / "sitemanage").mkdir(parents=True)
        (self.repo_root / "WebUI").mkdir(parents=True)
        (self.repo_root / "modules" / "perc-distribution-tree").mkdir(parents=True)
        (self.repo_root / "mvnw.cmd").write_text("@echo stub\r\n", encoding="utf-8")
        self.runner = _CliRunner(self.repo_root)

    def test_qa_rebuild_chain_dry_run(self):
        rc, out = self.runner.run(["qa-rebuild-chain", "--skip-tests"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("PLANNED STEP:qa-rebuild-sitemanage", out)
        self.assertIn("RESULT:OK STEP:qa-rebuild-chain", out)

    def test_qa_rebuild_chain_then_qa_up_dry_run(self):
        _clear_port_env()
        self.addCleanup(_clear_port_env)
        rc, out = self.runner.run(
            ["qa-rebuild-chain", "--skip-tests", "--then-qa-up"]
        )
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-rebuild-chain", out)
        self.assertIn("RESULT:OK STEP:qa-up", out)
        self.assertIn("RESULT:OK STEP:qa-deploy-webui", out)
        self.assertIn("TEST_CMS_URL=", out)

    def test_qa_rebuild_chain_then_qa_up_skip_webui_deploy(self):
        _clear_port_env()
        self.addCleanup(_clear_port_env)
        rc, out = self.runner.run(
            [
                "qa-rebuild-chain",
                "--skip-tests",
                "--then-qa-up",
                "--skip-webui-deploy",
            ]
        )
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-up", out)
        self.assertNotIn("RESULT:OK STEP:qa-deploy-webui", out)

    def test_qa_rebuild_chain_then_qa_deploy_webui_without_up(self):
        rc, out = self.runner.run(
            ["qa-rebuild-chain", "--skip-tests", "--then-qa-deploy-webui"]
        )
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-rebuild-chain", out)
        self.assertIn("RESULT:OK STEP:qa-deploy-webui", out)
        self.assertNotIn("RESULT:OK STEP:qa-up", out)


class TestDockerHealth(unittest.TestCase):
    """#2537 / #2481: ``_docker_health`` maps inspect output to RESULT HEALTH: values."""

    def test_empty_container_name_is_unknown(self):
        self.assertEqual(pdc._docker_health(""), "unknown")

    def test_inspect_failure_is_unknown(self):
        with unittest.mock.patch.object(pdc.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=1, stdout="", stderr="Error: No such object"
            )
            self.assertEqual(pdc._docker_health("missing"), "unknown")
            argv = mock_run.call_args[0][0]
            self.assertEqual(argv[0:3], ["docker", "inspect", "-f"])
            self.assertIn("{{if .State.Health}}", argv[3])
            self.assertEqual(argv[4], "missing")

    def test_health_status_passed_through(self):
        for status in ("healthy", "unhealthy", "starting", "none"):
            with self.subTest(status=status):
                with unittest.mock.patch.object(pdc.subprocess, "run") as mock_run:
                    mock_run.return_value = subprocess.CompletedProcess(
                        args=[], returncode=0, stdout=status + "\n", stderr=""
                    )
                    self.assertEqual(pdc._docker_health("perc-matrix-cms-h2"), status)

    def test_blank_stdout_is_unknown(self):
        with unittest.mock.patch.object(pdc.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=0, stdout="  \n", stderr=""
            )
            self.assertEqual(pdc._docker_health("c"), "unknown")


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
        _clear_port_env()
        self.addCleanup(_clear_port_env)
        rc, out = self.runner.run(["up", "--build"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:up", out)
        # Freeport / preferred host ports emitted for operator discovery (#2001).
        self.assertIn("CMS_PORT=", out)
        self.assertIn("DTS_PORT=", out)
        self.assertIn("VERIFY_CMS_URL=", out)
        self.assertIn("VERIFY_DTS_URL=", out)
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
        _clear_port_env()
        self.addCleanup(_clear_port_env)
        rc, out = self.runner.run(["verify", "--timeout-seconds", "60"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:verify", out)
        self.assertIn("CMS_HTTP:200", out)

    def test_qa_up_dry_run(self):
        _clear_port_env()
        self.addCleanup(_clear_port_env)
        rc, out = self.runner.run(["qa-up", "--timeout-seconds", "60"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-up", out)
        self.assertIn("TEST_CMS_URL=", out)
        self.assertIn("QA_CMS_HOST_PORT=", out)
        self.assertRegex(out, r"TEST_CMS_URL=http://127\.0\.0\.1:\d+")
        self.assertIn(f"ADMIN_USERNAME={pdc.QA_ADMIN_USERNAME}", out)

    def test_qa_health_dry_run(self):
        _clear_port_env()
        self.addCleanup(_clear_port_env)
        rc, out = self.runner.run(["qa-health", "--timeout-seconds", "30"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-health", out)
        self.assertIn("HTTP:200", out)
        self.assertIn("HEALTH:healthy", out)
        self.assertIn(pdc.QA_CMS_CONTAINER, out)

    def test_qa_down_dry_run(self):
        rc, out = self.runner.run(["qa-down"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-down", out)
        self.assertIn(f"QA_CONTAINER:{pdc.QA_CMS_CONTAINER}", out)

    def test_qa_rebuild_chain_dry_run(self):
        (self.repo_root / "projects" / "sitemanage").mkdir(parents=True, exist_ok=True)
        (self.repo_root / "WebUI").mkdir(parents=True, exist_ok=True)
        (self.repo_root / "modules" / "perc-distribution-tree").mkdir(
            parents=True, exist_ok=True
        )
        (self.repo_root / "mvnw.cmd").write_text("@echo stub\r\n", encoding="utf-8")
        rc, out = self.runner.run(["qa-rebuild-chain", "--dist-only"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-rebuild-chain", out)

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

    def test_qa_deploy_webui_dry_run(self):
        rc, out = self.runner.run(["qa-deploy-webui"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-deploy-webui", out)

    def test_qa_deploy_webui_argv_includes_entry_script_and_container(self):
        argv = pdc._qa_deploy_webui_argv(
            Path("/repo"),
            None,
            pdc.QA_CMS_CONTAINER,
            False,
        )
        self.assertEqual(argv[0], sys.executable)
        self.assertTrue(str(argv[1]).replace("\\", "/").endswith(
            "docker/scripts/hot-deploy-webui-modern.py"
        ))
        self.assertIn("--container", argv)
        self.assertIn(pdc.QA_CMS_CONTAINER, argv)
        self.assertNotIn("--skip-object-storage-check", argv)
        self.assertNotIn("--skip-kind-marker-check", argv)
        self.assertNotIn("sh", argv)
        self.assertNotIn("-c", argv)

    def test_qa_up_then_qa_deploy_webui_dry_run(self):
        _clear_port_env()
        self.addCleanup(_clear_port_env)
        rc, out = self.runner.run(["qa-up", "--then-qa-deploy-webui"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-up", out)
        self.assertIn("RESULT:OK STEP:qa-deploy-webui", out)
        self.assertIn("TEST_CMS_URL=", out)
        self.assertNotIn("RESULT:OK STEP:qa-deploy-war-jars", out)

    def test_qa_deploy_war_jars_dry_run(self):
        rc, out = self.runner.run(["qa-deploy-war-jars"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-deploy-war-jars", out)

    def test_qa_deploy_war_jars_argv_includes_script_and_container(self):
        argv = pdc._qa_deploy_war_jars_argv(
            Path("/repo"),
            pdc.QA_CMS_CONTAINER,
            True,
            False,
        )
        self.assertEqual(argv[0], sys.executable)
        self.assertTrue(
            str(argv[1]).replace("\\", "/").endswith(
                "docker/scripts/hot-deploy-rhythmyx-war-jars.py"
            )
        )
        self.assertIn("--container", argv)
        self.assertIn(pdc.QA_CMS_CONTAINER, argv)
        self.assertIn("--restart-jetty", argv)
        self.assertIn("--repo-root", argv)
        self.assertNotIn("sh", argv)
        self.assertNotIn("-c", argv)

    def test_qa_up_skip_image_build_then_webui_implies_war_jars(self):
        _clear_port_env()
        self.addCleanup(_clear_port_env)
        rc, out = self.runner.run(
            ["qa-up", "--skip-image-build", "--then-qa-deploy-webui"]
        )
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-up", out)
        self.assertIn("RESULT:OK STEP:qa-deploy-webui", out)
        self.assertIn("RESULT:OK STEP:qa-deploy-war-jars", out)

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
        self._slog = unittest.mock.patch.object(
            pdc, "_docker_read_server_log", return_value=""
        )
        self._slog.start()
        self.addCleanup(self._slog.stop)

    def test_verify_first_check_succeeds(self):
        """When curl + docker inspect + clean logs all return success values,
        the verify loop exits on the first check with RESULT:OK.
        """
        with unittest.mock.patch.object(pdc, "_curl_status") as mock_curl, \
             unittest.mock.patch.object(pdc, "_docker_health") as mock_health, \
             unittest.mock.patch.object(pdc, "_docker_logs_tail") as mock_logs, \
             unittest.mock.patch.object(pdc.time, "sleep"):
            mock_curl.side_effect = lambda url, **kw: 200
            mock_health.return_value = "healthy"
            mock_logs.return_value = "INFO [Server] Started @7879ms\n"
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
        mock_logs.assert_called()

    def test_verify_fails_when_http_ok_but_context_failed(self):
        """#2480: CMS/DTS HTTP ready + docker healthy + dead Rhythmyx context
        must FAIL (not OK), and fail-fast without burning the poll budget.
        """
        dead_ctx = (
            "WARN  [WebAppContext] Failed startup of context "
            "oeje11w.WebAppContext Rhythmyx\n"
            "BeanCurrentlyInCreationException: folderHelper\n"
            "INFO  [AbstractConnector] Started {HTTP/1.1}{0.0.0.0:9992}\n"
        )
        with unittest.mock.patch.object(pdc, "_curl_status") as mock_curl, \
             unittest.mock.patch.object(pdc, "_docker_health") as mock_health, \
             unittest.mock.patch.object(pdc, "_docker_logs_tail") as mock_logs, \
             unittest.mock.patch.object(pdc.time, "sleep") as mock_sleep:
            mock_curl.return_value = 200
            mock_health.return_value = "healthy"
            mock_logs.return_value = dead_ctx
            import io
            from contextlib import redirect_stdout
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "verify",
                    "--timeout-seconds", "30",
                    "--interval-seconds", "5",
                ])
            out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_SUBPROCESS_FAILED)
        self.assertIn("RESULT:FAIL STEP:verify", out)
        self.assertIn("rhythmyx_context_failed", out)
        self.assertIn("Failed startup of context", out)
        self.assertIn(pdc.DEFAULT_CONTAINER, out)
        mock_sleep.assert_not_called()

    def test_verify_timeout_prefers_context_failure_detail(self):
        """When HTTP/health never ready but logs show context fail, DETAIL uses that."""
        dead_ctx = "Failed startup of context Rhythmyx\n"
        with unittest.mock.patch.object(pdc, "_curl_status") as mock_curl, \
             unittest.mock.patch.object(pdc, "_docker_health") as mock_health, \
             unittest.mock.patch.object(pdc, "_docker_logs_tail") as mock_logs, \
             unittest.mock.patch.object(pdc.subprocess, "run") as mock_run, \
             unittest.mock.patch.object(pdc.time, "sleep"):
            mock_curl.return_value = 0
            mock_health.return_value = "starting"
            # max_checks=1 (5//5): one in-loop scan (empty) + one final scan (fail).
            mock_logs.side_effect = ["", dead_ctx]
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=0, stdout="", stderr=""
            )
            import io
            from contextlib import redirect_stdout
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "verify",
                    "--timeout-seconds", "5",
                    "--interval-seconds", "5",
                ])
            out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_SUBPROCESS_FAILED)
        self.assertIn("rhythmyx_context_failed", out)
        self.assertIn("Failed startup of context", out)


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


class TestQaHelpers(unittest.TestCase):
    """Pure helpers for QA mode (H2 Docker entrypoint) — no docker."""

    def setUp(self):
        _clear_port_env()
        self.addCleanup(_clear_port_env)
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = _stub_repo_root(Path(self.td.name))
        matrix = self.repo_root / "docker" / "scripts" / "matrix-install-smoke.py"
        matrix.write_text("# stub\n", encoding="utf-8")

    def test_qa_matrix_up_argv_includes_keep_and_h2(self):
        argv = pdc._qa_matrix_up_argv(
            self.repo_root,
            probe_timeout=120,
            skip_image_build=True,
            dry_run=True,
        )
        self.assertEqual(argv[0], sys.executable)
        self.assertTrue(any(str(a).endswith("matrix-install-smoke.py") for a in argv))
        self.assertIn("--product", argv)
        self.assertIn("cms", argv)
        self.assertIn("--db", argv)
        self.assertIn("h2", argv)
        self.assertIn("--keep", argv)
        self.assertIn("--probe-timeout", argv)
        self.assertIn("120", argv)
        self.assertIn("--skip-image-build", argv)
        self.assertIn("--dry-run", argv)
        script_idx = next(
            i for i, a in enumerate(argv) if str(a).endswith("matrix-install-smoke.py")
        )
        # Match production: str(repo_root / ...) without resolve(). Comparing a
        # resolved expected path fails on Windows when tempfile uses 8.3 short
        # names (e.g. RUNNER~1 vs runneradmin on GHA windows-latest).
        expected = self.repo_root / "docker" / "scripts" / "matrix-install-smoke.py"
        self.assertEqual(Path(argv[script_idx]), expected)

    def test_qa_matrix_up_argv_no_dry_run_flag_when_live(self):
        argv = pdc._qa_matrix_up_argv(
            self.repo_root,
            probe_timeout=900,
            skip_image_build=False,
            dry_run=False,
        )
        self.assertNotIn("--dry-run", argv)
        self.assertNotIn("--skip-image-build", argv)
        self.assertIn("--keep", argv)

    def test_qa_destroy_argv(self):
        argv = pdc._qa_destroy_argv("perc-matrix-cms-h2")
        self.assertEqual(argv, ["docker", "rm", "-f", "perc-matrix-cms-h2"])

    def test_qa_matrix_image_healthcheck_status_ok(self):
        """Image with a HEALTHCHECK Test array reports ok (#2484)."""
        completed = unittest.mock.Mock(
            returncode=0,
            stdout='{"Test":["CMD-SHELL","/usr/local/bin/rhythmyx_healthcheck.py"],'
                   '"Interval":30000000000}',
        )
        runner = unittest.mock.Mock(return_value=completed)
        status = pdc._qa_matrix_image_healthcheck_status(
            "percussion-matrix-cell:local", runner=runner
        )
        self.assertEqual(status, pdc.QA_IMAGE_HEALTHCHECK_OK)

    def test_qa_matrix_image_healthcheck_status_missing(self):
        """Pre-#2481 image (Healthcheck=null) reports missing so qa-up fails fast."""
        completed = unittest.mock.Mock(returncode=0, stdout="null")
        runner = unittest.mock.Mock(return_value=completed)
        status = pdc._qa_matrix_image_healthcheck_status(
            "percussion-matrix-cell:local", runner=runner
        )
        self.assertEqual(status, pdc.QA_IMAGE_HEALTHCHECK_MISSING)

    def test_qa_matrix_image_healthcheck_status_missing_empty_test(self):
        """Image with empty Test array (no-op healthcheck) still reports missing."""
        completed = unittest.mock.Mock(returncode=0, stdout='{"Test":[]}')
        runner = unittest.mock.Mock(return_value=completed)
        status = pdc._qa_matrix_image_healthcheck_status(
            "percussion-matrix-cell:local", runner=runner
        )
        self.assertEqual(status, pdc.QA_IMAGE_HEALTHCHECK_MISSING)

    def test_qa_matrix_image_healthcheck_status_absent(self):
        """Image not present locally (docker inspect non-zero) reports absent."""
        completed = unittest.mock.Mock(returncode=1, stdout="", stderr="No such image")
        runner = unittest.mock.Mock(return_value=completed)
        status = pdc._qa_matrix_image_healthcheck_status(
            "percussion-matrix-cell:local", runner=runner
        )
        self.assertEqual(status, pdc.QA_IMAGE_HEALTHCHECK_ABSENT)

    def test_qa_up_skip_image_build_stale_image_fails_fast(self):
        """qa-up --skip-image-build short-circuits when image lacks HEALTHCHECK (#2484)."""
        with unittest.mock.patch.object(
            pdc, "_qa_matrix_image_healthcheck_status",
            return_value=pdc.QA_IMAGE_HEALTHCHECK_MISSING,
        ), unittest.mock.patch.object(pdc, "_run_logged") as mock_run:
            rc = pdc.main([
                "--repo-root", str(self.repo_root),
                "qa-up", "--skip-image-build", "--timeout-seconds", "120",
            ])
        self.assertEqual(rc, pdc.EXIT_SUBPROCESS_FAILED)
        mock_run.assert_not_called()

    def test_qa_up_skip_image_build_fresh_image_proceeds(self):
        """qa-up --skip-image-build proceeds when image HEALTHCHECK is ok (#2484)."""
        fake_log = self.repo_root / "docker" / "logs" / "qa-up-fake.log"
        fake_log.parent.mkdir(parents=True, exist_ok=True)
        fake_log.write_text("RESULT:OK STEP:qa-up LOG:...\n", encoding="utf-8")
        with unittest.mock.patch.object(
            pdc, "_qa_matrix_image_healthcheck_status",
            return_value=pdc.QA_IMAGE_HEALTHCHECK_OK,
        ), unittest.mock.patch.object(
            pdc, "_run_logged", return_value=(pdc.EXIT_OK, fake_log),
        ), unittest.mock.patch.object(
            pdc, "_qa_fetch_admin_password", return_value="ADMIN_PASSWORD=demo",
        ):
            rc = pdc.main([
                "--repo-root", str(self.repo_root),
                "qa-up", "--skip-image-build", "--timeout-seconds", "120",
            ])
        self.assertEqual(rc, pdc.EXIT_OK)

    def test_qa_preferred_port_and_container_constants(self):
        """Preferred baseline aligns with matrix-install-smoke CMS host port."""
        self.assertEqual(pdc.PREFERRED_QA_CMS_HOST_PORT, 9993)
        self.assertEqual(pdc.QA_CMS_CONTAINER, "perc-matrix-cms-h2")
        # #2482 — default probe path is the matrix-recommended primary
        # (Spring-managed ``MimeTypeResource.ping()``). The env override
        # ``RHYTHMYX_HEALTH_PATH`` is honored by ``qa_cms_probe_url`` and
        # tested separately in ``TestFreeportAndUrlWiring``.
        self.assertTrue(
            pdc.qa_cms_probe_url(9993).endswith("/Rhythmyx/rest/mimetypes")
        )
        self.assertEqual(pdc.qa_cms_base_url(9993), "http://127.0.0.1:9993")


class TestQaHealthRealMode(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = _stub_repo_root(Path(self.td.name))
        # Do not docker-exec a live CMS from unit tests (#2556 product log scan).
        self._slog = unittest.mock.patch.object(
            pdc, "_docker_read_server_log", return_value=""
        )
        self._slog.start()
        self.addCleanup(self._slog.stop)

    def test_qa_health_success_on_first_check(self):
        with unittest.mock.patch.object(pdc, "_curl_status") as mock_curl, \
             unittest.mock.patch.object(pdc, "_docker_health") as mock_health, \
             unittest.mock.patch.object(pdc, "_docker_logs_tail") as mock_logs, \
             unittest.mock.patch.object(pdc.time, "sleep"):
            mock_curl.return_value = 200
            mock_health.return_value = "healthy"
            mock_logs.return_value = "INFO [Server] Started @7879ms\n"
            import io
            from contextlib import redirect_stdout
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "qa-health", "--timeout-seconds", "10",
                ])
            out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-health", out)
        self.assertIn("HTTP:200", out)
        self.assertIn("HEALTH:healthy", out)
        mock_logs.assert_called()
        mock_health.assert_called_with(pdc.QA_CMS_CONTAINER)

    def test_qa_health_timeout_emits_clear_error(self):
        with unittest.mock.patch.object(pdc, "_curl_status") as mock_curl, \
             unittest.mock.patch.object(pdc, "_docker_health") as mock_health, \
             unittest.mock.patch.object(pdc, "_docker_logs_tail") as mock_logs, \
             unittest.mock.patch.object(pdc.time, "sleep"):
            mock_curl.return_value = 0
            mock_health.return_value = "starting"
            mock_logs.return_value = ""
            import io
            from contextlib import redirect_stdout
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "qa-health",
                    "--timeout-seconds", "5",
                    "--interval-seconds", "5",
                ])
            out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_SUBPROCESS_FAILED)
        self.assertIn("RESULT:FAIL STEP:qa-health", out)
        self.assertIn("timeout", out)
        self.assertIn("health=starting", out)
        self.assertIn("LOG:", out)
        self.assertNotIn("LOG:\n", out)

    def test_qa_health_fails_when_http_ok_but_context_failed(self):
        """#2462: Jetty HTTP ready + dead Rhythmyx context must FAIL (not OK)."""
        dead_ctx = (
            "WARN  [WebAppContext] Failed startup of context "
            "oeje11w.WebAppContext Rhythmyx\n"
            "BeanCurrentlyInCreationException: folderHelper\n"
            "INFO  [AbstractConnector] Started {HTTP/1.1}{0.0.0.0:9992}\n"
        )
        with unittest.mock.patch.object(pdc, "_curl_status") as mock_curl, \
             unittest.mock.patch.object(pdc, "_docker_health") as mock_health, \
             unittest.mock.patch.object(pdc, "_docker_logs_tail") as mock_logs, \
             unittest.mock.patch.object(pdc.time, "sleep") as mock_sleep:
            mock_curl.return_value = 200
            mock_health.return_value = "unhealthy"
            mock_logs.return_value = dead_ctx
            import io
            from contextlib import redirect_stdout
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "qa-health",
                    "--timeout-seconds", "30",
                    "--interval-seconds", "5",
                ])
            out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_SUBPROCESS_FAILED)
        self.assertIn("RESULT:FAIL STEP:qa-health", out)
        self.assertIn("rhythmyx_context_failed", out)
        self.assertIn("Failed startup of context", out)
        self.assertIn("HEALTH:unhealthy", out)
        # Fail-fast: must not burn the full poll budget.
        mock_sleep.assert_not_called()

    def test_qa_health_timeout_prefers_context_failure_detail(self):
        """When HTTP never ready but logs show context fail, DETAIL uses that."""
        dead_ctx = "Failed startup of context Rhythmyx\n"
        with unittest.mock.patch.object(pdc, "_curl_status") as mock_curl, \
             unittest.mock.patch.object(pdc, "_docker_health") as mock_health, \
             unittest.mock.patch.object(pdc, "_docker_logs_tail") as mock_logs, \
             unittest.mock.patch.object(pdc.time, "sleep"):
            mock_curl.return_value = 0
            mock_health.return_value = "unhealthy"
            # max_checks=1 (5//5): one in-loop scan (empty) + one final scan (fail).
            mock_logs.side_effect = ["", dead_ctx]
            import io
            from contextlib import redirect_stdout
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "qa-health",
                    "--timeout-seconds", "5",
                    "--interval-seconds", "5",
                ])
            out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_SUBPROCESS_FAILED)
        self.assertIn("rhythmyx_context_failed", out)
        self.assertIn("Failed startup of context", out)
        self.assertIn("HEALTH:unhealthy", out)

    def test_qa_health_http_ok_but_health_starting_not_ready(self):
        """#2537: HTTP ready alone is insufficient until Health.Status=healthy."""
        with unittest.mock.patch.object(pdc, "_curl_status") as mock_curl, \
             unittest.mock.patch.object(pdc, "_docker_health") as mock_health, \
             unittest.mock.patch.object(pdc, "_docker_logs_tail") as mock_logs, \
             unittest.mock.patch.object(pdc.time, "sleep"):
            mock_curl.return_value = 200
            mock_health.return_value = "starting"
            mock_logs.return_value = "INFO [Server] Started @7879ms\n"
            import io
            from contextlib import redirect_stdout
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "qa-health",
                    "--timeout-seconds", "5",
                    "--interval-seconds", "5",
                ])
            out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_SUBPROCESS_FAILED)
        self.assertIn("RESULT:FAIL STEP:qa-health", out)
        self.assertIn("timeout", out)
        self.assertIn("health=starting", out)
        mock_health.assert_called_with(pdc.QA_CMS_CONTAINER)

    def test_qa_health_surfaces_health_none(self):
        """#2537: HEALTH:none when container has no Health block."""
        with unittest.mock.patch.object(pdc, "_curl_status") as mock_curl, \
             unittest.mock.patch.object(pdc, "_docker_health") as mock_health, \
             unittest.mock.patch.object(pdc, "_docker_logs_tail") as mock_logs, \
             unittest.mock.patch.object(pdc.time, "sleep"):
            mock_curl.return_value = 200
            mock_health.return_value = "none"
            mock_logs.return_value = "INFO [Server] Started\n"
            import io
            from contextlib import redirect_stdout
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "qa-health",
                    "--timeout-seconds", "5",
                    "--interval-seconds", "5",
                ])
            out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_SUBPROCESS_FAILED)
        self.assertIn("health=none", out)


class TestQaDownRealMode(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.repo_root = _stub_repo_root(Path(self.td.name))

    def test_qa_down_invokes_docker_rm(self):
        captured = []

        def fake_run(argv, *args, **kwargs):
            captured.append(list(argv))
            return subprocess.CompletedProcess(
                args=argv, returncode=0, stdout="", stderr=""
            )

        import io
        from contextlib import redirect_stdout
        buf = io.StringIO()
        with unittest.mock.patch.object(pdc.subprocess, "run", side_effect=fake_run):
            with redirect_stdout(buf):
                rc = pdc.main([
                    "--repo-root", str(self.repo_root),
                    "qa-down",
                ])
        out = buf.getvalue()
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("RESULT:OK STEP:qa-down", out)
        self.assertTrue(
            any(
                a[:3] == ["docker", "rm", "-f"] and pdc.QA_CMS_CONTAINER in a
                for a in captured
            ),
            msg=f"expected docker rm -f {pdc.QA_CMS_CONTAINER} in {captured!r}",
        )


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            pdc.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


class TestFreeportAndUrlWiring(unittest.TestCase):
    """#2001 — freeport allocator + env override + verify/QA URL wiring."""

    def setUp(self):
        _clear_port_env()
        self.addCleanup(_clear_port_env)

    def test_find_free_port_returns_bindable_int(self):
        port = pdc.find_free_port()
        self.assertIsInstance(port, int)
        self.assertGreater(port, 0)
        self.assertLess(port, 65536)
        # Port was released after allocation; may or may not still be free
        # (TOCTOU), but a second allocation must also succeed.
        port2 = pdc.find_free_port()
        self.assertIsInstance(port2, int)
        self.assertGreater(port2, 0)

    def test_is_port_free_true_for_ephemeral(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.bind(("127.0.0.1", 0))
            occupied = int(sock.getsockname()[1])
            self.assertFalse(pdc.is_port_free(occupied))
        # After close, preferred high-ish ephemeral range should often be free;
        # use a second bind-0 allocation and release to prove True path.
        free = pdc.find_free_port()
        self.assertTrue(pdc.is_port_free(free))

    def test_resolve_host_port_env_override(self):
        os.environ["QA_CMS_HOST_PORT"] = "18001"
        self.assertEqual(pdc.resolve_host_port("QA_CMS_HOST_PORT", preferred=9993), 18001)

    def test_resolve_host_port_invalid_env_raises(self):
        os.environ["CMS_PORT"] = "not-a-port"
        with self.assertRaises(ValueError):
            pdc.resolve_host_port("CMS_PORT", preferred=9992)

    def test_resolve_host_port_preferred_when_free(self):
        # Use find_free_port as preferred so we know it is free after release.
        preferred = pdc.find_free_port()
        self.assertEqual(
            pdc.resolve_host_port(preferred=preferred),
            preferred,
        )

    def test_resolve_host_port_falls_back_when_preferred_taken(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.bind(("127.0.0.1", 0))
            taken = int(sock.getsockname()[1])
            resolved = pdc.resolve_host_port(preferred=taken)
            self.assertNotEqual(resolved, taken)
            self.assertGreater(resolved, 0)

    def test_verify_urls_from_cms_dts_port_env(self):
        os.environ["CMS_PORT"] = "19111"
        os.environ["DTS_PORT"] = "19112"
        self.assertEqual(
            pdc.resolve_verify_cms_url(),
            "http://localhost:19111/Rhythmyx/rest/folders/by-path/Assets",
        )
        self.assertEqual(
            pdc.resolve_verify_dts_url(),
            "http://localhost:19112/",
        )

    def test_verify_urls_full_env_override(self):
        os.environ["VERIFY_CMS_URL"] = "http://example.test:1/cms"
        os.environ["VERIFY_DTS_URL"] = "http://example.test:2/dts"
        self.assertEqual(pdc.resolve_verify_cms_url(), "http://example.test:1/cms")
        self.assertEqual(pdc.resolve_verify_dts_url(), "http://example.test:2/dts")

    def test_qa_url_helpers_wire_port(self):
        self.assertEqual(pdc.qa_cms_base_url(12345), "http://127.0.0.1:12345")
        # #2482 — default probe path is the matrix-recommended primary
        # (``/Rhythmyx/rest/mimetypes`` — Spring-managed ``ping()``).
        self.assertEqual(
            pdc.qa_cms_probe_url(12345),
            "http://127.0.0.1:12345/Rhythmyx/rest/mimetypes",
        )

    def test_qa_probe_url_env_override(self):
        """#2482 — RHYTHMYX_HEALTH_PATH env override wins over default."""
        # Default (setUp cleared the env, so the module default applies):
        self.assertTrue(
            pdc.qa_cms_probe_url(12345).endswith("/Rhythmyx/rest/mimetypes")
        )
        os.environ[pdc.QA_CMS_PROBE_PATH_ENV] = "/Rhythmyx/rest/health"
        try:
            self.assertEqual(
                pdc.qa_cms_probe_url(12345),
                "http://127.0.0.1:12345/Rhythmyx/rest/health",
            )
        finally:
            os.environ.pop(pdc.QA_CMS_PROBE_PATH_ENV, None)
        # Back to default after the env is cleared.
        self.assertTrue(
            pdc.qa_cms_probe_url(12345).endswith("/Rhythmyx/rest/mimetypes")
        )

    def test_qa_probe_url_env_override_blank_falls_back_to_default(self):
        """#2482 — an empty env value falls back to the default path."""
        os.environ[pdc.QA_CMS_PROBE_PATH_ENV] = "   "
        try:
            self.assertTrue(
                pdc.qa_cms_probe_url(12345).endswith("/Rhythmyx/rest/mimetypes")
            )
        finally:
            os.environ.pop(pdc.QA_CMS_PROBE_PATH_ENV, None)

    def test_ensure_qa_cms_host_port_pins_env(self):
        os.environ["QA_CMS_HOST_PORT"] = "17777"
        port = pdc.ensure_qa_cms_host_port()
        self.assertEqual(port, 17777)
        self.assertEqual(os.environ["QA_CMS_HOST_PORT"], "17777")
        self.assertEqual(os.environ["CMS_HOST_PORT"], "17777")

    def test_ensure_compose_host_ports_pins_env(self):
        os.environ["CMS_PORT"] = "18881"
        os.environ["DTS_PORT"] = "18882"
        cms, dts = pdc.ensure_compose_host_ports()
        self.assertEqual((cms, dts), (18881, 18882))
        self.assertEqual(os.environ["CMS_PORT"], "18881")
        self.assertEqual(os.environ["DTS_PORT"], "18882")
        # #2004 — CMS/DTS ensure also pins compose DB host publishes.
        self.assertIn("MYSQL_PORT", os.environ)
        self.assertIn("POSTGRES_PORT", os.environ)
        self.assertIn("MSSQL_PORT", os.environ)
        self.assertGreater(int(os.environ["MYSQL_PORT"]), 0)

    def test_ensure_compose_db_host_ports_env_override(self):
        os.environ["MYSQL_PORT"] = "13306"
        os.environ["POSTGRES_PORT"] = "15433"
        os.environ["MSSQL_PORT"] = "11433"
        resolved = pdc.ensure_compose_db_host_ports()
        self.assertEqual(
            resolved,
            {
                "MYSQL_PORT": 13306,
                "POSTGRES_PORT": 15433,
                "MSSQL_PORT": 11433,
            },
        )
        self.assertEqual(os.environ["MYSQL_PORT"], "13306")

    def test_ensure_compose_db_host_ports_freeport_when_taken(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.bind(("127.0.0.1", 0))
            taken = int(sock.getsockname()[1])
            # Prefer a held port for MYSQL only via temporary preferred constant.
            original = pdc.PREFERRED_MYSQL_HOST_PORT
            pdc.PREFERRED_MYSQL_HOST_PORT = taken
            self.addCleanup(
                lambda: setattr(pdc, "PREFERRED_MYSQL_HOST_PORT", original)
            )
            resolved = pdc.ensure_compose_db_host_ports()
            self.assertNotEqual(resolved["MYSQL_PORT"], taken)
            self.assertEqual(os.environ["MYSQL_PORT"], str(resolved["MYSQL_PORT"]))

    def test_qa_up_dry_run_honors_env_port(self):
        os.environ["QA_CMS_HOST_PORT"] = "16666"
        td = tempfile.TemporaryDirectory()
        self.addCleanup(td.cleanup)
        repo = _stub_repo_root(Path(td.name))
        matrix = repo / "docker" / "scripts" / "matrix-install-smoke.py"
        matrix.write_text("# stub\n", encoding="utf-8")
        runner = _CliRunner(repo)
        rc, out = runner.run(["qa-up", "--timeout-seconds", "60"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("QA_CMS_HOST_PORT=16666", out)
        self.assertIn("TEST_CMS_URL=http://127.0.0.1:16666", out)

    def test_up_dry_run_honors_cms_dts_env(self):
        os.environ["CMS_PORT"] = "15551"
        os.environ["DTS_PORT"] = "15552"
        td = tempfile.TemporaryDirectory()
        self.addCleanup(td.cleanup)
        repo = _stub_repo_root(Path(td.name))
        runner = _CliRunner(repo)
        rc, out = runner.run(["up"])
        self.assertEqual(rc, pdc.EXIT_OK)
        self.assertIn("CMS_PORT=15551", out)
        self.assertIn("DTS_PORT=15552", out)
        self.assertIn(
            "VERIFY_CMS_URL=http://localhost:15551/Rhythmyx/rest/folders/by-path/Assets",
            out,
        )
        self.assertIn("VERIFY_DTS_URL=http://localhost:15552/", out)


if __name__ == "__main__":
    logging.getLogger("perc-devctl").setLevel(logging.CRITICAL)
    unittest.main()
