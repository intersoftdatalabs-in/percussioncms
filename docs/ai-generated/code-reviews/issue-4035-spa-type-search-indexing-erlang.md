# Erlang review — issue 4035 CD-10 SPA type-level search indexing

**Scope:** uncommitted `feat/issue-4035-spa-type-search-indexing` vs `origin/main`.
**Reviewer:** Erlang (independent of implementer).
**Date:** 2026-08-31
**Memory patterns hit:** change-class closure (WebUI screen → Vitest + Playwright + product-docs); behavioral tests for GET/PUT 409/400; dedicated PUT not mixed into bulk save; i18n English-after-`@` fallback (TMX matrix optional, prior UI slices).

## Summary

Developer Content Type detail gains a **Search indexing** checkbox (CD-10) next to **Enabled**. The SPA uses existing REST `GET`/`PUT /services/contenttypes/{idOrName}/searchIndexing` (Jackson root `ContentTypeSearchIndexing`). PUT requires a held design lock and does not lock/unlock. Distinct from per-field `searchable`. Default is on. Unlocked chrome stays disabled; a 409 on PUT clears the held lock and surfaces the save error. Product-docs 8.2 admin + REST catalog updated.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

N/A for production I/O (no new filesystem joins). Playwright spec uses `URLSearchParams` and existing `catalogRowSelector`. No path-string assertions.

## Issues

### nit

- New `DEV_MSG.CT_FORM_SEARCH_INDEXING` uses English-after-`@` fallback without `DeveloperUi.tmx` TUs (same as prior Developer chrome slices).
- GET `searchIndexing` failure defaults the flag on and does not surface a panel error (Workbench default-on; panel remains usable).

## Tests

- `contentTypesApi.test.ts`: wrap/unwrap, GET, PUT, encoded id, 409 unlocked, 400 missing boolean.
- `ContentTypeDetailPanel.test.tsx`: disabled until lock; GET load; dedicated PUT after lock; bulk PUT not called on SI failure; 409 clears lock.
- Playwright: `developer-content-type-search-indexing.spec.js` (unlocked no PUT; lock/toggle/GET round-trip; 409 surfaced).
- Product-docs: `product-docs/8.2/admin/developer-content-types.md`, `product-docs/8.2/developer/rest.md`.
