#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Rhythmyx / CMS readiness helpers for docker health probes (#2462 / #2423 / #2556).

Jetty can bind its HTTP connector while the ROOT/Rhythmyx webapp Spring
``ApplicationContext`` failed to start (circular beans, missing deps, etc.).
HTTP-only probes then look green (or hang until timeout) while login/REST
are dead. Product logs under ``jetty/base/logs/server.log`` and install logs
under ``rxconfig/Installer/`` may also show ERROR/FATAL while HTTP answers.

This module provides pure, stdlib-only helpers:

* HTTP status codes that count as "something answered" for login/REST
* Log-text markers that mean the Rhythmyx webapp context failed
* `server.log` / install-log ERROR/FATAL/SEVERE scanners (#2556)
* A small assessor that combines HTTP + log scans for fail-fast
* A documented **probe URL matrix** (#2482) so operators pick an HTTP
  endpoint that *actually implies* the Rhythmyx Spring context is up,
  rather than just proving Jetty answered (the legacy `/Rhythmyx/login`
  probe is weak because the login JSP renders even with a dead Spring
  context — only the form POST fails).

Callers: ``perc-devctl.py`` (``qa-health``, compose ``verify`` /
``_verify_inline``), ``matrix-install-smoke.py`` (``wait_for_http``),
and in-image ``rhythmyx_healthcheck.py`` (Docker HEALTHCHECK / #2481).
No secrets; no network I/O here.

Rules align with ``perc-doctor check-logs`` (#2556).
"""

from __future__ import annotations

import re
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

# RESULT / DETAIL tokens for agent parsers (stable contract).
DETAIL_CONTEXT_FAILED = "rhythmyx_context_failed"
DETAIL_SERVER_LOG_ERRORS = "server_log_errors"

# Container-side install root default (Linux matrix / QA cells).
DEFAULT_INSTALL_ROOT = "/opt/Percussion"
# Primary product log (Log4j RollingFile under jetty base).
DEFAULT_SERVER_LOG_REL = "jetty/base/logs/server.log"
# Full CMS install/startup log set (perc-doctor check-logs / #2556). Paths use /.
CMS_STARTUP_INSTALL_LOG_RELS: Tuple[str, ...] = (
    "jetty/base/logs/server.log",
    "rxconfig/Installer/InstallPackages.log",
    "logs/InstallPackages.log",
    "rxconfig/Installer/install.log",
    "rxconfig/Installer/tablefactory.log",
    "tablefactory.log",
)

# Log4j2 / JUL-style severity tokens on a line (case-sensitive levels so
# prose like "error manager" does not false-positive).
# Matches: " ERROR ", "[ERROR]", "ERROR:", leading "ERROR " / "FATAL " / "SEVERE ".
_SERVER_LOG_SEVERITY_RE = re.compile(
    r"(?:^|[\s\[])(?:ERROR|FATAL|SEVERE)(?:[\s\]:]|$)"
)

# Known non-fatal noise (prefer empty — keep startup clean).
SERVER_LOG_ERROR_ALLOWLIST: Tuple[str, ...] = ()

# Max characters of the matched line returned in DETAIL/MATCH.
_MATCH_LINE_MAX = 240


def container_server_log_path(install_root: str = DEFAULT_INSTALL_ROOT) -> str:
    """POSIX path to ``server.log`` inside a CMS container (always ``/``)."""
    root = (install_root or DEFAULT_INSTALL_ROOT).rstrip("/")
    if not root:
        root = DEFAULT_INSTALL_ROOT
    return root + "/" + DEFAULT_SERVER_LOG_REL


def container_cms_log_paths(install_root: str = DEFAULT_INSTALL_ROOT) -> Tuple[str, ...]:
    """POSIX paths for CMS startup + install logs inside a container."""
    root = (install_root or DEFAULT_INSTALL_ROOT).rstrip("/")
    if not root:
        root = DEFAULT_INSTALL_ROOT
    return tuple(root + "/" + rel for rel in CMS_STARTUP_INSTALL_LOG_RELS)

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


def _normalize_log_lines(log_text: str) -> Sequence[str]:
    if not log_text:
        return ()
    return [
        ln.strip("\r")
        for ln in log_text.replace("\r\n", "\n").split("\n")
        if ln.strip()
    ]


def _is_allowlisted_error_line(line: str) -> bool:
    for needle in SERVER_LOG_ERROR_ALLOWLIST:
        if needle and needle in line:
            return True
    return False


def find_server_log_startup_error(
    log_text: str,
    *,
    also_context_markers: bool = True,
) -> Optional[str]:
    """Return a short description of the first startup error in log text.

    Scans for:

    1. Rhythmyx context-failure markers (optional)
    2. Lines with ERROR / FATAL / SEVERE severity (Log4j / JUL style)

    Empty input is treated as "no evidence of failure yet".
    """
    if not log_text:
        return None

    if also_context_markers:
        ctx = find_rhythmyx_context_failure(log_text)
        if ctx is not None:
            return ctx

    for line in _normalize_log_lines(log_text):
        if _is_allowlisted_error_line(line):
            continue
        if _SERVER_LOG_SEVERITY_RE.search(line):
            if len(line) <= _MATCH_LINE_MAX:
                return line
            return line[: _MATCH_LINE_MAX - 3] + "..."
    return None


def is_http_ready_code(code: int) -> bool:
    """True when ``code`` is an accepted login/REST probe response."""
    return int(code) in HTTP_READY_CODES


def assess_rhythmyx_ready(
    http_code: int,
    log_text: str = "",
    *,
    server_log_text: str = "",
    require_http: bool = True,
) -> Tuple[bool, str]:
    """Combine HTTP probe + log scans into a ready / not-ready verdict.

    Returns ``(ready, detail)`` where ``detail`` is a short machine-friendly
    reason when not ready, or ``ok`` when ready.

    Rules (#2462 / #2556):

    1. Any context-failure marker in ``log_text`` **or** ``server_log_text``
       → **not ready** (fail-fast), even if HTTP returned a ready code.
    2. Any ERROR/FATAL/SEVERE line in ``server_log_text`` → **not ready**
       (``DETAIL_SERVER_LOG_ERRORS``).
    3. If ``require_http`` and HTTP is not a ready code → not ready.
    4. Else ready.

    ``log_text`` is typically recent ``docker logs``. ``server_log_text`` is
    product/install log files from the container (``check-logs`` path set).
    Empty ``server_log_text`` skips the file ERROR gate (files not written yet).
    """
    combined = log_text or ""
    if server_log_text:
        if combined:
            combined = combined + "\n" + server_log_text
        else:
            combined = server_log_text

    match = find_rhythmyx_context_failure(combined)
    if match is not None:
        return False, f"{DETAIL_CONTEXT_FAILED} match={match!r} http={http_code}"

    file_err = find_server_log_startup_error(
        server_log_text or "",
        also_context_markers=False,
    )
    if file_err is not None:
        return (
            False,
            f"{DETAIL_SERVER_LOG_ERRORS} match={file_err!r} http={http_code}",
        )

    if require_http and not is_http_ready_code(http_code):
        return False, f"http_not_ready http={http_code}"
    return True, "ok"
