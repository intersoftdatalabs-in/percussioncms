# Erlang review: #3368 Developer Sites list bind

**Branch:** `fix/issue-3368-sites-list-bind`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Date:** 2026-08-15

## Summary

Residual of QA #3129 / implement #3090 after #3198: Developer → Sites still showed an empty table after HTTP 200. `SiteList` is an `ArrayList` subclass; without `@JsonFormat(ARRAY)` Jackson can emit `{empty:false}` (peer: `ActionMenuList` #3379). The SPA `parseSiteList` from #3198 did not accept the live `SiteSummary` / `sites` / JAXB `item` envelopes or XML leftovers, and `listSites` did not fall back to the working sitemanage list.

Changes:

- `rest` `SiteList`: `@JsonFormat(shape = ARRAY)` + serial test asserts array, not `{empty}`.
- `WebUI` `parseSiteList`: SiteSummary / sites / item / empty-bean / JSON-text / XML.
- `listSites`: try `/services/sites`, trailing slash, then `/sitemanage/site/`; empty only when all sources are empty.
- Playwright `bug-3368-developer-sites-list.spec.js` compares both APIs.
- Product docs: `product-docs/8.2/admin/sites.md`, `developer/rest.md`.

## Issues

None (bugs / missing behavioral tests / non-portable paths).

## Cross-platform path checklist

N/A for filesystem I/O. XML walk uses DOM, not path separators. Playwright URLs use `/`.

## Tests

- `rest`: `SiteListSerialDeserialTest` (array shape).
- `WebUI` Vitest: live payload fixtures + listSites fallback.
- Playwright surface spec for #3368.

Memory patterns hit: ArrayList-subclass Jackson bean (`ActionMenuList`); silent empty catalog vs throw-on-unknown; change-class companions (Vitest + Playwright + product-docs).
