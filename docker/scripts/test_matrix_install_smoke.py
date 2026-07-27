#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for matrix-install-smoke.py and cell-entrypoint helpers."""

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent
MATRIX = SCRIPTS.parent / "matrix"


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
            db_password="PercPass123",
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


class DbOwnershipAndTeardownTests(unittest.TestCase):
    """Pure policy tests for #1516 external DB lifecycle (no live Docker)."""

    def test_db_container_name_mapping(self):
        self.assertEqual(smoke.db_container_name("postgres"), "percussion-postgres")
        self.assertEqual(smoke.db_container_name("mysql"), "percussion-mysql")
        self.assertEqual(smoke.db_container_name("sqlserver"), "percussion-sqlserver")
        self.assertEqual(smoke.db_container_name("other"), "percussion-other")

    def test_external_db_types_skips_h2(self):
        self.assertEqual(
            smoke.external_db_types(["h2", "postgresql", "mysql", "sqlserver"]),
            {"postgresql", "mysql", "sqlserver"},
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

    def test_keep_db_and_stop_db_are_mutually_exclusive(self):
        with self.assertRaises(SystemExit) as ctx:
            smoke._build_parser().parse_args(["--keep-db", "--stop-db"])
        self.assertNotEqual(ctx.exception.code, 0)


if __name__ == "__main__":
    unittest.main()
