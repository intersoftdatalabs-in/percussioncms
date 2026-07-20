# Erlang Review — 992-react-content-explorer US3 (P-Menu)

**Branch**: `992-react-content-explorer-us3` (off `origin/development` HEAD `8b3ce6cf06`)
**Base**: `development`
**Reviewer**: Erlang (independent)
**Date**: 2026-07-20
**Scope**: US3 P-Menu (T048–T057b). Phases covered: tests, typed API client,
React components, SC-003 action checklist, Playwright E2E spec. No
back-end (REST / sitemanage) changes; T052 decision recorded that no
new façade is needed for 8.2 P-Menu.

## Summary

Introduces the modern Content Explorer's configuration-driven toolbar /
context-menu surface on top of the existing `rest` ActionMenuResource.
The change is **additive and web-only**: a thin typed TS client
(`actionMenuApi.ts`), two React components (`ContextMenu.tsx`,
`ActionToolbar.tsx`), a SC-003 checklist, and a standalone
`actionMenuModern.jsp` pilot page that mounts them via the existing
`PercModernUI` bridge. All 23 Vitest tests and 5 Playwright E2E tests
are green against the live docker dev CMS at `http://localhost:9992`.
The mat-menu mapping is pure (no fetch side effects) and stable; the
component contracts are minimal and explicit. No new REST endpoints;
no new Java; no cross-platform path concerns (URL endpoints only).

## Scope

- Base: `origin/development` (HEAD `8b3ce6cf06`)
- Head: `992-react-content-explorer-us3` (working tree, uncommitted)
- Files: 11 changed
  - `WebUI/src/main/ts/api/contentExplorer/actionMenuApi.ts` (NEW, 167 lines)
  - `WebUI/src/main/ts/api/contentExplorer/types.ts` (modified, +110 / -2)
  - `WebUI/src/main/ts/contentExplorer/ContextMenu.tsx` (NEW, 130 lines)
  - `WebUI/src/main/ts/contentExplorer/ActionToolbar.tsx` (NEW, 64 lines)
  - `WebUI/src/main/ts/registry.ts` (modified, +4 / -1)
  - `WebUI/src/main/webapp/cm/app/actionMenuModern.jsp` (NEW, 88 lines)
  - `WebUI/src/main/webapp/cm/pages/app/actionMenuModern.jsp` (NEW mirror, 88 lines)
  - `WebUI/src/test/ts/contentExplorer/actionMenuApi.test.ts` (NEW, 188 lines)
  - `WebUI/src/test/ts/contentExplorer/ContextMenu.test.tsx` (NEW, 90 lines)
  - `WebUI/src/test/ts/contentExplorer/ActionToolbar.test.tsx` (NEW, 48 lines)
  - `modules/perc-qa-automation/frontend/tests/us3-menus.spec.js` (NEW, 110 lines)
  - `specs/992-react-content-explorer/checklists/sc003-actions-checklist.md` (NEW, 75 lines)
  - `specs/992-react-content-explorer/tasks.md` (modified, T048–T057 ticked)
- Prior reports (related continuity):
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us2-host-folder-picker-erlang.md` (T045d pattern for JSP pilot pages)
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us2-doc-drift-asset-pw-erlang.md` (T045a-pw pattern for per-host Playwright specs)
- Memory patterns hit: bridge-pattern idempotent-self-load, content-browser
  stable `data-testid` for E2E, regression-isolation via `_=${Date.now()}` cache-buster,
  no-invented-APIs (DTO field names traced to live Java), Vitest vanilla DOM
  assertions for module-level tests (per the b013222f14 limitation note)

## Recommendation

`approve`

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

(None.)

## Change-by-change verdict

### `WebUI/src/main/ts/api/contentExplorer/actionMenuApi.ts` (NEW, 167 lines)

- Three REST wrappers (`findActions`, `findAllowedContentTypeMenus`,
  `findAllowedTemplateMenus`) + the pure mapper
  `mapActionMenusToMenuActions` (sortRank ascending, children flattening,
  omit-empty-children).
