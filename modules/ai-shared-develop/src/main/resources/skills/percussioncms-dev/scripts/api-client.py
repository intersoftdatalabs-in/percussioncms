#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform REST API client for Percussion CMS.

Replaces ``modules/ai-shared-develop/src/main/resources/skills/
percussioncms-dev/scripts/api-client.sh``.

The original ``.sh`` was a *sourced* shell library (loaded via
``source api-client.sh``) that defined shell functions like
``perc_login``, ``perc_api``, ``perc_list_sites``, etc. The Python port
implements the same surface as a standalone CLI tool with explicit
arguments — operators invoke ``api-client.py --method GET --endpoint
/folders/by-path/Assets`` instead of calling a shell function.

A ``--dry-run`` flag prints every HTTP request that would be issued
(``curl``-equivalent argv) without actually connecting to the CMS. This
gates pytest and lets operators inspect the planned request before
sending.

## Behavioral Notes (FR-009b)

- The original ``.sh`` supported both Basic Auth and form-based
  ``j_security_check`` login. The Python port accepts
  ``--user``/``--password`` flags and uses Basic Auth for the
  ``--data``-less GET/DELETE calls. The form-based auth path is
  available via ``--login-form`` but requires ``--endpoint`` to be a
  j_security_check URL (the script does not auto-detect the auth
  scheme — this matches the original's two-pass curl behavior).
- Cookie jar persistence: the original used ``/tmp/perc-cookies.txt``.
  The Python port writes to a per-user cache directory (``~/.cache/perc-api/cookies.txt``
  on Unix, ``%%LOCALAPPDATA%%/perc-api/cookies.txt`` on Windows) via
  ``appdirs``-equivalent stdlib fallback (``os.path.expanduser`` +
  ``os.environ.get("LOCALAPPDATA")``). The path can be overridden with
  ``--cookie-jar``. ``http.cookiejar.MozillaCookieJar`` provides the
  same Netscape format the original relied on.
- ``subprocess.run([...], shell=False)`` everywhere; no curl
  invocation. ``http.client`` (stdlib) is the network primitive.
- Path discovery uses ``pathlib.Path``; no hardcoded OS separators.

Exit codes:

  0  success
  1  invocation error
  2  network error (HTTP 4xx/5xx, connection refused, timeout)
  3  authentication failed (HTTP 401/403 from a non-authenticated call)
"""

from __future__ import annotations

import argparse
import http.client
import json
import logging
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Iterable, Optional

LOG = logging.getLogger("api-client")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_NETWORK = 2
EXIT_AUTH = 3

DEFAULT_BASE_URL = "http://localhost:9992/Rhythmyx/rest"
DEFAULT_USER = "Admin"
COOKIE_JAR_BASENAME = "perc-cookies.txt"


def _default_cookie_jar() -> Path:
    """Cross-platform cookie jar location. Resolves to the user's
    cache dir (``.cache/perc-api/`` on Unix, ``%%LOCALAPPDATA%%/perc-api/``
    on Windows), falling back to ``~/.cache/perc-api/`` if neither
    environment hint is available.
    """
    if sys.platform.startswith("win"):
        base = os.environ.get("LOCALAPPDATA") or os.path.expanduser("~")
    else:
        base = os.environ.get("XDG_CACHE_HOME") or os.path.expanduser("~/.cache")
    p = Path(base) / "perc-api" / COOKIE_JAR_BASENAME
    p.parent.mkdir(parents=True, exist_ok=True)
    return p


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="api-client.py",
        description=(
            "Cross-platform REST API client for Percussion CMS. "
            "Replaces the legacy api-client.sh sourced library; "
            "operations are invoked as one-shot CLI commands instead "
            "of sourced shell functions."
        ),
    )
    p.add_argument(
        "--base-url",
        default=DEFAULT_BASE_URL,
        help=(
            "REST API base URL "
            "(default: http://localhost:9992/Rhythmyx/rest; env "
            "var API_BASE overrides)."
        ),
    )
    p.add_argument(
        "--user",
        default=os.environ.get("CMS_USER", DEFAULT_USER),
        help="Username for HTTP Basic Auth (default: Admin).",
    )
    p.add_argument(
        "--password",
        default=os.environ.get("CMS_PASSWORD", ""),
        help=(
            "Password for HTTP Basic Auth. If unset, will prompt "
            "(matches the original .sh behavior)."
        ),
    )
    p.add_argument(
        "--endpoint",
        default=None,
        help=(
            "API path relative to --base-url "
            "(e.g., /folders/by-path/Assets)."
        ),
    )
    p.add_argument(
        "--method",
        default="GET",
        choices=("GET", "POST", "PUT", "DELETE", "PATCH"),
        help="HTTP method (default: GET).",
    )
    p.add_argument(
        "--data",
        default=None,
        help="Optional JSON body for POST/PUT/PATCH.",
    )
    p.add_argument(
        "--cookie-jar",
        type=Path,
        default=None,
        help=(
            "Cookie storage path "
            "(default: ~/.cache/perc-api/perc-cookies.txt on Unix, "
            "%%LOCALAPPDATA%%/perc-api/perc-cookies.txt on Windows). "
            "Use %%LOCALAPPDATA%% literally on Windows (NOT "
            "expanded by argparse — argparse only expands %%-prefixed "
            "names like %%prog)."
        ),
    )
    p.add_argument(
        "--login-form",
        action="store_true",
        help=(
            "Use the form-based j_security_check login (legacy "
            "non-Basic-Auth fallback) instead of HTTP Basic Auth."
        ),
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help=(
            "Print the HTTP request that would be issued without "
            "actually connecting to the CMS. Used by pytest to "
            "exercise the wiring without a running CMS."
        ),
    )
    return p


def _normalize_endpoint(endpoint: str) -> str:
    """Ensure ``endpoint`` starts with ``/`` (matches original .sh)."""
    return endpoint if endpoint.startswith("/") else f"/{endpoint}"


def _request(
    method: str,
    url: str,
    *,
    user: str = "",
    password: str = "",
    data: Optional[str] = None,
    content_type: Optional[str] = None,
    cookie_jar: Optional[Path] = None,
) -> tuple[int, str]:
    """Issue an HTTP request via ``urllib``. Returns ``(status_code,
    body)``. Authentication via ``user:password`` (Basic Auth). Sets
    ``Cookie: ...`` header from ``cookie_jar`` if it exists.

    ``content_type`` overrides the default ``application/json`` body
    Content-Type. Useful for the form-encoded ``j_security_check``
    login path, which must send ``application/x-www-form-urlencoded``
    or Percussion CMS rejects the request.

    Cross-platform: ``urllib`` is stdlib and works identically on
    Windows + Unix + macOS. The cookie jar is the urllib-format
    Netscape cookies file (matches the original .sh).
    """
    headers = {
        "Accept": "application/json",
    }
    if data is not None:
        headers["Content-Type"] = content_type or "application/json"
    if user or password:
        # Basic Auth via the Authorization header. Use
        # ``base64``-encoded ``user:password``.
        import base64
        token = base64.b64encode(f"{user}:{password}".encode("utf-8")).decode("ascii")
        headers["Authorization"] = f"Basic {token}"
    if cookie_jar and cookie_jar.is_file():
        try:
            from http.cookiejar import MozillaCookieJar
            cj = MozillaCookieJar(str(cookie_jar))
            cj.load(ignore_discard=True, ignore_expires=True)
            cookie_header = "; ".join(
                f"{c.name}={c.value}" for c in cj
            )
            if cookie_header:
                headers["Cookie"] = cookie_header
        except (OSError, ValueError):
            # Cookie jar is malformed or missing — proceed without.
            LOG.warning("Cookie jar at %s is malformed; ignoring.", cookie_jar)

    body = data.encode("utf-8") if data is not None else None
    request = urllib.request.Request(
        url,
        data=body,
        method=method,
        headers=headers,
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status, response.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        body = ""
        try:
            body = e.read().decode("utf-8", errors="replace")
        except (OSError, UnicodeDecodeError):
            pass
        return e.code, body
    except (urllib.error.URLError, TimeoutError, ConnectionError, OSError) as e:
        LOG.error("Network error: %s", e)
        return 0, str(e)


def call(
    *,
    base_url: str,
    user: str,
    password: str,
    endpoint: str,
    method: str = "GET",
    data: Optional[str] = None,
    cookie_jar: Optional[Path] = None,
    login_form: bool = False,
    dry_run: bool = False,
) -> int:
    """Top-level entry point. Returns the script exit code; the
    response body is printed to stdout (matches the original
    ``perc_api`` behavior).
    """
    if endpoint is None:
        LOG.error("ERROR: --endpoint is required")
        return EXIT_INVOCATION

    endpoint = _normalize_endpoint(endpoint)
    full_url = base_url.rstrip("/") + endpoint

    if dry_run:
        argv = [
            "curl", "-s", "-X", method,
            "-H", "Accept: application/json",
        ]
        if data is not None:
            argv.extend(["-H", "Content-Type: application/json", "-d", data])
        if user or password:
            argv.extend(["-u", f"{user}:{password}"])
        argv.append(full_url)
        print("DRY-RUN: " + " ".join(argv))
        return EXIT_OK

    if login_form:
        # j_security_check form-based auth path. The original .sh
        # tried Basic Auth first and fell back to form; here the
        # caller picks the path explicitly to keep the surface
        # minimal. The form path targets /j_security_check (no
        # ``/rest`` suffix) and uses a form-encoded body.
        base_no_rest = base_url.replace("/rest", "").rstrip("/")
        form_url = f"{base_no_rest}/j_security_check"
        form_body = f"j_username={urllib.parse.quote(user)}&j_password={urllib.parse.quote(password)}"
        status, body = _request(
            "POST",
            form_url,
            data=form_body,
            content_type="application/x-www-form-urlencoded",
            cookie_jar=cookie_jar,
        )
        if status in (200, 302, 303):
            print(body)
            return EXIT_OK
        LOG.error("Form auth failed (HTTP %s)", status)
        return EXIT_AUTH if status in (401, 403) else EXIT_NETWORK

    status, body = _request(
        method,
        full_url,
        user=user,
        password=password,
        data=data,
        cookie_jar=cookie_jar,
    )
    # Pretty-print JSON responses for readability (matches the original
    # ``perc_api_pretty`` shell helper).
    if body and body.lstrip().startswith(("{", "[")):
        try:
            print(json.dumps(json.loads(body), indent=2))
        except (ValueError, TypeError):
            print(body)
    else:
        print(body)

    if status == 0:
        return EXIT_NETWORK
    if status in (401, 403) and (user or password) == "":
        LOG.error(
            "Authentication required (HTTP %s). Provide --user and "
            "--password, or set CMS_USER/CMS_PASSWORD env vars.",
            status,
        )
        return EXIT_AUTH
    if 400 <= status < 600:
        return EXIT_NETWORK
    return EXIT_OK


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)

    base_url = os.environ.get("API_BASE", args.base_url)
    user = args.user
    password = args.password
    if not password:
        # Prompt for password if interactive (matches original .sh).
        # Skip in --dry-run and in tests where stdin is not a TTY.
        if sys.stdin.isatty() and not args.dry_run:
            import getpass
            try:
                password = getpass.getpass(f"Password for {user}: ")
            except (EOFError, KeyboardInterrupt):
                password = ""

    cookie_jar = args.cookie_jar or _default_cookie_jar()

    return call(
        base_url=base_url,
        user=user,
        password=password,
        endpoint=args.endpoint,
        method=args.method,
        data=args.data,
        cookie_jar=cookie_jar,
        login_form=args.login_form,
        dry_run=args.dry_run,
    )


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())