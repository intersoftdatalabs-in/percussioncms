#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Verify AI resource signatures (Sigstore). Designed to be called by a
git pre-commit hook.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Replaces ``modules/ai-shared-develop/scripts/verify-signatures-hook.sh``.
The script:

1. Builds the ``ResourceVerifier`` Java utility via Maven (``mvn-env.sh``).
2. Collects every AI resource under the standard paths
   (``ai-shared-develop/{skills,instructions,prompts}`` +
   ``ai-shared-release/skills`` + every ``AGENTS.md`` and
   ``AGENTS.local.md``).
3. Invokes ``com.percussion.ai.signing.ResourceVerifier`` via
   ``mvn-env.sh exec:java`` with the collected files as a single
   ``-Dexec.args=...`` argument.

A ``--dry-run`` flag prints every Maven invocation and the discovered
file list without building or verifying. This gates pytest.

## Behavioral Notes (FR-009b)

- The original ``.sh`` used bash ``find ... -type f`` to collect files.
  The Python port uses ``Path.rglob`` with the same
  ``*.sha256``/``*.sha256.sig``/``*.sigstore.json`` exclusions.
- The original ``.sh`` checked ``if [ -f "$file" ]`` before adding to
  the verify list — only existing files are verified. The Python port
  applies the same filter via ``Path.is_file()`` (already implicit in
  ``rglob`` for files only).
- The original ``.sh`` did the build inline before the verify call;
  the Python port uses the same one-shot ``mvn-env.sh clean compile``
  invocation. ``--no-build`` lets callers skip the build step.
- All external invocations use ``subprocess.run([...], shell=False,
  check=False)``.

Exit codes:

  0  all signatures valid (or dry-run)
  1  invocation / build / verify failure
  2  no AI resources found
"""

from __future__ import annotations

import argparse
import logging
import shlex
import subprocess
import sys
from pathlib import Path
from typing import Iterable, List, Optional

LOG = logging.getLogger("verify-signatures-hook")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_NO_RESOURCES = 2

# Reuse the shared resource-discovery + exclusion logic from
# sign-ai-resources.py so both scripts agree on what's in scope.
from sign_ai_resources import _collect_resources  # type: ignore[import-not-found]


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="verify-signatures-hook.py",
        description=(
            "Verify Sigstore signatures for every AI resource. "
            "Designed to be called by a git pre-commit hook."
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
            "without building or verifying."
        ),
    )
    p.add_argument(
        "--no-build",
        action="store_true",
        help=(
            "Skip the initial ``mvn clean compile`` (useful when the "
            "ResourceVerifier is already built)."
        ),
    )
    return p


def _run_maven(
    mvn_argv0: List[str],
    *,
    cwd: Path,
    extra_args: List[str],
    dry_run: bool,
) -> int:
    """Run a mvn-env.sh invocation. Returns ``EXIT_OK`` on success;
    ``EXIT_INVOCATION`` on non-zero exit.
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
    """Resolve ``mvn-env.sh`` from the script location.
    Script lives at ``modules/ai-shared-develop/scripts/verify-signatures-hook.py``,
    repo root is four levels up.
    """
    repo_root = script_path.resolve().parents[3]
    mvn_env = repo_root / "mvn-env.sh"
    return [str(mvn_env)]


def verify(
    *,
    repo_root: Path,
    mvn_argv0: Optional[List[str]] = None,
    dry_run: bool,
    no_build: bool,
) -> int:
    """Top-level entry point used by both ``main()`` and pytest tests."""
    if mvn_argv0 is None:
        mvn_argv0 = _resolve_maven_argv0(Path(__file__))

    if not no_build:
        # Suppressed stdout from the build step — the verifier call is
        # the one whose output we want to see on a real run.
        if dry_run:
            LOG.info(
                "DRY-RUN: mvn -pl modules/ai-shared-develop clean compile "
                "(cwd=%s) [output suppressed in dry-run mode]",
                repo_root,
            )
        else:
            completed = subprocess.run(
                mvn_argv0
                + [
                    "-pl", "modules/ai-shared-develop",
                    "clean", "compile",
                ],
                cwd=str(repo_root),
                shell=False,
                check=False,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )
            if completed.returncode != 0:
                LOG.error("ERROR: build failed (exit %d)", completed.returncode)
                return EXIT_INVOCATION

    resources = _collect_resources(repo_root)
    if not resources:
        LOG.warning("No AI resources found to verify.")
        return EXIT_NO_RESOURCES

    if dry_run:
        LOG.info(
            "DRY-RUN: verifier files (%d): %s",
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
            "-Dexec.mainClass=com.percussion.ai.signing.ResourceVerifier",
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
    return verify(
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