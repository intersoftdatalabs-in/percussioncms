# Erlang review — #3870 Developer Sites object-storage Preview chrome

**Branch:** `feat/issue-3870-object-storage-preview-chrome`  
**Date:** 2026-08-26  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI panel + Vitest + Playwright + product-docs); Playwright HARD GATE for screen work; consume REST preview sibling without re-implementing it; no secrets on REST envelope; portable Path joins in QA fixture helper.

## Summary

Parent #2678 slice 3 of 3 (object-storage Preview chrome). Developer Sites **Preview assembled site** is shown for `sourceKind=object-storage` after save. Operators save a local object-key root, **Build Virtual Site**, then Preview last-build home HTML (REST `GET …/virtual/preview` from cluster #3867 / #3858). **Build** chrome stays visible (required to Preview after Build; overlaps open #3869 / PR #3875). **Publish** chrome stays hidden. Repository / unknown kinds still hide Virtual chrome. git/csv/sql/http-json Preview unchanged. Local QA fixture only (no cloud URLs, IAM, or access keys).

## Scope

- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — Preview (and Build) include `object-storage`; Publish does not
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — comments for chrome split
- `WebUI/src/main/ts/developer/messages.ts` — object-storage hint + Preview hint include Object storage
- Vitest: `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx`
- Playwright: `developer-site-virtual-source.spec.js` + `object-storage-virtual-qa-fixture.js` + fixture tree
- Product-docs: `admin/sites.md`, `admin/publishing.md`, `developer/virtual-sites.md`, `developer/rest.md`, `reference/site-config.md`
- Consumed (not re-implemented): REST last-build Preview on `main` (cluster #3867 / #3858)
- No QA assignment in this slice (human QA candidacy only after C5)

## Issues

None.

## Cross-platform path review

- [x] No new `".../" +` or `"...\\" +` filesystem joins in WebUI
- [x] QA fixture helper uses `path.join` for host files; in-container dest is POSIX (`/opt/Percussion/tmp/…`) — URL/ZIP/Linux-cell paths correctly use `/`
- [x] UI/tests use operator-style examples (`C:/object-docs`) as field values, not OS file joins
- [x] Preview home path still sanitized via existing `sanitizeVirtualPreviewHomePath` (rejects `..`, drive letters, URLs)
- [x] Line-ending assertions not added
- [x] Playwright preview URL segments encode path parts; filters `.` / `..`

## Tests

- `virtualSiteBuild` — object-storage shows Build + Preview; Publish still false; repository / `sql-api` stay hidden; git/csv/sql/http-json unchanged
- `VirtualSiteSourcePanel` — load/save object-storage shows Build + Preview, hides Publish; Preview click opens last-build home; repository switch hides chrome
- Playwright — object-storage option; Build + Preview visible; Publish hidden; live Preview after Build on H2 QA fixture; restore repository hides Preview
- REST internals — N/A (consume cluster #3867)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Chrome helper + panel i18n | yes |
| Vitest for helpers + panel | yes |
| Playwright surface spec + local object-key fixture | yes |
| Product-docs admin Sites (and peers) | yes |
| REST preview re-implementation | out of scope (consume #3858) |
| Publish chrome | later phase (#3868 REST; UI later) |
