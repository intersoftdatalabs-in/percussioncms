#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform Python port of scripts/authenticate-sigstore.sh.

Purpose
-------
Authenticate with Sigstore OIDC once per session by running the Maven
``exec:java`` invocation that drives ``com.percussion.ai.signing.OidcAuthenticator``,
capturing the identity token from stdout, and writing it to ``~/.sigstore-token``
plus exporting ``SIGSTORE_IDENTITY_TOKEN`` for the current process.

Usage
-----
::

    python3 scripts/authenticate-sigstore.py [--identity <token>] [--cache-path <path>]

Notes
-----
- The original bash script used ``[[ -f ~/.sigstore-token ]] && [[ -z "$SIGSTORE_IDENTITY_TOKEN" ]]``
  to decide whether to refresh. The Python port preserves that exact semantic:
  if a cached token already exists at ``--cache-path`` AND ``SIGSTORE_IDENTITY_TOKEN``
  is unset in the environment, the script exits 0 immediately without invoking
  Maven (idempotent re-run).
- The Maven invocation is preserved (``mvn-env.sh -pl modules/ai-shared-develop -q
  exec:java -Dexec.mainClass=...``); on Windows the bash wrapper is not invoked;
  the Python port invokes ``mvn`` directly (the same rationale used by
  ``verify-distribution-archive.sh``).
- ``mktemp`` is replaced by ``tempfile.NamedTemporaryFile`` (auto-cleanup via
  ``finally``). The bash ``xargs`` trim is replaced by ``str.strip()``.

Behavioral Notes
----------------
- On Windows interactive console, signal handlers (SIGINT/SIGTERM) are no-ops;
  cleanup of the temp file still runs via ``finally`` (R2).
- No third-party imports; stdlib only (FR-006).
"""
from __future__ import annotations

import argparse
import logging
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

LOGGER = logging.getLogger(__name__)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="authenticate-sigstore.py",
        description="Authenticate with Sigstore OIDC and cache the identity token.",
    )
    parser.add_argument(
        "--identity",
        default=os.environ.get("SIGSTORE_IDENTITY_TOKEN", ""),
        help="OIDC identity token (overrides cached token; default: env SIGSTORE_IDENTITY_TOKEN)",
    )
    parser.add_argument(
        "--cache-path",
        default=str(Path.home() / ".sigstore-token"),
        help="Cache file path (default: ~/.sigstore-token)",
    )
    return parser


def _resolve_mvn() -> list[str]:
    """Return the argv prefix that invokes Maven on the current OS.

    Linux/macOS prefer the repo ``mvn-env.sh`` wrapper (matches the bash original);
    Windows falls back to ``mvn`` directly. ``shutil.which`` guards against PATH gaps.
    """
    if sys.platform.startswith("win"):
        mvn = shutil.which("mvn")
        if mvn:
            return [mvn]
        # Fall back to the conventional name even if not on PATH (let subprocess raise).
        return ["mvn.cmd"]
    wrapper = REPO_ROOT / "mvn-env.sh"
    if wrapper.is_file():
        return [str(wrapper)]
    mvn = shutil.which("mvn")
    if mvn:
        return [mvn]
    return ["mvn"]


def _retrieve_token(tmp_path: Path) -> str | None:
    """Run Maven exec:java to drive ``OidcAuthenticator`` and capture the token.

    Returns the trimmed stdout of the Maven process, or ``None`` if Maven failed
    or produced empty output.
    """
    mvn_argv = _resolve_mvn()
    cmd = mvn_argv + [
        "-pl",
        "modules/ai-shared-develop",
        "-q",
        "exec:java",
        "-Dexec.mainClass=com.percussion.ai.signing.OidcAuthenticator",
    ]
    try:
        result = subprocess.run(
            cmd,
            shell=False,
            check=False,
            timeout=300,
            capture_output=True,
            text=True,
        )
    except FileNotFoundError:
        LOGGER.error("Maven executable not found on PATH: %s", mvn_argv)
        return None
    except subprocess.TimeoutExpired:
        LOGGER.error("Maven exec:java timed out after 300s")
        return None
    if result.returncode != 0:
        LOGGER.error(
            "Maven exec:java failed (rc=%d). stderr (first 500 chars): %s",
            result.returncode,
            (result.stderr or "")[:500],
        )
        return None
    return result.stdout.strip()


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    cache_path = Path(args.cache_path).expanduser()

    # Cache hit: only short-circuit when no explicit --identity AND no env override
    # AND the cache file exists. Mirrors the bash original's `[[ -f ~/.sigstore-token ]] && [[ -z "${SIGSTORE_IDENTITY_TOKEN}" ]]`.
    env_token = os.environ.get("SIGSTORE_IDENTITY_TOKEN", "")
    if not args.identity and not env_token and cache_path.is_file():
        LOGGER.info("Cached Sigstore token present at %s; reusing (no refresh).", cache_path)
        return 0

    if args.identity:
        token = args.identity.strip()
        LOGGER.info("Using --identity token (skipping Maven invocation).")
    else:
        with tempfile.NamedTemporaryFile(
            mode="w",
            prefix="auth-sigstore-",
            suffix=".txt",
            delete=False,
            encoding="utf-8",
        ) as tmp:
            tmp_path = Path(tmp.name)
        try:
            token = _retrieve_token(tmp_path) or ""
        finally:
            tmp_path.unlink(missing_ok=True)

    if not token:
        LOGGER.error("Failed to retrieve Sigstore identity token.")
        return 1

    cache_path.parent.mkdir(parents=True, exist_ok=True)
    cache_path.write_text(token + "\n", encoding="utf-8")
    os.environ["SIGSTORE_IDENTITY_TOKEN"] = token
    LOGGER.info("Sigstore token cached at %s and exported to SIGSTORE_IDENTITY_TOKEN.", cache_path)
    return 0


if __name__ == "__main__":
    sys.exit(main())
