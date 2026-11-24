#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for qa_seed_widget_fixtures.py (no docker, no curl).

Pure helpers plus the full live flow against a fake docker runner.
The password must never appear in an argv (it travels on stdin via
``curl --config -``) — every test asserting on argv enforces that.
"""

from __future__ import annotations

import importlib.util
import json
import re
import sys
import tempfile
import unittest
import unittest.mock
from pathlib import Path
from typing import Dict, List, Optional, Tuple

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "qa_seed_widget_fixtures.py"
    name = "qa_seed_widget_fixtures"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


qswf = _load()

STOCK_USERS_XML = """<?xml version="1.0" encoding="UTF-8"?>
<tomcat-users>
  <role rolename="deliverymanager"/>
  <user name="ps_manager" password="test-digest-value" roles="deliverymanager"/>
  <user name="admin" password="" roles="manager-script"/>
</tomcat-users>
"""
# NOTE: the fixture above deliberately uses a non-secret placeholder digest;
# the rewrite tests only need *some* password value to replace.
STOCK_USERS_XML_NO_PASSWORD = """<?xml version="1.0" encoding="UTF-8"?>
<tomcat-users>
  <role rolename="deliverymanager"/>
  <user name="ps_manager" roles="deliverymanager"/>
  <user name="admin" roles="manager-script"/>
