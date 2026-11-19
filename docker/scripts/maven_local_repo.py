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
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Mapping, Optional

_REPO_LOCAL_RE = re.compile(r"""-Dmaven\.repo\.local=(?:"([^"]*)"|'([^']*)'|(\S+))""")
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
