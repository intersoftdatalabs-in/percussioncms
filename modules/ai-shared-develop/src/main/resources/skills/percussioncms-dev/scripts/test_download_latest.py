#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for download-latest.py (no GitHub, no network).

``--dry-run`` exercises the full wiring without contacting GitHub.
Real-mode tests inject a stubbed ``urllib.request.urlopen`` to capture
invocations and assert exit-code / file-output mapping.
"""

from __future__ import annotations

import importlib.util
import io
import json
import logging
import sys
import tempfile
import unittest
import unittest.mock
import urllib.error
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "download-latest.py"
    name = "download_latest"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


dl = _load()


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            dl.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_invalid_release_choice_errors(self):
        with self.assertRaises(SystemExit) as cm:
            dl.main(["--release", "bogus"])
        self.assertEqual(cm.exception.code, 2)


class TestDryRun(unittest.TestCase):
    """``--dry-run`` returns EXIT_OK without contacting GitHub."""

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        logging.getLogger("download-latest").setLevel(logging.CRITICAL)

    def _run(self, **kwargs):
        # Pop overrides so callers can pass include_dts / release etc.
        release = kwargs.pop("release", "stable")
        include_dts = kwargs.pop("include_dts", False)
        return dl.run(
            release=release,
            target_dir=Path(self.td.name),
            include_dts=include_dts,
            dry_run=True,
            token=None,
            **kwargs,
        )

    def test_dry_run_cms_only(self):
        self.assertEqual(self._run(), dl.EXIT_OK)

    def test_dry_run_with_dts(self):
        self.assertEqual(self._run(include_dts=True), dl.EXIT_OK)

    def test_dry_run_creates_target_dir(self):
        new_dir = Path(self.td.name) / "fresh" / "sub"
        rc = dl.run(
            release="lts",
            target_dir=new_dir,
            include_dts=True,
            dry_run=True,
            token=None,
        )
        self.assertEqual(rc, dl.EXIT_OK)
        self.assertTrue(new_dir.is_dir())


class TestRealRun(unittest.TestCase):
    """Real-mode tests inject stubbed urllib responses."""

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        logging.getLogger("download-latest").setLevel(logging.CRITICAL)

    def _release_body(self, *, with_cms=True, with_dts=False):
        assets = []
        if with_cms:
            assets.append({
                "name": "perc-distribution-tree-8.2.0.jar",
                "browser_download_url": "https://example.test/cms.jar",
            })
        if with_dts:
            assets.append({
                "name": "delivery-tier-distribution-8.2.0.jar",
                "browser_download_url": "https://example.test/dts.jar",
            })
        return {"tag_name": "v8.2.0", "assets": assets}

    def _stub_urlopen(self, *, releases_payload=None, file_payload=b"fake jar"):
        def fake_urlopen(request, timeout=None):
            url = request.full_url if hasattr(request, "full_url") else str(request)
            if "api.github.com" in url:
                resp = unittest.mock.MagicMock()
                resp.status = 200
                resp.read.return_value = json.dumps(releases_payload).encode("utf-8")
                resp.__enter__ = lambda s: s
                resp.__exit__ = lambda s, *a: None
                return resp
            # Asset download
            resp = unittest.mock.MagicMock()
            resp.status = 200
            resp.read.side_effect = [file_payload, b""]
            resp.__enter__ = lambda s: s
            resp.__exit__ = lambda s, *a: None
            return resp
        return fake_urlopen

    def test_cms_download_success(self):
        with unittest.mock.patch.object(
            dl.urllib.request, "urlopen",
            new=self._stub_urlopen(releases_payload=self._release_body()),
        ):
            rc = dl.run(
                release="stable",
                target_dir=Path(self.td.name),
                include_dts=False,
                dry_run=False,
                token=None,
            )
        self.assertEqual(rc, dl.EXIT_OK)
        jar = Path(self.td.name) / "perc-distribution-tree.jar"
        self.assertTrue(jar.is_file())
        self.assertEqual(jar.read_bytes(), b"fake jar")

    def test_cms_and_dts_download_success(self):
        with unittest.mock.patch.object(
            dl.urllib.request, "urlopen",
            new=self._stub_urlopen(releases_payload=self._release_body(with_dts=True)),
        ):
            rc = dl.run(
                release="stable",
                target_dir=Path(self.td.name),
                include_dts=True,
                dry_run=False,
                token=None,
            )
        self.assertEqual(rc, dl.EXIT_OK)
        self.assertTrue((Path(self.td.name) / "perc-distribution-tree.jar").is_file())
        self.assertTrue((Path(self.td.name) / "delivery-tier-distribution.jar").is_file())

    def test_no_cms_asset_returns_exit_no_asset(self):
        body = self._release_body(with_cms=False)
        with unittest.mock.patch.object(
            dl.urllib.request, "urlopen",
            new=self._stub_urlopen(releases_payload=body),
        ):
            rc = dl.run(
                release="stable",
                target_dir=Path(self.td.name),
                include_dts=False,
                dry_run=False,
                token=None,
            )
        self.assertEqual(rc, dl.EXIT_NO_ASSET)

    def test_dts_missing_when_requested_warns_but_succeeds(self):
        with unittest.mock.patch.object(
            dl.urllib.request, "urlopen",
            new=self._stub_urlopen(releases_payload=self._release_body(with_dts=False)),
        ):
            rc = dl.run(
                release="stable",
                target_dir=Path(self.td.name),
                include_dts=True,
                dry_run=False,
                token=None,
            )
        self.assertEqual(rc, dl.EXIT_OK)

    def test_http_error_returns_exit_network(self):
        def fake_urlopen(request, timeout=None):
            raise urllib.error.HTTPError(
                "http://api.github.com/repos/x/y/releases/latest",
                403,
                "Forbidden",
                {},
                io.BytesIO(b""),
            )
        with unittest.mock.patch.object(dl.urllib.request, "urlopen", new=fake_urlopen):
            rc = dl.run(
                release="stable",
                target_dir=Path(self.td.name),
                include_dts=False,
                dry_run=False,
                token=None,
            )
        self.assertEqual(rc, dl.EXIT_NETWORK)

    def test_retry_after_header_honored(self):
        sleep_calls = []

        def fake_sleep(seconds):
            sleep_calls.append(seconds)

        release_body = self._release_body()
        call_count = {"n": 0}

        def fake_urlopen(request, timeout=None):
            call_count["n"] += 1
            if call_count["n"] == 1:
                raise urllib.error.HTTPError(
                    "http://api.github.com/repos/x/y/releases/latest",
                    429,
                    "Too Many Requests",
                    {"Retry-After": "7"},
                    io.BytesIO(b""),
                )
            return self._stub_urlopen(releases_payload=release_body)(request, timeout)

        with unittest.mock.patch.object(dl.urllib.request, "urlopen", new=fake_urlopen), \
             unittest.mock.patch.object(dl.time, "sleep", new=fake_sleep):
            rc = dl.run(
                release="stable",
                target_dir=Path(self.td.name),
                include_dts=False,
                dry_run=False,
                token=None,
            )
        self.assertEqual(rc, dl.EXIT_OK)
        self.assertEqual(sleep_calls, [7])

    def test_retry_after_non_numeric_falls_back_to_backoff(self):
        sleep_calls = []

        def fake_sleep(seconds):
            sleep_calls.append(seconds)

        release_body = self._release_body()
        call_count = {"n": 0}

        def fake_urlopen(request, timeout=None):
            call_count["n"] += 1
            if call_count["n"] == 1:
                raise urllib.error.HTTPError(
                    "http://api.github.com/repos/x/y/releases/latest",
                    429,
                    "Too Many Requests",
                    {"Retry-After": "Wed, 21 Oct 2025 07:28:00 GMT"},
                    io.BytesIO(b""),
                )
            return self._stub_urlopen(releases_payload=release_body)(request, timeout)

        with unittest.mock.patch.object(dl.urllib.request, "urlopen", new=fake_urlopen), \
             unittest.mock.patch.object(dl.time, "sleep", new=fake_sleep):
            rc = dl.run(
                release="stable",
                target_dir=Path(self.td.name),
                include_dts=False,
                dry_run=False,
                token=None,
            )
        self.assertEqual(rc, dl.EXIT_OK)
        self.assertEqual(sleep_calls, [dl.BACKOFF_SECONDS])


class TestFindAssetUrl(unittest.TestCase):
    def test_returns_first_match(self):
        body = {
            "assets": [
                {"name": "source.zip", "browser_download_url": "u1"},
                {"name": "perc-distribution-tree-1.0.jar", "browser_download_url": "u2"},
                {"name": "perc-distribution-tree-2.0.jar", "browser_download_url": "u3"},
            ]
        }
        self.assertEqual(dl._find_asset_url(body, "perc-distribution-tree"), "u2")

    def test_returns_none_when_no_match(self):
        self.assertIsNone(dl._find_asset_url({"assets": []}, "perc-distribution-tree"))

    def test_ignores_non_jar_assets(self):
        body = {
            "assets": [
                {"name": "perc-distribution-tree.tar.gz", "browser_download_url": "u1"},
                {"name": "perc-distribution-tree-1.0.jar", "browser_download_url": "u2"},
            ]
        }
        self.assertEqual(dl._find_asset_url(body, "perc-distribution-tree"), "u2")


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            dl.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


if __name__ == "__main__":
    logging.getLogger("download-latest").setLevel(logging.CRITICAL)
    unittest.main()