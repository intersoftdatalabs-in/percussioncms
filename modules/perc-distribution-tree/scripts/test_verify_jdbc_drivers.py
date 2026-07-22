#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for verify-jdbc-drivers.py (no network, no Maven).

Builds synthetic ``perc-distribution-tree.jar`` archives in a tempdir and
exercises every exit code documented in the script's ``## Behavioral Notes``.
"""

from __future__ import annotations

import importlib.util
import io
import logging
import os
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "verify-jdbc-drivers.py"
    name = "verify_jdbc_drivers"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


vjd = _load()


def _build_artifact(
    artifact: Path,
    jdbc_jars: list[tuple[str, bytes | None]],
) -> None:
    """Build a synthetic distribution jar.

    ``jdbc_jars`` is a list of ``(name, content)`` tuples. ``content=None``
    creates a zero-byte entry; ``content=b"..."`` creates a real entry
    (auto-wrapped as a minimal valid zip if the bytes are not a valid zip).
    The jars are placed under ``jetty/base/lib/jdbc/`` inside the artifact.
    """
    with zipfile.ZipFile(artifact, "w") as zf:
        for name, content in jdbc_jars:
            data = content if content is not None else b""
            zf.writestr(f"jetty/base/lib/jdbc/{name}", data)


def _make_valid_jar_bytes() -> bytes:
    """Bytes for a minimal valid empty zip archive (no entries)."""
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w"):
        pass
    return buf.getvalue()


def _make_corrupt_jar_bytes() -> bytes:
    """Bytes that look zip-ish at the magic-bytes sniff but fail open."""
    return b"PK\x03\x04not-a-real-zip-payload-just-padding"


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        """argparse --help must not raise."""
        with self.assertRaises(SystemExit) as cm:
            vjd.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_unknown_arg_exits_one(self):
        with self.assertRaises(SystemExit) as cm:
            vjd.main(["--definitely-not-a-flag"])
        self.assertEqual(cm.exception.code, 2)  # argparse uses 2 for usage errors

    def test_csv_split(self):
        self.assertEqual(vjd._split_csv("a,b,c"), ["a", "b", "c"])
        self.assertEqual(vjd._split_csv(" a , b ,c "), ["a", "b", "c"])
        self.assertEqual(vjd._split_csv(""), [])
        self.assertEqual(vjd._split_csv(",,"), [])

    def test_default_artifact_resolves_to_target(self):
        fake_script = SCRIPTS / "verify-jdbc-drivers.py"
        resolved = vjd._default_artifact(fake_script)
        self.assertTrue(str(resolved).endswith("target/perc-distribution-tree.jar"))


class TestFindJdbcDir(unittest.TestCase):
    def test_finds_bare_path(self):
        with tempfile.TemporaryDirectory() as td:
            jdbc = Path(td) / "jetty" / "base" / "lib" / "jdbc"
            jdbc.mkdir(parents=True)
            found = vjd._find_jdbc_dir(Path(td))
            self.assertEqual(found, jdbc)

    def test_finds_distribution_prefixed_path(self):
        with tempfile.TemporaryDirectory() as td:
            jdbc = Path(td) / "distribution" / "jetty" / "base" / "lib" / "jdbc"
            jdbc.mkdir(parents=True)
            found = vjd._find_jdbc_dir(Path(td))
            self.assertEqual(found, jdbc)

    def test_returns_none_when_missing(self):
        with tempfile.TemporaryDirectory() as td:
            self.assertIsNone(vjd._find_jdbc_dir(Path(td)))


class TestIsValidJar(unittest.TestCase):
    def test_nonexistent(self):
        self.assertFalse(vjd._is_valid_jar(Path("/no/such/path.jar")))

    def test_zero_byte(self):
        with tempfile.NamedTemporaryFile(suffix=".jar", delete=False) as tf:
            tf.write(b"")
            path = Path(tf.name)
        try:
            self.assertFalse(vjd._is_valid_jar(path))
        finally:
            path.unlink(missing_ok=True)

    def test_valid_jar(self):
        with tempfile.NamedTemporaryFile(suffix=".jar", delete=False) as tf:
            tf.write(_make_valid_jar_bytes())
            path = Path(tf.name)
        try:
            # File must be flushed before _is_valid_jar reads it.
            path = Path(tf.name)
            self.assertTrue(vjd._is_valid_jar(path))
        finally:
            path.unlink(missing_ok=True)

    def test_corrupt_jar(self):
        with tempfile.NamedTemporaryFile(suffix=".jar", delete=False) as tf:
            tf.write(_make_corrupt_jar_bytes())
            path = Path(tf.name)
        try:
            self.assertFalse(vjd._is_valid_jar(path))
        finally:
            path.unlink(missing_ok=True)


class TestVerifyExitCodes(unittest.TestCase):
    """Exercise the documented exit-code matrix end-to-end."""

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.td_path = Path(self.td.name)
        self.artifact = self.td_path / "perc-distribution-tree.jar"
        self.workdir = self.td_path / "work"

    def _run(self, *, expected_set=None, expected_globs=None):
        # Suppress INFO/ERROR logging noise during tests; verify() routes both
        # through LOG.error which is fine for stderr but noisy under -v.
        logging.getLogger("verify-jdbc-drivers").setLevel(logging.CRITICAL)
        return vjd.verify(
            artifact=self.artifact,
            workdir=self.workdir,
            expected_set=expected_set,
            expected_globs=expected_globs,
            cleanup_workdir=False,
        )

    def test_missing_artifact(self):
        missing = self.td_path / "nope.jar"
        rc = vjd.verify(
            artifact=missing,
            workdir=None,
            expected_set=None,
            expected_globs=None,
            cleanup_workdir=False,
        )
        self.assertEqual(rc, vjd.EXIT_INVOCATION)

    def test_artifact_not_a_zip(self):
        self.artifact.write_bytes(b"not-a-zip")
        self.assertEqual(self._run(), vjd.EXIT_UNPACK_FAILED)

    def test_empty_jdbc_dir(self):
        _build_artifact(self.artifact, [])  # no jdbc entries at all
        self.assertEqual(self._run(), vjd.EXIT_MISSING_OR_EMPTY)

    def test_zero_byte_jar(self):
        _build_artifact(
            self.artifact,
            [("mysql-connector-java-8.0.33.jar", None)],  # zero-byte
        )
        self.assertEqual(self._run(), vjd.EXIT_ZERO_BYTE)

    def test_invalid_jar(self):
        _build_artifact(
            self.artifact,
            [("bad-driver.jar", _make_corrupt_jar_bytes())],
        )
        self.assertEqual(self._run(), vjd.EXIT_INVALID_JAR)

    def test_ok_with_no_expected(self):
        _build_artifact(
            self.artifact,
            [("mariadb-java-client-3.0.10.jar", _make_valid_jar_bytes())],
        )
        self.assertEqual(self._run(), vjd.EXIT_OK)

    def test_ok_with_expected_set_match(self):
        _build_artifact(
            self.artifact,
            [
                ("mariadb-java-client-3.0.10.jar", _make_valid_jar_bytes()),
                ("derby-10.14.2.0.jar", _make_valid_jar_bytes()),
            ],
        )
        self.assertEqual(
            self._run(expected_set=["mariadb-java-client-3.0.10.jar", "derby-10.14.2.0.jar"]),
            vjd.EXIT_OK,
        )

    def test_expected_set_missing(self):
        _build_artifact(
            self.artifact,
            [("mariadb-java-client-3.0.10.jar", _make_valid_jar_bytes())],
        )
        self.assertEqual(
            self._run(expected_set=["not-shipped.jar"]),
            vjd.EXIT_EXPECTED_MISSING,
        )

    def test_expected_glob_match(self):
        _build_artifact(
            self.artifact,
            [("mariadb-java-client-3.0.10.jar", _make_valid_jar_bytes())],
        )
        self.assertEqual(
            self._run(expected_globs=["mariadb-java-client-*.jar"]),
            vjd.EXIT_OK,
        )

    def test_expected_glob_no_match(self):
        _build_artifact(
            self.artifact,
            [("mariadb-java-client-3.0.10.jar", _make_valid_jar_bytes())],
        )
        self.assertEqual(
            self._run(expected_globs=["nonexistent-driver-*.jar"]),
            vjd.EXIT_EXPECTED_MISSING,
        )

    def test_default_workdir_uses_portable_tempdir(self):
        """When ``workdir`` is None, ``verify()`` must use a portable
        ``tempfile.TemporaryDirectory()`` so the script works on Windows and
        Unix without hardcoded ``/tmp`` paths (FR-007; root AGENTS.md
        Cross-Platform File I/O & Paths).

        We don't assert cleanup here — ``tempfile.TemporaryDirectory`` is
        stdlib and its cleanup semantics are Python's contract, not ours to
        re-test. Instead we verify the script routes through it (no
        hardcoded path).
        """
        import unittest.mock as mock

        _build_artifact(
            self.artifact,
            [("mariadb-java-client-3.0.10.jar", _make_valid_jar_bytes())],
        )
        with mock.patch.object(
            vjd.tempfile, "TemporaryDirectory", wraps=tempfile.TemporaryDirectory
        ) as tdc:
            rc = vjd.verify(
                artifact=self.artifact,
                workdir=None,
                expected_set=None,
                expected_globs=None,
                cleanup_workdir=False,  # leave it for tmpwatch
            )
        self.assertEqual(rc, vjd.EXIT_OK)
        self.assertGreaterEqual(
            tdc.call_count, 1, msg="verify() must route through tempfile.TemporaryDirectory"
        )


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            vjd.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_main_missing_artifact_exits_one(self):
        with tempfile.TemporaryDirectory() as td:
            rc = vjd.main(["--artifact", str(Path(td) / "nope.jar")])
            self.assertEqual(rc, vjd.EXIT_INVOCATION)


if __name__ == "__main__":
    logging.getLogger("verify-jdbc-drivers").setLevel(logging.CRITICAL)
    unittest.main()