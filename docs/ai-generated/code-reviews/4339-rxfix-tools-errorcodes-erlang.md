# Erlang review: #4339 RxFix Tools IPS*Errors typed ErrorCodes

**Branch:** `fix/issue-4339-rxfix-tools-errorcodes`  
**Base:** `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** leftover `IPS*Errors` → `*ErrorCodes` companions (dual-write skip, exact exception types, allow-list shrink); behavioral tests for non-trivial catalog construction.

## Summary

Parent #2616 leftover slice. Two RxFix Tools production paths retyped to existing catalogs:

- `PSFixNavigation` — `NavigationErrorCodes.NAVIGATION_SERVICE_CANNOT_FIND_ANY_NAVONS` via compiled `cannotFindAnyNavons()` (`PSNavException`). The rest of `fix(boolean)` remains disabled (`UnsupportedOperationException`).
- `PSJdbcTableCheck` — `TableFactoryErrorCodes.SQL_CONNECTION_FAILED` on the live `PSJdbcTableFactoryException` constructor.

Allow-list shrinks those two exact paths. Residual remains `system/Testing/cms/HttpItemCopier.java` (#4338). Dual-write skip: both leftover codes are non-auditable; TableFactory ints are not flat-registered (collision with `ServerErrorCodes`).

## Issues

None blocking.

### Nit

- `cannotFindAnyNavons()` is package-visible solely so the typed constructor is compiled (the original throw lives in a comment block). Acceptable for this leftover class.

## Tests

- `PSRxFixToolsLeftoverErrorCodesSliceTest` — numeric parity, dual-write skip, exact `PSNavException` / `PSJdbcTableFactoryException` types.
- `scripts/test_verify_no_bare_ipserrors.py` — converted RxFix paths not re-listed; empty-allowlist still fails on HttpItemCopier.

## Cross-platform path checklist

N/A — no new filesystem path I/O.

## Product documentation

N/A — internal typed error-code retype; not operator/user/API-facing.

## C2 reverse-deps

Did not apply: no `final`/`sealed` on shared types; no public/protected signature change on types other modules compile against (`cannotFindAnyNavons` is package-private on a Tools class).
