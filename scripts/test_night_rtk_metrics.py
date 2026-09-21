#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

See the License for the specific language governing permissions and
limitations under the License.

Unit tests for scripts/night-rtk-metrics.py (stdlib unittest, no pytest).
"""
from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

SCRIPT_DIR = Path(__file__).resolve().parent


def _load():
    path = SCRIPT_DIR / "night-rtk-metrics.py"
    spec = importlib.util.spec_from_file_location("night_rtk_metrics", path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(mod)
    return mod


night_rtk_metrics = _load()


class DeltaTests(unittest.TestCase):
    def test_delta_subtracts_summaries(self) -> None:
        before = {
            "ok": True,
            "summary": {
                "total_commands": 10,
                "total_saved": 1000,
                "total_input": 5000,
                "total_output": 2000,
                "avg_savings_pct": 20.0,
            },
        }
        after = {
            "ok": True,
            "summary": {
                "total_commands": 25,
                "total_saved": 4000,
                "total_input": 9000,
                "total_output": 3000,
                "avg_savings_pct": 40.0,
            },
        }
        d = night_rtk_metrics._delta(before, after)
        self.assertEqual(d["commands"], 15)
        self.assertEqual(d["tokens_saved"], 3000)
        self.assertEqual(d["input_chars"], 4000)
        self.assertEqual(d["output_chars"], 1000)
        self.assertEqual(d["avg_savings_pct_after"], 40.0)
        self.assertTrue(d["ok"])
        self.assertTrue(d["after_gain_ok"])

    def test_delta_missing_before_is_full_after(self) -> None:
        after = {
            "ok": True,
            "summary": {"total_commands": 3, "total_saved": 99},
        }
        d = night_rtk_metrics._delta({}, after)
        self.assertEqual(d["commands"], 3)
        self.assertEqual(d["tokens_saved"], 99)
        self.assertTrue(d["ok"])
        self.assertTrue(d["after_gain_ok"])
        self.assertFalse(d["before_gain_ok"])


class SnapshotTests(unittest.TestCase):
    def test_main_writes_snapshot_and_delta(self) -> None:
        fake_gain = {
            "ok": True,
            "summary": {
                "total_commands": 2,
                "total_saved": 50,
                "total_input": 100,
                "total_output": 10,
                "avg_savings_pct": 5.0,
            },
        }
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            before = tmp / "before.json"
            after = tmp / "after.json"
            out = tmp / "metrics.json"
            before.write_text(
                json.dumps(
                    {
                        "global": {
                            "ok": True,
                            "summary": {
                                "total_commands": 1,
                                "total_saved": 10,
                                "total_input": 20,
                                "total_output": 5,
                                "avg_savings_pct": 1.0,
                            },
                        },
                        "project": {
                            "ok": True,
                            "summary": {
                                "total_commands": 0,
                                "total_saved": 0,
                                "total_input": 0,
                                "total_output": 0,
                                "avg_savings_pct": 0.0,
                            },
                        },
                    }
                ),
                encoding="utf-8",
            )
            with mock.patch.object(
                night_rtk_metrics, "_run_rtk_gain", return_value=fake_gain
            ):
                rc = night_rtk_metrics.main(
                    [
                        "--snapshot",
                        str(after),
                        "--delta-from",
                        str(before),
                        "--out",
                        str(out),
                    ]
                )
            self.assertEqual(rc, 0)
            metrics = json.loads(out.read_text(encoding="utf-8"))
            self.assertEqual(metrics["global_delta"]["commands"], 1)
            self.assertEqual(metrics["global_delta"]["tokens_saved"], 40)
            self.assertTrue(metrics["global_delta"]["ok"])
            snap = json.loads(after.read_text(encoding="utf-8"))
            self.assertIn("captured_at", snap)
            self.assertTrue(snap["global"]["ok"])

    def test_rtk_missing_still_writes_snapshot_exit_0(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            snap = Path(td) / "before.json"
            with mock.patch.object(night_rtk_metrics.shutil, "which", return_value=None):
                rc = night_rtk_metrics.main(["--snapshot", str(snap)])
            self.assertEqual(rc, 0)
            payload = json.loads(snap.read_text(encoding="utf-8"))
            self.assertEqual(payload["global"]["error"], "rtk_not_on_path")
            self.assertFalse(payload["global"]["ok"])

    def test_delta_from_missing_file_writes_metrics(self) -> None:
        fake_gain = {
            "ok": True,
            "summary": {"total_commands": 1, "total_saved": 8},
        }
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            after = tmp / "after.json"
            missing = tmp / "no-before.json"
            out = tmp / "metrics.json"
            with mock.patch.object(
                night_rtk_metrics, "_run_rtk_gain", return_value=fake_gain
            ):
                rc = night_rtk_metrics.main(
                    [
                        "--snapshot",
                        str(after),
                        "--delta-from",
                        str(missing),
                        "--out",
                        str(out),
                    ]
                )
            self.assertEqual(rc, 0)
            metrics = json.loads(out.read_text(encoding="utf-8"))
            self.assertEqual(metrics["global_delta"]["tokens_saved"], 8)
            self.assertTrue(metrics["global_delta"]["ok"])


if __name__ == "__main__":
    unittest.main()
