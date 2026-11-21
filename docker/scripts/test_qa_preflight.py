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
from unittest import mock
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
        # With no Maven override anywhere, the resolver falls back to ~/.m2/repository.
        with tempfile.TemporaryDirectory() as td, mock.patch.dict(
            os.environ,
            {"HOME": td, "USERPROFILE": td, "MAVEN_SKIP_RC": "1"},
        ):
            os.environ.pop("MAVEN_ARGS", None)
            os.environ.pop("MAVEN_OPTS", None)
            resolved = qa_preflight._default_m2_root(Path(td))
            self.assertEqual(resolved, Path(td) / ".m2" / "repository")


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


def _write_class_jar(path: Path, entries: list[str], mtime: float = 300) -> None:
    """Write a synthetic jar whose entries are (empty) .class files."""
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w") as zf:
        for entry in entries:
            zf.writestr(entry, b"\xca\xfe\xba\xbe fake-class")
    os.utime(path, (mtime, mtime))


def _write_java(path: Path, body: str = "package p; public class X {}") -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(body, encoding="utf-8")


class TestFindSitemanageInM2(unittest.TestCase):
    """Standard single-level m2 layout lookup (#6740).

    For groupId ``com.percussion`` Maven writes jars to
    ``com/percussion/sitemanage/<version>/`` directly — the fast path must
    scan those version dirs (not a doubled ``.../sitemanage/sitemanage/``
    level, which Maven never writes).
    """

    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.m2 = self.repo_helper.layout_m2()

    def _artifact_dir(self) -> Path:
        return self.m2 / "com" / "percussion" / "sitemanage"

    def test_newest_jar_across_version_dirs_wins(self):
        old = self._artifact_dir() / "8.1.8-SNAPSHOT" / "sitemanage-8.1.8-SNAPSHOT.jar"
        new = self._artifact_dir() / "8.2.0-SNAPSHOT" / "sitemanage-8.2.0-SNAPSHOT.jar"
        _write_class_jar(old, ["com/percussion/Old.class"], mtime=100)
        _write_class_jar(new, ["com/percussion/New.class"], mtime=300)
        self.assertEqual(qa_preflight.find_sitemanage_in_m2(self.m2), new)

    def test_sources_and_javadoc_jars_ignored(self):
        version = self._artifact_dir() / "8.2.0-SNAPSHOT"
        _write_class_jar(
            version / "sitemanage-8.2.0-SNAPSHOT-sources.jar",
            ["com/percussion/Src.class"],
            mtime=400,
        )
        binary = version / "sitemanage-8.2.0-SNAPSHOT.jar"
        _write_class_jar(binary, ["com/percussion/Bin.class"], mtime=100)
        self.assertEqual(qa_preflight.find_sitemanage_in_m2(self.m2), binary)

    def test_missing_layout_returns_none(self):
        self.assertIsNone(qa_preflight.find_sitemanage_in_m2(self.m2))


