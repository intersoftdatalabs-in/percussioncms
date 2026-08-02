#!/usr/bin/env python3
"""Unit tests for generate-third-party-inventory.py (issue #1689)."""

from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

# Load hyphenated sibling script as a module (stdlib only).
import sys

SCRIPTS = Path(__file__).resolve().parent
_SPEC = importlib.util.spec_from_file_location(
    "generate_third_party_inventory",
    SCRIPTS / "generate-third-party-inventory.py",
)
assert _SPEC and _SPEC.loader
g = importlib.util.module_from_spec(_SPEC)
# dataclasses frozen/order needs the module registered before exec_module
sys.modules[_SPEC.name] = g
_SPEC.loader.exec_module(g)


class CollectNpmFromLockTest(unittest.TestCase):
    def test_production_packages_include_react_exclude_dev(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            lock_dir = root / "frontend"
            lock_dir.mkdir()
            lock = {
                "lockfileVersion": 3,
                "packages": {
                    "": {"name": "app", "version": "1.0.0"},
                    "node_modules/react": {
                        "version": "19.2.8",
                        "license": "MIT",
                    },
                    "node_modules/vitest": {
                        "version": "4.1.0",
                        "license": "MIT",
                        "dev": True,
                    },
                    "node_modules/@scope/pkg": {
                        "version": "1.2.3",
                        "license": "Apache-2.0",
                    },
                },
            }
            lock_path = lock_dir / "package-lock.json"
            lock_path.write_text(json.dumps(lock), encoding="utf-8")

            pkgs = g.collect_npm_from_lock(lock_path, root)
            names = {p.name for p in pkgs}
            self.assertIn("react", names)
            self.assertIn("@scope/pkg", names)
            self.assertNotIn("vitest", names)
            react = next(p for p in pkgs if p.name == "react")
            self.assertEqual(react.version, "19.2.8")
            self.assertEqual(react.license, "MIT")
            self.assertIn("npm:react:19.2.8", react.format_line())


class MergeInventoriesTest(unittest.TestCase):
    def test_merge_contains_both_sections(self) -> None:
        maven = "Lists of 1 third-party dependencies.\n     (MIT) foo (g:a:1 - )"
        npm = "Lists of 1 third-party npm dependencies (production).\n     (MIT) react (npm:react:1 - x)"
        merged = g.merge_inventories(maven, npm)
        self.assertIn("Maven third-party dependencies", merged)
        self.assertIn("npm third-party dependencies (production)", merged)
        self.assertIn("foo", merged)
        self.assertIn("react", merged)
        self.assertIn("do not hand-edit", merged)


class GenerateEndToEndTest(unittest.TestCase):
    def test_generate_writes_merged_file(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            out = root / "out"
            # Minimal maven half
            out.mkdir()
            (out / g.DEFAULT_MAVEN_NAME).write_text(
                "Lists of 1 third-party dependencies.\n     (Apache License, Version 2.0) guava\n",
                encoding="utf-8",
            )
            # Lock list + lock
            lock_dir = root / "ui"
            lock_dir.mkdir()
            lock = {
                "lockfileVersion": 3,
                "packages": {
                    "": {},
                    "node_modules/jquery": {"version": "3.7.1", "license": "MIT"},
                },
            }
            (lock_dir / "package-lock.json").write_text(json.dumps(lock), encoding="utf-8")
            list_file = root / "locks.txt"
            list_file.write_text("ui/package-lock.json\n", encoding="utf-8")

            merged = g.generate(
                root=root,
                out_dir=out,
                lock_list=list_file,
                require_maven=True,
            )
            text = merged.read_text(encoding="utf-8")
            self.assertIn("guava", text)
            self.assertIn("jquery", text)
            self.assertIn("npm:jquery:3.7.1", text)
            self.assertTrue((out / g.DEFAULT_NPM_INTERMEDIATE).is_file())


if __name__ == "__main__":
    unittest.main()
