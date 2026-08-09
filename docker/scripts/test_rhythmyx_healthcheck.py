#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for rhythmyx_healthcheck.py (#2481)."""

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    # Ensure sibling rhythmyx_ready is importable the same way the CLI does.
    if str(SCRIPTS) not in sys.path:
        sys.path.insert(0, str(SCRIPTS))
    path = SCRIPTS / "rhythmyx_healthcheck.py"
    name = "rhythmyx_healthcheck"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


hc = _load()


class BuildProbeUrlTests(unittest.TestCase):
    def test_cms_default(self):
        self.assertEqual(
            hc.build_probe_url(product="cms"),
            "http://127.0.0.1:9992/Rhythmyx/login",
        )

    def test_cms_custom_port_path(self):
        self.assertEqual(
            hc.build_probe_url(
                product="cms",
                cms_port="19111",
                cms_path="/Rhythmyx/rest/ping",
            ),
            "http://127.0.0.1:19111/Rhythmyx/rest/ping",
        )

    def test_dts_root(self):
        self.assertEqual(
            hc.build_probe_url(product="dts", dts_port="9980"),
            "http://127.0.0.1:9980/",
        )

    def test_url_override_wins(self):
        self.assertEqual(
            hc.build_probe_url(
                product="cms",
                url_override="http://127.0.0.1:9/x",
            ),
            "http://127.0.0.1:9/x",
        )


class DiscoverAndReadLogsTests(unittest.TestCase):
    def test_missing_install_root(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td) / "missing"
            self.assertEqual(hc.discover_jetty_log_paths(root), [])
            self.assertEqual(hc.collect_log_text(root), "")

    def test_reads_jetty_log_tail(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            log_dir = root / "jetty" / "base" / "logs"
            log_dir.mkdir(parents=True)
            log_path = log_dir / "jetty.log"
            body = "INFO Server Started\nWARN Failed startup of context Rhythmyx\n"
            log_path.write_text(body, encoding="utf-8")
            paths = hc.discover_jetty_log_paths(root)
            self.assertEqual(paths, [log_path])
            text = hc.collect_log_text(root)
            self.assertIn("Failed startup of context", text)

    def test_read_log_tail_truncates(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "big.log"
            path.write_bytes(b"A" * 1000 + b"MARKER")
            text = hc.read_log_tail(path, max_bytes=10)
            self.assertTrue(text.endswith("MARKER") or "MARKER" in text)
            self.assertLessEqual(len(text.encode("utf-8")), 10)


class AssessContainerHealthTests(unittest.TestCase):
    def test_cms_http_ready_clean_logs(self):
        ok, detail = hc.assess_container_health(
            product="cms",
            http_code=200,
            log_text="INFO [Server] Started",
        )
        self.assertTrue(ok)
        self.assertEqual(detail, "ok")

    def test_cms_http_ready_but_context_failed(self):
        ok, detail = hc.assess_container_health(
            product="cms",
            http_code=200,
            log_text="WARN [WebAppContext] Failed startup of context Rhythmyx",
        )
        self.assertFalse(ok)
        self.assertIn(hc.DETAIL_CONTEXT_FAILED, detail)

    def test_cms_http_not_ready(self):
        ok, detail = hc.assess_container_health(
            product="cms",
            http_code=0,
            log_text="",
        )
        self.assertFalse(ok)
        self.assertIn("http_not_ready", detail)

    def test_cms_circular_marker(self):
        ok, detail = hc.assess_container_health(
            product="cms",
            http_code=302,
            log_text="BeanCurrentlyInCreationException: folderHelper",
        )
        self.assertFalse(ok)
        self.assertIn(hc.DETAIL_CONTEXT_FAILED, detail)

    def test_dts_any_http_ok(self):
        ok, detail = hc.assess_container_health(
            product="dts",
            http_code=404,
            log_text="BeanCurrentlyInCreationException",  # ignored for dts
        )
        self.assertTrue(ok)
        self.assertIn("dts_http", detail)

    def test_dts_unreachable(self):
        ok, detail = hc.assess_container_health(
            product="dts",
            http_code=0,
            log_text="",
        )
        self.assertFalse(ok)
        self.assertIn("dts_http_not_ready", detail)


class RunHealthcheckTests(unittest.TestCase):
    def test_healthy_exit_with_overrides(self):
        code, detail = hc.run_healthcheck(
            product="cms",
            http_code_override=200,
            log_text_override="Server Started cleanly",
        )
        self.assertEqual(code, hc.EXIT_HEALTHY)
        self.assertEqual(detail, "ok")

    def test_unhealthy_on_context_fail_even_if_http_ok(self):
        code, detail = hc.run_healthcheck(
            product="cms",
            http_code_override=200,
            log_text_override=(
                "WARN [WebAppContext] Failed startup of context oeje11w"
            ),
        )
        self.assertEqual(code, hc.EXIT_UNHEALTHY)
        self.assertIn(hc.DETAIL_CONTEXT_FAILED, detail)

    def test_unhealthy_http_not_ready(self):
        code, detail = hc.run_healthcheck(
            product="cms",
            http_code_override=0,
            log_text_override="",
        )
        self.assertEqual(code, hc.EXIT_UNHEALTHY)
        self.assertIn("http_not_ready", detail)

    def test_reads_real_log_files_with_http_override(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            log_dir = root / "jetty" / "base" / "logs"
            log_dir.mkdir(parents=True)
            (log_dir / "server.log").write_text(
                "Caused by: org.springframework.beans.factory."
                "BeanCurrentlyInCreationException: folderHelper\n",
                encoding="utf-8",
            )
            code, detail = hc.run_healthcheck(
                product="cms",
                install_root=root,
                http_code_override=302,
            )
            self.assertEqual(code, hc.EXIT_UNHEALTHY)
            self.assertIn(hc.DETAIL_CONTEXT_FAILED, detail)
            self.assertIn("BeanCurrentlyInCreationException", detail)

    def test_main_prints_detail_and_exits_unhealthy(self):
        # main() with env-free argv using only pure overrides via run path:
        # exercise main CLI lightly with --product dts and unreachable port.
        rc = hc.main(
            [
                "--product",
                "dts",
                "--dts-port",
                "1",
                "--timeout",
                "0.2",
            ]
        )
        self.assertEqual(rc, hc.EXIT_UNHEALTHY)

    def test_is_http_ready_reexport_path(self):
        # Sanity: healthcheck module exposes shared constant used in docs.
        self.assertTrue(hc.is_http_ready_code(200))
        self.assertFalse(hc.is_http_ready_code(503))


if __name__ == "__main__":
    unittest.main()
