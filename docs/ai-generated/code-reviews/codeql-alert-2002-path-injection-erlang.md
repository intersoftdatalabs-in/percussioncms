# Erlang review — fix/codeql-alert-2002-path-injection

**Scope:** uncommitted vs `HEAD` on `fix/codeql-alert-2002-path-injection`.
**Date:** 2026-08-28
**Recommendation:** approve
**Gate:** May commit/push: yes (after `system` standalone `mvnw clean install`)
**Memory patterns hit:** behavioral rejection tests; trusted root is object-store dir / RxDir, not a parent of untrusted input; portable `File.separator` / `@TempDir` / `PathUtils.setThreadOnlyRxDir`

## Summary

Runtime barriers for object-store application XML files (`resolveApplicationXmlFile` → `requireUnderBase`) and lock streams (`requireUnderRxDir` → RxDir containment). Sink-line annotations on `exists` / stream constructors. Query-filter + model pack + `suppressions.md`.

## Cross-platform path checklist

- [x] No hardcoded filesystem separators for joins
- [x] `requireUnderBase` + `Path.startsWith` in tests
- [x] Thread-local RxDir restored in `@AfterEach`

## Issues

None (hard-gate). Focused tests: 4 passed.