class TestSnapshotProvenance(unittest.TestCase):
    """SNAPSHOT provenance guard: branch-built jars in the shared repo (#6709)."""

    def setUp(self):
        self.repo_helper = _Repo()
        self.addCleanup(self.repo_helper.cleanup)
        self.repo = self.repo_helper.layout_repo()
        self.m2 = self.repo_helper.layout_m2()

    def _layout_rest_sources(self) -> Path:
        src = self.repo / "rest" / "src" / "main" / "java"
        _write_java(src / "com" / "percussion" / "rest" / "PipesResource.java")
        return src

    def _m2_rest_jar(self, entries: list[str], *rel: str) -> Path:
        jar = self.m2.joinpath(*rel)
        _write_class_jar(jar, entries)
        return jar

    def test_clean_jar_passes(self):
        self._layout_rest_sources()
        self._m2_rest_jar(
            ["com/percussion/rest/PipesResource.class"],
            "com", "percussion", "rest", "8.2.0-SNAPSHOT",
            "rest-8.2.0-SNAPSHOT.jar",
        )
        findings = qa_preflight.check_snapshot_provenance(self.repo, self.m2)
        self.assertFalse(qa_preflight.is_poisoned(findings))

    def test_single_level_standard_layout_jar_is_checked(self):
        """Single-level m2 layout is the standard Maven layout (#6740).

        For groupId ``com.percussion`` Maven writes
        ``com/percussion/rest/<version>/`` directly (groupPath/artifactId/
        version) — the ``.../rest/rest/...`` doubling assumed in #6738 would
        only be standard for a ``com.percussion.rest`` groupId, which no pom
        declares. A foreign class in the standard-layout jar must flag.
        """
        self._layout_rest_sources()
        self._m2_rest_jar(
            ["com/percussion/rest/pipelines/PipelineMapper.class"],
            "com", "percussion", "rest", "8.2.0-SNAPSHOT",
            "rest-8.2.0-SNAPSHOT.jar",
        )
        findings = qa_preflight.check_snapshot_provenance(self.repo, self.m2)
        self.assertTrue(qa_preflight.is_poisoned(findings))
        self.assertEqual(findings[0].label, "rest")
        self.assertIn(
            "com/percussion/rest/pipelines/PipelineMapper.class",
            findings[0].foreign_sample,
        )

    def test_inner_class_maps_to_outer_source(self):
        self._layout_rest_sources()
        self._m2_rest_jar(
            ["com/percussion/rest/PipesResource$Inner.class"],
            "com", "percussion", "rest", "8.2.0-SNAPSHOT",
            "rest-8.2.0-SNAPSHOT.jar",
        )
        findings = qa_preflight.check_snapshot_provenance(self.repo, self.m2)
        self.assertFalse(qa_preflight.is_poisoned(findings))

    def test_multi_top_level_types_in_one_file_pass(self):
        """Sibling top-level types map to their declaring file (#6829).

        Mirrors ``AssetAdaptor.java`` declaring package-private
        ``PSStreamingOutput``: a clean-checkout rebuild emits both
        classes, and neither is foreign.
        """
        src = self.repo / "rest" / "src" / "main" / "java"
        src_pkg = src / "com" / "percussion" / "rest"
        _write_java(
            src_pkg / "PipesResource.java",
            "package com.percussion.rest;\n"
            "public class PipesResource {}\n"
            "class PipesHelper implements Runnable {\n"
            "    class Inner {}\n"
            "}\n",
        )
        self._m2_rest_jar(
            [
                "com/percussion/rest/PipesResource.class",
                "com/percussion/rest/PipesHelper.class",
                "com/percussion/rest/PipesHelper$Inner.class",
            ],
            "com", "percussion", "rest", "8.2.0-SNAPSHOT",
            "rest-8.2.0-SNAPSHOT.jar",
        )
        findings = qa_preflight.check_snapshot_provenance(self.repo, self.m2)
        self.assertFalse(qa_preflight.is_poisoned(findings))

    def test_genuinely_foreign_class_in_same_package_still_flags(self):
        """An undeclared same-package class is still foreign (#6829).

        The sibling-type mapping must not degenerate into "any class in
        a known package is fine" — a different branch's jar keeps
        flagging.
        """
        src = self.repo / "rest" / "src" / "main" / "java"
        src_pkg = src / "com" / "percussion" / "rest"
        _write_java(
            src_pkg / "PipesResource.java",
            "package com.percussion.rest;\n"
            "public class PipesResource {}\n"
            "class PipesHelper {}\n",
        )
        self._m2_rest_jar(
            ["com/percussion/rest/PipesBranchOnly.class"],
            "com", "percussion", "rest", "8.2.0-SNAPSHOT",
            "rest-8.2.0-SNAPSHOT.jar",
        )
        findings = qa_preflight.check_snapshot_provenance(self.repo, self.m2)
        self.assertTrue(qa_preflight.is_poisoned(findings))
        self.assertIn(
            "com/percussion/rest/PipesBranchOnly.class",
            findings[0].foreign_sample,
        )

    def test_comment_and_string_noise_does_not_declare_types(self):
        """Fake declarations in comments/strings declare nothing (#6829)."""
        src = self.repo / "rest" / "src" / "main" / "java"
        src_pkg = src / "com" / "percussion" / "rest"
        _write_java(
            src_pkg / "PipesResource.java",
            "package com.percussion.rest;\n"
            "// class CommentFake {}\n"
            "/* class BlockFake {} */\n"
            'String s = "class StringFake {}";\n'
            "public class PipesResource {}\n",
        )
        self._m2_rest_jar(
            [
                "com/percussion/rest/PipesResource.class",
                "com/percussion/rest/CommentFake.class",
            ],
            "com", "percussion", "rest", "8.2.0-SNAPSHOT",
            "rest-8.2.0-SNAPSHOT.jar",
        )
        findings = qa_preflight.check_snapshot_provenance(self.repo, self.m2)
        self.assertTrue(qa_preflight.is_poisoned(findings))
        self.assertIn(
            "com/percussion/rest/CommentFake.class",
            findings[0].foreign_sample,
        )

    def test_generated_sources_count_as_source(self):
        src = self._layout_rest_sources()
        gen = self.repo / "rest" / "target" / "generated-sources" / "annotations"
        _write_java(gen / "com" / "percussion" / "rest" / "PipesMapperImpl.java")
        self._m2_rest_jar(
            [
                "com/percussion/rest/PipesResource.class",
                "com/percussion/rest/PipesMapperImpl.class",
            ],
            "com", "percussion", "rest", "8.2.0-SNAPSHOT",
            "rest-8.2.0-SNAPSHOT.jar",
        )
        findings = qa_preflight.check_snapshot_provenance(self.repo, self.m2)
        self.assertFalse(qa_preflight.is_poisoned(findings))

    def test_missing_jar_is_noop(self):
        self._layout_rest_sources()
        findings = qa_preflight.check_snapshot_provenance(self.repo, self.m2)
        self.assertFalse(qa_preflight.is_poisoned(findings))

    def test_missing_source_root_is_skipped(self):
        """Partial trees (no rest/ dir) never flag — avoid false positives."""
        self._m2_rest_jar(
            ["com/percussion/rest/pipelines/PipelineMapper.class"],
            "com", "percussion", "rest", "8.2.0-SNAPSHOT",
            "rest-8.2.0-SNAPSHOT.jar",
        )
        findings = qa_preflight.check_snapshot_provenance(self.repo, self.m2)
        self.assertFalse(qa_preflight.is_poisoned(findings))

    def test_scratch_and_metadata_entries_ignored(self):
        self._layout_rest_sources()
        self._m2_rest_jar(
            [
                "META-INF/MANIFEST.MF",
                "module-info.class",
                "com/percussion/rest/package-info.class",
                "com/percussion/rest/PipesResource.class",
            ],
            "com", "percussion", "rest", "8.2.0-SNAPSHOT",
            "rest-8.2.0-SNAPSHOT.jar",
        )
        findings = qa_preflight.check_snapshot_provenance(self.repo, self.m2)
        self.assertFalse(qa_preflight.is_poisoned(findings))

    def test_strict_main_exits_2_on_poison(self):
        self._layout_rest_sources()
        self._m2_rest_jar(
            ["com/percussion/rest/pipelines/PipelineMapper.class"],
            "com", "percussion", "rest", "8.2.0-SNAPSHOT",
            "rest-8.2.0-SNAPSHOT.jar",
        )
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_preflight.main(
                ["--repo-root", str(self.repo), "--m2-root", str(self.m2),
                 "--strict", "--no-content-hash"]
            )
        self.assertEqual(rc, 2)
        self.assertIn("POISONED: rest", buf.getvalue())

    def test_no_provenance_flag_disables_check(self):
        self._layout_rest_sources()
        self._m2_rest_jar(
            ["com/percussion/rest/pipelines/PipelineMapper.class"],
            "com", "percussion", "rest", "8.2.0-SNAPSHOT",
            "rest-8.2.0-SNAPSHOT.jar",
        )
        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = qa_preflight.main(
                ["--repo-root", str(self.repo), "--m2-root", str(self.m2),
                 "--strict", "--no-content-hash", "--no-provenance"]
            )
        # No sitemanage jar in fixture → not stale either, so rc is 0
        # and the report carries no provenance section at all.
        self.assertEqual(rc, 0)
        self.assertNotIn("POISONED", buf.getvalue())
        self.assertNotIn("PROVENANCE", buf.getvalue())

    def test_flat_layout_fallback_for_sitemanage_m2_row(self):
        """The stale-WAR m2 row is no longer blind on flattened hosts."""
        jar = self.m2 / "com" / "percussion" / "sitemanage" / "8.2.0-SNAPSHOT" / \
            "sitemanage-8.2.0-SNAPSHOT.jar"
        _write_jar(jar, b"flat-layout-payload", 400)
        self.assertEqual(qa_preflight.find_sitemanage_in_m2(self.m2), jar)


if __name__ == "__main__":
    unittest.main()
