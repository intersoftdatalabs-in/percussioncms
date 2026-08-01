#!/usr/bin/env python3
"""Shared persistent translation cache for perc-i18n developer tooling.

Both ``i18n_translate.py`` (Docker) and ``i18n_translate_direct.py``
(``trans`` / Docker fallback) read and write the same checked-in cache:

    modules/perc-i18n/scripts/cache/i18n_translate.json

Keys are ``sha256(target || NUL || source_text)``; values are translated
segment text (pre-XML-escape). The file is committed so locale back-fills
resume across machines and sessions. Only one person typically runs
locales; on merge, **accept both** (union of keys) via
``resolve_i18n_cache_conflicts.py`` — same operating rule as TMX.

Legacy local paths under ``scripts/.cache/`` are still read once and
merged into the canonical file so existing machine caches are not lost.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
CACHE_DIR = SCRIPTS_DIR / 'cache'
CACHE_FILE = CACHE_DIR / 'i18n_translate.json'

# Pre-persistence locations (gitignored). Loaded and folded into CACHE_FILE
# when the canonical file is missing or when migrating after a pull.
LEGACY_CACHE_FILES = (
    SCRIPTS_DIR / '.cache' / 'i18n_translate.json',
    SCRIPTS_DIR / '.cache' / 'i18n_translate_direct.json',
)

START_RE = re.compile(r'^<<<<<<< .+$', re.MULTILINE)
SEP_RE = re.compile(r'^=======$', re.MULTILINE)
END_RE = re.compile(r'^>>>>>>> .+$', re.MULTILINE)


def _read_json_object(path: Path) -> dict[str, str]:
    try:
        data = json.loads(path.read_text(encoding='utf-8'))
    except (OSError, json.JSONDecodeError):
        return {}
    if not isinstance(data, dict):
        return {}
    # Coerce values to str; skip non-string keys.
    out: dict[str, str] = {}
    for k, v in data.items():
        if isinstance(k, str) and isinstance(v, str):
            out[k] = v
    return out


def union_caches(
    base: dict[str, str],
    overlay: dict[str, str],
    *,
    stats: dict[str, int] | None = None,
) -> dict[str, str]:
    """Union two cache maps. ``overlay`` wins on key collision (ours).

    Matches TMX resolve semantics: keep both sides' unique keys; on the
    same key with different text, keep the working-tree / ``ours`` side.
    """
    if stats is None:
        stats = {}
    result = dict(base)
    for key, value in overlay.items():
        if key in result:
            if result[key] != value:
                stats['key_collisions'] = stats.get('key_collisions', 0) + 1
            else:
                stats['key_same'] = stats.get('key_same', 0) + 1
        else:
            stats['keys_added'] = stats.get('keys_added', 0) + 1
        result[key] = value
    stats['keys_total'] = len(result)
    return result


def format_cache(cache: dict[str, str]) -> str:
    """Stable, diff-friendly JSON (sorted keys, UTF-8, trailing newline)."""
    return json.dumps(cache, ensure_ascii=False, indent=2, sort_keys=True) + '\n'


def save_cache(cache: dict[str, str], path: Path | None = None) -> None:
    """Atomic write (``.tmp`` then replace) so SIGKILL cannot corrupt the file."""
    target = path if path is not None else CACHE_FILE
    target.parent.mkdir(parents=True, exist_ok=True)
    tmp = target.with_suffix(target.suffix + '.tmp')
    tmp.write_text(format_cache(cache), encoding='utf-8')
    tmp.replace(target)


def load_legacy_union() -> dict[str, str]:
    """Union all legacy ``.cache/*.json`` maps (later files win on clash)."""
    merged: dict[str, str] = {}
    for path in LEGACY_CACHE_FILES:
        if path.exists():
            merged = union_caches(merged, _read_json_object(path))
    return merged


def load_cache(
    path: Path | None = None,
    *,
    migrate_legacy: bool | None = None,
) -> dict[str, str]:
    """Load a cache file, optionally migrating legacy ``.cache/`` files.

    * If ``path`` (default ``CACHE_FILE``) exists and is valid JSON, load it.
    * When ``migrate_legacy`` is true (default **only** when loading the
      module-level ``CACHE_FILE`` path), fold any keys from legacy
      ``scripts/.cache/*.json`` files into the result and rewrite the
      target if new keys were added. This seeds the checked-in cache from
      older machine-local caches without touching test temp files.
    * If the target is missing and migration is enabled, build from legacy
      (if any), write the target, and return that map.
    * Otherwise return ``{}``.
    """
    target = path if path is not None else CACHE_FILE
    if migrate_legacy is None:
        # Auto-migrate only for the configured canonical path so unit tests
        # that point CACHE_FILE / path at a temp file stay isolated.
        try:
            migrate_legacy = target.resolve() == CACHE_FILE.resolve()
        except OSError:
            migrate_legacy = False

    legacy = load_legacy_union() if migrate_legacy else {}

    if target.exists():
        current = _read_json_object(target)
        if not legacy:
            return current
        # Fold any legacy-only keys into the checked-in cache.
        stats: dict[str, int] = {}
        merged = union_caches(current, legacy, stats=stats)
        if stats.get('keys_added', 0) > 0:
            save_cache(merged, target)
            return merged
        return current

    if legacy:
        save_cache(legacy, target)
        return legacy
    return {}


def has_conflict_markers(text: str) -> bool:
    return '<<<<<<<' in text and '=======' in text and '>>>>>>>' in text


def split_conflicted_text(text: str) -> tuple[str, str]:
    """Rebuild full ``theirs`` and ``ours`` texts from git conflict markers.

    Non-conflict regions are shared. Marker lines themselves are dropped.
    Raises ``RuntimeError`` on unbalanced markers.
    """
    theirs_parts: list[str] = []
    ours_parts: list[str] = []
    pos = 0
    while True:
        m_start = START_RE.search(text, pos)
        if m_start is None:
            tail = text[pos:]
            theirs_parts.append(tail)
            ours_parts.append(tail)
            break
        m_sep = SEP_RE.search(text, m_start.end())
        m_end = END_RE.search(text, m_sep.end() if m_sep else m_start.end())
        if m_sep is None or m_end is None:
            raise RuntimeError(
                f'unbalanced conflict markers near offset {m_start.start()}'
            )
        shared = text[pos : m_start.start()]
        theirs_parts.append(shared)
        ours_parts.append(shared)
        theirs_parts.append(text[m_start.end() : m_sep.start()])
        ours_parts.append(text[m_sep.end() : m_end.start()])
        pos = m_end.end()
        # Drop a single trailing newline after the end marker so JSON
        # structure stays sane when markers sit between object entries.
        if pos < len(text) and text[pos] == '\n':
            # keep the newline in shared next iteration via pos as-is
            pass
    return ''.join(theirs_parts), ''.join(ours_parts)


def parse_cache_text(text: str, *, label: str = 'cache') -> dict[str, str]:
    """Parse a cache JSON object from text; empty dict on blank input."""
    stripped = text.strip()
    if not stripped:
        return {}
    try:
        data = json.loads(stripped)
    except json.JSONDecodeError as exc:
        raise RuntimeError(f'{label}: invalid JSON after conflict split: {exc}') from exc
    if not isinstance(data, dict):
        raise RuntimeError(f'{label}: cache root must be a JSON object')
    out: dict[str, str] = {}
    for k, v in data.items():
        if isinstance(k, str) and isinstance(v, str):
            out[k] = v
    return out


def resolve_conflicted_cache_text(
    text: str,
    *,
    filename: str = 'i18n_translate.json',
    stats: dict[str, int] | None = None,
) -> tuple[str, dict[str, str]]:
    """Resolve conflict markers in a cache file body by unioning both sides.

    Returns ``(formatted_json, merged_dict)``. ``ours`` (after ``=======``)
    wins on key collisions — same rule as TMX.
    """
    if stats is None:
        stats = {}
    if not has_conflict_markers(text):
        cache = parse_cache_text(text, label=filename)
        stats['keys_total'] = len(cache)
        stats['hunks'] = 0
        return format_cache(cache), cache

    theirs_text, ours_text = split_conflicted_text(text)
    theirs = parse_cache_text(theirs_text, label=f'{filename} (theirs)')
    ours = parse_cache_text(ours_text, label=f'{filename} (ours)')
    stats['hunks'] = len(START_RE.findall(text))
    stats['keys_theirs'] = len(theirs)
    stats['keys_ours'] = len(ours)
    merged = union_caches(theirs, ours, stats=stats)
    return format_cache(merged), merged