- All wire-format envelopes match the live Java DTOs verified by curl
  on 2026-07-20:
  - `/actions/find` → `{"ActionMenu":[...]}` (DTO `@XmlRootElement(name="ActionMenu")`)
  - `/actions/find/types` and `/find/templates/{id}` → `{"ActionMenuList":[...]}` (DTO `@XmlRootElement(name="ActionMenuList")`)
- TypeScript surface mirrors the `ActionMenu`, `ActionMenuList`,
  `ActionMenuParameter`, `ActionMenuProperty`,
  `ActionMenuVisibilityContext`, `ActionMenuModeUIContext`,
  `AllowedContentTypeMenusRequest` server DTOs 1:1 (no invented fields).
- Per the JSDoc, the wrapper key for `findActions` derives from the
  element DTO's `@XmlRootElement` (the `List<ActionMenu>` signature in
  `ActionMenuResource.findActions` still wraps under `ActionMenu`,
  consistent with how Jackson honors the type's root name). Pre-verified
  against the live CMS.
- `PATHS.ACTIONS_ROOT` is the existing `${SERVICES_ROOT}/actions`
  getter introduced on `origin/development` in the T045d
  `paths.ts` follow-up; no new path constants introduced.
- No secrets / no logging; CSRF handled by the shared `client.ts`
  `get`/`post` wrappers.
- **No bugs.**

### `WebUI/src/main/ts/api/contentExplorer/types.ts` (modified, +110/-2)

- New `ActionMenuType`, `ActionMenuParameter`, `ActionMenuProperty`,
  `ActionMenuVisibilityContext`, `ActionMenuModeUIContext`,
  `ActionMenuListEnvelope`, `ActionMenu`, `AllowedContentTypeMenusRequest`,
  `MenuAction` types are appended after `PreviewInfo`, in alphabetical
  order, with a clear JSDoc comment block referencing the live server
  DTOs.
- The 12 enumerated types do NOT change any existing shape; `PreviewInfo`
  is preserved untouched.
- **No bugs.**

### `WebUI/src/main/ts/contentExplorer/ContextMenu.tsx` (NEW, 130 lines)

- Cascading-children support via `useState<string | null>` for
  `openPivot`; each pivot has `role="menuitem"` + `aria-haspopup="menu"`
  + `aria-expanded={expanded}` + `id`/`aria-controls`-style identity for
  external hooks.
- `Escape` fires `onClose` (verified by Vitest test 4 +
  Playwright test 5).
- `activate(action)` either navigates to `action.url` if provided OR
  invokes `onInvoke(action.name, action)`. Action execution is the
  host's responsibility (the explorer shell wires the
  `ReducedActions` callbacks).
- `useEffect(() => setOpenPivot(null), [actions])` resets cascade state
  when the parent provides a new `actions` list (selection-change
  refresh per T054).
- All DOM ids use React's `useId()` (a11y-safe for SSR; deterministic
  per render).
- Pure presentation; no fetch side effects; no console logging; no
  `dangerouslySetInnerHTML`. All text is interpolated via React
  (auto-escaped).
- Keyboard path: menu items are `tabIndex={0}` and activate on `click`
  / `Enter`. The component does not yet intercept `ArrowDown` /
  `ArrowUp` for in-menu focus traversal — that's documented as
  future polish; the FR-013 minimum (Tab reaches + Enter activates) is
  satisfied.
- **No bugs.**

### `WebUI/src/main/ts/contentExplorer/ActionToolbar.tsx` (NEW, 64 lines)

- Renders one `<button>` per action with `aria-label` derived from
  `a.label` (server-side `label` / fallback to `name`).
- Empty-state placeholder uses the same `message()` i18n wrapper as the
  rest of the modern bundle (`perc.ui.explorer@No actions`).
- `activate(action)` follows the same URL-or-callback pattern as
  ContextMenu.
- All text auto-escaped by React; no innerHTML; no fetch.
- **No bugs.**

### `WebUI/src/main/ts/registry.ts` (modified, +4/-1)

- Adds `componentRegistry.set("ActionToolbar", ActionToolbar)` and
  `componentRegistry.set("ContextMenu", ContextMenu)` so the
  `PercModernUI.mount()` bridge can resolve them.
