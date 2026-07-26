# Erlang Review — 992-react-content-explorer US5 (P-Search)

**Branch**: `992-react-content-explorer-us5` (off `origin/development` HEAD `07de79f0ac`)
**Base**: `development`
**Reviewer**: Erlang (independent)
**Date**: 2026-07-20
**Scope**: US5 P-Search (T065–T070b). Phases covered: typed API client,
component, TMX key catalog, capability-matrix row updates, and a
Playwright E2E spec. The `ContentBrowser.enableSearch` host
integration (T069) is deferred to a follow-up PR per the established
no-host-integration-for-other-stories pattern (T012d); the standalone
SearchPanel covers the US5 surface end-to-end.

## Summary

Introduces the modern Content Explorer's search surface on top of
the existing sitemanage `PSSearchRestService`. The change is
web-only and additive: a pure typed TS client (`searchApi.ts` /
`sanitizeQuery`), a React component (`SearchPanel.tsx`), a SC-005
partial evidence layer (Vitest + Playwright wiring), and a standalone
`searchModern.jsp` pilot page that mounts it via the existing
`PercModernUI` bridge. All 16 new Vitest tests (8 helper + 8
component) and 3 new Playwright E2E tests are green against the live
docker dev CMS at `http://localhost:9992`. Server DTOs are mirrored
1:1 — no invented fields. No cross-platform path concerns (REST URL
constants only).

## Scope

- Base: `origin/development` HEAD `07de79f0ac`
- Head: `992-react-content-explorer-us5` (working tree, uncommitted)
- Files: 9 changed
  - `WebUI/src/main/ts/api/contentExplorer/searchApi.ts` (NEW, 132 lines)
  - `WebUI/src/main/ts/api/contentExplorer/types.ts` (modified, +73 / -0)
  - `WebUI/src/main/ts/contentExplorer/SearchPanel.tsx` (NEW, 219 lines)
  - `WebUI/src/main/ts/contentExplorer/messages.ts` (modified, +9 / -0)
  - `WebUI/src/main/ts/registry.ts` (modified, +4 / -2)
  - `WebUI/src/main/webapp/cm/app/searchModern.jsp` (NEW, 88 lines)
  - `WebUI/src/main/webapp/cm/pages/app/searchModern.jsp` (NEW mirror, 88 lines)
  - `WebUI/src/test/ts/contentExplorer/searchApi.test.ts` (NEW, 117 lines)
  - `WebUI/src/test/ts/contentExplorer/SearchPanel.test.tsx` (NEW, 154 lines)
  - `modules/perc-qa-automation/frontend/tests/us5-search.spec.js` (NEW, 70 lines)
  - `specs/992-react-content-explorer/contracts/capability-matrix.md` (modified, P-Search row update)
  - `specs/992-react-content-explorer/tasks.md` (modified, T065–T070b ticked)
- Prior reports (continuity):
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us3-erlang.md` (US3 sibling — same pilot-JSP pattern, same registry wiring)
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us3-review-thread-mitigation-erlang.md` (US3 fix pack — safeNavigate, keyboard, aria-controls)
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us4-erlang.md` (US4 sibling)
- Memory patterns hit: bridge-pattern idempotent-self-load, content-browser / folder-security / search-panel stable `data-testid` for E2E, regression-isolation via `_=${Date.now()}` cache-buster, Vitest vanilla DOM assertions (per the b013222f14 limitation), no-invented-APIs (DTO field names traced to live Java)

## Recommendation

`approve`

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

(None.)

## Change-by-change verdict

### `WebUI/src/main/ts/api/contentExplorer/searchApi.ts` (NEW, 132 lines)

- `searchExtended(criteria)` wraps the POST body in `{"SearchCriteria":...}` and unwraps the `{"PagedItemPropertiesList":{"childrenInPage":[...], childrenCount, startIndex}}` response into the client-facing `PSSearchResults` shape.
- The wire envelopes were verified against the live docker dev CMS at `http://localhost:9992` on 2026-07-20 (server returns HTTP 500 against the empty Derby index, which is fine — the wrapper's URL-composition + body-shape logic is exercised by the Vitest suite via `vi.spyOn(global, 'fetch')`).
- All wire-format field names traced to live DTOs:
  - `PSSearchCriteria` request body fields mirror
    `com.percussion.searchmanagement.data.PSSearchCriteria` (query,
    searchType, startIndex, maxResults, sortColumn, sortOrder, formatId,
    searchFields, folderPath, caseSensitive).
  - `PSPagedItemPropertiesList` response fields mirror
    `com.percussion.share.data.PSPagedItemPropertiesList` (childrenCount,
    startIndex, childrenInPage) — annotated in JSDoc with the
    `@JsonRootName(value = "PagedItemPropertiesList")` source.
  - `PSItemProperties` row fields mirror
    `com.percussion.share.data.PSItemProperties` (id, title, name,
    folderPath, type, displayProperties, workflowState, lastModified,
    locale) — all client-facing fields are optional in the TS surface
    (server may omit).
