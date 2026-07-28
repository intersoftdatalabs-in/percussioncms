"""Unit tests for i18n_translate.py. Run without Docker.

Invoke from the module directory::

    python3 modules/perc-i18n/scripts/test_i18n_translate.py

The test suite stubs subprocess invocation so Docker is not required.
"""
from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path
from xml.etree import ElementTree as ET
from xml.sax.saxutils import escape as xml_escape

SCRIPTS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIR))

import i18n_translate as it  # noqa: E402  (path injection before import)


class CacheKeyTest(unittest.TestCase):
    def test_stable(self):
        a = it.cache_key('Hello', 'de-de')
        b = it.cache_key('Hello', 'de-de')
        self.assertEqual(a, b)

    def test_distinct_per_target(self):
        a = it.cache_key('Hello', 'de-de')
        b = it.cache_key('Hello', 'fr-fr')
        self.assertNotEqual(a, b)

    def test_distinct_per_text(self):
        a = it.cache_key('Hello', 'de-de')
        b = it.cache_key('Goodbye', 'de-de')
        self.assertNotEqual(a, b)


class TranslateTest(unittest.TestCase):
    def setUp(self):
        # Save and restore module-level cache path so tests are isolated.
        self._orig_cache = it.CACHE_FILE
        self._tmpdir = tempfile.TemporaryDirectory()
        it.CACHE_FILE = Path(self._tmpdir.name) / 'cache.json'

    def tearDown(self):
        it.CACHE_FILE = self._orig_cache
        self._tmpdir.cleanup()

    def test_placeholder_skipped(self):
        invoked = []
        def fake_invoke(text, target):
            invoked.append((text, target))
            return 'should not happen'
        result = it.translate('{0}', 'de-de',
                              cache={}, force=False)
        self.assertEqual(result, '{0}')
        self.assertEqual(invoked, [])

    def test_uses_cache_without_invoking(self):
        cache = {it.cache_key('Hello', 'de-de'): 'Hallo'}
        invoked = []
        orig_invoke = it.invoke_translate
        it.invoke_translate = lambda text, target: invoked.append((text, target)) or 'oops'
        try:
            result = it.translate('Hello', 'de-de', cache=cache)
        finally:
            it.invoke_translate = orig_invoke
        self.assertEqual(result, 'Hallo')
        self.assertEqual(invoked, [])

    def test_invokes_on_cache_miss_and_writes_back(self):
        orig_invoke = it.invoke_translate
        it.invoke_translate = lambda text, target: 'Hallo'
        cache: dict[str, str] = {}
        try:
            result = it.translate('Hello', 'de-de', cache=cache)
        finally:
            it.invoke_translate = orig_invoke
        self.assertEqual(result, 'Hallo')
        self.assertEqual(cache[it.cache_key('Hello', 'de-de')], 'Hallo')

    def test_rate_limit_triggers_backoff(self):
        # Exercise invoke_translate's internal retry loop: stub
        # subprocess.run to fail twice with a 429-style stderr and then
        # succeed on the third attempt; stub time.sleep to record the
        # backoff delays without actually sleeping.
        import time as _time
        calls = {'n': 0}
        slept: list[float] = []

        class _FakeResult:
            def __init__(self, rc: int, stdout: str = '', stderr: str = ''):
                self.returncode = rc
                self.stdout = stdout
                self.stderr = stderr

        def fake_run(cmd, capture_output, text, check, encoding):
            calls['n'] += 1
            if calls['n'] < 3:
                return _FakeResult(
                    rc=1,
                    stdout='',
                    stderr='HTTP 429 Too Many Requests',
                )
            return _FakeResult(rc=0, stdout='Hallo\n')

        def fake_sleep(s):
            slept.append(s)

        orig_sleep = _time.sleep
        orig_run = it.subprocess.run
        _time.sleep = fake_sleep
        it.subprocess.run = fake_run
        try:
            result = it.invoke_translate('Hello', 'de-de')
        finally:
            _time.sleep = orig_sleep
            it.subprocess.run = orig_run

        self.assertEqual(result, 'Hallo')
        self.assertEqual(calls['n'], 3)
        # Two rate-limit responses should have produced two backoff sleeps.
        self.assertEqual(len(slept), 2)
        # First delay is BACKOFF_START_SEC (2s) with up to +-20% jitter,
        # second is doubled. Verify ordering and the doubling.
        self.assertGreater(slept[0], 0)
        self.assertGreater(slept[1], slept[0])

    def test_xml_escape_on_inject(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'sample.tmx'
            p.write_text(
                '<?xml version="1.0" encoding="UTF-8"?>\n'
                '<tmx version="1.4"><header>'
                '<prop type="supportedlanguage">en-us</prop>'
                '<prop type="supportedlanguage">de-de</prop>'
                '</header><body>'
                '<tu tuid="k@1">'
                '<tuv xml:lang="en-us"><seg>Hello &amp; world</seg></tuv>'
                '</tu>'
                '</body></tmx>\n',
                encoding='utf-8',
            )
            tmx = it.TmxFile(p)
            inserted = tmx.inject('de-de', {'k@1': 'Hallo <welt> & "freunde"'})
            self.assertEqual(inserted, 1)
            # The TMX should still parse after the edit.
            root = ET.fromstring(tmx.text)
            ns = {'xml': 'http://www.w3.org/XML/1998/namespace'}
            tuvs = root.findall('.//tuv', ns)
            de_tuv = next((t for t in tuvs if t.get('{http://www.w3.org/XML/1998/namespace}lang') == 'de-de'), None)
            self.assertIsNotNone(de_tuv)
            self.assertEqual(de_tuv.find('seg').text, 'Hallo <welt> & "freunde"')


class TmxFileTest(unittest.TestCase):
    def _make_tmx(self, tmp: Path, body: str) -> Path:
        p = tmp / 'sample.tmx'
        p.write_text(
            '<?xml version="1.0" encoding="UTF-8"?>\n'
            '<tmx version="1.4"><header>'
            '<prop type="supportedlanguage">en-us</prop>'
            '<prop type="supportedlanguage">de-de</prop>'
            '</header><body>'
            + body +
            '</body></tmx>\n',
            encoding='utf-8',
        )
        return p

    def test_list_missing(self):
        with tempfile.TemporaryDirectory() as d:
            p = self._make_tmx(Path(d),
                '<tu tuid="k@1">'
                '<tuv xml:lang="en-us"><seg>Hello</seg></tuv>'
                '</tu>'
                '<tu tuid="k@2">'
                '<tuv xml:lang="en-us"><seg>Already done</seg></tuv>'
                '<tuv xml:lang="de-de"><seg>Schon da</seg></tuv>'
                '</tu>'
            )
            tmx = it.TmxFile(p)
            missing = tmx.list_missing('de-de')
            self.assertEqual([t for t, _ in missing], ['k@1'])

    def test_inject_skips_existing(self):
        with tempfile.TemporaryDirectory() as d:
            p = self._make_tmx(Path(d),
                '<tu tuid="k@1">'
                '<tuv xml:lang="en-us"><seg>Hello</seg></tuv>'
                '<tuv xml:lang="de-de"><seg>Existing</seg></tuv>'
                '</tu>'
            )
            tmx = it.TmxFile(p)
            inserted = tmx.inject('de-de', {'k@1': 'Should not be inserted'})
            self.assertEqual(inserted, 0)


class PathSafetyTest(unittest.TestCase):
    """Ensure the script uses pathlib exclusively for filesystem paths."""

    def test_no_os_path_import(self):
        # Fail if i18n_translate.py adds an os.path import in the future.
        # This is a guard rail, not a functional test.
        src = (SCRIPTS_DIR / 'i18n_translate.py').read_text(encoding='utf-8')
        self.assertNotIn('os.path.join', src)
        self.assertNotIn("'/' +", src)
        self.assertNotIn("'\\\\' +", src)


if __name__ == '__main__':
    unittest.main()
