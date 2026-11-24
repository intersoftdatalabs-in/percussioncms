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

    def test_url_only_skipped(self):
        url = (
            'https://percussioncmshelp.intsof.com/percussion-cm1/'
            'install-setup/installing-cm1/system-requirements#browsers'
        )
        invoked = []
        orig_invoke = it.invoke_translate
        it.invoke_translate = lambda text, target: invoked.append((text, target)) or 'oops'
        try:
            self.assertEqual(it.translate(url, 'tr', cache={}), url)
        finally:
            it.invoke_translate = orig_invoke
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

        def fake_run(cmd, capture_output, text, check, encoding, **kwargs):
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

    def test_inject_matches_entity_encoded_tuid(self):
        """Regression: decoded list_missing keys must inject into escaped attrs."""
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'sample.tmx'
            p.write_text(
                '<?xml version="1.0" encoding="UTF-8"?>\n'
                '<tmx version="1.4"><header>'
                '<prop type="supportedlanguage">en-us</prop>'
                '<prop type="supportedlanguage">de-de</prop>'
                '</header><body>'
                '<tu tuid="dialog@Advanced &gt;&gt;">'
                '<tuv xml:lang="en-us"><seg>Advanced &gt;&gt;</seg></tuv>'
                '</tu>'
                '</body></tmx>\n',
                encoding='utf-8',
            )
            tmx = it.TmxFile(p)
            missing = tmx.list_missing('de-de')
            self.assertEqual([t for t, _ in missing], ['dialog@Advanced >>'])
            inserted = tmx.inject('de-de', {'dialog@Advanced >>': 'Erweitert >>'})
            self.assertEqual(inserted, 1)
            self.assertIn('<seg>Erweitert &gt;&gt;</seg>', tmx.text)


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


class EngineHelpersTest(unittest.TestCase):
    def test_default_docker_cmd_shape(self):
        cmd = it._default_docker_cmd('Hello', 'de-de')
        self.assertEqual(cmd[:3], ['docker', 'run', '--rm'])
        self.assertIn('soimort/translate-shell', cmd)
        self.assertEqual(cmd[-2:], ['Hello', ':de-de'])

    def test_is_rate_limited(self):
        class R:
            def __init__(self, out, err):
                self.stdout = out
                self.stderr = err
        self.assertTrue(it._is_rate_limited(R('', 'HTTP 429 Too Many Requests')))
        self.assertTrue(it._is_rate_limited(R('Rate Limit exceeded', '')))
        self.assertTrue(it._is_rate_limited(R('', 'too many requests')))
        self.assertFalse(it._is_rate_limited(R('', 'some other failure')))

    def test_clean_engine_text(self):
        self.assertEqual(it._clean_engine_text(None), '')
        self.assertEqual(it._clean_engine_text('﻿  Hallo \n'), 'Hallo')

    def test_looks_like_translation(self):
        self.assertTrue(it._looks_like_translation('Hallo'))
        self.assertFalse(it._looks_like_translation(''))
        self.assertFalse(it._looks_like_translation('line1\nline2'))
        self.assertFalse(it._looks_like_translation('Error: something'))

    def test_success_output_prefers_stdout(self):
        class R:
            returncode = 0
            stdout = 'Hallo\n'
            stderr = ''
        self.assertEqual(it._success_output(R()), 'Hallo')

    def test_success_output_falls_back_to_stderr(self):
        class R:
            returncode = 0
            stdout = '  \n'
            stderr = 'Hallo'
        self.assertEqual(it._success_output(R()), 'Hallo')

    def test_success_output_none_when_unusable(self):
        class Empty:
            returncode = 0
            stdout = '  '
            stderr = 'multi\nline\ndiagnostics'
        self.assertIsNone(it._success_output(Empty()))

        class Failed:
            returncode = 1
            stdout = 'Hallo'
            stderr = ''
        self.assertIsNone(it._success_output(Failed()))

    def test_run_engine_missing_binary_raises_docker_hint(self):
        orig_run = it.subprocess.run
        def fake_run(*args, **kwargs):
            raise FileNotFoundError('docker')
        it.subprocess.run = fake_run
        try:
            with self.assertRaises(RuntimeError) as ctx:
                it._run_engine(['docker', 'run'])
        finally:
            it.subprocess.run = orig_run
        self.assertIn('Docker is not on PATH', str(ctx.exception))

    def test_backoff_sleep_doubles_delay(self):
        import time as _time
        slept: list[float] = []
        orig_sleep = _time.sleep
        _time.sleep = slept.append
        try:
            nxt = it._backoff_sleep(1, 2.0)
        finally:
            _time.sleep = orig_sleep
        self.assertEqual(len(slept), 1)
        # +-20% jitter around BACKOFF_START_SEC.
        self.assertGreaterEqual(slept[0], 2.0 * 0.8)
        self.assertLessEqual(slept[0], 2.0 * 1.2)
        self.assertEqual(nxt, 4.0)


