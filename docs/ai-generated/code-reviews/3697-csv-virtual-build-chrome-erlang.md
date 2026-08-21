# Erlang review: #3697 Developer Sites CSV virtual build chrome

**Branch:** `feat/issue-3697-csv-virtual-build-chrome`  
**Base:** stacked `origin/main` + #3687/#3694 (CSV source kind UI) + #3698/#3701 (REST csv-filesystem virtual/build)  
**Date:** 2026-08-21  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

**Memory patterns hit:** change-class companions (WebUI panel + Vitest + Playwright + product-docs); Playwright HARD GATE for screen work; Build vs Publish chrome split; portable host `path.join` vs POSIX container dest; fail-closed live QA (no skip).

## Summary

Parent #2678 slice. Developer Sites **Build Virtual Site** chrome is shown and invokable for `csv-filesystem` (peer git-filesystem #3020). `shouldShowVirtualBuildChrome` is git-filesystem **or** csv-filesystem; repository / unknown kinds stay hidden. `shouldShowVirtualPublishChrome` remains git-filesystem only (#3699 out of scope). CSV hint copy no longer says Build is Git-only. `buildVirtualSite` POSTs the Jackson/JAXB `{ VirtualSiteBuildRequest: … }` envelope (a bare `{}` is JAXB `NoSuchElementException` → HTTP 400). Playwright extends `developer-site-virtual-source.spec.js` with intercept HTTP 200 pagesWritten and a live H2 QA test that `docker cp`s a CSV fixture into the cell then Save + Build. Product-docs 8.2 admin Sites and developer Virtual Sites describe CSV Build. Stacked REST #3698 leftover `buildVirtualSite_rejectsCsvFilesystem` test removed (CSV build is now allowed).

## Cross-platform path checklist

- Host fixture paths use `path.join` / `fs.existsSync`.
- In-container dest is a POSIX constant (`/opt/Percussion/tmp/csv-virtual-qa-3697`) — URL/container path, not OS join.
- `docker cp` source is the Windows or Unix host file from `path.join`; destination is `container:posix`.
- Playwright live root path filled with the POSIX container path (Linux QA cell).

## Issues

None (gate-blocking).

### Notes (non-blocking)

- Live CSV Build test requires Docker QA cell `perc-matrix-cms-h2` (or `QA_CMS_CONTAINER`). Fail-closed if `docker cp` fails — do not skip.
- Publish chrome correctly stays git-only; do not treat missing Publish on CSV as a bug in this slice.

## Tests

- Vitest: `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx` (csv Build chrome + success, repository hides, publish still git-only).
- perc-qa-automation unit: `csv-virtual-qa-fixture.test.js`.
- Playwright: `tests/developer-site-virtual-source.spec.js` (chrome, intercept 200, live H2 Build).
