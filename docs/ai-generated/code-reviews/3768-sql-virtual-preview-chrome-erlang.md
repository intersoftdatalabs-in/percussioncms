# Erlang review: #3768 SQL Virtual Site Preview chrome

**Branch:** `fix/issue-3768-sql-virtual-preview-chrome`  
**Base:** `origin/cluster/night-issue-20260823-sql-virtual-site` (stack on #3777; do not re-implement REST)  
**Scope:** uncommitted WebUI copy/Vitest + Playwright live Preview + `product-docs/8.2/admin/sites.md`  
**Date:** 2026-08-23  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (WebUI + Vitest + Playwright + product-docs); UI live proof; portable POSIX in-container fixture roots

## Summary

Developer Sites already showed Preview for `sql-database` via `shouldShowVirtualPreviewChrome` (same allow-list as Build). This slice closes operator copy and live proof: SQL hint and Preview hint mention **Preview assembled site** / **SQL database**; Vitest clicks Preview for `sql-database`; Playwright live save+Build+GET `/virtual/preview` + home HTML; admin Sites documents SQL Preview after Build. Repository still hides Preview. REST last-build Preview is consumed, not reimplemented.

## Change-class closure

| Companion | Status |
|-----------|--------|
| WebUI chrome / i18n fallback | `SITE_VIRT_SQL_HINT`, `SITE_VIRT_PREVIEW_HINT` |
| Vitest | `VirtualSiteSourcePanel.test.tsx` SQL Preview click + hint |
| Playwright | `sql-database live Preview assembled site after Build (#3768)` |
| Product-docs | `product-docs/8.2/admin/sites.md` Preview procedure includes SQL |
| REST | Out of scope — cluster #3777 / #3767 |

## Cross-platform path checklist

- No new filesystem path joins in production.
- Playwright homePath normalizes `\` → `/`; fixture root is POSIX in-container (`/opt/Percussion/tmp/sql-virtual-qa`).
- Preview URL segments are encoded; `..` / `.` filtered (peer CSV #3707).

## Issues

None (bugs). Nits: none blocking.

## Evidence (informational)

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS. Java Tests run: 63. Vitest 390 files / 3016 tests.
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` — BUILD SUCCESS. `npm run test:unit` 441 passed.
- C5: `perc-devctl qa-up --skip-image-build` `TEST_CMS_URL=http://127.0.0.1:9993`; hot-copy cluster `perc-system`/`rest`/`sitemanage` + WebUI `cm/modern`; in-cell StopJetty/StartJetty; `qa-health` RESULT:OK HTTP:200 HEALTH:healthy; Playwright 18 passed including #3768; console-clean=yes; server.log-clean=yes (no virtual/preview ERROR).
