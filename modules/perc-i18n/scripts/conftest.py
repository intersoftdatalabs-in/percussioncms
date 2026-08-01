"""Pytest bootstrap for modules/perc-i18n/scripts.

Ensures sibling modules (``i18n_translate``, ``i18n_cache``,
``resolve_tmx_conflicts``, …) are importable when pytest is invoked from
the repo root via ``scripts/run-python-tests.{sh,cmd}``.
"""
from __future__ import annotations

import sys
from pathlib import Path

_SCRIPTS_DIR = Path(__file__).resolve().parent
_scripts = str(_SCRIPTS_DIR)
if _scripts not in sys.path:
    sys.path.insert(0, _scripts)
