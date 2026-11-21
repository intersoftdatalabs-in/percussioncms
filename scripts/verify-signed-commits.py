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
# See the License for the specific language governing permissions and
# limitations under the License.
"""Pre-push gate: every commit in ``<base>..HEAD`` must be signed and attributed (#6809).

Purpose
-------
Night-cycle follow-up commits on #6769 / #6770 were authored as
``opencode@local`` and unsigned, so the ``required_signatures`` branch
ruleset blocked ``gh pr merge --squash``. The work step produces signed,
maintainer-attributed commits; the follow-up / rebase path did not. This
gate fails closed on both halves of that defect:

1. ``check`` — fail when any commit in ``<base>..<head>`` has an
   unattributed author/committer identity or no **valid** (``G``) signature.
   Run before every push from both the work step and the follow-up step.
2. ``ensure-identity`` — fail when the effective git identity that the next
   commit/rebase would use is a placeholder (``opencode@local``,
   ``mkd-agent@localhost``, bare ``root@localhost``, ``@example.com``, ...),
   is empty, or has signing disabled. ``--apply-from <repo>`` copies the
   work step's identity/signing config (``user.name``, ``user.email``,
   ``commit.gpgsign``, ``user.signingkey``, ``gpg.format``) into the current
   repo so the follow-up/rebase path uses the **same** config as the work
   step. Rebase with ``git rebase --gpg-sign`` and commit with ``git commit
   -S`` afterwards — copying the identity alone does not sign anything.

Usage
-----
::

    # Pre-push check (default subcommand is ``check``):
    python3 scripts/verify-signed-commits.py [--base origin/main] [--head HEAD]
    python3 scripts/verify-signed-commits.py check --base origin/main

    # Identity gate before rebasing / committing in a follow-up worktree:
    python3 scripts/verify-signed-commits.py ensure-identity [--repo PATH]
    python3 scripts/verify-signed-commits.py ensure-identity --apply-from "$NIGHT_WORKTREE_PATH"

Windows::

    python scripts\\verify-signed-commits.py [check ... | ensure-identity ...]

Exit codes
----------
- ``check``: ``0`` every in-range commit is signed + attributed (or the
  range is empty); ``1`` at least one violation; ``2`` git/usage error.
- ``ensure-identity``: ``0`` identity + signing config are usable; ``1``
  placeholder/disabled identity (or ``--apply-from`` could not fix it);
  ``2`` git/usage error.

Portable: Python 3.9+; ``pathlib`` only, ``subprocess`` with ``shell=False``,
no OS temp hardcodes, no shell-isms.
"""
from __future__ import annotations

import argparse
import logging
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

# Separator-safe log format pieces. %x1f = unit separator, %x1e = record separator.
LOG_FORMAT = "%H%x1f%an%x1f%ae%x1f%cn%x1f%ce%x1f%G?%x1f%GS%x1e"

#: Exact placeholder author/committer emails that must never ship (lowercased).
DENIED_EMAILS_EXACT = frozenset(
    {
        "opencode@local",
        "mkd-agent@localhost",
        "root@localhost",
        "root@localhost.localdomain",
        "git@localhost",
        "you@example.com",
    }
)

#: Email suffixes that mark auto-generated / placeholder identities (lowercased).
DENIED_EMAIL_SUFFIXES = (
    "@local",
    "@localhost",
    ".local",
    "@localdomain",
    "@example.com",
    "@example.org",
    "@example.net",
)

#: Placeholder author/committer display names that must never ship (lowercased).
DENIED_NAMES_EXACT = frozenset(
    {
        "opencode",
        "mkd-agent",
        "root",
        "administrator",
    }
)

#: Identity/signing keys the follow-up path must share with the work step.
IDENTITY_KEYS = (
    "user.name",
    "user.email",
    "commit.gpgsign",
    "user.signingkey",
    "gpg.format",
)

#: Env overrides that take precedence over git config for the next commit.
IDENTITY_ENV_VARS = (
    "GIT_AUTHOR_NAME",
    "GIT_AUTHOR_EMAIL",
    "GIT_COMMITTER_NAME",
    "GIT_COMMITTER_EMAIL",
)


def _git(repo: Path | None, *args: str, timeout: int = 60) -> subprocess.CompletedProcess:
    """Run ``git <args>`` in ``repo`` (or CWD) and return the completed process."""
    cmd = ["git"]
    if repo is not None:
        cmd += ["-C", str(repo)]
    cmd += list(args)
    return subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=timeout,
        capture_output=True,
        text=True,
    )


def _email_problems(email: str) -> list[str]:
    """Return human-readable reasons why ``email`` is not an attributable identity."""
    lowered = (email or "").strip().lower()
    if not lowered:
        return ["empty author/committer email"]
    if lowered in DENIED_EMAILS_EXACT:
        return [f"placeholder email {email!r}"]
    for suffix in DENIED_EMAIL_SUFFIXES:
        if lowered.endswith(suffix):
            return [f"placeholder email {email!r} (matches {suffix!r})"]
    if "@" not in lowered:
        return [f"malformed email {email!r} (no '@')"]
    return []


