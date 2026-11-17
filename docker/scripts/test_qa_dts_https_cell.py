#!/usr/bin/env python3
# -*- coding: utf-8 -*-
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
"""Unit tests for the --with-dts HTTPS publish contract (#6375).

Covers: ``matrix-install-smoke.build_docker_run_argv`` DTS HTTPS ``-p``
mapping (default ``None`` keeps argv byte-identical), the HTTPS host-port
resolve/ensure pairs on both entry points, ``perc-devctl`` HTTPS base URL
+ endpoint banner lines, and the CMS-only banner default staying
byte-identical. No docker, no network, no secrets — freeport/env only.
"""

from __future__ import annotations

import importlib.util
import io
import os
import sys
import unittest
from contextlib import redirect_stdout
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent

# Env keys the HTTPS host-port resolution reads or pins. Cleared around
# every test so the host environment never leaks into assertions.
_HTTPS_ENV_KEYS = (
    "DTS_HTTPS_HOST_PORT",
    "DTS_HTTPS_PORT",
)


def _clear_https_env() -> None:
    for key in _HTTPS_ENV_KEYS:
        os.environ.pop(key, None)


def _load(path: Path, name: str):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


smoke = _load(SCRIPTS / "matrix-install-smoke.py", "matrix_install_smoke_qahttps")
pdc = _load(SCRIPTS / "perc-devctl.py", "perc_devctl_qahttps")


def _argv_text(argv) -> str:
    return " ".join(str(a) for a in argv)


def _banner(**kwargs) -> str:
    buf = io.StringIO()
    with redirect_stdout(buf):
        pdc._qa_print_endpoint_banner(9993, **kwargs)
    return buf.getvalue()


class MatrixDtsHttpsArgvTests(unittest.TestCase):
    def test_container_https_port_constant(self):
        self.assertEqual(smoke.DTS_CONTAINER_HTTPS_PORT, 8443)
        self.assertEqual(smoke.PREFERRED_DTS_HTTPS_HOST_PORT, 8443)

    def test_dts_argv_publishes_https_when_given(self):
        argv = smoke.build_docker_run_argv(
            image=smoke.MATRIX_IMAGE_TAG,
            container_name="perc-matrix-dts-h2",
            product="dts",
            db_type="h2",
            installer_jar_host=Path("/tmp/fake-dts.jar"),
            host_port=9983,
            network=smoke.MATRIX_NETWORK,
            db_meta=dict(smoke.DB_SERVICES["h2"]),
            keep=True,
            https_host_port=8443,
        )
        joined = _argv_text(argv)
        self.assertIn("9983:9980", joined)
        self.assertIn("8443:8443", joined)
        self.assertEqual(argv.count("-p"), 2)

    def test_default_argv_has_no_https_publish(self):
        """Omitting the parameter keeps argv byte-identical to before."""
        argv = smoke.build_docker_run_argv(
            image=smoke.MATRIX_IMAGE_TAG,
            container_name="perc-matrix-dts-h2",
            product="dts",
            db_type="h2",
            installer_jar_host=Path("/tmp/fake-dts.jar"),
            host_port=9983,
            network=smoke.MATRIX_NETWORK,
            db_meta=dict(smoke.DB_SERVICES["h2"]),
            keep=True,
        )
        self.assertEqual(argv.count("-p"), 1)
        self.assertNotIn("8443", _argv_text(argv))

    def test_cms_argv_ignores_https(self):
        """The HTTPS publish applies to dts cells only."""
        argv = smoke.build_docker_run_argv(
            image=smoke.MATRIX_IMAGE_TAG,
            container_name="perc-matrix-cms-h2",
            product="cms",
            db_type="h2",
            installer_jar_host=Path("/tmp/fake-cms.jar"),
            host_port=9993,
            network=smoke.MATRIX_NETWORK,
            db_meta=dict(smoke.DB_SERVICES["h2"]),
            keep=True,
            https_host_port=8443,
        )
        self.assertEqual(argv.count("-p"), 1)
        self.assertNotIn("8443", _argv_text(argv))