class TmxScanHelpersTest(unittest.TestCase):
    def _tuv(self, lang: str | None, seg: str | None) -> ET.Element:
        tuv = ET.Element('tuv')
        if lang is not None:
            tuv.set('{http://www.w3.org/XML/1998/namespace}lang', lang)
        if seg is not None:
            seg_el = ET.SubElement(tuv, 'seg')
            seg_el.text = seg
        return tuv

    def test_tuv_lang_namespaced(self):
        self.assertEqual(it._tuv_lang(self._tuv('de-de', 'x')), 'de-de')

    def test_tuv_lang_plain_attribute(self):
        tuv = ET.Element('tuv')
        tuv.set('xml:lang', 'fr-fr')
        self.assertEqual(it._tuv_lang(tuv), 'fr-fr')

    def test_tuv_lang_missing(self):
        self.assertIsNone(it._tuv_lang(ET.Element('tuv')))

    def test_seg_text_missing_seg(self):
        self.assertEqual(it._seg_text(ET.Element('tuv')), '')

    def test_iter_tus_skips_empty_tuid(self):
        root = ET.fromstring(
            '<body><tu><tuv xml:lang="en-us"><seg>x</seg></tuv></tu>'
            '<tu tuid="k@1"><tuv xml:lang="en-us"><seg>y</seg></tuv></tu></body>')
        self.assertEqual([t for t, _ in it._iter_tus(root)], ['k@1'])

    def test_scan_tu_missing_returns_none_when_target_present(self):
        tu = ET.fromstring(
            '<tu tuid="k@1"><tuv xml:lang="en-us"><seg>Hello</seg></tuv>'
            '<tuv xml:lang="de-de"><seg>Hallo</seg></tuv></tu>')
        self.assertIsNone(it._scan_tu_missing(tu, 'de-de'))

    def test_scan_tu_missing_returns_en_seg(self):
        tu = ET.fromstring(
            '<tu tuid="k@1"><tuv xml:lang="en-us"><seg>Hello</seg></tuv></tu>')
        self.assertEqual(it._scan_tu_missing(tu, 'de-de'), 'Hello')

    def test_scan_tu_missing_none_without_en_seg(self):
        tu = ET.fromstring(
            '<tu tuid="k@1"><tuv xml:lang="fr-fr"><seg>Bonjour</seg></tuv></tu>')
        self.assertIsNone(it._scan_tu_missing(tu, 'de-de'))


