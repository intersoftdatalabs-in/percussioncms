# Erlang review — #3741 DCE/ContentUI/TableFactory IPS*Errors → typed ErrorCodes

**Scope:** uncommitted branch `fix/issue-3741-dce-contentui-tablefactory-errorcodes` vs `origin/main` (parent #2616 slice 3/3).

**Memory patterns hit:** typed `*ErrorCodes` + `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; additive constructors (C2); portable `Path`/`@TempDir` in tests; do not delete `IPS*Errors` interfaces.

## Summary

Leftover production `IPSContentExplorerErrors` / `IPSCmsErrors` / `IPSSearchErrors` / `IPSServerErrors` / `IPSTableFactoryErrors` call-sites in DesktopContentExplorer, ContentUI search, and TableFactory now throw typed catalog enums. Additive `IPSErrorCode` constructors on `PSStandaloneException`, `PSContentExplorerException`, `PSExtensionProcessingException`, `PSWizardValidationError`, and `PSJdbcTableFactoryException` retain `getTypedErrorCode()` / `isAuditable()`. Allow-list shrunk by those exact paths. Dual-write skip tests cover HTML search missing-parameter (16053) and production throws.

## Recommendation

approve

## Gate

- Bugs: none found
- Behavioral tests: present (TableFactory 4, DCE 5, ContentUI 2; perc-auditlog dual-write skip)
- Cross-platform paths: DCE cataloger test uses `@TempDir Path` + `Files.createDirectories` + `toUri().toURL()` (no hardcoded separators)
- May commit/push: yes

## Issues

None.

## C2

Constructors are additive. Subclasses of `PSStandaloneException` still compile (`PSOptionException`, `PSServletException`, inner `PSRequestException`). No `extends PSExtensionProcessingException` / `PSJdbcTableFactoryException`. Standalone reverse-dep installs: `system`, `modules/DesktopContentExplorer`, `modules/ContentUI`.