class MatrixDtsHttpsPortTests(unittest.TestCase):
    def setUp(self):
        _clear_https_env()
        self.addCleanup(_clear_https_env)

    def test_resolve_env_override_first_key(self):
        os.environ["DTS_HTTPS_HOST_PORT"] = "18443"
        os.environ["DTS_HTTPS_PORT"] = "18444"
        self.assertEqual(smoke.resolve_matrix_dts_https_host_port(), 18443)

    def test_resolve_env_override_second_key(self):
        os.environ["DTS_HTTPS_PORT"] = "18444"
        self.assertEqual(smoke.resolve_matrix_dts_https_host_port(), 18444)

    def test_ensure_pins_both_keys(self):
        os.environ["DTS_HTTPS_HOST_PORT"] = "18445"
        port = smoke.ensure_matrix_dts_https_host_port()
        self.assertEqual(port, 18445)
        self.assertEqual(os.environ["DTS_HTTPS_HOST_PORT"], "18445")
        # Second key fills in when the operator did not pin it.
        self.assertEqual(os.environ["DTS_HTTPS_PORT"], "18445")

    def test_ensure_never_clobbers_explicit_second_key(self):
        os.environ["DTS_HTTPS_HOST_PORT"] = "18446"
        os.environ["DTS_HTTPS_PORT"] = "18447"
        smoke.ensure_matrix_dts_https_host_port()
        self.assertEqual(os.environ["DTS_HTTPS_PORT"], "18447")


class DevctlDtsHttpsPortTests(unittest.TestCase):
    def setUp(self):
        _clear_https_env()
        self.addCleanup(_clear_https_env)

    def test_constants(self):
        self.assertEqual(pdc.QA_DTS_CONTAINER_HTTPS_PORT, 8443)
        self.assertEqual(pdc.PREFERRED_QA_DTS_HTTPS_HOST_PORT, 8443)

    def test_https_base_url(self):
        self.assertEqual(
            pdc.qa_dts_https_base_url(18443), "https://127.0.0.1:18443"
        )

    def test_resolve_env_override(self):
        os.environ["DTS_HTTPS_HOST_PORT"] = "18448"
        self.assertEqual(pdc.resolve_qa_dts_https_host_port(), 18448)

    def test_ensure_pins_both_keys(self):
        os.environ["DTS_HTTPS_HOST_PORT"] = "18449"
        port = pdc.ensure_qa_dts_https_host_port()
        self.assertEqual(port, 18449)
        self.assertEqual(os.environ["DTS_HTTPS_HOST_PORT"], "18449")
        self.assertEqual(os.environ["DTS_HTTPS_PORT"], "18449")

    def test_banner_cms_only_ignores_https_kwarg(self):
        """No DTS cell: no DTS lines at all (default path byte-identical)."""
        out = _banner(dts_https_host_port=8443)
        self.assertNotIn("DTS_HTTPS", out)
        self.assertNotIn("DTS_URL", out)
        self.assertIn("TEST_CMS_URL=", out)

    def test_banner_dts_without_https_has_no_https_lines(self):
        out = _banner(dts_host_port=9983)
        self.assertIn("DTS_URL=http://127.0.0.1:9983", out)
        self.assertNotIn("DTS_HTTPS", out)

    def test_banner_dts_https_contract(self):
        out = _banner(dts_host_port=9983, dts_https_host_port=18443)
        self.assertIn("QA_DTS_HOST_PORT=9983", out)
        self.assertIn("QA_DTS_HTTPS_HOST_PORT=18443", out)
        self.assertIn("DTS_HTTPS_URL=https://127.0.0.1:18443", out)
        # Seed guidance names the provisioning step, never a secret.
        self.assertIn("qa-seed-widget-fixtures", out)
        self.assertIn("DTS_DELIVERY_PASSWORD", out)


if __name__ == "__main__":
    unittest.main()
