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
"""Hot-deploy the built WebUI modern SPA into an H2 QA cell.

Cross-platform (Windows / Linux / macOS). Stdlib only. ``subprocess.run``
uses ``shell=False`` (root AGENTS.md).

Cycle Verify #3893: copying only hashed files under ``cm/modern/assets/``
without the stable entry ``perc-modern-ui.js`` (and optional ``index.html``)
leaves the live SPA on a stale ``import("./developer-<oldhash>.js")``. That
chunk has csv/sql/http-json but not ``option[value=object-storage]``.

This script copies **every file** under
``WebUI/target/generated-webui/cm/modern/`` (entry JS/CSS, hashed chunks,
sourcemaps, any index.html) into the QA WAR:

``perc-matrix-cms-h2:/opt/Percussion/jetty/base/webapps/Rhythmyx/cm/modern/``

It does **not** ``docker restart`` the cell (silent install wipes copies).
Callers restart Jetty *inside* the cell (StopJetty/StartJetty) then run
``perc-devctl.py qa-health``.

By default the script refuses to deploy a bundle that does not contain
the ``object-storage`` marker in any ``*.js`` under ``assets/``.

Exit codes:

  0  deploy complete (or dry-run plan printed)
  1  invocation / argument error
  2  container not running
  3  source tree / entry file not found
  4  object-storage marker missing in built JS
  5  docker cp / docker exec failed
"""

from __future__ import annotations

import argparse
import logging
import subprocess
import sys
from pathlib import Path
from typing import Iterable, Optional

LOG = logging.getLogger("hot-deploy-webui-modern")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_CONTAINER_NOT_RUNNING = 2
EXIT_SRC_NOT_FOUND = 3
EXIT_MARKER_MISSING = 4
EXIT_DOCKER_FAILED = 5

DEFAULT_CONTAINER = "perc-matrix-cms-h2"
DEFAULT_DEST = "/opt/Percussion/jetty/base/webapps/Rhythmyx/cm/modern"
ENTRY_JS_REL = "assets/perc-modern-ui.js"
ENTRY_CSS_REL = "assets/perc-modern-ui.css"
OBJECT_STORAGE_MARKER = "object-storage"


