# perc-i18n Module

## Overview

The `perc-i18n` module is the canonical source for all internationalization (i18n) resources in Percussion CMS. It contains:

- **Framework Code**: Core i18n implementation classes for TMX (Translation Memory eXchange) document handling, localization utilities, and resource bundle management
- **Translation Resources**: Master TMX files used for server-wide string localization and UI translations

## Module Structure

```
src/main/java/com/percussion/i18n/          # Core i18n framework code
  ├── rxlt/                                  # Resource localization transformation framework
  ├── tmxdom/                                # TMX DOM parsing and manipulation
  ├── ui/                                    # UI-specific localization utilities
  └── *.java                                 # Core resource bundle and localization classes

src/main/resources/i18n/                     # Translation resources (canonical TMX files only)
  ├── ResourceBundle.tmx                     # Master i18n resource bundle (seed file)
  ├── CmsUi.tmx                              # CMS UI-specific translations
  └── SystemResources.tmx                    # System-wide resource translations

scripts/                                     # Developer tooling (not packaged into the JAR)
  ├── i18n_translate.py                      # Back-fill missing TUVs via Docker translate-shell
  ├── test_i18n_translate.py                 # Unit tests (no Docker required)
  └── README.md                              # CLI contract and cross-platform notes
```

## I18n Resources

All translation memory exchange (TMX) files are maintained in this module:

### ResourceBundle.tmx

- **Purpose**: Master i18n resource bundle for system initialization
- **Scope**: Server-wide resources and core system strings
- **Format**: TMX 1.4 with supported language declarations
- **Note**: Seeded with a single `en-us` header row; the body is
  empty by design. The runtime merges the content of `CmsUi.tmx` and
  `SystemResources.tmx` on top of this master.

### CmsUi.tmx

- **Purpose**: UI-specific translations for the CMS interface
- **Scope**: Content Manager UI components, dialogs, and labels
- **Supported Languages**: the 17-locale matrix below
- **Naming Convention**: Keys follow pattern `perc.ui.(IDENTIFIER).(TYPE)@(MESSAGE/KEY)`

### SystemResources.tmx

- **Purpose**: System and content editor resources
- **Scope**: Content editor actions, system messages, and resource definitions
- **Key Examples**: `psx.ce.action@Check-in`, `psx.ce.action@Update`
- **Supported Languages**: the 17-locale matrix below

### Canonical 17-Locale Matrix

Both `CmsUi.tmx` and `SystemResources.tmx` declare the same set of
languages in their `<header>` `<prop type="supportedlanguage">` lines:

| Family    | Codes                                              |
|-----------|----------------------------------------------------|
| English   | `en-us`, `en-gb`                                   |
| Spanish   | `es` (generic), `es-cl`, `es-es`, `es-mx`          |
| French    | `fr-ca`, `fr-fr`                                   |
| German    | `de-de`                                            |
| Hindi     | `hi` (generic), `hi-in`                            |
| Italian   | `it-it`                                            |
| Japanese  | `ja-jp`                                            |
| Dutch     | `nl-nl`                                            |
| Portuguese| `pt-br`, `pt-pt`                                   |
| Turkish   | `tr-tr`                                            |

Locale tags are normalized to BCP-47 lowercase hyphen form by
`PSTmxResourceBundle.normalizeLang(...)`. Per-locale sibling TMX
files (e.g. `de_DE.tmx`) are no longer supported; all translations
for a key live as inline `<tuv xml:lang="...">` siblings in the
canonical files.

## Integration with Distribution

The perc-i18n module is consumed by `perc-distribution-tree` during the build process:

1. **Dependency Declaration**: perc-distribution-tree declares perc-i18n as a provided dependency
2. **Resource Extraction**: During the Maven build, the `maven-dependency-plugin` unpacks i18n resources
3. **Output Location**: Resources are extracted to `distribution/rxconfig/i18n/` in the assembly
4. **Runtime Location**: At runtime, the server looks for TMX files at `rxconfig/i18n/` relative to the installation root

## Consolidation History

**Date**: March 10, 2026

The following consolidation was performed to align i18n resources with proper Maven structure:

### Files Moved to perc-i18n

- `ResourceBundle.tmx` (from `/system/config/I18n/`)
- `CmsUi.tmx` (from `/system/cms/content/applications/sys_resources/ApplicationFiles/i18n/`)
- `SystemResources.tmx` (from `/system/cms/content/applications/sys_resources/ApplicationFiles/i18n/`)

### Rationale

