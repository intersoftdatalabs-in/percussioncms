"""Unit tests for i18n_translate_direct.py. Run without ``trans`` on PATH.

Invoke from the repository root::

    python3 modules/perc-i18n/scripts/test_i18n_translate_direct.py
"""
from __future__ import annotations

import shutil
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

    def test_url_only_skipped(self):
        url = (
            "https://percussioncmshelp.intsof.com/percussion-cm1/"
            "install-setup/installing-cm1/system-requirements#browsers"
        )
        invoked = []
        orig = itd.invoke_translate
        itd.invoke_translate = lambda text, target: invoked.append((text, target)) or "nope"
        try:
            self.assertEqual(itd.translate(url, "tr", cache={}), url)
            self.assertEqual(itd.translate("  " + url + "  ", "tr", cache={}), "  " + url + "  ")
        finally:
            itd.invoke_translate = orig
        self.assertEqual(invoked, [])

    def test_prose_with_url_still_translates(self):
        text = "See https://example.com/docs for details"
        orig = itd.invoke_translate
        orig_sleep = itd.time.sleep
        itd.invoke_translate = lambda text, target: "translated"
        itd.time.sleep = lambda s: None
        try:
            self.assertEqual(itd.translate(text, "tr", cache={}), "translated")
        finally:
            itd.invoke_translate = orig
            itd.time.sleep = orig_sleep

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
        orig_sleep = itd.time.sleep
        slept: list[float] = []
        itd.invoke_translate = lambda text, target: "مرحبا"
        itd.time.sleep = lambda s: slept.append(s)
        cache: dict[str, str] = {}
        try:
            self.assertEqual(itd.translate("Hello", "ar", cache=cache), "مرحبا")
        finally:
            itd.invoke_translate = orig
            itd.time.sleep = orig_sleep
        self.assertEqual(cache[itd.cache_key("Hello", "ar")], "مرحبا")
        self.assertEqual(len(slept), 1)
        self.assertGreaterEqual(slept[0], itd.THROTTLE_MIN_SEC)
        self.assertLessEqual(slept[0], itd.THROTTLE_MAX_SEC)

    def test_throttle_not_applied_on_cache_hit(self):
        cache = {itd.cache_key("Hello", "ar"): "مرحبا"}
        orig_sleep = itd.time.sleep
        slept: list[float] = []
        itd.time.sleep = lambda s: slept.append(s)
        try:
            self.assertEqual(itd.translate("Hello", "ar", cache=cache), "مرحبا")
        finally:
            itd.time.sleep = orig_sleep
        self.assertEqual(slept, [])

    def test_throttle_not_applied_on_placeholder(self):
        orig_sleep = itd.time.sleep
        slept: list[float] = []
        itd.time.sleep = lambda s: slept.append(s)
        try:
            self.assertEqual(itd.translate("{0}", "ar", cache={}), "{0}")
        finally:
            itd.time.sleep = orig_sleep
        self.assertEqual(slept, [])


class BackoffTest(unittest.TestCase):
    def test_rate_limit_triggers_backoff(self):
        calls = {"n": 0}
        slept: list[float] = []

        class _FakeResult:
            def __init__(self, rc: int, stdout: str = "", stderr: str = ""):
                self.returncode = rc
                self.stdout = stdout
                self.stderr = stderr

        def fake_run(cmd, capture_output, text, check, encoding, **kwargs):
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


