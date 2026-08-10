# Erlang review: fix/issue-2016-utils-javac-warnings-batch1

## Summary

Batch-1 real fixes for all **23** project-default `-Xlint` javac warnings on `modules/utils` **main** sources (rawtypes, this-escape, removal deprecations, fallthrough, try, serial, missing `@Deprecated`). Main compile is now warning-clean under project Xlint. Module `mvnw clean install` green (311 tests, 0 failures). Residual: ~100 test-source javac warnings + suppressed main rawtypes/unchecked for later batches.

## Scope

- Branch: `fix/issue-2016-utils-javac-warnings-batch1`
- Module: `modules/utils` only
- Parent tracker: #2200 / issue #2016
- Prior patterns: final-class this-escape (#2302, #2287), direct field init (#2285), ArrayList serial fields (#2288)

## Recommendation

**approve**

## Gate

May commit/push: **yes**

## Cross-platform path review

N/A for new I/O. Existing `Paths.get(".")` in Tomcat connector test is portable. No hardcoded separators or platform-only paths introduced.

## Issues

None (bug).

### suggestion

- Residual batch should clear test-source rawtypes (PSReflectionHelper, PSItemIteratorTest, PSJexlEvaluatorTest raw maps) and eventually strip class-level `@SuppressWarnings("all")` on InstallUtil when safe.

### nit

- PSJexlEvaluatorTest still has pre-existing raw Map/List locals; not introduced by this batch; leave for residual.

## Behavioral tests added/extended

- PSWorkflowRoleInfoTest (4)
- PSSaxParseExceptionTest (1)
- PSValuesTest.testLongFromString
- PSJexlEvaluatorTest null-bindings rejection
- PSWorkflowUtilsBaseTest.filterUserNameIsNoopPreservingCommas
- PSXmlDocumentBuilderTest.testRemoveElementDeprecatedAndCurrent
- PSTomcatConnectorTest.schemePortConstructorSeedsFields

## Verification

- `cd modules/utils && mvnw clean install` → BUILD SUCCESS
- Main `src/main/**/*.java` project-Xlint warnings: **0** (was 23)
- Tests: 311 run, 0 fail, 9 skip

