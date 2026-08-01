#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Prune stale git worktrees for the Percussion CMS monorepo.

Cross-platform (Windows / Linux / macOS). Requires:
  - Python 3.9+
  - ``git`` on PATH
  - ``gh`` CLI authenticated when classifying by GitHub PR state

Why this exists
---------------
Agent sessions (Kilo, Grok, etc.) often create full-tree git worktrees under
``.kilo/worktrees/``, ``~/.grok/worktrees/``, or similar. Each copy is roughly
a full checkout and fills disks quickly when left behind after PRs merge.

Default behaviour is **dry-run** (list only). Pass ``--apply`` to remove.

Stale criteria (removable when ``--apply``):
  * Linked branch has a GitHub PR in state MERGED or CLOSED, OR
  * Linked branch has no open PR and ``--include-no-pr`` is set

Always kept:
  * The primary (main) worktree
  * The caller's current worktree (cwd)
  * Any worktree whose branch has an **open** PR (unless ``--include-open``)

See root ``AGENTS.md`` → **Git worktree hygiene (HARD GATE)**.
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Iterable, Optional


# ---------------------------------------------------------------------------
# Data
# ---------------------------------------------------------------------------


@dataclass
class Worktree:
    path: Path
    head: str = ""
    branch: Optional[str] = None  # None => detached
    locked: bool = False
    pr_states: list[str] = field(default_factory=list)  # e.g. ["MERGED"], ["OPEN"]
    pr_numbers: list[int] = field(default_factory=list)
    dirty: bool = False
    is_main: bool = False
    is_cwd: bool = False

    @property
    def has_open_pr(self) -> bool:
        return any(s == "OPEN" for s in self.pr_states)

    @property
    def has_terminal_pr(self) -> bool:
        """True if every known PR is MERGED or CLOSED (and at least one exists)."""
        if not self.pr_states:
            return False
        return all(s in ("MERGED", "CLOSED") for s in self.pr_states)

    @property
    def no_pr(self) -> bool:
        return not self.pr_states


@dataclass
class Decision:
    worktree: Worktree
    action: str  # keep | remove
    reason: str


# ---------------------------------------------------------------------------
# Git / gh helpers
# ---------------------------------------------------------------------------


def run(
    args: list[str],
    *,
    cwd: Optional[Path] = None,
    check: bool = False,
) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        args,
        cwd=str(cwd) if cwd else None,
        text=True,
        capture_output=True,
        check=check,
    )


def repo_root_from_cwd() -> Path:
    cp = run(["git", "rev-parse", "--show-toplevel"])
    if cp.returncode != 0:
        raise SystemExit(
            f"error: not inside a git work tree\n{cp.stderr.strip()}"
        )
    return Path(cp.stdout.strip()).resolve()


def parse_worktree_porcelain(text: str) -> list[Worktree]:
    """Parse ``git worktree list --porcelain`` output into Worktree rows."""
    rows: list[Worktree] = []
    current: Optional[Worktree] = None
    for raw in text.splitlines():
        line = raw.rstrip("\n")
        if not line:
            if current is not None:
                rows.append(current)
                current = None
            continue
        if line.startswith("worktree "):
            if current is not None:
                rows.append(current)
            current = Worktree(path=Path(line[len("worktree ") :]))
        elif current is None:
            continue
        elif line.startswith("HEAD "):
            current.head = line[len("HEAD ") :]
        elif line.startswith("branch "):
            ref = line[len("branch ") :]
            prefix = "refs/heads/"
            current.branch = ref[len(prefix) :] if ref.startswith(prefix) else ref
        elif line == "detached":
            current.branch = None
        elif line.startswith("locked"):
            current.locked = True
    if current is not None:
        rows.append(current)
    return rows


