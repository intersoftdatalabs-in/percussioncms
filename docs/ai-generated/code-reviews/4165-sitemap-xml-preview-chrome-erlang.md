# Erlang review — #4165 Developer Sites sitemap-xml Preview chrome

**Branch:** `feat/issue-4165-sitemap-xml-preview-chrome`  
**Date:** 2026-09-02  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI panel + Vitest + Playwright + product-docs); Playwright HARD GATE for screen work; consume REST preview sibling without re-implementing it; no secrets on REST envelope; portable Path joins in QA fixture helper.

## Summary

Parent #2678 slice (Developer Sites sitemap-xml Preview chrome). Developer Sites **Preview assembled site** is shown for `sourceKind=sitemap-xml` after save. Operators save a local sitemap.xml root, **Build Virtual Site**, then Preview last-build home HTML (REST `GET …/virtual/preview` from cluster #4151 / #4125). **Build** chrome is shown so operators can produce last-build HTML for Preview (overlaps sibling #4164). **Publish** chrome stays hidden (#4166). Missing last-build stays unavailable (`available=false`; no fake preview). Repository / unknown kinds still hide Virtual chrome. git/csv/sql/http-json/object-storage/rss-atom/icalendar Preview unchanged. Local QA fixture only (no live crawl, credentials, or `virtual.remoteUrl`).

## Scope

- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — Preview (and Build) include `sitemap-xml`; Publish does not
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — comments for chrome split
- `WebUI/src/main/ts/developer/messages.ts` — sitemap-xml hint + Preview hint include Sitemap XML
- Vitest: `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx`
- Playwright: `developer-site-virtual-source.spec.js` + `sitemap-xml-virtual-qa-fixture.js` + fixture tree
- Product-docs: `admin/sites.md`, `admin/publishing.md`, `developer/virtual-sites.md`, `developer/rest.md`, `reference/site-config.md`
- Consumed (not re-implemented): REST last-build Preview on `main` (cluster #4151 / #4125)
- No ActionMenu files (cluster #4151)
- No QA assignment in this slice (human QA candidacy only after C5)

## Issues

None.

## Cross-platform path review

- [x] No new `".../" +` or `"...\\" +` filesystem joins in WebUI
- [x] QA fixture helper uses `path.join` for host files; in-container dest is POSIX (`/opt/Percussion/tmp/…`) — URL/ZIP/Linux-cell paths correctly use `/`
- [x] UI/tests use operator-style examples (`C:/sitemap-xml-docs`) as field values, not OS file joins
- [x] Preview home path still sanitized via existing `sanitizeVirtualPreviewHomePath` (rejects `..`, drive letters, URLs)
- [x] Line-ending assertions not added
- [x] Playwright preview URL segments encode path parts; filters `.` / `..`

## Tests

- `virtualSiteBuild` — sitemap-xml shows Build + Preview; Publish still false; repository / `sql-api` stay hidden; git/csv/sql/http-json/object-storage/rss-atom/icalendar unchanged
- `VirtualSiteSourcePanel` — load/save sitemap-xml shows Build + Preview, hides Publish; Preview click opens last-build home; missing build shows empty state; repository switch hides chrome
- Playwright — sitemap-xml option; Build + Preview visible; Publish hidden; live Preview after Build on H2 QA fixture; restore repository hides Preview
- REST internals — N/A (consume cluster #4151)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Chrome helper + panel i18n | yes |
| Vitest for helpers + panel | yes |
| Playwright surface spec + local sitemap.xml fixture | yes |
| Product-docs admin Sites (and peers) | yes |
| REST preview re-implementation | out of scope (consume #4125 / #4151) |
| Publish chrome | later phase (#4166) |
