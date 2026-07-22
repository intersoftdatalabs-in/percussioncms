#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for hot-deploy-jar.py (no docker required).

``--dry-run`` mode exercises the full wiring without invoking docker;
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
    path = SCRIPTS / "hot-deploy-jar.py"
    name = "hot_deploy_jar"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


hdj = _load()


def _stub_subprocess(*, docker_ps_names=("percussion-cms-dts",), returncodes=None):
    """Replace ``subprocess.run`` with a recorder + returncode map.

    The first call is the ``docker ps --format {{.Names}}`` container
    check; the remaining calls are deploy / restart invocations.
    """
    returncodes = returncodes or {}
    calls = []

    def fake_run(argv, *args, **kwargs):
        calls.append((list(argv), kwargs))
        if argv[:2] == ["docker", "ps"]:
            return subprocess.CompletedProcess(
                args=argv, returncode=0,
                stdout="\n".join(docker_ps_names) + "\n",
                stderr="",
            )
        if argv[:2] == ["docker", "exec"]:
            # The backup-existence check uses sh -c; return "EXISTS" if asked.
            if any("EXISTS" in (a if isinstance(a, str) else "") for a in argv):
                return subprocess.CompletedProcess(
                    args=argv, returncode=0, stdout="EXISTS\n", stderr=""
                )
            return subprocess.CompletedProcess(
                args=argv, returncode=0, stdout="", stderr=""
            )
        if argv[:2] == ["docker", "cp"]:
            return subprocess.CompletedProcess(
                args=argv, returncode=returncodes.get("cp", 0), stdout="", stderr=""
            )
        if argv[:2] == ["docker", "restart"]:
            return subprocess.CompletedProcess(
                args=argv, returncode=returncodes.get("restart", 0), stdout="", stderr=""
            )
        return subprocess.CompletedProcess(args=argv, returncode=0, stdout="", stderr="")

    return calls, fake_run


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            hdj.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_missing_jar_errors(self):
        with self.assertRaises(SystemExit) as cm:
            hdj.main([])
        self.assertEqual(cm.exception.code, 2)  # argparse uses 2 for usage errors


class TestResolveTargets(unittest.TestCase):
    def test_known_target_cms(self):
        self.assertEqual(hdj._resolve_targets("cms"), ["/opt/Percussion/jetty/base/lib"])

    def test_known_target_dts(self):
        self.assertEqual(
            hdj._resolve_targets("dts"),
            ["/opt/Percussion/Deployment/Server/lib"],
        )

    def test_both_resolves_to_both_dirs(self):
        self.assertEqual(
            hdj._resolve_targets("both"),
            [
                "/opt/Percussion/jetty/base/lib",
                "/opt/Percussion/Deployment/Server/lib",
            ],
        )

    def test_absolute_path_resolves_to_self(self):
        self.assertEqual(
            hdj._resolve_targets("/custom/path"), ["/custom/path"]
        )

    def test_unsupported_raises(self):
        with self.assertRaises(ValueError):
            hdj._resolve_targets("not-a-target")


class TestDryRun(unittest.TestCase):
    """``--dry-run`` exercises the full wiring without invoking docker."""

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.td_path = Path(self.td.name)
        # Real jar file so the existence check passes.
        self.jar = self.td_path / "utils-1.0.0.jar"
        self.jar.write_bytes(b"fake jar content")

        logging.getLogger("hot-deploy-jar").setLevel(logging.CRITICAL)

    def _run(self, *, target="both", restart=False):
        return hdj.deploy(
            jar_path=self.jar,
            container_name="percussion-cms-dts",
            target=target,
            restart=restart,
            dry_run=True,
        )

    def test_dry_run_both_targets(self):
        rc = self._run()
        self.assertEqual(rc, hdj.EXIT_OK)

    def test_dry_run_cms_only(self):
        rc = self._run(target="cms")
        self.assertEqual(rc, hdj.EXIT_OK)

    def test_dry_run_absolute_target(self):
        rc = self._run(target="/opt/custom/lib")
        self.assertEqual(rc, hdj.EXIT_OK)

    def test_dry_run_unsupported_target_errors(self):
        rc = self._run(target="garbage")
        self.assertEqual(rc, hdj.EXIT_UNSUPPORTED_TARGET)


class TestRealRun(unittest.TestCase):
    """Real (non-dry-run) mode with a stubbed subprocess."""

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.td_path = Path(self.td.name)
        self.jar = self.td_path / "utils-1.0.0.jar"
        self.jar.write_bytes(b"fake jar content")

        logging.getLogger("hot-deploy-jar").setLevel(logging.CRITICAL)

    def test_jar_not_found_exits_three(self):
        rc = hdj.deploy(
            jar_path=self.td_path / "no-such.jar",
            container_name="percussion-cms-dts",
            target="cms",
            restart=False,
            dry_run=False,
        )
        self.assertEqual(rc, hdj.EXIT_JAR_NOT_FOUND)

    def test_container_not_running_exits_two(self):
        with unittest.mock.patch.object(hdj.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=0, stdout="other-container\n", stderr=""
            )
            rc = hdj.deploy(
                jar_path=self.jar,
                container_name="absent",
                target="cms",
                restart=False,
                dry_run=False,
            )
        self.assertEqual(rc, hdj.EXIT_CONTAINER_NOT_RUNNING)

    def test_real_run_docker_cp_success(self):
        calls, fake_run = _stub_subprocess()
        with unittest.mock.patch.object(hdj.subprocess, "run", side_effect=fake_run):
            rc = hdj.deploy(
                jar_path=self.jar,
                container_name="percussion-cms-dts",
                target="cms",
                restart=False,
                dry_run=False,
            )
        self.assertEqual(rc, hdj.EXIT_OK)
        # Verify docker cp was called with the right argv.
        cp_calls = [c for c in calls if c[0][:2] == ["docker", "cp"]]
        self.assertEqual(len(cp_calls), 1)
        self.assertEqual(cp_calls[0][0][2], str(self.jar.resolve()))
        self.assertEqual(cp_calls[0][0][3], "percussion-cms-dts:/opt/Percussion/jetty/base/lib/utils-1.0.0.jar")

    def test_real_run_restart_invocation(self):
        calls, fake_run = _stub_subprocess()
        with unittest.mock.patch.object(hdj.subprocess, "run", side_effect=fake_run):
            rc = hdj.deploy(
                jar_path=self.jar,
                container_name="percussion-cms-dts",
                target="cms",
                restart=True,
                dry_run=False,
            )
        self.assertEqual(rc, hdj.EXIT_OK)
        restart_calls = [c for c in calls if c[0][:2] == ["docker", "restart"]]
        self.assertEqual(len(restart_calls), 1)
        self.assertEqual(restart_calls[0][0][2], "percussion-cms-dts")

    def test_real_run_docker_cp_failure_propagates(self):
        calls, fake_run = _stub_subprocess(returncodes={"cp": 1})
        with unittest.mock.patch.object(hdj.subprocess, "run", side_effect=fake_run):
            rc = hdj.deploy(
                jar_path=self.jar,
                container_name="percussion-cms-dts",
                target="cms",
                restart=False,
                dry_run=False,
            )
        self.assertEqual(rc, hdj.EXIT_DOCKER_FAILED)


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            hdj.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


if __name__ == "__main__":
    logging.getLogger("hot-deploy-jar").setLevel(logging.CRITICAL)
    unittest.main()