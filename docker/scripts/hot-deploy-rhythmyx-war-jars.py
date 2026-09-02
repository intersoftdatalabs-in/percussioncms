#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Copyright (c) 2026 Intersoft Data Labs, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#
# See the License for the specific language governing permissions and
# limitations under the License.
"""Hot-deploy perc-system / rest / sitemanage SNAPSHOTs into an H2 QA WAR.

Cross-platform (Windows / Linux / macOS). Stdlib only. ``subprocess.run``
uses ``shell=False`` (root AGENTS.md).

Cycle Verify #4174: ``qa-up --skip-image-build --then-qa-deploy-webui``
copies a current SPA (``option[value=sitemap-xml]``) into a cell whose
``perc-system`` / ``sitemanage`` / ``rest`` SNAPSHOTs still predate the
sitemap-xml allow-list. Live PUT ``/services/sites/{name}/virtual`` then
returns 400, so ``[data-testid=developer-site-virtual-saved]`` never
appears.

This script copies matching module ``target/*.jar`` files into:

``perc-matrix-cms-h2:/opt/Percussion/jetty/base/webapps/Rhythmyx/WEB-INF/lib/``

It does **not** ``docker restart`` the cell (silent install wipes copies).
Optional ``--restart-jetty`` runs in-cell ``StopJetty.sh`` then detached
``StartJetty.sh``. Callers then run ``perc-devctl.py qa-health``.

``perc-system`` must contain
``com/percussion/services/virtualsite/PSSitemapXmlVirtualSiteSource.class``
so skip-image-build cells cannot keep a stale allow-list.

Exit codes:

  0  deploy complete (or dry-run plan printed)
  1  invocation / argument error
  2  container not running
  3  SNAPSHOT jar not found
  4  perc-system jar lacks sitemap-xml allow-list class
  5  docker cp / docker exec failed
"""

from __future__ import annotations

import argparse
import logging
import subprocess
import sys
import zipfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, Optional

LOG = logging.getLogger("hot-deploy-rhythmyx-war-jars")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_CONTAINER_NOT_RUNNING = 2
EXIT_JAR_NOT_FOUND = 3
EXIT_MARKER_MISSING = 4
EXIT_DOCKER_FAILED = 5

DEFAULT_CONTAINER = "perc-matrix-cms-h2"
DEFAULT_DEST = "/opt/Percussion/jetty/base/webapps/Rhythmyx/WEB-INF/lib"
STOP_JETTY = "/opt/Percussion/jetty/StopJetty.sh"
START_JETTY = "/opt/Percussion/jetty/StartJetty.sh"
SITEMAP_XML_CLASS = (
    "com/percussion/services/virtualsite/PSSitemapXmlVirtualSiteSource.class"
)
SKIP_JAR_SUFFIXES = ("-sources.jar", "-javadoc.jar", "-tests.jar")

# (module dir relative to repo root, Maven artifactId)
DEFAULT_MODULES: tuple[tuple[str, str], ...] = (
    ("system", "perc-system"),
    ("rest", "rest"),
    ("projects/sitemanage", "sitemanage"),
)


def _repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="hot-deploy-rhythmyx-war-jars.py",
        description=(
            "Copy perc-system, rest, and sitemanage SNAPSHOT jars into the "
            "H2 QA Rhythmyx WAR WEB-INF/lib (#4174). Default container: "
            f"{DEFAULT_CONTAINER}; dest: {DEFAULT_DEST}."
        ),
    )
    p.add_argument(
        "--repo-root",
        type=Path,
        default=None,
        help="Monorepo root (default: two parents above this script).",
    )
    p.add_argument(
        "--container",
        default=DEFAULT_CONTAINER,
        help=f"Target container name (default: {DEFAULT_CONTAINER}).",
    )
    p.add_argument(
        "--dest",
        default=DEFAULT_DEST,
        help=f"Absolute container WEB-INF/lib (default: {DEFAULT_DEST}).",
    )
    p.add_argument(
        "--restart-jetty",
        action="store_true",
        help=(
            "After copy, docker exec StopJetty.sh then detached StartJetty.sh. "
            "Does not docker-restart the cell."
        ),
    )
    p.add_argument(
        "--skip-sitemap-xml-check",
        action="store_true",
        help="Do not refuse perc-system without PSSitemapXmlVirtualSiteSource.",
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help=(
            "Print every docker invocation that would be performed "
            "without touching docker."
        ),
    )
    return p


