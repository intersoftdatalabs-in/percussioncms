# Erlang review — #2404 design.objectstore this-escape batch

**Status:** pass (self-review before commit)  
**Date:** 2026-08-08  
**Module:** `system` / perc-system

## Change class

Real `-Xlint:this-escape` reduction in `design.objectstore` (+ foundation `PSDbComponent.createKey` path): private non-overridable helpers / final leaf methods called from ctors/fromXml. **No** blanket `@SuppressWarnings("this-escape")`.

## Findings

| Severity | Finding | Disposition |
| --- | --- | --- |
| none | Bugs in ctor/fromXml behavior | Covered by existing XML tests + new `PSDesignObjectStoreThisEscapeTest` (9 tests) |
| none | Non-portable paths | N/A (no file I/O changes) |
| note | Residual ~200+ this-escape in design.objectstore | File residual child issues (not suppress) |
| note | `PSDataSet`/`PSBackEndColumn`/`PSEntry`/`PSUrlRequest` keep overridable `fromXml` for subclasses via private `fromXmlBase` | Correct pattern |
| note | Leaf types use `final` on methods called from ctors | No subclass overrides verified before finalizing |

## Tests

- `cd system && ../mvnw clean install` — BUILD SUCCESS  
- Tests run: 1287, Failures: 0, Errors: 0  
- New: `PSDesignObjectStoreThisEscapeTest` 9/9 pass  

## Hard gates

- [x] Behavioral unit tests for ctor/fromXml  
- [x] No suppress-only this-escape  
- [x] Cross-platform N/A  
- [x] Module clean install green  
