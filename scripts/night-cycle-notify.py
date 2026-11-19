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

POST a night-cycle-finished event to $NIGHT_CYCLE_WEBHOOK_URL when a
night-issue-prs cycle ends.

If NIGHT_CYCLE_WEBHOOK_URL is unset or empty, exit 0 silently. Optional
NIGHT_CYCLE_WEBHOOK_AUTH_HEADER is a full "Name: value" header line, applied
verbatim; its value is never printed. Timeouts are ~10s. Always exits 0 —
notify must never fail the cycle.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import socket
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Mapping, Optional, Sequence

DEFAULT_REPO = "intersoftdatalabs-in/percussioncms"
REQUEST_TIMEOUT_SEC = 10
USER_AGENT = "percussioncms-night-cycle-notify/1.0"
URL_ENV = "NIGHT_CYCLE_WEBHOOK_URL"
AUTH_HEADER_ENV = "NIGHT_CYCLE_WEBHOOK_AUTH_HEADER"
REPORT_ENV = "NIGHT_REPORT_PATH"
REPO_ENV = "NIGHT_REPO"

_PR_URL_RE = re.compile(
    r"https://github\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)/pull/(\d+)",
    re.IGNORECASE,
)
_OPENED_LINE_RE = re.compile(r"(?im)^\s*(?:[-*]\s*)?Opened:\s*(.+)$")
_HASH_NUM_RE = re.compile(r"#(\d+)")


def _trim(value: Optional[str]) -> str:
    return (value or "").strip()


def _log(message: str) -> None:
    sys.stderr.write(f"night-cycle-notify: {message}\n")


def utc_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def parse_auth_header(raw: str) -> Optional[tuple[str, str]]:
    """Parse a full 'Name: value' header line. Returns None if malformed."""
    text = raw.strip()
    if not text or ":" not in text:
        return None
    name, value = text.split(":", 1)
    name = name.strip()
    # Keep the value as given, minus one optional leading space after the colon.
    if value.startswith(" "):
        value = value[1:]
    if not name or any(ch in name for ch in ("\r", "\n", ":")):
        return None
    if "\r" in value or "\n" in value:
        return None
    return name, value


def _heading_section(text: str, heading: str) -> str:
    pattern = re.compile(
        rf"(?ims)^##\s+{re.escape(heading)}\s*\n(.*?)(?=^##\s+|\Z)"
    )
    match = pattern.search(text)
    return match.group(1) if match else ""


def extract_pr_urls(report_text: str, repo: str) -> list[str]:
    """Collect PR URLs from a night report, preferring the 'Opened' list."""
    urls: list[str] = []
    seen: set[str] = set()

    def add(url: str) -> None:
        if url and url not in seen:
            seen.add(url)
            urls.append(url)

    def add_number(number: str) -> None:
        add(f"https://github.com/{repo}/pull/{number}")

    for match in _PR_URL_RE.finditer(report_text):
        add(
            "https://github.com/"
            f"{match.group(1)}/{match.group(2)}/pull/{match.group(3)}"
        )

    search_text = _heading_section(report_text, "PRs") or report_text
    opened = _OPENED_LINE_RE.search(search_text)
    if opened:
        for number in _HASH_NUM_RE.findall(opened.group(1)):
            add_number(number)
    return urls


def repo_from_remote_url(url: str) -> str:
    """Parse owner/repo from HTTPS, SSH, or scp-style remotes (incl. SSH aliases)."""
    text = _trim(url).rstrip("/")
    if text.endswith(".git"):
        text = text[:-4]
    # scp-style git@host:owner/repo (host may be an SSH alias)
    if "://" not in text and ":" in text:
        _host, path = text.split(":", 1)
        text = path
    else:
        # https://github.com/owner/repo or ssh://git@github.com/owner/repo
        if "://" in text:
            text = text.split("://", 1)[1]
        parts = [p for p in text.split("/") if p]
        # drop user@host or host
        if len(parts) >= 3:
            text = "/".join(parts[-2:])
        elif len(parts) == 2 and "@" not in parts[0] and "." not in parts[0]:
            text = "/".join(parts)
        else:
            return ""
    parts = [p for p in text.split("/") if p]
    if len(parts) < 2:
        return ""
    return f"{parts[-2]}/{parts[-1]}"


