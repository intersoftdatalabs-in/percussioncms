#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""QA rebuild chain driver: sitemanage → WebUI → perc-distribution-tree (#2533).

Preflight (``qa_preflight.py`` / ``perc-devctl qa-preflight``) only **detects**
a stale WebUI WAR vs a freshly installed sitemanage SNAPSHOT. Operators and
agents still had to re-type the Maven order by hand — often wrong order,
skipping WebUI, or packaging only ``perc-distribution-tree``.

This module runs the documented portable order via the **repo-root** Maven
wrapper (``mvnw`` / ``mvnw.cmd``), ``pathlib.Path``, and
``subprocess.run([...], shell=False)``:

1. ``projects/sitemanage`` — ``clean install`` (optional ``-DskipTests``)
2. ``WebUI`` — ``package -DskipTests``
3. ``modules/perc-distribution-tree`` — ``clean package -DskipTests``

Each step emits a parseable ``RESULT:OK`` / ``RESULT:FAIL`` line. ``--dry-run``
prints the planned argv + cwd for every step and never invokes Maven.

Standalone::

    python3 docker/scripts/qa_rebuild_chain.py [--dry-run] [--skip-tests]
    python3 docker/scripts/qa_rebuild_chain.py --dist-only

Via perc-devctl::

    python3 docker/scripts/perc-devctl.py qa-rebuild-chain [--dry-run]
