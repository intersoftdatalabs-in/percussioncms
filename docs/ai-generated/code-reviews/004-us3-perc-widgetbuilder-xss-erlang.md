# Erlang review — 004/us3-perc-widgetbuilder-xss

## Summary

Single-commit branch (`925c97f44`, sole commit ahead of `development`) fixes 6
GitHub CodeQL `js/xss-through-dom` alerts by refactoring two `showErrors()`
sinks — `WidgetDefinitionGeneralView.showErrors()` in
`PercWidgetBuilderDefinitionView.js` and `FieldEditorView.showErrors()` in
`PercWidgetFieldsViews.js` — from unsafe HTML-string concatenation +
`.append(html)` into safe DOM-API construction
(`$("<label></label>").addClass().attr().css().text(message)`). The fix is
applied byte-identically across all three deployed copies of each file
(`WebUI/src/main/webapp/cm/widgetbuilder/js/views/`,
`WebUI/src/main/webapp/cm/app/widgetbuilder/js/views/`,
`WebUI/war/widgetbuilder/js/views/`) and is backed by two new Vitest
regression suites. No bugs found; the change is narrowly scoped, correct, and
independently verified fail-then-pass against the pre-fix source. Two
non-blocking follow-up items are noted (test coverage on the two untested
lockstep copies, and other pre-existing instances of the same unsafe pattern
elsewhere in the codebase that were not part of this alert batch).

## Scope

- Base: `development` (`0278bf348`)
- Head: `925c97f44` (tip of `004/us3-perc-widgetbuilder-xss`, single commit)
- Files: 8 changed (3 lockstep copies × 2 source files + 2 new Vitest test
  files)
- Prior report: none found under `docs/ai-generated/code-reviews/` for this
  branch/topic
- Memory patterns hit:
  - `patterns.md` → "Multi-copy shared WebUI / package assets edited in only
    one of several lockstep paths" — checked; **not violated** here (all 3
    copies edited identically, verified below).
  - `patterns.md` → "Structural / registry / XML string presence treated as
    sole proof of runtime behavior" — checked; **not violated** — new tests
    are behavioral (DOM assertions), see Testing verification below.

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Verification performed (not just read-through)

1. **Diff read in full** via `git show 925c97f44` and `git diff
   development...925c97f44`; also read the complete post-fix content of all 4
   distinct file bodies (2 files × 2 representative copies) plus the 2 new
   test files in full (not only the hunks).
2. **Lockstep check**: diffed the `app/`, `main/webapp/`, and `war/` copies of
   both `PercWidgetBuilderDefinitionView.js` and `PercWidgetFieldsViews.js`
   against each other post-fix — the `showErrors()` sink logic is identical
   in all 3 copies of each file (only pre-existing surrounding formatting
   differs, e.g. `war/` copy uses older un-prettified style, which predates
   this change).
3. **Behavioral test execution (fixed source)**: ran
   `npx vitest run src/test/js/percWidgetBuilderDefinitionView.test.js
   src/test/js/percWidgetFieldsViews.test.js` from `WebUI/` — **6/6 pass**
   against the committed (fixed) source.
4. **Fail-then-pass re-verification (pre-fix source)**: temporarily swapped
   `WebUI/src/main/webapp/cm/widgetbuilder/js/views/PercWidgetBuilderDefinitionView.js`
   and `.../PercWidgetFieldsViews.js` for the pre-fix blobs from commit
   `0278bf348` (byte-for-byte via `git show <rev>:<path>`, UTF-8, no line-ending
   collapse) and re-ran the same two test files: **all 6 cases genuinely
   fail** against the vulnerable source — jsdom actually executes the
   injected `<script>alert('XSS')</script>` (`Error: Not implemented:
   window.alert`, confirming a live script node was created and run),
   `querySelectorAll("img")`/`("script")` find 1 element instead of 0, and
   `textContent` for the `<b>bold</b>` payload comes back as the full HTML
   string instead of `bold`. This is a real behavioral demonstration of the
   XSS, not a token/structural check, and matches the commit message's
   fail-then-pass claim.
5. **Restore**: `git reset --hard 925c97f44` to return the working tree to
   the exact reviewed commit state; re-ran the 2 new test files — 6/6 pass
   again, tree clean (`git status -sb` shows no diff from `925c97f44`).
6. **Test-runner wiring check**: confirmed `WebUI/package.json` → `"test":
   "vitest run"` uses `WebUI/vite.config.ts`, whose `test.include` covers
   `src/test/js/**/*.{test,spec}.js` — the new test files' location
   (`WebUI/src/test/js/*.test.js`) is picked up by the existing test runner
   without additional config changes.
