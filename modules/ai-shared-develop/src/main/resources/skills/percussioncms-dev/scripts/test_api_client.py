#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for api-client.py (no running CMS required).

``--dry-run`` exercises the full wiring without connecting. Real-mode
tests inject a stubbed ``urllib.request.urlopen`` to capture invocations
and assert exit-code / response-body mapping.
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
import urllib.request
from http.cookiejar import Cookie
from http.cookiejar import MozillaCookieJar
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "api-client.py"
    name = "api_client"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


ac = _load()


def _stub_urlopen(*, status=200, body=b"{}", url_error=None):
    """Replace ``urllib.request.urlopen`` with a fake response."""
    def fake_urlopen(request, timeout=None):
        if url_error is not None:
            raise url_error
        resp = unittest.mock.MagicMock()
        resp.status = status
        resp.read.return_value = body
        resp.__enter__ = lambda self: self
        resp.__exit__ = lambda self, *args: None
        return resp
    return fake_urlopen


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            ac.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_missing_endpoint_errors(self):
        """No --endpoint -> EXIT_INVOCATION (1)."""
        rc = ac.main([])
        self.assertEqual(rc, ac.EXIT_INVOCATION)


class TestNormalizeEndpoint(unittest.TestCase):
    def test_already_absolute(self):
        self.assertEqual(ac._normalize_endpoint("/folders"), "/folders")

    def test_missing_slash(self):
        self.assertEqual(ac._normalize_endpoint("folders"), "/folders")

    def test_already_absolute_with_path(self):
        self.assertEqual(
            ac._normalize_endpoint("/folders/by-path/Sites"),
            "/folders/by-path/Sites",
        )


class TestDefaultCookieJar(unittest.TestCase):
    def test_returns_cross_platform_path(self):
        path = ac._default_cookie_jar()
        self.assertTrue(str(path).endswith("perc-cookies.txt"))
        self.assertIn("perc-api", str(path))


class TestDryRun(unittest.TestCase):
    """``--dry-run`` exercises the wiring without connecting to a CMS."""

    def setUp(self):
        logging.getLogger("api-client").setLevel(logging.CRITICAL)

    def _run(self, **kwargs):
        # Default to GET; tests that need a different method pass it via kwargs.
        method = kwargs.pop("method", "GET")
        return ac.call(
            base_url="http://example.test/Rhythmyx/rest",
            user="admin",
            password="hunter2",
            endpoint="/folders/by-path/Assets",
            method=method,
            dry_run=True,
            **kwargs,
        )

    def test_dry_run_returns_exit_ok(self):
        self.assertEqual(self._run(), ac.EXIT_OK)

    def test_dry_run_with_post_and_data(self):
        self.assertEqual(
            self._run(method="POST", data='{"a":1}'),
            ac.EXIT_OK,
        )

    def test_dry_run_with_login_form(self):
        self.assertEqual(
            self._run(login_form=True),
            ac.EXIT_OK,
        )

    def test_dry_run_with_delete(self):
        self.assertEqual(
            self._run(method="DELETE"),
            ac.EXIT_OK,
        )


class TestRealRun(unittest.TestCase):
    """Real-mode tests inject a stubbed ``urllib.request.urlopen``."""

    def setUp(self):
        logging.getLogger("api-client").setLevel(logging.CRITICAL)

    def test_200_response_returns_exit_ok(self):
        body_bytes = json.dumps({"folders": [{"name": "Assets"}]}).encode("utf-8")
        with unittest.mock.patch.object(
            ac.urllib.request, "urlopen", new=_stub_urlopen(status=200, body=body_bytes)
        ):
            rc = ac.call(
                base_url="http://example.test/Rhythmyx/rest",
                user="admin",
                password="hunter2",
                endpoint="/folders/by-path/Assets",
                method="GET",
            )
        self.assertEqual(rc, ac.EXIT_OK)

    def test_401_without_auth_returns_exit_auth(self):
        """HTTP 401 with empty credentials -> EXIT_AUTH."""
        with unittest.mock.patch.object(
            ac.urllib.request,
            "urlopen",
            new=_stub_urlopen(
                status=401,
                body=b"",
                url_error=urllib.error.HTTPError(
                    "http://x", 401, "Unauthorized", {}, io.BytesIO(b""),
                ),
            ),
        ):
            rc = ac.call(
                base_url="http://example.test/Rhythmyx/rest",
                user="",
                password="",
                endpoint="/folders/by-path/Assets",
                method="GET",
            )
        self.assertEqual(rc, ac.EXIT_AUTH)

    def test_404_returns_exit_network(self):
        with unittest.mock.patch.object(
            ac.urllib.request,
            "urlopen",
            new=_stub_urlopen(
                status=404,
                body=b"not found",
                url_error=urllib.error.HTTPError(
                    "http://x", 404, "Not Found", {}, io.BytesIO(b"not found"),
                ),
            ),
        ):
            rc = ac.call(
                base_url="http://example.test/Rhythmyx/rest",
                user="admin",
                password="hunter2",
                endpoint="/missing",
                method="GET",
            )
        self.assertEqual(rc, ac.EXIT_NETWORK)

    def test_connection_error_returns_exit_network(self):
        with unittest.mock.patch.object(
            ac.urllib.request,
            "urlopen",
            new=_stub_urlopen(
                url_error=urllib.error.URLError("Connection refused"),
            ),
        ):
            rc = ac.call(
                base_url="http://example.test/Rhythmyx/rest",
                user="admin",
                password="hunter2",
                endpoint="/x",
                method="GET",
            )
        self.assertEqual(rc, ac.EXIT_NETWORK)

    def test_login_form_returns_exit_ok_on_302(self):
        with unittest.mock.patch.object(
            ac.urllib.request,
            "urlopen",
            new=_stub_urlopen(
                status=302,
                body=b"",
                url_error=urllib.error.HTTPError(
                    "http://x", 302, "Found", {}, io.BytesIO(b""),
                ),
            ),
        ):
            rc = ac.call(
                base_url="http://example.test/Rhythmyx/rest",
                user="admin",
                password="hunter2",
                endpoint="/x",
                method="GET",
                login_form=True,
            )
        self.assertEqual(rc, ac.EXIT_OK)


