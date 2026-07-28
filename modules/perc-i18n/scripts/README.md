# i18n_translate.py — Developer Tooling for the perc-i18n Module

This directory contains cross-platform Python tooling that automates the
back-fill of missing translations in the canonical TMX files. It replaces
the legacy per-locale sibling `*.tmx` files and the previous
hand-maintained Python/shell scripts.

## Files

| File | Purpose |
|------|---------|
| `i18n_translate.py` | CLI: walks `CmsUi.tmx` and `SystemResources.tmx`, fills missing `<tuv>` blocks via `soimort/translate-shell`. |
| `test_i18n_translate.py` | Pytest-compatible unit tests (run without Docker). |

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
- **Rate limits**: when `soimort/translate-shell` exits with a 429-like
  message, the script sleeps with exponential backoff (2s base, 60s cap,
  ±20% jitter) and retries up to 5 attempts before failing the run.
- **XML safety**: every translated segment is XML-escaped via
  `xml.sax.saxutils.escape` before being written back, so `<` / `>` /
  `&` / `"` in translations cannot corrupt the TMX file.
- **Atomicity**: the cache is written to a sibling `.tmp` file and
  renamed, so a SIGKILL mid-write does not corrupt the cache.

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
