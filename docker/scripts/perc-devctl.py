#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform consolidated docker dev/test stack control.

Replaces ``docker/scripts/perc-devctl.sh`` (390 lines of bash). The
script is the operator-facing entry point for the cms-dts dev/test
docker compose stack and exposes every subcommand the original
``.sh`` provided: ``install``, ``up``, ``down``, ``status``, ``verify``,
``it-verify``, ``deploy-jar``, ``verify-fix``, ``logs-path``,
``inspect-install``, ``show-generated-passwords``.

QA mode (H2-in-Docker, no host install — issue #1827 / #1927) adds:

* ``qa-up`` — one-shot CMS on H2 via matrix cell (``--keep``), health wait
* ``qa-health`` — poll published CMS URL until ready (clear timeout);
  fail-fast when Jetty logs show Rhythmyx ApplicationContext failure
  (``Failed startup of context`` / ``BeanCurrentlyInCreationException``)
  even if HTTP still answers (#2462 / #2423). RESULT lines include
  ``HEALTH:healthy|unhealthy|starting|none`` from ``docker inspect``
  (matrix cell HEALTHCHECK — #2481 residual #2537)
* ``qa-down`` — destroy the QA cell (frees ports; no multi-GB orphans)
* ``qa-preflight`` — rebuild-chain preflight; detect a stale WebUI WAR vs
  a freshly built sitemanage SNAPSHOT (#2486)
* ``qa-up --skip-image-build`` — fail fast when the cached
  ``percussion-matrix-cell:local`` image lacks the in-image HEALTHCHECK
  (#2481); the precheck runs ``docker inspect`` and prints a clear
  rebuild hint instead of waiting the full ``--probe-timeout`` for
  ``docker_health_timeout health=none`` (#2484)
* ``qa-rebuild-chain`` — drive the documented Maven order
  (sitemanage install → WebUI package → perc-distribution-tree package)
  via repo-root ``mvnw``/``mvnw.cmd``, portable paths, ``shell=False``
  (#2533 residual of #2423 / #2486). ``--then-qa-up`` also copies
  ``WebUI/target/generated-webui/cm/modern`` into the H2 QA cell after
  the cell is up (post-jar SPA deploy; #3948) unless ``--skip-webui-deploy``.
* ``qa-deploy-webui`` — copy the full generated ``cm/modern`` tree
  (stable ``perc-modern-ui.js`` entry + CSS + hashed chunks + any
  ``index.html``) into the H2 QA WAR. Copying only hashed ``assets/``
  files, or jar-only Cycle Verify hot-deploys of rest/sitemanage,
  leaves a stale developer chunk without
  ``option[value=object-storage]`` / ``rss-atom`` / ``icalendar`` /
  ``sitemap-xml`` (#3893 / #3948 / #4141) or
  ``[data-testid=developer-am-new]`` (#4123)
* ``qa-deploy-war-jars`` — copy perc-system / rest / sitemanage SNAPSHOTs
  into the H2 QA WAR ``WEB-INF/lib`` so skip-image-build cells pick up
  the sitemap-xml allow-list (#4174). Does not ``docker restart``.
  Optional in-cell StopJetty/StartJetty via ``--restart-jetty``.

Compose ``verify`` / ``verify-fix`` / ``deploy-jar --verify`` apply the same
Rhythmyx context log scan against the cms-dts container (#2480 companion to
#2462): HTTP + docker health alone is not enough when Spring context is dead.
Both ``qa-health`` (matrix cell) and ``verify`` (cms-dts) print ``HEALTH:``
from ``_docker_health`` so host log-scan and inspect status share one RESULT.

Each subcommand logs its full output to a timestamped file under
``docker/logs/`` and emits a single ``RESULT:OK STEP:<step> LOG:<path>``
or ``RESULT:FAIL STEP:<step> LOG:<path>`` line on stdout so agent
workflows can parse the result without parsing free-form output.

## Behavioral Notes (FR-009b)

- The original ``.sh`` used bash ``set -euo pipefail`` for early-fail.
  The Python port raises explicit exceptions and surfaces non-zero
  exit codes via ``subprocess.run([...], check=False)`` so failures
  are caught at the right granularity.
- The original ``.sh`` shell-built-in ``bash -lc "$cmd"`` for
  ``run_logged`` (which forces login-shell sourcing of ``.bashrc`` /
  ``/etc/profile``). The Python port replaces this with
  ``subprocess.run([...], shell=False)`` — the OS resolves the
  executable via PATH lookup, so the same effect is achieved without
  a shell. Operators who relied on ``.bashrc`` exports in their
  subshells need to set them in ``docker-compose.yml`` instead.
- All external invocations use ``subprocess.run([...], shell=False,
  check=False)`` (FR-008).
- Path discovery uses ``pathlib.Path``; the cross-platform
  ``docker compose`` invocation is built as an argv list (no shell
  quoting hazards).
- The dry-run gating for pytest uses ``--dry-run`` on every
  subcommand; the script logs the planned compose / curl / docker
  command and returns ``RESULT:OK STEP:<step> LOG:`` without invoking
  the external program.

Exit codes (per subcommand):

  0  success (or dry-run)
  1  invocation error / subcommand not found
  2  docker / curl / mvn invocation failed (mirrors the original
     ``.sh`` exit codes where ``RESULT:FAIL`` lines were followed by
     a non-zero exit code)
"""

from __future__ import annotations

import argparse
import logging
import os
import shlex
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, List, Optional, Sequence

# Sibling freeport helpers (stdlib only). Ensure scripts dir is importable
# when this file is loaded via importlib (unit tests) or as a script.
_SCRIPTS_DIR = Path(__file__).resolve().parent
if str(_SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS_DIR))
from perc_host_ports import find_free_port, is_port_free, resolve_host_port  # noqa: E402
from rhythmyx_ready import (  # noqa: E402
    DETAIL_CONTEXT_FAILED,
    DETAIL_SERVER_LOG_ERRORS,
    assess_rhythmyx_ready,
    container_cms_log_paths,
    container_server_log_path,
    find_rhythmyx_context_failure,
    find_server_log_startup_error,
)

LOG = logging.getLogger("perc-devctl")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_SUBPROCESS_FAILED = 2

DEFAULT_CONTAINER = "percussion-cms-dts"
DEFAULT_ENV_FILE = ".env.compose"
ENV_FILE_FALLBACK = ".env.compose.example"
DEFAULT_COMPOSE_FILE = "docker-compose.yml"

VERIFY_TIMEOUT_SECONDS_DEFAULT = 300
VERIFY_INTERVAL_SECONDS_DEFAULT = 5
VERIFY_FIX_TIMEOUT_SECONDS_DEFAULT = 240

# Preferred host ports when free and no env override (single-worktree baseline).
# Multi-worktree: set CMS_PORT / DTS_PORT / QA_CMS_HOST_PORT (or leave unset
# so freeport allocates) — see resolve_host_port / docker/README.md.
PREFERRED_VERIFY_CMS_HOST_PORT = 9992
PREFERRED_VERIFY_DTS_HOST_PORT = 9980
PREFERRED_QA_CMS_HOST_PORT = 9993
# Compose DB host publishes (docker-compose.yml ${*_PORT:-…}); #2004.
PREFERRED_MYSQL_HOST_PORT = 3306
PREFERRED_POSTGRES_HOST_PORT = 5433
PREFERRED_MSSQL_HOST_PORT = 1433

VERIFY_CMS_PATH = "/Rhythmyx/rest/folders/by-path/Assets"
VERIFY_DTS_PATH = "/"

# ---------------------------------------------------------------------------
# QA mode — H2 matrix cell for Playwright (no host install). Host port is
# resolved at runtime (env override or freeport); preferred baseline 9993
# matches matrix-install-smoke.py when that port is free (#2001).
# ---------------------------------------------------------------------------
QA_CMS_PRODUCT = "cms"
QA_CMS_DB = "h2"
QA_CMS_CELL_ID = f"{QA_CMS_PRODUCT}-{QA_CMS_DB}"
QA_CMS_CONTAINER = f"perc-matrix-{QA_CMS_CELL_ID}"
# Same default as docker/scripts/hot-deploy-rhythmyx-war-jars.py --dest.
QA_WAR_JARS_DEST = "/opt/Percussion/jetty/base/webapps/Rhythmyx/WEB-INF/lib"
# Probe URL path for ``qa-health`` (#2482). The matrix-recommended primary
# is ``/Rhythmyx/rest/mimetypes`` (Spring-managed ``MimeTypeResource.ping()``
# — returns 404 when the Rhythmyx Spring ApplicationContext is dead, instead
# of the legacy ``/Rhythmyx/login`` 200 from the JSP renderer).
QA_CMS_PROBE_PATH = "/Rhythmyx/rest/mimetypes"
QA_ADMIN_USERNAME = "Admin"
QA_INSTALL_ROOT = "/opt/Percussion"
QA_PASSWORDS_REL = "var/config/generated/passwords"
# Matrix silent install can take several minutes on first run.
QA_PROBE_TIMEOUT_SECONDS_DEFAULT = 900
QA_PROBE_INTERVAL_SECONDS_DEFAULT = 5
# Tail size for docker logs when scanning for Rhythmyx context failure (#2462).
QA_LOG_SCAN_TAIL_LINES = 800
# Env var that overrides the QA probe path. Same name as the in-image
# ``rhythmyx_healthcheck.py`` (``RHYTHMYX_HEALTH_PATH``) so host, Docker,
# and matrix cells can be configured from one place. ``#2482`` matrix.
QA_CMS_PROBE_PATH_ENV = "RHYTHMYX_HEALTH_PATH"
# Local matrix image tag baked by ``matrix-install-smoke.py`` (also matches
# docker/README.md → "Rebuild note"). Kept here so agent preflight can
# detect a stale image that lacks HEALTHCHECK (#2481) before we wait for a
# 20-minute ``docker_health_timeout`` to surface the same condition.
QA_MATRIX_IMAGE_TAG = "percussion-matrix-cell:local"

# Status returned by :func:`_qa_matrix_image_healthcheck_status` (#2484).
QA_IMAGE_HEALTHCHECK_OK = "ok"
QA_IMAGE_HEALTHCHECK_MISSING = "missing"
QA_IMAGE_HEALTHCHECK_ABSENT = "absent"

# Tail size when reading jetty/base/logs + install logs inside the container (#2556).
QA_SERVER_LOG_TAIL_LINES = 4000


# Freeport primitives live in perc_host_ports.py (shared with matrix) — #2001/#2005.


def resolve_verify_cms_url() -> str:
    """CMS probe URL for ``verify`` / ``verify-fix``.

    Override with ``VERIFY_CMS_URL``, else build from ``CMS_PORT`` (compose)
    / freeport with preferred host port :data:`PREFERRED_VERIFY_CMS_HOST_PORT`.
    """
    explicit = os.environ.get("VERIFY_CMS_URL", "").strip()
    if explicit:
        return explicit
    port = resolve_host_port(
        "CMS_PORT", preferred=PREFERRED_VERIFY_CMS_HOST_PORT
    )
    return f"http://localhost:{port}{VERIFY_CMS_PATH}"


def resolve_verify_dts_url() -> str:
    """DTS probe URL for ``verify`` / ``verify-fix``.

    Override with ``VERIFY_DTS_URL``, else build from ``DTS_PORT`` / freeport
    with preferred host port :data:`PREFERRED_VERIFY_DTS_HOST_PORT`.
    """
    explicit = os.environ.get("VERIFY_DTS_URL", "").strip()
    if explicit:
        return explicit
    port = resolve_host_port(
        "DTS_PORT", preferred=PREFERRED_VERIFY_DTS_HOST_PORT
    )
    return f"http://localhost:{port}{VERIFY_DTS_PATH}"


def resolve_qa_cms_host_port() -> int:
    """Host port for the QA CMS matrix cell publish mapping.

    Override with ``QA_CMS_HOST_PORT`` or ``CMS_HOST_PORT``; else preferred
    9993 when free, else freeport. Exports nothing by itself — callers that
    start the cell should write the chosen port into the environment for
    child processes and operator discovery.
    """
    return resolve_host_port(
        "QA_CMS_HOST_PORT",
        "CMS_HOST_PORT",
        preferred=PREFERRED_QA_CMS_HOST_PORT,
    )


def qa_cms_base_url(port: int) -> str:
    """Base URL for the QA CMS cell on the given host port."""
    return f"http://127.0.0.1:{port}"


def qa_cms_probe_url(port: int) -> str:
    """Health probe URL for the QA CMS cell.

    Honors :data:`QA_CMS_PROBE_PATH_ENV` (``RHYTHMYX_HEALTH_PATH``) so the
    QA cell, in-image ``rhythmyx_healthcheck.py`` (#2481), and any
    external orchestrator can be configured from the same env var. The
    default path is the matrix-recommended primary (#2482); see
    :data:`PROBE_URL_MATRIX` in :mod:`rhythmyx_ready`.
    """
    env_override = os.environ.get(QA_CMS_PROBE_PATH_ENV, "").strip()
    path = env_override or QA_CMS_PROBE_PATH
    return f"{qa_cms_base_url(port)}{path}"


def ensure_compose_db_host_ports() -> dict[str, int]:
    """Resolve compose DB host ports and pin them in ``os.environ`` (#2004).

    Compose maps ``${MYSQL_PORT:-3306}:3306``, ``${POSTGRES_PORT:-5433}:5432``,
    and ``${MSSQL_PORT:-1433}:1433``. Process-env pins override ``.env.compose``
    defaults so concurrent worktrees do not fail with address-already-in-use.
    Returns mapping of env key → resolved host port.
    """
    specs = (
        ("MYSQL_PORT", PREFERRED_MYSQL_HOST_PORT),
        ("POSTGRES_PORT", PREFERRED_POSTGRES_HOST_PORT),
        ("MSSQL_PORT", PREFERRED_MSSQL_HOST_PORT),
    )
    resolved: dict[str, int] = {}
    for env_key, preferred in specs:
        port = resolve_host_port(env_key, preferred=preferred)
        os.environ[env_key] = str(port)
        resolved[env_key] = port
    return resolved


def ensure_compose_host_ports() -> tuple[int, int]:
    """Resolve CMS/DTS (and DB) host ports for compose and pin ``os.environ``.

    Compose already maps ``${CMS_PORT:-9992}:9992`` and
    ``${DTS_PORT:-9980}:9980``. Pinning the resolved values into the process
    environment makes ``docker compose`` and later ``verify`` in the same
    session use the same published ports (critical for multi-worktree freeport).

    Also pins ``MYSQL_PORT`` / ``POSTGRES_PORT`` / ``MSSQL_PORT`` via
    :func:`ensure_compose_db_host_ports` so full compose stacks share the
    freeport contract (#2004).
    """
    cms = resolve_host_port("CMS_PORT", preferred=PREFERRED_VERIFY_CMS_HOST_PORT)
    dts = resolve_host_port("DTS_PORT", preferred=PREFERRED_VERIFY_DTS_HOST_PORT)
    os.environ["CMS_PORT"] = str(cms)
    os.environ["DTS_PORT"] = str(dts)
    ensure_compose_db_host_ports()
    return cms, dts


def ensure_qa_cms_host_port() -> int:
    """Resolve QA CMS host port and pin it for child processes / discovery."""
    port = resolve_qa_cms_host_port()
    os.environ["QA_CMS_HOST_PORT"] = str(port)
    # matrix-install-smoke reads CMS_HOST_PORT / QA_CMS_HOST_PORT for docker -p (#2005).
    os.environ.setdefault("CMS_HOST_PORT", str(port))
    return port


def _build_arg_parser() -> argparse.ArgumentParser:
    """Build the top-level arg parser with the canonical subcommand
    set. Subcommand-specific flags live in ``_build_<sub>_parser``.
    """
    p = argparse.ArgumentParser(
        prog="perc-devctl.py",
        description=(
            "Cross-platform docker dev/test stack control. Each "
            "subcommand writes full output to a timestamped file under "
            "docker/logs/ and emits a single RESULT:OK/FAIL line on "
            "stdout for agent workflows."
        ),
    )
    p.add_argument(
        "--repo-root",
        type=Path,
        default=None,
        help="Repo root (default: parent of this script's grandparent).",
    )
    p.add_argument(
        "--env-file",
        default=None,
        help=(
            f"docker compose env file (default: {DEFAULT_ENV_FILE} if it "
            f"exists, else {ENV_FILE_FALLBACK})."
        ),
    )
    p.add_argument(
        "--compose-file",
        default=DEFAULT_COMPOSE_FILE,
        help=(
            f"docker compose file "
            f"(default: {DEFAULT_COMPOSE_FILE})."
        ),
    )
    sub = p.add_subparsers(dest="command", required=True)

    # install
    pi = sub.add_parser(
        "install",
        help="Run the host-side installer (scripts/install-cms-dev.py).",
    )
    pi.add_argument("--reset", action="store_true")
    pi.add_argument("--no-bootstrap", action="store_true")
    pi.add_argument("--install-root")
    pi.add_argument("--dry-run", action="store_true")

    # up
    pu = sub.add_parser("up", help="Start mysql + cms-dts compose stack.")
    pu.add_argument("--build", action="store_true")
    pu.add_argument("--dry-run", action="store_true")

    # down
    pd = sub.add_parser("down", help="Stop compose stack.")
    pd.add_argument("--volumes", action="store_true")
    pd.add_argument("--dry-run", action="store_true")

    # status
    ps = sub.add_parser("status", help="Print concise stack status.")
    ps.add_argument("--dry-run", action="store_true")

    # verify
    pv = sub.add_parser("verify", help="Verify running stack health.")
    pv.add_argument("--timeout-seconds", type=int, default=VERIFY_TIMEOUT_SECONDS_DEFAULT)
    pv.add_argument("--interval-seconds", type=int, default=VERIFY_INTERVAL_SECONDS_DEFAULT)
    pv.add_argument("--dry-run", action="store_true")

    # it-verify
    pit = sub.add_parser(
        "it-verify",
        help="Run Maven integration verification with compose profile.",
    )
    pit.add_argument("--dry-run", action="store_true")

    # deploy-jar
    pdj = sub.add_parser("deploy-jar", help="Hot deploy a built jar.")
    pdj.add_argument("--jar", required=True)
    pdj.add_argument("--target", default="both")
    pdj.add_argument("--restart", action="store_true")
    pdj.add_argument("--verify", action="store_true")
    pdj.add_argument("--dry-run", action="store_true")

    # verify-fix
    pvf = sub.add_parser(
        "verify-fix",
        help="Deploy jar and run verification as one operation.",
    )
    pvf.add_argument("--jar", required=True)
    pvf.add_argument("--target", default="both")
    pvf.add_argument("--restart", action="store_true")
    pvf.add_argument("--no-restart", action="store_true")
    pvf.add_argument(
        "--timeout-seconds",
        type=int,
        default=VERIFY_FIX_TIMEOUT_SECONDS_DEFAULT,
    )
    pvf.add_argument("--dry-run", action="store_true")

    # logs-path
    sub.add_parser("logs-path", help="Print the logs directory path.")

    # inspect-install
    sub.add_parser(
        "inspect-install",
        help="Capture effective CMS and DTS database configuration.",
    )

    # show-generated-passwords
    sub.add_parser(
        "show-generated-passwords",
        help="Capture generated passwords file from running container.",
    )

    # --- QA mode (H2 Docker, no host install) — #1827 slice 1 / #1927 ---
    pqu = sub.add_parser(
        "qa-up",
        help=(
            "Start CMS on H2 in Docker for QA/Playwright (no host install). "
            "Waits for health; prints TEST_CMS_URL and admin creds hint."
        ),
    )
    pqu.add_argument(
        "--timeout-seconds",
        type=int,
        default=QA_PROBE_TIMEOUT_SECONDS_DEFAULT,
        help=(
            f"Seconds to wait for CMS ready during install/start "
            f"(default: {QA_PROBE_TIMEOUT_SECONDS_DEFAULT})."
        ),
    )
    pqu.add_argument(
        "--skip-image-build",
        action="store_true",
        help="Pass --skip-image-build to matrix-install-smoke (reuse local image).",
    )
    pqu.add_argument(
        "--then-qa-deploy-webui",
        action="store_true",
        help=(
            "After a successful qa-up, copy WebUI/target/generated-webui/"
            "cm/modern into the H2 QA WAR (#3948 / #4141). Required when "
            "--skip-image-build leaves a stale SPA without "
            "object-storage / rss-atom / icalendar / sitemap-xml kind "
            "options and Action Menus developer-am-new catalog chrome. "
            "With --skip-image-build, also copies perc-system/rest/"
            "sitemanage SNAPSHOTs into WEB-INF/lib (#4174)."
        ),
    )
    pqu.add_argument(
        "--then-qa-deploy-war-jars",
        action="store_true",
        help=(
            "After a successful qa-up, copy perc-system, rest, and "
            "sitemanage SNAPSHOTs into the H2 QA WAR WEB-INF/lib "
            "(#4174). Implied by --then-qa-deploy-webui when "
            "--skip-image-build is set."
        ),
    )
    pqu.add_argument(
        "--no-restart-jetty",
        action="store_true",
        help=(
            "With --then-qa-deploy-war-jars, copy SNAPSHOTs without "
            "in-cell StopJetty/StartJetty (default restarts Jetty)."
        ),
    )
    pqu.add_argument("--dry-run", action="store_true")

    # --- Rebuild-chain preflight — #2486 / #2532 ---
    pqp = sub.add_parser(
        "qa-preflight",
        help=(
            "Detect a stale WebUI WAR vs a freshly built sitemanage "
            "SNAPSHOT before qa-up (#2486 / #2532). Default content-hash "
            "(SHA-256 m2 jar vs WAR zip entry); --no-content-hash for "
            "mtime-only. Exits non-zero in --strict mode when stale."
        ),
    )
    pqp.add_argument("--strict", action="store_true")
    pqp.add_argument(
        "--no-content-hash",
        action="store_true",
        help="Mtime-only comparison (disable default SHA-256 content hash).",
    )
    pqp.add_argument("--dry-run", action="store_true")

    # --- Rebuild-chain driver — #2533 ---
    pqrc = sub.add_parser(
        "qa-rebuild-chain",
        help=(
            "Run sitemanage install → WebUI package → "
            "perc-distribution-tree package via repo-root mvnw "
            "(#2533). Use after STALE preflight or SNAPSHOT changes."
        ),
    )
    pqrc.add_argument(
        "--skip-tests",
        action="store_true",
        help="Pass -DskipTests on the sitemanage step (WebUI/dist always skip).",
    )
    pqrc.add_argument(
        "--dist-only",
        action="store_true",
        help="Only package perc-distribution-tree (WAR inputs already fresh).",
    )
    pqrc.add_argument(
        "--timeout-seconds",
        type=int,
        default=None,
        metavar="N",
        help="Optional wall-clock timeout per Maven step.",
    )
    pqrc.add_argument(
        "--then-qa-up",
        action="store_true",
        help=(
            "After a successful rebuild chain, run qa-up "
            "(narrow optional handoff; fails if chain fails). "
            "Also copies generated-webui/cm/modern into the cell "
            "unless --skip-webui-deploy (#3948)."
        ),
    )
    pqrc.add_argument(
        "--then-qa-deploy-webui",
        action="store_true",
        help=(
            "After a successful rebuild chain (and after --then-qa-up "
            "when both are set), copy WebUI/target/generated-webui/"
            "cm/modern into the running H2 QA cell (#3948)."
        ),
    )
    pqrc.add_argument(
        "--skip-webui-deploy",
        action="store_true",
        help=(
            "Do not copy the modern SPA after --then-qa-up "
            "(opt out of the implied post-jar WebUI deploy)."
        ),
    )
    pqrc.add_argument(
        "--skip-image-build",
        action="store_true",
        help="With --then-qa-up: pass --skip-image-build to qa-up.",
    )
    pqrc.add_argument("--dry-run", action="store_true")

    pqh = sub.add_parser(
        "qa-health",
        help=(
            "Poll QA CMS health URL until ready or timeout "
            "(default: freeport/env-resolved QA CMS probe URL)."
        ),
    )
    pqh.add_argument(
        "--timeout-seconds",
        type=int,
        default=QA_PROBE_TIMEOUT_SECONDS_DEFAULT,
        help=f"Seconds to wait (default: {QA_PROBE_TIMEOUT_SECONDS_DEFAULT}).",
    )
    pqh.add_argument(
        "--interval-seconds",
        type=int,
        default=QA_PROBE_INTERVAL_SECONDS_DEFAULT,
        help=f"Poll interval seconds (default: {QA_PROBE_INTERVAL_SECONDS_DEFAULT}).",
    )
    pqh.add_argument(
        "--url",
        default=None,
        help=(
            "Probe URL (default: from QA_CMS_HOST_PORT / CMS_HOST_PORT env, "
            f"preferred {PREFERRED_QA_CMS_HOST_PORT} when free, else freeport)."
        ),
    )
    pqh.add_argument("--dry-run", action="store_true")

    pqd = sub.add_parser(
        "qa-down",
        help=(
            "Tear down the H2 QA CMS cell (docker rm -f). "
            "Frees published ports; does not leave multi-GB orphans."
        ),
    )
    pqd.add_argument(
        "--container",
        default=QA_CMS_CONTAINER,
        help=f"Container name to remove (default: {QA_CMS_CONTAINER}).",
    )
    pqd.add_argument("--dry-run", action="store_true")

    pqdw = sub.add_parser(
        "qa-deploy-webui",
        help=(
            "Hot-copy the built WebUI modern SPA (entry perc-modern-ui.js + "
            "hashed chunks + CSS + any index.html) into the H2 QA WAR. "
            "Post-jar companion: run this after rest/sitemanage SNAPSHOT "
            "copies so the live kind select keeps object-storage, "
            "rss-atom, icalendar, and sitemap-xml and Action Menus "
            "catalog New chrome (#3893 / #3948 / #4141 / #4123). "
            "Does not docker-restart the cell."
        ),
    )
    pqdw.add_argument(
        "--src",
        default=None,
        help=(
            "Host modern directory (default: "
            "WebUI/target/generated-webui/cm/modern)."
        ),
    )
    pqdw.add_argument(
        "--container",
        default=QA_CMS_CONTAINER,
        help=f"QA cell name (default: {QA_CMS_CONTAINER}).",
    )
    pqdw.add_argument(
        "--skip-object-storage-check",
        "--skip-kind-marker-check",
        action="store_true",
        dest="skip_object_storage_check",
        help=(
            "Allow a bundle whose JS lacks quoted object-storage, "
            "rss-atom, icalendar, sitemap-xml, and/or developer-am-new "
            "markers (#3948 / #4141 / #4123)."
        ),
    )
    pqdw.add_argument("--dry-run", action="store_true")

    pqdj = sub.add_parser(
        "qa-deploy-war-jars",
        help=(
            "Hot-copy perc-system, rest, and sitemanage SNAPSHOT jars "
            "into the H2 QA WAR WEB-INF/lib so skip-image-build cells "
            "allow-list sitemap-xml (#4174). Does not docker-restart "
            "the cell."
        ),
    )
    pqdj.add_argument(
        "--container",
        default=QA_CMS_CONTAINER,
        help=f"QA cell name (default: {QA_CMS_CONTAINER}).",
    )
    pqdj.add_argument(
        "--restart-jetty",
        action="store_true",
        help="In-cell StopJetty.sh then detached StartJetty.sh after copy.",
    )
    pqdj.add_argument(
        "--skip-sitemap-xml-check",
        action="store_true",
        help="Allow perc-system without PSSitemapXmlVirtualSiteSource.",
    )
    pqdj.add_argument(
        "--dest",
        default=QA_WAR_JARS_DEST,
        help=f"Absolute container WEB-INF/lib (default: {QA_WAR_JARS_DEST}).",
    )
    pqdj.add_argument("--dry-run", action="store_true")

    return p


def _resolve_paths(args: argparse.Namespace) -> tuple[Path, Path, Path]:
    """Return (repo_root, env_file, compose_file) resolved from CLI
    args. ``env_file`` falls back to ``.env.compose.example`` when
    ``.env.compose`` is absent (matches the original ``.sh``).
    """
    repo_root = (
        args.repo_root.resolve()
        if args.repo_root
        else Path(__file__).resolve().parents[2]
    )
    env_file = (
        Path(args.env_file).resolve()
        if args.env_file
        else (repo_root / DEFAULT_ENV_FILE).resolve()
    )
    if not env_file.is_file():
        env_file = (repo_root / ENV_FILE_FALLBACK).resolve()
    compose_file = (Path(args.compose_file) if Path(args.compose_file).is_absolute()
                    else (repo_root / args.compose_file)).resolve()
    return repo_root, env_file, compose_file


def _log_dir(repo_root: Path) -> Path:
    """Return the docker/logs directory (created if absent)."""
    d = repo_root / "docker" / "logs"
    d.mkdir(parents=True, exist_ok=True)
    return d


def _ts() -> str:
    return datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")


def _new_log_file(log_dir: Path, prefix: str) -> Path:
    return log_dir / f"{prefix}-{_ts()}.log"


def _run_logged(
    label: str,
    argv: Sequence[str],
    *,
    log_dir: Path,
    cwd: Optional[Path] = None,
    dry_run: bool,
) -> tuple[int, Path]:
    """Run a subprocess and capture full output to ``docker/logs/<label>-<ts>.log``.
    Emits a single ``RESULT:OK`` / ``RESULT:FAIL`` line on stdout for
    agent consumption (matches the original ``.sh`` ``run_logged``).

    Returns ``(exit_code, log_file_path)`` so callers (e.g.
    ``cmd_verify_fix``) can include the log path in their own
    ``RESULT:FAIL`` lines. The agent-workflow contract is
    ``RESULT:OK/FAIL STEP:<label> LOG:<path>``; downstream subcommands
    MUST include the path so retry/loop diagnostics stay reachable.

    Cross-platform note: the log file is opened inside a ``with``
    block in BOTH the dry-run and real-run paths so the OS file
    handle is released deterministically. On Windows, leaving an
    unclosed ``open("w")`` handle causes ``PermissionError
    [WinError 32]`` when a later code path tries to open a file in
    the same directory (the OS keeps the handle locked until
    Python's garbage collector runs, which is unpredictable across
    tests). The ``with`` block fixes this on Windows without
    affecting the POSIX behavior.
    """
    log_file = _new_log_file(log_dir, label)
    if dry_run:
        LOG.info("DRY-RUN: %s (cwd=%s)", " ".join(argv), cwd)
        with log_file.open("w", encoding="utf-8") as f:
            f.write(f"DRY-RUN: {' '.join(argv)}\n")
            if cwd:
                f.write(f"cwd={cwd}\n")
        print(f"RESULT:OK STEP:{label} LOG:{log_file}")
        return EXIT_OK, log_file
    LOG.info("Running: %s (cwd=%s)", " ".join(argv), cwd)
    with log_file.open("w", encoding="utf-8") as f:
        completed = subprocess.run(
            list(argv),
            cwd=str(cwd) if cwd else None,
            shell=False,
            check=False,
            stdout=f,
            stderr=subprocess.STDOUT,
        )
    if completed.returncode != 0:
        print(f"RESULT:FAIL STEP:{label} LOG:{log_file}")
        return EXIT_SUBPROCESS_FAILED, log_file
    print(f"RESULT:OK STEP:{label} LOG:{log_file}")
    return EXIT_OK, log_file


def _docker_compose(env_file: Path, compose_file: Path, *args: str) -> List[str]:
    """Build a ``docker compose`` argv list."""
    return [
        "docker", "compose",
        f"--env-file={env_file}",
        f"-f", str(compose_file),
        *args,
    ]


# ---------------------------------------------------------------------------
# Subcommand implementations
# ---------------------------------------------------------------------------


def cmd_install(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    install_argv = [str(repo_root / "scripts" / "install-cms-dev.py")]
    if args.reset:
        install_argv.append("--reset")
    if args.no_bootstrap:
        install_argv.append("--no-bootstrap")
    if args.install_root:
        install_argv.extend(["--install-root", args.install_root])
    rc, _log_path = _run_logged(
        "install",
        install_argv,
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    return rc


def cmd_up(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    cms_port, dts_port = ensure_compose_host_ports()
    db_ports = {
        key: int(os.environ[key])
        for key in ("MYSQL_PORT", "POSTGRES_PORT", "MSSQL_PORT")
        if os.environ.get(key)
    }
    compose_argv = _docker_compose(env_file, compose_file, "up", "-d")
    if args.build:
        compose_argv.append("--build")
    rc, _log_path = _run_logged(
        "up",
        compose_argv,
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    # Agent/operator discovery: published host ports for this worktree cell.
    print(f"CMS_PORT={cms_port}")
    print(f"DTS_PORT={dts_port}")
    for key in ("MYSQL_PORT", "POSTGRES_PORT", "MSSQL_PORT"):
        if key in db_ports:
            print(f"{key}={db_ports[key]}")
    print(f"VERIFY_CMS_URL={resolve_verify_cms_url()}")
    print(f"VERIFY_DTS_URL={resolve_verify_dts_url()}")
    return rc


def cmd_down(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    compose_argv = _docker_compose(env_file, compose_file, "down")
    if args.volumes:
        compose_argv.append("-v")
    rc, _log_path = _run_logged(
        "down",
        compose_argv,
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    return rc


def cmd_status(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    rc, _log_path = _run_logged(
        "status",
        _docker_compose(env_file, compose_file, "ps", "--format", "json"),
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    return rc


def _curl_status(url: str, *, timeout: float) -> int:
    """Return the HTTP status code for ``url``, or 0 on network error."""
    try:
        with urllib.request.urlopen(url, timeout=timeout) as r:
            return r.status
    except urllib.error.HTTPError as e:
        return e.code
    except (urllib.error.URLError, TimeoutError, ConnectionError):
        return 0


def _docker_health(container_name: str) -> str:
    """Return docker ``Health.Status`` for ``container_name``.

    Values typically seen by operators / RESULT lines (#2481 / #2537):

    * ``healthy`` / ``unhealthy`` / ``starting`` — when the image has a HEALTHCHECK
    * ``none`` — container exists but has no ``.State.Health`` block
    * ``unknown`` — docker missing, inspect failed, or empty output
    """
    if not container_name:
        return "unknown"
    completed = subprocess.run(
        [
            "docker",
            "inspect",
            "-f",
            # Prefer explicit none over docker template error when Health is absent
            # (same pattern as matrix-install-smoke wait-for-health).
            "{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}",
            container_name,
        ],
        shell=False,
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        return "unknown"
    return (completed.stdout or "").strip() or "unknown"


def _docker_logs_tail(
    container_name: str,
    *,
    tail: int = QA_LOG_SCAN_TAIL_LINES,
    timeout: float = 30.0,
) -> str:
    """Return recent ``docker logs`` text for ``container_name``, or empty.

    Used by ``qa-health`` and compose ``verify`` / ``_verify_inline`` to
    fail-fast when Jetty reports Rhythmyx context startup failure while the
    HTTP port may still answer (#2462 / #2480).
    """
    if not container_name:
        return ""
    try:
        completed = subprocess.run(
            [
                "docker",
                "logs",
                "--tail",
                str(max(1, int(tail))),
                container_name,
            ],
            shell=False,
            check=False,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
    except (OSError, subprocess.TimeoutExpired):
        return ""
    if completed.returncode != 0:
        return ""
    # docker logs may put stream content on stderr depending on version.
    out = completed.stdout or ""
    err = completed.stderr or ""
    if out and err:
        return out + "\n" + err
    return out or err


def _docker_read_server_log(
    container_name: str,
    *,
    install_root: str = QA_INSTALL_ROOT,
    tail_lines: int = QA_SERVER_LOG_TAIL_LINES,
    timeout: float = 30.0,
) -> str:
    """Tail CMS startup + install logs inside the container (perc-doctor #2556 set).

    Paths always use POSIX ``/``. Missing files are skipped.
    """
    if not container_name:
        return ""
    paths = container_cms_log_paths(install_root)
    server_log = container_server_log_path(install_root)
    logs_dir = server_log.rsplit("/", 1)[0]
    n = max(1, int(tail_lines))
    parts: list[str] = []
    for p in paths:
        parts.append(
            f'if [ -f "{p}" ]; then echo "--- {p} ---"; '
            f'tail -n {n} "{p}" 2>/dev/null || true; fi'
        )
    parts.append(
        f'for f in "{logs_dir}"/*jetty*.log; do '
        f'[ -f "$f" ] && echo "--- $f ---" && tail -n 500 "$f" 2>/dev/null || true; '
        f"done"
    )
    script = "; ".join(parts)
    try:
        completed = subprocess.run(
            ["docker", "exec", container_name, "sh", "-c", script],
            shell=False,
            check=False,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
    except (OSError, subprocess.TimeoutExpired):
        return ""
    return (completed.stdout or "") + (completed.stderr or "")


def _log_scan_fail_detail(docker_logs: str, server_logs: str) -> Optional[str]:
    """Return assessor detail when docker logs or product logs show startup fail."""
    _, detail = assess_rhythmyx_ready(
        200,
        docker_logs,
        server_log_text=server_logs,
        require_http=False,
    )
    if DETAIL_CONTEXT_FAILED in detail or DETAIL_SERVER_LOG_ERRORS in detail:
        return detail
    return None


def _match_from_logs(docker_logs: str, server_logs: str, detail: str) -> str:
    """Pick a human MATCH string from the failing log scan."""
    if DETAIL_SERVER_LOG_ERRORS in detail:
        return (
            find_server_log_startup_error(server_logs, also_context_markers=False)
            or find_server_log_startup_error(server_logs)
            or "unknown"
        )
    combined = (docker_logs or "") + "\n" + (server_logs or "")
    return find_rhythmyx_context_failure(combined) or "unknown"


def cmd_verify(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    """Poll CMS/DTS HTTP + docker health until ready or timeout / context fail.

    Ready means CMS and DTS probe HTTP codes are in the verify set, the
    cms-dts container health is ``healthy``, **and** recent ``docker logs``
    for that container do **not** contain Rhythmyx ApplicationContext
    failure markers (see :mod:`rhythmyx_ready`).

    Context-failure markers cause **immediate** FAIL even when HTTP answers
    and docker reports healthy (Jetty up, Spring webapp dead — #2480 / #2462).
    """
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    timeout = args.timeout_seconds
    interval = args.interval_seconds
    max_checks = max(1, timeout // interval) if interval > 0 else 1
    log_file = _new_log_file(log_dir, "verify")
    cms_url = resolve_verify_cms_url()
    dts_url = resolve_verify_dts_url()
    container = DEFAULT_CONTAINER

    if args.dry_run:
        LOG.info(
            "DRY-RUN: verify plan: %d checks x %ds interval against %s + %s "
            "(+ Rhythmyx context + CMS product log scan on %s)",
            max_checks, interval, cms_url, dts_url, container,
        )
        with log_file.open("w", encoding="utf-8") as f:
            f.write(
                f"DRY-RUN: verify max_checks={max_checks} interval={interval}\n"
                f"endpoints={cms_url} {dts_url}\n"
                f"container={container}\n"
                f"log_scan=rhythmyx_context_fail_markers+server_log_errors\n"
            )
        print(f"RESULT:OK STEP:verify CMS_HTTP:200 DTS_HTTP:200 HEALTH:healthy LOG:{log_file}")
        return EXIT_OK

    last_cms = 0
    last_dts = 0
    last_health = "unknown"
    last_detail = "not_checked"
    for check in range(1, max_checks + 1):
        last_cms = _curl_status(cms_url, timeout=5.0)
        last_dts = _curl_status(dts_url, timeout=5.0)
        last_health = _docker_health(container)
        logs = _docker_logs_tail(container)
        server_logs = _docker_read_server_log(container)
        # require_http=False: HTTP readiness is still gated by the verify
        # code set below; this assessor is for log fail markers only.
        _ready_ctx, last_detail = assess_rhythmyx_ready(
            last_cms, logs, server_log_text=server_logs, require_http=False,
        )

        # Fail-fast: dead Spring context or ERROR/FATAL in product/install logs.
        if DETAIL_CONTEXT_FAILED in last_detail or DETAIL_SERVER_LOG_ERRORS in last_detail:
            match = _match_from_logs(logs, server_logs, last_detail)
            detail_token = (
                DETAIL_SERVER_LOG_ERRORS
                if DETAIL_SERVER_LOG_ERRORS in last_detail
                else DETAIL_CONTEXT_FAILED
            )
            with log_file.open("w", encoding="utf-8") as f:
                f.write("verify failed\n")
                f.write(f"cms_http={last_cms}\n")
                f.write(f"dts_http={last_dts}\n")
                f.write(f"container_health={last_health}\n")
                f.write(f"check={check}\n")
                f.write(f"container={container}\n")
                f.write(f"detail={last_detail}\n")
                f.write(f"match={match}\n")
                f.write(f"cms_url={cms_url}\n")
                f.write(f"dts_url={dts_url}\n")
                f.write(
                    "hint: Rhythmyx startup not clean; Jetty HTTP / "
                    "docker health may still look green. Inspect docker logs "
                    "and jetty/base/logs/server.log + Installer logs "
                    "(parent #2423 / #2480 / #2556). Do not treat port-up as ready.\n"
                )
            print(
                f"RESULT:FAIL STEP:verify DETAIL:{detail_token} "
                f"MATCH:{match} CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
                f"HEALTH:{last_health} CONTAINER:{container} LOG:{log_file}"
            )
            return EXIT_SUBPROCESS_FAILED

        if (
            last_cms in (200, 401, 403)
            and last_dts in (200, 401, 403)
            and last_health == "healthy"
        ):
            with log_file.open("w", encoding="utf-8") as f:
                f.write("verify success\n")
                f.write(f"cms_http={last_cms}\n")
                f.write(f"dts_http={last_dts}\n")
                f.write(f"container_health={last_health}\n")
                f.write(f"cms_url={cms_url}\n")
                f.write(f"dts_url={dts_url}\n")
                f.write("rhythmyx_context=ok\n")
                f.write("server_log=ok\n")
            print(
                f"RESULT:OK STEP:verify CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
                f"HEALTH:{last_health} LOG:{log_file}"
            )
            return EXIT_OK
        time.sleep(interval)

    # Timeout path: re-scan logs once more so DETAIL prefers startup failure.
    final_logs = _docker_logs_tail(container)
    final_server = _docker_read_server_log(container)
    final_detail = _log_scan_fail_detail(final_logs, final_server)
    final_match = (
        _match_from_logs(final_logs, final_server, final_detail)
        if final_detail
        else None
    )
    with log_file.open("w", encoding="utf-8") as f:
        f.write("verify failed\n")
        f.write(f"timeout_seconds={timeout}\n")
        f.write(f"interval_seconds={interval}\n")
        f.write(f"cms_http={last_cms}\n")
        f.write(f"dts_http={last_dts}\n")
        f.write(f"container_health={last_health}\n")
        f.write(f"container={container}\n")
        f.write(f"last_detail={last_detail}\n")
        if final_match:
            f.write(f"match={final_match}\n")
            f.write(
                "hint: startup failure markers found in docker logs / product logs; "
                "see parent #2423 / #2480 / #2556. HTTP-only / health-only ready is "
                "insufficient.\n"
            )
        f.write("--- compose ps\n")
        # capture compose ps in a child log to avoid clobbering
        compose_ps_log = _new_log_file(log_dir, "verify-compose-ps")
        with compose_ps_log.open("w") as ps_f:
            subprocess.run(
                _docker_compose(env_file, compose_file, "ps"),
                shell=False,
                check=False,
                stdout=ps_f,
                stderr=subprocess.STDOUT,
            )
        f.write(f"see {compose_ps_log}\n")
    if final_detail and DETAIL_SERVER_LOG_ERRORS in final_detail:
        print(
            f"RESULT:FAIL STEP:verify DETAIL:{DETAIL_SERVER_LOG_ERRORS} "
            f"MATCH:{final_match} CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
            f"HEALTH:{last_health} CONTAINER:{container} LOG:{log_file}"
        )
    elif final_match:
        print(
            f"RESULT:FAIL STEP:verify DETAIL:{DETAIL_CONTEXT_FAILED} "
            f"MATCH:{final_match} CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
            f"HEALTH:{last_health} CONTAINER:{container} LOG:{log_file}"
        )
    else:
        print(
            f"RESULT:FAIL STEP:verify DETAIL:timeout after {timeout}s "
            f"(cms_http={last_cms} dts_http={last_dts} health={last_health}) "
            f"LOG:{log_file}"
        )
    return EXIT_SUBPROCESS_FAILED


def cmd_it_verify(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    # Windows: Maven wrapper is mvnw.cmd; Unix: mvnw (shell script).
    mvnw = repo_root / ("mvnw.cmd" if sys.platform.startswith("win") else "mvnw")
    rc, _log_path = _run_logged(
        "it-verify",
        [
            str(mvnw),
            "-P", "integration-test,docker-compose",
            "verify",
        ],
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    return rc


def _qa_deploy_webui_ns(
    *,
    dry_run: bool,
    src: Optional[str] = None,
    container: str = QA_CMS_CONTAINER,
    skip_object_storage_check: bool = False,
) -> argparse.Namespace:
    """Namespace for :func:`cmd_qa_deploy_webui` post-jar / post-up handoff."""
    return argparse.Namespace(
        dry_run=bool(dry_run),
        src=src,
        container=container,
        skip_object_storage_check=bool(skip_object_storage_check),
    )


def _qa_deploy_webui_argv(
    repo_root: Path,
    src: Optional[str],
    container: str,
    skip_object_storage_check: bool,
) -> List[str]:
    """Build the argv list for ``docker/scripts/hot-deploy-webui-modern.py``.

    Uses ``sys.executable`` so Windows/Linux/macOS all invoke the same
    interpreter that is running perc-devctl (no hardcoded ``python3``).
    """
    argv = [
        sys.executable,
        str(repo_root / "docker" / "scripts" / "hot-deploy-webui-modern.py"),
        "--container",
        container,
    ]
    if src:
        argv.extend(["--src", src])
    if skip_object_storage_check:
        argv.append("--skip-object-storage-check")
    return argv


def cmd_qa_deploy_webui(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, _env_file, _compose_file = paths
    log_dir = _log_dir(repo_root)
    rc, _log_path = _run_logged(
        "qa-deploy-webui",
        _qa_deploy_webui_argv(
            repo_root,
            getattr(args, "src", None),
            args.container,
            bool(getattr(args, "skip_object_storage_check", False)),
        ),
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    return rc


def _qa_deploy_war_jars_argv(
    repo_root: Path,
    container: str,
    restart_jetty: bool,
    skip_sitemap_xml_check: bool,
    dest: str = QA_WAR_JARS_DEST,
) -> List[str]:
    """Build the argv list for ``docker/scripts/hot-deploy-rhythmyx-war-jars.py``."""
    argv = [
        sys.executable,
        str(repo_root / "docker" / "scripts" / "hot-deploy-rhythmyx-war-jars.py"),
        "--repo-root",
        str(repo_root),
        "--container",
        container,
        "--dest",
        dest,
    ]
    if restart_jetty:
        argv.append("--restart-jetty")
    if skip_sitemap_xml_check:
        argv.append("--skip-sitemap-xml-check")
    return argv


def cmd_qa_deploy_war_jars(
    args: argparse.Namespace, paths: tuple[Path, Path, Path]
) -> int:
    repo_root, _env_file, _compose_file = paths
    log_dir = _log_dir(repo_root)
    rc, _log_path = _run_logged(
        "qa-deploy-war-jars",
        _qa_deploy_war_jars_argv(
            repo_root,
            getattr(args, "container", QA_CMS_CONTAINER),
            bool(getattr(args, "restart_jetty", False)),
            bool(getattr(args, "skip_sitemap_xml_check", False)),
            getattr(args, "dest", QA_WAR_JARS_DEST),
        ),
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    return rc


def _deploy_jar_argv(
    repo_root: Path,
    jar_path: str,
    target: str,
    restart: bool,
) -> List[str]:
    """Build the argv list for ``docker/scripts/hot-deploy-jar.py``."""
    argv = [
        "python3",
        str(repo_root / "docker" / "scripts" / "hot-deploy-jar.py"),
        "--jar", jar_path,
        "--target", target,
    ]
    if restart:
        argv.append("--restart")
    return argv


def cmd_deploy_jar(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    rc, _log_path = _run_logged(
        "deploy-jar",
        _deploy_jar_argv(repo_root, args.jar, args.target, args.restart),
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    if rc != EXIT_OK or not args.verify:
        return rc
    # ``--verify`` triggers a follow-up health check (3 minutes by default).
    rc_verify, _log_path = _verify_inline(
        repo_root, env_file, compose_file, log_dir,
        timeout_seconds=180, interval_seconds=5, dry_run=args.dry_run,
    )
    return rc_verify


def cmd_verify_fix(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, env_file, compose_file = paths
    log_dir = _log_dir(repo_root)
    restart_flag = args.restart and not args.no_restart
    # Phase 1 — deploy
    deploy_argv = _deploy_jar_argv(repo_root, args.jar, args.target, restart_flag)
    rc_deploy, deploy_log = _run_logged(
        "deploy-jar",
        deploy_argv,
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=args.dry_run,
    )
    if rc_deploy != EXIT_OK:
        # Include the deploy log path so the agent-workflow contract
        # ``RESULT:OK/FAIL STEP:<label> LOG:<path>`` is honored. Without
        # this, downstream retry/loop tooling can't find the diagnostics
        # (kilo-code-bot review thread 3631740695).
        print(f"RESULT:FAIL STEP:verify-fix PHASE:deploy LOG:{deploy_log}")
        return EXIT_SUBPROCESS_FAILED
    # Phase 2 — verify
    rc_verify, verify_log = _verify_inline(
        repo_root, env_file, compose_file, log_dir,
        timeout_seconds=args.timeout_seconds, interval_seconds=5,
        dry_run=args.dry_run,
    )
    if rc_verify != EXIT_OK:
        # Same contract — always include the verify log path
        # (kilo-code-bot review thread 3631740700).
        print(f"RESULT:FAIL STEP:verify-fix PHASE:verify LOG:{verify_log}")
        return EXIT_SUBPROCESS_FAILED
    print(f"RESULT:OK STEP:verify-fix LOG:{verify_log}")
    return EXIT_OK


def _verify_inline(
    repo_root: Path,
    env_file: Path,
    compose_file: Path,
    log_dir: Path,
    *,
    timeout_seconds: int,
    interval_seconds: int,
    dry_run: bool,
) -> tuple[int, Path]:
    """Inline verify used by ``deploy-jar --verify`` and ``verify-fix``.

    Mirrors ``cmd_verify`` (including Rhythmyx context log fail-fast —
    #2480) but bypasses the wrapped ``verify`` parser argument. Returns
    ``(exit_code, log_file_path)`` so callers (e.g. ``cmd_verify_fix``)
    can include the log path in their own ``RESULT:FAIL`` lines.
    """
    max_checks = max(1, timeout_seconds // interval_seconds) if interval_seconds > 0 else 1
    log_file = _new_log_file(log_dir, "verify")

    cms_url = resolve_verify_cms_url()
    dts_url = resolve_verify_dts_url()
    container = DEFAULT_CONTAINER

    if dry_run:
        with log_file.open("w", encoding="utf-8") as f:
            f.write(
                f"DRY-RUN: verify-inline max_checks={max_checks} interval={interval_seconds}\n"
                f"endpoints={cms_url} {dts_url}\n"
                f"container={container}\n"
                f"log_scan=rhythmyx_context_fail_markers+server_log_errors\n"
            )
        return EXIT_OK, log_file

    last_cms = 0
    last_dts = 0
    last_health = "unknown"
    for _ in range(1, max_checks + 1):
        last_cms = _curl_status(cms_url, timeout=5.0)
        last_dts = _curl_status(dts_url, timeout=5.0)
        last_health = _docker_health(container)
        logs = _docker_logs_tail(container)
        server_logs = _docker_read_server_log(container)
        _ready_ctx, detail = assess_rhythmyx_ready(
            last_cms, logs, server_log_text=server_logs, require_http=False,
        )
        if DETAIL_CONTEXT_FAILED in detail or DETAIL_SERVER_LOG_ERRORS in detail:
            match = _match_from_logs(logs, server_logs, detail)
            detail_token = (
                DETAIL_SERVER_LOG_ERRORS
                if DETAIL_SERVER_LOG_ERRORS in detail
                else DETAIL_CONTEXT_FAILED
            )
            with log_file.open("w", encoding="utf-8") as f:
                f.write("verify failed\n")
                f.write(f"cms_http={last_cms}\n")
                f.write(f"dts_http={last_dts}\n")
                f.write(f"container_health={last_health}\n")
                f.write(f"container={container}\n")
                f.write(f"detail={detail}\n")
                f.write(f"match={match}\n")
                f.write(
                    "hint: Rhythmyx startup not clean during "
                    "verify-inline (#2480 / #2423 / #2556).\n"
                )
            print(
                f"RESULT:FAIL STEP:verify DETAIL:{detail_token} "
                f"MATCH:{match} CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
                f"HEALTH:{last_health} CONTAINER:{container} LOG:{log_file}"
            )
            return EXIT_SUBPROCESS_FAILED, log_file
        if (
            last_cms in (200, 401, 403)
            and last_dts in (200, 401, 403)
            and last_health == "healthy"
        ):
            with log_file.open("w", encoding="utf-8") as f:
                f.write("verify success\n")
                f.write(f"cms_http={last_cms}\n")
                f.write(f"dts_http={last_dts}\n")
                f.write(f"container_health={last_health}\n")
                f.write("rhythmyx_context=ok\n")
                f.write("server_log=ok\n")
            print(
                f"RESULT:OK STEP:verify CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
                f"HEALTH:{last_health} LOG:{log_file}"
            )
            return EXIT_OK, log_file
        time.sleep(interval_seconds)

    final_logs = _docker_logs_tail(container)
    final_server = _docker_read_server_log(container)
    final_detail = _log_scan_fail_detail(final_logs, final_server)
    final_match = (
        _match_from_logs(final_logs, final_server, final_detail)
        if final_detail
        else None
    )
    with log_file.open("w", encoding="utf-8") as f:
        f.write("verify failed\n")
        f.write(f"cms_http={last_cms}\n")
        f.write(f"dts_http={last_dts}\n")
        f.write(f"container_health={last_health}\n")
        if final_match:
            f.write(f"match={final_match}\n")
    if final_detail and DETAIL_SERVER_LOG_ERRORS in final_detail:
        print(
            f"RESULT:FAIL STEP:verify DETAIL:{DETAIL_SERVER_LOG_ERRORS} "
            f"MATCH:{final_match} CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
            f"HEALTH:{last_health} CONTAINER:{container} LOG:{log_file}"
        )
    elif final_match:
        print(
            f"RESULT:FAIL STEP:verify DETAIL:{DETAIL_CONTEXT_FAILED} "
            f"MATCH:{final_match} CMS_HTTP:{last_cms} DTS_HTTP:{last_dts} "
            f"HEALTH:{last_health} CONTAINER:{container} LOG:{log_file}"
        )
    else:
        print(f"RESULT:FAIL STEP:verify LOG:{log_file}")
    return EXIT_SUBPROCESS_FAILED, log_file


def cmd_logs_path(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, _, _ = paths
    log_dir = _log_dir(repo_root)
    print(f"RESULT:OK STEP:logs-path LOG_DIR:{log_dir}")
    return EXIT_OK


# Note: the original ``.sh`` had a ``_docker_exec_capture`` helper that
# wrapped ``docker exec container bash -lc '<script>'``. With the refactor
# to ``_run_logged`` returning ``(rc, log_path)`` tuples (kilo-code-bot
# review thread #7 on PR #1468), that wrapper would need its signature
# updated; but the wrapper has no remaining callers in ``_DISPATCH`` so
# the simpler fix is to delete it. Direct ``docker exec`` invocations live
# in ``cmd_inspect_install`` and ``cmd_show_generated_passwords`` inline.


_INSPECT_SCRIPT = """set -euo pipefail
install_root=\"${PERC_INSTALL_ROOT:-/opt/Percussion}\"
cms_repo=\"$install_root/rxconfig/Installer/rxrepository.properties\"
dts_ds=\"$install_root/Deployment/Server/conf/perc/perc-datasources.properties\"
echo \"install_root=$install_root\"
if [ -f \"$cms_repo\" ]; then
  echo \"--- cms rxrepository.properties\"
  grep -E \"^(DB_BACKEND|DB_DRIVER_NAME|DB_DRIVER_CLASS_NAME|DB_SERVER|DB_SCHEMA|DB_NAME|UID)=\" \"$cms_repo\" || true
else
  echo \"cms_repo_missing=$cms_repo\"
fi
if [ -f \"$dts_ds\" ]; then
  echo \"--- dts perc-datasources.properties\"
  grep -E \"^(db.username|db.name|db.schema|jdbcDriver|jdbcUrl|hibernate.dialect)=\" \"$dts_ds\" || true
else
  echo \"dts_datasource_missing=$dts_ds\"
fi
"""


_SHOW_PASSWORDS_SCRIPT = """set -euo pipefail
install_root=\"${PERC_INSTALL_ROOT:-/opt/Percussion}\"
pwd_file=\"$install_root/var/config/generated/passwords\"
if [ ! -f \"$pwd_file\" ]; then
  echo \"generated_passwords_missing=$pwd_file\"
  exit 1
fi
echo \"generated_passwords_file=$pwd_file\"
cat \"$pwd_file\"
"""


def cmd_inspect_install(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, _, _ = paths
    log_dir = _log_dir(repo_root)
    rc, _log_path = _run_logged(
        "inspect-install",
        ["docker", "exec", DEFAULT_CONTAINER, "bash", "-lc", _INSPECT_SCRIPT],
        log_dir=log_dir,
        dry_run=getattr(args, "dry_run", False),
    )
    return rc


def cmd_show_generated_passwords(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    repo_root, _, _ = paths
    log_dir = _log_dir(repo_root)
    rc, _log_path = _run_logged(
        "show-generated-passwords",
        ["docker", "exec", DEFAULT_CONTAINER, "bash", "-lc", _SHOW_PASSWORDS_SCRIPT],
        log_dir=log_dir,
        dry_run=getattr(args, "dry_run", False),
    )
    return rc


# ---------------------------------------------------------------------------
# QA mode — H2-in-Docker one-shot (issue #1827 slice 1 / #1927)
# ---------------------------------------------------------------------------


def _qa_matrix_image_healthcheck_status(
    image_tag: str = QA_MATRIX_IMAGE_TAG,
    *,
    runner=None,
) -> str:
    """Detect whether the local matrix image has HEALTHCHECK baked in (#2484).

    Used by ``qa-up --skip-image-build`` to fail fast when the cached
    ``percussion-matrix-cell:local`` image is too old to flip to
    ``Health.Status=healthy`` (#2481). Without this precheck the smoke
    waits the full ``--probe-timeout`` (default 900s, 1800s on slow boxes)
    and then reports a confusing ``docker_health_timeout health=none``.
    See docker/README.md → "Docker ``Health.Status`` (in-image HEALTHCHECK)".

    Returns one of:

    * :data:`QA_IMAGE_HEALTHCHECK_OK` — image exists and has a HEALTHCHECK
      block with a non-empty ``Test`` array (the in-image
      ``rhythmyx_healthcheck.py`` script per ``docker/matrix/Dockerfile``).
    * :data:`QA_IMAGE_HEALTHCHECK_MISSING` — image exists but the
      HEALTHCHECK block is absent or empty (pre-#2481 bake). Caller must
      surface a rebuild hint.
    * :data:`QA_IMAGE_HEALTHCHECK_ABSENT` — image not present locally.
      Caller can proceed; the downstream ``matrix-install-smoke.py`` will
      build it normally when ``--skip-image-build`` is *not* set, or the
      user can let the normal build path handle it.

    Pure helper. ``runner`` defaults to :func:`subprocess.run`; tests
    inject a stub. ``docker inspect`` failures other than "no such image"
    map to :data:`QA_IMAGE_HEALTHCHECK_ABSENT` so the caller does not
    block on a transient docker daemon hiccup — the downstream smoke will
    still surface the real failure.
    """
    import json  # local import keeps top-of-file imports compact

    if runner is None:
        runner = subprocess.run

    # ``--format '{{json .Config.Healthcheck}}'`` keeps the JSON small and
    # avoids pulling the whole inspect payload; the image config has no
    # secrets we care about here.
    completed = runner(
        ["docker", "inspect", "--format", "{{json .Config.Healthcheck}}", image_tag],
        shell=False,
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        # docker inspect exits non-zero when the image is missing
        # ("Error response from daemon: No such image: ..."). Any other
        # failure (daemon down, etc.) is also treated as "absent" so the
        # smoke can still attempt a normal bring-up.
        return QA_IMAGE_HEALTHCHECK_ABSENT
    raw = (completed.stdout or "").strip()
    # ``Healthcheck=null`` means the image exists but the Dockerfile baked
    # no HEALTHCHECK directive — that is exactly the stale pre-#2481 case
    # we want to detect. Only an empty/garbage payload (without the
    # explicit ``null`` token) is treated as "absent" so we do not false-
    # positive on a daemon hiccup.
    if raw == "null":
        return QA_IMAGE_HEALTHCHECK_MISSING
    if not raw:
        return QA_IMAGE_HEALTHCHECK_ABSENT
    try:
        parsed = json.loads(raw)
    except ValueError:
        return QA_IMAGE_HEALTHCHECK_ABSENT
    if not isinstance(parsed, dict):
        return QA_IMAGE_HEALTHCHECK_ABSENT
    # Pre-#2481 images have a Healthcheck key but its Test array is empty
    # (a no-op healthcheck) — treat that as missing too so the rebuild
    # hint always fires when the smoke would otherwise time out.
    test = parsed.get("Test")
    if not test or not isinstance(test, list) or not any(test):
        return QA_IMAGE_HEALTHCHECK_MISSING
    return QA_IMAGE_HEALTHCHECK_OK


def _qa_matrix_up_argv(
    repo_root: Path,
    *,
    probe_timeout: int,
    skip_image_build: bool,
    dry_run: bool,
) -> List[str]:
    """Build argv for matrix-install-smoke CMS+H2 with ``--keep`` (pure).

    Uses ``sys.executable`` so Windows/Linux/macOS all invoke the same
    interpreter that is running perc-devctl (no hardcoded ``python3``).
    """
    script = repo_root / "docker" / "scripts" / "matrix-install-smoke.py"
    argv: List[str] = [
        sys.executable,
        str(script),
        "--repo-root",
        str(repo_root),
        "--product",
        QA_CMS_PRODUCT,
        "--db",
        QA_CMS_DB,
        "--keep",
        "--probe-timeout",
        str(probe_timeout),
    ]
    if skip_image_build:
        argv.append("--skip-image-build")
    if dry_run:
        argv.append("--dry-run")
    return argv


def _qa_destroy_argv(container_name: str) -> List[str]:
    """``docker rm -f`` argv for the QA cell (pure; unit-tested)."""
    return ["docker", "rm", "-f", container_name]


def _qa_print_endpoint_banner(
    host_port: int,
    *,
    admin_password_line: Optional[str] = None,
) -> None:
    """Emit agent-parseable endpoint + credential guidance after qa-up.

    Does not emit RESULT: lines — callers use ``_run_logged`` for that
    contract so agent parsers see a single OK/FAIL per step.
    ``host_port`` is the resolved published host port (env or freeport).
    """
    base = qa_cms_base_url(host_port)
    print(f"QA_CMS_HOST_PORT={host_port}")
    print(f"QA_CMS_URL:{base}")
    print(f"TEST_CMS_URL={base}")
    print(f"TEST_DB_TYPE={QA_CMS_DB}")
    print(f"TEST_PRODUCT={QA_CMS_PRODUCT}")
    print(f"QA_CONTAINER:{QA_CMS_CONTAINER}")
    print(f"ADMIN_USERNAME={QA_ADMIN_USERNAME}")
    if admin_password_line:
        print(admin_password_line)
    else:
        # URL path inside container always uses '/'; not a host filesystem path.
        pwd_path = f"{QA_INSTALL_ROOT}/{QA_PASSWORDS_REL}"
        print(
            "ADMIN_PASSWORD: fetch with "
            f"docker exec {QA_CMS_CONTAINER} cat {pwd_path} "
            f"(look for {QA_ADMIN_USERNAME}=…)"
        )


def _qa_fetch_admin_password(container_name: str) -> Optional[str]:
    """Best-effort read of Admin=… from generated passwords in the QA cell.

    Returns a line ``ADMIN_PASSWORD=<value>`` or None if unavailable.
    Never raises; callers treat missing passwords as non-fatal (URL is enough
    for health; Playwright login may need env set separately).
    """
    pwd_path = f"{QA_INSTALL_ROOT}/{QA_PASSWORDS_REL}"
    try:
        completed = subprocess.run(
            ["docker", "exec", container_name, "cat", pwd_path],
            shell=False,
            check=False,
            capture_output=True,
            text=True,
            timeout=30,
        )
    except (OSError, subprocess.TimeoutExpired):
        return None
    if completed.returncode != 0:
        return None
    text = completed.stdout or ""
    for line in text.splitlines():
        line = line.strip()
        if line.startswith(f"{QA_ADMIN_USERNAME}="):
            value = line.split("=", 1)[1].strip()
            if value:
                return f"ADMIN_PASSWORD={value}"
    return None


def cmd_qa_up(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    """Bring up CMS on H2 in Docker and wait until the cell is ready.

    Delegates install/start/health-wait to ``matrix-install-smoke.py`` with
    ``--product cms --db h2 --keep``. CMS wait requires docker
    ``Health.Status=healthy`` (fail-fast when already unhealthy — #2535 /
    #2481), host HTTP probe, and host ``rhythmyx_ready`` log scan. On success
    prints ``TEST_CMS_URL`` and admin credential guidance for Playwright /
    agents.

    Host port is resolved via :func:`ensure_qa_cms_host_port` (env override
    or freeport) and exported as ``QA_CMS_HOST_PORT`` / ``CMS_HOST_PORT`` so
    matrix-install-smoke docker ``-p`` and operators / Playwright agree (#2005).
    """
    repo_root, _env_file, _compose_file = paths
    log_dir = _log_dir(repo_root)
    dry_run = bool(args.dry_run)
    probe_timeout = int(args.timeout_seconds)
    skip_image_build = bool(args.skip_image_build)
    host_port = ensure_qa_cms_host_port()

    # Fail fast when --skip-image-build reuses a stale local matrix image
    # whose HEALTHCHECK was baked before #2481 — without this the smoke
    # waits the full --probe-timeout window then reports a confusing
    # ``docker_health_timeout health=none``. See docker/README.md →
    # "Docker Health.Status (in-image HEALTHCHECK)" and #2484.
    if skip_image_build and not dry_run:
        hc_status = _qa_matrix_image_healthcheck_status()
        if hc_status == QA_IMAGE_HEALTHCHECK_MISSING:
            rebuild_hint = (
                "docker build -t "
                f"{QA_MATRIX_IMAGE_TAG} "
                f"-f {repo_root / 'docker' / 'matrix' / 'Dockerfile'} "
                f"{repo_root / 'docker'}"
            )
            LOG.warning(
                "Local matrix image %s lacks HEALTHCHECK (#2481); "
                "qa-up --skip-image-build would otherwise hit "
                "docker_health_timeout after %ss. Rebuild hint: %s",
                QA_MATRIX_IMAGE_TAG,
                probe_timeout,
                rebuild_hint,
            )
            print(
                f"RESULT:FAIL STEP:qa-up "
                f"DETAIL:matrix_image_stale "
                f"IMAGE:{QA_MATRIX_IMAGE_TAG} "
                f"HINT:rebuild-image"
            )
            print(
                f"QA_DETAIL:matrix image {QA_MATRIX_IMAGE_TAG} has no "
                f"HEALTHCHECK (pre-#2481 bake). Drop --skip-image-build or "
                f"run: {rebuild_hint}"
            )
            return EXIT_SUBPROCESS_FAILED

    matrix_argv = _qa_matrix_up_argv(
        repo_root,
        probe_timeout=probe_timeout,
        skip_image_build=skip_image_build,
        dry_run=dry_run,
    )
    # Use a dedicated label so logs are easy to find for QA mode.
    # _run_logged emits RESULT:OK/FAIL STEP:qa-up LOG:<path>.
    rc, log_file = _run_logged(
        "qa-up",
        matrix_argv,
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=dry_run,
    )
    if rc != EXIT_OK:
        # Extra detail line for agent timeout diagnosis (RESULT already printed).
        print(
            f"QA_DETAIL:matrix-install-smoke failed "
            f"timeout_seconds={probe_timeout} "
            f"QA_CMS_HOST_PORT={host_port} LOG:{log_file}"
        )
        return rc

    deploy_webui = bool(getattr(args, "then_qa_deploy_webui", False))
    deploy_jars = bool(getattr(args, "then_qa_deploy_war_jars", False)) or (
        deploy_webui and skip_image_build
    )

    if dry_run:
        _qa_print_endpoint_banner(host_port)
        rc = EXIT_OK
        if deploy_webui:
            rc = cmd_qa_deploy_webui(
                _qa_deploy_webui_ns(dry_run=True, container=QA_CMS_CONTAINER),
                paths,
            )
            if rc != EXIT_OK:
                return rc
        if deploy_jars:
            return cmd_qa_deploy_war_jars(
                argparse.Namespace(
                    dry_run=True,
                    container=QA_CMS_CONTAINER,
                    restart_jetty=not bool(getattr(args, "no_restart_jetty", False)),
                    skip_sitemap_xml_check=False,
                    dest=QA_WAR_JARS_DEST,
                ),
                paths,
            )
        return rc

    admin_line = _qa_fetch_admin_password(QA_CMS_CONTAINER)
    _qa_print_endpoint_banner(host_port, admin_password_line=admin_line)
    rc = EXIT_OK
    if deploy_webui:
        rc = cmd_qa_deploy_webui(
            _qa_deploy_webui_ns(dry_run=False, container=QA_CMS_CONTAINER),
            paths,
        )
        if rc != EXIT_OK:
            return rc
    if deploy_jars:
        return cmd_qa_deploy_war_jars(
            argparse.Namespace(
                dry_run=False,
                container=QA_CMS_CONTAINER,
                restart_jetty=not bool(getattr(args, "no_restart_jetty", False)),
                skip_sitemap_xml_check=False,
                dest=QA_WAR_JARS_DEST,
            ),
            paths,
        )
    return rc


def cmd_qa_health(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    """Poll the QA CMS probe URL until ready or a clear timeout / context fail.

    Ready means:

    * HTTP status on the probe URL is in the ready set (200/302/401/403), **and**
    * Recent ``docker logs`` for the QA cell do **not** contain Rhythmyx
      ApplicationContext failure markers (see :mod:`rhythmyx_ready`), **and**
    * Product/install logs (``server.log``, InstallPackages, install,
      tablefactory — #2556 / perc-doctor check-logs) have no ERROR/FATAL/SEVERE, **and**
    * Docker ``Health.Status`` for the QA cell is ``healthy`` (matrix HEALTHCHECK
      after #2481 — same gate as compose ``verify`` for cms-dts).

    RESULT lines always include ``HEALTH:<status>`` so operators see host
    log-scan **and** inspect health in one line (#2537 residual of #2481).

    Context-failure markers or product-log ERRORs cause **immediate** FAIL even
    when HTTP answers (Jetty up, dirty startup — #2462 / #2556).

    Agents must call this after ``qa-up`` **and** after every jar copy / Jetty
    restart. Do not HTTP-poll ``/Rhythmyx/login``: connector-up + 503 can hide
    ``Failed startup of context`` / ``NoClassDefFoundError``. After copying
    ``sitemanage`` into the QA WAR ``WEB-INF/lib``, also copy a matching
    ``perc-system`` (``--skip-image-build`` does not refresh it).
    """
    repo_root, _env_file, _compose_file = paths
    log_dir = _log_dir(repo_root)
    timeout = int(args.timeout_seconds)
    interval = int(args.interval_seconds)
    host_port = resolve_qa_cms_host_port()
    url = args.url or qa_cms_probe_url(host_port)
    container = QA_CMS_CONTAINER
    max_checks = max(1, timeout // interval) if interval > 0 else 1
    log_file = _new_log_file(log_dir, "qa-health")

    if args.dry_run:
        LOG.info(
            "DRY-RUN: qa-health plan: %d checks x %ds interval against %s "
            "(+ Rhythmyx context + product log scan + docker Health.Status on %s)",
            max_checks,
            interval,
            url,
            container,
        )
        with log_file.open("w", encoding="utf-8") as f:
            f.write(
                f"DRY-RUN: qa-health max_checks={max_checks} interval={interval}\n"
                f"url={url}\n"
                f"container={container}\n"
                f"log_scan=rhythmyx_context_fail_markers+server_log_errors\n"
                f"docker_health=inspect Health.Status\n"
            )
        print(
            f"RESULT:OK STEP:qa-health HTTP:200 URL:{url} "
            f"HEALTH:healthy CONTAINER:{container} LOG:{log_file}"
        )
        return EXIT_OK

    last_code = 0
    last_health = "unknown"
    last_detail = "not_checked"
    for check in range(1, max_checks + 1):
        last_code = _curl_status(url, timeout=5.0)
        last_health = _docker_health(container)
        logs = _docker_logs_tail(container)
        server_logs = _docker_read_server_log(container)
        ready, last_detail = assess_rhythmyx_ready(
            last_code, logs, server_log_text=server_logs,
        )

        # Fail-fast: dead Spring context or ERROR/FATAL in product/install logs.
        if DETAIL_CONTEXT_FAILED in last_detail or DETAIL_SERVER_LOG_ERRORS in last_detail:
            match = _match_from_logs(logs, server_logs, last_detail)
            detail_token = (
                DETAIL_SERVER_LOG_ERRORS
                if DETAIL_SERVER_LOG_ERRORS in last_detail
                else DETAIL_CONTEXT_FAILED
            )
            with log_file.open("w", encoding="utf-8") as f:
                f.write("qa-health failed\n")
                f.write(f"url={url}\n")
                f.write(f"last_http={last_code}\n")
                f.write(f"container_health={last_health}\n")
                f.write(f"check={check}\n")
                f.write(f"container={container}\n")
                f.write(f"detail={last_detail}\n")
                f.write(f"match={match}\n")
                f.write(
                    "hint: Rhythmyx startup not clean; Jetty HTTP may "
                    "still bind. Inspect docker logs and jetty/base/logs/server.log "
                    "+ Installer logs (parent #2423 / #2556). Do not treat port-up as ready.\n"
                )
            print(
                f"RESULT:FAIL STEP:qa-health DETAIL:{detail_token} "
                f"MATCH:{match} HTTP:{last_code} HEALTH:{last_health} "
                f"URL:{url} CONTAINER:{container} LOG:{log_file}"
            )
            return EXIT_SUBPROCESS_FAILED

        # HTTP + clean logs + docker healthy (parity with compose verify).
        if ready and last_health == "healthy":
            with log_file.open("w", encoding="utf-8") as f:
                f.write("qa-health success\n")
                f.write(f"url={url}\n")
                f.write(f"http={last_code}\n")
                f.write(f"container_health={last_health}\n")
                f.write(f"check={check}\n")
                f.write(f"container={container}\n")
                f.write("rhythmyx_context=ok\n")
                f.write("server_log=ok\n")
            print(
                f"RESULT:OK STEP:qa-health HTTP:{last_code} HEALTH:{last_health} "
                f"URL:{url} CONTAINER:{container} LOG:{log_file}"
            )
            return EXIT_OK

        time.sleep(interval)

    # Timeout path: re-scan logs once more so DETAIL prefers startup failure.
    final_logs = _docker_logs_tail(container)
    final_server = _docker_read_server_log(container)
    final_detail = _log_scan_fail_detail(final_logs, final_server)
    final_match = (
        _match_from_logs(final_logs, final_server, final_detail)
        if final_detail
        else None
    )
    with log_file.open("w", encoding="utf-8") as f:
        f.write("qa-health failed\n")
        f.write(f"url={url}\n")
        f.write(f"last_http={last_code}\n")
        f.write(f"container_health={last_health}\n")
        f.write(f"timeout_seconds={timeout}\n")
        f.write(f"interval_seconds={interval}\n")
        f.write(f"container={container}\n")
        f.write(f"last_detail={last_detail}\n")
        if final_match:
            f.write(f"match={final_match}\n")
            f.write(
                "hint: startup failure markers found in docker logs / product logs; "
                "see parent #2423 / #2556. HTTP-only ready is insufficient.\n"
            )
        else:
            f.write(
                "hint: run qa-up first; ensure installer jar is built "
                "(modules/perc-distribution-tree package); if Jetty is up but "
                "login fails, scan docker logs and jetty/base/logs/server.log; "
                "also check docker inspect Health.Status on the QA cell\n"
            )
    if final_detail and DETAIL_SERVER_LOG_ERRORS in final_detail:
        print(
            f"RESULT:FAIL STEP:qa-health DETAIL:{DETAIL_SERVER_LOG_ERRORS} "
            f"MATCH:{final_match} HTTP:{last_code} HEALTH:{last_health} "
            f"URL:{url} CONTAINER:{container} LOG:{log_file}"
        )
    elif final_match:
        print(
            f"RESULT:FAIL STEP:qa-health DETAIL:{DETAIL_CONTEXT_FAILED} "
            f"MATCH:{final_match} HTTP:{last_code} HEALTH:{last_health} "
            f"URL:{url} CONTAINER:{container} LOG:{log_file}"
        )
    else:
        print(
            f"RESULT:FAIL STEP:qa-health DETAIL:timeout after {timeout}s "
            f"(last_http={last_code} health={last_health}) URL:{url} "
            f"CONTAINER:{container} LOG:{log_file}"
        )
    return EXIT_SUBPROCESS_FAILED


def cmd_qa_down(args: argparse.Namespace, paths: tuple[Path, Path, Path]) -> int:
    """Destroy the H2 QA CMS cell so ports/disk are freed by default.

    Uses ``docker rm -f`` on the matrix cell. Install lives inside the
    container filesystem (no named multi-GB volume by default), so removing
    the container frees the port and disk used by the cell.
    """
    repo_root, _env_file, _compose_file = paths
    log_dir = _log_dir(repo_root)
    container = args.container or QA_CMS_CONTAINER
    dry_run = bool(args.dry_run)
    rc, log_file = _run_logged(
        "qa-down",
        _qa_destroy_argv(container),
        log_dir=log_dir,
        cwd=repo_root,
        dry_run=dry_run,
    )
    # RESULT:OK/FAIL already emitted by _run_logged; add agent-parseable detail.
    print(f"QA_CONTAINER:{container}")
    if rc == EXIT_OK:
        print("QA_DETAIL:removed (ports/disk freed)")
    return rc


def cmd_qa_preflight(
    args: argparse.Namespace, paths: tuple[Path, Path, Path]
) -> int:
    """Run the rebuild-chain preflight (#2486 / #2532).

    Detects a stale WebUI WAR vs a freshly built sitemanage SNAPSHOT
    so the operator / agent does not launch a container that
    silently ships an outdated ``sitemanage-*.jar`` inside the WAR.
    Default uses SHA-256 content hash (mtime-resistant); pass
    ``--no-content-hash`` for legacy mtime-only. Delegates to
    ``docker/scripts/qa_preflight.py``.
    """
    import qa_preflight

    repo_root, _env_file, _compose_file = paths
    log_dir = _log_dir(repo_root)
    log_file = log_dir / f"qa-preflight-{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}.log"
    if args.dry_run:
        print("RESULT:OK STEP:qa-preflight LOG:")
        print("PREFLIGHT: dry-run — skipping filesystem checks")
        return EXIT_OK
    argv = [
        "--repo-root", str(repo_root),
        "--log-file", str(log_file),
        "--strict" if args.strict else "--no-strict",
    ]
    if getattr(args, "no_content_hash", False):
        argv.append("--no-content-hash")
    rc = qa_preflight.main(argv)
    # Mirror the rest of perc-devctl: emit a single RESULT line for agents.
    if rc == 0:
        print(f"RESULT:OK STEP:qa-preflight LOG:{log_file}")
    else:
        print(f"RESULT:FAIL STEP:qa-preflight LOG:{log_file}")
    return rc


def cmd_qa_rebuild_chain(
    args: argparse.Namespace, paths: tuple[Path, Path, Path]
) -> int:
    """Drive the documented Maven rebuild chain (#2533).

    Delegates to ``docker/scripts/qa_rebuild_chain.py`` (portable
    ``mvnw``/``mvnw.cmd``, ``shell=False``, RESULT lines). Optional
    ``--then-qa-up`` runs ``cmd_qa_up`` after a successful chain only.
    After ``--then-qa-up`` the generated ``cm/modern`` tree is copied
    into the H2 QA cell unless ``--skip-webui-deploy`` (#3948). Explicit
    ``--then-qa-deploy-webui`` copies the tree even without qa-up (cell
    already running after a jar-only hot-deploy).
    """
    import qa_rebuild_chain

    repo_root, _env_file, _compose_file = paths
    log_dir = _log_dir(repo_root)
    rc = qa_rebuild_chain.run_chain(
        repo_root,
        dry_run=args.dry_run,
        skip_tests=args.skip_tests,
        dist_only=args.dist_only,
        log_dir=log_dir,
        timeout_seconds=args.timeout_seconds,
    )
    if rc != EXIT_OK:
        return rc
    if args.then_qa_up:
        # Narrow handoff: reuse qa-up after a green rebuild chain.
        # Rebuild-chain ``--timeout-seconds`` is optional (None = no Maven
        # cap); qa-up requires a concrete probe timeout — fall back to the
        # standard QA default when the operator did not pass one.
        qa_up_args = argparse.Namespace(
            dry_run=bool(args.dry_run),
            timeout_seconds=(
                args.timeout_seconds
                if args.timeout_seconds is not None
                else QA_PROBE_TIMEOUT_SECONDS_DEFAULT
            ),
            skip_image_build=bool(getattr(args, "skip_image_build", False)),
            then_qa_deploy_webui=False,
        )
        rc = cmd_qa_up(qa_up_args, paths)
        if rc != EXIT_OK:
            return rc
    deploy_webui = bool(getattr(args, "then_qa_deploy_webui", False)) or (
        bool(args.then_qa_up) and not bool(getattr(args, "skip_webui_deploy", False))
    )
    if deploy_webui:
        return cmd_qa_deploy_webui(
            _qa_deploy_webui_ns(
                dry_run=bool(args.dry_run),
                container=QA_CMS_CONTAINER,
            ),
            paths,
        )
    return rc


# ---------------------------------------------------------------------------
# Dispatch
# ---------------------------------------------------------------------------


_DISPATCH = {
    "install": cmd_install,
    "up": cmd_up,
    "down": cmd_down,
    "status": cmd_status,
    "verify": cmd_verify,
    "it-verify": cmd_it_verify,
    "deploy-jar": cmd_deploy_jar,
    "verify-fix": cmd_verify_fix,
    "logs-path": cmd_logs_path,
    "inspect-install": cmd_inspect_install,
    "show-generated-passwords": cmd_show_generated_passwords,
    "qa-up": cmd_qa_up,
    "qa-health": cmd_qa_health,
    "qa-down": cmd_qa_down,
    "qa-preflight": cmd_qa_preflight,
    "qa-rebuild-chain": cmd_qa_rebuild_chain,
    "qa-deploy-webui": cmd_qa_deploy_webui,
    "qa-deploy-war-jars": cmd_qa_deploy_war_jars,
}


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    paths = _resolve_paths(args)
    handler = _DISPATCH.get(args.command)
    if handler is None:
        LOG.error("ERROR: unknown subcommand: %s", args.command)
        return EXIT_INVOCATION
    return handler(args, paths)


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())
