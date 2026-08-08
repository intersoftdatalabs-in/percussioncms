#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Rhythmyx / CMS readiness helpers for docker health probes (#2462 / #2423).

Jetty can bind its HTTP connector while the ROOT/Rhythmyx webapp Spring
``ApplicationContext`` failed to start (circular beans, missing deps, etc.).
HTTP-only probes then look green (or hang until timeout) while login/REST
are dead.

This module provides pure, stdlib-only helpers:

* HTTP status codes that count as "something answered" for login/REST
* Log-text markers that mean the Rhythmyx webapp context failed
* A small assessor that combines HTTP + log scan for fail-fast

Callers: ``perc-devctl.py`` (``qa-health``), ``matrix-install-smoke.py``
(``wait_for_http``). No secrets; no network I/O here.
"""

from __future__ import annotations

from typing import FrozenSet, Optional, Sequence, Tuple

# Login / REST probes treat these as "endpoint is up" (auth may still apply).
HTTP_READY_CODES: FrozenSet[int] = frozenset({200, 302, 401, 403})

# Substrings from Jetty / Spring logs when ROOT/Rhythmyx context fails.
# Keep markers specific enough to avoid installer noise (e.g. file names).
RHYTHMYX_CONTEXT_FAIL_MARKERS: Tuple[str, ...] = (
    "Failed startup of context",
    "BeanCurrentlyInCreationException",
    "Requested bean is currently in creation",
    "Is there an unresolvable circular reference",
)

# RESULT / DETAIL token for agent parsers (stable contract).
DETAIL_CONTEXT_FAILED = "rhythmyx_context_failed"


def find_rhythmyx_context_failure(
    log_text: str,
    *,
    markers: Sequence[str] = RHYTHMYX_CONTEXT_FAIL_MARKERS,
) -> Optional[str]:
    """Return the first context-failure marker found in ``log_text``, or None.

    Matching is plain substring (case-sensitive, same as Jetty/Spring log
    phrasing). Empty / None input yields None.
    """
    if not log_text:
        return None
    for marker in markers:
        if marker and marker in log_text:
            return marker
    return None


def is_http_ready_code(code: int) -> bool:
    """True when ``code`` is an accepted login/REST probe response."""
    return int(code) in HTTP_READY_CODES


def assess_rhythmyx_ready(
    http_code: int,
    log_text: str = "",
    *,
    require_http: bool = True,
) -> Tuple[bool, str]:
    """Combine HTTP probe + log scan into a ready / not-ready verdict.

    Returns ``(ready, detail)`` where ``detail`` is a short machine-friendly
    reason when not ready, or ``ok`` when ready.

    Rules (issue #2462):

    1. Any context-failure marker in logs → **not ready** (fail-fast), even
       if HTTP returned a ready code (dead Spring webapp behind Jetty).
    2. If ``require_http`` and HTTP is not a ready code → not ready.
    3. Else ready.
    """
    match = find_rhythmyx_context_failure(log_text)
    if match is not None:
        return False, f"{DETAIL_CONTEXT_FAILED} match={match!r} http={http_code}"
    if require_http and not is_http_ready_code(http_code):
        return False, f"http_not_ready http={http_code}"
    return True, "ok"
