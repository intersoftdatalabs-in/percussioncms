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

SNAPSHOT provenance (#6709): the same run also verifies that the
``rest`` / ``sitemanage`` SNAPSHOT jars in the resolved local repository
were built from *this* checkout. Concurrent agent worktrees share one
``~/.m2`` (or one ``~/.m2-night``); a ``clean install`` from a feature
branch silently overwrites the SNAPSHOTs another cell's ``qa-up`` then
boots — a Frankenstein mix that fails with Spring injection errors
looking like product defects. Any ``.class`` entry in the m2 jar whose
top-level type has no ``.java`` source in this checkout is reported as
``POISONED:`` (strict mode exits ``2``). Disable with
``--no-provenance``.
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

_SCRIPTS_DIR = Path(__file__).resolve().parent
if str(_SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS_DIR))
import maven_local_repo  # noqa: E402  # resolve -Dmaven.repo.local like mvn (#6709)

# Hard-coded product coordinates. Kept here (not in env) because the
# preflight is a developer-machine tool and the coordinates are
# stable across 8.2.x.
# NOTE (#6740): for groupId ``com.percussion`` the standard Maven
# local-repo layout is ``com/percussion/<artifactId>/<version>/``
# (groupPath/artifactId/version) — e.g. ``com/percussion/sitemanage/``
# holds the version dirs directly. The doubled ``.../rest/rest/...``
# path would only be standard for a groupId of ``com.percussion.rest``,
# which no pom in this repo declares (verified by repo-wide grep).
_M2_GROUP = "com/percussion/sitemanage"
_M2_ARTIFACT = "sitemanage"
_M2_DIR = _M2_GROUP
_SITEMANAGE_JAR_PATTERN = re.compile(
    r"^" + re.escape(_M2_ARTIFACT) + r"-.*\.jar$"
)
_WEBUI_WAR_PATTERN = re.compile(
    r"^perc-web-ui-.*\.war$"
)
# Chunk size for streaming SHA-256 over jar / zip entry bytes.
_HASH_CHUNK = 1024 * 1024

# --- SNAPSHOT provenance (#6709) -------------------------------------------
# (label, m2 group-relative dir, jar-name prefix, checkout source roots).
# The group dir is searched at any depth. NOTE (#6740): for groupId
# ``com.percussion`` the single-level ``com/percussion/rest/<version>/``
# layout IS the standard Maven layout (groupPath/artifactId/version) —
# no repo script or Maven invocation produces it, because nothing needs
# to: ``mvn install`` writes it directly. The doubled
# ``com/percussion/rest/rest/<version>/`` path would only be standard for
# a groupId of ``com.percussion.rest``, which no pom declares.
_PROVENANCE_ARTIFACTS: tuple[
    tuple[str, str, str, tuple[str, ...]], ...
] = (
    ("rest", "com/percussion/rest", "rest", ("rest/src/main/java",)),
    (
        "sitemanage",
        "com/percussion/sitemanage",
        "sitemanage",
        ("projects/sitemanage/src/main/java",),
    ),
)
# Jar-name suffixes that are never the installed binary (same stem match).
_PROVENANCE_SKIP_SUFFIXES = ("-sources.jar", "-javadoc.jar")
# Max foreign class entries shown per artifact; the total count is always
# reported so long tails do not flood agent logs.
_PROVENANCE_SAMPLE = 10

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
    """Locate the newest ``sitemanage-*.jar`` under the maven local repo.

    Standard Maven layout (``.../sitemanage/<version>/`` for groupId
    ``com.percussion``) wins; falls back to an any-depth search under the
    group dir so the preflight is not blind on hosts that carry an extra
    nesting level (#6709, root-caused in #6740).
    """
    standard = _resolve_m2_dir(m2_root)
    if standard.is_dir():
        candidates: list[Path] = []
        try:
            for version_dir in standard.iterdir():
                if not version_dir.is_dir():
                    continue
                candidates.extend(
                    p
                    for p in version_dir.iterdir()
                    if p.is_file()
                    and _SITEMANAGE_JAR_PATTERN.match(p.name)
                    and not p.name.endswith(_PROVENANCE_SKIP_SUFFIXES)
                )
        except OSError:
            candidates = []
        if candidates:
            return max(candidates, key=lambda p: p.stat().st_mtime)
    return find_snapshot_jar(m2_root, "com/percussion/sitemanage", "sitemanage")


