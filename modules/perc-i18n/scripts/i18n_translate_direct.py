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
from dataclasses import dataclass
import html
import random
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path
from xml.sax.saxutils import escape as xml_escape

# Sibling modules live in the same directory (dev tool, not a package).
_SCRIPTS_DIR = Path(__file__).resolve().parent
if str(_SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS_DIR))

import i18n_cache as _i18n_cache  # noqa: E402
import i18n_glossary as _i18n_glossary  # noqa: E402

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
# Shared checked-in cache with i18n_translate.py (scripts/cache/i18n_translate.json).
# Reassignable by unit tests so they never touch the committed file.
CACHE_FILE = _i18n_cache.CACHE_FILE

DEFAULT_FILES = ('CmsUi.tmx', 'SystemResources.tmx', 'DeveloperUi.tmx')

PLACEHOLDER_RE = re.compile(r'^\s*\{[0-9]+(,[0-9]+)*\}\s*$')

# URL-only segments (e.g. help-doc hrefs). translate-shell often hangs or
# rewrites these into google-translate wrapper URLs; keep them identity.
URL_ONLY_RE = re.compile(r'^\s*https?://\S+\s*$', re.IGNORECASE)

SOURCE_LANG = 'en-us'


def is_identity_segment(text: str) -> bool:
    """True when the source segment must pass through untranslated.

    Covers placeholder-only keys (``{0}``) and bare URL keys. Mixed prose
    that merely *contains* a URL is translated normally.
    """
    return bool(PLACEHOLDER_RE.match(text) or URL_ONLY_RE.match(text))

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
    """Stable hash for a (text, target) translation request."""
    h = hashlib.sha256()
    h.update(target.encode('utf-8'))
    h.update(b'\x00')
    h.update(text.encode('utf-8'))
    return h.hexdigest()


def load_cache() -> dict[str, str]:
    """Load the shared translation cache (migrates legacy ``.cache/`` once)."""
    # Resolve through this module's CACHE_FILE so tests can redirect it.
    return _i18n_cache.load_cache(CACHE_FILE)


def save_cache(cache: dict[str, str]) -> None:
    """Persist the shared translation cache (atomic write)."""
    _i18n_cache.save_cache(cache, CACHE_FILE)


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
        _note_trans_fallback(
            'trans not found on PATH; using Docker translate-shell '
            '(pass --docker to force this path)',
        )
        return _default_docker_cmd(text, target)
    raise RuntimeError(
        'trans (translate-shell) is not on PATH and Docker is unavailable. '
        'Install translate-shell (https://github.com/soimort/translate-shell) '
        'or install Docker and pull the soimort/translate-shell image.',
    )


def _note_trans_fallback(message: str) -> None:
    """Print a trans -> Docker fallback notice once per process."""
    global _warned_trans_fallback
    if not _warned_trans_fallback:
        print(message, file=sys.stderr, flush=True)
        _warned_trans_fallback = True


def _run_engine(cmd: list[str]):
    """Run one translate-shell invocation (thin wrapper for test seams)."""
    return subprocess.run(
        cmd,
        capture_output=True,
        text=True,
        check=False,
        encoding='utf-8',
        errors='replace',
    )


def _engine_unavailable_error(cause: Exception) -> RuntimeError:
    """Error raised when neither ``trans`` nor Docker can be executed."""
    return RuntimeError(
        'trans (translate-shell) is not on PATH and Docker is unavailable. '
        'Install translate-shell (https://github.com/soimort/translate-shell) '
        'or install Docker and pull the soimort/translate-shell image. '
        f'Underlying error: {cause}',
    )


def _maybe_switch_to_docker(cmd: list[str], text: str, target: str,
                            trans_cmd: list[str] | None) -> list[str] | None:
    """Return the Docker argv when the ``trans`` binary vanished mid-run.

    Returns None when the Docker fallback does not apply (explicit
    ``trans_cmd`` override, ``--docker`` mode, or no Docker on PATH).
    """
    if (trans_cmd is None and not _force_docker
            and cmd and cmd[0] == 'trans'
            and shutil.which('docker') is not None):
        _note_trans_fallback(
            'trans not found on PATH; falling back to Docker translate-shell',
        )
        return _default_docker_cmd(text, target)
    return None


def _jittered(base: float) -> float:
    """Apply the shared ±20% backoff jitter to a base sleep."""
    return base * (1.0 + random.uniform(-BACKOFF_JITTER, BACKOFF_JITTER))


