"""Unit tests for i18n_glossary.py and the repair_zh_tw_takedown.py plan/apply.

Run from the repository root (no network, no Docker, no ``trans``)::

    python3 modules/perc-i18n/scripts/test_i18n_glossary.py

The OpenCC conversion test skips when the ``opencc`` package is missing.
"""
from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path
from xml.sax.saxutils import escape as xml_escape

SCRIPTS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIR))

import i18n_glossary as glossary  # noqa: E402
import i18n_translate as it  # noqa: E402
import i18n_translate_direct as itd  # noqa: E402
import repair_zh_tw_takedown as repair  # noqa: E402

try:
    from opencc import OpenCC  # noqa: F401

    HAS_OPENCC = True
except ImportError:
    HAS_OPENCC = False


class LookupTest(unittest.TestCase):
    def test_take_down_de(self):
        self.assertEqual(glossary.lookup('Take Down', 'de'), 'Zurückziehen')

    def test_take_down_sentence_case(self):
        self.assertEqual(glossary.lookup('Take down', 'de'), 'Zurückziehen')

    def test_take_downs_plural(self):
        self.assertEqual(glossary.lookup('Take Downs', 'fr'), 'Retraits')

    def test_zh_variants(self):
        self.assertEqual(glossary.lookup('Take Down', 'zh-cn'), '下线')
        self.assertEqual(glossary.lookup('Take Down', 'zh-tw'), '下線')

    def test_target_normalized(self):
        self.assertEqual(glossary.lookup('Take Down', 'ZH_TW'), '下線')
        self.assertEqual(glossary.lookup('Take Down', 'zh_TW'), '下線')

    def test_unknown_source_returns_none(self):
        self.assertIsNone(glossary.lookup('Take down failed', 'de'))
        # Case variants are listed explicitly, never folded.
        self.assertIsNone(glossary.lookup('take down', 'de'))
        self.assertIsNone(glossary.lookup('TAKE DOWN', 'de'))

    def test_unknown_target_returns_none(self):
        self.assertIsNone(glossary.lookup('Take Down', 'xx-yy'))

    def test_lou_not_pinned(self):
        # Louisiana Creole drafts stay owned by i18n_lou_draft.py.
        self.assertIsNone(glossary.lookup('Take Down', 'lou'))
        self.assertIsNone(glossary.lookup('Take Downs', 'lou'))

    def test_every_term_covers_every_finder_locale(self):
        # The repair only rewrites locales present in the TU; the table must
        # at least cover every locale the Take Down TUs ship on main.
        finder_locales = {
            'ar', 'bn', 'de', 'es', 'fr', 'he', 'hi', 'it', 'ja-jp', 'nb',
            'nl', 'pl', 'pt', 'ru', 'sv', 'te', 'tr', 'uk', 'zh-cn', 'zh-tw',
        }
        for term in ('Take Down', 'Take down', 'Take Downs'):
            self.assertTrue(
                finder_locales <= set(glossary.TERMS[term]),
                f'{term} missing locales: '
                f'{finder_locales - set(glossary.TERMS[term])}')


class TranslateHookTest(unittest.TestCase):
    """Both pipelines prefer the glossary over cache and engine."""

    def _check(self, mod):
        orig_invoke = mod.invoke_translate
        calls: list[tuple[str, str]] = []
        mod.invoke_translate = lambda text, target: calls.append(
            (text, target)) or 'ENGINE MUST NOT RUN'
        try:
            # Stale cached machine translation loses to the glossary ...
            cache = {mod.cache_key('Take Down', 'de'): 'Notieren'}
            self.assertEqual(
                mod.translate('Take Down', 'de', cache=cache), 'Zurückziehen')
            self.assertEqual(cache[mod.cache_key('Take Down', 'de')],
                             'Zurückziehen')
            # ... and non-terms still use the cache without invoking.
            cache2 = {mod.cache_key('Hello', 'de'): 'Hallo'}
            self.assertEqual(mod.translate('Hello', 'de', cache=cache2), 'Hallo')
        finally:
            mod.invoke_translate = orig_invoke
        self.assertEqual(calls, [])

    def test_docker_pipeline(self):
        self._check(it)

    def test_direct_pipeline(self):
        self._check(itd)


