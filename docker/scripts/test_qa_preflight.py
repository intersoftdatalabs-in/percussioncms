#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for docker/scripts/qa_preflight.py (#2486 / #2532).

Filesystem-only: builds a synthetic repo + m2 layout in a tempdir and
exercises the preflight without docker, maven, or curl. Includes
SHA-256 content-hash cases with synthetic WAR/m2 bytes (#2532).
"""

from __future__ import annotations

import hashlib
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

# Shared synthetic jar payloads for content-hash tests.
_BYTES_A = b"sitemanage-jar-payload-A-v1"
_BYTES_B = b"sitemanage-jar-payload-B-v2-different"


def _sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _write_jar(path: Path, data: bytes, mtime: float) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    os.utime(path, (mtime, mtime))


def _touch(path: Path, mtime: float) -> None:
    """Legacy empty-file helper used by mtime-only tests."""
    _write_jar(path, b"", mtime)


def _make_war(
    war_path: Path,
    *,
    sitemanage_jar_name: str = "sitemanage-1.0.0.jar",
    sitemanage_bytes: bytes = b"PK fake",
    mtime: float | None = None,
) -> None:
    war_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(war_path, "w") as zf:
        # ZIP entry paths always use '/' regardless of OS.
        zf.writestr("WEB-INF/lib/" + sitemanage_jar_name, sitemanage_bytes)
    if mtime is not None:
        os.utime(war_path, (mtime, mtime))


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

    def test_fresh_when_war_at_or_after_m2_mtime_only(self):
        # m2 jar older than WAR's mtime → FRESH under mtime-only mode
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", 200)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar", mtime=300)

        rows = qa_preflight.run_preflight(
            self.repo, self.m2, content_hash=False
        )
        self.assertFalse(qa_preflight.is_stale(rows, content_hash=False))
        report = qa_preflight.format_report(rows, strict=True, content_hash=False)
        self.assertIn("FRESH", report)

    def test_fresh_when_war_exactly_at_m2_mtime_only(self):
        same = 200
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", same)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar", mtime=same)

        rows = qa_preflight.run_preflight(
            self.repo, self.m2, content_hash=False
        )
        self.assertFalse(qa_preflight.is_stale(rows, content_hash=False))

    def test_fresh_when_content_hashes_match_despite_mtime_skew(self):
        # WAR mtime older than m2 → would be mtime-STALE, but identical
        # bytes → content-hash FRESH (#2532 mtime-resistant).
        _write_jar(
            self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar",
            _BYTES_A,
            500,
        )
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(
            war_path,
            sitemanage_jar_name="sitemanage-8.2.0-SNAPSHOT.jar",
            sitemanage_bytes=_BYTES_A,
            mtime=100,
        )

        rows = qa_preflight.run_preflight(self.repo, self.m2, content_hash=True)
        self.assertFalse(qa_preflight.is_stale(rows, content_hash=True))
        report = qa_preflight.format_report(rows, strict=True, content_hash=True)
        self.assertIn("FRESH", report)
        self.assertIn("content hashes match", report)


class TestPreflightStale(unittest.TestCase):
    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.repo = self.repo_helper.layout_repo()
        self.m2 = self.repo_helper.layout_m2()

    def test_stale_when_war_built_before_m2_update_mtime_only(self):
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", 300)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar", mtime=250)

        rows = qa_preflight.run_preflight(
            self.repo, self.m2, content_hash=False
        )
        self.assertTrue(qa_preflight.is_stale(rows, content_hash=False))
        report = qa_preflight.format_report(rows, strict=True, content_hash=False)
        self.assertIn("STALE", report)

    def test_strict_returns_nonzero_when_stale(self):
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", 300)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar", mtime=250)

        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_preflight.main([
                "--repo-root", str(self.repo),
                "--m2-root", str(self.m2),
                "--strict",
                "--no-content-hash",
            ])
        self.assertEqual(rc, 2)
        self.assertIn("STALE", buf.getvalue())

    def test_non_strict_returns_zero_when_stale(self):
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", 300)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar", mtime=250)

        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_preflight.main([
                "--repo-root", str(self.repo),
                "--m2-root", str(self.m2),
                "--no-content-hash",
            ])
        self.assertEqual(rc, 0)
        self.assertIn("STALE", buf.getvalue())

    def test_no_strict_flag_accepted(self):
        """perc-devctl passes --no-strict; argparse must accept it."""
        _touch(self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar", 300)
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar", mtime=250)

        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_preflight.main([
                "--repo-root", str(self.repo),
                "--m2-root", str(self.m2),
                "--no-strict",
                "--no-content-hash",
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
        with zipfile.ZipFile(war_path, "w") as zf:
            zf.writestr("WEB-INF/lib/some-other.jar", b"PK fake")
        os.utime(war_path, (250, 250))

        rows = qa_preflight.run_preflight(self.repo, self.m2)
        # War present but doesn't bundle sitemanage → cannot preflight → stale
        self.assertTrue(qa_preflight.is_stale(rows))


class TestContentHash(unittest.TestCase):
    """#2532: SHA-256 m2 jar vs WAR zip entry (mtime-resistant)."""

    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.repo = self.repo_helper.layout_repo()
        self.m2 = self.repo_helper.layout_m2()

    def test_stale_when_hashes_differ_even_if_mtimes_look_fresh(self):
        # WAR mtime newer than m2 → mtime would say FRESH, but different
        # payload → content-hash STALE.
        _write_jar(
            self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar",
            _BYTES_A,
            100,
        )
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(
            war_path,
            sitemanage_jar_name="sitemanage-8.2.0-SNAPSHOT.jar",
            sitemanage_bytes=_BYTES_B,
            mtime=900,
        )

        rows = qa_preflight.run_preflight(self.repo, self.m2, content_hash=True)
        self.assertTrue(qa_preflight.is_stale(rows, content_hash=True))
        self.assertEqual(qa_preflight.content_hash_mismatch(rows), "m2-vs-war")
        report = qa_preflight.format_report(rows, strict=True, content_hash=True)
        self.assertIn("STALE", report)
        self.assertIn("content hash mismatch", report)
        self.assertIn("m2 vs war", report)
        # Mtime-only would incorrectly call this fresh:
        self.assertFalse(qa_preflight.is_stale(rows, content_hash=False))

    def test_hashes_equal_for_matching_synthetic_bytes(self):
        _write_jar(
            self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar",
            _BYTES_A,
            100,
        )
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(
            war_path,
            sitemanage_jar_name="sitemanage-8.2.0-SNAPSHOT.jar",
            sitemanage_bytes=_BYTES_A,
            mtime=100,
        )

        rows = qa_preflight.run_preflight(self.repo, self.m2, content_hash=True)
        by = {r.label: r for r in rows}
        expected = _sha(_BYTES_A)
        self.assertEqual(by["m2:sitemanage"].sha256, expected)
        self.assertEqual(by["war:sitemanage"].sha256, expected)
        self.assertTrue(qa_preflight.content_hashes_agree(rows))
        self.assertIsNone(qa_preflight.content_hash_mismatch(rows))

    def test_sha256_zip_entry_streams_entry_bytes(self):
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(
            war_path,
            sitemanage_jar_name="sitemanage-x.jar",
            sitemanage_bytes=_BYTES_B,
        )
        entry = qa_preflight.find_sitemanage_zip_entry(war_path)
        self.assertIsNotNone(entry)
        self.assertEqual(
            qa_preflight.sha256_zip_entry(war_path, entry),
            _sha(_BYTES_B),
        )
        self.assertEqual(
            qa_preflight.sha256_sitemanage_in_war(war_path),
            _sha(_BYTES_B),
        )

    def test_sha256_file_matches_hashlib(self):
        jar = self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar"
        _write_jar(jar, _BYTES_A, 50)
        self.assertEqual(qa_preflight.sha256_file(jar), _sha(_BYTES_A))
        self.assertIsNone(qa_preflight.sha256_file(None))
        self.assertIsNone(qa_preflight.sha256_file(self.m2 / "missing.jar"))

    def test_strict_content_hash_mismatch_exit_2(self):
        _write_jar(
            self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar",
            _BYTES_A,
            100,
        )
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(
            war_path,
            sitemanage_jar_name="sitemanage-8.2.0-SNAPSHOT.jar",
            sitemanage_bytes=_BYTES_B,
            mtime=900,
        )

        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_preflight.main([
                "--repo-root", str(self.repo),
                "--m2-root", str(self.m2),
                "--strict",
                "--content-hash",
            ])
        self.assertEqual(rc, 2)
        out = buf.getvalue()
        self.assertIn("STALE", out)
        self.assertIn("content hash mismatch", out)

    def test_optional_dist_hash_mismatch(self):
        _write_jar(
            self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar",
            _BYTES_A,
            100,
        )
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(
            war_path,
            sitemanage_jar_name="sitemanage-8.2.0-SNAPSHOT.jar",
            sitemanage_bytes=_BYTES_A,
            mtime=100,
        )
        # Loose dist jar with different content under perc-distribution-tree.
        dist_jar = (
            self.repo
            / "modules"
            / "perc-distribution-tree"
            / "target"
            / "Rhythmyx"
            / "WEB-INF"
            / "lib"
            / "sitemanage-8.2.0-SNAPSHOT.jar"
        )
        _write_jar(dist_jar, _BYTES_B, 100)

        rows = qa_preflight.run_preflight(self.repo, self.m2, content_hash=True)
        labels = {r.label for r in rows}
        self.assertIn("dist:sitemanage", labels)
        self.assertEqual(qa_preflight.content_hash_mismatch(rows), "m2-vs-dist")
        self.assertTrue(qa_preflight.is_stale(rows, content_hash=True))
        report = qa_preflight.format_report(rows, strict=True, content_hash=True)
        self.assertIn("m2 vs dist", report)

    def test_content_hash_default_on(self):
        _write_jar(
            self.m2 / qa_preflight._M2_DIR / "sitemanage-8.2.0-SNAPSHOT.jar",
            _BYTES_A,
            100,
        )
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(
            war_path,
            sitemanage_jar_name="sitemanage-8.2.0-SNAPSHOT.jar",
            sitemanage_bytes=_BYTES_B,
            mtime=900,
        )
        # Default (no flags) uses content-hash.
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_preflight.main([
                "--repo-root", str(self.repo),
                "--m2-root", str(self.m2),
                "--strict",
            ])
        self.assertEqual(rc, 2)
        self.assertIn("content hash mismatch", buf.getvalue())


class TestPreflightNoOp(unittest.TestCase):
    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.repo = self.repo_helper.layout_repo()
        self.m2 = self.repo_helper.layout_m2()

    def test_no_m2_jar_means_noop(self):
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar", mtime=250)

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
        _make_war(war_path, sitemanage_jar_name="sitemanage-1.0.0.jar", mtime=250)
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
            qa_preflight.war_bundles_sitemanage(
                self.repo / "WebUI" / "target" / "missing.war"
            )
        )

    def test_find_sitemanage_zip_entry_prefers_web_inf_lib(self):
        war_path = self.repo / "WebUI" / "target" / "perc-web-ui-8.2.0-SNAPSHOT.war"
        with zipfile.ZipFile(war_path, "w") as zf:
            zf.writestr("other/sitemanage-old.jar", b"old")
            zf.writestr("WEB-INF/lib/sitemanage-new.jar", b"new")
        entry = qa_preflight.find_sitemanage_zip_entry(war_path)
        self.assertEqual(entry, "WEB-INF/lib/sitemanage-new.jar")


if __name__ == "__main__":
    unittest.main()
