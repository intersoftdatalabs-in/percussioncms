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
"""Run ``gh`` with stdout kept pure for JSON parsing (issue #6986).

gh 2.105.0 prints a multi-line ``gh event listen ...`` hint to **stderr**
after ``gh pr view`` and related commands. Shell one-liners that merge the
streams (``gh ... --json ... 2>&1 | python3 -c 'json.load(sys.stdin)'``)
then feed the hint into the JSON parser and die with ``JSONDecodeError``.

This wrapper is the blessed alternative for night workers and scripts::

    python3 scripts/gh-json.py issue view 123 --json number,title,body

Contract (cross-platform, pure Python so no ``.cmd`` counterpart is needed):

- ``gh`` stdout is forwarded untouched to our stdout (byte-identical).
- ``gh`` stderr is inherited untouched: visible on your terminal, never
  merged into the JSON stream.
- A non-zero ``gh`` exit fails loudly: a ``gh-json:`` line on stderr plus
  the same exit code (never a silent empty stdout).
- ``gh`` missing from ``PATH`` fails loudly with exit 127.

Never wrap this in ``2>&1`` — that reintroduces the bug it exists to fix.
Prefer ``--jq`` for shaping output instead of piping through Python.
"""
from __future__ import annotations

import os
import shutil
import subprocess
import sys

USAGE = (
    "usage: python3 scripts/gh-json.py <gh args...>\n"
    "example: python3 scripts/gh-json.py issue view 123 --json number,title\n"
)


def resolve_gh() -> str | None:
    """Resolve the ``gh`` executable honoring Windows ``PATHEXT``.

    ``subprocess.run(["gh", ...], shell=False)`` on Windows relies on
    ``CreateProcess`` search, which appends ``.exe`` only — a ``gh.cmd``
    shim earlier on ``PATH`` (e.g. the regression-test fake, or a real
    ``.cmd``-shim install) is silently skipped in favour of a ``gh.exe``
    later on ``PATH``. ``shutil.which`` honors the full ``PATHEXT``
    (``.COM;.EXE;.BAT;.CMD;...``), so resolve explicitly and exec the
    absolute path. On POSIX this finds the same binary ``execvp`` would.

    The explicit ``nt`` scan below is belt-and-braces for runtimes whose
    ``which`` does not apply ``PATHEXT``; it also makes the Windows lookup
    unit-testable on any host by simulating ``nt`` (see
    ``test_resolve_gh_honors_pathext_cmd_shim``).

    Returns ``None`` when ``gh`` is not on ``PATH``.
    """
    exe = shutil.which("gh")
    if exe is not None:
        return exe
    if os.name != "nt":
        return None
    pathext = os.environ.get("PATHEXT", ".COM;.EXE;.BAT;.CMD").split(";")
    exts = [e if e.startswith(".") else f".{e}" for e in pathext if e]
    for directory in os.environ.get("PATH", "").split(os.pathsep):
        if not directory:
            continue
        try:
            entries = {name.lower(): name for name in os.listdir(directory)}
        except OSError:
            continue
        for ext in ("", *exts):
            # Lowercase compare: Windows filesystems are usually
            # case-insensitive, so ``gh.CMD`` must match ``gh.cmd``.
            hit = entries.get(f"gh{ext}".lower())
            if hit is not None:
                return os.path.join(directory, hit)
    return None


def main(argv: list[str]) -> int:
    """Run ``gh`` with ``argv``; return the process exit code."""
    args = list(argv)
    if args and args[0] == "--":
        args = args[1:]
    if not args or args[0] in ("-h", "--help"):
        sys.stdout.write(USAGE)
        return 0 if args else 2
    exe = resolve_gh()
    if exe is None:
        print("gh-json: `gh` not found on PATH", file=sys.stderr)
        return 127
    try:
        proc = subprocess.run([exe, *args], check=False)
    except FileNotFoundError:
        print("gh-json: `gh` not found on PATH", file=sys.stderr)
        return 127
    if proc.returncode != 0:
        print(
            "gh-json: gh " + " ".join(args)
            + f" failed with exit {proc.returncode}",
            file=sys.stderr,
        )
    return proc.returncode


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
