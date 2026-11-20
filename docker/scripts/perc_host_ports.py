#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Shared host-port / freeport helpers for docker scripts (#2001 / #2005 / #2004).

Cross-platform (Windows / Linux / macOS). Uses stdlib ``socket`` only —
no Unix-only tooling. Callers: ``perc-devctl.py``, ``matrix-install-smoke.py``
(product CMS/DTS publish ports and compose DB host publishes).

Resolution order for :func:`resolve_host_port`:

  1. First non-empty environment variable among the given keys (integer).
  2. ``preferred`` when that port is free on the loopback interface.
  3. :func:`find_free_port` ephemeral allocation.
"""

from __future__ import annotations

import os
import re
import socket
from typing import Optional


def find_free_port(host: str = "127.0.0.1") -> int:
    """Allocate an ephemeral free TCP port via bind(port=0).

    The port is free at return time; a short TOCTOU race remains until
    the consumer binds (docker / compose).
    """
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind((host, 0))
        return int(sock.getsockname()[1])


def is_port_free(port: int, host: str = "127.0.0.1") -> bool:
    """Return True if ``port`` can be bound on ``host`` right now."""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.bind((host, port))
            return True
    except OSError:
        return False


def resolve_host_port(
    *env_keys: str,
    preferred: Optional[int] = None,
) -> int:
    """Resolve a host TCP port for published docker services.

    Env override lets operators pin ports across worktrees or match an
    already-running stack. Leaving env unset prefers the historical
    single-worktree defaults when free, otherwise allocates a free port
    so a second worktree does not fail with ``address already in use``.
    """
    for key in env_keys:
        raw = os.environ.get(key, "").strip()
        if not raw:
            continue
        try:
            return int(raw)
        except ValueError as exc:
            raise ValueError(
                f"env {key}={raw!r} is not an integer host port"
            ) from exc
    if preferred is not None and is_port_free(preferred):
        return preferred
    return find_free_port()


# QA cell name suffix for parallel shards (issue #6704). ``qa-up`` used to
# address a single fixed cell (``perc-matrix-cms-h2``) and unconditionally
# ``docker rm -f`` it before ``docker run``, so two ``max_parallel_work=2``
# shards in the same window deterministically killed each other's in-flight
# cell. Exporting a distinct ``PERC_QA_CELL_SUFFIX`` per shard (or passing
# ``--cell-suffix``) scopes every QA cell name to
# ``perc-matrix-<product>-<db>-<suffix>`` so shards hold isolated cells.
# Unset (the default) keeps the legacy fixed names — single-worktree
# behavior is unchanged.
QA_CELL_SUFFIX_ENV = "PERC_QA_CELL_SUFFIX"
MAX_CELL_SUFFIX_LEN = 24
_CELL_SUFFIX_BAD_CHARS_RE = re.compile(r"[^a-z0-9-]+")


def sanitize_cell_suffix(raw: object) -> str:
    """Normalize a QA cell name suffix to a safe docker-name fragment.

    Lowercases, drops anything outside ``[a-z0-9-]``, strips leading /
    trailing dashes, and truncates to :data:`MAX_CELL_SUFFIX_LEN`.
    Empty / blank input maps to ``""`` (legacy fixed cell names).
    """
    if raw is None:
        return ""
    text = str(raw).strip().lower()
    if not text:
        return ""
    text = _CELL_SUFFIX_BAD_CHARS_RE.sub("", text).strip("-")
    return text[:MAX_CELL_SUFFIX_LEN]


def resolve_cell_suffix(explicit: object = None) -> str:
    """Resolve the effective QA cell suffix: explicit flag wins, else env.

    ``explicit`` is the ``--cell-suffix`` value (``None`` when the flag
    was not passed); the fallback reads :data:`QA_CELL_SUFFIX_ENV`.
    """
    if explicit is not None:
        return sanitize_cell_suffix(explicit)
    return sanitize_cell_suffix(os.environ.get(QA_CELL_SUFFIX_ENV, ""))


def suffixed_cell_container_name(cell_id: str, suffix: object = "") -> str:
    """QA cell container name for ``cell_id`` (e.g. ``cms-h2``).

    Returns the legacy fixed name (``perc-matrix-<cell_id>``) when the
    suffix sanitizes empty, else ``perc-matrix-<cell_id>-<suffix>``.
    """
    clean = sanitize_cell_suffix(suffix)
    base = f"perc-matrix-{cell_id}"
    return f"{base}-{clean}" if clean else base
