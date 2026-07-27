#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/create-large-folder-fixture.sh.

Purpose
-------
Create a single CMS folder with ``FIXTURE_COUNT`` children (default 500) for
the SC-005 perf UAT scenario of feature 992-react-content-explorer. Idempotent:
re-running creates additional children under the same parent.

Usage
-----
::

    python3 scripts/create-large-folder-fixture.py
        [--fixture-path /Sites/PerfFixture]
        [--fixture-count 500]
        [--base-url https://localhost:8443]
        [--user <name>]
        [--password <pwd>]

Security
--------
Uses a temporary netrc file (mode 0600 on POSIX) for curl credentials so the
password is NOT placed on the process command line (Erlang hard gate).
The netrc file is removed on EXIT via a try/finally block (R2).

Behavioral Notes
----------------
- bash ``mktemp`` is replaced by ``tempfile.NamedTemporaryFile`` with
  ``delete=False`` so we can chmod it and write credentials before curl reads
  it; cleanup runs via ``try``/``finally`` (R2).
- ``curl`` is invoked via ``subprocess.run`` with the same flags as the bash
  original (``-sS -k --netrc-file``, ``-X GET``, ``-o /dev/null -w '%{http_code}'``).
- HTTP 2xx and 409 (already-exists) are both acceptable (idempotent re-run).
"""
from __future__ import annotations

import argparse
import logging
import os
import re
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="create_large_folder_fixture.py",
        description="Create a CMS folder fixture (perf UAT).",
    )
    parser.add_argument(
        "--fixture-path",
        default=os.environ.get("FIXTURE_PATH", "/Sites/PerfFixture"),
        help="CMS folder path (default: /Sites/PerfFixture; env: FIXTURE_PATH)",
    )
    parser.add_argument(
        "--fixture-count",
        type=int,
        default=int(os.environ.get("FIXTURE_COUNT", "500")),
        help="Number of children (default: 500; env: FIXTURE_COUNT)",
    )
    parser.add_argument(
        "--base-url",
        default=os.environ.get("CMS_BASE_URL", "https://localhost:8443"),
        help="CMS base URL (default: https://localhost:8443; env: CMS_BASE_URL)",
    )
    parser.add_argument(
        "--user",
        default=os.environ.get("CMS_USER", ""),
        help="CMS user (env: CMS_USER)",
    )
    parser.add_argument(
        "--password",
        default=os.environ.get("CMS_PASS", ""),
        help="CMS password (env: CMS_PASS)",
    )
    return parser


def _http_status(url: str, netrc_file: Path, *, timeout: int = 30) -> str:
    """Run curl and return the HTTP status code as a string."""
    cmd = [
        "curl",
        "-sS",
        "-k",
        "--netrc-file",
        str(netrc_file),
        "-X",
        "GET",
        url,
        "-o",
        os.devnull,
        "-w",
        "%{http_code}",
    ]
    result = subprocess.run(
        cmd,
        shell=False,
        check=False,
        timeout=timeout,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()


def _netrc_host(base_url: str) -> str:
    parsed = urlparse(base_url)
    return (parsed.hostname or "localhost").strip()


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    if not args.user or not args.password:
        print("ERROR: CMS_USER and CMS_PASS are required (or --user/--password).", file=sys.stderr)
        return 2

    host = _netrc_host(args.base_url)
    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    print(
        f"[{timestamp}] Creating fixture folder {args.fixture_path} with "
        f"{args.fixture_count} children at {args.base_url}"
    )

    # Netrc file: written + chmod 0600 + removed in finally.
    fd, netrc_name = tempfile.mkstemp(prefix="create-large-folder-fixture-", suffix=".netrc")
    netrc_path = Path(netrc_name)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as fp:
            fp.write(f"machine {host} login {args.user} password {args.password}\n")
        # POSIX-only chmod 0600; on Windows this is a no-op (NTSEC is per-account).
        try:
            os.chmod(netrc_path, 0o600)
        except (OSError, NotImplementedError):
            pass

        # 1. Create the parent folder. 2xx or 409 (already exists) are both acceptable.
        parent_url = (
            f"{args.base_url}/Rhythmyx/services/pathmanagement/path/"
            f"addNewFolder/{args.fixture_path}?name=PerfFixtureRoot"
        )
        create_code = _http_status(parent_url, netrc_path)
        print(f"createRoot={create_code}")
        if not (create_code.startswith("2") or create_code == "409"):
            print(f"[{timestamp}] FAILED: parent folder creation returned {create_code}", file=sys.stderr)
            return 1

        # 2. Create N children. Track failures; exit non-zero on any.
        failures = 0
        for i in range(1, args.fixture_count + 1):
            child = f"child_{i:04d}"
            url = (
                f"{args.base_url}/Rhythmyx/services/pathmanagement/path/"
                f"addNewFolder/{args.fixture_path}/PerfFixtureRoot?name={child}"
            )
            code = _http_status(url, netrc_path)
            print(f"{child}={code}")
            if not (code.startswith("2") or code == "409"):
                failures += 1

        if failures > 0:
            print(
                f"[{timestamp}] FAILED: {failures}/{args.fixture_count} child creations "
                "did not return 2xx or 409. See above.",
                file=sys.stderr,
            )
            return 1
    finally:
        try:
            netrc_path.unlink(missing_ok=True)
        except OSError:
            pass

    print(f"[{timestamp}] Done.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
