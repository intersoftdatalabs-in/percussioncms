# Plan: Replace EOL FullCalendar 2.9.1 in the calendarTwo widget

**Tracking issue:** [#6687](https://github.com/intersoftdatalabs-in/percussioncms/issues/6687)
— `[8.2] Plan replacement of EOL FullCalendar 2.9.1 in calendarTwo widget (audit #5627)`
**Parent audit:** [#5627](https://github.com/intersoftdatalabs-in/percussioncms/issues/5627)
**Slice 1 PR:** behavioral surface lock + this plan (no payload bytes change)
**Last updated:** 2026-11-20

## Status

FullCalendar 2.x has been end-of-life since 2016: no security fixes, no
upstream, and the published calendarTwo widget serves it to site visitors.
Slice 5 of #6502 rebased the payload pair onto the npm
`fullcalendar@2.9.1` dist (exact pin, Dependabot-visible) plus two
documented Percussion mods, and slice 6 of #6522 did the same for the gcal +
lang-all companions — so the vendoring gap is closed, but Dependabot
visibility into a dead line is not a maintenance story. This plan records the
replacement path. **No keep-2.x-forever decision is recorded here**: that
outcome needs an explicit maintainer risk sign-off on #6687 (see
§Alternatives).

## Consumed surface inventory (method step 1)

Derived from `percCalendarTwoSnippet.vm` (full layout) plus the mini-calendar
branch of the same template. Every row below is locked behaviorally by
`WebUI/src/main/frontend/src/test/js/calendarTwoFullcalendarSurface.test.js`
(slice 1); byte parity stays pinned by `widgetPayloadNpmParity.test.js` and
`WidgetPayloadVendoredJsAuditTest`.

| # | Surface | Consumer | Notes |
|---|---------|----------|-------|
| 1 | `$("#…").fullCalendar({…})` init with `timeFormat`, `defaultDate`, `lang`, `firstDay`, `defaultView`, `header`, `eventSources`, `editable: false`, `dayClick`, `eventRender` | full layout | jQuery-plugin API, removed upstream in v4 |
| 2 | `fullCalendar("destroy")` + re-init | view toggle buttons | calendar ⇄ list toggle re-creates the instance |
| 3 | `reportEvents` → `Perc.events[target].resolve(_events)` (Percussion mod 1) | list layout + toggle | snippet awaits the Deferred to render the event list; also consumed `start._d` moment internals in the DTS fetch |
| 4 | `MonthView.computeRange` reads `Perc.range` (Percussion mod 2) | list layout | 6 weeks calendar / 52 weeks list; stock hard-codes 6 |
| 5 | `googleCalendarApiKey` / `googleCalendarId` event sources (gcal.js) | google feeds | `googleCalendarArray` loop; `className: perc-<class>` |
| 6 | `lang:` option + 56-locale `lang-all.js` | i18n | `perc_calendar_display_language`; mini-calendar branch uses `$.datepicker.regional[lang]` from the shipped jquery-ui |
| 7 | qTip2 `eventRender` summary tooltips | full layout | already migrated to npm `qtip2@3.0.3` (slice 9 of #6522); any successor keeps the call site |
| 8 | `fc-*` CSS classes + `fullcalendar.print.css` | snippet inline `<style>` | responsive overrides target `fc-left/center/right`, `fc-button`, `fc-state-default`; print stylesheet ships alongside |
| 9 | moment event formatting (`moment(start).format/isBefore`) | list layout | payload already on npm `moment@2.31.0` (slice 1 of #6319) |

## Evaluation (method step 2)

All license checks run 2026-11-20 via `npm view <pkg>[@<ver>] license`
(license must be MIT/BSD/Apache-2.0 for distribution — never GPL).

### Recommended: FullCalendar v6 maintained line (MIT)

- `fullcalendar@6.1.15` Standard Bundle — `license: MIT`, ships
  `index.global.min.js` (`unpkg`/`jsdelivr` fields), i.e. a plain-`<script>`
  global build matching how the widget loads scripts today (no bundler
  required on published pages).
- Scoped plugins, same version line, all MIT: `@fullcalendar/core`,
  `@fullcalendar/daygrid`, `@fullcalendar/timegrid`, `@fullcalendar/list`,
  `@fullcalendar/interaction`, `@fullcalendar/google-calendar`
  (`@fullcalendar/core@6.1.22 license: MIT`).
- `locales-all` dist replaces `lang-all.js` (same 56-locale coverage to
  verify in the migration slice); `@fullcalendar/list` may subsume the
  hand-rolled list rendering, but the snippet's list markup is custom — keep
  the `Perc.events`/`Perc.range` fetch semantics either way.
- API rewrite (no jQuery plugin): `new Calendar(el, {…}).render()`,
  `locale`/`locales` instead of `lang:`, event-source function signature
  `(fetchInfo, successCallback, failureCallback)`, google-calendar plugin
  options instead of gcal.js. Both Percussion mods must be re-derived
  against the new API (§Carry-over).

### Deferred: FullCalendar v7 line (MIT, restructured)

- `fullcalendar@7.1.5`, `@fullcalendar/core@7.1.5` — `license: MIT`, but v7
  restructured around `@full-ui/headless-calendar` + `temporal-polyfill`
  peer deps and `@fullcalendar/core` is now a types package. Bigger unknown
  surface than v6; re-evaluate only if v6 leaves npm support.
  (`npm view` 2026-11-20.)

### Not recommended

- **Keep 2.x forever.** Would need a maintainer-signed risk rationale on
  #6687: EOL grid served to visitors, jQuery-4-era shims (`$.isFunction` /
  `$.type` / `$.isArray` only work via jquery-migrate), moment-era date
  handling. Not recorded here — offered as the explicit alternative for the
  maintainer decision.
- **First-party month-view rewrite.** Loses the gcal feed plugin and the
  56-locale matrix; re-implements accessibility/keyboard surface from
  scratch. Higher cost than adopting the maintained MIT line.

## Percussion-mod carry-over (method step 3)

1. **`Perc.events` deferred.** v6 `events(fetchInfo, onSuccess, onFailure)`
   receives the same fetch; resolve `Perc.events[target]` inside `onSuccess`
   (or the `eventsSet` callback) with the returned array so the snippet's
   `jQuery.when(…)` list rendering keeps working. Re-derive, do not copy
   the 2.x `reportEvents` hunk.
2. **`Perc.range` month expansion.** v6 `dayGridMonth` has no 52-week mode;
   the list layout either keeps a hidden month instance with an extended
   `validRange`/custom `dayCount`, or fetches via the DTS-backed function
   source directly without rendering. The migration slice decides; the
   behavioral lock (`6 vs 52 weeks`) is the acceptance bar either way.

## Phased slices

1. **Slice 1 (this PR): plan + behavioral lock.** This document plus
   `calendarTwoFullcalendarSurface.test.js` (7/7 green). No payload,
   snippet, or manifest change — no user-visible behavior change.
2. **Slice 2 (spike): v6 global-bundle packaging proof.** `npm install`
   the v6 bundle + daygrid/list/google-calendar/locales-all pins in the
   frontend manifests (exact pins, Dependabot-visible), load
   `index.global.min.js` in a jsdom spike, and map each §Inventory row to
   the v6 API. Output: mapping table + open questions (CSS class deltas,
   print stylesheet, mini-calendar datepicker interplay).
3. **Slice 3 (migration): payload swap + snippet rewrite.** Replace the
   `calendarTwo/js` 2.9.1 payloads with the v6 global dists (+ re-derived
   Perc mods), update `percCalendarTwo.xml` script entries and the snippet
   init/teardown, port the surface lock to the new API, update
   `WidgetPayloadVendoredJsAuditTest` pins, product-docs widget page, and
   Playwright parity for the published widget (month render + list render +
   view toggle). C5 live proof required.
4. **Slice 4 (cleanup): drop the 2.9.1 pin.** Remove `fullcalendar@2.9.1`
   from both frontend manifests once no payload references it; guard +
   parity tests updated in the same change.

## Residuals / open questions (not filed as issues per the #6687 slice scope)

- ~~v6 `dayGridMonth` CSS class names vs the snippet's `fc-*` responsive
  overrides — verify in slice 2 (visual diff, not just class presence).~~
  **Slice 2a landed (#6804 follow-on PR):** v6.1.15 Standard Bundle pinned
  as the `fullcalendar-v6` npm alias (exact, both frontend manifests;
  `fullcalendar@2.9.1` untouched) with a 6-assertion jsdom spike
  (`calendarTwoFullcalendarV6CssMapping.test.js`) rendering the real
  `index.global.min.js` dayGridMonth (42 day cells, event + `h2` title).
  Findings: toolbar sections are gone (three unstyled `.fc-toolbar-chunk`
  divs — the 480px override must be retargeted, v6 does not wrap on its
  own); no `.fc-state-default` / `.fc-corner-*` anywhere (toggle buttons
  need a compat rule); event text hook is `.fc-event-title` (not
  `.fc-title`); view wrapper is `.fc-view-harness` (not
  `.fc-view-container`); styles are JS-injected (`<style
  data-fullcalendar>`, honors `meta[name=csp-nonce]` — feeds slice 3b).
  Visual parity beyond DOM hooks (real browser screenshot diff) stays with
  the slice-3 Playwright run.
- ~~`fullcalendar.print.css` equivalent in the v6 bundle.~~ **Slice 2a
  boundary:** the v6 bundle ships zero `*.css` and its injected CSSOM
  carries no `@media print` rules — authoring that equivalent is slice 2b
  scope (asserted as absence, not resolved, in the 2a spike).
- Mini-calendar branch (`$.datepicker`) is jquery-ui, unaffected — but its
  `fullCalendarPage` link target assumes the full layout keeps its URL
  contract; preserve in slice 3.
- gcal JSONP + `googleCalendarApiKey` handling under the site CSP —
  re-verify in the slice-3 Playwright run.
