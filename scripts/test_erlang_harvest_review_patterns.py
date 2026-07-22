#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for erlang-harvest-review-patterns.py (no network)."""

from __future__ import annotations

import importlib.util
import json
import os
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "erlang-harvest-review-patterns.py"
    name = "erlang_harvest_review_patterns"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    # dataclasses need the module registered before exec
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


harvest = _load()


class TestGeneralize(unittest.TestCase):
    def test_strips_severity_and_paths(self):
        sev, rest = harvest.extract_severity(
            "**WARNING:** Path join in `modules/foo/Bar.java:42` uses hardcoded `/`\n\nMore detail."
        )
        self.assertEqual(sev, "warning")
        g = harvest.generalize_body(rest)
        self.assertNotIn("Bar.java", g)
        self.assertNotIn(":42", g)
        self.assertIn("hardcoded", g.lower())

    def test_skips_mitigation(self):
        self.assertTrue(
            harvest.should_skip_body("**Mitigation (commit abc):** fixed it")
        )
        self.assertFalse(
            harvest.should_skip_body("**WARNING:** real finding about tests")
        )

    def test_critical_alias(self):
        sev, _ = harvest.extract_severity(
            "**CRITICAL:** Duplicate method declaration prevents compilation"
        )
        self.assertEqual(sev, "critical")


class TestClusterAndPromote(unittest.TestCase):
    def test_cluster_merges_similar(self):
        comments = [
            harvest.RawComment(
                1,
                "kilo-code-bot[bot]",
                "**WARNING:** Missing unit test for error path when input is null",
                "a/Foo.java",
                100,
                "",
                "http://example/1",
                None,
            ),
            harvest.RawComment(
                2,
                "kilo-code-bot[bot]",
                "**WARNING:** Missing unit tests for error path when input is null",
                "b/Bar.java",
                101,
                "",
                "http://example/2",
                None,
            ),
            harvest.RawComment(
                3,
                "kilo-code-bot[bot]",
                "**SUGGESTION:** Typo in validation message wording",
                "c/Baz.java",
                102,
                "",
                "http://example/3",
                None,
            ),
        ]
        clusters = harvest.cluster_comments(comments)
        self.assertGreaterEqual(len(clusters), 1)
        top = clusters[0]
        self.assertGreaterEqual(top.count, 2)
        self.assertGreaterEqual(len(top.prs), 2)

    def test_select_for_apply_respects_min_and_dedup(self):
        c1 = harvest.Cluster(
            key="a",
            principle="Missing unit tests for error and edge case paths",
            category="Tests",
            count=3,
        )
        c1.prs = {1, 2, 3}
        c1.severity_counts["warning"] = 3
        c2 = harvest.Cluster(
            key="b",
            principle="Missing unit tests for error and edge case paths",
            category="Tests",
            count=2,
        )
        c2.prs = {4}
        c2.severity_counts["warning"] = 2
        c3 = harvest.Cluster(
            key="c",
            principle="Unrelated style nit about spacing",
            category="Maintainability",
            count=1,
        )
        c3.prs = {5}
        c3.severity_counts["nit"] = 1

        chosen = harvest.select_for_apply(
            [c1, c2, c3],
            min_count=2,
            min_prs=2,
            existing=set(),
            max_new=10,
        )
        self.assertEqual(len(chosen), 1)
        self.assertIn("Missing unit tests", chosen[0].principle)

    def test_is_promotable_critical_hard_gate(self):
        cl = harvest.Cluster(
            key="x",
            principle="Duplicate method declaration prevents compilation",
            category="Maintainability",
            count=1,
            hard_gate_hint=True,
        )
        cl.severity_counts["critical"] = 1
        cl.prs = {9}
        self.assertTrue(
            harvest.is_promotable(
                cl, min_count=2, min_prs=2, promote_critical=True
            )
        )
        self.assertFalse(
            harvest.is_promotable(
                cl, min_count=2, min_prs=2, promote_critical=False
            )
        )

    def test_is_promotable_rejects_single_pr_without_multi(self):
        cl = harvest.Cluster(
            key="y",
            principle="Copyright year regression from 2025 to 2023",
            category="Maintainability",
            count=3,
            hard_gate_hint=False,
        )
        cl.severity_counts["suggestion"] = 3
        cl.prs = {1}
        self.assertFalse(harvest.is_promotable(cl, min_count=2, min_prs=2))
        crit = harvest.Cluster(
            key="z",
            principle="Copyright year regression is wrong",
            category="Maintainability",
            count=1,
            hard_gate_hint=True,
        )
        crit.severity_counts["critical"] = 1
        crit.prs = {2}
        self.assertFalse(
            harvest.is_promotable(
                crit, min_count=2, min_prs=2, promote_critical=True
            )
        )


