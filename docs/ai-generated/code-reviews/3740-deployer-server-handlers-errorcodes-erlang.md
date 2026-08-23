# Erlang review — #3740 deployer server/handlers IPS*Errors typed ErrorCodes

**Date:** 2026-08-23  
**Branch:** `fix/issue-3740-deployer-server-handlers-errorcodes`  
**Base:** stacked on #3739 / PR #3753 (`origin/feat/issue-3739-deployer-catalog-client-errorcodes`); PR targets `main`  
**Reviewer:** Erlang (pre-commit, independent of implementer)

## Summary

Parent #2616 slice 2/3. Leftover deployer **server jobs/helpers + dependency handlers** production `IPS*Errors` int sites retyped to typed `*ErrorCodes` (`DeploymentErrorCodes`, `JobErrorCodes`, `SecurityErrorCodes`, `ServerErrorCodes`, `FilterServiceErrorCodes`). Additive `IPSErrorCode` constructors on `PSLockedException`, `PSDeployNonUniqueException`, `PSJobException`, `PSServerException`, and `PSAuthenticationFailedException`. Residual allow-list shrunk by those exact `deployer/.../server/` paths (70). Catalog/client remain on #3739; DCE/TableFactory and `PSDeployJexlUtils` remain later slices. No mass behavior change of deploy jobs.

## Recommendation

**approve**

## Gate

- Bugs: none found
- Behavioral tests: present (package-config production throws, typed lock/job/auth/server constructors, dual-write skip)
- Cross-platform paths: N/A (no filesystem path construction)
- Change-class companions: exception typed ctors + production retype + allow-list shrink + dual-write tests
- May commit/push: **yes** (after module `clean install` evidence)

## Issues

None.

## Notes

- Flat registry unchanged: leftover deploy ints 1–73 stay enum-only (WF/assembly/WS collision). Dual-write tests go through the enum, not `LegacyErrorCodeRegistry.find(int)`.
- Aliases mapped to existing enum names: `VERSION_LOWER_THEN_INSTALLED` → `VERSION_LOWER_THAN_INSTALLED`; `SERVER_VERSION_LOWER` → `SERVER_VERSION_MISMATCH`; `SERVER_VERSION_HIGHER` → `SERVER_BUILD_MISMATCH`. Numeric codes unchanged.
- `getErrorCode() == IPS*Errors.X` comparisons became `== *ErrorCodes.X.numericCode()` so int equality still holds for mixed typed/legacy construction.
- C2: constructors are additive. `PSAuthenticationFailedExException` extends `PSAuthenticationFailedException`; no anonymous `new <Type>() {` sites. system + deployer standalone install green.
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.
- Dual-write skip tests live in `perc-auditlog` (`CapturingAuditLogSink` is test-scoped and not on the deployer test classpath).

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion); collision-safe registry (do not flatten 1–73).
