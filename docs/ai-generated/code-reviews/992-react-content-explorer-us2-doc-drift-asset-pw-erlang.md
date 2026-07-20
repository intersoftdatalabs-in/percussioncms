# Erlang Review — 992-react-content-explorer T045a-pw + doc drift fix

**Branch**: `992-react-content-explorer-us2-doc-drift-asset-pw` (off `origin/development` HEAD `0744f207a1`)
**Base**: `development`
**Reviewer**: Erlang (independent)
**Date**: 2026-07-20
**Scope**: Uncommitted working-tree changes (1 new file, 2 modified).

## Summary

Closes two real follow-up items that fell out of the PR #1391 squash-merge:
(a) the missing per-host Playwright spec for `host-asset-picker`
(`tests/host-asset-picker.spec.js`, T045a-pw) — modeled exactly on
the already-merged `host-page-picker.spec.js` / `host-folder-picker.spec.js`
siblings and verified against the live docker dev CMS (4 / 4 passing,
16.1 s); (b) the doc-drift in `tasks.md` and `cutover-inventory.md`
where T045d / T045d-pw were never ticked `[x]` and the §C
`host-folder-picker` row Status was still `Pending — T045d` even
though the code and tests were already on `development` via the
squash. All four `data-testid` selectors used by the new spec match
the rendered DOM in `WebUI/src/main/ts/contentBrowser/ContentBrowser.tsx`
(content lines 315 / 382 / 393 / 403). No code or runtime behavior
changes in this commit; behavioral risk is therefore minimal and
fully covered by the live-CMS spec.

## Scope

- Base: `origin/development` (HEAD `0744f207a1`)
- Head: `992-react-content-explorer-us2-doc-drift-asset-pw` (working tree, uncommitted)
- Files: 3 changed
  - `modules/perc-qa-automation/frontend/tests/host-asset-picker.spec.js` (NEW, 108 lines)
  - `specs/992-react-content-explorer/tasks.md` (modified, +4 / -2)
  - `specs/992-react-content-explorer/checklists/cutover-inventory.md` (modified, +4 / -4)
- Prior reports (same feature, relevant for continuity):
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us2-content-browser-pilot-erlang.md` (T045a pilot)
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us2-host-page-picker-erlang.md` (T045b)
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us2-host-folder-picker-erlang.md` (T045d)
- Memory patterns hit: bridge-pattern idempotent-self-load, content-browser
  stable `data-testid` for E2E, regression-isolation via `_=${Date.now()}` cache-buster,
  no-invented-APIs (test IDs traced to source)

## Recommendation

`approve`

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

(None.)

## Change-by-change verdict

### `host-asset-picker.spec.js` (NEW, 4 tests)

- License header (Apache 2.0, 2026 Percussion Software, Inc.) matches
  the established pattern in `host-page-picker.spec.js:1-15` and
  `host-folder-picker.spec.js:1-15`.
- Module-level JSDoc explains purpose (T045a-pw, SC-002 / FR-008a) and
  explicitly notes the relationship to the generic
  `us2-content-browser.spec.js` so the next reader does not think one
  supersedes the other.
- `loginAsAdmin` + `BASE_URL` helpers from `tests/helpers/auth.js`
  reused; cache-buster `?_=${Date.now()}` per the qa-automation
  AGENTS.md "Fast iteration" tip (matches both sibling specs).
- All four `data-testid` selectors trace to the rendered DOM in
  `WebUI/src/main/ts/contentBrowser/ContentBrowser.tsx`:
  - `content-browser` (test 1, mount) → :315 (root dialog div)
  - `content-browser-confirm` (test 3, initial state) → :403
  - `content-browser-selection-summary` (test 3) → :382
  - `content-browser-cancel` (test 4, keyboard-completable) → :393
  These are the same DOM IDs asserted by the merged
  `host-page-picker.spec.js:47,62,63,73` and
  `host-folder-picker.spec.js:51,66,67,77`, so the asset-picker
  spec is a structural twin — no new selectors introduced.
- Test 2 asserts `.perc-mcol` count is zero (legacy miller-column
  Finder chrome is not loaded). This is a behavioral DOM-count
  assertion, not a token grep; the legacy widget is a real DOM
  element when shipped.
- Test 3 asserts the confirm button is `disabled` and the
  selection-summary is visible at the empty initial state —
  matches the `us2-content-browser.spec.js:81-90` pattern and
  locks in the empty-selection guard (the defense-in-depth
  mitigation from PR #1391 review-thread `PRRT_kwDOKZBp3M6SIbUI`).
- Test 4 exercises keyboard-completability by focusing the
  Cancel button and asserting `document.activeElement?.tagName
  === 'BUTTON'` — same shape as the page/folder siblings.
- 60 s timeout per test (generous for a fresh dev CMS login +
  navigation; same as siblings).
- **No bugs, no missing behavioral tests, no security smells.**
  Verified by `cd modules/perc-qa-automation/frontend && npm test
  -- tests/host-asset-picker.spec.js --workers=1` against the
  live docker dev CMS at `http://localhost:9992` (4 / 4 passed
  in 16.1 s on 2026-07-20).

