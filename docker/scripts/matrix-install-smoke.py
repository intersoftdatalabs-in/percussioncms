#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Ephemeral CMS/DTS install matrix smoke for #1500 (and future DB backends).

Layer 1 harness:

  1. Resolve installer jars from Maven ``target/``
  2. Start external DB container if needed (compose profiles)
  3. ``docker run --rm`` a matrix cell that:
       - copies/mounts the installer jar
       - silent + no-tty install with ``--db.*``
       - starts Jetty (CMS) or Tomcat (DTS)
  4. Probe login / health URL from the host; CMS cells also require
     docker ``Health.Status=healthy`` (fail-fast when already unhealthy —
     #2535 residual of #2481 HEALTHCHECK) while keeping host
     ``rhythmyx_ready`` log scan as belt-and-braces
  5. Record JSON + RESULT line under ``docker/logs/``
  6. Destroy the cell unless ``--keep``
  7. Stop external DBs this process started (unless ``--keep`` / ``--keep-db``)

DB lifecycle (#1516)
--------------------
External compose DBs (``percussion-postgres`` / ``-mysql`` / ``-sqlserver`` /
``-oracle``) are started only when not already running. After the matrix report
is written:

* **Default** — stop services this process brought up (``compose stop``, no ``-v``).
* ``--keep`` — leave cells **and** DBs up (Playwright Layer 2 / debugging).
* ``--keep-db`` — destroy cells (unless ``--keep``) but leave external DBs running.
* ``--stop-db`` — stop every external DB used by this matrix, even if pre-existing
  (destructive; does not remove volumes).

Operator-owned DBs that were already running before the harness are left alone
unless ``--stop-db`` is set.

Usage
-----
::

    # CMS + H2 (no external DB)
    python3 docker/scripts/matrix-install-smoke.py --product cms --db h2

    # CMS + PostgreSQL
    python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql

    # CMS + Oracle XE (compose profile oracle; heavy image — see docker/README.md)
    python3 docker/scripts/matrix-install-smoke.py --product cms --db oracle

    # Both products × H2 and PostgreSQL
    python3 docker/scripts/matrix-install-smoke.py --product cms,dts --db h2,postgresql

    # Leave stack up for Playwright Layer 2
    python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql --keep

    # Destroy cells but reuse DBs for the next matrix run
    python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql --keep-db

    # Dry-run (no docker/mvn)
    python3 docker/scripts/matrix-install-smoke.py --product cms --db h2 --dry-run

    # CMS cells run rebuild-chain preflight by default (strict STALE refuse).
    # Override only when intentionally debugging a known-stale tree:
    python3 docker/scripts/matrix-install-smoke.py --product cms --db h2 --skip-preflight

Exit codes
----------
0  all selected cells passed
1  invocation / config error
2  one or more cells failed (including CMS preflight STALE)
"""

from __future__ import annotations

import argparse
import json
import logging
import os
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Iterable, List, Mapping, Optional, Sequence, Set, Tuple

# Sibling freeport helpers (stdlib only). Ensure scripts dir is importable
# when this file is loaded via importlib (unit tests) or as a script.
_SCRIPTS_DIR = Path(__file__).resolve().parent
if str(_SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS_DIR))
from perc_host_ports import find_free_port, is_port_free, resolve_host_port  # noqa: E402
from rhythmyx_ready import (  # noqa: E402
    DETAIL_CONTEXT_FAILED,
    find_rhythmyx_context_failure,
    is_http_ready_code,
)
import qa_preflight  # noqa: E402  # rebuild-chain preflight (#2531 / #2486)

LOG = logging.getLogger("matrix-install-smoke")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_CELL_FAILED = 2

# DETAIL tokens for docker Health.Status wait policy (#2535 residual of #2481).
# Coordinate RESULT shape with perc-devctl HEALTH: column (#2537): same tokens.
DETAIL_DOCKER_UNHEALTHY = "docker_health_unhealthy"
DETAIL_DOCKER_HEALTH_TIMEOUT = "docker_health_timeout"
DETAIL_CONTAINER_NOT_RUNNING = "container_not_running"
# Rebuild-chain preflight refused CMS matrix cells (#2531 residual of #2486).
DETAIL_PREFLIGHT_STALE = "preflight_stale"

# docker inspect format shared by DB + CMS health waits (#2481 / #2535).
_DOCKER_HEALTH_INSPECT_FMT = (
    "{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}"
    "|{{.State.Status}}"
)

DEFAULT_COMPOSE_FILE = "docker-compose.yml"
DEFAULT_ENV_FILE = ".env.compose"
ENV_FILE_FALLBACK = ".env.compose.example"
MATRIX_IMAGE_TAG = "percussion-matrix-cell:local"
MATRIX_NETWORK = "perc-matrix-net"

# Preferred host ports when free and no env override (single-worktree baseline).
# Multi-worktree: set CMS_HOST_PORT / QA_CMS_HOST_PORT / DTS_HOST_PORT (or leave
# unset so freeport allocates) — see resolve_matrix_host_port / docker/README.md.
# Historical constants kept as aliases for callers/tests that still reference them.
PREFERRED_CMS_HOST_PORT = 9993
PREFERRED_DTS_HOST_PORT = 9983
CMS_HOST_PORT = PREFERRED_CMS_HOST_PORT  # preferred baseline (not a pinned bind)
DTS_HOST_PORT = PREFERRED_DTS_HOST_PORT
CMS_PROBE_PATH = "/Rhythmyx/login"
DTS_PROBE_PATH = "/"

# Compose DB published host ports (docker-compose.yml ``${*_PORT:-…}:container``).
# Container listen ports and matrix cell DB_PORT stay fixed (3306/5432/1433/1521);
# cells reach DBs via Docker DNS on perc-matrix-net, not host publish ports.
# Multi-worktree: set MYSQL_PORT / POSTGRES_PORT / MSSQL_PORT / ORACLE_PORT or
# leave unset so freeport allocates — see ensure_compose_db_host_ports / README.
PREFERRED_MYSQL_HOST_PORT = 3306
PREFERRED_POSTGRES_HOST_PORT = 5433
PREFERRED_MSSQL_HOST_PORT = 1433
PREFERRED_ORACLE_HOST_PORT = 1521

# matrix db_type → (compose env key, preferred host port)
COMPOSE_DB_HOST_PORT_SPEC: Dict[str, Tuple[str, int]] = {
    "mysql": ("MYSQL_PORT", PREFERRED_MYSQL_HOST_PORT),
    "postgresql": ("POSTGRES_PORT", PREFERRED_POSTGRES_HOST_PORT),
    "sqlserver": ("MSSQL_PORT", PREFERRED_MSSQL_HOST_PORT),
    "oracle": ("ORACLE_PORT", PREFERRED_ORACLE_HOST_PORT),
}

# Compose service name → stable container_name from docker-compose.yml
CONTAINER_BY_SERVICE: Dict[str, str] = {
    "postgres": "percussion-postgres",
    "mysql": "percussion-mysql",
    "sqlserver": "percussion-sqlserver",
    "oracle": "percussion-oracle",
}

PRODUCTS = ("cms", "dts")
# Static shape without passwords (passwords resolved from env / .env.compose).
_DB_SERVICE_BASE: Dict[str, Dict[str, str]] = {
    "h2": {
        "profile": "",
        "service": "",
        "host": "",
        "port": "",
        "container_host": "",
    },
    "postgresql": {
        "profile": "postgres",
        "service": "postgres",
        "host": "localhost",
        "port": "5432",
        "container_host": "postgres",
        "user": "percuser",
        "password_env": "POSTGRES_PASSWORD",
        "name": "percdb",
        "schema": "public",
    },
    "mysql": {
        "profile": "mysql",
        "service": "mysql",
        "host": "localhost",
        "port": "3306",
        "container_host": "mysql",
        "user": "percuser",
        "password_env": "MYSQL_PASSWORD",
        "name": "percdb",
    },
    "sqlserver": {
        "profile": "sqlserver",
        "service": "sqlserver",
        "host": "localhost",
        "port": "1433",
        "container_host": "sqlserver",
        "user": "sa",
        "password_env": "MSSQL_SA_PASSWORD",
        "name": "percdb",
    },
    # Oracle XE (gvenzl/oracle-xe): --db.name is service name XEPDB1 (Easy Connect
    # service form; not a SID). APP_USER is CMS user/schema. Password from
    # ORACLE_APP_PASSWORD. Long first-start — wait_for_container_healthy +
    # WAIT_DB_SECONDS in the cell.
    "oracle": {
        "profile": "oracle",
        "service": "oracle",
        "host": "localhost",
        "port": "1521",
        "container_host": "oracle",
        "user": "percuser",
        "password_env": "ORACLE_APP_PASSWORD",
        "name": "XEPDB1",
        "schema": "percuser",
        # Host-side compose health wait (seconds) before docker run cell.
        "healthy_timeout": "600",
        # Cell-side TCP wait after network attach (seconds).
        "wait_db_seconds": "600",
    },
}

DB_TYPES = tuple(_DB_SERVICE_BASE.keys())


def load_env_file(path: Path) -> Dict[str, str]:
    """Parse a simple ``KEY=VALUE`` env file (no export, no multi-line)."""
    out: Dict[str, str] = {}
    if not path.is_file():
        return out
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, val = line.partition("=")
        key = key.strip()
        if not key:
            continue
        val = val.strip()
        if len(val) >= 2 and val[0] == val[-1] and val[0] in ("'", '"'):
            val = val[1:-1]
        out[key] = val
    return out


def resolve_env_file(repo_root: Path, env_file: Optional[Path] = None) -> Path:
    """Prefer ``.env.compose``, fall back to ``.env.compose.example``."""
    if env_file is not None:
        return env_file.resolve()
    preferred = (repo_root / DEFAULT_ENV_FILE).resolve()
    if preferred.is_file():
        return preferred
    return (repo_root / ENV_FILE_FALLBACK).resolve()


def _pick_env(env: Mapping[str, str], *keys: str, default: str = "") -> str:
    """First non-empty value from ``env`` then ``os.environ``."""
    for key in keys:
        if not key:
            continue
        val = (env.get(key) or os.environ.get(key) or "").strip()
        if val:
            return val
    return default


def build_db_services(env: Mapping[str, str]) -> Dict[str, Dict[str, str]]:
    """Build DB metadata; passwords come from env keys (never hardcoded)."""
    services: Dict[str, Dict[str, str]] = {}
    for name, base in _DB_SERVICE_BASE.items():
        meta = {k: v for k, v in base.items() if k != "password_env"}
        pwd_key = base.get("password_env", "")
        if pwd_key:
            meta["password"] = _pick_env(env, pwd_key)
        if name == "postgresql":
            meta["user"] = _pick_env(env, "POSTGRES_USER", default=meta.get("user", "percuser"))
            meta["name"] = _pick_env(env, "POSTGRES_DB", default=meta.get("name", "percdb"))
        elif name == "mysql":
            meta["user"] = _pick_env(env, "MYSQL_USER", default=meta.get("user", "percuser"))
            meta["name"] = _pick_env(env, "MYSQL_DATABASE", default=meta.get("name", "percdb"))
        elif name == "sqlserver":
            meta["user"] = "sa"
        elif name == "oracle":
            # APP_USER / service name / schema — never hardcode secrets.
            meta["user"] = _pick_env(
                env, "ORACLE_APP_USER", "APP_USER", default=meta.get("user", "percuser")
            )
            meta["name"] = _pick_env(
                env, "ORACLE_SERVICE", default=meta.get("name", "XEPDB1")
            )
            # Schema defaults to the app user (Oracle unquoted identifiers).
            meta["schema"] = _pick_env(
                env, "ORACLE_SCHEMA", default=meta.get("user", "percuser")
            )
        services[name] = meta
    return services


def require_db_passwords(
    services: Mapping[str, Mapping[str, str]], db_types: Iterable[str]
) -> None:
    """Fail fast if an external DB cell lacks a password from env."""
    missing: List[str] = []
    for db_type in db_types:
        meta = services.get(db_type) or {}
        if not meta.get("service"):
            continue
        if not (meta.get("password") or "").strip():
            pwd_key = _DB_SERVICE_BASE.get(db_type, {}).get("password_env", "PASSWORD")
            missing.append(f"{db_type} ({pwd_key})")
    if missing:
        raise ValueError(
            "Missing DB password(s) for: "
            + ", ".join(missing)
            + ". Copy .env.compose.example to .env.compose and set credentials "
            "(or export the matching env vars)."
        )


# Module-level snapshot for unit tests that only need host/port/service shape.
# Passwords empty unless process env already has them (never hardcoded).
DB_SERVICES: Dict[str, Dict[str, str]] = build_db_services(os.environ)


def db_container_name(service: str) -> str:
    """Return the Docker container name for a compose DB service."""
    return CONTAINER_BY_SERVICE.get(service, f"percussion-{service}")


def external_db_types(db_types: Iterable[str]) -> Set[str]:
    """Return matrix DB keys that use an external compose service (not H2)."""
    out: Set[str] = set()
    for db_type in db_types:
        meta = _DB_SERVICE_BASE.get(db_type) or {}
        if meta.get("service"):
            out.add(db_type)
    return out


def select_dbs_to_stop(
    *,
    started_by_matrix: Iterable[str],
    used_external: Iterable[str],
    keep: bool,
    keep_db: bool,
    stop_db: bool,
) -> Set[str]:
    """Decide which external DB types to stop after a matrix run (pure policy).

    Parameters
    ----------
    started_by_matrix
        DB types this process actually brought up via ``compose up``.
    used_external
        External DB types selected for the matrix (pre-existing or started).
    keep
        Leave cells and DBs up (Layer 2).
    keep_db
        Leave DBs up even when cells are destroyed.
    stop_db
        Hard-stop every used external DB, including pre-existing ones.

    ``--keep`` and ``--keep-db`` always win over ``--stop-db``.
    Never includes H2 (no external container).
    """
    if keep or keep_db:
        return set()
    used = set(used_external)
    if stop_db:
        return used
    return set(started_by_matrix) & used


@dataclass
class CellSpec:
    product: str
    db_type: str

    @property
    def cell_id(self) -> str:
        return f"{self.product}-{self.db_type}"


@dataclass
class CellResult:
    cell_id: str
    product: str
    db_type: str
    status: str  # pass | fail | skip
    probe_url: str = ""
    container_name: str = ""
    detail: str = ""
    duration_seconds: float = 0.0
    log_path: str = ""


@dataclass
class MatrixReport:
    started_at: str
    finished_at: str = ""
    results: List[CellResult] = field(default_factory=list)

    def to_dict(self) -> dict:
        return {
            "started_at": self.started_at,
            "finished_at": self.finished_at,
            "results": [asdict(r) for r in self.results],
            "passed": sum(1 for r in self.results if r.status == "pass"),
            "failed": sum(1 for r in self.results if r.status == "fail"),
        }


def repo_root_from(script_path: Path) -> Path:
    # docker/scripts/matrix-install-smoke.py → repo root is parents[2]
    return script_path.resolve().parent.parent.parent


def parse_csv(value: str, allowed: Sequence[str], label: str) -> List[str]:
    items = [p.strip().lower() for p in value.split(",") if p.strip()]
    if not items:
        raise ValueError(f"{label} must not be empty")
    bad = [i for i in items if i not in allowed]
    if bad:
        raise ValueError(f"Unknown {label} value(s): {bad}; allowed: {list(allowed)}")
    return items


def expand_matrix(products: Sequence[str], dbs: Sequence[str]) -> List[CellSpec]:
    return [CellSpec(product=p, db_type=d) for p in products for d in dbs]


def check_cms_rebuild_preflight(
    repo_root: Path,
    m2_root: Optional[Path] = None,
) -> Tuple[bool, str]:
    """Run rebuild-chain preflight for CMS product cells (#2531 / #2486).

    Compares the newest ``sitemanage-*.jar`` under the maven local repo with
    the ``perc-web-ui-*.war`` under ``WebUI/target`` so a stale WAR cannot
    silently ship into the matrix cell installer path.

    Returns ``(ok, report)`` where ``ok`` is False when the tree is STALE
    (strict refuse). Missing m2 sitemanage is not STALE (check is a no-op
    NOTE); see :func:`qa_preflight.is_stale`.

    Pure filesystem — no docker, curl, or maven. Safe to call from dry-run
    and unit tests with a stub ``repo_root`` / ``m2_root``.
    """
    m2 = m2_root if m2_root is not None else qa_preflight._default_m2_root()
    rows = qa_preflight.run_preflight(repo_root, m2)
    report = qa_preflight.format_report(rows, strict=True)
    return (not qa_preflight.is_stale(rows), report)


# Customer-shipped installer assembly names (maven-assembly finalName, not *-SNAPSHOT.jar).
CMS_INSTALLER_JAR_NAME = "perc-distribution-tree.jar"
DTS_INSTALLER_JAR_NAME = "delivery-tier-distribution.jar"


def resolve_installer_jar(repo_root: Path, product: str) -> Path:
    """Resolve the **shipped** installer assembly jar only.

    CMS ships ``perc-distribution-tree.jar``; DTS ships
    ``delivery-tier-distribution.jar``. These are the maven-assembly
    ``jar-with-dependencies`` artifacts with ``Main-Class`` set — the same
    files given to customers. Versioned ``*-SNAPSHOT.jar`` / ``*-javadoc.jar``
    artifacts from the plain jar plugin are **never** used.
    """
    if product == "cms":
        path = (
            repo_root
            / "modules"
            / "perc-distribution-tree"
            / "target"
            / CMS_INSTALLER_JAR_NAME
        )
        build_hint = (
            "cd modules/perc-distribution-tree && ../../mvnw package "
            f"(produces target/{CMS_INSTALLER_JAR_NAME})"
        )
    elif product == "dts":
        path = (
            repo_root
            / "deliverytiersuite"
            / "delivery-tier-suite"
            / "delivery-tier-distribution"
            / "target"
            / DTS_INSTALLER_JAR_NAME
        )
        build_hint = (
            "cd deliverytiersuite/delivery-tier-suite/delivery-tier-distribution "
            f"&& ../../../../mvnw package (produces target/{DTS_INSTALLER_JAR_NAME})"
        )
    else:
        raise ValueError(f"Unknown product: {product}")

    if not path.is_file() or path.stat().st_size == 0:
        raise FileNotFoundError(
            f"Shipped installer jar missing or empty: {path}. Build with: {build_hint}. "
            "Do not use *-SNAPSHOT.jar — only the unversioned assembly jar is the customer artifact."
        )
    return path


def build_probe_url(product: str, host_port: int) -> str:
    path = CMS_PROBE_PATH if product == "cms" else DTS_PROBE_PATH
    return f"http://127.0.0.1:{host_port}{path}"


def resolve_matrix_host_port(product: str) -> int:
    """Resolve published host port for a matrix cell (env or freeport).

    CMS (aligned with ``perc-devctl`` QA cell / ``TEST_CMS_URL``):

      1. ``QA_CMS_HOST_PORT`` or ``CMS_HOST_PORT`` env (integer)
      2. Preferred :data:`PREFERRED_CMS_HOST_PORT` (9993) when free
      3. Ephemeral freeport

    DTS:

      1. ``DTS_HOST_PORT`` env
      2. Preferred :data:`PREFERRED_DTS_HOST_PORT` (9983) when free
      3. Ephemeral freeport

    Does not pin env by itself — use :func:`ensure_matrix_host_port` when
    the chosen port should be visible to child processes / operators.
    """
    product = product.lower().strip()
    if product == "cms":
        return resolve_host_port(
            "QA_CMS_HOST_PORT",
            "CMS_HOST_PORT",
            preferred=PREFERRED_CMS_HOST_PORT,
        )
    if product == "dts":
        return resolve_host_port(
            "DTS_HOST_PORT",
            preferred=PREFERRED_DTS_HOST_PORT,
        )
    raise ValueError(f"Unknown product for host port: {product}")


def ensure_matrix_host_port(product: str) -> int:
    """Resolve matrix host port and pin discovery env for operators / Playwright.

    CMS pins ``CMS_HOST_PORT`` and ``QA_CMS_HOST_PORT`` (setdefault for the
    second key when already set by ``perc-devctl qa-up``). DTS pins
    ``DTS_HOST_PORT``.
    """
    port = resolve_matrix_host_port(product)
    product = product.lower().strip()
    if product == "cms":
        os.environ["CMS_HOST_PORT"] = str(port)
        os.environ.setdefault("QA_CMS_HOST_PORT", str(port))
    elif product == "dts":
        os.environ["DTS_HOST_PORT"] = str(port)
    return port


def resolve_compose_db_host_port(db_type: str) -> int:
    """Resolve published host port for a compose external DB (env or freeport).

    Resolution order (via :func:`resolve_host_port`):

      1. Env override: ``MYSQL_PORT`` / ``POSTGRES_PORT`` / ``MSSQL_PORT`` /
         ``ORACLE_PORT``
      2. Preferred baseline when free (3306 / 5433 / 1433 / 1521)
      3. Ephemeral freeport

    Does not pin env — use :func:`ensure_compose_db_host_ports` before
    ``docker compose up`` so shell env overrides ``.env.compose`` defaults.
    H2 and unknown types raise ``ValueError``.
    """
    db_type = db_type.lower().strip()
    spec = COMPOSE_DB_HOST_PORT_SPEC.get(db_type)
    if spec is None:
        raise ValueError(
            f"No compose host-port mapping for db_type={db_type!r}; "
            f"known: {sorted(COMPOSE_DB_HOST_PORT_SPEC)}"
        )
    env_key, preferred = spec
    return resolve_host_port(env_key, preferred=preferred)


def ensure_compose_db_host_ports(db_types: Iterable[str]) -> Dict[str, int]:
    """Resolve and pin compose DB host ports for external DBs in ``db_types``.

    Pins ``MYSQL_PORT`` / ``POSTGRES_PORT`` / ``MSSQL_PORT`` / ``ORACLE_PORT``
    into ``os.environ`` so concurrent worktrees and ``docker compose`` publish
    / probe stay consistent (#2004). Skips H2 and unknown non-external types.
    Returns mapping of matrix ``db_type`` → resolved host port.
    """
    resolved: Dict[str, int] = {}
    for raw in db_types:
        db_type = raw.lower().strip()
        if db_type not in COMPOSE_DB_HOST_PORT_SPEC:
            continue
        env_key, _preferred = COMPOSE_DB_HOST_PORT_SPEC[db_type]
        port = resolve_compose_db_host_port(db_type)
        os.environ[env_key] = str(port)
        resolved[db_type] = port
        LOG.info(
            "Compose DB host publish %s %s=%s (container port unchanged)",
            db_type,
            env_key,
            port,
        )
    return resolved


def build_docker_run_argv(
    *,
    image: str,
    container_name: str,
    product: str,
    db_type: str,
    installer_jar_host: Path,
    host_port: int,
    network: str,
    db_meta: Dict[str, str],
    keep: bool,
) -> List[str]:
    """Build ``docker run`` argv for one cell (pure; unit-tested)."""
    container_port = 9992 if product == "cms" else 9980
    # Jetty/Tomcat defaults inside install; we publish host_port → container_port.
    # If product listen port differs post-install, probe still uses host_port.
    argv = [
        "docker",
        "run",
        "-d",
        "--name",
        container_name,
        "--network",
        network,
        "-p",
        f"{host_port}:{container_port}",
        "-v",
        f"{installer_jar_host.resolve()}:/installer/installer.jar:ro",
        "-e",
        f"PRODUCT={product}",
        "-e",
        "INSTALLER_JAR=/installer/installer.jar",
        "-e",
        "INSTALL_ROOT=/opt/Percussion",
        "-e",
        f"DB_TYPE={db_type}",
        "-e",
        "SILENT=true",
        "-e",
        "KEEP_ALIVE=true",
    ]
    if db_type not in ("h2", "derby"):
        argv.extend(
            [
                "-e",
                f"DB_HOST={db_meta.get('container_host', '')}",
                "-e",
                f"DB_PORT={db_meta.get('port', '')}",
                "-e",
                f"DB_NAME={db_meta.get('name', 'percdb')}",
                "-e",
                f"DB_USER={db_meta.get('user', 'percuser')}",
                "-e",
                f"DB_PASSWORD={db_meta.get('password', '')}",
            ]
        )
        schema = db_meta.get("schema", "")
        if schema:
            argv.extend(["-e", f"DB_SCHEMA={schema}"])
        wait_db = (db_meta.get("wait_db_seconds") or "").strip()
        if wait_db:
            argv.extend(["-e", f"WAIT_DB_SECONDS={wait_db}"])
    argv.append(image)
    return argv


def _run(
    argv: Sequence[str],
    *,
    dry_run: bool,
    check: bool = False,
    capture: bool = False,
    timeout: Optional[int] = None,
) -> subprocess.CompletedProcess:
    LOG.info("exec: %s", " ".join(str(a) for a in argv))
    if dry_run:
        return subprocess.CompletedProcess(argv, 0, stdout="", stderr="")
    return subprocess.run(
        list(argv),
        shell=False,
        check=check,
        capture_output=capture,
        text=True,
        timeout=timeout,
    )


def ensure_network(network: str, *, dry_run: bool) -> None:
    listed = _run(
        ["docker", "network", "ls", "--format", "{{.Name}}"],
        dry_run=dry_run,
        capture=True,
    )
    names = (listed.stdout or "").splitlines() if not dry_run else []
    if dry_run or network not in names:
        _run(["docker", "network", "create", network], dry_run=dry_run, check=False)


def build_matrix_image(repo_root: Path, *, dry_run: bool) -> None:
    """Build matrix cell image with context ``docker/`` (matrix + scripts).

    Context is the ``docker/`` tree (not monorepo root, not ``docker/matrix/``
    alone) so the image can COPY ``scripts/rhythmyx_ready.py`` and
    ``scripts/rhythmyx_healthcheck.py`` for the in-image HEALTHCHECK (#2481).
    """
    docker_dir = repo_root / "docker"
    dockerfile = docker_dir / "matrix" / "Dockerfile"
    _run(
        [
            "docker",
            "build",
            "-t",
            MATRIX_IMAGE_TAG,
            "-f",
            str(dockerfile),
            str(docker_dir),
        ],
        dry_run=dry_run,
        check=not dry_run,
    )


def start_db(
    repo_root: Path,
    compose_file: Path,
    db_type: str,
    *,
    dry_run: bool,
    db_services: Mapping[str, Mapping[str, str]],
    env_file: Optional[Path] = None,
) -> Optional[bool]:
    """Start an external compose DB for ``db_type`` if needed.

    Returns
    -------
    Optional[bool]
        ``None`` if ``db_type`` has no external compose service (e.g. H2).
        ``True`` if this invocation brought the service up (or would under
        ``--dry-run``).
        ``False`` if the container was already running (operator-owned / reused).
    """
    meta = db_services[db_type]
    profile = meta.get("profile") or ""
    service = meta.get("service") or ""
    if not profile or not service:
        return None
    # Attach DB container to matrix network with a DNS alias matching the
    # compose service name (e.g. "postgres") so cells can use DB_HOST=postgres.
    container = db_container_name(service)
    started_by_matrix = False

    # Pin freeport/env host publish before compose so multi-worktree cells do
    # not collide on MYSQL_PORT / POSTGRES_PORT / MSSQL_PORT / ORACLE_PORT
    # (#2004). Process env overrides .env.compose defaults for compose.
    ensure_compose_db_host_ports([db_type])

    # If compose DB is already running (common on long-lived dev hosts), skip
    # ``compose up`` so a host port clash (e.g. local Postgres on 5432) does not
    # fail the cell after a no-op recreate attempt.
    if not dry_run and _docker_container_running(container):
        LOG.info("DB container %s already running; skipping compose up", container)
    else:
        argv = [
            "docker",
            "compose",
            "-f",
            str(compose_file),
        ]
        if env_file is not None and env_file.is_file():
            argv.extend(["--env-file", str(env_file)])
        argv.extend(
            [
                "--profile",
                profile,
                "up",
                "-d",
                service,
            ]
        )
        _run(argv, dry_run=dry_run, check=not dry_run)
        started_by_matrix = True

    _run(
        [
            "docker",
            "network",
            "connect",
            "--alias",
            service,
            MATRIX_NETWORK,
            container,
        ],
        dry_run=dry_run,
        check=False,
    )
    # Heavy DBs (Oracle XE) may report TCP open before PDB/APP_USER is ready.
    # Wait for compose healthcheck when the service defines one.
    healthy_timeout_raw = meta.get("healthy_timeout") or ""
    if healthy_timeout_raw.strip():
        try:
            healthy_timeout = int(healthy_timeout_raw)
        except ValueError:
            healthy_timeout = 0
        if healthy_timeout > 0:
            ok, detail = wait_for_container_healthy(
                container, healthy_timeout, dry_run=dry_run
            )
            if not ok:
                raise RuntimeError(
                    f"Waiting for healthy status on {container} failed after "
                    f"{healthy_timeout}s (db_type={db_type}): {detail}"
                )
    return started_by_matrix


def inspect_container_health(
    container: str,
    *,
    timeout: float = 30.0,
) -> Tuple[str, str]:
    """Return ``(health_status, container_status)`` from ``docker inspect``.

    * ``health_status`` — ``healthy`` / ``unhealthy`` / ``starting`` / ``none``
      when inspect succeeds; ``unknown`` when docker is missing or inspect fails.
    * ``container_status`` — e.g. ``running`` / ``exited`` / empty / ``unknown``.

    Pure side-effect: one ``docker inspect`` call. Unit-testable via
    ``subprocess.run`` mock (#2535).
    """
    if not container:
        return "unknown", "unknown"
    try:
        proc = subprocess.run(
            [
                "docker",
                "inspect",
                "--format",
                _DOCKER_HEALTH_INSPECT_FMT,
                container,
            ],
            shell=False,
            check=False,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
    except (OSError, subprocess.SubprocessError) as exc:
        LOG.debug("inspect_container_health %s: %s", container, exc)
        return "unknown", "unknown"
    if proc.returncode != 0:
        return "unknown", "unknown"
    raw = (proc.stdout or "").strip()
    health, _, status = raw.partition("|")
    health = health.strip().lower() or "unknown"
    status = status.strip().lower() or "unknown"
    return health, status


def wait_for_container_healthy(
    container: str,
    timeout_seconds: int,
    *,
    dry_run: bool,
    interval_seconds: float = 5.0,
    allow_no_healthcheck: bool = True,
) -> Tuple[bool, str]:
    """Wait until ``docker inspect`` reports Health.Status == healthy.

    Returns ``(ok, detail)`` where ``detail`` is a short machine-friendly token
    string for cell / RuntimeError messages (#2535).

    Policy:

    * ``healthy`` → success immediately.
    * ``unhealthy`` → **fail-fast** (do not wait remaining timeout). Docker has
      already decided the HEALTHCHECK failed (DB or CMS cell).
    * ``exited`` / ``dead`` / ``removing`` → fail-fast.
    * ``starting`` → keep polling until timeout.
    * No healthcheck (``none`` / empty) + running:
      - if ``allow_no_healthcheck`` (default, compose DB profiles without a
        healthcheck) → treat as ready;
      - else keep waiting / timeout (CMS cells require a real HEALTHCHECK).

    Host-side log scan is **not** done here — CMS cells use
    :func:`wait_for_http` with ``require_docker_health=True`` for the combined
    policy (Health.Status + host HTTP + ``rhythmyx_ready`` log scan).
    """
    if dry_run:
        LOG.info("DRY-RUN: wait for healthy %s (%ss)", container, timeout_seconds)
        return True, "dry-run"
    deadline = time.time() + timeout_seconds
    last_health = "unknown"
    last_status = "unknown"
    while time.time() < deadline:
        health, status = inspect_container_health(container)
        last_health, last_status = health, status
        if health == "healthy":
            LOG.info("Container %s is healthy", container)
            return True, f"healthy status={status}"
        if health == "unhealthy":
            detail = (
                f"{DETAIL_DOCKER_UNHEALTHY} health={health} status={status}"
            )
            LOG.error("Container %s is unhealthy — fail-fast (%s)", container, detail)
            return False, detail
        if status in ("exited", "dead", "removing"):
            detail = (
                f"{DETAIL_CONTAINER_NOT_RUNNING} status={status} health={health}"
            )
            LOG.error("Container %s is %s (health=%s)", container, status, health)
            return False, detail
        if allow_no_healthcheck and health in ("", "none") and status == "running":
            LOG.info(
                "Container %s is running (no healthcheck); treating as ready",
                container,
            )
            return True, f"no_healthcheck status={status}"
        LOG.info(
            "Waiting for %s healthy (health=%s status=%s)",
            container,
            health or "?",
            status or "?",
        )
        time.sleep(interval_seconds)
    detail = (
        f"{DETAIL_DOCKER_HEALTH_TIMEOUT} health={last_health} "
        f"status={last_status} timeout={timeout_seconds}s"
    )
    LOG.error(
        "Timed out waiting for healthy status on %s after %ss (%s)",
        container,
        timeout_seconds,
        detail,
    )
    return False, detail


def stop_external_dbs(
    compose_file: Path,
    db_types: Iterable[str],
    *,
    dry_run: bool,
    db_services: Mapping[str, Mapping[str, str]],
    env_file: Optional[Path] = None,
) -> None:
    """Stop external compose DB services (no volume wipe).

    Disconnects each container from ``perc-matrix-net`` best-effort, then
    ``docker compose … stop <service>`` (not ``down -v``).
    """
    for db_type in sorted(set(db_types)):
        meta = db_services.get(db_type) or {}
        profile = meta.get("profile") or ""
        service = meta.get("service") or ""
        if not profile or not service:
            continue
        container = db_container_name(service)
        LOG.info("Stopping matrix external DB %s (%s)", db_type, container)
        _run(
            [
                "docker",
                "network",
                "disconnect",
                MATRIX_NETWORK,
                container,
            ],
            dry_run=dry_run,
            check=False,
        )
        argv = [
            "docker",
            "compose",
            "-f",
            str(compose_file),
        ]
        if env_file is not None and env_file.is_file():
            argv.extend(["--env-file", str(env_file)])
        argv.extend(
            [
                "--profile",
                profile,
                "stop",
                service,
            ]
        )
        _run(argv, dry_run=dry_run, check=False)


def _docker_container_running(name: str) -> bool:
    """Return True if a Docker container with ``name`` is in running state."""
    try:
        completed = subprocess.run(
            [
                "docker",
                "inspect",
                "-f",
                "{{.State.Running}}",
                name,
            ],
            check=False,
            capture_output=True,
            text=True,
            timeout=30,
        )
    except (OSError, subprocess.TimeoutExpired):
        return False
    return completed.returncode == 0 and completed.stdout.strip().lower() == "true"


def destroy_container(name: str, *, dry_run: bool) -> None:
    _run(["docker", "rm", "-f", name], dry_run=dry_run, check=False)


def _docker_logs_tail(
    container_name: str,
    *,
    tail: int = 800,
    timeout: float = 30.0,
) -> str:
    """Return recent ``docker logs`` for context-failure scanning (#2462)."""
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
    out = completed.stdout or ""
    err = completed.stderr or ""
    if out and err:
        return out + "\n" + err
    return out or err


def wait_for_http(
    url: str,
    *,
    timeout_seconds: int,
    interval_seconds: float = 5.0,
    dry_run: bool,
    container_name: Optional[str] = None,
    require_docker_health: bool = False,
) -> Tuple[bool, str]:
    """Poll ``url`` until HTTP ready codes, or fail-fast on Rhythmyx context death.

    When ``container_name`` is set (CMS cells), each poll also scans recent
    ``docker logs`` for Spring/Jetty ApplicationContext failure markers.
    A dead Rhythmyx context fails the probe **immediately** even if Jetty
    still answers HTTP (#2462 residual of #2423).

    When ``require_docker_health`` is True (CMS matrix cells / qa-up path after
    #2481 — issue #2535), each poll also reads ``docker inspect`` Health.Status:

    * ``unhealthy`` → fail-fast with :data:`DETAIL_DOCKER_UNHEALTHY` (do not
      wait the full ``timeout_seconds``).
    * Success requires Health.Status ``healthy`` **and** HTTP ready **and**
      no context-failure markers (host log scan remains belt-and-braces).
    * ``starting`` / ``none`` / ``unknown`` → keep polling until timeout.

    DETAIL strings use stable tokens so agents / RESULT parsers can match
    ``docker_health_unhealthy`` and ``rhythmyx_context_failed`` (coordinate
    with perc-devctl ``HEALTH:`` column from #2537).
    """
    if dry_run:
        return True, "dry-run"
    deadline = time.time() + timeout_seconds
    last = ""
    last_health = "unknown"
    while time.time() < deadline:
        # Fail-fast: Jetty up + Spring context dead must not wait full timeout.
        if container_name:
            logs = _docker_logs_tail(container_name)
            match = find_rhythmyx_context_failure(logs)
            if match is not None:
                health_suffix = ""
                if require_docker_health:
                    last_health, _st = inspect_container_health(container_name)
                    health_suffix = f" health={last_health}"
                return (
                    False,
                    f"{DETAIL_CONTEXT_FAILED} match={match!r} "
                    f"last={last or 'n/a'}{health_suffix}",
                )
            if require_docker_health:
                last_health, status = inspect_container_health(container_name)
                if last_health == "unhealthy":
                    return (
                        False,
                        f"{DETAIL_DOCKER_UNHEALTHY} health={last_health} "
                        f"status={status} last={last or 'n/a'}",
                    )
                if status in ("exited", "dead", "removing"):
                    return (
                        False,
                        f"{DETAIL_CONTAINER_NOT_RUNNING} status={status} "
                        f"health={last_health} last={last or 'n/a'}",
                    )
        try:
            req = urllib.request.Request(url, method="GET")
            with urllib.request.urlopen(req, timeout=10) as resp:
                code = resp.getcode()
                if is_http_ready_code(code):
                    # Re-check logs after a "ready" HTTP so false positives fail.
                    if container_name:
                        logs = _docker_logs_tail(container_name)
                        match = find_rhythmyx_context_failure(logs)
                        if match is not None:
                            return (
                                False,
                                f"{DETAIL_CONTEXT_FAILED} match={match!r} "
                                f"http={code}",
                            )
                    if require_docker_health:
                        # Host HTTP ready is not enough — need inspect healthy.
                        if last_health != "healthy":
                            last = f"HTTP {code} health={last_health}"
                        else:
                            return True, f"HTTP {code} health={last_health}"
                    else:
                        return True, f"HTTP {code}"
                last = f"HTTP {code}"
        except urllib.error.HTTPError as exc:
            # Some login paths return 401/403 before auth — treat as up.
            if is_http_ready_code(exc.code):
                if container_name:
                    logs = _docker_logs_tail(container_name)
                    match = find_rhythmyx_context_failure(logs)
                    if match is not None:
                        return (
                            False,
                            f"{DETAIL_CONTEXT_FAILED} match={match!r} "
                            f"http={exc.code}",
                        )
                if require_docker_health:
                    if last_health != "healthy":
                        last = f"HTTP {exc.code} health={last_health}"
                    else:
                        return True, f"HTTP {exc.code} health={last_health}"
                else:
                    return True, f"HTTP {exc.code}"
            last = f"HTTPError {exc.code}"
        except (urllib.error.URLError, TimeoutError, OSError) as exc:
            last = str(exc)
        time.sleep(interval_seconds)
    # Final log scan so timeout DETAIL prefers context failure when present.
    if container_name:
        logs = _docker_logs_tail(container_name)
        match = find_rhythmyx_context_failure(logs)
        if match is not None:
            health_suffix = f" health={last_health}" if require_docker_health else ""
            return (
                False,
                f"{DETAIL_CONTEXT_FAILED} match={match!r} "
                f"last={last or 'timeout'}{health_suffix}",
            )
        if require_docker_health:
            last_health, status = inspect_container_health(container_name)
            if last_health == "unhealthy":
                return (
                    False,
                    f"{DETAIL_DOCKER_UNHEALTHY} health={last_health} "
                    f"status={status} last={last or 'timeout'}",
                )
            return (
                False,
                f"{DETAIL_DOCKER_HEALTH_TIMEOUT} health={last_health} "
                f"status={status} last={last or 'timeout'}",
            )
    return False, last or "timeout"


def cell_container_name(cell: CellSpec) -> str:
    return f"perc-matrix-{cell.cell_id}"


def run_cell(
    cell: CellSpec,
    *,
    repo_root: Path,
    compose_file: Path,
    keep: bool,
    probe_timeout: int,
    dry_run: bool,
    log_dir: Path,
    db_services: Mapping[str, Mapping[str, str]],
    env_file: Optional[Path] = None,
    started_dbs: Optional[Set[str]] = None,
    engaged_dbs: Optional[Set[str]] = None,
) -> CellResult:
    started = time.time()
    name = cell_container_name(cell)
    # Env override (QA_CMS_HOST_PORT / CMS_HOST_PORT / DTS_HOST_PORT) or freeport
    # so multi-worktree agents do not collide on preferred 9993/9983 (#2005).
    host_port = ensure_matrix_host_port(cell.product)
    probe_url = build_probe_url(cell.product, host_port)
    LOG.info(
        "Cell %s host publish %s → probe %s",
        cell.cell_id,
        host_port,
        probe_url,
    )
    cell_log = log_dir / f"matrix-{cell.cell_id}-{_ts()}.log"
    db_meta = dict(db_services[cell.db_type])

    try:
        jar = resolve_installer_jar(repo_root, cell.product)
    except FileNotFoundError as exc:
        return CellResult(
            cell_id=cell.cell_id,
            product=cell.product,
            db_type=cell.db_type,
            status="fail",
            detail=str(exc),
            duration_seconds=time.time() - started,
            log_path=str(cell_log),
        )

    LOG.info("Cell %s using jar %s (%s bytes)", cell.cell_id, jar, jar.stat().st_size if jar.is_file() else 0)

    destroy_container(name, dry_run=dry_run)
    try:
        ownership = start_db(
            repo_root,
            compose_file,
            cell.db_type,
            dry_run=dry_run,
            db_services=db_services,
            env_file=env_file,
        )
    except RuntimeError as exc:
        return CellResult(
            cell_id=cell.cell_id,
            product=cell.product,
            db_type=cell.db_type,
            status="fail",
            container_name=name,
            probe_url=probe_url,
            detail=str(exc),
            duration_seconds=time.time() - started,
            log_path=str(cell_log),
        )
    if ownership is not None:
        if engaged_dbs is not None:
            engaged_dbs.add(cell.db_type)
        if ownership and started_dbs is not None:
            started_dbs.add(cell.db_type)

    run_argv = build_docker_run_argv(
        image=MATRIX_IMAGE_TAG,
        container_name=name,
        product=cell.product,
        db_type=cell.db_type,
        installer_jar_host=jar,
        host_port=host_port,
        network=MATRIX_NETWORK,
        db_meta=db_meta,
        keep=keep,
    )
    try:
        _run(run_argv, dry_run=dry_run, check=not dry_run)
    except subprocess.CalledProcessError as exc:
        return CellResult(
            cell_id=cell.cell_id,
            product=cell.product,
            db_type=cell.db_type,
            status="fail",
            container_name=name,
            probe_url=probe_url,
            detail=f"docker run failed: {exc}",
            duration_seconds=time.time() - started,
            log_path=str(cell_log),
        )

    # Capture container logs while waiting. CMS cells pass container_name so
    # a dead Rhythmyx ApplicationContext fails the probe even if Jetty HTTP
    # still answers (#2462 / #2423). After #2481 HEALTHCHECK, CMS cells also
    # require docker Health.Status=healthy and fail-fast when already
    # unhealthy (#2535). Host log scan remains belt-and-braces.
    is_cms = cell.product == "cms"
    ok, detail = wait_for_http(
        probe_url,
        timeout_seconds=probe_timeout,
        dry_run=dry_run,
        container_name=name if is_cms else None,
        require_docker_health=is_cms,
    )
    if not dry_run:
        logs = _run(
            ["docker", "logs", name],
            dry_run=False,
            capture=True,
            check=False,
        )
        cell_log.write_text(
            (logs.stdout or "") + "\n" + (logs.stderr or ""),
            encoding="utf-8",
            errors="replace",
        )

    if not ok:
        if not keep:
            destroy_container(name, dry_run=dry_run)
        return CellResult(
            cell_id=cell.cell_id,
            product=cell.product,
            db_type=cell.db_type,
            status="fail",
            container_name=name,
            probe_url=probe_url,
            detail=f"probe failed: {detail}",
            duration_seconds=time.time() - started,
            log_path=str(cell_log),
        )

    if not keep:
        destroy_container(name, dry_run=dry_run)

    return CellResult(
        cell_id=cell.cell_id,
        product=cell.product,
        db_type=cell.db_type,
        status="pass",
        container_name=name if keep else "",
        probe_url=probe_url,
        detail=detail,
        duration_seconds=time.time() - started,
        log_path=str(cell_log),
    )


def _ts() -> str:
    return datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")


def _build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="matrix-install-smoke.py",
        description="Ephemeral CMS/DTS install matrix smoke (Layer 1).",
    )
    p.add_argument(
        "--repo-root",
        type=Path,
        default=None,
        help="Repository root (default: discovered from script location).",
    )
    p.add_argument(
        "--product",
        default="cms",
        help="Comma list: cms,dts (default: cms)",
    )
    p.add_argument(
        "--db",
        default="h2",
        help="Comma list: h2,postgresql,mysql,sqlserver,oracle (default: h2)",
    )
    p.add_argument(
        "--compose-file",
        type=Path,
        default=None,
        help="docker-compose.yml path (default: <repo>/docker-compose.yml)",
    )
    p.add_argument(
        "--env-file",
        type=Path,
        default=None,
        help=(
            "Env file for compose DB credentials (default: <repo>/.env.compose, "
            "fallback .env.compose.example). Passwords must not be committed."
        ),
    )
    p.add_argument(
        "--keep",
        action="store_true",
        help=(
            "Leave cell containers running for Playwright Layer 2; "
            "also leaves external DBs up (implies no DB teardown)"
        ),
    )
    db_life = p.add_mutually_exclusive_group()
    db_life.add_argument(
        "--keep-db",
        action="store_true",
        help=(
            "Leave external DB containers running after the matrix finishes "
            "(cells still destroyed unless --keep). Opt-in to previous default."
        ),
    )
    db_life.add_argument(
        "--stop-db",
        action="store_true",
        help=(
            "Stop every external DB used by this matrix, including containers "
            "that were already running before the run (destructive; no volume wipe)"
        ),
    )
    p.add_argument(
        "--probe-timeout",
        type=int,
        default=900,
        help="Seconds to wait for login/health HTTP (default: 900)",
    )
    p.add_argument(
        "--skip-image-build",
        action="store_true",
        help="Do not docker build the matrix cell image",
    )
    p.add_argument(
        "--skip-preflight",
        action="store_true",
        help=(
            "Skip CMS rebuild-chain preflight (WebUI WAR vs m2 sitemanage). "
            "Default is strict: refuse CMS cells when STALE (#2531 / #2486)."
        ),
    )
    p.add_argument(
        "--m2-root",
        type=Path,
        default=None,
        help=(
            "Maven local repository root for CMS preflight "
            "(default: ~/.m2/repository). Intended for tests / CI overrides."
        ),
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help="Print planned docker/compose commands without executing",
    )
    return p


def main(argv: Optional[Sequence[str]] = None) -> int:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
        stream=sys.stderr,
    )
    args = _build_parser().parse_args(argv)
    script_path = Path(__file__).resolve()
    repo_root = (args.repo_root or repo_root_from(script_path)).resolve()
    compose_file = (args.compose_file or (repo_root / DEFAULT_COMPOSE_FILE)).resolve()
    env_file = resolve_env_file(repo_root, args.env_file)
    file_env = load_env_file(env_file)
    # Process env overrides file values for passwords/users.
    merged_env: Dict[str, str] = dict(file_env)
    merged_env.update({k: v for k, v in os.environ.items() if v})
    db_services = build_db_services(merged_env)
    # Keep module snapshot in sync for any helper still reading DB_SERVICES.
    DB_SERVICES.clear()
    DB_SERVICES.update(db_services)
    log_dir = repo_root / "docker" / "logs"
    log_dir.mkdir(parents=True, exist_ok=True)

    try:
        products = parse_csv(args.product, PRODUCTS, "product")
        dbs = parse_csv(args.db, DB_TYPES, "db")
        require_db_passwords(db_services, dbs)
    except ValueError as exc:
        LOG.error("%s", exc)
        print(f"RESULT:FAIL STEP:matrix LOG:")
        return EXIT_INVOCATION

    LOG.info("Using compose env file %s", env_file)
    cells = expand_matrix(products, dbs)

    # CMS rebuild-chain preflight (#2531): refuse before install/start when the
    # WebUI WAR is older than m2 sitemanage (or WAR missing while m2 present).
    # DTS-only matrices skip this gate. Operators who intentionally bypass use
    # --skip-preflight (e.g. known-stale debug; not recommended for CI).
    if "cms" in products:
        if args.skip_preflight:
            LOG.warning(
                "Skipping CMS rebuild-chain preflight (--skip-preflight); "
                "cells may install a stale WebUI WAR"
            )
            print("PREFLIGHT: skipped (--skip-preflight)")
        else:
            m2_root = (
                args.m2_root.resolve()
                if args.m2_root is not None
                else None
            )
            preflight_ok, preflight_report = check_cms_rebuild_preflight(
                repo_root, m2_root
            )
            print(preflight_report)
            if not preflight_ok:
                LOG.error(
                    "CMS rebuild-chain preflight STALE — refusing matrix "
                    "before install/start (use --skip-preflight to override)"
                )
                print(
                    f"RESULT:FAIL STEP:matrix-preflight "
                    f"DETAIL:{DETAIL_PREFLIGHT_STALE}"
                )
                return EXIT_CELL_FAILED

    # Pin compose DB host publishes once for the whole matrix so concurrent
    # worktrees get a stable freeport set before any compose up (#2004).
    db_host_ports = ensure_compose_db_host_ports(dbs)
    for db_type, port in sorted(db_host_ports.items()):
        env_key = COMPOSE_DB_HOST_PORT_SPEC[db_type][0]
        print(f"{env_key}={port}")
    # Engaged = external DBs we actually start_db()'d for a cell (not merely selected).
    engaged_external: Set[str] = set()
    started_by_matrix: Set[str] = set()
    report = MatrixReport(started_at=datetime.now(timezone.utc).isoformat())
    exit_code = EXIT_OK

    if not args.dry_run and shutil.which("docker") is None:
        LOG.error("docker not found on PATH")
        print("RESULT:FAIL STEP:matrix LOG:")
        return EXIT_INVOCATION

    try:
        ensure_network(MATRIX_NETWORK, dry_run=args.dry_run)
        if not args.skip_image_build:
            try:
                build_matrix_image(repo_root, dry_run=args.dry_run)
            except subprocess.CalledProcessError as exc:
                LOG.error("matrix image build failed: %s", exc)
                print("RESULT:FAIL STEP:matrix-image LOG:")
                exit_code = EXIT_CELL_FAILED
                return exit_code

        for cell in cells:
            LOG.info("=== matrix cell %s ===", cell.cell_id)
            result = run_cell(
                cell,
                repo_root=repo_root,
                compose_file=compose_file,
                keep=args.keep,
                probe_timeout=args.probe_timeout,
                dry_run=args.dry_run,
                log_dir=log_dir,
                db_services=db_services,
                env_file=env_file,
                started_dbs=started_by_matrix,
                engaged_dbs=engaged_external,
            )
            report.results.append(result)
            LOG.info(
                "Cell %s → %s (%0.1fs) %s",
                result.cell_id,
                result.status,
                result.duration_seconds,
                result.detail,
            )

        report.finished_at = datetime.now(timezone.utc).isoformat()
        report_path = log_dir / f"matrix-results-{_ts()}.json"
        report_path.write_text(
            json.dumps(report.to_dict(), indent=2) + "\n", encoding="utf-8"
        )

        failed = [r for r in report.results if r.status == "fail"]
        if failed:
            print(f"RESULT:FAIL STEP:matrix LOG:{report_path}")
            exit_code = EXIT_CELL_FAILED
        else:
            print(f"RESULT:OK STEP:matrix LOG:{report_path}")
            exit_code = EXIT_OK
        return exit_code
    finally:
        # Always attempt DB teardown after cells (pass or fail), unless opted out.
        # Only consider DBs this run actually engaged (start_db called).
        to_stop = select_dbs_to_stop(
            started_by_matrix=started_by_matrix,
            used_external=engaged_external,
            keep=args.keep,
            keep_db=args.keep_db,
            stop_db=args.stop_db,
        )
        if to_stop:
            LOG.info(
                "Tearing down external DBs: %s",
                ", ".join(sorted(to_stop)),
            )
            stop_external_dbs(
                compose_file,
                to_stop,
                dry_run=args.dry_run,
                db_services=db_services,
                env_file=env_file,
            )
        elif engaged_external and (args.keep or args.keep_db):
            LOG.info(
                "Leaving external DBs running (%s)",
                "--keep" if args.keep else "--keep-db",
            )
        elif engaged_external and not started_by_matrix and not args.stop_db:
            LOG.info(
                "Leaving pre-existing external DBs running (not started by this matrix run)"
            )


if __name__ == "__main__":
    sys.exit(main())
