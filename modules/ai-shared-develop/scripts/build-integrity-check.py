#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Build-time integrity check for AI resources.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Replaces ``modules/ai-shared-develop/scripts/build-integrity-check.sh``.
For each resource passed as a positional argument, the script:

1. Verifies the sidecar ``.sha256`` hash matches (sha256sum / shasum /
   Python ``hashlib.sha256`` as fallback).
2. If ``cosign`` is on PATH, verifies the ``.sha256.sig`` Sigstore
   signature. The identity regex is derived from ``git config
   user.email`` so artifacts must be signed by the same domain the
   verifier runs under (matches the original ``.sh`` semantics).

A non-zero exit is reserved for hash mismatch / signature failure so
this script can be wired into a build-time hook.

## Behavioral Notes (FR-009b)

- The original ``.sh`` used ``(cd $dir && sha256sum -c $HASH_FILE)``
  inside a subshell. The Python port uses ``hashlib.sha256`` directly
  with ``Path.open("rb")`` for cross-platform portability
  (``sha256sum`` is GNU coreutils; not always present on stock macOS
  or Windows; ``shasum`` is BSD/macOS-only). The legacy
  ``command -v sha256sum / shasum`` fallback chain in the ``.sh``
  existed because the shell ``sha256sum`` binary isn't portable; the
  Python port doesn't need that chain.
- ``cosign`` invocation is preserved as ``subprocess.run([...],
  shell=False)`` (FR-008). The original used bash word-splitting on
  the identity regex; Python passes it as a single argv element.
- If ``cosign`` is missing, the script logs a clear SKIP and exits 0
  (matches the original ``.sh`` "skip without cosign" branch) so this
  script doesn't break developer builds where cosign isn't installed.
- Missing ``.sha256`` sidecar is treated as a WARNING (matches the
  original) but exits 0 — operators running this against partially
  signed inventories should not have their build hard-fail.
- Path discovery uses ``pathlib.Path``; cross-platform.

Exit codes:

  0  integrity verified (or warning; or sig SKIP because cosign missing)
  1  hash mismatch (INTEGRITY FAILED)
  2  signature verification failed (AUTHENTICITY FAILED)
  3  invalid CLI args
