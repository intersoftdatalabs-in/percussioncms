# Erlang review — #3894 Developer Content Type control property values chrome (CD-07)

**Date:** 2026-08-27  
**Branch:** `feat/issue-3894-ct-control-property-values-chrome`  
**Scope:** uncommitted vs `HEAD` (WebUI SPA client + ContentTypeDetailPanel, Vitest, Playwright, product-docs 8.2)  
**Memory patterns hit:** change-class closure (Vitest + Playwright for WebUI screen + product-docs); dedicated REST consumed (no rest/sitemanage re-implement); 409 no steal; always-mounted chrome; no filesystem path I/O.

## Summary

Developer Content Type detail consumes REST #3786: after a held design lock, operators can view and edit field control **property values** (not names-only) and save via `PUT .../fields/{fieldName}/controlProperties`. Save omits `choices` so choice catalogs stay unchanged (filter / null-entry / default-selected remain out of scope). Unlocked editors stay disabled; 409 other-user lock does not steal.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Issues

None blocking.

### Companions (checked)

| Artifact | Present |
|----------|---------|
| WebUI API wrap/unwrap `ContentTypeFieldControlProperties` | yes |
| ContentTypeDetailPanel always-mounted chrome + lock → PUT → GET | yes |
| Vitest API + helpers + panel + DeveloperShell mock | yes |
| Playwright surface spec | `developer-content-type-control-properties.spec.js` |
| product-docs `8.2/admin/developer-content-types.md` + REST CD-07 SPA note | yes |
| Out of scope: item-level exits (#3895), field-rule chrome (#3896), choice filter writes | not shipped |

### Notes (non-blocking)

- Jackson WRAP_ROOT uses class name `ContentTypeFieldControlProperties`; client wrap matches.
- Cross-platform path checklist: N/A (REST URL `/` only; no filesystem joins).
- Always-mounted control-property chrome follows CD-12/CD-13 Cycle Verify lessons (#3834/#3836).
