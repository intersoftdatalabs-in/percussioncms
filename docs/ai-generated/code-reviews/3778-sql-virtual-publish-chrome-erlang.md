# Erlang review — #3778 Developer Sites SQL virtual Publish chrome

**Branch:** `feat/issue-3778-sql-virtual-publish-chrome`  
**Date:** 2026-08-23  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI chrome + Vitest + Playwright + product-docs); Playwright HARD GATE live H2 QA; stack/hot-deploy REST sibling without re-implementing adaptor; portable fixture paths (POSIX in-container, `path.join` on host).

## Summary

Parent #2678 slice 2 of 3. Developer Sites **Publish Virtual Site** is shown for `sourceKind=sql-database` (never repository) and is live-proved after a successful **Build**: HTTP 200, filesCopied > 0, assembled `8.2/index.html` exists under the Site filesystem root in the H2 QA cell. REST publish is stacked from #3779 / PR #3788 (not re-implemented). SQL hint copy and product-docs 8.2 admin Sites/Publishing describe Build-then-Publish.

## Scope

- `WebUI/src/main/ts/developer/messages.ts` — SQL hint + Publish hint include SQL filesystem target
- Vitest: `VirtualSiteSourcePanel.test.tsx` — Build then Publish dest path for sql-database
- Playwright: `developer-site-virtual-source.spec.js` — intercept #3778 + live Build then Publish + on-disk HTML
- Fixture helper: `normalizeQaPublishDestPath` / `assertPublishedSqlFilesOnQaCell` (fail-closed traversal)
- Product-docs 8.2: admin Sites, admin Publishing, developer Virtual Sites
- Stacked consume: REST #3779 / PR #3788 on cluster #3777

## Issues

None.

## Cross-platform path review

- [x] In-container dest/HTML paths are POSIX (`/` join after rejecting `..` and drive letters)
- [x] Host fixture paths use `path.join(__dirname, …)`
- [x] `docker exec` / `docker cp` use arg arrays and `container:posixDest`
- [x] Unit tests reject blank, relative, `C:/…`, and `..` dests
- [x] No Unix-only `/tmp` hardcodes in production UI

## Tests

- Vitest: SQL Publish remains after Build success; dest/filesCopied shown
- Playwright intercept: sql-database POST `/virtual/publish` HTTP 200 filesCopied
- Playwright live: save sql-database, Build, Publish, dest files exist with fixture marker, restore repository
- Helper unit: POSIX dest normalize + posixJoin

## Change-class closure

| Companion | Status |
|-----------|--------|
| Publish chrome for sql-database | yes (stacked cluster + this live proof) |
| Vitest chrome/form helpers | yes |
| Playwright union spec | yes (17/17 H2 QA) |
| Product-docs 8.2 admin Sites | yes |
| REST adaptor | stacked consume #3779 (not re-implemented) |
| Human QA #2962 | not stolen |

## C5 evidence (pre-PR)

- `perc-devctl qa-up --skip-image-build` → `TEST_CMS_URL=http://127.0.0.1:9993`
- Hot-deploy: perc-system + rest + sitemanage SNAPSHOTs into WAR `WEB-INF/lib`; WebUI `cm/modern` assets; in-cell StopJetty/StartJetty
- `qa-health` RESULT:OK HTTP:200 HEALTH:healthy
- Playwright `npm run test:surface -- --path tests/developer-site-virtual-source.spec.js` — **17 passed**
- console-clean=yes (pageerror listeners); server.log-clean=yes (no new ERROR/FATAL for this feature)
