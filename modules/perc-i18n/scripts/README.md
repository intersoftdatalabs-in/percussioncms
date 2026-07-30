# i18n_translate.py — Developer Tooling for the perc-i18n Module

This directory contains cross-platform Python tooling that automates the
back-fill of missing translations in the canonical TMX files. It replaces
the legacy per-locale sibling `*.tmx` files and the previous
hand-maintained Python/shell scripts.

## Files

|              File               |                                                                Purpose                                                                 |
|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `i18n_translate.py`             | CLI: walks canonical TMX files, fills missing `<tuv>` blocks via **Docker** `soimort/translate-shell`.                                 |
| `i18n_translate_direct.py`      | Same job as `i18n_translate.py`, but prefers **`trans` on PATH** (translate-shell) and falls back to Docker if `trans` is unavailable. |
| `test_i18n_translate.py`        | Unit tests for the Docker variant (no Docker required).                                                                                |
| `test_i18n_translate_direct.py` | Unit tests for the direct `trans` variant (no `trans` required).                                                                       |

## Quick start

```bash
# Translate every <tu> that lacks a German <tuv> in CmsUi.tmx +
# SystemResources.tmx, using the on-disk cache.
python3 modules/perc-i18n/scripts/i18n_translate.py --target de-de

# Just see what's missing without contacting the translation service.
python3 modules/perc-i18n/scripts/i18n_translate.py --target ja-jp --dry-run

# Re-translate (ignore cache) for Turkish, only CmsUi.tmx, max 50 keys.
python3 modules/perc-i18n/scripts/i18n_translate.py \
  --target tr-tr --force --file CmsUi.tmx --limit 50
```

## Behaviour

- **Source**: always `en-us` `<seg>` text. The `<tuv xml:lang="en-us">` of
  each `<tu>` is the canonical source for the translation request.
- **Target**: the `--target` value (BCP-47 lowercase hyphen, e.g. `de-de`,
  `es-es`, `hi-in`). The runtime normalizes incoming tags to that form,
  so non-canonical targets like `de_DE` are NOT supported here.
- **Placeholder-only segments** (e.g. `{0}`, `{1,2,3}`) are skipped — they
  pass through to the output unchanged so parameter substitution still
  works.
- **Cache**: results are keyed by `sha256(target || \0 || text)` and
  stored at `scripts/.cache/i18n_translate.json`. Re-running resumes
  exactly where the previous run stopped. Pass `--force` to bypass the
  cache.
- **Rate limits / throttling**: when `soimort/translate-shell` exits with a 429-like
  message, the script sleeps with exponential backoff (2s base, 60s cap,
  ±20% jitter, 5 attempts) before failing the run. In addition, every successful
  translation sleeps for a random 1–10 seconds to avoid provider rate-limiting.
- **XML safety**: every translated segment is XML-escaped via
  `xml.sax.saxutils.escape` before being written back, so `<` / `>` /
  `&` / `"` in translations cannot corrupt the TMX file.
- **Atomicity**: the cache is written to a sibling `.tmp` file and
  renamed, so a SIGKILL mid-write does not corrupt the cache.

## Direct variant (`i18n_translate_direct.py`)

Use this when you have translate-shell installed locally, or when Docker is
available but `trans` is not on PATH. The script tries `trans --brief` first;
if `trans` is missing it automatically falls back to
`docker run --rm soimort/translate-shell --brief ...`.

```bash
# Fill missing Hindi TUVs (same files / cache / inject semantics as the Docker tool)
python3 modules/perc-i18n/scripts/i18n_translate_direct.py --target hi

# Arabic base locale fill
python3 modules/perc-i18n/scripts/i18n_translate_direct.py --target ar

# Windows (Docker Desktop only — no local trans): force Docker + UTF-8 capture
python modules/perc-i18n/scripts/i18n_translate_direct.py --target ar --docker --force

# Fix rows where the target still equals English
python3 modules/perc-i18n/scripts/i18n_translate_direct.py --target es --fix-matching-en

# Variant locale (only store differences from base when using --variant-base)
python3 modules/perc-i18n/scripts/i18n_translate_direct.py --target hi-in --variant-base hi
```

On **Windows with Docker Desktop**, prefer `--docker`. The script already falls
back to Docker when `trans` is missing; `--docker` makes that path explicit.
Each successful translation is logged as
`[trans] <source> -> <lang> = <translated>` (flushed immediately so PowerShell /
`cmd` show the line without waiting for process exit).

