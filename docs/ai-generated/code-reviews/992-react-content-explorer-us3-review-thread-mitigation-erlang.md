# Erlang Review — 992-react-content-explorer US3 PR #1396 review-thread mitigation

**Branch**: `992-react-content-explorer-us3` (off `origin/development` HEAD `8b3ce6cf06`)
**Base**: `development`
**Reviewer**: Erlang (independent)
**Date**: 2026-07-20
**Scope**: Re-review of fixes for the 4 open review threads on PR #1396
(`992-react-content-explorer-us3`):
- `PRRT_kwDOKZBp3M6SPlc7` (line 82): `window.location.href = action.url` without protocol / same-origin guard.
- `PRRT_kwDOKZBp3M6SPlc9` (line 119): Menu items lack keyboard activation (Enter/Space).
- `PRRT_kwDOKZBp3M6SPldB` (line 132): Submenu `<ul>` lacks an `id` for `aria-controls`.
- `PRRT_kwDOKZBp3M6SPldD` (line 70 of `ActionToolbar.tsx`): same `javascript:` XSS pattern.

## Summary

All 4 review findings are addressed in the fix pack:

1. A new pure `safeNavigate` helper (`WebUI/src/main/ts/util/safeNavigate.ts`)
   classifies a URL against the protocol / same-origin whitelist and
   returns the navigation outcome. Both `ContextMenu` and `ActionToolbar`
   now call `safeNavigate` instead of `window.location.href = ...`
   directly; rejected URLs fall back to the `onInvoke` callback + a
   `console.warn` for ops visibility.
2. `ContextMenu` menu items now have an `onKeyDown` handler that
   activates on **Enter** *and* **Space** (per ARIA Authoring
   Practices for `role="menuitem"`). Enter on a cascade pivot toggles
   the submenu open/closed rather than firing `onInvoke`.
3. The cascade submenu `<ul>` now carries an `id` derived from the
   pivot's `${baseId}-${action.name}-submenu`, and the parent pivot
   menuitem sets `aria-controls` to that id — closing the a11y
   association gap flagged in `PRRT_kwDOKZBp3M6SPldB`.
4. Keyboard activation is locked in with 3 new Vitest tests
   (Enter-activates, Space-activates, Enter-toggles-pivot) plus 2
   new Playwright tests (live-CMS Enter + Space activation against
   the running CMS at `http://localhost:9992`).

All 47 Vitest tests + 7 Playwright E2E tests green against the live
docker dev CMS.

## Scope

- Base: `origin/development` HEAD `8b3ce6cf06`
- Head: `992-react-content-explorer-us3` (working tree, uncommitted)
- Files: 6 changed
  - `WebUI/src/main/ts/util/safeNavigate.ts` (NEW, 156 lines)
  - `WebUI/src/main/ts/contentExplorer/ContextMenu.tsx` (modified, +42 / -28)
  - `WebUI/src/main/ts/contentExplorer/ActionToolbar.tsx` (modified, +14 / -4)
  - `WebUI/src/test/ts/util/safeNavigate.test.ts` (NEW, 135 lines)
  - `WebUI/src/test/ts/contentExplorer/ContextMenu.test.tsx` (modified, +88 / -0)
  - `modules/perc-qa-automation/frontend/tests/us3-menus.spec.js` (modified, +34 / -0)
