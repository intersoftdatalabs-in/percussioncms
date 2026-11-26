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
"""Unit tests for the QA Apache frontend sidecar (issue #7323).

Covers: :mod:`qa_apache_cell` pure surface (container naming, port
resolution order, ``docker run`` argv shape, mkcert argv, cert-dir
idempotence, banner lines) and the static vhost conf parity between
the :80 and :443 virtual hosts. No docker, no network, no secrets —
freeport/env only.
"""

from __future__ import annotations

import importlib.util
import os
import re
import ssl
import sys
import tempfile
import unittest
import unittest.mock
import urllib.error
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent
if str(SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SCRIPTS))

# Env keys the Apache resolution reads or pins. Cleared around every
# test so the host environment never leaks into assertions.
_APACHE_ENV_KEYS = (
    "QA_APACHE_HTTP_PORT",
    "QA_APACHE_HTTPS_PORT",
    "QA_APACHE_HOSTNAME",
    "PERC_QA_CELL_SUFFIX",
)


def _clear_apache_env() -> None:
    for key in _APACHE_ENV_KEYS:
        os.environ.pop(key, None)


def _load(path: Path, name: str):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


qac = _load(SCRIPTS / "qa_apache_cell.py", "qa_apache_cell")


def _conf_text() -> str:
    conf = SCRIPTS.parent / "apache" / "qa-apache-vhost.conf"
    return conf.read_text(encoding="utf-8")


class ContainerNamingTests(unittest.TestCase):
    def setUp(self):
        _clear_apache_env()

    def tearDown(self):
        _clear_apache_env()

    def test_default_sidecar_name(self):
        self.assertEqual(qac.apache_container(), "perc-matrix-apache-h2")

    def test_suffix_scopes_sidecar(self):
        self.assertEqual(
            qac.apache_container("qa-a"), "perc-matrix-apache-h2-qa-a"
        )

    def test_default_hostname(self):
        self.assertEqual(
            qac.apache_hostname(), "qa-published.percussion.local"
        )

    def test_hostname_env_override(self):
        os.environ["QA_APACHE_HOSTNAME"] = "cell.example.test"
        self.assertEqual(qac.apache_hostname(), "cell.example.test")

    def test_cert_dir_default_and_suffix(self):
        repo = Path(tempfile.gettempdir()) / "repo"
        self.assertEqual(
            qac.apache_cert_dir(repo),
            repo / "docker" / "logs" / "qa-apache-certs",
        )
        self.assertEqual(
            qac.apache_cert_dir(repo, "qa-a"),
            repo / "docker" / "logs" / "qa-apache-certs-qa-a",
        )

    def test_service_contexts_cover_dts_wars(self):
        self.assertEqual(
            set(qac.DTS_SERVICE_CONTEXTS),
            {
                "perc-comments-services",
                "feeds",
                "perc-form-processor",
                "perc-membership-services",
                "perc-metadata-services",
                "perc-polls-services",
                "perc-secure-membership",
            },
        )