class TestMergePatterns(unittest.TestCase):
    def test_merge_inserts_under_category(self):
        sample = """# Erlang review patterns (Percussion CMS)

## Hard gates (always scan)

- Existing hard gate

## Recurring findings

### Tests

- Happy-path-only coverage for validation paths

### Maintainability

- Orphaned javadoc left above renamed methods

## False-positive guards (do not flag)

- URL paths that correctly use `/`
"""
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "patterns.md"
            path.write_text(sample, encoding="utf-8", newline="\n")
            cl = harvest.Cluster(
                key="t",
                principle="Vacuous assertions that any exception would pass",
                category="Tests",
                count=4,
            )
            cl.prs = {1, 2}
            text, n = harvest.merge_into_patterns_md(path, [cl], dry_run=False)
            self.assertEqual(n, 1)
            body = path.read_text(encoding="utf-8")
            self.assertIn("Vacuous assertions", body)
            self.assertIn("_(harvested, seen 4×", body)
            self.assertIn("Existing hard gate", text)


class TestParseComments(unittest.TestCase):
    def test_filters_replies_and_authors(self):
        raw = [
            {
                "id": 1,
                "user": {"login": "kilo-code-bot[bot]"},
                "body": "**WARNING:** Something important about paths",
                "path": "a.java",
                "pull_request_url": "https://api.github.com/repos/o/r/pulls/12",
                "created_at": "2026-01-01T00:00:00Z",
                "html_url": "https://github.com/o/r/pull/12#x",
                "in_reply_to_id": None,
            },
            {
                "id": 2,
                "user": {"login": "kilo-code-bot[bot]"},
                "body": "**Mitigation:** fixed",
                "path": "a.java",
                "pull_request_url": "https://api.github.com/repos/o/r/pulls/12",
                "created_at": "2026-01-01T00:00:00Z",
                "html_url": "https://github.com/o/r/pull/12#y",
                "in_reply_to_id": 1,
            },
            {
                "id": 3,
                "user": {"login": "human"},
                "body": "**WARNING:** human only",
                "path": "b.java",
                "pull_request_url": "https://api.github.com/repos/o/r/pulls/13",
                "created_at": "2026-01-01T00:00:00Z",
                "html_url": "https://github.com/o/r/pull/13#z",
                "in_reply_to_id": None,
            },
        ]
        out = harvest.parse_comments(raw, {"kilo-code-bot[bot]"})
        self.assertEqual(len(out), 1)
        self.assertEqual(out[0].pr_number, 12)


