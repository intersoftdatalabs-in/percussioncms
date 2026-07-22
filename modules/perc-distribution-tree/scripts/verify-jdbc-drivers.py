#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Verify the assembled Percussion distribution artifact ships a valid JDBC driver set.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Behavior parity with ``modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh``
(FR-002). The build-time gate is the canonical Java main
``com.percussion.distribution.install.VerifyJdbcDrivers`` invoked by the
``exec-maven-plugin:java`` goal in ``modules/perc-distribution-tree/pom.xml``;
this Python port is the cross-platform operator-facing entry point.

Exit codes match the original POSIX script (and the Java main) exactly so any
tooling that observes them keeps working:

  0  all checks passed
  1  invocation error / missing tool / artifact not found
  2  ``jetty/base/lib/jdbc/`` missing or empty
  3  one or more JARs are zero-byte
  4  one or more JARs are not valid Java archives
  5  artifact could not be unpacked
  6  ``--expected-driver-set`` / ``--expected-driver-glob`` does not match

## Behavioral Notes (FR-009b)

- The original POSIX script used ``trap cleanup EXIT`` to remove the scratch
  workdir on exit. The Python port uses ``try``/``finally`` with the same
  semantics (R2). On Windows the POSIX tempdir semantics differ subtly
  (no sticky-bit semantics; ``tempfile.TemporaryDirectory`` is portable).
- The original used ``unzip``/``stat``/``find`` from ``$PATH``. The Python
  port uses ``zipfile`` (for both unpacking and ``testzip`` validation) and
  ``Path.stat().st_size`` for size — all stdlib, no external tools.
- The original used ``bash``-style globbing for ``--expected-driver-glob``
  (and explicitly shellcheck-disabled ``SC2086`` on the expansion). The
  Python port uses ``fnmatch.fnmatch`` against ``Path.name`` — same observable
  behavior, no shell-word-splitting footguns.
- Path discovery uses ``Path(__file__).resolve().parents[N]`` for the repo
  root and module root (R7); no hardcoded separators.

