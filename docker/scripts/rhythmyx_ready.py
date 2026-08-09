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
* A documented **probe URL matrix** (#2482) so operators pick an HTTP
  endpoint that *actually implies* the Rhythmyx Spring context is up,
  rather than just proving Jetty answered (the legacy ``/Rhythmyx/login``
  probe is weak because the login JSP renders even with a dead Spring
  context — only the form POST fails).

Callers: ``perc-devctl.py`` (``qa-health``, compose ``verify`` /
``_verify_inline``), ``matrix-install-smoke.py`` (``wait_for_http``),
and in-image ``rhythmyx_healthcheck.py`` (Docker HEALTHCHECK / #2481).
No secrets; no network I/O here.
"""

from __future__ import annotations

from typing import Dict, FrozenSet, Optional, Sequence, Tuple

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

# ---------------------------------------------------------------------------
# Probe URL matrix (#2482). Pure data; no network I/O.
#
# Each entry describes one candidate endpoint on the ROOT/Rhythmyx webapp:
#
# * ``path`` — the path component (URL host/port are caller-supplied).
# * ``implies_spring_context`` — when ``True`` and the path returns a ready
#   code (see :data:`HTTP_READY_CODES`), the Rhythmyx Spring
#   ``ApplicationContext`` is loaded *enough* to register the resource. When
#   ``False`` (e.g. a static JSP or a separate webapp) the endpoint can
#   return ``200`` even with a dead Spring webapp, so it is **not safe**
#   as the sole readiness signal.
# * ``source`` — short note for the matrix doc / agents.
# * ``recommended_role`` — one of ``"primary"``, ``"secondary"``,
#   ``"fallback"``, or ``"avoid"`` (see :data:`PROBE_URL_MATRIX` for the
#   per-environment recommendation).
#
# The matrix is consumed by :func:`assess_probe_url` (path-level
# validation) and by agent / operator docs
# (``docs/ai-generated/tasks/2482-readiness-signal/
# rhythmyx-readiness-probe-matrix.md``).
# ---------------------------------------------------------------------------


class ProbeUrlSpec:
    """Description of one candidate readiness probe URL.

    Plain attribute holder (not a ``dataclass``) — kept stdlib-only so the
    module has no extra runtime dependencies and can be imported from
    in-image healthcheck scripts.
    """

    __slots__ = ("path", "implies_spring_context", "source", "recommended_role")

    def __init__(
        self,
        path: str,
        implies_spring_context: bool,
        source: str,
        recommended_role: str,
    ) -> None:
        self.path = path
        self.implies_spring_context = bool(implies_spring_context)
        self.source = source
        self.recommended_role = recommended_role

    def __repr__(self) -> str:
        return (
            "ProbeUrlSpec(path="
            + repr(self.path)
            + ", implies_spring_context="
            + repr(self.implies_spring_context)
            + ", recommended_role="
            + repr(self.recommended_role)
            + ")"
        )


# Matrix ordered: the strongest primary first, then secondary, then
# fallback, then avoid. ``assess_probe_url`` uses ``recommended_role`` to
# classify, not list position.
PROBE_URL_MATRIX: Dict[str, ProbeUrlSpec] = {
    "/Rhythmyx/rest/mimetypes": ProbeUrlSpec(
        path="/Rhythmyx/rest/mimetypes",
        implies_spring_context=True,
        source=(
            "com.percussion.rest.mimetypes.MimeTypeResource.ping() "
            "— documented 'Ping endpoint for health check'"
        ),
        recommended_role="primary",
    ),
    "/Rhythmyx/rest/folders/by-path/Assets": ProbeUrlSpec(
        path="/Rhythmyx/rest/folders/by-path/Assets",
        implies_spring_context=True,
        source=(
            "com.percussion.rest.folders.FoldersResource.findByPath — "
            "deeper sitemanage bean graph; used as VERIFY_CMS_PATH"
        ),
        recommended_role="secondary",
    ),
    "/Rhythmyx/rest/": ProbeUrlSpec(
        path="/Rhythmyx/rest/",
        implies_spring_context=True,
        source=(
            "com.percussion.rest.Root @Path(\"/\") @OpenAPIDefinition "
            "— equivalent to /mimetypes for probe purposes (entity body)"
        ),
        recommended_role="secondary",
    ),
    "/Rhythmyx/login": ProbeUrlSpec(
        path="/Rhythmyx/login",
        implies_spring_context=False,
        source=(
            "rxlogin.jsp (legacy JSP) — renders 200 even with dead Spring "
            "context; only the form POST fails (#2462 / PR #2479)"
        ),
        recommended_role="fallback",
    ),
    "/Rhythmyx/openapi/openapi.json": ProbeUrlSpec(
        path="/Rhythmyx/openapi/openapi.json",
        implies_spring_context=False,
        source=(
            "modules/perc-openapi-webapp — separate Jetty webapp at "
            "/openapi context, static JSON; not Rhythmyx Spring"
        ),
        recommended_role="avoid",
    ),
    "/Rhythmyx/openapi/index.html": ProbeUrlSpec(
        path="/Rhythmyx/openapi/index.html",
        implies_spring_context=False,
        source=(
            "modules/perc-openapi-webapp — separate webapp, static HTML"
        ),
        recommended_role="avoid",
    ),
}

# Default probe URL primary path (matrix-recommended primary). ``None``
# means "no default; caller must supply a path".
DEFAULT_PROBE_URL_PRIMARY: Optional[str] = "/Rhythmyx/rest/mimetypes"

# Default probe URL secondary path (matrix-recommended secondary — deeper
# readiness check).
DEFAULT_PROBE_URL_SECONDARY: Optional[str] = (
    "/Rhythmyx/rest/folders/by-path/Assets"
)


def assess_probe_url(path: str) -> Tuple[Optional[ProbeUrlSpec], str]:
    """Validate a probe URL path against the matrix.

    Returns a ``(spec, verdict)`` tuple:

    * ``spec`` is the matching :class:`ProbeUrlSpec` when ``path`` is in
      the matrix, else ``None``.
    * ``verdict`` is one of:

      * ``"known_primary"`` / ``"known_secondary"`` / ``"known_fallback"``
        / ``"known_avoid"`` — path is in the matrix with the given role.
      * ``"unknown"`` — path is not in the matrix (callers may still
        accept it; the assessor cannot tell them whether it implies
        Spring context, so they should pair with the log scan).
      * ``"empty"`` — path is empty / blank.

    Pure function; safe to call without any network I/O.
    """
    if path is None or not str(path).strip():
        return None, "empty"
    spec = PROBE_URL_MATRIX.get(path)
    if spec is None:
        return None, "unknown"
    return spec, f"known_{spec.recommended_role}"


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
