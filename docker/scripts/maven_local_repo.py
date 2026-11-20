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
"""Resolve the Maven local repository the same way ``mvn`` would (#6709).

The QA helpers (``qa_preflight.py``, ``hot-deploy-rhythmyx-war-jars.py``,
``matrix-install-smoke.py``) read freshly installed SNAPSHOT jars out of the
Maven local repository. When a host isolates a checkout's local repository
(for example the opencode ``night-issue-prs`` worktrees building into
``~/.m2-night/repository`` so they never overwrite the SNAPSHOTs another
checkout installs into ``~/.m2/repository``), those helpers must read the
same repository Maven wrote to, or they hot-deploy the other checkout's jars.

Resolution order (first hit wins; stdlib only, cross-platform):

  1. ``-Dmaven.repo.local=...`` in ``MAVEN_ARGS`` / ``MAVEN_OPTS``.
  2. POSIX only, unless ``MAVEN_SKIP_RC`` is set: the same variables after
     sourcing the mavenrc files the ``mvn`` launcher sources
     (``/usr/local/etc/mavenrc``, ``/etc/mavenrc``, ``~/.mavenrc``), run with
     the checkout root as the working directory — so a cwd-scoped
     ``~/.mavenrc`` applies to these helpers exactly as it does to ``mvn``.
  3. ``-Dmaven.repo.local=...`` in ``<repo_root>/.mvn/maven.config``.
  4. ``<localRepository>`` in ``~/.m2/settings.xml``.
  5. ``~/.m2/repository``.

Per-worktree isolation (#6739): ``worktree_local_repository`` /
``ensure_worktree_repository`` (plus this module's ``__main__`` CLI)
assign each checkout its own ``-Dmaven.repo.local`` under
``<repo_root>/tmp/m2-worktree/repository`` and seed third-party artifacts
once, so concurrent shards never overwrite each other's ``SNAPSHOT`` jars.
``qa-up`` auto-runs the strict preflight against the resolved repository
and refuses with an isolation hint when it is stale or poisoned.
"""

from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Mapping, Optional

_REPO_LOCAL_RE = re.compile(r"""-Dmaven\.repo\.local=(?:"([^"]*)"|'([^']*)'|(\S+))""")
# mkd-code-review[paths.hardcoded_sep]: adjudicated FP (issue #6781, precedent #6341) —
# POSIX-only mavenrc paths by construction (sourced by the POSIX mvn launcher only);
# _from_mavenrc returns None on Windows before these are ever read. Host joins use pathlib.
_MAVENRC_FILES = ("/usr/local/etc/mavenrc", "/etc/mavenrc", "$HOME/.mavenrc")
_MAVENRC_TIMEOUT_S = 5
# Bound at import so callers' tests that stub ``subprocess.run`` (e.g. "dry
# run must not shell out to docker") neither see nor break this probe.
_run = subprocess.run


def home_dir() -> Path:
    # `Path("~")` does NOT expand on Windows; expanduser on the string does.
    return Path(os.path.expanduser("~"))


def default_m2_repository() -> Path:
    """Maven's built-in default: ``~/.m2/repository``."""
    return home_dir() / ".m2" / "repository"


def parse_repo_local(text: Optional[str]) -> Optional[Path]:
    """Return the last ``-Dmaven.repo.local=`` value in ``text`` (Maven: last wins)."""
    if not text:
        return None
    value = None
    for match in _REPO_LOCAL_RE.finditer(text):
        value = next(g for g in match.groups() if g is not None)
    if not value:
        return None
    return Path(os.path.expanduser(value))


def _from_env(env: Mapping[str, str]) -> Optional[Path]:
    return parse_repo_local(env.get("MAVEN_ARGS")) or parse_repo_local(env.get("MAVEN_OPTS"))


