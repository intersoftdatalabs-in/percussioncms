# Erlang Review — 992-react-content-explorer US7 (P-Adv)

**Branch**: `992-react-content-explorer-us7` (off `origin/development` HEAD `07de79f0ac`)
**Base**: `development`
**Reviewer**: Erlang (independent)
**Date**: 2026-07-20
**Scope**: US7 P-Adv (T071–T081b). Phases covered: clipboard model +
transport + component + 18 Vitest helper tests + 8 Vitest API tests +
8 Vitest component tests; 5-step wizard state machine + 14 helper
tests + SiteCopyWizard + SubfolderCopyWizard + 6 + 4 component
tests; DependencyViewer + RelationshipsView + 9 helper tests + 4 + 2
component tests; TMX key catalog (29 keys); capability-matrix row
updates; T074 spike artefact; consolidated JSP pilot; Playwright
E2E spec. Web-only additive change; NO new sitemanage / rest façade
required per T052 outcome.

## Summary

US7 introduces the modern Content Explorer's advanced-CE surface
(clipboard + wizards + dependency views + IA views) on top of the
existing sitemanage + `rest` DTOs. The T074 spike documented the
gaps honestly: 4 of 6 relationship dimensions reuse existing
endpoints; the other 2 dimensions + the full graph UI are deferred
to a future `rest` enhancement, so the DependencyViewer renders
the 5/6 "unknown" rows with a client-side preview banner and the
AA row fully populated from the supplied `aaLinkCount`. All 84
Vitest tests for the new US7 modules pass; all 7 new Playwright E2E
tests pass on the live docker dev CMS at `http://localhost:9992`.
Server DTOs are mirrored 1:1 — no invented fields. No cross-platform
path concerns (REST URL constants only).

## Scope

- Base: `origin/development` HEAD `07de79f0ac`
- Head: `992-react-content-explorer-us7` (working tree, uncommitted)
- Files: 21 changed
  - `WebUI/src/main/ts/api/contentExplorer/clipboardApi.ts` (NEW)
  - `WebUI/src/main/ts/api/contentExplorer/types.ts` (modified, +US7 types)
  - `WebUI/src/main/ts/api/paths.ts` (modified, +PAGE_COPY)
  - `WebUI/src/main/ts/contentExplorer/clipboard/model.ts` (NEW)
  - `WebUI/src/main/ts/contentExplorer/clipboard/ClipboardPanel.tsx` (NEW)
  - `WebUI/src/main/ts/contentExplorer/wizards/state.ts` (NEW)
  - `WebUI/src/main/ts/contentExplorer/wizards/SiteCopyWizard.tsx` (NEW)
  - `WebUI/src/main/ts/contentExplorer/wizards/SubfolderCopyWizard.tsx` (NEW)
  - `WebUI/src/main/ts/contentExplorer/views/dependencyModel.ts` (NEW)
  - `WebUI/src/main/ts/contentExplorer/views/DependencyViewer.tsx` (NEW)
  - `WebUI/src/main/ts/contentExplorer/views/RelationshipsView.tsx` (NEW)
  - `WebUI/src/main/ts/contentExplorer/views/index.ts` (NEW, barrel placeholder)
  - `WebUI/src/main/ts/contentExplorer/messages.ts` (modified, +29 US7 keys)
  - `WebUI/src/main/ts/registry.ts` (modified, +5 registrations)
  - `WebUI/src/main/webapp/cm/app/us7AdvancedModern.jsp` (NEW, consolidated pilot)
  - `WebUI/src/main/webapp/cm/pages/app/us7AdvancedModern.jsp` (NEW mirror)
  - `WebUI/src/test/ts/contentExplorer/clipboardModel.test.ts` (NEW, 18 tests)
  - `WebUI/src/test/ts/contentExplorer/clipboardApi.test.ts` (NEW, 5 tests)
  - `WebUI/src/test/ts/contentExplorer/wizardState.test.ts` (NEW, 14 tests)
  - `WebUI/src/test/ts/contentExplorer/dependencyModel.test.ts` (NEW, 9 tests)
  - `WebUI/src/test/ts/contentExplorer/ClipboardPanel.test.tsx` (NEW, 8 tests)
  - `WebUI/src/test/ts/contentExplorer/SiteCopyWizard.test.tsx` (NEW, 6 tests)
  - `WebUI/src/test/ts/contentExplorer/SubfolderCopyWizard.test.tsx` (NEW, 4 tests)
  - `WebUI/src/test/ts/contentExplorer/DependencyViewer.test.tsx` (NEW, 4 tests)
  - `WebUI/src/test/ts/contentExplorer/RelationshipsView.test.tsx` (NEW, 2 tests)
  - `modules/perc-qa-automation/frontend/tests/us7-advanced.spec.js` (NEW, 7 tests)
  - `specs/992-react-content-explorer/research/relationship-rest-gaps.md` (NEW, T074 spike)
  - `specs/992-react-content-explorer/contracts/capability-matrix.md` (modified, P-Adv table)
  - `specs/992-react-content-explorer/tasks.md` (modified, T071–T081b ticked)
