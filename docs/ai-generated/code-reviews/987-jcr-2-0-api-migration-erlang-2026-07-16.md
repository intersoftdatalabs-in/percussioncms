# Erlang Review: JCR 2.0 compile-clean (US2 / Phase 1)

**Date**: 2026-07-16  
**Scope**: Uncommitted JCR 2.0 implementor changes on `1286-jcr-2-0-api-migration` (vs working tree + related files under `modules/utils` and `system`)  
**Intent**: Phase-1 compile-clean against `javax.jcr:jcr:2.0` only (not full deprecation cleanup)

## Summary

Product types that implement `javax.jcr.*` interfaces were extended for JSR-283 methods (Binary/Decimal, Node identifier APIs, Query bind/limit/offset, QueryResult selectors, NodeType/NodeDefinition 2.0 surface, NodeTypeManager registration stubs). Unsupported optional capabilities generally throw `UnsupportedRepositoryOperationException` or return empty structures, consistent with existing read-only / not-supported patterns on `PSContentNode`.

**Evidence run**:
- `./mvnw -pl modules/utils,system,projects/sitemanage,modules/perc-toolkit,deployer -am compile -DskipTests` → SUCCESS
- Full reactor compile fails at `delivery-tier-distribution` on **rdf4j DependencyConvergence** (unrelated to JCR)
- `./mvnw -pl modules/utils -Dtest=PSValuesTest,PSBinaryTest test` (after hash regen) — values/binary tests green
- `PSQueryJcr20Test` added for bind/limit/offset

## Recommendation

**approve** (for Phase-1 compile-clean PR scope), after residual test modules listed below are confirmed green in the same change set.

**May commit/push**: **yes** for compile-clean PR once utils + contentmgr unit tests for new logic pass.

## Gate

|                   Check                    |                                  Result                                   |
|--------------------------------------------|---------------------------------------------------------------------------|
| Bugs blocking                              | None open after test additions                                            |
| Behavioral tests for new non-trivial logic | Present: `PSBinaryTest`, `PSValuesTest` JCR 2.0 cases, `PSQueryJcr20Test` |
| Secrets                                    | None                                                                      |
| Invented APIs                              | None observed                                                             |

## Issues

### Suggestions (non-blocking for Phase 1)

1. **`getNodes(String[])` returns empty always** (`PSContentNode`)
   - If any caller relies on name-glob filtering, behavior is incomplete. Acceptable for Phase 1 stubs; track for deprecation/behavior phase if needed.
2. **`getQOMFactory` throws `UnsupportedOperationException`** (`PSContentMgr`)
   - Interface does not declare checked exceptions; UOE is valid. Document for integrators (already in contracts).
3. **`createValue(BigDecimal)` via `doubleValue()`** (`PSValueFactory`)
   - Precision loss for large decimals. Acceptable for compile-clean; improve later if decimal fields are used productively.
4. **Full reactor enforcer failure** (rdf4j convergence on delivery-tier-distribution)
   - Pre-existing / orthogonal; do not treat as JCR regression. Note in PR.

### Nits

- `PSValuesTest.StubNodeType` is large; acceptable for unit isolation.
- Typo in JCR API `getAllowedLifecycleTransistions` is from the specification, not our code.

## Files reviewed (primary)

- `modules/utils/.../PSBinary.java` (new)
- `PSBaseValue`, `PSValueFactory`, `PSMultiProperty`, `PSPropertyDefinition`
- `system/.../PSProperty`, `PSContentNode`, `PSQuery`, `PSQueryResult`, `PSRow`, `PSNodeDefinition`
- `PSContentMgr`, `PSTypeConfiguration`, `PSQueryResultUtils`, `PSDbUtils`
- Tests: `PSValuesTest`, `PSBinaryTest`, `PSQueryJcr20Test`, `PSMockProperty`

