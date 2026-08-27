#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Grep gate: freeze bare production ``IPS*Errors`` call-sites (#3586).

Purpose
-------
Parent #2616 unifies legacy ``IPS*Errors`` int catalogs onto typed
``*ErrorCodes`` peers. The ObjectStore family already has a dedicated gate
(``verify-no-bare-ipsobjectstoreerrors.py``, #3143). This gate covers the
**remaining** live ``IPS*Errors`` families so **new** production Java sources
cannot grow bare imports / ``implements`` / qualified constant uses without
an explicit allow-list entry.

Allow-list classes
------------------
1. **Interface definitions** — files named ``IPS*Errors.java``
2. **Typed *ErrorCodes bridges** — files named ``*ErrorCodes.java`` or
   ``*ErrorCode.java`` (perc-auditlog enums and utils-local peers)
3. **Documented residual production call-sites** — exact paths in
   ``scripts/ipserrors-residual-allowlist.txt``. Prefer exact files so a
   **new** file under the same tree fails until it is listed with an issue
   link. Shrink as retypes merge (#3585/#3861 webservices, #3739/#3740 deployer,
   #3882 cms builders, #3883 cms handlers, #3884 cms.objectstore+client).

``IPSObjectStoreErrors`` is **excluded** (sibling #3143 gate). Tests and
comment/javadoc-only mentions are ignored.

Usage
-----
::

    python3 scripts/verify-no-bare-ipserrors.py [--repo-root <path>]
    python3 scripts/verify-no-bare-ipserrors.py --list-allowlist
    python3 scripts/verify-no-bare-ipserrors.py --dump-residuals

Windows::

    python scripts\\verify-no-bare-ipserrors.py [--repo-root <path>]

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

# Dedicated sibling gate — do not double-count ObjectStore residuals here.
EXCLUDED_FAMILIES: frozenset[str] = frozenset({"IPSObjectStoreErrors"})

# git grep POSIX ERE (not PCRE — no \\b). Token word-boundaries applied in Python.
GREP_PATTERN = r"IPS[A-Za-z0-9]+Errors"
GREP_PATHSPECS = ("*.java",)

TOKEN_RE = re.compile(r"\b(IPS[A-Za-z0-9]+Errors)\b")
INTERFACE_NAME_RE = re.compile(r"^IPS[A-Za-z0-9]+Errors\.java$")

RESIDUAL_ALLOWLIST_FILE = Path(__file__).resolve().parent / (
    "ipserrors-residual-allowlist.txt"
)

# This gate, its tests, and the residual list may mention family tokens.
SCRIPT_ALLOW_PREFIXES: tuple[str, ...] = (
    "scripts/verify-no-bare-ipserrors.py",
    "scripts/test_verify_no_bare_ipserrors.py",
    "scripts/ipserrors-residual-allowlist.txt",
)

# Strip block comments then line comments for a coarse "is this code?" check.
_BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.DOTALL)
_LINE_COMMENT_RE = re.compile(r"//.*?$", re.MULTILINE)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="verify-no-bare-ipserrors.py",
        description=(
            "Fail on new bare production IPS*Errors call-sites outside the "
            "documented allow-list (#3586 / parent #2616). IPSObjectStoreErrors "
            "is covered by verify-no-bare-ipsobjectstoreerrors.py (#3143)."
        ),
    )
    parser.add_argument(
        "--repo-root",
        default=str(REPO_ROOT),
        help="Repo root for the checks (default: this script's repo root)",
    )
    parser.add_argument(
        "--allowlist",
        default=str(RESIDUAL_ALLOWLIST_FILE),
        help="Exact-path residual allow-list file (default: sibling txt)",
    )
    parser.add_argument(
        "--list-allowlist",
        action="store_true",
        help="Print allow-list rules/paths and exit 0",
    )
    parser.add_argument(
        "--dump-residuals",
        action="store_true",
        help=(
            "Print current production residual paths (not interfaces / "
            "typed bridges / tests) and exit 0 — maintenance helper"
        ),
    )
    return parser


def _norm_path(path: str) -> str:
    """Normalize git-grep paths to posix relative form.

    Only strip a leading ``./`` segment (not character-class ``lstrip("./")``,
    which would corrupt ``../foo`` into ``foo``). Match peer freeze scripts that
    only rewrite separators.
    """
    norm = path.replace("\\", "/")
    while norm.startswith("./"):
        norm = norm[2:]
    return norm


def _is_test_path(path: str) -> bool:
    """True for test / it / testFixtures sources under any module layout.

    Detects ``src/test``, ``src/it``, and ``src/testFixtures`` as consecutive
    path segments so both module-prefixed paths and repo-root layouts classify
    as tests. Also treats a ``test`` / ``tests`` directory that appears *before*
    any ``src`` segment as a legacy test root (``system/webservices/test/``).
    """
    norm = _norm_path(path)
    parts = Path(norm).parts
    for i in range(len(parts) - 1):
        if parts[i] == "src" and parts[i + 1] in ("test", "it", "testFixtures"):
            return True
    for i, part in enumerate(parts):
        if part in ("test", "tests") and "src" not in parts[: i + 1]:
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


def _is_interface_file(path: str) -> bool:
    return INTERFACE_NAME_RE.fullmatch(Path(_norm_path(path)).name) is not None


def _is_typed_bridge(path: str) -> bool:
    name = Path(_norm_path(path)).name
    return name.endswith("ErrorCodes.java") or name.endswith("ErrorCode.java")


def _is_always_allowlisted(path: str) -> bool:
    return _is_interface_file(path) or _is_typed_bridge(path)


def _load_residual_allowlist(allowlist_path: Path) -> frozenset[str]:
    """Load exact posix paths from the residual allow-list file.

    Blank lines and ``#`` comments are ignored. Entries are exact files only —
    a trailing slash does **not** become a prefix match.
    """
    if not allowlist_path.is_file():
        return frozenset()
    try:
        text = allowlist_path.read_text(encoding="utf-8", errors="replace")
    except OSError as exc:
        LOGGER.warning("could not read allow-list %s: %s", allowlist_path, exc)
        return frozenset()
    entries: set[str] = set()
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        entries.add(_norm_path(line))
    return frozenset(entries)


def _gated_tokens_in_text(text: str) -> set[str]:
    return {tok for tok in TOKEN_RE.findall(text) if tok not in EXCLUDED_FAMILIES}


def _code_has_gated_token(source: str) -> bool:
    """True if non-comment Java code still mentions a gated IPS*Errors family."""
    without_blocks = _BLOCK_COMMENT_RE.sub("", source)
    without_line = _LINE_COMMENT_RE.sub("", without_blocks)
    return bool(_gated_tokens_in_text(without_line))


def _git_grep_lines(repo_root: Path, pattern: str) -> list[tuple[str, int, str]]:
    """Return (path, line_no, text) from ``git grep -n -E``."""
    cmd = ["git", "grep", "-n", "-E", pattern, "--", *GREP_PATHSPECS]
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
        parts = raw.split(":", 2)
        if len(parts) < 3:
            continue
        path_part, line_s, text = parts
        try:
            line_no = int(line_s)
        except ValueError:
            m = re.match(r"^(.*):(\d+):(.*)$", raw)
            if not m:
                continue
            path_part, line_s, text = m.group(1), m.group(2), m.group(3)
            line_no = int(line_s)
        hits.append((_norm_path(path_part), line_no, text))
    return hits


def _paths_with_gated_code(
    repo_root: Path,
) -> dict[str, list[tuple[int, str]]]:
    """Map tracked Java paths → grep hits that mention a gated family token."""
    hits = _git_grep_lines(repo_root, GREP_PATTERN)
    by_path: dict[str, list[tuple[int, str]]] = {}
    for path, line_no, text in hits:
        if not _gated_tokens_in_text(text):
            # Line only mentions excluded IPSObjectStoreErrors (or a non-token).
            continue
        by_path.setdefault(path, []).append((line_no, text))
    return by_path


def _read_source(
    repo_root: Path, path: str, lines: list[tuple[int, str]]
) -> str:
    abs_path = repo_root / path
    if abs_path.is_file():
        try:
            return abs_path.read_text(encoding="utf-8", errors="replace")
        except OSError as exc:
            LOGGER.warning("could not read %s: %s", path, exc)
    return "\n".join(t for _, t in lines)


def _iter_production_residuals(
    repo_root: Path,
) -> list[str]:
    """Production paths with gated tokens that are not always-allow / tests."""
    by_path = _paths_with_gated_code(repo_root)
    residuals: list[str] = []
    for path, lines in sorted(by_path.items()):
        if _is_test_path(path):
            continue
        if _is_script_allowlisted(path):
            continue
        if _is_always_allowlisted(path):
            continue
        source = _read_source(repo_root, path, lines)
        if _code_has_gated_token(source):
            residuals.append(path)
    return residuals


def _offenders(repo_root: Path, residual_allow: frozenset[str]) -> list[str]:
    return [p for p in _iter_production_residuals(repo_root) if p not in residual_allow]


def _print_allowlist(residual_allow: frozenset[str]) -> None:
    print("== Always allow (filename rules) ==")
    print("  IPS*Errors.java  (legacy constant interfaces)")
    print("  *ErrorCodes.java / *ErrorCode.java  (typed bridges)")
    print("== Excluded families (sibling gates) ==")
    for name in sorted(EXCLUDED_FAMILIES):
        print(f"  {name}  (scripts/verify-no-bare-ipsobjectstoreerrors.py #3143)")
    print("== Residual production allow-list exact (shrink as retypes land) ==")
    print(f"  file: scripts/ipserrors-residual-allowlist.txt ({len(residual_allow)} paths)")
    print(
        "  siblings: #3585/#3861 webservices, #3739/#3740 deployer, "
        "#3882 cms builders, #3883 cms handlers, #3884 cms.objectstore+client"
    )
    for path in sorted(residual_allow):
        print(f"  {path}")
    print("== Script self-allow ==")
    for prefix in SCRIPT_ALLOW_PREFIXES:
        print(f"  {prefix}")


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    allowlist_path = Path(args.allowlist)
    residual_allow = _load_residual_allowlist(allowlist_path)

    if args.list_allowlist:
        _print_allowlist(residual_allow)
        return 0

    repo_root = Path(args.repo_root)

    if args.dump_residuals:
        try:
            residuals = _iter_production_residuals(repo_root)
        except RuntimeError as exc:
            print(f"  FAIL: {exc}", file=sys.stderr)
            return 1
        for path in residuals:
            print(path)
        return 0

    print("==> scanning production Java for new bare IPS*Errors usages")
    try:
        offenders = _offenders(repo_root, residual_allow)
    except RuntimeError as exc:
        print(f"  FAIL: {exc}", file=sys.stderr)
        print("verify-no-bare-ipserrors: FAIL", file=sys.stderr)
        return 1

    if offenders:
        print(
            "  FAIL: new bare IPS*Errors production call-site(s) "
            "outside allow-list:",
            file=sys.stderr,
        )
        for path in offenders:
            print(f"    {path}", file=sys.stderr)
        print(
            "  Prefer typed *ErrorCodes (perc-auditlog) with explicit "
            "isAuditable. To document an intentional residual, add the "
            "exact path to scripts/ipserrors-residual-allowlist.txt with an "
            "issue link (#3586 / parent #2616 / SiteManage #3584 / "
            "webservices #3585/#3861 / cms builders #3882 / cms handlers #3883 / "
            "cms.objectstore+client #3884). Do not add directory prefixes.",
            file=sys.stderr,
        )
        print("verify-no-bare-ipserrors: FAIL", file=sys.stderr)
        return 1

    print("  OK: no new bare production IPS*Errors call-sites")
    print("verify-no-bare-ipserrors: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
