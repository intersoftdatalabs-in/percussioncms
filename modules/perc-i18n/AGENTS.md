# perc-i18n Module Agent Guidelines

## Before Starting Any Task

**IMPORTANT**: Read [README.md](README.md) first. It contains critical context about the module's architecture, resource consolidation, and build integration.

## Module Overview

The `perc-i18n` module is the **canonical source for all i18n resources** in Percussion CMS. Any changes to internationalization, TMX files, or localization should start here.

## Key Rules for This Module

### 1. I18n Resources Consolidation (Effective March 10, 2026)

All TMX files are centralized in `src/main/resources/i18n/`:
- **ResourceBundle.tmx** - Master resource bundle
- **CmsUi.tmx** - UI translations (en-us, es, hi)
- **SystemResources.tmx** - System/editor resources (en-us, es)

**Legacy locations are deprecated:**
- ~~`system/config/I18n/`~~ (TMX files removed)
- ~~`system/cms/content/applications/sys_resources/ApplicationFiles/i18n/`~~ (TMX files removed)

### 2. Build Integration

When modifying i18n resources:
1. Changes are made in `modules/perc-i18n/src/main/resources/i18n/`
2. Build compiles framework code and packages resources in JAR
3. `perc-distribution-tree` extracts resources during its build
4. Final location in installation: `rxconfig/i18n/`

**Important**: Always rebuild both perc-i18n AND perc-distribution-tree when changing TMX files:

```bash
./mvn-env.sh -pl modules/perc-i18n clean install
./mvn-env.sh -pl modules/perc-distribution-tree clean install
```

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
  - `PSTmxResourceBundle` - Loads and caches TMX resources
  - `PSTmxDocument` - Parses TMX documents
  - `PSI18nUtils` - Utility methods for localization
- **No changes to resource loading logic without architecture review**
- Use existing extension points (`Java/i18n/sys_LocalizedTextLookup`, `Java/i18n/sys_LocalizedTextLookupUser`)
- Follow Google Java Style Guide per root AGENTS.md

### 6. Testing

Run i18n tests before committing:

```bash
./mvn-env.sh -pl modules/perc-i18n test
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

### Adding a New Language to CmsUi.tmx

1. Add language to header: `<prop type="supportedlanguage">NEW_LANG</prop>`
2. Add translation unit variants for each new language
3. Validate XML: `xmllint --noout CmsUi.tmx`
4. Rebuild: `./mvn-env.sh -pl modules/perc-i18n clean install`
5. Test: `./mvn-env.sh -pl modules/perc-i18n test`

### Adding a New Translation Unit

1. Edit appropriate TMX file (ResourceBundle.tmx, CmsUi.tmx, or SystemResources.tmx)
2. Follow key naming convention for that file
3. Include tu (translation unit) element with all supported languages
4. Validate and rebuild

### Fixing TMX Parsing Errors

If seeing `PSTmxResourceBundle` errors:
1. Check XML syntax: `xmllint --noout <file.tmx>`
2. Verify supported languages declared in header
3. Check for properly closed elements
4. Validate against TMX 1.4 schema

## Related Modules

- **perc-distribution-tree** - Pulls i18n resources; keeps `src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml` in sync
- **system** - Initializes i18n at startup; keeps legacy config patterns for now
- **WebUI** - Consumes i18n resources for web UI

## Quick Reference

|        File         |         Purpose         |   Languages   |          Location          |
|---------------------|-------------------------|---------------|----------------------------|
| ResourceBundle.tmx  | Master bundle           | en-us         | `src/main/resources/i18n/` |
| CmsUi.tmx           | UI labels/strings       | en-us, es, hi | `src/main/resources/i18n/` |
| SystemResources.tmx | System/editor resources | en-us, es     | `src/main/resources/i18n/` |

## Questions or Issues?

- Check README.md first for architectural context
- Review `/memories/repo/i18n-consolidation-2026-03-10.md` for consolidation history
- Examine existing TMX files for formatting examples
- Run tests to catch issues early

