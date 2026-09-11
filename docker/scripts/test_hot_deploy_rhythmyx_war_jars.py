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


MAIL2_VERSION = "2.0.0-M1"
JAKARTA_MAIL_VERSION = "2.0.2"


def _write_parent_pom(
    root: Path, version: str = MAIL2_VERSION, mail_impl: str = JAKARTA_MAIL_VERSION
) -> None:
    (root / "pom.xml").write_text(
        f"""<?xml version="1.0" encoding="UTF-8"?>
<project>
  <properties>
    <commons-email.version>{version}</commons-email.version>
    <jakarta.mail.impl.version>{mail_impl}</jakarta.mail.impl.version>
  </properties>
</project>
""",
        encoding="utf-8",
    )


def _artifact_version(artifact: str) -> str:
    if artifact == "jakarta.mail":
        return JAKARTA_MAIL_VERSION
    return MAIL2_VERSION


def _write_mail2_jars(directory: Path) -> None:
    directory.mkdir(parents=True, exist_ok=True)
    for artifact, _prop, _group in hdj.EXTRA_RUNTIME_JARS:
        ver = _artifact_version(artifact)
        _write_jar(directory / f"{artifact}-{ver}.jar", "marker/Marker.class")


def _m2_mail2_dir(m2_root: Path, artifact: str, group: tuple[str, ...], version: str) -> Path:
    return m2_root.joinpath(*group, artifact, version)


def _layout(root: Path, *, sitemap: bool = True, mail2: bool = True) -> Path:
    _write_parent_pom(root)
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
        root / "modules" / "extensions-workflow" / "target" / "extensions-workflow-8.2.0-SNAPSHOT.jar",
        "com/percussion/workflow/mail/PSSecureMailProgram.class",
    )
    _write_jar(
        root / "system" / "target" / "perc-system-8.2.0-SNAPSHOT-javadoc.jar",
        "index.html",
    )
    if mail2:
        _write_mail2_jars(root / "system" / "target")
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


