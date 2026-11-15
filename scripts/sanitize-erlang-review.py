#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

See the License for the specific language governing permissions and
limitations under the License.

Sanitize mkd-code-review (Erlang) markdown before it is committed to
``docs/ai-generated/code-reviews/`` or pasted into a GitHub PR body.

Raw ``mkd-code-review analyze --format markdown`` stdout embeds
operator-machine details that must not ship in the repo or on the forge:

- absolute home-directory paths (``Persona source:
  /home/<user>/.local/share/mkd/agents/erlang``, ``--models
  /home/<user>/workspaces/.../models.<profile>.toml``, worktree paths),
- the LLM provider key env var name (``OPENROUTER_API_KEY``),
- provider route/model ids (``openrouter-free``, ``openrouter/free``),
- stray ``residual model=...`` debug lines emitted above the report.

## Behavioral Notes

- Stdin/stdout filter by default; ``--in`` / ``--out`` select files,
  ``--check`` exits 1 when the input still contains a leak (CI gate use).
- ``pathlib.Path`` everywhere; no hardcoded separators; stdlib only.
- Home-directory collapse (Unix, macOS, and Windows home prefixes) is
  deliberately generic: it redacts ANY username, not just the current
  operator's.
- Provider token collapse is exact-match (``openrouter-free``,
  ``openrouter/free``, ``OPENROUTER_API_KEY``); generic ``llm.error`` /
  ``llm warn`` text is preserved.
"""
from __future__ import annotations

import argparse
import logging
import re
import sys
from pathlib import Path

log = logging.getLogger(__name__)

# mkd-code-review[paths.hardcoded_sep]: docstring documents redaction of foreign
# text paths (URLs/report content), not a filesystem join; all I/O here uses pathlib.
# (Lines below with regex literals and test fixtures carry the same justification.)
RESIDUAL_LINE = re.compile(r"^[ \t]*residual model=.*$")
# mkd-code-review[paths.hardcoded_sep]: regex matches foreign markdown text paths
# for redaction, not a filesystem join; all I/O in this file uses pathlib.
HOME_UNIX = re.compile(r"/home/[A-Za-z0-9_.\-]+")
HOME_MAC = re.compile(r"/Users/[A-Za-z0-9_.\-]+")
HOME_WIN = re.compile(r"[A-Za-z]:[\\/]Users[\\/][A-Za-z0-9_.\-]+")
WORKTREE_PATH = re.compile(
    r"~/(?:projects|workspaces)/[^\s`\"']*?worktrees?/[^\s`\"']*"
)
MODELS_PATH = re.compile(r"(?:[\w.~][\w.~/#\\-]*/)?models\.([\w][\w.\-]*)\.toml")
OPENROUTER_KEY = "OPENROUTER_API_KEY"
OPENROUTER_ROUTE = "openrouter/free"
OPENROUTER_ID = "openrouter-free"

LEAK_PATTERNS = (
    # mkd-code-review[paths.hardcoded_sep]: regex matches foreign markdown text
    # paths for leak detection, not a filesystem join; all I/O here uses pathlib.
    re.compile(r"/home/[A-Za-z0-9_.\-]+"),
    re.compile(r"/Users/[A-Za-z0-9_.\-]+"),
    re.compile(r"[A-Za-z]:[\\/]Users[\\/][A-Za-z0-9_.\-]+"),
    re.compile(r"OPENROUTER_API_KEY"),
    re.compile(r"openrouter/free"),
    re.compile(r"openrouter-free"),
    re.compile(r"^[ \t]*residual model=", re.MULTILINE),
)


def sanitize_text(text: str) -> str:
    """Return *text* with operator-machine details redacted."""
    lines = [ln for ln in text.splitlines() if not RESIDUAL_LINE.match(ln)]
    text = "\n".join(lines)
    text = HOME_UNIX.sub("~", text)
    text = HOME_MAC.sub("~", text)
    text = HOME_WIN.sub("~", text)
    text = WORKTREE_PATH.sub("<worktree>", text)
    text = MODELS_PATH.sub(r"models.\1.toml", text)
    text = text.replace(OPENROUTER_KEY, "<llm-provider-key-env>")
    text = text.replace(OPENROUTER_ROUTE, "<llm-model>")
    text = re.sub(r"\bllm\." + re.escape(OPENROUTER_ID) + r"\b", "llm", text)
    text = re.sub(
        r"(`)"+ re.escape(OPENROUTER_ID) + r"(`)", r"\1<llm-model>\2", text
    )
    if text and not text.endswith("\n"):
        text += "\n"
    return text


def find_leaks(text: str) -> list[str]:
    """Return human-readable descriptions of leaks still present."""
    hits: list[str] = []
    for pat in LEAK_PATTERNS:
        for match in pat.finditer(text):
            line_no = text.count("\n", 0, match.start()) + 1
            hits.append(f"line {line_no}: {match.group(0).strip()[:80]}")
    return hits


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Redact operator-machine details from Erlang review markdown."
    )
    parser.add_argument("--in", dest="in_path", default=None,
                        help="Input markdown file (default: stdin).")
    parser.add_argument("--out", dest="out_path", default=None,
                        help="Output markdown file (default: stdout).")
    parser.add_argument("--check", action="store_true",
                        help="Exit 1 if the input still contains a leak; emit no output.")
    parser.add_argument("--fix-tree", dest="fix_tree", default=None,
                        help="Sanitize every *.md file under this directory in place.")
    args = parser.parse_args(argv)

    logging.basicConfig(level=logging.INFO,
                        format="%(asctime)s %(levelname)s %(message)s")

    if args.fix_tree is not None:
        root = Path(args.fix_tree)
        changed = 0
        for path in sorted(root.rglob("*.md")):
            original = path.read_text(encoding="utf-8")
            cleaned = sanitize_text(original)
            if cleaned != original:
                path.write_text(cleaned, encoding="utf-8")
                changed += 1
        log.info("sanitized %d file(s) under %s", changed, root)
        return 0

    if args.in_path is not None:
        text = Path(args.in_path).read_text(encoding="utf-8")
    elif sys.stdin.isatty():
        parser.error("no input: pass --in <file> or pipe markdown on stdin")
        return 2
    else:
        text = sys.stdin.read()

    if args.check:
        hits = find_leaks(text)
        for hit in hits:
            log.warning("leak %s", hit)
        return 1 if hits else 0

    cleaned = sanitize_text(text)
    if args.out_path is not None:
        Path(args.out_path).write_text(cleaned, encoding="utf-8")
    else:
        sys.stdout.write(cleaned)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
