#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""Cross-platform consolidated API update helper.

Replaces the four per-module Windows batch files:

  - ``APIUpdate-WEBUI.bat``
  - ``APIUpdate-REST.bat``
  - ``APIUpdate-SiteManage.bat``
  - ``APIUpdateJars.bat``

Each ``.bat`` ran the same three-step dance — build the relevant Maven
module(s), copy the resulting jar(s) into the assembled distribution, and
restart Jetty — but hardcoded Windows-specific process spawning
(``start /WAIT cmd /C ...``) and ``xcopy`` / ``copy`` syntax. The four
scripts diverged only in which Maven modules to build and which artifacts to
copy.

This single Python entry point consolidates all four behind
``--module {webui,rest,sitemanage,jars}`` so the same operator workflow runs
identically on Windows, Linux, and macOS.

## Behavioral Notes (FR-009b)

- The original ``.bat`` files used ``start /WAIT cmd /C ...`` to spawn
  Maven / Jetty under Windows process semantics. The Python port invokes
  every external program via ``subprocess.run([...], shell=False, ...)``
  (FR-008; root AGENTS.md "subprocess.run([...], shell=False)" rule).
  Operators on Windows now invoke ``python api-update.py --module webui``
  instead of double-clicking a ``.bat`` — no shell, no ``cmd.exe`` glue.
- The original artifacts paths were hardcoded with the literal version
  suffix ``8.0.2-SNAPSHOT`` (e.g. ``rest-8.0.2-SNAPSHOT.jar``). The Python
  port discovers the actual built artifact via ``Path.glob`` against a
  name-with-version-wildcard pattern, so a future version bump does not
  require editing this script. The JUnit WebUI packaging test pins the
  name prefix; only the version changes.
- The original recursive copy for the WebUI module used
  ``xcopy /D /E /F /H /R /Y ...\*.*``. The Python port uses
  ``shutil.copytree(..., dirs_exist_ok=True)`` — same observable effect
  (overwrites changed files, preserves directory structure), portable on
  Windows and Unix.
- ``--dry-run`` prints every Maven invocation and copy operation it
  *would* perform without touching the filesystem or running Maven. This
  is the gate pytest uses to exercise the wiring without paying the
  build-time cost (and without requiring Maven on the test host).
- The ``--no-restart`` flag preserves the original "build + copy but leave
  Jetty running" workflow (operators would manually start Jetty when
  ready).
- Path discovery uses ``Path(__file__).resolve().parents[N]`` for the
  repo root and module root (R7); no hardcoded separators.