### `tasks.md` (modified)

- T045a-pw (line 139) ticked `[x]` with a brief evidence note
  (4 tests, 16.1 s, live CMS) and a pointer to the
  complementarity with `us2-content-browser.spec.js`. The change
  is plain markdown; no risk.
- T045d (line 144) ticked `[x]` retroactively with a note that
  the work landed via PR #1391 squash-merge (commit `0744f207a1`)
  on 2026-07-19 and that tasks.md was not updated by the squash.
  This is honest provenance, not a false "done" claim.
- T045d-pw (line 145) ticked `[x]` with the live-CMS evidence
  for the 4 folder-picker tests.
- All other task rows untouched. No accidental flips.

### `cutover-inventory.md` (modified)

- §C `host-asset-picker` row Status updated from "Pilot complete
  (2026-07-19)" to "Complete (2026-07-20)" with the new T045a-pw
  Playwright evidence added (4 tests, 16.1 s, live CMS). The
  status-string change tracks the convention used by
  `host-page-picker` (line 158) — "Complete" is the canonical
  Status label for a per-host migration that has both a JSP
  pilot and a passing Playwright spec.
- §C `host-folder-picker` row Status updated from
  "Pending — T045d" to "Complete (2026-07-20)" with the
  `folderPickerModern.jsp` + `tests/host-folder-picker.spec.js`
  evidence. This was the residual doc-drift finding from the
  PR #1391 squash (the squash-merge did not update the
  inventory). The `cm/pages/app/` mirror is explicitly noted.
- The "Pilot complete" framing on `host-asset-picker` is upgraded
  to "Complete" because the per-host Playwright spec is now in
  place. The follow-up call-site migration in
  `perc_delete_page_button.js` / `PercActionDataTable.js` /
  `PercPageView.js` is still noted as a separate PR; that is
  correct per the established per-host pattern.
- All other rows (host-page-picker, host-aa-contentbrowser-dialog,
  host-home-library, host-perc-finder-inline-edit) untouched.
  No accidental status flips.

## Cross-platform path review

Not applicable — the diff is test data-testid selectors, Jest/Playwright
URL constants (CMS is on `localhost:9992`; URLs use `/Rhythmyx/...`),
and markdown edits. No filesystem path construction; no cross-platform
path checklist triggered.

## PR thread protocol

No prior review threads on this branch (newly cut off `development`).
No prior report for this branch slug. After PR open, the next step
is to address any `kilo-code-bot[bot]` / human review comments per
constitution IX: inline reply with mitigation commit hash + run
`gh api graphql resolveReviewThread` for each thread.

## Handoff

- Recommendation: `approve`. May commit/push: yes.
- Suggested commit message (single commit; the changes are small
  and tightly coupled — T045a-pw Playwright + the doc-drift
  fixes that flip T045a-pw / T045d / T045d-pw in the same PR):
  `feat(992/us2): T045a-pw host-asset-picker spec + doc drift fix
  (T045d / T045d-pw tick)`
- The next concrete open task after this PR lands: T045f
  (verify all in-scope hosts migrated; per-host call-site
  follow-up in `perc_delete_page_button.js` /
  `PercActionDataTable.js` / `PercPageView.js` /
  `perc_folderproperties_button.js` / `PercFolderHelper.js`).
