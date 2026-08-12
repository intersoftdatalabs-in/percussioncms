#!/usr/bin/env python3
"""Fill missing translations in the perc-i18n canonical TMX files.

Walks ``modules/perc-i18n/src/main/resources/i18n/{CmsUi,SystemResources}.tmx``
and, for every ``<tuv xml:lang="<target>">`` that is absent on a ``<tu>``,
shells out to ``docker run --rm soimort/translate-shell --brief ...`` to
fetch a translation. Honors rate-limit responses with exponential backoff,
caches results in the shared checked-in file
``scripts/cache/i18n_translate.json`` (via ``i18n_cache``) so re-runs
resume across machines, skips placeholder-only source segments, and
writes the new ``<tuv>`` back into the canonical TMX with proper XML
escaping.

Usage (from repository root)::

    python3 modules/perc-i18n/scripts/i18n_translate.py --target de-de
    python3 modules/perc-i18n/scripts/i18n_translate.py --target ja-jp --file CmsUi.tmx --dry-run
    python3 modules/perc-i18n/scripts/i18n_translate.py --target tr-tr --force

The script is a developer tool, not a Maven build gate. It requires Docker
(``soimort/translate-shell`` image) on PATH; missing Docker is a fail-loud
condition with a printable install hint. The unit tests in
``test_i18n_translate.py`` exercise the cache, backoff, placeholder skip,
and XML-escape paths without needing Docker.

Cross-platform: paths use :mod:`pathlib` exclusively; no ``os.path`` joins
or hardcoded separators. Line endings are written verbatim from the
parser's text content; no transformations.
"""
from __future__ import annotations

import argparse
import hashlib
import html
import random
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path
from xml.etree import ElementTree as ET
from xml.sax.saxutils import escape as xml_escape

# Sibling modules live in the same directory (dev tool, not a package).
_SCRIPTS_DIR = Path(__file__).resolve().parent
if str(_SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS_DIR))

import i18n_cache as _i18n_cache  # noqa: E402

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

REPO_ROOT = Path(__file__).resolve().parent.parent.parent.parent
I18N_DIR = REPO_ROOT / 'modules' / 'perc-i18n' / 'src' / 'main' / 'resources' / 'i18n'
# Shared checked-in cache (scripts/cache/i18n_translate.json). Reassignable
# by unit tests so they never touch the committed file.
CACHE_FILE = _i18n_cache.CACHE_FILE

# Canonical TMX files this script edits.
DEFAULT_FILES = ('CmsUi.tmx', 'SystemResources.tmx', 'DeveloperUi.tmx')

# Placeholder-only segment pattern (e.g. '{0}', '{1,2,3}'). Matches the
# existing translate_tmx.py rule from update_tmx_limited.py:40.
PLACEHOLDER_RE = re.compile(r'^\s*\{[0-9]+(,[0-9]+)*\}\s*$')

# URL-only segments (e.g. help-doc hrefs). translate-shell often hangs or
# rewrites these into google-translate wrapper URLs; keep them identity.
URL_ONLY_RE = re.compile(r'^\s*https?://\S+\s*$', re.IGNORECASE)


def is_identity_segment(text: str) -> bool:
    """True when the source segment must pass through untranslated.

    Covers placeholder-only keys (``{0}``) and bare URL keys. Mixed prose
    that merely *contains* a URL is translated normally.
    """
    return bool(PLACEHOLDER_RE.match(text) or URL_ONLY_RE.match(text))


# Exponential-backoff parameters.
BACKOFF_START_SEC = 2.0
BACKOFF_MAX_SEC = 60.0
BACKOFF_JITTER = 0.2

# Docker invocation. soimort/translate-shell accepts the form
# `docker run --rm soimort/translate-shell --brief "<text>" :<target>`.
DOCKER_IMAGE = 'soimort/translate-shell'
DOCKER_BRIEF_FLAG = '--brief'

# Languages whose source string is already non-English and shouldn't be
# retranslated. We always translate FROM English to the target lang.
SOURCE_LANG = 'en-us'


# ---------------------------------------------------------------------------
# Cache helpers (shared with i18n_translate_direct via i18n_cache)
# ---------------------------------------------------------------------------

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


# ---------------------------------------------------------------------------
# Translation invocation
# ---------------------------------------------------------------------------

