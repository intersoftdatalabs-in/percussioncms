#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cross-platform TinyMCE asset sync helper.

Replaces ``modules/perc-distribution-tree/UpdateTinyMCE.bat``.

Syncs the TinyMCE source directory under
``modules/perc-tinymce/src/main/tinymce`` into the packaged-resources
directory ``modules/perc-tinymce/src/main/resources/tinymce`` so the
:class:``perc-tinymce`` Maven module picks them up on the next build.

Exit codes:

  0  sync complete
  1  invocation error / source missing
  2  copy error

## Behavioral Notes (FR-009b)

- The original ``UpdateTinyMCE.bat`` invoked Maven (``mvn clean install
  -DskipTests=true -pl :perc-tinymce``), then copied the resulting
  ``perc-tinymce-<ver>.jar`` into the assembled distribution's
  ``webapps/Rhythmyx/WEB-INF/lib``, and finally restarted Jetty. The
  Python port implements only the *asset-sync* leg of that dance: the
  build-and-deploy leg is now performed by
  ``api-update.py --module jars`` (which already covers the perc-tinymce
  module), and the Jetty restart is operator-controlled. Operators wanting
  the all-in-one workflow chain ``api-update.py --module jars --no-restart
  && update-tinymce.py && api-update.py ...``.
- The asset sync uses ``shutil.copytree(..., dirs_exist_ok=True)`` so
  re-runs overwrite stale files without deleting newly-added entries the
  operator placed manually (same observable effect as the original
  ``xcopy /D /E /Y``-style semantics the .bat was approximating via the
  jar-copy path).
- Path discovery uses ``Path(__file__).resolve().parents[N]`` for the
  repo root and module root (R7); no hardcoded separators.
"""

from __future__ import annotations

import argparse
import logging
import shutil
import sys
from pathlib import Path
from typing import Iterable, Optional

LOG = logging.getLogger("update-tinymce")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_COPY_FAILED = 2


def _default_paths(script_path: Path) -> tuple[Path, Path]:
    """Return ``(source, target)`` defaults resolved from the script
    location. ``scripts/`` lives at
    ``modules/perc-distribution-tree/scripts/``; the perc-tinymce module
    is a sibling at ``modules/perc-tinymce/``.
    """
    repo_root = script_path.resolve().parents[3]
    source = repo_root / "modules" / "perc-tinymce" / "src" / "main" / "tinymce"
    target = repo_root / "modules" / "perc-tinymce" / "src" / "main" / "resources" / "tinymce"
    return source, target


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="update-tinymce.py",
        description=(
            "Sync TinyMCE assets from the source directory into the "
            "packaged-resources directory under modules/perc-tinymce/."
        ),
    )
    p.add_argument(
        "--source",
        type=Path,
        default=None,
        help=(
            "TinyMCE source directory "
            "(default: modules/perc-tinymce/src/main/tinymce resolved from "
            "the script location)."
        ),
    )
    p.add_argument(
        "--target",
        type=Path,
        default=None,
        help=(
            "Target directory for synced TinyMCE assets "
            "(default: modules/perc-tinymce/src/main/resources/tinymce "
            "resolved from the script location)."
        ),
    )
    return p


def sync(source: Path, target: Path) -> int:
    """Sync ``source`` into ``target``. Returns the exit code. Does not call
    ``sys.exit`` — callers (CLI main and pytest tests) decide what to do.
    """
    if not source.is_dir():
        LOG.error("ERROR: source directory not found: %s", source)
        return EXIT_INVOCATION
    try:
        target.mkdir(parents=True, exist_ok=True)
        # ``dirs_exist_ok=True`` preserves any operator-placed files in
        # ``target`` that are not present in ``source``. The original
        # ``copy /Y`` semantics in UpdateTinyMCE.bat only overwrote the
        # single jar; the Python port syncs the full tree, which is more
        # faithful to "update TinyMCE assets" than the .bat was.
        shutil.copytree(str(source), str(target), dirs_exist_ok=True)
    except (OSError, shutil.Error) as exc:
        LOG.error("ERROR: copytree failed: %s (%s)", target, exc)
        return EXIT_COPY_FAILED
    LOG.info("OK: synced %s -> %s", source, target)
    return EXIT_OK


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    default_source, default_target = _default_paths(Path(__file__))
    source = args.source.resolve() if args.source else default_source
    target = args.target.resolve() if args.target else default_target
    return sync(source, target)


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())