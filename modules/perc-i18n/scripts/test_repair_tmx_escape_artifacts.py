# Copyright (c) 2026 Intersoft Data Labs, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#
# See the License for the specific language governing permissions and
# limitations under the License.
"""Unit tests for repair_tmx_escape_artifacts.py. No Docker required.

Invoke from the repository root::

    python3 modules/perc-i18n/scripts/test_repair_tmx_escape_artifacts.py
"""
from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import repair_tmx_escape_artifacts as repair  # noqa: E402


class RepairSegmentTest(unittest.TestCase):
    def test_u200c_after_virama_becomes_real_zwnj(self) -> None:
        seg = 'పాస్' + 'u200c' + 'వర్డ్'
        fixed, problems = repair.repair_segment(seg, 'te', 'u200c')
        self.assertEqual(fixed, 'పాస్' + '\u200c' + 'వర్డ్')
        self.assertEqual(problems, [])

    def test_u200c_not_after_virama_is_left_for_adjudication(self) -> None:
        seg = 'abc u200c def'
        fixed, problems = repair.repair_segment(seg, 'te', 'u200c')
        self.assertEqual(fixed, seg)
        self.assertEqual(len(problems), 1)

    def test_u200c_outside_telugu_is_skipped(self) -> None:
        seg = 'foo u200c bar'
        fixed, problems = repair.repair_segment(seg, 'pt', 'u200c')
        self.assertEqual(fixed, seg)
        self.assertEqual(len(problems), 1)

    def test_u200b_runs_are_deleted(self) -> None:
        seg = 'Variáveis u200bu200btotais'
        fixed, problems = repair.repair_segment(seg, 'pt', 'u200b')
        self.assertEqual(fixed, 'Variáveis totais')
        self.assertEqual(problems, [])

    def test_clean_segment_is_untouched(self) -> None:
        seg = ' already clean '
        for artifact in ('u200c', 'u200b'):
            fixed, problems = repair.repair_segment(seg, 'te', artifact)
            self.assertEqual(fixed, seg)
            self.assertEqual(problems, [])


class RepairTextTest(unittest.TestCase):
    TMX = ('<?xml version="1.0" encoding="UTF-8"?><tmx version="1.4">'
           '<body>'
           '<tu tuid="k1"><tuv xml:lang="en-us"><seg>Password</seg></tuv>'
           '<tuv xml:lang="te"><seg>పాస్u200cవర్డ్</seg></tuv></tu>'
           '<tu tuid="k2"><tuv xml:lang="en-us"><seg>Total</seg></tuv>'
           '<tuv xml:lang="pt"><seg>Totais u200bu200bgerais</seg></tuv></tu>'
           '</body></tmx>')

    def test_repairs_only_non_source_tuvs(self) -> None:
        new_text, count, problems = repair.repair_text(self.TMX)
        self.assertEqual(count, 2)
        self.assertEqual(problems, [])
        self.assertIn('<seg>పాస్' + '\u200c' + 'వర్డ్</seg>', new_text)
        self.assertIn('<seg>Totais gerais</seg>', new_text)
        self.assertIn('<tuv xml:lang="en-us"><seg>Password</seg></tuv>',
                      new_text)

    def test_lang_filter_restricts_scope(self) -> None:
        new_text, count, _ = repair.repair_text(self.TMX, 'pt')
        self.assertEqual(count, 1)
        self.assertIn('u200c', new_text)
        self.assertNotIn('u200b', new_text)

    def test_dry_run_leaves_file_and_cache_alone(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmx = Path(tmp) / 'CmsUi.tmx'
            tmx.write_text(self.TMX, encoding='utf-8')
            # repair_text is pure; file writes only happen via main().
            before = tmx.read_text(encoding='utf-8')
            repair.repair_text(before)
            self.assertEqual(tmx.read_text(encoding='utf-8'), before)


class SyncCacheTest(unittest.TestCase):
    def test_cache_values_follow_repaired_segments(self) -> None:
        import xml.etree.ElementTree as ET  # noqa: F401
        with tempfile.TemporaryDirectory() as tmp:
            tmx = Path(tmp) / 'CmsUi.tmx'
            tmx.write_text(RepairTextTest.TMX, encoding='utf-8')
            new_text, _, _ = repair.repair_text(
                RepairTextTest.TMX)
            tmx.write_text(new_text, encoding='utf-8')
            cache = {
                repair.cache_key('Password', 'te'):
                    'పాస్u200cవర్డ్',
                repair.cache_key('Total', 'pt'):
                    'Totais u200bu200bgerais',
            }
            updated = repair.sync_cache_for_file(tmx, cache)
            self.assertEqual(updated, 2)
            self.assertEqual(cache[repair.cache_key('Password', 'te')],
                             'పాస్' + '\u200c' + 'వర్డ్')
            self.assertEqual(cache[repair.cache_key('Total', 'pt')],
                             'Totais gerais')


if __name__ == '__main__':
    unittest.main()
