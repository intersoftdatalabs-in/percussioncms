#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Shared ``docker cp`` with tar-pipe fallback (#7194; fallback #7157).

Consolidates the three near-identical ``_docker_cp_with_tar_fallback``
copies that #7186 added to ``hot-deploy-jar.py``,
``hot-deploy-rhythmyx-war-jars.py`` and ``hot-deploy-webui-modern.py``
into one module. Callers keep a thin ``_docker_cp_with_tar_fallback``
wrapper that injects their own ``_run`` (``docker cp`` step) and logger,
so ``--dry-run`` semantics and exit codes stay caller-owned.

Cross-platform (Windows / Linux / macOS). Stdlib only: the fallback
archive is built with in-memory stdlib ``tarfile`` — no host ``tar``
binary is needed.

The archive member name is derived from the basename of
``container_dest`` (not the host file name) so the fallback writes
exactly the path ``docker cp`` would. A caller that renames on deploy
(e.g. versioned host jar ``foo-1.2.jar`` deployed as container
``.../foo.jar``) previously had the fallback quietly write a different
file next to the destination, leaving the old file in place.
"""

from __future__ import annotations

import io
import logging
import subprocess
import tarfile
from pathlib import Path
from typing import Callable, List

LOG = logging.getLogger("docker-cp-tar-fallback")

EXIT_OK = 0
EXIT_DOCKER_FAILED = 5


def tar_member_for_container_dest(container_dest: str) -> str:
    """Return the tar member name for ``container_dest`` (#7194).

    The fallback extracts with ``tar -xf - -C <parent>`` where
    ``<parent>`` is everything up to the last ``/``, so the member must
    be the basename of the destination — the same file ``docker cp``
    would write. Container paths are always POSIX (the cell runs
    Linux), hence ``rpartition("/")`` regardless of host OS.
    """
    return container_dest.rpartition("/")[2]


def tar_parent_for_container_dest(container_dest: str) -> str:
    """Return the ``-C`` parent directory for ``container_dest``."""
    return container_dest.rpartition("/")[0] or "/"


def is_safe_tar_member(member: str) -> bool:
    """Return True if ``member`` is a safe bare file name."""
    return bool(member) and member not in (".", "..") and "/" not in member and "\\" not in member


def build_single_file_tar_bytes(*, member: str, payload: bytes, mtime: int) -> bytes:
    """Build a one-member tar archive holding ``payload`` as ``member``.

    Preserves the source mtime in the archive member: ``TarInfo``
    defaults to epoch 0, and Jetty serves stable entry URLs with
    Last-Modified conditional requests — a 1970 mtime can make browsers
    keep a stale bundle after a changed deploy.
    """
    buf = io.BytesIO()
    with tarfile.open(fileobj=buf, mode="w") as tf:
        info = tarfile.TarInfo(name=member)
        info.size = len(payload)
        info.mtime = mtime
        tf.addfile(info, io.BytesIO(payload))
    return buf.getvalue()


def docker_cp_with_tar_fallback(
    host_path: Path,
    container_name: str,
    container_dest: str,
    *,
    dry_run: bool,
    cp_runner: Callable[[List[str]], int],
    logger: logging.Logger | None = None,
) -> int:
    """Copy ``host_path`` to ``container_name:container_dest`` (#7157).

    ``docker cp`` fails deterministically on some QA cells whose
    writable-layer / graphdriver state is corrupt (daemon:
    ``mkdirat installer/installer.jar: file exists``) even though
    ``docker exec`` into the same cell still works. When ``docker cp``
    fails, retry the same single file through a ``tar`` pipe
    (``docker exec -i <cell> tar -xf - -C <parent>``) fed from an
    in-memory stdlib-``tarfile`` archive.

    ``cp_runner`` performs the ``docker cp`` step (callers pass their
    own ``_run`` so ``--dry-run`` logging stays caller-owned) and must
    return ``EXIT_OK`` on success. Under ``--dry-run`` only the
    ``docker cp`` plan runs; the fallback never triggers because the
    caller's dry-run runner always succeeds.
    """
    log = logger if logger is not None else LOG
    host_str = str(host_path.resolve())
    rc = cp_runner(["docker", "cp", host_str, f"{container_name}:{container_dest}"])
    if rc == EXIT_OK:
        return EXIT_OK
    if dry_run:
        return rc
    member = tar_member_for_container_dest(container_dest)
    if not is_safe_tar_member(member):
        log.error("refusing tar fallback for unsafe container file name: %r", member)
        return EXIT_DOCKER_FAILED
    log.warning(
        "docker cp %s -> %s:%s failed; retrying via tar pipe (#7157).",
        host_str,
        container_name,
        container_dest,
    )
    try:
        payload = host_path.read_bytes()
        # Preserve the source mtime in the archive member: TarInfo
        # defaults to epoch 0, and Jetty serves stable WebUI entry URLs
        # with Last-Modified conditional requests — a 1970 mtime can make
        # browsers keep a stale bundle after a changed deploy.
        mtime = int(host_path.stat().st_mtime)
    except OSError as exc:
        log.error("cannot read %s for tar fallback: %s", host_path, exc)
        return EXIT_DOCKER_FAILED
    try:
        archive = build_single_file_tar_bytes(member=member, payload=payload, mtime=mtime)
    except (tarfile.TarError, OSError) as exc:
        log.error("cannot build tar fallback archive for %s: %s", host_path, exc)
        return EXIT_DOCKER_FAILED
    parent = tar_parent_for_container_dest(container_dest)
    completed = subprocess.run(
        ["docker", "exec", "-i", container_name, "tar", "-xf", "-", "-C", parent],
        input=archive,
        capture_output=True,
        shell=False,
        check=False,
    )
    if completed.returncode != 0:
        stderr = (completed.stderr or b"").decode("utf-8", "replace").strip()
        log.error(
            "docker cp AND tar-pipe fallback both failed for %s -> %s:%s "
            "(tar extract stderr: %s). The cell writable layer is likely "
            "corrupt (daemon 'mkdirat ... file exists', #7157): recreate it "
            "with 'python docker/scripts/perc-devctl.py qa-down' then "
            "'python docker/scripts/perc-devctl.py qa-up'.",
            host_str,
            container_name,
            container_dest,
            stderr or "<empty>",
        )
        return EXIT_DOCKER_FAILED
    log.info(
        "tar-pipe fallback copy succeeded: %s -> %s:%s (#7157).",
        host_str,
        container_name,
        container_dest,
    )
    return EXIT_OK