def _retry_after_failure(result, attempt: int, delay: float) -> tuple[float, float]:
    """Return (sleep_s, next_delay) or raise when retries are exhausted.

    Rate-limit responses back off exponentially; other transient failures
    use the fixed settle wait and reset the exponential delay.
    """
    if attempt >= BACKOFF_MAX_ATTEMPTS:
        raise RuntimeError(
            f'translate-shell failed (rc={result.returncode}): '
            f'stdout={result.stdout!r} stderr={result.stderr!r}',
        )
    lowered = (result.stderr + result.stdout).lower()
    if '429' in lowered or 'rate limit' in lowered or 'too many requests' in lowered:
        sleep_s = _jittered(min(BACKOFF_MAX_SEC, delay))
        print(
            f'  rate limit; sleeping {sleep_s:.1f}s (attempt {attempt})',
            file=sys.stderr,
            flush=True,
        )
        return sleep_s, delay * 2
    sleep_s = _jittered(TRANSIENT_RETRY_SEC)
    print(
        f'  translate-shell error (rc={result.returncode}); '
        f'retrying in {sleep_s:.1f}s (attempt {attempt})',
        file=sys.stderr,
    )
    return sleep_s, BACKOFF_START_SEC


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
            result = _run_engine(cmd)
        except FileNotFoundError as e:
            # Binary disappeared mid-run, or PATH race — try Docker once.
            switched = _maybe_switch_to_docker(cmd, text, target, trans_cmd)
            if switched is None:
                raise _engine_unavailable_error(e) from e
            cmd = switched
            attempt = 0
            delay = BACKOFF_START_SEC
            continue
        if result.returncode == 0:
            out = _normalize_engine_output(result.stdout, result.stderr)
            if out:
                return out
            raise RuntimeError(
                f'translate-shell returned empty translation '
                f'(rc={result.returncode}): stdout={result.stdout!r} '
                f'stderr={result.stderr!r}',
            )
        sleep_s, delay = _retry_after_failure(result, attempt, delay)
        time.sleep(sleep_s)


def _preview(s: str, n: int = 50) -> str:
    """Single-line preview for progress logs."""
    one = s.replace('\n', ' ').replace('\r', ' ')
    return one if len(one) <= n else one[:n] + '…'


def translate(text: str, target: str, *, cache: dict[str, str] | None = None, force: bool = False) -> str:
    """Translate text to target, honoring cache."""
    if is_identity_segment(text):
        return text
    # Pinned product terms (issue #7131) win over cache and engine alike so
    # a stale cached machine translation can never regress a reviewed term.
    pinned = _i18n_glossary.lookup(text, target)
    if pinned is not None:
        if cache is None:
            cache = load_cache()
        key = cache_key(text, target)
        if cache.get(key) != pinned:
            cache[key] = pinned
        print(
            f'  [glossary] {_preview(text, 30)} -> {target} = {_preview(pinned)}',
            flush=True,
        )
        return pinned
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


def _tuv_lang(tuv) -> str | None:
    """Language tag of a <tuv>, accepting namespaced and plain attributes."""
    return tuv.get('{http://www.w3.org/XML/1998/namespace}lang') or tuv.get('xml:lang')


def _seg_text(tuv) -> str:
    """Text of the <tuv>'s <seg>, or '' when missing/empty."""
    seg_el = tuv.find('seg')
    return seg_el.text if seg_el is not None and seg_el.text else ''


def _iter_tus(root):
    """Yield (tuid, tu) pairs, skipping entries without a tuid."""
    for tu in root.iter('tu'):
        tuid = tu.get('tuid') or ''
        if tuid:
            yield tuid, tu


def _tu_segments(tu):
    """Yield (lang, seg_text) for each <tuv> child of a <tu>."""
    for tuv in tu.findall('tuv'):
        yield _tuv_lang(tuv), _seg_text(tuv)


def _scan_tu_missing(tu, target: str) -> tuple[str, bool]:
    """Return (en_seg, has_target) for one <tu>."""
    en_seg = ''
    for tlang, text in _tu_segments(tu):
        if tlang == target:
            return en_seg, True
        if tlang == SOURCE_LANG and text:
            en_seg = text
    return en_seg, False


