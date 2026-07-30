"""Unit tests for i18n_translate_direct.py. Run without ``trans`` on PATH.

Invoke from the repository root::

    python3 modules/perc-i18n/scripts/test_i18n_translate_direct.py
"""
from __future__ import annotations

import sys
import tempfile
import time as _time
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIR))

import i18n_translate_direct as itd  # noqa: E402


class CacheKeyTest(unittest.TestCase):
    def test_stable(self):
        self.assertEqual(itd.cache_key("Hello", "ar"), itd.cache_key("Hello", "ar"))

    def test_distinct_per_target(self):
        self.assertNotEqual(itd.cache_key("Hello", "ar"), itd.cache_key("Hello", "hi"))


class TranslateTest(unittest.TestCase):
    def setUp(self):
        self._orig_cache = itd.CACHE_FILE
        self._tmpdir = tempfile.TemporaryDirectory()
        itd.CACHE_FILE = Path(self._tmpdir.name) / "cache.json"

    def tearDown(self):
        itd.CACHE_FILE = self._orig_cache
        self._tmpdir.cleanup()

    def test_placeholder_skipped(self):
        self.assertEqual(itd.translate("{0}", "ar", cache={}), "{0}")

    def test_uses_cache_without_invoking(self):
        cache = {itd.cache_key("Hello", "ar"): "مرحبا"}
        invoked = []
        orig = itd.invoke_translate
        itd.invoke_translate = lambda text, target: invoked.append((text, target)) or "nope"
        try:
            self.assertEqual(itd.translate("Hello", "ar", cache=cache), "مرحبا")
        finally:
            itd.invoke_translate = orig
        self.assertEqual(invoked, [])

    def test_invokes_on_cache_miss(self):
        orig = itd.invoke_translate
        itd.invoke_translate = lambda text, target: "مرحبا"
        cache: dict[str, str] = {}
        try:
            self.assertEqual(itd.translate("Hello", "ar", cache=cache), "مرحبا")
        finally:
            itd.invoke_translate = orig
        self.assertEqual(cache[itd.cache_key("Hello", "ar")], "مرحبا")


class BackoffTest(unittest.TestCase):
    def test_rate_limit_triggers_backoff(self):
        calls = {"n": 0}
        slept: list[float] = []

        class _FakeResult:
            def __init__(self, rc: int, stdout: str = "", stderr: str = ""):
                self.returncode = rc
                self.stdout = stdout
                self.stderr = stderr

        def fake_run(cmd, capture_output, text, check, encoding):
            calls["n"] += 1
            if calls["n"] < 3:
                return _FakeResult(rc=1, stderr="HTTP 429 Too Many Requests")
            return _FakeResult(rc=0, stdout="مرحبا\n")

        orig_sleep = _time.sleep
        orig_run = itd.subprocess.run
        _time.sleep = lambda s: slept.append(s)
        itd.subprocess.run = fake_run
        try:
            result = itd.invoke_translate("Hello", "ar")
        finally:
            _time.sleep = orig_sleep
            itd.subprocess.run = orig_run

        self.assertEqual(result, "مرحبا")
        self.assertEqual(calls["n"], 3)
        self.assertEqual(len(slept), 2)
        self.assertGreater(slept[1], slept[0])

    def test_success_does_not_sleep(self):
        slept: list[float] = []

        class _FakeResult:
            def __init__(self):
                self.returncode = 0
                self.stdout = "ok\n"
                self.stderr = ""

        orig_sleep = _time.sleep
        orig_run = itd.subprocess.run
        _time.sleep = lambda s: slept.append(s)
        itd.subprocess.run = lambda *a, **k: _FakeResult()
        try:
            self.assertEqual(itd.invoke_translate("Hello", "ar"), "ok")
        finally:
            _time.sleep = orig_sleep
            itd.subprocess.run = orig_run
        self.assertEqual(slept, [])


class TmxInjectTest(unittest.TestCase):
    def test_xml_escape_on_inject(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.tmx"
            p.write_text(
                '<?xml version="1.0" encoding="UTF-8"?>\n'
                '<tmx version="1.4"><header>'
                '<prop type="supportedlanguage">en-us</prop>'
                '<prop type="supportedlanguage">ar</prop>'
                "</header><body>"
                '<tu tuid="k@1">'
                '<tuv xml:lang="en-us"><seg>Hello &amp; world</seg></tuv>'
                "</tu>"
                "</body></tmx>\n",
                encoding="utf-8",
            )
            tmx = itd.TmxFile(p)
            inserted = tmx.inject("ar", {"k@1": 'مرحبا <عالم> & "أصدقاء"'})
            self.assertEqual(inserted, 1)
            self.assertIn("&lt;عالم&gt;", tmx.text)
            self.assertIn("&amp;", tmx.text)

    def test_replace_translation(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.tmx"
            p.write_text(
                '<?xml version="1.0" encoding="UTF-8"?>\n'
                "<tmx version=\"1.4\"><header></header><body>"
                '<tu tuid="k@1">'
                '<tuv xml:lang="en-us"><seg>Hello</seg></tuv>'
                '<tuv xml:lang="es"><seg>Hello</seg></tuv>'
                "</tu>"
                "</body></tmx>\n",
                encoding="utf-8",
            )
            tmx = itd.TmxFile(p)
            n = tmx.replace_translation("es", {"k@1": "Hola"})
            self.assertEqual(n, 1)
            self.assertIn("<seg>Hola</seg>", tmx.text)


if __name__ == "__main__":
    unittest.main()
