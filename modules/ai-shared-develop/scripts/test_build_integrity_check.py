#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for build-integrity-check.py (no cosign, no git, no network).

Pure stdlib tests exercise the hash-verification path, the missing-
sidecar / missing-cosign skip paths, and the git-email-derived identity
regex derivation. ``cosign`` is mocked out for the signature-verification
path so the test does not require the binary.
"""

from __future__ import annotations

import hashlib
import importlib.util
import logging
import subprocess
import sys
import tempfile
import unittest
import unittest.mock
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "build-integrity-check.py"
    name = "build_integrity_check"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


bic = _load()


class TestArgParser(unittest.TestCase):
    def test_help_exits_cleanly(self):
        with self.assertRaises(SystemExit) as cm:
            bic.main(["--help"])
        self.assertEqual(cm.exception.code, 0)

    def test_missing_resource_arg_errors(self):
        with self.assertRaises(SystemExit) as cm:
            bic.main([])
        self.assertEqual(cm.exception.code, 2)


class TestSha256File(unittest.TestCase):
    def test_matches_known_value(self):
        with tempfile.NamedTemporaryFile(delete=False) as tf:
            tf.write(b"hello world")
            path = Path(tf.name)
        try:
            self.assertEqual(
                bic._sha256_file(path),
                "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
            )
        finally:
            path.unlink(missing_ok=True)


class TestVerifyHash(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.td_path = Path(self.td.name)
        logging.getLogger("build-integrity-check").setLevel(logging.CRITICAL)

    def _write_resource(self, content: bytes = b"fixture content"):
        resource = self.td_path / "foo.md"
        resource.write_bytes(content)
        # Write matching sha256 sidecar (sha256sum -c format: "<hex>  <filename>")
        h = hashlib.sha256(content).hexdigest()
        sidecar = self.td_path / "foo.md.sha256"
        sidecar.write_text(f"{h}  foo.md\n", encoding="utf-8")
        return resource, sidecar

    def test_matching_hash_passes(self):
        resource, _ = self._write_resource()
        self.assertEqual(bic._verify_hash(resource), bic.EXIT_OK)

    def test_mismatched_hash_fails(self):
        resource, sidecar = self._write_resource(b"original")
        # Tamper with the sidecar to claim a wrong hash.
        sidecar.write_text("0" * 64 + "  foo.md\n", encoding="utf-8")
        self.assertEqual(bic._verify_hash(resource), bic.EXIT_INTEGRITY_FAILED)

    def test_missing_sidecar_warns_and_passes(self):
        resource = self.td_path / "foo.md"
        resource.write_bytes(b"x")
        # No sidecar.
        self.assertEqual(bic._verify_hash(resource), bic.EXIT_OK)

    def test_unparseable_sidecar_warns_and_passes(self):
        resource = self.td_path / "foo.md"
        resource.write_bytes(b"x")
        sidecar = self.td_path / "foo.md.sha256"
        sidecar.write_text("# not a valid line\n", encoding="utf-8")
        self.assertEqual(bic._verify_hash(resource), bic.EXIT_OK)


class TestIdentityRegexFromGit(unittest.TestCase):
    def test_no_email_returns_catchall(self):
        with unittest.mock.patch.object(bic.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=0, stdout="", stderr=""
            )
            self.assertEqual(bic._identity_regex_from_git(), ".*")

    def test_email_with_domain_extracts_domain(self):
        with unittest.mock.patch.object(bic.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=0, stdout="user@example.com\n", stderr=""
            )
            self.assertEqual(bic._identity_regex_from_git(), ".*@example.com")

    def test_email_without_at_sign_returns_catchall(self):
        with unittest.mock.patch.object(bic.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=0, stdout="not-an-email\n", stderr=""
            )
            self.assertEqual(bic._identity_regex_from_git(), ".*")

    def test_git_missing_returns_catchall(self):
        with unittest.mock.patch.object(
            bic.subprocess, "run", side_effect=FileNotFoundError
        ):
            self.assertEqual(bic._identity_regex_from_git(), ".*")


class TestVerifySignature(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.td_path = Path(self.td.name)
        logging.getLogger("build-integrity-check").setLevel(logging.CRITICAL)

    def test_missing_sig_file_skips(self):
        resource = self.td_path / "foo.md"
        resource.write_bytes(b"x")
        self.assertEqual(
            bic._verify_signature(resource, id_regexp=".*"),
            bic.EXIT_OK,
        )

    def test_cosign_missing_skips(self):
        resource = self.td_path / "foo.md"
        resource.write_bytes(b"x")
        sig = self.td_path / "foo.md.sha256.sig"
        sig.write_bytes(b"{}")
        with unittest.mock.patch.object(bic.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=1, stdout="", stderr="cosign not found"
            )
            self.assertEqual(
                bic._verify_signature(resource, id_regexp=".*"),
                bic.EXIT_OK,
            )

    def test_cosign_success(self):
        resource = self.td_path / "foo.md"
        resource.write_bytes(b"x")
        (self.td_path / "foo.md.sha256").write_text("ignored\n", encoding="utf-8")
        sig = self.td_path / "foo.md.sha256.sig"
        sig.write_bytes(b"{}")
        with unittest.mock.patch.object(bic.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=0, stdout="verified", stderr=""
            )
            self.assertEqual(
                bic._verify_signature(
                    resource, id_regexp=".*@example.com",
                    cosign_path=["cosign"],
                ),
                bic.EXIT_OK,
            )

    def test_cosign_failure_propagates(self):
        resource = self.td_path / "foo.md"
        resource.write_bytes(b"x")
        (self.td_path / "foo.md.sha256").write_text("ignored\n", encoding="utf-8")
        sig = self.td_path / "foo.md.sha256.sig"
        sig.write_bytes(b"{}")
        with unittest.mock.patch.object(bic.subprocess, "run") as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(
                args=[], returncode=1, stdout="", stderr="verification failed"
            )
            self.assertEqual(
                bic._verify_signature(
                    resource, id_regexp=".*@example.com",
                    cosign_path=["cosign"],
                ),
                bic.EXIT_AUTHENTICITY_FAILED,
            )


class TestMain(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.td_path = Path(self.td.name)
        logging.getLogger("build-integrity-check").setLevel(logging.CRITICAL)

    def test_main_missing_resource_skips(self):
        rc = bic.main([str(self.td_path / "no-such-file")])
        self.assertEqual(rc, bic.EXIT_OK)

    def test_main_good_hash_passes(self):
        resource = self.td_path / "foo.md"
        resource.write_bytes(b"ok")
        h = hashlib.sha256(b"ok").hexdigest()
        (self.td_path / "foo.md.sha256").write_text(f"{h}  foo.md\n", encoding="utf-8")
        # Stub out _verify_signature so we don't need cosign or git.
        with unittest.mock.patch.object(bic, "_verify_signature", return_value=bic.EXIT_OK):
            rc = bic.main([str(resource)])
        self.assertEqual(rc, bic.EXIT_OK)

    def test_main_bad_hash_fails(self):
        resource = self.td_path / "foo.md"
        resource.write_bytes(b"actual content")
        # Wrong hash in sidecar.
        (self.td_path / "foo.md.sha256").write_text("0" * 64 + "  foo.md\n", encoding="utf-8")
        rc = bic.main([str(resource)])
        self.assertEqual(rc, bic.EXIT_INTEGRITY_FAILED)


if __name__ == "__main__":
    logging.getLogger("build-integrity-check").setLevel(logging.CRITICAL)
    unittest.main()