def list_worktrees(root: Path) -> list[Worktree]:
    cp = run(["git", "worktree", "list", "--porcelain"], cwd=root)
    if cp.returncode != 0:
        raise SystemExit(f"error: git worktree list failed\n{cp.stderr.strip()}")
    rows = parse_worktree_porcelain(cp.stdout)
    if not rows:
        return rows
    # First entry is the main worktree.
    rows[0].is_main = True
    cwd = Path.cwd().resolve()
    for wt in rows:
        try:
            resolved = wt.path.resolve()
        except OSError:
            resolved = wt.path
        try:
            wt.is_cwd = resolved == cwd or cwd == resolved or (
                hasattr(cwd, "is_relative_to") and cwd.is_relative_to(resolved)
            )
        except (ValueError, OSError):
            wt.is_cwd = resolved == cwd
        # dirty check (best-effort; skip if path missing)
        if resolved.is_dir():
            st = run(["git", "status", "--porcelain"], cwd=resolved)
            wt.dirty = bool(st.stdout.strip()) if st.returncode == 0 else False
    return rows


def gh_prs_for_branch(branch: str, repo: Optional[str]) -> list[dict[str, Any]]:
    """Return PR dicts for head branch (all states). Empty on failure."""
    args = [
        "gh",
        "pr",
        "list",
        "--state",
        "all",
        "--head",
        branch,
        "--limit",
        "20",
        "--json",
        "number,state,title,url,mergedAt",
    ]
    if repo:
        args.extend(["--repo", repo])
    cp = run(args)
    if cp.returncode != 0:
        # Network / auth failure: treat as unknown (no auto-remove without PR data)
        return []
    try:
        data = json.loads(cp.stdout or "[]")
    except json.JSONDecodeError:
        return []
    return data if isinstance(data, list) else []


def attach_pr_state(worktrees: Iterable[Worktree], repo: Optional[str]) -> None:
    for wt in worktrees:
        if not wt.branch:
            continue
        prs = gh_prs_for_branch(wt.branch, repo)
        wt.pr_numbers = [int(p["number"]) for p in prs if "number" in p]
        wt.pr_states = [str(p.get("state", "")).upper() for p in prs]


# ---------------------------------------------------------------------------
# Policy
# ---------------------------------------------------------------------------


def decide(
    wt: Worktree,
    *,
    include_no_pr: bool,
    include_open: bool,
    require_force_for_dirty: bool,
    force: bool,
) -> Decision:
    if wt.is_main:
        return Decision(wt, "keep", "main worktree")
    if wt.is_cwd:
        return Decision(wt, "keep", "current working directory worktree")
    if wt.locked:
        return Decision(wt, "keep", "locked worktree")
    if wt.branch is None:
        return Decision(wt, "keep", "detached HEAD (manual review)")
    if wt.has_open_pr and not include_open:
        nums = ",".join(f"#{n}" for n in wt.pr_numbers) or "?"
        return Decision(wt, "keep", f"open PR {nums}")
    if wt.has_terminal_pr:
        nums = ",".join(f"#{n}" for n in wt.pr_numbers) or "?"
        states = ",".join(sorted(set(wt.pr_states)))
        if wt.dirty and require_force_for_dirty and not force:
            return Decision(
                wt,
                "keep",
                f"stale PR {nums} ({states}) but dirty; re-run with --force",
            )
        return Decision(wt, "remove", f"PR {nums} is {states}")
    if wt.no_pr:
        if not include_no_pr:
            return Decision(wt, "keep", "no PR linked; pass --include-no-pr to remove")
        if wt.dirty and require_force_for_dirty and not force:
            return Decision(
                wt,
                "keep",
                "no PR, dirty; re-run with --force --include-no-pr",
            )
        return Decision(wt, "remove", "no open PR (--include-no-pr)")
    # Mixed OPEN+MERGED etc.
    if wt.has_open_pr:
        return Decision(wt, "keep", "has open PR among linked PRs")
    return Decision(wt, "keep", "unknown PR state; left in place")