1. **Single Source of Truth**: All i18n resources now reside in one module, eliminating duplication
2. **Maven Compliance**: Resources follow Maven standard directory structure (`src/main/resources/`)
3. **Cleaner Distribution**: perc-distribution-tree pulls resources from perc-i18n rather than scattering across multiple locations
4. **Maintainability**: Framework code and translation resources are colocated

### Legacy Location Cleanup

- Removed TMX files from `/system/config/I18n/` (keeping only `sys_createTranslations.properties`)
- Removed TMX files from `/system/cms/content/applications/sys_resources/ApplicationFiles/i18n/`
- These legacy locations are no longer part of the Maven build

## Building

To build the perc-i18n module:

```bash
cd modules/perc-i18n
../../mvn-env.sh clean install
```

The module will:
1. Compile Java i18n framework classes
2. Package all resources into `perc-i18n-{version}.jar`
3. Include i18n resources at path: `i18n/*.tmx` within the JAR

**Note**: When the `i18n_translate.py` CLI is used to back-fill
translations, the resulting edits land directly in the canonical TMX
files. There is no separate staging branch or sandbox; review the
diff (`git diff`) and commit alongside any related Java changes.

## Translation Pipeline

The `scripts/i18n_translate.py` CLI is the single source of new
translation text. It backs every missing `<tuv xml:lang="...">` via
`docker run --rm soimort/translate-shell`, honours rate limits with
exponential backoff, and caches results on disk for resume.

```bash
# Translate every <tu> missing a German <tuv> across both canonical files.
python3 modules/perc-i18n/scripts/i18n_translate.py --target de-de

# See what's missing without contacting the translation service.
python3 modules/perc-i18n/scripts/i18n_translate.py --target ja-jp --dry-run
```

See `scripts/README.md` for the full CLI contract, rate-limit
semantics, Docker requirement, and cross-platform guarantees.

## Using i18n Resources

### At Runtime

- The `PSTmxResourceBundle` class loads TMX files from
  `rxconfig/I18n/ResourceBundle.tmx` (master) plus `rx_resources/I18n/`
  and `sys_resources/I18n/`. Since this revision it also scans the
  lowercase `rxconfig/i18n/` directory (where the Maven build extracts
  `CmsUi.tmx` and `SystemResources.tmx`).
- Locale tags declared in `<prop type="supportedlanguage">` headers
  and looked up via the public API are normalized to lowercase hyphen
  BCP-47 form by `PSTmxResourceBundle.normalizeLang(String)`. Inline
  `<tuv xml:lang="...">` attributes that the loader encounters are
  normalized on read as well, so `en_US.tmx`-style tags collapse into
  the canonical `en-us` bucket automatically.
- When a requested locale is missing, the lookup falls back to the
  language-only bucket (e.g. `en-gb` -> `en-us`) and finally to the
  system default language. Callers do not need to know which regional
  variants ship out of the box.
- Language selection happens during server initialization via
  `PSI18nStartupManager`.
- Additional TMX files (CmsUi.tmx, SystemResources.tmx) are loaded by
  the runtime once `rxconfig/i18n/` is scanned.

### In Applications

Applications reference i18n extensions and resources through content editor definitions:
- Extension: `Java/i18n/sys_LocalizedTextLookup`
- Extension: `Java/i18n/sys_LocalizedTextLookupUser`

These extensions enable dynamic string lookups from TMX resources during runtime.

## Dependencies

- **perc-security-utils**: For security-related utilities used in i18n processing
- **perc-legacy**: For legacy compatibility and common utilities
- **utils**: Core utility classes

## Testing

The module includes unit tests for:
- TMX document parsing and validation
- Resource bundle loading and caching
- Localization transformations
- Language-specific resource handling

Run tests with:

```bash
../../mvn-env.sh -pl modules/perc-i18n test
```

## Related Modules

- **perc-distribution-tree**: Consumes perc-i18n resources during distribution build
- **perc-legacy**: Provides legacy translation infrastructure
- **WebUI**: Uses i18n resources for web UI localization
- **system**: Core system that initializes i18n resources at startup

## Future Enhancements

- One-time DB migration that rewrites persisted `sys_lang` and
  `LOCALE` values from the legacy `hi` / `es` / `en` codes to the
  canonical regional forms (`hi-in`, `es-es`, `en-us`). Not in scope
  for the current revision because the runtime loader already handles
  both forms via normalization.
- Migrate additional application-specific TMX files to perc-i18n
- Enhance language tool integration for new language support
- Implement more sophisticated resource merging strategies
- Add support for runtime language switching without server restart