class PortResolutionTests(unittest.TestCase):
    def setUp(self):
        _clear_apache_env()

    def tearDown(self):
        _clear_apache_env()

    def test_env_wins(self):
        os.environ["QA_APACHE_HTTP_PORT"] = "18080"
        port, source = qac.resolve_qa_apache_host_port(Path.cwd(), "http")
        self.assertEqual((port, source), (18080, "env"))

    def test_env_wins_https(self):
        os.environ["QA_APACHE_HTTPS_PORT"] = "18443"
        port, source = qac.resolve_qa_apache_host_port(Path.cwd(), "https")
        self.assertEqual((port, source), (18443, "env"))

    def test_docker_publish_second(self):
        with unittest.mock.patch.object(
            qac, "_docker_port", return_value=19080
        ):
            port, source = qac.resolve_qa_apache_host_port(
                Path.cwd(), "http", container="some-cell"
            )
        self.assertEqual((port, source), (19080, "docker"))

    def test_qa_up_log_third(self):
        with unittest.mock.patch.object(
            qac, "_docker_port", return_value=None
        ), unittest.mock.patch.object(
            qac, "_latest_qa_up_log_apache_port", return_value=18081
        ):
            port, source = qac.resolve_qa_apache_host_port(Path.cwd(), "http")
        self.assertEqual((port, source), (18081, "qa-up-log"))

    def test_preferred_when_free(self):
        with unittest.mock.patch.object(
            qac, "_docker_port", return_value=None
        ), unittest.mock.patch.object(
            qac, "_latest_qa_up_log_apache_port", return_value=None
        ), unittest.mock.patch(
            "perc_host_ports.is_port_free", return_value=True
        ):
            port, source = qac.resolve_qa_apache_host_port(Path.cwd(), "http")
        self.assertEqual((port, source), (80, "preferred"))

    def test_log_line_parsing(self):
        body = (
            "QA_CMS_HOST_PORT=19993\n"
            "QA_APACHE_HTTP_PORT=18080\n"
            "QA_APACHE_HTTPS_PORT=18443\n"
        )
        self.assertEqual(qac._QA_UP_LOG_APACHE_HTTP_RE.findall(body), ["18080"])
        self.assertEqual(
            qac._QA_UP_LOG_APACHE_HTTPS_RE.findall(body), ["18443"]
        )


class RunArgvTests(unittest.TestCase):
    def test_argv_shape(self):
        argv = qac.build_apache_run_argv(
            container="perc-matrix-apache-h2",
            network="perc-matrix-net",
            http_port=18080,
            https_port=18443,
            conf_file=Path("/repo/docker/apache/qa-apache-vhost.conf"),
            site_dir=Path("/repo/tmp/qa-fixture-published/site"),
            cert_dir=Path("/repo/docker/logs/qa-apache-certs"),
            dts_backend="perc-matrix-dts-h2",
            hostname="qa-published.percussion.local",
        )
        text = " ".join(argv)
        self.assertEqual(argv[:7], [
            "docker", "run", "-d",
            "--name", "perc-matrix-apache-h2",
            "--network", "perc-matrix-net",
        ])
        self.assertIn("18080:80", text)
        self.assertIn("18443:443", text)
        # Read-only bind mounts: conf, docroot, certs.
        # mkd-code-review[paths.hardcoded_sep]: adjudicated FP (issue #7323,
        # precedent #6570) — Linux-cell-internal mount paths in test
        # assertion literals, never host path joins.
        self.assertIn(
            "/repo/docker/apache/qa-apache-vhost.conf:"
            "/usr/local/apache2/conf/qa-cell.conf:ro",
            text,
        )
        self.assertIn(
            "/repo/tmp/qa-fixture-published/site:"
            "/usr/local/apache2/htdocs/site:ro",
            text,
        )
        self.assertIn("/repo/docker/logs/qa-apache-certs:/certs:ro", text)
        # Stock image + foreground + Include + env defines.
        self.assertIn("httpd:2.4", text)
        self.assertIn("httpd-foreground", text)
        self.assertIn("DTS_BACKEND=perc-matrix-dts-h2", text)
        self.assertIn("APACHE_HOSTNAME=qa-published.percussion.local", text)


