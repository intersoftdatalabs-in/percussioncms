# Erlang review — #3940 system security leftover IPS*Errors typed ErrorCodes

**Scope:** uncommitted branch `fix/issue-3940-security-errorcodes` vs `origin/main`  
**Memory patterns hit:** typed `*ErrorCodes` + `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; additive constructors (C2); do not delete `IPS*Errors` interfaces; `getErrorCode() == IPS*Errors.X` comparisons remain int-equal via `.numericCode()`; int-only APIs (`PSLogServerWarning`, `PSErrorManager.createMessage`/`getErrorText`, `PSConsole.printMsg`) use `.numericCode()`.  
**Cross-platform path checklist:** N/A (no new filesystem path joins; tests construct exceptions in-memory).

## Summary

Parent #2616 leftover slice: 31 `system/src/main/java/com/percussion/security/` production `IPS*Errors` sites now construct typed `SecurityErrorCodes` / `ServerErrorCodes`. Additive `IPSErrorCode` constructors on `PSSecurityException`. Residual allow-list shrunk by those exact 31 paths. Dual-write skip tests cover leftover non-auditable catalog codes; leftover auditable SEC codes remain dual-write eligible. Production exception types retain typed codes. No product UI/config surface.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

## Verification

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 2578, Failures: 0, Skipped: 246 (`PSSecurityLeftoverErrorCodesSliceTest` 3/3)
- `python scripts/verify-no-bare-ipserrors.py` — PASS
- `python -m pytest scripts/test_verify_no_bare_ipserrors.py -q` — 19 passed

## Notes

- C2: constructors are additive. No `final`/`sealed`. Grep found no `extends PSSecurityException` and no anonymous `new PSSecurityException() {`.
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.
- `IPSSecurityErrors.DATA_ENCRYPTION_ERROR_MSG` maps to `SecurityErrorCodes.DATA_ENCRYPTION_ERROR` (same 9017).
- `IPSServerErrors.RAW_DUMP` / `RESPONSE_SEND_ERROR` on the two ACL exits and encryption handler map to `ServerErrorCodes`.

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion); dual-write skip when non-auditable.
