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
"""Unit tests for hot-deploy-webui-modern.py (no docker required)."""

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
    path = SCRIPTS / "hot-deploy-webui-modern.py"
    name = "hot_deploy_webui_modern"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


hdw = _load()


def _stub_subprocess(*, docker_ps_names=("perc-matrix-cms-h2",), returncodes=None):
    returncodes = returncodes or {}
    calls = []

    def fake_run(argv, *args, **kwargs):
        calls.append((list(argv), kwargs))
        if argv[:2] == ["docker", "ps"]:
            return subprocess.CompletedProcess(
                args=argv,
                returncode=0,
                stdout="\n".join(docker_ps_names) + "\n",
                stderr="",
            )
        if argv[:2] == ["docker", "exec"]:
            return subprocess.CompletedProcess(
                args=argv, returncode=returncodes.get("exec", 0), stdout="", stderr=""
            )
        if argv[:2] == ["docker", "cp"]:
            return subprocess.CompletedProcess(
                args=argv, returncode=returncodes.get("cp", 0), stdout="", stderr=""
            )
        if argv[:2] == ["docker", "restart"]:
            return subprocess.CompletedProcess(
                args=argv, returncode=0, stdout="", stderr=""
            )
        return subprocess.CompletedProcess(args=argv, returncode=0, stdout="", stderr="")

    return calls, fake_run


def _write_modern_tree(
    root: Path,
    *,
    include_object_storage: bool,
    include_rss_atom: bool = True,
    include_icalendar: bool = True,
    include_sitemap_xml: bool = True,
    include_robots_txt: bool = True,
    include_developer_am_new: bool = True,
    include_index: bool = True,
) -> Path:
    modern = root / "cm" / "modern"
    assets = modern / "assets"
    assets.mkdir(parents=True)
    (assets / "perc-modern-ui.js").write_text(
        'import"./developer-AbCd1234.js";\n',
        encoding="utf-8",
    )
    (assets / "perc-modern-ui.css").write_text("/* css */\n", encoding="utf-8")
    chunk = 'export const k="http-json";\n'
    if include_object_storage:
        chunk += 'export const os="object-storage";\n'
    if include_rss_atom:
        chunk += 'export const rss="rss-atom";\n'
    if include_icalendar:
        chunk += 'export const ics="icalendar";\n'
    if include_sitemap_xml:
        chunk += 'export const sm="sitemap-xml";\n'
    if include_robots_txt:
        chunk += 'export const rb="robots-txt";\n'
    if include_developer_am_new:
        chunk += 'export const am="developer-am-new";\n'
    (assets / "developer-AbCd1234.js").write_text(chunk, encoding="utf-8")
    if include_index:
        (modern / "index.html").write_text(
            '<script type="module" src="/cm/modern/assets/perc-modern-ui.js"></script>\n',
            encoding="utf-8",
        )
    return modern


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            hdw.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


