# Erlang review: #3819 HTTP JSON Virtual Site Preview chrome

**Branch:** `feat/issue-3819-http-json-preview-chrome`  
**Base:** `origin/main`  
**Scope:** uncommitted WebUI Preview chrome + Vitest + Playwright live Preview + product-docs 8.2  
**Date:** 2026-08-25  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (WebUI + Vitest + Playwright + product-docs); UI live proof; portable POSIX in-container fixture roots

## Summary

Developer Sites **Preview assembled site** is shown and invokable for `sourceKind=http-json` after a successful Build (peer git-filesystem #3299 / CSV #3707 / SQL #3768). `shouldShowVirtualPreviewChrome` now includes `http-json`. Build chrome from #3808 stays visible. Publish chrome stays hidden (slice 3 / #3820). REST last-build Preview is consumed, not reimplemented (#3807 / cluster #3817).

## Change-class closure

| Companion | Status |
|-----------|--------|
| WebUI chrome / i18n fallback | `shouldShowVirtualPreviewChrome`; `SITE_VIRT_HTTP_JSON_HINT`; `SITE_VIRT_PREVIEW_HINT` |
| Vitest | `virtualSiteBuild.test.ts`; `VirtualSiteSourcePanel.test.tsx` load/save/build + Preview click |
| Playwright | `http-json live Preview assembled site after Build (#3819)` plus chrome visibility |
| Product-docs | `product-docs/8.2/admin/sites.md` Preview after HTTP JSON Build; developer/reference aligned |
| REST | Out of scope — cluster #3817 / #3807 |

## Cross-platform path review

- No new filesystem path joins in production (`shouldShowVirtualPreviewChrome` is a string allow-list).
- Playwright homePath normalizes `\` → `/`; fixture root is POSIX in-container (`/opt/Percussion/tmp/http-json-virtual-qa`).
- Preview URL segments are encoded; `..` / `.` filtered (peer SQL #3768 / CSV #3707).

## Issues

None (bugs). Nits: none blocking.

## Evidence (informational)

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS. Vitest Test Files 392 passed / Tests 3076 passed.
- C5: `perc-devctl qa-up --skip-image-build` `TEST_CMS_URL=http://127.0.0.1:9993`; hot-copy `perc-system`/`rest`/`sitemanage` + WebUI `cm/modern/assets`; in-cell StopJetty/StartJetty; `qa-health` RESULT:OK HTTP:200 HEALTH:healthy; Playwright 23 passed including #3819; console-clean=yes; server.log-clean=yes (0 ERROR/FATAL; no virtual/preview ERROR).
