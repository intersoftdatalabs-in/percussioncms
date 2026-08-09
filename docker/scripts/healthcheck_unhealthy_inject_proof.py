#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Live/scripted proof that Docker HEALTHCHECK reports unhealthy (#2536 / #2481).

Parent epic #2423 residual of #2481. Unit tests already cover
``rhythmyx_healthcheck`` assessor logic; this harness proves the **end-to-end
contract operators care about**:

1. **Healthy path** — HTTP ready codes + clean Jetty logs → assessor exit 0
   (maps to Docker ``Health.Status=healthy`` once the in-image HEALTHCHECK runs).
2. **Unhealthy inject path** — same HTTP ready, but a known Rhythmyx
   context-failure marker is present under the Jetty log path the HEALTHCHECK
   tails → assessor exit 1 + ``rhythmyx_context_failed`` detail.

Modes
-----
* **mock** (default, agent-safe / CI) — builds a temporary install-root fixture
  under the OS temp dir (or ``--fixture-root``), writes Jetty logs with Path
  joins (Windows host + Linux container portable), and runs
  :func:`rhythmyx_healthcheck.run_healthcheck` with HTTP overrides (no Docker,
  no full CMS install).
* **live** — against a keep cell (``perc-matrix-cms-h2``) or named container:
  append the marker into the container Jetty log via ``docker exec``, then
  re-run the in-container healthcheck and/or ``docker inspect`` Health.Status.
  Requires Docker + a running cell; skip for overnight CI unless the stack is up.

Agent-parseable stdout ends with::

    RESULT:OK STEP:healthcheck-unhealthy-inject-proof MODE:mock
    RESULT:FAIL STEP:healthcheck-unhealthy-inject-proof MODE:mock REASON:...

Exit codes: 0 success, 1 failure / invocation error.

No secrets. Stdlib + sibling ``rhythmyx_healthcheck`` / ``rhythmyx_ready``.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Callable, List, Optional, Sequence, Tuple

# Sibling helpers (stdlib only). Same import pattern as freeport / matrix.
_SCRIPTS_DIR = Path(__file__).resolve().parent
if str(_SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS_DIR))

from rhythmyx_healthcheck import (  # noqa: E402
    EXIT_HEALTHY,
    EXIT_UNHEALTHY,
    collect_log_text,
    discover_jetty_log_paths,
    run_healthcheck,
)
from rhythmyx_ready import (  # noqa: E402
    DETAIL_CONTEXT_FAILED,
    RHYTHMYX_CONTEXT_FAIL_MARKERS,
)

EXIT_OK = 0
EXIT_FAIL = 1

STEP = "healthcheck-unhealthy-inject-proof"

# Marker injected into Jetty logs (must match rhythmyx_ready markers).
DEFAULT_INJECT_MARKER = "Failed startup of context"
assert DEFAULT_INJECT_MARKER in RHYTHMYX_CONTEXT_FAIL_MARKERS

# Relative Jetty log path under install root (POSIX segments via Path — portable).
JETTY_LOG_REL = ("jetty", "base", "logs", "jetty.log")

# Default live container names (qa-up / matrix --keep).
DEFAULT_LIVE_CONTAINERS = (
    "perc-matrix-cms-h2",
    "percussion-cms-dts",
)

DEFAULT_CONTAINER_INSTALL_ROOT = "/opt/Percussion"


def format_result_line(
    ok: bool,
    *,
    mode: str,
    reason: str = "",
    detail: str = "",
) -> str:
    """Build a single RESULT:OK|FAIL line (pure; unit-tested)."""
    status = "OK" if ok else "FAIL"
    parts = [f"RESULT:{status}", f"STEP:{STEP}", f"MODE:{mode}"]
    if detail:
        parts.append(f"DETAIL:{detail}")
    if reason and not ok:
        # Keep REASON free of spaces that break shell scrapers when possible.
        safe = reason.replace("\n", " ").strip()
        parts.append(f"REASON:{safe}")
    return " ".join(parts)


def jetty_log_path(install_root: Path) -> Path:
    """Return portable Path to the default Jetty log under ``install_root``."""
    path = Path(install_root)
    for part in JETTY_LOG_REL:
        path = path / part
    return path


def write_jetty_log(
    install_root: Path,
    body: str,
    *,
    encoding: str = "utf-8",
) -> Path:
    """Create Jetty log directory tree and write ``body``. Returns log path."""
    log_path = jetty_log_path(install_root)
    log_path.parent.mkdir(parents=True, exist_ok=True)
    log_path.write_text(body, encoding=encoding)
    return log_path


