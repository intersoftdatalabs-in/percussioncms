#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for rhythmyx_ready.py (#2462)."""

from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "rhythmyx_ready.py"
    name = "rhythmyx_ready"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(mod)
    return mod


rr = _load()


class FindContextFailureTests(unittest.TestCase):
    def test_empty_and_clean(self):
        self.assertIsNone(rr.find_rhythmyx_context_failure(""))
        self.assertIsNone(rr.find_rhythmyx_context_failure(None))  # type: ignore[arg-type]
        self.assertIsNone(
            rr.find_rhythmyx_context_failure(
                "INFO [Server] Started @7879ms\n"
                "INFO [AbstractConnector] Started {HTTP/1.1}{0.0.0.0:9992}\n"
            )
        )

    def test_failed_startup_of_context(self):
        text = (
            "WARN  [WebAppContext] Failed startup of context "
            "oeje11w.WebAppContext@…{ROOT,/,b=…Rhythmyx}\n"
            "org.springframework.beans.factory.UnsatisfiedDependencyException\n"
        )
        self.assertEqual(
            rr.find_rhythmyx_context_failure(text),
            "Failed startup of context",
        )

    def test_bean_currently_in_creation(self):
        text = (
            "Caused by: org.springframework.beans.factory."
            "BeanCurrentlyInCreationException:\n"
            "  Error creating bean with name 'folderHelper'\n"
        )
        self.assertEqual(
            rr.find_rhythmyx_context_failure(text),
            "BeanCurrentlyInCreationException",
        )

    def test_circular_reference_phrase(self):
        text = (
            "Is there an unresolvable circular reference or an "
            "asynchronous initialization dependency?\n"
        )
        self.assertEqual(
            rr.find_rhythmyx_context_failure(text),
            "Is there an unresolvable circular reference",
        )

    def test_installer_folderhelper_filename_not_a_match(self):
        """Installer unzip lines mention PercFolderHelper.js — not a context fail."""
        text = (
            "Unzipping to /tmp/x/jetty/base/webapps/Rhythmyx/cm/plugins/"
            "PercFolderHelper.js\n"
            "Creating file /tmp/x/…/PercFolderHelper.js\n"
        )
        self.assertIsNone(rr.find_rhythmyx_context_failure(text))


class ServerLogErrorTests(unittest.TestCase):
    def test_error_line(self):
        text = "INFO boot\nERROR [PSX] boom failed\n"
        match = rr.find_server_log_startup_error(text)
        self.assertIsNotNone(match)
        self.assertIn("ERROR", match)

    def test_empty_clean(self):
        self.assertIsNone(rr.find_server_log_startup_error(""))

    def test_fresh_h2_drop_table_not_found_allowlisted(self):
        """Upgrade cleanup DROP TABLE on missing tables must not fail qa-up (#2540)."""
        text = (
            '[PSExecSQLStmt] 04:40:52.843 [main] ERROR com.percussion.install.PSLogger '
            '- Table "PSX_QJOB_LISTENERS" not found; SQL statement:\n'
            "INFO install continues\n"
        )
        self.assertIsNone(rr.find_server_log_startup_error(text))

    def test_fresh_h2_empty_db_delete_not_found_allowlisted(self):
        text = (
            '[PSExecSQLStmt] 04:50:29.951 [main] ERROR com.percussion.install.PSLogger '
            '- Table "PSX_CONTENTCHANGEEVENT" not found (this database is empty); '
            "SQL statement:\n"
        )
        self.assertIsNone(rr.find_server_log_startup_error(text))

    def test_runtime_error_still_fails(self):
        text = "ERROR [Assembly] Failed to save template id=0-4-1003\n"
        match = rr.find_server_log_startup_error(text)
        self.assertIsNotNone(match)
        self.assertIn("Failed to save template", match)

    def test_install_sql_syntax_error_allowlisted(self):
        text = (
            '[PSExecSQLStmt] 05:00:17.816 [main] ERROR com.percussion.install.PSLogger '
            '- Syntax error in SQL statement "[*]RENAME COLUMN RXS_CT_GENERIC.USAGE '
            'TO PUSAGE"; expected "ROLLBACK, REVOKE"; SQL statement:\n'
        )
        self.assertIsNone(rr.find_server_log_startup_error(text))

    def test_package_install_failure_still_fails(self):
        text = (
            "ERROR [Server] Package: perc.Baseline failed to install: "
            "com.percussion.error.PSException\n"
        )
        match = rr.find_server_log_startup_error(text)
        self.assertIsNotNone(match)
        self.assertIn("perc.Baseline", match)

    def test_psdatahandler_cehandler_allowlisted(self):
        text = (
            "ERROR [com.percussion.data.PSDataHandler] Application .sys_CEHandler1, "
            "Dataset InsertChild229, Request InsertChild229\n"
        )
        self.assertIsNone(rr.find_server_log_startup_error(text))

    def test_container_cms_log_paths(self):
        paths = rr.container_cms_log_paths("/opt/Percussion")
        self.assertIn("/opt/Percussion/jetty/base/logs/server.log", paths)
        self.assertIn("/opt/Percussion/rxconfig/Installer/install.log", paths)
        self.assertIn("/opt/Percussion/rxconfig/Installer/tablefactory.log", paths)