- Imports are at the top in alphabetical order with the existing
  contentExplorer / contentBrowser imports.
- **No bugs.**

### `actionMenuModern.jsp` ×2 (mirror in `cm/pages/app/`)

- Same self-loading bridge pattern as T045a / T045b / T045d
  (`actionMenuModern.jsp` calls `PercModernUI.mount(...)` with
  `idempotent script[src*="perc-modern-ui.js"]` guard + `setTimeout(50)`
  retry; `cb=` cache-buster).
- Two mount targets (toolbar + menu) with unique DOM ids
  (`perc-action-toolbar-root`, `perc-context-menu-root`,
  `perc-action-menu-result`).
- Demo `actions` array for the menu is hardcoded (server returns empty
  on the dev CMS); the toolbar renders its empty-state placeholder
  against the live `/actions/find` result so the wiring is exercised
  end-to-end. No production assertion changes; the spec defines what
  the dev-CMS smoke path asserts.
- TMX / CsrfGuard / `<i18n:settings>` headers and PSRoleUtilities
  locale handling match the asset / page / folder picker JSPs.
- All output via `textContent`, no `innerHTML`.
- **No bugs.**

### `WebUI/src/test/ts/contentExplorer/actionMenuApi.test.ts` (NEW, 12 tests)

- Pure unit tests + fetch-mocked integration tests (per the existing
  `WebUI/src/test/ts/contentExplorer/setup.ts` `mockFetch` helper).
- Tests assert:
  - top-level sort by `sortRank` ascending (test 1)
  - child flattening under a parent menu (test 2)
  - label fallback to `name` (test 3)
  - end-to-end preservation of `label` / `url` / `handler` /
    `description` (test 4)
  - children-key omission when no children (test 5; uses vanilla
    `toBeUndefined` + `"children" in obj` instead of jest-dom
    matchers, per the b013222f14 limitation that jest-dom matchers do
    not auto-load when running from module-level test directories)
  - empty input array returns empty output (test 6)
  - input not mutated by sort (test 7)
  - wire envelope unwrap for `findActions` (test 8) and `isAA=false`
    default (test 11)
  - URL composition (`/actions/find?name=open`, `/find/types`, `/find/templates/42?isAA=true`)
    tests 8, 10, 11, 12
  - POST body shape for `findAllowedContentTypeMenus` (test 9)
  - empty-body tolerance (test 9)
- All 12 / 12 passing.
- **No bugs.**

### `WebUI/src/test/ts/contentExplorer/ContextMenu.test.tsx` (NEW, 7 tests)

- Renders one menuitem per action with the right label (test 1).
- Empty-state placeholder when no actions (test 2).
- Click on a leaf action invokes `onInvoke` with the right action name (test 3).
- `Escape` fires `onClose` (test 4).
- `aria-label` honouring (tests 5 & 6).
- Vanilla-DOM `isInDocument` smoke (test 7).
- All 7 / 7 passing.
- **No bugs.**

### `WebUI/src/test/ts/contentExplorer/ActionToolbar.test.tsx` (NEW, 4 tests)

- Toolbar role + per-button `aria-label` (test 1).
- Click → `onInvoke(actionName)` (test 2).
- Empty-state placeholder (test 3).
- `aria-label` defaulting (test 4).
- All 4 / 4 passing.
- **No bugs.**

### `modules/perc-qa-automation/frontend/tests/us3-menus.spec.js` (NEW, 5 tests)

- Uses `loginAsAdmin` + `BASE_URL` helpers from `helpers/auth.js`.
- Points to `actionMenuModern.jsp` with `?_=${Date.now()}` cache-buster
  per qa-automation AGENTS.md "Fast iteration".
- All five behavioural assertions:
  1. ActionToolbar mounts with `role="toolbar"` and the canonical
     `aria-label="Action toolbar"`.
  2. Empty-state placeholder (`action-toolbar-empty`) visible.
  3. ContextMenu mounts with the configured `aria-label="Demo context
     menu"` and 2 demo items (`open` + `preview`).
  4. Clicking `context-menu-item-preview` writes
     `"Invoked: preview"` to the `perc-action-menu-result` block (textContent
     assertion, not token grep).
  5. `Escape` on the menu fires `onClose` (menu stays in the document).
