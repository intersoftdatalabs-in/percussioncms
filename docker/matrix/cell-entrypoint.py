#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""In-container matrix cell: silent install of CMS or DTS, then start service.

Designed for ephemeral ``docker run --rm`` cells driven by
``docker/scripts/matrix-install-smoke.py``.

Env / flags
-----------
PRODUCT          cms | dts
INSTALLER_JAR    absolute path to the installer jar inside the container
INSTALL_ROOT     install directory (default: /opt/Percussion)
DB_TYPE          h2 | postgresql | mysql | sqlserver | oracle
DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, DB_SCHEMA
SILENT           true (default) → pass --silent --no-tty
KEEP_ALIVE       true (default) → stream logs after start so the container stays up

Exit codes
----------
0  install + start succeeded (process stays alive when KEEP_ALIVE)
1  usage / config error
2  install failed
3  start failed
"""

from __future__ import annotations

import argparse
import logging
import os
import subprocess
import sys
import time
from pathlib import Path
from typing import List, Optional

LOG = logging.getLogger("matrix-cell")

EXIT_OK = 0
EXIT_USAGE = 1
EXIT_INSTALL = 2
EXIT_START = 3

DEFAULT_INSTALL_ROOT = "/opt/Percussion"
CMS_START = Path("jetty") / "StartJetty.sh"
DTS_START_PRIMARY = "TomcatStartup.sh"
DTS_START_FALLBACK = "startup.sh"


def _env_bool(name: str, default: bool = False) -> bool:
    raw = os.environ.get(name)
    if raw is None or raw == "":
        return default
    return raw.strip().lower() in ("1", "true", "yes", "y", "on")


def _build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="Matrix cell: install + start CMS or DTS")
    p.add_argument(
        "--product",
        choices=("cms", "dts"),
        default=os.environ.get("PRODUCT", "cms"),
    )
    p.add_argument(
        "--installer-jar",
        default=os.environ.get("INSTALLER_JAR", ""),
        help="Path to perc-distribution-tree.jar or delivery-tier-distribution.jar",
    )
    p.add_argument(
        "--install-root",
        default=os.environ.get("INSTALL_ROOT", DEFAULT_INSTALL_ROOT),
    )
    p.add_argument("--db-type", default=os.environ.get("DB_TYPE", "h2"))
    p.add_argument("--db-host", default=os.environ.get("DB_HOST", ""))
    p.add_argument("--db-port", default=os.environ.get("DB_PORT", ""))
    p.add_argument("--db-name", default=os.environ.get("DB_NAME", "percdb"))
    p.add_argument("--db-user", default=os.environ.get("DB_USER", "percuser"))
    p.add_argument("--db-password", default=os.environ.get("DB_PASSWORD", "PercPass123"))
    p.add_argument("--db-schema", default=os.environ.get("DB_SCHEMA", ""))
    p.add_argument(
        "--silent",
        action=argparse.BooleanOptionalAction,
        default=_env_bool("SILENT", True),
    )
    p.add_argument(
        "--keep-alive",
        action=argparse.BooleanOptionalAction,
        default=_env_bool("KEEP_ALIVE", True),
    )
    p.add_argument(
        "--wait-db-seconds",
        type=int,
        default=int(os.environ.get("WAIT_DB_SECONDS", "120")),
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help="Print install/start commands without executing them",
    )
    return p


def build_install_argv(
    *,
    java: str,
    installer_jar: Path,
    install_root: Path,
    product: str,  # noqa: ARG001 — reserved for product-specific flags later
    db_type: str,
    db_host: str,
    db_port: str,
    db_name: str,
    db_user: str,
    db_password: str,
    db_schema: str,
    silent: bool,
) -> List[str]:
    """Build ``java -jar <installer> <installRoot> --db.* ...`` argv.

    Pure function for unit tests.
    """
    argv: List[str] = [java, "-jar", str(installer_jar), str(install_root)]
    if silent:
        argv.extend(["--silent", "--no-tty"])
    argv.append(f"--db.type={db_type}")
    # Embedded defaults need no host/credentials.
    if db_type.lower() not in ("h2", "derby"):
        if db_host:
            argv.append(f"--db.host={db_host}")
        if db_port:
            argv.append(f"--db.port={db_port}")
        if db_name:
            argv.append(f"--db.name={db_name}")
        if db_user:
            argv.append(f"--db.user={db_user}")
        if db_password:
            argv.append(f"--db.password={db_password}")
        if db_schema:
            argv.append(f"--db.schema={db_schema}")
    return argv


def wait_for_db(
    db_type: str,
    host: str,
    port: str,
    user: str,
    password: str,
    name: str,
    timeout_seconds: int,
    *,
    dry_run: bool,
) -> bool:
    """Best-effort wait until the external DB accepts connections."""
    db_type = db_type.lower()
    if db_type in ("h2", "derby"):
        return True
    if dry_run:
        LOG.info("DRY-RUN: wait for db type=%s host=%s port=%s", db_type, host, port)
        return True
    if not host:
        LOG.error("DB_HOST required for db.type=%s", db_type)
        return False

    deadline = time.time() + timeout_seconds
    attempt = 0
    while time.time() < deadline:
        attempt += 1
        try:
            if db_type == "postgresql":
                rc = subprocess.run(
                    ["pg_isready", "-h", host, "-p", port or "5432", "-U", user or "percuser"],
                    shell=False,
                    check=False,
                    capture_output=True,
                    text=True,
                ).returncode
                if rc == 0:
                    LOG.info("PostgreSQL ready at %s:%s", host, port)
                    return True
            elif db_type == "mysql":
                rc = subprocess.run(
                    [
                        "mysqladmin",
                        "ping",
                        f"-h{host}",
                        f"-P{port or '3306'}",
                        f"-u{user or 'percuser'}",
                        f"-p{password or ''}",
                        "--silent",
                    ],
                    shell=False,
                    check=False,
                    capture_output=True,
                    text=True,
                ).returncode
                if rc == 0:
                    LOG.info("MySQL ready at %s:%s", host, port)
                    return True
            else:
                # SQL Server / Oracle: simple TCP connect via Python
                import socket

                sock = socket.create_connection(
                    (host, int(port or ("1433" if db_type == "sqlserver" else "1521"))),
                    timeout=3,
                )
                sock.close()
                LOG.info("%s TCP ready at %s:%s", db_type, host, port)
                return True
        except OSError as exc:
            LOG.debug("wait_for_db attempt %s: %s", attempt, exc)
        time.sleep(2)
    LOG.error("Timed out waiting for %s at %s:%s", db_type, host, port)
    return False


def run_install(argv: List[str], *, dry_run: bool, timeout: int = 1800) -> int:
    LOG.info("Install command: %s", " ".join(argv))
    if dry_run:
        return EXIT_OK
    completed = subprocess.run(argv, shell=False, check=False, timeout=timeout)
    if completed.returncode != 0:
        LOG.error("Installer exited %s", completed.returncode)
        return EXIT_INSTALL
    return EXIT_OK


def start_product(product: str, install_root: Path, *, dry_run: bool) -> int:
    if product == "cms":
        start = install_root / CMS_START
        cwd = install_root / "jetty"
    else:
        primary = install_root / DTS_START_PRIMARY
        fallback = install_root / DTS_START_FALLBACK
        if primary.is_file():
            start = primary
        elif fallback.is_file():
            start = fallback
        else:
            start = primary
        cwd = install_root

    if dry_run:
        LOG.info("DRY-RUN: start %s (cwd=%s)", start, cwd)
        return EXIT_OK

    if not start.is_file():
        LOG.error("Start script missing: %s", start)
        return EXIT_START

    # Ensure executable bit (jar extraction may not preserve it on all FS).
    try:
        start.chmod(start.stat().st_mode | 0o111)
    except OSError as exc:
        LOG.warning("Could not chmod %s: %s", start, exc)

    LOG.info("Starting %s via %s", product, start)
    completed = subprocess.run(
        [str(start)],
        cwd=str(cwd),
        shell=False,
        check=False,
    )
    if completed.returncode != 0:
        LOG.error("Start script exited %s", completed.returncode)
        return EXIT_START
    return EXIT_OK


def keep_alive(install_root: Path, *, dry_run: bool) -> int:
    if dry_run:
        LOG.info("DRY-RUN: keep-alive tail")
        return EXIT_OK
    log_dirs = [
        install_root / "jetty" / "base" / "logs",
        install_root / "Deployment" / "Server" / "logs",
    ]
    logs: List[str] = []
    for d in log_dirs:
        if d.is_dir():
            logs.extend(str(p) for p in sorted(d.glob("*.log")))
    if logs:
        os.execvp("tail", ["tail", "-F", *logs])
    os.execvp("tail", ["tail", "-f", "/dev/null"])
    return EXIT_OK  # pragma: no cover


def main(argv: Optional[List[str]] = None) -> int:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
        stream=sys.stderr,
    )
    args = _build_parser().parse_args(argv)

    if not args.installer_jar:
        LOG.error("--installer-jar / INSTALLER_JAR is required")
        return EXIT_USAGE
    installer = Path(args.installer_jar)
    if not args.dry_run and not installer.is_file():
        LOG.error("Installer jar not found: %s", installer)
        return EXIT_USAGE

    install_root = Path(args.install_root)
    install_root.mkdir(parents=True, exist_ok=True)

    if not wait_for_db(
        args.db_type,
        args.db_host,
        args.db_port,
        args.db_user,
        args.db_password,
        args.db_name,
        args.wait_db_seconds,
        dry_run=args.dry_run,
    ):
        return EXIT_USAGE

    java = os.environ.get("JAVA_HOME", "")
    java_bin = str(Path(java) / "bin" / "java") if java else "java"
    if java and not Path(java_bin).is_file():
        java_bin = "java"

    install_argv = build_install_argv(
        java=java_bin,
        installer_jar=installer,
        install_root=install_root,
        product=args.product,
        db_type=args.db_type,
        db_host=args.db_host,
        db_port=args.db_port,
        db_name=args.db_name,
        db_user=args.db_user,
        db_password=args.db_password,
        db_schema=args.db_schema,
        silent=args.silent,
    )
    rc = run_install(install_argv, dry_run=args.dry_run)
    if rc != EXIT_OK:
        return rc

    rc = start_product(args.product, install_root, dry_run=args.dry_run)
    if rc != EXIT_OK:
        return rc

    print(
        f"RESULT:OK STEP:matrix-cell PRODUCT:{args.product} "
        f"DB:{args.db_type} INSTALL_ROOT:{install_root}",
        flush=True,
    )

    if args.keep_alive:
        return keep_alive(install_root, dry_run=args.dry_run)
    return EXIT_OK


if __name__ == "__main__":
    sys.exit(main())
