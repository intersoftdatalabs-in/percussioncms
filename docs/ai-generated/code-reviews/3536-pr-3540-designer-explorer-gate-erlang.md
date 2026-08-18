# Erlang review — PR #3540 follow-up (Explorer ungated for Designer)

**Date:** 2026-08-17  
**Scope:** Uncommitted follow-up on `fix/issue-3536-profile-landing-explorer` (Kilo threads on PR #3540).  
**Recommendation:** approve  
**May commit/push:** yes  
**Memory patterns hit:** dual-tree lockstep WebUI JSP hosts; behavioral tests for role-gated landing; do not treat source-string tests as sole proof (JSP parse is paired with `PSDefaultLandingView` role assertions).

## Summary

Kilo asked to add `"explorer"` to `designerViews` in both `index.jsp` hosts. That list is only consulted inside `if (isAdminView && !admin)`. Explorer is not in `adminViews`, so Designer stored landings are already not reset. Adding explorer to `designerViews` alone is a no-op; adding it to `adminViews` would fail-closed Contributor landings.

This change documents that contract on both dual-tree JSPs and locks it with:

- `PSDefaultLandingViewTest` — Designer + Contributor + Admin `resolveAuthorizedView` / `isViewAuthorized`; `isRoleGatedView(VIEW_EXPLORER) == false`
- `spaCutover.test.ts` — both JSP hosts: explorer in `spaViews`, absent from `adminViews` and `designerViews`

No production authorization change.

## Gate

- **Bugs:** none
- **Behavioral tests:** present for Java role mapping; JSP allowlist contract covered via dual-tree parse + Java behavior
- **Cross-platform paths:** no new filesystem joins; existing `read()` normalizes CRLF
- **Change-class companions:** review-thread fix only (comments + tests). No new UI surface; Playwright N/A. Product-docs N/A (behavior unchanged).
- **New warnings:** none attributable (WebUI `cd WebUI && ../mvnw.cmd clean install` BUILD SUCCESS; Vitest 2747 passed; `PSDefaultLandingViewTest` 11/0)

## Issues

None.
