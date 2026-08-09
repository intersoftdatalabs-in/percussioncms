#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""QA preflight: detect a stale WebUI WAR in the dist tree vs the freshly
built sitemanage SNAPSHOT (#2486 / #2532).

When a developer / CI pipeline rebuilds sitemanage without re-packaging
perc-distribution-tree, the unpack step copies a **stale**
``perc-web-ui-*.war`` (whose ``sitemanage-*.jar`` inside
``WEB-INF/lib`` is older than the snapshot in ``~/.m2``) into the
Rhythmyx webapp. Docker then loads the WAR; the stale sitemanage jar
overrides the live fix on the classpath, and cycle / DI regressions
reappear in the container — even though the developer believes the
fix is on main.

The preflight compares:

* the ``sitemanage-*.jar`` inside ``~/.m2`` (the source of truth the
  developer just rebuilt), and
* the ``sitemanage-*.jar`` inside the **WebUI WAR** that the dist
  tree will unpack into the container.

**Content-hash mode (default, #2532):** SHA-256 of the m2 jar bytes vs
the ``WEB-INF/lib/sitemanage-*.jar`` zip entry inside the WAR. This is
mtime-resistant: clock skew, restore-from-cache, and ``touch``-only
rebuilds no longer produce false FRESH / false STALE. When both hashes
are available they are the primary signal; mtime is the fallback when
hashing is unavailable. Optional dist-tree jar is compared the same way
when present.

If the WAR-side jar content differs from (or is missing vs) the m2-side
jar, the preflight prints a clear, single-line ``STALE:`` summary so the
agent or human running ``perc-devctl qa-up`` can fix the chain before the
container starts. Exit code is ``0`` when fresh, ``2`` when stale
(strict). A non-strict mode (``--no-strict``) only prints the
``STALE:`` line and returns ``0`` so callers can log without blocking.

The check is filesystem-only — no docker, no curl, no maven. Operators
run ``python3 docker/scripts/qa_preflight.py`` (or
``perc-devctl.py qa-preflight``) before ``qa-up``.
"""

from __future__ import annotations

import argparse
import hashlib
import logging
import os
import re
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Optional

# Hard-coded product coordinates. Kept here (not in env) because the
# preflight is a developer-machine tool and the coordinates are
# stable across 8.2.x.
_M2_GROUP = "com/percussion/sitemanage"
_M2_ARTIFACT = "sitemanage"
_M2_DIR = _M2_GROUP + "/" + _M2_ARTIFACT
_SITEMANAGE_JAR_PATTERN = re.compile(
    r"^" + re.escape(_M2_ARTIFACT) + r"-.*\.jar$"
)
_WEBUI_WAR_PATTERN = re.compile(
    r"^perc-web-ui-.*\.war$"
)
# Chunk size for streaming SHA-256 over jar / zip entry bytes.
_HASH_CHUNK = 1024 * 1024

LOG = logging.getLogger("qa_preflight")


@dataclass(frozen=True)
class PreflightResult:
    """One row in the preflight report."""

    label: str  # "m2:sitemanage" / "war:sitemanage" / "war:webui" / "dist:sitemanage"
    path: Optional[Path]
    mtime: Optional[float]  # seconds since epoch; None when missing
    sha256: Optional[str] = None  # hex digest; None when missing or not computed

    def is_present(self) -> bool:
        return self.path is not None and self.mtime is not None


def _resolve_m2_dir(m2_root: Path) -> Path:
    """Return the maven local repo directory for the sitemanage artifact."""
    return m2_root / _M2_DIR


def find_sitemanage_in_m2(m2_root: Path) -> Optional[Path]:
    """Locate the newest ``sitemanage-*.jar`` under the maven local repo."""
    base = _resolve_m2_dir(m2_root)
    if not base.is_dir():
        return None
    candidates = [p for p in base.iterdir() if _SITEMANAGE_JAR_PATTERN.match(p.name)]
    if not candidates:
        return None
    # Pick the most recently modified — SNAPSHOT refresh leaves dated
    # copies in place; the newest is the active install candidate.
    return max(candidates, key=lambda p: p.stat().st_mtime)


def find_webui_war(repo_root: Path) -> Optional[Path]:
    """Locate the newest ``perc-web-ui-*.war`` under ``WebUI/target``."""
    target = repo_root / "WebUI" / "target"
    if not target.is_dir():
        return None
    candidates = [p for p in target.iterdir() if _WEBUI_WAR_PATTERN.match(p.name)]
    if not candidates:
        return None
    return max(candidates, key=lambda p: p.stat().st_mtime)


def find_sitemanage_in_dist(repo_root: Path) -> Optional[Path]:
    """Locate an optional loose ``sitemanage-*.jar`` under the dist tree.

    Looks under ``modules/perc-distribution-tree/target`` for a jar whose
    name matches ``sitemanage-*.jar`` (depth-limited walk). Missing dist
    layout is normal for many developer trees — return ``None`` and skip
    the optional dist row.
    """
    target = repo_root / "modules" / "perc-distribution-tree" / "target"
    if not target.is_dir():
        return None
    candidates: list[Path] = []
    # Depth-limited walk via Path.rglob is fine for target trees; filter
    # by name pattern only (portable Path, no hardcoded separators).
    try:
        for p in target.rglob("sitemanage-*.jar"):
            if p.is_file() and _SITEMANAGE_JAR_PATTERN.match(p.name):
                candidates.append(p)
    except OSError:
        return None
    if not candidates:
        return None
    return max(candidates, key=lambda p: p.stat().st_mtime)


def war_bundles_sitemanage(war_path: Path) -> bool:
    """Return ``True`` when the WAR contains a ``sitemanage-*.jar`` entry.

    We use the entry-name check rather than extracting the JAR so the
    preflight stays filesystem-only and never touches a multi-GB
    extract cache. A bundled ``sitemanage-*.jar`` is the explicit
    signal that the dist tree will unpack a stale sitemanage into the
    Rhythmyx webapp.
    """
    return find_sitemanage_zip_entry(war_path) is not None


def find_sitemanage_zip_entry(archive_path: Optional[Path]) -> Optional[str]:
    """Return the zip entry name of ``sitemanage-*.jar`` inside a WAR/zip.

    Prefers ``WEB-INF/lib/sitemanage-*.jar`` when present; otherwise the
    first matching ``sitemanage-*.jar`` basenamed entry. Zip entry paths
    always use ``/`` (ZIP format), independent of OS separators.
    """
    if archive_path is None or not archive_path.is_file():
        return None
    try:
        with zipfile.ZipFile(archive_path, "r") as zf:
            lib_hits: list[str] = []
            other_hits: list[str] = []
            for name in zf.namelist():
                base = name.rsplit("/", 1)[-1]
                if not (base.startswith(_M2_ARTIFACT + "-") and base.endswith(".jar")):
                    continue
                # Prefer WEB-INF/lib/… (product layout)
                if "/WEB-INF/lib/" in ("/" + name.replace("\\", "/")) or name.startswith(
                    "WEB-INF/lib/"
                ):
                    lib_hits.append(name)
                else:
                    other_hits.append(name)
            if lib_hits:
                return sorted(lib_hits)[0]
            if other_hits:
                return sorted(other_hits)[0]
    except (OSError, zipfile.BadZipFile):
        return None
    return None


def sha256_file(path: Optional[Path]) -> Optional[str]:
    """Stream SHA-256 hex digest of a file; ``None`` when missing/unreadable."""
    if path is None or not path.is_file():
        return None
    try:
        h = hashlib.sha256()
        with path.open("rb") as fh:
            while True:
                chunk = fh.read(_HASH_CHUNK)
                if not chunk:
                    break
                h.update(chunk)
        return h.hexdigest()
    except OSError:
        return None


def sha256_zip_entry(archive_path: Optional[Path], entry_name: Optional[str]) -> Optional[str]:
    """Stream SHA-256 of one zip entry without extracting to disk."""
    if archive_path is None or entry_name is None or not archive_path.is_file():
        return None
    try:
        with zipfile.ZipFile(archive_path, "r") as zf:
            h = hashlib.sha256()
            with zf.open(entry_name, "r") as entry:
                while True:
                    chunk = entry.read(_HASH_CHUNK)
                    if not chunk:
                        break
                    h.update(chunk)
            return h.hexdigest()
    except (OSError, KeyError, zipfile.BadZipFile):
        return None


def sha256_sitemanage_in_war(war_path: Optional[Path]) -> Optional[str]:
    """SHA-256 of the bundled ``sitemanage-*.jar`` entry inside a WAR."""
    entry = find_sitemanage_zip_entry(war_path)
    return sha256_zip_entry(war_path, entry)


def _mtime(path: Optional[Path]) -> Optional[float]:
    if path is None or not path.exists():
        return None
    return path.stat().st_mtime


def run_preflight(
    repo_root: Path,
    m2_root: Path,
    *,
    content_hash: bool = True,
) -> list[PreflightResult]:
    """Compute the preflight rows; never raises.

    Missing artifacts are reported as ``is_present() == False`` rather
    than as exceptions so the caller can render a single summary line
    and decide on exit code based on the strict / non-strict policy.

    When ``content_hash`` is True (default), each present jar/war-entry
    row carries a ``sha256`` hex digest for content comparison.
    """
    m2_jar = find_sitemanage_in_m2(m2_root)
    webui_war = find_webui_war(repo_root)
    war_entry = find_sitemanage_zip_entry(webui_war) if webui_war else None
    war_bundles = war_entry is not None
    dist_jar = find_sitemanage_in_dist(repo_root)

    m2_sha = sha256_file(m2_jar) if content_hash else None
    war_sha = (
        sha256_zip_entry(webui_war, war_entry)
        if content_hash and war_bundles
        else None
    )
    dist_sha = sha256_file(dist_jar) if content_hash else None

    rows = [
        PreflightResult("m2:sitemanage", m2_jar, _mtime(m2_jar), m2_sha),
        PreflightResult("war:webui", webui_war, _mtime(webui_war), None),
        # ``war:sitemanage`` is a derived row: present iff the WAR
        # bundles a sitemanage jar; its mtime mirrors the WAR file so
        # the mtime fallback is "WAR built before m2 was updated".
        PreflightResult(
            "war:sitemanage",
            webui_war if war_bundles else None,
            _mtime(webui_war) if war_bundles else None,
            war_sha,
        ),
    ]
    # Optional dist row — only include when a loose dist jar exists so
    # reports stay short for the common "no dist target" case.
    if dist_jar is not None:
        rows.append(
            PreflightResult(
                "dist:sitemanage",
                dist_jar,
                _mtime(dist_jar),
                dist_sha,
            )
        )
    return rows


def _hash_pair(
    rows: Iterable[PreflightResult], left: str, right: str
) -> tuple[Optional[str], Optional[str]]:
    by_label = {r.label: r for r in rows}
    a = by_label.get(left)
    b = by_label.get(right)
    return (
        a.sha256 if a is not None else None,
        b.sha256 if b is not None else None,
    )


def content_hash_mismatch(rows: Iterable[PreflightResult]) -> Optional[str]:
    """Return a short mismatch label when content hashes disagree.

    Checks m2 vs WAR first (primary acceptance for #2532), then m2 vs
    optional dist. Returns ``None`` when hashes match or are unavailable.
    """
    row_list = list(rows)
    m2_h, war_h = _hash_pair(row_list, "m2:sitemanage", "war:sitemanage")
    if m2_h is not None and war_h is not None and m2_h != war_h:
        return "m2-vs-war"
    m2_h, dist_h = _hash_pair(row_list, "m2:sitemanage", "dist:sitemanage")
    if m2_h is not None and dist_h is not None and m2_h != dist_h:
        return "m2-vs-dist"
    return None


def content_hashes_agree(rows: Iterable[PreflightResult]) -> bool:
    """True when m2 and war hashes are both present and equal."""
    m2_h, war_h = _hash_pair(list(rows), "m2:sitemanage", "war:sitemanage")
    return m2_h is not None and war_h is not None and m2_h == war_h


def is_stale(rows: Iterable[PreflightResult], *, content_hash: bool = True) -> bool:
    """Return ``True`` when the WAR-side sitemanage is stale vs m2.

    Missing m2-side jar is **not** considered stale (the developer may
    not have built sitemanage yet); the preflight prints a ``NOTE:``
    line so the operator knows the check was a no-op.

    When ``content_hash`` is True and both m2 + war hashes are available
    they are the **primary** signal (mtime-resistant, #2532):

    * hashes differ → STALE
    * hashes match → FRESH (even if mtimes look inverted)

    When hashes are unavailable, fall back to WAR mtime vs m2 mtime.
    Missing WAR / missing bundled sitemanage still counts as STALE.
    """
    row_list = list(rows)
    by_label = {r.label: r for r in row_list}
    m2 = by_label.get("m2:sitemanage")
    war = by_label.get("war:sitemanage")
    war_war = by_label.get("war:webui")
    if m2 is None or not m2.is_present():
        return False
    if war_war is None or not war_war.is_present():
        return True  # no WAR at all → qa-up will fail anyway
    if war is None or not war.is_present():
        return True  # WAR present but does not bundle sitemanage → cannot preflight

    if content_hash:
        mismatch = content_hash_mismatch(row_list)
        if mismatch is not None:
            return True
        if content_hashes_agree(row_list):
            # Content matches — also require optional dist match if present
            # (content_hash_mismatch already caught dist mismatch above).
            return False
        # Hashes unavailable on one side — fall through to mtime.

    return war.mtime < m2.mtime  # type: ignore[operator]


def format_report(
    rows: Iterable[PreflightResult],
    strict: bool,
    *,
    content_hash: bool = True,
) -> str:
    """Render the preflight rows as a single-line summary plus detail."""
    row_list = list(rows)
    by_label = {r.label: r for r in row_list}
    stale = is_stale(row_list, content_hash=content_hash)
    m2 = by_label.get("m2:sitemanage")

    if m2 is None or not m2.is_present():
        note = "NOTE: no sitemanage jar in m2 — build sitemanage first; check skipped"
    elif stale:
        mismatch = content_hash_mismatch(row_list) if content_hash else None
        if mismatch == "m2-vs-war":
            note = (
                "STALE: sitemanage content hash mismatch (m2 vs war) — "
                "repackage WebUI / perc-distribution-tree "
                "(hashes differ even if mtimes look fresh)"
            )
        elif mismatch == "m2-vs-dist":
            note = (
                "STALE: sitemanage content hash mismatch (m2 vs dist) — "
                "repackage perc-distribution-tree"
            )
        else:
            note = (
                "STALE: war:sitemanage is older than m2:sitemanage — "
                "repackage perc-distribution-tree (or run mvn install on the WebUI module)"
            )
    else:
        if content_hash and content_hashes_agree(row_list):
            note = "FRESH: m2 and war sitemanage content hashes match"
        else:
            note = "FRESH: war:sitemanage >= m2:sitemanage"

    parts = [
        f"PREFLIGHT: {note}",
        f"  m2:sitemanage   = {_render(by_label.get('m2:sitemanage'))}",
        f"  war:webui       = {_render(by_label.get('war:webui'))}",
        f"  war:sitemanage  = {_render(by_label.get('war:sitemanage'))}",
    ]
    dist = by_label.get("dist:sitemanage")
    if dist is not None:
        parts.append(f"  dist:sitemanage = {_render(dist)}")
    if not strict and stale:
        parts.append("  (non-strict mode: returning OK; qa-up will likely use a stale jar)")
    return "\n".join(parts)


def _render(row: Optional[PreflightResult]) -> str:
    if row is None or not row.is_present():
        return "<missing>"
    sha = f"  sha256={row.sha256[:12]}…" if row.sha256 else ""
    return f"{row.path}  mtime={row.mtime:.0f}{sha}"


def _default_m2_root() -> Path:
    """Cross-platform default for the maven local repository root."""
    # `Path("~")` does NOT expand the tilde on Windows; expanduser on
    # the string is the portable way to resolve the user's home.
    home = os.path.expanduser("~")
    return Path(home) / ".m2" / "repository"


def main(argv: Optional[list[str]] = None) -> int:
    parser = argparse.ArgumentParser(
        prog="qa-preflight",
        description=(
            "Detect a stale WebUI WAR vs a freshly built sitemanage "
            "SNAPSHOT before running perc-devctl qa-up (#2486 / #2532)."
        ),
    )
    parser.add_argument(
        "--repo-root",
        type=Path,
        default=Path(os.getcwd()),
        help="Path to the Percussion CMS checkout root (default: cwd).",
    )
    parser.add_argument(
        "--m2-root",
        type=Path,
        default=_default_m2_root(),
        help="Maven local repository root (default: ~/.m2/repository).",
    )
    strict_group = parser.add_mutually_exclusive_group()
    strict_group.add_argument(
        "--strict",
        dest="strict",
        action="store_true",
        help="Exit non-zero when stale.",
    )
    strict_group.add_argument(
        "--no-strict",
        dest="strict",
        action="store_false",
        help="Print STALE: but exit 0 (default).",
    )
    parser.set_defaults(strict=False)
    hash_group = parser.add_mutually_exclusive_group()
    hash_group.add_argument(
        "--content-hash",
        dest="content_hash",
        action="store_true",
        help=(
            "Compare SHA-256 of m2 sitemanage jar vs WAR zip entry "
            "(default; mtime-resistant, #2532)."
        ),
    )
    hash_group.add_argument(
        "--no-content-hash",
        dest="content_hash",
        action="store_false",
        help="Mtime-only comparison (legacy #2486 behaviour).",
    )
    parser.set_defaults(content_hash=True)
    parser.add_argument(
        "--log-file",
        type=Path,
        default=None,
        help="Optional path to append the report to (agent workflows).",
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="Verbose logging on stderr.",
    )
    args = parser.parse_args(argv)

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )

    rows = run_preflight(
        args.repo_root, args.m2_root, content_hash=args.content_hash
    )
    stale = is_stale(rows, content_hash=args.content_hash)
    report = format_report(rows, strict=args.strict, content_hash=args.content_hash)
    print(report)
    if args.log_file is not None:
        args.log_file.parent.mkdir(parents=True, exist_ok=True)
        with args.log_file.open("a", encoding="utf-8") as fh:
            fh.write(report + "\n")

    if stale and args.strict:
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
