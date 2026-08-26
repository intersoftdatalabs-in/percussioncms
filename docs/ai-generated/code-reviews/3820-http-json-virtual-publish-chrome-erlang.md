# Erlang review — #3820 Developer Sites http-json Publish chrome

**Branch:** `feat/issue-3820-http-json-publish-chrome`  
**Date:** 2026-08-26  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI chrome + Vitest + Playwright + product-docs); Playwright HARD GATE live H2 QA; stack/hot-deploy REST sibling without re-implementing adaptor; portable fixture paths (POSIX in-container, `path.join` on host).

## Summary

Parent #2678 slice 3 of 3. Developer Sites **Publish Virtual Site** is shown for `sourceKind=http-json` (never repository) and is live-proved after a successful **Build**: HTTP 200, filesCopied > 0, assembled `8.2/index.html` exists under the Site filesystem root in the H2 QA cell. REST publish is stacked from #3818 / PR #3822 (not re-implemented). Preview chrome stays hidden for HTTP JSON (sibling #3819). HTTP JSON hint copy and product-docs 8.2 admin Sites/Publishing describe Build-then-Publish.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — Publish allow-list includes `http-json`
- `WebUI/src/main/ts/developer/messages.ts` — HTTP JSON hint + Publish hint include filesystem target
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — comments
- Vitest: `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx` — Build then Publish dest path for http-json
- Playwright: `developer-site-virtual-source.spec.js` — intercept #3820 + live Build then Publish + on-disk HTML
- Fixture helper: `normalizeQaPublishDestPath` / `assertPublishedHttpJsonFilesOnQaCell` (fail-closed traversal)
- Product-docs 8.2: admin Sites, admin Publishing, developer Virtual Sites / REST, reference site-config
- Stacked consume: REST #3818 / PR #3822

## Issues

None.

## Cross-platform path review

- [x] In-container dest/HTML paths are POSIX (`/` join after rejecting `..` and drive letters)
- [x] Host fixture paths use `path.join(__dirname, …)`
- [x] `docker exec` / `docker cp` use arg arrays and `container:posixDest`
- [x] Unit tests reject blank, relative, `C:/…`, and `..` dests
- [x] No Unix-only `/tmp` hardcodes in production UI

## Tests

- Vitest: `shouldShowVirtualPublishChrome("http-json")` true; Preview still false; repository hidden
- Vitest: HTTP JSON Publish remains after Build success; dest/filesCopied shown
- Playwright intercept: http-json POST `/virtual/publish` HTTP 200 filesCopied
- Playwright live: save http-json, Build, Publish, dest files exist with fixture marker, restore repository
- Helper unit: POSIX dest normalize + posixJoin (5 passed)
- Surface spec **24 passed** on H2 QA (`TEST_CMS_URL=http://127.0.0.1:9993`)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Publish chrome for http-json | yes (stacked REST #3818 + this live proof) |
| Preview chrome stays hidden | yes (sibling #3819) |
| Vitest chrome/form helpers | yes |
| Playwright union spec | yes (24/24 H2 QA) |
| Product-docs 8.2 admin Sites | yes |
| REST adaptor | stacked consume #3818 (not re-implemented) |
| Human QA #2962 | not stolen |

## C5 evidence (pre-PR)

- `perc-devctl qa-up --skip-image-build` → `TEST_CMS_URL=http://127.0.0.1:9993`
- Hot-deploy: perc-system + rest + sitemanage SNAPSHOTs into WAR `WEB-INF/lib`; WebUI `cm/modern` assets (replace dest dir, do not nest `modern/modern`); in-cell StopJetty/StartJetty
- `qa-health` RESULT:OK HTTP:200 HEALTH:healthy (pin `QA_CMS_HOST_PORT=9993`)
- Playwright `npm run test:surface -- --path tests/developer-site-virtual-source.spec.js` — **24 passed**, 0 failed, 0 skipped (1.1m)
- console-clean=yes (pageerror listeners empty on live HTTP JSON Publish)
- server.log-clean=yes (no new ERROR/FATAL for this feature)
