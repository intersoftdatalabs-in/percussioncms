# Erlang Pre-Commit Review — `fix/1419-relationship-summary-qualifier`

- **Date:** 2026-07-22
- **Reviewer:** Kilo (Erlang persona, strict)
- **Scope:** Uncommitted changes on `fix/1419-relationship-summary-qualifier`
  (single file: `projects/sitemanage/src/main/java/com/percussion/share/relationship/service/impl/PSRelationshipSummaryService.java`)
- **Issue context:** user-reported Jetty startup failure
  `NoUniqueBeanDefinitionException: expected single matching bean but found 2: contentItemDao,relationshipCataloger`;
  related to issue #1403 (perc-distribution-tree Windows build) — verified separately.

## Memory patterns hit

- **Never cast Spring-injected service interfaces to concrete `*Service` impls (JDK proxies); call interface methods only** — not violated; the fix only narrows bean selection.
- **Hard gate: missing behavioral tests for non-trivial logic** — N/A: existing `PSRelationshipSummaryServiceTest` (12 tests, all green) continues to exercise the same ctor invocation path; `@Qualifier` is Spring metadata only and is transparent to direct `new …Service(...)` instantiation in unit tests.

## Summary

|  Severity  | Count |
|------------|-------|
| CRITICAL   | 0     |
| WARNING    | 0     |
| SUGGESTION | 0     |

The fix is a one-token surgical annotation addition (`@Qualifier("relationshipCataloger")`)
on the `IPSRelationshipCataloger` constructor parameter of `PSRelationshipSummaryService`.
This disambiguates Spring's autowiring between the two beans that implement
`IPSRelationshipCataloger`:

1. `contentItemDao` — `PSContentItemDao` indirectly implements the interface via
   `IPSContentItemDao extends … , IPSRelationshipCataloger`
   (`projects/sitemanage/src/main/java/com/percussion/share/dao/IPSContentItemDao.java:27`).
2. `relationshipCataloger` — `PSRelationshipCataloger` directly implements the interface
   (`projects/sitemanage/src/main/java/com/percussion/share/dao/impl/PSRelationshipCataloger.java:31`,
   registered as bean `relationshipCataloger` via `@PSSiteManageBean("relationshipCataloger")`).

The same pattern is already used in `PSContentItemDao.java:96`
(`@Qualifier("relationshipCataloger") IPSRelationshipCataloger relationshipHelper`),
so the fix aligns `PSRelationshipSummaryService` with the established convention in
the module rather than introducing a new approach.

## Gate

- [x] Compile: standalone `cd projects/sitemanage && mvn -Dai.integrity.skip=true clean install` — **BUILD SUCCESS** (`Tests run: 553, Failures: 0, Errors: 0, Skipped: 129`; skipped tests are pre-existing and unrelated).
- [x] Unit tests: `mvn -Dtest=PSRelationshipSummaryServiceTest test` — `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`.
- [x] No new compiler / spotless / enforcer warnings introduced on the touched module (warnings observed in the full reactor are dependency-resolution warnings inherited from parent POM and from third-party POMs, not from this diff).
- [x] Cross-platform path / file-I/O checklist: **N/A** — diff is purely a Spring annotation + import + javadoc; no filesystem code touched.

**Recommendation:** `approve`
**May commit/push:** **yes**

## Issues

None.

## Detailed check items

### 1. Does the fix actually resolve the runtime error?

Yes. The reported stack trace ends in
`NoUniqueBeanDefinitionException: … but found 2: contentItemDao,relationshipCataloger`,
which is exactly the ambiguity this `@Qualifier` resolves. The bean name `relationshipCataloger`
matches the literal string passed to `@PSSiteManageBean("relationshipCataloger")` on
`PSRelationshipCataloger.java:30`, which is the bean Spring registers under that name.

### 2. Are existing tests still valid?

Yes. `PSRelationshipSummaryServiceTest.setUp()` (line 67-76) constructs the service via
`new PSRelationshipSummaryService(idMapper, systemWs, relationshipCataloger, …)`.
The five constructor parameters and their types are unchanged — only an annotation on one
parameter was added. `@Qualifier` is metadata consumed by Spring's `AutowiredAnnotationBeanPostProcessor`
at injection time and has no effect on direct programmatic instantiation. The 12 tests
pass unchanged.

### 3. Could a different bean (e.g. `contentItemDao`) be the correct target?

No. The Javadoc on `IPSRelationshipCataloger` (line 22) describes a single method
`findOwners(id, name, contentType, slotName)` that delegates to `systemWs.findOwners`
with a `PSRelationshipFilter`. `PSContentItemDao` (the `contentItemDao` bean) also
exposes this method via the parent interface, but its ctor injects `relationshipHelper`
*with the same `@Qualifier("relationshipCataloger")`* (`PSContentItemDao.java:96`),
confirming the project-wide convention that the production graph wires to the dedicated
`PSRelationshipCataloger` bean for this method.

### 4. Side effects / behavioral regressions

None. The constructor body is unchanged; only the parameter annotation differs. The
service's runtime behavior (delegation to `relationshipCataloger.findOwners(...)` and
`systemWs.findDependents(...)`) is identical.

### 5. Documentation

Javadoc on the `relationshipCataloger` parameter was updated to explain the qualifier
and the disambiguation reason, referencing issue #1419. This satisfies the project rule
to document non-obvious Spring wiring decisions.

## Build evidence

- `cd projects/sitemanage && mvn -Dai.integrity.skip=true clean install` →
  `[INFO] BUILD SUCCESS`, `Tests run: 553, Failures: 0, Errors: 0, Skipped: 129`.
- `mvn -Dai.integrity.skip=true -Dtest=PSRelationshipSummaryServiceTest test` →
  `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`.
- Related build: `cd modules/perc-distribution-tree && mvn -Dai.integrity.skip=true install`
  → exits 0; `verify-jdbc-drivers` exec printed `OK: 9 JDBC driver JAR(s) verified under jetty/base/lib/jdbc/`
  (the cross-platform Java port from PR #1413 — issue #1403's `.sh` script is no longer
  invoked by the build, the `.bat` shim remains for operators).