Differences from `i18n_translate.py`:

| | Docker (`i18n_translate.py`) | Direct (`i18n_translate_direct.py`) |
|--|------------------------------|--------------------------------------|
| Binary | `docker run … soimort/translate-shell` | `trans --brief` (preferred); `docker run … soimort/translate-shell` (fallback) |
| Cache file | `scripts/.cache/i18n_translate.json` | `scripts/.cache/i18n_translate_direct.json` |
| Rate limits | Exponential backoff on 429-like errors (2s base, 60s cap, ±20% jitter, 5 attempts) | **Same backoff** + random 1–10s throttle after each successful translation |
| Extra flags | (see Quick start) | `--fix-matching-en`, `--variant-base` |

### RTL / Arabic (`--target ar`)

Both scripts pass **`-no-bidi`** to translate-shell. Without that flag, RTL
output is terminal-padded and rewritten into Arabic presentation forms /
visual order, which looks like a blank or garbled translation and corrupts
placeholders such as `{0}`.

Default mode only **inserts missing** target TUVs. If `ar` rows already exist
but are polluted (the usual case after a pre-`-no-bidi` run), use
**`--replace-existing`** to re-translate and overwrite them. That flag always
bypasses the cache. Local `trans` is fine — no Docker required:

```bash
# Full Arabic repair (missing insert + overwrite every existing ar TUV)
python3 modules/perc-i18n/scripts/i18n_translate_direct.py \
  --target ar --replace-existing

# Same thing, solemn edition:
python3 modules/perc-i18n/scripts/i18n_translate_direct.py \
  --target ar --waste-another-6-hours-of-your-life

# Smoke-test a handful first
python3 modules/perc-i18n/scripts/i18n_translate_direct.py \
  --target ar --replace-existing --file CmsUi.tmx --limit 5
```

`--limit` applies **per phase** (missing / replace-existing / fix-matching-en).

Both scripts cache translations atomically (write `.tmp` then rename) and
XML-escape every inserted segment.

## Requirements

- **Docker** on PATH. The script invokes
  `docker run --rm soimort/translate-shell --brief "<text>" :<target>`.
- **Python 3.10+** (uses PEP 604 unions and modern `subprocess.run`).
- **Network access** to Docker Hub (first run pulls the image; later
  runs use the local cache).

If Docker is not available the script fails loud with an install hint
instead of silently falling back to a less-accurate provider.

## Tests

```bash
python3 modules/perc-i18n/scripts/test_i18n_translate.py
```

The test suite covers:

1. `cache_key` is stable across calls and distinct per (text, target).
2. `translate()` returns the cached value without calling
   `invoke_translate` when present.
3. `translate()` short-circuits placeholder-only sources.
4. Exponential backoff is invoked when `invoke_translate` reports a 429.
5. `TmxFile.inject()` writes only TUVs for missing languages and
   produces an XML-safe `<seg>`.
6. Paths are constructed with `pathlib`; no `os.path` joins.

The tests stub `invoke_translate` so Docker is not required to run them.
CI should still pull `soimort/translate-shell` for the optional
end-to-end smoke test, but the build gate does not depend on it.

## Operating-model notes

- This is a **developer tool**, not a Maven build gate. `mvn clean
  install` on `modules/perc-i18n` does not call it.
- The TMX files it edits (`modules/perc-i18n/src/main/resources/i18n/`)
  are committed to the repo. After running, review the diff and commit
  it alongside any related Java/REST changes.
- The script edits the canonical TMX files in place. There is no
  staging branch or sandbox — by design. If you need to experiment,
  copy the file aside first.
- For audits of which keys changed in a run, compare `git diff` on the
  two canonical TMX files.

## Cross-platform guarantees

- **Paths**: built with `pathlib.Path` exclusively; no string joins with
  `"/"` / `"\\"` for filesystem paths.
- **Docker invocation**: uses `subprocess.run([...], capture_output=True)`
  with no shell interpolation, so Windows `cmd.exe` quoting rules don't
  bite.
- **Encoding**: every file read/write uses `encoding='utf-8'` explicitly
  (Windows defaults to cp1252 otherwise).
- **Line endings**: the script preserves whatever line endings are in
  the source TMX; it does not rewrite `\r\n` ↔ `\n`.

