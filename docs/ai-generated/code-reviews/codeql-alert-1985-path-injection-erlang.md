# Erlang review — fix/codeql-alert-1985-path-injection

**Scope:** uncommitted vs `HEAD` on `fix/codeql-alert-1985-path-injection` (vs `origin/main`).
**Date:** 2026-08-28
**Recommendation:** approve
**Gate:** May commit/push: yes (after `system` standalone `mvnw clean install`)
**Memory patterns hit:** behavioral tests for sanitizer rejection; path containment uses trusted virtual-root, not a parent derived from untrusted catalog; portable `Path`/`File.separator`/`@TempDir`

## Summary

Runtime path-injection barrier for XML JDBC catalog resolution (CodeQL #1985–#1987). `PSVirtualApplicationDirectory.getPhysicalPath` now rejects `..` escape via normalize + `Path.startsWith`, then `PSPathInjectionGuard.requireUnderBase` when the virtual root exists. Driver re-applies `requireUnderBase` on the returned `File`. Metadata sinks get same-line `// codeql[java/path-injection]`; DTD table join uses `requireUnderBase`. Query-filter + model pack + `suppressions.md` complete ladder steps 2–4.

## Cross-platform path checklist

- [x] No new `".../" +` / `"...\\" +` filesystem joins
- [x] New path logic uses `Path` / `Files` / `requireUnderBase`
- [x] Tests use `@TempDir` and `File.separator`; no Unix-only absolute shapes
- [x] Containment is `Path.startsWith` / canonical `requireUnderBase`, not raw string prefix

## Issues

None (hard-gate). Focused tests: 8 passed (`PSVirtualApplicationDirectoryPathInjectionTest`, `PSFileSystemDriverPathInjectionTest`).
