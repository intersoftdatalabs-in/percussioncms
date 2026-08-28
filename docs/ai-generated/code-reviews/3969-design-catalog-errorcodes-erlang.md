# Erlang review — #3969 system design.catalog leftover IPS*Errors typed ErrorCodes

**Scope:** uncommitted branch `fix/issue-3969-design-catalog-typed-errorcodes` vs `origin/main`  
**Memory patterns hit:** typed `*ErrorCodes` + `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; do not delete `IPS*Errors` interfaces; int-only APIs (`PSLogServerWarning`, `PSCatalogRequestError`) use `.numericCode()`; allow-list companion shrink.  
**Cross-platform path checklist:** N/A (no new filesystem path joins; tests construct exceptions in-memory).

## Summary

Parent #2616 leftover slice: 22 `system/src/main/java/com/percussion/design/catalog/` production `IPS*Errors` sites now construct typed `CatalogErrorCodes` / `ServerErrorCodes`. Residual allow-list shrunk by those exact 22 paths. Dual-write skip tests cover leftover non-auditable catalog protocol codes and `RESPONSE_SEND_ERROR`. Production exception type is `PSIllegalArgumentException` with typed codes retained. `IPSCatalogErrors` interface is not deleted. Out of scope (`design.objectstore`, `design.server`, `com.percussion.error`) left on the allow-list. No product UI/config surface.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

## Verification

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 2581, Failures: 0, Skipped: 246 (`PSDesignCatalogLeftoverErrorCodesSliceTest` 3/3)
- `python scripts/verify-no-bare-ipserrors.py` — PASS
- `python -m pytest scripts/test_verify_no_bare_ipserrors.py -q` — 20 passed

## Notes

- C2: call-site retype only. No `final`/`sealed`, no public/protected signature change. Grep: no `extends PSIllegalArgumentException`; no anonymous `new PSIllegalArgumentException() {`.
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.
- `IPSCatalogErrors.CATALOG_EXCEPTION` and `IPSServerErrors.RESPONSE_SEND_ERROR` stay int-only at `PSCatalogRequestError` / `PSLogServerWarning` via `.numericCode()`.

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion); dual-write skip when non-auditable.
