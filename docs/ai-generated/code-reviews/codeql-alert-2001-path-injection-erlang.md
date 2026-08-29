# Erlang review — fix/codeql-alert-2001-path-injection

**Scope:** uncommitted vs `HEAD` on `fix/codeql-alert-2001-path-injection`.
**Date:** 2026-08-28
**Recommendation:** approve
**Gate:** May commit/push: yes (after `system` standalone `mvnw clean install`)
**Memory patterns hit:** behavioral rejection tests; trusted root is RxDir; portable `@TempDir` / thread RxDir

## Summary

`getAppRootDir`, RecoverableFile construction, and `deleteFile`/`deleteDirectory` now call `requireUnderBase(PSServer.getRxDir(), ...)`. Sink-line annotations on residual File API. Query-filter + model pack + `suppressions.md`.

## Cross-platform path checklist

- [x] No hardcoded filesystem separators for joins
- [x] Tests use `File.separator` and `@TempDir`
- [x] Thread RxDir restored in `@AfterEach`

## Issues

None (hard-gate). Focused tests: 3 passed.
