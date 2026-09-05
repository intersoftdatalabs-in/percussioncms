# Erlang review — fix/codeql-alert-2039-path-injection

**Date:** 2026-09-05  
**Base:** origin/main  
**Recommendation:** approve  
**Gate:** pass  
**May commit/push:** yes

## Summary

Runtime path-injection barrier for `PSConfigService` package config files (CodeQL #2039–#2044): `requireSafeFileName` on package names, `requireUnderBase(RxDir)` before File API, sink-line annotations, model pack, query-filter, behavioral tests.

## Scope

- `deployer/src/main/java/com/percussion/rx/config/impl/PSConfigService.java`
- `deployer/src/test/java/com/percussion/rx/config/impl/PSConfigServicePathInjectionTest.java`
- CodeQL config/models + `suppressions.md`

Cross-platform path review: tests join with `Path.resolve` / `File.separator`; no hardcoded OS separators for filesystem paths. `requireUnderBase` canonical containment is portable.

## Issues

None (no bug / missing behavioral test / non-portable I/O).
