# perc-i18n Module Agent Guidelines

## Before Starting Any Task

**IMPORTANT**: Read [README.md](README.md) first. It contains critical context about the module's architecture, resource consolidation, and build integration.

## Module Overview

The `perc-i18n` module is the **canonical source for all i18n resources** in Percussion CMS. Any changes to internationalization, TMX files, or localization should start here.

## Key Rules for This Module

### 1. I18n Resources Consolidation (Effective March 10, 2026)

All TMX files are centralized in `src/main/resources/i18n/`:
- **ResourceBundle.tmx** - Master resource bundle
- **CmsUi.tmx** - UI translations (ship locale matrix, see Quick Reference)
- **SystemResources.tmx** - System/editor resources (same ship locale matrix)

**Legacy locations are deprecated:**
- ~~`system/config/I18n/`~~ (TMX files removed)
- ~~`system/cms/content/applications/sys_resources/ApplicationFiles/i18n/`~~ (TMX files removed)
- ~~Per-locale sibling `en_US.tmx`, `de_DE.tmx`, `hi_IN.tmx`, etc.~~ (forbidden; inline only)
- ~~`backup/*.tmx` snapshots~~ (forbidden; inline only)

### 1b. Locale format profiles (`RXLOCALEFORMAT`)

Format metadata (text direction, date/time patterns, currency, separators,
measurement, default TZ) lives in **`RXLOCALEFORMAT`**, keyed by
**`LANGUAGESTRING`** (BCP-47 code) — not `LOCALEID` — so customer-added
locales stay stable across installs.

- Schema: `modules/perc-distribution-tree/.../cmsTableDef.xml`
- Seed: `cmsTableData.xml` table `RXLOCALEFORMAT`
- Entity / resolver: `system` module — `PSLocaleFormat`,
  `PSLocaleFormatResolver`, `PSLocaleFormatDefaults`, `PSLocaleFormatCatalog`
- Lookup chain: exact → language-only → `en-us`, merging non-null fields
- Missing row for a customer locale is fine (inherits)
- Login bootstrap exposes `localeFormat` + `<html dir="…">` for UI work

### 1a. Canonical TMX Layout Rules

- **Inline `<tuv>` is the only supported TMX layout.** Every translation for a
  given key lives inside the same canonical file (`CmsUi.tmx` or
  `SystemResources.tmx`) as `<tuv xml:lang="...">` siblings. Do not add
  per-locale sibling files (e.g. `de_DE.tmx`, `es_ES.tmx`, `ja-JP.tmx`)
  — they are ignored by `PSTmxResourceBundle` and bypass the cache
  normalization.
- **Locale tag convention is lowercase hyphen BCP-47**: `en-us`,
  `es-es`, `hi-in`. The runtime normalizes incoming tags via
  `PSTmxResourceBundle.normalizeLang(...)`, so `EN_US`, `en-us`,
  `en-US` and `EN-us` all resolve to the same `en-us` bucket.
  Tag mismatches between the `<header>` `<prop type="supportedlanguage">`
  line and the inline `<tuv xml:lang="...">` attribute still work
  because both are normalized on read, but new content must use the
  canonical form.
- **Ship locale matrix** (bases + regionals) is defined in `RXLOCALE`
  (`cmsTableData.xml`) and mirrored in TMX headers / `RXLOCALEFORMAT`.
  **Base / language-only** codes (`ISBASE=1`): `ar`, `bn`, `de`, `es`,
  `fr`, `he`, `hi`, `it`, `nl`, `pl`, `pt`, `ru`, `sv`, `te`, `tr`.
  **Regionals** (`ISBASE=0`) include e.g. `de-de`, `de-at`, `es-es`,
  `es-mx`, `fr-fr`, `fr-ca`, `he-il`, `hi-in`, `it-it`, `nl-nl`,
  `pt-br`, `pt-pt`, `tr-tr`, `zh-cn`, `zh-tw`, plus additional
  `es-*` / `fr-*` / `de-*` variants listed in the TMX header. Product string fallback when no
  locale is set is always **`en-us`**. Do not introduce a code outside
  this set without updating `RXLOCALE` **and** `RXLOCALEFORMAT` in
  `modules/perc-distribution-tree/.../cmsTableData.xml` first (and
  `PSLocaleFormatDefaults` / WebUI `SHIP_LOCALE_ENDONYMS`).
