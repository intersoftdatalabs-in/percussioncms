#!/usr/bin/env python3
"""Fill missing translations using trans (translate-shell) directly.

This script is a variant of i18n_translate.py that uses ``trans``
(translate-shell) on PATH instead of Docker. Rate limits use the same
exponential backoff contract as ``i18n_translate.py`` (2s base, 60s cap,
±20% jitter, up to 5 attempts). Successful calls do not sleep.

Usage:
    python3 modules/perc-i18n/scripts/i18n_translate_direct.py --target hi
    python3 modules/perc-i18n/scripts/i18n_translate_direct.py --target hi --fix-matching-en
    python3 modules/perc-i18n/scripts/i18n_translate_direct.py --target hi-in --variant-base hi
"""
from __future__ import annotations

import argparse
import hashlib
import json
import random
import re
import subprocess
import sys
import time
from pathlib import Path
from xml.sax.saxutils import escape as xml_escape

REPO_ROOT = Path(__file__).resolve().parent.parent.parent.parent
I18N_DIR = REPO_ROOT / 'modules' / 'perc-i18n' / 'src' / 'main' / 'resources' / 'i18n'
CACHE_FILE = Path(__file__).resolve().parent / '.cache' / 'i18n_translate_direct.json'

DEFAULT_FILES = ('CmsUi.tmx', 'SystemResources.tmx')

PLACEHOLDER_RE = re.compile(r'^\s*\{[0-9]+(,[0-9]+)*\}\s*$')

SOURCE_LANG = 'en-us'

# Exponential-backoff parameters (shared contract with i18n_translate.py).
BACKOFF_START_SEC = 2.0
BACKOFF_MAX_SEC = 60.0
BACKOFF_JITTER = 0.2
BACKOFF_MAX_ATTEMPTS = 5

# Variant locale mapping: variant -> base
VARIANT_BASES = {
    'hi-in': 'hi',
    'es-cl': 'es',
    'es-es': 'es',
    'es-mx': 'es',
    'fr-ca': 'fr-fr',
    'fr-fr': 'fr-fr',
    'pt-br': 'pt-pt',
    'pt-pt': 'pt-pt',
    'en-gb': 'en-us',
}


def cache_key(text: str, target: str) -> str:
    h = hashlib.sha256()
    h.update(target.encode('utf-8'))
    h.update(b'\x00')
    h.update(text.encode('utf-8'))
    return h.hexdigest()


def load_cache() -> dict[str, str]:
    if CACHE_FILE.exists():
        try:
            return json.loads(CACHE_FILE.read_text(encoding='utf-8'))
        except (OSError, json.JSONDecodeError):
            return {}
    return {}


def save_cache(cache: dict[str, str]) -> None:
    CACHE_FILE.parent.mkdir(parents=True, exist_ok=True)
    tmp = CACHE_FILE.with_suffix('.tmp')
    tmp.write_text(json.dumps(cache, ensure_ascii=False, indent=2, sort_keys=True),
                   encoding='utf-8')
    tmp.replace(CACHE_FILE)


def invoke_translate(text: str, target: str, *,
                     trans_cmd: list[str] | None = None) -> str:
    """Run ``trans`` (translate-shell) on PATH. Returns the translated string.

    Raises :class:`RuntimeError` if ``trans`` is unavailable or the translation
    could not be obtained after exhausting retries. Rate-limit responses
    (429 / "too many requests") trigger exponential backoff; successful calls
    do not sleep.
    """
    cmd = trans_cmd if trans_cmd is not None else [
        'trans', '--brief', f':{target}', text,
    ]
    delay = BACKOFF_START_SEC
    attempt = 0
    while True:
        attempt += 1
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                check=False,
                encoding='utf-8',
            )
        except FileNotFoundError as e:
            raise RuntimeError(
                'trans (translate-shell) is not on PATH. Install translate-shell '
                '(https://github.com/soimort/translate-shell) or use '
                'i18n_translate.py with Docker. Underlying error: '
                f'{e}',
            ) from e
        if result.returncode == 0:
            return result.stdout.rstrip('\n')
        lowered = (result.stderr + result.stdout).lower()
        is_rate_limit = (
            '429' in lowered
            or 'rate limit' in lowered
            or 'too many requests' in lowered
        )
        if is_rate_limit and attempt < BACKOFF_MAX_ATTEMPTS:
            jitter = 1.0 + random.uniform(-BACKOFF_JITTER, BACKOFF_JITTER)
            sleep_s = min(BACKOFF_MAX_SEC, delay) * jitter
            print(
                f'  rate limit; sleeping {sleep_s:.1f}s (attempt {attempt})',
                file=sys.stderr,
            )
            time.sleep(sleep_s)
            delay *= 2
            continue
        raise RuntimeError(
            f'trans failed (rc={result.returncode}): '
            f'stdout={result.stdout!r} stderr={result.stderr!r}',
        )


