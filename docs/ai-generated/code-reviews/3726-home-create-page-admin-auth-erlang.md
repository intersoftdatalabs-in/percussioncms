# Erlang review — #3726 Home Create Page Admin authorization

**Scope:** uncommitted `fix/issue-3726-create-page-admin-auth` vs `origin/main`  
**Recommendation:** approve (partial slice; residual TX rollback)  
**Gate:** May commit/push: yes (folder-path + template-load + error mapping)  
**Memory patterns hit:** site name vs folder root bind; Playwright companion for WebUI screens; CMS paths use `/` (not OS separators)

## Summary

Home → Create → Page posted `folderPath=/Sites/${SITENAME}`. FastForward sample sites list as `Corporate_Investments` but the repository folder is `//Sites/CorporateInvestments` (#3326). Classic CUI resolves `PathItem.folderPath` via `get_folder_path` before `perc_page_manager.createPage`. The wizard skipped that lookup, so `getIdByPath` missed and `PSFolderHelper.addItem` tried to create a sibling folder under `//Sites`.

Follow-on H2 QA: with the repository folder, `templateDao.find` of **Page - Database Template** (assembly GUID) failed `getThumbImgPath().get(0)` then `addRecentTemplate` required a percTemplate content guid. Those are fixed. Live create still returns HTTP 500 `Transaction silently rolled back because it has been marked as rollback-only` — residual, not this slice’s remaining UI path bug.

## Issues

- Residual (not a gate on this slice): page save TX rollback-only after template load on Corporate_Investments + perc.pageDatabase. Playwright still fails with “Server Error”. File leftover issue.

## Cross-platform path checklist

- CMS finder/repository paths use `/` (URL-style), not OS file separators.
- New helpers normalize `//Sites` → `/Sites` for UI and re-prefix `//` only for page-create POST.
- No filesystem I/O, drive letters, or OS temp roots.

## Tests

- Vitest: `siteRootFolderFromSummary`, `repositoryFolderFromPathItem`, `mapSiteSummary`, `resolveSiteRootFolderPath`, `createPage` FF folder, PageWizard Corporate_Investments POST
- Playwright: `tests/home-react-editor.spec.js` Create Page Admin case
- Module `mvnw clean install`: WebUI BUILD SUCCESS; perc-qa-automation BUILD SUCCESS