def _from_mavenrc(cwd: Path, env: Mapping[str, str]) -> Optional[Path]:
    if os.name == "nt" or sys.platform.startswith("win") or env.get("MAVEN_SKIP_RC"):
        return None
    home = env.get("HOME") or os.path.expanduser("~")
    present = [f for f in _MAVENRC_FILES if Path(f.replace("$HOME", home)).is_file()]
    if not present:
        return None
    script = "".join(f'[ -f "{f}" ] && . "{f}"\n' for f in present)
    script += 'printf "%s\\n%s\\n" "${MAVEN_ARGS:-}" "${MAVEN_OPTS:-}"\n'
    try:
        proc = _run(
            ["sh", "-c", script],
            cwd=str(cwd) if cwd.is_dir() else None,
            env=dict(env),
            capture_output=True,
            text=True,
            timeout=_MAVENRC_TIMEOUT_S,
            check=False,
        )
    except (OSError, subprocess.SubprocessError):
        return None
    stdout = getattr(proc, "stdout", None)
    # Callers' tests sometimes stub subprocess.run; ignore non-text results.
    return parse_repo_local(stdout) if isinstance(stdout, str) else None


def _from_maven_config(repo_root: Path) -> Optional[Path]:
    config = repo_root / ".mvn" / "maven.config"
    try:
        text = config.read_text(encoding="utf-8")
    except OSError:
        return None
    return parse_repo_local(text)


def _from_settings(home: Path) -> Optional[Path]:
    settings = home / ".m2" / "settings.xml"
    try:
        root = ET.parse(settings).getroot()
    except (OSError, ET.ParseError):
        return None
    for elem in root.iter():
        if elem.tag.rsplit("}", 1)[-1] == "localRepository" and (elem.text or "").strip():
            raw = elem.text.strip().replace("${user.home}", str(home))
            return Path(os.path.expanduser(raw))
    return None


def resolve_m2_repository(
    repo_root: Optional[Path] = None,
    env: Optional[Mapping[str, str]] = None,
) -> Path:
    """Resolve the Maven local repository for ``repo_root`` (default: cwd)."""
    env = os.environ if env is None else env
    root = Path(repo_root) if repo_root is not None else Path(os.getcwd())
    return (
        _from_env(env)
        or _from_mavenrc(root, env)
        or _from_maven_config(root)
        or _from_settings(home_dir())
        or default_m2_repository()
    )


# Per-worktree local repositories (#6739).
#
# ``~/.m2-night`` is still shared across all night shards on a host: the
# provenance guard flags cross-talk after the fact but nothing stops two
# shards building the same ``8.2.0-SNAPSHOT`` concurrently. A per-worktree
# repository (a directory *under the shard*, so every worktree resolves a
# different path by construction) prevents the overwrite instead of
# detecting it. The directory lives under ``<repo_root>/tmp`` (gitignored,
# repo temp dir per root AGENTS.md) so it vanishes with the worktree and
# never pollutes ``git status``.
_WORKTREE_M2_DIRNAME = "m2-worktree"
_WORKTREE_SEED_MARKER = ".worktree-m2-seeded"


def worktree_local_repository(repo_root: Path) -> Path:
    """Return the per-worktree Maven local repository for a checkout.

    Portable: built with ``pathlib`` joins only, no separators. Distinct
    for every worktree because ``repo_root`` differs per worktree.
    """
    return Path(repo_root).resolve() / "tmp" / _WORKTREE_M2_DIRNAME / "repository"


def repo_local_flag(repo_path: Path) -> str:
    """Format ``-Dmaven.repo.local=...`` for ``MAVEN_ARGS`` / ``MAVEN_OPTS``.

    The value is the OS-native ``str`` of ``repo_path`` — correct for Maven
    on every platform (the JVM accepts the native separator). Values
    containing whitespace are wrapped in double quotes because
    ``MAVEN_ARGS`` / ``MAVEN_OPTS`` are whitespace-split by the ``mvn``
    launcher; :func:`parse_repo_local` strips those quotes on the way back.
    Callers embedding the flag in a POSIX-shell export line (Git Bash / WSL)
    must preserve backslashes (single-quoted ``export`` or ``cmd`` /
    PowerShell) — a bare double-quoted POSIX export eats ``\\``.
    """
    text = str(repo_path)
    if any(ch.isspace() for ch in text):
        text = f'"{text}"'
    return f"-Dmaven.repo.local={text}"


