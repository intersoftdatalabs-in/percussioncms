#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for docker/scripts/qa_preflight.py (#2486).

Filesystem-only: synthetic repo + m2 layout in a tempdir — no docker,
maven, or curl.
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


def _touch(path: Path, mtime: float, content: bytes = b"x") -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)
    os.utime(path, (mtime, mtime))


def _make_war(
    war_path: Path, *, sitemanage_jar_name: str = "sitemanage-8.2.0-SNAPSHOT.jar"
) -> None:
    war_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(war_path, "w") as zf:
        zf.writestr("WEB-INF/lib/" + sitemanage_jar_name, b"PK fake")


def _m2_sitemanage(m2_root: Path, mtime: float) -> Path:
    """Standard Maven layout: com/percussion/sitemanage/<ver>/sitemanage-<ver>.jar."""
    jar = (
        m2_root
        / "com"
        / "percussion"
        / "sitemanage"
        / "8.2.0-SNAPSHOT"
        / "sitemanage-8.2.0-SNAPSHOT.jar"
    )
    _touch(jar, mtime)
    return jar


class _Repo:
    def __init__(self) -> None:
        self.td = tempfile.TemporaryDirectory()
        self.root = Path(self.td.name)

    def cleanup(self) -> None:
        self.td.cleanup()

    def layout_repo(self) -> Path:
        repo = self.root / "repo"
        (repo / "WebUI" / "target").mkdir(parents=True)
        (repo / "modules" / "perc-distribution-tree" / "target").mkdir(parents=True)
        return repo

    def layout_m2(self) -> Path:
        return self.root / "m2"


class TestFindSitemanageInM2(unittest.TestCase):
    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.m2 = self.repo_helper.layout_m2()

    def test_finds_versioned_layout(self):
        jar = _m2_sitemanage(self.m2, 100)
        found = qa_preflight.find_sitemanage_in_m2(self.m2)
        self.assertEqual(found, jar)

    def test_skips_classifier_jars(self):
        base = self.m2 / "com" / "percussion" / "sitemanage" / "8.2.0-SNAPSHOT"
        _touch(base / "sitemanage-8.2.0-SNAPSHOT-javadoc.jar", 500)
        _touch(base / "sitemanage-8.2.0-SNAPSHOT-sources.jar", 500)
        main = base / "sitemanage-8.2.0-SNAPSHOT.jar"
        _touch(main, 100)
        found = qa_preflight.find_sitemanage_in_m2(self.m2)
        self.assertEqual(found, main)

    def test_picks_newest_when_multiple_versions(self):
        old = (
            self.m2
            / "com"
            / "percussion"
            / "sitemanage"
            / "8.1.0-SNAPSHOT"
            / "sitemanage-8.1.0-SNAPSHOT.jar"
        )
        new = (
            self.m2
            / "com"
            / "percussion"
            / "sitemanage"
            / "8.2.0-SNAPSHOT"
            / "sitemanage-8.2.0-SNAPSHOT.jar"
        )
        _touch(old, 100)
        _touch(new, 300)
        self.assertEqual(qa_preflight.find_sitemanage_in_m2(self.m2), new)


class TestPreflightFresh(unittest.TestCase):
    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.repo = self.repo_helper.layout_repo()
        self.m2 = self.repo_helper.layout_m2()

    def _fresh_chain(self, m2_t=100, war_t=200, dist_t=300):
        _m2_sitemanage(self.m2, m2_t)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path)
        os.utime(war_path, (war_t, war_t))
        dist = (
            self.repo
            / "modules"
            / "perc-distribution-tree"
            / "target"
            / "perc-distribution-tree.jar"
        )
        _touch(dist, dist_t, b"dist")
        return war_path, dist

    def test_fresh_when_chain_ordered(self):
        self._fresh_chain()
        report = qa_preflight.run_preflight(self.repo, self.m2)
        self.assertFalse(report.stale)
        self.assertFalse(report.skipped)
        text = qa_preflight.format_report(report, strict=True)
        self.assertIn("FRESH", text)

    def test_fresh_when_equal_mtimes(self):
        self._fresh_chain(m2_t=200, war_t=200, dist_t=200)
        report = qa_preflight.run_preflight(self.repo, self.m2)
        self.assertFalse(report.stale)

    def test_strict_main_returns_zero_when_fresh(self):
        self._fresh_chain()
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_preflight.main(
                [
                    "--repo-root",
                    str(self.repo),
                    "--m2-root",
                    str(self.m2),
                    "--strict",
                ]
            )
        self.assertEqual(rc, 0)
        self.assertIn("FRESH", buf.getvalue())


