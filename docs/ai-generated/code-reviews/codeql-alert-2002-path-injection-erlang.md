# Erlang review — fix/codeql-alert-2002-path-injection

**Scope:** uncommitted vs `HEAD` on `fix/codeql-alert-2002-path-injection`.
**Date:** 2026-08-29
**Recommendation:** approve
**Gate:** May commit/push: yes (after `system` standalone `mvnw clean install`)
**Memory patterns hit:** behavioral rejection tests; trusted root is object-store dir / RxDir, not a parent of untrusted input; portable `File.separator` / `@TempDir` / `PathUtils.setThreadOnlyRxDir`; request-facing IAE → not-found

## Summary

Runtime barriers for object-store application XML files (`resolveApplicationXmlFile` → `requireUnderBase`) and lock streams (`requireUnderRxDir` → RxDir containment). Sink-line annotations on `exists` / stream constructors. Query-filter + model pack + `suppressions.md`.

## Cross-platform path checklist

- [x] No hardcoded filesystem separators for joins
- [x] `requireUnderBase` + `Path.startsWith` in tests
- [x] Thread-local RxDir restored in `@AfterEach`

## Issues

None (hard-gate). Focused tests: 4 passed.

## Re-review (2026-08-29)

PR #3979 review threads. `updateSummaryEntry` / `loadApplication` translate `IllegalArgumentException` from `getApplicationFile` to `PSNotFoundException` (request-facing 4xx, not 500). `init` swallows the same IAE when setting summary mtime. Javadoc matches `requireUnderBase` (relative multi-segment allowed; `..` rejected). Kept `// codeql[java/path-injection]` on `appFile.exists()` because CodeQL still flags that residual (#2008). New tests: `getApplicationFile` traversal, `loadApplication` → not-found, `lockOutputStream`/`lockInputStream` happy path + outside RxDir. Cross-platform: `@TempDir`, `File.separator`, `PathUtils.setThreadOnlyRxDir` restored in `@AfterEach`.