- **Header `<prop type="supportedlanguage">` lines** list every
  base + regional the product recognizes (see development TMX header).
  The header is the source of truth for "what languages ship out of
  the box".
- **Base vs regional TMX storage:** store shared translations under the
  base language tag (`de`, `es`, `fr`, `hi`, `it`, `ar`, …). Regional
  tags store **only dialect overrides**. Lookup is
  `regional → base → en-us` via `PSTmxResourceBundle.languageLookupChain`
  (and the same chain for `RXLOCALEFORMAT`).
- **Login dropdown:** server-side `PSLocaleLoginSelection` hides a base
  locale when any active regional sibling exists (e.g. hide `es` when
  `es-es` is active; show `ar` when no `ar-*` exists).
- **Per-key translations are owned by `i18n_translate.py`.** Do not
  hand-edit translated `<seg>` text in a TMX file; re-run the script
  (see section 1b). Hand-edits are only acceptable for English source
  strings and for review-driven fixes.
- **Duplicate `<tu tuid="...">` entries are runtime last-wins** via
  `PSTmxResourceBundle.addResourcesToCache` (`map.put` at line ~564).
  Resolve duplicates by hand before commit; rely on order is fragile
  because per-locale sibling files and inline merges can re-introduce
  the same key.

### 2. Build Integration

When modifying i18n resources:
1. Changes are made in `modules/perc-i18n/src/main/resources/i18n/`
2. Build compiles framework code and packages resources in JAR
3. `perc-distribution-tree` extracts resources during its build
4. Final location in installation: `rxconfig/i18n/`

**Important**: Always rebuild both perc-i18n AND perc-distribution-tree when changing TMX files:

```bash
./mvnw -pl modules/perc-i18n clean install
./mvnw -pl modules/perc-distribution-tree clean install
```

**Canonical on-disk directory is lowercase only:** `rxconfig/i18n/`.
Never create or write `rxconfig/I18n/` (uppercase) — on case-sensitive
Linux that becomes a second folder and splits product TMX from RXLT
output. The runtime reads the canonical path first and falls back to
legacy uppercase only when that distinct path still has files.

**Upgrade:** product TMX (`CmsUi.tmx`, `SystemResources.tmx`,
`DeveloperUi.tmx`, seed `ResourceBundle.tmx`) is force-copied from the
package into `rxconfig/i18n/` by
`rxconfig/Installer/updateConfiguration.xml`. Bulk upgrade preserves
`rxconfig/**` for customer config, so without that force-copy upgrades
kept stale TMX and Language Tool remerged the old sources.

### 2a. Translation Pipeline

The `scripts/i18n_translate.py` / `i18n_translate_direct.py` CLIs are
the single source of new translation text. They shell out to
translate-shell (Docker and/or local `trans`) and write results back
into the canonical TMX files with proper XML escaping. They honor rate
limits with exponential backoff and share a **checked-in** cache at
`scripts/cache/i18n_translate.json` (via `i18n_cache.py`) so re-runs
resume across machines. Commit cache updates with TMX changes after a
translate run. On cache merge conflicts run
`scripts/resolve_i18n_cache_conflicts.py` (union both sides).

```bash
# Translate every <tu> missing a German TUV in both canonical files.
python3 modules/perc-i18n/scripts/i18n_translate.py --target de-de

# See what's missing without invoking the translation service.
python3 modules/perc-i18n/scripts/i18n_translate.py --target ja-jp --dry-run

# Re-translate Turkish, ignoring the cache, only CmsUi.tmx, max 50 keys.
python3 modules/perc-i18n/scripts/i18n_translate.py --target tr-tr --force --file CmsUi.tmx --limit 50
```

The script is a developer tool, not a Maven build gate. See
`scripts/README.md` for the full CLI contract, rate-limit semantics,
and cross-platform guarantees.

### 2b. Adding a New Locale

1. Add `<prop type="supportedlanguage"><code></prop>` to the header of
   `CmsUi.tmx` and `SystemResources.tmx` (alphabetical position).