def _is_snapshot_path(path: Path) -> bool:
    """True when any path part carries Maven's ``SNAPSHOT`` marker."""
    return any("SNAPSHOT" in part for part in path.parts)


def _copy_third_party_tree(source: Path, repo: Path) -> None:
    """Copy non-``SNAPSHOT`` files from ``source`` into ``repo`` (portable).

    Existing destination files are kept (never overwrite a newer local
    copy). ``SNAPSHOT`` version directories and files are pruned so
    project snapshots are always rebuilt in place by the worktree.
    """
    for dirpath, dirnames, filenames in os.walk(source):
        rel = Path(dirpath).relative_to(source)
        if _is_snapshot_path(rel):
            dirnames[:] = []
            continue
        # Prune SNAPSHOT version dirs in place so os.walk never descends.
        dirnames[:] = [d for d in dirnames if "SNAPSHOT" not in d]
        target_dir = repo / rel if str(rel) != "." else repo
        target_dir.mkdir(parents=True, exist_ok=True)
        for name in filenames:
            if "SNAPSHOT" in name:
                continue
            dst = target_dir / name
            if dst.exists():
                continue
            try:
                shutil.copy2(Path(dirpath) / name, dst)
            except OSError:
                continue


def ensure_worktree_repository(
    repo_root: Path,
    seed_from: Optional[Path] = None,
) -> Path:
    """Create the per-worktree repository, seeding third-party artifacts once.

    Creates ``worktree_local_repository(repo_root)`` plus its parent chain,
    then copies third-party (non-``SNAPSHOT``) artifacts from ``seed_from``
    (default: the currently resolved shared repository) so the first
    worktree build does not re-download the internet. Project ``SNAPSHOT``
    jars are deliberately *not* copied — they are rebuilt in place by the
    worktree's own ``clean install``, which is the isolation this exists
    for. A marker file records the seed source; a second call with the same
    source is a no-op (idempotent). Uses ``shutil`` / ``pathlib`` only —
    no ``rsync`` dependency, so it runs on Windows runners too.
    """
    repo = worktree_local_repository(repo_root)
    repo.mkdir(parents=True, exist_ok=True)
    source = Path(seed_from) if seed_from is not None else resolve_m2_repository(repo_root)
    marker = repo.parent / _WORKTREE_SEED_MARKER
    try:
        seeded = marker.read_text(encoding="utf-8").strip()
    except OSError:
        seeded = ""
    if seeded == str(source):
        return repo
    if source.is_dir():
        _copy_third_party_tree(source, repo)
    try:
        marker.write_text(str(source), encoding="utf-8")
    except OSError:
        pass
    return repo


def main(argv: Optional[list] = None) -> int:
    """One-shot helper: create/seed the per-worktree repo and print the export."""
    parser = argparse.ArgumentParser(
        prog="maven-local-repo",
        description=(
            "Create (and seed once) the per-worktree Maven local repository "
            "(#6739) and print the MAVEN_ARGS export that isolates this "
            "checkout's SNAPSHOT builds from sibling worktrees."
        ),
    )
    parser.add_argument(
        "--repo-root",
        type=Path,
        default=Path(os.getcwd()),
        help="Path to the Percussion CMS checkout root (default: cwd).",
    )
    parser.add_argument(
        "--seed-from",
        type=Path,
        default=None,
        help=(
            "Shared repository to seed third-party artifacts from "
            "(default: the currently resolved local repository)."
        ),
    )
    parser.add_argument(
        "--no-seed",
        action="store_true",
        help="Create the directory without copying third-party artifacts.",
    )
    args = parser.parse_args(argv)
    repo = worktree_local_repository(args.repo_root)
    repo.mkdir(parents=True, exist_ok=True)
    if not args.no_seed:
        repo = ensure_worktree_repository(args.repo_root, args.seed_from)
    print(f'export MAVEN_ARGS="{repo_local_flag(repo)}${{MAVEN_ARGS:+ $MAVEN_ARGS}}"')
    print(f"Per-worktree Maven local repo: {repo}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
