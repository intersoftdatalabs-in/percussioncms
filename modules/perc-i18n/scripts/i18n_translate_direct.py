#!/usr/bin/env python3
"""Fill missing translations using translate-shell.

This script is a variant of i18n_translate.py that prefers ``trans``
(translate-shell) on PATH, falling back to the ``soimort/translate-shell``
Docker image when ``trans`` is not available. Pass ``--docker`` to force
Docker (typical on Windows where only Docker Desktop is installed). Rate
limits use the same exponential backoff contract as ``i18n_translate.py``
(2s base, 60s cap, ±20% jitter, up to 5 attempts).

Usage:
    python3 modules/perc-i18n/scripts/i18n_translate_direct.py --target hi
    python3 modules/perc-i18n/scripts/i18n_translate_direct.py --target ar --docker
    python3 modules/perc-i18n/scripts/i18n_translate_direct.py --target ar --replace-existing
    python3 modules/perc-i18n/scripts/i18n_translate_direct.py --target hi --fix-matching-en
    python3 modules/perc-i18n/scripts/i18n_translate_direct.py --target hi-in --variant-base hi
"""
from __future__ import annotations

import argparse
import hashlib
import html
import json
import random
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path
from xml.sax.saxutils import escape as xml_escape

# Windows terminals often default to cp1252 / cp65001; force UTF-8 so Arabic
# and other non-Latin translations can be printed and written safely.
def _force_utf8_stdio() -> None:
    for stream in (sys.stdout, sys.stderr):
        enc = (getattr(stream, 'encoding', None) or '').lower().replace('-', '')
        if enc in ('utf8', 'cp65001'):
            continue
        try:
            stream.reconfigure(encoding='utf-8')  # type: ignore[attr-defined]
        except (AttributeError, OSError, ValueError):
            pass


_force_utf8_stdio()

# Module-level engine preference (set from CLI --docker).
_force_docker = False

REPO_ROOT = Path(__file__).resolve().parent.parent.parent.parent
I18N_DIR = REPO_ROOT / 'modules' / 'perc-i18n' / 'src' / 'main' / 'resources' / 'i18n'
CACHE_FILE = Path(__file__).resolve().parent / '.cache' / 'i18n_translate_direct.json'

DEFAULT_FILES = ('CmsUi.tmx', 'SystemResources.tmx', 'DeveloperUi.tmx')

PLACEHOLDER_RE = re.compile(r'^\s*\{[0-9]+(,[0-9]+)*\}\s*$')

SOURCE_LANG = 'en-us'

# Exponential-backoff parameters (shared contract with i18n_translate.py).
BACKOFF_START_SEC = 2.0
BACKOFF_MAX_SEC = 60.0
BACKOFF_JITTER = 0.2
BACKOFF_MAX_ATTEMPTS = 5

# Fixed wait before retrying non-rate-limit transient failures such as
# translate-shell returning a "Null response" / "Oops! Something went wrong"
# hiccup. Lets the upstream settle without spinning the throttle jitter.
TRANSIENT_RETRY_SEC = 10.0

# Docker fallback. soimort/translate-shell accepts the form
# `docker run --rm soimort/translate-shell --brief "<text>" :<target>`.
DOCKER_IMAGE = 'soimort/translate-shell'
DOCKER_BRIEF_FLAG = '--brief'

# Whether the trans -> Docker fallback notice has already been emitted.
_warned_trans_fallback = False

# Throttle new translations to avoid provider rate-limiting.
THROTTLE_MIN_SEC = 1.0
THROTTLE_MAX_SEC = 10.0

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


def _default_trans_cmd(text: str, target: str) -> list[str]:
    """Build the preferred local ``trans`` argv.

    ``-no-bidi`` is required for RTL targets (e.g. ``ar``): without it,
    translate-shell pads the line for terminal display and rewrites Arabic
    into presentation forms / visual order, which corrupts TMX storage and
    placeholders such as ``{0}``.
    """
    return ['trans', '--brief', '-no-bidi', f':{target}', text]


