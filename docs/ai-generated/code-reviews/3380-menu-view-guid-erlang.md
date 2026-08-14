# Erlang review — #3380 Action Menu + View GUID for Object ACL

**Date:** 2026-08-14  
**Branch:** `fix/issue-3380-menu-view-guid`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** nested Guid / `guidString` / catalog fallback (Display Format #3200 peer); change-class companions (Vitest + Playwright + product-docs)

## Summary

Action Menu and View Developer detail only passed `detail.guid?.stringValue` into Object ACL, so Jackson envelopes, missing `stringValue`, or omitted Guid left ACL on the permanent no-GUID shell. This change copies the Display Format resolver (`nested Guid` → `guidString` → catalog → typed id synthesis), unwraps catalog/detail envelopes, and sets `PSTypeEnum.ACTION` Guid on `convertPSActionMenu` (previously omitted). View REST already emitted Guid; client unwrap/synthesis closes the bind.

## Issues

None (no bugs, no missing behavioral tests for the new resolvers/unwrap, no non-portable path I/O).

## Cross-platform path checklist

N/A — no filesystem path construction. REST Guid uses `PSGuid` host-type-uuid strings; tests compare those strings.

## Tests / evidence

- Vitest: resolver + unwrap + detail/panel wiring + no-GUID message
- sitemanage: `ApiUtilsActionMenuConvertTest` Guid from actionId; `ViewAdaptorMapTest` Guid.stringValue
- Playwright H2 QA: two `#3380` tests passed (Menu + View GUID → Object ACL load)
- Standalone `mvnw clean install`: `projects/sitemanage`, `WebUI`
