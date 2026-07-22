"""release-audit: v8.1.x → 8.2 migration audit pipeline.

Cross-platform Python port of ``scripts/release-audit/`` (spec 005-migrate-8.1.7-changes).
Modules are imported as flat names (``import common``) rather than relative
imports (``from . import common``) because the package directory name
``release-audit`` contains a dash, which Python's identifier rules disallow —
so users invoke the pipeline as ``python3 scripts/release-audit/__main__.py``
rather than ``python3 -m release_audit``.

Modules:
- ``common``: shared helpers (logging, atomic writer, output dir)
- ``inventory``: PR collection + dependabot classification + enrichment
- ``verdicts``: per-PR verdict classification (path-resolution + heuristics)
- ``backlog``: migration backlog Markdown generator
- ``report``: summary Markdown report generator
- ``port``: per-item porting workflow helpers (cherry-pick, JDK 8 scan)
"""

__all__ = [
    "common",
    "inventory",
    "verdicts",
    "backlog",
    "report",
    "port",
]
