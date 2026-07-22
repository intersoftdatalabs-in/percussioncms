#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/hot-deploy-local.sh.

Purpose
-------
Build selected CMS modules (system, rest, sitemanage, webui) and copy the
artifacts into a local install. For jar modules the artifact is copied into
``jetty/base/webapps/Rhythmyx/WEB-INF/lib``; for webui the built WAR is unzipped
into ``jetty/base/webapps/Rhythmyx``.

Usage
-----
::

    python3 scripts/hot-deploy-local.py --install-dir <path> [options]

Options
-------
- ``--install-dir <path>``  CMS install directory (contains jetty/base). Required.
- ``--modules <csv>``       Comma-separated: system,rest,sitemanage,webui (default: system)
- ``--skip-build``          Skip Maven build, deploy existing artifacts from ``target/``
- ``--restart``             Restart local Jetty after deploy
- ``--with-tests``          Run tests during Maven build (default: -DskipTests)
- ``--timeout-seconds N``   Maven timeout in seconds (default: 600)
- ``--target <cms|dts|both|/abs/path>``  Alias for backwards compatibility with
  the docker hot-deploy-jar.py contract; informational only here.

Behavioral Notes
----------------
- bash ``set -euo pipefail`` → Python try/except + explicit ``returncode`` checks (R2).
- ``chmod +x`` on Jetty start/stop scripts is skipped (FR-007 cross-platform; the
  Windows filesystem doesn't have an executable bit). On Windows the operator is
  expected to invoke ``StartJetty.cmd`` (or run the JAR directly).
- The bash version used ``ls -1t | head -n1`` for "newest jar"; the port uses
  ``sorted(..., key=lambda p: p.stat().st_mtime, reverse=True)[0]`` for the same
  semantic. ``glob`` is portable across OS filesystems.
- The script targets local installs, not Docker containers. For the docker
  variant, see ``docker/scripts/hot-deploy-jar.py`` (separate phase).
"""
from __future__ import annotations

import argparse
import fnmatch
import logging
import os
import shutil
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

JAR_MODULE_ALIASES = {
    "system": ("system", "perc-system"),
    "rest": ("rest", "rest"),
    "sitemanage": ("projects/sitemanage", "sitemanage"),
}


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="hot-deploy-local.py",
        description="Build selected CMS modules and copy outputs into a local install.",
    )
    parser.add_argument(
        "--install-dir",
        required=True,
        help="CMS install directory (contains jetty/base).",
    )
    parser.add_argument(
        "--modules",
        default="system",
        help="Comma-separated modules: system,rest,sitemanage,webui (default: system)",
    )
    parser.add_argument(
        "--skip-build",
        action="store_true",
        help="Skip Maven build, deploy existing artifacts from target/",
    )
    parser.add_argument(
        "--restart",
        action="store_true",
        help="Restart local Jetty after deploy",
    )
    parser.add_argument(
        "--with-tests",
        action="store_true",
        help="Run tests during Maven build (default: -DskipTests)",
    )
    parser.add_argument(
        "--timeout-seconds",
        type=int,
        default=600,
        help="Maven build timeout in seconds (default: 600)",
    )
    parser.add_argument(
        "--target",
        default="both",
        help="Informational only (matches docker hot-deploy-jar.py contract).",
    )
    parser.add_argument(
        "--verify",
        action="store_true",
        help="Informational only; no-op in this local-deploy variant.",
    )
    return parser


def _newest_jar(target_dir: Path, artifact_id: str) -> Path | None:
    """Return the newest (by mtime) primary jar in ``target_dir`` whose name starts
    with ``<artifact_id>-`` and is not a ``-sources`` / ``-javadoc`` / ``-tests``
    / ``original`` variant. Mirrors the bash ``find_primary_jar`` semantic.
    """
    candidates: list[Path] = []
    for p in target_dir.glob(f"{artifact_id}-*.jar"):
        name = p.name
        if name.endswith("-sources.jar"):
            continue
        if name.endswith("-javadoc.jar"):
            continue
        if name.endswith("-tests.jar"):
            continue
        if name.endswith("original.jar"):
            continue
        candidates.append(p)
    if not candidates:
        return None
    return sorted(candidates, key=lambda p: p.stat().st_mtime, reverse=True)[0]


def _newest_war(target_dir: Path) -> Path | None:
    candidates = sorted(target_dir.glob("perc-web-ui-*.war"), key=lambda p: p.stat().st_mtime)
    if not candidates:
        return None
    return candidates[-1]


def _run_maven(module_path: str, run_tests: bool, timeout: int) -> int:
    wrapper = REPO_ROOT / "mvn-env.sh"
    argv: list[str]
    if wrapper.is_file() and not sys.platform.startswith("win"):
        argv = [str(wrapper), "-pl", module_path, "clean", "install"]
    else:
        mvn = shutil.which("mvn") or "mvn"
        argv = [mvn, "-pl", module_path, "clean", "install"]
    if not run_tests:
        argv.append("-DskipTests")
    LOGGER.info("Building module path: %s", module_path)
    return subprocess.run(
        argv,
        shell=False,
        check=False,
        cwd=str(REPO_ROOT),
        timeout=timeout,
    ).returncode


def _deploy_jar_module(
    install_dir: Path,
    lib_dir: Path,
    module_path: str,
    artifact_id: str,
    skip_build: bool,
    run_tests: bool,
    timeout: int,
) -> int:
    target_dir = REPO_ROOT / module_path / "target"
    if not skip_build:
        rc = _run_maven(module_path, run_tests, timeout)
        if rc != 0:
            LOGGER.error("Maven build failed for %s (rc=%d)", module_path, rc)
            return rc
    if not target_dir.is_dir():
        LOGGER.error("target directory not found for %s: %s", module_path, target_dir)
        return 1
    jar = _newest_jar(target_dir, artifact_id)
    if jar is None:
        LOGGER.error("Could not find built jar for %s in %s", artifact_id, target_dir)
        return 1
    # Remove stale duplicate versions of the same artifact.
    for existing in list(lib_dir.iterdir()):
        if (
            existing.is_file()
            and existing.name.startswith(f"{artifact_id}-")
            and existing.name.endswith(".jar")
            and existing.name != jar.name
        ):
            existing.unlink()
    shutil.copy2(jar, lib_dir / jar.name)
    LOGGER.info("Deployed %s -> %s", jar.name, lib_dir)
    return 0


def _deploy_webui(
    install_dir: Path,
    webapp_dir: Path,
    js_target: Path,
    skip_build: bool,
    run_tests: bool,
    timeout: int,
) -> int:
    target_dir = REPO_ROOT / "WebUI" / "target"
    if not skip_build:
        rc = _run_maven("WebUI", run_tests, timeout)
        if rc != 0:
            LOGGER.error("Maven build failed for WebUI (rc=%d)", rc)
            return rc
    if not target_dir.is_dir():
        LOGGER.error("target directory not found for WebUI: %s", target_dir)
        return 1
    war = _newest_war(target_dir)
    if war is None:
        LOGGER.error("Could not find built WAR in %s", target_dir)
        return 1
    LOGGER.info("Unzipping %s -> %s", war.name, webapp_dir)
    # Use Python's zipfile to extract (cross-platform; no ``unzip`` dependency).
    import zipfile

    webapp_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(war, "r") as zf:
        zf.extractall(webapp_dir)
    src_js = webapp_dir / "cm" / "common" / "js"
    if src_js.is_dir():
        js_target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copytree(src_js, js_target, dirs_exist_ok=True)
        LOGGER.info("Synced common JS -> %s", js_target)
    return 0


def _restart_jetty(install_dir: Path, jetty_base: Path) -> int:
    jetty_dir = install_dir / "jetty"
    stop_script = jetty_dir / "StopJetty.sh"
    start_script = jetty_dir / "StartJetty.sh"
    server_log = jetty_base / "logs" / "server.log"
    rc = 0
    if stop_script.is_file():
        LOGGER.info("Stopping Jetty...")
        try:
            subprocess.run(
                [str(stop_script)],
                shell=False,
                check=False,
                cwd=str(jetty_dir),
                timeout=60,
            )
        except (OSError, subprocess.TimeoutExpired) as exc:
            LOGGER.warning("StopJetty.sh raised %s (continuing)", exc)
    if server_log.is_file():
        server_log.unlink()
        LOGGER.info("Removed log file: %s", server_log)
    if start_script.is_file():
        LOGGER.info("Starting Jetty...")
        try:
            rc = subprocess.run(
                [str(start_script)],
                shell=False,
                check=False,
                cwd=str(jetty_dir),
                timeout=120,
            ).returncode
        except (OSError, subprocess.TimeoutExpired) as exc:
            LOGGER.error("StartJetty.sh raised %s", exc)
            return 1
    else:
        LOGGER.warning(
            "%s not found; skipping restart. On Windows, invoke StartJetty.cmd manually.",
            start_script,
        )
    return rc


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    install_dir = Path(args.install_dir)
    jetty_base = install_dir / "jetty" / "base"
    webapp_dir = jetty_base / "webapps" / "Rhythmyx"
    lib_dir = webapp_dir / "WEB-INF" / "lib"
    js_target = install_dir / "web_resources" / "cm" / "common" / "js"

    if not jetty_base.is_dir():
        LOGGER.error("Jetty base directory not found: %s", jetty_base)
        return 1
    if not lib_dir.is_dir():
        LOGGER.error("CMS lib directory not found: %s", lib_dir)
        return 1

    requested = [m.strip() for m in args.modules.split(",") if m.strip()]
    if not requested:
        LOGGER.error("No modules were provided (--modules was empty).")
        return 1
    valid = {"system", "rest", "sitemanage", "webui"}
    bad = [m for m in requested if m not in valid]
    if bad:
        LOGGER.error("Unsupported module(s) %s. Allowed: %s", bad, sorted(valid))
        return 1

    LOGGER.info("Project root : %s", REPO_ROOT)
    LOGGER.info("Install dir  : %s", install_dir)
    LOGGER.info("Modules      : %s", ",".join(requested))
    LOGGER.info("Skip build   : %s", args.skip_build)
    LOGGER.info("Run tests    : %s", args.with_tests)

    rc = 0
    for module in requested:
        if module == "webui":
            rc = _deploy_webui(
                install_dir,
                webapp_dir,
                js_target,
                args.skip_build,
                args.with_tests,
                args.timeout_seconds,
            )
        else:
            module_path, artifact_id = JAR_MODULE_ALIASES[module]
            rc = _deploy_jar_module(
                install_dir,
                lib_dir,
                module_path,
                artifact_id,
                args.skip_build,
                args.with_tests,
                args.timeout_seconds,
            )
        if rc != 0:
            return rc

    if args.restart:
        rc = _restart_jetty(install_dir, jetty_base)
    else:
        LOGGER.info("Deploy complete. Restart Jetty to load updated classes/resources.")
    return rc


if __name__ == "__main__":
    sys.exit(main())
