#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for matrix-install-smoke.py and cell-entrypoint helpers."""

from __future__ import annotations

import importlib.util
import os
import socket
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent
MATRIX = SCRIPTS.parent / "matrix"

# Env keys freeport / matrix host-port resolution may set or read.
_PORT_ENV_KEYS = (
    "QA_CMS_HOST_PORT",
    "CMS_HOST_PORT",
    "DTS_HOST_PORT",
    "MYSQL_PORT",
    "POSTGRES_PORT",
    "MSSQL_PORT",
    "ORACLE_PORT",
)


def _clear_port_env() -> None:
    for key in _PORT_ENV_KEYS:
        os.environ.pop(key, None)


def _load(path: Path, name: str):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


smoke = _load(SCRIPTS / "matrix-install-smoke.py", "matrix_install_smoke")
cell = _load(MATRIX / "cell-entrypoint.py", "matrix_cell_entrypoint")


class ParseAndExpandTests(unittest.TestCase):
    def test_parse_csv_products(self):
        self.assertEqual(smoke.parse_csv("cms,dts", smoke.PRODUCTS, "product"), ["cms", "dts"])

    def test_parse_csv_rejects_unknown(self):
        with self.assertRaises(ValueError):
            smoke.parse_csv("cockroach", smoke.DB_TYPES, "db")

    def test_expand_matrix(self):
        cells = smoke.expand_matrix(["cms", "dts"], ["h2", "postgresql"])
        self.assertEqual(
            [c.cell_id for c in cells],
            ["cms-h2", "cms-postgresql", "dts-h2", "dts-postgresql"],
        )