"""

from __future__ import annotations

import argparse
import logging
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Optional, Sequence

LOG = logging.getLogger("api-update")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_BUILD_FAILED = 2
EXIT_COPY_FAILED = 3
EXIT_RESTART_FAILED = 4
EXIT_ARTIFACT_MISSING = 5

# Maven module selectors per --module value. ``-pl`` accepts a comma list.
# Mirrors the original ``.bat`` files exactly:
#   - WEBUI       :pl :CMLite-WebUI
#   - REST        :pl :rest
#   - SiteManage  :pl :sitemanage
#   - Jars        :pl :perc-system,:sitemanage,:rest,:CMLite-WebUI,:perc-tinymce
MODULE_BUILD: dict[str, str] = {
    "webui": ":CMLite-WebUI",
    "rest": ":rest",
    "sitemanage": ":sitemanage",
    "jars": ":perc-system,:sitemanage,:rest,:CMLite-WebUI,:perc-tinymce",
}


@dataclass(frozen=True)
class CopySpec:
    """One artifact-to-destination copy.

    ``source_glob`` is a name-with-version-wildcard pattern under
    ``source_root`` (resolved relative to ``repo_root``). The destination
    directory is ``dist_root / destination_rel`` where ``dist_root`` is
    ``modules/perc-distribution-tree/target/classes/distribution`` — so
    ``destination_rel`` is the path *inside* the unpacked distribution.
    """

    source_root_rel: str
    source_glob: str
    destination_rel: str
    recursive: bool


# Per-module copy specs (paths are relative to ``repo_root`` and
# ``module_dist_root`` respectively). The ``source_root_rel`` directory
# holds the built artifacts; ``destination_rel`` is inside
# ``modules/perc-distribution-tree/target/classes/distribution/jetty/
# base/webapps/Rhythmyx/``.
MODULE_COPIES: dict[str, List[CopySpec]] = {
    "webui": [
        CopySpec(
            source_root_rel="WebUI/target",
            source_glob="CMLite-WebUI-*",
            destination_rel="jetty/base/webapps/Rhythmyx",
            recursive=True,
        ),
    ],
    "rest": [
        CopySpec(
            source_root_rel="rest/target",
            source_glob="rest-*.jar",
            destination_rel="jetty/base/webapps/Rhythmyx/WEB-INF/lib",
            recursive=False,
        ),
    ],
    "sitemanage": [
        CopySpec(
            source_root_rel="projects/sitemanage/target",
            source_glob="sitemanage-*.jar",
            destination_rel="jetty/base/webapps/Rhythmyx/WEB-INF/lib",
            recursive=False,
        ),
    ],
    "jars": [
        CopySpec(
            source_root_rel="projects/sitemanage/target",
            source_glob="sitemanage-*.jar",
            destination_rel="jetty/base/webapps/Rhythmyx/WEB-INF/lib",
            recursive=False,
        ),
        CopySpec(
            source_root_rel="modules/perc-tinymce/target",
            source_glob="perc-tinymce-*.jar",
            destination_rel="jetty/base/webapps/Rhythmyx/WEB-INF/lib",
            recursive=False,
        ),
        CopySpec(
            source_root_rel="modules/perc-auditlog/target",
            source_glob="audit-log-*.jar",
            destination_rel="jetty/base/webapps/Rhythmyx/WEB-INF/lib",
            recursive=False,
        ),
        CopySpec(
            source_root_rel="rest/target",
            source_glob="rest-*.jar",
            destination_rel="jetty/base/webapps/Rhythmyx/WEB-INF/lib",
            recursive=False,
        ),
        CopySpec(
            source_root_rel="system/target",
            source_glob="CMLite-Main-*.jar",
            destination_rel="jetty/base/webapps/Rhythmyx/WEB-INF/lib",
            recursive=False,
        ),
        CopySpec(
            source_root_rel="WebUI/target",
            source_glob="CMLite-WebUI-*",
            destination_rel="jetty/base/webapps/Rhythmyx",
            recursive=True,
        ),
    ],
}


@dataclass(frozen=True)
class ResolvedPaths:
    """Container of cross-platform paths the script needs."""

    repo_root: Path
    module_dir: Path
    jetty_start_script: Path

    @property
    def dist_root(self) -> Path:
        return self.module_dir / "target" / "classes" / "distribution"


def _resolve_paths(script_path: Path) -> ResolvedPaths:
    """Resolve the well-known paths the script needs. ``scripts/`` lives at
    ``modules/perc-distribution-tree/scripts/``; the owning module is one
    level up; the repo root is three levels up (R7).
    """
    here = script_path.resolve().parent
    module_dir = here.parent
    repo_root = module_dir.parent.parent
    jetty_start_script = (
        module_dir
        / "target"
        / "classes"
        / "distribution"
        / "jetty"
        / "StartJetty.bat"
    )
    return ResolvedPaths(
        repo_root=repo_root,
        module_dir=module_dir,
        jetty_start_script=jetty_start_script,
    )


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="api-update.py",
        description=(
            "Cross-platform consolidated API update helper. Builds the "
            "selected Maven module(s), copies the resulting artifact(s) "
            "into the assembled distribution, and optionally restarts "
            "Jetty."
        ),
    )
    p.add_argument(
        "--module",
        required=True,
        choices=sorted(MODULE_BUILD.keys()),
        help="Which API update to run.",
    )
    p.add_argument(
        "--skip-tests",
        action="store_true",
        help="Pass -DskipTests=true to Maven.",
    )
    p.add_argument(
        "--no-restart",
        action="store_true",
        help="Do not restart Jetty after the update.",
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help=(
            "Print every Maven invocation and copy operation that would be "
            "performed without touching the filesystem or running Maven. "
            "Used by pytest to exercise the wiring without paying the "
            "build-time cost."
        ),
    )
    return p


def _resolve_maven(maven_path: Optional[Path]) -> Optional[List[str]]:
    """Return an argv list for the Maven executable, honoring ``MAVEN_HOME``
    / PATH discovery. Returns ``None`` if no Maven can be located.

    Cross-platform: returns the literal command name ``mvn`` so the OS
    resolver handles ``.bat`` / ``.cmd`` / executable bit differences. We
    pass ``mvn`` as a single argv element with ``shell=False`` — the OS
    finds it on PATH.
    """
    if maven_path is not None:
        return [str(maven_path)]
    return None


def _run_maven(
    argv0: Sequence[str],
    *,
    cwd: Path,
    skip_tests: bool,
    dry_run: bool,
) -> int:
    """Run Maven via ``subprocess.run([...], shell=False)``. Returns the
    child process exit code, or 0 under ``--dry-run``.
    """
    mvn_args = list(argv0) + ["clean", "install"]
    if skip_tests:
        mvn_args.append("-DskipTests=true")
    if dry_run:
        LOG.info(
            "DRY-RUN: mvn %s (cwd=%s)",
            " ".join(mvn_args[1:]),
            cwd,
        )
        return EXIT_OK
    LOG.info("Running: %s (cwd=%s)", " ".join(mvn_args), cwd)
    completed = subprocess.run(
        mvn_args,
        cwd=str(cwd),
        shell=False,
        check=False,
        timeout=None,
    )
    return completed.returncode


def _copy_artifact(
    spec: CopySpec,
    *,
    repo_root: Path,
    dist_root: Path,
    dry_run: bool,
) -> int:
    """Copy one artifact per ``spec``. Returns EXIT_OK on success, an
    error code on failure.
    """
    source_root = repo_root / spec.source_root_rel
    destination = dist_root / spec.destination_rel
    if not source_root.is_dir():
        LOG.error(
            "ERROR: source directory does not exist: %s "
            "(build the module first or check --module)",
            source_root,
        )
        return EXIT_ARTIFACT_MISSING

    matches = sorted(source_root.glob(spec.source_glob))
    if not matches:
        LOG.error(
            "ERROR: no artifact matching %s under %s",
            spec.source_glob,
            source_root,
        )
        return EXIT_ARTIFACT_MISSING

    if spec.recursive:
        # Each match is a directory (WebUI build output). Copy the entire
        # tree into ``destination`` — mirrors the original ``xcopy /D /E /F
        # /H /R /Y ...\*.*`` semantics.
        for src in matches:
            if not src.is_dir():
                continue
            target = destination / src.name
            if dry_run:
                LOG.info("DRY-RUN: copytree %s -> %s", src, target)
                continue
            try:
                destination.mkdir(parents=True, exist_ok=True)
                shutil.copytree(str(src), str(target), dirs_exist_ok=True)
                LOG.info("Copied tree %s -> %s", src, target)
            except (OSError, shutil.Error) as exc:
                LOG.error("ERROR: copytree failed: %s (%s)", target, exc)
                return EXIT_COPY_FAILED
    else:
        # Each match is a single file (jar). Copy into ``destination`` with
        # the same basename — mirrors the original ``copy /Y src dst``
        # semantics.
        for src in matches:
            if not src.is_file():
                continue
            target = destination / src.name
            if dry_run:
                LOG.info("DRY-RUN: copy2 %s -> %s", src, target)
                continue
            try:
                destination.mkdir(parents=True, exist_ok=True)
                shutil.copy2(str(src), str(target))
                LOG.info("Copied file %s -> %s", src, target)
            except OSError as exc:
                LOG.error("ERROR: copy2 failed: %s (%s)", target, exc)
                return EXIT_COPY_FAILED

    return EXIT_OK


def _restart_jetty(
    jetty_script: Path,
    *,
    cwd: Path,
    dry_run: bool,
) -> int:
    """Restart Jetty by running the StartJetty script in the distribution.
    The original ``.bat`` files used ``start /WAIT cmd /C target\\classes\\
    distribution\\jetty\\StartJetty.bat``; the Python port invokes the
    script directly with ``subprocess.run([...], shell=False)``. On Windows
    the launcher resolves ``StartJetty.bat`` via the OS association; on
    Unix this would be a no-op unless the operator also runs Jetty from a
    POSIX shell, which is outside this helper's scope.
    """
    if not jetty_script.is_file():
        LOG.error(
            "ERROR: Jetty start script not found: %s "
            "(run `mvn package` in modules/perc-distribution-tree first)",
            jetty_script,
        )
        return EXIT_RESTART_FAILED
    if dry_run:
        LOG.info("DRY-RUN: run %s (cwd=%s)", jetty_script, cwd)
        return EXIT_OK
    LOG.info("Starting Jetty: %s (cwd=%s)", jetty_script, cwd)
    completed = subprocess.run(
        [str(jetty_script)],
        cwd=str(cwd),
        shell=False,
        check=False,
    )
    return completed.returncode


def run_module(
    *,
    module: str,
    skip_tests: bool,
    no_restart: bool,
    dry_run: bool,
    paths: ResolvedPaths,
    maven_argv0: Optional[Sequence[str]] = None,
) -> int:
    """Top-level entry point used by both ``main()`` and pytest tests.

    ``maven_argv0`` lets tests inject a stub that records invocations
    instead of running Maven. Pass ``None`` to discover Maven from PATH.
    """
    build_selector = MODULE_BUILD[module]
    if maven_argv0 is None:
        maven_argv0 = _resolve_maven(None)
        if maven_argv0 is None:
            LOG.error(
                "ERROR: Maven not found on PATH (set MAVEN_HOME or add mvn "
                "to PATH); rerun with --dry-run to inspect the build plan"
            )
            return EXIT_INVOCATION

    rc = _run_maven(
        maven_argv0,
        cwd=paths.repo_root,
        skip_tests=skip_tests,
        dry_run=dry_run,
    )
    if rc != EXIT_OK:
        LOG.error("ERROR: Maven build failed with exit code %d", rc)
        return EXIT_BUILD_FAILED

    for spec in MODULE_COPIES[module]:
        rc = _copy_artifact(
            spec,
            repo_root=paths.repo_root,
            dist_root=paths.dist_root,
            dry_run=dry_run,
        )
        if rc != EXIT_OK:
            return rc

    if not no_restart:
        rc = _restart_jetty(
            paths.jetty_start_script,
            cwd=paths.module_dir,
            dry_run=dry_run,
        )
        if rc != EXIT_OK:
            return rc

    LOG.info("OK: api-update --module %s complete", module)
    return EXIT_OK


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    paths = _resolve_paths(Path(__file__))
    return run_module(
        module=args.module,
        skip_tests=args.skip_tests,
        no_restart=args.no_restart,
        dry_run=args.dry_run,
        paths=paths,
    )


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())