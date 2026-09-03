#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Copyright (c) 2026 Intersoft Data Labs, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#
# See the License for the specific language governing permissions and
# limitations under the License.
"""Unit tests for hot-deploy-rhythmyx-war-jars.py (no docker required)."""

from __future__ import annotations

import importlib.util
import os
import subprocess
import sys
import tempfile
import unittest
import unittest.mock
import zipfile
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "hot-deploy-rhythmyx-war-jars.py"
    name = "hot_deploy_rhythmyx_war_jars"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


hdj = _load()


def _stub_subprocess(*, docker_ps_names=("perc-matrix-cms-h2",), lib_listing=None):
    calls = []
    lib_listing = lib_listing if lib_listing is not None else []

    def fake_run(argv, *args, **kwargs):
        calls.append(list(argv))
        if argv[:2] == ["docker", "ps"]:
            return subprocess.CompletedProcess(
                args=argv,
                returncode=0,
                stdout="\n".join(docker_ps_names) + "\n",
                stderr="",
            )
        if argv[:2] == ["docker", "exec"] and "ls" in argv:
            return subprocess.CompletedProcess(
                args=argv,
                returncode=0,
                stdout="\n".join(lib_listing) + "\n",
                stderr="",
            )
        if argv[:2] == ["docker", "exec"] and "stat" in argv:
            return subprocess.CompletedProcess(
                args=argv, returncode=1, stdout="", stderr=""
            )
        if argv[:2] == ["docker", "exec"]:
            return subprocess.CompletedProcess(
                args=argv, returncode=0, stdout="", stderr=""
            )
        if argv[:2] == ["docker", "cp"]:
            return subprocess.CompletedProcess(
                args=argv, returncode=0, stdout="", stderr=""
            )
        if argv[:2] == ["docker", "restart"]:
            raise AssertionError("must not docker restart the QA cell")
        return subprocess.CompletedProcess(args=argv, returncode=0, stdout="", stderr="")

    return calls, fake_run


def _write_jar(path: Path, *entries: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w") as zf:
        for name in entries:
            zf.writestr(name, b"class")


def _layout(root: Path, *, sitemap: bool = True) -> Path:
    system_jar = root / "system" / "target" / "perc-system-8.2.0-SNAPSHOT.jar"
    entries = ["com/percussion/foo.class"]
    if sitemap:
        entries.append(hdj.SITEMAP_XML_CLASS)
    _write_jar(system_jar, *entries)
    _write_jar(root / "rest" / "target" / "rest-8.2.0-SNAPSHOT.jar", "com/percussion/rest/Marker.class")
    _write_jar(
        root / "projects" / "sitemanage" / "target" / "sitemanage-8.2.0-SNAPSHOT.jar",
        "com/percussion/apibridge/SitesAdaptor.class",
    )
    _write_jar(
        root / "system" / "target" / "perc-system-8.2.0-SNAPSHOT-javadoc.jar",
        "index.html",
    )
    return root


class TestNewestPrimaryJar(unittest.TestCase):
    def test_skips_javadoc_and_picks_snapshot(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            _layout(root)
            jar = hdj.newest_primary_jar(root / "system" / "target", "perc-system")
            self.assertIsNotNone(jar)
            self.assertEqual(jar.name, "perc-system-8.2.0-SNAPSHOT.jar")

    def test_missing_dir_is_none(self):
        with tempfile.TemporaryDirectory() as td:
            self.assertIsNone(
                hdj.newest_primary_jar(Path(td) / "missing", "perc-system")
            )

    def test_prefers_snapshot_filename_over_mtime(self):
        with tempfile.TemporaryDirectory() as td:
            target = Path(td) / "system" / "target"
            older = target / "perc-system-8.1.0-SNAPSHOT.jar"
            newer_name = target / "perc-system-8.2.0-SNAPSHOT.jar"
            stale = target / "perc-system-8.0.0.jar"
            _write_jar(stale, "x")
            _write_jar(older, "x")
            _write_jar(newer_name, "x")
            os.utime(stale, (9_999_999_999, 9_999_999_999))
            jar = hdj.newest_primary_jar(target, "perc-system")
            self.assertIsNotNone(jar)
            self.assertEqual(jar.name, "perc-system-8.2.0-SNAPSHOT.jar")

    def test_is_artifact_backup_name(self):
        self.assertTrue(
            hdj.is_artifact_backup_name(
                "perc-system-8.2.0-SNAPSHOT.jar.bak.20260902120000",
                "perc-system",
            )
        )
        self.assertFalse(
            hdj.is_artifact_backup_name(
                "perc-system-8.2.0-SNAPSHOT.jar",
                "perc-system",
            )
        )
        self.assertFalse(
            hdj.is_artifact_backup_name(
                "sitemanage-8.2.0-SNAPSHOT.jar.bak.1",
                "perc-system",
            )
        )


class TestSitemapXmlMarker(unittest.TestCase):
    def test_present(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td), sitemap=True)
            jar = root / "system" / "target" / "perc-system-8.2.0-SNAPSHOT.jar"
            self.assertTrue(hdj.jar_has_sitemap_xml_source(jar))

    def test_absent(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td), sitemap=False)
            jar = root / "system" / "target" / "perc-system-8.2.0-SNAPSHOT.jar"
            self.assertFalse(hdj.jar_has_sitemap_xml_source(jar))


