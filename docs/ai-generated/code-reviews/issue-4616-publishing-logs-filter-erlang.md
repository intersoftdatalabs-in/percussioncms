## Summary

Issue 4616 adds client-side search/status filtering on PublishingShell **Logs**, plus a **Failures only (server)** checkbox that sets `showOnlyFailures` on `POST …/pubstatus/logs`. Pure `filterLogEntries` / status helpers are covered by Vitest; `LogsSection` wiring is covered by a component test; Playwright mocks the logs POST and asserts row hide/show; `product-docs/8.2/admin/publishing.md` documents the operator flow. No blocking correctness, missing-behavioral-test, or non-portable path issues. Recommendation: **approve**. Uncommitted (including untracked QA/test files) must still be staged before commit.

## Scope

- Base: `origin/main` (no commits on branch vs main; all work is working-tree)
- Head: uncommitted + untracked on `fix/issue-4616-publishing-logs-filter`
- Files: 8 (4 modified, 4 untracked)
  - `WebUI/src/main/ts/publishing/logsFilter.ts`
  - `WebUI/src/main/ts/publishing/sections/LogsSection.tsx`
  - `WebUI/src/test/ts/publishing/logsFilter.test.ts`
  - `WebUI/src/test/ts/publishing/LogsSection.filter.test.tsx` (untracked)
  - `product-docs/8.2/admin/publishing.md`
  - `modules/perc-qa-automation/frontend/tests/helpers/publishing-logs-filter.js` (untracked)
  - `modules/perc-qa-automation/frontend/tests/publishing-logs-filter.spec.js` (untracked)
  - `modules/perc-qa-automation/frontend/tests/unit/publishing-logs-filter.test.js` (untracked)
- Prior report: none
- Memory patterns hit: change-class closure (WebUI screen + Playwright + product-docs); tests.structural-only (not hit — filter behavior is exercised); paths.hardcoded-sep (URL `/` only — not filesystem)
- Cross-platform path review: applied. Helpers join SPA **URLs** with `/` (correct). No filesystem path concatenation, Unix-only roots, or Windows-only paths.

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion
- File: `WebUI/src/main/ts/publishing/sections/LogsSection.tsx:246`
- Description: New filter chrome uses hardcoded English (`Status`, `Search`, `Failures only (server)`, placeholders) while neighboring controls use `message(MSG.*)` for section/table labels. Operators on non-English locales will see a mixed UI.
- Suggestion: Add TMX keys for the new labels and placeholders, matching existing Publish i18n, if this screen is considered locale-complete.
- Status: open

### Issue 2 -- Severity: suggestion
- File: `WebUI/src/main/ts/publishing/sections/LogsSection.tsx:84`
- Description: **Status** (client) and **Failures only (server)** can disagree: a user can request server failures then filter the loaded set to Success, or filter Failed while the server still returns mixed rows until Apply. Product docs explain the split, but the UI does not hint that Status does not re-fetch.
- Suggestion: Disable or sync the client status control when `showOnlyFailures` is checked, or auto-apply the server checkbox on change so the two filters cannot silently contradict.
- Status: open

### Issue 3 -- Severity: nit
- File: `WebUI/src/main/ts/publishing/logsFilter.ts:130`
- Description: Search is substring over a joined haystack (`hay.includes(q)`). Query `"1"` matches job `10`/`11` as well as `1`. Acceptable for a simple table filter; the unit test uses ids 1/2/3 so it does not show that.
- Suggestion: If operators expect exact job-id match, compare `String(jobId)` equality when `q` is numeric; otherwise leave as documented substring search.
- Status: open

### Issue 4 -- Severity: nit
- File: `modules/perc-qa-automation/frontend/tests/publishing-logs-filter.spec.js:75`
- Description: Playwright covers client search/status after a mocked POST. It does not assert that checking **Failures only (server)** posts `showOnlyFailures: true`. Vitest already covers that request field.
- Suggestion: Optional extra route assertion on POST body if E2E should own the server flag as well.
- Status: open
