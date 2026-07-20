# Erlang Review — PR #1391 follow-up (T045d folder-picker + paths.ts re-add)

**PR**: https://github.com/intersoftdatalabs-in/percussioncms/pull/1391
**Branch**: `992-react-content-explorer-us2`
**Base**: `development`
**Reviewer**: Erlang (independent)
**Date**: 2026-07-20
**Scope**: Uncommitted changes — `WebUI/src/main/ts/api/paths.ts` (+38 lines, 12
PATH_* getters + ACTIONS_ROOT) and T045d follow-up (`folderPickerModern.jsp`
×2 + `host-folder-picker.spec.js`).

## Summary

T045d (`folderPickerModern.jsp` + mirror + Playwright spec) is a faithful
clone of the T045b `pagePickerModern.jsp` pattern with `allowFolderSelect:
true, allowItemSelect: false` for folder-only mode — same self-loading
bridge, same `setTimeout(50)` polling, same idempotent script-tag guard,
same result-rendering block, same TMX/CsrfGuard/`PSRoleUtilities` headers.
The `paths.ts` re-add restores the 12 sitemanage `pathmanagement/*` REST
getters + `ACTIONS_ROOT` consumed by `pathApi.ts` (verified — the same
twelve `PATHS.PATH_*` strings appear in `WebUI/src/main/ts/api/contentExplorer/pathApi.ts`).
No bugs, no missing tests for the change under review (T045d has 4
Playwright spec cases; the paths.ts getters are pure URL constants).

## Scope

- Base: `origin/development`
- Head: `992-react-content-explorer-us2` (working tree)
- Files: 4 changed
  - `WebUI/src/main/ts/api/paths.ts` (modified, +38 lines)
  - `WebUI/src/main/webapp/cm/app/folderPickerModern.jsp` (new, 97 lines)
  - `WebUI/src/main/webapp/cm/pages/app/folderPickerModern.jsp` (new mirror, 97 lines)
  - `modules/perc-qa-automation/frontend/tests/host-folder-picker.spec.js` (new, 85 lines)
- Prior reports (same PR):
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us2-content-browser-pilot-erlang.md` (asset-pilot)
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us2-host-page-picker-erlang.md` (T045b)
- Memory patterns hit: bridge-pattern idempotent-self-load, content-browser
  stable `data-testid` for E2E, regression-isolation via `_=${Date.now()}` cache-buster

## Recommendation

`approve`

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

(None.)

## Change-by-change verdict

### `WebUI/src/main/ts/api/paths.ts` (+38)

Twelve `PATH_*` getters (`PATH_PAGINATED_FOLDER`, `PATH_ITEM`, `PATH_ITEM_ID`,
`PATH_ADD_NEW_FOLDER`, `PATH_RENAME_FOLDER`, `PATH_MOVE_ITEM`,
`PATH_DELETE_ITEM`, `PATH_FOLDER_PROPERTIES`, `PATH_SAVE_FOLDER_PROPERTIES`,
`PATH_VALIDATE`, `PATH_LAST_EXISTING`) plus one `ACTIONS_ROOT` getter. All
relative to the existing `${SERVICES_ROOT}/pathmanagement/path/*` and
`${SERVICES_ROOT}/actions` REST roots — matches the in-code call sites
audited via `grep "PATHS\\.PATH_" pathApi.ts | sort -u` (returns exactly
the 12 strings listed above). The `ACTIONS_ROOT` is a forward-looking helper
for US3 (per in-source comment); not imported by current code, but harmless
and keeps adjacent endpoints cohesive. Pattern matches the existing getter
form. **No bugs.**

### `folderPickerModern.jsp` ×2

- `cm/app/folderPickerModern.jsp` and `cm/pages/app/folderPickerModern.jsp`
  are byte-identical mirrors, consistent with the established dual-path
  pattern (T045b `pagePickerModern.jsp` and the T045c asset picker each
  ship the same mirror).
- TMX locale header, CsrfGuard token meta, `PSRoleUtilities.getUserCurrentLocale()`
  fallback to `en-us`, `<i18n:settings>` debug flag — all identical to the
  asset/page variants.
- Mount target: `perc-folder-picker-root` (unique to this host so the
  `PercModernUI.mount()` lookup cannot pick up a sibling's leftover mount).
- Config passed: `initialPath: "/Sites"`, `mode: "select"`, `multiSelect:
  false`, `allowFolderSelect: true`, `allowItemSelect: false`,
  `enableSearch: false`, `enablePreview: false`, `title: "Pick a folder"` —
  consistent with the cutover-inventory §C description (folder-only mode,
  mirrors the legacy `$.perc_finder().open(newPath.split('/'))` call shape).
- Self-loading bridge block, idempotent script tag guard (`script[src*="perc-modern-ui.js"]`),
  and `setTimeout(50)` polling are exact copies of the proven asset/page
  patterns — already validated in US2 PR #1391 review.
- Result/cancel/error callbacks write to a unique `perc-folder-picker-result`
  `<pre>` — no DOM collisions.
- **No bugs, no security smells (everything is `textContent`, not `innerHTML`).**

### `host-folder-picker.spec.js` (4 tests)

- Uses the shared `loginAsAdmin` + `BASE_URL` helpers; cache-buster
  `?_=${Date.now()}` per the qa-automation AGENTS.md "Fast iteration" tip.
- All four test IDs match rendered DOM in `ContentBrowser.tsx`:
  - `[data-testid="content-browser"]` → `:315`
  - `[data-testid="content-browser-confirm"]` → `:403`
  - `[data-testid="content-browser-selection-summary"]` → `:382`
  - `[data-testid="content-browser-cancel"]` → `:393`
- `test 2` asserts `.perc-mcol` count is zero — the legacy miller-column
  Finder chrome should be absent (modern-UI cutover). This is a
  **behavioral** assertion (DOM count), not a token-grep.
- `test 3` asserts the confirm button is disabled and the selection summary
  is visible at the empty initial state — covers the empty-selection guard
  from PR #1391 review thread `PRRT_kwDOKZBp3M6SIbUI`.
- `test 4` exercises keyboard-completability by focusing the cancel button
  and asserting `document.activeElement?.tagName === 'BUTTON'` — reasonable
  a11y smoke for a modal/dialog host.
- 60 s timeout per test is generous for a fresh dev CMS login + navigation.
- **No bugs. Tests are behavioral, not token-greps.**

## Cross-platform path review

Not applicable — all paths in this diff are REST URLs (`/Rhythmyx/...`,
`/cm/modern/assets/...`) or in-`/Services`-relative URL strings. No
filesystem path construction; no cross-platform path checklist triggered.

## PR thread protocol

The four existing review threads on PR #1391
(`PRRT_kwDOKZBp3M6SIbUI`, `PRRT_kwDOKZBp3M6SIbUK`,
`PRRT_kwDOKZBp3M6SIbUL`, `PRRT_kwDOKZBp3M6SIf7r`) were all marked `isResolved`
in an earlier session. The T045d follow-up is additive and does not
re-introduce any thread.

## Handoff

- Recommendation: `approve`. May commit/push: yes.
- Suggested commit split:
  1. `fix(992/us2): re-add missing path-management + actions REST getters in paths.ts`
  2. `feat(992/us2): US2 T045d — host-folder-picker migration (JSP + mirror + Playwright)`
- Patterns loaded; prior reports for the same PR loaded for continuity.