class TestMail2Resolve(unittest.TestCase):
    def test_parse_commons_email_version(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            _write_parent_pom(root, "2.0.0-M1")
            self.assertEqual(hdj.parse_commons_email_version(root), "2.0.0-M1")

    def test_is_stale_commons_email1_name(self):
        self.assertTrue(hdj.is_stale_commons_email1_name("commons-email-1.6.0.jar"))
        self.assertFalse(
            hdj.is_stale_commons_email1_name("commons-email2-core-2.0.0-M1.jar")
        )
        self.assertFalse(
            hdj.is_stale_commons_email1_name("commons-email2-jakarta-2.0.0-M1.jar")
        )
        self.assertFalse(hdj.is_stale_commons_email1_name("perc-system-8.2.0-SNAPSHOT.jar"))

    def test_is_versioned_artifact_jar_distinguishes_mail_api(self):
        self.assertTrue(
            hdj.is_versioned_artifact_jar("jakarta.mail-2.0.2.jar", "jakarta.mail")
        )
        self.assertTrue(
            hdj.is_versioned_artifact_jar("jakarta.mail-1.6.8.jar", "jakarta.mail")
        )
        self.assertFalse(
            hdj.is_versioned_artifact_jar("jakarta.mail-api-2.1.3.jar", "jakarta.mail")
        )

    def test_default_m2_root_is_user_repository(self):
        resolved = hdj.default_m2_root()
        self.assertTrue(str(resolved).replace("\\", "/").endswith(".m2/repository"))

    def test_resolves_from_system_target(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td))
            jars, rc = hdj.resolve_mail2_jars(root, m2_root=Path(td) / "empty-m2")
            self.assertEqual(rc, hdj.EXIT_OK)
            names = {artifact: path.name for artifact, path in jars}
            self.assertEqual(
                names["commons-email2-core"],
                "commons-email2-core-2.0.0-M1.jar",
            )
            self.assertEqual(
                names["commons-email2-jakarta"],
                "commons-email2-jakarta-2.0.0-M1.jar",
            )
            self.assertEqual(names["jakarta.mail"], "jakarta.mail-2.0.2.jar")

    def test_resolves_from_m2_when_target_missing(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td), mail2=False)
            m2 = Path(td) / "m2"
            for artifact, _prop, group in hdj.EXTRA_RUNTIME_JARS:
                ver = _artifact_version(artifact)
                dest = _m2_mail2_dir(m2, artifact, group, ver)
                _write_jar(
                    dest / f"{artifact}-{ver}.jar",
                    "marker/Marker.class",
                )
            jars, rc = hdj.resolve_mail2_jars(root, m2_root=m2)
            self.assertEqual(rc, hdj.EXIT_OK)
            self.assertEqual(len(jars), 3)
            for _artifact, path in jars:
                self.assertTrue(str(path).replace("\\", "/").find("/m2/") >= 0 or "m2" in path.parts)

    def test_missing_mail2_is_jar_not_found(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td), mail2=False)
            jars, rc = hdj.resolve_mail2_jars(root, m2_root=Path(td) / "empty-m2")
            self.assertEqual(rc, hdj.EXIT_JAR_NOT_FOUND)
            self.assertEqual(jars, [])


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

    def test_missing_mail2_refuses_deploy(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td), mail2=False)
            rc = hdj.deploy(
                root, dry_run=True, m2_root=Path(td) / "empty-m2"
            )
            self.assertEqual(rc, hdj.EXIT_JAR_NOT_FOUND)

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
            self.assertEqual(len(cp), 7)
            dests = [c[-1] for c in cp]
            self.assertTrue(all("WEB-INF/lib/" in d.replace("\\", "/") for d in dests))
            self.assertTrue(any("perc-system-8.2.0-SNAPSHOT.jar" in d for d in dests))
            self.assertTrue(any("rest-8.2.0-SNAPSHOT.jar" in d for d in dests))
            self.assertTrue(any("sitemanage-8.2.0-SNAPSHOT.jar" in d for d in dests))
            self.assertTrue(any("extensions-workflow-8.2.0-SNAPSHOT.jar" in d for d in dests))
            self.assertTrue(any("commons-email2-core-2.0.0-M1.jar" in d for d in dests))
            self.assertTrue(any("commons-email2-jakarta-2.0.0-M1.jar" in d for d in dests))
            self.assertTrue(any("jakarta.mail-2.0.2.jar" in d for d in dests))
            self.assertFalse(any(c[:2] == ["docker", "restart"] for c in calls))
            rms = [c for c in calls if c[:2] == ["docker", "exec"] and "rm" in c]
            self.assertTrue(
                any("perc-system-old.jar" in c[-1] for c in rms),
                msg=rms,
            )

    def test_removes_stale_commons_email1(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td))
            calls, fake = _stub_subprocess(
                lib_listing=["commons-email-1.6.0.jar", "commons-email2-core-2.0.0-M1.jar"]
            )
            with unittest.mock.patch.object(hdj.subprocess, "run", side_effect=fake):
                rc = hdj.deploy(root, dry_run=False, restart_jetty=False)
            self.assertEqual(rc, hdj.EXIT_OK)
            rms = [c for c in calls if c[:2] == ["docker", "exec"] and "rm" in c]
            self.assertTrue(
                any("commons-email-1.6.0.jar" in c[-1] for c in rms),
                msg=rms,
            )
            self.assertFalse(
                any("commons-email2-core-2.0.0-M1.jar" in c[-1] for c in rms),
                msg=rms,
            )

    def test_removes_stale_jakarta_mail_1x_not_api(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td))
            calls, fake = _stub_subprocess(
                lib_listing=[
                    "jakarta.mail-1.6.8.jar",
                    "jakarta.mail-api-2.1.3.jar",
                    "jakarta.mail-2.0.2.jar",
                ]
            )
            with unittest.mock.patch.object(hdj.subprocess, "run", side_effect=fake):
                rc = hdj.deploy(root, dry_run=False, restart_jetty=False)
            self.assertEqual(rc, hdj.EXIT_OK)
            rms = [c for c in calls if c[:2] == ["docker", "exec"] and "rm" in c]
            self.assertTrue(
                any("jakarta.mail-1.6.8.jar" in c[-1] for c in rms),
                msg=rms,
            )
            self.assertFalse(
                any("jakarta.mail-api-2.1.3.jar" in c[-1] for c in rms),
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
                [
                    "docker",
                    "exec",
                    "perc-matrix-cms-h2",
                    "truncate",
                    "-s",
                    "0",
                    hdj.SERVER_LOG,
                ],
                calls,
            )
            self.assertIn(
                ["docker", "exec", "-d", "perc-matrix-cms-h2", hdj.START_JETTY],
                calls,
            )
            self.assertFalse(
                any("perc-truncate-server-log" in c for c in calls),
                msg=calls,
            )

    def test_restart_jetty_falls_back_when_truncate_missing(self):
        with tempfile.TemporaryDirectory() as td:
            root = _layout(Path(td))
            calls, fake = _stub_subprocess()

            def fake_with_missing_truncate(argv, *args, **kwargs):
                if (
                    argv[:2] == ["docker", "exec"]
                    and len(argv) >= 4
                    and argv[3] == "truncate"
                ):
                    calls.append(list(argv))
                    return subprocess.CompletedProcess(
                        args=argv,
                        returncode=127,
                        stdout="",
                        stderr="truncate: not found",
                    )
                return fake(argv, *args, **kwargs)

            with unittest.mock.patch.object(
                hdj.subprocess, "run", side_effect=fake_with_missing_truncate
            ):
                rc = hdj.deploy(root, dry_run=False, restart_jetty=True)
            self.assertEqual(rc, hdj.EXIT_OK)
            self.assertIn(
                [
                    "docker",
                    "exec",
                    "perc-matrix-cms-h2",
                    "sh",
                    "-c",
                    ': > "$1"',
                    "perc-truncate-server-log",
                    hdj.SERVER_LOG,
                ],
                calls,
            )


if __name__ == "__main__":
    unittest.main()
