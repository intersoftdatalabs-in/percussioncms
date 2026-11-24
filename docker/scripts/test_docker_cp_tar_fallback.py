#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for docker_cp_tar_fallback.py (no docker required).

Covers the #7194 consolidation: one shared ``docker cp`` with tar-pipe
fallback (fallback #7157) used by the three hot-deploy scripts. A stub
``subprocess.run`` captures invocations and asserts exit-code mapping;
the renamed-destination test proves the member name comes from the
``container_dest`` basename rather than the host file name.
"""

from __future__ import annotations

import io
import logging
import subprocess
import tarfile
import tempfile
import unittest
import unittest.mock
from pathlib import Path

import docker_cp_tar_fallback as dctf


def _stub_subprocess(*, returncodes=None):
    """Replace ``subprocess.run`` with a recorder + returncode map."""
    returncodes = returncodes or {}
    calls = []

    def fake_run(argv, *args, **kwargs):
        calls.append((list(argv), kwargs))
        if argv[:2] == ["docker", "exec"] and "tar" in argv:
            return subprocess.CompletedProcess(
                args=argv, returncode=returncodes.get("tar", 0), stdout="", stderr=""
            )
        return subprocess.CompletedProcess(args=argv, returncode=0, stdout="", stderr="")

    return calls, fake_run


class TestPureHelpers(unittest.TestCase):
    def test_member_comes_from_container_dest_basename(self):
        self.assertEqual(
            dctf.tar_member_for_container_dest("/opt/Percussion/jetty/base/lib/foo.jar"),
            "foo.jar",
        )

    def test_member_ignores_host_style_prefix(self):
        # Container paths are always POSIX even on a Windows host.
        self.assertEqual(dctf.tar_member_for_container_dest("C:/x/y/bar.js"), "bar.js")

    def test_parent_is_dirname_or_root(self):
        self.assertEqual(
            dctf.tar_parent_for_container_dest("/opt/Percussion/jetty/base/lib/foo.jar"),
            "/opt/Percussion/jetty/base/lib",
        )
        self.assertEqual(dctf.tar_parent_for_container_dest("foo.jar"), "/")

    def test_unsafe_members_rejected(self):
        for bad in ("", ".", "..", "a/b", "a\\b"):
            self.assertFalse(dctf.is_safe_tar_member(bad), msg=bad)
        self.assertTrue(dctf.is_safe_tar_member("foo-1.2.jar"))


class TestDockerCpWithTarFallback(unittest.TestCase):
    def test_cp_success_never_touches_tar(self):
        with tempfile.TemporaryDirectory() as td:
            host = Path(td) / "foo.jar"
            host.write_bytes(b"jar-bytes")
            calls, fake_run = _stub_subprocess()
            with unittest.mock.patch.object(
                dctf.subprocess, "run", side_effect=fake_run
            ):
                rc = dctf.docker_cp_with_tar_fallback(
                    host,
                    "cell",
                    "/opt/Percussion/jetty/base/lib/foo.jar",
                    dry_run=False,
                    cp_runner=lambda argv: dctf.EXIT_OK,
                )
            self.assertEqual(rc, dctf.EXIT_OK)
            self.assertEqual(calls, [])

    def test_renamed_destination_uses_container_basename(self):
        """#7194: host ``foo-1.2.jar`` deployed as container ``foo.jar``.

        The fallback archive member must be ``foo.jar`` (what ``docker
        cp`` would write), not the host file name, and ``-C`` must be
        the destination parent.
        """
        with tempfile.TemporaryDirectory() as td:
            host = Path(td) / "foo-1.2.jar"
            host.write_bytes(b"versioned-jar-bytes")
            calls, fake_run = _stub_subprocess(returncodes={"tar": 0})
            with unittest.mock.patch.object(
                dctf.subprocess, "run", side_effect=fake_run
            ):
                rc = dctf.docker_cp_with_tar_fallback(
                    host,
                    "cell",
                    "/opt/Percussion/jetty/base/lib/foo.jar",
                    dry_run=False,
                    cp_runner=lambda argv: dctf.EXIT_DOCKER_FAILED,
                )
            self.assertEqual(rc, dctf.EXIT_OK)
            tar_calls = [c for c in calls if c[0][:2] == ["docker", "exec"]]
            self.assertEqual(len(tar_calls), 1)
            argv = tar_calls[0][0]
            self.assertEqual(
                argv,
                [
                    "docker", "exec", "-i", "cell",
                    "tar", "-xf", "-", "-C", "/opt/Percussion/jetty/base/lib",
                ],
            )
            payload = tar_calls[0][1].get("input")
            self.assertIsNotNone(payload, msg="fallback must pipe archive bytes via input=")
            with tarfile.open(fileobj=io.BytesIO(payload), mode="r") as tf:
                members = tf.getmembers()
                self.assertEqual(len(members), 1)
                self.assertEqual(members[0].name, "foo.jar")
                extracted = tf.extractfile(members[0])
                assert extracted is not None
                self.assertEqual(extracted.read(), b"versioned-jar-bytes")
                self.assertEqual(members[0].mtime, int(host.stat().st_mtime))

    def test_same_name_destination_keeps_basename(self):
        """Non-rename deploys keep working: member == shared basename."""
        with tempfile.TemporaryDirectory() as td:
            host = Path(td) / "foo.jar"
            host.write_bytes(b"jar-bytes")
            calls, fake_run = _stub_subprocess(returncodes={"tar": 0})
            with unittest.mock.patch.object(
                dctf.subprocess, "run", side_effect=fake_run
            ):
                rc = dctf.docker_cp_with_tar_fallback(
                    host,
                    "cell",
                    "/opt/Percussion/jetty/base/lib/foo.jar",
                    dry_run=False,
                    cp_runner=lambda argv: dctf.EXIT_DOCKER_FAILED,
                )
            self.assertEqual(rc, dctf.EXIT_OK)
            payload = calls[0][1].get("input")
            with tarfile.open(fileobj=io.BytesIO(payload), mode="r") as tf:
                self.assertEqual(tf.getmembers()[0].name, "foo.jar")

    def test_tar_failure_propagates(self):
        with tempfile.TemporaryDirectory() as td:
            host = Path(td) / "foo.jar"
            host.write_bytes(b"jar-bytes")
            calls, fake_run = _stub_subprocess(returncodes={"tar": 1})
            with unittest.mock.patch.object(
                dctf.subprocess, "run", side_effect=fake_run
            ):
                rc = dctf.docker_cp_with_tar_fallback(
                    host,
                    "cell",
                    "/opt/Percussion/jetty/base/lib/foo.jar",
                    dry_run=False,
                    cp_runner=lambda argv: dctf.EXIT_DOCKER_FAILED,
                )
            self.assertEqual(rc, dctf.EXIT_DOCKER_FAILED)
            self.assertEqual(len(calls), 1)

    def test_unsafe_container_basename_refused(self):
        with tempfile.TemporaryDirectory() as td:
            host = Path(td) / "foo.jar"
            host.write_bytes(b"jar-bytes")
            calls, fake_run = _stub_subprocess()
            with unittest.mock.patch.object(
                dctf.subprocess, "run", side_effect=fake_run
            ):
                rc = dctf.docker_cp_with_tar_fallback(
                    host,
                    "cell",
                    "/opt/Percussion/jetty/base/lib/..",
                    dry_run=False,
                    cp_runner=lambda argv: dctf.EXIT_DOCKER_FAILED,
                )
            self.assertEqual(rc, dctf.EXIT_DOCKER_FAILED)
            self.assertEqual(calls, [])

    def test_dry_run_cp_failure_returns_without_tar(self):
        with tempfile.TemporaryDirectory() as td:
            host = Path(td) / "foo.jar"
            host.write_bytes(b"jar-bytes")
            calls, fake_run = _stub_subprocess()
            with unittest.mock.patch.object(
                dctf.subprocess, "run", side_effect=fake_run
            ):
                rc = dctf.docker_cp_with_tar_fallback(
                    host,
                    "cell",
                    "/opt/Percussion/jetty/base/lib/foo.jar",
                    dry_run=True,
                    cp_runner=lambda argv: dctf.EXIT_DOCKER_FAILED,
                )
            self.assertEqual(rc, dctf.EXIT_DOCKER_FAILED)
            self.assertEqual(calls, [])


if __name__ == "__main__":
    logging.basicConfig(level=logging.CRITICAL)
    unittest.main()