class DockerFallbackTest(unittest.TestCase):
    def setUp(self):
        self._force = itd._force_docker
        itd._force_docker = False
        itd._warned_trans_fallback = False

    def tearDown(self):
        itd._force_docker = self._force
        itd._warned_trans_fallback = False

    def test_uses_docker_when_trans_missing(self):
        calls: list[list[str]] = []

        class _FakeResult:
            def __init__(self, rc: int, stdout: str = "", stderr: str = ""):
                self.returncode = rc
                self.stdout = stdout
                self.stderr = stderr

        def fake_run(cmd, *a, **k):
            calls.append(list(cmd))
            return _FakeResult(rc=0, stdout='مرحبا\n')

        orig_run = itd.subprocess.run
        orig_which = itd.shutil.which
        itd.subprocess.run = fake_run
        itd.shutil.which = lambda name: '/usr/bin/docker' if name == 'docker' else None
        try:
            result = itd.invoke_translate('Hello', 'ar')
        finally:
            itd.subprocess.run = orig_run
            itd.shutil.which = orig_which

        self.assertEqual(result, 'مرحبا')
        self.assertEqual(len(calls), 1)
        self.assertEqual(calls[0][:3], ['docker', 'run', '--rm'])
        self.assertIn('-no-bidi', calls[0])
        self.assertIn('-no-bidi', itd._default_trans_cmd('Hello', 'ar'))
        self.assertIn('-no-bidi', itd._default_docker_cmd('Hello', 'ar'))

    def test_force_docker_flag(self):
        calls: list[list[str]] = []

        class _FakeResult:
            def __init__(self):
                self.returncode = 0
                self.stdout = 'مرحبا\n'
                self.stderr = ''

        def fake_run(cmd, *a, **k):
            calls.append(cmd[0])
            return _FakeResult()

        orig_run = itd.subprocess.run
        orig_which = itd.shutil.which
        itd.subprocess.run = fake_run
        # Even if trans exists, --docker must win.
        itd.shutil.which = lambda name: f'/usr/bin/{name}'
        itd._force_docker = True
        try:
            self.assertEqual(itd.invoke_translate('Hello', 'ar'), 'مرحبا')
        finally:
            itd.subprocess.run = orig_run
            itd.shutil.which = orig_which
        self.assertEqual(calls, ['docker'])

    def test_stderr_brief_on_windows_docker(self):
        """Some Windows Docker hosts put the brief line on stderr."""
        class _FakeResult:
            returncode = 0
            stdout = ''
            stderr = 'مرحبا\n'

        orig_run = itd.subprocess.run
        orig_which = itd.shutil.which
        itd.subprocess.run = lambda *a, **k: _FakeResult()
        itd.shutil.which = lambda name: '/usr/bin/docker' if name == 'docker' else None
        itd._force_docker = True
        try:
            self.assertEqual(itd.invoke_translate('Hello', 'ar'), 'مرحبا')
        finally:
            itd.subprocess.run = orig_run
            itd.shutil.which = orig_which

    def test_raises_when_trans_and_docker_unavailable(self):
        orig_which = itd.shutil.which
        itd.shutil.which = lambda name: None
        try:
            with self.assertRaises(RuntimeError) as ctx:
                itd.invoke_translate('Hello', 'ar')
            self.assertIn('trans', str(ctx.exception))
            self.assertIn('Docker', str(ctx.exception))
        finally:
            itd.shutil.which = orig_which


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

    def test_inject_matches_entity_encoded_tuid(self):
        """list_missing returns decoded tuids; raw XML keeps &gt;/&lt;/&amp;."""
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.tmx"
            p.write_text(
                '<?xml version="1.0" encoding="UTF-8"?>\n'
                '<tmx version="1.4"><header>'
                '<prop type="supportedlanguage">en-us</prop>'
                '<prop type="supportedlanguage">es</prop>'
                "</header><body>"
                '<tu tuid="dialog@Advanced &gt;&gt;">'
                '<tuv xml:lang="en-us"><seg>Advanced &gt;&gt;</seg></tuv>'
                "</tu>"
                '<tu tuid="dialog@A &amp; B">'
                '<tuv xml:lang="en-us"><seg>A &amp; B</seg></tuv>'
                "</tu>"
                '<tu tuid="dialog@val &lt;{0}&gt;">'
                '<tuv xml:lang="en-us"><seg>val &lt;{0}&gt;</seg></tuv>'
                "</tu>"
                "</body></tmx>\n",
                encoding="utf-8",
            )
            tmx = itd.TmxFile(p)
            missing = tmx.list_missing("es")
            keys = [t for t, _ in missing]
            self.assertEqual(
                keys,
                ["dialog@Advanced >>", "dialog@A & B", "dialog@val <{0}>"],
            )
            translations = {
                "dialog@Advanced >>": "Avanzado >>",
                "dialog@A & B": "A y B",
                "dialog@val <{0}>": "val <{0}>",
            }
            inserted = tmx.inject("es", translations)
            self.assertEqual(inserted, 3)
            self.assertIn('tuid="dialog@Advanced &gt;&gt;"', tmx.text)
            self.assertIn("<seg>Avanzado &gt;&gt;</seg>", tmx.text)
            self.assertIn("<seg>A y B</seg>", tmx.text)
            self.assertIn("<seg>val &lt;{0}&gt;</seg>", tmx.text)

    def test_replace_entity_encoded_tuid(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.tmx"
            p.write_text(
                '<?xml version="1.0" encoding="UTF-8"?>\n'
                "<tmx version=\"1.4\"><header></header><body>"
                '<tu tuid="dialog@Advanced &gt;&gt;">'
                '<tuv xml:lang="en-us"><seg>Advanced &gt;&gt;</seg></tuv>'
                '<tuv xml:lang="es"><seg>Advanced &gt;&gt;</seg></tuv>'
                "</tu>"
                "</body></tmx>\n",
                encoding="utf-8",
            )
            tmx = itd.TmxFile(p)
            n = tmx.replace_translation("es", {"dialog@Advanced >>": "Avanzado >>"})
            self.assertEqual(n, 1)
            self.assertIn("<seg>Avanzado &gt;&gt;</seg>", tmx.text)

    def test_inject_single_quoted_tuid(self):
        """Tuids containing double quotes are single-quoted in raw TMX."""
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.tmx"
            p.write_text(
                '<?xml version="1.0" encoding="UTF-8"?>\n'
                "<tmx version=\"1.4\"><header></header><body>"
                "<tu tuid='arch@Delete section \"{0}\"?'>"
                '<tuv xml:lang="en-us"><seg>Delete section "{0}"?</seg></tuv>'
                "</tu>"
                "</body></tmx>\n",
                encoding="utf-8",
            )
            tmx = itd.TmxFile(p)
            missing = tmx.list_missing("es")
            self.assertEqual(
                missing, [('arch@Delete section "{0}"?', 'Delete section "{0}"?')]
            )
            inserted = tmx.inject("es", {'arch@Delete section "{0}"?': 'Eliminar "{0}"?'})
            self.assertEqual(inserted, 1)
            self.assertIn('<seg>Eliminar "{0}"?</seg>', tmx.text)

    def test_replace_single_quoted_tuid(self):
        """replace_translation also matches single-quoted tuids."""
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.tmx"
            p.write_text(
                '<?xml version="1.0" encoding="UTF-8"?>\n'
                "<tmx version=\"1.4\"><header></header><body>"
                "<tu tuid='arch@Delete section \"{0}\"?'>"
                '<tuv xml:lang="en-us"><seg>Delete section "{0}"?</seg></tuv>'
                '<tuv xml:lang="es"><seg>Delete section "{0}"?</seg></tuv>'
                "</tu>"
                "</body></tmx>\n",
                encoding="utf-8",
            )
            tmx = itd.TmxFile(p)
            n = tmx.replace_translation(
                "es", {'arch@Delete section "{0}"?': 'Eliminar "{0}"?'}
            )
            self.assertEqual(n, 1)
            self.assertIn('<seg>Eliminar "{0}"?</seg>', tmx.text)

    def test_list_existing_includes_padded_and_empty(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.tmx"
            p.write_text(
                '<?xml version="1.0" encoding="UTF-8"?>\n'
                '<tmx version="1.4"><header></header><body>'
                '<tu tuid="k@ok">'
                '<tuv xml:lang="en-us"><seg>Hello</seg></tuv>'
                '<tuv xml:lang="ar"><seg>مرحبا</seg></tuv>'
                '</tu>'
                '<tu tuid="k@pad">'
                '<tuv xml:lang="en-us"><seg>Save</seg></tuv>'
                '<tuv xml:lang="ar"><seg>          يحفظ</seg></tuv>'
                '</tu>'
                '<tu tuid="k@missing">'
                '<tuv xml:lang="en-us"><seg>Only English</seg></tuv>'
                '</tu>'
                '</body></tmx>\n',
                encoding='utf-8',
            )
            tmx = itd.TmxFile(p)
            existing = tmx.list_existing('ar')
            keys = [t for t, _, __ in existing]
            self.assertEqual(keys, ['k@ok', 'k@pad'])
            self.assertEqual(tmx.list_missing('ar'), [('k@missing', 'Only English')])

    def test_replace_existing_cli_flag_accepted(self):
        # Ensure both long names parse onto the same dest.
        for flag in (
            '--replace-existing',
            '--waste-another-6-hours-of-your-life',
        ):
            with self.assertRaises(SystemExit) as ctx:
                # Missing --target should still parse the flag name.
                itd.main([flag])
            self.assertEqual(ctx.exception.code, 2)


class MainSkipOnErrorTest(unittest.TestCase):
    """Verify that a per-key translate failure skips the key and the run
    continues (does not return non-zero). This is the contract change that
    prevents one bad key from killing a multi-hour translation run.
    """

    def setUp(self):
        self._orig_argv = sys.argv
        self._orig_cache_file = itd.CACHE_FILE
        self._orig_invoke = itd.invoke_translate
        self._orig_load_cache = itd.load_cache
        self._orig_save_cache = itd.save_cache
        self._orig_sleep = itd.time.sleep
        self._tmpdir = tempfile.TemporaryDirectory()
        itd.CACHE_FILE = Path(self._tmpdir.name) / "cache.json"
        # Ensure load_cache returns a fresh dict; don't touch disk during run.
        itd.load_cache = lambda: {}
        itd.save_cache = lambda _cache: None
        itd.time.sleep = lambda _s: None  # no throttling in tests
        self.tmx_path = Path(self._tmpdir.name) / "sample.tmx"
        self.tmx_path.write_text(
            '<?xml version="1.0" encoding="UTF-8"?>\n'
            '<tmx version="1.4"><header>'
            '<prop type="supportedlanguage">en-us</prop>'
            '</header><body>'
            '<tu tuid="k@1"><tuv xml:lang="en-us"><seg>Hello</seg></tuv></tu>'
            '<tu tuid="k@2"><tuv xml:lang="en-us"><seg>World</seg></tuv></tu>'
            '</body></tmx>\n',
            encoding="utf-8",
        )

    def tearDown(self):
        sys.argv = self._orig_argv
        itd.CACHE_FILE = self._orig_cache_file
        itd.invoke_translate = self._orig_invoke
        itd.load_cache = self._orig_load_cache
        itd.save_cache = self._orig_save_cache
        itd.time.sleep = self._orig_sleep
        self._tmpdir.cleanup()

    def test_runtime_error_on_one_key_continues_run(self):
        """First key fails; second key succeeds; run returns 0."""
        calls: list[str] = []

        def fake_invoke(text, target):
            calls.append(text)
            if text == "Hello":
                raise RuntimeError("Null response from translate-shell")
            return "[te] " + text

        itd.invoke_translate = fake_invoke
        sys.argv = ["i18n_translate_direct.py", "--target", "te", "--file", str(self.tmx_path)]

        rc = itd.main()

        self.assertEqual(rc, 0, "main() must return 0 even when a key fails")
        self.assertEqual(calls, ["Hello", "World"], "both keys must be attempted")
        # TMX should have a Telugu TUV for k@2 only (k@1 was skipped).
        # Both TUs are present (k@1 with en-us only, k@2 with en-us + te).
        body = self.tmx_path.read_text(encoding="utf-8")
        self.assertIn('tuid="k@1"', body)
        self.assertIn('tuid="k@2"', body)
        self.assertIn("Hello", body)
        self.assertIn("[te] World", body)
        # k@1 must NOT have a Telugu TUV (the skip path leaves it en-us only).
        # Verify by extracting the k@1 <tu>...</tu> block.
        import re as _re
        k1_block = _re.search(r'<tu tuid="k@1">.*?</tu>', body, _re.DOTALL).group(0)
        self.assertNotIn('xml:lang="te"', k1_block, "skipped key must not get a Te TUV")


class MainReplaceExistingTest(unittest.TestCase):
    """Step 2 (--replace-existing) via main(): success path and abort path."""

    def setUp(self):
        self._orig_invoke = itd.invoke_translate
        self._orig_load_cache = itd.load_cache
        self._orig_save_cache = itd.save_cache
        self._orig_sleep = itd.time.sleep
        self._tmpdir = tempfile.TemporaryDirectory()
        itd.load_cache = lambda: {}
        itd.save_cache = lambda _cache: None
        itd.time.sleep = lambda _s: None
        self.tmx_path = Path(self._tmpdir.name) / "sample.tmx"
        self.tmx_path.write_text(
            '<?xml version="1.0" encoding="UTF-8"?>\n'
            '<tmx version="1.4"><header>'
            '<prop type="supportedlanguage">en-us</prop>'
            '</header><body>'
            '<tu tuid="k@1"><tuv xml:lang="en-us"><seg>Hello</seg></tuv>'
            '<tuv xml:lang="te"><seg>Hello</seg></tuv></tu>'
            "</body></tmx>\n",
            encoding="utf-8",
        )

    def tearDown(self):
        itd.invoke_translate = self._orig_invoke
        itd.load_cache = self._orig_load_cache
        itd.save_cache = self._orig_save_cache
        itd.time.sleep = self._orig_sleep
        self._tmpdir.cleanup()

    def test_replace_existing_retranslates(self):
        itd.invoke_translate = lambda text, target: "[te] " + text
        rc = itd.main(["--target", "te", "--file", str(self.tmx_path),
                       "--replace-existing"])
        self.assertEqual(rc, 0)
        body = self.tmx_path.read_text(encoding="utf-8")
        self.assertIn("<seg>[te] Hello</seg>", body)

    def test_replace_existing_hard_failure_aborts(self):
        def boom(text, target):
            raise RuntimeError("Null response from translate-shell")

        itd.invoke_translate = boom
        rc = itd.main(["--target", "te", "--file", str(self.tmx_path),
                       "--replace-existing"])
        self.assertEqual(rc, 1)


class MainFixMatchingEnTest(unittest.TestCase):
    """Step 3 (--fix-matching-en) via main()."""

    def setUp(self):
        self._orig_invoke = itd.invoke_translate
        self._orig_load_cache = itd.load_cache
        self._orig_save_cache = itd.save_cache
        self._orig_sleep = itd.time.sleep
        self._tmpdir = tempfile.TemporaryDirectory()
        itd.load_cache = lambda: {}
        itd.save_cache = lambda _cache: None
        itd.time.sleep = lambda _s: None
        self.tmx_path = Path(self._tmpdir.name) / "sample.tmx"
        self.tmx_path.write_text(
            '<?xml version="1.0" encoding="UTF-8"?>\n'
            '<tmx version="1.4"><header>'
            '<prop type="supportedlanguage">en-us</prop>'
            '</header><body>'
            '<tu tuid="k@1"><tuv xml:lang="en-us"><seg>Hello</seg></tuv>'
            '<tuv xml:lang="te"><seg>Hello</seg></tuv></tu>'
            '<tu tuid="k@2"><tuv xml:lang="en-us"><seg>World</seg></tuv>'
            '<tuv xml:lang="te"><seg>[te] World</seg></tuv></tu>'
            "</body></tmx>\n",
            encoding="utf-8",
        )

    def tearDown(self):
        itd.invoke_translate = self._orig_invoke
        itd.load_cache = self._orig_load_cache
        itd.save_cache = self._orig_save_cache
        itd.time.sleep = self._orig_sleep
        self._tmpdir.cleanup()

    def test_fix_matching_en_retranslates_only_matches(self):
        itd.invoke_translate = lambda text, target: "[te] " + text
        rc = itd.main(["--target", "te", "--file", str(self.tmx_path),
                       "--fix-matching-en"])
        self.assertEqual(rc, 0)
        body = self.tmx_path.read_text(encoding="utf-8")
        self.assertIn("<seg>[te] Hello</seg>", body)
        # k@2 already differed; its seg must be untouched.
        self.assertIn("<seg>[te] World</seg>", body)

    def test_fix_matching_en_skipped_for_variant(self):
        """Variant locales never take the fix-matching-en path."""
        itd.invoke_translate = lambda text, target: "[xx] " + text
        # hi-in TUVs already exist (identical to en); Step 1 finds
        # nothing missing, and Step 3 must stay skipped for variants.
        self.tmx_path.write_text(
            '<?xml version="1.0" encoding="UTF-8"?>\n'
            '<tmx version="1.4"><header>'
            '<prop type="supportedlanguage">en-us</prop>'
            '</header><body>'
            '<tu tuid="k@1"><tuv xml:lang="en-us"><seg>Hello</seg></tuv>'
            '<tuv xml:lang="hi-in"><seg>Hello</seg></tuv></tu>'
            "</body></tmx>\n",
            encoding="utf-8",
        )
        rc = itd.main(["--target", "hi-in", "--file", str(self.tmx_path),
                       "--fix-matching-en", "--variant-base", "hi"])
        self.assertEqual(rc, 0)
        body = self.tmx_path.read_text(encoding="utf-8")
        self.assertNotIn("[xx]", body)


class VariantFilterTest(unittest.TestCase):
    """_maybe_filter_variant: only true dialect differences are kept."""

    def _tmx_with_base(self, tmpdir):
        p = Path(tmpdir) / "v.tmx"
        p.write_text(
            '<?xml version="1.0" encoding="UTF-8"?>\n'
            '<tmx version="1.4"><header></header><body>'
            '<tu tuid="k@1"><tuv xml:lang="en-us"><seg>Hello</seg></tuv>'
            '<tuv xml:lang="hi"><seg>Hi</seg></tuv></tu>'
            "</body></tmx>\n",
            encoding="utf-8",
        )
        return itd.TmxFile(p)

    def test_no_base_lang_passes_through(self):
        with tempfile.TemporaryDirectory() as d:
            tmx = self._tmx_with_base(d)
            got = itd._maybe_filter_variant({"k@1": "X"}, tmx, "hi", None)
            self.assertEqual(got, {"k@1": "X"})

    def test_base_equals_target_passes_through(self):
        with tempfile.TemporaryDirectory() as d:
            tmx = self._tmx_with_base(d)
            got = itd._maybe_filter_variant({"k@1": "X"}, tmx, "hi", "hi")
            self.assertEqual(got, {"k@1": "X"})

    def test_identical_to_base_is_dropped(self):
        with tempfile.TemporaryDirectory() as d:
            tmx = self._tmx_with_base(d)
            got = itd._maybe_filter_variant(
                {"k@1": "Hi", "k@2": "Different"}, tmx, "hi-in", "hi")
            self.assertEqual(got, {"k@2": "Different"})


class LenientBatchTest(unittest.TestCase):
    """_translate_missing_lenient: skip counting and --limit."""

    def setUp(self):
        self._orig_translate = itd.translate
        self._orig_save_cache = itd.save_cache
        itd.save_cache = lambda _cache: None

    def tearDown(self):
        itd.translate = self._orig_translate
        itd.save_cache = self._orig_save_cache

    def test_skip_counted_and_run_continues(self):
        def fake_translate(text, target, cache=None, force=False):
            if text == "bad":
                raise RuntimeError("boom")
            return "[" + text + "]"

        itd.translate = fake_translate
        translations, skipped = itd._translate_missing_lenient(
            [("a", "bad"), ("b", "good")], target="te",
            cache={}, force=False, limit=0)
        self.assertEqual(translations, {"b": "[good]"})
        self.assertEqual(skipped, 1)

    def test_limit_bounds_processed_keys(self):
        seen = []

        def fake_translate(text, target, cache=None, force=False):
            seen.append(text)
            return text

        itd.translate = fake_translate
        translations, skipped = itd._translate_missing_lenient(
            [("a", "one"), ("b", "two"), ("c", "three")], target="te",
            cache={}, force=False, limit=2)
        self.assertEqual(sorted(translations), ["a", "b"])
        self.assertEqual(sorted(translations.values()), ["one", "two"])
        self.assertEqual(skipped, 0)
        self.assertEqual(seen, ["one", "two"])


class RetryHelperTest(unittest.TestCase):
    """_retry_after_failure: exhaustion raises, rate limit doubles delay."""

    class _FakeResult:
        def __init__(self, rc=1, stdout="", stderr=""):
            self.returncode = rc
            self.stdout = stdout
            self.stderr = stderr

    def test_exhausted_attempts_raise(self):
        with self.assertRaises(RuntimeError) as ctx:
            itd._retry_after_failure(
                self._FakeResult(stderr="oops"),
                itd.BACKOFF_MAX_ATTEMPTS, itd.BACKOFF_START_SEC)
        self.assertIn("rc=1", str(ctx.exception))

    def test_rate_limit_doubles_delay(self):
        sleep_s, nxt = itd._retry_after_failure(
            self._FakeResult(stderr="HTTP 429 Too Many Requests"), 1, 2.0)
        self.assertGreater(sleep_s, 0)
        self.assertEqual(nxt, 4.0)

    def test_transient_resets_delay(self):
        sleep_s, nxt = itd._retry_after_failure(
            self._FakeResult(stderr="Null response"), 1, 8.0)
        self.assertGreater(sleep_s, 0)
        self.assertEqual(nxt, itd.BACKOFF_START_SEC)


class MidRunDockerSwitchTest(unittest.TestCase):
    """_maybe_switch_to_docker: trans vanishing mid-run falls back once."""

    def setUp(self):
        self._force = itd._force_docker
        itd._force_docker = False
        itd._warned_trans_fallback = False

    def tearDown(self):
        itd._force_docker = self._force
        itd._warned_trans_fallback = False

    def test_file_not_found_switches_to_docker(self):
        calls = []

        class _FakeResult:
            returncode = 0
            stdout = "ok\n"
            stderr = ""

        state = {"n": 0}

        def fake_run(cmd, **kwargs):
            calls.append(list(cmd))
            state["n"] += 1
            if state["n"] == 1:
                raise FileNotFoundError("trans gone")
            return _FakeResult()

        orig_run = itd.subprocess.run
        orig_which = itd.shutil.which
        itd.subprocess.run = fake_run
        # trans resolves initially so the first call uses it; the binary
        # then "vanishes" (FileNotFoundError) and Docker takes over.
        # (which results are only null-checked, so no real paths needed.)
        fake_paths = {"trans": "trans", "docker": "docker"}
        itd.shutil.which = lambda name: fake_paths.get(name)
        try:
            self.assertEqual(itd.invoke_translate("Hello", "ar"), "ok")
        finally:
            itd.subprocess.run = orig_run
            itd.shutil.which = orig_which
        self.assertEqual(calls[0][0], "trans")
        self.assertEqual(calls[1][:3], ["docker", "run", "--rm"])

    def test_file_not_found_without_docker_raises(self):
        def fake_run(cmd, **kwargs):
            raise FileNotFoundError("trans gone")

        orig_run = itd.subprocess.run
        orig_which = itd.shutil.which
        itd.subprocess.run = fake_run
        itd.shutil.which = lambda name: None
        try:
            with self.assertRaises(RuntimeError) as ctx:
                itd.invoke_translate("Hello", "ar")
            self.assertIn("Docker", str(ctx.exception))
        finally:
            itd.subprocess.run = orig_run
            itd.shutil.which = orig_which


class RunFilesTest(unittest.TestCase):
    """_resolve_run_files: relative names resolve under I18N_DIR."""

    def test_relative_and_absolute(self):
        files = itd._resolve_run_files(["CmsUi.tmx"])
        self.assertEqual(files, [itd.I18N_DIR / "CmsUi.tmx"])
        with tempfile.TemporaryDirectory() as d:
            abs_p = str(Path(d) / "x.tmx")
            self.assertEqual(itd._resolve_run_files([abs_p]), [Path(abs_p)])


if __name__ == "__main__":
    unittest.main()
