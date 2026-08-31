# Erlang review — #4046 Developer Content Type SPA field choices catalog (CD-07)

**Date:** 2026-08-31  
**Branch:** `feat/issue-4046-spa-field-choices-catalog`  
**Scope:** uncommitted vs `HEAD` (WebUI SPA client + ContentTypeDetailPanel, Vitest, Playwright, product-docs 8.2)  
**Memory patterns hit:** change-class closure (Vitest + Playwright for WebUI screen + product-docs); dedicated REST consumed (no rest/sitemanage re-implement); 409 no steal; omitted `choices` must not wipe catalog; type `none` clears; no filesystem path I/O.

## Summary

Developer Content Type detail consumes REST #3995 on the existing CD-07
`GET`/`PUT .../fields/{fieldName}/controlProperties` path. After a held design
lock, operators can view and save the field **choice catalog** (`none` / local
list / keyword / lookup / table, plus null-entry, default-selected, and
filter). A properties-only save still **omits** `choices` so the catalog is
not cleared. `type: none` is sent only when the operator clears the catalog.
Unlocked editors stay disabled; 409 other-user lock does not steal.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Issues

None blocking.

### Companions (checked)

| Artifact | Present |
|----------|---------|
| WebUI wrap/unwrap optional `choices` on `ContentTypeFieldControlProperties` | yes |
| Catalog parse / payload / equality helpers | yes |
| ContentTypeDetailPanel always-mounted choices chrome + lock → PUT → GET | yes |
| Vitest API + helpers + panel (omit choices, local save, type none, 409) | yes |
| Playwright surface spec | `developer-content-type-field-choices.spec.js` |
| product-docs `8.2/admin/developer-content-types.md` + REST CD-07 SPA note | yes |
| Out of scope: shared-field SPA (#4029/#4044), REST re-implement | not shipped |

### Notes (non-blocking)

- Jackson WRAP_ROOT still uses `ContentTypeFieldControlProperties`; client wrap includes `choices` only when the fourth argument is passed.
- Cross-platform path checklist: N/A (REST URL `/` only; no filesystem joins).
- Always-mounted chrome follows CD-07 property-values (#3894) Cycle Verify lessons.