def _default_docker_cmd(text: str, target: str) -> list[str]:
    """Build the Docker translate-shell argv (same flags as local ``trans``).

    Forces UTF-8 inside the container so Windows Docker Desktop captures
    Arabic (and other non-Latin) stdout correctly.
    """
    return [
        'docker', 'run', '--rm',
        '-e', 'LANG=C.UTF-8',
        '-e', 'LC_ALL=C.UTF-8',
        DOCKER_IMAGE,
        DOCKER_BRIEF_FLAG, '-no-bidi', text, f':{target}',
    ]


def _normalize_engine_output(stdout: str, stderr: str) -> str:
    """Return the brief translation from subprocess streams.

    Strips BOM / whitespace. On some Windows Docker setups the brief result
    can land on stderr while stdout is empty; accept stderr when it does not
    look like an error banner.
    """
    out = (stdout or '').replace('\ufeff', '').strip()
    if out:
        return out
    err = (stderr or '').replace('\ufeff', '').strip()
    if not err:
        return ''
    lowered = err.lower()
    if any(tok in lowered for tok in (
        'error', 'warning', 'unable', 'failed', 'not found', 'denied',
    )):
        return ''
    # Single-line brief translation only (reject multi-line diagnostics).
    if '\n' in err:
        return ''
    return err


def _resolve_initial_cmd(text: str, target: str,
                         trans_cmd: list[str] | None) -> list[str]:
    """Pick local ``trans`` or Docker based on PATH and ``--docker``."""
    if trans_cmd is not None:
        return trans_cmd
    if _force_docker:
        if shutil.which('docker') is None:
            raise RuntimeError(
                '--docker was set but docker is not on PATH. '
                'Install Docker Desktop and ensure `docker` works in this shell.',
            )
        return _default_docker_cmd(text, target)
    if shutil.which('trans') is not None:
        return _default_trans_cmd(text, target)
    if shutil.which('docker') is not None:
        global _warned_trans_fallback
        if not _warned_trans_fallback:
            print(
                'trans not found on PATH; using Docker translate-shell '
                '(pass --docker to force this path)',
                file=sys.stderr,
                flush=True,
            )
            _warned_trans_fallback = True
        return _default_docker_cmd(text, target)
    raise RuntimeError(
        'trans (translate-shell) is not on PATH and Docker is unavailable. '
        'Install translate-shell (https://github.com/soimort/translate-shell) '
        'or install Docker and pull the soimort/translate-shell image.',
    )


def invoke_translate(text: str, target: str, *,
                     trans_cmd: list[str] | None = None) -> str:
    """Run translate-shell, preferring ``trans`` on PATH, falling back to Docker.

    Returns the translated string. Raises :class:`RuntimeError` if neither
    ``trans`` nor Docker is available, or if the translation could not be
    obtained after exhausting retries. Rate-limit responses (429 / "too many
    requests") trigger exponential backoff; successful calls do not sleep.
    """
    cmd = _resolve_initial_cmd(text, target, trans_cmd)
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
                errors='replace',
            )
        except FileNotFoundError as e:
            # Binary disappeared mid-run, or PATH race — try Docker once.
            if (trans_cmd is None and not _force_docker
                    and cmd and cmd[0] == 'trans'
                    and shutil.which('docker') is not None):
                global _warned_trans_fallback
                if not _warned_trans_fallback:
                    print(
                        'trans not found on PATH; falling back to Docker translate-shell',
                        file=sys.stderr,
                        flush=True,
                    )
                    _warned_trans_fallback = True
                cmd = _default_docker_cmd(text, target)
                attempt = 0
                delay = BACKOFF_START_SEC
                continue
            raise RuntimeError(
                'trans (translate-shell) is not on PATH and Docker is unavailable. '
                'Install translate-shell (https://github.com/soimort/translate-shell) '
                'or install Docker and pull the soimort/translate-shell image. '
                f'Underlying error: {e}',
            ) from e
        if result.returncode == 0:
            out = _normalize_engine_output(result.stdout, result.stderr)
            if out:
                return out
            raise RuntimeError(
                f'translate-shell returned empty translation '
                f'(rc={result.returncode}): stdout={result.stdout!r} '
                f'stderr={result.stderr!r}',
            )
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
                flush=True,
            )
            time.sleep(sleep_s)
            delay *= 2
            continue
        if attempt < BACKOFF_MAX_ATTEMPTS:
            jitter = 1.0 + random.uniform(-BACKOFF_JITTER, BACKOFF_JITTER)
            sleep_s = TRANSIENT_RETRY_SEC * jitter
            print(
                f'  translate-shell error (rc={result.returncode}); '
                f'retrying in {sleep_s:.1f}s (attempt {attempt})',
                file=sys.stderr,
            )
            time.sleep(sleep_s)
            delay = BACKOFF_START_SEC
            continue
        raise RuntimeError(
            f'translate-shell failed (rc={result.returncode}): '
            f'stdout={result.stdout!r} stderr={result.stderr!r}',
        )


