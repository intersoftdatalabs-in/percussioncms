#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Sign AI resources (skills, prompts, instructions) using Sigstore.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Replaces ``modules/ai-shared-develop/scripts/sign-ai-resources.sh``. The
script:

1. Builds the ``ResourceSigner`` Java utility via Maven (``mvn-env.sh``).
2. Collects every tracked AI resource under
   ``modules/ai-shared-develop/src/main/resources/{skills,instructions,prompts}``
   and ``modules/ai-shared-release/src/main/resources/skills``, plus
   ``AGENTS.md`` (root) and every ``AGENTS.md`` / ``AGENTS.local.md``
   under ``modules/``.
3. Invokes ``com.percussion.ai.signing.ResourceSigner`` via
   ``mvn-env.sh exec:java`` with the collected files as a single
   ``-Dexec.args=...`` argument.

A ``--dry-run`` flag prints every Maven invocation and the discovered
file list without building or signing. This gates pytest.

## Behavioral Notes (FR-009b)

- The original ``.sh`` used bash ``find ... -type f`` to collect files
  in three separate loops. The Python port uses ``Path.rglob`` for the
  same result with a single loop per directory; the excluded patterns
  (``*.sha256``, ``*.sha256.sig``, ``*.sigstore.json``) are matched via
  ``Path.match`` so they're exact-equivalent.
- The original ``.sh`` passed all collected files as a single
  ``-Dexec.args="${files[*]}"`` argument to ``mvn exec:java``. The
  Python port does the same with proper argv handling
  (``-Dexec.args=<space-joined files>``) so the Java main sees the same
  argument list.
- Path discovery uses ``pathlib.Path``; the cross-platform
  ``Path.as_posix()`` is used when constructing the joined file list
  for the ``-Dexec.args=`` string (the Java main is platform-agnostic
  and accepts forward-slash paths).
- All external invocations use ``subprocess.run([...], shell=False,
  check=False)`` so failed Maven runs surface as a non-zero exit code
  without raising.

Exit codes:

  0  signing complete (or dry-run)
  1  invocation error / Maven invocation failed
  2  no AI resources found to sign
