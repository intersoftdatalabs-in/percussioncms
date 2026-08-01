"""Unit tests for i18n_cache.py and resolve_i18n_cache_conflicts.py."""
from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
if str(SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPTS_DIR))

import i18n_cache as ic  # noqa: E402
import resolve_i18n_cache_conflicts as resolve_mod  # noqa: E402


class UnionCachesTest(unittest.TestCase):
    def test_union_adds_missing_keys(self) -> None:
        stats: dict[str, int] = {}
        out = ic.union_caches({'a': '1'}, {'b': '2'}, stats=stats)
        self.assertEqual(out, {'a': '1', 'b': '2'})
        self.assertEqual(stats.get('keys_added'), 1)

    def test_union_ours_wins_on_collision(self) -> None:
        stats: dict[str, int] = {}
        out = ic.union_caches({'a': 'theirs'}, {'a': 'ours'}, stats=stats)
        self.assertEqual(out['a'], 'ours')
        self.assertEqual(stats.get('key_collisions'), 1)

    def test_union_same_value_not_collision(self) -> None:
        stats: dict[str, int] = {}
        out = ic.union_caches({'a': 'x'}, {'a': 'x'}, stats=stats)
        self.assertEqual(out['a'], 'x')
        self.assertEqual(stats.get('key_collisions', 0), 0)
        self.assertEqual(stats.get('key_same'), 1)


class ConflictResolveTest(unittest.TestCase):
    def test_no_markers_round_trip(self) -> None:
        body = ic.format_cache({'k1': 'v1', 'k2': 'v2'})
        text, merged = ic.resolve_conflicted_cache_text(body)
        self.assertEqual(merged, {'k1': 'v1', 'k2': 'v2'})
        self.assertEqual(json.loads(text), merged)

    def test_markers_union_both_sides(self) -> None:
        # Sorted keys: shared prefix/suffix with a mid-file conflict.
        text = (
            '{\n'
            '  "aaa": "1",\n'
            '<<<<<<< Updated upstream\n'
            '  "bbb": "theirs",\n'
            '=======\n'
            '  "ccc": "ours",\n'
            '>>>>>>> Stashed changes\n'
            '  "ddd": "4"\n'
            '}\n'
        )
        stats: dict[str, int] = {}
        out_text, merged = ic.resolve_conflicted_cache_text(
            text, filename='t.json', stats=stats
        )
        self.assertEqual(merged['aaa'], '1')
        self.assertEqual(merged['bbb'], 'theirs')
        self.assertEqual(merged['ccc'], 'ours')
        self.assertEqual(merged['ddd'], '4')
        self.assertNotIn('<<<<<<<', out_text)
        self.assertEqual(stats.get('hunks'), 1)

    def test_markers_collision_keeps_ours(self) -> None:
        text = (
            '{\n'
            '<<<<<<< Updated upstream\n'
            '  "k": "theirs"\n'
            '=======\n'
            '  "k": "ours"\n'
            '>>>>>>> Stashed changes\n'
            '}\n'
        )
        stats: dict[str, int] = {}
        _, merged = ic.resolve_conflicted_cache_text(text, stats=stats)
        self.assertEqual(merged, {'k': 'ours'})
        self.assertEqual(stats.get('key_collisions'), 1)


