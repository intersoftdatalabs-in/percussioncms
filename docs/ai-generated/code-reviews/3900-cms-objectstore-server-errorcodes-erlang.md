# Erlang review — #3900 cms.objectstore.server leftover IPS*Errors typed ErrorCodes

**Scope:** uncommitted branch `fix/issue-3900-cms-objectstore-server-errorcodes` vs `origin/main`  
**Memory patterns hit:** typed `*ErrorCodes` + `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; additive constructors (C2); do not delete `IPS*Errors` interfaces; `getErrorCode() == IPS*Errors.X` comparisons became `== *ErrorCodes.X.numericCode()`; SQL_EXCEPTION_WRAPPER (1002) maps to `ServerErrorCodes.RAW_DUMP` (collision with Cms catalog).  
**Cross-platform path checklist:** N/A (no new filesystem path joins; tests use dummy strings / in-memory XML, not OS paths).

## Summary

Parent #2616 leftover slice: thirteen `system/src/main/java/com/percussion/cms/objectstore/server/` production `IPS*Errors` sites now construct typed `CmsErrorCodes` / `ServerErrorCodes` / `PathItemErrorCodes` / `DataErrorCodes` / `SecurityErrorCodes` / `HttpErrorCodes` / `ServerWebServicesErrorCodes`. Additive `IPSErrorCode` constructor on `PSFieldValidationException` (no subclasses). `getItemFromCache` takes `IPSErrorCode` so typed codes survive. Residual allow-list shrunk by those exact 13 paths. Dual-write skip tests cover leftover non-auditable catalog codes; `PSIdGeneratorExit` exercises a production throw path. No product UI/config surface.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

### Notes (non-blocking)

- `IPSCmsErrors.SQL_EXCEPTION_WRAPPER` (1002) is not a `CmsErrorCodes` constant (collision); production uses `ServerErrorCodes.RAW_DUMP` matching the catalog comment.
- `IPSCmsErrors.FOLDER_ERROR_MSG` is owned by `PathItemErrorCodes`.
- cms builders / handlers / objectstore+client remain on the allow-list (siblings #3882–#3884 / cluster PR #3904).
