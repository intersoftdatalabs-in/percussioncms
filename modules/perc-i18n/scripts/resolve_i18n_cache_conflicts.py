#!/usr/bin/env python3
"""Resolve merge conflicts in the checked-in i18n translation cache.

Same operating rule as ``resolve_tmx_conflicts.py``: **accept both** —
union all keys from both sides of every conflict hunk. On the same key
with different values, keep **ours** (working tree / after ``=======``).

No per-key human review. Run after a stash pop / merge / rebase leaves
``<<<<<<<`` markers in ``scripts/cache/i18n_translate.json``.

Usage
-----
    # Resolve conflict markers in the canonical cache file
    python3 modules/perc-i18n/scripts/resolve_i18n_cache_conflicts.py

    # Explicit path
    python3 modules/perc-i18n/scripts/resolve_i18n_cache_conflicts.py \\
        modules/perc-i18n/scripts/cache/i18n_translate.json

    # Union two clean cache files (no markers) into the canonical path
    python3 modules/perc-i18n/scripts/resolve_i18n_cache_conflicts.py \\
        --merge path/to/theirs.json path/to/ours.json

    python3 modules/perc-i18n/scripts/resolve_i18n_cache_conflicts.py --dry-run
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from i18n_cache import (
    CACHE_FILE,
    format_cache,
    has_conflict_markers,
    parse_cache_text,
    resolve_conflicted_cache_text,
    save_cache,
    union_caches,
)


def resolve_file(
    path: Path,
    *,
    dry_run: bool = False,
) -> dict[str, int]:
    if not path.exists():
        raise FileNotFoundError(path)
    text = path.read_text(encoding='utf-8')
    stats: dict[str, int] = {}
    if has_conflict_markers(text):
        new_text, merged = resolve_conflicted_cache_text(
            text, filename=path.name, stats=stats
        )
    else:
        merged = parse_cache_text(text, label=path.name)
        new_text = format_cache(merged)
        stats['hunks'] = 0
        stats['keys_total'] = len(merged)

    if dry_run:
        return stats
    # Use atomic writer (also normalizes formatting).
    save_cache(merged, path)
    # new_text kept for symmetry / future stdout mode
    _ = new_text
    return stats


def merge_files(
    theirs_path: Path,
    ours_path: Path,
    *,
    out_path: Path,
    dry_run: bool = False,
) -> dict[str, int]:
    theirs = parse_cache_text(
        theirs_path.read_text(encoding='utf-8'), label=str(theirs_path)
    )
    ours = parse_cache_text(
        ours_path.read_text(encoding='utf-8'), label=str(ours_path)
    )
    stats: dict[str, int] = {
        'keys_theirs': len(theirs),
        'keys_ours': len(ours),
        'hunks': 0,
    }
    merged = union_caches(theirs, ours, stats=stats)
    if not dry_run:
        save_cache(merged, out_path)
    return stats


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description=(
            'Union-resolve merge conflicts in the checked-in i18n translation '
            'cache (accept both sides; ours wins on key clash).'
        )
    )
    parser.add_argument(
        'path',
        nargs='?',
        type=Path,
        default=CACHE_FILE,
        help=f'Cache file with conflict markers (default: {CACHE_FILE})',
    )
    parser.add_argument(
        '--merge',
        nargs=2,
        metavar=('THEIRS', 'OURS'),
        type=Path,
        help='Union two clean JSON cache files instead of resolving markers',
    )
    parser.add_argument(
        '--out',
        type=Path,
        default=None,
        help='Output path for --merge (default: canonical CACHE_FILE)',
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Report stats without writing',
    )
    args = parser.parse_args(argv)

    try:
        if args.merge:
            out = args.out if args.out is not None else CACHE_FILE
            stats = merge_files(
                args.merge[0],
                args.merge[1],
                out_path=out,
                dry_run=args.dry_run,
            )
            action = 'would merge' if args.dry_run else 'merged'
            print(
                f'{action}: {args.merge[0]} + {args.merge[1]} -> {out} '
                f'(keys={stats.get("keys_total", 0)}, '
                f'collisions={stats.get("key_collisions", 0)}, '
                f'added_from_ours={stats.get("keys_added", 0)})'
            )
        else:
            path = args.path
            stats = resolve_file(path, dry_run=args.dry_run)
            action = 'would resolve' if args.dry_run else 'resolved'
            print(
                f'{action}: {path} '
                f'(hunks={stats.get("hunks", 0)}, '
                f'keys={stats.get("keys_total", 0)}, '
                f'collisions={stats.get("key_collisions", 0)})'
            )
    except (OSError, RuntimeError, FileNotFoundError) as exc:
        print(f'error: {exc}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
