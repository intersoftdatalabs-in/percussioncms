#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""QA rebuild chain driver: sitemanage → WebUI → license inventory → dist (#2533 / #4420).

Preflight (``qa_preflight.py`` / ``perc-devctl qa-preflight``) only **detects**
a stale WebUI WAR vs a freshly installed sitemanage SNAPSHOT. Operators and
agents still had to re-type the Maven order by hand — often wrong order,
skipping WebUI, or packaging only ``perc-distribution-tree``.

This module runs the documented portable order via the **repo-root** Maven
wrapper (``mvnw`` / ``mvnw.cmd``), ``pathlib.Path``, and
``subprocess.run([...], shell=False)``:

1. ``projects/sitemanage`` — ``clean install`` (optional ``-DskipTests``)
2. ``WebUI`` — ``package -DskipTests``
3. reactor root — ``license:aggregate-add-third-party`` (no ``-N``) when
   ``target/generated-sources/license/THIRD-PARTY-MAVEN.txt`` is missing
   (#4420). ``perc-distribution-tree`` merge-third-party-inventory
   ``--require-maven`` fails closed without that file.
4. ``deliverytiersuite/delivery-tier-suite/secure-membership`` —
   ``package -DskipTests`` when ``target/dependency`` is missing
   (ANT copies those jars into the installer; #4420 residual).
   (The ``modules/perc-tinymce`` step retired with that module in #5734:
   no TinyMCE trees ship anymore, so there is nothing for ANT to copy.)
5. ``modules/perc-common-ui-bundle`` — ``package -DskipTests`` when
   ``perc_common_ui.js`` is missing from ``target/classes`` (#4695).
6. ``deliverytiersuite/delivery-tier-suite/delivery-tier-distribution`` —
   ``package -DskipTests`` when ``target/delivery-tier-distribution.jar``
   is missing (shipped DTS installer for ``qa-up --with-dts``; #6237).
7. ``modules/perc-distribution-tree`` — ``clean package -DskipTests``

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
from typing import Callable, List, Optional, Sequence

LOG = logging.getLogger("qa_rebuild_chain")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_SUBPROCESS_FAILED = 2

# Root license-maven-plugin aggregator goal. Must not be invoked with -N:
# non-recursive mode only loads the empty parent POM and never writes
# THIRD-PARTY-MAVEN.txt (#4420 / src/license/README.md).
LICENSE_AGGREGATE_GOAL = "license:aggregate-add-third-party"
LICENSE_INVENTORY_REL = (
    Path("target") / "generated-sources" / "license" / "THIRD-PARTY-MAVEN.txt"
)
SECURE_MEMBERSHIP_REL = (
    Path("deliverytiersuite") / "delivery-tier-suite" / "secure-membership"
)
COMMON_UI_BUNDLE_REL = Path("modules") / "perc-common-ui-bundle"
COMMON_UI_JS_REL = (
    Path("target")
    / "classes"
    / "META-INF"
    / "resources"
    / "cm"
    / "common"
    / "js"
    / "perc_common_ui.js"
)
# Shipped DTS installer assembly (maven-assembly ``jar-with-dependencies``,
# ``finalName`` delivery-tier-distribution) consumed by the ``--with-dts``
# QA cell (matrix ``dts`` product; spec 2402-qa-dts-publish, #6237).
DTS_DIST_REL = (
    Path("deliverytiersuite") / "delivery-tier-suite" / "delivery-tier-distribution"
)
DTS_DIST_JAR_NAME = "delivery-tier-distribution.jar"

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


def maven_inventory_path(repo_root: Path) -> Path:
    """Reactor-root Maven third-party inventory produced by license aggregate."""
    return (repo_root / LICENSE_INVENTORY_REL).resolve()


def maven_inventory_present(repo_root: Path) -> bool:
    """True when THIRD-PARTY-MAVEN.txt exists and is non-empty."""
    path = maven_inventory_path(repo_root)
    try:
        return path.is_file() and path.stat().st_size > 0
    except OSError:
        return False


def license_inventory_step() -> ChainStep:
    """Repo-root license aggregate. Never pass ``-N`` / ``--non-recursive``."""
    return ChainStep(
        label="qa-rebuild-license-inventory",
        module_rel=Path("."),
        goals=(LICENSE_AGGREGATE_GOAL,),
        extra_args=(),
    )


def secure_membership_dependency_dir(repo_root: Path) -> Path:
    """Directory populated by secure-membership ``dependency:copy-dependencies``."""
    return (repo_root / SECURE_MEMBERSHIP_REL / "target" / "dependency").resolve()


def _nonempty_dir(path: Path) -> bool:
    try:
        if not path.is_dir():
            return False
        next(path.iterdir())
        return True
    except OSError:
        return False
    except StopIteration:
        return False


def secure_membership_deps_present(repo_root: Path) -> bool:
    """True when ANT can copy secure-membership runtime jars + classes."""
    dep = secure_membership_dependency_dir(repo_root)
    classes = (repo_root / SECURE_MEMBERSHIP_REL / "target" / "classes").resolve()
    return _nonempty_dir(dep) and classes.is_dir()


def secure_membership_step() -> ChainStep:
    """Package perc-secure-membership so ``target/dependency`` exists for ANT."""
    return ChainStep(
        label="qa-rebuild-secure-membership",
        module_rel=SECURE_MEMBERSHIP_REL,
        goals=("package",),
        extra_args=("-DskipTests",),
    )


def common_ui_bundle_js_path(repo_root: Path) -> Path:
    """perc_common_ui.js ANT copies into the installer web_resources tree."""
    return (repo_root / COMMON_UI_BUNDLE_REL / COMMON_UI_JS_REL).resolve()


def common_ui_bundle_present(repo_root: Path) -> bool:
    """True when perc-common-ui-bundle already produced perc_common_ui.js."""
    path = common_ui_bundle_js_path(repo_root)
    try:
        return path.is_file() and path.stat().st_size > 0
    except OSError:
        return False


def common_ui_bundle_step() -> ChainStep:
    """Package perc-common-ui-bundle so ANT can copy perc_common_ui.js (#4695)."""
    return ChainStep(
        label="qa-rebuild-common-ui-bundle",
        module_rel=COMMON_UI_BUNDLE_REL,
        goals=("package",),
        extra_args=("-DskipTests",),
    )


def dts_distribution_jar_path(repo_root: Path) -> Path:
    """Shipped DTS installer jar the ``--with-dts`` QA cell mounts (#6237)."""
    return (
        repo_root / DTS_DIST_REL / "target" / DTS_DIST_JAR_NAME
    ).resolve()


def dts_distribution_present(repo_root: Path) -> bool:
    """True when the DTS installer assembly is already built and non-empty."""
    path = dts_distribution_jar_path(repo_root)
    try:
        return path.is_file() and path.stat().st_size > 0
    except OSError:
        return False


def dts_distribution_step() -> ChainStep:
    """Package delivery-tier-distribution so the DTS cell can install (#6237)."""
    return ChainStep(
        label="qa-rebuild-dts-distribution",
        module_rel=DTS_DIST_REL,
        goals=("package",),
        extra_args=("-DskipTests",),
    )


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
        When True, skip sitemanage/WebUI and only run the license
        inventory prereq plus ``perc-distribution-tree`` package. Use
        when only installer packaging resources changed and SNAPSHOT
        artifacts under the WAR are already fresh.
    """
    skip = ("-DskipTests",)
    dist_steps = [
        license_inventory_step(),
        secure_membership_step(),
        common_ui_bundle_step(),
        dts_distribution_step(),
        ChainStep(
            label="qa-rebuild-dist",
            module_rel=Path("modules") / "perc-distribution-tree",
            goals=("package",) if dist_only else ("clean", "package"),
            extra_args=skip,
        ),
    ]
    if dist_only:
        return dist_steps

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
        *dist_steps,
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


def _prepare_step_log(
    log_dir: Optional[Path], step_label: str, index: int
) -> Optional[Path]:
    """Create the ``<log_dir>/<step_label>-<index>.log`` path (mkdir -p)."""
    if log_dir is None:
        return None
    log_dir.mkdir(parents=True, exist_ok=True)
    return log_dir / f"{step_label}-{index}.log"


def _write_log_lines(log_path: Optional[Path], lines: Sequence[str]) -> None:
    """Write short planned/skip lines to a step log; no-op without a log."""
    if log_path is None:
        return
    with log_path.open("w", encoding="utf-8") as fh:
        for line in lines:
            fh.write(line + "\n")


def _fail_step(label: str, log_path: Optional[Path], message: str) -> None:
    """Report a failed step: stderr message plus a ``RESULT:FAIL`` line."""
    print(message, file=sys.stderr)
    _print_result(False, label, log_path)


def _license_argv_banned(step: ChainStep, argv: Sequence[str]) -> bool:
    """True when the license-inventory step would run with ``-N`` (#4420)."""
    return step.label == "qa-rebuild-license-inventory" and bool(
        {"-N", "--non-recursive"}.intersection(argv)
    )


def _report_missing_mvnw(mvnw: Path) -> None:
    """Report a missing repo-root Maven wrapper before any step runs."""
    print(
        f"ERROR: Maven wrapper not found at {mvnw} "
        f"(expected mvnw / mvnw.cmd at repo root)",
        file=sys.stderr,
    )
    _print_result(False, "qa-rebuild-chain")


# (label, present-check, reason-format) for steps the chain may skip when
# their outputs already exist. Table-driven so a new skippable step does
# not grow branch complexity in the per-step processor.
_SKIP_CHECKS: tuple[
    tuple[str, Callable[[Path], bool], Callable[[Path], str]], ...
] = (
    (
        "qa-rebuild-license-inventory",
        maven_inventory_present,
        lambda root: f"inventory-present PATH:{maven_inventory_path(root)}",
    ),
    (
        "qa-rebuild-secure-membership",
        secure_membership_deps_present,
        lambda root: (
            "secure-membership-deps-present "
            f"PATH:{secure_membership_dependency_dir(root)}"
        ),
    ),
    (
        "qa-rebuild-common-ui-bundle",
        common_ui_bundle_present,
        lambda root: (
            "common-ui-bundle-present " f"PATH:{common_ui_bundle_js_path(root)}"
        ),
    ),
    (
        "qa-rebuild-dts-distribution",
        dts_distribution_present,
        lambda root: (
            "dts-distribution-present "
            f"PATH:{dts_distribution_jar_path(root)}"
        ),
    ),
)


def skip_reason_for_step(step: ChainStep, repo_root: Path) -> Optional[str]:
    """Return the SKIP reason when a step's outputs already exist, else None."""
    for label, present, reason in _SKIP_CHECKS:
        if step.label == label and present(repo_root):
            return reason(repo_root)
    return None


def _report_step_skip(
    step: ChainStep, planned: str, log_path: Optional[Path], skip_reason: str
) -> None:
    """Report a skipped step: SKIP line plus a ``RESULT:OK`` line."""
    skip_line = f"SKIP STEP:{step.label} REASON:{skip_reason}"
    print(skip_line)
    LOG.info(skip_line)
    _write_log_lines(log_path, [planned, skip_line])
    _print_result(True, step.label, log_path)


def _report_dry_run_step(
    step: ChainStep, planned: str, log_path: Optional[Path]
) -> None:
    """Report a dry-run step: short planned log plus a ``RESULT:OK`` line."""
    _write_log_lines(log_path, [planned])
    _print_result(True, step.label, log_path)


def _invoke_maven_runner(
    runner: RunFn,
    argv: Sequence[str],
    cwd: Path,
    log_path: Optional[Path],
    timeout_seconds: Optional[int],
) -> subprocess.CompletedProcess:
    """Invoke Maven for one step; the log variant tees stdout+stderr to disk."""
    if log_path is not None:
        with log_path.open("w", encoding="utf-8") as fh:
            return runner(
                list(argv),
                cwd=str(cwd),
                shell=False,
                check=False,
                stdout=fh,
                stderr=subprocess.STDOUT,
                timeout=timeout_seconds,
            )
    return runner(
        list(argv),
        cwd=str(cwd),
        shell=False,
        check=False,
        timeout=timeout_seconds,
    )


def _execute_real_step(
    *,
    runner: RunFn,
    step: ChainStep,
    cwd: Path,
    argv: Sequence[str],
    log_path: Optional[Path],
    timeout_seconds: Optional[int],
) -> bool:
    """Run one non-skipped Maven step. Returns True on success (reported)."""
    LOG.info("Running: %s (cwd=%s)", " ".join(argv), cwd)
    try:
        completed = _invoke_maven_runner(
            runner, argv, cwd, log_path, timeout_seconds
        )
    except subprocess.TimeoutExpired:
        _fail_step(
            step.label,
            log_path,
            f"ERROR: step {step.label} timed out after {timeout_seconds}s",
        )
        return False
    except OSError as exc:
        _fail_step(
            step.label,
            log_path,
            f"ERROR: failed to start Maven for {step.label}: {exc}",
        )
        return False
    if completed.returncode != 0:
        _fail_step(
            step.label,
            log_path,
            f"ERROR: step {step.label} exited {completed.returncode}",
        )
        return False
    _print_result(True, step.label, log_path)
    return True


def _process_chain_step(
    *,
    step: ChainStep,
    index: int,
    repo_root: Path,
    mvnw: Path,
    log_dir: Optional[Path],
    dry_run: bool,
    runner: RunFn,
    timeout_seconds: Optional[int],
) -> bool:
    """Process one chain step (announce -> guard -> dry-run/skip/run).

    Returns True when the chain may continue, False on failure (already
    reported via ``RESULT:FAIL``).
    """
    planned = _format_planned(step, repo_root, mvnw)
    print(planned)
    LOG.info(planned)
    cwd = step.cwd(repo_root)
    argv = step.argv(mvnw)
    log_path = _prepare_step_log(log_dir, step.label, index)
    if _license_argv_banned(step, argv):
        _fail_step(
            step.label,
            log_path,
            "ERROR: license aggregate must not use -N / --non-recursive "
            "(empty root POM; THIRD-PARTY-MAVEN.txt is never written)",
        )
        return False
    if dry_run:
        _report_dry_run_step(step, planned, log_path)
        return True
    if not cwd.is_dir():
        _fail_step(
            step.label,
            log_path,
            f"ERROR: module directory missing for {step.label}: {cwd}",
        )
        return False
    skip_reason = skip_reason_for_step(step, repo_root)
    if skip_reason is not None:
        _report_step_skip(step, planned, log_path, skip_reason)
        return True
    return _execute_real_step(
        runner=runner,
        step=step,
        cwd=cwd,
        argv=argv,
        log_path=log_path,
        timeout_seconds=timeout_seconds,
    )


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
        _report_missing_mvnw(mvnw)
        return EXIT_INVOCATION

    overall_ok = True
    for index, step in enumerate(steps):
        if not _process_chain_step(
            step=step,
            index=index,
            repo_root=repo_root,
            mvnw=mvnw,
            log_dir=log_dir,
            dry_run=dry_run,
            runner=runner,
            timeout_seconds=timeout_seconds,
        ):
            overall_ok = False
            break

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
            "WebUI package → license:aggregate-add-third-party (no -N) → "
            "secure-membership package → "
            "perc-common-ui-bundle package (#4695) → "
            "delivery-tier-distribution package (#6237) → "
            "perc-distribution-tree package (#2533 / #4420). "
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
            "Skip sitemanage/WebUI. Still generate the Maven license "
            "inventory when missing, then package "
            "perc-distribution-tree (when SNAPSHOT WAR inputs are already "
            "fresh)."
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
