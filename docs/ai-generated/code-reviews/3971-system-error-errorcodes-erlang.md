# Erlang review — #3971 system error leftover IPS*Errors typed ErrorCodes

**Scope:** uncommitted branch `fix/issue-3971-system-error-ipserrors-errorcodes` vs `origin/main`  
**Memory patterns hit:** typed `*ErrorCodes` + `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; do not delete `IPS*Errors` interfaces; int-only APIs (`PSLogSubMessage`, `PSErrorManager.createMessage`/`getErrorText`, `PSLogServerWarning`) use `.numericCode()`; exact production exception types in tests (not supertypes); incomplete change-class closure (allow-list companion).  
**Cross-platform path checklist:** N/A (no new filesystem path joins; tests construct exceptions in-memory).

## Summary

Parent #2616 leftover slice: 26 `system/src/main/java/com/percussion/error/` production `IPS*Errors` sites now use typed `ServerErrorCodes` / `BackEndErrorCodes` / `DataErrorCodes` / `CatalogErrorCodes` / `HttpErrorCodes` / `XmlErrorCodes`. `PSErrorException` constructs with typed `ServerErrorCodes.WRAPPED_LOG_ERROR`. Residual allow-list shrunk by those exact 26 paths. Dual-write skip tests cover leftover non-auditable catalog codes; leftover auditable authorization codes remain dual-write eligible. Production exception type `PSErrorException` retains typed code. No product UI/config surface.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

## Verification

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 2582, Failures: 0, Skipped: 246 (`PSErrorLeftoverErrorCodesSliceTest` 4/4)
- `python scripts/verify-no-bare-ipserrors.py` — PASS
- `python -m pytest scripts/test_verify_no_bare_ipserrors.py -q` — 19 passed

## Notes

- C2: no public/protected signature change, not `final`/`sealed`. Existing `extends PSErrorException` webservices types still use the same `PSLogError` constructors.
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.
- Comment-only historical `IPS*Errors` mentions in leftover log-error builders are ignored by the freeze gate.

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion); dual-write skip when non-auditable.
