#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""QA rebuild-chain preflight: sitemanage → WebUI WAR → dist (#2486).

``perc-distribution-tree`` unpacks ``WebUI/target/perc-web-ui-*.war`` into
Rhythmyx ``WEB-INF/lib`` (including ``sitemanage-*.jar``). Packaging only
the dist module after a sitemanage-only install leaves a **stale**
sitemanage jar inside the WAR — Docker then proves the old cycle even
when ``~/.m2`` has the fix.

This preflight is filesystem-only (no docker, curl, or maven). It
compares modification times of:

1. ``~/.m2/repository/com/percussion/sitemanage/<ver>/sitemanage-*.jar``
   (newest non-classifier jar after ``mvn install`` on sitemanage)
2. ``WebUI/target/perc-web-ui-*.war`` (must be rebuilt after sitemanage)
3. ``modules/perc-distribution-tree/target/perc-distribution-tree.jar``
   (must be repackaged after the WebUI WAR)

Exit codes:

* ``0`` — FRESH (or check skipped when no m2 sitemanage jar), or
  non-strict mode after printing ``STALE:``
* ``2`` — STALE under ``--strict`` (default)
* ``1`` — invocation / unexpected error

Operators:

```text
python docker/scripts/qa_preflight.py --repo-root .
python docker/scripts/perc-devctl.py qa-preflight
# rebuild order when STALE:
#   sitemanage install → WebUI package → perc-distribution-tree package → qa-up
```
"""

from __future__ import annotations

import argparse
import logging
import os
import re
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Optional, Sequence

# Maven coordinates for sitemanage (groupId com.percussion, artifactId sitemanage).
# Layout: ~/.m2/repository/com/percussion/sitemanage/<version>/sitemanage-<ver>.jar
_M2_GROUP_PATH = Path("com") / "percussion" / "sitemanage"
_SITEMANAGE_JAR_RE = re.compile(r"^sitemanage-.+\.jar$")
_CLASSIFIER_SUFFIXES = ("-sources.jar", "-javadoc.jar", "-tests.jar")
_WEBUI_WAR_RE = re.compile(r"^perc-web-ui-.+\.war$")
_DIST_JAR_NAMES = ("perc-distribution-tree.jar",)

LOG = logging.getLogger("qa_preflight")

# Exit codes
EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_STALE = 2

REBUILD_HINT = (
    "rebuild chain: "
    "cd projects/sitemanage && ../../mvnw clean install; "
    "cd ../../WebUI && ../mvnw package -DskipTests; "
    "cd ../modules/perc-distribution-tree && ../../mvnw clean package -DskipTests; "
    "then qa-up"
)


@dataclass(frozen=True)
class ArtifactRef:
    """One artifact the preflight knows about."""

    label: str
    path: Optional[Path]
    mtime: Optional[float]

    def is_present(self) -> bool:
        return self.path is not None and self.mtime is not None


@dataclass(frozen=True)
class PreflightReport:
    """Structured result of a preflight run."""

    rows: List[ArtifactRef]
    stale: bool
    reasons: List[str]
    skipped: bool  # True when no m2 sitemanage — check is a no-op


def _mtime(path: Optional[Path]) -> Optional[float]:
    if path is None or not path.is_file():
        return None
    try:
        return path.stat().st_mtime
    except OSError:
        return None


def _is_main_sitemanage_jar(name: str) -> bool:
    if not _SITEMANAGE_JAR_RE.match(name):
        return False
    return not any(name.endswith(suf) for suf in _CLASSIFIER_SUFFIXES)


def find_sitemanage_in_m2(m2_root: Path) -> Optional[Path]:
    """Newest main ``sitemanage-*.jar`` under the local Maven repo.

    Walks ``com/percussion/sitemanage/<version>/`` (standard Maven layout).
    Skips sources/javadoc/tests classifiers.
    """
    base = Path(m2_root) / _M2_GROUP_PATH
    if not base.is_dir():
        return None
    candidates: List[Path] = []
    try:
        for p in base.rglob("sitemanage-*.jar"):
            if p.is_file() and _is_main_sitemanage_jar(p.name):
                candidates.append(p)
    except OSError:
        return None
    if not candidates:
        return None
    return max(candidates, key=lambda p: p.stat().st_mtime)


def find_webui_war(repo_root: Path) -> Optional[Path]:
    """Newest ``perc-web-ui-*.war`` under ``WebUI/target``."""
    target = Path(repo_root) / "WebUI" / "target"
    if not target.is_dir():
        return None
    try:
        candidates = [
            p for p in target.iterdir() if p.is_file() and _WEBUI_WAR_RE.match(p.name)
        ]
    except OSError:
        return None
    if not candidates:
        return None
    return max(candidates, key=lambda p: p.stat().st_mtime)


def find_dist_jar(repo_root: Path) -> Optional[Path]:
    """``perc-distribution-tree.jar`` under the distribution module target."""
    target = Path(repo_root) / "modules" / "perc-distribution-tree" / "target"
    if not target.is_dir():
        return None
    for name in _DIST_JAR_NAMES:
        candidate = target / name
        if candidate.is_file():
            return candidate
    # Fallback: any perc-distribution-tree*.jar that is not a plain module SNAPSHOT
    # sources jar (prefer exact finalName from assembly).
    try:
        candidates = [
            p
            for p in target.iterdir()
            if p.is_file()
            and p.name.startswith("perc-distribution-tree")
            and p.suffix == ".jar"
            and not any(p.name.endswith(suf) for suf in _CLASSIFIER_SUFFIXES)
        ]
    except OSError:
        return None
    if not candidates:
        return None
    # Prefer exact finalName if present among candidates.
    for name in _DIST_JAR_NAMES:
        for p in candidates:
            if p.name == name:
                return p
    return max(candidates, key=lambda p: p.stat().st_mtime)


def war_bundles_sitemanage(war_path: Optional[Path]) -> bool:
    """True when the WAR has a ``sitemanage-*.jar`` under ``WEB-INF/lib``."""
    if war_path is None or not war_path.is_file():
        return False
    try:
        with zipfile.ZipFile(war_path, "r") as zf:
            for name in zf.namelist():
                base = name.rsplit("/", 1)[-1]
                if _is_main_sitemanage_jar(base):
                    return True
    except (OSError, zipfile.BadZipFile):
        return False
    return False


def run_preflight(repo_root: Path, m2_root: Path) -> PreflightReport:
    """Collect artifact rows and decide STALE vs FRESH vs skipped."""
    m2_jar = find_sitemanage_in_m2(m2_root)
    webui_war = find_webui_war(repo_root)
    dist_jar = find_dist_jar(repo_root)
    war_has_sm = war_bundles_sitemanage(webui_war)

    rows = [
        ArtifactRef("m2:sitemanage", m2_jar, _mtime(m2_jar)),
        ArtifactRef("war:webui", webui_war, _mtime(webui_war)),
        ArtifactRef(
            "war:sitemanage",
            webui_war if war_has_sm else None,
            _mtime(webui_war) if war_has_sm else None,
        ),
        ArtifactRef("dist:tree", dist_jar, _mtime(dist_jar)),
    ]

    m2 = rows[0]
    war = rows[1]
    war_sm = rows[2]
    dist = rows[3]

    # No m2 jar → operator has not installed sitemanage; do not block (NOTE).
    if not m2.is_present():
        return PreflightReport(rows=rows, stale=False, reasons=[], skipped=True)

    reasons: List[str] = []
    if not war.is_present():
        reasons.append("WebUI WAR missing (WebUI/target/perc-web-ui-*.war)")
    else:
        if not war_sm.is_present():
            reasons.append(
                "WebUI WAR does not bundle sitemanage-*.jar under WEB-INF/lib"
            )
        elif war.mtime is not None and m2.mtime is not None and war.mtime < m2.mtime:
            reasons.append(
                "WebUI WAR older than m2 sitemanage (repackage WebUI after sitemanage)"
            )

    if not dist.is_present():
        reasons.append(
            "dist jar missing (modules/perc-distribution-tree/target/"
            "perc-distribution-tree.jar)"
        )
    else:
        # Dist must be packaged after the WAR when WAR exists; else after m2.
        if war.is_present() and dist.mtime is not None and war.mtime is not None:
            if dist.mtime < war.mtime:
                reasons.append(
                    "dist jar older than WebUI WAR (repackage perc-distribution-tree)"
                )
        elif dist.mtime is not None and m2.mtime is not None and dist.mtime < m2.mtime:
            reasons.append(
                "dist jar older than m2 sitemanage (rebuild WebUI then dist)"
            )

    return PreflightReport(
        rows=rows, stale=bool(reasons), reasons=reasons, skipped=False
    )


def format_report(report: PreflightReport, *, strict: bool) -> str:
    """Render a multi-line agent-friendly report."""
    by_label = {r.label: r for r in report.rows}

    if report.skipped:
        note = (
            "NOTE: no sitemanage jar in m2 — build sitemanage first; check skipped"
        )
    elif report.stale:
        note = "STALE: rebuild chain required before qa-up — " + "; ".join(
            report.reasons
        )
    else:
        note = "FRESH: m2 sitemanage ≤ WebUI WAR ≤ dist jar"

    lines = [
        f"PREFLIGHT: {note}",
        f"  m2:sitemanage   = {_render(by_label.get('m2:sitemanage'))}",
        f"  war:webui       = {_render(by_label.get('war:webui'))}",
        f"  war:sitemanage  = {_render(by_label.get('war:sitemanage'))}",
        f"  dist:tree       = {_render(by_label.get('dist:tree'))}",
    ]
    if report.stale:
        lines.append(f"  HINT: {REBUILD_HINT}")
        if not strict:
            lines.append(
                "  (non-strict mode: returning OK; qa-up will likely use a stale jar)"
            )
    return "\n".join(lines)


def _render(row: Optional[ArtifactRef]) -> str:
    if row is None or not row.is_present():
        return "<missing>"
    # Portable path display for logs (forward slashes).
    path_s = str(row.path).replace("\\", "/")
    return f"{path_s}  mtime={row.mtime:.0f}"


def _default_m2_root() -> Path:
    """Cross-platform default for the Maven local repository root."""
    home = os.path.expanduser("~")
    return Path(home) / ".m2" / "repository"


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = argparse.ArgumentParser(
        prog="qa-preflight",
        description=(
            "Detect stale WebUI WAR / dist vs m2 sitemanage before qa-up (#2486)."
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
        help="Exit 2 when stale (default).",
    )
    strict_group.add_argument(
        "--no-strict",
        dest="strict",
        action="store_false",
        help="Print STALE: but exit 0 (warn-only).",
    )
    parser.set_defaults(strict=True)
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
    args = parser.parse_args(list(argv) if argv is not None else None)

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )

    try:
        report = run_preflight(Path(args.repo_root), Path(args.m2_root))
    except Exception as exc:  # pragma: no cover - defensive
        LOG.exception("preflight failed: %s", exc)
        print(f"PREFLIGHT: ERROR: {exc}", file=sys.stderr)
        return EXIT_INVOCATION

    text = format_report(report, strict=bool(args.strict))
    print(text)
    if args.log_file is not None:
        log_path = Path(args.log_file)
        log_path.parent.mkdir(parents=True, exist_ok=True)
        with log_path.open("a", encoding="utf-8") as fh:
            fh.write(text + "\n")

    if report.stale and args.strict:
        return EXIT_STALE
    return EXIT_OK


if __name__ == "__main__":
    sys.exit(main())
