# Erlang review — #3821 Architecture Delete enabled for non-root section

**Branch:** `fix/issue-3821-architecture-delete-nonroot`  
**Base:** `origin/main`  
**Date:** 2026-08-25  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** missing behavioral tests; change-class completeness (Vitest + Playwright + product-docs for WebUI screens); URL `/` vs filesystem paths

## Summary

After Architecture **Create section**, the new non-root navon was not selected (selection stayed on the parent/root or empty). `canDeleteNavNode` is already true for any non-root selected node, so `architecture-action-delete` stayed disabled. This slice auto-selects the created child (POST `SiteSection` id, else parent-child diff / title hint after tree reload). Root remains not deletable. Confirm-cancel does not delete; confirm DELETEs and the tree reload drops the node.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests: `resolveCreatedNavNodeId` + `canDeleteNavNode`; `createSiteSection` parse; ArchitectureShell enablement after Create (id and title-fallback) and root still disabled; Playwright H2 surface create → Delete enabled → cancel keeps → confirm removes. Product-docs Create/Delete procedures updated.

Cross-platform path checklist: **clean** — no filesystem path joins; REST/Playwright matchers use URL `/`; helper unit tests use `http://127.0.0.1` URLs.

## Issues

None (blocking).

### Suggestions (non-blocking)

1. DELETE success is HTTP 204 (client already treats 204 as success). Playwright asserts 2xx rather than 200 only.
2. Sibling spec `#3797` rename POST 500 on a 4-day-old `--skip-image-build` matrix image is backend stale vs merged PR #3816, not this selection fix.

## Change-class closure

| Companion | Status |
|-----------|--------|
| Auto-select after Create (`ArchitectureShell` + `resolveCreatedNavNodeId`) | Present |
| Vitest helper + shell enablement after create; root still blocked | Present |
| Playwright `architecture-nav-mutations-smoke.spec.js` (#3821) | Present |
| Helper `isSectionDeleteRequest` + unit tests | Present |
| `product-docs/8.2/admin/architecture-navigation.md` | Present |
| rest/sitemanage | N/A (delete REST already works) |

## Tests / builds observed

- `WebUI`: standalone `mvnw.cmd clean install` BUILD SUCCESS; Vitest 392 files / 3079 passed
- `modules/perc-qa-automation`: standalone `mvnw.cmd clean install` BUILD SUCCESS; helper unit tests 452 passed
- Playwright H2: `qa-up --skip-image-build` `TEST_CMS_URL=http://127.0.0.1:9993`; hot-copy `WebUI/target/generated-webui/cm/modern/assets/` into cell WAR; `qa-health` RESULT:OK HTTP:200 HEALTH:healthy; `npm run test:surface -- --path tests/architecture-nav-mutations-smoke.spec.js --grep "#3821"` 1 passed
- console-clean=yes (spec filters known noise); 3821 path did not add feature-related server.log ERROR/FATAL