def _preview(s: str, n: int = 50) -> str:
    """Single-line preview for progress logs."""
    one = s.replace('\n', ' ').replace('\r', ' ')
    return one if len(one) <= n else one[:n] + '…'


def translate(text: str, target: str, *, cache: dict[str, str] | None = None, force: bool = False) -> str:
    """Translate text to target, honoring cache."""
    if PLACEHOLDER_RE.match(text):
        return text
    if cache is None:
        cache = load_cache()
    key = cache_key(text, target)
    if not force and key in cache:
        cached = cache[key]
        print(
            f'  [cache] {_preview(text, 30)} -> {target} = {_preview(cached)}',
            flush=True,
        )
        return cached
    print(f'  [trans] {_preview(text, 30)} -> {target}', flush=True)
    translated = invoke_translate(text, target)
    print(
        f'  [trans] {_preview(text, 30)} -> {target} = {_preview(translated)}',
        flush=True,
    )
    cache[key] = translated
    throttle_s = random.uniform(THROTTLE_MIN_SEC, THROTTLE_MAX_SEC)
    print(f'  sleeping {throttle_s:.1f}s', file=sys.stderr, flush=True)
    time.sleep(throttle_s)
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

    def list_existing(self, target: str) -> list[tuple[str, str, str]]:
        """Return [(tuid, en_seg, target_seg)] for every <tu> that already has target.

        Used by ``--replace-existing`` to re-translate and overwrite polluted
        rows (e.g. pre ``-no-bidi`` Arabic presentation forms + padding).
        Includes empty or whitespace-only target segs.
        """
        root = self._parse()
        existing: list[tuple[str, str, str]] = []
        for tu in root.iter('tu'):
            tuid = tu.get('tuid') or ''
            if not tuid:
                continue
            en_seg = ''
            target_seg: str | None = None
            for tuv in tu.findall('tuv'):
                tlang = tuv.get('{http://www.w3.org/XML/1998/namespace}lang') or tuv.get('xml:lang')
                seg_el = tuv.find('seg')
                text = seg_el.text if seg_el is not None and seg_el.text else ''
                if tlang == SOURCE_LANG:
                    en_seg = text
                elif tlang == target:
                    target_seg = text  # may be '' for empty <seg></seg>
            if en_seg and target_seg is not None:
                existing.append((tuid, en_seg, target_seg))
        return existing

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
        """Insert missing <tuv> blocks. Returns number inserted.

        ``translations`` keys are logical tuids as returned by ElementTree
        (XML entities unescaped). Raw TMX attribute values may still contain
        ``&gt;`` / ``&lt;`` / ``&amp;`` — those are unescaped before lookup.
        """
        if not translations:
            return 0
        inserted = 0
        out = []
        pos = 0
        tu_pattern = re.compile(r'<tu\s+tuid="([^"]+)"[^>]*>.*?</tu>', re.DOTALL)
        for m in tu_pattern.finditer(self.text):
            # ElementTree / list_missing keys are entity-decoded; raw attrs are not.
            tuid = html.unescape(m.group(1))
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
        """Replace existing translations. Returns number replaced.

        See :meth:`inject` for tuid entity-decoding notes.
        """
        if not translations:
            return 0
        replaced = 0
        out = []
        pos = 0
        tu_pattern = re.compile(r'<tu\s+tuid="([^"]+)"[^>]*>.*?</tu>', re.DOTALL)
        for m in tu_pattern.finditer(self.text):
            tuid = html.unescape(m.group(1))
            if tuid not in translations:
                continue
            tu_text = m.group(0)
            # Allow optional sibling elements (e.g. <prop>) before <seg>.
            pattern = (
                rf'(<tuv\s+xml:lang="{re.escape(target)}"[^>]*>)'
                rf'(.*?)'
                rf'(<seg>)(.*?)(</seg>)'
            )

            def replacer(match, _tuid=tuid):
                nonlocal replaced
                replaced += 1
                seg = xml_escape(translations[_tuid])
                return f'{match.group(1)}{match.group(2)}{match.group(3)}{seg}{match.group(5)}'

            new_tu_text = re.sub(pattern, replacer, tu_text, count=1, flags=re.DOTALL)
            out.append(self.text[pos:m.start()])
            out.append(new_tu_text)
            pos = m.end()
        out.append(self.text[pos:])
        if replaced:
            self.text = ''.join(out)
        return replaced

    def commit(self):
        self.path.write_text(self.text, encoding='utf-8')


