# Erlang review — #3759 Developer Sites SQL virtual Build chrome

**Branch:** `feat/issue-3759-sql-virtual-build-chrome`  
**Date:** 2026-08-23  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI chrome + Vitest + Playwright + product-docs); Playwright HARD GATE live H2 QA; stack/hot-deploy REST sibling without re-implementing adaptor; union spec with #3735 (do not drop source-kind cases); portable fixture paths (POSIX in-container, `path.join` on host).

## Summary

Parent #2678 slice 2 of 3. Developer Sites **Build Virtual Site** is clickable after save for `sourceKind=sql-database`. Chrome/helpers already allow-list sql-database on stacked #3735; this slice live-proves POST `/virtual/build` on H2 QA (pagesWritten > 0) using a SELECT-of-literals fixture (no `INIT=`/`RUNSCRIPT`, no table seed in another JVM). Repository still hides Build chrome. REST adaptor is stacked from #3758 (not re-implemented).

## Scope

- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — comment: SQL Build after saved sourceKind
- Playwright union: `modules/perc-qa-automation/frontend/tests/developer-site-virtual-source.spec.js` — intercept + live SQL Build; keep git/csv/#3735 cases
- Fixture: `tests/helpers/sql-virtual-qa-fixture.js`, `tests/fixtures/sql-virtual-site/`
- Unit: `tests/unit/sql-virtual-qa-fixture.test.js` (POSIX root, no INIT/Oracle)
- Product-docs 8.2: admin Sites/Publishing, developer REST/virtual-sites — operators Build SQL after save
- Stacked consume: REST Build #3758 / PR #3765, SQL source chrome #3735 / PR #3764
- No QA #2962 assignment

## Issues

None.

## Cross-platform path review

- [x] In-container root is POSIX literal (`/opt/Percussion/tmp/sql-virtual-qa`), not `path.join`
- [x] Host fixture paths use `path.join(__dirname, …)`
- [x] `docker cp` uses `container:posixDest`
- [x] No `INIT=` / `RUNSCRIPT` / file H2 (SPI rejects those JDBC tokens)
- [x] Line-ending assertions not added (YAML `query: |` is trimmed by SPI)

## Tests

- Vitest (stacked #3735): `shouldShowVirtualBuildChrome("sql-database")`; panel Build success for SQL
- Playwright intercept: sql-database GET + POST `/virtual/build` HTTP 200 pagesWritten=1
- Playwright live: save sql-database rootPath to fixture, Build, HTTP 200, pagesWritten > 0, restore repository
- Helper unit: POSIX root, container env, `_config.yaml` is H2 mem SELECT

## Change-class closure

| Companion | Status |
|-----------|--------|
| Build chrome for sql-database | yes (stacked #3735 + this live proof) |
| Vitest chrome/form helpers | yes (stacked) |
| Playwright union spec | yes |
| Product-docs 8.2 admin Sites | yes |
| REST/SPI adaptor | stacked consume #3758 / #3733 (not re-implemented) |
| Human QA #2962 | not stolen |
