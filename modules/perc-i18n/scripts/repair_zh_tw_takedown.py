#!/usr/bin/env python3
# Copyright (c) 2026 Intersoft Data Labs, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#
# See the License for the specific language governing permissions and
# limitations under the License.
"""Repair zh-tw script + "Take Down" mistranslations in the canonical TMX.

Issue #7131 found two systemic quality problems in the machine-translated
locales:

1. ``zh-tw`` ships Simplified Chinese: on main ~3,260 translation units
   across ``CmsUi.tmx`` / ``SystemResources.tmx`` / ``DeveloperUi.tmx``
   carry byte-identical ``zh-cn`` / ``zh-tw`` segments. This script converts
   the identical ``zh-tw`` segments to Traditional script with OpenCC
   ``s2twp`` (Taiwan phrasing) over the ``zh-cn`` source text. Units whose
   ``zh-tw`` already differs (human/review-fixed, e.g. ``登入`` vs
   ``登录``) are left untouched.
2. "Take Down" is translated literally ("write down" / "knock down" /
   "dismantle") instead of the product sense "remove from the live site".
   Existing mistranslated segments are replaced from the pinned glossary
   (:mod:`i18n_glossary`); only segments that already exist are rewritten —
   missing TUVs stay missing (back-fill via ``i18n_translate.py``, which now
   consults the same glossary, e.g. PR #7116's new batch-helper TUs).

The matching entries in the shared checked-in cache
(``scripts/cache/i18n_translate.json``) are rewritten to the repaired text
so the next back-fill / ``--force`` run cannot regress them. ``en-us``
source segments are never touched.

Requires the ``opencc`` Python package (``pip install opencc``) when a
``zh-tw`` conversion is pending; fail-loud otherwise.

Usage (from repository root)::

    python3 modules/perc-i18n/scripts/repair_zh_tw_takedown.py --dry-run
    python3 modules/perc-i18n/scripts/repair_zh_tw_takedown.py --file CmsUi.tmx
    python3 modules/perc-i18n/scripts/repair_zh_tw_takedown.py --skip-zh-tw

Cross-platform: :mod:`pathlib` only; explicit ``encoding='utf-8'`` everywhere.
"""

from __future__ import annotations

import argparse
import hashlib
import html
import re
import sys
from pathlib import Path
from xml.sax.saxutils import escape as xml_escape

SCRIPTS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIR))

import i18n_cache as _i18n_cache  # noqa: E402
import i18n_glossary as _i18n_glossary  # noqa: E402

I18N_DIR = SCRIPTS_DIR.parent / 'src' / 'main' / 'resources' / 'i18n'
DEFAULT_FILES = ('CmsUi.tmx', 'SystemResources.tmx', 'DeveloperUi.tmx')
CACHE_FILE = _i18n_cache.CACHE_FILE

SOURCE_LANG = 'en-us'
TARGET_ZH_TW = 'zh-tw'
PLACEHOLDER_RE = re.compile(r'\{[0-9]+(,[0-9]+)*\}')


def cache_key(text: str, target: str) -> str:
    """Cache key scheme shared with the translate CLIs."""
    return hashlib.sha256(f'{target}\0{text}'.encode('utf-8')).hexdigest()


def _converter():
    """Lazy OpenCC s2twp converter with a fail-loud install hint."""
    try:
        from opencc import OpenCC
    except ImportError as e:
        raise RuntimeError(
            'The opencc Python package is required for the zh-tw '
            "conversion (pip install opencc). "
            'Use --skip-zh-tw to run only the Take Down repair.'
        ) from e
    return OpenCC('s2twp').convert


def _check_placeholders(old: str, new: str, where: str,
                        problems: list[str]) -> bool:
    """Guard: MessageFormat placeholders must survive the rewrite."""
    if PLACEHOLDER_RE.findall(old) != PLACEHOLDER_RE.findall(new):
        problems.append(f'{where}: placeholders changed '
                        f'{PLACEHOLDER_RE.findall(old)!r} -> '
                        f'{PLACEHOLDER_RE.findall(new)!r}; left in place')
        return False
    if old and not new:
        problems.append(f'{where}: conversion produced empty segment; '
                        'left in place')
        return False
    return True


def plan_file(path: Path, convert_zh_tw: bool, apply_takedown: bool,
              convert=None) -> tuple[dict[str, dict[str, tuple[str, str]]],
                                     int, int, list[str]]:
    """Compute the repair plan for one TMX file.

    Returns ``(plan, identical, differing, problems)`` where ``plan`` maps
    ``tuid -> {lang -> (old_seg, new_seg)}`` (decoded text), ``identical``
    counts byte-identical zh-cn/zh-tw pairs and ``differing`` counts pairs
    already distinct (left alone).
    """
    import xml.etree.ElementTree as ET

    root = ET.fromstring(path.read_text(encoding='utf-8'))
    plan: dict[str, dict[str, tuple[str, str]]] = {}
    counts = [0, 0]  # [identical, differing]
    problems: list[str] = []
    for tu in root.iter('tu'):
        _plan_tu(tu, path.name, plan, counts, problems,
                 convert_zh_tw, apply_takedown, convert)
    return plan, counts[0], counts[1], problems


