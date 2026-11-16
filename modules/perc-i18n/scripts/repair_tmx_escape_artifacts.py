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
"""Repair ASCII escape artifacts left by the translation pipeline in TMX files.

Translation engines (via ``i18n_translate.py`` / ``i18n_translate_direct.py``)
sometimes emit dropped-backslash escapes as literal ASCII — e.g. ``u200c``
instead of a real zero-width non-joiner. Nothing at runtime unescapes them,
so they render as visible garbage, and the ``perc-i18n`` integrity canaries
(``PSTmxCatalogIntegrityTest``, ``PSTipTapEditorKeysTmxTest``) fail the build.

Repair rules (precedent: issues #6173 / #6276 — do not invent new ones):

* ``u200c`` (Telugu ``te`` only): every audited site sits immediately after a
  virama (U+0C4D) before a consonant — a genuine ZWNJ position preventing
  unwanted conjuncts — so each ASCII occurrence is replaced with real U+200C.
  An occurrence NOT after a virama is left in place and reported; the run
  exits nonzero so a human adjudicates instead of guessing.
* ``u200b``: every audited occurrence was a doubled run at a word boundary
  where a space already precedes it (or inside CJK/katakana runs that take no
  space) — a zero-width space was never genuine there — so each maximal ASCII
  run is deleted.

The script also rewrites the matching entries in the shared checked-in cache
(``scripts/cache/i18n_translate.json``) so cache and TMX stay in sync, and
never touches ``en-us`` source segments.

Usage (from repository root)::

    python3 modules/perc-i18n/scripts/repair_tmx_escape_artifacts.py --dry-run
    python3 modules/perc-i18n/scripts/repair_tmx_escape_artifacts.py --file CmsUi.tmx

Cross-platform: :mod:`pathlib` only; explicit ``encoding='utf-8`` everywhere.
"""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from pathlib import Path
from xml.sax.saxutils import escape as xml_escape

SCRIPTS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIR))

import i18n_cache as _i18n_cache  # noqa: E402

I18N_DIR = SCRIPTS_DIR.parent / 'src' / 'main' / 'resources' / 'i18n'
DEFAULT_FILES = ('CmsUi.tmx', 'SystemResources.tmx', 'DeveloperUi.tmx')
CACHE_FILE = _i18n_cache.CACHE_FILE

SOURCE_LANG = 'en-us'
VIRAMA_TE = '\u0c4d'
ZWNJ = '\u200c'

ARTIFACT_RES = {
    'u200c': re.compile(r'u200c'),
    'u200b': re.compile(r'u200b+'),
}


def cache_key(text: str, target: str) -> str:
    """Cache key scheme shared with the translate CLIs."""
    return hashlib.sha256(f'{target}\0{text}'.encode('utf-8')).hexdigest()


def repair_segment(seg: str, lang: str, artifact: str) -> tuple[str, list[str]]:
    """Return ``(repaired, problems)`` for one segment.

    ``problems`` lists human-readable adjudication notes for sites that do
    not match the precedent rule; those sites are left unrepaired.
    """
    problems: list[str] = []
    if artifact == 'u200c':
        if lang != 'te':
            return seg, [f'u200c repair is Telugu-only (lang={lang}); skipped']
        out: list[str] = []
        pos = 0
        for m in ARTIFACT_RES['u200c'].finditer(seg):
            prev = seg[m.start() - 1] if m.start() > 0 else ''
            if prev != VIRAMA_TE:
                problems.append(
                    f'u200c at index {m.start()} not after virama '
                    f'(prev={prev!r}); left in place'
                )
                continue
            out.append(seg[pos:m.start()])
            out.append(ZWNJ)
            pos = m.end()
        out.append(seg[pos:])
        return ''.join(out), problems
    if artifact == 'u200b':
        return ARTIFACT_RES['u200b'].sub('', seg), problems
    raise ValueError(f'unknown artifact: {artifact}')