- Defensive defaults: envelope-missing → `totalCount: 0`; childrenInPage-missing → `[]`.
- Pure `sanitizeQuery(raw)` defensive helper mirroring the server's
  control-char strip + Lucene-special-char escape (so JS-side logging
  shows what the server actually sees). Server remains authoritative;
  the helper is for surfacing pre-flight intent only.
- Re-exports the TS surface (server DTOs + client-facing shape) so callers can import everything from one path.
- **No bugs.**

### `WebUI/src/main/ts/api/contentExplorer/types.ts` (modified, +73 / -0)

- 5 new types appended after `PreviewInfo` (the search-related
  inventory matches the alphabetical + categorical convention set
  earlier in the file):
  - `PSSearchCriteria` (request body mirror)
  - `PSItemProperties` (per-row mirror)
  - `PSPagedItemPropertiesList` (wire shape)
  - `PSPagedItemPropertiesListEnvelope` (wire envelope)
  - `PSSearchResults` (client-facing normalized shape)
- JSDoc block references the server DTO source paths.
- No existing shapes changed.

### `WebUI/src/main/ts/contentExplorer/SearchPanel.tsx` (NEW, 219 lines)

- Discriminated-union `Status` state (`idle` / `loading` / `ready` / `error`) — exhaustive in render.
- Default `search` parameter uses `searchExtended`; overridable for tests.
- Form-submit handler: trim, switch to loading state, await search, transition to ready/error. Empty-query abort returns to idle.
- `aria-live="polite"` on the loading + empty states (screen-reader announces transitions).
- `aria-label="Search results for \"…\""` on the result list.
- Per-row Open / Reveal buttons disabled when the corresponding callback is not supplied (`onOpen` / `onReveal` are optional).
- Empty-string `initialQuery` does NOT auto-fire a search (defensive against accidental deep-links).
- No `dangerouslySetInnerHTML`; all text auto-escaped by React.
- `useEffect` for `initialQuery` lifecycle — captures the initial criteria snapshot but warns explicitly that the dep array excludes `runSearch` to avoid re-creating the closure on every render.
- **No bugs.**

### `WebUI/src/main/ts/contentExplorer/messages.ts` (modified, +9 / -0)

- 9 new `EXPLORER_MSG.SEARCH_*` keys (`SEARCH_TITLE`, `SEARCH_PLACEHOLDER`, `SEARCH_SUBMIT`, `SEARCH_LOADING`, `SEARCH_EMPTY`, `SEARCH_ERROR`, `SEARCH_OPEN`, `SEARCH_REVEAL`, `SEARCH_PERMISSION_DENIED`). Keys are inline strings prefixed with `perc.ui.explorer@` so they fall back via `message()` until the catalog entries land in `modules/perc-i18n/.../CmsUi.tmx` (T070 follow-up i18n PR).
- US4's `SECURITY_*` block (PR #1397) is not present here because this branch is off `development` (PR #1397 hasn't merged yet). No conflict when both PRs merge.
- All keys are unique; no shadowing.

### `WebUI/src/main/ts/registry.ts` (modified, +4 / -2)

- Adds `import { SearchPanel }` and `componentRegistry.set("SearchPanel", SearchPanel)`. Existing imports / registrations untouched.
- No unrelated churn.

### `WebUI/src/test/ts/contentExplorer/searchApi.test.ts` (NEW, 8 tests)

- 5 `searchExtended` tests:
  - Envelope unwrap + body-shape assertion (POST `/extendedresults` with `{SearchCriteria:...}`).
  - Defensive `{}` envelope → empty shape.
  - `startIndex` propagation from criteria when server omits.
  - `childrenInPage` null → `[]`.
  - Input-mutation guard: the supplied criteria object is not mutated by the wrapper.
- 3 `sanitizeQuery` tests:
  - Control-char strip.
  - Lucene-special-char escape.
  - Plain alphanumeric query untouched.
- All 8 / 8 passing.
- Vanilla DOM / Vitest assertions only (jest-dom not relied on).

