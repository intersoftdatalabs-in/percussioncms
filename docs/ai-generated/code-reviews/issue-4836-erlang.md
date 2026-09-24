<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue 4836 (uncommitted)

## Scope

- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Status: mkd-code-review 0.1.18, pack percussion, `--gate advisory`
- Commands:
  1. `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main` (11 tracked working-tree files; HEAD == `origin/main`)
  2. Same pack/format/gate with `--diff` of working tree **plus untracked** (`tmp/issue-4836-working.diff`, `a/`/`b/` prefixes) so Playwright + Java test files were in the machine pass
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/4836 (parent #4531 slice 31)
- Branch: `fix/issue-4836-clear-incremental-queue` (HEAD `d6f46de2f8` == `origin/main`; all work uncommitted)
- Files reviewed (11 modified + 2 untracked):
  - `WebUI/src/main/ts/api/publishing/publishApi.ts`
  - `WebUI/src/main/ts/i18n/message.ts`
  - `WebUI/src/main/ts/publishing/sections/SiteWorkspace.tsx`
  - `WebUI/src/test/ts/publishing/siteWorkspaceBadConfig.test.tsx`
  - `WebUI/src/test/ts/publishing/siteWorkspaceIncremental.test.tsx`
  - `WebUI/src/test/ts/publishing/siteWorkspaceQueueList.test.tsx`
  - `WebUI/src/test/ts/publishing/siteWorkspaceStopJob.test.tsx`
  - `product-docs/8.2/admin/publishing.md`
  - `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/IPSSitePublishService.java`
  - `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java`
  - `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishServiceWebAdapter.java`
  - `modules/perc-qa-automation/frontend/tests/publishing-incremental-queue-clear.spec.js` (untracked)
  - `projects/sitemanage/src/test/java/com/percussion/sitemanage/service/impl/PSSitePublishServiceClearQueueTest.java` (untracked)
- Memory: `~/.agents/skills/erlang/PATTERNS.md`
- Prior report: none for #4836; peer memory from #4787 list / #4788 remove-one-item
- Rule-file diffs: none
- Cross-platform path review: no new filesystem path I/O. REST/UI joins use URL `/` (`PATHS.INCREMENTAL_LIST` + encoded segments). Playwright matchers are URL regexes. Java tests use no `Path`/`File` construction. CLI `paths.hardcoded_sep` rows on `PSSitePublishService` are preexisting (site-name English, CMS finder `/`).

## This-diff behavior

PublishingShell site workspace gains **Clear queue** when incremental preview has at least one row. Confirm calls `DELETE …/sitemanage/publish/incremental/content/{site}/{server}` (no content id), then reloads the first page. Empty reload is success; leftover rows plus “still on the incremental queue” is a visible error; cancel does not DELETE; HTTP 403/404 stay errors.

Server: `IPSSitePublishService.clearQueuedIncrementalContent` resolves the named server to live vs staging the same way as list/remove-one, then `contentChangeService.deleteChangeEventsForSite(siteId, changeType)`. Missing site is 404 and does not delete. JAX-RS `DELETE` on the existing GET list path; remove-one stays the longer `/{contentId}` path.

Change-class companions in this tree: Vitest (confirm / cancel / leftover rows / 403), Playwright surface spec, product-docs `publishing.md`, sitemanage service + web adapter, Java service tests. This is internal CM1 `sitemanage` REST (same class as #4788), not a new `rest` module adaptor.

## CLI stdout (`mkd-code-review analyze --format markdown`)

### Pass A — `--git-base origin/main` (tracked working tree)

## Summary

Machine analysis found **6** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- In-diff: 0 finding(s); preexisting: 6
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:550 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 550)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:757 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 757)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:778 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 778)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:805 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 805)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:807 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 807)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:1026 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1026)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Pass B — `--diff` including untracked

## Summary

Machine analysis found **7** finding(s), **0** bug(s).

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 13 analyzed
- In-diff: 1 finding(s); preexisting: 6
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:550 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 550)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:757 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 757)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:778 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 778)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:805 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 805)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:807 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 807)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:1026 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1026)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/publishing-incremental-queue-clear.spec.js:67 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `stubQueueApis` cognitive=17 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang commentary

Gate counts **in-diff bugs**, missing behavioral tests, and non-portable path I/O only. Preexisting `paths.hardcoded_sep` on `PSSitePublishService` (English site-name list, CMS finder `/`) is outside this slice. Issue 7 is a test-helper complexity suggestion; the same `stubQueueApis` shape already shipped on `publishing-incremental-queue-remove.spec.js`.

Independent read of surrounding code:

- `clearIncrementalQueue` DELETE URL is the list path without a third segment; `removeIncrementalQueueItem` keeps `/{contentId}`. JAX-RS methods on `PSSitePublishServiceWebAdapter` match that (GET + DELETE on `{name}/{server}`, DELETE on `{name}/{server}/{contentId}`). `handleResponse` already treats HTTP 204 as success.
- Live vs staging uses `PSPubServer.STAGING` → `PENDING_STAGED`, else `PENDING_LIVE`, then `deleteChangeEventsForSite`. That is the same store as list/remove-one (site + change type). `PSPubServerQueueHelper` swallows failures; this slice calls the content-change service directly so 403/404/500 can surface.
- `isPublishAllowed()` still returns `true` (same as remove-one). UI 403 tests mock the HTTP contract.
- New files use Intersoft 2026 Apache headers.

Behavioral tests for the new logic:

| Path | Coverage |
|------|----------|
| Production server → `PENDING_LIVE` delete | `PSSitePublishServiceClearQueueTest.clearsLiveQueueForProductionServer` |
| Staging server → `PENDING_STAGED` delete | `clearsStagingQueueForStagingServer` |
| Missing site 404, no delete | `missingSiteIs404AndDoesNotDelete` |
| Confirm → DELETE + empty reload | Vitest + Playwright |
| Cancel → no DELETE | Vitest + Playwright |
| 204 with leftover rows | Vitest + Playwright |
| 403 stays an error, rows remain | Vitest + Playwright |

Suggestions (not blocking):

- `queueClearMessage` `not_found` and missing-server 404 have no dedicated UI/Java case; 403 and missing-site cover the same mappers. Add if you want parity with remove-one’s 404 Vitest.
- `stubQueueApis` cognitive 17 (CLI issue 7) — extract if the next queue spec copies it again.

Parent #4531 Agent progress is issue-tracker work, not this diff.

Recommendation: **approve**.

## Gate

- Blocking bugs: 0
- Missing behavioral tests: no
- Non-portable paths: no
- Recommendation: approve
- May commit/push: yes

> Co-Authored by Grok Build using grok-4.6 with agent Erlang Shen.
