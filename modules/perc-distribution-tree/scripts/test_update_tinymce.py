#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for update-tinymce.py (no network, no Maven)."""

from __future__ import annotations

import importlib.util
import logging
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "update-tinymce.py"
    name = "update_tinymce"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


ut = _load()


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            ut.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_unknown_arg_exits_two(self):
        with self.assertRaises(SystemExit) as cm:
            ut.main(["--not-a-flag"])
        self.assertEqual(cm.exception.code, 2)


class TestDefaultPaths(unittest.TestCase):
    def test_default_paths_resolve_to_perc_tinymce_module(self):
        fake_script = SCRIPTS / "update-tinymce.py"
        source, target = ut._default_paths(fake_script)
        self.assertTrue(
            str(source).endswith("modules/perc-tinymce/src/main/tinymce")
        )
        self.assertTrue(
            str(target).endswith("modules/perc-tinymce/src/main/resources/tinymce")
        )


class TestSync(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.td_path = Path(self.td.name)
        # Source tree with a few files + a nested subdir.
        self.source = self.td_path / "src"
        self.source.mkdir(parents=True, exist_ok=True)
        (self.source / "tinymce.js").write_text("v1", encoding="utf-8")
        (self.source / "themes").mkdir(parents=True, exist_ok=True)
        (self.source / "themes" / "modern.js").write_text("theme-v1", encoding="utf-8")
        # Pre-existing target with a stale file the operator placed manually.
        self.target = self.td_path / "tgt"
        self.target.mkdir(parents=True, exist_ok=True)
        (self.target / "manual-keep.js").write_text("operator file", encoding="utf-8")
        # Suppress logging noise.
        logging.getLogger("update-tinymce").setLevel(logging.CRITICAL)

    def test_sync_copies_all_files_recursively(self):
        rc = ut.sync(self.source, self.target)
        self.assertEqual(rc, ut.EXIT_OK)
        self.assertEqual((self.target / "tinymce.js").read_text(encoding="utf-8"), "v1")
        self.assertEqual(
            (self.target / "themes" / "modern.js").read_text(encoding="utf-8"),
            "theme-v1",
        )

    def test_sync_preserves_operator_placed_files(self):
        """``dirs_exist_ok=True`` ensures files already in target that are
        not in source are NOT removed.
        """
        rc = ut.sync(self.source, self.target)
        self.assertEqual(rc, ut.EXIT_OK)
        self.assertTrue((self.target / "manual-keep.js").is_file())

    def test_sync_overwrites_stale_files(self):
        """A file present in both source and target is overwritten with
        the source version.
        """
        (self.target / "tinymce.js").write_text("STALE", encoding="utf-8")
        rc = ut.sync(self.source, self.target)
        self.assertEqual(rc, ut.EXIT_OK)
        self.assertEqual((self.target / "tinymce.js").read_text(encoding="utf-8"), "v1")

    def test_sync_missing_source_exits_one(self):
        rc = ut.sync(self.td_path / "no-such", self.target)
        self.assertEqual(rc, ut.EXIT_INVOCATION)

    def test_sync_creates_missing_target(self):
        fresh_target = self.td_path / "fresh-tgt"
        rc = ut.sync(self.source, fresh_target)
        self.assertEqual(rc, ut.EXIT_OK)
        self.assertTrue(fresh_target.is_dir())
        self.assertEqual(
            (fresh_target / "tinymce.js").read_text(encoding="utf-8"), "v1"
        )


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            ut.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_main_missing_source_exits_one(self):
        with tempfile.TemporaryDirectory() as td:
            rc = ut.main(["--source", str(Path(td) / "no-such")])
            self.assertEqual(rc, ut.EXIT_INVOCATION)


if __name__ == "__main__":
    logging.getLogger("update-tinymce").setLevel(logging.CRITICAL)
    unittest.main()