def repair_text(
    text: str, lang_filter: str | None = None
) -> tuple[str, int, list[str]]:
    """Repair all known artifacts in raw TMX text for one language.

    Returns ``(new_text, repaired_segments, problems)``. Operates on the raw
    file text with a ``<tu>``-scoped regex (same approach as
    ``i18n_translate.TmxFile.inject``) so surrounding formatting is preserved.
    """
    repaired_segments = 0
    problems: list[str] = []
    tu_pattern = re.compile(r'(<tu\s+tuid="(?P<tuid>[^"]+)"[^>]*>.*?</tu>)',
                            re.DOTALL)
    tuv_pattern = re.compile(
        r'<tuv\s+xml:lang="(?P<lang>[^"]+)">(?P<body>.*?)</tuv>', re.DOTALL)

    def fix_tu(m: re.Match[str]) -> str:
        nonlocal repaired_segments
        tu_text = m.group(0)
        tuid = m.group('tuid')

        def fix_tuv(tm: re.Match[str]) -> str:
            nonlocal repaired_segments
            lang = tm.group('lang')
            if lang_filter is not None and lang != lang_filter:
                return tm.group(0)
            if lang == SOURCE_LANG:
                return tm.group(0)
            body = tm.group('body')
            seg_m = re.fullmatch(r'<seg>(?P<seg>.*)</seg>', body, re.DOTALL)
            if seg_m is None:
                return tm.group(0)
            seg = seg_m.group('seg')
            new_seg = seg
            for artifact in ARTIFACT_RES:
                if artifact not in new_seg:
                    continue
                # u200c repair is Telugu-only; other langs keep the artifact
                # and get a problem note instead of a silent pass.
                fixed, probs = repair_segment(new_seg, lang, artifact)
                new_seg = fixed
                problems.extend(f'{tuid} [{lang}]: {p}' for p in probs)
            if new_seg != seg:
                repaired_segments += 1
                return (f'<tuv xml:lang="{xml_escape(lang)}">'
                        f'<seg>{new_seg}</seg></tuv>')
            return tm.group(0)

        return tuv_pattern.sub(fix_tuv, tu_text)

    return tu_pattern.sub(fix_tu, text), repaired_segments, problems


def _collect_targets(
    tu: 'ET.Element', lang_filter: str | None
) -> tuple[str, dict[str, str]]:
    """Return ``(en_seg, {lang: seg})`` for one ``<tu>`` element.

    Segments that still carry raw ASCII artifacts are excluded — repair
    deliberately left them for human adjudication.
    """
    en_seg = ''
    targets: dict[str, str] = {}
    for tuv in tu.findall('tuv'):
        lang = tuv.get('{http://www.w3.org/XML/1998/namespace}lang') or ''
        seg = tuv.findtext('seg') or ''
        if lang == SOURCE_LANG:
            en_seg = seg
            continue
        if lang_filter is not None and lang != lang_filter:
            continue
        if any(a in seg for a in ARTIFACT_RES):
            continue
        targets[lang] = seg
    return en_seg, targets


def sync_cache_for_file(
    path: Path, cache: dict[str, str], lang_filter: str | None = None
) -> int:
    """Rewrite cache values to match repaired TMX segments.

    Returns the number of cache entries updated. Only touches entries whose
    English source pivot is present in the file.
    """
    import xml.etree.ElementTree as ET

    root = ET.fromstring(path.read_text(encoding='utf-8'))
    updated = 0
    for tu in root.iter('tu'):
        en_seg, targets = _collect_targets(tu, lang_filter)
        for lang, seg in targets.items():
            key = cache_key(en_seg, lang)
            if key in cache and cache[key] != seg:
                cache[key] = seg
                updated += 1
    return updated


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--file', action='append', dest='files',
                        help='Restrict to a single TMX file (repeatable). '
                             f'Default: {", ".join(DEFAULT_FILES)}.')
    parser.add_argument('--lang', default=None,
                        help='Restrict to a single xml:lang (e.g. te). '
                             'Default: all non-en-us TUVs.')
    parser.add_argument('--dry-run', action='store_true',
                        help='Report repairs without writing TMX or cache.')
    args = parser.parse_args(argv)

    files = _resolve_files(args.files)
    if files is None:
        return 2

    cache = _i18n_cache.load_cache(CACHE_FILE)
    total_repaired = 0
    all_problems: list[str] = []
    for f in files:
        repaired, problems = _repair_one_file(f, args.lang, args.dry_run)
        all_problems.extend(problems)
        total_repaired += repaired

    if total_repaired and not args.dry_run:
        updated = sum(sync_cache_for_file(f, cache, args.lang) for f in files)
        _i18n_cache.save_cache(cache, CACHE_FILE)
        print(f'cache: {updated} entr(ies) synced to repaired segments')

    print(f'\nDone. Segments repaired: {total_repaired}; '
          f'sites needing adjudication: {len(all_problems)}')
    return 1 if all_problems else 0


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


def _repair_one_file(f: Path, lang: str | None, dry_run: bool) -> tuple[int, list[str]]:
    """Repair one TMX file. Returns ``(repaired, problems)``."""
    text = f.read_text(encoding='utf-8')
    new_text, repaired, problems = repair_text(text, lang)
    print(f'{f.name}: {repaired} segment(s) repaired'
          + (f' [lang={lang}]' if lang else ''))
    for p in problems:
        print(f'  ADJUDICATE {f.name}: {p}')
    if repaired and not dry_run:
        f.write_text(new_text, encoding='utf-8')
    return repaired, [f'{f.name}: {p}' for p in problems]


if __name__ == '__main__':
    sys.exit(main())
