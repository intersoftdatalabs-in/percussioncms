#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for check-no-glob-deletes.py (no network, no Maven).

Builds synthetic install.xml files in a tempdir and exercises every exit
code documented in the script's ``## Behavioral Notes``.
"""

from __future__ import annotations

import importlib.util
import logging
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "check-no-glob-deletes.py"
    name = "check_no_glob_deletes"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


cng = _load()


def _write_install_xml(path: Path, target_body: str) -> None:
    """Write a minimal install.xml wrapping ``target_body`` inside the
    ``install_jdbc_drivers`` target.
    """
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        f"""<?xml version="1.0" encoding="UTF-8"?>
<project name="perc-distribution-tree" default="install">
    <target name="install_jdbc_drivers">
{target_body}
    </target>
    <target name="unrelated">
        <delete>
            <include name="*.tmp"/>
        </delete>
    </target>
</project>
""",
        encoding="utf-8",
    )


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            cng.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_unknown_arg_exits_two(self):
        with self.assertRaises(SystemExit) as cm:
            cng.main(["--not-a-flag"])
        self.assertEqual(cm.exception.code, 2)


class TestDefaultPath(unittest.TestCase):
    def test_default_install_xml_resolves_to_distribution_rsrc(self):
        fake_script = SCRIPTS / "check-no-glob-deletes.py"
        resolved = cng._default_install_xml(fake_script)
        # Cross-platform: Path.as_posix() always uses '/' so the suffix
        # comparison works on Windows (where str(Path) uses '\\') and Unix.
        self.assertTrue(
            resolved.as_posix().endswith(
                "src/main/resources/distribution/rxconfig/Installer/install.xml"
            )
        )


class TestCheck(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.td_path = Path(self.td.name)

    def _check(self, install_xml: Path) -> int:
        logging.getLogger("check-no-glob-deletes").setLevel(logging.CRITICAL)
        return cng.check(install_xml)

    def test_missing_install_xml(self):
        missing = self.td_path / "no-such.xml"
        self.assertEqual(self._check(missing), cng.EXIT_INVOCATION)

    def test_invalid_xml(self):
        bad = self.td_path / "bad.xml"
        bad.write_text("not <xml at all", encoding="utf-8")
        self.assertEqual(self._check(bad), cng.EXIT_INVOCATION)

    def test_missing_target(self):
        xml = self.td_path / "install.xml"
        xml.write_text(
            "<project><target name=\"other\"/></project>",
            encoding="utf-8",
        )
        self.assertEqual(self._check(xml), cng.EXIT_INVOCATION)

    def test_no_delete_block(self):
        xml = self.td_path / "install.xml"
        xml.write_text(
            (
                "<project><target name=\"install_jdbc_drivers\">"
                "<copy><include name=\"a.jar\"/></copy>"
                "</target></project>"
            ),
            encoding="utf-8",
        )
        self.assertEqual(self._check(xml), cng.EXIT_INVOCATION)

    def test_ok_exact_filenames(self):
        xml = self.td_path / "install.xml"
        _write_install_xml(
            xml,
            """        <delete>
            <include name="mariadb-java-client-3.0.10.jar"/>
            <include name="derby-10.14.2.0.jar"/>
        </delete>""",
        )
        self.assertEqual(self._check(xml), cng.EXIT_OK)

    def test_fail_star_glob(self):
        xml = self.td_path / "install.xml"
        _write_install_xml(
            xml,
            """        <delete>
            <include name="mysql-connector-java-*.jar"/>
        </delete>""",
        )
        self.assertEqual(self._check(xml), cng.EXIT_GLOB_FOUND)

    def test_fail_question_mark_glob(self):
        xml = self.td_path / "install.xml"
        _write_install_xml(
            xml,
            """        <delete>
            <include name="derbyclient-?.0.0.jar"/>
        </delete>""",
        )
        self.assertEqual(self._check(xml), cng.EXIT_GLOB_FOUND)

    def test_unrelated_target_globs_are_ignored(self):
        """The first ``<delete>`` inside ``install_jdbc_drivers`` is the
        only one inspected. A glob elsewhere in install.xml must NOT cause
        the check to fail (mirrors the original POSIX script's narrow
        scope).
        """
        xml = self.td_path / "install.xml"
        _write_install_xml(
            xml,
            """        <delete>
            <include name="mariadb-java-client-3.0.10.jar"/>
        </delete>""",
        )
        self.assertEqual(self._check(xml), cng.EXIT_OK)

    def test_first_delete_block_only(self):
        """Two ``<delete>`` blocks inside install_jdbc_drivers: the first
        has exact filenames, the second has a glob. The original POSIX
        script inspects the FIRST only — this test pins that semantic so
        a future refactor doesn't accidentally widen the scope to match
        ``InstallXmlDeleteSetTest``'s broader check.
        """
        xml = self.td_path / "install.xml"
        _write_install_xml(
            xml,
            """        <delete>
            <include name="mariadb-java-client-3.0.10.jar"/>
        </delete>
        <delete>
            <include name="bad-*.jar"/>
        </delete>""",
        )
        self.assertEqual(self._check(xml), cng.EXIT_OK)


class TestMain(unittest.TestCase):
    def test_main_help(self):
        with self.assertRaises(SystemExit) as cm:
            cng.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_main_missing_install_xml_exits_one(self):
        with tempfile.TemporaryDirectory() as td:
            rc = cng.main(["--install-xml", str(Path(td) / "no-such.xml")])
            self.assertEqual(rc, cng.EXIT_INVOCATION)


if __name__ == "__main__":
    logging.getLogger("check-no-glob-deletes").setLevel(logging.CRITICAL)
    unittest.main()