2. Add a new row in `RXLOCALE` in
   `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml`
   with `STATUS=1` (visible by default), a sensible `SORTORDER`, and
   `ISBASE=1` for language-only / base codes (no region) or `ISBASE=0`
   for regionals. Schema column is defined in `cmsTableDef.xml`.
3. If the new code is a regional variant (e.g. `xx-yy` where `xx`
   already exists), update locale-aware consumers:
   - `modules/perc-packages/.../perc.widget.calendar/percCalendarTwo.xml`
     enum if a new picker entry is required.
   - `system/src/main/java/com/percussion/search/lucene/analyzer/PSLocaleSpecificLuceneAnalyzer.java`
     if a new analyzer branch is required.
4. Run `python3 modules/perc-i18n/scripts/i18n_translate.py --target <code>`
   to back-fill translations for the existing keys.
5. Run `./mvnw -pl modules/perc-i18n clean install` and the
   matching `perc-distribution-tree` build.
6. Update the Quick Reference table at the bottom of this file and
   the Supported Languages section in `README.md`.

For a generic (non-regional) locale, step 3 is a no-op.

### 3. TMX File Format

All TMX files must follow this structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<tmx version="1.4">
   <header>
      <prop type="supportedlanguage">en-us</prop>
      <prop type="supportedlanguage">es</prop>
   </header>
   <body>
      <!-- Translation units here -->
   </body>
</tmx>
```

**Validation**:
- Use `xmllint --noout file.tmx` to validate XML
- Ensure all supported languages are declared in header
- Do not use self-closing tags for empty elements

### 4. Naming Conventions for Keys

**CmsUi.tmx**:

```
perc.ui.(IDENTIFIER).(TYPE)@(MESSAGE/KEY)
Examples:
  - perc.ui.common.label@Ok
  - perc.ui.sitedialog.summary@summary1
```

**SystemResources.tmx**:

```
psx.ce.(TYPE)@(MESSAGE/KEY)
Examples:
  - psx.ce.action@Check-in
  - psx.ce.action@Update