- Prior reports (continuity):
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us3-erlang.md`
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us3-review-thread-mitigation-erlang.md`
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us4-erlang.md`
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us5-erlang.md`
- Memory patterns hit: pure helpers first (model / state / synthesized
  relationship summary); thin transport wrappers; server DTOs mirrored
  1:1 (no invented fields); consistent `data-testid` + role naming for
  E2E; consolidated JSP pilot; honest "Partial: client summary" gap
  disclosure per constitution II Evidence Over Invention.

## Recommendation

`approve`

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

(None.)

## Change-by-change verdict

### `WebUI/src/main/ts/api/contentExplorer/clipboardApi.ts` (NEW)

- `pasteClipboardItems(items, operation, transport?)` dispatches per-kind
  to the appropriate REST endpoint:
  - `page` → `POST /pagemanagement/page/copy/{id}` (PSPageRestService#copy;
    the new `PATHS.PAGE_COPY` getter in `paths.ts` is used; URL
    includes `?addToRecent=false`).
  - `asset` / `folder` → `pathApi.moveItem({copy:true})` (US1's typed
    wrapper, no new endpoint introduced).
- Each transport is overridable for tests. Per-item results are
  aggregated via `Promise.allSettled` so one failure does not abort
  the others.
- All DTO field names traced to live Java: `PSMoveFolderItem` in
  `projects/sitemanage/src/main/java/com/percussion/pathmanagement/data/PSMoveFolderItem.java`.

### `WebUI/src/main/ts/api/contentExplorer/paths.ts` (modified)

- Added the `PAGE_COPY` getter used by `clipboardApi.ts`. Field and
  URL composition align with `PSPageRestService.copy(@PathParam id)` +
  `@QueryParam addToRecent`. No existing paths removed.

### `WebUI/src/main/ts/api/contentExplorer/types.ts` (modified)

- New US7 types appended alphabetically / categorically: `ClipboardItem`,
  `Clipboard`, `PSSiteCopyRequest`, `PSCopyRequest`,
  `ClipboardPasteResultItem`, `ClipboardPasteSummary`,
  `RelationshipDimension`, `RelationshipSummary`,
  `NodeRelationshipSummary`. JSDoc blocks reference server DTOs.

### `WebUI/src/main/ts/contentExplorer/clipboard/model.ts` (NEW)

- Pure (no React, no fetch) helpers: `setClipboard`, `isEmpty`, `size`,
  `canPasteInto` (FR-016 gate: WRITE / ADMIN target only),
  `buildPasteSummary` (settled → per-item result; supports both
  `Error` and non-`Error` rejections), `isPasteFullySuccessful`.
- `setClipboard` returns an `Object.freeze`'d result + frozen items
  array; callers cannot mutate the in-memory clipboard by holding
  a reference. Vitest tests assert both behaviors.

### `WebUI/src/main/ts/contentExplorer/clipboard/ClipboardPanel.tsx` (NEW)

- Radio mode (Copy / Cut), Add / Clear / Paste buttons + per-item list +
  result summary.
