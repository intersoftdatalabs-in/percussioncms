#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for qa_service_lifecycle.py (no docker, no systemctl).

Pure argv builders plus the full lifecycle sequence against a fake
docker runner. No step may shell bare ``systemctl``/``journalctl`` on
the host — every service-manager invocation is ``docker exec <cell>``.
"""

from __future__ import annotations

import importlib.util
import subprocess
import sys
import unittest
from pathlib import Path
from typing import Dict, List

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "qa_service_lifecycle.py"
    name = "qa_service_lifecycle"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


qsl = _load()

CELL = "perc-matrix-cms-systemd"


class NamingTests(unittest.TestCase):
    def test_unit_for_products(self):
        self.assertEqual(qsl.unit_for("cms"), "PercussionCMS")
        self.assertEqual(qsl.unit_for("dts"), "PercussionProductionDTS")
        with self.assertRaises(ValueError):
            qsl.unit_for("cockroach")

    def test_container_names(self):
        self.assertEqual(
            qsl.systemd_container_name("cms"), "perc-matrix-cms-systemd"
        )
        self.assertEqual(
            qsl.systemd_container_name("dts", "shard-a"),
            "perc-matrix-dts-systemd-shard-a",
        )
        self.assertEqual(qsl.systemd_container_name("cms", None), CELL)

    def test_validate_container_name_accepts_systemd_cells(self):
        self.assertEqual(qsl.validate_container_name(CELL), CELL)
        self.assertEqual(
            qsl.validate_container_name("perc-matrix-dts-systemd-x1"),
            "perc-matrix-dts-systemd-x1",
        )

    def test_validate_container_name_refuses_host_and_plain_cells(self):
        for bad in ("", "perc-matrix-cms-h2", "myhost", "localhost",
                    "perc-matrix-cms-h2-suffix"):
            with self.assertRaises(qsl.LifecycleError):
                qsl.validate_container_name(bad)


class ArgvTests(unittest.TestCase):
    def test_run_argv_is_unprivileged_with_private_cgroup(self):
        argv = qsl.docker_run_systemd_argv(
            image=qsl.SYSTEMD_IMAGE,
            container_name=CELL,
            installer_jar_host=Path("/tmp") / "perc-distribution-tree.jar",
            network="perc-matrix-net",
            product="cms",
        )
        self.assertEqual(argv[:4], ["docker", "run", "-d", "--name"])
        self.assertNotIn("--privileged", argv)
        self.assertNotIn("--cgroupns=host", argv)
        self.assertIn("--cgroupns=private", argv)
        self.assertIn("--cap-add=SYS_ADMIN", argv)
        self.assertNotIn("/sys/fs/cgroup:/sys/fs/cgroup:rw", argv)
        self.assertIn("--tmpfs", argv)
        self.assertEqual(argv[-1], qsl.SYSTEMD_IMAGE)
        # No host ports published: all assertions run in-container.
        self.assertNotIn("-p", argv)

    def test_run_argv_refuses_non_cell_target(self):
        with self.assertRaises(qsl.LifecycleError):
            qsl.docker_run_systemd_argv(
                image=qsl.SYSTEMD_IMAGE,
                container_name="some-host",
                installer_jar_host=Path("/tmp") / "x.jar",
                network="n",
                product="cms",
            )

    def test_install_exec_uses_no_start(self):
        argv = qsl.install_exec_argv(CELL, product="cms")
        self.assertEqual(argv[:3], ["docker", "exec", CELL])
        self.assertIn("--no-start", argv)
        self.assertIn("--no-keep-alive", argv)
        self.assertNotIn("--db.password=test-only-pw-7324", " ".join(argv))
        # mkd-code-review[secrets.heuristic]: adjudicated FP — the literal
        # above is a canary asserting NO test password appears in argv.

    def test_service_install_exec_requires_systemd(self):
        argv = qsl.service_install_exec_argv(CELL, product="cms", unit="PercussionCMS")
        self.assertEqual(
            argv,
            ["docker", "exec", "-i", CELL, "bash", qsl.CMS_SERVICE_SCRIPT,
             "PercussionCMS", "install", "--systemd"],
        )
        dts = qsl.service_install_exec_argv(
            "perc-matrix-dts-systemd", product="dts", unit="PercussionProductionDTS")
        self.assertIn(qsl.DTS_SERVICE_SCRIPT, dts)
        self.assertIn("--systemd", dts)

    def test_systemctl_exec_verbs_and_units(self):
        for verb in ("start", "stop", "restart", "status"):
            argv = qsl.systemctl_exec_argv(CELL, verb, "PercussionCMS")
            self.assertEqual(
                argv, ["docker", "exec", CELL, "systemctl", verb,
                       "PercussionCMS.service"])
        bare = qsl.systemctl_exec_argv(CELL, "daemon-reload")
        self.assertEqual(bare, ["docker", "exec", CELL, "systemctl", "daemon-reload"])
        with self.assertRaises(ValueError):
            qsl.systemctl_exec_argv(CELL, "poweroff", "PercussionCMS")
        with self.assertRaises(ValueError):
            qsl.systemctl_exec_argv(CELL, "start", "")

    def test_no_bare_host_systemctl_anywhere(self):
        """Every builder routes through ``docker exec`` — host systemd untouched."""
        argvs = [
            qsl.install_exec_argv(CELL, product="cms"),
            qsl.service_install_exec_argv(CELL, product="cms", unit="PercussionCMS"),
            qsl.systemctl_exec_argv(CELL, "restart", "PercussionCMS"),
            qsl.journalctl_exec_argv(CELL, "PercussionCMS"),
            qsl.service_uninstall_exec_argv(CELL, product="cms", unit="PercussionCMS"),
            qsl.leftover_check_exec_argv(CELL, "PercussionCMS"),
            qsl.wait_systemd_exec_argv(CELL),
        ]
        for argv in argvs:
            self.assertEqual(argv[:2], ["docker", "exec"], msg=argv)

    def test_leftover_paths_cover_units_and_links(self):
        paths = qsl.leftover_unit_paths("PercussionCMS")
        self.assertIn("/etc/systemd/system/PercussionCMS.service", paths)
        self.assertIn(
            "/etc/systemd/system/multi-user.target.wants/PercussionCMS.service", paths)
        self.assertIn("/etc/init.d/PercussionCMS", paths)
        self.assertIn("/etc/default/PercussionCMS", paths)

    def test_parse_active_state(self):
        self.assertTrue(qsl.is_unit_active("active\n"))
        self.assertFalse(qsl.is_unit_active("inactive\n"))
        self.assertFalse(qsl.is_unit_active("failed\n"))
        self.assertFalse(qsl.is_unit_active(""))
        self.assertEqual(qsl.parse_active_state("  enabled\n"), "enabled")


class FakeRunner:
    """Scripted ``docker exec`` fake: maps argv fragments to (rc, output).

    ``is-active`` is stateful: ``systemctl stop`` flips the unit to
    ``inactive``; ``start``/``restart`` flip it back to ``active``.
    """

    def __init__(self, script: Dict[str, tuple]):
        self.script = script
        self.calls: List[List[str]] = []
        self.unit_active = False

    def __call__(self, argv, **kwargs):
        self.calls.append(list(argv))
        joined = " ".join(argv)
        if "timeout-step" in joined:
            raise subprocess.TimeoutExpired(argv, timeout=kwargs.get("timeout", 0))
        if "systemctl stop" in joined:
            self.unit_active = False
        elif "systemctl start" in joined or "systemctl restart" in joined:
            self.unit_active = True
        if "is-active " in joined or joined.endswith("is-active"):
            state = "active\n" if self.unit_active else "inactive\n"
            return subprocess.CompletedProcess(argv, 3 if not self.unit_active else 0,
                                              stdout=state, stderr="")
        for fragment, (rc, out) in self.script.items():
            if fragment in joined:
                return subprocess.CompletedProcess(argv, rc, stdout=out, stderr="")
        raise AssertionError(f"unstubbed docker call: {joined}")


def _green_script() -> Dict[str, tuple]:
    return {
        "is-system-running --wait": (0, "running\n"),
        "cell-entrypoint.py": (0, "RESULT:OK STEP:matrix-cell\n"),
        "install --systemd": (0, "Enabled PercussionCMS.service (not started).\n"),
        "is-enabled": (0, "enabled\n"),
        "systemctl start": (0, ""),
        "systemctl stop": (0, ""),
        "systemctl restart": (0, ""),
        "systemctl status": (0, "Active: active (running)\n"),
        "curl -sf": (0, "<html>login</html>"),
        "journalctl": (0, "rxjetty.sh: Jetty running pid=123\n"),
        "uninstall": (0, "done\n"),
        "test ! -e": (0, ""),
        "rm -f": (0, ""),
    }


class LifecycleSequenceTests(unittest.TestCase):
    def _run_with(self, script: Dict[str, tuple], **overrides):
        fake = FakeRunner(script)
        result = qsl.run_service_lifecycle(
            container=CELL, product="cms", run=fake, ready_interval=0, **overrides)
        return fake, result

    def test_full_green_sequence(self):
        fake, result = self._run_with(_green_script())
        self.assertTrue(result.ok, msg=result.detail)
        names = [step.name for step in result.steps]
        for expected in ("wait-systemd", "install", "install-unit",
                         "assert-enabled", "start", "assert-active-after-start",
                         "readiness-probe",
                         "stop", "assert-inactive-after-stop",
                         "restart-cycle-start",
                         "readiness-probe-after-cycle-start",
                         "restart", "assert-active-after-restart",
                         "readiness-probe-after-restart",
                         "status", "journalctl",
                         "uninstall", "assert-no-leftovers"):
            self.assertIn(expected, names)
        # Lifecycle verbs all driven through docker exec systemctl.
        systemctl_calls = [c for c in fake.calls if "systemctl" in c]
        self.assertGreaterEqual(len(systemctl_calls), 8)
        for call in systemctl_calls:
            self.assertEqual(call[:2], ["docker", "exec"])

    def test_stop_leaves_unit_inactive_but_sequence_continues(self):
        script = _green_script()
        fake, result = self._run_with(script)
        self.assertTrue(result.ok)
        # stop was issued before the cycle start: order check.
        joined = [" ".join(c) for c in fake.calls]
        stop_idx = next(i for i, j in enumerate(joined) if "systemctl stop" in j)
        cycle_idx = next(
            i for i, j in enumerate(joined) if "restart-cycle-start" in j or
            ("systemctl start" in j and i > stop_idx))
        self.assertLess(stop_idx, cycle_idx)

    def test_start_failure_aborts_before_stop(self):
        script = _green_script()
        script["systemctl start"] = (1, "Failed to start\n")
        fake, result = self._run_with(script)
        self.assertFalse(result.ok)
        joined = [" ".join(c) for c in fake.calls]
        self.assertFalse(any("systemctl stop" in j for j in joined))

    def test_leftover_failure_fails_run(self):
        script = _green_script()
        script["test ! -e"] = (1, "")
        _fake, result = self._run_with(script)
        self.assertFalse(result.ok)
        self.assertIn("leftover", result.detail)

    def test_empty_journal_fails_run(self):
        script = _green_script()
        script["journalctl"] = (0, "\n")
        _fake, result = self._run_with(script)
        self.assertFalse(result.ok)
        self.assertIn("journalctl", result.detail)

    def test_readiness_after_restart_failure_fails_run(self):
        """A running-but-broken context after restart must not pass (Codex P2)."""
        script = _green_script()
        calls = {"curl": 0}

        base = FakeRunner(script)
        orig_call = base.__call__

        def _flaky_curl(argv, **kwargs):
            if "curl -sf" in " ".join(argv):
                calls["curl"] += 1
                # Initial + cycle probes pass; post-restart probe fails.
                if calls["curl"] >= 3:
                    return subprocess.CompletedProcess(argv, 7, stdout="", stderr="")
            return orig_call(argv, **kwargs)

        result = qsl.run_service_lifecycle(
            container=CELL, product="cms", run=_flaky_curl,
            ready_interval=0, ready_timeout=1)
        self.assertFalse(result.ok)
        names = [step.name for step in result.steps]
        self.assertIn("readiness-probe-after-restart", names)

    def test_boot_race_retries_bus_only(self):
        """Early 'Failed to connect to bus' retries; other failures end fast."""
        attempts = {"n": 0}

        def _boot_race(argv, **kwargs):
            joined = " ".join(argv)
            if "is-system-running" in joined:
                attempts["n"] += 1
                if attempts["n"] < 3:
                    return subprocess.CompletedProcess(
                        argv, 1, stdout="Failed to connect to bus: No such file or directory\n",
                        stderr="")
                return subprocess.CompletedProcess(argv, 0, stdout="running\n", stderr="")
            return FakeRunner(_green_script())(argv, **kwargs)

        steps = qsl.wait_for_systemd(_boot_race, CELL, op_timeout=30,
                                     bus_timeout=60, bus_interval=0)
        self.assertEqual(len(steps), 3)
        self.assertEqual(steps[-1].name, "wait-systemd")
        self.assertEqual(steps[-1].returncode, 0)
        self.assertTrue(steps[0].name.startswith("wait-systemd-attempt-"))

    def test_boot_race_non_bus_failure_ends_fast(self):
        def _dead_bus(argv, **kwargs):
            joined = " ".join(argv)
            if "is-system-running" in joined:
                return subprocess.CompletedProcess(argv, 1, stdout="nope\n", stderr="")
            return FakeRunner(_green_script())(argv, **kwargs)

        steps = qsl.wait_for_systemd(_dead_bus, CELL, op_timeout=30,
                                     bus_timeout=60, bus_interval=0)
        self.assertEqual(len(steps), 1)
        self.assertEqual(steps[0].returncode, 1)

    def test_step_timeout_becomes_evidence_not_exception(self):
        """TimeoutExpired converts to an rc=124 step; verdict still FAIL (Codex P2)."""
        def _timeout_run(argv, **kwargs):
            raise subprocess.TimeoutExpired(argv, timeout=kwargs.get("timeout", 0))

        step = qsl._run_step(_timeout_run, "timeout-step",
                             ["docker", "exec", CELL, "timeout-step"], timeout=30)
        self.assertEqual(step.returncode, 124)
        self.assertIn("TIMEOUT", step.output_tail)

    def test_remove_cell_container_argv(self):
        argv = qsl.remove_cell_container(
            CELL, FakeRunner(_green_script()))
        self.assertEqual(argv.name, "remove-cell")
        self.assertEqual(argv.argv, ["docker", "rm", "-f", CELL])

    def test_refuses_non_cell_container(self):
        with self.assertRaises(qsl.LifecycleError):
            qsl.run_service_lifecycle(
                container="perc-matrix-cms-h2", product="cms",
                run=FakeRunner(_green_script()))


if __name__ == "__main__":
    unittest.main()
