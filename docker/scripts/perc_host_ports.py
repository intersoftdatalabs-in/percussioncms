#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Shared host-port / freeport helpers for docker scripts (#2001 / #2005).

Cross-platform (Windows / Linux / macOS). Uses stdlib ``socket`` only —
no Unix-only tooling. Callers: ``perc-devctl.py``, ``matrix-install-smoke.py``.

Resolution order for :func:`resolve_host_port`:

  1. First non-empty environment variable among the given keys (integer).
  2. ``preferred`` when that port is free on the loopback interface.
  3. :func:`find_free_port` ephemeral allocation.
"""

from __future__ import annotations

import os
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
