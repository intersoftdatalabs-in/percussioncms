#!/usr/bin/env python3
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
"""Unit tests for i18n_lou_draft.py single-quoted tuid handling.

Invoke from the repository root::

    python3 modules/perc-i18n/scripts/test_i18n_lou_draft.py
"""
from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIR))

import i18n_lou_draft as lou  # noqa: E402


class SingleQuotedTuidTest(unittest.TestCase):
    """Tuids containing double quotes are single-quoted in raw TMX."""

    SAMPLE = (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<tmx version="1.4"><header></header><body>'
        "<tu tuid='arch@Delete section \"{0}\"?'>"
        '<tuv xml:lang="en-us"><seg>Delete section "{0}"?</seg></tuv>'
        "</tu>"
        "</body></tmx>\n"
    )

    def test_list_en_finds_single_quoted_tuid(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.tmx"
            p.write_text(self.SAMPLE, encoding="utf-8")
            tmx = lou.TmxFile(p)
            self.assertEqual(
                tmx.list_en(),
                [('arch@Delete section "{0}"?', 'Delete section "{0}"?')],
            )

    def test_inject_single_quoted_tuid(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.tmx"
            p.write_text(self.SAMPLE, encoding="utf-8")
            tmx = lou.TmxFile(p)
            n = tmx.inject({'arch@Delete section "{0}"?': "Efase section"}, False)
            self.assertEqual(n, 1)
            self.assertIn('<tuv xml:lang="lou"><seg>Efase section</seg></tuv>', tmx.text)


if __name__ == "__main__":
    unittest.main()
