#!/usr/bin/env python3
"""Unit tests for resolve_tmx_conflicts.py (no git / Docker required).

Discovered by pytest via ``scripts/run-python-tests.{sh,cmd}`` (spec 994 gate
includes ``modules/perc-i18n/scripts/``). Also runnable directly::

    python3 modules/perc-i18n/scripts/test_resolve_tmx_conflicts.py
"""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
if str(SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPTS_DIR))

from resolve_tmx_conflicts import resolve  # noqa: E402


def _wrap(body: str) -> str:
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<tmx version="1.4"><header></header><body>\n'
        f'{body}\n'
        '</body></tmx>\n'
    )


class ResolveTmxConflictsTest(unittest.TestCase):
    def test_simple_hunk_unions_langs(self) -> None:
        text = _wrap(
            '''
        <tu tuid="k1">
            <tuv xml:lang="en-us"><seg>Hi</seg></tuv>
<<<<<<< Updated upstream
            <tuv xml:lang="fr"><seg>Bonjour</seg></tuv>
</tu>
=======
            <tuv xml:lang="de"><seg>Hallo</seg></tuv>
</tu>
>>>>>>> Stashed changes
'''
        )
        out, hunks, warnings, unresolved = resolve(text, 't.tmx')
        self.assertEqual(hunks, 1)
        self.assertEqual(unresolved, -1)
        self.assertIn('xml:lang="fr"', out)
        self.assertIn('xml:lang="de"', out)
        self.assertIn('xml:lang="en-us"', out)
        self.assertNotIn('<<<<<<<', out)

    def test_lang_collision_keeps_ours(self) -> None:
        text = _wrap(
            '''
        <tu tuid="k1">
            <tuv xml:lang="en-us"><seg>Hi</seg></tuv>
<<<<<<< Updated upstream
            <tuv xml:lang="fr"><seg>Salut</seg></tuv>
</tu>
=======
            <tuv xml:lang="fr"><seg>Bonjour</seg></tuv>
</tu>
>>>>>>> Stashed changes
'''
        )
        out, hunks, warnings, unresolved = resolve(text, 't.tmx')
        self.assertEqual(hunks, 1)
        self.assertEqual(unresolved, -1)
        self.assertEqual(out.count('xml:lang="fr"'), 1)
        self.assertIn('Bonjour', out)
        self.assertNotIn('Salut', out)
        self.assertTrue(
            any("lang='fr'" in w or 'lang="fr"' in w or "lang=fr" in w
                or "lang='fr'" in w or "keeping Stashed changes" in w
                for w in warnings),
            msg=f'expected collision warning, got {warnings!r}',
        )

    def test_structural_abort_leaves_markers(self) -> None:
        text = _wrap(
            '''
<<<<<<< Updated upstream
        <tu tuid="k-a"><tuv xml:lang="en-us"><seg>A</seg></tuv></tu>
=======
        <tu tuid="k-b"><tuv xml:lang="en-us"><seg>B</seg></tuv></tu>
>>>>>>> Stashed changes
'''
        )
        out, hunks, warnings, unresolved = resolve(
            text, 't.tmx', structural_strategy='abort'
        )
        self.assertEqual(hunks, 1)
        self.assertGreaterEqual(unresolved, 0)
        self.assertIn('<<<<<<<', out)
        self.assertTrue(any('structural' in w for w in warnings))

    def test_structural_theirs_keeps_upstream_tu(self) -> None:
        text = _wrap(
            '''
<<<<<<< Updated upstream
        <tu tuid="k-a"><tuv xml:lang="en-us"><seg>A</seg></tuv></tu>
=======
        <tu tuid="k-b"><tuv xml:lang="en-us"><seg>B</seg></tuv></tu>
>>>>>>> Stashed changes
'''
        )
        out, hunks, _, unresolved = resolve(
            text, 't.tmx', structural_strategy='theirs'
        )
        self.assertEqual(hunks, 1)
        self.assertEqual(unresolved, -1)
        self.assertIn('tuid="k-a"', out)
        self.assertNotIn('tuid="k-b"', out)
        self.assertNotIn('<<<<<<<', out)

    def test_structural_ours_keeps_stash_tu(self) -> None:
        text = _wrap(
            '''
<<<<<<< Updated upstream
        <tu tuid="k-a"><tuv xml:lang="en-us"><seg>A</seg></tuv></tu>
=======
        <tu tuid="k-b"><tuv xml:lang="en-us"><seg>B</seg></tuv></tu>
>>>>>>> Stashed changes
'''
        )
        out, hunks, _, unresolved = resolve(
            text, 't.tmx', structural_strategy='ours'
        )
        self.assertEqual(hunks, 1)
        self.assertEqual(unresolved, -1)
        self.assertIn('tuid="k-b"', out)
        self.assertNotIn('tuid="k-a"', out)
        self.assertNotIn('<<<<<<<', out)

    def test_no_markers_passthrough(self) -> None:
        text = _wrap(
            '<tu tuid="k1"><tuv xml:lang="en-us"><seg>Hi</seg></tuv></tu>'
        )
        out, hunks, warnings, unresolved = resolve(text, 't.tmx')
        self.assertEqual(hunks, 0)
        self.assertEqual(unresolved, -1)
        self.assertEqual(out, text)
        self.assertEqual(warnings, [])


if __name__ == '__main__':
    unittest.main()