Does not write to ``%TEMP%`` / ``$TMPDIR`` — scratch workdir defaults to
``tempfile.TemporaryDirectory()`` under the OS-default temp location (per
portable API; the repo ``tmp/`` convention is honored when ``--workdir`` is
explicit).
"""

from __future__ import annotations

import argparse
import fnmatch
import logging
import sys
import tempfile
import zipfile
from pathlib import Path
from typing import Iterable, List, Optional, Sequence

LOG = logging.getLogger("verify-jdbc-drivers")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_MISSING_OR_EMPTY = 2
EXIT_ZERO_BYTE = 3
EXIT_INVALID_JAR = 4
EXIT_UNPACK_FAILED = 5
EXIT_EXPECTED_MISSING = 6

# Candidate jdbc/ locations inside the unpacked distribution. Older builds
# placed the directory under ``distribution/``; current builds use the bare
# ``jetty/`` path. Both are tried in order.
JDBC_DIR_CANDIDATES: tuple[str, ...] = (
    "jetty/base/lib/jdbc",
    "distribution/jetty/base/lib/jdbc",
)


def _split_csv(raw: str) -> List[str]:
    """Split a comma-separated value into a list of stripped, non-empty items."""
    return [item.strip() for item in raw.split(",") if item.strip()]


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="verify-jdbc-drivers.py",
        description=(
            "Verify the assembled Percussion distribution artifact ships a valid "
            "JDBC driver set under jetty/base/lib/jdbc/."
        ),
    )
    p.add_argument(
        "--artifact",
        type=Path,
        default=None,
        help=(
            "Path to perc-distribution-tree.jar "
            "(default: modules/perc-distribution-tree/target/perc-distribution-tree.jar "
            "resolved from the script location)."
        ),
    )
    p.add_argument(
        "--workdir",
        type=Path,
        default=None,
        help="Scratch directory for unpacking (default: a portable tempfile.TemporaryDirectory).",
    )
    p.add_argument(
        "--expected-driver-set",
        type=_split_csv,
        default=None,
        help=(
            "Comma-separated exact driver filenames that must be present "
            "(default: empty = any non-empty valid set is acceptable)."
        ),
    )
    p.add_argument(
        "--expected-driver-glob",
        type=_split_csv,
        default=None,
        help=(
            "Comma-separated globs; for each glob at least one matching JAR "
            "must be present under jetty/base/lib/jdbc/. Version-resilient."
        ),
    )
    return p


def _default_artifact(script_path: Path) -> Path:
    """Resolve the default artifact path from the script location.

    scripts/ lives at ``modules/perc-distribution-tree/scripts/``; the owning
    module is one level up. The artifact path is two levels up from scripts/.
    """
    module_dir = script_path.resolve().parent.parent
    return module_dir / "target" / "perc-distribution-tree.jar"


def _find_jdbc_dir(dist_root: Path) -> Optional[Path]:
    """Return the first existing candidate jdbc/ directory, or None."""
    for candidate in JDBC_DIR_CANDIDATES:
        path = dist_root / candidate
        if path.is_dir():
            return path
    return None


def _enumerate_jars(jdbc_dir: Path) -> List[Path]:
    """Return all .jar files in ``jdbc_dir`` (sorted by name for stable output)."""
    if not jdbc_dir.is_dir():
        return []
    return sorted(p for p in jdbc_dir.iterdir() if p.is_file() and p.suffix.lower() == ".jar")


def _is_valid_jar(path: Path) -> bool:
    """Return True if ``path`` is a non-empty file with a valid zip header and at
    least one readable entry. Uses ``zipfile.is_zipfile`` plus an open round-trip
    to catch half-written jars that pass the magic-bytes sniff but corrupt on read.
    """
    if not path.is_file() or path.stat().st_size == 0:
        return False
    if not zipfile.is_zipfile(path):
        return False
    try:
        with zipfile.ZipFile(path) as zf:
            bad = zf.testzip()
            return bad is None
    except (zipfile.BadZipFile, OSError):
        return False


def _safe_extract(zf: zipfile.ZipFile, target: Path) -> None:
    """Extract every member of ``zf`` under ``target`` after asserting that
    the resolved path is contained within ``target``. Raises
    ``ValueError`` on any member whose name escapes ``target``.

    Background: ``zipfile.ZipFile.extractall`` only installs the
    ``filter`` / ``members`` safety check by default in Python 3.12+ (PEP
    706). The script supports Python 3.9+ per FR-002, so we do the check
    manually on the older interpreters. Without this guard, an artifact
    containing a member like ``jetty/base/lib/jdbc/../../tmp/evil.py``
    would write outside ``target`` on Python 3.9 / 3.10 / 3.11.
    """
    resolved_target = target.resolve()
    for member in zf.infolist():
        member_path = (resolved_target / member.filename).resolve()
        # ``is_relative_to`` was added in Python 3.9 — exactly the floor
        # this script promises to support, so it is always available.
        if not member_path.is_relative_to(resolved_target):
            raise ValueError(
                f"zip member escapes target directory: {member.filename!r} "
                f"resolves to {member_path}"
            )
    zf.extractall(resolved_target)


def verify(
    *,
    artifact: Path,
    workdir: Optional[Path],
    expected_set: Optional[Sequence[str]],
    expected_globs: Optional[Sequence[str]],
    cleanup_workdir: bool,
) -> int:
    """Run the full verification and return the exit code. Does not call
    ``sys.exit`` — callers (the CLI main and pytest tests) decide what to do.
    """
    if not artifact.is_file():
        LOG.error("ERROR: artifact not found: %s", artifact)
        return EXIT_INVOCATION

    if workdir is None:
        workdir_ctx: Optional[tempfile.TemporaryDirectory] = tempfile.TemporaryDirectory()
        workdir_path = Path(workdir_ctx.name)
        owns_tempdir = True
    else:
        workdir_path = workdir
        try:
            workdir_path.mkdir(parents=True, exist_ok=True)
        except OSError as exc:
            LOG.error("ERROR: cannot create workdir: %s (%s)", workdir, exc)
            return EXIT_INVOCATION
        workdir_ctx = None
        owns_tempdir = False

    try:
        dist_root = workdir_path / "dist"
        dist_root.mkdir(parents=True, exist_ok=True)

        # Unpack the artifact. zipfile.extractall raises BadZipFile / OSError on
        # truncated or unreadable archives; map both to EXIT_UNPACK_FAILED to
        # match the shell script's `if ! unzip -q ... ; then exit 5` semantics.
        #
        # Path-traversal guard (PEP 706 — Python 3.12+ default behavior):
        # Python 3.9-3.11 don't defend against members whose joined path
        # escapes ``dist_root`` (e.g. ``../etc/passwd``). Filter explicitly
        # so this script is safe on the entire ``requires-python = ">=3.9"``
        # range promised in the module docstring.
        try:
            with zipfile.ZipFile(artifact) as zf:
                _safe_extract(zf, dist_root)
        except (zipfile.BadZipFile, OSError, ValueError) as exc:
            LOG.error("ERROR: failed to unpack artifact: %s (%s)", artifact, exc)
            return EXIT_UNPACK_FAILED

        jdbc_dir = _find_jdbc_dir(dist_root)
        if jdbc_dir is None:
            tried = ", ".join(str(dist_root / c) for c in JDBC_DIR_CANDIDATES)
            LOG.error(
                "ERROR: jdbc directory missing: jetty/base/lib/jdbc/ (also tried %s)",
                tried,
            )
            return EXIT_MISSING_OR_EMPTY

        jars = _enumerate_jars(jdbc_dir)
        if not jars:
            LOG.error("ERROR: no JARs found under %s", jdbc_dir)
            return EXIT_MISSING_OR_EMPTY

        zero_byte: List[str] = []
        invalid: List[str] = []
        for jar in jars:
            size = jar.stat().st_size
            name = jar.name
            if size == 0:
                LOG.error("  [FAIL] %s — zero bytes", name)
                zero_byte.append(name)
                continue
            if not _is_valid_jar(jar):
                LOG.error("  [FAIL] %s — not a valid JAR", name)
                invalid.append(name)
                continue
            LOG.info("  [ OK ] %s — %d bytes", name, size)

        if zero_byte:
            LOG.error("ERROR: %d zero-byte JAR(s) found", len(zero_byte))
            return EXIT_ZERO_BYTE
        if invalid:
            LOG.error("ERROR: %d invalid JAR(s) found", len(invalid))
            return EXIT_INVALID_JAR

        present_names = {jar.name for jar in jars}

        if expected_set:
            missing = [name for name in expected_set if name not in present_names]
            if missing:
                LOG.error(
                    "ERROR: expected driver(s) missing from %s: %s",
                    jdbc_dir,
                    " ".join(missing),
                )
                return EXIT_EXPECTED_MISSING

        if expected_globs:
            unmatched = [
                pat
                for pat in expected_globs
                if not any(fnmatch.fnmatch(name, pat) for name in present_names)
            ]
            if unmatched:
                LOG.error(
                    "ERROR: no JAR matched any of expected driver globs: %s",
                    " ".join(unmatched),
                )
                return EXIT_EXPECTED_MISSING

        LOG.info(
            "OK: %d JDBC driver JAR(s) verified under %s",
            len(jars),
            jdbc_dir,
        )
        return EXIT_OK
    finally:
        if cleanup_workdir and owns_tempdir and workdir_ctx is not None:
            workdir_ctx.cleanup()
        elif owns_tempdir and workdir_ctx is not None and not cleanup_workdir:
            # Caller asked us to keep it (e.g. for debugging) but tempfile gave
            # ownership to us — release without removing.
            try:
                workdir_ctx._finalizer.detach()  # type: ignore[attr-defined]
            except AttributeError:
                # Python 3.13+ may rename; best-effort no-op if so.
                pass


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)

    artifact = args.artifact or _default_artifact(Path(__file__))
    return verify(
        artifact=artifact.resolve(),
        workdir=args.workdir.resolve() if args.workdir else None,
        expected_set=args.expected_driver_set,
        expected_globs=args.expected_driver_glob,
        cleanup_workdir=True,
    )


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())