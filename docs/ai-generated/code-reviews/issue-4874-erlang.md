<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# Erlang Code Review — GH-4874 Explorer create a page in the selected folder

## Summary

The prior block is closed. Folder-root matching now uses the last non-empty CMS path segment, and `longerFolderNameDoesNotStealMobilePreview` lists `EnterpriseInvestmentsArchive` before `EnterpriseInvestments`. That test passed. The Explorer create-page dialog, list refresh, Vitest, Playwright spec, and `product-docs` note still match issue #4874.

**Recommendation: approve.**

Independent review. This session did not author the diff.

## Scope

- Base: `origin/main` (`1fd74d4121`)
- Head commit: `13980702c5` `feat(explorer): create a page in the selected folder (#4874)`
- Plus uncommitted working tree (tracked edits and two untracked tests)
- Branch: `fix/issue-4874-explorer-create-page`
- Issue: [#4874](https://github.com/intersoftdatalabs-in/percussioncms/issues/4874) — parent #4530 slice 42
- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Pattern memory: `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md` and `~/.local/share/mkd/agents/erlang/PATTERNS.md`
- Machine pass: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --machine-only --diff` (working tree vs `origin/main`, including the untracked tests). Exit 0. LLM stage skipped. Advisory "May commit/push: yes" agrees with the strict gate below.

| Path | Role |
|------|------|
| `WebUI/src/main/ts/contentExplorer/CreatePageDialog.tsx` | Name + page type dialog |
| `WebUI/src/main/ts/contentExplorer/createPageInFolder.ts` | Page-type filter and field validation |
| `WebUI/src/main/ts/contentExplorer/createPageErrors.ts` | Field and HTTP error text |
| `WebUI/src/main/ts/contentExplorer/ReducedActions.tsx` | Toolbar button and create handler |
| `WebUI/src/main/ts/contentExplorer/ContentExplorerShell.tsx` | Create, then refresh the open folder list |
| `WebUI/src/main/ts/contentExplorer/messages.ts` | `perc.ui.explorer@…` keys |
| `WebUI/src/test/ts/contentExplorer/ContentExplorerShell.createPage.test.tsx` | Confirm, cancel, blank name, HTTP 409 |
| `WebUI/src/test/ts/contentExplorer/createPageInFolder.test.ts` | Type filter and field validation |
| `modules/perc-qa-automation/frontend/tests/explorer-create-page.spec.js` | H2 surface spec; waits for a real type option; asserts the name cell |
| `product-docs/8.2/admin/content-explorer.md` | Reduced-actions note; `id: admin-content-explorer` unchanged |
| `projects/sitemanage/.../PSFolderHelper.java` | `folderRootLeafEquals` + `applyPageMobilePreview` |
| `projects/sitemanage/.../PSPageChangeHandler.java` | `UnexpectedRollbackException` reload logs at warn |
| `.../PSFolderHelperPagePreviewTest.java` | Untracked |
| `.../PSPageChangeHandlerRollbackTest.java` | Untracked |

## Change class

**WebUI Explorer screen (create one page in the selected folder) plus a folder-list transaction fix** so the follow-up list does not roll back the create.

### Closure

| Companion | Present |
|-----------|---------|
| Dialog: confirm creates, cancel does not, blank name stays an error | yes — Vitest and Playwright |
| Missing content type / path separators | yes — `validateCreatePageFields` unit test; dialog uses it |
| HTTP failure stays in the dialog and does not add a row | yes — shell Vitest throws status 409 |
| List shows the new page | yes — shell refreshes the list; Vitest asserts `detail-row-99`; Playwright asserts the name cell |
| Playwright on the product screen | yes — `explorer-create-page.spec.js` waits for a non-empty option value before confirm |
| `message(EXPLORER_MSG.*)` chrome (no bare English labels) | yes |
| Vitest `renderA11yGate` and Playwright `expectNoSeriousA11yViolations` | yes |
| `product-docs/8.2/` | yes — first allowed template, cancel, 400/403/404/409. Matches `templates[0]` |
| Server: do not call `loadSite` for a page folder leaf that is not the site name | yes — `findSite`, then last-segment folder-root match |
| Behavioral test of a longer folder name listed first | yes — `longerFolderNameDoesNotStealMobilePreview` |
| Behavioral test of the rollback log predicate | yes — `isSilentRollback` |
| Agent rule files | none |
| Dual-ship `WebUI/war/**` | N/A — TS source is canonical |

`PSSiteManager` is `@Transactional`. `loadSite(String)` throws `PSNotFoundException` on a miss. `findSite(String)` returns null (`bySimpleNaturalId().load`). The page path calls `findSite` and `findAllSites`. Both new tests assert `never().loadSite(...)`.

Focused JDK 21 run (not a full module `clean install`):

```text
cd projects/sitemanage
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ../../mvnw -Dtest=PSFolderHelperPagePreviewTest,PSPageChangeHandlerRollbackTest test
```

BUILD SUCCESS. Tests run: 5, Failures: 0, Errors: 0. No compiler warnings on `PSFolderHelper`, `PSPageChangeHandler`, or the two new test classes. Pre-existing unchecked warnings elsewhere in the module are unchanged.

## Prior block

`PSFolderHelper.folderRootLeafEquals` (`PSFolderHelper.java:1338`) keeps the last non-empty segment after splitting on `/` (a leading `\` is normalized first). Comparison is `leaf.equals(last)`.

`//Sites/EnterpriseInvestmentsArchive` ends in `EnterpriseInvestmentsArchive`. `//Sites/EnterpriseInvestments/` ends in `EnterpriseInvestments` (trailing slash dropped by `split`). A checked stand-in of that method returns false for the archive root and true for the real root, including the trailing-slash form.

`PSFolderHelperPagePreviewTest.longerFolderNameDoesNotStealMobilePreview` (`PSFolderHelperPagePreviewTest.java:53`):

- `findSite("EnterpriseInvestments")` returns null
- `findAllSites()` is `List.of(archive, real)` — archive first
- archive root `//Sites/EnterpriseInvestmentsArchive`, `isMobilePreviewEnabled` false
- real root `//Sites/EnterpriseInvestments/`, `isMobilePreviewEnabled` true
- page path `//Sites/EnterpriseInvestments/Pages` (segment index 3 is the site folder)
- asserts the flag is true and `loadSite` is never called

`PSPathItem.mobilePreviewEnabled` defaults to false, and the archive stub is false, so a first-hit substring match fails this test. The test passed.

`/` here is the CMS folder separator. It is a repository path, same as the rest of `PSFolderHelper`.

## Issues

### 1 — suggestion — `applyPageMobilePreview` is still over the pack complexity cap

`PSFolderHelper.java:1352`

Cognitive 17 / cyclomatic 16 (pack max 15). `folderRootLeafEquals` already holds the segment check. `pageChanged` was already over the cap (19/19) before this diff. Do not block on either score.

### 2 — suggestion — full site scan on a natural-id miss

`applyPageMobilePreview` calls `findAllSites()` when `findSite(leaf)` is null. For FastForward that miss is the normal case (`EnterpriseInvestments` vs `Enterprise_Investments`). `setFolderAccessLevel(List)` still calls `setFolderAccessLevel(PSPathItem)` only for the first leaf, then copies that ACL, so the scan runs once per such list. Later page rows in the same list still do not get their own preview flag. That short-circuit was already there when the lookup lived inside `setFolderAccessLevel(PSPathItem)`.

### 3 — nit — the page-row note sits above the wrong method

`PSFolderHelper.java:1326`

The comment about `loadSite` vs `findSite` is immediately followed by the `folderRootLeafEquals` doc comment, so it is not attached to `applyPageMobilePreview`. Move it onto `applyPageMobilePreview`. The JDK 21 compile of this module did not warn.

## Machine pass

Advisory, machine-only: **8** findings, **0** counted bugs, recommendation `approve`, **May commit/push: yes**. One in-diff finding (complexity, issue 1). Seven preexisting.

| CLI item | Disposition |
|----------|-------------|
| `PSFolderHelper.java` lines 315, 429, 543, 561, 568, 592 `paths.hardcoded_sep` (preexisting) | Repository and URL `/`. Line 315 is a `/Rhythmyx` thumbnail URL plus a `PSPathUtils` finder path. |
| `pageChanged` cognitive complexity (preexisting) | Issue 1. |
| `applyPageMobilePreview` cognitive complexity (in diff) | Issue 1. |

No secrets, no new agent-rule files, no non-portable filesystem joins, no wrong-type test fakes. Mocks are `IPSSite` and `IPSSiteManager`.

## Recommendation

**approve**

Commit the two untracked sitemanage test classes with `PSFolderHelper` and `PSPageChangeHandler`. The Playwright spec change (non-empty type option, name cell) belongs in that commit as well.

Gate: PASS
May commit/push: yes

> Co-Authored by Grok Build using grok-4.7 with agent erlang.