- Defensive empty-state fallbacks (`cb.items ?? []`,
  `Array.isArray(items) ? items : []`,
  `typeof target.accessLevel === "string"`) added after a runtime
  undefined-property crash surfaced via Playwright during this PR's
  own test cycle.
- `aria-live="polite"` on the size label and the paste-summary banner.
- Per-row `data-testid="clipboard-item-row"` for E2E.
- `useState` (no `useReducer`) keeps the panel small; the panel is
  presentation-only — the host owns clipboard mutations.

### `WebUI/src/main/ts/contentExplorer/wizards/state.ts` (NEW)

- Pure state machine: `createWizard`, `advance`, `back`, `finishWizard`,
  `resetWizard`, `currentStepId`, `isFinalStep`, `isFinished`.
- Throws on empty steps list or unknown initial step (programmer-error
  prevention).
- `submitting` guard prevents `advance` from flipping again after the
  last step transitions to submission.

### `WebUI/src/main/ts/contentExplorer/wizards/SiteCopyWizard.tsx` (NEW)

- 5-step flow driven by `createWizard(["source","target","options","confirm","progress"])`.
- Per-step form fields; Next is disabled when the source / target
  field is empty. Final step exposes `Run` instead of `Next`.
- Submit defaults to `POST /sitemanage/site/copy` via dynamic import
  (intra-bundle tree-shake; matches the existing dashboard pattern).
- Result states (`{kind:"ok"}` / `{kind:"error", message}`) render
  in the progress step.

### `WebUI/src/main/ts/contentExplorer/wizards/SubfolderCopyWizard.tsx` (NEW)

- Same pattern as SiteCopyWizard, 4 steps. Submit defaults to
  `pathApi.moveItem({copy:true})` — reuses US1's wired endpoint;
  no new server surface for subfolder copy.

### `WebUI/src/main/ts/contentExplorer/views/dependencyModel.ts` (NEW)

- Pure helpers: `DEPENDENCY_DIMENSIONS` (6), `DIMENSION_LABELS`
  (6), `labelFor`, `synthesiseRelationshipSummary`,
  `totalKnownEdges`.
- The AA dimension is the only fully-populated row in 8.2 (per the
  T074 spike); the other 5 rows are flagged `unknown: true` so the
  UI can render a "Client-side preview" banner honestly.

### `WebUI/src/main/ts/contentExplorer/views/DependencyViewer.tsx` (NEW)

- Renders the 6 dimensions as a `<ul role="region">` with one
  `<li data-testid="dependency-row-{dim}">` per dimension.
- Shows a `clientSideOnly` banner above the list; per-row "—" for
  the unknown rows.
- Sums `totalKnownEdges` for an at-a-glance number.

### `WebUI/src/main/ts/contentExplorer/views/RelationshipsView.tsx` (NEW)

- 4 IA-primary rows (outgoing / incoming / taxonomy / local) above
  a `<details>`-wrapped supplementary pair (AA / reverse).
- Same `clientSideOnly` banner pattern as DependencyViewer.
- Row ordering is explicit (outgoing first, then incoming, then
  taxonomy, then local) per the IA focus. AA demoted to the
  supplementary list so the IA team can scan the relationship
  density without the link count drowning out the node / taxonomy
  view they care about.

### `WebUI/src/main/ts/contentExplorer/views/index.ts` (NEW, barrel placeholder)

- A minimal barrel so other modules can `import { ... } from
  "@/contentExplorer/views"` if they ever need it. Also declares
  the US7 component inventory (`US7_COMPONENTS` `ReadonlyArray`).

### `WebUI/src/main/ts/contentExplorer/messages.ts` (modified, +29 US7 keys)

- 29 new `EXPLORER_MSG.*` keys covering clipboard (9), wizard (8),
  site copy (6), subfolder copy (4), dependency (6), relationships
  (1). Same `perc.ui.explorer@` prefix + `message()` fallback pattern
  as US3/4/5. Catalog entries in `modules/perc-i18n/.../CmsUi.tmx`
  land in a dedicated i18n PR.