def append_jetty_log(install_root: Path, line: str) -> Path:
    """Append a line to the Jetty log (creates file if missing)."""
    log_path = jetty_log_path(install_root)
    log_path.parent.mkdir(parents=True, exist_ok=True)
    with log_path.open("a", encoding="utf-8", newline="\n") as fh:
        if not line.endswith("\n"):
            line = line + "\n"
        fh.write(line)
    return log_path


def clean_log_body() -> str:
    """Minimal clean Jetty log body (no context-failure markers)."""
    return (
        "INFO [Server] Started @1234ms\n"
        "INFO [WebAppContext] Started o.e.j.w.WebAppContext@abc{/Rhythmyx}\n"
    )


def inject_log_body(marker: str = DEFAULT_INJECT_MARKER) -> str:
    """Jetty log body that includes a known context-failure marker."""
    return (
        clean_log_body()
        + f"WARN [WebAppContext] {marker} oeje11w.WebAppContext@deadbeef"
        + "{/Rhythmyx,file:///opt/Percussion/jetty/base/webapps/Rhythmyx/}\n"
        + "Caused by: org.springframework.beans.factory."
        "BeanCurrentlyInCreationException: folderHelper\n"
    )


def assess_fixture(
    install_root: Path,
    *,
    http_code: int = 200,
    product: str = "cms",
) -> Tuple[int, str]:
    """Run healthcheck against a local fixture tree (HTTP overridden)."""
    return run_healthcheck(
        product=product,
        install_root=Path(install_root),
        http_code_override=int(http_code),
    )


def parse_health_status(raw: str) -> str:
    """Normalize ``docker inspect`` Health.Status text (pure).

    Accepts plain status (``healthy``), JSON health object, or full inspect
    blob. Empty / unknown → ``unknown``.
    """
    text = (raw or "").strip()
    if not text:
        return "unknown"
    # Plain single-token status from -f "{{.State.Health.Status}}"
    lower = text.lower()
    if lower in ("healthy", "unhealthy", "starting", "none", "unknown"):
        return lower
    # JSON object or array from docker inspect
    try:
        data = json.loads(text)
    except (json.JSONDecodeError, TypeError, ValueError):
        # Last line may be the status if mixed noise
        last = text.splitlines()[-1].strip().lower()
        if last in ("healthy", "unhealthy", "starting", "none"):
            return last
        return "unknown"
    if isinstance(data, list) and data:
        data = data[0]
    if not isinstance(data, dict):
        return "unknown"
    # Full container inspect: State.Health.Status
    state = data.get("State")
    if isinstance(state, dict):
        health = state.get("Health")
        if isinstance(health, dict):
            status = str(health.get("Status") or "").strip().lower()
            return status or "none"
        return "none"
    # Health object alone
    status = str(data.get("Status") or "").strip().lower()
    return status or "unknown"


def expected_healthy(http_code: int, log_text: str) -> bool:
    """True when mock proof should expect EXIT_HEALTHY (pure)."""
    code, _detail = run_healthcheck(
        product="cms",
        http_code_override=int(http_code),
        log_text_override=log_text,
    )
    return code == EXIT_HEALTHY