def invoke_translate(text: str, target: str, *,
                     docker_cmd: list[str] | None = None) -> str:
    """Run soimort/translate-shell on Docker. Returns the translated string.

    Raises :class:`RuntimeError` if Docker is unavailable or the translation
    could not be obtained after exhausting retries. Rate-limit responses
    trigger exponential backoff per ``backoff_sleep``.
    """
    # ``-no-bidi`` keeps RTL languages (e.g. ar) in logical order without
    # terminal padding / presentation-form rewriting — required for TMX.
    # LANG/LC_ALL help Windows Docker Desktop capture UTF-8 Arabic stdout.
    cmd = docker_cmd if docker_cmd is not None else [
        'docker', 'run', '--rm',
        '-e', 'LANG=C.UTF-8',
        '-e', 'LC_ALL=C.UTF-8',
        DOCKER_IMAGE,
        DOCKER_BRIEF_FLAG, '-no-bidi', text, f':{target}',
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
                errors='replace',
            )
        except FileNotFoundError as e:
            raise RuntimeError(
                'Docker is not on PATH. Install Docker Desktop (Windows/macOS) '
                'or docker-engine (Linux), then pull the soimort/translate-shell '
                f'image. Underlying error: {e}',
            ) from e
        if result.returncode == 0:
            out = (result.stdout or '').replace('\ufeff', '').strip()
            if not out:
                err = (result.stderr or '').replace('\ufeff', '').strip()
                if err and '\n' not in err and 'error' not in err.lower():
                    out = err
            if out:
                return out
            raise RuntimeError(
                f'translate-shell returned empty translation '
                f'(rc={result.returncode}): stdout={result.stdout!r} '
                f'stderr={result.stderr!r}',
            )
        # Crude rate-limit detection: 429-like substrings in stderr or stdout.
        lowered = (result.stderr + result.stdout).lower()
        is_rate_limit = (
            '429' in lowered
            or 'rate limit' in lowered
            or 'too many requests' in lowered
        )
        if is_rate_limit and attempt < 5:
            jitter = 1.0 + random.uniform(-BACKOFF_JITTER, BACKOFF_JITTER)
            sleep_s = min(BACKOFF_MAX_SEC, delay) * jitter
            print(f'  rate limit; sleeping {sleep_s:.1f}s (attempt {attempt})',
                  file=sys.stderr)
            time.sleep(sleep_s)
            delay *= 2
            continue
        raise RuntimeError(
            f'translate-shell failed (rc={result.returncode}): '
            f'stdout={result.stdout!r} stderr={result.stderr!r}',
        )


# ---------------------------------------------------------------------------
# Translation request (cached)
# ---------------------------------------------------------------------------

def _preview(s: str, n: int = 50) -> str:
    one = s.replace('\n', ' ').replace('\r', ' ')
    return one if len(one) <= n else one[:n] + '…'


def translate(text: str, target: str, *,
              cache: dict[str, str] | None = None,
              force: bool = False) -> str:
    """Translate ``text`` to ``target``, honoring the on-disk cache."""
    if is_identity_segment(text):
        return text  # placeholders and bare URLs pass through
    if cache is None:
        cache = load_cache()
    key = cache_key(text, target)
    if not force and key in cache:
        cached = cache[key]
        print(f'  [cache] {_preview(text, 30)} -> {target} = {_preview(cached)}',
              flush=True)
        return cached
    print(f'  [trans] {_preview(text, 30)} -> {target}', flush=True)
    translated = invoke_translate(text, target)
    print(f'  [trans] {_preview(text, 30)} -> {target} = {_preview(translated)}',
          flush=True)
    cache[key] = translated
    return translated


# ---------------------------------------------------------------------------
# TMX editing
# ---------------------------------------------------------------------------

