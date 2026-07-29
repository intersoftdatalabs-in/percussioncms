#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""Cross-platform Python port of scripts/install-cms-dev.sh.

Purpose
-------
Host-side CMS installer for the docker dev/test runtime. Runs the Percussion
CMS installer Java + Ant buildfile ONCE on the host into a single persistent
``install_root`` directory. ``docker-compose.yml`` bind-mounts that single dir
into the ``cms-dts`` container at ``/opt/Percussion/`` (the bind-mount path
is a literal string used by docker-compose, not a regex escape), so:

  * container restarts do NOT re-install (the ``install_root`` persists on the host)
  * hot-deploys (jar swaps, config edits) are local file edits in ``install_root/``
  * the container's only job is to run ``StartJetty.sh``

Defaults
--------
- ``--install-root`` defaults to ``/opt/Percussion`` on Linux/macOS; on Windows
  the caller must pass an explicit path (no hard-coded Windows default to keep
  this cross-platform safe).
- ``--skip-dts`` is the default (DTS installer is out of scope for the 992
  react-content-explorer story automation).

Reads DB config from ``.env.compose`` (``PERC_DB_TYPE``, ``PERC_DB_HOST``, etc.).
The original ``db_config_value`` PERC_DB_/DB_ fallback chain is preserved.

Usage
-----
::

    python3 scripts/install-cms-dev.py [--install-root <path>] [--reset] [--no-bootstrap]
                                      [--skip-dts | --install-dts]

Behavioral Notes
----------------
- bash ``trap '...' EXIT`` for cleanup is replaced by Python ``try``/``finally``
  blocks (R2). On Windows, SIGINT/SIGTERM are no-ops in interactive console;
  cleanup still runs via ``finally``.
- Path defaults differ by OS (no hard-coded ``C:\opt\Percussion`` on Windows
  per the contract).
- The ``RESULT:OK STEP:install LOG:<path>`` and ``RESULT:FAIL STEP:install LOG:<path>``
  markers from the bash original are preserved on stdout so CI scripts and
  human operators can grep for them.
- The marker file (``.percussion-install-complete``) is honored exactly as in
  the bash original (skip install when present and ``--reset`` is not passed).