def run_mock_proof(
    *,
    fixture_root: Optional[Path] = None,
    marker: str = DEFAULT_INJECT_MARKER,
    http_code: int = 200,
) -> Tuple[bool, List[str]]:
    """Execute healthy + unhealthy-inject fixture proof. No Docker.

    Returns ``(ok, log_lines)``.
    """
    lines: List[str] = []
    own_temp: Optional[Path] = None

    def log(msg: str) -> None:
        lines.append(msg)

    try:
        if fixture_root is None:
            own_temp = Path(tempfile.mkdtemp(prefix="perc-hc-inject-"))
            root = own_temp
        else:
            root = Path(fixture_root)
            root.mkdir(parents=True, exist_ok=True)

        log(f"MODE mock fixture_root={root}")

        # --- 1) Healthy path: clean logs + HTTP ready ---
        clean_path = write_jetty_log(root, clean_log_body())
        discovered = discover_jetty_log_paths(root)
        if clean_path not in discovered:
            return False, lines + [
                f"FAIL discover: expected {clean_path} in {discovered}"
            ]
        log(f"OK wrote clean jetty log path={clean_path}")

        code_ok, detail_ok = assess_fixture(root, http_code=http_code)
        if code_ok != EXIT_HEALTHY:
            return False, lines + [
                f"FAIL healthy path: exit={code_ok} detail={detail_ok!r}"
            ]
        if DETAIL_CONTEXT_FAILED in detail_ok:
            return False, lines + [
                f"FAIL healthy path: unexpected context fail detail={detail_ok!r}"
            ]
        log(f"OK healthy path exit={code_ok} detail={detail_ok}")

        # --- 2) Unhealthy inject: same HTTP ready, marker in Jetty log ---
        inject_path = write_jetty_log(root, inject_log_body(marker))
        log_text = collect_log_text(root)
        if marker not in log_text:
            return False, lines + [
                f"FAIL inject: marker {marker!r} not found in collected log text"
            ]
        log(f"OK injected marker={marker!r} into {inject_path}")

        code_bad, detail_bad = assess_fixture(root, http_code=http_code)
        if code_bad != EXIT_UNHEALTHY:
            return False, lines + [
                f"FAIL unhealthy inject: exit={code_bad} detail={detail_bad!r}"
            ]
        if DETAIL_CONTEXT_FAILED not in detail_bad:
            return False, lines + [
                f"FAIL unhealthy inject: missing {DETAIL_CONTEXT_FAILED} "
                f"in detail={detail_bad!r}"
            ]
        log(
            f"OK unhealthy inject exit={code_bad} detail={detail_bad} "
            f"(maps to Docker Health.Status=unhealthy)"
        )

        # --- 3) Document healthy recovery after marker removal ---
        write_jetty_log(root, clean_log_body())
        code_rec, detail_rec = assess_fixture(root, http_code=http_code)
        if code_rec != EXIT_HEALTHY:
            return False, lines + [
                f"FAIL recovery healthy: exit={code_rec} detail={detail_rec!r}"
            ]
        log(f"OK recovery healthy path exit={code_rec} detail={detail_rec}")

        log("OK healthcheck unhealthy inject proof complete (mock)")
        return True, lines
    finally:
        if own_temp is not None:
            shutil.rmtree(own_temp, ignore_errors=True)


def _docker_run(
    argv: Sequence[str],
    *,
    timeout: float = 60.0,
    run: Optional[Callable[..., subprocess.CompletedProcess]] = None,
) -> subprocess.CompletedProcess:
    """Run a docker argv list (shell=False). Inject ``run`` for tests."""
    runner = run or subprocess.run
    return runner(
        list(argv),
        capture_output=True,
        text=True,
        timeout=timeout,
        check=False,
        shell=False,
    )


def docker_health_status(
    container: str,
    *,
    run: Optional[Callable[..., subprocess.CompletedProcess]] = None,
) -> str:
    """Return normalized Health.Status for ``container`` via docker inspect."""
    cp = _docker_run(
        [
            "docker",
            "inspect",
            "-f",
            "{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}",
            container,
        ],
        run=run,
    )
    if cp.returncode != 0:
        err = (cp.stderr or cp.stdout or "").strip()
        return f"error:{err}" if err else "error"
    return parse_health_status(cp.stdout or "")


def docker_container_running(
    container: str,
    *,
    run: Optional[Callable[..., subprocess.CompletedProcess]] = None,
) -> bool:
    """True when ``docker inspect`` reports running=true."""
    cp = _docker_run(
        ["docker", "inspect", "-f", "{{.State.Running}}", container],
        run=run,
    )
    if cp.returncode != 0:
        return False
    return (cp.stdout or "").strip().lower() in ("true", "1")


def docker_inject_marker(
    container: str,
    *,
    install_root: str = DEFAULT_CONTAINER_INSTALL_ROOT,
    marker: str = DEFAULT_INJECT_MARKER,
    run: Optional[Callable[..., subprocess.CompletedProcess]] = None,
) -> Tuple[bool, str]:
    """Append context-failure marker to Jetty log inside the container.

    Uses ``docker exec`` + ``sh -c`` with a single printf line (no host
    path separators hardcoded for the container FS — container is Linux).
    """
    # Container paths always use '/'; build with posix-style join for the remote.
    remote_log = "/".join(
        [install_root.rstrip("/")] + list(JETTY_LOG_REL)
    )
    remote_dir = remote_log.rsplit("/", 1)[0]
    # Inject a single-line WARN with the marker (shell-safe: no quotes in marker).
    inject_line = f"WARN [WebAppContext] {marker} inject-proof"
    # Use mkdir + printf via sh -c; escape carefully.
    script = (
        f'mkdir -p "{remote_dir}" && '
        f'printf "%s\\n" "{inject_line}" >> "{remote_log}"'
    )
    cp = _docker_run(
        ["docker", "exec", container, "sh", "-c", script],
        run=run,
    )
    if cp.returncode != 0:
        err = (cp.stderr or cp.stdout or "docker exec failed").strip()
        return False, err
    return True, remote_log


