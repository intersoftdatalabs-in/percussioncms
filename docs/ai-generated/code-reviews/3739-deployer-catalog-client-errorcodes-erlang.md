# Erlang review — #3739 deployer catalog/client IPS*Errors typed ErrorCodes

**Date:** 2026-08-23  
**Branch:** `feat/issue-3739-deployer-catalog-client-errorcodes`  
**Base:** `origin/main`  
**Reviewer:** Erlang (pre-commit, independent of implementer)

## Summary

Parent #2616 slice 1/3. Leftover deployer **catalog + client + objectstore + PSDeployService** production `IPSDeploymentErrors` int sites retyped to `DeploymentErrorCodes`. `PSDeployException` gained `IPSErrorCode` constructors, `getTypedErrorCode()`, and `isAuditable()` (peer of `PSException`). Residual allow-list shrunk by those exact paths. Server/handlers, DCE/TableFactory, and deleting `IPS*Errors` interfaces remain out of scope.

## Recommendation

**approve**

## Gate

- Bugs: none found
- Behavioral tests: present (typed construction, catalog/id-map production throws, dual-write skip)
- Cross-platform paths: N/A (no filesystem path construction)
- Change-class companions: exception typed ctors + production retype + allow-list shrink + dual-write tests
- May commit/push: **yes** (after module `clean install` evidence)

## Issues

None.

## Notes

- Flat registry unchanged: leftover deploy ints 1–73 stay enum-only (WF/assembly/WS collision). Dual-write tests go through the enum, not `LegacyErrorCodeRegistry.find(int)`.
- `PSDeployException` XML round-trip still drops typed metadata (legacy int only). Callers that reconstruct from XML keep int `getErrorCode()`; new tests cover typed construction, not XML restore.
- C2: constructors are additive. Subclasses `PSLockedException` / `PSDeployNonUniqueException` still call `super(int, Object[])`. No anonymous `new PSDeployException() {` sites.
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.
- Dual-write skip tests live in `perc-auditlog` (`CapturingAuditLogSink` is test-scoped and not on the deployer test classpath).

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion); collision-safe registry (do not flatten 1–73).