class TestValidateSrc(unittest.TestCase):
    def test_missing_dir(self):
        with tempfile.TemporaryDirectory() as td:
            missing = Path(td) / "no-such-modern-dir"
            self.assertFalse(missing.exists())
            rc = hdw.validate_src(missing, require_object_storage=True)
            self.assertEqual(rc, hdw.EXIT_SRC_NOT_FOUND)

    def test_missing_entry_js(self):
        with tempfile.TemporaryDirectory() as td:
            src = Path(td) / "modern"
            (src / "assets").mkdir(parents=True)
            (src / "assets" / "developer-x.js").write_text(
                'k="object-storage"', encoding="utf-8"
            )
            rc = hdw.validate_src(src, require_object_storage=True)
            self.assertEqual(rc, hdw.EXIT_SRC_NOT_FOUND)

    def test_marker_missing(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(
                Path(td),
                include_object_storage=False,
                include_rss_atom=False,
                include_icalendar=False,
                include_sitemap_xml=False,
                include_robots_txt=False,
                include_developer_am_new=False,
            )
            rc = hdw.validate_src(src, require_object_storage=True)
            self.assertEqual(rc, hdw.EXIT_MARKER_MISSING)
            self.assertEqual(
                hdw.bundle_missing_kind_markers(src),
                [
                    "object-storage",
                    "rss-atom",
                    "icalendar",
                    "sitemap-xml",
                    "robots-txt",
                    "developer-am-new",
                ],
            )

    def test_rss_atom_missing_fails_even_when_object_storage_present(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(
                Path(td), include_object_storage=True, include_rss_atom=False
            )
            rc = hdw.validate_src(src, require_kind_markers=True)
            self.assertEqual(rc, hdw.EXIT_MARKER_MISSING)
            self.assertEqual(hdw.bundle_missing_kind_markers(src), ["rss-atom"])
            self.assertTrue(hdw.bundle_contains_marker(src, hdw.OBJECT_STORAGE_MARKER))
            self.assertFalse(hdw.bundle_contains_marker(src, hdw.RSS_ATOM_MARKER))

    def test_object_storage_missing_fails_even_when_rss_atom_present(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(
                Path(td), include_object_storage=False, include_rss_atom=True
            )
            rc = hdw.validate_src(src, require_kind_markers=True)
            self.assertEqual(rc, hdw.EXIT_MARKER_MISSING)
            self.assertEqual(hdw.bundle_missing_kind_markers(src), ["object-storage"])

    def test_sitemap_xml_missing_fails_even_when_older_kinds_present(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(
                Path(td),
                include_object_storage=True,
                include_rss_atom=True,
                include_icalendar=True,
                include_sitemap_xml=False,
            )
            rc = hdw.validate_src(src, require_kind_markers=True)
            self.assertEqual(rc, hdw.EXIT_MARKER_MISSING)
            self.assertEqual(hdw.bundle_missing_kind_markers(src), ["sitemap-xml"])
            self.assertFalse(hdw.bundle_contains_marker(src, hdw.SITEMAP_XML_MARKER))

    def test_robots_txt_missing_fails_even_when_older_kinds_present(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(
                Path(td),
                include_object_storage=True,
                include_rss_atom=True,
                include_icalendar=True,
                include_sitemap_xml=True,
                include_robots_txt=False,
            )
            rc = hdw.validate_src(src, require_kind_markers=True)
            self.assertEqual(rc, hdw.EXIT_MARKER_MISSING)
            self.assertEqual(hdw.bundle_missing_kind_markers(src), ["robots-txt"])
            self.assertFalse(hdw.bundle_contains_marker(src, hdw.ROBOTS_TXT_MARKER))

    def test_marker_present(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(Path(td), include_object_storage=True)
            rc = hdw.validate_src(src, require_object_storage=True)
            self.assertEqual(rc, hdw.EXIT_OK)
            self.assertTrue(hdw.bundle_contains_marker(src))
            self.assertTrue(hdw.bundle_contains_marker(src, hdw.RSS_ATOM_MARKER))
            self.assertTrue(hdw.bundle_contains_marker(src, hdw.ICALENDAR_MARKER))
            self.assertTrue(hdw.bundle_contains_marker(src, hdw.SITEMAP_XML_MARKER))
            self.assertTrue(hdw.bundle_contains_marker(src, hdw.DEVELOPER_AM_NEW_MARKER))
            self.assertEqual(hdw.bundle_missing_kind_markers(src), [])

    def test_unquoted_object_storage_substring_is_not_enough(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(
                Path(td), include_object_storage=False, include_rss_atom=False
            )
            (src / "assets" / "developer-AbCd1234.js").write_text(
                'const u="https://example.com/object-storage/keys";\n'
                'const f="feed/rss-atom.xml";\n'
                'const am="developer-am-new";\n',
                encoding="utf-8",
            )
            rc = hdw.validate_src(src, require_object_storage=True)
            self.assertEqual(rc, hdw.EXIT_MARKER_MISSING)
            self.assertFalse(hdw.bundle_contains_marker(src))
            self.assertFalse(hdw.bundle_contains_marker(src, hdw.RSS_ATOM_MARKER))

    def test_backtick_quoted_production_bundle_is_enough(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(
                Path(td),
                include_object_storage=False,
                include_rss_atom=False,
                include_icalendar=False,
                include_sitemap_xml=False,
                include_robots_txt=False,
            )
            (src / "assets" / "developer-AbCd1234.js").write_text(
                "export const os=`object-storage`;\n"
                "export const rss=`rss-atom`;\n"
                "export const ics=`icalendar`;\n"
                "export const sm=`sitemap-xml`;\n"
                "export const rb=`robots-txt`;\n"
                "export const am=`developer-am-new`;\n",
                encoding="utf-8",
            )
            rc = hdw.validate_src(src, require_kind_markers=True)
            self.assertEqual(rc, hdw.EXIT_OK)
            self.assertTrue(hdw.bundle_contains_marker(src, hdw.OBJECT_STORAGE_MARKER))
            self.assertTrue(hdw.bundle_contains_marker(src, hdw.RSS_ATOM_MARKER))
            self.assertTrue(hdw.bundle_contains_marker(src, hdw.ICALENDAR_MARKER))
            self.assertTrue(hdw.bundle_contains_marker(src, hdw.SITEMAP_XML_MARKER))
            self.assertTrue(hdw.bundle_contains_marker(src, hdw.ROBOTS_TXT_MARKER))
            self.assertTrue(hdw.bundle_contains_marker(src, hdw.DEVELOPER_AM_NEW_MARKER))

    def test_unquoted_rss_atom_substring_is_not_enough(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(
                Path(td), include_object_storage=True, include_rss_atom=False
            )
            chunk = (src / "assets" / "developer-AbCd1234.js").read_text(encoding="utf-8")
            (src / "assets" / "developer-AbCd1234.js").write_text(
                chunk + 'const p="/services/rss-atom/preview";\n',
                encoding="utf-8",
            )
            rc = hdw.validate_src(src, require_kind_markers=True)
            self.assertEqual(rc, hdw.EXIT_MARKER_MISSING)
            self.assertFalse(hdw.bundle_contains_marker(src, hdw.RSS_ATOM_MARKER))

    def test_quoted_marker_only_in_unrelated_chunk_fails(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(Path(td), include_object_storage=False)
            (src / "assets" / "other-XXXX.js").write_text(
                'export const os="object-storage";\n',
                encoding="utf-8",
            )
            rc = hdw.validate_src(src, require_object_storage=True)
            self.assertEqual(rc, hdw.EXIT_MARKER_MISSING)
            self.assertFalse(hdw.bundle_contains_marker(src))

    def test_quoted_marker_inlined_in_entry_without_developer_import(self):
        with tempfile.TemporaryDirectory() as td:
            src = Path(td) / "modern"
            assets = src / "assets"
            assets.mkdir(parents=True)
            (assets / "perc-modern-ui.js").write_text(
                'const k="object-storage";\nconst r="rss-atom";\n'
                'const i="icalendar";\nconst s="sitemap-xml";\n'
                'const rb="robots-txt";\n'
                'const am="developer-am-new";\n',
                encoding="utf-8",
            )
            (assets / "perc-modern-ui.css").write_text("/* css */\n", encoding="utf-8")
            rc = hdw.validate_src(src, require_object_storage=True)
            self.assertEqual(rc, hdw.EXIT_OK)

    def test_inlined_object_storage_without_rss_atom_fails(self):
        with tempfile.TemporaryDirectory() as td:
            src = Path(td) / "modern"
            assets = src / "assets"
            assets.mkdir(parents=True)
            (assets / "perc-modern-ui.js").write_text(
                'const k="object-storage";\nconst am="developer-am-new";\n',
                encoding="utf-8",
            )
            (assets / "perc-modern-ui.css").write_text("/* css */\n", encoding="utf-8")
            rc = hdw.validate_src(src, require_kind_markers=True)
            self.assertEqual(rc, hdw.EXIT_MARKER_MISSING)

    def test_developer_am_new_missing_fails_even_when_kinds_present(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(
                Path(td),
                include_object_storage=True,
                include_rss_atom=True,
                include_developer_am_new=False,
            )
            rc = hdw.validate_src(src, require_kind_markers=True)
            self.assertEqual(rc, hdw.EXIT_MARKER_MISSING)
            self.assertEqual(
                hdw.bundle_missing_kind_markers(src), ["developer-am-new"]
            )


class TestContainerDestFile(unittest.TestCase):
    def test_posix_join_rejects_backslash_rel_as_os_path(self):
        dest = hdw.container_dest_file(
            "/opt/Percussion/jetty/base/webapps/Rhythmyx/cm/modern",
            "assets/perc-modern-ui.js",
        )
        self.assertEqual(
            dest,
            "/opt/Percussion/jetty/base/webapps/Rhythmyx/cm/modern/assets/perc-modern-ui.js",
        )
        self.assertNotIn("\\", dest)

    def test_rejects_relative_dest(self):
        with self.assertRaises(ValueError):
            hdw.container_dest_file("cm/modern", "assets/x.js")


class TestDeploy(unittest.TestCase):
    def test_dry_run_copies_entry_hashed_chunk_and_index(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(Path(td), include_object_storage=True)
            rc = hdw.deploy(src, dry_run=True)
            self.assertEqual(rc, hdw.EXIT_OK)

    def test_real_run_docker_cp_entry_and_chunk_no_restart(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(Path(td), include_object_storage=True)
            calls, fake_run = _stub_subprocess()
            with unittest.mock.patch.object(hdw.subprocess, "run", side_effect=fake_run):
                rc = hdw.deploy(src, dry_run=False)
            self.assertEqual(rc, hdw.EXIT_OK)
            restart = [c for c in calls if c[0][:2] == ["docker", "restart"]]
            self.assertEqual(restart, [])
            cp_calls = [c[0] for c in calls if c[0][:2] == ["docker", "cp"]]
            copied = [argv[3] for argv in cp_calls]
            self.assertTrue(
                any(c.endswith("/cm/modern/assets/perc-modern-ui.js") for c in copied),
                copied,
            )
            self.assertTrue(
                any(c.endswith("/cm/modern/assets/developer-AbCd1234.js") for c in copied),
                copied,
            )
            self.assertTrue(
                any(c.endswith("/cm/modern/index.html") for c in copied),
                copied,
            )
            for argv in cp_calls:
                self.assertNotIn("sh", argv)
                self.assertNotIn("-c", argv)
                self.assertTrue(argv[3].startswith("perc-matrix-cms-h2:/"))
                self.assertNotIn("\\", argv[3])

    def test_container_not_running(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(Path(td), include_object_storage=True)
            _calls, fake_run = _stub_subprocess(docker_ps_names=("other",))
            with unittest.mock.patch.object(hdw.subprocess, "run", side_effect=fake_run):
                rc = hdw.deploy(src, dry_run=False)
            self.assertEqual(rc, hdw.EXIT_CONTAINER_NOT_RUNNING)

    def test_docker_cp_failure(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(Path(td), include_object_storage=True)
            _calls, fake_run = _stub_subprocess(returncodes={"cp": 1})
            with unittest.mock.patch.object(hdw.subprocess, "run", side_effect=fake_run):
                rc = hdw.deploy(src, dry_run=False)
            self.assertEqual(rc, hdw.EXIT_DOCKER_FAILED)

    def test_skip_marker_allows_stale_bundle(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(
                Path(td), include_object_storage=False, include_rss_atom=False
            )
            rc = hdw.deploy(src, require_object_storage=False, dry_run=True)
            self.assertEqual(rc, hdw.EXIT_OK)

    def test_skip_kind_marker_flag_is_alias(self):
        with tempfile.TemporaryDirectory() as td:
            src = _write_modern_tree(
                Path(td), include_object_storage=False, include_rss_atom=False
            )
            rc = hdw.main(["--src", str(src), "--skip-kind-marker-check", "--dry-run"])
            self.assertEqual(rc, hdw.EXIT_OK)


if __name__ == "__main__":
    logging.basicConfig(level=logging.CRITICAL)
    unittest.main()