def docker_run_incontainer_healthcheck(
    container: str,
    *,
    install_root: str = DEFAULT_CONTAINER_INSTALL_ROOT,
    run: Optional[Callable[..., subprocess.CompletedProcess]] = None,
) -> Tuple[int, str]:
    """Execute the in-image healthcheck CLI inside ``container``.

    Prefers ``/usr/local/lib/perc/rhythmyx_healthcheck.py`` (matrix image bake
    path from #2481). Falls back to ``python3 -c`` assessor only when missing.
    """
    script_candidates = (
        "/usr/local/lib/perc/rhythmyx_healthcheck.py",
        "/usr/local/lib/rhythmyx_healthcheck.py",
    )
    for remote in script_candidates:
        cp = _docker_run(
            [
                "docker",
                "exec",
                "-e",
                f"INSTALL_ROOT={install_root}",
                container,
                "python3",
                remote,
                "--install-root",
                install_root,
            ],
            timeout=120.0,
            run=run,
        )
        # 127 / not found → try next; other codes are health results.
        out = (cp.stdout or "").strip()
        err = (cp.stderr or "").strip()
        detail = out or err or f"exit={cp.returncode}"
        if cp.returncode in (0, 1):
            return int(cp.returncode), detail
        if "No such file" in err or "can't open file" in err.lower():
            continue
        # Usage / other — still return
        return int(cp.returncode), detail
    return EXIT_FAIL, "rhythmyx_healthcheck.py not found in container"


def run_live_proof(
    *,
    container: str,
    install_root: str = DEFAULT_CONTAINER_INSTALL_ROOT,
    marker: str = DEFAULT_INJECT_MARKER,
    require_inspect_unhealthy: bool = False,
    run: Optional[Callable[..., subprocess.CompletedProcess]] = None,
) -> Tuple[bool, List[str]]:
    """Inject marker into a running cell and assert in-container healthcheck fails.

    When ``require_inspect_unhealthy`` is True, also wait/assert Docker
    ``Health.Status=unhealthy`` (may need HEALTHCHECK interval time; default
    off so a single exec of the healthcheck CLI is enough proof).
    """
    lines: List[str] = []

    def log(msg: str) -> None:
        lines.append(msg)

    log(f"MODE live container={container} install_root={install_root}")

    if not docker_container_running(container, run=run):
        return False, lines + [
            f"FAIL container not running: {container} "
            f"(start with perc-devctl qa-up or matrix --keep)"
        ]
    log(f"OK container running={container}")

    status_before = docker_health_status(container, run=run)
    log(f"NOTE Health.Status before inject={status_before}")

    ok_inj, inj_detail = docker_inject_marker(
        container,
        install_root=install_root,
        marker=marker,
        run=run,
    )
    if not ok_inj:
        return False, lines + [f"FAIL docker inject: {inj_detail}"]
    log(f"OK injected marker into container log path={inj_detail}")

    hc_code, hc_detail = docker_run_incontainer_healthcheck(
        container,
        install_root=install_root,
        run=run,
    )
    if hc_code != EXIT_UNHEALTHY:
        return False, lines + [
            f"FAIL live healthcheck: expected exit={EXIT_UNHEALTHY} "
            f"got exit={hc_code} detail={hc_detail!r}"
        ]
    if DETAIL_CONTEXT_FAILED not in hc_detail:
        # Some older images may only print short detail; still require unhealthy exit.
        log(
            f"NOTE live healthcheck unhealthy but detail missing "
            f"{DETAIL_CONTEXT_FAILED}: {hc_detail!r}"
        )
    else:
        log(f"OK live healthcheck exit={hc_code} detail={hc_detail}")

    if require_inspect_unhealthy:
        status_after = docker_health_status(container, run=run)
        log(f"NOTE Health.Status after inject={status_after}")
        if status_after != "unhealthy":
            return False, lines + [
                f"FAIL inspect: expected unhealthy got {status_after!r} "
                f"(HEALTHCHECK interval may not have re-run yet; "
                f"re-run docker inspect later or use in-container CLI proof)"
            ]
        log("OK docker inspect Health.Status=unhealthy")

    log("OK healthcheck unhealthy inject proof complete (live)")
    return True, lines


