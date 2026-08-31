# Erlang review: #4070 REST UI-07 view write

**Branch:** `feat/issue-4070-rest-view-write`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Change class:** public REST adaptor write surface (`rest` resource + `IViewAdaptor` + sitemanage apibridge + Spring test stub + product-docs)  
**Memory patterns hit:** incomplete change-class closure; rest `MainTest` Spring stub exact adaptor type; focused `-Dtest` not sufficient without module suite

## Summary

Admin POST/PUT/DELETE on `/services/views` persist CX standard (field-criteria) views through existing `IPSUiDesignWs` (`createViews` / `loadViews` / `saveViews` / `deleteViews`). Execute is unchanged. Inbox/system custom-URL views are 409 on write. Duplicate 409, invalid 400, missing 404, non-Admin 403. Peer is searches write (#4069 / PR #4075).

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

## Companions (closure)

| Artifact | Status |
|----------|--------|
| `IViewAdaptor` create/save/delete | present |
| `ViewResource` POST/PUT/DELETE + status mapping | present |
| Mockito `ViewResourceTest` | present (30 tests, write + existing execute) |
| Spring `TestViewAdaptor` exact `IViewAdaptor` | present |
| `ViewAdaptor` persist, `overrideLock=false`, no execute on write | present |
| `ViewAdaptorWriteTest` | present (24 tests) |
| `product-docs/8.2/developer/rest.md` write contract (no SPA claim) | present |
| Standalone `rest` + `projects/sitemanage` clean install | BUILD SUCCESS |

## Cross-platform path checklist

N/A — no filesystem path/file I/O in this diff. Catalog keys still reject `/` `\` `..` as unsafe view keys (URL-style, not OS join).

## Tests / evidence

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS. Tests run: 927, Failures: 0. `ViewResourceTest` Tests run: 30.
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS. Tests run: 1992, Failures: 0, Errors: 0, Skipped: 125 (baseline). `ViewAdaptorWriteTest` Tests run: 24. `ViewAdaptorExecuteTest` Tests run: 31.
- C2: `IViewAdaptor` gained additive write methods. Grep `implements IViewAdaptor` → `ViewAdaptor` + `TestViewAdaptor` only. Reverse-dep sitemanage standalone install green.
