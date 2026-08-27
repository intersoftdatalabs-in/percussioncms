# Erlang review — #3879 Developer Sites object-storage Publish chrome

**Branch:** `feat/issue-3879-object-storage-publish-chrome`  
**Date:** 2026-08-26  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI chrome + Vitest + Playwright + product-docs); Playwright HARD GATE live H2 QA; consume REST publish sibling without re-implementing adaptor; portable fixture paths (POSIX in-container, `path.join` on host).

## Summary

Parent #2678 slice (next object-storage phase after cluster #3877). Developer Sites **Publish Virtual Site** is shown for `sourceKind=object-storage` after a successful **Build** (peer HTTP JSON #3820). REST `POST /virtual/publish` is consumed from #3868 / cluster #3877 (not re-implemented). Repository / blank / unknown kinds still hide Preview/Build/Publish. Local object-key fixture only; `virtual.remoteUrl` / secrets stay off the envelope. Product-docs 8.2 admin Sites / Publishing / developer Virtual Sites / REST / reference site-config drop remaining “later phase” / “Publish hidden” wording for this kind.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — Publish allow-list includes `object-storage`
- `WebUI/src/main/ts/developer/messages.ts` — object-storage hint + Publish hint include filesystem target
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — comments
- `WebUI/src/main/ts/api/developer/types.ts` — DTO comment
- Vitest: `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx` — Build then Publish dest path for object-storage
- Playwright: `developer-site-virtual-source.spec.js` — intercept #3879 + live Build then Publish + on-disk HTML
- Fixture helper: `normalizeQaPublishDestPath` / `assertPublishedObjectStorageFilesOnQaCell` (fail-closed traversal)
- Product-docs 8.2: admin Sites, admin Publishing, developer Virtual Sites / REST, reference site-config
- Stacked consume: REST #3868 / cluster #3877

## Issues

None.

## Cross-platform path review

- [x] In-container dest/HTML paths are POSIX (`/` join after rejecting `..` and drive letters)
- [x] Host fixture paths use `path.join(__dirname, …)`
- [x] `docker exec` / `docker cp` use arg arrays and `container:posixDest`
- [x] Unit tests reject blank, relative, `C:/…`, and `..` dests
- [x] No Unix-only `/tmp` hardcodes in production UI

## Tests

- Vitest: `shouldShowVirtualPublishChrome("object-storage")` true; repository hidden
- Vitest: object-storage Publish remains after Build success; dest/filesCopied shown
- Playwright intercept: object-storage POST `/virtual/publish` HTTP 200 filesCopied
- Playwright live: save object-storage, Build, Publish, dest files exist with fixture marker, restore repository
- Helper unit: POSIX dest normalize + posixJoin
- Human QA #3878 not stolen

## Change-class closure

| Companion | Status |
|-----------|--------|
| Publish chrome for object-storage | yes (stacked REST #3868 + this live proof) |
| Repository still hides Preview/Build/Publish | yes |
| Vitest chrome/form helpers | yes |
| Playwright union spec | yes (30/30 H2 QA) |
| Product-docs 8.2 admin Sites | yes |
| REST adaptor | stacked consume #3868 (not re-implemented) |
| Human QA #3878 | not stolen |

## C5 evidence (pre-PR)

- `perc-devctl qa-up --skip-image-build` → `TEST_CMS_URL=http://127.0.0.1:9993`
- Hot-deploy: perc-system + rest + sitemanage SNAPSHOTs into WAR `WEB-INF/lib` (stale skip-image-build cell lacked object-storage allow-list); WebUI `cm/modern/assets` (replace dest dir); in-cell StopJetty/StartJetty
- `qa-health` RESULT:OK HTTP:200 HEALTH:healthy (pin `QA_CMS_HOST_PORT=9993`) after qa-up and after jar/asset copy + Jetty restart
- Playwright `npm run test:surface -- --path tests/developer-site-virtual-source.spec.js` — **30 passed**, 0 failed, 0 skipped (1.3m)
- console-clean=yes (pageerror listeners empty on live object-storage Publish)
- server.log-clean=yes (no ERROR/FATAL in CMS server.log for this feature)