def _run(argv0: Iterable[str], *, dry_run: bool) -> int:
    cmd = list(argv0)
    if dry_run:
        LOG.info("DRY-RUN: %s", " ".join(cmd))
        return EXIT_OK
    LOG.info("Running: %s", " ".join(cmd))
    completed = subprocess.run(cmd, shell=False, check=False)
    if completed.returncode != 0:
        return EXIT_DOCKER_FAILED
    return EXIT_OK


def _container_running(container_name: str, *, dry_run: bool) -> bool:
    if dry_run:
        return True
    completed = subprocess.run(
        ["docker", "ps", "--format", "{{.Names}}"],
        capture_output=True,
        text=True,
        shell=False,
        check=False,
    )
    if completed.returncode != 0:
        return False
    running = {
        line.strip()
        for line in (completed.stdout or "").splitlines()
        if line.strip()
    }
    return container_name in running


def newest_primary_jar(target_dir: Path, artifact_id: str) -> Optional[Path]:
    """Newest ``<artifactId>-*.jar`` that is not sources/javadoc/tests."""
    if not target_dir.is_dir():
        return None
    candidates: list[Path] = []
    for p in target_dir.glob(f"{artifact_id}-*.jar"):
        name = p.name
        if any(name.endswith(sfx) for sfx in SKIP_JAR_SUFFIXES):
            continue
        if name.startswith("original-"):
            continue
        candidates.append(p)
    if not candidates:
        return None
    return max(candidates, key=lambda item: item.stat().st_mtime)


def jar_has_sitemap_xml_source(jar_path: Path) -> bool:
    """True when perc-system SNAPSHOT ships the sitemap-xml adapter class."""
    try:
        with zipfile.ZipFile(jar_path, "r") as zf:
            return SITEMAP_XML_CLASS in zf.namelist()
    except (OSError, zipfile.BadZipFile):
        return False


def resolve_module_jars(repo_root: Path) -> tuple[list[tuple[str, Path]], int]:
    """Return ``[(artifactId, jar_path), ...]`` or an exit code on failure."""
    found: list[tuple[str, Path]] = []
    for rel, artifact_id in DEFAULT_MODULES:
        jar = newest_primary_jar(repo_root / rel / "target", artifact_id)
        if jar is None:
            LOG.error(
                "SNAPSHOT jar not found for %s under %s/target "
                "(build the module, then re-run). Needed so skip-image-build "
                "QA cells pick up sitemap-xml allow-list (#4174).",
                artifact_id,
                rel,
            )
            return [], EXIT_JAR_NOT_FOUND
        found.append((artifact_id, jar))
    return found, EXIT_OK


def _list_lib_jars(
    container_name: str, dest: str, *, dry_run: bool
) -> tuple[int, list[str]]:
    if dry_run:
        LOG.info("DRY-RUN: docker exec %s ls %s", container_name, dest)
        return EXIT_OK, []
    completed = subprocess.run(
        ["docker", "exec", container_name, "ls", dest],
        capture_output=True,
        text=True,
        shell=False,
        check=False,
    )
    if completed.returncode != 0:
        return EXIT_DOCKER_FAILED, []
    names = [
        line.strip()
        for line in (completed.stdout or "").splitlines()
        if line.strip()
    ]
    return EXIT_OK, names


def _remove_artifact_jars(
    container_name: str,
    dest: str,
    artifact_id: str,
    *,
    keep_name: str,
    dry_run: bool,
) -> int:
    """Remove other ``<artifactId>-*.jar`` so only one SNAPSHOT is on the WAR classpath."""
    rc, names = _list_lib_jars(container_name, dest, dry_run=dry_run)
    if rc != EXIT_OK:
        return rc
    prefix = artifact_id + "-"
    for name in names:
        if name == keep_name:
            continue
        if not name.startswith(prefix) or not name.endswith(".jar"):
            continue
        if any(name.endswith(sfx) for sfx in SKIP_JAR_SUFFIXES):
            continue
        remote = f"{dest}/{name}"
        rc = _run(
            ["docker", "exec", container_name, "rm", "-f", remote],
            dry_run=dry_run,
        )
        if rc != EXIT_OK:
            return rc
    return EXIT_OK


