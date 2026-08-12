#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Grep gate: freeze bare production ``IPSObjectStoreErrors`` call-sites (#3143).

Purpose
-------
Phase 2b ObjectStore retype work (#2616 family) moved production throws onto typed
``ObjectStoreErrorCodes`` / ``ObjectStoreErrorCode`` / ``DesignErrorCodes`` peers.
This gate fails when **new** production Java sources introduce bare
``IPSObjectStoreErrors`` imports, ``implements``, or qualified constant uses
outside an explicit allow-list.

Allow-list classes
------------------
1. **Interface definition** — ``IPSObjectStoreErrors`` itself
2. **Dual-write / typed peer bridge** — utils ``ObjectStoreErrorCode`` enum
3. **Documented residual production call-sites** until sibling retypes land
   (Desktop CX #3141, Design ACL #3142, deployer objectstore XML residual,
   legacy ``implements IPSObjectStoreErrors`` handlers)

Tests and comment/javadoc-only mentions are ignored.

Usage
-----
::

    python3 scripts/verify-no-bare-ipsobjectstoreerrors.py [--repo-root <path>]
    python3 scripts/verify-no-bare-ipsobjectstoreerrors.py --list-allowlist

Exit codes
----------
- ``0`` clean (only allow-listed / test / comment hits)
- ``1`` at least one new bare production usage

Portable: Python 3.9+; uses ``git grep`` (tracked files only) with
``shell=False``.
"""
from __future__ import annotations

import argparse
import logging
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)

# Marker substring scanned via git grep -F.
BANNED_TOKEN = "IPSObjectStoreErrors"

GREP_PATHSPECS = ("*.java",)

# Paths (posix, relative to repo root) always allowed — interface + bridges.
ALWAYS_ALLOW_EXACT: frozenset[str] = frozenset(
    {
        # Legacy constant interface (must remain until full retirement).
        "modules/utils/src/main/java/com/percussion/design/objectstore/"
        "IPSObjectStoreErrors.java",
        # Utils-local typed peers (numeric bridge from interface constants).
        "modules/utils/src/main/java/com/percussion/design/objectstore/"
        "ObjectStoreErrorCode.java",
    }
)

# Prefix allow-list for documented residual production call-sites.
# Shrink this list as retype residuals merge; do not grow without an issue link.
RESIDUAL_ALLOW_PREFIXES: tuple[tuple[str, str], ...] = (
    # #3141 — Desktop CX PSNode bare sites (PR open at gate land).
    (
        "modules/DesktopContentExplorer/src/main/java/com/percussion/cx/"
        "objectstore/PSNode.java",
        "Desktop CX PSNode residual (#3141)",
    ),
    # #3142 — Design ACL PSAclEntry bare sites (PR open at gate land).
    (
        "system/src/main/java/com/percussion/design/objectstore/PSAclEntry.java",
        "Design ACL PSAclEntry residual (#3142)",
    ),
    # Legacy implements IPSObjectStoreErrors (unqualified constant use).
    (
        "system/src/main/java/com/percussion/design/objectstore/server/"
        "PSXmlObjectStoreHandler.java",
        "implements IPSObjectStoreErrors handler residual",
    ),
    (
        "system/src/main/java/com/percussion/design/objectstore/server/"
        "PSXmlObjectStoreLockManager.java",
        "implements IPSObjectStoreErrors lock residual",
    ),
    # Deployer objectstore XML residual (not yet sliced for ObjectStore retype).
    (
        "deployer/src/main/java/",
        "deployer objectstore XML residual (pre-existing bare sites)",
    ),
)

# This gate and its tests may mention the token.
SCRIPT_ALLOW_PREFIXES: tuple[str, ...] = (
    "scripts/verify-no-bare-ipsobjectstoreerrors.py",
    "scripts/test_verify_no_bare_ipsobjectstoreerrors.py",
)

# Strip block comments then line comments for a coarse "is this code?" check.
_BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.DOTALL)
_LINE_COMMENT_RE = re.compile(r"//.*?$", re.MULTILINE)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify-no-bare-ipsobjectstoreerrors.py",
        description=(
            "Fail on new bare production IPSObjectStoreErrors call-sites "
            "outside the documented allow-list (#3143 / parent #2616)."
        ),
    )
    parser.add_argument(
        "--repo-root",
        default=str(REPO_ROOT),
        help="Repo root for the checks (default: this script's repo root)",
    )
    parser.add_argument(
        "--list-allowlist",
        action="store_true",
        help="Print allow-list paths/prefixes and exit 0",
    )
    return parser


def _norm_path(path: str) -> str:
    return path.replace("\\", "/").lstrip("./")


def _is_test_path(path: str) -> bool:
    norm = _norm_path(path)
    if "/src/test/" in norm or "/src/it/" in norm or "/src/testFixtures/" in norm:
        return True
    name = Path(norm).name
    if name.endswith("Test.java") or name.endswith("Tests.java"):
        return True
    return False


def _is_script_allowlisted(path: str) -> bool:
    norm = _norm_path(path)
    for prefix in SCRIPT_ALLOW_PREFIXES:
        if norm == prefix or norm.startswith(prefix):
            return True
    return False


def _is_always_allowlisted(path: str) -> bool:
    return _norm_path(path) in ALWAYS_ALLOW_EXACT


def _residual_reason(path: str) -> str | None:
    norm = _norm_path(path)
    for prefix, reason in RESIDUAL_ALLOW_PREFIXES:
        if norm == prefix or norm.startswith(prefix):
            return reason
    return None


def _code_mentions_token(source: str) -> bool:
    """True if non-comment Java code still mentions IPSObjectStoreErrors."""
    without_blocks = _BLOCK_COMMENT_RE.sub("", source)
    without_line = _LINE_COMMENT_RE.sub("", without_blocks)
    return BANNED_TOKEN in without_line


def _git_grep_lines(repo_root: Path, pattern: str) -> list[tuple[str, int, str]]:
    """Return (path, line_no, text) from ``git grep -n -F``."""
    cmd = ["git", "grep", "-n", "-F", pattern, "--", *GREP_PATHSPECS]
    result = subprocess.run(
        cmd,
        shell=False,
        check=False,
        cwd=str(repo_root),
        timeout=120,
        capture_output=True,
        text=True,
    )
    if result.returncode not in (0, 1):
        raise RuntimeError(
            f"git grep failed for {pattern!r} (rc={result.returncode}): "
            f"{result.stderr.strip()}"
        )
    hits: list[tuple[str, int, str]] = []
    for raw in result.stdout.splitlines():
        if not raw.strip():
            continue
        # path:line:text  (path may contain drive letters on Windows — use maxsplit)
        parts = raw.split(":", 2)
        if len(parts) < 3:
            # Some git versions may emit path\0line\0 — not used with default -n.
            continue
        path_part, line_s, text = parts
        try:
            line_no = int(line_s)
        except ValueError:
            # Windows path with drive letter: C:/foo:12:text already split wrong.
            # Re-parse from the right: last path-ish segment before line number.
            m = re.match(r"^(.*):(\d+):(.*)$", raw)
            if not m:
                continue
            path_part, line_s, text = m.group(1), m.group(2), m.group(3)
            line_no = int(line_s)
        hits.append((_norm_path(path_part), line_no, text))
    return hits


def _paths_with_code_hits(repo_root: Path) -> list[str]:
    """Unique production paths that mention the token in non-comment code."""
    try:
        hits = _git_grep_lines(repo_root, BANNED_TOKEN)
    except RuntimeError:
        raise

    # Group lines by path for comment stripping at file granularity (cheaper
    # than reading every file when git already scoped to tracked *.java).
    by_path: dict[str, list[tuple[int, str]]] = {}
    for path, line_no, text in hits:
        by_path.setdefault(path, []).append((line_no, text))

    offenders: list[str] = []
    for path, lines in sorted(by_path.items()):
        if _is_test_path(path):
            continue
        if _is_script_allowlisted(path):
            continue
        if _is_always_allowlisted(path):
            continue
        if _residual_reason(path) is not None:
            continue

        abs_path = repo_root / path
        if abs_path.is_file():
            try:
                source = abs_path.read_text(encoding="utf-8", errors="replace")
            except OSError as exc:
                LOGGER.warning("could not read %s: %s", path, exc)
                source = "\n".join(t for _, t in lines)
        else:
            # Fake git-only trees in unit tests may not have full file content
            # if only staged blob is available — fall back to grepped lines.
            source = "\n".join(t for _, t in lines)

        if _code_mentions_token(source):
            offenders.append(path)

    return offenders


def _print_allowlist() -> None:
    print("== Always allow (interface + typed bridges) ==")
    for path in sorted(ALWAYS_ALLOW_EXACT):
        print(f"  {path}")
    print("== Residual production allow-list (shrink as retypes land) ==")
    for prefix, reason in RESIDUAL_ALLOW_PREFIXES:
        print(f"  {prefix}")
        print(f"    reason: {reason}")
    print("== Script self-allow ==")
    for prefix in SCRIPT_ALLOW_PREFIXES:
        print(f"  {prefix}")


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    if args.list_allowlist:
        _print_allowlist()
        return 0

    repo_root = Path(args.repo_root)
    print("==> scanning production Java for bare IPSObjectStoreErrors usages")
    try:
        offenders = _paths_with_code_hits(repo_root)
    except RuntimeError as exc:
        print(f"  FAIL: {exc}", file=sys.stderr)
        print("verify-no-bare-ipsobjectstoreerrors: FAIL", file=sys.stderr)
        return 1

    if offenders:
        print(
            "  FAIL: new bare IPSObjectStoreErrors production call-site(s) "
            "outside allow-list:",
            file=sys.stderr,
        )
        for path in offenders:
            print(f"    {path}", file=sys.stderr)
        print(
            "  Prefer typed ObjectStoreErrorCodes / ObjectStoreErrorCode / "
            "DesignErrorCodes. To document an intentional residual, extend "
            "RESIDUAL_ALLOW_PREFIXES in this script with an issue link "
            "(#3143 / parent #2616).",
            file=sys.stderr,
        )
        print("verify-no-bare-ipsobjectstoreerrors: FAIL", file=sys.stderr)
        return 1

    print("  OK: no new bare production IPSObjectStoreErrors call-sites")
    print("verify-no-bare-ipsobjectstoreerrors: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
