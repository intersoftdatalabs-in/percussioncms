# app + home audit

Manual review of the 3 + 3 regex candidates produced by the Phase-0 sweep for
`app/` and `home/` under `WebUI/src/main/ts/`. Counts and disposition per
file:line, with reuse and TMX pre-flight notes.

Prefix conventions (per `WebUI/src/main/ts/i18n/message.ts` and the Phase-0
plan):

- `perc.ui.app.shell@…` for app/router chrome (landing + fallback routes).
- `perc.ui.home.modern@…` for Home section labels and chrome.
- `perc.ui.home@…` for Home screen-level labels already shipped under the
  short Home prefix.
- `perc.ui.navMenu.*@…` reused for top-level nav links (already a published
  `perc.ui.navMenu.home@Home` in TMX).

## Scope

- `app/`: 3 candidate hits across 3 files.
  - `app/App.tsx:42` — 1 false positive (JSDoc).
  - `app/LandingShell.tsx:105` — 1 aria-label on the landing nav.
  - `app/routes.tsx:55` — 1 `title` prop on the `<FeaturePlaceholder />`
    for the catch-all `/unavailable` route.
- `home/`: 3 candidate hits across 2 files.
  - `home/sections/GadgetsSection.tsx:33` — 1 aria-label on the Gadgets
    section container.
  - `home/UnavailableView.tsx:41` — 1 anchor text "Home" (recovery link).
  - `home/UnavailableView.tsx:43` — 1 anchor text "Gadgets" (recovery
    link).

## Reusable keys (already in MSG or TMX)

- `perc.ui.navMenu.home@Home` — used by `home/UnavailableView.tsx:41`.
  Already in `MSG.NAV_HOME` (`WebUI/src/main/ts/i18n/message.ts:175`) and in
  `CmsUi.tmx` (line 1490). Reuse `message(MSG.NAV_HOME)`.
- `perc.ui.home.modern@Gadgets` — used by `home/sections/GadgetsSection.tsx:33`
  and `home/UnavailableView.tsx:43`. Already in `MSG.SECTION_GADGETS`
  (`message.ts:81`) and `MSG.DASHBOARD_EMBEDDED_TITLE` (`message.ts:193`).
  Not yet in `CmsUi.tmx` (the `perc.ui.home.modern@` prefix has 31 TUs but
  no `@Gadgets` tuid; the `perc.ui.dashboard.modern@` prefix has 0 TUs and
  is the new chrome-prefix backfill for Phase 2). Reuse `message(MSG.SECTION_GADGETS)`.
- `perc.ui.home.modern@Unavailable` — used by `app/routes.tsx:55`. Already in
  `CmsUi.tmx` (line 77982). Not yet exposed via a `MSG.*` constant — add a
  new constant (e.g. `MSG.ROUTE_UNAVAILABLE_TITLE =
  "perc.ui.home.modern@Unavailable"`) in Phase 1; reuse the existing tuid
  here. Reusing the Home-screen tuid is preferred over minting a duplicate
  `perc.ui.app.shell@Unavailable` because the text is identical and the
  runtime is last-wins on duplicate tuids (per
  `modules/perc-i18n/AGENTS.md` §1a).

## New keys (need new TMX entry + MSG constant)

|         file:line          |        english         |              proposed tuid               |    proposed MSG constant     |                                                                                                                                                                                                                                                               notes                                                                                                                                                                                                                                                                |
|----------------------------|------------------------|------------------------------------------|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `app/LandingShell.tsx:105` | `Temporary navigation` | `perc.ui.app.shell@Temporary navigation` | `MSG.LANDING_NAV_ARIA_LABEL` | `aria-label` on the landing page's `<nav>` (sibling anchors are `Home`, `Publishing`, `Gadgets` and are also plain English today — not in the candidate list because the regex only flagged attributes, but the `aria-label` is the screen-reader-visible label for that nav block, so it ships first; the anchor text is a follow-up). Confirmed not in `CmsUi.tmx` via `Select-String -Pattern 'tuid="perc\.ui\.[^"]*@Temporary navigation"'` (no match). Place under the `perc.ui.app.shell@` prefix per Phase-1 prefix census. |

> All other real candidates in scope already have a usable `MSG.*` constant
> — see the "Reusable keys" section above. No other new tuids are needed
> for `app/` or `home/` based on this sweep.

## False positives (do NOT localize)

- `app/App.tsx:42` — JSDoc prose `* <p>Must run <em>before</em> {@code
  BrowserRouter} mounts …` inside the `applyEntryQueryToPath` doc-block
  (`app/App.tsx:38-48`). This is developer documentation in a `/** … */`
  block, not user-visible chrome. Per the Phase-0 manual-review rules,
  comments/JSDoc are explicitly excluded from i18n extraction.

## Notes for Phase-1 / Phase-2 / Phase-3 implementers

- Phase 1 (`WebUI/src/main/ts/i18n/message.ts`): add
  `MSG.LANDING_NAV_ARIA_LABEL = "perc.ui.app.shell@Temporary navigation"`
  and `MSG.ROUTE_UNAVAILABLE_TITLE = "perc.ui.home.modern@Unavailable"`.
  Group the new constants under a small `APP_SHELL` block (or top-level if
  the file stays flat) to keep grep predictable.
- Phase 2 (`CmsUi.tmx`): add exactly one new `<tu tuid="perc.ui.app.shell@Temporary navigation">`
  with an `en-us` `<seg>`. The `perc.ui.home.modern@Unavailable` tuid
  already exists; do **not** add a second `perc.ui.app.shell@Unavailable`
  (last-wins duplicate risk). The two `perc.ui.home.modern@Gadgets` /
  `perc.ui.dashboard.modern@Gadgets` keys are part of the broader Phase-2
  backfill for `perc.ui.home.modern@` and `perc.ui.dashboard.modern@` —
  outside the scope of this `app + home` audit but cited here so the
  wiring PR doesn't double-book them.
- Phase 3 (SPA wiring):
  - `app/LandingShell.tsx:105` — `<nav style={navStyle} aria-label="Temporary navigation">`
    → `aria-label={message(MSG.LANDING_NAV_ARIA_LABEL)}`.
  - `app/routes.tsx:55` — `<FeaturePlaceholder title="Unavailable" testId="route-unavailable" />`
    → `<FeaturePlaceholder title={message(MSG.ROUTE_UNAVAILABLE_TITLE)} testId="route-unavailable" />`.
    Verify the `FeaturePlaceholder` component renders `title` as visible
    text (not a `title=` attribute); if it does both, localize the visible
    heading and leave the `title` attribute alone.
  - `home/sections/GadgetsSection.tsx:33` — `aria-label="Gadgets"` →
    `aria-label={message(MSG.SECTION_GADGETS)}`.
  - `home/UnavailableView.tsx:41` — `<a …>Home</a>` → text content
    `{message(MSG.NAV_HOME)}`.
  - `home/UnavailableView.tsx:43` — `<a …>Gadgets</a>` → text content
    `{message(MSG.SECTION_GADGETS)}`.
- Playwright (HARD GATE per `WebUI/AGENTS.md`): the Phase-3 PR-A (Dashboard
  / Gadgets / app shell / home stragglers) must add or extend a spec under
  `modules/perc-qa-automation/frontend/tests/` that switches locale
  (helpers in `tests/helpers/locale.js`) and asserts the localized values
  for the two new keys (nav aria-label, route title) and the three reused
  ones. Reuse `data-testid="route-unavailable"` for the route title
  selector and add a `data-testid` for the landing nav if one is missing.