@unittest.skipUnless(HAS_OPENCC, 'opencc package not installed')
class RepairPlanTest(unittest.TestCase):
    def _tmx(self, directory: Path) -> Path:
        tmx = directory / 'CmsUi.tmx'
        tmx.write_text(
            '<?xml version="1.0" encoding="UTF-8"?>\n'
            '<tmx version="1.4"><header></header><body>\n'
            '<tu tuid="perc.ui.finder.view@Take Down">\n'
            '<tuv xml:lang="en-us"><seg>Take Down</seg></tuv>\n'
            '<tuv xml:lang="de"><seg>Notieren</seg></tuv>\n'
            '<tuv xml:lang="zh-tw"><seg>记录下来</seg></tuv>\n'
            '<tuv xml:lang="zh-cn"><seg>记录下来</seg></tuv>\n'
            '</tu>\n'
            '<tu tuid="k@Plain">\n'
            '<tuv xml:lang="en-us"><seg>Cancel</seg></tuv>\n'
            '<tuv xml:lang="zh-tw"><seg>取消</seg></tuv>\n'
            '<tuv xml:lang="zh-cn"><seg>取消</seg></tuv>\n'
            '</tu>\n'
            '<tu tuid="k@Done">\n'
            '<tuv xml:lang="en-us"><seg>Done</seg></tuv>\n'
            '<tuv xml:lang="zh-tw"><seg>完成</seg></tuv>\n'
            '<tuv xml:lang="zh-cn"><seg>完成</seg></tuv>\n'
            '</tu>\n'
            '</body></tmx>\n',
            encoding='utf-8')
        return tmx

    def test_plan_and_apply(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmx = self._tmx(Path(tmp))
            convert = repair._converter()
            plan, identical, differing, problems = repair.plan_file(
                tmx, True, True, convert)
            self.assertEqual(problems, [])
            self.assertEqual(identical, 3)
            # Glossary wins over mechanical conversion for the Take Down TU ...
            self.assertEqual(plan['perc.ui.finder.view@Take Down']['de'],
                             ('Notieren', 'Zurückziehen'))
            self.assertEqual(plan['perc.ui.finder.view@Take Down']['zh-tw'],
                             ('记录下来', '下線'))
            self.assertEqual(plan['perc.ui.finder.view@Take Down']['zh-cn'],
                             ('记录下来', '下线'))
            # ... while script-neutral pairs (取消, 完成) convert to
            # themselves and are not scheduled.
            self.assertNotIn('k@Plain', plan)
            self.assertNotIn('k@Done', plan)
            new_text, applied = repair.apply_plan(
                tmx.read_text(encoding='utf-8'), plan, tmx.name, problems)
            self.assertEqual(problems, [])
            self.assertEqual(applied, 3)
            self.assertIn('<seg>Zurückziehen</seg>', new_text)
            self.assertIn('<seg>下線</seg>', new_text)
            self.assertIn('<seg>下线</seg>', new_text)
            self.assertIn('<seg>完成</seg>', new_text)
            # Cache sync points the stale entries at the repaired text.
            cache = {
                repair.cache_key('Take Down', 'de'): 'Notieren',
            }
            tmx.write_text(new_text, encoding='utf-8')
            updated, added = repair.sync_cache(cache, tmx, plan)
            self.assertEqual(cache[repair.cache_key('Take Down', 'de')],
                             'Zurückziehen')
            self.assertEqual(
                cache[repair.cache_key('Take Down', 'zh-tw')], '下線')
            self.assertEqual(
                cache[repair.cache_key('Take Down', 'zh-cn')], '下线')
            self.assertEqual(updated + added, 3)

    def test_placeholders_guarded(self):
        self.assertFalse(repair._check_placeholders(
            '{0} items', 'items', 'w', []))
        problems: list[str] = []
        self.assertTrue(repair._check_placeholders(
            '{0} items {1}', '{0} items {1}', 'w', problems))
        self.assertEqual(problems, [])

    @unittest.skipUnless(HAS_OPENCC, 'opencc package not installed')
    def test_converter_is_script_only_no_taiwan_phrases(self):
        # #7145 review: s2twp's Taiwan phrase substitutions (项目→專案,
        # 发布→釋出) change meaning where 项目 means "item" and 发布 means
        # "publish". The repair converter must be script-only, with
        # OpenCC's 發佈 normalized to the catalog convention 發布 (#7116).
        convert = repair._converter()
        self.assertEqual(convert('过滤项目'), '過濾項目')
        self.assertEqual(convert('发布失败'), '發布失敗')
        self.assertEqual(convert('文件'), '文件')
        self.assertNotIn('專案', convert('过滤项目'))
        self.assertNotIn('釋出', convert('发布失败'))
        self.assertNotIn('發佈', convert('发布失败'))


if __name__ == '__main__':
    unittest.main()
