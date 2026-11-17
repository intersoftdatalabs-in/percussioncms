#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""QA widget-fixture seeder (spec 2402-qa-dts-publish, slice 8, #6341).

Issue #6341 (parent #6237): the published RSS-feed surface's entries
assertion needs a seeded feed descriptor (``PUT /rss/descriptors`` as
``deliverymanager``) plus indexed rows behind ``generateFeed``. The
stock ``--with-dts`` cell cannot seed either path from a spec: both
seed paths are CONFIDENTIAL + deliverymanager-gated, the shipped
``tomcat-users.xml`` carries a digest nobody can present, and
plain-HTTP seed calls 302 to the unpublished HTTPS port (residuals
#6343 / #6375).

This module closes the loop **cell-side**, so the spec asserts entries
with **no credentials**: it provisions a known deliverymanager password
into the cell's ``tomcat-users.xml`` (``docker cp`` out / rewrite /
``docker cp`` in — Tomcat's editable UserDatabase picks the edit up),
then seeds over **container-local HTTPS** (``docker exec`` + ``curl
-k``, no host HTTPS publish needed) and verifies the feed serves the
fixture entries over plain HTTP from the host.

Stdlib only. All host joins use :class:`pathlib.Path` (spec 994 FR-001
cross-platform rule). Container-side paths are POSIX by construction
(Linux matrix cells) and are built with :class:`PurePosixPath`, never
host separators.

Secret handling: the deliverymanager password travels to the cell via
``curl --config -`` on stdin (``docker exec -i``); it never appears in
an argv, a log line, or a RESULT/QA_DETAIL message. Only the user name
and redacted markers are printed.
"""

from __future__ import annotations

import hashlib
import json
import os
import re
import secrets
import subprocess
import time
import urllib.request
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from typing import Callable, Dict, List, Optional, Sequence, Tuple

#: RESULT contract token (agent-parseable, cf. perc-devctl ``_run_logged``).
STEP_QA_SEED_WIDGET_FIXTURES = "qa-seed-widget-fixtures"

#: ``tomcat-users.xml`` location relative to the DTS install root.
TOMCAT_USERS_REL = PurePosixPath("Deployment/Server/conf/tomcat-users.xml")

#: Container-side staging dir for the seed payloads (wiped best-effort).
# mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #6341) —
# container-side POSIX path by construction (Linux matrix cells); host joins use pathlib.
CONTAINER_STAGE_DIR = PurePosixPath("/tmp/qa-seed-widget-fixtures")

#: In-cell DTS HTTPS port (``https.port`` in ``perc-catalina.properties``).
DTS_HTTPS_PORT_DEFAULT = 8443

#: Shipped deliverymanager login (``dts-realm.properties`` / packaged
#: ``tomcat-users.xml``). The *password* is provisioned per run — see
#: :func:`resolve_delivery_password`.
DELIVERY_USER_DEFAULT = "ps_manager"

#: Name of the env var carrying an operator-chosen deliverymanager
#: password (``--delivery-password`` wins when both are given).
DELIVERY_PASSWORD_ENV = "DTS_DELIVERY_PASSWORD"

#: Feed fixture contract (mirrors ``FEED_FIXTURE`` in
#: ``modules/perc-qa-automation/frontend/tests/helpers/published-widget-surfaces.js``;
#: ``test_qa_seed_widget_fixtures.py::FeedFixtureParityTest`` fails on drift).
FEED_SITE = "qacell"
FEED_NAME = "qasurface-fixture"
FEED_TITLE = "QA surface fixture feed"
FEED_DESCRIPTION = (
    "Disposable QA-cell fixture feed for the published RSS surface (#6341)."
)
FEED_LINK = "http://localhost/qacell"
FEED_TYPE = "RSS"
FEED_QUERY_MAX_RESULTS = 25
FEED_QUERY_START_INDEX = 0
FEED_ENTRIES = (
    {
        "path": "/qacell/surface-fixture-1.html",
        "title": "QA surface fixture item 1",
        "abstract": "QA surface fixture probe item 1 (#6341).",
    },
    {
        "path": "/qacell/surface-fixture-2.html",
        "title": "QA surface fixture item 2",
        "abstract": "QA surface fixture probe item 2 (#6341).",
    },
)

#: How long (seconds) to poll the descriptor PUT while the cell's
#: UserDatabase reloads the rewritten password before restarting Tomcat.
AUTH_POLL_BEFORE_RESTART_SECONDS = 60.0
#: Total budget (seconds) for the descriptor PUT to succeed.
AUTH_POLL_TOTAL_SECONDS = 240.0
#: Pause (seconds) between seed attempts.
SEED_RETRY_PAUSE_SECONDS = 5.0


@dataclass
class SeedPlan:
    """Everything the live flow needs (pure — printable for ``--dry-run``)."""

    container: str = "perc-matrix-dts-h2"
    install_root: str = "/opt/Percussion"
    https_port: int = DTS_HTTPS_PORT_DEFAULT
    delivery_user: str = DELIVERY_USER_DEFAULT
    delivery_password: Optional[str] = None
    dts_http_base: str = ""
    staging_dir: Optional[Path] = None
    auth_poll_before_restart: float = AUTH_POLL_BEFORE_RESTART_SECONDS
    auth_poll_total: float = AUTH_POLL_TOTAL_SECONDS


@dataclass
class SeedResult:
    """Outcome of :func:`run_seed_feed_flow` (no secrets — safe to print)."""

    ok: bool
    detail: str
    feed_url: str = ""
    attempts: int = 0
    restarted_tomcat: bool = False


def resolve_delivery_password(explicit: Optional[str]) -> str:
    """Password to provision into the cell realm (never committed).

    Precedence: explicit flag, then the ``DTS_DELIVERY_PASSWORD`` env
    var, then a fresh random value per seed (the driver holds it in
    memory for the run; nothing needs to recall it afterwards).
    """
    if explicit:
        return explicit
    from_env = os.environ.get(DELIVERY_PASSWORD_ENV, "")
    if from_env:
        return from_env
    return f"qacell-{secrets.token_hex(8)}"


def sha1_hex(secret: str) -> str:
    """SHA-1 hex digest (Tomcat ``MessageDigestCredentialHandler``,
    ``algorithm="SHA"``)."""
    return hashlib.sha1(secret.encode("utf-8")).hexdigest()


_USER_TAG_RE = re.compile(
    r'<user\b(?P<attrs>[^>]*?)\bname="(?P<name>[^"]+)"(?P<rest>[^>]*?)/?>',
    re.DOTALL,
)
_PASSWORD_ATTR_RE = re.compile(r'\bpassword="[^"]*"')


def tomcat_users_with_password(xml_text: str, user: str, sha_hex: str) -> str:
    """Return ``tomcat-users.xml`` with ``user``'s password set to ``sha_hex``.

    Raises :class:`ValueError` when no ``<user name="<user>" ...>`` entry
    exists (fail fast instead of seeding a cell whose realm cannot auth).
    """
    found = False

    def _replace(match: re.Match) -> str:
        nonlocal found
        if match.group("name") != user:
            return match.group(0)
        found = True
        tag = match.group(0)
        if _PASSWORD_ATTR_RE.search(tag):
            return _PASSWORD_ATTR_RE.sub(f'password="{sha_hex}"', tag, count=1)
        return tag[:-1].rstrip() + f' password="{sha_hex}">'

    rewritten = _USER_TAG_RE.sub(_replace, xml_text)
    if not found:
        raise ValueError(f"tomcat-users.xml has no <user name={user!r}> entry")
    return rewritten


def feed_query_criteria(site: str) -> List[str]:
    """Metadata query criteria limiting rows to the fixture ``site``."""
    return [f"site = '{site}'"]


def feed_descriptor_payload(
    *,
    site: str = FEED_SITE,
    name: str = FEED_NAME,
    title: str = FEED_TITLE,
    description: str = FEED_DESCRIPTION,
    link: str = FEED_LINK,
    service_url: str,
) -> Dict:
    """``PUT /rss/descriptors`` body (shape: ``PSFeedDescriptors``)."""
    return {
        "descriptors": [
            {
                "name": name,
                "site": site,
                "description": description,
                "link": link,
                "title": title,
                "query": json.dumps(
                    {
                        "criteria": feed_query_criteria(site),
                        "maxResults": FEED_QUERY_MAX_RESULTS,
                        "startIndex": FEED_QUERY_START_INDEX,
                    }
                ),
                "type": FEED_TYPE,
            }
        ],
        "serviceUrl": service_url,
        "serviceUser": "qasurface",
        "servicePass": "qasurface",
        "servicePassEncrypted": False,
        "site": site,
    }


def index_entry_payload(*, path: str, title: str, abstract: str, site: str) -> Dict:
    """``POST /indexer/entry<path>`` body (shape: ``PSMetadataEntry``).

    Property values ride on ``value`` (not ``stringvalue``): the live
    services deserialize through ``JacksonXmlBindJsonProvider``, under
    which ``PSMetadataProperty.setStringvalue`` is ``@XmlTransient`` so
    a ``stringvalue`` field is silently dropped and the row indexes with
    null properties. ``value`` binds via the ``@XmlElement(name =
    "value")`` accessors on every mapper.
    """
    name = path.rsplit("/", 1)[-1]
    # mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #6341) —
    # pagepath is a URL path (always `/`), never a host filesystem path.
    folder = path.rsplit("/", 1)[0] + "/"
    return {
        "name": name,
        "folder": folder,
        "pagepath": path,
        "linktext": title,
        "type": "page",
        "site": site,
        "properties": [
            {"name": "dcterms:title", "value": title},
            {"name": "dcterms:abstract", "value": abstract},
        ],
    }


def container_users_path(install_root: str) -> str:
    """Container-side ``tomcat-users.xml`` path (POSIX by construction)."""
    return str(PurePosixPath(install_root) / TOMCAT_USERS_REL)


def cp_from_argv(container: str, remote: str, local: Path) -> List[str]:
    """``docker cp`` the cell file out to ``local`` (no secrets in argv)."""
    return ["docker", "cp", f"{container}:{remote}", str(local)]


def cp_to_argv(local: Path, container: str, remote: str) -> List[str]:
    """``docker cp`` the host file into the cell (no secrets in argv)."""
    return ["docker", "cp", str(local), f"{container}:{remote}"]


def exec_argv(container: str, *cmd: str, interactive: bool = False) -> List[str]:
    """``docker exec`` argv (callers keep secrets out — use stdin config)."""
    argv = ["docker", "exec"]
    if interactive:
        argv.append("-i")
    argv.append(container)
    argv.extend(cmd)
    return argv


def authed_curl_input(user: str, password: str) -> bytes:
    """``curl --config -`` stdin: the BASIC credential (never in argv)."""
    return f'user = "{user}:{password}"\n'.encode("utf-8")


def seed_curl_argv(
    container: str, method: str, url: str, payload_remote: Optional[str] = None,
    body_remote: Optional[str] = None,
) -> List[str]:
    """Container-local ``curl -k --config -`` argv (secret-free).

    The credential arrives on stdin via :func:`authed_curl_input`;
    ``-w`` prints the HTTP code on stdout for the poll loop and (when
    ``body_remote`` is set) the response body lands in a cell file the
    flow cats back only on failure paths.
    """
    argv = exec_argv(container, "curl", "-sk", "--config", "-", "-o",
                     body_remote or "/dev/null",
                     "-w", "%{http_code}", "-X", method, url, interactive=True)
    if payload_remote is not None:
        argv.extend(["-H", "Content-Type: application/json", "-d", f"@{payload_remote}"])
    return argv


def https_origin(port: int) -> str:
    """Container-local HTTPS origin (``curl`` runs inside the cell)."""
    return f"https://localhost:{port}"


def feeds_service_origin(https_port: int) -> str:
    """Container-local feeds service root."""
    return f"{https_origin(https_port)}/feeds/rss"


def indexer_service_origin(https_port: int) -> str:
    """Container-local metadata extractor root (context-level ``/indexer``)."""
    return f"{https_origin(https_port)}/perc-metadata-services/indexer"


def entry_url(indexer_origin: str, page_path: str) -> str:
    """Extractor entry URL for ``page_path`` (which carries its leading ``/``).

    The leading slash is significant: the DAO matches ``pagepath``
    verbatim, and the ``{pagePath:.*}`` JAX-RS parameter only preserves
    it when the URL carries a double slash (``entry//qacell/...``). A
    single-slash ``entry/qacell/...`` deletes nothing while answering
    204 — the silent-litter trap the fixture cleanup avoids.
    """
    return f"{indexer_origin}/entry/{page_path}"


def feed_url_for(dts_http_base: str, site: str = FEED_SITE,
                 name: str = FEED_NAME) -> str:
    """Host-visible feed URL the spec asserts (plain HTTP is fine — only
    the seed paths are CONFIDENTIAL)."""
    return f"{dts_http_base.rstrip('/')}/feeds/rss/{site}/{name}/localhost"


RunFn = Callable[..., Tuple[int, str, str]]
HttpGetFn = Callable[[str], Tuple[int, str]]
SleepFn = Callable[[float], None]


def _default_run(argv: Sequence[str], *, input: Optional[bytes] = None,
                 timeout: int = 120) -> Tuple[int, str, str]:
    proc = subprocess.run(
        list(argv), input=input, capture_output=True, timeout=timeout, check=False
    )
    return (
        proc.returncode,
        proc.stdout.decode("utf-8", "replace"),
        proc.stderr.decode("utf-8", "replace"),
    )


def _default_http_get(url: str, timeout: int = 30) -> Tuple[int, str]:
    req = urllib.request.Request(url, headers={"Accept": "text/xml"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as err:
        try:
            body = err.read().decode("utf-8", "replace")
        except Exception:  # noqa: BLE001 — best-effort body on error pages
            body = ""
        return err.code, body


def _parse_http_code(output: str) -> int:
    try:
        return int(output.strip()[-3:])
    except (ValueError, IndexError):
        return 0


class _SeedAbort(Exception):
    """Internal control flow: abort the flow with a printable detail."""


class _SeedFlow:
    """One live seed run (injectable ``run``/``http_get``/``sleep``/``now``).

    Phases raise :class:`_SeedAbort`; :meth:`execute` converts to a
    :class:`SeedResult`. Keeps per-phase complexity reviewable and never
    logs the password.
    """

    def __init__(
        self,
        plan: SeedPlan,
        *,
        run: RunFn,
        http_get: HttpGetFn,
        sleep: SleepFn,
        now: Callable[[], float],
    ) -> None:
        self.plan = plan
        self.run = run
        self.http_get = http_get
        self.sleep = sleep
        self.now = now
        self.password = resolve_delivery_password(plan.delivery_password)
        self.curl_input = authed_curl_input(plan.delivery_user, self.password)
        self.attempts = 0
        self.restarted = False
        self.staging = plan.staging_dir or (Path.cwd() / "tmp" / "qa-seed-widget-fixtures")
        self.users_remote = container_users_path(plan.install_root)
        self.svc = feeds_service_origin(plan.https_port)
        self.indexer = indexer_service_origin(plan.https_port)
        self.put_body_remote = str(CONTAINER_STAGE_DIR / "put-body.txt")
        self.remote_payloads: Dict[str, str] = {}

    def execute(self) -> SeedResult:
        """Run every phase; always leave the cell tidy."""
        try:
            self._provision_password()
            self._stage_payloads()
            self._put_descriptor()
            self._seed_rows()
        except _SeedAbort as abort:
            self._cleanup()
            return SeedResult(
                ok=False, detail=str(abort),
                attempts=self.attempts, restarted_tomcat=self.restarted)
        self._cleanup()
        return self._verify()

    def _cleanup(self) -> None:
        # Best-effort removal of the staged payloads (keeps the cell tidy).
        self.run(
            exec_argv(self.plan.container, "rm", "-rf", str(CONTAINER_STAGE_DIR)),
            timeout=60,
        )

    def _authed(self, method: str, url: str,
                payload: Optional[str] = None,
                body_remote: Optional[str] = None) -> int:
        rc, out, _err = self.run(
            seed_curl_argv(self.plan.container, method, url, payload, body_remote),
            input=self.curl_input, timeout=120)
        if rc != 0:
            return 0
        return _parse_http_code(out)

    def _cell_file_text(self, remote: str, limit: int = 500) -> str:
        rc, out, _err = self.run(exec_argv(self.plan.container, "cat", remote), timeout=60)
        if rc != 0:
            return ""
        return out.strip().replace("\n", " ")[:limit]

    def _provision_password(self) -> None:
        # 1. Copy the realm file out, set the SHA digest, copy back.
        plan = self.plan
        staging = self.staging
        staging.mkdir(parents=True, exist_ok=True)
        local_users = staging / "tomcat-users.xml"
        rc, _out, err = self.run(
            cp_from_argv(plan.container, self.users_remote, local_users), timeout=60)
        if rc != 0:
            raise _SeedAbort(
                f"could not read cell {self.users_remote}: {err.strip()[-300:]}")
        try:
            xml_text = local_users.read_text(encoding="utf-8")
        except OSError as exc:
            raise _SeedAbort(f"could not read staged users file: {exc}") from exc
        try:
            rewritten = tomcat_users_with_password(
                xml_text, plan.delivery_user, sha1_hex(self.password))
        except ValueError as exc:
            raise _SeedAbort(str(exc)) from exc
        try:
            local_users.write_text(rewritten, encoding="utf-8")
        except OSError as exc:
            raise _SeedAbort(f"could not stage users file: {exc}") from exc
        rc, _out, err = self.run(
            cp_to_argv(local_users, plan.container, self.users_remote), timeout=60)
        if rc != 0:
            raise _SeedAbort(
                f"could not write cell {self.users_remote}: {err.strip()[-300:]}")

    def _stage_payloads(self) -> None:
        # 2. Write the seed JSONs locally and copy them into the cell.
        plan = self.plan
        payloads: Dict[str, Dict] = {
            "descriptor.json": feed_descriptor_payload(
                service_url=f"{https_origin(plan.https_port)}"
                            f"/perc-metadata-services/metadata/get"),
        }
        for i, row in enumerate(FEED_ENTRIES):
            payloads[f"entry-{i}.json"] = index_entry_payload(
                path=row["path"], title=row["title"],
                abstract=row["abstract"], site=FEED_SITE)
        self.run(
            exec_argv(plan.container, "mkdir", "-p", str(CONTAINER_STAGE_DIR)),
            timeout=60,
        )
        for fname, payload in payloads.items():
            local = self.staging / fname
            try:
                local.write_text(json.dumps(payload), encoding="utf-8")
            except OSError as exc:
                raise _SeedAbort(f"could not stage {fname}: {exc}") from exc
            remote = str(CONTAINER_STAGE_DIR / fname)
            rc, _out, err = self.run(cp_to_argv(local, plan.container, remote), timeout=60)
            if rc != 0:
                raise _SeedAbort(
                    f"could not stage {fname} in cell: {err.strip()[-300:]}")
            self.remote_payloads[fname] = remote

    def _restart_tomcat(self) -> None:
        plan = self.plan
        self.run(exec_argv(plan.container, "bash",
                           f"{plan.install_root.rstrip('/')}/TomcatShutdown.sh"),
                 timeout=180)
        self.sleep(10)
        self.run(exec_argv(plan.container, "bash",
                           f"{plan.install_root.rstrip('/')}/TomcatStartup.sh"),
                 timeout=180)
        self.restarted = True

    def _put_descriptor(self) -> None:
        # 3. PUT the descriptor (retry: the editable UserDatabase reloads
        # the rewritten file asynchronously; a Tomcat restart is the fallback).
        plan = self.plan
        deadline = self.now() + plan.auth_poll_total
        restart_at = self.now() + plan.auth_poll_before_restart
        last_code = 0
        while True:
            self.attempts += 1
            last_code = self._authed(
                "PUT", f"{self.svc}/descriptors",
                self.remote_payloads["descriptor.json"], self.put_body_remote)
            if 200 <= last_code < 300:
                return
            if last_code == 302:
                raise _SeedAbort(
                    "descriptor PUT answered 302 over container-local HTTPS — "
                    "the cell HTTPS connector is misconfigured (expected 200/401)")
            if last_code >= 500:
                # Deterministic server errors (e.g. a DTO mapping
                # regression) never heal by retrying: surface the
                # server's snippet and stop.
                snippet = self._cell_file_text(self.put_body_remote)
                if "Cannot construct instance" in snippet:
                    raise _SeedAbort(
                        f"descriptor PUT rejected by the server: {snippet} "
                        f"(DTO mapping — fix the server type, then re-seed)")
            if self.now() >= deadline:
                break
            if last_code == 401 and not self.restarted and self.now() >= restart_at:
                self._restart_tomcat()
            self.sleep(SEED_RETRY_PAUSE_SECONDS)
        hint = ("deliverymanager credential did not take on the cell"
                if last_code == 401
                else "see the cell feeds.log for the server error")
        raise _SeedAbort(
            f"descriptor PUT never succeeded (last HTTP {last_code} "
            f"after {self.attempts} attempts) — {hint}")

    def _seed_rows(self) -> None:
        # 4. Index rows: delete-then-post per fixture path. The staged
        # payloads must still be in the cell here — cleanup runs only
        # after the POSTs (an early rm makes curl post empty bodies,
        # which the services accept with 204 while persisting nothing).
        # Entry URLs keep the page path's leading slash (double slash):
        # single-slash deletes match nothing while answering 204.
        for i, row in enumerate(FEED_ENTRIES):
            self._authed("DELETE", entry_url(self.indexer, row["path"]))
            code = self._authed("POST", entry_url(self.indexer, row["path"]),
                                self.remote_payloads[f"entry-{i}.json"])
            if not 200 <= code < 300:
                raise _SeedAbort(
                    f"index seeding '{row['path']}' answered HTTP {code}")

    def _feed_titles(self) -> List[str]:
        return [FEED_TITLE, *[r["title"] for r in FEED_ENTRIES],
                *[r["abstract"] for r in FEED_ENTRIES]]

    def _verify(self) -> SeedResult:
        # 5. Host-side verify: the feed must serve the entries over HTTP.
        plan = self.plan
        if not plan.dts_http_base:
            return SeedResult(ok=False, attempts=self.attempts,
                              restarted_tomcat=self.restarted,
                              detail="seeded but no dts_http_base to verify against")
        feed_url = feed_url_for(plan.dts_http_base)
        try:
            status, body = self.http_get(feed_url)
        except Exception as exc:  # noqa: BLE001 — transport failure is a seed failure
            return SeedResult(ok=False, attempts=self.attempts,
                              restarted_tomcat=self.restarted,
                              detail=f"feed verify GET threw: {exc}")
        missing = [t for t in self._feed_titles() if t not in body]
        if status != 200 or missing:
            return SeedResult(
                ok=False, attempts=self.attempts, restarted_tomcat=self.restarted,
                feed_url=feed_url,
                detail=f"feed verify GET {feed_url} answered HTTP {status} "
                       f"missing={missing}")
        return SeedResult(ok=True, attempts=self.attempts,
                          restarted_tomcat=self.restarted,
                          feed_url=feed_url,
                          detail=f"feed serves {len(FEED_ENTRIES)} fixture entries")


def run_seed_feed_flow(
    plan: SeedPlan,
    *,
    run: RunFn = _default_run,
    http_get: HttpGetFn = _default_http_get,
    sleep: SleepFn = time.sleep,
    now: Callable[[], float] = time.monotonic,
) -> SeedResult:
    """Provision the credential, seed the fixture, verify entries (live).

    ``run``/``http_get``/``sleep``/``now`` are injectable so unit tests
    cover the whole flow without docker. Never logs the password.
    """
    return _SeedFlow(plan, run=run, http_get=http_get, sleep=sleep, now=now).execute()


def describe_plan(plan: SeedPlan) -> str:
    """One-line redacted plan summary (safe for ``--dry-run`` output)."""
    https = https_origin(plan.https_port)
    return (
        f"container={plan.container} users={container_users_path(plan.install_root)} "
        f"user={plan.delivery_user} https={https} "
        f"feed={FEED_SITE}/{FEED_NAME} entries={len(FEED_ENTRIES)}"
    )