class TmxFile:
    """Lightweight TMX editor that preserves existing formatting."""

    def __init__(self, path: Path):
        self.path = path
        self.text = path.read_text(encoding='utf-8')

    def _parse(self) -> ET.Element:
        return ET.fromstring(self.text)

    def list_missing(self, target: str) -> list[tuple[str, str]]:
        """Return [(tuid, en_seg)] for every <tu> that has no <tuv xml:lang=target>."""
        root = self._parse()
        missing: list[tuple[str, str]] = []
        for tu in root.iter('tu'):
            tuid = tu.get('tuid') or ''
            if not tuid:
                continue
            en_seg = ''
            has_target = False
            for tuv in tu.findall('tuv'):
                tlang = tuv.get('{http://www.w3.org/XML/1998/namespace}lang') \
                    or tuv.get('xml:lang')
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

    def inject(self, target: str, translations: dict[str, str]) -> int:
        """Insert ``<tuv xml:lang="target"><seg>...</seg></tuv>`` blocks for
        every tuid in ``translations`` that is missing that lang. Returns the
        number of inserted TUVs.

        ``translations`` keys must match ElementTree-decoded tuids. Raw TMX
        attribute values may still contain ``&gt;`` / ``&lt;`` / ``&amp;``;
        those are unescaped before lookup so keys with ``>>``, ``&``, etc.
        actually inject.
        """
        if not translations:
            return 0
        # Build a regex that matches each <tu ...>...</tu> we care about.
        inserted = 0
        out: list[str] = []
        pos = 0
        tu_pattern = re.compile(r'<tu\s+tuid="([^"]+)"[^>]*>.*?</tu>', re.DOTALL)
        for m in tu_pattern.finditer(self.text):
            # list_missing() returns entity-decoded tuids; raw attrs do not.
            tuid = html.unescape(m.group(1))
            if tuid not in translations:
                continue
            # Check if this TU already has the target lang (skip).
            tu_text = m.group(0)
            if re.search(rf'<tuv\s+xml:lang="{re.escape(target)}"', tu_text):
                continue
            # Build the new TUV. Escape XML element-content characters in seg
            # text only (no `"` -> `&quot;` mapping here: double quotes never
            # need escaping in element content, and pre-escaping breaks
            # round-trip fidelity for translations that contain `&quot;`).
            seg = xml_escape(translations[tuid])
            new_tuv = f'<tuv xml:lang="{xml_escape(target)}"><seg>{seg}</seg></tuv>'
            # Insert before </tu>.
            replacement = tu_text[:-len('</tu>')] + new_tuv + '</tu>'
            out.append(self.text[pos:m.start()])
            out.append(replacement)
            pos = m.end()
            inserted += 1
        out.append(self.text[pos:])
        if inserted:
            self.text = ''.join(out)
        return inserted

    def commit(self) -> None:
        self.path.write_text(self.text, encoding='utf-8')


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--target', required=True,
                        help='Target BCP-47 locale code, e.g. de-de, ja-jp, tr-tr.')
    parser.add_argument('--file', action='append', dest='files',
                        help='Restrict to a single TMX file (repeatable). '
                             f'Default: {", ".join(DEFAULT_FILES)}.')
    parser.add_argument('--dry-run', action='store_true',
                        help='Report missing TUVs without calling translate-shell.')
    parser.add_argument('--force', action='store_true',
                        help='Ignore the on-disk cache and refetch every translation.')
    parser.add_argument('--limit', type=int, default=0,
                        help='Stop after processing this many missing TUVs (0 = unlimited).')
    parser.add_argument('--no-docker-check', action='store_true',
                        help='Skip the upfront Docker availability check.')
    args = parser.parse_args(argv)

    files: list[Path] = []
    for name in (args.files or list(DEFAULT_FILES)):
        candidate = Path(name)
        if candidate.is_absolute():
            print(f'error: --file path must be relative to {I18N_DIR}: {name}',
                  file=sys.stderr)
            return 2
        files.append(I18N_DIR / candidate)
    for f in files:
        if not f.exists():
            print(f'error: {f} not found', file=sys.stderr)
            return 2

    if not args.no_docker_check and not args.dry_run:
        if shutil.which('docker') is None:
            print('error: docker is not on PATH. Install Docker and run:\n'
                  '  docker pull soimort/translate-shell',
                  file=sys.stderr)
            return 2

    cache = load_cache()
    total_missing = 0
    total_inserted = 0
    for f in files:
        tmx = TmxFile(f)
        missing = tmx.list_missing(args.target)
        total_missing += len(missing)
        print(f'{f.name}: {len(missing)} <tu> missing <tuv xml:lang="{args.target}">')
        if args.dry_run or not missing:
            continue
        translations: dict[str, str] = {}
        processed = 0
        for tuid, en_seg in missing:
            if args.limit and processed >= args.limit:
                print(f'  --limit={args.limit} reached, stopping')
                break
            try:
                translations[tuid] = translate(en_seg, args.target,
                                               cache=cache, force=args.force)
            except RuntimeError as e:
                print(f'  ERROR on tuid={tuid!r}: {e}', file=sys.stderr)
                save_cache(cache)  # persist progress
                return 1
            processed += 1
            if processed % 25 == 0:
                print(f'  ... {processed}/{len(missing)} translated')
                save_cache(cache)
        save_cache(cache)
        if translations:
            inserted = tmx.inject(args.target, translations)
            if not args.dry_run:
                tmx.commit()
            total_inserted += inserted
            print(f'  inserted {inserted} TUVs into {f.name}')

    print(f'\nDone. Total missing: {total_missing}; inserted: {total_inserted}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