def _plan_tu(tu, file_name: str,
             plan: dict[str, dict[str, tuple[str, str]]],
             counts: list[int], problems: list[str],
             convert_zh_tw: bool, apply_takedown: bool,
             convert) -> None:
    """Schedule repairs for one ``<tu>`` element."""
    tuid = tu.get('tuid') or ''
    if not tuid:
        return
    segs: dict[str, str] = {}
    for tuv in tu.findall('tuv'):
        lang = (tuv.get('{http://www.w3.org/XML/1998/namespace}lang')
                or tuv.get('xml:lang') or '')
        seg_el = tuv.find('seg')
        segs[lang] = (seg_el.text or '') if seg_el is not None else ''
    if convert_zh_tw and TARGET_ZH_TW in segs and 'zh-cn' in segs:
        if segs[TARGET_ZH_TW] == segs['zh-cn']:
            counts[0] += 1
            assert convert is not None
            _schedule(plan, problems, file_name, tuid, TARGET_ZH_TW,
                      segs[TARGET_ZH_TW], convert(segs['zh-cn']))
        else:
            counts[1] += 1
    if apply_takedown:
        _plan_takedown(segs, file_name, tuid, plan, problems)


def _plan_takedown(segs: dict[str, str], file_name: str, tuid: str,
                   plan: dict[str, dict[str, tuple[str, str]]],
                   problems: list[str]) -> None:
    """Schedule glossary replacements for one ``<tu>`` (product-term sense)."""
    en = segs.get(SOURCE_LANG, '').strip()
    if en not in _i18n_glossary.TERMS:
        return
    for lang, pinned in _i18n_glossary.TERMS[en].items():
        if lang in segs:
            _schedule(plan, problems, file_name, tuid, lang,
                      segs[lang], pinned)


def _schedule(plan: dict[str, dict[str, tuple[str, str]]],
              problems: list[str], file_name: str, tuid: str,
              lang: str, old: str, new: str) -> None:
    """Record one segment rewrite after the placeholder/empty guards."""
    if new == old:
        return
    where = f'{file_name} :: {tuid} [{lang}]'
    if _check_placeholders(old, new, where, problems):
        plan.setdefault(tuid, {})[lang] = (old, new)


def apply_plan(text: str, plan: dict[str, dict[str, tuple[str, str]]],
               file_name: str, problems: list[str]) -> tuple[str, int]:
    """Rewrite planned segments in raw TMX text, preserving formatting.

    Returns ``(new_text, applied)``. A segment whose raw form does not
    decode to the expected old text is left in place and reported.
    """
    # Accept single-quoted tuids holding double quotes (e.g. "{0}").
    tu_pattern = re.compile(
        r'(<tu\s+tuid=(?:"(?P<tuid_d>[^"]+)"|\'(?P<tuid_s>[^\']+)\')[^>]*>.*?</tu>)',
        re.DOTALL)
    tuv_pattern = re.compile(
        r'<tuv\s+xml:lang="(?P<lang>[^"]+)">(?P<body>.*?)</tuv>', re.DOTALL)
    applied = 0

    def fix_tu(m: re.Match[str]) -> str:
        nonlocal applied
        raw = m.group('tuid_d') if m.group('tuid_d') is not None else m.group('tuid_s')
        tuid = html.unescape(raw)
        if tuid not in plan:
            return m.group(0)
        wanted = plan[tuid]

        def fix_tuv(tm: re.Match[str]) -> str:
            nonlocal applied
            lang = tm.group('lang')
            if lang not in wanted:
                return tm.group(0)
            old, new = wanted[lang]
            seg_m = re.fullmatch(r'<seg>(?P<seg>.*)</seg>', tm.group('body'),
                                 re.DOTALL)
            if seg_m is None:
                problems.append(f'{file_name} :: {tuid} [{lang}]: '
                                'non-plain <seg> body; left in place')
                return tm.group(0)
            if html.unescape(seg_m.group('seg')) != old:
                problems.append(f'{file_name} :: {tuid} [{lang}]: raw text '
                                'does not match parsed segment; left in place')
                return tm.group(0)
            applied += 1
            return (f'<tuv xml:lang="{xml_escape(lang)}">'
                    f'<seg>{xml_escape(new)}</seg></tuv>')

        return tuv_pattern.sub(fix_tuv, m.group(0))

    return tu_pattern.sub(fix_tu, text), applied


def sync_cache(cache: dict[str, str], path: Path,
               plan: dict[str, dict[str, tuple[str, str]]]) -> tuple[int, int]:
    """Point cache entries at the repaired segments. Returns (updated, added)."""
    en_by_tuid = _read_en_map(path)
    updated = 0
    added = 0
    for tuid, langs in plan.items():
        en_seg = en_by_tuid.get(tuid, '')
        if not en_seg:
            continue
        for _lang, (_old, new) in langs.items():
            key = cache_key(en_seg, _lang)
            if key in cache:
                if cache[key] != new:
                    cache[key] = new
                    updated += 1
            else:
                cache[key] = new
                added += 1
    return updated, added


