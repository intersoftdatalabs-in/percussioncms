"""Shared helpers for the release-audit pipeline.

Per Constitution Principle II (Evidence Over Invention), every helper below
is grounded in the Python 3.9+ stdlib plus tools already present in this
repository (``gh``, ``git``; ``jq`` is no longer required — Python parses JSON).
No invented APIs.
"""
from __future__ import annotations

import json
import logging
import os
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any

LOGGER = logging.getLogger("release_audit")


def log_info(message: str) -> None:
    LOGGER.info("%s", message)


def log_warn(message: str) -> None:
    LOGGER.warning("%s", message)


def log_error(message: str) -> None:
    LOGGER.error("%s", message)


def die(code: int, message: str) -> None:
    """Log an error and exit with ``code``. Mirrors the bash ``die`` helper."""
    log_error(message)
    sys.exit(code)


def require_origin(repo_root: Path, *, timeout: int = 10) -> None:
    """Exit 4 if ``origin`` is not configured / reachable."""
    result = subprocess.run(
        ["git", "remote", "get-url", "origin"],
        shell=False,
        check=False,
        cwd=str(repo_root),
        timeout=timeout,
        capture_output=True,
    )
    if result.returncode != 0:
        die(4, "origin remote is not configured")
    try:
        rc = subprocess.run(
            ["git", "ls-remote", "origin"],
            shell=False,
            check=False,
            cwd=str(repo_root),
            timeout=timeout,
            capture_output=True,
        ).returncode
    except subprocess.TimeoutExpired:
        die(4, "origin remote is unreachable (timeout or network error)")
    if rc != 0:
        die(4, "origin remote is unreachable")


def require_tag(repo_root: Path, tag: str, *, timeout: int = 10) -> str:
    """Return the commit SHA for ``tag`` on origin. Exit 3 if not resolvable."""
    if not tag:
        die(3, "require_tag: tag argument is empty")
    try:
        result = subprocess.run(
            ["git", "ls-remote", "origin", f"refs/tags/{tag}"],
            shell=False,
            check=False,
            cwd=str(repo_root),
            timeout=timeout,
            capture_output=True,
            text=True,
        )
    except subprocess.TimeoutExpired:
        die(3, f"tag '{tag}' does not resolve on origin (timeout)")
    if result.returncode != 0 or not result.stdout.strip():
        die(3, f"tag '{tag}' does not resolve on origin")
    return result.stdout.strip().split()[0]


def ensure_output_dir(path: Path) -> None:
    """Create ``path`` (including parents) if it does not exist. Exit 2 on failure."""
    try:
        path.mkdir(parents=True, exist_ok=True)
    except OSError as exc:
        die(2, f"cannot create output directory: {path} ({exc})")


def write_atomic(path: Path, content: str) -> None:
    """Atomically write ``content`` to ``path`` (via temp file + os.replace).

    Mirrors the bash ``write_atomic`` helper from the original ``common.sh``.
    """
    ensure_output_dir(path.parent)
    fd, tmp_name = tempfile.mkstemp(
        prefix=f".audit-{path.name}-",
        dir=str(path.parent),
    )
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as tmp:
            tmp.write(content)
            if not content.endswith("\n"):
                tmp.write("\n")
        os.replace(tmp_name, str(path))
    except OSError:
        try:
            os.unlink(tmp_name)
        except OSError:
            pass
        raise


def read_json(path: Path) -> Any:
    """Read and JSON-parse ``path``. Exit 3 on parse error."""
    if not path.is_file():
        die(3, f"read_json: file not found: {path}")
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        die(3, f"read_json: invalid JSON in {path}: {exc}")


def write_json(path: Path, data: Any) -> None:
    """Write ``data`` as JSON to ``path`` (atomic)."""
    write_atomic(path, json.dumps(data, indent=2))


def tag_commit_date(repo_root: Path, tag: str) -> str:
    """Return ``%cs`` (short commit date) for ``tag`` as an ISO date string."""
    result = subprocess.run(
        ["git", "log", "-1", "--format=%cs", tag],
        shell=False,
        check=False,
        cwd=str(repo_root),
        timeout=30,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()
