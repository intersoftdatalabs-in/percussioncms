# Erlang review: fix/issue-2362-utils-residual-batch4

## Summary

Batch 4 residual after utils test-source Xlint batches 1–3. Clears **~72** `PSWorkflowUtilsBaseTest` project-Xlint diags with typed locals (keeps documented raw public-API probe). Strips main-source suppressions where real generics were already present or safe: `InstallUtil` class `@SuppressWarnings("all")`, `PSParseArguments` / `PSOrganizeProperties` class-level rawtypes. Bonus: 5 residual Xml test-source diags (`PSXmlDocumentBuilderTest` / `PSXmlTreeWalkerTest`). Module `mvnw clean install` green; **0** project-Xlint compiler warnings on compile.

## Scope

- Branch: `fix/issue-2362-utils-residual-batch4`
- Module: `modules/utils` only
- Parent module issue: #2016
- Tracker: #2200
- Issue: #2362

## Recommendation

**approve**

## Gate

May commit/push: **yes**

## Cross-platform path review

N/A — no new file I/O or path handling. Existing portable `Paths.get` / `File.separator` usage in InstallUtil unchanged.

## Issues

None (bug).

### suggestion

- Remaining main-source method/class suppressions are intentional or larger refactors: `PSWorkflowUtilsBase` raw public List/Map API (source-compat PRRT), `PSCollection`, `PSJexlEvaluator`, `PSXmlSerializationHelper`, brand-code / connector `this-escape`. File residual under #2016.

### nit

- `PSWorkflowUtilsBaseTest.castList` bridges raw return values; acceptable for intentional raw production API.

## Behavioral tests

- Existing `PSWorkflowUtilsBaseTest` suite (typed locals + raw probe) still covers public API.
- No new production logic paths; suppression strip only where code already clean under project Xlint.
- Xml Book `Comparable<Book>` + static `getElementData` qualify are compile-warning fixes; existing tests exercise them.

## Verification

- `cd modules/utils && mvnw clean install` → BUILD SUCCESS
- Tests: 311 run, 0 fail, 9 skip (pre-existing)
- Project-Xlint compiler warnings during compile: **0**
- `InstallUtil` compiles without class-level suppress (deprecation remains excluded by project `-Xlint:-deprecation`)

