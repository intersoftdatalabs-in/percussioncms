# Erlang review — fix/codeql-alert-2001-path-injection

**Scope:** uncommitted vs `HEAD` on `fix/codeql-alert-2001-path-injection`.
**Date:** 2026-08-29
**Recommendation:** approve
**Gate:** May commit/push: yes (after `system` standalone `mvnw clean install`)
**Memory patterns hit:** behavioral rejection tests; trusted root is RxDir; portable `@TempDir` / thread RxDir; symlink-not-follow deletes; empty appRoot contract

## Summary

`getAppRootDir`, RecoverableFile construction, and `deleteFile`/`deleteDirectory` now call `requireUnderBase(PSServer.getRxDir(), ...)`. Sink-line annotations on residual File API. Query-filter + model pack + `suppressions.md`.

## Cross-platform path checklist

- [x] No hardcoded filesystem separators for joins
- [x] Tests use `File.separator` and `@TempDir`
- [x] Thread RxDir restored in `@AfterEach`

## Issues

None (hard-gate). Focused tests: 3 passed.

## Re-review (2026-08-29)

PR #3980 review threads. Empty `appRoot` again maps to RxDir (`getAppRootFileList` contract); null still throws `"appRoot may not be null"`. `deleteFile` validates with `requireUnderBase` then operates on the original path so a symlink under RxDir is unlinked, not followed. `listFiles()` has same-line `// codeql[java/path-injection]`. `deleteDirectory` delegates to `deleteFile`. Tests: empty→RxDir, `deleteFile`/`deleteDirectory` happy+escape, symlink-not-target (assumed when the OS allows links), `RecoverableFile` reject/recover. Cross-platform: `@TempDir`, `File.separator`, thread RxDir `@AfterEach`.
