# Erlang review — traditional site managed-navigation flag

**Branch:** `feat/traditional-site-managed-nav-flag`  
**Base:** `origin/main`  
**Date:** 2026-08-13  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** missing behavioral tests; change-class completeness (product-docs + Playwright for WebUI); shared-context DI (N/A)

## Summary

Traditional Create Site always planted a NavTree + homepage. Operators need a site with no CMS managed navigation. Virtual Sites already have `virtual.sourceKind` and must not get a second flag.

This change adds `navigation.managed` / `managedNavigation` for **traditional sites only**. Default remains include-nav. `false` creates the folder only. Virtual GET/list omit the flag. Create Site wizard checkbox (default on) plus product-docs and Playwright chrome coverage.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

Cross-platform path checklist: **clean** (no new filesystem joins).

## Issues

None blocking.

### Suggestions (non-blocking)

1. Persist of `navigation.managed=false` is best-effort when no publishing context exists. Create still skips the NavTree via the DTO flag. Acceptable.
2. WebUI Maven `clean install` was not run (Vitest + Playwright helper unit tests were). Frontend packaging is unchanged except TS sources.

## Change-class closure

| Companion | Status |
|-----------|--------|
| Helper + persist + create skip | Present |
| REST `Site.managedNavigation` + adaptor mapping | Present |
| Create Site SPA checkbox | Present |
| Playwright helper + spec | Present |
| product-docs (sites, navigation, site-config) | Present |
| Virtual Sites | Flag omitted (by design) |

## Tests / builds observed

- `system`: standalone `clean install` BUILD SUCCESS; `PSManagedNavSiteHelperTest` 5/5
- `rest`: standalone `clean install` BUILD SUCCESS
- `sitemanage`: standalone `clean install` BUILD SUCCESS — Tests run: 1126, Failures: 0 (`PSSiteContentDaoManagedNavTest` 2/2)
- WebUI Vitest: `siteCreateApi` + `SiteCreateWizard` 18/18
- perc-qa-automation `npm run test:unit` 263/263
