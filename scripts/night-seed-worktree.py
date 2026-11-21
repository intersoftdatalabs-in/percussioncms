#!/usr/bin/env python3
"""Seed gitignored AGENTS.local.md files into a git worktree.

Root (and nested module) ``AGENTS.local.md`` files are gitignored, so
``git worktree add`` and ``git reset --hard`` never carry them over.
Night-issue-prs lane/shard worktrees used to miss the operator's GH
identity and signing settings unless a post-checkout hook was installed.
This helper copies those files one way, primary → worktree, so a runner
on another machine only needs its own ``AGENTS.local.md`` in the primary
checkout — no git hook required.

## Behavioral Notes

- Cross-platform Python 3.9+ per ``scripts/README.md`` (spec 994). No
  ``shell=True``, ``os.system``, or hardcoded path separators.
- Primary checkout is ``dirname(git rev-parse --path-format=absolute
  --git-common-dir)`` when that path ends in ``.git``.
- Copies every ``AGENTS.local.md`` under the primary (root and nested),
  skipping ``.git``, other worktrees checked out inside the primary, and
  common generated directories.
- One way only: never copies back. Default is seed-if-missing (does not
  overwrite). ``overwrite=True`` refreshes from the primary, which is
  what the night launcher uses on each sync.
- Missing source is a silent no-op. Never prints file contents — only
  relative paths and worktree/primary locations.
- ``--check`` seeds if missing, then exits 2 if the primary still has a
  file the worktree does not, so a seat can stop before committing.

## Usage

    python3 scripts/night-seed-worktree.py <worktree>
    python3 scripts/night-seed-worktree.py --check <worktree>
    python3 scripts/night-seed-worktree.py --overwrite <worktree>
"""
from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

AGENTS_LOCAL_NAME = "AGENTS.local.md"

# Directories that are never a source of operator overrides and are
# expensive to walk in this monorepo.
_SKIP_DIRS = frozenset(
    {
        ".git",
        "node_modules",
        "target",
        "__pycache__",
        ".venv",
        "venv",
        "dist",
        "build",
        ".tox",
        ".m2",
        "tmp",
        "scratch",
    }
)

# --check: primary has AGENTS.local.md but the worktree still does not.
EXIT_CHECK_FAILED = 2


def resolve_primary_checkout(repo: Path) -> Path:
    """Return the primary checkout that owns this repo or worktree.

    Uses ``git rev-parse --path-format=absolute --git-common-dir``. When
    the common dir is ``<primary>/.git``, the parent is the checkout.
    """
    repo = Path(repo)
    completed = subprocess.run(
        ["git", "rev-parse", "--path-format=absolute", "--git-common-dir"],
        cwd=str(repo),
        shell=False,
        check=True,
        timeout=30,
        capture_output=True,
        text=True,
    )
    common = Path(completed.stdout.strip())
    if not common.is_absolute():
        common = (repo / common).resolve()
    if common.name == ".git":
        return common.parent
    return common


def iter_agents_local(primary: Path) -> list[Path]:
    """Relative paths of ``AGENTS.local.md`` under the primary checkout."""
    primary = Path(primary)
    if not primary.is_dir():
        return []
    found: list[Path] = []
    for dirpath, dirnames, filenames in os.walk(primary, followlinks=False):
        current = Path(dirpath)
        dirnames[:] = [
            name
            for name in dirnames
            if name not in _SKIP_DIRS and not (current / name / ".git").is_file()
        ]
        if AGENTS_LOCAL_NAME in filenames:
            path = current / AGENTS_LOCAL_NAME
            if path.is_file():
                found.append(path.relative_to(primary))
    found.sort()
    return found


def seed_agents_local(
    worktree: Path,
    *,
    primary: Path | None = None,
    overwrite: bool = False,
) -> list[Path]:
    """Copy ``AGENTS.local.md`` files one way, primary → worktree.

    Returns the relative paths that were copied. Existing destinations are
    left alone unless ``overwrite`` is true. Never copies back, never
    prints file contents. No-op when the primary has no such files.
    """
    worktree = Path(worktree)
    if primary is None:
        primary = resolve_primary_checkout(worktree)
    else:
        primary = Path(primary)

    copied: list[Path] = []
    for rel in iter_agents_local(primary):
        source = primary / rel
        dest = worktree / rel
        if dest.exists() and not overwrite:
            continue
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, dest)
        copied.append(rel)
    return copied


def missing_agents_local(worktree: Path, primary: Path) -> list[Path]:
    """Relative paths present in the primary but still absent in the worktree."""
    missing: list[Path] = []
    for rel in iter_agents_local(primary):
        if not (Path(worktree) / rel).is_file():
            missing.append(rel)
    return missing


def _rel_display(rel: Path) -> str:
    return rel.as_posix()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="night-seed-worktree.py",
        description=(
            "Copy gitignored AGENTS.local.md from the primary checkout "
            "into a git worktree (one way). Never prints file contents."
        ),
    )
    parser.add_argument(
        "worktree",
        type=Path,
        help="Worktree path to seed (must already exist).",
    )
    parser.add_argument(
        "--primary",
        type=Path,
        default=None,
        help="Override the primary checkout (default: git-common-dir parent).",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Refresh existing copies from the primary (launcher sync).",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help=(
            "Seed if missing, then exit 2 if the primary still has "
            "AGENTS.local.md that the worktree does not."
        ),
    )
    args = parser.parse_args(argv)

    worktree = args.worktree
    if not worktree.exists():
        print(f"night-seed-worktree: worktree does not exist: {worktree}", file=sys.stderr)
        return 1

    try:
        primary = args.primary if args.primary is not None else resolve_primary_checkout(worktree)
    except (OSError, subprocess.SubprocessError) as exc:
        print(f"night-seed-worktree: cannot resolve primary checkout: {exc}", file=sys.stderr)
        return 1

    try:
        copied = seed_agents_local(worktree, primary=primary, overwrite=args.overwrite)
    except OSError as exc:
        print(f"night-seed-worktree: copy failed: {exc}", file=sys.stderr)
        return 1

    sources = iter_agents_local(primary)
    if copied:
        for rel in copied:
            print(f"seeded: {_rel_display(rel)}")
    elif not sources:
        print(f"night-seed-worktree: no {AGENTS_LOCAL_NAME} in primary {primary}")
    else:
        print(
            f"night-seed-worktree: already present "
            f"({len(sources)} file(s)) in {worktree}"
        )

    if args.check:
        missing = missing_agents_local(worktree, primary)
        if missing:
            names = ", ".join(_rel_display(rel) for rel in missing)
            print(
                f"night-seed-worktree: STOP — primary has {AGENTS_LOCAL_NAME} "
                f"but worktree is still missing: {names}",
                file=sys.stderr,
            )
            return EXIT_CHECK_FAILED
    return 0


if __name__ == "__main__":
    sys.exit(main())