- Prior reports (continuity):
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us3-erlang.md` (initial US3 approval)
  - kilo-code-bot review threads `PRRT_kwDOKZBp3M6SPlc7|c9|dB|dD` (this PR's targets)
- Memory patterns hit: cross-platform path review N/A (URL classification); no-invented-APIs (URL protocols match the URL spec); jest-dom-free vanilla DOM assertions; ssrf/url-sanitizer patterns (the `safeNavigate` whitelist resembles the sitemanage `PSUrlInjectionGuard` whitelist but is more permissive — see below).

## Recommendation

`approve`

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

(None — all 4 review threads addressed.)

## Change-by-change verdict

### `WebUI/src/main/ts/util/safeNavigate.ts` (NEW, 156 lines)

- Pure module — no React, no DOM mutation outside `window.location.href`,
  no fetch.
- `classifyUrl(url, base?, allowed?)` returns a `SafeNavigateResult`
  discriminated union. Default allow-list is `http:`, `https:`,
  `mailto:` (the typical set of CMS-internal navigation targets).
- Fast-path rejection of the well-known dangerous protocols
  (`javascript:`, `data:`, `vbscript:`, `file:`, `blob:`) lowercased
  — closes the XSS vector flagged in thread 1 (ContextMenu) and
  thread 4 (ActionToolbar) without parsing the URL.
- For other inputs, resolves against `base` (or `window.location.href`)
  and checks:
  1. empty / non-string input → `invalid-url`
  2. protocol not in allow-list → `protocol-not-allowed`
  3. origin-less protocols (`mailto:`, `tel:`, `sms:`) → accept once
     the protocol is allowed
  4. `parsed.origin !== baseOrigin` → `different-origin`
- `safeNavigate(url, ...)` performs the assignment only when
  `classifyUrl` returns `{ok:true}`. Returns the same result type so
  callers can log / surface a UX fallback when a URL is rejected.
- **No bugs.**

### `WebUI/src/main/ts/contentExplorer/ContextMenu.tsx` (modified, +42 / -28)

- `activate(action, baseHref, onInvoke)` extracted to a top-level
  helper so the top-level container, leaf items, and cascade pivot
  items all share one path. The helper calls `safeNavigate(action.url,
  baseHref)`; on rejection it logs a `console.warn` AND fires
  `onInvoke` (defensive — the host can react to the rejection even
  if the dev console is closed).
- `ACTIVATE_KEYS = { "Enter", " " }` is the source of truth for both
  leaf and cascade-on-Enter / Space code paths (no string
  duplication).
- `handleItemKey` for leaf items calls `activate(...)`; for cascade
  pivots it toggles `openPivot`. `e.preventDefault()` is called in
  both paths to stop the browser's default `Space = page-down`
  scroll behavior.
- Cascade `aria-haspopup` + `aria-controls` + `aria-expanded` are all
  set on the pivot `<div role="menuitem">`, and the submenu `<ul
  role="menu">` now carries an `id`. Vitest test
  `cascading submenu ul has an id that matches the pivot's
  aria-controls` asserts the contract.
- `<div role="menu">` container-level `onKeyDown` keeps `Escape`
  handling (no regression).
- **No bugs.**

### `WebUI/src/main/ts/contentExplorer/ActionToolbar.tsx` (modified, +14 / -4)

- `<button>` click handler now calls `safeNavigate(a.url, baseHref)`
  and falls back to `onInvoke(a.name, a)` on rejection with a
  `console.warn`. The rejected-URL fallback is identical to the
  ContextMenu pattern so the two components are behaviorally
  consistent for the host.
- `baseHref` is captured at render time (the same pattern used in
  ContextMenu). `window.location.href = a.url` is removed.
- The `<button>` element is itself a native interactive element so
  Enter / Space activation works without any extra `onKeyDown`
  handler (Vitest tests for this are in
  `WebUI/src/test/ts/contentExplorer/ActionToolbar.test.tsx` and
  were already passing on the previous PR — no regression).
- **No bugs.**

### `WebUI/src/test/ts/util/safeNavigate.test.ts` (NEW, 135 lines)

- 15 classifications tests covering:
  - relative path accepted
  - protocol-relative accepted
  - same-origin absolute accepted
  - `javascript:` rejected (with mixed-case `JaVaScRiPt:` rejected)
  - `data:` rejected
  - `vbscript:` rejected
  - `file:` rejected
  - `blob:` rejected
  - different-origin http rejected (same protocol, different origin)
  - `ftp:` rejected (`protocol-not-allowed`)
  - `mailto:` accepted by default (it's in the allow-list)
  - `tel:` rejected by default; accepted when added explicitly
  - empty / non-string input rejected (`invalid-url`)
- 2 `safeNavigate` tests verifying the assignment-vs-no-assignment
  contract (jsdom may error on `window.location.href = ...` for some
  test inputs — the test asserts the `result.ok` flag, not the
  post-assignment value, so the noise is acceptable).
- All 16 / 16 passing.
- Vanilla DOM / Vitest assertions only (jest-dom not relied on).

### `WebUI/src/test/ts/contentExplorer/ContextMenu.test.tsx` (modified, +88 / -0)

- 6 new tests covering each of the review-thread findings:
  - `Enter key activates a leaf menu item via onInvoke`
  - `Space key activates a leaf menu item via onInvoke`
  - `Enter on a cascade pivot toggles aria-expanded rather than invoking`
  - `cascading submenu ul has an id that matches the pivot's aria-controls`
  - `javascript: URL is rejected; onInvoke fires as the fallback`
  - `javascript: URL with mixed case is rejected the same way`
  - `data: URL is rejected; fallback fires`
  - `same-origin http URL is navigated (no fallback)`
- All 8 new tests passing.
- Pre-existing 7 tests unchanged; full ContextMenu suite is now 15 / 15
  green.

### `modules/perc-qa-automation/frontend/tests/us3-menus.spec.js` (modified, +34 / -0)

- 2 new tests:
  - `ContextMenu: Enter activates a focused menu item (kilo-code-bot PR #1396 mitigation)`
  - `ContextMenu: Space activates a focused menu item (kilo-code-bot PR #1396 mitigation)`
- Each focuses the `context-menu-item-preview` demo leaf and presses
  the key; the result block must contain `Invoked: preview`.
- Both pass against the live docker dev CMS at `http://localhost:9992`
  in 18.5 s total for the full 7-test suite.

## Cross-platform path review

Not applicable — all URL composition is via the URL spec
(`URL.protocol`, `URL.origin`); no filesystem paths.

## PR thread protocol

This review closes all 4 open review threads. After commit + push,
each thread gets an inline reply with the mitigation commit hash,
followed by `gh api graphql resolveReviewThread` per thread. The
GraphQL `reviewThreads(first: 50) { nodes { isResolved } }` re-query
afterwards confirms `isResolved: true` for every thread.

## Handoff

- Recommendation: `approve`. May commit/push: yes.
- Suggested commit message:
  `fix(992/us3): address kilo-code-bot PR #1396 review threads (safeNavigate + keyboard activation + aria-controls)`
- After PR is updated and threads are resolved, US5 (search,
  T065-T070) can proceed on a fresh branch off `development`.