class TestPreflightStale(unittest.TestCase):
    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.repo = self.repo_helper.layout_repo()
        self.m2 = self.repo_helper.layout_m2()

    def test_stale_when_war_built_before_m2(self):
        _m2_sitemanage(self.m2, 300)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path)
        os.utime(war_path, (250, 250))
        dist = (
            self.repo
            / "modules"
            / "perc-distribution-tree"
            / "target"
            / "perc-distribution-tree.jar"
        )
        _touch(dist, 260, b"dist")

        report = qa_preflight.run_preflight(self.repo, self.m2)
        self.assertTrue(report.stale)
        text = qa_preflight.format_report(report, strict=True)
        self.assertIn("STALE", text)
        self.assertTrue(any("WebUI WAR older" in r for r in report.reasons))

    def test_stale_when_dist_older_than_war(self):
        _m2_sitemanage(self.m2, 100)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path)
        os.utime(war_path, (300, 300))
        dist = (
            self.repo
            / "modules"
            / "perc-distribution-tree"
            / "target"
            / "perc-distribution-tree.jar"
        )
        _touch(dist, 200, b"dist")

        report = qa_preflight.run_preflight(self.repo, self.m2)
        self.assertTrue(report.stale)
        self.assertTrue(any("dist jar older" in r for r in report.reasons))

    def test_strict_returns_nonzero_when_stale(self):
        _m2_sitemanage(self.m2, 300)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path)
        os.utime(war_path, (250, 250))
        _touch(
            self.repo
            / "modules"
            / "perc-distribution-tree"
            / "target"
            / "perc-distribution-tree.jar",
            260,
            b"dist",
        )

        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_preflight.main(
                [
                    "--repo-root",
                    str(self.repo),
                    "--m2-root",
                    str(self.m2),
                    "--strict",
                ]
            )
        self.assertEqual(rc, qa_preflight.EXIT_STALE)
        self.assertIn("STALE", buf.getvalue())
        self.assertIn("HINT:", buf.getvalue())

    def test_no_strict_returns_zero_when_stale(self):
        _m2_sitemanage(self.m2, 300)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path)
        os.utime(war_path, (250, 250))
        _touch(
            self.repo
            / "modules"
            / "perc-distribution-tree"
            / "target"
            / "perc-distribution-tree.jar",
            260,
            b"dist",
        )

        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_preflight.main(
                [
                    "--repo-root",
                    str(self.repo),
                    "--m2-root",
                    str(self.m2),
                    "--no-strict",
                ]
            )
        self.assertEqual(rc, 0)
        self.assertIn("STALE", buf.getvalue())
        self.assertIn("non-strict", buf.getvalue())

    def test_stale_when_war_missing(self):
        _m2_sitemanage(self.m2, 300)
        _touch(
            self.repo
            / "modules"
            / "perc-distribution-tree"
            / "target"
            / "perc-distribution-tree.jar",
            100,
            b"dist",
        )
        report = qa_preflight.run_preflight(self.repo, self.m2)
        self.assertTrue(report.stale)
        self.assertTrue(any("WAR missing" in r for r in report.reasons))

    def test_stale_when_dist_missing(self):
        _m2_sitemanage(self.m2, 100)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path)
        os.utime(war_path, (200, 200))
        report = qa_preflight.run_preflight(self.repo, self.m2)
        self.assertTrue(report.stale)
        self.assertTrue(any("dist jar missing" in r for r in report.reasons))

    def test_stale_when_war_does_not_bundle_sitemanage(self):
        _m2_sitemanage(self.m2, 300)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        with zipfile.ZipFile(war_path, "w") as zf:
            zf.writestr("WEB-INF/lib/some-other.jar", b"PK fake")
        os.utime(war_path, (400, 400))
        _touch(
            self.repo
            / "modules"
            / "perc-distribution-tree"
            / "target"
            / "perc-distribution-tree.jar",
            500,
            b"dist",
        )
        report = qa_preflight.run_preflight(self.repo, self.m2)
        self.assertTrue(report.stale)
        self.assertTrue(any("does not bundle" in r for r in report.reasons))


class TestPreflightNoOp(unittest.TestCase):
    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.repo = self.repo_helper.layout_repo()
        self.m2 = self.repo_helper.layout_m2()

    def test_no_m2_jar_means_noop(self):
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path)
        os.utime(war_path, (250, 250))
        report = qa_preflight.run_preflight(self.repo, self.m2)
        self.assertFalse(report.stale)
        self.assertTrue(report.skipped)
        text = qa_preflight.format_report(report, strict=True)
        self.assertIn("NOTE", text)

    def test_default_m2_root_is_home_m2(self):
        resolved = qa_preflight._default_m2_root()
        self.assertTrue(str(resolved).replace("\\", "/").endswith(".m2/repository"))

    def test_handles_missing_repo_layout(self):
        empty = self.repo_helper.root / "empty"
        empty.mkdir()
        report = qa_preflight.run_preflight(empty, self.m2)
        self.assertFalse(report.stale)
        self.assertTrue(report.skipped)

    def test_handles_missing_m2_root(self):
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path)
        report = qa_preflight.run_preflight(self.repo, self.repo_helper.root / "no-m2")
        self.assertFalse(report.stale)
        self.assertTrue(report.skipped)


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
            qa_preflight.war_bundles_sitemanage(
                self.repo / "WebUI" / "target" / "missing.war"
            )
        )


if __name__ == "__main__":
    unittest.main()
