# Erlang review: #4144 managednav NavigationErrorCodes leftover

- **Branch:** `fix/issue-4144-managednav-navigation-error-codes`
- **Base:** `origin/main`
- **Scope:** uncommitted managednav leftover retype vs `HEAD` / `origin/main`
- **Date:** 2026-09-02
- **Memory patterns hit:** incomplete change-class closure (leftover typed-code companions); missing behavioral tests; public API shape (`PSNavException` ctor overloads)

## Summary

`PSManagedNavService` production throw sites now construct `PSNavException` with `NavigationErrorCodes` instead of bare `IPSNavigationErrors` ints. Typed constructors were added on `PSNavException` matching existing int overloads (including `(code, Object)` so `Exception` remains a message argument, not a cause). `IPSNavigationErrors` stays as the compile-time numeric bridge. Dual-write skip is covered because every `NavigationErrorCodes` constant is non-auditable.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

N/A — no filesystem path / I/O changes.

## Issues

None (hard-gate).

### Notes (not blocking)

- `system/Tools/RxFix/.../PSFixNavigation.java` still uses `IPSNavigationErrors.NAVIGATION_SERVICE_CANNOT_FIND_ANY_NAVONS` (out of scope per issue).
- Added `PSNavException(IPSErrorCode, …)` overloads; no `final`/`sealed`; no subclasses or anonymous subclasses found. Existing int constructors unchanged so reverse-deps keep compiling.
- Product-docs N/A (internal error-catalog retype, not operator-facing).
- Playwright N/A (no UI).
