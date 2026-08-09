#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""QA preflight: detect a stale WebUI WAR in the dist tree vs the freshly
built sitemanage SNAPSHOT (#2486).

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

If the WAR-side jar is older than (or missing) the m2-side jar, the
preflight prints a clear, single-line ``STALE:`` summary so the agent
or human running ``perc-devctl qa-up`` can fix the chain before the
container starts. Exit code is ``0`` when fresh, ``2`` when stale
(strict). A non-strict mode (``--no-strict``) only prints the
``STALE:`` line and returns ``0`` so callers can log without blocking.

The check is filesystem-only — no docker, no curl, no maven. Operators
run ``python3 docker/scripts/qa_preflight.py`` (or
``perc-devctl.py qa-preflight``) before ``qa-up``.
"""

from __future__ import annotations

import argparse
import logging
import os
import re
import sys
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

LOG = logging.getLogger("qa_preflight")


@dataclass(frozen=True)
class PreflightResult:
    """One row in the preflight report."""

    label: str  # "m2:sitemanage" / "war:sitemanage" / "war:webui"
    path: Optional[Path]
    mtime: Optional[float]  # seconds since epoch; None when missing

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


def war_bundles_sitemanage(war_path: Path) -> bool:
    """Return ``True`` when the WAR contains a ``sitemanage-*.jar`` entry.

    We use the entry-name check rather than extracting the JAR so the
    preflight stays filesystem-only and never touches a multi-GB
    extract cache. A bundled ``sitemanage-*.jar`` is the explicit
    signal that the dist tree will unpack a stale sitemanage into the
    Rhythmyx webapp; the freshness comparison then keys off the WAR
    file mtime (when the WAR was built) vs the m2 jar mtime (when
    sitemanage was last installed). If the m2 jar was updated after the
    WAR was built, the WAR's bundled copy is by definition stale.
    """
    if war_path is None or not war_path.is_file():
        return False
    import zipfile

    with zipfile.ZipFile(war_path, "r") as zf:
        for name in zf.namelist():
            base = name.rsplit("/", 1)[-1]
            if base.startswith(_M2_ARTIFACT + "-") and base.endswith(".jar"):
                return True
    return False


def _mtime(path: Optional[Path]) -> Optional[float]:
    if path is None or not path.exists():
        return None
    return path.stat().st_mtime


def run_preflight(repo_root: Path, m2_root: Path) -> list[PreflightResult]:
    """Compute the preflight rows; never raises.

    Missing artifacts are reported as ``is_present() == False`` rather
    than as exceptions so the caller can render a single summary line
    and decide on exit code based on the strict / non-strict policy.

    The "war:sitemanage" row reports the WAR file mtime, not an
    extracted jar mtime. The preflight keys off the WAR's build time
    vs the m2 jar's install time — see {@link is_stale} for the
    rationale.
    """
    m2_jar = find_sitemanage_in_m2(m2_root)
    webui_war = find_webui_war(repo_root)
    war_bundles = war_bundles_sitemanage(webui_war) if webui_war else False

    return [
        PreflightResult("m2:sitemanage", m2_jar, _mtime(m2_jar)),
        PreflightResult("war:webui", webui_war, _mtime(webui_war)),
        # ``war:sitemanage`` is a derived row: present iff the WAR
        # bundles a sitemanage jar; its mtime mirrors the WAR file so
        # the comparison below is "WAR built before m2 was updated".
        PreflightResult(
            "war:sitemanage",
            webui_war if war_bundles else None,
            _mtime(webui_war) if war_bundles else None,
        ),
    ]


def is_stale(rows: Iterable[PreflightResult]) -> bool:
    """Return ``True`` when the WAR was built before the m2 sitemanage jar
    was last installed, or when the WAR / m2 jar is missing entirely.

    Missing m2-side jar is **not** considered stale (the developer may
    not have built sitemanage yet); the preflight prints a ``NOTE:``
    line so the operator knows the check was a no-op.
    """
    by_label = {r.label: r for r in rows}
    m2 = by_label.get("m2:sitemanage")
    war = by_label.get("war:sitemanage")
    war_war = by_label.get("war:webui")
    if m2 is None or not m2.is_present():
        return False
    if war_war is None or not war_war.is_present():
        return True  # no WAR at all → qa-up will fail anyway
    if war is None or not war.is_present():
        return True  # WAR present but does not bundle sitemanage → cannot preflight
    return war.mtime < m2.mtime  # type: ignore[operator]


def format_report(rows: Iterable[PreflightResult], strict: bool) -> str:
    """Render the preflight rows as a single-line summary plus detail."""
    by_label = {r.label: r for r in rows}
    stale = is_stale(rows)
    m2 = by_label.get("m2:sitemanage")
    war = by_label.get("war:sitemanage")
    war_war = by_label.get("war:webui")

    if m2 is None or not m2.is_present():
        note = "NOTE: no sitemanage jar in m2 — build sitemanage first; check skipped"
    elif stale:
        note = (
            "STALE: war:sitemanage is older than m2:sitemanage — "
            "repackage perc-distribution-tree (or run mvn install on the WebUI module)"
        )
    else:
        note = "FRESH: war:sitemanage >= m2:sitemanage"

    parts = [
        f"PREFLIGHT: {note}",
        f"  m2:sitemanage   = {_render(by_label.get('m2:sitemanage'))}",
        f"  war:webui       = {_render(by_label.get('war:webui'))}",
        f"  war:sitemanage  = {_render(by_label.get('war:sitemanage'))}",
    ]
    if not strict and stale:
        parts.append("  (non-strict mode: returning OK; qa-up will likely use a stale jar)")
    return "\n".join(parts)


def _render(row: Optional[PreflightResult]) -> str:
    if row is None or not row.is_present():
        return "<missing>"
    return f"{row.path}  mtime={row.mtime:.0f}"


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
            "SNAPSHOT before running perc-devctl qa-up (#2486)."
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
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Exit non-zero when stale (default: exit 0, print STALE: line).",
    )
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

    rows = run_preflight(args.repo_root, args.m2_root)
    stale = is_stale(rows)
    report = format_report(rows, strict=args.strict)
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