class TestDeploy(unittest.TestCase):
    def test_dry_run_does_not_call_docker(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td))
            with unittest.mock.patch.object(hdj.subprocess, "run") as mock_run:
                rc = hdj.deploy(root, dry_run=True)
            self.assertEqual(rc, hdj.EXIT_OK)
            mock_run.assert_not_called()

    def test_missing_jar(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            rc = hdj.deploy(root, dry_run=True)
            self.assertEqual(rc, hdj.EXIT_JAR_NOT_FOUND)

    def test_missing_sitemap_class(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td), sitemap=False)
            rc = hdj.deploy(root, dry_run=True)
            self.assertEqual(rc, hdj.EXIT_MARKER_MISSING)

    def test_container_not_running(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td))
            calls, fake = _stub_subprocess(docker_ps_names=("other",))
            with unittest.mock.patch.object(hdj.subprocess, "run", side_effect=fake):
                rc = hdj.deploy(root, dry_run=False)
            self.assertEqual(rc, hdj.EXIT_CONTAINER_NOT_RUNNING)
            self.assertTrue(any(c[:2] == ["docker", "ps"] for c in calls))

    def test_copies_three_jars_and_does_not_docker_restart(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td))
            calls, fake = _stub_subprocess(
                lib_listing=["perc-system-old.jar", "readme.txt"]
            )
            with unittest.mock.patch.object(hdj.subprocess, "run", side_effect=fake):
                rc = hdj.deploy(root, dry_run=False, restart_jetty=False)
            self.assertEqual(rc, hdj.EXIT_OK)
            cp = [c for c in calls if c[:2] == ["docker", "cp"]]
            self.assertEqual(len(cp), 3)
            dests = [c[-1] for c in cp]
            self.assertTrue(all("WEB-INF/lib/" in d.replace("\\", "/") for d in dests))
            self.assertTrue(any("perc-system-8.2.0-SNAPSHOT.jar" in d for d in dests))
            self.assertTrue(any("rest-8.2.0-SNAPSHOT.jar" in d for d in dests))
            self.assertTrue(any("sitemanage-8.2.0-SNAPSHOT.jar" in d for d in dests))
            self.assertFalse(any(c[:2] == ["docker", "restart"] for c in calls))
            rms = [c for c in calls if c[:2] == ["docker", "exec"] and "rm" in c]
            self.assertTrue(
                any("perc-system-old.jar" in c[-1] for c in rms),
                msg=rms,
            )

    def test_restart_jetty_is_in_cell_stop_then_detached_start(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td))
            calls, fake = _stub_subprocess()
            with unittest.mock.patch.object(hdj.subprocess, "run", side_effect=fake):
                rc = hdj.deploy(root, dry_run=False, restart_jetty=True)
            self.assertEqual(rc, hdj.EXIT_OK)
            self.assertIn(
                ["docker", "exec", "perc-matrix-cms-h2", hdj.STOP_JETTY],
                calls,
            )
            self.assertIn(
                ["docker", "exec", "-d", "perc-matrix-cms-h2", hdj.START_JETTY],
                calls,
            )


if __name__ == "__main__":
    unittest.main()