class FillTranslationsTest(unittest.TestCase):
    def setUp(self):
        self._orig_cache = it.CACHE_FILE
        self._tmpdir = tempfile.TemporaryDirectory()
        it.CACHE_FILE = Path(self._tmpdir.name) / 'cache.json'

    def tearDown(self):
        it.CACHE_FILE = self._orig_cache
        self._tmpdir.cleanup()

    def test_limit_stops_translation(self):
        calls: list[str] = []
        orig_translate = it.translate
        it.translate = lambda text, target, **kw: calls.append(text) or 'X'
        try:
            out = it._fill_translations(
                [('a', 'one'), ('b', 'two'), ('c', 'three')],
                target='de-de', cache={}, force=False, limit=2)
        finally:
            it.translate = orig_translate
        self.assertEqual(out, {'a': 'X', 'b': 'X'})
        self.assertEqual(calls, ['one', 'two'])

    def test_runtime_error_aborts_and_persists_cache(self):
        orig_translate = it.translate
        def boom(text, target, **kw):
            raise RuntimeError('engine down')
        it.translate = boom
        try:
            out = it._fill_translations(
                [('a', 'one')], target='de-de',
                cache={'k': 'v'}, force=False, limit=0)
        finally:
            it.translate = orig_translate
        self.assertIsNone(out)
        saved = json.loads(it.CACHE_FILE.read_text(encoding='utf-8'))
        self.assertEqual(saved, {'k': 'v'})

    def test_empty_missing_returns_empty(self):
        self.assertEqual(
            it._fill_translations([], target='de-de', cache={},
                                  force=False, limit=0), {})


class ResolveFilesTest(unittest.TestCase):
    def test_absolute_path_rejected(self):
        # Path.cwd() is absolute on every supported platform (a POSIX-only
        # literal like '/abs/path.tmx' is relative on Windows).
        with self.assertRaises(it._CliError) as ctx:
            it._resolve_files([str(Path.cwd() / 'abs-path.tmx')])
        self.assertEqual(ctx.exception.code, 2)
        self.assertIn('must be relative', ctx.exception.message)

    def test_missing_file_rejected(self):
        with self.assertRaises(it._CliError) as ctx:
            it._resolve_files(['Nope.tmx'])
        self.assertEqual(ctx.exception.code, 2)
        self.assertIn('not found', ctx.exception.message)

    def test_resolves_against_i18n_dir(self):
        with tempfile.TemporaryDirectory() as d:
            target = Path(d) / 'CmsUi.tmx'
            target.write_text('<tmx/>', encoding='utf-8')
            orig_dir = it.I18N_DIR
            it.I18N_DIR = Path(d)
            try:
                self.assertEqual(it._resolve_files(['CmsUi.tmx']), [target])
            finally:
                it.I18N_DIR = orig_dir


class MainDryRunTest(unittest.TestCase):
    def _write_sample(self, tmp: Path) -> None:
        (tmp / 'CmsUi.tmx').write_text(
            '<?xml version="1.0" encoding="UTF-8"?>\n'
            '<tmx version="1.4"><header>'
            '<prop type="supportedlanguage">en-us</prop>'
            '<prop type="supportedlanguage">de-de</prop>'
            '</header><body>'
            '<tu tuid="k@1">'
            '<tuv xml:lang="en-us"><seg>Hello</seg></tuv>'
            '</tu></body></tmx>\n',
            encoding='utf-8',
        )

    def test_dry_run_reports_missing(self):
        import io
        from contextlib import redirect_stdout
        with tempfile.TemporaryDirectory() as d:
            tmp = Path(d)
            self._write_sample(tmp)
            orig_dir, orig_cache = it.I18N_DIR, it.CACHE_FILE
            it.I18N_DIR = tmp
            it.CACHE_FILE = tmp / 'cache.json'
            try:
                buf = io.StringIO()
                with redirect_stdout(buf):
                    code = it.main(['--target', 'de-de', '--dry-run',
                                    '--file', 'CmsUi.tmx'])
            finally:
                it.I18N_DIR, it.CACHE_FILE = orig_dir, orig_cache
        self.assertEqual(code, 0)
        out = buf.getvalue()
        self.assertIn('CmsUi.tmx: 1 <tu> missing <tuv xml:lang="de-de">', out)
        self.assertIn('Total missing: 1; inserted: 0', out)

    def test_absolute_file_returns_2(self):
        self.assertEqual(
            it.main(['--target', 'de-de', '--dry-run',
                     '--file', str(Path.cwd() / 'abs-path.tmx')]), 2)


if __name__ == '__main__':
    unittest.main()
