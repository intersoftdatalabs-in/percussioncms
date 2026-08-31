# Erlang review — #4013 system cx leftover IPS*Errors typed ErrorCodes

**Scope:** uncommitted branch `fix/issue-4013-cx-errorcodes` vs `origin/main`  
**Memory patterns hit:** typed `*ErrorCodes` + `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; do not delete `IPS*Errors` interfaces; allow-list companion shrink; behavioral tests on exact production exception types.  
**Cross-platform path checklist:** N/A (no new filesystem path joins; tests construct XML/exceptions in-memory).

## Summary

Parent #2616 leftover slice: four `system/src/main/java/com/percussion/cx/` production `IPSContentExplorerErrors` sites (`PSFont`, `PSOption`, `PSOptions`, `PSUserOptions`) now construct typed `ContentExplorerErrorCodes`. Residual allow-list shrunk by those exact four paths. Dual-write skip tests cover leftover non-auditable `MISC_PROCESSING_OPTIONS_ERROR` / `PSCLASS_INSTANTIATION_ERROR`. Production exception type is `PSContentExplorerException` with typed codes retained. `IPSContentExplorerErrors` interface is not deleted. Out of scope (`conn`, `mail`, `server` mega-tree) left on the allow-list. No product UI/config surface.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

## Verification

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 2661, Failures: 0, Skipped: 247 (`PSCxLeftoverErrorCodesSliceTest` 7/7)
- `python scripts/verify-no-bare-ipserrors.py` — PASS
- `python scripts/test_verify_no_bare_ipserrors.py` — 22 passed

## Notes

- C2: call-site retype only. No `final`/`sealed`, no public/protected signature change. Grep: no `extends PSContentExplorerException`; no anonymous `new PSContentExplorerException() {`.
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.
- `PSOption.fromXml` still wraps `makeObject` failures as `MISC_PROCESSING_OPTIONS_ERROR` (legacy wrap kept). Tests cover that public wrap plus the private `makeObject` typed `PSCLASS_INSTANTIATION_ERROR` site.

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion); dual-write skip when non-auditable.