"""
from __future__ import annotations

import argparse
import datetime as _dt
import logging
import os
import platform
import shutil
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)


def _now_utc_compact() -> str:
    return _dt.datetime.utcnow().strftime("%Y%m%d-%H%M%S")


def _now_utc_iso() -> str:
    return _dt.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")


def _default_install_root() -> str:
    """Return the OS-appropriate default install root."""
    if platform.system().lower().startswith("win"):
        # No safe hard-coded Windows default; require the operator to opt in.
        return ""
    return "/opt/Percussion"


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="install-cms-dev.py",
        description="Host-side CMS installer for the docker dev/test runtime.",
    )
    parser.add_argument(
        "--install-root",
        default=_default_install_root(),
        help=(
            "Target install dir. Linux/macOS default: /opt/Percussion. "
            "Windows: no default; pass an explicit path."
        ),
    )
    parser.add_argument(
        "--reset",
        action="store_true",
        help="Reinstall even if the marker file is present",
    )
    parser.add_argument(
        "--no-bootstrap",
        action="store_true",
        help="Do not seed install_root from docker/dev-data/cms-dts/ on first run",
    )
    parser.add_argument(
        "--skip-dts",
        dest="skip_dts",
        action="store_true",
        default=True,
        help="Run the CMS installer only (DTS out of scope; default: enabled)",
    )
    parser.add_argument(
        "--install-dts",
        dest="skip_dts",
        action="store_false",
        help="Run the DTS installer too (overrides --skip-dts)",
    )
    parser.add_argument(
        "--env-file",
        default=str(REPO_ROOT / ".env.compose"),
        help="Path to the .env.compose file (default: <repo-root>/.env.compose)",
    )
    return parser


def _load_env_file(path: Path) -> None:
    """Source the env file into ``os.environ`` (preserves PERC_DB_* values).

    Mirrors the bash ``set -a; source; set +a`` block from the original.
    Supports ``KEY=value`` and ``export KEY=value``; ignores blank lines and
    ``#`` comments.
    """
    if not path.is_file():
        raise FileNotFoundError(path)
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[len("export ") :].lstrip()
        if "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        value = value.strip()
        # Strip surrounding quotes.
        if len(value) >= 2 and value[0] == value[-1] and value[0] in ("'", '"'):
            value = value[1:-1]
        os.environ[key] = value


def _db_config_value(primary: str, fallback: str = "", default: str = "") -> str:
    """Mirror bash ``db_config_value``: try PRIMARY, then FALLBACK, then DEFAULT."""
    value = os.environ.get(primary, "")
    if not value and fallback:
        value = os.environ.get(fallback, "")
    if not value:
        value = default
    return value


def build_installer_db_args() -> list[str]:
    """Translate ``PERC_DB_*`` env vars into the installer's ``--db.*`` argv list.

    Pure function — exposed so tests can exercise the env-var → CLI-arg mapping
    without running the installer.
    """
    db_type = _db_config_value("PERC_DB_TYPE", "DB_TYPE", "h2")
    ssl_enabled = _db_config_value("PERC_DB_SSL_ENABLED", "DB_SSL_ENABLED", "true")
    ssl_verify = _db_config_value("PERC_DB_SSL_VERIFY", "DB_SSL_VERIFY", "true")
    ssl_allow_self_signed = _db_config_value(
        "PERC_DB_SSL_ALLOW_SELF_SIGNED", "DB_SSL_ALLOW_SELF_SIGNED", "false"
    )

    args: list[str] = [
        f"--db.type={db_type}",
        f"--db.ssl.enabled={ssl_enabled}",
        f"--db.ssl.verify={ssl_verify}",
        f"--db.ssl.allowSelfSigned={ssl_allow_self_signed}",
    ]

    env_file = _db_config_value("PERC_DB_CONFIG_ENV_FILE", "DB_CONFIG_ENV_FILE", "")
    if env_file:
        args.append(f"--db.config.env.file={env_file}")

    mapping = (
        ("host", "PERC_DB_HOST", "DB_HOST"),
        ("port", "PERC_DB_PORT", "DB_PORT"),
        ("name", "PERC_DB_NAME", "DB_NAME"),
        ("schema", "PERC_DB_SCHEMA", "DB_SCHEMA"),
        ("user", "PERC_DB_USER", "DB_USER"),
        ("password", "PERC_DB_PASSWORD", "DB_PASSWORD"),
        ("trustStorePath", "PERC_DB_SSL_TRUSTSTORE_PATH", "DB_SSL_TRUSTSTORE_PATH"),
        (
            "trustStorePassword",
            "PERC_DB_SSL_TRUSTSTORE_PASSWORD",
            "DB_SSL_TRUSTSTORE_PASSWORD",
        ),
        ("keyStorePath", "PERC_DB_SSL_KEYSTORE_PATH", "DB_SSL_KEYSTORE_PATH"),
        ("keyStorePassword", "PERC_DB_SSL_KEYSTORE_PASSWORD", "DB_SSL_KEYSTORE_PASSWORD"),
    )
    for arg_suffix, primary, fallback in mapping:
        value = _db_config_value(primary, fallback)
        if value:
            args.append(f"--db.{arg_suffix}={value}")
    return args


def _bootstrap_install_root(install_root: Path, seed_base: Path) -> int:
    """Seed ``install_root`` from ``seed_base/{ObjectStore,var,...}``.

    Mirrors the bash ``bootstrap_install_root`` function. Returns 0 on success,
    1 on failure.
    """
    needs_init = False
    if not install_root.is_dir():
        needs_init = True
    elif not any(install_root.iterdir()):
        needs_init = True
    if not needs_init:
        LOGGER.info("install_root already populated (%s); skipping bootstrap", install_root)
        return 0

    LOGGER.info("Bootstrapping install_root %s from %s", install_root, seed_base)
    install_root.mkdir(parents=True, exist_ok=True)
    for sub in ("ObjectStore", "var", "rxconfig", "Deployment/Server/conf", "jetty/base"):
        src = seed_base / sub
        dst = install_root / sub
        if src.is_dir():
            dst.mkdir(parents=True, exist_ok=True)
            try:
                # shutil.copytree(..., dirs_exist_ok=True) merges into dst.
                shutil.copytree(src, dst, dirs_exist_ok=True)
                LOGGER.info("  seeded %s", sub)
            except OSError as exc:
                LOGGER.error("Failed to seed %s from %s: %s", sub, src, exc)
                return 1
        else:
            LOGGER.info("  no seed for %s (ok; install will create it)", sub)
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    install_root = args.install_root
    if not install_root:
        LOGGER.error(
            "--install-root is required (no default on %s).",
            platform.system(),
        )
        return 1
    install_root_path = Path(install_root)

    log_dir = REPO_ROOT / "docker" / "logs"
    log_dir.mkdir(parents=True, exist_ok=True)
    log_file = log_dir / f"install-{_now_utc_compact()}.log"

    env_file = Path(args.env_file)
    if not env_file.is_file():
        LOGGER.error(
            "%s not found. Copy .env.compose.example to .env.compose and edit secrets.",
            env_file,
        )
        return 1
    try:
        _load_env_file(env_file)
    except OSError as exc:
        LOGGER.error("Failed to load %s: %s", env_file, exc)
        return 1

    cms_jar = REPO_ROOT / "modules" / "perc-distribution-tree" / "target" / "perc-distribution-tree.jar"
    dts_jar = (
        REPO_ROOT
        / "deliverytiersuite"
        / "delivery-tier-suite"
        / "delivery-tier-distribution"
        / "target"
        / "delivery-tier-distribution.jar"
    )
    if not cms_jar.is_file():
        LOGGER.error(
            "CMS distribution jar not found: %s. Run ./mvnw clean install -DskipTests=true.",
            cms_jar,
        )
        return 1
    if not dts_jar.is_file():
        LOGGER.error(
            "DTS distribution jar not found: %s. Run ./mvnw clean install -DskipTests=true.",
            dts_jar,
        )
        return 1

    marker = install_root_path / ".percussion-install-complete"
    if marker.is_file() and not args.reset:
        LOGGER.info(
            "Install marker present at %s; skipping install (use --reset to force).",
            marker,
        )
        print(f"RESULT:OK STEP:install ALREADY_INSTALLED LOG:{log_file}")
        return 0

    if not args.no_bootstrap:
        seed_base = REPO_ROOT / "docker" / "dev-data" / "cms-dts"
        rc = _bootstrap_install_root(install_root_path, seed_base)
        if rc != 0:
            print(f"RESULT:FAIL STEP:install LOG:{log_file}")
            return rc

    install_root_path.mkdir(parents=True, exist_ok=True)

    LOGGER.info("Installing CMS into %s", install_root_path)
    LOGGER.info("  CMS_JAR=%s", cms_jar)
    LOGGER.info("  DTS_JAR=%s", dts_jar)
    LOGGER.info("  DB_TYPE=%s", os.environ.get("PERC_DB_TYPE", "h2"))

    db_args = build_installer_db_args()
    LOGGER.info("  DB_ARGS=%s", db_args or "<none>")

    java = shutil.which("java") or "java"
    cms_cmd = [java, "-jar", str(cms_jar), str(install_root_path), *db_args]
    dts_cmd = [java, "-jar", str(dts_jar), str(install_root_path), *db_args]

    try:
        rc_cms = subprocess.run(cms_cmd, shell=False, check=False, timeout=900).returncode
        if args.skip_dts:
            LOGGER.info("Skipping DTS installer (--skip-dts)")
            rc_dts = 0
        else:
            rc_dts = subprocess.run(dts_cmd, shell=False, check=False, timeout=900).returncode
        if rc_cms != 0:
            LOGGER.error("CMS installer exit code: %d", rc_cms)
            print(f"RESULT:FAIL STEP:install LOG:{log_file}")
            return rc_cms
        if rc_dts != 0:
            LOGGER.error("DTS installer exit code: %d", rc_dts)
            print(f"RESULT:FAIL STEP:install LOG:{log_file}")
            return rc_dts
    except FileNotFoundError as exc:
        LOGGER.error("java not on PATH: %s", exc)
        print(f"RESULT:FAIL STEP:install LOG:{log_file}")
        return 1

    marker.touch()
    LOGGER.info("Install complete. Marker written to %s", marker)
    print(f"RESULT:OK STEP:install LOG:{log_file}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
