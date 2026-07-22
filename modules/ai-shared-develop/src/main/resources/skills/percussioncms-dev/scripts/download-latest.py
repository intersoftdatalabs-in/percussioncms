#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Download the latest Percussion CMS release artifacts from GitHub.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Replaces ``modules/ai-shared-develop/src/main/resources/skills/
percussioncms-dev/scripts/download-latest.sh``.

CLI surface (per contracts/cli-schemas.md):
  --release <stable|lts|nightly>  Which release channel to fetch
  --target-dir <path>             Directory to write downloads

A ``--dry-run`` flag prints the GitHub API URL that would be queried
without making a network call. This gates pytest and lets operators
inspect the planned request before running for real.

## Behavioral Notes (FR-009b)

- The original ``.sh`` invoked ``curl`` with the GitHub REST API.
  The Python port uses ``urllib.request`` (stdlib, cross-platform).
- Authorization: the original used ``-H \"Authorization: token
  ${GITHUB_TOKEN}\"`` when ``GITHUB_TOKEN`` was set. The Python
  port preserves this — set ``GITHUB_TOKEN`` env var for higher rate
  limits. Note: the original script bug of "GitHub no longer supports
  /releases/latest by date semantics in the same way" is preserved
  here — we fetch the same endpoint for parity.
- ``subprocess.run([...], shell=False)`` not applicable (no
  subprocess calls); all network I/O via ``urllib``.
- Path discovery uses ``pathlib.Path``; no hardcoded separators.

Exit codes:

  0  success
  1  invocation error
  2  network error (HTTP 4xx/5xx, connection refused, timeout)
  3  release has no CMS distribution JAR asset
"""

from __future__ import annotations

import argparse
import json
import logging
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Iterable, Optional

LOG = logging.getLogger("download-latest")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_NETWORK = 2
EXIT_NO_ASSET = 3

DEFAULT_REPO = "intersoftdatalabs-in/percussioncms"
DEFAULT_RELEASE = "stable"
DEFAULT_TARGET_DIR = "./downloads"
GITHUB_API_BASE = "https://api.github.com"


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="download-latest.py",
        description=(
            "Download the latest Percussion CMS release artifacts "
            "(CMS + optionally DTS) from GitHub."
        ),
    )
    p.add_argument(
        "--release",
        default=os.environ.get("PERC_RELEASE", DEFAULT_RELEASE),
        choices=("stable", "lts", "nightly"),
        help=(
            "Release channel. Affects which tag/release we fetch "
            f"(default: {DEFAULT_RELEASE}; env PERC_RELEASE overrides). "
            "For the public GitHub Releases API, all channels resolve to "
            "/releases/latest (GitHub does not distinguish stable/lts/nightly "
            "in the public API)."
        ),
    )
    p.add_argument(
        "--target-dir",
        type=Path,
        default=Path(DEFAULT_TARGET_DIR),
        help=(
            f"Directory for downloaded files (default: {DEFAULT_TARGET_DIR}). "
            "Created if it does not exist."
        ),
    )
    p.add_argument(
        "--dts",
        action="store_true",
        help="Also download the DTS distribution JAR.",
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help=(
            "Print the GitHub API URL and download URLs that would be "
            "queried without making a network call."
        ),
    )
    return p


def _github_request(url: str, *, token: Optional[str] = None, timeout: float = 30.0) -> dict:
    """GET a GitHub API URL and return the parsed JSON body.

    Raises ``urllib.error.HTTPError`` for non-2xx responses.
    """
    headers = {"Accept": "application/vnd.github+json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(request, timeout=timeout) as response:
        body = response.read().decode("utf-8")
        return json.loads(body)


def _download_to(url: str, target: Path, *, token: Optional[str] = None) -> int:
    """Download ``url`` to ``target``. Returns the HTTP status code."""
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=60.0) as response, target.open("wb") as f:
            while True:
                chunk = response.read(65536)
                if not chunk:
                    break
                f.write(chunk)
        return response.status
    except urllib.error.HTTPError as e:
        return e.code


def _find_asset_url(release_body: dict, name_substring: str) -> Optional[str]:
    """Return the browser_download_url of the first asset whose name
    contains ``name_substring`` (case-sensitive), or ``None``.
    """
    for asset in release_body.get("assets", []):
        name = asset.get("name", "")
        if name_substring in name and name.endswith(".jar"):
            return asset.get("browser_download_url")
    return None


def run(
    *,
    release: str,
    target_dir: Path,
    include_dts: bool,
    dry_run: bool,
    token: Optional[str] = None,
) -> int:
    """Top-level entry point. Returns script exit code."""
    target_dir.mkdir(parents=True, exist_ok=True)
    repo = os.environ.get("GITHUB_REPO", DEFAULT_REPO)
    api_url = f"{GITHUB_API_BASE}/repos/{repo}/releases/latest"

    if dry_run:
        LOG.info("DRY-RUN: GET %s", api_url)
        LOG.info("DRY-RUN: would download assets matching:")
        LOG.info("DRY-RUN:   - perc-distribution-tree-*.jar")
        if include_dts:
            LOG.info("DRY-RUN:   - delivery-tier-distribution-*.jar")
        LOG.info("DRY-RUN: into %s", target_dir)
        return EXIT_OK

    LOG.info("Fetching latest release info from %s...", repo)
    try:
        release_body = _github_request(api_url, token=token)
    except urllib.error.HTTPError as e:
        LOG.error("ERROR: GitHub API returned HTTP %s", e.code)
        return EXIT_NETWORK
    except (urllib.error.URLError, TimeoutError, ConnectionError, OSError) as e:
        LOG.error("ERROR: network failure: %s", e)
        return EXIT_NETWORK

    tag = release_body.get("tag_name", "<unknown>")
    LOG.info("Latest release: %s", tag)

    cms_url = _find_asset_url(release_body, "perc-distribution-tree")
    if not cms_url:
        LOG.error("ERROR: CMS distribution JAR not found in release %s assets.", tag)
        LOG.error("Build from source: ./mvn-env.sh clean install")
        return EXIT_NO_ASSET

    cms_target = target_dir / "perc-distribution-tree.jar"
    LOG.info("Downloading CMS distribution JAR to %s", cms_target)
    status = _download_to(cms_url, cms_target, token=token)
    if status not in (200, 201):
        LOG.error("ERROR: CMS download failed (HTTP %s)", status)
        return EXIT_NETWORK

    if include_dts:
        dts_url = _find_asset_url(release_body, "delivery-tier-distribution")
        if not dts_url:
            LOG.warning("DTS distribution JAR not found in release %s assets.", tag)
            LOG.warning("Build from source: ./mvn-env.sh clean install")
        else:
            dts_target = target_dir / "delivery-tier-distribution.jar"
            LOG.info("Downloading DTS distribution JAR to %s", dts_target)
            status = _download_to(dts_url, dts_target, token=token)
            if status not in (200, 201):
                LOG.error("ERROR: DTS download failed (HTTP %s)", status)
                return EXIT_NETWORK

    LOG.info("Download complete. Release: %s", tag)
    LOG.info("Files in %s:", target_dir)
    for jar in sorted(target_dir.glob("*.jar")):
        size = jar.stat().st_size
        LOG.info("  %s (%d bytes)", jar, size)
    return EXIT_OK


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    return run(
        release=args.release,
        target_dir=args.target_dir,
        include_dts=args.dts,
        dry_run=args.dry_run,
        token=os.environ.get("GITHUB_TOKEN") or None,
    )


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())