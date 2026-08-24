# Erlang review — #3728 FF assembly template save rollback

**Scope:** uncommitted vs `HEAD` on `fix/issue-3728-ff-assembly-template-save-rollback`  
**Persona:** independent pre-commit (Erlang)  
**Memory patterns hit:** UnexpectedRollbackException after caught RuntimeException (#3393 / #1563); recent-template GUID type; FastForward SITENAME vs repository folder (#3326); WebUI Playwright companion; product-docs for Home Create.

## Summary

Home → Create → Page with FastForward **Page - Database Template** (`perc.pageDatabase`, assembly TEMPLATE / snippet assembler) marked the save transaction rollback-only. Causes: `templateDao.find` `List.get(0)` on empty PAGE_ASSEMBLER catalog/thumbs; `addRecentTemplate` `IllegalArgumentException` for non-`LEGACY_CONTENT` guids on `@Transactional` recentService. Fix: stub assembly templates instead of rethrowing; skip recent-template for assembly guids; isolate addToRecent in try/catch; map posted SITENAME folder to repository folder on save; empty HTTP 500 is not CREATE_NOT_AUTHORIZED.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

- Behavioral unit tests: `PSTemplateCopyHelpersTest` (empty thumbs/catalog); `PSPageServiceSaveRollbackHelpersTest` (recent GUID skip, folder map, swallow recent IAE); `PSRecentServiceLegacyGuidTest`; Vitest `formatApiError` empty-body 500.
- Playwright: `tests/home-react-editor.spec.js` Create Page Admin case (prefer Database Template).
- Product-docs: `product-docs/8.2/admin/index.md` Home → Create page FastForward assembly template.
- Cross-platform path checklist: CMS finder paths use `/` (not OS separators). No new filesystem path construction.

## Issues

None that block commit.

### Notes (not gates)

- Does **not** re-do #3726 / PR #3729 client `resolveSiteRootFolderPath`. Server maps SITENAME → repository folder (and FastForward `_` leaf strip) so save is 2xx even if the wizard still posts `/Sites/Corporate_Investments`.
- Assembly TEMPLATE guids skip `templateDao.find` on save (nested empty catalog/thumbs otherwise mark the request TX rollback-only).
- C5: `tests/home-react-editor.spec.js` Create Page Admin passed on H2 QA after hot-deploy. `pageChanged` may still warn rollback-only; save itself is 2xx and the editor opens.
