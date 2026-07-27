#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/fetch-gh-code-scanning-alerts.sh.

Purpose
-------
Fetch code-scanning (CodeQL) alerts for a repository via the ``gh`` CLI and
write a Markdown report to ``docs/ai-generated/tasks/gh-codeql-alerts/alerts.md``.
The format mirrors the original bash output (one bullet block per alert with
rule, severity, tool, state, created date, URL, location, message).

Usage
-----
::

    python3 scripts/fetch-gh-code-scanning-alerts.py [--repo OWNER/REPO]
                                                     [--state open|dismissed|fixed|all]
                                                     [--output <path>]

Behavioral Notes
----------------
- ``gh api`` + ``jq`` is replaced by ``gh api`` + Python's ``json`` module
  (stdlib only, no third-party deps per FR-006).
- The bash version invoked ``scripts/filter-stale-alerts.sh`` at the end. The
  port invokes the Python ``filter_stale_alerts.py`` replacement directly via
  ``subprocess.run([sys.executable, str(...), ...])`` (R2).
- Pagination: ``gh api --paginate`` is used unchanged; the Python wrapper just
  collects the pages into a single list.
- Exit code ``2`` if ``gh`` is missing or not authenticated (per the contract).
"""
from __future__ import annotations

import argparse
import json
import logging
import re
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parent.parent
SCRIPT_DIR = REPO_ROOT / "scripts"

LOGGER = logging.getLogger(__name__)

VALID_STATES = ("open", "dismissed", "fixed", "all")
ALERT_LINE_RE = re.compile(r"^- \*\*Alert #(?P<id>[^*]+)\*\*")


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="fetch-gh-code-scanning-alerts.py",
        description="Fetch GitHub code-scanning alerts and write a Markdown report.",
    )
    parser.add_argument(
        "--repo",
        default="intersoftdatalabs-in/percussioncms",
        help="owner/repo (default: intersoftdatalabs-in/percussioncms)",
    )
    parser.add_argument(
        "--state",
        choices=VALID_STATES,
        default="open",
        help="Alert state filter (default: open)",
    )
    parser.add_argument(
        "--output",
        default="docs/ai-generated/tasks/gh-codeql-alerts/alerts.md",
        help="Output markdown path",
    )
    return parser


def _gh_api(repo: str, state: str) -> list[dict[str, Any]]:
    """Invoke ``gh api`` paginated; return the parsed JSON list."""
    endpoint = (
        f"/repos/{repo}/code-scanning/alerts?per_page=100&state={state}"
    )
    cmd = [
        "gh",
        "api",
        "-H",
        "Accept: application/vnd.github+json",
        endpoint,
        "--paginate",
    ]
    result = subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=180,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise RuntimeError(
            f"gh api failed (rc={result.returncode}): "
            f"{(result.stderr or '').strip()[:500]}"
        )
    payload = result.stdout.strip()
    if not payload:
        return []
    try:
        data = json.loads(payload)
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"gh api returned invalid JSON: {exc}") from exc
    if not isinstance(data, list):
        raise RuntimeError("gh api returned a non-list payload")
    return data


def _format_alert(alert: dict[str, Any]) -> str:
    """Format a single alert as a Markdown bullet block (matches bash output)."""
    number = alert.get("number", "<no-number>")
    rule = (alert.get("rule") or {}).get("id") or "<no-rule>"
    severity = (
        (alert.get("rule") or {}).get("security_severity_level")
        or (alert.get("rule") or {}).get("severity")
        or "<no-sev>"
    )
    tool = (alert.get("tool") or {}).get("name") or "<no-tool>"
    state = alert.get("state") or "<no-state>"
    created = alert.get("created_at") or "<no-date>"
    url = alert.get("html_url") or "<no-url>"
    inst = alert.get("most_recent_instance") or {}
    message = (inst.get("message") or {}).get("text") or "<no-message>"
    path = (inst.get("location") or {}).get("path") or "<no-path>"
    line = (inst.get("location") or {}).get("start_line") or "<no-line>"
    return (
        f"- **Alert #{number}** — `{rule}` ({severity}, CodeQL)\n"
        f"  - **Tool:** {tool}\n"
        f"  - **State:** {state}\n"
        f"  - **Created:** {created}\n"
        f"  - **URL:** {url}\n"
        f"  - **Location:** {path}:{line}\n"
        f"  - **Message:** {message}\n\n"
    )


def write_report(alerts: list[dict[str, Any]], repo: str, state: str, output: Path) -> None:
    """Write the Markdown report to ``output`` (pathlib-only path handling)."""
    output.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        f"# Code Scanning Alerts for {repo}",
        "",
        f"State filter: {state}",
        "Generated: (python port — timestamp not embedded; see file mtime)",
        "",
    ]
    for alert in alerts:
        lines.append(_format_alert(alert))
    output.write_text("\n".join(lines), encoding="utf-8")


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    if shutil.which("gh") is None:
        LOGGER.error("gh CLI not found. Install and authenticate (gh auth login).")
        return 2

    repo = args.repo
    state = args.state
    output = Path(args.output)

    LOGGER.info("Fetching code scanning alerts via gh API for %s (state=%s)", repo, state)
    try:
        alerts = _gh_api(repo, state)
    except RuntimeError as exc:
        LOGGER.error("%s", exc)
        return 2
    write_report(alerts, repo, state, output)
    LOGGER.info("Wrote %d alerts to %s", len(alerts), output)

    # Best-effort stale-cache filter (matches bash original).
    stale_script = SCRIPT_DIR / "filter_stale_alerts.py"
    stale_out = Path("docs/ai-generated/tasks/gh-codeql-alerts/alerts-stale-cache.md")
    if stale_script.is_file():
        try:
            subprocess.run(
                [
                    sys.executable,
                    str(stale_script),
                    str(output),
                    str(stale_out),
                ],
                shell=False,
                check=False,
                timeout=120,
            )
        except (OSError, subprocess.TimeoutExpired) as exc:
            LOGGER.warning("stale-alert filter raised %s (continuing)", exc)
    else:
        LOGGER.info("filter_stale_alerts.py not present; skipping stale-cache filter")

    print(f"Done. Alerts written to: {output}")
    print(f"Stale-cache filter: {stale_out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