class MkcertTests(unittest.TestCase):
    def setUp(self):
        _clear_apache_env()
        # Hermetic binary name: the host PATH may resolve a shim path.
        patcher = unittest.mock.patch.object(
            qac, "mkcert_binary", return_value="mkcert"
        )
        patcher.start()
        self.addCleanup(patcher.stop)

    def tearDown(self):
        _clear_apache_env()

    def test_issue_argv_sans(self):
        argv = qac.mkcert_issue_argv(
            Path("/c/apache-cert.pem"), Path("/c/apache-key.pem"),
            "qa-published.percussion.local",
        )
        self.assertEqual(argv[:5], [
            "mkcert", "-cert-file", "/c/apache-cert.pem",
            "-key-file", "/c/apache-key.pem",
        ])
        for san in (
            "qa-published.percussion.local", "127.0.0.1", "localhost", "::1"
        ):
            self.assertIn(san, argv)

    def test_missing_mkcert_fails_with_hint(self):
        with unittest.mock.patch.object(
            qac, "mkcert_available", return_value=False
        ):
            ok, detail = qac.ensure_apache_certs(
                "qa-published.percussion.local", Path(tempfile.gettempdir())
            )
        self.assertFalse(ok)
        self.assertIn("mkcert_not_found", detail)

    def test_reuse_existing_certs_refreshes_ca(self):
        with tempfile.TemporaryDirectory() as tmp:
            cert_dir = Path(tmp)
            (cert_dir / qac.APACHE_CERT_FILE).write_text("cert")
            (cert_dir / qac.APACHE_KEY_FILE).write_text("key")
            caroot = Path(tmp) / "caroot"
            caroot.mkdir()
            (caroot / "rootCA.pem").write_text("ca")
            completed = unittest.mock.Mock(returncode=0, stdout=f"{caroot}\n")
            with unittest.mock.patch.object(
                qac, "mkcert_available", return_value=True
            ), unittest.mock.patch.object(
                qac.subprocess, "run", return_value=completed
            ) as run_mock:
                ok, detail = qac.ensure_apache_certs("h", cert_dir)
            self.assertTrue(ok, detail)
            # Existing cert/key reused: no mkcert issue call.
            self.assertEqual(run_mock.call_count, 1)
            self.assertEqual(
                run_mock.call_args[0][0], ["mkcert", "-CAROOT"]
            )
            self.assertEqual(
                (cert_dir / qac.APACHE_CA_FILE).read_text(), "ca"
            )


class ProbeHelperTests(unittest.TestCase):
    def test_http_ok_with_server(self):
        resp = unittest.mock.MagicMock()
        resp.status = 200
        resp.headers = {"Server": "Apache/2.4.62 (Unix)"}
        resp.__enter__.return_value = resp
        with unittest.mock.patch.object(
            qac.urllib.request, "urlopen", return_value=resp
        ):
            self.assertEqual(
                qac.http_get_status_and_server("http://127.0.0.1:1/"),
                (200, "Apache/2.4.62 (Unix)"),
            )

    def test_http_error_code_returned(self):
        err = urllib.error.HTTPError(
            "http://127.0.0.1:1/", 404, "nf", {"Server": "x"}, None
        )
        with unittest.mock.patch.object(
            qac.urllib.request, "urlopen", side_effect=err
        ):
            self.assertEqual(
                qac.http_get_status_and_server("http://127.0.0.1:1/")[0], 404
            )

    def test_http_network_failure_is_zero(self):
        with unittest.mock.patch.object(
            qac.urllib.request, "urlopen",
            side_effect=urllib.error.URLError("refused"),
        ):
            self.assertEqual(
                qac.http_get_status_and_server("http://127.0.0.1:1/"), (0, "")
            )

    def test_https_verifies_against_ca(self):
        resp = unittest.mock.MagicMock()
        resp.status = 200
        resp.headers = {"Server": "Apache/2.4.62 (Unix)"}
        resp.__enter__.return_value = resp
        with tempfile.TemporaryDirectory() as tmp:
            ca = Path(tmp) / "rootCA.pem"
            ca.write_text("ca")
            with unittest.mock.patch.object(
                qac.ssl, "create_default_context"
            ) as ctx_factory, unittest.mock.patch.object(
                qac.urllib.request, "urlopen", return_value=resp
            ) as urlopen_mock:
                code, _server = qac.https_get_status_with_ca(
                    "https://127.0.0.1:1/", ca
                )
            self.assertEqual(code, 200)
            ctx_factory.assert_called_once_with(cafile=str(ca))
            _, kwargs = urlopen_mock.call_args
            self.assertIn("context", kwargs)


