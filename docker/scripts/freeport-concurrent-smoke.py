#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Two-cell freeport concurrent allocation smoke (#2006).

CI-friendly / operator dry-run that **does not** start Docker or install CMS.
It proves the shared freeport contract used by ``perc-devctl`` and
``matrix-install-smoke`` (``perc_host_ports.py``):

1. **Preferred baseline** is chosen when free and env is unset.
2. **Second cell** with the preferred port held gets a **distinct** freeport
   (simulates two worktrees without env pin).
3. **Env override wins** over preferred and freeport.
4. After holders release, preferred ports are free again (tear-down analogy).

Agent-parseable stdout ends with::

    RESULT:OK STEP:freeport-concurrent-smoke
    RESULT:FAIL STEP:freeport-concurrent-smoke REASON:...

Exit codes: 0 success, 1 failure / invocation error.

Full live two-worktree stacks (real ``qa-up`` / ``up`` + probes) remain an
operator checklist in ``docker/README.md`` — this script only checks
allocation wiring so overnight agents can green-check freeport without a
multi-GB install.
"""

from __future__ import annotations

import argparse
import os
import socket
import sys
from pathlib import Path
from typing import List, Optional, Sequence, Tuple

# Sibling freeport helpers (stdlib only).
_SCRIPTS_DIR = Path(__file__).resolve().parent
if str(_SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS_DIR))
from perc_host_ports import find_free_port, is_port_free, resolve_host_port  # noqa: E402

EXIT_OK = 0
EXIT_FAIL = 1

# Preferred baselines match perc-devctl / matrix (#2001 / #2003 / #2005).
PREFERRED_QA_CMS = 9993
PREFERRED_CMS = 9992
PREFERRED_DTS = 9980

STEP = "freeport-concurrent-smoke"

# Env keys this smoke may set or clear — never leave process polluted.
_SMOKE_ENV_KEYS = (
    "QA_CMS_HOST_PORT",
    "CMS_HOST_PORT",
    "CMS_PORT",
    "DTS_PORT",
    "DTS_HOST_PORT",
)


def _clear_port_env() -> None:
    for key in _SMOKE_ENV_KEYS:
        os.environ.pop(key, None)


def _hold_port(port: int, host: str = "127.0.0.1") -> socket.socket:
    """Bind and hold ``port`` so freeport consumers treat it as taken."""
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # Reuse helps on Windows when a previous run left TIME_WAIT; fail loud if busy.
    try:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    except OSError:
        pass
    sock.bind((host, port))
    sock.listen(1)
    return sock


def run_smoke() -> Tuple[bool, List[str]]:
    """Execute allocation checks. Returns ``(ok, log_lines)``."""
    lines: List[str] = []
    holders: List[socket.socket] = []

    def log(msg: str) -> None:
        lines.append(msg)

    try:
        _clear_port_env()

        # --- 1) Preferred when free (cell A baseline) ---
        # Ensure preferred QA is free for this assertion when possible.
        preferred_qa = PREFERRED_QA_CMS
        if not is_port_free(preferred_qa):
            # Cannot assert preferred-when-free for 9993; use ephemeral as preferred.
            preferred_qa = find_free_port()
            log(f"NOTE preferred {PREFERRED_QA_CMS} busy; using ephemeral preferred={preferred_qa}")
        port_a = resolve_host_port(preferred=preferred_qa)
        if port_a != preferred_qa:
            return False, lines + [
                f"FAIL cell-A preferred: expected {preferred_qa}, got {port_a}"
            ]
        log(f"OK cell-A preferred QA_CMS_HOST_PORT={port_a}")

        # Hold cell-A's port (simulates published docker mapping still live).
        holders.append(_hold_port(port_a))
        log(f"OK cell-A holding port {port_a}")

        # --- 2) Second cell without env pin → distinct freeport ---
        port_b = resolve_host_port(preferred=preferred_qa)
        if port_b == port_a:
            return False, lines + [
                f"FAIL cell-B freeport: got same port as cell-A ({port_a})"
            ]
        if not is_port_free(port_b):
            # resolve released the ephemeral bind; should still be free for docker.
            # If not free, something else raced — soft note only if bind fails later.
            pass
        log(f"OK cell-B freeport QA_CMS_HOST_PORT={port_b} (distinct from {port_a})")

        # Hold B too and resolve a third to show multi-cell uniqueness continues.
        holders.append(_hold_port(port_b))
        port_c = resolve_host_port(preferred=preferred_qa)
        if port_c in (port_a, port_b):
            return False, lines + [
                f"FAIL cell-C freeport: collided with A/B ({port_a}/{port_b}) got {port_c}"
            ]
        log(f"OK cell-C freeport QA_CMS_HOST_PORT={port_c}")

        # --- 3) Env override wins (even when preferred free) ---
        # Free a high ephemeral and pin it via env while preferred is free after
        # we only hold A/B (preferred may still be held if it was port_a).
        override = find_free_port()
        os.environ["QA_CMS_HOST_PORT"] = str(override)
        resolved_override = resolve_host_port(
            "QA_CMS_HOST_PORT",
            "CMS_HOST_PORT",
            preferred=preferred_qa,
        )
        if resolved_override != override:
            return False, lines + [
                f"FAIL env override: expected {override}, got {resolved_override}"
            ]
        log(f"OK env override QA_CMS_HOST_PORT={override} wins over preferred")
        del os.environ["QA_CMS_HOST_PORT"]

        # Compose CMS/DTS pair (dev stack) — both freeport when preferred held.
        preferred_cms = PREFERRED_CMS
        preferred_dts = PREFERRED_DTS
        if not is_port_free(preferred_cms):
            preferred_cms = find_free_port()
        if not is_port_free(preferred_dts):
            preferred_dts = find_free_port()
        cms_a = resolve_host_port("CMS_PORT", preferred=preferred_cms)
        dts_a = resolve_host_port("DTS_PORT", preferred=preferred_dts)
        holders.append(_hold_port(cms_a))
        holders.append(_hold_port(dts_a))
        cms_b = resolve_host_port("CMS_PORT", preferred=preferred_cms)
        dts_b = resolve_host_port("DTS_PORT", preferred=preferred_dts)
        if cms_b == cms_a or dts_b == dts_a:
            return False, lines + [
                f"FAIL compose second cell: CMS {cms_a}->{cms_b} DTS {dts_a}->{dts_b}"
            ]
        if cms_b == dts_b:
            return False, lines + [
                f"FAIL compose second cell: CMS and DTS collided on {cms_b}"
            ]
        log(
            f"OK compose freeport cell-A CMS_PORT={cms_a} DTS_PORT={dts_a}; "
            f"cell-B CMS_PORT={cms_b} DTS_PORT={dts_b}"
        )

        os.environ["CMS_PORT"] = "19111"
        os.environ["DTS_PORT"] = "19112"
        if resolve_host_port("CMS_PORT", preferred=preferred_cms) != 19111:
            return False, lines + ["FAIL CMS_PORT env override"]
        if resolve_host_port("DTS_PORT", preferred=preferred_dts) != 19112:
            return False, lines + ["FAIL DTS_PORT env override"]
        log("OK compose env override CMS_PORT=19111 DTS_PORT=19112")
        del os.environ["CMS_PORT"]
        del os.environ["DTS_PORT"]

        # --- 4) Tear-down: release holders; preferred free again when it was ours ---
        for sock in holders:
            sock.close()
        holders.clear()
        # Only assert preferred free if nothing else on the machine holds it.
        if preferred_qa == PREFERRED_QA_CMS or is_port_free(preferred_qa):
            if not is_port_free(preferred_qa):
                return False, lines + [
                    f"FAIL after tear-down: preferred {preferred_qa} still not free"
                ]
            log(f"OK after tear-down preferred {preferred_qa} free")
        else:
            log(f"NOTE after tear-down preferred {preferred_qa} still busy (external holder)")

        log("OK freeport concurrent smoke complete")
        return True, lines
    except OSError as exc:
        return False, lines + [f"FAIL OSError: {exc}"]
    finally:
        for sock in holders:
            try:
                sock.close()
            except OSError:
                pass
        _clear_port_env()


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = argparse.ArgumentParser(
        prog="freeport-concurrent-smoke.py",
        description=(
            "Dry-run two-cell freeport allocation smoke (no docker / CMS install). "
            "See docker/README.md → Two-worktree concurrent freeport smoke."
        ),
    )
    parser.add_argument(
        "--quiet",
        action="store_true",
        help="Only print the RESULT line.",
    )
    args = parser.parse_args(list(argv) if argv is not None else None)

    ok, lines = run_smoke()
    if not args.quiet:
        for line in lines:
            print(line)
    if ok:
        print(f"RESULT:OK STEP:{STEP}")
        return EXIT_OK
    reason = "see log above"
    for line in reversed(lines):
        if line.startswith("FAIL"):
            reason = line
            break
    print(f"RESULT:FAIL STEP:{STEP} REASON:{reason}")
    return EXIT_FAIL


if __name__ == "__main__":
    sys.exit(main())
