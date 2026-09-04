# Erlang review — issue #4283 (XSpLit parent dirs on config save)

**Branch:** `fix/issue-4283-xsplitt-parent-dirs`  
**Scope:** `PSSystemService.saveConfiguration` + `PSSystemServiceSaveConfigurationTest`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  

## Summary

Before writing allow-listed server config files, `saveConfiguration` now creates missing parent directories with portable NIO (`Path` + `Files.createDirectories`). This unblocks `TIDY_CONFIG` / `SERVER_PAGE_TAGS` under `rxconfig/XSpLit` on fresh H2/QA cells. Behavioral unit tests cover missing-parent TIDY and SERVER_PAGE_TAGS saves plus the already-present-parent path. Paths in tests use `Path.resolve` / `Files.readString` (no hardcoded separators).

## Cross-platform path checklist

- [x] No hardcoded `/` or `\` filesystem joins in new code
- [x] Uses `Path` / `Files.createDirectories`
- [x] Tests assert via NIO Path equality / content, not OS path strings
- [x] Temp dirs via JUnit `@TempDir`

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Build

`cd system && ../mvnw.cmd clean install` → BUILD SUCCESS
