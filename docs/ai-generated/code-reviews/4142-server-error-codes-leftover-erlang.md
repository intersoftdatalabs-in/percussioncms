# Erlang review — #4142 leftover IPSServerErrors typed ServerErrorCodes

**Scope:** uncommitted branch `fix/issue-4142-server-error-codes-leftover` vs `origin/main`  
**Memory patterns hit:** typed `*ErrorCodes` + `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; do not delete `IPS*Errors` interfaces; exact production exception types in tests; incomplete change-class closure (allow-list companion).  
**Cross-platform path checklist:** N/A (no new filesystem path joins; tests construct exceptions / in-memory DTD bytes).

## Summary

Parent #2616 leftover slice: remaining named `system/src/main` production `IPSServerErrors` throw sites (`PSDtdTree`/`PSDtdParser`, `PSWorkFlowUtils`, `PSRelationshipUtils`, `PSDate`, `PSCms`) plus close peers (`PSPromote`, fastforward `PSUtils`) now use typed `ServerErrorCodes`. Additive `IPSErrorCode` constructors on utils `PSCatalogException` retain `getTypedErrorCode()` / `isAuditable()`. `IPSServerErrors` stays as the numeric bridge. Residual allow-list shrunk for fully converted paths; `PSDtdTree` / `PSPromote` / `PSUtils` remain listed for other `IPS*Errors` families. Dual-write skip tests cover leftover non-auditable catalog codes. No product UI/config surface.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

## Verification

- `cd modules/utils && ../../mvnw.cmd clean install` — BUILD SUCCESS (`PSExceptionTypedConstructorSliceTest` Tests run: 3, Failures: 0)
- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS (`PSServerErrorsLeftoverErrorCodesSliceTest` Tests run: 6, Failures: 0)
- `python scripts/verify-no-bare-ipserrors.py` — PASS
- `python -m pytest scripts/test_verify_no_bare_ipserrors.py -q` — 23 passed

## Notes

- C2: additive public constructors on `PSCatalogException` (utils). Grep found no `extends PSCatalogException` / anonymous subclasses. Not `final`/`sealed`. Reverse-deps not rebuilt (additive API).
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.
- `PSWorkFlowUtils` production missing-IR path is typed the same as peers; class static init (`PathUtils.getRxDir`) is not unit-testable without a server rxdir, so that method is covered by constructed `PSNotFoundException` plus sibling production throws.

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion); dual-write skip when non-auditable.
