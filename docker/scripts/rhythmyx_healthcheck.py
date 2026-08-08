#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""In-container Docker HEALTHCHECK for Rhythmyx readiness (#2481 / #2423).

Jetty can bind HTTP while the ROOT/Rhythmyx Spring ``ApplicationContext``
failed. Host-side probes (``qa-health``, matrix ``wait_for_http``) already
scan logs via :mod:`rhythmyx_ready`. This CLI is the **in-image** counterpart
so ``docker inspect … Health.Status`` reports **unhealthy** for orchestrators
that only watch Docker health.

Exit codes (Docker HEALTHCHECK contract)
----------------------------------------
* ``0`` — healthy (login HTTP ready **and** no context-failure markers in
  local Jetty logs)
* ``1`` — unhealthy (HTTP not ready and/or context failed)
* ``2`` — usage / configuration error (also treated as unhealthy)

Environment
-----------
* ``PRODUCT`` — ``cms`` (default) or ``dts``
* ``INSTALL_ROOT`` / ``PERC_INSTALL_ROOT`` — CMS install root (default
  ``/opt/Percussion``)
* ``CMS_PORT`` — CMS listen port inside the container (default ``9992``)
* ``DTS_PORT`` — DTS listen port (default ``9980``)
* ``RHYTHMYX_HEALTH_PATH`` — CMS probe path (default ``/Rhythmyx/login``)
* ``RHYTHMYX_HEALTH_URL`` — full override URL for the HTTP probe
* ``RHYTHMYX_LOG_TAIL_BYTES`` — max bytes read per log file (default 262144)

