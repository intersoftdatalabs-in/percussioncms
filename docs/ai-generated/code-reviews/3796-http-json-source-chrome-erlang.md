# Erlang review — #3796 Developer Sites http-json source chrome

**Branch:** `feat/issue-3796-http-json-source-chrome`  
**Date:** 2026-08-25 (re-review after rebase onto `origin/main`, HEAD `3391c510`)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI panel + Vitest + Playwright + product-docs); Playwright HARD GATE for screen work; omit-vs-empty remoteUrl keep/clear (#3568); consume REST/SPI siblings without re-implementing them.

## Summary

Parent #2678 slice 3 of 3 (HTTP JSON source chrome). Developer Sites Virtual Site source panel offers **HTTP JSON** (`http-json`) as a source-kind option, peer of csv-filesystem (#3687) and sql-database (#3735). Operators can select it, set the existing **Root path** field, save, reload, and switch back to **Repository (traditional)**. Git remotes / config / site key stay Git-only. Catalog URL (`http.url`) or local fixture (`http.file`) stay in `_config.yaml`; PUT never sends Authorization or API keys. **Build / Preview / Publish chrome is intentionally hidden** for http-json (later phase). PUT sends `remoteUrl: ""` / `branch: ""` so a prior Git remote is cleared (REST 400 if leftover remote). SPI #3794 / PR #3798 is on `main` (allow-list includes `http-json`); this slice consumes it. REST OpenAPI/tests remain sibling #3795.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteForm.ts` — `SOURCE_KIND_HTTP_JSON`, normalize, `formToVirtualProps`
- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — Build/Preview/Publish remain git/csv/sql only
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — select option + HTTP JSON hint + reuse root path
- `WebUI/src/main/ts/developer/messages.ts` — i18n keys (`perc.ui.developer@HTTP JSON`)
- `WebUI/src/main/ts/api/developer/types.ts` — allow-list javadoc
- Vitest: `virtualSiteForm.test.ts`, `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx`, `sitesApi.virtual.test.ts`
- Playwright: `modules/perc-qa-automation/frontend/tests/developer-site-virtual-source.spec.js`
- Product-docs: `product-docs/8.2/admin/sites.md`, `developer/virtual-sites.md`, `developer/rest.md`, `reference/site-config.md`
- Consumed (not re-implemented): SPI #3794 / REST helper allow-list on `main`
- No QA assignment

## Issues

None.

## Cross-platform path review

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] UI/tests use operator-style examples (`C:/http-json-docs`) as field values, not OS file joins
- [x] Playwright fill uses the same portable example style as SQL/CSV peers
- [x] Line-ending assertions not added

## Tests

- `virtualSiteForm` — normalize http-json; unknown (`sql-api`) still repository; PUT clears leftover Git remotes; no password/Authorization keys; root-required / root-unsafe for HTTP JSON
- `virtualSiteBuild` — http-json does **not** show Build/Preview/Publish chrome; git/csv/sql unchanged; repository / unknown stay hidden
- `VirtualSiteSourcePanel` — option list includes http-json; load root-only; save envelope; switch back to repository hides fields; no Build chrome after save
- Playwright — option present; HTTP JSON vs Git field visibility; mocked save envelope + GET round-trip; live save+reload then restore repository (H2 QA **20 passed**, including #3796 live + intercept tests)
- REST/SPI internals — N/A for this slice (consume #3794 allow-list; #3795 REST tests remain sibling)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Source-kind select + form helpers | yes |
| Vitest panel/form/build | yes |
| Playwright `developer-site-virtual-source.spec.js` | yes |
| Product-docs 8.2 admin Sites | yes |
| REST/SPI internals | consume SPI #3794 on main (not re-implemented); Build chrome out of scope |
| Human QA assignment | not created |

## Re-review (rebase onto main, PR #3800)

**Date:** 2026-08-25  
**HEAD:** `3391c510473468f2c8ef6fbe72e328d93466350d` (`rebase (finish)` onto `458f521a6b`)  
**Base:** `origin/main` (primary checkout `refs/heads/main`)  
**Conflicts:** product-docs only (`sites.md`, `rest.md`, `virtual-sites.md`, `site-config.md`)  
**Smoke:** `scripts/ci-smoke-product-docs.bat` reported OK by author (not re-run in this pass)

### Summary

Rebase union keeps main's REST GET/PUT `http-json` persist contract (safe `rootPath`, `virtual.remoteUrl` **400**, catalog URL/file in `_config.yaml`, no secrets on the envelope) and this PR's Developer Sites save/GET-roundtrip chrome. Build/Preview/Publish for `http-json` remains a later phase in UI, REST tables, and Vitest/Playwright. No conflict markers. WebUI form/panel/build helpers and tests survived the rebase. One leftover sentence in `virtual-sites.md` still calls Developer Sites chrome a "sibling slice"; it does not undo the union elsewhere. No blocking bugs, missing behavioral tests, or non-portable path I/O.

### Scope

- Base: `origin/main` (primary tree `product-docs/` as main-side)
- Head: `feat/issue-3796-http-json-source-chrome` worktree `night-issue-prs` @ `3391c510`
- Files: 14 (WebUI src+tests, Playwright spec, 4 product-docs, this report)
- Prior report: `docs/ai-generated/code-reviews/3796-http-json-source-chrome-erlang.md` (approve)
- Memory patterns hit: change-class companions; product-docs must match shipped behavior; path examples as field values not OS joins

### Recommendation

approve

### Gate

- Blocking bugs: 0
- May commit/push: yes

### Issues

#### Issue 1 -- Severity: nit
- File: `product-docs/8.2/developer/virtual-sites.md:338-339`
- Description: Union left main's REST-persist closer: "In-product Build, preview, publish, and Developer Sites chrome for `http-json` are sibling slices." Same page already documents Developer Sites save/GET-roundtrip (intro, Goals, property table, offline-build section). The leftover under-claims shipped save chrome; it does not drop REST persist details or claim Build chrome exists.
- Suggestion: Align with the offline-build paragraph: REST persist is on main; Developer Sites can save/GET-roundtrip; Build/Preview/Publish chrome remains a later phase.
- Status: open (optional; does not block)

### Cross-platform path review

Docs-only conflict resolution. Checklist applied to the unioned pages and unchanged WebUI/tests:

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] Operator examples remain portable field values (`C:/http-json-docs`, `C:/workspaces/product-docs`)
- [x] REST still documents NIO `Path.normalize()` / remaining `..` rejection
- [x] No line-ending assertions added

### Union check (conflict files)

| File | Main REST persist | PR save/GET-roundtrip chrome | Build chrome later |
|------|-------------------|------------------------------|--------------------|
| `product-docs/8.2/admin/sites.md` | kept | kept | kept |
| `product-docs/8.2/developer/rest.md` | kept | kept (one sentence on GET/PUT row) | kept |
| `product-docs/8.2/developer/virtual-sites.md` | kept | kept except REST-persist closer (nit) | kept |
| `product-docs/8.2/reference/site-config.md` | kept | kept | kept |