class TmxFile:
    def __init__(self, path: Path):
        self.path = path
        self.text = path.read_text(encoding='utf-8')

    def _parse(self):
        import xml.etree.ElementTree as ET
        return ET.fromstring(self.text)

    def list_missing(self, target: str) -> list[tuple[str, str]]:
        """Return [(tuid, en_seg)] for every <tu> missing target lang."""
        missing = []
        for tuid, tu in _iter_tus(self._parse()):
            en_seg, has_target = _scan_tu_missing(tu, target)
            if not has_target and en_seg:
                missing.append((tuid, en_seg))
        return missing

    def list_matching_en(self, target: str) -> list[tuple[str, str, str]]:
        """Return [(tuid, en_seg, target_seg)] for where target matches en-us."""
        matching = []
        for tuid, tu in _iter_tus(self._parse()):
            en_seg = ''
            target_seg = ''
            for tlang, text in _tu_segments(tu):
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
        existing: list[tuple[str, str, str]] = []
        for tuid, tu in _iter_tus(self._parse()):
            en_seg = ''
            target_seg: str | None = None
            for tlang, text in _tu_segments(tu):
                if tlang == SOURCE_LANG:
                    en_seg = text
                elif tlang == target:
                    target_seg = text  # may be '' for empty <seg></seg>
            if en_seg and target_seg is not None:
                existing.append((tuid, en_seg, target_seg))
        return existing

    def get_base_translations(self, base_lang: str) -> dict[str, str]:
        """Get all translations for base_lang: {tuid: seg_text}."""
        translations = {}
        for tuid, tu in _iter_tus(self._parse()):
            for tlang, text in _tu_segments(tu):
                if tlang == base_lang:
                    if text:
                        translations[tuid] = text
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
        # Tuids containing a double quote are single-quoted in raw TMX
        # (e.g. tuid='...Delete section "{0}"?...'); accept both styles.
        tu_pattern = re.compile(
            r"<tu\s+tuid=(?:\"([^\"]+)\"|'([^']+)')[^>]*>.*?</tu>", re.DOTALL
        )
        for m in tu_pattern.finditer(self.text):
            # ElementTree / list_missing keys are entity-decoded; raw attrs are not.
            raw = m.group(1) if m.group(1) is not None else m.group(2)
            tuid = html.unescape(raw)
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
        # Accept single-quoted tuids (see inject).
        tu_pattern = re.compile(
            r"<tu\s+tuid=(?:\"([^\"]+)\"|'([^']+)')[^>]*>.*?</tu>", re.DOTALL
        )
        for m in tu_pattern.finditer(self.text):
            raw = m.group(1) if m.group(1) is not None else m.group(2)
            tuid = html.unescape(raw)
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


@dataclass
class _RunOptions:
    """Per-run CLI state shared by the three processing steps."""
    target: str
    base_lang: str | None = None
    force: bool = False
    limit: int = 0
    dry_run: bool = False
    replace_existing: bool = False
    fix_matching_en: bool = False


@dataclass
class _RunTotals:
    """Counters reported in the final ``Done.`` summary line."""
    missing: int = 0
    inserted: int = 0
    replaced: int = 0
    fixed: int = 0
    skipped: int = 0


def _build_parser() -> argparse.ArgumentParser:
    """Build the CLI parser (extracted so ``main`` stays flat)."""
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
    return parser


def _resolve_run_files(files_arg: list[str] | None) -> list[Path]:
    """Resolve CLI ``--file`` names against the canonical i18n directory."""
    files = []
    for name in (files_arg or list(DEFAULT_FILES)):
        candidate = Path(name)
        files.append(I18N_DIR / candidate if not candidate.is_absolute() else candidate)
    return files


def _maybe_filter_variant(translations: dict[str, str], tmx: TmxFile,
                          target: str, base_lang: str | None) -> dict[str, str]:
    """Drop translations identical to the base locale (variant locales only)."""
    if not base_lang or base_lang == target:
        return translations
    base_trans = tmx.get_base_translations(base_lang)
    filtered = {k: v for k, v in translations.items()
                if k not in base_trans or base_trans[k] != v}
    print(
        f'  variant filter: {len(translations)} -> {len(filtered)} '
        f'(only differences)',
        flush=True,
    )
    return filtered


def _translate_missing_lenient(pairs: list[tuple[str, str]], *, target: str,
                               cache: dict[str, str], force: bool, limit: int,
                               ) -> tuple[dict[str, str], int]:
    """Translate ``(tuid, en_seg)`` pairs, skipping per-key failures.

    Returns (translations, skipped). Unlike :func:`_translate_batch` (which
    aborts the run with None on a hard failure), one bad key must not kill
    a multi-hour run.
    """
    translations: dict[str, str] = {}
    skipped = 0
    processed = 0
    total = len(pairs)
    for tuid, en_seg in pairs:
        if limit and processed >= limit:
            break
        try:
            translations[tuid] = translate(en_seg, target, cache=cache, force=force)
        except Exception as e:
            print(f'  SKIP {tuid}: {e}', file=sys.stderr)
            save_cache(cache)
            skipped += 1
            continue
        processed += 1
        if processed % 10 == 0:
            print(f'  ... {processed}/{total}')
            save_cache(cache)
    save_cache(cache)
    return translations, skipped