No secrets. Stdlib only. Pure helpers are unit-tested without Docker.
"""

from __future__ import annotations

import argparse
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Iterable, List, Optional, Sequence, Tuple

# ---------------------------------------------------------------------------
# Import shared markers / assessor (same tree on host tests; image layout
# under /usr/local/lib/perc when baked into matrix/cms images).
# ---------------------------------------------------------------------------


def _ensure_rhythmyx_ready_on_path() -> None:
    candidates = (
        Path(__file__).resolve().parent,
        Path("/usr/local/lib/perc"),
        Path("/usr/local/lib"),
    )
    for candidate in candidates:
        ready = candidate / "rhythmyx_ready.py"
        if ready.is_file():
            path_str = str(candidate)
            if path_str not in sys.path:
                sys.path.insert(0, path_str)
            return


_ensure_rhythmyx_ready_on_path()

from rhythmyx_ready import (  # noqa: E402
    DETAIL_CONTEXT_FAILED,
    assess_rhythmyx_ready,
    is_http_ready_code,
)

DEFAULT_INSTALL_ROOT = "/opt/Percussion"
DEFAULT_CMS_PORT = "9992"
DEFAULT_DTS_PORT = "9980"
DEFAULT_CMS_PATH = "/Rhythmyx/login"
DEFAULT_LOG_TAIL_BYTES = 256 * 1024

EXIT_HEALTHY = 0
EXIT_UNHEALTHY = 1
EXIT_USAGE = 2


def discover_jetty_log_paths(install_root: Path) -> List[Path]:
    """Return existing Jetty/server log files under ``install_root``.

    Paths use ``Path`` join (portable). Missing dirs yield an empty list —
    common before install finishes inside a matrix cell.
    """
    root = Path(install_root)
    dirs = (
        root / "jetty" / "base" / "logs",
        root / "Deployment" / "Server" / "logs",
    )
    found: List[Path] = []
    for log_dir in dirs:
        if not log_dir.is_dir():
            continue
        for path in sorted(log_dir.glob("*.log")):
            if path.is_file():
                found.append(path)
    return found


def read_log_tail(path: Path, max_bytes: int = DEFAULT_LOG_TAIL_BYTES) -> str:
    """Read the last ``max_bytes`` of a log file as text (lossy-safe)."""
    if max_bytes <= 0:
        return ""
    try:
        size = path.stat().st_size
    except OSError:
        return ""
    try:
        with path.open("rb") as fh:
            if size > max_bytes:
                fh.seek(size - max_bytes)
            data = fh.read(max_bytes)
    except OSError:
        return ""
    # Jetty logs are mostly ASCII/UTF-8; replace undecodable bytes.
    return data.decode("utf-8", errors="replace")


def collect_log_text(
    install_root: Path,
    *,
    max_bytes_per_file: int = DEFAULT_LOG_TAIL_BYTES,
    log_paths: Optional[Sequence[Path]] = None,
) -> str:
    """Concatenate tails of Jetty logs for context-failure scanning."""
    paths: Iterable[Path]
    if log_paths is not None:
        paths = log_paths
    else:
        paths = discover_jetty_log_paths(install_root)
    chunks: List[str] = []
    for path in paths:
        text = read_log_tail(Path(path), max_bytes=max_bytes_per_file)
        if text:
            chunks.append(text)
    return "\n".join(chunks)


def http_probe_status(url: str, *, timeout: float = 5.0) -> int:
    """Return HTTP status code, or ``0`` when the endpoint is unreachable."""
    if not url:
        return 0
    try:
        req = urllib.request.Request(url, method="GET")
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return int(resp.getcode())
    except urllib.error.HTTPError as exc:
        # 401/403/etc. still prove the endpoint answered.
        try:
            return int(exc.code)
        except (TypeError, ValueError):
            return 0
    except (urllib.error.URLError, TimeoutError, OSError, ValueError):
        return 0


def build_probe_url(
    *,
    product: str,
    cms_port: str = DEFAULT_CMS_PORT,
    dts_port: str = DEFAULT_DTS_PORT,
    cms_path: str = DEFAULT_CMS_PATH,
    url_override: str = "",
) -> str:
    """Build the in-container probe URL (pure; unit-tested)."""
    if url_override and url_override.strip():
        return url_override.strip()
    product = (product or "cms").strip().lower()
    if product == "dts":
        port = (dts_port or DEFAULT_DTS_PORT).strip() or DEFAULT_DTS_PORT
        return f"http://127.0.0.1:{port}/"
    port = (cms_port or DEFAULT_CMS_PORT).strip() or DEFAULT_CMS_PORT
    path = cms_path if cms_path.startswith("/") else f"/{cms_path}"
    return f"http://127.0.0.1:{port}{path}"


def assess_container_health(
    *,
    product: str,
    http_code: int,
    log_text: str = "",
) -> Tuple[bool, str]:
    """Return ``(healthy, detail)`` for HEALTHCHECK exit mapping.

    * **cms** — :func:`assess_rhythmyx_ready` (HTTP ready codes + context
      markers). Context failure always wins (unhealthy).
    * **dts** — HTTP answered with any positive status (Tomcat up). No
      Rhythmyx context scan (DTS has no ROOT/Rhythmyx Spring app).
    """
    product = (product or "cms").strip().lower()
    if product == "dts":
        if int(http_code) > 0:
            return True, f"ok dts_http={http_code}"
        return False, f"dts_http_not_ready http={http_code}"
    return assess_rhythmyx_ready(http_code, log_text, require_http=True)


def run_healthcheck(
    *,
    product: str = "cms",
    install_root: Path = Path(DEFAULT_INSTALL_ROOT),
    probe_url: str = "",
    cms_port: str = DEFAULT_CMS_PORT,
    dts_port: str = DEFAULT_DTS_PORT,
    cms_path: str = DEFAULT_CMS_PATH,
    log_tail_bytes: int = DEFAULT_LOG_TAIL_BYTES,
    http_timeout: float = 5.0,
    log_text_override: Optional[str] = None,
    http_code_override: Optional[int] = None,
) -> Tuple[int, str]:
    """Execute one health assessment; return ``(exit_code, detail)``.

    Overrides are for unit tests (no network / no real install tree).
    """
    url = probe_url or build_probe_url(
        product=product,
        cms_port=cms_port,
        dts_port=dts_port,
        cms_path=cms_path,
    )
    if http_code_override is not None:
        http_code = int(http_code_override)
    else:
        http_code = http_probe_status(url, timeout=http_timeout)

    if log_text_override is not None:
        log_text = log_text_override
    elif (product or "cms").strip().lower() == "dts":
        log_text = ""
    else:
        log_text = collect_log_text(
            Path(install_root),
            max_bytes_per_file=log_tail_bytes,
        )

    healthy, detail = assess_container_health(
        product=product,
        http_code=http_code,
        log_text=log_text,
    )
    if healthy:
        return EXIT_HEALTHY, detail
    return EXIT_UNHEALTHY, detail


def _env_int(name: str, default: int) -> int:
    raw = os.environ.get(name, "")
    if raw is None or str(raw).strip() == "":
        return default
    try:
        return int(str(raw).strip())
    except ValueError:
        return default


def _build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description=(
            "Docker HEALTHCHECK for Percussion CMS/DTS containers "
            "(#2481). Exit 0=healthy, 1=unhealthy."
        )
    )
    p.add_argument(
        "--product",
        default=os.environ.get("PRODUCT", "cms"),
        help="cms (default) or dts",
    )
    p.add_argument(
        "--install-root",
        default=(
            os.environ.get("INSTALL_ROOT")
            or os.environ.get("PERC_INSTALL_ROOT")
            or DEFAULT_INSTALL_ROOT
        ),
    )
    p.add_argument(
        "--url",
        default=os.environ.get("RHYTHMYX_HEALTH_URL", ""),
        help="Full probe URL override",
    )
    p.add_argument(
        "--cms-port",
        default=os.environ.get("CMS_PORT", DEFAULT_CMS_PORT),
    )
    p.add_argument(
        "--dts-port",
        default=os.environ.get("DTS_PORT", DEFAULT_DTS_PORT),
    )
    p.add_argument(
        "--cms-path",
        default=os.environ.get("RHYTHMYX_HEALTH_PATH", DEFAULT_CMS_PATH),
    )
    p.add_argument(
        "--log-tail-bytes",
        type=int,
        default=_env_int("RHYTHMYX_LOG_TAIL_BYTES", DEFAULT_LOG_TAIL_BYTES),
    )
    p.add_argument(
        "--timeout",
        type=float,
        default=5.0,
        help="HTTP probe timeout seconds",
    )
    return p


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = _build_parser().parse_args(list(argv) if argv is not None else None)
    try:
        exit_code, detail = run_healthcheck(
            product=args.product,
            install_root=Path(args.install_root),
            probe_url=args.url or "",
            cms_port=args.cms_port,
            dts_port=args.dts_port,
            cms_path=args.cms_path,
            log_tail_bytes=int(args.log_tail_bytes),
            http_timeout=float(args.timeout),
        )
    except (OSError, ValueError, TypeError) as exc:
        print(f"healthcheck_error: {exc}", file=sys.stderr, flush=True)
        return EXIT_USAGE

    # Docker captures HEALTHCHECK stdout/stderr in inspect State.Health.Log.
    # Keep one machine-friendly line for operators.
    print(detail, flush=True)
    if DETAIL_CONTEXT_FAILED in detail:
        # Explicit marker for log scrapers / humans reading health log.
        print(f"RESULT:FAIL DETAIL:{DETAIL_CONTEXT_FAILED}", flush=True)
    return int(exit_code)


if __name__ == "__main__":
    sys.exit(main())
