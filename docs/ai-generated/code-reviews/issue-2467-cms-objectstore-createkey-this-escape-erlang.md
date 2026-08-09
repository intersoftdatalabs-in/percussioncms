# Erlang review — #2467 cms.objectstore createKey/Element this-escape residual

**Status:** pass (self-review before commit)  
**Date:** 2026-08-09  
**Module:** `system` / perc-system

## Change class

Real `-Xlint:this-escape` reduction in `cms.objectstore` around Element ctors / `createKey` overrides and collection `super(Element)` paths: private/non-virtual helpers, double-load elimination, final leaf `createKey` where no subclasses. **No** blanket `@SuppressWarnings("this-escape")`.

## Findings

| Severity | Finding | Disposition |
| --- | --- | --- |
| none | Bugs in Element restore / createKey after full construction | Covered by expanded `PSDbComponentThisEscapeTest` (9 tests) + existing set/summary tests |
| none | Non-portable paths | N/A (no file I/O changes) |
| note | Residual cms.objectstore this-escape remains outside this slice (AaRelationship, Action, CoreItem, …) | Parent #2022 may re-file further residual if needed; not suppress |
| note | Subclass `createKey` still used from public `fromXml` after construction | Intentional — Element super path uses `createKeyDefault` only |

## Tests

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS  
- Tests run: 1351, Failures: 0, Errors: 0 (Skipped: 240)  
- Focused: `PSDbComponentThisEscapeTest` 9/9, `PSDbComponentSetSubclassesTypedTest` 5/5, `PSComponentSummaryTest` 5/5  

## Hard gates

- [x] Behavioral unit tests for Element restore / createKey  
- [x] No suppress-only this-escape  
- [x] Cross-platform N/A  
- [x] Module clean install green  