class TestRequestAuth(unittest.TestCase):
    def setUp(self):
        logging.getLogger("api-client").setLevel(logging.CRITICAL)

    def test_basic_auth_header_added_when_credentials_present(self):
        captured = {}

        def fake_urlopen(request, timeout=None):
            captured["authorization"] = request.headers.get("Authorization")
            resp = unittest.mock.MagicMock()
            resp.status = 200
            resp.read.return_value = b"{}"
            resp.__enter__ = lambda s: s
            resp.__exit__ = lambda s, *a: None
            return resp

        with unittest.mock.patch.object(ac.urllib.request, "urlopen", new=fake_urlopen):
            ac._request(
                "GET", "http://x.test/y",
                user="u", password="p",
            )
        import base64
        expected = base64.b64encode(b"u:p").decode("ascii")
        self.assertEqual(captured["authorization"], f"Basic {expected}")


class TestCookieJar(unittest.TestCase):
    def setUp(self):
        logging.getLogger("api-client").setLevel(logging.CRITICAL)

    def test_missing_cookie_jar_no_error(self):
        """A cookie jar that doesn't exist is silently ignored."""
        captured = {}

        def fake_urlopen(request, timeout=None):
            captured["cookie"] = request.headers.get("Cookie")
            resp = unittest.mock.MagicMock()
            resp.status = 200
            resp.read.return_value = b"{}"
            resp.__enter__ = lambda s: s
            resp.__exit__ = lambda s, *a: None
            return resp

        with tempfile.NamedTemporaryFile(delete=False) as tf:
            nonexistent = Path(tf.name)
        nonexistent.unlink()
        with unittest.mock.patch.object(ac.urllib.request, "urlopen", new=fake_urlopen):
            rc = ac._request(
                "GET", "http://x.test/y",
                cookie_jar=nonexistent,
            )
        self.assertEqual(rc, (200, "{}"))
        self.assertIsNone(captured["cookie"])

    def test_existing_cookie_jar_sends_cookie_header(self):
        """A real Netscape-format cookie jar sends a Cookie header."""
        with tempfile.TemporaryDirectory() as td:
            jar_path = Path(td) / "cookies.txt"
            cj = MozillaCookieJar(str(jar_path))
            cj.set_cookie(Cookie(
                version=0, name="JSESSIONID", value="abc123",
                port=None, port_specified=False,
                domain="x.test", domain_specified=True,
                domain_initial_dot=False,
                path="/", path_specified=True,
                secure=False, expires=None, discard=True,
                comment=None, comment_url=None,
                rest={"HttpOnly": None},
                rfc2109=False,
            ))
            cj.save(ignore_discard=True)

            captured = {}

            def fake_urlopen(request, timeout=None):
                captured["cookie"] = request.headers.get("Cookie")
                resp = unittest.mock.MagicMock()
                resp.status = 200
                resp.read.return_value = b"{}"
                resp.__enter__ = lambda s: s
                resp.__exit__ = lambda s, *a: None
                return resp

            with unittest.mock.patch.object(ac.urllib.request, "urlopen", new=fake_urlopen):
                rc = ac._request(
                    "GET", "http://x.test/y",
                    cookie_jar=jar_path,
                )
            self.assertEqual(rc, (200, "{}"))
            self.assertIn("JSESSIONID=abc123", captured["cookie"])


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            ac.main(["--help"])
        self.assertEqual(cm.exception.code, 0)


if __name__ == "__main__":
    logging.getLogger("api-client").setLevel(logging.CRITICAL)
    unittest.main()