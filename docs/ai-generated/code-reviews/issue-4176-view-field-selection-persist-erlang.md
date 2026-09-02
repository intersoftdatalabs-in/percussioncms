# Erlang review — #4176 view field-selection persist

**Date:** 2026-09-02  
**Branch:** `fix/issue-4176-view-field-selection-persist`  
**Base:** `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: **yes** (after module clean install + C5 Playwright)  
**Cross-platform path review:** no new filesystem path joins; JDBC uses constant table/column names.

## Summary

H2 QA `developer-view-field-selection.spec.js` failed at field-criteria save with `View not found` after a unique standard view create. Two persist/lookup holes:

1. `PSUiDesignWs.getComponentKey` used `IPSGuid.longValue()` as `PSX_SEARCHES.SEARCHID`. VIEW_DEF/SEARCH_DEF packed longs are not the uuid; load/save/delete by GUID missed JDBC rows when `findAllViews` XML lagged.
2. Create required catalog visibility then PUT used GUID-first; if the list missed, PUT 404. Create now returns the saved object when the catalog lags; PUT retries by body `name`; SPA retries PUT by name after GUID 404.

Playwright spec is unchanged.

## Scope

- `system` `PSUiDesignWs.searchComponentKey` + `PSUiDesignWsViewPersistTest`
- `projects/sitemanage` `ViewAdaptor` + write/fields tests
- `WebUI` `ViewDetailPanel` GUID 404 → name retry + Vitest
- `product-docs/8.2` developer REST + admin Developer Views

## Issues

None at `bug` severity.

### suggestion

- Keep Playwright GET-optional branch for catalog lag after field PUT until `findAllViews` cache invalidation is a dedicated slice.

## Tests

- `PSUiDesignWsViewPersistTest.searchComponentKey_usesUuidNotPackedLong`
- `ViewAdaptorWriteTest.create_returnsSavedViewWhenFindAllViewsLags`
- `ViewAdaptorFieldsTest.update_usesBodyNameWhenPathGuidMissesCatalog`
- Vitest: after create, field PUT retries by name on 404
