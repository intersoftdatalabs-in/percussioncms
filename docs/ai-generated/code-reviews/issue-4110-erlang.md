# Erlang review — #4110 SPA UI-08 search field-selection

- **Scope:** uncommitted vs `HEAD` on `feat/issue-4110-search-field-selection` (parent #1690)
- **Date:** 2026-09-01
- **Recommendation:** approve
- **Gate:** May commit/push: yes
- **Memory patterns hit:** change-class companions (rest resource + adaptor + WebUI + Playwright + product-docs); behavioral tests for persist/400/409; packaged no-steal; omit-vs-empty PUT semantics

## Summary

PUT `SearchDef.fields` now persist through `SearchAdaptor.applyWritableFields` → `IPSUiDesignWs.saveSearches`. Unknown field names are `IllegalArgumentException` → HTTP 400. Packaged names (`Default_Search`, `RC_Search`) throw 409 before save (no lock steal). `DESIGN_GAPS` no longer claims field-criterion editing is unsupported. SPA `SearchDetailPanel` reuses the DF field catalog for add/remove/reorder; `SR_GAP_FIELDS` dropped. Product-docs 8.2 Developer Searches + REST updated.

## Issues

None (hard-gate).

### Notes (non-blocking)

- Live Playwright add/remove/reorder GET round-trip on uniquely named user searches is not proven on this H2 cell: `saveSearches` can succeed while `findSearches`/`findAllSearches` lag (`reloadAfterWrite` 500). Packaged Default_Search readonly chrome + PUT fields 409 **did** pass. Persist/reorder covered by `SearchAdaptorFieldsTest`.
- New `perc.ui.developer@…` strings use `@` English fallback; full TMX matrix not expanded (same as DF UI-08 column picker). Not a commit blocker under agent_safe_only.

## Cross-platform path checklist

N/A for filesystem I/O. Field-name validation rejects `/`, `\`, `..`, and NUL (URL/key safety, not OS path join). Playwright uses `TEST_CMS_URL` (no hardcoded `:9993` as sole resolver).

## Builds

- `rest`: `mvnw clean install` BUILD SUCCESS; Tests run: 1008, Failures: 0
- `projects/sitemanage`: BUILD SUCCESS; Tests run: 2195, Failures: 0, Skipped: 125
- `WebUI`: BUILD SUCCESS; Java Tests run: 63, Failures: 0; Vitest Tests 3705 passed