class EnvAndPasswordTests(unittest.TestCase):
    def test_load_env_file_ignores_comments(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / ".env.compose"
            path.write_text(
                "# comment\nPOSTGRES_PASSWORD=from-file\nMYSQL_PASSWORD='quoted'\n",
                encoding="utf-8",
            )
            env = smoke.load_env_file(path)
            self.assertEqual(env["POSTGRES_PASSWORD"], "from-file")
            self.assertEqual(env["MYSQL_PASSWORD"], "quoted")

    def test_build_db_services_reads_passwords_from_env(self):
        services = smoke.build_db_services(
            {
                "POSTGRES_PASSWORD": "pg-secret",
                "MYSQL_PASSWORD": "my-secret",
                "MSSQL_SA_PASSWORD": "sa-secret",
                "ORACLE_APP_PASSWORD": "ora-secret",
            }
        )
        self.assertEqual(services["postgresql"]["password"], "pg-secret")
        self.assertEqual(services["mysql"]["password"], "my-secret")
        self.assertEqual(services["sqlserver"]["password"], "sa-secret")
        self.assertEqual(services["oracle"]["password"], "ora-secret")
        self.assertEqual(services["h2"].get("password", ""), "")

    def test_build_db_services_oracle_metadata_shape(self):
        """Oracle harness metadata: service alias, XEPDB1, schema, no secrets in base."""
        services = smoke.build_db_services(
            {
                "ORACLE_APP_PASSWORD": "ora-secret",
                "ORACLE_APP_USER": "cmsuser",
                "ORACLE_SERVICE": "XEPDB1",
                "ORACLE_SCHEMA": "cmsuser",
            }
        )
        ora = services["oracle"]
        self.assertEqual(ora["profile"], "oracle")
        self.assertEqual(ora["service"], "oracle")
        self.assertEqual(ora["container_host"], "oracle")
        self.assertEqual(ora["port"], "1521")
        self.assertEqual(ora["user"], "cmsuser")
        self.assertEqual(ora["name"], "XEPDB1")
        self.assertEqual(ora["schema"], "cmsuser")
        self.assertEqual(ora["password"], "ora-secret")
        self.assertEqual(ora["healthy_timeout"], "600")
        self.assertEqual(ora["wait_db_seconds"], "600")
        # Defaults when only password is provided
        defaults = smoke.build_db_services({"ORACLE_APP_PASSWORD": "x"})
        self.assertEqual(defaults["oracle"]["user"], "percuser")
        self.assertEqual(defaults["oracle"]["name"], "XEPDB1")
        self.assertEqual(defaults["oracle"]["schema"], "percuser")

    def test_require_db_passwords_fails_when_missing(self):
        services = smoke.build_db_services({})
        with self.assertRaises(ValueError) as ctx:
            smoke.require_db_passwords(services, ["postgresql"])
        self.assertIn("POSTGRES_PASSWORD", str(ctx.exception))

    def test_require_db_passwords_fails_for_oracle_when_missing(self):
        services = smoke.build_db_services({})
        with self.assertRaises(ValueError) as ctx:
            smoke.require_db_passwords(services, ["oracle"])
        self.assertIn("ORACLE_APP_PASSWORD", str(ctx.exception))

    def test_require_db_passwords_allows_h2(self):
        services = smoke.build_db_services({})
        smoke.require_db_passwords(services, ["h2"])  # no raise

    def test_db_types_includes_oracle(self):
        self.assertIn("oracle", smoke.DB_TYPES)
        self.assertEqual(smoke.CONTAINER_BY_SERVICE["oracle"], "percussion-oracle")


class ResolveJarTests(unittest.TestCase):
    def test_resolve_uses_shipped_cms_name_only(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            target = root / "modules" / "perc-distribution-tree" / "target"
            target.mkdir(parents=True)
            shipped = target / "perc-distribution-tree.jar"
            shipped.write_bytes(b"shipped-assembly")
            # SNAPSHOT must never be selected even if larger / present.
            (target / "perc-distribution-tree-8.2.0-SNAPSHOT.jar").write_bytes(
                b"x" * 50_000
            )
            resolved = smoke.resolve_installer_jar(root, "cms")
            self.assertEqual(resolved, shipped)
            self.assertEqual(resolved.name, smoke.CMS_INSTALLER_JAR_NAME)

    def test_resolve_uses_shipped_dts_name_only(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            target = (
                root
                / "deliverytiersuite"
                / "delivery-tier-suite"
                / "delivery-tier-distribution"
                / "target"
            )
            target.mkdir(parents=True)
            shipped = target / "delivery-tier-distribution.jar"
            shipped.write_bytes(b"dts-assembly")
            (target / "delivery-tier-distribution-8.2.0-SNAPSHOT.jar").write_bytes(
                b"y" * 50_000
            )
            resolved = smoke.resolve_installer_jar(root, "dts")
            self.assertEqual(resolved.name, smoke.DTS_INSTALLER_JAR_NAME)

    def test_resolve_missing_raises(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            (root / "modules" / "perc-distribution-tree" / "target").mkdir(parents=True)
            # Only SNAPSHOT present — not good enough.
            (
                root
                / "modules"
                / "perc-distribution-tree"
                / "target"
                / "perc-distribution-tree-8.2.0-SNAPSHOT.jar"
            ).write_bytes(b"not-the-customer-jar")
            with self.assertRaises(FileNotFoundError) as ctx:
                smoke.resolve_installer_jar(root, "cms")
            self.assertIn("perc-distribution-tree.jar", str(ctx.exception))
            self.assertIn("Do not use *-SNAPSHOT.jar", str(ctx.exception))

    def test_resolve_empty_shipped_raises(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            target = root / "modules" / "perc-distribution-tree" / "target"
            target.mkdir(parents=True)
            (target / "perc-distribution-tree.jar").write_bytes(b"")
            with self.assertRaises(FileNotFoundError):
                smoke.resolve_installer_jar(root, "cms")


class DockerRunArgvTests(unittest.TestCase):
    def test_cms_postgresql_argv(self):
        jar = Path("/tmp/fake-cms.jar")
        argv = smoke.build_docker_run_argv(
            image=smoke.MATRIX_IMAGE_TAG,
            container_name="perc-matrix-cms-postgresql",
            product="cms",
            db_type="postgresql",
            installer_jar_host=jar,
            host_port=9993,
            network=smoke.MATRIX_NETWORK,
            db_meta=smoke.DB_SERVICES["postgresql"],
            keep=False,
        )
        joined = " ".join(argv)
        self.assertIn("docker run -d", joined.replace("  ", " "))
        self.assertIn("--name perc-matrix-cms-postgresql", joined)
        self.assertIn("DB_TYPE=postgresql", joined)
        self.assertIn("DB_HOST=postgres", joined)
        self.assertIn("PRODUCT=cms", joined)
        self.assertIn("/installer/installer.jar:ro", joined)
        self.assertIn("9993:9992", joined)

    def test_h2_omits_host(self):
        argv = smoke.build_docker_run_argv(
            image=smoke.MATRIX_IMAGE_TAG,
            container_name="perc-matrix-cms-h2",
            product="cms",
            db_type="h2",
            installer_jar_host=Path("/tmp/j.jar"),
            host_port=9993,
            network=smoke.MATRIX_NETWORK,
            db_meta=smoke.DB_SERVICES["h2"],
            keep=False,
        )
        joined = " ".join(argv)
        self.assertIn("DB_TYPE=h2", joined)
        self.assertNotIn("DB_HOST=", joined)

    def test_cms_oracle_argv(self):
        jar = Path("/tmp/fake-cms.jar")
        db_meta = smoke.build_db_services(
            {
                "ORACLE_APP_PASSWORD": "ora-secret",
                "ORACLE_APP_USER": "percuser",
                "ORACLE_SERVICE": "XEPDB1",
            }
        )["oracle"]
        argv = smoke.build_docker_run_argv(
            image=smoke.MATRIX_IMAGE_TAG,
            container_name="perc-matrix-cms-oracle",
            product="cms",
            db_type="oracle",
            installer_jar_host=jar,
            host_port=9993,
            network=smoke.MATRIX_NETWORK,
            db_meta=db_meta,
            keep=False,
        )
        joined = " ".join(argv)
        self.assertIn("--name perc-matrix-cms-oracle", joined)
        self.assertIn("DB_TYPE=oracle", joined)
        self.assertIn("DB_HOST=oracle", joined)
        self.assertIn("DB_PORT=1521", joined)
        self.assertIn("DB_NAME=XEPDB1", joined)
        self.assertIn("DB_USER=percuser", joined)
        self.assertIn("DB_PASSWORD=ora-secret", joined)
        self.assertIn("DB_SCHEMA=percuser", joined)
        self.assertIn("PRODUCT=cms", joined)
        # Oracle cold-start can exceed the cell default 120s TCP wait.
        self.assertIn("WAIT_DB_SECONDS=600", joined)

    def test_wait_for_container_healthy_dry_run(self):
        ok, detail = smoke.wait_for_container_healthy(
            "percussion-oracle", 30, dry_run=True
        )
        self.assertTrue(ok)
        self.assertEqual(detail, "dry-run")

    def test_build_matrix_image_uses_docker_dir_context(self):
        """#2481: build context is docker/ so HEALTHCHECK scripts can COPY in."""
        captured: list = []

        def fake_run(argv, *, dry_run, check=False, capture=False, timeout=None):
            captured.append(list(argv))
            return __import__("subprocess").CompletedProcess(argv, 0, "", "")

        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            (root / "docker" / "matrix").mkdir(parents=True)
            (root / "docker" / "matrix" / "Dockerfile").write_text(
                "FROM scratch\n", encoding="utf-8"
            )
            (root / "docker" / "scripts").mkdir(parents=True)
            orig = smoke._run
            smoke._run = fake_run  # type: ignore[assignment]
            try:
                smoke.build_matrix_image(root, dry_run=False)
            finally:
                smoke._run = orig  # type: ignore[assignment]
        self.assertEqual(len(captured), 1)
        argv = captured[0]
        self.assertEqual(argv[0:3], ["docker", "build", "-t"])
        self.assertIn(smoke.MATRIX_IMAGE_TAG, argv)
        # Context path is …/docker (parent of matrix/), not …/docker/matrix alone.
        self.assertEqual(Path(argv[-1]), root / "docker")
        self.assertEqual(Path(argv[argv.index("-f") + 1]), root / "docker" / "matrix" / "Dockerfile")


class WaitForContainerHealthyPolicyTests(unittest.TestCase):
    """#2535: docker Health.Status wait policy (fail-fast unhealthy)."""

    def test_inspect_container_health_parses_format(self):
        import unittest.mock
        import subprocess as sp

        proc = sp.CompletedProcess(
            args=[], returncode=0, stdout="healthy|running\n", stderr=""
        )
        with unittest.mock.patch.object(smoke.subprocess, "run", return_value=proc):
            health, status = smoke.inspect_container_health("perc-matrix-cms-h2")
        self.assertEqual(health, "healthy")
        self.assertEqual(status, "running")

    def test_inspect_container_health_none_when_no_healthblock(self):
        import unittest.mock
        import subprocess as sp

        proc = sp.CompletedProcess(
            args=[], returncode=0, stdout="none|running\n", stderr=""
        )
        with unittest.mock.patch.object(smoke.subprocess, "run", return_value=proc):
            health, status = smoke.inspect_container_health("some-db")
        self.assertEqual(health, "none")
        self.assertEqual(status, "running")

    def test_wait_for_container_healthy_fail_fast_unhealthy(self):
        """Do not burn the full timeout when inspect already reports unhealthy."""
        import unittest.mock

        with unittest.mock.patch.object(
            smoke, "inspect_container_health", return_value=("unhealthy", "running")
        ) as mock_inspect, unittest.mock.patch.object(
            smoke.time, "sleep"
        ) as mock_sleep:
            ok, detail = smoke.wait_for_container_healthy(
                "perc-matrix-cms-h2",
                timeout_seconds=600,
                dry_run=False,
                interval_seconds=5,
            )
        self.assertFalse(ok)
        self.assertIn(smoke.DETAIL_DOCKER_UNHEALTHY, detail)
        self.assertIn("health=unhealthy", detail)
        mock_inspect.assert_called()
        mock_sleep.assert_not_called()

    def test_wait_for_container_healthy_success(self):
        import unittest.mock

        with unittest.mock.patch.object(
            smoke, "inspect_container_health", return_value=("healthy", "running")
        ), unittest.mock.patch.object(smoke.time, "sleep") as mock_sleep:
            ok, detail = smoke.wait_for_container_healthy(
                "percussion-oracle",
                timeout_seconds=30,
                dry_run=False,
            )
        self.assertTrue(ok)
        self.assertIn("healthy", detail)
        mock_sleep.assert_not_called()

    def test_wait_for_container_healthy_no_healthcheck_allowed(self):
        import unittest.mock

        with unittest.mock.patch.object(
            smoke, "inspect_container_health", return_value=("none", "running")
        ):
            ok, detail = smoke.wait_for_container_healthy(
                "percussion-mysql",
                timeout_seconds=30,
                dry_run=False,
                allow_no_healthcheck=True,
            )
        self.assertTrue(ok)
        self.assertIn("no_healthcheck", detail)

    def test_wait_for_container_healthy_exited_fail_fast(self):
        import unittest.mock

        with unittest.mock.patch.object(
            smoke, "inspect_container_health", return_value=("none", "exited")
        ), unittest.mock.patch.object(smoke.time, "sleep") as mock_sleep:
            ok, detail = smoke.wait_for_container_healthy(
                "dead-box",
                timeout_seconds=100,
                dry_run=False,
            )
        self.assertFalse(ok)
        self.assertIn(smoke.DETAIL_CONTAINER_NOT_RUNNING, detail)
        mock_sleep.assert_not_called()


class WaitForHttpContextFailTests(unittest.TestCase):
    """#2462: matrix probe must fail-fast on Rhythmyx context death."""

    def setUp(self):
        self._slog = unittest.mock.patch.object(
            smoke, "_docker_read_server_log", return_value=""
        )
        self._slog.start()
        self.addCleanup(self._slog.stop)

    def test_wait_for_http_dry_run(self):
        ok, detail = smoke.wait_for_http(
            "http://127.0.0.1:9993/Rhythmyx/login",
            timeout_seconds=1,
            dry_run=True,
            container_name="perc-matrix-cms-h2",
        )
        self.assertTrue(ok)
        self.assertEqual(detail, "dry-run")

    def test_wait_for_http_context_fail_without_http(self):
        import unittest.mock

        dead = "WARN [WebAppContext] Failed startup of context Rhythmyx\n"
        with unittest.mock.patch.object(
            smoke, "_docker_logs_tail", return_value=dead
        ), unittest.mock.patch.object(smoke.time, "sleep"):
            ok, detail = smoke.wait_for_http(
                "http://127.0.0.1:9993/Rhythmyx/login",
                timeout_seconds=30,
                interval_seconds=5,
                dry_run=False,
                container_name="perc-matrix-cms-h2",
            )
        self.assertFalse(ok)
        self.assertIn("rhythmyx_context_failed", detail)
        self.assertIn("Failed startup of context", detail)

    def test_wait_for_http_http_ok_but_context_failed(self):
        import unittest.mock
        import urllib.error

        dead = (
            "Failed startup of context Rhythmyx\n"
            "BeanCurrentlyInCreationException: folderHelper\n"
        )

        class _Resp:
            def getcode(self):
                return 200

            def __enter__(self):
                return self

            def __exit__(self, *a):
                return False

        with unittest.mock.patch.object(
            smoke, "_docker_logs_tail", return_value=dead
        ), unittest.mock.patch.object(
            smoke.urllib.request, "urlopen", return_value=_Resp()
        ), unittest.mock.patch.object(smoke.time, "sleep"):
            # First loop iteration: log scan runs before HTTP and fails immediately.
            ok, detail = smoke.wait_for_http(
                "http://127.0.0.1:9993/Rhythmyx/login",
                timeout_seconds=30,
                interval_seconds=5,
                dry_run=False,
                container_name="perc-matrix-cms-h2",
            )
        self.assertFalse(ok)
        self.assertIn("rhythmyx_context_failed", detail)

    def test_wait_for_http_no_container_skips_log_scan(self):
        """DTS / no container: HTTP-only path unchanged."""
        import unittest.mock

        class _Resp:
            def getcode(self):
                return 200

            def __enter__(self):
                return self

            def __exit__(self, *a):
                return False

        with unittest.mock.patch.object(
            smoke, "_docker_logs_tail"
        ) as mock_logs, unittest.mock.patch.object(
            smoke.urllib.request, "urlopen", return_value=_Resp()
        ):
            ok, detail = smoke.wait_for_http(
                "http://127.0.0.1:9983/",
                timeout_seconds=5,
                dry_run=False,
                container_name=None,
            )
        self.assertTrue(ok)
        self.assertEqual(detail, "HTTP 200")
        mock_logs.assert_not_called()


class WaitForHttpDockerHealthTests(unittest.TestCase):
    """#2535: CMS wait requires Health.Status=healthy + host belt-and-braces."""

    def setUp(self):
        # Isolate from live docker exec of product logs (#2556).
        self._slog = unittest.mock.patch.object(
            smoke, "_docker_read_server_log", return_value=""
        )
        self._slog.start()
        self.addCleanup(self._slog.stop)

    def test_require_health_fail_fast_unhealthy(self):
        """Unhealthy inspect must not wait the full probe timeout."""
        import unittest.mock

        with unittest.mock.patch.object(
            smoke, "_docker_logs_tail", return_value="Jetty started\n"
        ), unittest.mock.patch.object(
            smoke, "inspect_container_health", return_value=("unhealthy", "running")
        ), unittest.mock.patch.object(
            smoke.time, "sleep"
        ) as mock_sleep, unittest.mock.patch.object(
            smoke.urllib.request, "urlopen"
        ) as mock_http:
            ok, detail = smoke.wait_for_http(
                "http://127.0.0.1:9993/Rhythmyx/login",
                timeout_seconds=600,
                interval_seconds=5,
                dry_run=False,
                container_name="perc-matrix-cms-h2",
                require_docker_health=True,
            )
        self.assertFalse(ok)
        self.assertIn(smoke.DETAIL_DOCKER_UNHEALTHY, detail)
        self.assertIn("health=unhealthy", detail)
        mock_http.assert_not_called()
        mock_sleep.assert_not_called()

    def test_require_health_success_when_healthy_and_http_ok(self):
        import unittest.mock

        class _Resp:
            def getcode(self):
                return 200

            def __enter__(self):
                return self

            def __exit__(self, *a):
                return False

        with unittest.mock.patch.object(
            smoke, "_docker_logs_tail", return_value="Server Started\n"
        ), unittest.mock.patch.object(
            smoke, "inspect_container_health", return_value=("healthy", "running")
        ), unittest.mock.patch.object(
            smoke.urllib.request, "urlopen", return_value=_Resp()
        ), unittest.mock.patch.object(smoke.time, "sleep") as mock_sleep:
            ok, detail = smoke.wait_for_http(
                "http://127.0.0.1:9993/Rhythmyx/login",
                timeout_seconds=30,
                interval_seconds=5,
                dry_run=False,
                container_name="perc-matrix-cms-h2",
                require_docker_health=True,
            )
        self.assertTrue(ok)
        self.assertIn("HTTP 200", detail)
        self.assertIn("health=healthy", detail)
        mock_sleep.assert_not_called()

    def test_require_health_waits_while_starting_even_if_http_ok(self):
        """HTTP green during start_period is not enough — need healthy."""
        import unittest.mock

        class _Resp:
            def getcode(self):
                return 200

            def __enter__(self):
                return self

            def __exit__(self, *a):
                return False

        # First poll: starting; second: healthy.
        health_seq = iter(
            [("starting", "running"), ("healthy", "running")]
        )

        def _inspect(_name, **_kw):
            return next(health_seq)

        with unittest.mock.patch.object(
            smoke, "_docker_logs_tail", return_value="ok\n"
        ), unittest.mock.patch.object(
            smoke, "inspect_container_health", side_effect=_inspect
        ), unittest.mock.patch.object(
            smoke.urllib.request, "urlopen", return_value=_Resp()
        ), unittest.mock.patch.object(smoke.time, "sleep") as mock_sleep:
            ok, detail = smoke.wait_for_http(
                "http://127.0.0.1:9993/Rhythmyx/login",
                timeout_seconds=30,
                interval_seconds=0.01,
                dry_run=False,
                container_name="perc-matrix-cms-h2",
                require_docker_health=True,
            )
        self.assertTrue(ok)
        self.assertIn("health=healthy", detail)
        mock_sleep.assert_called()

    def test_require_health_still_fail_fast_on_context_log_markers(self):
        """Host rhythmyx_ready log scan remains belt-and-braces (#2462)."""
        import unittest.mock

        dead = "Failed startup of context Rhythmyx\n"
        with unittest.mock.patch.object(
            smoke, "_docker_logs_tail", return_value=dead
        ), unittest.mock.patch.object(
            smoke, "inspect_container_health", return_value=("starting", "running")
        ), unittest.mock.patch.object(smoke.time, "sleep") as mock_sleep:
            ok, detail = smoke.wait_for_http(
                "http://127.0.0.1:9993/Rhythmyx/login",
                timeout_seconds=600,
                interval_seconds=5,
                dry_run=False,
                container_name="perc-matrix-cms-h2",
                require_docker_health=True,
            )
        self.assertFalse(ok)
        self.assertIn(smoke.DETAIL_CONTEXT_FAILED, detail)
        self.assertIn("health=starting", detail)
        mock_sleep.assert_not_called()


class InstallArgvTests(unittest.TestCase):
    def test_cms_postgres_silent_argv(self):
        argv = cell.build_install_argv(
            java="java",
            installer_jar=Path("/installer/installer.jar"),
            install_root=Path("/opt/Percussion"),
            product="cms",
            db_type="postgresql",
            db_host="postgres",
            db_port="5432",
            db_name="percdb",
            db_user="percuser",
            db_password="test-db-password",
            db_schema="public",
            silent=True,
        )
        # Paths are POSIX form even when this test runs on Windows (container entrypoint).
        self.assertEqual(
            argv[0:4],
            ["java", "-jar", "/installer/installer.jar", "/opt/Percussion"],
        )
        self.assertIn("--silent", argv)
        self.assertIn("--no-tty", argv)
        self.assertIn("--db.type=postgresql", argv)
        self.assertIn("--db.host=postgres", argv)
        self.assertIn("--db.port=5432", argv)
        # Compose matrix DBs have no TLS; installer default ssl=true breaks MySQL/SQL Server.
        self.assertIn("--db.ssl.enabled=false", argv)
        self.assertIn("--db.ssl.verify=false", argv)
        # Ensure we never emit Windows separators into java -jar argv.
        for part in argv[0:4]:
            self.assertNotIn("\\", part)

    def test_h2_omits_remote_fields(self):
        argv = cell.build_install_argv(
            java="java",
            installer_jar=Path("/installer/installer.jar"),
            install_root=Path("/opt/Percussion"),
            product="cms",
            db_type="h2",
            db_host="ignored",
            db_port="1",
            db_name="x",
            db_user="u",
            db_password="p",
            db_schema="",
            silent=True,
        )
        self.assertIn("--db.type=h2", argv)
        self.assertNotIn("--db.host=ignored", argv)
        self.assertNotIn("--db.ssl.enabled=false", argv)

    def test_oracle_install_argv_service_schema_ssl_off(self):
        """Oracle cell: host/port/service/user/password/schema + SSL off (compose)."""
        argv = cell.build_install_argv(
            java="java",
            installer_jar=Path("/installer/installer.jar"),
            install_root=Path("/opt/Percussion"),
            product="cms",
            db_type="oracle",
            db_host="oracle",
            db_port="1521",
            db_name="XEPDB1",
            db_user="percuser",
            db_password="test-db-password",
            db_schema="percuser",
            silent=True,
        )
        self.assertIn("--db.type=oracle", argv)
        self.assertIn("--db.host=oracle", argv)
        self.assertIn("--db.port=1521", argv)
        self.assertIn("--db.name=XEPDB1", argv)
        self.assertIn("--db.user=percuser", argv)
        self.assertIn("--db.password=test-db-password", argv)
        self.assertIn("--db.schema=percuser", argv)
        self.assertIn("--db.ssl.enabled=false", argv)
        self.assertIn("--db.ssl.verify=false", argv)
        self.assertIn("--silent", argv)
        self.assertIn("--no-tty", argv)


class ProbeUrlTests(unittest.TestCase):
    def test_cms_and_dts_urls(self):
        self.assertEqual(
            smoke.build_probe_url("cms", 9993),
            "http://127.0.0.1:9993/Rhythmyx/login",
        )
        self.assertEqual(
            smoke.build_probe_url("dts", 9983),
            "http://127.0.0.1:9983/",
        )


class MatrixHostPortFreeportTests(unittest.TestCase):
    """#2005 — matrix docker -p / probe host port from env or freeport."""

    def setUp(self):
        _clear_port_env()
        self.addCleanup(_clear_port_env)

    def test_preferred_baselines(self):
        self.assertEqual(smoke.PREFERRED_CMS_HOST_PORT, 9993)
        self.assertEqual(smoke.PREFERRED_DTS_HOST_PORT, 9983)
        # Historical aliases remain for callers/docs.
        self.assertEqual(smoke.CMS_HOST_PORT, 9993)
        self.assertEqual(smoke.DTS_HOST_PORT, 9983)

    def test_cms_env_override_qa_cms_host_port(self):
        os.environ["QA_CMS_HOST_PORT"] = "18001"
        self.assertEqual(smoke.resolve_matrix_host_port("cms"), 18001)

    def test_cms_env_override_cms_host_port(self):
        os.environ["CMS_HOST_PORT"] = "18002"
        self.assertEqual(smoke.resolve_matrix_host_port("cms"), 18002)

    def test_cms_qa_env_wins_over_cms_host_port(self):
        os.environ["QA_CMS_HOST_PORT"] = "18003"
        os.environ["CMS_HOST_PORT"] = "18004"
        self.assertEqual(smoke.resolve_matrix_host_port("cms"), 18003)

    def test_dts_env_override(self):
        os.environ["DTS_HOST_PORT"] = "18005"
        self.assertEqual(smoke.resolve_matrix_host_port("dts"), 18005)

    def test_invalid_env_raises(self):
        os.environ["CMS_HOST_PORT"] = "not-a-port"
        with self.assertRaises(ValueError):
            smoke.resolve_matrix_host_port("cms")

    def test_preferred_when_free(self):
        preferred = smoke.find_free_port()
        # Temporarily treat preferred as free preferred baseline via direct resolve.
        self.assertEqual(
            smoke.resolve_host_port(preferred=preferred),
            preferred,
        )

    def test_falls_back_when_preferred_taken(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.bind(("127.0.0.1", 0))
            taken = int(sock.getsockname()[1])
            resolved = smoke.resolve_host_port(preferred=taken)
            self.assertNotEqual(resolved, taken)
            self.assertGreater(resolved, 0)

    def test_ensure_cms_pins_env(self):
        os.environ["QA_CMS_HOST_PORT"] = "17777"
        port = smoke.ensure_matrix_host_port("cms")
        self.assertEqual(port, 17777)
        self.assertEqual(os.environ["CMS_HOST_PORT"], "17777")
        self.assertEqual(os.environ["QA_CMS_HOST_PORT"], "17777")

    def test_ensure_dts_pins_env(self):
        os.environ["DTS_HOST_PORT"] = "17778"
        port = smoke.ensure_matrix_host_port("dts")
        self.assertEqual(port, 17778)
        self.assertEqual(os.environ["DTS_HOST_PORT"], "17778")

    def test_docker_run_argv_uses_resolved_host_port(self):
        """Probe URL and -p mapping must share the same host port (e2e contract)."""
        os.environ["CMS_HOST_PORT"] = "16661"
        host_port = smoke.ensure_matrix_host_port("cms")
        self.assertEqual(host_port, 16661)
        argv = smoke.build_docker_run_argv(
            image=smoke.MATRIX_IMAGE_TAG,
            container_name="perc-matrix-cms-h2",
            product="cms",
            db_type="h2",
            installer_jar_host=Path("/tmp/j.jar"),
            host_port=host_port,
            network=smoke.MATRIX_NETWORK,
            db_meta=smoke.DB_SERVICES["h2"],
            keep=False,
        )
        joined = " ".join(argv)
        self.assertIn("16661:9992", joined)
        self.assertEqual(
            smoke.build_probe_url("cms", host_port),
            "http://127.0.0.1:16661/Rhythmyx/login",
        )

    def test_unknown_product_raises(self):
        with self.assertRaises(ValueError):
            smoke.resolve_matrix_host_port("oracle")


class ComposeDbHostPortFreeportTests(unittest.TestCase):
    """#2004 — compose MYSQL/POSTGRES/MSSQL host ports from env or freeport."""

    def setUp(self):
        _clear_port_env()
        self.addCleanup(_clear_port_env)

    def test_preferred_baselines(self):
        self.assertEqual(smoke.PREFERRED_MYSQL_HOST_PORT, 3306)
        self.assertEqual(smoke.PREFERRED_POSTGRES_HOST_PORT, 5433)
        self.assertEqual(smoke.PREFERRED_MSSQL_HOST_PORT, 1433)
        self.assertEqual(smoke.PREFERRED_ORACLE_HOST_PORT, 1521)
        self.assertEqual(
            smoke.COMPOSE_DB_HOST_PORT_SPEC["mysql"],
            ("MYSQL_PORT", 3306),
        )
        self.assertEqual(
            smoke.COMPOSE_DB_HOST_PORT_SPEC["postgresql"],
            ("POSTGRES_PORT", 5433),
        )
        self.assertEqual(
            smoke.COMPOSE_DB_HOST_PORT_SPEC["sqlserver"],
            ("MSSQL_PORT", 1433),
        )
        self.assertEqual(
            smoke.COMPOSE_DB_HOST_PORT_SPEC["oracle"],
            ("ORACLE_PORT", 1521),
        )

    def test_env_override_mysql(self):
        os.environ["MYSQL_PORT"] = "13306"
        self.assertEqual(smoke.resolve_compose_db_host_port("mysql"), 13306)

    def test_env_override_postgres(self):
        os.environ["POSTGRES_PORT"] = "15433"
        self.assertEqual(smoke.resolve_compose_db_host_port("postgresql"), 15433)

    def test_env_override_mssql(self):
        os.environ["MSSQL_PORT"] = "11433"
        self.assertEqual(smoke.resolve_compose_db_host_port("sqlserver"), 11433)

    def test_env_override_oracle(self):
        os.environ["ORACLE_PORT"] = "11521"
        self.assertEqual(smoke.resolve_compose_db_host_port("oracle"), 11521)

    def test_invalid_env_raises(self):
        os.environ["MYSQL_PORT"] = "not-a-port"
        with self.assertRaises(ValueError):
            smoke.resolve_compose_db_host_port("mysql")

    def test_h2_and_unknown_raise(self):
        with self.assertRaises(ValueError):
            smoke.resolve_compose_db_host_port("h2")
        with self.assertRaises(ValueError):
            smoke.resolve_compose_db_host_port("cockroach")

    def test_preferred_when_free(self):
        preferred = smoke.find_free_port()
        # Direct resolve_host_port path used by compose DB helpers.
        self.assertEqual(
            smoke.resolve_host_port("MYSQL_PORT", preferred=preferred),
            preferred,
        )

    def test_falls_back_when_preferred_taken(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.bind(("127.0.0.1", 0))
            taken = int(sock.getsockname()[1])
            resolved = smoke.resolve_host_port("POSTGRES_PORT", preferred=taken)
            self.assertNotEqual(resolved, taken)
            self.assertGreater(resolved, 0)

    def test_ensure_pins_all_external_dbs(self):
        os.environ["MYSQL_PORT"] = "13306"
        os.environ["POSTGRES_PORT"] = "15433"
        os.environ["MSSQL_PORT"] = "11433"
        os.environ["ORACLE_PORT"] = "11521"
        resolved = smoke.ensure_compose_db_host_ports(
            ["h2", "mysql", "postgresql", "sqlserver", "oracle"]
        )
        self.assertEqual(
            resolved,
            {
                "mysql": 13306,
                "postgresql": 15433,
                "sqlserver": 11433,
                "oracle": 11521,
            },
        )
        self.assertNotIn("h2", resolved)
        self.assertEqual(os.environ["MYSQL_PORT"], "13306")
        self.assertEqual(os.environ["POSTGRES_PORT"], "15433")
        self.assertEqual(os.environ["MSSQL_PORT"], "11433")
        self.assertEqual(os.environ["ORACLE_PORT"], "11521")

    def test_ensure_skips_h2_only(self):
        resolved = smoke.ensure_compose_db_host_ports(["h2"])
        self.assertEqual(resolved, {})
        self.assertNotIn("MYSQL_PORT", os.environ)

    def test_ensure_freeport_when_preferred_taken(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.bind(("127.0.0.1", 0))
            taken = int(sock.getsockname()[1])
            # Force preferred to the held port via a temporary preferred override
            # by pre-setting env empty and monkeypatching preferred in resolve path:
            # Call resolve_host_port with taken preferred through ensure after
            # temporarily swapping COMPOSE_DB_HOST_PORT_SPEC preferred.
            original = smoke.COMPOSE_DB_HOST_PORT_SPEC["mysql"]
            smoke.COMPOSE_DB_HOST_PORT_SPEC["mysql"] = ("MYSQL_PORT", taken)
            self.addCleanup(
                lambda: smoke.COMPOSE_DB_HOST_PORT_SPEC.__setitem__(
                    "mysql", original
                )
            )
            resolved = smoke.ensure_compose_db_host_ports(["mysql"])
            self.assertEqual(list(resolved.keys()), ["mysql"])
            self.assertNotEqual(resolved["mysql"], taken)
            self.assertEqual(os.environ["MYSQL_PORT"], str(resolved["mysql"]))

    def test_dry_run_postgresql_prints_postgres_port(self):
        """Dry-run external DB path pins and prints POSTGRES_PORT (#2004)."""
        import io
        from contextlib import redirect_stdout

        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            target = root / "modules" / "perc-distribution-tree" / "target"
            target.mkdir(parents=True)
            (target / "perc-distribution-tree.jar").write_bytes(b"stub-jar-content")
            (root / "docker" / "logs").mkdir(parents=True)
            (root / "docker" / "matrix").mkdir(parents=True)
            (root / "docker" / "matrix" / "Dockerfile").write_text(
                "FROM scratch\n", encoding="utf-8"
            )
            (root / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
            (root / ".env.compose.example").write_text(
                "POSTGRES_PASSWORD=test-local-only\n"
                "MYSQL_PASSWORD=test-local-only\n"
                "MSSQL_SA_PASSWORD=test-local-only\n"
                "ORACLE_APP_PASSWORD=test-local-only\n"
                "ORACLE_PASSWORD=test-local-only\n",
                encoding="utf-8",
            )
            os.environ["POSTGRES_PORT"] = "15444"
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = smoke.main(
                    [
                        "--repo-root",
                        str(root),
                        "--product",
                        "cms",
                        "--db",
                        "postgresql",
                        "--dry-run",
                        "--skip-image-build",
                    ]
                )
            self.assertEqual(rc, 0)
            out = buf.getvalue()
            self.assertIn("POSTGRES_PORT=15444", out)
            self.assertEqual(os.environ["POSTGRES_PORT"], "15444")


class DbOwnershipAndTeardownTests(unittest.TestCase):
    """Pure policy tests for #1516 external DB lifecycle (no live Docker)."""

    def test_db_container_name_mapping(self):
        self.assertEqual(smoke.db_container_name("postgres"), "percussion-postgres")
        self.assertEqual(smoke.db_container_name("mysql"), "percussion-mysql")
        self.assertEqual(smoke.db_container_name("sqlserver"), "percussion-sqlserver")
        self.assertEqual(smoke.db_container_name("oracle"), "percussion-oracle")
        self.assertEqual(smoke.db_container_name("other"), "percussion-other")

    def test_external_db_types_skips_h2(self):
        self.assertEqual(
            smoke.external_db_types(
                ["h2", "postgresql", "mysql", "sqlserver", "oracle"]
            ),
            {"postgresql", "mysql", "sqlserver", "oracle"},
        )
        self.assertEqual(smoke.external_db_types(["h2"]), set())

    def test_default_stops_only_started_by_matrix(self):
        self.assertEqual(
            smoke.select_dbs_to_stop(
                started_by_matrix={"postgresql"},
                used_external={"postgresql", "mysql"},
                keep=False,
                keep_db=False,
                stop_db=False,
            ),
            {"postgresql"},
        )

    def test_default_stops_nothing_when_all_preexisting(self):
        self.assertEqual(
            smoke.select_dbs_to_stop(
                started_by_matrix=set(),
                used_external={"postgresql", "mysql"},
                keep=False,
                keep_db=False,
                stop_db=False,
            ),
            set(),
        )

    def test_keep_leaves_all_dbs(self):
        self.assertEqual(
            smoke.select_dbs_to_stop(
                started_by_matrix={"postgresql", "mysql"},
                used_external={"postgresql", "mysql"},
                keep=True,
                keep_db=False,
                stop_db=False,
            ),
            set(),
        )

    def test_keep_db_leaves_dbs_without_keep(self):
        self.assertEqual(
            smoke.select_dbs_to_stop(
                started_by_matrix={"postgresql"},
                used_external={"postgresql"},
                keep=False,
                keep_db=True,
                stop_db=False,
            ),
            set(),
        )

    def test_stop_db_stops_all_used_including_preexisting(self):
        self.assertEqual(
            smoke.select_dbs_to_stop(
                started_by_matrix={"postgresql"},
                used_external={"postgresql", "mysql", "sqlserver"},
                keep=False,
                keep_db=False,
                stop_db=True,
            ),
            {"postgresql", "mysql", "sqlserver"},
        )

    def test_keep_overrides_stop_db(self):
        self.assertEqual(
            smoke.select_dbs_to_stop(
                started_by_matrix={"postgresql"},
                used_external={"postgresql"},
                keep=True,
                keep_db=False,
                stop_db=True,
            ),
            set(),
        )

    def test_keep_db_overrides_stop_db_via_mutex_policy(self):
        # CLI makes these mutually exclusive; policy still prefers keep_db.
        self.assertEqual(
            smoke.select_dbs_to_stop(
                started_by_matrix={"mysql"},
                used_external={"mysql"},
                keep=False,
                keep_db=True,
                stop_db=True,
            ),
            set(),
        )

    def test_started_unknown_to_used_is_ignored(self):
        # Defensive: only stop DBs that were part of this matrix selection.
        self.assertEqual(
            smoke.select_dbs_to_stop(
                started_by_matrix={"postgresql", "mysql"},
                used_external={"postgresql"},
                keep=False,
                keep_db=False,
                stop_db=False,
            ),
            {"postgresql"},
        )


class DryRunCliTests(unittest.TestCase):
    def _stub_repo(self, root: Path) -> None:
        target = root / "modules" / "perc-distribution-tree" / "target"
        target.mkdir(parents=True)
        # Exact customer-shipped assembly name (not *-SNAPSHOT.jar).
        (target / "perc-distribution-tree.jar").write_bytes(b"stub-jar-content")
        (root / "docker" / "logs").mkdir(parents=True)
        (root / "docker" / "matrix").mkdir(parents=True)
        (root / "docker" / "matrix" / "Dockerfile").write_text(
            "FROM scratch\n", encoding="utf-8"
        )
        (root / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
        # Credentials for external DB matrix cells (mirrors .env.compose.example).
        # Placeholders only — never real secrets (matrix harness unit tests).
        (root / ".env.compose.example").write_text(
            "POSTGRES_PASSWORD=test-local-only\n"
            "MYSQL_PASSWORD=test-local-only\n"
            "MSSQL_SA_PASSWORD=test-local-only\n"
            "ORACLE_APP_PASSWORD=test-local-only\n"
            "ORACLE_PASSWORD=test-local-only\n",
            encoding="utf-8",
        )

    def test_dry_run_exits_zero_for_h2(self):
        # dry-run still needs a jar on disk for resolve in run_cell —
        # dry-run path resolves jar before docker; create a stub tree.
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            self._stub_repo(root)
            rc = smoke.main(
                [
                    "--repo-root",
                    str(root),
                    "--product",
                    "cms",
                    "--db",
                    "h2",
                    "--dry-run",
                    "--skip-image-build",
                ]
            )
            self.assertEqual(rc, 0)

    def test_dry_run_postgresql_exits_zero(self):
        """External DB path exercises start/stop planning without live Docker."""
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            self._stub_repo(root)
            rc = smoke.main(
                [
                    "--repo-root",
                    str(root),
                    "--product",
                    "cms",
                    "--db",
                    "postgresql",
                    "--dry-run",
                    "--skip-image-build",
                ]
            )
            self.assertEqual(rc, 0)

    def test_dry_run_oracle_exits_zero(self):
        """Oracle cell accepted by CLI; dry-run exercises compose port pin (#1508)."""
        import io
        from contextlib import redirect_stdout

        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            self._stub_repo(root)
            os.environ["ORACLE_PORT"] = "11521"
            self.addCleanup(lambda: os.environ.pop("ORACLE_PORT", None))
            buf = io.StringIO()
            with redirect_stdout(buf):
                rc = smoke.main(
                    [
                        "--repo-root",
                        str(root),
                        "--product",
                        "cms",
                        "--db",
                        "oracle",
                        "--dry-run",
                        "--skip-image-build",
                    ]
                )
            self.assertEqual(rc, 0)
            self.assertIn("ORACLE_PORT=11521", buf.getvalue())

    def test_parse_csv_accepts_oracle(self):
        self.assertEqual(
            smoke.parse_csv("oracle", smoke.DB_TYPES, "db"),
            ["oracle"],
        )

    def test_keep_db_and_stop_db_are_mutually_exclusive(self):
        with self.assertRaises(SystemExit) as ctx:
            smoke._build_parser().parse_args(["--keep-db", "--stop-db"])
        self.assertNotEqual(ctx.exception.code, 0)


if __name__ == "__main__":
    unittest.main()
