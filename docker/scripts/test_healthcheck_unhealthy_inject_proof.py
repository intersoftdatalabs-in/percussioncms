#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for healthcheck_unhealthy_inject_proof.py (#2536)."""

from __future__ import annotations

import importlib.util
import io
import json
import subprocess
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from typing import List, Optional
from unittest import mock

SCRIPTS = Path(__file__).resolve().parent


def _load(path: Path, name: str):
    if str(SCRIPTS) not in sys.path:
        sys.path.insert(0, str(SCRIPTS))
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


proof = _load(
    SCRIPTS / "healthcheck_unhealthy_inject_proof.py",
    "healthcheck_unhealthy_inject_proof",
)


def _cp(
    code: int = 0,
    stdout: str = "",
    stderr: str = "",
) -> subprocess.CompletedProcess:
    return subprocess.CompletedProcess(
        args=[],
        returncode=code,
        stdout=stdout,
        stderr=stderr,
    )


class FormatResultLineTests(unittest.TestCase):
    def test_ok_mock(self):
        line = proof.format_result_line(True, mode="mock", detail="healthy+inject_unhealthy_ok")
        self.assertTrue(line.startswith("RESULT:OK"))
        self.assertIn(f"STEP:{proof.STEP}", line)
        self.assertIn("MODE:mock", line)
        self.assertIn("DETAIL:healthy+inject_unhealthy_ok", line)

    def test_fail_includes_reason(self):
        line = proof.format_result_line(
            False,
            mode="live",
            reason="container not running",
        )
        self.assertTrue(line.startswith("RESULT:FAIL"))
        self.assertIn("MODE:live", line)
        self.assertIn("REASON:container not running", line)