def find_snapshot_jar(
    m2_root: Path, group_rel: str, artifact: str
) -> Optional[Path]:
    """Locate the newest ``<artifact>-*.jar`` at any depth under a group dir.

    Covers the standard Maven layout (``.../<artifact>/<version>/`` for
    groupId ``com.percussion``) plus any extra nesting level, so the
    preflight keeps working even if a host carries a non-standard depth
    (#6709, root-caused in #6740).
    ``*-sources.jar`` / ``*-javadoc.jar`` never match. Returns ``None``
    when the group dir is absent or holds no binary jar.
    """
    base = m2_root / Path(*group_rel.split("/"))
    if not base.is_dir():
        return None
    pattern = re.compile(r"^" + re.escape(artifact) + r"-.*\.jar$")
    candidates: list[Path] = []
    try:
        for p in base.rglob("*.jar"):
            if not p.is_file():
                continue
            if not pattern.match(p.name):
                continue
            if p.name.endswith(_PROVENANCE_SKIP_SUFFIXES):
                continue
            candidates.append(p)
    except OSError:
        return None
    if not candidates:
        return None
    # SNAPSHOT refresh leaves dated copies in place; the newest is the
    # active install candidate.
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


@dataclass(frozen=True)
class ProvenanceFinding:
    """One artifact whose m2 SNAPSHOT jar does not match this checkout."""

    label: str  # "rest" / "sitemanage"
    jar: Path
    foreign_count: int  # total .class entries with no checkout source
    foreign_sample: tuple[str, ...]  # first _PROVENANCE_SAMPLE entry names


def _java_source_index(repo_root: Path, source_roots: Iterable[str]) -> set[str]:
    """Index ``<top-level-type>.java`` paths (POSIX form) under source roots.

    Inner-class entries (``Foo$Bar.class``) map to ``Foo.java`` at lookup
    time, so only top-level ``*.java`` stems are indexed. Annotation
    processing output (``target/generated-sources/*``) counts as source
    when the module was built in this checkout; a ``clean`` checkout
    without rebuilt generated sources may over-report — the finding text
    says to rebuild in that case.
    """
    index: set[str] = set()
    roots: list[Path] = []
    for rel in source_roots:
        root = repo_root / Path(*rel.split("/"))
        if root.is_dir():
            roots.append(root)
        # Module dir is the source root minus trailing src/main/java.
        parts = Path(*rel.split("/")).parts
        if len(parts) > 3 and parts[-3:] == ("src", "main", "java"):
            gen = repo_root / Path(*parts[:-3]) / "target" / "generated-sources"
            if gen.is_dir():
                try:
                    roots.extend(d for d in gen.iterdir() if d.is_dir())
                except OSError:
                    pass
    for root in roots:
        try:
            for java in root.rglob("*.java"):
                try:
                    index.add(java.relative_to(root).with_suffix("").as_posix())
                except ValueError:
                    continue
        except OSError:
            continue
    return index


def _jar_class_entries(jar_path: Path) -> list[str]:
    """List ``.class`` entries in a jar, minus metadata and package-info."""
    try:
        with zipfile.ZipFile(jar_path, "r") as zf:
            return [
                name
                for name in zf.namelist()
                if name.endswith(".class")
                and not name.startswith("META-INF/")
                and name != "module-info.class"
                and not name.endswith("/package-info.class")
            ]
    except (OSError, zipfile.BadZipFile):
        return []


def foreign_classes_in_jar(jar_path: Path, sources: set[str]) -> list[str]:
    """Return class entries whose top-level type has no ``.java`` source."""
    foreign: list[str] = []
    for entry in _jar_class_entries(jar_path):
        top = entry[: -len(".class")].split("$")[0]
        if top not in sources:
            foreign.append(entry)
    return sorted(foreign)