def _process_missing(tmx: TmxFile, options: _RunOptions,
                     totals: _RunTotals, cache: dict[str, str]) -> None:
    """Step 1: translate every <tu> missing the target TUV."""
    missing = tmx.list_missing(options.target)
    totals.missing += len(missing)
    print(f'{tmx.path.name}: {len(missing)} missing <tuv xml:lang="{options.target}">', flush=True)
    if options.dry_run or not missing:
        return
    translations, skipped = _translate_missing_lenient(
        missing, target=options.target, cache=cache,
        force=options.force, limit=options.limit,
    )
    totals.skipped += skipped
    if not translations:
        return
    translations = _maybe_filter_variant(translations, tmx, options.target, options.base_lang)
    inserted = tmx.inject(options.target, translations)
    if not options.dry_run:
        tmx.commit()
    totals.inserted += inserted
    print(f'  inserted {inserted} TUVs', flush=True)


def _process_replace_existing(tmx: TmxFile, options: _RunOptions,
                              totals: _RunTotals, cache: dict[str, str]) -> bool:
    """Step 2: re-translate every existing target TUV. False = abort the run."""
    if not options.replace_existing:
        return True
    # Re-read after a possible inject so we see freshly written rows too;
    # inject already wrote fresh text, so list_existing on current tmx is
    # correct for pre-existing polluted rows.
    existing = tmx.list_existing(options.target)
    print(
        f'{tmx.path.name}: {len(existing)} existing <tuv xml:lang="{options.target}"> '
        f'to re-translate',
        flush=True,
    )
    if options.dry_run or not existing:
        return True
    pairs = [(tuid, en_seg) for tuid, en_seg, _ in existing]
    # Always bypass cache — that is the whole point of this flag.
    translations = _translate_batch(
        pairs,
        options.target,
        cache=cache,
        force=True,
        limit=options.limit,
        label='replace-existing',
    )
    if translations is None:
        return False
    if not translations:
        return True
    translations = _maybe_filter_variant(translations, tmx, options.target, options.base_lang)
    replaced = tmx.replace_translation(options.target, translations)
    if not options.dry_run:
        tmx.commit()
    totals.replaced += replaced
    print(f'  replaced {replaced} TUVs', flush=True)
    return True


def _process_fix_matching_en(tmx: TmxFile, options: _RunOptions,
                             totals: _RunTotals, cache: dict[str, str]) -> None:
    """Step 3: re-translate target segs identical to English (base locales)."""
    if not options.fix_matching_en or options.base_lang:
        return
    matching = tmx.list_matching_en(options.target)
    totals.fixed += len(matching)
    print(f'{tmx.path.name}: {len(matching)} match English, fixing...', flush=True)
    if options.dry_run or not matching:
        return
    pairs = [(tuid, en_seg) for tuid, en_seg, _ in matching]
    translations, skipped = _translate_missing_lenient(
        pairs, target=options.target, cache=cache,
        force=options.force, limit=options.limit,
    )
    totals.skipped += skipped
    if not translations:
        return
    replaced = tmx.replace_translation(options.target, translations)
    if not options.dry_run:
        tmx.commit()
    print(f'  fixed {replaced} translations', flush=True)


def main(argv: list[str] | None = None) -> int:
    args = _build_parser().parse_args(argv)

    global _force_docker
    _force_docker = bool(args.docker)

    files = _resolve_run_files(args.files)
    options = _RunOptions(
        target=args.target,
        base_lang=args.variant_base or VARIANT_BASES.get(args.target),
        force=args.force,
        limit=args.limit,
        dry_run=args.dry_run,
        replace_existing=args.replace_existing,
        fix_matching_en=args.fix_matching_en,
    )
    cache = load_cache()
    totals = _RunTotals()

    for f in files:
        if not f.exists():
            print(f'error: {f} not found', file=sys.stderr)
            return 2
        tmx = TmxFile(f)
        _process_missing(tmx, options, totals, cache)
        if not _process_replace_existing(tmx, options, totals, cache):
            return 1
        _process_fix_matching_en(tmx, options, totals, cache)

    print(f'\nDone. Missing: {totals.missing}; inserted: {totals.inserted}; fixed: {totals.fixed}; skipped: {totals.skipped}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