def remove_worktree(
    root: Path,
    wt: Worktree,
    *,
    force: bool,
    delete_branch: bool,
) -> tuple[bool, str]:
    args = ["git", "worktree", "remove"]
    if force or wt.dirty:
        args.append("--force")
    args.append(str(wt.path))
    cp = run(args, cwd=root)
    if cp.returncode != 0:
        return False, (cp.stderr or cp.stdout or "git worktree remove failed").strip()
    msg = f"removed worktree {wt.path}"
    if delete_branch and wt.branch:
        b = run(["git", "branch", "-D", wt.branch], cwd=root)
        if b.returncode == 0:
            msg += f"; deleted local branch {wt.branch}"
        else:
            msg += f"; branch -D {wt.branch} skipped: {(b.stderr or b.stdout).strip()}"
    return True, msg


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="List or remove stale git worktrees (merged/closed PR branches).",
    )
    p.add_argument(
        "--apply",
        action="store_true",
        help="Actually remove worktrees (default is dry-run).",
    )
    p.add_argument(
        "--force",
        action="store_true",
        help="Allow removing dirty worktrees (passes git worktree remove --force).",
    )
    p.add_argument(
        "--delete-local-branches",
        action="store_true",
        help="After remove, delete the local branch (git branch -D).",
    )
    p.add_argument(
        "--include-no-pr",
        action="store_true",
        help="Also remove worktrees whose branch has no GitHub PR.",
    )
    p.add_argument(
        "--include-open",
        action="store_true",
        help="Allow removing worktrees that still have an open PR (dangerous).",
    )
    p.add_argument(
        "--repo",
        default=None,
        help="GitHub owner/name if not inferred by gh (e.g. intersoftdatalabs-in/percussioncms).",
    )
    p.add_argument(
        "--skip-gh",
        action="store_true",
        help="Do not call gh; only list worktrees (remove disabled unless --include-no-pr).",
    )
    return p


def main(argv: Optional[list[str]] = None) -> int:
    args = build_parser().parse_args(argv)
    root = repo_root_from_cwd()
    worktrees = list_worktrees(root)

    if not args.skip_gh:
        attach_pr_state(worktrees, args.repo)
    elif args.apply and not args.include_no_pr:
        print(
            "error: --skip-gh with --apply requires --include-no-pr "
            "(cannot classify PR state)",
            file=sys.stderr,
        )
        return 2

    decisions = [
        decide(
            wt,
            include_no_pr=args.include_no_pr,
            include_open=args.include_open,
            require_force_for_dirty=True,
            force=args.force,
        )
        for wt in worktrees
    ]

    print(f"Repo root: {root}")
    print(f"Mode: {'APPLY' if args.apply else 'DRY-RUN'}")
    print()
    print(f"{'ACTION':<8} {'BRANCH':<48} {'DIRTY':<6} REASON")
    print("-" * 100)
    for d in decisions:
        branch = d.worktree.branch or "(detached)"
        dirty = "yes" if d.worktree.dirty else "no"
        print(f"{d.action:<8} {branch:<48} {dirty:<6} {d.reason}")
        print(f"         path: {d.worktree.path}")

    to_remove = [d for d in decisions if d.action == "remove"]
    print()
    print(f"Keep: {sum(1 for d in decisions if d.action == 'keep')}  "
          f"Remove: {len(to_remove)}")

    if not args.apply:
        print("\nDry-run only. Re-run with --apply [--force] to remove.")
        return 0

    failures = 0
    for d in to_remove:
        ok, msg = remove_worktree(
            root,
            d.worktree,
            force=args.force,
            delete_branch=args.delete_local_branches,
        )
        print(("OK  " if ok else "FAIL") + " " + msg)
        if not ok:
            failures += 1

    prune = run(["git", "worktree", "prune", "-v"], cwd=root)
    if prune.stdout.strip():
        print(prune.stdout.strip())
    if prune.stderr.strip():
        print(prune.stderr.strip())

    print("\nRemaining worktrees:")
    remaining = run(["git", "worktree", "list"], cwd=root)
    print(remaining.stdout.rstrip() or "(none)")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