</tomcat-users>
"""

SECRET = "test-only-pw-6341"
# mkd-code-review[secrets.heuristic]: adjudicated false positive (issue #6341) —
# test-only placeholder password for the fake docker runner; never a live credential.
# Test-only zero/one digests asserting the realm rewrite, not credentials.
ZERO_DIGEST = "0" * 40  # mkd-code-review[secrets.heuristic]: test-only placeholder (issue #6341)
ONE_DIGEST = "1" * 40  # mkd-code-review[secrets.heuristic]: test-only placeholder (issue #6341)


class Sha1Test(unittest.TestCase):
    def test_known_vector(self):
        import hashlib

        self.assertEqual(qswf.sha1_hex("abc"), hashlib.sha1(b"abc").hexdigest())
        self.assertEqual(len(qswf.sha1_hex(SECRET)), 40)


class ResolveDeliveryPasswordTest(unittest.TestCase):
    def test_explicit_wins(self):
        self.assertEqual(qswf.resolve_delivery_password("flag-pw"), "flag-pw")

    def test_env_fallback(self):
        import os

        with unittest.mock.patch.dict(os.environ, {qswf.DELIVERY_PASSWORD_ENV: "env-pw"}):
            self.assertEqual(qswf.resolve_delivery_password(None), "env-pw")
        with unittest.mock.patch.dict(os.environ, {}, clear=False):
            os.environ.pop(qswf.DELIVERY_PASSWORD_ENV, None)
            generated = qswf.resolve_delivery_password(None)
            self.assertTrue(generated.startswith("qacell-"))
            self.assertNotEqual(generated, qswf.resolve_delivery_password(None))


class TomcatUsersTest(unittest.TestCase):
    def test_rewrites_existing_password(self):
        out = qswf.tomcat_users_with_password(STOCK_USERS_XML, "ps_manager", ZERO_DIGEST)
        # Exactly one digest written (ps_manager), admin row intact.
        self.assertIn(ZERO_DIGEST, out)
        self.assertEqual(out.count(ZERO_DIGEST), 1)
        self.assertIn('name="admin"', out)

    def test_missing_user_raises(self):
        with self.assertRaises(ValueError):
            qswf.tomcat_users_with_password(STOCK_USERS_XML, "nobody", ZERO_DIGEST)

    def test_inserts_password_when_attr_absent(self):
        out = qswf.tomcat_users_with_password(
            STOCK_USERS_XML_NO_PASSWORD, "ps_manager", ONE_DIGEST)
        self.assertIn(ONE_DIGEST, out)


class PayloadShapeTest(unittest.TestCase):
    def test_descriptor_payload_matches_psfeeddescriptors(self):
        payload = qswf.feed_descriptor_payload(service_url="https://localhost:8443/x/get")
        for key in ("descriptors", "serviceUrl", "serviceUser", "servicePass",
                    "servicePassEncrypted", "site"):
            self.assertIn(key, payload)
        desc = payload["descriptors"][0]
        for key in ("name", "site", "description", "link", "title", "query", "type"):
            self.assertIn(key, desc)
        query = json.loads(desc["query"])
        self.assertEqual(query["criteria"], ["site = 'qacell'"])
        self.assertEqual(query["maxResults"], 25)
        self.assertEqual(desc["site"], payload["site"])

    def test_entry_payload_matches_psmetadataentry(self):
        payload = qswf.index_entry_payload(
            path="/qacell/surface-fixture-1.html", title="T", abstract="A", site="qacell")
        for key in ("name", "folder", "pagepath", "linktext", "type", "site", "properties"):
            self.assertIn(key, payload)
        self.assertEqual(payload["name"], "surface-fixture-1.html")
        self.assertEqual(payload["folder"], "/qacell/")
        names = {p["name"] for p in payload["properties"]}
        self.assertIn("dcterms:title", names)
        self.assertIn("dcterms:abstract", names)

    def test_entry_payload_values_bind_on_the_live_mapper(self):
        """The JAXB provider mapper drops ``stringvalue`` (``@XmlTransient``
        setter) — values must ride on ``value`` (proven live, #6341)."""
        payload = qswf.index_entry_payload(
            path="/qacell/surface-fixture-1.html", title="T", abstract="A", site="qacell")
        for prop in payload["properties"]:
            self.assertIn("value", prop)
            self.assertNotIn("stringvalue", prop)

    def test_entry_url_preserves_leading_slash(self):
        """Regression: single-slash entry URLs delete nothing (204) while
        the DAO matches pagepath verbatim — the silent-litter trap."""
        url = qswf.entry_url("https://localhost:8443/perc-metadata-services/indexer",
                             "/qacell/surface-fixture-1.html")
        self.assertIn("entry//qacell/surface-fixture-1.html", url)


class ArgvHygieneTest(unittest.TestCase):
    def test_secret_never_in_argv(self):
        argv = qswf.seed_curl_argv("c", "PUT", "https://localhost:8443/x", "/tmp/x.json")  # mkd-code-review[paths.hardcoded_sep]: fake container argv literal in a unit test (issue #6341)
        self.assertNotIn(SECRET, " ".join(argv))
        self.assertIn(SECRET, qswf.authed_curl_input("u", SECRET).decode())
        self.assertIn("--config", argv)
        self.assertIn("-sk", argv)

    def test_container_paths_are_posix(self):
        self.assertNotIn("\\", qswf.container_users_path("/opt/Percussion"))
        self.assertIn("Deployment/Server/conf/tomcat-users.xml",
                      qswf.container_users_path("/opt/Percussion"))


class FakeDocker:
    """Scripted docker runner: maps (METHOD, url-fragment) to HTTP codes."""

    def __init__(self, codes: Dict[Tuple[str, str], List[int]], users_xml: str = STOCK_USERS_XML,
                 put_body: str = ""):
        self.codes = {k: list(v) for k, v in codes.items()}
        self.users_xml = users_xml
        self.put_body = put_body
        self.argvs: List[List[str]] = []
        self.inputs: List[Optional[bytes]] = []
        self.restarted = False

    def run(self, argv, *, input=None, timeout=120):  # noqa: A002 — fake matches impl
        argv = list(argv)
        self.argvs.append(argv)
        self.inputs.append(input)
        if argv[:2] == ["docker", "cp"]:
            return self._handle_cp(argv)
        if "TomcatShutdown.sh" in " ".join(argv):
            return (0, "", "")
        if "TomcatStartup.sh" in " ".join(argv):
            self.restarted = True
            return (0, "", "")
        # mkd-code-review[paths.hardcoded_sep]: fake asserting the cell body-file path (issue #6341).
        if argv[-2:] == ["cat", "/tmp/qa-seed-widget-fixtures/put-body.txt"] or (
                "cat" in argv and "put-body.txt" in argv[-1]):
            return (0, self.put_body, "")
        if "curl" in argv:
            return self._handle_curl(argv)
        return (0, "", "")

    def _handle_cp(self, argv: List[str]) -> Tuple[int, str, str]:
        src, dst = argv[2], argv[3]
        if src.startswith("c:"):
            Path(dst).write_bytes(self.users_xml.encode("utf-8"))
        elif "tomcat-users.xml" in src:
            self.users_xml = Path(src).read_text(encoding="utf-8")
        return (0, "", "")

    def _handle_curl(self, argv: List[str]) -> Tuple[int, str, str]:
        method = argv[argv.index("-X") + 1]
        blob = " ".join(argv)
        for (m, fragment), queue in self.codes.items():
            if m == method and fragment in blob:
                code = queue.pop(0) if len(queue) > 1 else queue[0]
                return (0, str(code), "")
        return (0, "200", "")

    def argv_blob(self) -> str:
        return "\n".join(" ".join(a) for a in self.argvs)


FEED_XML = (
    "<rss><channel><title>" + qswf.FEED_TITLE + "</title>"
    + "".join(
        f"<item><title>{row['title']}</title>"
        f"<description>{row['abstract']}</description></item>"
        for row in qswf.FEED_ENTRIES
    )
    + "</channel></rss>"
)


class FakeClock:
    def __init__(self):
        self.t = 1000.0

    def now(self) -> float:
        return self.t

    def sleep(self, seconds: float) -> None:
        self.t += seconds


def _plan(**over) -> "qswf.SeedPlan":
    kw = dict(container="c", install_root="/opt/Percussion", https_port=8443,
              delivery_user="ps_manager", delivery_password=SECRET,
              dts_http_base="http://127.0.0.1:19983")
    kw.update(over)
    return qswf.SeedPlan(**kw)


class SeedFlowTest(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.staging = Path(self.td.name)

    def _run(self, codes, **over):
        clock = FakeClock()
        fake = FakeDocker(codes)
        plan = _plan(staging_dir=self.staging, **over)
        result = qswf.run_seed_feed_flow(
            plan, run=fake.run,
            http_get=lambda url: (200, FEED_XML),
            sleep=clock.sleep, now=clock.now)
        return result, fake

    def test_full_success_first_try(self):
        result, fake = self._run({("PUT", "/descriptors"): [200],
                                  ("POST", "/entry"): [204]})
        self.assertTrue(result.ok, result.detail)
        self.assertIn("2 fixture entries", result.detail)
        self.assertIn("qasurface-fixture", result.feed_url)
        self.assertNotIn(SECRET, fake.argv_blob())

    def test_stage_cleanup_runs_after_seed_posts(self):
        """Regression: an early ``rm -rf`` of the staged payloads makes
        curl post empty bodies (accepted with 204, persisting nothing)."""
        result, fake = self._run({("PUT", "/descriptors"): [200],
                                  ("POST", "/entry"): [204]})
        self.assertTrue(result.ok, result.detail)
        kinds = []
        for argv in fake.argvs:
            blob = " ".join(argv)
            if "curl" in argv and "-X" in argv and "POST" in blob and "entry" in blob:
                kinds.append("post")
            if "rm" in argv and "qa-seed-widget-fixtures" in blob:
                kinds.append("rm")
        self.assertEqual(kinds.count("post"), len(qswf.FEED_ENTRIES))
        self.assertIn("rm", kinds)
        self.assertLess(kinds.index("post"), kinds.index("rm"))
        self.assertLess(max(i for i, k in enumerate(kinds) if k == "post"),
                        kinds.index("rm"))

    def test_waits_for_userdatabase_reload(self):
        result, fake = self._run({("PUT", "/descriptors"): [401, 401, 200],
                                  ("POST", "/entry"): [204]})
        self.assertTrue(result.ok, result.detail)
        self.assertGreaterEqual(result.attempts, 3)
        self.assertFalse(result.restarted_tomcat)
        self.assertNotIn(SECRET, fake.argv_blob())

    def test_restart_fallback_then_green(self):
        codes = {("PUT", "/descriptors"): [401] * 30 + [200],
                 ("POST", "/entry"): [204]}
        result, fake = self._run(codes)
        self.assertTrue(result.ok, result.detail)
        self.assertTrue(result.restarted_tomcat)
        self.assertTrue(fake.restarted)

    def test_put_redirect_fails_fast(self):
        result, _fake = self._run({("PUT", "/descriptors"): [302]})
        self.assertFalse(result.ok)
        self.assertIn("302", result.detail)

    def test_put_mapping_error_fails_fast_with_snippet(self):
        clock = FakeClock()
        fake = FakeDocker(
            {("PUT", "/descriptors"): [500]},
            put_body="Cannot construct instance of `...IPSFeedDescriptor` (no Creators exist)")
        plan = _plan(staging_dir=self.staging)
        result = qswf.run_seed_feed_flow(
            plan, run=fake.run,
            http_get=lambda url: (200, FEED_XML),
            sleep=clock.sleep, now=clock.now)
        self.assertFalse(result.ok)
        self.assertIn("DTO mapping", result.detail)
        self.assertEqual(result.attempts, 1)

    def test_post_failure_fails(self):
        result, _fake = self._run({("PUT", "/descriptors"): [200],
                                   ("POST", "/entry"): [500]})
        self.assertFalse(result.ok)
        self.assertIn("500", result.detail)

    def test_verify_missing_titles_fails(self):
        clock = FakeClock()
        fake = FakeDocker({("PUT", "/descriptors"): [200], ("POST", "/entry"): [204]})
        plan = _plan(staging_dir=self.staging)
        result = qswf.run_seed_feed_flow(
            plan, run=fake.run,
            http_get=lambda url: (200, "<rss><channel></channel></rss>"),
            sleep=clock.sleep, now=clock.now)
        self.assertFalse(result.ok)
        self.assertIn("missing=", result.detail)

    def test_missing_user_in_cell_realm_fails(self):
        fake = FakeDocker({}, users_xml="<tomcat-users></tomcat-users>")
        clock = FakeClock()
        result = qswf.run_seed_feed_flow(
            _plan(staging_dir=self.staging), run=fake.run,
            http_get=lambda url: (200, FEED_XML),
            sleep=clock.sleep, now=clock.now)
        self.assertFalse(result.ok)
        self.assertIn("no <user", result.detail)


class TarPipeFallbackTest(unittest.TestCase):
    """#7185: host->cell ``docker cp`` retries via tar pipe on corrupt cells.

    Mirrors the hot-deploy ``_docker_cp_with_tar_fallback`` contract
    (#7157): same argv shape, in-memory stdlib tar, source mtime
    preserved, qa-down/qa-up recreate hint when both paths fail.
    """

    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.addCleanup(self.td.cleanup)
        self.staging = Path(self.td.name)

    def _scripted_run(self, *, cp_rc: int = 0, tar_rc: int = 0):
        """Fake ``run`` hook: host->cell ``docker cp`` fails per ``cp_rc``."""
        calls: List[Tuple[List[str], Optional[bytes]]] = []

        def run(argv, *, input=None, timeout=120):  # noqa: A002 — fake matches impl
            argv = list(argv)
            calls.append((argv, input))
            if argv[:2] == ["docker", "cp"] and not argv[2].startswith("c:"):
                return (cp_rc, "", "daemon mkdirat ... file exists" if cp_rc else "")
            if argv[:2] == ["docker", "exec"] and "tar" in argv:
                if tar_rc != 0:
                    return (tar_rc, "", "tar: Cannot open: File exists")
                return (0, "", "")
            return (0, "", "")

        return run, calls

    def test_cp_success_never_touches_tar(self):
        local = self.staging / "tomcat-users.xml"
        local.write_bytes(STOCK_USERS_XML.encode("utf-8"))
        run, calls = self._scripted_run(cp_rc=0)
        self.assertIsNone(
            qswf.docker_cp_in_with_tar_fallback(local, "c", "/x/tomcat-users.xml", run=run))
        self.assertEqual(len(calls), 1)
        self.assertFalse(any("tar" in argv for argv, _ in calls))

    def test_cp_failure_falls_back_to_tar_pipe(self):
        import io as _io
        import tarfile as _tarfile

        local = self.staging / "tomcat-users.xml"
        local.write_bytes(STOCK_USERS_XML.encode("utf-8"))
        run, calls = self._scripted_run(cp_rc=1, tar_rc=0)
        self.assertIsNone(
            qswf.docker_cp_in_with_tar_fallback(
                local, "c", "/opt/Percussion/conf/tomcat-users.xml", run=run))
        tar_calls = [c for c in calls if c[0][:2] == ["docker", "exec"] and "tar" in c[0]]
        self.assertEqual(len(tar_calls), 1)
        argv, payload = tar_calls[0]
        self.assertEqual(
            argv,
            ["docker", "exec", "-i", "c", "tar", "-xf", "-", "-C", "/opt/Percussion/conf"],
        )
        self.assertIsNotNone(payload, msg="tar fallback must pipe archive bytes via input=")
        assert payload is not None
        with _tarfile.open(fileobj=_io.BytesIO(payload), mode="r") as tf:
            members = tf.getmembers()
            self.assertEqual(len(members), 1)
            self.assertEqual(members[0].name, "tomcat-users.xml")
            extracted = tf.extractfile(members[0])
            assert extracted is not None
            self.assertEqual(extracted.read(), STOCK_USERS_XML.encode("utf-8"))
            self.assertEqual(members[0].mtime, int(local.stat().st_mtime))

    def test_both_fail_mentions_recreate(self):
        local = self.staging / "tomcat-users.xml"
        local.write_bytes(STOCK_USERS_XML.encode("utf-8"))
        run, _calls = self._scripted_run(cp_rc=1, tar_rc=1)
        detail = qswf.docker_cp_in_with_tar_fallback(
            local, "c", "/x/tomcat-users.xml", run=run)
        self.assertIsNotNone(detail)
        assert detail is not None
        self.assertIn("qa-down", detail)
        self.assertIn("qa-up", detail)

    def test_unsafe_name_refuses_fallback(self):
        local = self.staging / ".."
        run, calls = self._scripted_run(cp_rc=1, tar_rc=0)
        detail = qswf.docker_cp_in_with_tar_fallback(
            local, "c", "/x/tomcat-users.xml", run=run)
        self.assertIsNotNone(detail)
        assert detail is not None
        self.assertIn("refusing tar fallback", detail)
        self.assertFalse(any("tar" in argv for argv, _ in calls))

    def test_flow_survives_cp_failure_via_tar_pipe(self):
        """End to end: every host->cell copy fails, the tar pipe ships
        the rewritten realm, and the seed still goes green."""
        tar_inputs: List[bytes] = []

        def run(argv, *, input=None, timeout=120):  # noqa: A002 — fake matches impl
            argv = list(argv)
            if argv[:2] == ["docker", "cp"]:
                src, dst = argv[2], argv[3]
                if src.startswith("c:"):
                    Path(dst).write_bytes(STOCK_USERS_XML.encode("utf-8"))
                    return (0, "", "")
                return (1, "", "daemon mkdirat ... file exists")
            if argv[:2] == ["docker", "exec"] and "tar" in argv:
                tar_inputs.append(input or b"")
                return (0, "", "")
            if "curl" in argv:
                return (0, "200", "")
            return (0, "", "")

        clock = FakeClock()
        result = qswf.run_seed_feed_flow(
            _plan(staging_dir=self.staging), run=run,
            http_get=lambda url: (200, FEED_XML),
            sleep=clock.sleep, now=clock.now)
        self.assertTrue(result.ok, result.detail)
        # The fallback actually shipped the rewritten users file: the
        # tarred tomcat-users.xml carries the provisioned digest.
        import io as _io
        import tarfile as _tarfile

        blobs = [b for b in tar_inputs if b]
        self.assertTrue(blobs, "expected the tar fallback to pipe archive bytes")
        shipped = b"".join(
            _tarfile.open(fileobj=_io.BytesIO(b), mode="r").extractfile(m).read()
            for b in blobs
            for m in _tarfile.open(fileobj=_io.BytesIO(b), mode="r").getmembers()
        )
        self.assertIn(qswf.sha1_hex(SECRET).encode("ascii"), shipped)


class FeedFixtureParityTest(unittest.TestCase):
    """Drift guard: Python fixture contract mirrors the JS FEED_FIXTURE."""

    def _js_block(self) -> str:
        helper = (SCRIPTS.parent.parent / "modules" / "perc-qa-automation"
                  / "frontend" / "tests" / "helpers" / "published-widget-surfaces.js")
        if not helper.is_file():
            self.skipTest(f"helper not checked out: {helper}")
        text = helper.read_text(encoding="utf-8")
        match = re.search(r"const FEED_FIXTURE = Object\.freeze\(\{(.*?)\}\);",
                          text, re.DOTALL)
        self.assertIsNotNone(match, "expected FEED_FIXTURE in the helper")
        assert match is not None
        return match.group(1)

    def test_contract_matches_helper(self):
        block = self._js_block()

        def lit(key: str) -> str:
            match = re.search(rf'{key}:\s*"([^"]+)"', block)
            self.assertIsNotNone(match, f"expected {key} in FEED_FIXTURE")
            assert match is not None
            return match.group(1)

        self.assertEqual(lit("site"), qswf.FEED_SITE)
        self.assertEqual(lit("name"), qswf.FEED_NAME)
        self.assertEqual(lit("title"), qswf.FEED_TITLE)
        self.assertEqual(lit("link"), qswf.FEED_LINK)
        self.assertEqual(lit("description"), qswf.FEED_DESCRIPTION)
        for row in qswf.FEED_ENTRIES:
            self.assertIn(row["title"], block)
            self.assertIn(row["path"], block)
            self.assertIn(row["abstract"], block)

    def test_spec_consumes_fixture(self):
        spec = (SCRIPTS.parent.parent / "modules" / "perc-qa-automation"
                / "frontend" / "tests" / "published-widget-feed.spec.js")
        if not spec.is_file():
            self.skipTest(f"spec not checked out: {spec}")
        text = spec.read_text(encoding="utf-8")
        self.assertIn("FEED_FIXTURE", text)
        self.assertNotIn("DTS_DELIVERY_PASSWORD", text,
                         "the spec must not carry seed credentials (#6341)")


if __name__ == "__main__":
    unittest.main()
