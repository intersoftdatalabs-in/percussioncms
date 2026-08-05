#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for freeport-concurrent-smoke.py (#2006)."""

from __future__ import annotations

import importlib.util
import os
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load(path: Path, name: str):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    # Ensure sibling perc_host_ports is importable the same way as the script.
    import sys

    if str(SCRIPTS) not in sys.path:
        sys.path.insert(0, str(SCRIPTS))
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


smoke = _load(SCRIPTS / "freeport-concurrent-smoke.py", "freeport_concurrent_smoke")


class FreeportConcurrentSmokeTests(unittest.TestCase):
    def setUp(self):
        smoke._clear_port_env()
        self.addCleanup(smoke._clear_port_env)

    def test_run_smoke_ok(self):
        ok, lines = smoke.run_smoke()
        self.assertTrue(ok, msg="\n".join(lines))
        joined = "\n".join(lines)
        self.assertIn("OK cell-A preferred", joined)
        self.assertIn("OK cell-B freeport", joined)
        self.assertIn("OK env override", joined)
        self.assertIn("OK compose freeport", joined)
        self.assertIn("OK freeport concurrent smoke complete", joined)

    def test_main_prints_result_ok(self):
        # Capture via return code; main prints RESULT line.
        import io
        from contextlib import redirect_stdout

        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = smoke.main(["--quiet"])
        self.assertEqual(rc, smoke.EXIT_OK)
        self.assertIn(f"RESULT:OK STEP:{smoke.STEP}", buf.getvalue())

    def test_hold_port_makes_is_port_free_false(self):
        from perc_host_ports import find_free_port, is_port_free

        free = find_free_port()
        holder = smoke._hold_port(free)
        self.addCleanup(holder.close)
        self.assertFalse(is_port_free(free))
        holder.close()
        self.assertTrue(is_port_free(free))

    def test_clear_port_env_removes_keys(self):
        os.environ["QA_CMS_HOST_PORT"] = "1"
        os.environ["CMS_PORT"] = "2"
        smoke._clear_port_env()
        self.assertNotIn("QA_CMS_HOST_PORT", os.environ)
        self.assertNotIn("CMS_PORT", os.environ)


if __name__ == "__main__":
    unittest.main()
