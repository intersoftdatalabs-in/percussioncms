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
  4. Probe login / health URL from the host
  5. Record JSON + RESULT line under ``docker/logs/``
  6. Destroy the cell unless ``--keep``
  7. Stop external DBs this process started (unless ``--keep`` / ``--keep-db``)

DB lifecycle (#1516)
--------------------
External compose DBs (``percussion-postgres`` / ``-mysql`` / ``-sqlserver``) are
started only when not already running. After the matrix report is written:

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

    # Both products × H2 and PostgreSQL
    python3 docker/scripts/matrix-install-smoke.py --product cms,dts --db h2,postgresql

    # Leave stack up for Playwright Layer 2
    python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql --keep

    # Destroy cells but reuse DBs for the next matrix run
    python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql --keep-db

    # Dry-run (no docker/mvn)
    python3 docker/scripts/matrix-install-smoke.py --product cms --db h2 --dry-run

Exit codes
----------
0  all selected cells passed
1  invocation / config error
2  one or more cells failed
"""

from __future__ import annotations

import argparse
import json
import logging
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Set, Tuple

LOG = logging.getLogger("matrix-install-smoke")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_CELL_FAILED = 2

DEFAULT_COMPOSE_FILE = "docker-compose.yml"
MATRIX_IMAGE_TAG = "percussion-matrix-cell:local"
MATRIX_NETWORK = "perc-matrix-net"

# Host ports published for single-cell sequential runs (one CMS, one DTS at a time).
CMS_HOST_PORT = 9993
DTS_HOST_PORT = 9983
CMS_PROBE_PATH = "/Rhythmyx/login"
DTS_PROBE_PATH = "/"

# DB service names + compose profiles already in docker-compose.yml
DB_SERVICES: Dict[str, Dict[str, str]] = {
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
        "password": "PercPass123",
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
        "password": "PercPass123",
        "name": "percdb",
    },
    "sqlserver": {
        "profile": "sqlserver",
        "service": "sqlserver",
        "host": "localhost",
        "port": "1433",
        "container_host": "sqlserver",
        "user": "sa",
        "password": "PercPass123!",
        "name": "percdb",
    },
}

# Compose service name → stable container_name from docker-compose.yml
CONTAINER_BY_SERVICE: Dict[str, str] = {
    "postgres": "percussion-postgres",
    "mysql": "percussion-mysql",
    "sqlserver": "percussion-sqlserver",
}

PRODUCTS = ("cms", "dts")
DB_TYPES = tuple(DB_SERVICES.keys())


def db_container_name(service: str) -> str:
    """Return the Docker container name for a compose DB service."""
    return CONTAINER_BY_SERVICE.get(service, f"percussion-{service}")


def external_db_types(db_types: Iterable[str]) -> Set[str]:
    """Return matrix DB keys that use an external compose service (not H2)."""
    out: Set[str] = set()
    for db_type in db_types:
        meta = DB_SERVICES.get(db_type) or {}
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
            "cd modules/perc-distribution-tree && ../../mvn-env.sh package "
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
            f"&& ../../../../mvn-env.sh package (produces target/{DTS_INSTALLER_JAR_NAME})"
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
    """Build from ``docker/matrix/`` context only (small; not the monorepo root)."""
    matrix_dir = repo_root / "docker" / "matrix"
    dockerfile = matrix_dir / "Dockerfile"
    _run(
        [
            "docker",
            "build",
            "-t",
            MATRIX_IMAGE_TAG,
            "-f",
            str(dockerfile),
            str(matrix_dir),
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
    meta = DB_SERVICES[db_type]
    profile = meta.get("profile") or ""
    service = meta.get("service") or ""
    if not profile or not service:
        return None
    # Attach DB container to matrix network with a DNS alias matching the
    # compose service name (e.g. "postgres") so cells can use DB_HOST=postgres.
    container = db_container_name(service)
    started_by_matrix = False

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
            "--profile",
            profile,
            "up",
            "-d",
            service,
        ]
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
    return started_by_matrix


def stop_external_dbs(
    compose_file: Path,
    db_types: Iterable[str],
    *,
    dry_run: bool,
) -> None:
    """Stop external compose DB services (no volume wipe).

    Disconnects each container from ``perc-matrix-net`` best-effort, then
    ``docker compose … stop <service>`` (not ``down -v``).
    """
    for db_type in sorted(set(db_types)):
        meta = DB_SERVICES.get(db_type) or {}
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
        _run(
            [
                "docker",
                "compose",
                "-f",
                str(compose_file),
                "--profile",
                profile,
                "stop",
                service,
            ],
            dry_run=dry_run,
            check=False,
        )


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


def wait_for_http(
    url: str,
    *,
    timeout_seconds: int,
    interval_seconds: float = 5.0,
    dry_run: bool,
) -> Tuple[bool, str]:
    if dry_run:
        return True, "dry-run"
    deadline = time.time() + timeout_seconds
    last = ""
    while time.time() < deadline:
        try:
            req = urllib.request.Request(url, method="GET")
            with urllib.request.urlopen(req, timeout=10) as resp:
                code = resp.getcode()
                if code in (200, 302, 401, 403):
                    return True, f"HTTP {code}"
                last = f"HTTP {code}"
        except urllib.error.HTTPError as exc:
            # Some login paths return 401/403 before auth — treat as up.
            if exc.code in (200, 302, 401, 403):
                return True, f"HTTP {exc.code}"
            last = f"HTTPError {exc.code}"
        except (urllib.error.URLError, TimeoutError, OSError) as exc:
            last = str(exc)
        time.sleep(interval_seconds)
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
    started_dbs: Optional[Set[str]] = None,
    engaged_dbs: Optional[Set[str]] = None,
) -> CellResult:
    started = time.time()
    name = cell_container_name(cell)
    host_port = CMS_HOST_PORT if cell.product == "cms" else DTS_HOST_PORT
    probe_url = build_probe_url(cell.product, host_port)
    cell_log = log_dir / f"matrix-{cell.cell_id}-{_ts()}.log"
    db_meta = DB_SERVICES[cell.db_type]

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
    ownership = start_db(repo_root, compose_file, cell.db_type, dry_run=dry_run)
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

    # Capture container logs while waiting
    ok, detail = wait_for_http(
        probe_url,
        timeout_seconds=probe_timeout,
        dry_run=dry_run,
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
        help="Comma list: h2,postgresql,mysql,sqlserver (default: h2)",
    )
    p.add_argument(
        "--compose-file",
        type=Path,
        default=None,
        help="docker-compose.yml path (default: <repo>/docker-compose.yml)",
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
    log_dir = repo_root / "docker" / "logs"
    log_dir.mkdir(parents=True, exist_ok=True)

    try:
        products = parse_csv(args.product, PRODUCTS, "product")
        dbs = parse_csv(args.db, DB_TYPES, "db")
    except ValueError as exc:
        LOG.error("%s", exc)
        print(f"RESULT:FAIL STEP:matrix LOG:")
        return EXIT_INVOCATION

    cells = expand_matrix(products, dbs)
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
            stop_external_dbs(compose_file, to_stop, dry_run=args.dry_run)
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