class BannerTests(unittest.TestCase):
    def test_banner_lines(self):
        lines = qac.apache_banner_lines(
            18080, 18443, "qa-published.percussion.local",
            Path("/repo/docker/logs/qa-apache-certs/rootCA.pem"),
            "perc-matrix-apache-h2",
        )
        text = "\n".join(lines)
        self.assertIn("QA_APACHE_HTTP_PORT=18080", text)
        self.assertIn("APACHE_URL=http://127.0.0.1:18080", text)
        self.assertIn("QA_APACHE_HTTPS_PORT=18443", text)
        self.assertIn("APACHE_HTTPS_URL=https://127.0.0.1:18443", text)
        self.assertIn("APACHE_HOSTNAME=qa-published.percussion.local", text)
        self.assertIn("APACHE_CA_PEM=/repo/docker/logs/qa-apache-certs/rootCA.pem", text)


class VhostConfParityTests(unittest.TestCase):
    """Both vhosts share the proxy map; TLS lives only in :443."""

    def _vhost_body(self, text: str, port: int) -> str:
        pattern = re.compile(
            rf"<VirtualHost \*:{port}>(.*?)</VirtualHost>", re.DOTALL
        )
        match = pattern.search(text)
        assert match is not None, f"no :{port} vhost in conf"
        return match.group(1)

    def _proxy_lines(self, body: str) -> list:
        return sorted(
            line.strip()
            for line in body.splitlines()
            if line.strip().startswith("ProxyPass")
        )

    def test_conf_exists_with_both_vhosts(self):
        text = _conf_text()
        self.assertIn("<VirtualHost *:80>", text)
        self.assertIn("<VirtualHost *:443>", text)

    def test_proxy_map_identical(self):
        text = _conf_text()
        http_proxy = self._proxy_lines(self._vhost_body(text, 80))
        https_proxy = self._proxy_lines(self._vhost_body(text, 443))
        self.assertEqual(http_proxy, https_proxy)
        self.assertTrue(http_proxy, "expected a non-empty proxy map")

    def test_all_service_contexts_proxied(self):
        text = _conf_text()
        for service in qac.DTS_SERVICE_CONTEXTS:
            self.assertIn(f"ProxyPass /{service}/ ", text)
            self.assertIn(f"http://${{DTS_BACKEND}}:9980/{service}/", text)

    def test_fallback_proxy_present(self):
        text = _conf_text()
        for port in (80, 443):
            body = self._vhost_body(text, port)
            self.assertIn("ProxyPass / http://${DTS_BACKEND}:9980/", body)

    def test_static_exclusions_present(self):
        text = _conf_text()
        for port in (80, 443):
            body = self._vhost_body(text, port)
            self.assertIn("ProxyPassMatch ^/$ !", body)
            for excluded in ("/web_resources/", "/widgets/", "/index.html", "/category.html"):
                self.assertIn(f"ProxyPass {excluded} !", body)

    def test_tls_only_in_443(self):
        text = _conf_text()
        body80 = self._vhost_body(text, 80)
        body443 = self._vhost_body(text, 443)
        self.assertNotIn("SSLEngine", body80)
        self.assertIn("SSLEngine on", body443)
        self.assertIn("SSLCertificateFile /certs/apache-cert.pem", body443)
        self.assertIn("SSLCertificateKeyFile /certs/apache-key.pem", body443)
        self.assertIn("ServerName ${APACHE_HOSTNAME}", body443)
        self.assertIn("ServerName ${APACHE_HOSTNAME}", body80)

    def test_documentroot_matches_mount(self):
        text = _conf_text()
        self.assertIn(
            f"DocumentRoot {qac.APACHE_DOCROOT_CONTAINER_PATH}", text
        )


if __name__ == "__main__":
    unittest.main()
