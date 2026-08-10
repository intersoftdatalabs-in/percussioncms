# Erlang review - #2652 cms.objectstore CoreItem/processors this-escape residual

**Status:** pass (self-review before commit)  
**Date:** 2026-08-10  
**Module:** `system` / perc-system

## Change class

Real `-Xlint:this-escape` reduction in residual `cms.objectstore` after #2613: CoreItem definition extract without passing `this`, final leaf types, private construction helpers for processors/values. **No** suppress-only.

## Findings

| Severity | Finding | Disposition |
| --- | --- | --- |
| none | CoreItem extractDef behavioral change | Covered by `PSObjectStoreThisEscapeCoreItemTest` (definition extract + fields) |
| none | ProcessorProxy NPE contract for invalid type | Restored flushCache-without-null-check; `PSActiveAssemblyProcessorProxyTest` green |
| none | Non-portable paths | BinaryFileValue uses `File` / NIO-style streams only; path string for mime uses existing `/` normalize for URL-ish metadata |
| note | Many leaf types marked `final` | No in-repo subclasses (verified before marking) |
| note | cms.objectstore this-escape fully cleared this batch | Residual 0 under package-scoped measure |

## Metrics (cms.objectstore `this-escape`, `-Xmaxwarns 10000`)

| | Count |
| --- | ---: |
| Before | 61 |
| After | 0 |
| Δ | **−61** |

## Tests

- `cd system && ../mvnw.cmd clean install` - BUILD SUCCESS  
- Tests run: 1573, Failures: 0, Errors: 0 (Skipped: 240)  
- Focused: `PSObjectStoreThisEscapeCoreItemTest` 6/6, residual + DbComponent suites green  

## Hard gates

- [x] Behavioral unit tests for touched ctor/extract paths  
- [x] No suppress-only this-escape  
- [x] Cross-platform path handling OK  
- [x] Module clean install green  
