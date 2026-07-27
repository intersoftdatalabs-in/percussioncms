#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/verify-no-finder-jsp-references.{sh,bat}.

Purpose
-------
CI-gate artifact-grep for spec 992-react-content-explorer FR-019a: after US6's
hard cut of the modern ContentExplorerShell onto the primary-nav shells, the
production-built WebUI WAR must contain ZERO references to ``finder.jsp`` as a
navigation entry point in the modern Track B shell
(``WebUI/src/main/webapp/cm/app/webmgt.jsp``).

Usage
-----
::

    python3 scripts/verify_no_finder_jsp_references.py
        [--target <path>]
        [--allow-include <substr>]
        [--allow-track-a]

Exit codes
----------
- ``0`` clean (no navigation entries to ``finder.jsp``)
- ``1`` at least one navigation entry found OR missing target file

Behavioral Notes
----------------
- bash ``perl -0777 -pe 's/<%--.*?--%>//gs'`` is replaced by Python's
  ``re.sub(r"<%--.*?--%>", "", text, flags=re.DOTALL)`` (no Perl dependency).
- The bash version depended on ``perl`` and ``grep -E``; the Python port
  relies only on stdlib (FR-006).
- The regex matches the literal navigation-entry forms:
    - ``<jsp:include page="includes/finder.jsp" ...>``
    - ``<%@include file="includes/finder.jsp" ...>``
  It does NOT match the ``finder_js.jsp`` shared-library include (explicit
  carve-out per the contract).
"""
from __future__ import annotations

import argparse
import logging
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

JSP_COMMENT_RE = re.compile(r"<%--.*?--%>", flags=re.DOTALL)
NAV_INCLUDE_RE = re.compile(
    r"""(?P<hit>
        <jsp:include\s+page\s*=\s*"includes/finder\.jsp"
        |
        <%@include\s+file\s*=\s*"includes/finder\.jsp"
    )""",
    flags=re.VERBOSE,
)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify_no_finder_jsp_references.py",
        description="Verify modern Track B shell has no navigation entries to finder.jsp.",
    )
    parser.add_argument(
        "--target",
        default="WebUI/src/main/webapp/cm/app/webmgt.jsp",
        help="Target JSP path (default: modern Track B primary-nav shell)",
    )
    parser.add_argument(
        "--allow-include",
        action="append",
        default=None,
        help="Additional carve-out substrings (repeatable; default: finder_js.jsp)",
    )
    parser.add_argument(
        "--allow-track-a",
        action="store_true",
        help="Also allow finder.jsp navigation in cm/pages/app/webmgt.jsp (Track A)",
    )
    return parser


def _strip_comments(text: str) -> str:
    return JSP_COMMENT_RE.sub("", text)


def _find_navigation_entries(stripped_text: str) -> list[tuple[int, str]]:
    """Return a list of ``(line_number, matched_text)`` for every match.

    ``line_number`` is 1-indexed (matches ``grep -n``).
    """
    matches: list[tuple[int, str]] = []
    # Re-split the stripped text on newlines so we can report the line number.
    lines = stripped_text.split("\n")
    cumulative = 0
    for line_idx, line in enumerate(lines, start=1):
        for m in NAV_INCLUDE_RE.finditer(line):
            matches.append((line_idx, m.group(0)))
        cumulative += len(line) + 1  # +1 for the newline
    return matches


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    target = Path(args.target)
    if not target.is_file():
        print(f"  FAIL: target JSP does not exist: {target}", file=sys.stderr)
        return 1

    text = target.read_text(encoding="utf-8", errors="replace")
    stripped = _strip_comments(text)
    matches = _find_navigation_entries(stripped)

    # Apply carve-outs.
    allow = list(args.allow_include) if args.allow_include else ["finder_js.jsp"]
    if args.allow_track_a:
        allow.append("cm/pages/app/webmgt.jsp")
    filtered: list[tuple[int, str]] = []
    for line_no, hit in matches:
        if any(a in hit for a in allow):
            continue
        filtered.append((line_no, hit))

    if filtered:
        print(f"  FAIL: {target} contains finder.jsp navigation entry:", file=sys.stderr)
        for line_no, hit in filtered:
            print(f"    line {line_no}: {hit}", file=sys.stderr)
        print("verify-no-finder-jsp-references: FAIL", file=sys.stderr)
        return 1
    print("verify-no-finder-jsp-references: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
