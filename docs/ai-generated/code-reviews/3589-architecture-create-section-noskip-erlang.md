# Erlang review — #3589 Architecture create-section no-skip

**Branch:** `fix/issue-3589-architecture-create-section-noskip`  
**Base:** `origin/main`  
**Date:** 2026-08-19  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** missing behavioral tests; change-class completeness (Playwright + product-docs + installer companions); do not seed a second NavTree

## Summary

H2 QA sample sites already have `rffNavTree` items (type 315). `perc.nav` remaps `percNav*` to 1015–1017, and leftover `psx_cerffNav*.xml` editors were `active="no"`, so `GET /section/tree/{site}` returned HTTP 500 (`No content type info found for content type id: 315`) and Create stayed disabled. This change starts those rff nav editors (313–315), copies them from `installSampleSites`, and adds an H2 Playwright no-skip surface for Escape-to-close plus create/enabled dialog. Does not seed a second NavTree.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests: Node unit helpers, `InstallSampleSitesWiringTest` (copy list + `active="yes"`), Playwright no-skip + mutations smoke.

Cross-platform path checklist: **clean** — URL joins use `/`; no OS filesystem concatenation; helper unit tests use `http://127.0.0.1` URLs.

## Issues

None (blocking).

### Suggestions (non-blocking)

1. Existing cells still have `active="no"` leftover XML until reinstall or an operator activates the three editors and restarts Jetty (proven on this QA cell).
2. Second Playwright test accepts a fully enabled dialog when templates fail to load; H2 sample sites loaded templates in this run.

## Change-class closure

| Companion | Status |
|-----------|--------|
| Installer copy + `active="yes"` on rff nav editors | Present |
| `InstallSampleSitesWiringTest` | Present (7 tests) |
| Playwright no-skip + helper unit | Present |
| `product-docs/8.2/admin` Navigation + Sites | Present |
| Do not seed second NavTree | Honored |

## Tests / builds observed

- `modules/perc-qa-automation`: standalone `clean install` BUILD SUCCESS; `npm run test:unit` 325 pass
- `modules/perc-distribution-tree`: standalone `clean install` BUILD SUCCESS — Tests run: 268, Failures: 0, Errors: 0, Skipped: 5
- Playwright H2: `architecture-create-section-noskip.spec.js` 2 passed; `architecture-nav-mutations-smoke.spec.js` 1 passed
- console-clean=yes; server.log-clean=yes for the post-restart test window (no new type-315 ERROR)
- `scripts/ci-smoke-product-docs.bat` OK