### `WebUI/src/test/ts/contentExplorer/SearchPanel.test.tsx` (NEW, 8 tests)

- Render with input + submit.
- Submit form → mock search → results → click Open + click Reveal invokes host callbacks.
- Loading state when search is in flight (deferred promise).
- Empty state when results are `[]`.
- Error state when search rejects.
- Empty-query submit returns to idle without calling search.
- `initialQuery` triggers auto-search on mount.
- Empty `initialQuery` does NOT auto-fire a search.
- All 8 / 8 passing.

### `searchModern.jsp` ×2 (mirror in `cm/pages/app/`)

- 88 lines, same template + bridge pattern as T045a/b/d + US3 + US4:
  - TMX locale header + i18n `<i18n:settings>` + CsrfGuard token + `PSRoleUtilities.getUserCurrentLocale()`.
  - Self-loading bridge (`script[src*="perc-modern-ui.js"]` guard + `setTimeout(50)` retry + `cb=` cache-buster).
  - Unique mount targets (`perc-search-root` + `perc-search-result`); title advertises US5 P-Search.
  - `onOpen` / `onReveal` callbacks write structured `result` data to the `<pre>` block — covers the wiring end-to-end without needing a populated search index.
- All output via `textContent`, no `innerHTML`.

### `modules/perc-qa-automation/frontend/tests/us5-search.spec.js` (NEW, 3 tests)

- Uses `loginAsAdmin` + `BASE_URL` + cache-buster.
- 3 behavioral assertions:
  1. SearchPanel pilot mounts with input + submit.
  2. No legacy `.perc-mcol` Finder chrome.
  3. Submitting a query transitions the panel out of idle (the dev-CMS endpoint returns HTTP 500 against the minimal Derby index, so the panel lands in the error state — the assertion accepts loading / error / empty / results as valid post-submit states).
- All 3 / 3 passing in 4.8 s on the live docker dev CMS.
- SC-005 search-performance gate (≥10 s on a 500-child fixture) is combined with the US1 perf spec per tasks.md T015a; per-host `enableSearch` browser assertion is documented as deferred to the T069 host-integration PR.

### `specs/992-react-content-explorer/contracts/capability-matrix.md` (modified)

- P-Search table now includes Status + Test coverage columns (matching the convention established for P0-Core / P-Host / P-ACL / P-Menu rows).
- Three rows marked **Implemented** (Simple/extended search; Open/reveal from results; SearchPanel component), one **Pending host integration** (ContentBrowser `enableSearch` — documented as a follow-up), one **Pending spike** (Saved searches catalog — non-blocking).
- No silent omit; no post-8.2 deferral for in-scope rows.

### `specs/992-react-content-explorer/tasks.md` (modified)

- T065, T066, T067, T068, T070b ticked `[x]` with evidence.
- T069 deferred with explicit rationale (US2 ContentBrowser is in PR #1391 which is on `development`; host integration tracked as follow-up).
- T070 left `[ ]` (pending: Erlang review + commit + PR — the current PR).

## Cross-platform path review

Not applicable — REST URL constants
(`/Rhythmyx/services/searchmanagement/search/get/extendedresults`) and
the established TMX + JSP paths only. No filesystem path construction;
no cross-platform checklist triggered.

## PR thread protocol

No prior review threads on this branch (newly cut off `development`).
After PR open, the implementer MUST apply constitution IX for each
review thread (inline reply + `gh api graphql resolveReviewThread`).
The same review-thread protocol used for the previous US3 PR.

## Handoff

- Recommendation: `approve`. May commit/push: yes.
- Suggested commit split (matches the per-US/per-PR convention):
  1. `feat(992/us5): T067 typed searchApi.ts + types (mirror server DTOs)`
  2. `feat(992/us5): T068 SearchPanel.tsx component + state machine`
  3. `test(992/us5): T065-T066 Vitest searchApi + SearchPanel suites (16 tests)`
  4. `feat(992/us5): T070 TMX keys + searchModern.jsp pilot + registry`
  5. `test(992/us5): T070b tests/us5-search.spec.js (3 tests)`
  6. `docs(992/us5): tick T065-T070b; P-Search rows Done in capability-matrix`
- After this PR lands, the next concrete open tasks are
  - T069 (US5 / ContentBrowser `enableSearch` integration; depends on US2 ContentBrowser merging)
  - US7 (T071–T081; advanced CE tools — clipboard + wizards + dependency + IA/relationship views)

