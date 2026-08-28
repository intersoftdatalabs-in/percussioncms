# Erlang review — #3939 system/src/main data leftover IPS*Errors typed ErrorCodes

**Scope:** uncommitted branch `fix/issue-3939-data-errorcodes` vs `origin/main`  
**Memory patterns hit:** typed `*ErrorCodes` + existing/additive `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; additive constructors (C2); do not delete `IPS*Errors` interfaces; tests must exercise production exception types; int-only log/status APIs use `.numericCode()`.  
**Cross-platform path checklist:** N/A (no new filesystem path joins).

## Summary

Parent #2616 leftover slice. Remaining origin/main allow-list production `IPS*Errors` sites under `system/src/main/java/com/percussion/data/` (including `data/macro` and `data/vfs`) now construct typed `DataErrorCodes` / `BackEndErrorCodes` / `ServerErrorCodes` / `ExtensionErrorCodes` / `HttpErrorCodes` / `UtilErrorCodes` / `SecurityErrorCodes`. Additive `IPSErrorCode` constructors on `PSEvaluationException`, `PSIllegalArgumentException`, `PSSqlException` (utils), `PSInvalidRequestTypeException`, and the 4-arg `PSSystemValidationException`. Residual allow-list shrunk by those exact 43 paths. Dual-write skip tests cover leftover non-auditable catalog codes; `SESS_NOT_AUTHORIZED` remains auditable. HTTP status and log APIs use `.numericCode()`. `PSVirtualApplicationDirectory` dropped unused `implements IPSDataErrors`. No product UI/config surface.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

### Notes (non-blocking)

- Int-only APIs (`PSLogServerWarning`, `PSLogExecutionPlan`, `PSNonFatalError`, `setStatus`, `printWarnMsg`, `PSResponseSendError`, `logCacheEntryException`, `logFullUserActivityAction`, `errorCode =`, `getErrorCode() ==`) correctly use `.numericCode()` so typed construction is retained only on exception throw sites.
- `PSSqlException` is not a `PSException` subclass; tests assert `getTypedErrorCode()` / `isAuditable()` on that type directly.
- C2: additive constructors only; grepped `extends` / anonymous subclasses (`PSMinorValidationException` already had typed ctors). Existing int constructors unchanged.

### Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Tests do not assert Unix-only absolute path shapes
- [x] Temp files / OS temp hardcodes not used
- [x] Line-ending sensitive assertions not used