def _name_problems(name: str) -> list[str]:
    """Return human-readable reasons why ``name`` is not an attributable identity."""
    lowered = (name or "").strip().lower()
    if not lowered:
        return ["empty author/committer name"]
    if lowered in DENIED_NAMES_EXACT:
        return [f"placeholder name {name!r}"]
    return []


def evaluate_row(row: dict) -> list[str]:
    """Return violation reasons for one parsed ``git log`` row (pure function).

    ``row`` keys: ``sha``, ``author_name``, ``author_email``,
    ``committer_name``, ``committer_email``, ``sig`` (``%G?``), ``signer``
    (``%GS``). Only ``G`` (good, trusted signature) passes the signature
    gate — ``U`` (untrusted), ``B`` (bad), ``E``/``X``/``Y``/``R``
    (error/expired/revoked) and ``N`` (none) all fail, because the
    ``required_signatures`` ruleset only accepts verified signatures.
    """
    reasons: list[str] = []
    for problem in _name_problems(row.get("author_name", "")):
        reasons.append(f"author {problem}")
    for problem in _email_problems(row.get("author_email", "")):
        reasons.append(f"author {problem}")
    for problem in _name_problems(row.get("committer_name", "")):
        reasons.append(f"committer {problem}")
    for problem in _email_problems(row.get("committer_email", "")):
        reasons.append(f"committer {problem}")
    sig = (row.get("sig") or "").strip()
    if sig != "G":
        signer = (row.get("signer") or "").strip()
        detail = f" signed by {signer!r}" if signer else ""
        reasons.append(f"no valid signature (git status {sig!r}{detail})")
    return reasons


def _parse_log(raw: str) -> list[dict]:
    """Parse the ``LOG_FORMAT`` record stream into row dicts."""
    rows: list[dict] = []
    for record in raw.split("\x1e"):
        # NOTE: strip only line whitespace, never \x1f — Python's default
        # str.strip() treats \x1f (unit separator) as whitespace and would
        # eat the trailing field separator of unsigned-commit rows.
        if not record.strip(" \r\n\t"):
            continue
        fields = record.split("\x1f")
        if len(fields) != 7:
            raise ValueError(f"unexpected git log record with {len(fields)} fields: {record[:80]!r}")
        rows.append(
            {
                "sha": fields[0],
                "author_name": fields[1],
                "author_email": fields[2],
                "committer_name": fields[3],
                "committer_email": fields[4],
                "sig": fields[5],
                "signer": fields[6],
            }
        )
    return rows


def cmd_check(base: str, head: str, repo: Path | None) -> int:
    """Fail when any commit in ``base..head`` is unsigned or unattributed."""
    where = f" in {repo}" if repo else ""
    proc = _git(repo, "log", f"--format={LOG_FORMAT}", f"{base}..{head}")
    if proc.returncode != 0:
        print(f"ERROR: git log {base}..{head}{where} failed: {(proc.stderr or '').strip()}")
        print("Hint: fetch first (git fetch origin <base-branch>) and spell the base as origin/<branch>.")
        return 2
    try:
        rows = _parse_log(proc.stdout or "")
    except ValueError as exc:
        print(f"ERROR: cannot parse git log output: {exc}")
        return 2
    if not rows:
        print(f"OK: no commits in {base}..{head}; nothing to verify.")
        return 0
    failures = 0
    for row in rows:
        reasons = evaluate_row(row)
        if reasons:
            failures += 1
            print(f"FAIL {row['sha'][:12]} author={row['author_name']!r} <{row['author_email']}> "
                  f"committer={row['committer_name']!r} <{row['committer_email']}>")
            for reason in reasons:
                print(f"  - {reason}")
    if failures:
        print(f"FAIL: {failures}/{len(rows)} commit(s) in {base}..{head} would be rejected by "
              f"required_signatures or ship unattributed. Do not push.")
        print("Fix: set the work-step identity (ensure-identity --apply-from <work-repo>), "
              "re-create the commit(s) with 'git commit -S' (rebase with 'git rebase --gpg-sign'), "
              "then re-run this gate.")
        return 1
    print(f"OK: {len(rows)} commit(s) in {base}..{head} are signed and attributed.")
    return 0


def _effective_config(repo: Path | None, key: str) -> str:
    proc = _git(repo, "config", "--get", key)
    if proc.returncode != 0:
        return ""
    return (proc.stdout or "").strip()