def translate(text: str, target: str, *, cache: dict[str, str] | None = None, force: bool = False) -> str:
    """Translate text to target, honoring cache."""
    if PLACEHOLDER_RE.match(text):
        return text
    if cache is None:
        cache = load_cache()
    key = cache_key(text, target)
    if not force and key in cache:
        print(f"  [cache] {text[:30]}... -> {target}")
        return cache[key]
    print(f"  [trans] {text[:30]}... -> {target}")
    translated = invoke_translate(text, target)
    cache[key] = translated
    return translated


class TmxFile:
    def __init__(self, path: Path):
        self.path = path
        self.text = path.read_text(encoding='utf-8')

    def _parse(self):
        import xml.etree.ElementTree as ET
        return ET.fromstring(self.text)

    def list_missing(self, target: str) -> list[tuple[str, str]]:
        """Return [(tuid, en_seg)] for every <tu> missing target lang."""
        root = self._parse()
        missing = []
        for tu in root.iter('tu'):
            tuid = tu.get('tuid') or ''
            if not tuid:
                continue
            en_seg = ''
            has_target = False
            for tuv in tu.findall('tuv'):
                tlang = tuv.get('{http://www.w3.org/XML/1998/namespace}lang') or tuv.get('xml:lang')
                if tlang == target:
                    has_target = True
                    break
                if tlang == SOURCE_LANG:
                    seg_el = tuv.find('seg')
                    if seg_el is not None and seg_el.text:
                        en_seg = seg_el.text
            if not has_target and en_seg:
                missing.append((tuid, en_seg))
        return missing

    def list_matching_en(self, target: str) -> list[tuple[str, str, str]]:
        """Return [(tuid, en_seg, target_seg)] for where target matches en-us."""
        root = self._parse()
        matching = []
        for tu in root.iter('tu'):
            tuid = tu.get('tuid') or ''
            if not tuid:
                continue
            en_seg = ''
            target_seg = ''
            for tuv in tu.findall('tuv'):
                tlang = tuv.get('{http://www.w3.org/XML/1998/namespace}lang') or tuv.get('xml:lang')
                seg_el = tuv.find('seg')
                text = seg_el.text if seg_el is not None and seg_el.text else ''
                if tlang == SOURCE_LANG:
                    en_seg = text
                elif tlang == target:
                    target_seg = text
            if en_seg and target_seg and en_seg.strip() == target_seg.strip():
                matching.append((tuid, en_seg, target_seg))
        return matching

    def get_base_translations(self, base_lang: str) -> dict[str, str]:
        """Get all translations for base_lang: {tuid: seg_text}."""
        root = self._parse()
        translations = {}
        for tu in root.iter('tu'):
            tuid = tu.get('tuid') or ''
            if not tuid:
                continue
            for tuv in tu.findall('tuv'):
                tlang = tuv.get('{http://www.w3.org/XML/1998/namespace}lang') or tuv.get('xml:lang')
                if tlang == base_lang:
                    seg_el = tuv.find('seg')
                    if seg_el is not None and seg_el.text:
                        translations[tuid] = seg_el.text
                    break
        return translations

    def inject(self, target: str, translations: dict[str, str]) -> int:
        """Insert missing <tuv> blocks. Returns number inserted."""
        if not translations:
            return 0
        inserted = 0
        out = []
        pos = 0
        tu_pattern = re.compile(r'<tu\s+tuid="([^"]+)"[^>]*>.*?</tu>', re.DOTALL)
        for m in tu_pattern.finditer(self.text):
            tuid = m.group(1)
            if tuid not in translations:
                continue
            tu_text = m.group(0)
            if re.search(rf'<tuv\s+xml:lang="{re.escape(target)}"', tu_text):
                continue
            seg = xml_escape(translations[tuid])
            new_tuv = f'<tuv xml:lang="{xml_escape(target)}"><seg>{seg}</seg></tuv>'
            replacement = tu_text[:-len('</tu>')] + new_tuv + '</tu>'
            out.append(self.text[pos:m.start()])
            out.append(replacement)
            pos = m.end()
            inserted += 1
        out.append(self.text[pos:])
        if inserted:
            self.text = ''.join(out)
        return inserted

    def replace_translation(self, target: str, translations: dict[str, str]) -> int:
        """Replace existing translations. Returns number replaced."""
        if not translations:
            return 0
        replaced = 0
        out = []
        pos = 0
        tu_pattern = re.compile(r'<tu\s+tuid="([^"]+)"[^>]*>.*?</tu>', re.DOTALL)
        for m in tu_pattern.finditer(self.text):
            tuid = m.group(1)
            if tuid not in translations:
                continue
            tu_text = m.group(0)
            # Find and replace the target tuv
            pattern = rf'(<tuv\s+xml:lang="{re.escape(target)}"[^>]*>)\s*<seg>(.*?)</seg>\s*(</tuv>)'
            def replacer(match):
                nonlocal replaced
                replaced += 1
                seg = xml_escape(translations[tuid])
                return f'{match.group(1)}<seg>{seg}</seg>{match.group(3)}'
            new_tu_text = re.sub(pattern, replacer, tu_text, flags=re.DOTALL)
            out.append(self.text[pos:m.start()])
            out.append(new_tu_text)
            pos = m.end()
        out.append(self.text[pos:])
        if replaced:
            self.text = ''.join(out)
        return replaced

    def commit(self):
        self.path.write_text(self.text, encoding='utf-8')


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target', required=True, help='Target BCP-47 locale code')
    parser.add_argument('--file', action='append', dest='files', help='TMX file to process')
    parser.add_argument('--dry-run', action='store_true', help='Report without translating')
    parser.add_argument('--force', action='store_true', help='Ignore cache')
    parser.add_argument('--fix-matching-en', action='store_true', help='Fix translations matching English')
    parser.add_argument('--variant-base', help='Base locale for variant (e.g., hi for hi-in)')
    parser.add_argument('--limit', type=int, default=0, help='Max translations (0=unlimited)')
    args = parser.parse_args(argv)

    files = []
    for name in (args.files or list(DEFAULT_FILES)):
        candidate = Path(name)
        files.append(I18N_DIR / candidate if not candidate.is_absolute() else candidate)

    target = args.target
    base_lang = args.variant_base or VARIANT_BASES.get(target)

    cache = load_cache()
    total_missing = 0
    total_inserted = 0
    total_fixed = 0

    for f in files:
        if not f.exists():
            print(f'error: {f} not found', file=sys.stderr)
            return 2

        tmx = TmxFile(f)

        # Step 1: Handle missing translations
        missing = tmx.list_missing(target)
        total_missing += len(missing)
        print(f'{f.name}: {len(missing)} missing <tuv xml:lang="{target}">')

        if args.dry_run or not missing:
            pass
        else:
            translations = {}
            processed = 0
            for tuid, en_seg in missing:
                if args.limit and processed >= args.limit:
                    break
                try:
                    translations[tuid] = translate(en_seg, target, cache=cache, force=args.force)
                except Exception as e:
                    print(f'  ERROR: {e}', file=sys.stderr)
                    save_cache(cache)
                    return 1
                processed += 1
                if processed % 10 == 0:
                    print(f'  ... {processed}/{len(missing)}')
                    save_cache(cache)
            save_cache(cache)

            if translations:
                # For variant locales, filter to only include differences from base
                if base_lang and base_lang != target:
                    base_trans = tmx.get_base_translations(base_lang)
                    filtered = {k: v for k, v in translations.items()
                                if k not in base_trans or base_trans[k] != v}
                    print(f'  variant filter: {len(translations)} -> {len(filtered)} (only differences)')
                    translations = filtered

                inserted = tmx.inject(target, translations)
                if not args.dry_run:
                    tmx.commit()
                total_inserted += inserted
                print(f'  inserted {inserted} TUVs')

        # Step 2: Fix translations matching English (only for base locales)
        if args.fix_matching_en and not base_lang:
            matching = tmx.list_matching_en(target)
            total_fixed += len(matching)
            print(f'{f.name}: {len(matching)} match English, fixing...')

            if args.dry_run or not matching:
                pass
            else:
                translations = {}
                processed = 0
                for tuid, en_seg, _ in matching:
                    if args.limit and processed >= args.limit:
                        break
                    try:
                        translations[tuid] = translate(en_seg, target, cache=cache, force=args.force)
                    except Exception as e:
                        print(f'  ERROR: {e}', file=sys.stderr)
                        save_cache(cache)
                        return 1
                    processed += 1
                    if processed % 10 == 0:
                        print(f'  ... {processed}/{len(matching)}')
                        save_cache(cache)
                save_cache(cache)

                if translations:
                    replaced = tmx.replace_translation(target, translations)
                    if not args.dry_run:
                        tmx.commit()
                    print(f'  fixed {replaced} translations')

    print(f'\nDone. Missing: {total_missing}; inserted: {total_inserted}; fixed: {total_fixed}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
