# Erlang review: issue #3882 system/src/main cms builders leftover IPS*Errors typed ErrorCodes

- **Branch:** `fix/issue-3882-cms-builders-errorcodes`
- **Base:** `origin/main`
- **Date:** 2026-08-27
- **Reviewer:** Erlang (independent of implementer)
- **Recommendation:** approve
- **Gate:** May commit/push: yes
- **Memory patterns hit:** missing behavioral tests; incomplete change-class closure; constructor overload ambiguity (`PSException(String, Throwable)` vs typed 2-arg); tests that only grep source strings

## Summary

Converts remaining origin/main allow-list production `IPS*Errors` sites under `system/src/main/java/com/percussion/cms` document builders (`PS*Builder`, `PSEditorDocumentContext`, `PSModifyPlanBuilder`, `PSValidateModifyStep`) to typed `ServerErrorCodes` / `CmsErrorCodes`. `PSDataExtractionException` gained `IPSErrorCode` constructors (peer of `PSSystemValidationException` / `PSCmsException`). Dual-write skip is `isAuditable() == false` on leftover CE/operational codes. `PSLogServerWarning` still takes `int` (`RAW_DUMP.numericCode()`).

A 2-arg `PSException(String, IPSErrorCode)` overload was **not** added: it is ambiguous with `PSException(String, Throwable)` when the second argument is `null` (`PSExceptionCauseCtorTest`). Language+code uses 3/4-arg overloads.

`PSEditorDocumentContext.getParentPageId` now continues after the first summary parent so a second summary parent throws `CE_AMBIGUOUS_PAGEID` (javadoc contract; prior `&& !found` made the throw unreachable). Behavioral tests cover that path plus missing page-map / no-parent and `PSValidateModifyStep` production throws.

## Issues

None blocking.

### Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Tests do not assert Unix-only absolute path shapes
- [x] Temp files / OS temp hardcodes not used
- [x] Line-ending sensitive assertions not used

## Notes (non-blocking)

- Handlers and cms objectstore remain on sibling leftovers (#3883 / #3884).
- C2: additive constructors only; dropped the 2-arg language+code overload to preserve `new PSException("msg", null)` compile.
- `perc-toolkit` reverse-dep uses `(String, Throwable)` — still unique.