class TestFixtureEndToEnd(unittest.TestCase):
    def test_main_fixture_write_report(self):
        fixture = [
            {
                "id": 10,
                "user": {"login": "kilo-code-bot[bot]"},
                "body": "**WARNING:** Missing behavioral unit test for failure path",
                "path": "src/A.java",
                "pull_request_url": "https://api.github.com/repos/o/r/pulls/1",
                "created_at": "2026-01-01T00:00:00Z",
                "html_url": "https://github.com/o/r/pull/1#d1",
                "in_reply_to_id": None,
            },
            {
                "id": 11,
                "user": {"login": "kilo-code-bot[bot]"},
                "body": "**WARNING:** Missing behavioral unit tests for failure paths",
                "path": "src/B.java",
                "pull_request_url": "https://api.github.com/repos/o/r/pulls/2",
                "created_at": "2026-01-01T00:00:00Z",
                "html_url": "https://github.com/o/r/pull/2#d2",
                "in_reply_to_id": None,
            },
        ]
        with tempfile.TemporaryDirectory() as td:
            td_path = Path(td)
            (td_path / "AGENTS.md").write_text("# t\n", encoding="utf-8")
            (td_path / ".git").mkdir()
            patterns = (
                td_path
                / "modules"
                / "ai-shared-develop"
                / "src"
                / "main"
                / "resources"
                / "skills"
                / "erlang-review"
                / "patterns.md"
            )
            patterns.parent.mkdir(parents=True)
            patterns.write_text(
                "# Erlang review patterns\n\n"
                "## Hard gates (always scan)\n\n- Gate one\n\n"
                "## Recurring findings\n\n"
                "### Tests\n\n- Old test pattern\n\n"
                "## False-positive guards (do not flag)\n\n- Guard\n",
                encoding="utf-8",
                newline="\n",
            )
            fix = td_path / "fixture.json"
            fix.write_text(json.dumps(fixture), encoding="utf-8")
            out = td_path / "docs" / "ai-generated" / "code-reviews" / "out.md"
            out.parent.mkdir(parents=True)

            old = Path.cwd()
            try:
                os.chdir(td_path)
                rc = harvest.main(
                    [
                        "--fixture",
                        str(fix),
                        "--repo",
                        "o/r",
                        "--output",
                        str(out),
                        "--apply",
                        "--min-count",
                        "2",
                    ]
                )
            finally:
                os.chdir(old)
            self.assertEqual(rc, 0)
            self.assertTrue(out.is_file())
            body = patterns.read_text(encoding="utf-8")
            self.assertIn("harvested", body)
            self.assertIn("Missing behavioral", body)

    def test_report_does_not_reference_removed_wrappers(self):
        """Spec 994 US3: the candidate report must not point readers at
        scripts/erlang-harvest-review-patterns.{sh,bat}, since those wrappers
        were deleted. The Python script itself is the only entry point."""
        with tempfile.TemporaryDirectory() as td:
            td_path = Path(td)
            (td_path / "AGENTS.md").write_text("# t\n", encoding="utf-8")
            (td_path / ".git").mkdir()
            patterns = (
                td_path
                / "modules"
                / "ai-shared-develop"
                / "src"
                / "main"
                / "resources"
                / "skills"
                / "erlang-review"
                / "patterns.md"
            )
            patterns.parent.mkdir(parents=True)
            patterns.write_text(
                "# Erlang review patterns\n\n## Hard gates\n\n- Gate\n",
                encoding="utf-8",
                newline="\n",
            )
            fix = td_path / "fixture.json"
            fix.write_text("[]", encoding="utf-8")
            out = td_path / "docs" / "ai-generated" / "code-reviews" / "out.md"
            out.parent.mkdir(parents=True)

            old = Path.cwd()
            try:
                os.chdir(td_path)
                rc = harvest.main(
                    [
                        "--fixture",
                        str(fix),
                        "--repo",
                        "o/r",
                        "--output",
                        str(out),
                    ]
                )
            finally:
                os.chdir(old)
            self.assertEqual(rc, 0)
            self.assertTrue(out.is_file())
            report = out.read_text(encoding="utf-8")
            self.assertNotIn(
                "erlang-harvest-review-patterns.bat", report,
                msg="candidate report must not reference deleted .bat wrapper",
            )
            self.assertNotIn(
                "erlang-harvest-review-patterns.sh", report,
                msg="candidate report must not reference deleted .sh wrapper",
            )


if __name__ == "__main__":
    unittest.main()
