#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for qa_publish_fixture.py (no docker, no network).

The live ``docker cp`` + HTTP verify path is exercised by
``perc-devctl qa-publish-fixture`` (dry-run argv capture in
``test_perc_devctl.py``); this suite covers the pure staging logic.
"""

from __future__ import annotations

import importlib.util
import io
import posixpath
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from tempfile import TemporaryDirectory

SCRIPTS = Path(__file__).resolve().parent


def _load(name: str, filename: str):
    path = SCRIPTS / filename
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    import sys

    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


qpf = _load("qa_publish_fixture", "qa_publish_fixture.py")


def _fake_cms_install(root: Path, *, with_dynatree_leak: bool = False) -> Path:
    """Build a synthetic <cms-install>/ with a web_resources publish tree."""
    src = root / "web_resources"
    for asset in qpf.REQUIRED_PUBLISHED_ASSETS:
        # Assets are context-relative ("web_resources/..."); the publish
        # tree root already IS web_resources, so strip the first segment.
        rel = Path(*asset.split("/")).relative_to(qpf.CMS_WEB_RESOURCES_DIR)
        target = src / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(f"/* fixture bytes for {asset} */", encoding="utf-8")
    if with_dynatree_leak:
        rel = Path(*qpf.REMOVED_PUBLISHED_ASSETS[0].split("/")).relative_to(
            qpf.CMS_WEB_RESOURCES_DIR
        )
        leaked = src / rel
        leaked.parent.mkdir(parents=True, exist_ok=True)
        leaked.write_text("/* retired copy must not ship */", encoding="utf-8")
    return root


class FixtureContractTest(unittest.TestCase):
    def test_site_base_is_dts_origin_without_port(self):
        # Site-root semantics: the fixture IS the cell origin (the smoke
        # spec asserts assets at <origin>/web_resources/...).
        self.assertEqual(
            qpf.site_base_for("http://127.0.0.1:9983/"),
            "http://127.0.0.1:9983",
        )

    def test_category_url_points_at_fixture_page(self):
        self.assertEqual(
            qpf.category_url_for("http://127.0.0.1:9983"),
            "http://127.0.0.1:9983/category.html",
        )

    def test_expected_assets_cover_both_spec_lists(self):
        urls = qpf.expected_asset_urls("http://127.0.0.1:9983")
        self.assertIn(
            "http://127.0.0.1:9983"
            "/web_resources/cm/jslib/jquery.fancytree.js",
            urls,
        )
        self.assertIn(
            "http://127.0.0.1:9983"
            "/web_resources/cm/css/fancytree/skin-win8/loading.gif",
            urls,
        )

    def test_removed_assets_are_dynatree_only(self):
        urls = qpf.removed_asset_urls("http://127.0.0.1:9983")
        self.assertTrue(all("dynatree" in url for url in urls))
        self.assertTrue(urls)

    def test_home_html_has_no_hardcoded_origin(self):
        html = qpf.fixture_home_html()
        self.assertNotIn("http://", html)
        self.assertNotIn("127.0.0.1", html)
        self.assertIn("web_resources/cm/jslib/jquery.fancytree.js", html)
        self.assertIn("category.html", html)

    def test_category_html_mirrors_delivery_pattern(self):
        html = qpf.fixture_category_html()
        self.assertIn("perc-category-list-collapsible", html)
        self.assertIn(".fancytree(", html)
        self.assertIn("minExpandLevel", html)
        # Nested categories so the expand spec finds a collapsed parent.
        self.assertIn("<ul>", html)
        self.assertGreater(html.count("<li>"), 2)
        self.assertNotIn("http://", html)


class BuildFixtureTreeTest(unittest.TestCase):
    def setUp(self):
        self.td = TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.root = Path(self.td.name)

    def test_build_copies_assets_and_writes_pages(self):
        cms = _fake_cms_install(self.root / "cms")
        staging = self.root / "staging"
        written = qpf.build_fixture_tree(staging, cms)
        self.assertIn(qpf.FIXTURE_HOME_PAGE, written)
        self.assertIn(qpf.FIXTURE_CATEGORY_PAGE, written)
        for asset in qpf.REQUIRED_PUBLISHED_ASSETS:
            self.assertTrue(
                (staging / Path(*asset.split("/"))).is_file(), asset
            )
        self.assertEqual(qpf.verify_fixture_tree(staging), [])

    def test_missing_publish_tree_raises(self):
        with self.assertRaises(FileNotFoundError):
            qpf.build_fixture_tree(
                self.root / "staging", self.root / "no-such-tree"
            )

    def test_missing_required_asset_raises(self):
        cms = self.root / "cms"
        (cms / "web_resources" / "cm").mkdir(parents=True)
        staging = self.root / "staging"
        with self.assertRaises(FileNotFoundError):
            qpf.build_fixture_tree(staging, cms)

    def test_dynatree_leak_fails_staging(self):
        cms = _fake_cms_install(self.root / "cms", with_dynatree_leak=True)
        with self.assertRaises(FileExistsError):
            qpf.build_fixture_tree(self.root / "staging", cms)

    def test_verify_reports_missing_page(self):
        staging = self.root / "staging"
        staging.mkdir()
        problems = qpf.verify_fixture_tree(staging)
        self.assertTrue(any("index.html" in p for p in problems))
        self.assertTrue(any("jquery.fancytree.js" in p for p in problems))

    def test_verify_reports_retired_asset(self):
        cms = _fake_cms_install(self.root / "cms")
        staging = self.root / "staging"
        qpf.build_fixture_tree(staging, cms)
        leaked = staging / Path(*qpf.REMOVED_PUBLISHED_ASSETS[0].split("/"))
        leaked.parent.mkdir(parents=True, exist_ok=True)
        leaked.write_text("x", encoding="utf-8")
        problems = qpf.verify_fixture_tree(staging)
        self.assertTrue(any("retired" in p for p in problems))


class WidgetFixturePagesTest(unittest.TestCase):
    """US2 (#6240): one staged page per pinned matrix widget."""

    def setUp(self):
        self.td = TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.root = Path(self.td.name)

    def _staged(self) -> Path:
        cms = _fake_cms_install(self.root / "cms")
        staging = self.root / "staging"
        qpf.build_fixture_tree(staging, cms)
        return staging

    def test_table_matches_js_inventory(self):
        """Drift guard: the Python table mirrors WIDGETS exactly.

        The JS helper is the matrix's source of truth; this test parses its
        pinned rows and fails on any name/kind/marker the fixture does not
        cover (or covers without a matrix case).
        """
        import re

        helper = (
            SCRIPTS.parent.parent
            / "modules"
            / "perc-qa-automation"
            / "frontend"
            / "tests"
            / "helpers"
            / "published-widget-matrix.js"
        )
        if not helper.is_file():
            self.skipTest(f"matrix inventory not checked out: {helper}")
        row_re = re.compile(
            r"name:\s*\"([^\"]+)\",\s*label:\s*\"[^\"]*\",\s*"
            r"kind:\s*\"([^\"]+)\",\s*marker:\s*(\"([^\"]*)\"|'([^']*)'|null)"
        )
        parsed = {}
        for match in row_re.finditer(helper.read_text(encoding="utf-8")):
            name, kind = match.group(1), match.group(2)
            marker = match.group(4) if match.group(4) is not None else match.group(5)
            parsed[name] = (kind, marker)
        self.assertTrue(parsed, "expected pinned WIDGETS rows in the helper")
        tabled = {name: (kind, marker) for name, _label, kind, marker in qpf.WIDGET_FIXTURES}
        self.assertEqual(
            sorted(tabled),
            sorted(parsed),
            "fixture table and matrix inventory pin the same widgets",
        )
        for name, (kind, marker) in parsed.items():
            self.assertEqual(tabled[name], (kind, marker), f"drift for {name}")

    def test_build_writes_every_widget_page(self):
        staging = self._staged()
        self.assertEqual(len(qpf.WIDGET_FIXTURES), 45)
        for name, _label, _kind, _marker in qpf.WIDGET_FIXTURES:
            page = staging / qpf.WIDGET_FIXTURE_DIR / f"{name}.html"
            self.assertTrue(page.is_file(), f"missing {page}")
            text = page.read_text(encoding="utf-8")
            self.assertNotIn("http://", text, name)
            self.assertNotIn("https://", text, name)

    def test_widget_pages_carry_their_markers(self):
        staging = self._staged()
        self.assertEqual(qpf.verify_fixture_tree(staging), [])
        # Spot-check each marker shape end to end.
        by_name = {n: (l, k, m) for n, l, k, m in qpf.WIDGET_FIXTURES}
        text = (staging / qpf.WIDGET_FIXTURE_DIR / "percImage.html").read_text(
            encoding="utf-8"
        )
        self.assertIn("<img", text)
        self.assertIn("../web_resources/", text)
        text = (staging / qpf.WIDGET_FIXTURE_DIR / "percLogin.html").read_text(
            encoding="utf-8"
        )
        self.assertIn('id="perc-login-form"', text)
        text = (staging / qpf.WIDGET_FIXTURE_DIR / "percOpenGraph.html").read_text(
            encoding="utf-8"
        )
        self.assertLess(
            text.index('property="og:title"'), text.index("</head>"),
            "head marker must live in <head>",
        )
        text = (staging / qpf.WIDGET_FIXTURE_DIR / "percRichText.html").read_text(
            encoding="utf-8"
        )
        _label, _kind, marker = by_name["percRichText"]
        self.assertIsNone(marker)
        self.assertTrue(len(text.strip()) > 0)

    def test_verify_reports_missing_widget_page(self):
        staging = self._staged()
        (staging / qpf.WIDGET_FIXTURE_DIR / "percPoll.html").unlink()
        problems = qpf.verify_fixture_tree(staging)
        self.assertTrue(
            any("widgets/percPoll.html" in p for p in problems), problems
        )

    def test_verify_reports_missing_marker(self):
        staging = self._staged()
        page = staging / qpf.WIDGET_FIXTURE_DIR / "percTagList.html"
        page.write_text(
            "<!DOCTYPE html><html><head><title>x</title></head>"
            "<body><h1>Tag List</h1></body></html>",
            encoding="utf-8",
        )
        problems = qpf.verify_fixture_tree(staging)
        self.assertTrue(
            any("percTagList" in p and "marker" in p for p in problems),
            problems,
        )

    def test_unknown_marker_shapes_fail_loudly(self):
        with self.assertRaises(ValueError):
            qpf.fixture_widget_html("percFuture", "Future", "static", "div > p")
        with self.assertRaises(ValueError):
            qpf.fixture_widget_html("percFuture", "Future", "head", "title")


class DockerArgvTest(unittest.TestCase):
    """Container-side paths are POSIX by construction (Linux matrix cells).

    They are built with :mod:`posixpath` (never host-``Path`` joins or
    ``"/"`` literals) so the intent survives Windows checkouts; the
    ``container:/path`` argv shape is docker CLI protocol, not a host
    filesystem join.
    """

    def setUp(self):
        self.td = TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.root = Path(self.td.name)

    def test_cp_into_container_argv(self):
        host_src = self.root / "staging"
        container_dest = posixpath.join(posixpath.sep, "some", "dir")
        argv = qpf.docker_cp_into_container_argv(
            host_src, "perc-matrix-dts-h2", container_dest
        )
        self.assertEqual(
            argv,
            ["docker", "cp", str(host_src), f"perc-matrix-dts-h2:{container_dest}"],
        )

    def test_cp_from_container_argv(self):
        host_dest = self.root / "x"
        container_src = posixpath.join(
            posixpath.sep, "opt", "Percussion", qpf.CMS_WEB_RESOURCES_DIR
        )
        argv = qpf.docker_copy_from_container_argv(
            "perc-matrix-cms-h2", container_src, host_dest
        )
        self.assertEqual(
            argv,
            [
                "docker",
                "cp",
                f"perc-matrix-cms-h2:{container_src}",
                str(host_dest),
            ],
        )

    def test_print_published_contract(self):
        buf = io.StringIO()
        with redirect_stdout(buf):
            qpf.print_published_contract(
                "http://127.0.0.1:9983",
                "http://127.0.0.1:9983/category.html",
            )
        out = buf.getvalue()
        self.assertIn("PUBLISHED_SITE_URL=http://127.0.0.1:9983", out)
        self.assertIn(
            "PUBLISHED_CATEGORY_URL=http://127.0.0.1:9983/category.html",
            out,
        )


if __name__ == "__main__":
    unittest.main()