def check_snapshot_provenance(
    repo_root: Path, m2_root: Path
) -> list[ProvenanceFinding]:
    """Flag m2 SNAPSHOT jars built from a different checkout/branch (#6709).

    For each known artifact, when *both* the m2 jar and this checkout's
    source roots exist, every jar ``.class`` without a matching checkout
    ``.java`` is foreign — the jar came from another branch's
    ``clean install`` into the shared local repo. Missing jars or missing
    source roots (partial trees) are skipped, never flagged.
    """
    findings: list[ProvenanceFinding] = []
    for label, group_rel, artifact, source_roots in _PROVENANCE_ARTIFACTS:
        jar = find_snapshot_jar(m2_root, group_rel, artifact)
        if jar is None:
            continue
        if not any(
            (repo_root / Path(*rel.split("/"))).is_dir() for rel in source_roots
        ):
            continue
        sources = _java_source_index(repo_root, source_roots)
        foreign = foreign_classes_in_jar(jar, sources)
        if foreign:
            findings.append(
                ProvenanceFinding(
                    label=label,
                    jar=jar,
                    foreign_count=len(foreign),
                    foreign_sample=tuple(foreign[:_PROVENANCE_SAMPLE]),
                )
            )
    return findings


def is_poisoned(findings: Iterable[ProvenanceFinding]) -> bool:
    """Return ``True`` when any SNAPSHOT jar carries foreign classes."""
    return any(f.foreign_count > 0 for f in findings)


def format_provenance(findings: Iterable[ProvenanceFinding]) -> str:
    """Render the provenance section of the preflight report."""
    found = list(findings)
    if not found:
        return "PROVENANCE: OK — m2 SNAPSHOT jars match this checkout"
    lines = []
    for f in found:
        lines.append(
            f"POISONED: {f.label} {f.jar} carries "
            f"{f.foreign_count} class(es) with no source in this checkout "
            f"(branch-built jar in the shared local repo?) — "
            f"rebuild with `clean install` from this checkout before qa-up"
        )
        for entry in f.foreign_sample:
            lines.append(f"  foreign: {entry}")
        if f.foreign_count > len(f.foreign_sample):
            lines.append(
                f"  ... and {f.foreign_count - len(f.foreign_sample)} more"
            )
    return "\n".join(lines)


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


def _default_m2_root(repo_root: Optional[Path] = None) -> Path:
    """Maven local repository root, resolved the way ``mvn`` would (#6709).

    Honors ``-Dmaven.repo.local`` from ``MAVEN_ARGS`` / ``MAVEN_OPTS``, the
    mavenrc files, ``.mvn/maven.config`` and ``settings.xml`` before falling
    back to ``~/.m2/repository`` — see :mod:`maven_local_repo`.
    """
    return maven_local_repo.resolve_m2_repository(repo_root)


def main(argv: Optional[list[str]] = None) -> int:
    parser = argparse.ArgumentParser(
        prog="qa-preflight",
        description=(
            "Detect a stale WebUI WAR vs a freshly built sitemanage "
            "SNAPSHOT before running perc-devctl qa-up (#2486 / #2532), "
            "and verify m2 SNAPSHOT jars were built from this checkout "
            "(--no-provenance to skip, #6709)."
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
        default=None,
        help=(
            "Maven local repository root (default: resolved like mvn — "
            "-Dmaven.repo.local in MAVEN_ARGS/MAVEN_OPTS/~/.mavenrc/"
            ".mvn/maven.config, settings.xml, else ~/.m2/repository)."
        ),
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
    prov_group = parser.add_mutually_exclusive_group()
    prov_group.add_argument(
        "--provenance",
        dest="provenance",
        action="store_true",
        help=(
            "Verify m2 SNAPSHOT jars were built from this checkout "
            "(default; branch-built jars in the shared repo fail strict)."
        ),
    )
    prov_group.add_argument(
        "--no-provenance",
        dest="provenance",
        action="store_false",
        help="Skip the SNAPSHOT provenance check (#6709).",
    )
    parser.set_defaults(provenance=True)
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

    m2_root = (
        args.m2_root
        if args.m2_root is not None
        else _default_m2_root(args.repo_root)
    )
    rows = run_preflight(
        args.repo_root, m2_root, content_hash=args.content_hash
    )
    stale = is_stale(rows, content_hash=args.content_hash)
    findings = (
        check_snapshot_provenance(args.repo_root, m2_root)
        if args.provenance
        else []
    )
    poisoned = is_poisoned(findings)
    report = format_report(rows, strict=args.strict, content_hash=args.content_hash)
    if args.provenance:
        report += "\n" + format_provenance(findings)
    print(report)
    if args.log_file is not None:
        args.log_file.parent.mkdir(parents=True, exist_ok=True)
        with args.log_file.open("a", encoding="utf-8") as fh:
            fh.write(report + "\n")

    if (stale or poisoned) and args.strict:
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