class LoadSaveTest(unittest.TestCase):
    def test_save_load_round_trip(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'cache' / 'i18n_translate.json'
            data = {'abc': '你好', 'def': 'Hallo'}
            ic.save_cache(data, path)
            loaded = ic.load_cache(path)
            self.assertEqual(loaded, data)
            # Stable formatting: sorted keys, trailing newline
            raw = path.read_text(encoding='utf-8')
            self.assertTrue(raw.endswith('\n'))
            self.assertIn('"abc"', raw)

    def test_load_migrates_legacy_into_canonical(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            canonical = root / 'cache' / 'i18n_translate.json'
            legacy_dir = root / '.cache'
            legacy_dir.mkdir()
            legacy = legacy_dir / 'i18n_translate_direct.json'
            legacy.write_text(
                json.dumps({'legacy-only': 'L', 'shared': 'old'}, ensure_ascii=False),
                encoding='utf-8',
            )
            orig_legacy = ic.LEGACY_CACHE_FILES
            try:
                ic.LEGACY_CACHE_FILES = (legacy,)  # type: ignore[misc]
                # Explicit path + migrate_legacy so we do not touch the real
                # checked-in cache file during the unit test.
                loaded = ic.load_cache(canonical, migrate_legacy=True)
                self.assertEqual(loaded['legacy-only'], 'L')
                self.assertTrue(canonical.exists())
            finally:
                ic.LEGACY_CACHE_FILES = orig_legacy  # type: ignore[misc]

    def test_load_explicit_path_skips_legacy_by_default(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'cache.json'
            path.write_text(ic.format_cache({'only': 'here'}), encoding='utf-8')
            loaded = ic.load_cache(path)
            self.assertEqual(loaded, {'only': 'here'})


class ResolveCliTest(unittest.TestCase):
    def test_resolve_file_writes_union(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'i18n_translate.json'
            path.write_text(
                '{\n'
                '<<<<<<< Updated upstream\n'
                '  "a": "1"\n'
                '=======\n'
                '  "b": "2"\n'
                '>>>>>>> Stashed changes\n'
                '}\n',
                encoding='utf-8',
            )
            stats = resolve_mod.resolve_file(path)
            self.assertEqual(stats.get('hunks'), 1)
            data = json.loads(path.read_text(encoding='utf-8'))
            self.assertEqual(data, {'a': '1', 'b': '2'})

    def test_merge_files_union(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            t = Path(tmp) / 'theirs.json'
            o = Path(tmp) / 'ours.json'
            out = Path(tmp) / 'out.json'
            t.write_text(ic.format_cache({'a': '1', 'c': 'old'}), encoding='utf-8')
            o.write_text(ic.format_cache({'b': '2', 'c': 'new'}), encoding='utf-8')
            stats = resolve_mod.merge_files(t, o, out_path=out)
            data = json.loads(out.read_text(encoding='utf-8'))
            self.assertEqual(data, {'a': '1', 'b': '2', 'c': 'new'})
            self.assertEqual(stats.get('key_collisions'), 1)

    def test_resolve_file_dry_run_does_not_write(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'i18n_translate.json'
            original = (
                '{\n'
                '<<<<<<< Updated upstream\n'
                '  "a": "1"\n'
                '=======\n'
                '  "b": "2"\n'
                '>>>>>>> Stashed changes\n'
                '}\n'
            )
            path.write_text(original, encoding='utf-8')
            stats = resolve_mod.resolve_file(path, dry_run=True)
            self.assertEqual(stats.get('hunks'), 1)
            self.assertEqual(path.read_text(encoding='utf-8'), original)

    def test_main_resolve_exit_0(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'i18n_translate.json'
            path.write_text(
                '{\n'
                '<<<<<<< Updated upstream\n'
                '  "a": "1"\n'
                '=======\n'
                '  "b": "2"\n'
                '>>>>>>> Stashed changes\n'
                '}\n',
                encoding='utf-8',
            )
            rc = resolve_mod.main([str(path)])
            self.assertEqual(rc, 0)
            data = json.loads(path.read_text(encoding='utf-8'))
            self.assertEqual(data, {'a': '1', 'b': '2'})

    def test_main_missing_file_exit_1(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            missing = Path(tmp) / 'does-not-exist.json'
            rc = resolve_mod.main([str(missing)])
            self.assertEqual(rc, 1)

    def test_main_merge_flag(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            t = Path(tmp) / 'theirs.json'
            o = Path(tmp) / 'ours.json'
            out = Path(tmp) / 'out.json'
            t.write_text(ic.format_cache({'x': '1'}), encoding='utf-8')
            o.write_text(ic.format_cache({'y': '2'}), encoding='utf-8')
            rc = resolve_mod.main(
                ['--merge', str(t), str(o), '--out', str(out)]
            )
            self.assertEqual(rc, 0)
            self.assertEqual(
                json.loads(out.read_text(encoding='utf-8')),
                {'x': '1', 'y': '2'},
            )


class MultiHunkConflictTest(unittest.TestCase):
    def test_two_hunks_union(self) -> None:
        text = (
            '{\n'
            '<<<<<<< Updated upstream\n'
            '  "a": "1",\n'
            '=======\n'
            '  "b": "2",\n'
            '>>>>>>> Stashed changes\n'
            '<<<<<<< Updated upstream\n'
            '  "c": "3"\n'
            '=======\n'
            '  "d": "4"\n'
            '>>>>>>> Stashed changes\n'
            '}\n'
        )
        stats: dict[str, int] = {}
        _, merged = ic.resolve_conflicted_cache_text(text, stats=stats)
        self.assertEqual(merged, {'a': '1', 'b': '2', 'c': '3', 'd': '4'})
        self.assertEqual(stats.get('hunks'), 2)


class PathSafetyTest(unittest.TestCase):
    """Guard rails: pathlib only for filesystem paths in cache tooling."""

    def test_no_os_path_joins_in_cache_modules(self) -> None:
        for name in (
            'i18n_cache.py',
            'resolve_i18n_cache_conflicts.py',
            'i18n_translate.py',
            'i18n_translate_direct.py',
        ):
            src = (SCRIPTS_DIR / name).read_text(encoding='utf-8')
            self.assertNotIn('os.path.join', src, msg=name)
            self.assertNotIn("os.path.", src, msg=name)


if __name__ == '__main__':
    unittest.main()