def resolve_live_container(
    preferred: str = "",
    *,
    candidates: Sequence[str] = DEFAULT_LIVE_CONTAINERS,
    run: Optional[Callable[..., subprocess.CompletedProcess]] = None,
) -> Optional[str]:
    """Pick first running container from preferred + candidates."""
    order: List[str] = []
    if preferred and preferred.strip():
        order.append(preferred.strip())
    for name in candidates:
        if name not in order:
            order.append(name)
    for name in order:
        if docker_container_running(name, run=run):
            return name
    return None


def _build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description=(
            "Prove HEALTHCHECK unhealthy on context-failure inject "
            "(#2536 residual of #2481). Default mock mode needs no Docker."
        )
    )
    p.add_argument(
        "--mode",
        choices=("mock", "live"),
        default=os.environ.get("HC_INJECT_PROOF_MODE", "mock"),
        help="mock=fixture+assessor (default); live=docker inject into keep cell",
    )
    p.add_argument(
        "--fixture-root",
        default="",
        help="Optional install-root directory for mock mode (default: temp dir)",
    )
    p.add_argument(
        "--marker",
        default=DEFAULT_INJECT_MARKER,
        help="Context-failure marker to inject (must be in rhythmyx_ready list)",
    )
    p.add_argument(
        "--http-code",
        type=int,
        default=200,
        help="HTTP override for mock mode (default 200 ready)",
    )
    p.add_argument(
        "--container",
        default=os.environ.get("HC_INJECT_PROOF_CONTAINER", ""),
        help="Live mode container name (default: first running keep cell)",
    )
    p.add_argument(
        "--install-root",
        default=os.environ.get(
            "INSTALL_ROOT",
            os.environ.get("PERC_INSTALL_ROOT", DEFAULT_CONTAINER_INSTALL_ROOT),
        ),
        help="Container install root for live mode",
    )
    p.add_argument(
        "--require-inspect-unhealthy",
        action="store_true",
        help=(
            "Live mode: also require docker inspect Health.Status=unhealthy "
            "(needs HEALTHCHECK re-interval)"
        ),
    )
    p.add_argument(
        "--quiet",
        action="store_true",
        help="Only print the RESULT line",
    )
    return p


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = _build_parser().parse_args(list(argv) if argv is not None else None)
    mode = (args.mode or "mock").strip().lower()
    marker = (args.marker or DEFAULT_INJECT_MARKER).strip()

    if marker not in RHYTHMYX_CONTEXT_FAIL_MARKERS:
        print(
            format_result_line(
                False,
                mode=mode,
                reason=f"marker not in RHYTHMYX_CONTEXT_FAIL_MARKERS: {marker!r}",
            ),
            flush=True,
        )
        return EXIT_FAIL

    if mode == "mock":
        fixture = Path(args.fixture_root) if args.fixture_root else None
        ok, lines = run_mock_proof(
            fixture_root=fixture,
            marker=marker,
            http_code=int(args.http_code),
        )
    elif mode == "live":
        run = subprocess.run  # real docker
        container = (args.container or "").strip()
        if not container:
            resolved = resolve_live_container(run=run)
            if not resolved:
                msg = (
                    "no running keep cell "
                    f"(tried {', '.join(DEFAULT_LIVE_CONTAINERS)}); "
                    "start with perc-devctl qa-up or pass --container"
                )
                if not args.quiet:
                    print(msg, flush=True)
                print(
                    format_result_line(False, mode=mode, reason=msg),
                    flush=True,
                )
                return EXIT_FAIL
            container = resolved
        ok, lines = run_live_proof(
            container=container,
            install_root=str(args.install_root),
            marker=marker,
            require_inspect_unhealthy=bool(args.require_inspect_unhealthy),
            run=run,
        )
    else:
        print(
            format_result_line(False, mode=mode, reason=f"unknown mode {mode}"),
            flush=True,
        )
        return EXIT_FAIL

    if not args.quiet:
        for line in lines:
            print(line, flush=True)

    detail = ""
    if ok:
        detail = "healthy+inject_unhealthy_ok"
    else:
        # Surface last FAIL line in DETAIL when present
        for line in reversed(lines):
            if line.startswith("FAIL"):
                detail = line
                break
    reason = ""
    if not ok and detail:
        reason = detail
    print(format_result_line(ok, mode=mode, reason=reason, detail=detail if ok else ""), flush=True)
    return EXIT_OK if ok else EXIT_FAIL


if __name__ == "__main__":
    sys.exit(main())
