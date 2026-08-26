# Erlang review — #3847 system/services leftover IPS*Errors typed ErrorCodes

- Branch: `fix/issue-3847-system-services-errorcodes`
- Base: `origin/main`
- Date: 2026-08-26
- Recommendation: **approve**
- Gate: **May commit/push: yes**
- Memory patterns hit: behavioral tests for changed logic; incomplete change-class closure (typed exception ctors + allow-list shrink + dual-write skip)

## Summary

Parent #2616 leftover slice. 39 `system/services` production `IPS*Errors` call-sites retyped to existing `*ErrorCodes` enums (plus new `ServiceSecurityErrorCodes` for package-local `com.percussion.services.security.IPSSecurityErrors`, and `ContentErrorCodes.MISSING_KEYWORD`). `PSBaseException` gained additive `IPSErrorCode` constructors with `private static requireCode` (must stay private so webservices subclasses keep their own private helpers). Residual allow-list shrunk for those exact paths. `verify-no-bare-ipserrors.py` PASS.

## Change class

Typed ErrorCodes production call-site conversion (package-local leftover catalogs).

Companions present: exception typed ctors; allow-list shrink; dual-write skip tests (enum-direct, not `find(int)` — WF collisions); representative typed throw tests.

## Cross-platform path checklist

N/A — no filesystem path / I/O changes.

## Issues

None.

## Verification

- `cd modules/utils && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 394, Failures: 0, Skipped: 9
- `cd modules/perc-auditlog && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 311, Failures: 0
- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 2383, Failures: 0, Skipped: 241 (`SystemServicesLeftoverErrorCodesSliceTest` 6/6)
- `python scripts/verify-no-bare-ipserrors.py` — PASS

## Notes

- C2: constructors are additive. No `final`/`sealed`. Grep found no anonymous `new <Exception>() {` of the retyped types.
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.
- Dual-write skip tests go through the enum (`isAuditable()`), not `LegacyErrorCodeRegistry.find(int)`, because leftover ints 1–14 collide with `WorkflowErrorCodes`.
- `PSCmsException(IPSErrorCode, Throwable)` is a one-line companion in `system/src/main` so `PSCmsObjectMgr` can throw `FAILED_GET_REL_CONFIG_FROM_XML` with a cause. Not a leftover cms/data/server conversion.

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion); collision-safe registry (do not flatten leftover package-local ints).