"""

from __future__ import annotations

import argparse
import logging
import shlex
import subprocess
import sys
from pathlib import Path
from typing import Iterable, List, Optional

LOG = logging.getLogger("sign-ai-resources")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_NO_RESOURCES = 2

EXCLUDED_SUFFIXES = (".sha256", ".sha256.sig", ".sigstore.json")


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="sign-ai-resources.py",
        description=(
            "Sign every tracked AI resource under modules/ai-shared-develop "
            "+ modules/ai-shared-release + module AGENTS.md files using "
            "Sigstore. Invokes the ResourceSigner Java utility via mvn "
            "exec:java (built first)."
        ),
    )
    p.add_argument(
        "--repo-root",
        type=Path,
        default=None,
        help="Repo root (default: parent of this script's great-grandparent).",
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help=(
            "Print every Maven invocation and the discovered file list "
            "without building or signing. Used by pytest to exercise "
            "the wiring without the build-time cost."
        ),
    )
    p.add_argument(
        "--no-build",
        action="store_true",
        help=(
            "Skip the initial ``mvn clean compile`` (useful when the "
            "ResourceSigner is already built, e.g. in CI caches)."
        ),
    )
    return p


def _is_excluded(path: Path) -> bool:
    """Return True if ``path`` should be skipped from the signing list
    (matches the original ``.sh`` exclusions: ``*.sha256``,
    ``*.sha256.sig``, ``*.sigstore.json``).
    """
    return any(path.name.endswith(suffix) for suffix in EXCLUDED_SUFFIXES)


def _collect_resources(repo_root: Path) -> List[Path]:
    """Collect every trackable AI resource under the standard paths.
    Returns a stable, sorted list so dry-run output is reproducible.
    """
    resources: List[Path] = []

    # ai-shared-develop: skills / instructions / prompts
    dev_resources = repo_root / "modules" / "ai-shared-develop" / "src" / "main" / "resources"
    for sub in ("skills", "instructions", "prompts"):
        sub_path = dev_resources / sub
        if sub_path.is_dir():
            for p in sub_path.rglob("*"):
                if p.is_file() and not _is_excluded(p):
                    resources.append(p)

    # ai-shared-release: skills (the only signed subdir there)
    rel_resources = repo_root / "modules" / "ai-shared-release" / "src" / "main" / "resources"
    if rel_resources.is_dir():
        skills_dir = rel_resources / "skills"
        if skills_dir.is_dir():
            for p in skills_dir.rglob("*"):
                if p.is_file() and not _is_excluded(p):
                    resources.append(p)

    # Root AGENTS.md (single file)
    root_agents = repo_root / "AGENTS.md"
    if root_agents.is_file():
        resources.append(root_agents)

    # Module-level AGENTS.md (every module)
    modules_root = repo_root / "modules"
    if modules_root.is_dir():
        for p in modules_root.rglob("AGENTS.md"):
            if p.is_file() and not _is_excluded(p):
                resources.append(p)

    # AGENTS.local.md (root + every module — local protection, not committed)
    for p in repo_root.rglob("AGENTS.local.md"):
        if p.is_file() and not _is_excluded(p):
            resources.append(p)

    return sorted(set(resources))


def _run_maven(
    mvn_argv0: List[str],
    *,
    cwd: Path,
    extra_args: List[str],
    dry_run: bool,
) -> int:
    """Run ``mvn-env.sh <extra_args>`` via ``subprocess.run([...], shell=False)``.

    Returns ``EXIT_OK`` on success; ``EXIT_INVOCATION`` on non-zero exit
    (including dry-run, where ``EXIT_OK`` is always returned).
    """
    argv = mvn_argv0 + extra_args
    if dry_run:
        LOG.info("DRY-RUN: mvn %s (cwd=%s)", " ".join(extra_args), cwd)
        return EXIT_OK
    LOG.info("Running: mvn %s (cwd=%s)", " ".join(extra_args), cwd)
    completed = subprocess.run(argv, cwd=str(cwd), shell=False, check=False)
    if completed.returncode != 0:
        LOG.error("ERROR: mvn invocation failed with exit code %d", completed.returncode)
        return EXIT_INVOCATION
    return EXIT_OK


def _resolve_maven_argv0(script_path: Path) -> List[str]:
    """Return an argv list for ``mvn-env.sh``. Discovers ``mvn-env.sh``
    by walking up from the script location (mirrors the original ``.sh``
    layout). The script lives at
    ``modules/ai-shared-develop/scripts/sign-ai-resources.py`` so the
    repo root is four levels up.
    """
    repo_root = script_path.resolve().parents[3]
    mvn_env = repo_root / "mvn-env.sh"
    return [str(mvn_env)]


def sign(
    *,
    repo_root: Path,
    mvn_argv0: Optional[List[str]] = None,
    dry_run: bool,
    no_build: bool,
) -> int:
    """Top-level entry point used by both ``main()`` and pytest tests."""
    if mvn_argv0 is None:
        mvn_argv0 = _resolve_maven_argv0(Path(__file__))

    # Step 1 — build ResourceSigner
    if not no_build:
        rc = _run_maven(
            mvn_argv0,
            cwd=repo_root,
            extra_args=["-pl", "modules/ai-shared-develop", "clean", "compile"],
            dry_run=dry_run,
        )
        if rc != EXIT_OK:
            return rc

    # Step 2 — collect resources
    resources = _collect_resources(repo_root)
    if not resources:
        LOG.warning("No AI resources found to sign.")
        return EXIT_NO_RESOURCES

    # Step 3 — invoke the signer via mvn exec:java. The Java main accepts
    # a single ``-Dexec.args=<space-joined files>`` string; pass argv
    # with ``shlex.quote`` to handle spaces / unicode in paths.
    if dry_run:
        LOG.info(
            "DRY-RUN: signer files (%d): %s",
            len(resources),
            ", ".join(p.as_posix() for p in resources[:5])
            + ("..." if len(resources) > 5 else ""),
        )
        return EXIT_OK

    quoted = " ".join(shlex.quote(p.as_posix()) for p in resources)
    return _run_maven(
        mvn_argv0,
        cwd=repo_root,
        extra_args=[
            "-pl", "modules/ai-shared-develop", "exec:java",
            "-Dexec.mainClass=com.percussion.ai.signing.ResourceSigner",
            f"-Dexec.args={quoted}",
        ],
        dry_run=dry_run,
    )


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    repo_root = args.repo_root.resolve() if args.repo_root else (
        Path(__file__).resolve().parents[3]
    )
    return sign(
        repo_root=repo_root,
        dry_run=args.dry_run,
        no_build=args.no_build,
    )


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())