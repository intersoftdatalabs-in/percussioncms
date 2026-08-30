# Erlang review — #4016 conn leftover IPS*Errors typed ErrorCodes

**Scope:** uncommitted branch `fix/issue-4016-conn-leftover-errorcodes` vs `origin/main`  
**Memory patterns hit:** typed `*ErrorCodes` + `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; do not delete `IPS*Errors` interfaces; int-only reconstruction paths use `.numericCode()`; exact production exception types in tests; incomplete change-class closure (allow-list companion).  
**Cross-platform path checklist:** N/A (no new filesystem path joins; tests construct exceptions / XML in-memory).

## Summary

Parent #2616 leftover slice: remaining `system/src/main/java/com/percussion/conn/` production `IPS*Errors` call-sites (`PSDesignerConnection`, `PSServerException`) now use typed `ConnectionErrorCodes` and `ServerErrorCodes.RAW_DUMP`. `PSServerException(Exception)` constructs with typed `UNKNOWN_SERVER_EXCEPTION`. Remote XML reconstruction still uses `SERVER_GENERATED_EXCEPTION.numericCode()` as the mutable int default (protocol errorCode can be overwritten). Residual allow-list shrunk by those two exact paths. Dual-write skip tests cover leftover non-auditable catalog codes; leftover `UNAUTHORIZED` remains dual-write eligible. Production exception type `PSServerException` retains typed codes. `createExceptionFromXml` default numeric code is covered. No product UI/config surface.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

## Verification

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 2658, Failures: 0, Skipped: 247 (`PSConnLeftoverErrorCodesSliceTest` 4/4)
- `python scripts/verify-no-bare-ipserrors.py` — PASS
- `python -m pytest scripts/test_verify_no_bare_ipserrors.py -q` — 21 passed

## Notes

- C2: no public/protected signature change, not `final`/`sealed`.
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion); dual-write skip when non-auditable.
