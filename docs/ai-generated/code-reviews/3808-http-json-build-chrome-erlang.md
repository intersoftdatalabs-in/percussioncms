# Erlang review — #3808 Developer Sites http-json Build chrome

**Branch:** `feat/issue-3808-http-json-build-chrome`  
**Date:** 2026-08-25  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI chrome + Vitest + Playwright + product-docs); Playwright HARD GATE live H2 QA; split Preview/Publish allow-list from Build; portable fixture paths (POSIX in-container, `path.join` on host); no secrets on REST envelope.

## Summary

Parent #2678 slice: Developer Sites **Build Virtual Site** is shown and invokable after save for `sourceKind=http-json`. Preview and Publish chrome stay hidden for HTTP JSON (later phase). Repository still hides Build. REST/SPI consume factory already on `main` (not re-implemented). Live H2 QA `POST /virtual/build` returns pagesWritten > 0 against a local `pages.json` fixture.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — Build allow-list includes `http-json`; Preview/Publish remain git/csv/sql
- `WebUI/src/main/ts/developer/messages.ts` — HTTP JSON hint: save then Build; Preview/Publish later
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — comments
- `WebUI/src/main/ts/api/developer/types.ts` — javadoc
- Vitest: `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx`
- Playwright union: `developer-site-virtual-source.spec.js` — intercept + live HTTP JSON Build; keep git/csv/sql
- Fixture: `tests/helpers/http-json-virtual-qa-fixture.js`, `tests/fixtures/http-json-virtual-site/`, unit `http-json-virtual-qa-fixture.test.js` + `package.json` `test:unit` list
- Product-docs 8.2: admin Sites, developer Virtual Sites / REST, reference site-config
- No QA #2962 assignment

## Issues

None.

## Cross-platform path review

- [x] In-container root is POSIX literal (`/opt/Percussion/tmp/http-json-virtual-qa`), not `path.join`
- [x] Host fixture paths use `path.join(__dirname, …)`
- [x] `docker cp` uses `container:posixDest`
- [x] No Authorization / API keys in fixture YAML/JSON
- [x] Operator examples remain portable field values (`C:/http-json-docs`)
- [x] Line-ending assertions not added

## Tests

- `shouldShowVirtualBuildChrome("http-json")` true; Preview/Publish false; git/csv/sql unchanged; repository hidden
- Panel: Build chrome after load/save; click Build success; no Preview/Publish
- Playwright intercept: HTTP JSON GET + POST `/virtual/build` HTTP 200 pagesWritten=1
- Playwright live: save http-json rootPath to fixture, Build, HTTP 200, pagesWritten > 0, restore repository
- Helper unit: POSIX root, container env, `_config.yaml` http.file, pages.json, no secrets
- Surface spec **22 passed** on H2 QA (`TEST_CMS_URL=http://127.0.0.1:9993`)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Build chrome for http-json | yes |
| Preview/Publish stay hidden | yes |
| Vitest chrome/form helpers | yes |
| Playwright union spec | yes |
| Product-docs 8.2 admin Sites | yes |
| REST/SPI adaptor | consume factory on main (not re-implemented) |
| Human QA assignment | not created |