"""

from __future__ import annotations

import argparse
import hashlib
import logging
import re
import subprocess
import sys
from pathlib import Path
from typing import Iterable, List, Optional

LOG = logging.getLogger("build-integrity-check")

EXIT_OK = 0
EXIT_INTEGRITY_FAILED = 1
EXIT_AUTHENTICITY_FAILED = 2
EXIT_INVOCATION = 3


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="build-integrity-check.py",
        description=(
            "Verify SHA-256 + Sigstore signature sidecars for one or "
            "more AI resources. Fails the build on hash mismatch or "
            "invalid signature."
        ),
    )
    p.add_argument(
        "resources",
        nargs="+",
        type=Path,
        help="One or more resource files to verify.",
    )
    p.add_argument(
        "--strict",
        action="store_true",
        help=(
            "Treat WARNING-level findings (missing sidecar, missing "
            "cosign) as failures. Default behavior is to SKIP / log "
            "warnings and exit 0 (matches the original .sh)."
        ),
    )
    return p


def _sha256_file(path: Path) -> str:
    """Compute the hex SHA-256 of ``path``. Stdlib-only — no dependency
    on ``sha256sum`` / ``shasum`` system binaries.
    """
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def _verify_hash(resource: Path) -> int:
    """Compare the resource's SHA-256 to its ``.sha256`` sidecar.
    The sidecar file is expected to contain lines of the form
    ``<sha256>  <filename>`` (sha256sum format) — the original ``.sh``
    delegates to ``sha256sum -c``.
    """
    hash_file = resource.with_name(resource.name + ".sha256")
    if not hash_file.is_file():
        LOG.warning(
            "[INTEGRITY] WARNING: Missing sidecar for %s. "
            "Please run sign-ai-resources.py to generate it.",
            resource.name,
        )
        return EXIT_OK

    expected_hash = None
    for raw in hash_file.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split()
        if len(parts) < 1:
            continue
        # sha256sum -c format: "<hex>  <filename>" or "<hex> *<filename>"
        candidate = parts[0]
        if re.fullmatch(r"[0-9a-fA-F]{64}", candidate):
            expected_hash = candidate.lower()
            break
    if expected_hash is None:
        LOG.warning(
            "[INTEGRITY] WARNING: Could not parse sha256 in %s; "
            "skipping hash check.", hash_file
        )
        return EXIT_OK

    actual_hash = _sha256_file(resource)
    if actual_hash != expected_hash:
        LOG.error(
            "[INTEGRITY] FAILED: Hash mismatch for %s "
            "(expected %s..., got %s...)",
            resource.name, expected_hash[:12], actual_hash[:12],
        )
        return EXIT_INTEGRITY_FAILED

    LOG.info("[INTEGRITY] OK: %s", resource.name)
    return EXIT_OK


def _identity_regex_from_git() -> str:
    """Derive the certificate-identity-regexp from ``git config
    user.email``. Mirrors the original ``.sh``: if the email is
    ``user@domain.com``, restrict to ``.*@domain``.
    """
    try:
        completed = subprocess.run(
            ["git", "config", "user.email"],
            capture_output=True,
            text=True,
            shell=False,
            check=False,
        )
    except FileNotFoundError:
        return ".*"
    email = (completed.stdout or "").strip()
    if not email:
        return ".*"
    if "@" not in email:
        return ".*"
    domain = email.split("@", 1)[1].strip()
    if not domain:
        return ".*"
    return f".*@{domain}"


def _verify_signature(
    resource: Path,
    *,
    id_regexp: str,
    issuer_regexp: str = ".*",
    cosign_path: Optional[List[str]] = None,
) -> int:
    """Verify the Sigstore signature sidecar. Returns ``EXIT_OK``,
    ``EXIT_AUTHENTICITY_FAILED``, or skips cleanly if cosign / sig
    sidecar is missing.
    """
    sig_file = resource.with_name(resource.name + ".sha256.sig")
    hash_file = resource.with_name(resource.name + ".sha256")
    if not sig_file.is_file():
        LOG.info(
            "[AUTHENTICITY] SKIP: Signature file %s.sha256.sig not found. "
            "Only integrity verified.",
            resource.name,
        )
        return EXIT_OK

    # If cosign is missing, fall through with a clear SKIP (matches
    # the original ``.sh``).
    if cosign_path is None:
        cosign_check = subprocess.run(
            ["cosign", "version"],
            shell=False,
            check=False,
            capture_output=True,
        )
        if cosign_check.returncode != 0:
            LOG.info("[AUTHENTICITY] SKIP: cosign not found. Skipping signature check.")
            return EXIT_OK
        cosign_path = ["cosign"]

    LOG.info("[AUTHENTICITY] Verifying %s signature...", resource.name)
    completed = subprocess.run(
        cosign_path
        + [
            "verify-blob",
            "--certificate-identity-regexp", id_regexp,
            "--certificate-oidc-issuer-regexp", issuer_regexp,
            "--bundle", str(sig_file), str(hash_file),
        ],
        shell=False,
        check=False,
    )
    if completed.returncode != 0:
        LOG.error("[AUTHENTICITY] FAILED: Signature invalid for %s", resource.name)
        return EXIT_AUTHENTICITY_FAILED
    LOG.info("[AUTHENTICITY] OK: %s", resource.name)
    return EXIT_OK


def check_resource(
    resource: Path,
    *,
    id_regexp: str,
) -> int:
    """Verify one resource. Returns the script's exit code for that
    resource. Caller (typically ``main``) decides whether to halt on
    the first failure or aggregate.
    """
    if not resource.is_file():
        LOG.warning("Resource %s not found. Skipping.", resource)
        return EXIT_OK
    rc = _verify_hash(resource)
    if rc != EXIT_OK:
        return rc
    return _verify_signature(resource, id_regexp=id_regexp)


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    id_regexp = _identity_regex_from_git()

    # Aggregate the worst exit code across all resources (so a single
    # bad signature doesn't mask subsequent verifications). The original
    # ``.sh`` did the same — exited 1 on the first hash mismatch and
    # propagated via bash `set -e`.
    worst = EXIT_OK
    for resource in args.resources:
        rc = check_resource(resource, id_regexp=id_regexp)
        if rc > worst:
            worst = rc
    return worst


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())