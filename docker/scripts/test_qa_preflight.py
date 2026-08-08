#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for docker/scripts/qa_preflight.py (#2486).

Filesystem-only: builds a synthetic repo + m2 layout in a tempdir and
exercises the preflight without docker, maven, or curl.
"""

from __future__ import annotations

import importlib.util
import io
import os
import sys
import tempfile
import unittest
import zipfile
from contextlib import redirect_stdout
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent

_spec = importlib.util.spec_from_file_location(
    "qa_preflight", SCRIPTS / "qa_preflight.py"
)
qa_preflight = importlib.util.module_from_spec(_spec)
assert _spec.loader is not None
sys.modules["qa_preflight"] = qa_preflight
_spec.loader.exec_module(qa_preflight)


def _touch(path: Path, mtime: float) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"")
    os.utime(path, (mtime, mtime))


def _make_war(war_path: Path, *, sitemanage_jar_name: str = "sitemanage-1.0.0.jar") -> None:
    war_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(war_path, "w") as zf:
        zf.writestr("WEB-INF/lib/" + sitemanage_jar_name, b"PK fake")


class _Repo:
    def __init__(self) -> None:
        self.td = tempfile.TemporaryDirectory()
        self.root = Path(self.td.name)

    def cleanup(self) -> None:
        self.td.cleanup()

    def layout_repo(self) -> Path:
        repo = self.root / "repo"
        (repo / "WebUI" / "target").mkdir(parents=True)
        (repo / "modules" / "perc-distribution-tree").mkdir(parents=True)
        return repo

    def layout_m2(self) -> Path:
        return self.root / "m2"


class TestPreflightFresh(unittest.TestCase):
    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.repo = self.repo_helper.layout_repo()
        self.m2 = self.repo_helper.layout_m2()

    def test_fresh_when_war_at_or_after_m2(self):
        # m2 jar older than WAR's mtime → FRESH (WAR was built after m2 install)
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", 200)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar")
        os.utime(war_path, (300, 300))

        rows = qa_preflight.run_preflight(self.repo, self.m2)
        self.assertFalse(qa_preflight.is_stale(rows))
        report = qa_preflight.format_report(rows, strict=True)
        self.assertIn("FRESH", report)

    def test_fresh_when_war_exactly_at_m2(self):
        same = 200
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", same)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar")
        os.utime(war_path, (same, same))

        rows = qa_preflight.run_preflight(self.repo, self.m2)
        self.assertFalse(qa_preflight.is_stale(rows))


class TestPreflightStale(unittest.TestCase):
    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.repo = self.repo_helper.layout_repo()
        self.m2 = self.repo_helper.layout_m2()

    def test_stale_when_war_built_before_m2_update(self):
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", 300)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar")
        os.utime(war_path, (250, 250))

        rows = qa_preflight.run_preflight(self.repo, self.m2)
        self.assertTrue(qa_preflight.is_stale(rows))
        report = qa_preflight.format_report(rows, strict=True)
        self.assertIn("STALE", report)

    def test_strict_returns_nonzero_when_stale(self):
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", 300)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar")
        os.utime(war_path, (250, 250))

        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_preflight.main([
                "--repo-root", str(self.repo),
                "--m2-root", str(self.m2),
                "--strict",
            ])
        self.assertEqual(rc, 2)
        self.assertIn("STALE", buf.getvalue())

    def test_non_strict_returns_zero_when_stale(self):
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", 300)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar")
        os.utime(war_path, (250, 250))

        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_preflight.main([
                "--repo-root", str(self.repo),
                "--m2-root", str(self.m2),
            ])
        self.assertEqual(rc, 0)
        self.assertIn("STALE", buf.getvalue())

    def test_stale_when_war_missing(self):
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", 300)
        rows = qa_preflight.run_preflight(self.repo, self.m2)
        self.assertTrue(qa_preflight.is_stale(rows))
        report = qa_preflight.format_report(rows, strict=True)
        self.assertIn("STALE", report)

    def test_stale_when_war_does_not_bundle_sitemanage(self):
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", 300)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        # WAR without sitemanage jar inside
        with zipfile.ZipFile(war_path, "w") as zf:
            zf.writestr("WEB-INF/lib/some-other.jar", b"PK fake")
        os.utime(war_path, (250, 250))

        rows = qa_preflight.run_preflight(self.repo, self.m2)
        # War present but doesn't bundle sitemanage → cannot preflight → stale (incomplete dist)
        self.assertTrue(qa_preflight.is_stale(rows))


class TestPreflightNoOp(unittest.TestCase):
    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.repo = self.repo_helper.layout_repo()
        self.m2 = self.repo_helper.layout_m2()

    def test_no_m2_jar_means_noop(self):
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar")
        os.utime(war_path, (250, 250))

        rows = qa_preflight.run_preflight(self.repo, self.m2)
        # Not stale: the developer hasn't built sitemanage yet, so the check is a no-op.
        self.assertFalse(qa_preflight.is_stale(rows))
        report = qa_preflight.format_report(rows, strict=True)
        self.assertIn("NOTE", report)

    def test_default_m2_root_is_home_m2(self):
        # The default resolver should point at ~/.m2/repository regardless of cwd.
        resolved = qa_preflight._default_m2_root()
        self.assertTrue(str(resolved).replace("\\", "/").endswith(".m2/repository"))


class TestPreflightNoArtifacts(unittest.TestCase):
    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.repo = self.repo_helper.layout_repo()
        self.m2 = self.repo_helper.layout_m2()

    def test_handles_missing_repo_layout(self):
        empty = self.repo_helper.root / "empty"
        empty.mkdir()
        rows = qa_preflight.run_preflight(empty, self.m2)
        # No m2 jar → no-op, not stale
        self.assertFalse(qa_preflight.is_stale(rows))

    def test_handles_missing_m2_root(self):
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar")
        os.utime(war_path, (250, 250))
        rows = qa_preflight.run_preflight(self.repo, self.repo_helper.root / "no-m2")
        # m2 missing entirely → no-op
        self.assertFalse(qa_preflight.is_stale(rows))


class TestWarBundlesSitemanage(unittest.TestCase):
    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.repo = self.repo_helper.layout_repo()

    def test_war_bundles_sitemanage_true(self):
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-8.2.0-SNAPSHOT.jar")
        self.assertTrue(qa_preflight.war_bundles_sitemanage(war_path))

    def test_war_bundles_sitemanage_false_when_only_other_jars(self):
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        with zipfile.ZipFile(war_path, "w") as zf:
            zf.writestr("WEB-INF/lib/some-other.jar", b"PK fake")
        self.assertFalse(qa_preflight.war_bundles_sitemanage(war_path))

    def test_war_bundles_sitemanage_false_for_missing_war(self):
        self.assertFalse(
            qa_preflight.war_bundles_sitemanage(self.repo / "WebUI" / "target" / "missing.war")
        )


if __name__ == "__main__":
    unittest.main()