"""

from __future__ import annotations

import argparse
import logging
import os
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Iterable, List, Optional, Sequence

LOG = logging.getLogger("qa_rebuild_chain")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_SUBPROCESS_FAILED = 2

# Callable matching subprocess.run for tests to inject stubs.
RunFn = Callable[..., subprocess.CompletedProcess]


@dataclass(frozen=True)
class ChainStep:
    """One Maven invocation in the rebuild chain."""

    label: str
    module_rel: Path  # relative to repo root; cwd for the step
    goals: tuple[str, ...]  # e.g. ("clean", "install") or ("package",)
    extra_args: tuple[str, ...] = ()

    def cwd(self, repo_root: Path) -> Path:
        return (repo_root / self.module_rel).resolve()

    def argv(self, mvnw: Path) -> List[str]:
        return [str(mvnw), *self.goals, *self.extra_args]


def resolve_mvnw(repo_root: Path) -> Path:
    """Return the repo-root Maven wrapper path for this OS.

    Windows uses ``mvnw.cmd``; Unix uses the ``mvnw`` shell script.
    Does not require the file to exist (callers may dry-run against a
    synthetic tree); existence is checked before a real run.
    """
    name = "mvnw.cmd" if sys.platform.startswith("win") else "mvnw"
    return (repo_root / name).resolve()


def plan_chain(
    *,
    skip_tests: bool = False,
    dist_only: bool = False,
) -> List[ChainStep]:
    """Return the ordered list of Maven steps for the QA rebuild chain.

    Parameters
    ----------
    skip_tests:
        When True, pass ``-DskipTests`` on the sitemanage step as well.
        WebUI and perc-distribution-tree always skip tests (matches the
        documented operator commands in workbench-rest-and-qa-modes.md).
    dist_only:
        When True, only package ``perc-distribution-tree`` (no clean on
        sitemanage/WebUI). Use when only installer packaging resources
        changed and SNAPSHOT artifacts under the WAR are already fresh.
    """
    skip = ("-DskipTests",)
    if dist_only:
        return [
            ChainStep(
                label="qa-rebuild-dist",
                module_rel=Path("modules") / "perc-distribution-tree",
                goals=("package",),
                extra_args=skip,
            ),
        ]

    sitemanage_extra = skip if skip_tests else ()
    return [
        ChainStep(
            label="qa-rebuild-sitemanage",
            module_rel=Path("projects") / "sitemanage",
            goals=("clean", "install"),
            extra_args=sitemanage_extra,
        ),
        ChainStep(
            label="qa-rebuild-webui",
            module_rel=Path("WebUI"),
            goals=("package",),
            extra_args=skip,
        ),
        ChainStep(
            label="qa-rebuild-dist",
            module_rel=Path("modules") / "perc-distribution-tree",
            goals=("clean", "package"),
            extra_args=skip,
        ),
    ]


def _format_planned(step: ChainStep, repo_root: Path, mvnw: Path) -> str:
    argv = step.argv(mvnw)
    cwd = step.cwd(repo_root)
    # Use Path.as_posix() only for display of relative module; argv paths
    # remain OS-native strings for subprocess.
    return (
        f"PLANNED STEP:{step.label} "
        f"CWD:{cwd} "
        f"ARGV:{subprocess.list2cmdline(argv) if sys.platform.startswith('win') else ' '.join(argv)}"
    )


def _print_result(ok: bool, label: str, log_path: Optional[Path] = None) -> None:
    status = "OK" if ok else "FAIL"
    log_part = f" LOG:{log_path}" if log_path is not None else " LOG:"
    print(f"RESULT:{status} STEP:{label}{log_part}")


def run_chain(
    repo_root: Path,
    *,
    dry_run: bool = False,
    skip_tests: bool = False,
    dist_only: bool = False,
    log_dir: Optional[Path] = None,
    run_fn: Optional[RunFn] = None,
    timeout_seconds: Optional[int] = None,
) -> int:
    """Execute (or dry-run) the rebuild chain. Returns EXIT_*.

    ``run_fn`` defaults to ``subprocess.run``; unit tests inject a stub
    that records argv/cwd and returns ``CompletedProcess``.

    When ``log_dir`` is set, each real step writes stdout+stderr to
    ``<log_dir>/<label>-<n>.log``. Dry-run still emits RESULT lines
    without creating logs unless ``log_dir`` is provided (then a short
    planned-argv log is written).
    """
    repo_root = repo_root.resolve()
    mvnw = resolve_mvnw(repo_root)
    steps = plan_chain(skip_tests=skip_tests, dist_only=dist_only)
    runner = run_fn or subprocess.run

    if not dry_run and not mvnw.is_file():
        print(
            f"ERROR: Maven wrapper not found at {mvnw} "
            f"(expected mvnw / mvnw.cmd at repo root)",
            file=sys.stderr,
        )
        _print_result(False, "qa-rebuild-chain")
        return EXIT_INVOCATION

    overall_ok = True
    for index, step in enumerate(steps):
        planned = _format_planned(step, repo_root, mvnw)
        print(planned)
        LOG.info(planned)

        cwd = step.cwd(repo_root)
        argv = step.argv(mvnw)
        log_path: Optional[Path] = None
        if log_dir is not None:
            log_dir.mkdir(parents=True, exist_ok=True)
            log_path = log_dir / f"{step.label}-{index}.log"

        if dry_run:
            if log_path is not None:
                with log_path.open("w", encoding="utf-8") as fh:
                    fh.write(planned + "\n")
            _print_result(True, step.label, log_path)
            continue

        if not cwd.is_dir():
            print(
                f"ERROR: module directory missing for {step.label}: {cwd}",
                file=sys.stderr,
            )
            _print_result(False, step.label, log_path)
            overall_ok = False
            break

        LOG.info("Running: %s (cwd=%s)", " ".join(argv), cwd)
        try:
            if log_path is not None:
                with log_path.open("w", encoding="utf-8") as fh:
                    completed = runner(
                        list(argv),
                        cwd=str(cwd),
                        shell=False,
                        check=False,
                        stdout=fh,
                        stderr=subprocess.STDOUT,
                        timeout=timeout_seconds,
                    )
            else:
                completed = runner(
                    list(argv),
                    cwd=str(cwd),
                    shell=False,
                    check=False,
                    timeout=timeout_seconds,
                )
        except subprocess.TimeoutExpired:
            print(
                f"ERROR: step {step.label} timed out after {timeout_seconds}s",
                file=sys.stderr,
            )
            _print_result(False, step.label, log_path)
            overall_ok = False
            break
        except OSError as exc:
            print(f"ERROR: failed to start Maven for {step.label}: {exc}", file=sys.stderr)
            _print_result(False, step.label, log_path)
            overall_ok = False
            break

        if completed.returncode != 0:
            print(
                f"ERROR: step {step.label} exited {completed.returncode}",
                file=sys.stderr,
            )
            _print_result(False, step.label, log_path)
            overall_ok = False
            break

        _print_result(True, step.label, log_path)

    chain_label = "qa-rebuild-chain"
    if overall_ok:
        _print_result(True, chain_label)
        return EXIT_OK
    _print_result(False, chain_label)
    return EXIT_SUBPROCESS_FAILED


def _default_repo_root() -> Path:
    """Repo root when invoked as docker/scripts/qa_rebuild_chain.py."""
    return Path(__file__).resolve().parents[2]


def build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="qa_rebuild_chain.py",
        description=(
            "Run the QA Maven rebuild chain: sitemanage install → "
            "WebUI package → perc-distribution-tree package (#2533). "
            "Uses repo-root mvnw/mvnw.cmd with shell=False."
        ),
    )
    p.add_argument(
        "--repo-root",
        type=Path,
        default=None,
        help="Percussion CMS checkout root (default: grandparent of this script).",
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help="Print planned argv/cwd for each step; do not invoke Maven.",
    )
    p.add_argument(
        "--skip-tests",
        action="store_true",
        help=(
            "Pass -DskipTests on the sitemanage step "
            "(WebUI and dist already skip tests)."
        ),
    )
    p.add_argument(
        "--dist-only",
        action="store_true",
        help=(
            "Only run modules/perc-distribution-tree package -DskipTests "
            "(when SNAPSHOT WAR inputs are already fresh)."
        ),
    )
    p.add_argument(
        "--log-dir",
        type=Path,
        default=None,
        help="Directory for per-step logs (default: docker/logs under repo root).",
    )
    p.add_argument(
        "--timeout-seconds",
        type=int,
        default=None,
        metavar="N",
        help="Optional wall-clock timeout per Maven step (default: no limit).",
    )
    p.add_argument(
        "--verbose",
        action="store_true",
        help="Verbose logging on stderr.",
    )
    return p


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )

    repo_root = (
        args.repo_root.resolve()
        if args.repo_root is not None
        else _default_repo_root()
    )
    log_dir = args.log_dir
    if log_dir is None and not args.dry_run:
        log_dir = repo_root / "docker" / "logs"
    elif log_dir is None and args.dry_run:
        # Dry-run still can write short planned logs when default log dir
        # is useful for agent workflows; keep None to avoid mkdir noise
        # unless operator passed --log-dir.
        log_dir = None

    return run_chain(
        repo_root,
        dry_run=args.dry_run,
        skip_tests=args.skip_tests,
        dist_only=args.dist_only,
        log_dir=log_dir,
        timeout_seconds=args.timeout_seconds,
    )


if __name__ == "__main__":
    sys.exit(main())