def _deploy_one(
    jar_path: Path,
    container_name: str,
    dest: str,
    artifact_id: str,
    *,
    dry_run: bool,
) -> int:
    jar_basename = jar_path.name
    target_jar = f"{dest}/{jar_basename}"
    ts = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
    backup_jar = f"{target_jar}.bak.{ts}"

    LOG.info("Deploying %s -> %s:%s", jar_basename, container_name, dest)

    rc = _run(
        ["docker", "exec", container_name, "mkdir", "-p", dest],
        dry_run=dry_run,
    )
    if rc != EXIT_OK:
        return EXIT_DOCKER_FAILED

    rc = _remove_artifact_jars(
        container_name,
        dest,
        artifact_id,
        keep_name=jar_basename,
        dry_run=dry_run,
    )
    if rc != EXIT_OK:
        return rc

    if dry_run:
        LOG.info(
            "DRY-RUN: backup-if-exists: docker exec %s stat %s ; then mv %s %s",
            container_name,
            target_jar,
            target_jar,
            backup_jar,
        )
    else:
        check_rc = subprocess.run(
            ["docker", "exec", container_name, "stat", target_jar],
            capture_output=True,
            shell=False,
            check=False,
        )
        if check_rc.returncode == 0:
            backup_rc = _run(
                ["docker", "exec", container_name, "mv", target_jar, backup_jar],
                dry_run=False,
            )
            if backup_rc != EXIT_OK:
                return EXIT_DOCKER_FAILED

    return _run(
        [
            "docker",
            "cp",
            str(jar_path.resolve()),
            f"{container_name}:{target_jar}",
        ],
        dry_run=dry_run,
    )


def _restart_jetty(container_name: str, *, dry_run: bool) -> int:
    rc = _run(
        ["docker", "exec", container_name, STOP_JETTY],
        dry_run=dry_run,
    )
    if rc != EXIT_OK:
        LOG.warning("StopJetty.sh returned %s (continuing to StartJetty)", rc)
    # Detached so StartJetty.sh cannot hold this process in the foreground.
    rc = _run(
        ["docker", "exec", "-d", container_name, START_JETTY],
        dry_run=dry_run,
    )
    if rc != EXIT_OK:
        return EXIT_DOCKER_FAILED
    LOG.info(
        "Restarted Jetty inside %s via StopJetty/StartJetty. Next: perc-devctl.py qa-health. "
        "Do not docker restart %s.",
        container_name,
        container_name,
    )
    return EXIT_OK


def deploy(
    repo_root: Path,
    *,
    container_name: str = DEFAULT_CONTAINER,
    dest: str = DEFAULT_DEST,
    restart_jetty: bool = False,
    require_sitemap_xml: bool = True,
    dry_run: bool = False,
) -> int:
    if not dest.startswith("/"):
        LOG.error("unsupported --dest (must be absolute POSIX): %s", dest)
        return EXIT_INVOCATION
    jars, rc = resolve_module_jars(repo_root)
    if rc != EXIT_OK:
        return rc
    for artifact_id, jar in jars:
        if artifact_id == "perc-system" and require_sitemap_xml:
            if not jar_has_sitemap_xml_source(jar):
                LOG.error(
                    "%s lacks %s — rebuild system so sitemap-xml is allow-listed (#4174).",
                    jar,
                    SITEMAP_XML_CLASS,
                )
                return EXIT_MARKER_MISSING
    if not _container_running(container_name, dry_run=dry_run):
        LOG.error("container not running: %s", container_name)
        return EXIT_CONTAINER_NOT_RUNNING
    for artifact_id, jar in jars:
        rc = _deploy_one(
            jar,
            container_name,
            dest,
            artifact_id,
            dry_run=dry_run,
        )
        if rc != EXIT_OK:
            return rc
    if restart_jetty:
        return _restart_jetty(container_name, dry_run=dry_run)
    LOG.info(
        "Copied %d SNAPSHOTs -> %s:%s. Next: in-cell StopJetty/StartJetty, then qa-health.",
        len(jars),
        container_name,
        dest,
    )
    return EXIT_OK


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    root = args.repo_root.resolve() if args.repo_root is not None else _repo_root()
    return deploy(
        root,
        container_name=args.container,
        dest=args.dest,
        restart_jetty=bool(args.restart_jetty),
        require_sitemap_xml=not bool(args.skip_sitemap_xml_check),
        dry_run=bool(args.dry_run),
    )


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(levelname)s %(name)s: %(message)s",
    )
    sys.exit(main())
