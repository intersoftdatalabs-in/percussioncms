# Erlang review — #3351 blog navon type disposition

**Branch:** `fix/issue-3351-blog-navon-disposition`  
**Scope:** uncommitted Navigation blog-type support vs `origin/main`  
**Date:** 2026-08-14  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** do not invent a parallel product; sign IN/OUT instead of silent Missing; companion tests + product-docs + Playwright for WebUI screens.

## Summary

Signs blog-typed navons as **read-only** in Navigation: keep/i18n the **Blog** tree badge, extract `canRenameNavNode` so blogs cannot be renamed here, and document that create/edit stays on the existing Home dashboard Blogs gadget (`POST /sitemanage/section` `sectionType=blog`) and Home → Create for posts. No second blog editor. Vitest locks the action matrix and badge. Playwright asserts the signed note and that Navigation has no Create-blog action.

## Cross-platform path checklist

N/A — no new filesystem path I/O, installers, or OS path assertions.

## Issues

None blocking.

### Notes (not gates)

- Operator alternative is fully spelled in `product-docs/8.2/admin/architecture-navigation.md`; chrome `BLOG_NOTE` keeps the existing TMX tuid (no mass i18n).
- H2 sample sites typically have no blog navons; Playwright treats a live blog row as optional and always checks the signed note when the tree panel is present.

## Tests

- `WebUI/src/test/ts/architecture/NavTree.test.tsx` (blog badge + `data-section-type`)
- `WebUI/src/test/ts/architecture/ArchitectureShell.test.tsx` (#3351 action gating)
- `WebUI/src/test/ts/api/architecture/sectionMutations.test.ts` (`BLOG_NAVON_NAVIGATION_SUPPORT`, create-body blog fields)
- `modules/perc-qa-automation/frontend/tests/architecture-nav-blog-disposition.spec.js`

Standalone `WebUI` `mvnw clean install`: **BUILD SUCCESS**.  
Standalone `modules/perc-qa-automation` `mvnw clean install`: **BUILD SUCCESS**.  
Focused Vitest (`NavTree` / `ArchitectureShell` / `sectionMutations`): **69 passed**.  
Playwright `test:surface --path tests/architecture-nav-blog-disposition.spec.js`: **1 passed**; console-clean=yes; server.log-clean=yes.