def _config_problems(repo: Path | None) -> list[str]:
    """Return identity problems from the effective git config of ``repo``."""
    problems: list[str] = []
    name = _effective_config(repo, "user.name")
    email = _effective_config(repo, "user.email")
    for problem in _name_problems(name):
        problems.append(f"git config user.name {problem} (effective {name!r})")
    for problem in _email_problems(email):
        problems.append(f"git config user.email {problem} (effective {email!r})")
    gpgsign = _effective_config(repo, "commit.gpgsign").lower()
    if gpgsign != "true":
        problems.append(
            f"signing disabled (effective commit.gpgsign={(gpgsign or 'unset')!r}); "
            "rebases strip signatures unless commit.gpgsign=true plus --gpg-sign"
        )
    return problems


def _env_override_problems() -> list[str]:
    """Return identity problems from GIT_* env overrides shadowing git config."""
    import os

    problems: list[str] = []
    for var in IDENTITY_ENV_VARS:
        value = (os.environ.get(var) or "").strip()
        if not value:
            continue
        checks = _email_problems if var.endswith("_EMAIL") else _name_problems
        for problem in checks(value):
            problems.append(f"env {var} {problem} (overrides git config)")
    return problems


def collect_identity_problems(repo: Path | None) -> list[str]:
    """Return reasons the next commit/rebase in ``repo`` would be unattributed/unsigned."""
    return _config_problems(repo) + _env_override_problems()


def _apply_identity(target: Path, apply_from: Path) -> int:
    """Copy the shared identity/signing keys from ``apply_from`` into ``target``."""
    probe = _git(apply_from, "rev-parse", "--is-inside-work-tree")
    if probe.returncode != 0:
        print(f"ERROR: --apply-from {apply_from} is not inside a git working tree.")
        return 2
    applied: list[str] = []
    for key in IDENTITY_KEYS:
        value = _effective_config(apply_from, key)
        if not value:
            continue
        put = _git(target, "config", "--local", key, value)
        if put.returncode != 0:
            print(f"ERROR: could not set {key} in {target}: {(put.stderr or '').strip()}")
            return 2
        applied.append(f"{key}={value}")
    if applied:
        print(f"Applied identity from {apply_from} to {target}: " + ", ".join(applied))
    else:
        print(f"WARNING: source {apply_from} has none of {', '.join(IDENTITY_KEYS)} set; nothing applied.")
    return 0


def _report_identity_problems(target: Path, problems: list[str]) -> int:
    print(f"FAIL: git identity in {target} would produce unattributed/unsigned commits:")
    for problem in problems:
        print(f"  - {problem}")
    print("Fix: python3 scripts/verify-signed-commits.py ensure-identity "
          "--apply-from \"$NIGHT_WORKTREE_PATH\", then rebase with "
          "'git rebase --gpg-sign' and commit with 'git commit -S'.")
    return 1


def cmd_ensure_identity(repo: Path | None, apply_from: Path | None) -> int:
    """Verify (and optionally install) the shared work-step git identity."""
    target = repo if repo is not None else REPO_ROOT
    if apply_from is not None:
        rc = _apply_identity(target, apply_from)
        if rc != 0:
            return rc
    problems = collect_identity_problems(target)
    if problems:
        return _report_identity_problems(target, problems)
    name = _effective_config(target, "user.name")
    email = _effective_config(target, "user.email")
    print(f"OK: git identity in {target} is {name!r} <{email}> with signing enabled.")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify-signed-commits",
        description="Pre-push gate: commits in <base>..HEAD must be signed and attributed (#6809).",
    )
    sub = parser.add_subparsers(dest="command")
    p_check = sub.add_parser("check", help="Verify commits in <base>..<head> (default).")
    p_check.add_argument("--base", default="origin/main", help="Range base (default: origin/main).")
    p_check.add_argument("--head", default="HEAD", help="Range head (default: HEAD).")
    p_check.add_argument("--repo", default="", help="Repo path to inspect (default: current directory).")
    p_id = sub.add_parser("ensure-identity", help="Verify (optionally install) the shared git identity.")
    p_id.add_argument("--repo", default="", help="Repo path to check/fix (default: current directory).")
    p_id.add_argument(
        "--apply-from",
        default="",
        help="Copy user.name/user.email/commit.gpgsign/user.signingkey/gpg.format "
        "from this repo (e.g. \"$NIGHT_WORKTREE_PATH\") into --repo before checking.",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
    parser = build_parser()
    args = parser.parse_args(argv)
    command = args.command or "check"
    if command == "check":
        base = getattr(args, "base", "origin/main")
        head = getattr(args, "head", "HEAD")
        repo_raw = getattr(args, "repo", "") or ""
        return cmd_check(base, head, Path(repo_raw) if repo_raw else None)
    repo_raw = getattr(args, "repo", "") or ""
    src_raw = getattr(args, "apply_from", "") or ""
    return cmd_ensure_identity(
        Path(repo_raw) if repo_raw else None,
        Path(src_raw) if src_raw else None,
    )


if __name__ == "__main__":
    sys.exit(main())
