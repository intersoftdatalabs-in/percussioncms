# Erlang review — #3797 Architecture rename persist + move up/down

**Branch:** `fix/issue-3797-arch-rename-move`  
**Base:** `origin/main`  
**Date:** 2026-08-25  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** sample-workflow prepare NPE marks surrounding TX rollback-only (#3364 / #3676); JCR `PSContentNode` is a read-only wrapper; change-class completeness (adaptor tests with production types, product-docs, fail-closed Playwright); same-parent GUID `toString` includes revision so Move up/down no-ops

## Summary

Architecture **Rename** posted `POST /sitemanage/section/update` and got HTTP 500 (`UnexpectedRollbackException` / later JCR `LockException: Read-only instance`). **Move up/down** posted `POST /section/move` and either no-op'd (parent GUID revision mismatch) or 500'd (`item is not checked out`) because `reArrangeContentRelations` requires parent checkout.

This change:

- Skips folder ACL rewrite on title-only rename (`shouldSkipFolderPropertySave`) so Oval still sees `folderPermission` on the wire.
- Persists navon `displaytitle` via `loadItems` / `saveItems` (not JCR `setProperty`). Isolates `prepareForEdit` with `TransactionTemplate` `NOT_SUPPORTED` so sample-workflow NPE cannot mark the REST TX rollback-only. Skips prepare when the navon is already checked out (fresh create-section items).
- Same-parent move compares `PSLegacyGuid` content id (not revisioned `toString`) and reorders `sys_sortrank` on AA relationships without parent checkout / `reArrangeContentRelations`.
- Skips landing-page link-title checkout/save on this slice (landing-page is out of scope).
- Playwright `architecture-nav-mutations-smoke.spec.js` fail-closed: rename HTTP 200 + GET tree contains new title; move HTTP 200 + sibling order survives reload. Deep-links `?site=` so the chrome smoke sees treeitems.

Out of scope: landing-page, section-link, convert-to-folder, ACL dialog. Does not steal QA #3151.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests use production types (`PSLegacyGuid`, `PSSiteSectionProperties`, `PSFolderPermission`). Cross-platform path checklist: **clean** — no new filesystem path construction.

## Issues

None (blocking).

Non-blocking: landing-page link title is not updated on Architecture rename (explicit skip; residual of #3151 landing-page slice).

## Change-class closure

| Companion | Status |
|-----------|--------|
| Title persist without rollback-only 500 | Done (`PSManagedNavService.setNavonProperties` + isolated prepare) |
| Skip folder ACL save when folder name unchanged | Done (`PSSiteSectionService.shouldSkipFolderPropertySave`) |
| Same-parent move by content id + no parent checkout | Done (`sameNavonContentId` + `rearrangeSameParentChild` / `saveAaRelationships`) |
| Adaptor/unit tests with exact production types | Done (`PSManagedNavServiceSetNavonPropertiesTest`, `PSManagedNavServiceSameParentMoveTest`, `PSSiteSectionServiceUpdateRenameTest`) |
| Playwright fail-closed H2 QA | Done (`architecture-nav-mutations-smoke.spec.js` 2 passed) |
| `product-docs/8.2/admin/architecture-navigation.md` | Done (Rename + Move up/down rows) |

## Tests / builds observed

Standalone `mvnw clean install`: `system` BUILD SUCCESS (Tests run: 2419, Failures: 0); `projects/sitemanage` BUILD SUCCESS (Tests run: 1540, Failures: 0). WebUI Vitest 3074 passed (clean install in progress / recorded in PR). C5: `perc-devctl qa-health` RESULT:OK HTTP:200 HEALTH:healthy `TEST_CMS_URL=http://127.0.0.1:9993`; Playwright `npm run test:surface -- --path tests/architecture-nav-mutations-smoke.spec.js` **2 passed**; console-clean=yes; server.log-clean=yes (no new ERROR during the passing run).