7. **Cross-platform path/file I/O checklist**: **N/A / clean** — the diff
   touches no filesystem path construction in production code. The two new
   test files use `resolve(__dirname, "../../main/webapp/...")` via Node's
   `path.resolve`/`path.dirname`/`url.fileURLToPath` (portable NIO-equivalent
   APIs for Node), not hardcoded `/` or `\` string concatenation. No temp
   files, no OS-specific roots. Checklist explicitly applied per persona
   instructions even though the change is not path-centric; no findings.
8. **No invented APIs**: `$(...).addClass()`, `.attr()`, `.css()`, `.text()`
   are real, documented jQuery API methods (jQuery 1.0+; project depends on
   jQuery 3.7.1 per `WebUI/src/main/frontend/package.json`).
9. **No secrets** introduced; **no silent failures** introduced (no new
   catch blocks).
10. **Copyright headers** on new test files use `Copyright 1999-2026` per the
    current-year convention in `copyright-and-license.instructions.md`.

## Issues

No blocking issues.

### Issue 1 -- Severity: suggestion
- File: `WebUI/src/main/webapp/cm/app/widgetbuilder/js/views/PercWidgetBuilderDefinitionView.js`, `WebUI/war/widgetbuilder/js/views/PercWidgetBuilderDefinitionView.js`, `WebUI/src/main/webapp/cm/app/widgetbuilder/js/views/PercWidgetFieldsViews.js`, `WebUI/war/widgetbuilder/js/views/PercWidgetFieldsViews.js`
- Description: The new Vitest suites (`percWidgetBuilderDefinitionView.test.js`,
  `percWidgetFieldsViews.test.js`) load and exercise only the
  `src/main/webapp/cm/widgetbuilder/...` copy of each file via `readFileSync` +
  `eval`. The `app/` and `war/` lockstep copies received the identical fix
  (verified byte-for-byte in this review) but have no dedicated regression
  test, so a future edit that touches only one of those two copies would not
  be caught by CI. This matches an existing repo-wide convention (e.g.
  `percUserView.test.js` also tests only one of the 3 `PercUserView.js`
  copies), so it is not a new regression introduced by this commit and is not
  treated as a blocking bug — but it is worth tracking.
- Suggestion: Consider a follow-up that either (a) parameterizes the two new
  test files over all 3 `SRC_PATH` candidates (`app/`, `main/webapp/`,
  `war/`) with `it.each`, or (b) files a tracking item for the larger
  "eliminate triplicated WebUI legacy JS copies" cleanup referenced in the
  WebUI module `AGENTS.md` migration plan, so drift between the 3 copies is
  caught by CI rather than by manual diffing.
- Status: open
- Pattern-id: tests.lockstep-copy-coverage-gap

### Issue 2 -- Severity: suggestion
- File: `WebUI/src/main/webapp/cm/plugins/PercPathSelectionDialog.js:236`,
  `WebUI/src/main/webapp/cm/plugins/PercFolderPropertiesDialog.js:123`,
  `WebUI/src/main/webapp/cm/plugins/perc_upload_theme_file_dialog.js:153`,
  `WebUI/src/main/webapp/cm/plugins/perc_save_as_shared_asset_dialog.js:70,82`,
  `WebUI/src/main/webapp/cm/views/PublishView.js:792`,
  `WebUI/src/main/webapp/cm/shared-common.js:95881,96237`,
  `WebUI/src/main/webapp/cm/shared-common-minuet.js:99141,99398,99754`
  (and their `app/`/`war/` lockstep copies)
- Description: These files build a `.perc_field_error` label/span using the
  same vulnerable pattern (string-concatenated HTML with an interpolated
  variable, inserted via `.append()`/`html()`/string literal) that this
  commit fixed for `PercWidgetBuilderDefinitionView.js` /
  `PercWidgetFieldsViews.js`. They are out of scope for the 6 alerts this
  commit closes (different CodeQL alert IDs, if flagged at all — not verified
  here), but they are the same vulnerability class and likely candidates for
  a follow-up CodeQL sweep / fix batch using the same DOM-API refactor
  pattern established in this commit.
- Suggestion: File a follow-up ticket to audit these sinks with the same
  `$("<tag></tag>").attr()/.text()` refactor pattern, and re-run CodeQL to
  confirm scope/severity before committing to a fix batch.
- Status: open
- Pattern-id: security.xss-through-dom.string-concat-append (generalizable)

## Non-blocking observation (unrelated to this diff)

`npm run test` (`vitest run`) on `WebUI` currently reports 69 failing tests
across 17 files, all in unrelated dashboard React widget suites
(`WorkflowStatusWidget`, `SEOAuditWidget`, `SiteimproveWidget`,
`ProcessMonitorWidget`, `WidgetConfigurationWidget`,
`useDashboardConfig`, etc. under `src/test/ts/dashboard/`). None of these
files are touched by `925c97f44` (diff-stat confirms only the 8 widgetbuilder
files listed in Scope changed), so these failures cannot have been introduced
by this commit. Most failures show `Invalid Chai property: toBeInTheDocument`,
suggesting a missing/misconfigured `@testing-library/jest-dom` matcher setup
in the current environment rather than a functional regression. This is
flagged per the "CI red — note it, don't approve as if green" rule: the
targeted tests for this diff (the 2 new widgetbuilder test files) are
verified green in isolation, but the full `WebUI` suite is not currently
green independent of this change. Recommend the team track/fix the
dashboard test environment setup separately; it does not block this commit.
