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

src/main/resources/i18n/                     # Translation resources
  ├── ResourceBundle.tmx                     # Master i18n resource bundle
  ├── CmsUi.tmx                              # CMS UI-specific translations
  └── SystemResources.tmx                    # System-wide resource translations
```

## I18n Resources

All translation memory exchange (TMX) files are maintained in this module:

### ResourceBundle.tmx

- **Purpose**: Master i18n resource bundle for system initialization
- **Scope**: Server-wide resources and core system strings
- **Format**: TMX 1.4 with supported language declarations

### CmsUi.tmx

- **Purpose**: UI-specific translations for the CMS interface
- **Scope**: Content Manager UI components, dialogs, and labels
- **Supported Languages**: en-us, es, hi
- **Naming Convention**: Keys follow pattern `perc.ui.(IDENTIFIER).(TYPE)@(MESSAGE/KEY)`

### SystemResources.tmx

- **Purpose**: System and content editor resources
- **Scope**: Content editor actions, system messages, and resource definitions
- **Key Examples**: `psx.ce.action@Check-in`, `psx.ce.action@Update`
- **Supported Languages**: en-us, es

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

## Using i18n Resources

### At Runtime

- The `PSTmxResourceBundle` class loads TMX files from `rxconfig/i18n/ResourceBundle.tmx`
- Language selection happens during server initialization via `PSI18nStartupManager`
- Additional TMX files (CmsUi.tmx, SystemResources.tmx) are loaded by applications that reference them

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

- Migrate additional application-specific TMX files to perc-i18n
- Enhance language tool integration for new language support
- Implement more sophisticated resource merging strategies
- Add support for runtime language switching without server restart