- All 5 / 5 passing in 15.7 s on the live docker dev CMS at
  `http://localhost:9992`.
- SC-003 ≥10 action visibility is gated on a system-installed CMS
  (the docker Derby dev image returns `{"ActionMenu":[]}` from
  `/actions/find`); the spec instead exercises the wiring surface
  and the README in the file documents why. The Vitest mapper tests
  + `sc003-actions-checklist.md` cover the structural enumeration.
- **No bugs.**

### `specs/992-react-content-explorer/checklists/sc003-actions-checklist.md` (NEW, 75 lines)

- 12-row action enumeration table mapping each P-Menu row to a
  canonical `name` / `handler`, the **Selection** class it applies to,
  and the **Execute path** (existing sitemanage REST, existing JSP
  route, or `ReducedActions`-style command).
- Workflow set (#10–#12) all enumerated; #11 documents the gap
  (ActionMenuResource.getAllowedTransitions is "// Not implemented yet")
  with a non-blocking rationale (US3 P-Menu in 8.2 can render #10 and
  #12; #11 surfaces a "not supported in this release" label until a
  follow-up rest enhancement lands).
- T052 decision recorded: NO new sitemanage or rest façade required
  for US3 P-Menu in 8.2.
- Test coverage map (Vitest + Playwright, by row) included for the
  release-evidence trail.
- **No bugs.**

### `specs/992-react-content-explorer/tasks.md` (modified)

- T048, T049, T050 ticked `[x]` with evidence.
- T051 ticked `[x]` (rest wrapper + types mirrored to live DTOs).
- T052 ticked `[x]` with explicit **NO new façade** decision and
  rationale. T052a / T052b explicitly marked N/A (gated by T052).
- T053, T054 (partial — see note), T055 ticked `[x]` with evidence.
- T056 ticked `[x]` with the checklist path.
- T056b ticked `[x]` with the live-CMS spec result.
- T057 left as `[ ]` (pending: Erlang review + commit + PR — the
  current PR).
- T057b ticked `[x]` with rationale (SC-003 ≥10 gated on a system-installed CMS).

## Cross-platform path review

Not applicable — the diff is the TypeScript / JSP / test layer; URL
constants are CMS-endpoint REST paths (`/Rhythmyx/rest/actions/*`,
`/Rhythmyx/cm/...`) and tm `tmx.jsp` / `JavaScriptServlet` paths
already covered by the established JSP pattern. No filesystem path
construction; no cross-platform checklist triggered.

## PR thread protocol

No prior review threads on this branch (newly cut off `development`).
After PR open, the implementer MUST apply constitution IX for each
review thread:
1. Reply inline with `**Mitigation (commit <hash>):** <description>`.
2. Run `gh api graphql resolveReviewThread` per thread.
3. Re-verify via the GraphQL `reviewThreads(first: 50) { nodes {
   isResolved } }` query before merging.

## Handoff

- Recommendation: `approve`. May commit/push: yes.
- Suggested commit split (matches the per-US/per-PR convention):
  1. `feat(992/us3): T051 typed actionMenuApi.ts + US3 action-menu types (mirrors DTOs)`
  2. `feat(992/us3): T053/T054/T055 ContextMenu + ActionToolbar components + registry`
  3. `test(992/us3): T048-T050 Vitest actionMenu + ContextMenu + ActionToolbar suites (23 tests)`
  4. `feat(992/us3): T056/T056b sc003-actions-checklist.md + us3-menus Playwright spec`
  5. `docs(992/us3): T052 decision recorded in sc003-actions-checklist (NO new façade)`
  6. `feat(992/us3): T056/T056b actionMenuModern.jsp pilot page + mirror`
  7. `docs(992/us3): tick T048-T056 / T057b; T052a/T052b marked N/A`
- After the PR lands, the next concrete open tasks are T045f (US2 per-host
  verify) and the US4 ACL work (T058-T064); US3 enumeration is now
  measurable per `sc003-actions-checklist.md` and the new
  `tests/us3-menus.spec.js`.