def detect_repo(env: Mapping[str, str], cwd: Optional[Path] = None) -> str:
    explicit = _trim(env.get(REPO_ENV, ""))
    if explicit:
        return explicit.removeprefix("https://github.com/").removesuffix(".git")
    try:
        proc = subprocess.run(
            ["git", "remote", "get-url", "origin"],
            cwd=str(cwd) if cwd else None,
            capture_output=True,
            text=True,
            check=False,
            timeout=5,
        )
    except (OSError, subprocess.TimeoutExpired):
        proc = None
    if proc is not None and proc.returncode == 0:
        parsed = repo_from_remote_url(proc.stdout)
        if parsed:
            return parsed
    return DEFAULT_REPO


def resolve_report_path(raw: str, cwd: Optional[Path] = None) -> Optional[Path]:
    text = _trim(raw)
    if not text:
        return None
    path = Path(text).expanduser()
    if not path.is_absolute():
        base = cwd if cwd is not None else Path.cwd()
        path = base / path
    return path


def build_payload(
    *,
    repo: str,
    report_path: Optional[Path],
    finished_at: str,
    hostname: str,
    report_text: str = "",
) -> dict[str, object]:
    return {
        "event": "night-cycle-finished",
        "repo": repo,
        "report_path": str(report_path) if report_path is not None else "",
        "finished_at": finished_at,
        "hostname": hostname,
        "pr_urls": extract_pr_urls(report_text, repo),
    }


def post_webhook(
    url: str,
    payload: Mapping[str, object],
    auth_header: Optional[tuple[str, str]],
    *,
    timeout: int = REQUEST_TIMEOUT_SEC,
    opener: Optional[object] = None,
) -> int:
    body = json.dumps(payload, separators=(",", ":"), ensure_ascii=True).encode("utf-8")
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "User-Agent": USER_AGENT,
    }
    if auth_header is not None:
        headers[auth_header[0]] = auth_header[1]
    request = urllib.request.Request(url, data=body, headers=headers, method="POST")
    urlopen = opener if opener is not None else urllib.request.urlopen
    try:
        with urlopen(request, timeout=timeout) as response:
            status = int(getattr(response, "status", 0) or 0)
    except urllib.error.HTTPError as exc:
        # HTTPError is also a response; read and close without leaking headers.
        try:
            exc.read()
        except Exception:
            pass
        finally:
            try:
                exc.close()
            except Exception:
                pass
        _log(f"POST failed (HTTP {exc.code}); cycle continues")
        return 0
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        reason = type(exc).__name__
        _log(f"POST failed ({reason}); cycle continues")
        return 0
    _log(f"posted event=night-cycle-finished status={status or 'ok'}")
    return 0


def run(argv: Optional[Sequence[str]] = None, env: Optional[Mapping[str, str]] = None) -> int:
    try:
        return _run(argv, env)
    except Exception as exc:  # noqa: BLE001 — fail-open by contract
        _log(f"failed ({type(exc).__name__}); cycle continues")
        return 0


def _run(argv: Optional[Sequence[str]], env: Optional[Mapping[str, str]]) -> int:
    environ = dict(os.environ if env is None else env)
    parser = argparse.ArgumentParser(
        description="POST a night-cycle-finished webhook (fail-open)."
    )
    parser.add_argument(
        "--report-path",
        default="",
        help="Override NIGHT_REPORT_PATH (the cycle report to parse for PR URLs).",
    )
    parser.add_argument(
        "--repo",
        default="",
        help="Override NIGHT_REPO / detected origin (owner/repo).",
    )
    parser.add_argument(
        "--finished-at",
        default="",
        help="Override finished timestamp (UTC ISO-8601). Default: now.",
    )
    args = parser.parse_args(list(argv) if argv is not None else None)

    url = _trim(environ.get(URL_ENV, ""))
    if not url:
        return 0

    auth_raw = environ.get(AUTH_HEADER_ENV, "")
    auth_header = None
    if _trim(auth_raw):
        auth_header = parse_auth_header(auth_raw)
        if auth_header is None:
            _log(f"{AUTH_HEADER_ENV} is malformed; posting without an auth header")

    cwd = Path.cwd()
    repo = _trim(args.repo) or detect_repo(environ, cwd=cwd)
    report_path = resolve_report_path(args.report_path or environ.get(REPORT_ENV, ""), cwd)
    report_text = ""
    if report_path is not None:
        try:
            report_text = report_path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            _log(f"report unreadable at path; posting without PR URLs")
            report_text = ""

    payload = build_payload(
        repo=repo,
        report_path=report_path,
        finished_at=_trim(args.finished_at) or utc_now(),
        hostname=socket.gethostname(),
        report_text=report_text,
    )
    return post_webhook(url, payload, auth_header)


if __name__ == "__main__":
    sys.exit(run())
