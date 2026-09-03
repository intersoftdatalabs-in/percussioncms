# Erlang review — #4196 deployer PSDeployJexlUtils IPSDeploymentErrors typed ErrorCodes

**Date:** 2026-09-02  
**Branch:** `fix/issue-4196-deploy-jexl-errorcodes`  
**Base:** `origin/main`  
**Reviewer:** Erlang (pre-commit, independent of implementer)

## Summary

Parent #2616 leftover slice. Production throw in `deployer/.../jexl/PSDeployJexlUtils.java` retyped from `IPSDeploymentErrors.UNEXPECTED_ERROR` to `DeploymentErrorCodes.UNEXPECTED_ERROR`. Legacy interface remains the numeric bridge (`numericCode()` == 5). Residual allow-list drops this exact path; deployer production leftovers are fully converted. Dual-write skip is asserted via `isAuditable() == false` on the non-auditable catalog code. Out of scope: servletutils `PSTomcatUtils` (#4195), `PSDtdTree` IPSXmlErrors (#4197).

## Recommendation

**approve**

## Gate

- Bugs: none found
- Behavioral tests: present (`PSDeployJexlUtilsTypedErrorCodeSliceTest` — parse failure typed code, blank IAE, happy-path id extract, numeric-bridge parity)
- Cross-platform paths: N/A for Java; scripts tests use posix allow-list paths (no OS path joins)
- Change-class companions: production retype + dual-write skip tests + allow-list shrink + gate pytest
- May commit/push: **yes** (`cd deployer && ../mvnw.cmd clean install` BUILD SUCCESS; pytest 27 passed)

## Issues

None.

## Notes

- Cause chaining still uses `e.getCause()` (often null for `ParseException`); behavior preserved, not part of this retype.
- C2: `getIdsFromBinding` signature unchanged; no `final`/`sealed`; no reverse-dep blast radius.
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.
- Auditable dual-write still covered by existing `PSDeployExceptionTest` lock codes; this class has no auditable throw.

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion).
