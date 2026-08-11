# Erlang review: issue #2030 perc-toolkit test-source Xlint

**Date:** 2026-08-11  
**Branch:** `fix/issue-2030-perc-toolkit-xlint-residual`  
**Scope:** Uncommitted changes in `modules/perc-toolkit` test sources only  
**Reviewer persona:** Erlang (independent of implementer)

## Summary

Clears remaining **test-source** project `-Xlint` diagnostics for `perc-toolkit` after main-source was already at 0 (#2658 / #2419). Real generics / typed matchers / static-method qualification — no blanket `@SuppressWarnings`.

**Diagnostic inventory (project `-Xlint`):**

| Phase | Before | After |
|-------|--------|-------|
| main compile | 0 | 0 |
| test compile | 46 | 0 |
| javadoc | ~100 (capped report) | unchanged (out of this PR) |

## Recommendation

**approve**

## Gate

- Bugs: none
- Behavioral tests: existing suite exercises the typed call sites; tests still green (245 run, 0 fail, 16 skipped)
- Cross-platform paths: N/A (no path/file I/O in diff)
- **May commit/push: yes**

## Issues

None blocking.

### Nits (non-blocking)

- Residual **Javadoc** (~100 warnings) remains for a follow-up residual under #2030 acceptance (javac+javadoc zero). Not introduced by this PR.
- Deprecation still surfaces as compiler **INFO** (`-Xlint:-deprecation` in parent); not project WARNING under current `-Xlint` set.

## Files reviewed

- `modules/perc-toolkit/src/test/java/test/percussion/pso/jexl/PSOListToolsTest.java`
- `modules/perc-toolkit/src/test/java/test/percussion/pso/imageedit/data/MasterImageMetaDataTest.java`
- `modules/perc-toolkit/src/test/java/test/percussion/pso/preview/SiteFolderFinderImplTest.java`
- `modules/perc-toolkit/src/test/java/test/percussion/pso/preview/ActionPreviewControllerTest.java`
- `modules/perc-toolkit/src/test/java/test/percussion/pso/preview/ConfigurableSiteLoaderImplTest.java`
- `modules/perc-toolkit/src/test/java/test/percussion/pso/utils/MutableHttpServletRequestWrapperTest.java`
- `modules/perc-toolkit/src/test/java/test/percussion/pso/utils/SimplifyParameterMapTest.java`
- `modules/perc-toolkit/src/test/java/test/percussion/pso/validation/PSOUniqueFieldWithInFoldersValidatorTest.java`

## Build evidence

```text
cd modules/perc-toolkit && ../../mvnw.cmd clean install
BUILD SUCCESS
Tests run: 245, Failures: 0, Errors: 0, Skipped: 16
testCompile primary file:line [WARNING]: 0
```

Memory patterns hit: prefer real generics over suppress; typed Mockito matchers (`anyList` / typed `any()`); qualify static setters by type name.

> Co-Authored by Grok Build using grok-4.5 with agent main.