class JettyLogPathTests(unittest.TestCase):
    def test_portable_join(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            path = proof.jetty_log_path(root)
            # Must use Path segments (jetty/base/logs/jetty.log), not hardcoded OS sep
            self.assertEqual(path.name, "jetty.log")
            self.assertEqual(path.parent.name, "logs")
            self.assertTrue(str(path).endswith(str(Path("jetty") / "base" / "logs" / "jetty.log")))

    def test_write_and_append(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            p1 = proof.write_jetty_log(root, proof.clean_log_body())
            self.assertTrue(p1.is_file())
            self.assertIn("Server] Started", p1.read_text(encoding="utf-8"))
            p2 = proof.append_jetty_log(root, "EXTRA LINE")
            self.assertEqual(p1, p2)
            self.assertIn("EXTRA LINE", p2.read_text(encoding="utf-8"))


class ParseHealthStatusTests(unittest.TestCase):
    def test_plain_tokens(self):
        self.assertEqual(proof.parse_health_status("healthy"), "healthy")
        self.assertEqual(proof.parse_health_status("UNHEALTHY\n"), "unhealthy")
        self.assertEqual(proof.parse_health_status("starting"), "starting")
        self.assertEqual(proof.parse_health_status(""), "unknown")

    def test_full_inspect_json(self):
        blob = json.dumps(
            {
                "State": {
                    "Running": True,
                    "Health": {"Status": "unhealthy", "FailingStreak": 2},
                }
            }
        )
        self.assertEqual(proof.parse_health_status(blob), "unhealthy")

    def test_health_object_json(self):
        blob = json.dumps({"Status": "healthy", "Log": []})
        self.assertEqual(proof.parse_health_status(blob), "healthy")

    def test_list_inspect(self):
        blob = json.dumps([{"State": {"Health": {"Status": "starting"}}}])
        self.assertEqual(proof.parse_health_status(blob), "starting")

    def test_none_when_no_health(self):
        blob = json.dumps({"State": {"Running": True}})
        self.assertEqual(proof.parse_health_status(blob), "none")


class MockProofTests(unittest.TestCase):
    def test_run_mock_proof_ok(self):
        ok, lines = proof.run_mock_proof()
        self.assertTrue(ok, msg="\n".join(lines))
        joined = "\n".join(lines)
        self.assertIn("OK healthy path", joined)
        self.assertIn("OK unhealthy inject", joined)
        self.assertIn("OK recovery healthy", joined)
        self.assertIn(proof.DEFAULT_INJECT_MARKER, joined)

    def test_run_mock_proof_with_fixture_root(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td) / "install"
            ok, lines = proof.run_mock_proof(fixture_root=root)
            self.assertTrue(ok, msg="\n".join(lines))
            # Fixture root should retain last written clean log after recovery
            log_path = proof.jetty_log_path(root)
            self.assertTrue(log_path.is_file())
            body = log_path.read_text(encoding="utf-8")
            self.assertNotIn(proof.DEFAULT_INJECT_MARKER, body)

    def test_inject_body_contains_marker(self):
        body = proof.inject_log_body()
        self.assertIn(proof.DEFAULT_INJECT_MARKER, body)
        self.assertIn("BeanCurrentlyInCreationException", body)

    def test_assess_fixture_unhealthy_after_inject(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            proof.write_jetty_log(root, proof.inject_log_body())
            code, detail = proof.assess_fixture(root, http_code=302)
            self.assertEqual(code, 1)
            self.assertIn("rhythmyx_context_failed", detail)

    def test_expected_healthy_helper(self):
        self.assertTrue(proof.expected_healthy(200, proof.clean_log_body()))
        self.assertFalse(proof.expected_healthy(200, proof.inject_log_body()))
        self.assertFalse(proof.expected_healthy(0, proof.clean_log_body()))


class LiveProofPureLogicTests(unittest.TestCase):
    """Live mode with injected subprocess.run — no real Docker."""

    def test_docker_container_running_true(self):
        def fake_run(argv, **_kwargs):
            self.assertEqual(argv[0], "docker")
            return _cp(0, stdout="true\n")

        self.assertTrue(
            proof.docker_container_running("perc-matrix-cms-h2", run=fake_run)
        )

    def test_docker_container_running_false(self):
        def fake_run(argv, **_kwargs):
            return _cp(1, stderr="Error: No such object")

        self.assertFalse(proof.docker_container_running("missing", run=fake_run))

    def test_docker_health_status(self):
        def fake_run(argv, **_kwargs):
            return _cp(0, stdout="unhealthy\n")

        self.assertEqual(
            proof.docker_health_status("c1", run=fake_run),
            "unhealthy",
        )

    def test_docker_inject_marker_ok(self):
        calls: List[List[str]] = []

        def fake_run(argv, **_kwargs):
            calls.append(list(argv))
            return _cp(0, stdout="")

        ok, path = proof.docker_inject_marker(
            "perc-matrix-cms-h2",
            install_root="/opt/Percussion",
            run=fake_run,
        )
        self.assertTrue(ok)
        self.assertIn("jetty.log", path)
        self.assertTrue(path.startswith("/opt/Percussion/"))
        # Container path uses '/' (Linux container), not Windows sep
        self.assertNotIn("\\", path)
        self.assertEqual(calls[0][0:3], ["docker", "exec", "perc-matrix-cms-h2"])

    def test_docker_run_incontainer_healthcheck_unhealthy(self):
        def fake_run(argv, **_kwargs):
            # Simulate healthcheck CLI exit 1 with context fail detail
            if argv[-1].endswith("rhythmyx_healthcheck.py") or (
                len(argv) > 4 and "rhythmyx_healthcheck.py" in argv[4]
            ):
                return _cp(
                    1,
                    stdout=(
                        "rhythmyx_context_failed match='Failed startup of context' "
                        "http=200\n"
                    ),
                )
            # Also match when script is argv element
            for a in argv:
                if "rhythmyx_healthcheck.py" in a:
                    return _cp(
                        1,
                        stdout="rhythmyx_context_failed match='Failed startup of context' http=200\n",
                    )
            return _cp(1, stderr="unexpected")

        code, detail = proof.docker_run_incontainer_healthcheck(
            "perc-matrix-cms-h2",
            run=fake_run,
        )
        self.assertEqual(code, 1)
        self.assertIn("rhythmyx_context_failed", detail)

    def test_run_live_proof_ok_with_stubs(self):
        state = {"running": True, "status": "healthy", "injected": False}

        def fake_run(argv, **_kwargs):
            cmd = " ".join(argv)
            if "inspect" in argv and "Running" in cmd:
                return _cp(0, stdout="true\n" if state["running"] else "false\n")
            if "inspect" in argv and "Health" in cmd:
                return _cp(0, stdout=state["status"] + "\n")
            if "exec" in argv and "printf" in cmd:
                state["injected"] = True
                state["status"] = "unhealthy"
                return _cp(0)
            if "exec" in argv and "rhythmyx_healthcheck.py" in cmd:
                if state["injected"]:
                    return _cp(
                        1,
                        stdout=(
                            "rhythmyx_context_failed "
                            "match='Failed startup of context' http=200\n"
                        ),
                    )
                return _cp(0, stdout="ok\n")
            return _cp(1, stderr=f"unexpected argv={argv}")

        ok, lines = proof.run_live_proof(
            container="perc-matrix-cms-h2",
            run=fake_run,
        )
        self.assertTrue(ok, msg="\n".join(lines))
        self.assertTrue(state["injected"])
        joined = "\n".join(lines)
        self.assertIn("OK live healthcheck", joined)

    def test_run_live_proof_fails_when_not_running(self):
        def fake_run(argv, **_kwargs):
            return _cp(1, stderr="No such object")

        ok, lines = proof.run_live_proof(container="gone", run=fake_run)
        self.assertFalse(ok)
        self.assertTrue(any("not running" in ln for ln in lines))

    def test_resolve_live_container(self):
        def fake_run(argv, **_kwargs):
            name = argv[-1]
            if name == "perc-matrix-cms-h2":
                return _cp(0, stdout="true\n")
            return _cp(1)

        name = proof.resolve_live_container(run=fake_run)
        self.assertEqual(name, "perc-matrix-cms-h2")


class MainCliTests(unittest.TestCase):
    def test_main_mock_quiet_ok(self):
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = proof.main(["--mode", "mock", "--quiet"])
        self.assertEqual(rc, proof.EXIT_OK)
        out = buf.getvalue()
        self.assertIn(f"RESULT:OK STEP:{proof.STEP} MODE:mock", out)

    def test_main_mock_verbose(self):
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = proof.main(["--mode", "mock"])
        self.assertEqual(rc, proof.EXIT_OK)
        out = buf.getvalue()
        self.assertIn("OK healthy path", out)
        self.assertIn("RESULT:OK", out)

    def test_main_bad_marker(self):
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = proof.main(["--mode", "mock", "--marker", "not-a-real-marker"])
        self.assertEqual(rc, proof.EXIT_FAIL)
        self.assertIn("RESULT:FAIL", buf.getvalue())

    def test_main_live_no_container(self):
        def fake_run(argv, **_kwargs):
            return _cp(1, stderr="No such object")

        buf = io.StringIO()
        with mock.patch.object(proof.subprocess, "run", side_effect=fake_run):
            with redirect_stdout(buf):
                rc = proof.main(["--mode", "live", "--quiet"])
        self.assertEqual(rc, proof.EXIT_FAIL)
        self.assertIn("RESULT:FAIL", buf.getvalue())
        self.assertIn("MODE:live", buf.getvalue())


if __name__ == "__main__":
    unittest.main()