```

### 5. Java Code Guidelines for i18n

- **Framework Code Path**: `src/main/java/com/percussion/i18n/`
- **Core Classes**:
  - `PSTmxResourceBundle` - Loads and caches TMX resources; normalizes
    locale tags via `normalizeLang(String)`, scans `rxconfig/i18n/`
    (lowercase) in addition to the uppercase legacy paths.
  - `PSTmxDocument` - Parses TMX documents
  - `PSI18nUtils` - Utility methods for localization
- **No changes to resource loading logic without architecture review**
- Use existing extension points (`Java/i18n/sys_LocalizedTextLookup`, `Java/i18n/sys_LocalizedTextLookupUser`)
- Follow Google Java Style Guide per root AGENTS.md
- Test-only entry points are package-private with `@VisibleForTesting`
  JavaDoc: `flushCacheForTest()`, `addResourcesToCacheForTest(Document)`,
  `getResourceBundlesForTest()`. Do not call them from production code.

### 6. Testing

Run i18n tests before committing:

```bash
./mvnw -pl modules/perc-i18n test
```

Tests verify:
- TMX document parsing
- Resource bundle caching
- Localization transformations
- Language handling

### 7. Dependencies

Do not add dependencies without review. Current dependencies:
- `perc-security-utils` - Security utilities
- `perc-legacy` - Legacy compatibility
- `utils` - Core utilities

### 8. When to Update This File

Update AGENTS.md if:
- Major architectural changes to i18n system
- New supported languages added
- Resource directory structure changes
- Build process changes

Update README.md if:
- New TMX files added/removed
- Changes to resource consolidation
- Build integration changes
- Dependencies added/removed

## Common Tasks

### Adding a New Translation to an Existing Key

1. Run `python3 modules/perc-i18n/scripts/i18n_translate.py --target <lang> --file CmsUi.tmx`
   to fill the missing `<tuv>` from the canonical source. Pass `--dry-run`
   first to confirm the key set.
2. Validate the diff: `xmllint --noout CmsUi.tmx`
3. Rebuild: `./mvnw -pl modules/perc-i18n clean install`
4. Test: `./mvnw -pl modules/perc-i18n test`

Hand-editing a single `<tuv>` is allowed for review fixes but should be
followed by a re-run of `i18n_translate.py --force` for the affected
key so the script's cache and the committed file stay in sync.

### Adding a New Translation Unit

1. Edit the appropriate TMX file (`CmsUi.tmx` or `SystemResources.tmx`).
   `ResourceBundle.tmx` is the master/seed file and is documented as
   having an empty body — do not add TUs there.
2. Follow the key naming convention for that file (`perc.ui.*` for
   CmsUi, `psx.ce.*` for SystemResources).
3. Always include `<tuv xml:lang="en-us">` as the canonical source.
4. Use `python3 modules/perc-i18n/scripts/i18n_translate.py --target <lang>`
   to back-fill the rest of the matrix.
5. Validate XML and rebuild.

### Adding a New Language to CmsUi.tmx / SystemResources.tmx

Follow the checklist in section 2b above. The single source of new
translation text is `i18n_translate.py`; do not hand-translate.

### Fixing TMX Parsing Errors

If seeing `PSTmxResourceBundle` errors:
1. Check XML syntax: `xmllint --noout <file.tmx>`
2. Verify supported languages declared in header (17 codes, see
Quick Reference)
3. Check for properly closed elements (especially `<seg>` content with
`<` / `>` / `&` characters that must be XML-escaped)
4. Validate against TMX 1.4 schema
5. If a new TUV was just inserted by hand and the file no longer
parses, re-run `i18n_translate.py --force --target <lang>` for the
affected key — the script XML-escapes `<seg>` content via
`xml.sax.saxutils.escape` on write.

## Related Modules

- **perc-distribution-tree** - Pulls i18n resources; keeps `src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml` in sync
- **system** - Initializes i18n at startup; keeps legacy config patterns for now
- **WebUI** - Consumes i18n resources for web UI

## Quick Reference

|        File         |         Purpose         |            Languages            |          Location          |
|---------------------|-------------------------|---------------------------------|----------------------------|
| ResourceBundle.tmx  | Master bundle (seed)    | en-us                           | `src/main/resources/i18n/` |
| CmsUi.tmx           | UI labels/strings       | base + regional matrix (header) | `src/main/resources/i18n/` |
| SystemResources.tmx | System/editor resources | base + regional matrix (header) | `src/main/resources/i18n/` |

**Base locales** (`RXLOCALE.ISBASE=1`): `ar`, `bn`, `de`, `es`, `fr`,
`he`, `hi`, `it`, `nl`, `pl`, `pt`, `ru`, `sv`, `te`, `tr`. TMX bodies
store shared strings under these tags; regionals hold dialect overrides
only.

**Representative regionals** (`ISBASE=0`): `de-de`/`de-at`/…, `en-us`/
`en-gb`, `es-es`/`es-mx`/…, `fr-fr`/`fr-ca`/…, `he-il`, `hi-in`,
`it-it`/`it-ch`, `ja-jp`, `nl-nl`/`nl-be`, `pt-br`/`pt-pt`, `tr-tr`,
`zh-cn`/`zh-tw`. Full list: `RXLOCALE` + TMX `<header>` in
`cmsTableData.xml` / canonical TMX files.

Lookup is `regional → base → en-us`. Login hides a base locale when any
active regional sibling exists (`PSLocaleLoginSelection`). Default
login/string fallback: **`en-us`**. Format profiles: `RXLOCALEFORMAT`
(+ `PSLocaleFormatDefaults`).

The runtime loader normalizes incoming tags so `EN_US`, `en-US`,
`es_ES`, `ja-JP`, etc. all resolve to their canonical bucket.

**Developer tooling** (outside the JAR; in `scripts/`):

|               File               |                         Purpose                          |
|----------------------------------|----------------------------------------------------------|
| `scripts/i18n_translate.py`      | Back-fill missing TUVs via Docker translate-shell        |
| `scripts/test_i18n_translate.py` | Unit tests (no Docker required)                          |
| `scripts/README.md`              | CLI contract, rate-limit semantics, cross-platform notes |

## Questions or Issues?

- Check README.md first for architectural context
- Review `/memories/repo/i18n-consolidation-2026-03-10.md` for consolidation history
- Examine existing TMX files for formatting examples
- Run tests to catch issues early

