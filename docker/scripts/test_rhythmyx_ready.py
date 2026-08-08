#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for rhythmyx_ready.py (#2462)."""

from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "rhythmyx_ready.py"
    name = "rhythmyx_ready"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(mod)
    return mod


rr = _load()


class FindContextFailureTests(unittest.TestCase):
    def test_empty_and_clean(self):
        self.assertIsNone(rr.find_rhythmyx_context_failure(""))
        self.assertIsNone(rr.find_rhythmyx_context_failure(None))  # type: ignore[arg-type]
        self.assertIsNone(
            rr.find_rhythmyx_context_failure(
                "INFO [Server] Started @7879ms\n"
                "INFO [AbstractConnector] Started {HTTP/1.1}{0.0.0.0:9992}\n"
            )
        )

    def test_failed_startup_of_context(self):
        text = (
            "WARN  [WebAppContext] Failed startup of context "
            "oeje11w.WebAppContext@…{ROOT,/,b=…Rhythmyx}\n"
            "org.springframework.beans.factory.UnsatisfiedDependencyException\n"
        )
        self.assertEqual(
            rr.find_rhythmyx_context_failure(text),
            "Failed startup of context",
        )

    def test_bean_currently_in_creation(self):
        text = (
            "Caused by: org.springframework.beans.factory."
            "BeanCurrentlyInCreationException:\n"
            "  Error creating bean with name 'folderHelper'\n"
        )
        self.assertEqual(
            rr.find_rhythmyx_context_failure(text),
            "BeanCurrentlyInCreationException",
        )

    def test_circular_reference_phrase(self):
        text = (
            "Is there an unresolvable circular reference or an "
            "asynchronous initialization dependency?\n"
        )
        self.assertEqual(
            rr.find_rhythmyx_context_failure(text),
            "Is there an unresolvable circular reference",
        )

    def test_installer_folderhelper_filename_not_a_match(self):
        """Installer unzip lines mention PercFolderHelper.js — not a context fail."""
        text = (
            "Unzipping to /tmp/x/jetty/base/webapps/Rhythmyx/cm/plugins/"
            "PercFolderHelper.js\n"
            "Creating file /tmp/x/…/PercFolderHelper.js\n"
        )
        self.assertIsNone(rr.find_rhythmyx_context_failure(text))


class AssessReadyTests(unittest.TestCase):
    def test_http_ready_clean_logs(self):
        ok, detail = rr.assess_rhythmyx_ready(200, "Server Started")
        self.assertTrue(ok)
        self.assertEqual(detail, "ok")

    def test_http_ready_but_context_failed(self):
        ok, detail = rr.assess_rhythmyx_ready(
            200,
            "WARN [WebAppContext] Failed startup of context Rhythmyx",
        )
        self.assertFalse(ok)
        self.assertIn(rr.DETAIL_CONTEXT_FAILED, detail)
        self.assertIn("Failed startup of context", detail)

    def test_http_not_ready_clean_logs(self):
        ok, detail = rr.assess_rhythmyx_ready(0, "")
        self.assertFalse(ok)
        self.assertIn("http_not_ready", detail)

    def test_context_fail_wins_over_http(self):
        ok, detail = rr.assess_rhythmyx_ready(
            302,
            "BeanCurrentlyInCreationException: folderHelper",
        )
        self.assertFalse(ok)
        self.assertIn(rr.DETAIL_CONTEXT_FAILED, detail)

    def test_is_http_ready_codes(self):
        for code in (200, 302, 401, 403):
            self.assertTrue(rr.is_http_ready_code(code), code)
        for code in (0, 404, 500, 503):
            self.assertFalse(rr.is_http_ready_code(code), code)


if __name__ == "__main__":
    unittest.main()