def _read_en_map(path: Path) -> dict[str, str]:
    """Map ``tuid -> en-us seg`` for one TMX file."""
    import xml.etree.ElementTree as ET

    root = ET.fromstring(path.read_text(encoding='utf-8'))
    en_by_tuid: dict[str, str] = {}
    for tu in root.iter('tu'):
        for tuv in tu.findall('tuv'):
            lang = (tuv.get('{http://www.w3.org/XML/1998/namespace}lang')
                    or tuv.get('xml:lang') or '')
            if lang == SOURCE_LANG:
                seg_el = tuv.find('seg')
                en_by_tuid[tu.get('tuid') or ''] = (
                    seg_el.text or '') if seg_el is not None else ''
    return en_by_tuid


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--file', action='append', dest='files',
                        help='Restrict to a single TMX file (repeatable). '
                             f'Default: {", ".join(DEFAULT_FILES)}.')
    parser.add_argument('--dry-run', action='store_true',
                        help='Report repairs without writing TMX or cache.')
    parser.add_argument('--skip-zh-tw', action='store_true',
                        help='Only apply the Take Down glossary repair.')
    parser.add_argument('--skip-takedown', action='store_true',
                        help='Only apply the zh-tw conversion.')
    parser.add_argument('--verbose', action='store_true',
                        help='List every planned segment change.')
    args = parser.parse_args(argv)

    files = _resolve_files(args.files)
    if files is None:
        return 2

    convert = None
    if not args.skip_zh_tw:
        try:
            convert = _converter()
        except RuntimeError as e:
            print(f'error: {e}', file=sys.stderr)
            return 2

    cache = _i18n_cache.load_cache(CACHE_FILE)
    results, problems = _collect_plans(files, not args.skip_zh_tw,
                                       not args.skip_takedown, convert,
                                       args.verbose)
    return _apply_results(results, problems, cache, args.dry_run)


def _resolve_files(files_arg: list[str] | None) -> list[Path] | None:
    """Resolve ``--file`` args against the canonical i18n dir.

    Returns the file list, or ``None`` (after printing an error) when a
    path is absolute or missing.
    """
    files: list[Path] = []
    for name in (files_arg or list(DEFAULT_FILES)):
        candidate = Path(name)
        if candidate.is_absolute():
            print(f'error: --file path must be relative to {I18N_DIR}: {name}',
                  file=sys.stderr)
            return None
        files.append(I18N_DIR / candidate)
    for f in files:
        if not f.exists():
            print(f'error: {f} not found', file=sys.stderr)
            return None
    return files


def _collect_plans(files: list[Path], do_zh_tw: bool, do_takedown: bool,
                   convert, verbose: bool
                   ) -> tuple[list[tuple[Path, dict, int, int]], list[str]]:
    """Plan every file and print the per-file summary. Returns results + problems."""
    results: list[tuple[Path, dict, int, int]] = []
    all_problems: list[str] = []
    for f in files:
        plan, identical, differing, problems = plan_file(
            f, do_zh_tw, do_takedown, convert)
        all_problems.extend(problems)
        print(f'{f.name}: {identical} identical zh-cn/zh-tw pairs '
              f'({differing} already distinct, left alone); '
              f'{sum(len(v) for v in plan.values())} segment(s) to repair')
        _print_samples(plan, verbose)
        results.append((f, plan, identical, differing))
    return results, all_problems


def _print_samples(plan: dict[str, dict[str, tuple[str, str]]],
                   verbose: bool) -> None:
    """Print the first sample repairs (or every repair with --verbose)."""
    total = sum(len(v) for v in plan.values())
    shown = 0
    for tuid, langs in plan.items():
        for lang, (old, new) in langs.items():
            if not verbose and shown >= 12:
                print(f'  ... and {total - shown} more')
                return
            print(f'  {tuid} [{lang}]: {old[:48]!r} -> {new[:48]!r}')
            shown += 1


def _apply_results(results: list[tuple[Path, dict, int, int]],
                   plan_problems: list[str],
                   cache: dict[str, str], dry_run: bool) -> int:
    """Write TMX + cache updates for every plan. Returns the exit code."""
    all_problems: list[str] = list(plan_problems)
    total_applied = 0
    total_updated = 0
    total_added = 0
    for f, plan, _identical, _differing in results:
        if dry_run or not plan:
            continue
        new_text, applied = apply_plan(
            f.read_text(encoding='utf-8'), plan, f.name, all_problems)
        if applied:
            f.write_text(new_text, encoding='utf-8')
        total_applied += applied
        updated, added = sync_cache(cache, f, plan)
        total_updated += updated
        total_added += added
    for p in all_problems:
        print(f'  ADJUDICATE: {p}')
    if total_applied and not dry_run:
        _i18n_cache.save_cache(cache, CACHE_FILE)
        print(f'cache: {total_updated} entr(ies) updated, '
              f'{total_added} added for repaired segments')
    print(f'\nDone. Segments repaired: {total_applied}; '
          f'sites needing adjudication: {len(all_problems)}')
    return 1 if all_problems else 0


if __name__ == '__main__':
    sys.exit(main())
