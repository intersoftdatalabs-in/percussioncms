# Erlang review — #3612 Architecture bookmark / ?view=arch console-clean

**Branch:** `fix/issue-3612-arch-bookmark-console-clean`  
**Base:** `origin/main`  
**Date:** 2026-08-19  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** missing behavioral tests; change-class completeness (product-docs + Playwright); do not allowlist HTTP 500; FastForward rffNav* vs percNav* dual ids

## Summary

Cycle-verify residual of cluster #3610: `architecture-legacy-redirect.spec.js` already landed SPA Architecture (`#perc_site_map` count 0) but failed console-clean on `Failed to load resource: 500`. Live reproduce named the URL: `GET /Rhythmyx/services/sitemanage/section/tree/Corporate_Investments` → HTTP 500 `No content type info found for content type id: 315` (same root as #3611).

This branch cherry-picks #3611 (perc/rff JCR alias so type 315 loads via percNavTree 1017) and tightens the bookmark spec: wait for section/tree GET 200, record HTTP 5xx with method+URL, do **not** allowlist 500, do **not** restore `siteArchitecture.jsp`.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Backend alias logic already has behavioral tests (#3611). New Playwright helper has unit tests that prove 500 is tracked (not allowlisted) and that formatted hits name `section/tree`. Product-docs mention bookmark console-clean + tree GET 200.

Cross-platform path checklist: **clean** — no filesystem path construction; URLs use `/` (correct for HTTP).

## Issues

None (blocking).

### suggestion (non-blocking)

Stacked overlap with open PR #3614 (#3611). Reviewers should treat this PR as the bookmark console-clean acceptance gate; the JCR alias is the same backend. Merge order either way is fine as long as one lands.

## Change-class closure

| Companion | Status |
|-----------|--------|
| Identify 500 URL (`section/tree` type 315) | Done (live H2 before deploy) |
| perc/rff JCR alias (#3611 cherry-pick) | Done |
| Playwright bookmark + `?view=arch` tree 200 / console-clean | Done (2 passed) |
| Do not allowlist 500 / do not restore JSP host | Honored |
| `product-docs/8.2/admin/architecture-navigation.md` | Done |

## Tests / builds observed

- `cd system && ../mvnw.cmd clean install` BUILD SUCCESS — `PSNavNameAliasesTest` Tests run: 5; `PSServicesContentmgrTypedTest` Tests run: 6
- `cd projects/sitemanage && ../../mvnw.cmd clean install` BUILD SUCCESS — `PSSiteSectionServiceLoadTreeEmptyTest` Tests run: 8; `PSSiteSectionRestServiceLoadTreeEmptyTest` Tests run: 6
- Live H2 before deploy: tree GET 500 type 315
- Live H2 after `docker cp` perc-system + sitemanage + in-cell StopJetty/StartJetty: tree GET 200 with SectionNode children
- `npm run test:unit` includes `architecture-legacy-redirect.test.js` — 334 passed
- Playwright: `npm run test:surface -- --path tests/architecture-legacy-redirect.spec.js` **2 passed** (2.6s); console-clean=yes; no new type-315 ERROR after Jetty restart (BUG:#3606 FastForward GIF remains pre-existing)