def _translate_batch(
    pairs: list[tuple[str, str]],
    target: str,
    *,
    cache: dict[str, str],
    force: bool,
    limit: int,
    label: str,
) -> dict[str, str] | None:
    """Translate ``(tuid, en_seg)`` pairs. Returns None on hard failure."""
    translations: dict[str, str] = {}
    processed = 0
    total = len(pairs)
    for tuid, en_seg in pairs:
        if limit and processed >= limit:
            print(f'  --limit={limit} reached for {label}', flush=True)
            break
        try:
            translations[tuid] = translate(
                en_seg, target, cache=cache, force=force,
            )
        except Exception as e:
            print(f'  ERROR on tuid={tuid!r}: {e}', file=sys.stderr, flush=True)
            save_cache(cache)
            return None
        processed += 1
        if processed % 10 == 0:
            print(f'  ... {processed}/{total} ({label})', flush=True)
            save_cache(cache)
    save_cache(cache)
    return translations


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target', required=True, help='Target BCP-47 locale code')
    parser.add_argument('--file', action='append', dest='files', help='TMX file to process')
    parser.add_argument('--dry-run', action='store_true', help='Report without translating')
    parser.add_argument('--force', action='store_true', help='Ignore cache')
    parser.add_argument(
        '--docker',
        action='store_true',
        help='Force Docker soimort/translate-shell (optional; local trans is fine)',
    )
    parser.add_argument(
        '--replace-existing',
        '--waste-another-6-hours-of-your-life',
        action='store_true',
        help=(
            'Re-translate and overwrite every existing target <tuv> from en-us '
            '(always bypasses cache). Use to repair polluted ar from pre -no-bidi '
            'runs. Alias: --waste-another-6-hours-of-your-life'
        ),
    )
    parser.add_argument('--fix-matching-en', action='store_true', help='Fix translations matching English')
    parser.add_argument('--variant-base', help='Base locale for variant (e.g., hi for hi-in)')
    parser.add_argument('--limit', type=int, default=0, help='Max translations per phase (0=unlimited)')
    args = parser.parse_args(argv)

    global _force_docker
    _force_docker = bool(args.docker)

    files = []
    for name in (args.files or list(DEFAULT_FILES)):
        candidate = Path(name)
        files.append(I18N_DIR / candidate if not candidate.is_absolute() else candidate)

    target = args.target
    base_lang = args.variant_base or VARIANT_BASES.get(target)

    cache = load_cache()
    total_missing = 0
    total_inserted = 0
    total_replaced = 0
    total_fixed = 0
    total_skipped = 0

    for f in files:
        if not f.exists():
            print(f'error: {f} not found', file=sys.stderr)
            return 2

        tmx = TmxFile(f)

        # Step 1: Handle missing translations
        missing = tmx.list_missing(target)
        total_missing += len(missing)
        print(f'{f.name}: {len(missing)} missing <tuv xml:lang="{target}">', flush=True)

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
                    print(f'  SKIP {tuid}: {e}', file=sys.stderr)
                    total_skipped += 1
                    save_cache(cache)
                    continue
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
                    print(
                        f'  variant filter: {len(translations)} -> {len(filtered)} '
                        f'(only differences)',
                        flush=True,
                    )
                    translations = filtered

                inserted = tmx.inject(target, translations)
                if not args.dry_run:
                    tmx.commit()
                total_inserted += inserted
                print(f'  inserted {inserted} TUVs', flush=True)

        # Step 2: Overwrite every existing target TUV (full repair path)
        if args.replace_existing:
            # Re-read after possible inject so we see newly written rows too
            # only when we want to also re-hit them — skip: inject already
            # wrote fresh text. list_existing on current tmx is correct for
            # pre-existing polluted rows.
            existing = tmx.list_existing(target)
            print(
                f'{f.name}: {len(existing)} existing <tuv xml:lang="{target}"> '
                f'to re-translate',
                flush=True,
            )
            if args.dry_run or not existing:
                pass
            else:
                pairs = [(tuid, en_seg) for tuid, en_seg, _ in existing]
                # Always bypass cache — that is the whole point of this flag.
                translations = _translate_batch(
                    pairs,
                    target,
                    cache=cache,
                    force=True,
                    limit=args.limit,
                    label='replace-existing',
                )
                if translations is None:
                    return 1
                if translations:
                    if base_lang and base_lang != target:
                        base_trans = tmx.get_base_translations(base_lang)
                        filtered = {
                            k: v for k, v in translations.items()
                            if k not in base_trans or base_trans[k] != v
                        }
                        print(
                            f'  variant filter: {len(translations)} -> '
                            f'{len(filtered)} (only differences)',
                            flush=True,
                        )
                        translations = filtered
                    replaced = tmx.replace_translation(target, translations)
                    if not args.dry_run:
                        tmx.commit()
                    total_replaced += replaced
                    print(f'  replaced {replaced} TUVs', flush=True)

        # Step 3: Fix translations matching English (only for base locales)
        if args.fix_matching_en and not base_lang:
            matching = tmx.list_matching_en(target)
            total_fixed += len(matching)
            print(f'{f.name}: {len(matching)} match English, fixing...', flush=True)

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
                        print(f'  SKIP {tuid}: {e}', file=sys.stderr)
                        save_cache(cache)
                        total_skipped += 1
                        continue
                    processed += 1
                    if processed % 10 == 0:
                        print(f'  ... {processed}/{len(matching)}')
                        save_cache(cache)
                save_cache(cache)

                if translations:
                    replaced = tmx.replace_translation(target, translations)
                    if not args.dry_run:
                        tmx.commit()
                    total_fixed = total_fixed  # count already from list size
                    print(f'  fixed {replaced} translations', flush=True)

    print(f'\nDone. Missing: {total_missing}; inserted: {total_inserted}; fixed: {total_fixed}; skipped: {total_skipped}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