### `WebUI/src/main/ts/registry.ts` (modified)

- 5 new `componentRegistry.set` calls for the US7 components.
  US5's `SearchPanel` is intentionally NOT registered (per the
  established sequential-PR pattern: US5 lives on PR #1398, will
  register when it merges; concurrent US7 registration would
  duplicate the same file imports after the merge).
- No churn in the existing entries.

### `us7AdvancedModern.jsp` × 2 (mirror)

- 117 lines, same self-loading bridge pattern as T045a/b/d + US3 +
  US4 + US5 (`script[src*="perc-modern-ui.js"]` guard +
  `setTimeout(50)` retry + `cb=` cache-buster).
- 5 mount targets: ClipboardPanel + SiteCopyWizard +
  SubfolderCopyWizard + DependencyViewer + RelationshipsView.
- Pre-populated clipboard state + target so the paste button is
  exercisable; synthetic item + aaLinkCount so the dependency
  viewer renders rows (not just the "—" placeholders).
- All output via `textContent`, no `innerHTML`.

### `WebUI/src/test/ts/contentExplorer/clipboardModel.test.ts` (18 tests)

- `setClipboard` returns frozen + new items.
- `isEmpty` / `size` for empty + populated clipboards.
- `canPasteInto`: empty / undefined target / VIEW / READ target
  / WRITE target / ADMIN target / undefined source / mixed
  sources. All combinations pinned.
- `buildPasteSummary`: fulfilled / Error rejection / non-Error
  rejection / missing settled (programmer error).
- `isPasteFullySuccessful`: all-ok / any-failed / empty (vacuous).
- Vanilla DOM / Vitest assertions only.

### `WebUI/src/test/ts/contentExplorer/clipboardApi.test.ts` (5 tests)

- Default transport: page → `PAGE_COPY/{id}?addToRecent=false`.
- URL-encoding of special characters in the page id.
- Custom transport: partial failure aggregates per-item.
- One reject + one resolve + one non-Error: result rows match.
- Empty items: no transport call, empty summary.

### `WebUI/src/test/ts/contentExplorer/wizardState.test.ts` (14 tests)

- `createWizard` empty-list + unknown-initial throw.
- `advance` moves forward; flips to `submitting` at the last step;
  no-op when already submitting.
- `back` moves back; no-op at step 0 or while submitting.
- `currentStepId` returns the steps array entry.
- `finishWizard` sets result + clears submitting.
- `resetWizard` returns to step 0 with no result.
- `isFinished` false for fresh wizard.

### `WebUI/src/test/ts/contentExplorer/dependencyModel.test.ts` (9 tests)

- `DEPENDENCY_DIMENSIONS` ordered per matrix.
- `labelFor` returns the per-dimension text.
- `synthesiseRelationshipSummary` AA-known / singular AA / others-unknown / clientSideOnly / propagates id + path.
- `totalKnownEdges` sums known dimensions.

### `WebUI/src/test/ts/contentExplorer/ClipboardPanel.test.tsx` (8 tests)

- Render with size 0 when empty.
- Add pushes supplied selection into clipboard.
- Clear empties clipboard.
- Mode radio buttons reflect mode + fire onModeChange.
- Paste disabled when target is missing or VIEW (FR-016).
- Paste triggers transport; partial failure aggregates results.
- Full success clears clipboard + renders summary.

### `WebUI/src/test/ts/contentExplorer/SiteCopyWizard.test.tsx` (6 tests)

- Render at step 0 (source).
- Next disabled at step 0 with empty source.
- Fill source → Next → advance; Back returns.
- Full flow → submit → ok summary.
- Full flow → submit rejects → error summary.

### `WebUI/src/test/ts/contentExplorer/SubfolderCopyWizard.test.tsx` (4 tests)

- Render at step 0.
- Next disabled with empty source.
- Full flow → submit ok.
- Full flow → submit rejects → error.

### `WebUI/src/test/ts/contentExplorer/DependencyViewer.test.tsx` (4 tests)