def _repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def default_src(repo_root: Optional[Path] = None) -> Path:
    root = repo_root if repo_root is not None else _repo_root()
    return root / "WebUI" / "target" / "generated-webui" / "cm" / "modern"


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="hot-deploy-webui-modern.py",
        description=(
            "Copy the built WebUI modern SPA (entry perc-modern-ui.js + "
            "hashed chunks + CSS + any index.html) into an H2 QA cell. "
            "Default container: "
            f"{DEFAULT_CONTAINER}; dest: {DEFAULT_DEST}."
        ),
    )
    p.add_argument(
        "--src",
        type=Path,
        default=None,
        help=(
            "Host directory to copy (default: "
            "WebUI/target/generated-webui/cm/modern)."
        ),
    )
    p.add_argument(
        "--container",
        default=DEFAULT_CONTAINER,
        help=f"Target container name (default: {DEFAULT_CONTAINER}).",
    )
    p.add_argument(
        "--dest",
        default=DEFAULT_DEST,
        help=f"Absolute container directory (default: {DEFAULT_DEST}).",
    )
    p.add_argument(
        "--skip-object-storage-check",
        action="store_true",
        help=(
            "Do not refuse a bundle whose JS lacks the object-storage "
            "marker (escape hatch only)."
        ),
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help=(
            "Print every docker invocation that would be performed "
            "without touching docker or the host beyond reading --src."
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


def iter_src_files(src: Path) -> list[Path]:
    """Return files under ``src`` (stable order). Directories skipped."""
    if not src.is_dir():
        return []
    files = [p for p in src.rglob("*") if p.is_file()]
    files.sort(key=lambda p: p.relative_to(src).as_posix())
    return files


def container_dest_file(dest_root: str, rel_posix: str) -> str:
    """Join container dest + relative path with POSIX slashes only."""
    root = dest_root.strip()
    if not root.startswith("/"):
        raise ValueError(f"container dest must be an absolute POSIX path: {dest_root!r}")
    rel = rel_posix.replace("\\", "/").lstrip("/")
    return f"{root.rstrip('/')}/{rel}"


def bundle_contains_marker(src: Path, marker: str = OBJECT_STORAGE_MARKER) -> bool:
    """True when any ``assets/*.js`` (not maps) contains ``marker``."""
    assets = src / "assets"
    if not assets.is_dir():
        return False
    for p in sorted(assets.glob("*.js")):
        if p.name.endswith(".map"):
            continue
        try:
            text = p.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        if marker in text:
            return True
    return False


def validate_src(src: Path, *, require_object_storage: bool) -> int:
    """Return an exit code if ``src`` is not a deployable modern tree."""
    if not src.is_dir():
        LOG.error("modern source directory not found: %s", src)
        LOG.error(
            "hint: cd WebUI && ../mvnw.cmd clean install  "
            "(Unix: ../mvnw clean install)"
        )
        return EXIT_SRC_NOT_FOUND
    entry = src / "assets" / "perc-modern-ui.js"
    if not entry.is_file():
        LOG.error("missing SPA entry %s under %s", ENTRY_JS_REL, src)
        LOG.error(
            "hint: copy the full generated-webui/cm/modern tree, "
            "not only hashed files under assets/ (#3893)"
        )
        return EXIT_SRC_NOT_FOUND
    css = src / "assets" / "perc-modern-ui.css"
    if not css.is_file():
        LOG.warning("missing %s under %s (JSPs still link it)", ENTRY_CSS_REL, src)
    if require_object_storage and not bundle_contains_marker(src):
        LOG.error(
            "built modern JS under %s does not contain %r — "
            "the live kind select would omit option[value=object-storage] (#3893)",
            src,
            OBJECT_STORAGE_MARKER,
        )
        LOG.error(
            "hint: rebuild WebUI so the developer chunk includes "
            "SOURCE_KIND_OBJECT_STORAGE, then deploy entry + hashed chunks"
        )
        return EXIT_MARKER_MISSING
    return EXIT_OK


def deploy(
    src: Path,
    *,
    container_name: str = DEFAULT_CONTAINER,
    dest: str = DEFAULT_DEST,
    require_object_storage: bool = True,
    dry_run: bool = False,
) -> int:
    """Copy every file under ``src`` into ``container_name:dest``."""
    src = src.resolve()
    rc = validate_src(src, require_object_storage=require_object_storage)
    if rc != EXIT_OK:
        return rc
    if not dest.startswith("/"):
        LOG.error("unsupported --dest (must be absolute POSIX): %s", dest)
        return EXIT_INVOCATION
    if not _container_running(container_name, dry_run=dry_run):
        LOG.error("container not running: %s", container_name)
        return EXIT_CONTAINER_NOT_RUNNING

    files = iter_src_files(src)
    if not files:
        LOG.error("no files under %s", src)
        return EXIT_SRC_NOT_FOUND

    rc = _run(
        ["docker", "exec", container_name, "mkdir", "-p", dest],
        dry_run=dry_run,
    )
    if rc != EXIT_OK:
        return EXIT_DOCKER_FAILED

    parents: set[str] = set()
    for f in files:
        rel = f.relative_to(src).as_posix()
        dest_file = container_dest_file(dest, rel)
        parent = dest_file.rsplit("/", 1)[0]
        if parent and parent not in parents:
            parents.add(parent)
            rc = _run(
                ["docker", "exec", container_name, "mkdir", "-p", parent],
                dry_run=dry_run,
            )
            if rc != EXIT_OK:
                return EXIT_DOCKER_FAILED
        rc = _run(
            [
                "docker",
                "cp",
                str(f),
                f"{container_name}:{dest_file}",
            ],
            dry_run=dry_run,
        )
        if rc != EXIT_OK:
            return EXIT_DOCKER_FAILED

    LOG.info(
        "Deployed %d files from %s -> %s:%s (entry %s)",
        len(files),
        src,
        container_name,
        dest,
        ENTRY_JS_REL,
    )
    LOG.info(
        "Next: in-cell StopJetty/StartJetty, then perc-devctl.py qa-health. "
        "Do not docker restart %s.",
        container_name,
    )
    return EXIT_OK


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    src = args.src.resolve() if args.src is not None else default_src()
    return deploy(
        src,
        container_name=args.container,
        dest=args.dest,
        require_object_storage=not args.skip_object_storage_check,
        dry_run=args.dry_run,
    )


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())
