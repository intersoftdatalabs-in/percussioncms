# Erlang review — #2613 cms.objectstore residual this-escape (post-#2467)

**Status:** pass (self-review before commit)  
**Date:** 2026-08-09  
**Module:** `system` / perc-system

## Change class

Real `-Xlint:this-escape` reduction in `cms.objectstore` residual hotspots **outside** createKey/Element double-load (#2467): final leaf types, private `loadFieldsFromXml` / construction helpers, final relationship property APIs. **No** suppress-only.

## Findings

| Severity | Finding | Disposition |
| --- | --- | --- |
| none | Bugs in Action/Folder/Search/AaRelationship Element or named ctor restore | Covered by `PSObjectStoreThisEscapeResidualTest` (8) + existing `PSDbComponentThisEscapeTest` (9) |
| none | Non-portable paths | N/A (no file I/O changes) |
| note | Residual cms.objectstore this-escape remains (~64) on CoreItem/ServerItem, processors, leaf value types, etc. | Parent #2022 may re-file further residual; PR-sized leftover out of this slice |
| note | Several leaf types marked `final` | No in-repo subclasses of Action/Search/DisplayFormat/Folder/AaRelationship; ant `PSAction` is a different type |

## Metrics (cms.objectstore `this-escape` with `-Xmaxwarns 10000`)

| | Count |
| --- | ---: |
| Before | 82 |
| After | 64 |
| Δ | **−18** |

## Tests

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS  
- Tests run: 1398, Failures: 0, Errors: 0 (Skipped: 240)  
- Focused: `PSObjectStoreThisEscapeResidualTest` 8/8, `PSDbComponentThisEscapeTest` 9/9  

## Hard gates

- [x] Behavioral unit tests for touched Element/ctor paths  
- [x] No suppress-only this-escape  
- [x] Cross-platform N/A  
- [x] Module clean install green  