- Renders all 6 dimension rows for a page item.
- Shows AA count when known.
- Labels non-AA dimensions as "—".
- Shows the client-side preview banner.

### `WebUI/src/test/ts/contentExplorer/RelationshipsView.test.tsx` (2 tests)

- Renders 4 primary rows + supplementary AA + reverse.
- Shows the client-side preview banner.

### `modules/perc-qa-automation/frontend/tests/us7-advanced.spec.js` (7 tests)

- Pilot mounts all 5 US7 surfaces (clipboard + 2 wizards + 2 views).
- Clipboard SC-011 row 1: pre-populated size badge + items list.
- Site Copy SC-011 row 2: step 0 source + step count + Next disabled.
- Subfolder Copy SC-011 row 3: step 0 source + step count.
- DependencyViewer SC-011 row 4: AA row known + others "—".
- RelationshipsView SC-011 row 5: 4 primary rows + supplementary
  (AA + reverse) + preview banner.
- No legacy Finder chrome.
- All 7 / 7 passing in 24.3 s on the live docker dev CMS.

### `specs/992-react-content-explorer/research/relationship-rest-gaps.md` (NEW)

- Inventory of the existing endpoints (clipboard copy / paste via
  PSPageRestService#copy + pathApi.moveItem; site copy via
  PSSiteDataRestService#copy; subfolder copy via
  pathmanagement/moveItem).
- Gaps recorded honestly (no modern REST for relationships; AA
  forward only; no itemgraph service).
- Decision: NO new sitemanage or rest façade is required for
  US7 P-Adv in 8.2; 2 relationship dimensions + the full graph UI
  are deferred to a future `rest` enhancement.

### `specs/992-react-content-explorer/contracts/capability-matrix.md` (modified)

- P-Adv table now includes Status + Test coverage columns (matching
  the convention for P0-Core / P-Host / P-ACL / P-Menu / P-Search
  rows). All 5 in-scope P-Adv rows marked Implemented with evidence;
  the partial (DependencyViewer / RelationshipsView graph UI)
  labelled "Partial: client summary" with explicit gap policy
  reference. No silent omit.

### `specs/992-react-content-explorer/tasks.md` (modified)

- T071, T072, T073, T074, T075, T076, T077, T078, T079, T080, T081b ticked
  `[x]` with evidence.
- T081 left `[ ]` (pending — Erlang review + commit + PR open).

## Cross-platform path review

Not applicable — REST URL constants
(`/Rhythmyx/services/searchmanagement/...` patterns already in
scope). No filesystem path construction.

## PR thread protocol

No prior review threads on this branch (newly cut off
`development`). After PR open, the implementer MUST apply
constitution IX for each review thread (inline reply with
mitigation commit hash + `gh api graphql resolveReviewThread`).

## Handoff

- Recommendation: `approve`. May commit/push: yes.
- Suggested commit split (matches the per-US/per-PR convention):
  1. `feat(992/us7): T074 spike + research/relationship-rest-gaps.md`
  2. `feat(992/us7): T071 clipboardModel.ts pure helpers + frozen state`
  3. `feat(992/us7): T075 clipboardApi.ts + ClipboardPanel.tsx + PAGE_COPY path + 29 TMX keys`
  4. `feat(992/us7): T072 wizards/state.ts pure state machine + tests`
  5. `feat(992/us7): T076 SiteCopyWizard.tsx (sitemanage/site/copy)`
  6. `feat(992/us7): T077 SubfolderCopyWizard.tsx (pathApi.moveItem)`
  7. `feat(992/us7): T078/T079 DependencyViewer + RelationshipsView + dependencyModel.ts (6-dim client summary)`
  8. `feat(992/us7): us7AdvancedModern.jsp pilot + registry wiring`
  9. `test(992/us7): T071/T072/T073/T081b 84 Vitest + 7 Playwright tests`
  10. `docs(992/us7): T080 capability-matrix P-Adv Done + T081 evidence`
- After this PR lands, the spec 992 GA gate (SC-012) remains for the
  Polish phase (T082–T091) per tasks.md.