class AssessReadyTests(unittest.TestCase):
    def test_http_ready_clean_logs(self):
        ok, detail = rr.assess_rhythmyx_ready(200, "Server Started")
        self.assertTrue(ok)
        self.assertEqual(detail, "ok")

    def test_http_ready_but_context_failed(self):
        ok, detail = rr.assess_rhythmyx_ready(
            200,
            "WARN [WebAppContext] Failed startup of context Rhythmyx",
        )
        self.assertFalse(ok)
        self.assertIn(rr.DETAIL_CONTEXT_FAILED, detail)
        self.assertIn("Failed startup of context", detail)

    def test_http_ready_but_server_log_error(self):
        ok, detail = rr.assess_rhythmyx_ready(
            200,
            "INFO docker clean",
            server_log_text="INFO ok\nERROR [PSX] residual\n",
        )
        self.assertFalse(ok)
        self.assertIn(rr.DETAIL_SERVER_LOG_ERRORS, detail)

    def test_http_not_ready_clean_logs(self):
        ok, detail = rr.assess_rhythmyx_ready(0, "")
        self.assertFalse(ok)
        self.assertIn("http_not_ready", detail)

    def test_context_fail_wins_over_http(self):
        ok, detail = rr.assess_rhythmyx_ready(
            302,
            "BeanCurrentlyInCreationException: folderHelper",
        )
        self.assertFalse(ok)
        self.assertIn(rr.DETAIL_CONTEXT_FAILED, detail)

    def test_is_http_ready_codes(self):
        for code in (200, 302, 401, 403):
            self.assertTrue(rr.is_http_ready_code(code), code)
        for code in (0, 404, 500, 503):
            self.assertFalse(rr.is_http_ready_code(code), code)


class ProbeUrlMatrixTests(unittest.TestCase):
    """#2482 — documented probe URL matrix + assess_probe_url."""

    def test_matrix_invariants(self):
        """Every matrix entry has a valid path, role, and source note."""
        self.assertGreater(len(rr.PROBE_URL_MATRIX), 0)
        seen_roles = set()
        for path, spec in rr.PROBE_URL_MATRIX.items():
            self.assertTrue(path.startswith("/"), f"path {path!r} must start with /")
            self.assertEqual(spec.path, path)
            self.assertIn(
                spec.recommended_role,
                {"primary", "secondary", "fallback", "avoid"},
                f"unknown role {spec.recommended_role!r} for {path}",
            )
            self.assertTrue(
                spec.source,
                f"empty source note for {path}",
            )
            seen_roles.add(spec.recommended_role)
        # Matrix must include at least one primary and at least one avoid
        # entry — otherwise it is not a real matrix.
        self.assertIn("primary", seen_roles)
        self.assertIn("avoid", seen_roles)

    def test_default_primary_is_known_primary(self):
        """DEFAULT_PROBE_URL_PRIMARY is in the matrix and tagged ``primary``."""
        self.assertIsNotNone(rr.DEFAULT_PROBE_URL_PRIMARY)
        spec, verdict = rr.assess_probe_url(rr.DEFAULT_PROBE_URL_PRIMARY)  # type: ignore[arg-type]
        self.assertIsNotNone(spec)
        self.assertEqual(verdict, "known_primary")
        self.assertTrue(spec.implies_spring_context)  # type: ignore[union-attr]

    def test_default_secondary_is_known_secondary(self):
        """DEFAULT_PROBE_URL_SECONDARY is in the matrix and tagged ``secondary``."""
        self.assertIsNotNone(rr.DEFAULT_PROBE_URL_SECONDARY)
        spec, verdict = rr.assess_probe_url(rr.DEFAULT_PROBE_URL_SECONDARY)  # type: ignore[arg-type]
        self.assertIsNotNone(spec)
        self.assertEqual(verdict, "known_secondary")

    def test_login_path_is_fallback_not_spring_implied(self):
        """Legacy ``/Rhythmyx/login`` is matrix-tagged fallback — proves the
        weak-signal warning is encoded in the matrix (not just in docs)."""
        spec, verdict = rr.assess_probe_url("/Rhythmyx/login")
        self.assertIsNotNone(spec)
        self.assertEqual(verdict, "known_fallback")
        self.assertFalse(spec.implies_spring_context)  # type: ignore[union-attr]

    def test_openapi_paths_are_avoided(self):
        """The /openapi webapp is independent of the Rhythmyx Spring context."""
        for path in ("/Rhythmyx/openapi/openapi.json", "/Rhythmyx/openapi/index.html"):
            spec, verdict = rr.assess_probe_url(path)
            self.assertIsNotNone(spec, path)
            self.assertEqual(verdict, "known_avoid", path)
            self.assertFalse(spec.implies_spring_context)  # type: ignore[union-attr]

    def test_unknown_path_is_not_a_matrix_member(self):
        """Unrecognized paths get ``unknown`` verdict, never a fake ``known_*``."""
        spec, verdict = rr.assess_probe_url("/Rhythmyx/rest/widgets/zzz")
        self.assertIsNone(spec)
        self.assertEqual(verdict, "unknown")

    def test_empty_path_is_empty(self):
        """Empty / None path yields ``empty`` verdict (never raises)."""
        for raw in ("", None, "   "):
            spec, verdict = rr.assess_probe_url(raw)  # type: ignore[arg-type]
            self.assertIsNone(spec)
            self.assertEqual(verdict, "empty")

    def test_assess_rhythmyx_ready_unchanged_by_matrix(self):
        """Adding the matrix must not regress the existing assessor contract."""
        # Same ready / not-ready verdicts as the AssessReadyTests cases above.
        self.assertTrue(rr.assess_rhythmyx_ready(200, "")[0])
        self.assertFalse(rr.assess_rhythmyx_ready(200, "Failed startup of context")[0])
        self.assertFalse(rr.assess_rhythmyx_ready(0, "")[0])


if __name__ == "__main__":
    unittest.main()
