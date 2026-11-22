---
id: developer-calendar-widget
title: Calendar 2.0 widget runtime
description: FullCalendar runtime shipped inside the Calendar 2.0 (calendarTwo) widget
version: "8.2"
order: 56
tags: [developer, widgets, calendar]
---

# Calendar 2.0 widget runtime

The **Calendar 2.0** (`perc.widget.calendar` → `percCalendarTwo`) widget
renders site calendars from Percussion calendar assets, Google Calendar
feeds, or both. As of 8.2 the published widget runs the maintained
**FullCalendar v6.1.15** global bundle (MIT) that ships *inside the widget
package* — the end-of-life FullCalendar 2.9.1 line is gone from the payload
(#6919).

## What ships where

The widget package carries, under
`web_resources/widgets/calendarTwo/js/`:

- `fullcalendar.js` / `fullcalendar.min.js` — stock FullCalendar 6.1.15
  Standard Bundle (preview / published copies, byte-identical to npm).
- `gcal.js` — stock `@fullcalendar/google-calendar` 6.1.15 global dist,
  wiring the `googleCalendarArray` feed loop.
- `lang-all.js` — stock `@fullcalendar/core` 6.1.15 `locales-all` global
  dist, backing the widget **Language** preference (`locale:`).
- `moment.min.js` — Moment 2.31.0 for list-view date formatting (unchanged).

`percCalendarTwo.xml` loads these scripts for both preview and published
output; no bundler is required on delivered sites.

## Dependency tracking

The EOL `fullcalendar@2.9.1` npm pin is gone from both WebUI frontend
manifests (`WebUI/package.json`,
`WebUI/src/main/frontend/package.json`, slice 3, #6946): no payload
referenced the EOL line after the v6 migration, so Dependabot no longer
watches a line that ships nothing. The `fullcalendar-v6*` aliases stay
exact-pinned at 6.1.15, and the frontend parity test
(`widgetPayloadNpmParity.test.js`) fails if the EOL pin returns.

## View mapping (2.x → v6)

The widget **Default calendar type** and **Detail Level** preferences map to
v6 views in `percCalendarTwoSnippet.vm`:

| Preference | v6 view |
|------------|---------|
| Month | `dayGridMonth` (fixed 6 weeks) |
| Week + Agenda | `timeGridWeek` |
| Day + Agenda | `timeGridDay` |
| Week + Basic | `dayGridWeek` (event times hidden) |
| Day + Basic | `dayGridDay` (event times hidden) |

**Time format** `ampm` renders 12-hour times, `military` 24-hour times.
`Basic` detail hides event times in week/day views; month view always shows
them. The responsive rule stacks the three toolbar chunks below 480px, and
the Calendar/List toggle buttons carry the previous theme look.

## Layout switching

The **Calendar/List toggle buttons** (`Show Calendar` / `Show List`)
destroy and re-create the calendar instance when both layouts are enabled.
Switching is instant (no animation) so printed and captured output is
deterministic. The buttons expose `aria-pressed` mirroring the visible
layout inside a `role="group"` labelled "Calendar layout", and the list
region announces updates via `aria-live="polite"`.

While the list fetch is in flight the list shows a **Loading events…**
row; when no event survives the today-or-later filter the list shows an
explicit **No upcoming events.** row instead of a blank region. Both
strings are widget preferences — **List loading text** and **List empty
text** (English defaults as above) — so a non-English widget can
localize the `aria-live="polite"` list region instead of announcing
English; existing widgets keep the English output unchanged.

## Source-filter buttons (keyboard access, #6992)

The **source-filter buttons** (one per Google feed plus the Percussion
events button, shown above the calendar in Calendar layout) toggle the
per-source event layers. They are native buttons inside a
`role="group"` labelled "Calendar sources", so keyboard users Tab to a
filter and press **Enter** or **Space** to toggle it — the same
show/hide contract as a mouse click. Each button exposes
`aria-pressed` (`true` while its source is shown) and a dimmed style
while its source is hidden; keyboard focus paints a visible outline
that mouse clicks never show, so the mouse-path rendering is
unchanged.

## Google Calendar feeds (keep decision, #6945)

Google feeds stay enabled. The widget ships the stock
`@fullcalendar/google-calendar` 6.1.15 plugin, which loads feeds with a
CORS `fetch()` against
`https://www.googleapis.com/calendar/v3/calendars` — the 2.x JSONP feed
path is gone with the old payload, so there is nothing JSONP-era left to
remove. If your site sends a restrictive Content-Security-Policy, allow
`connect-src https://www.googleapis.com` or Google feeds will fail to
load. A feed configured **without** an API key fails gracefully: that
source contributes zero events while the calendar and every other source
keep rendering.

## Source-filter buttons

Each widget instance renders a **Calendar sources** filter bar
(`role="group"`, labelled "Calendar sources") with one native button
per Google feed plus one for Percussion events. The buttons are
keyboard-operable (Tab to reach, Enter or Space to toggle) and expose
`aria-pressed` mirroring the layer visibility, with a
`:focus-visible`-only ring so the mouse path is pixel-identical.

## Multiple widgets on one page

Filter button ids and event-layer classes are suffixed with the widget
instance's content and widget ids, so two Calendar 2.0 widgets on the
same page never emit duplicate DOM ids and never share a layer class.
Toggling a source in one widget hides only that widget's events and
flips only that widget's `aria-pressed` state — even when both widgets
configure the same feed. No page changes are needed to get this
isolation; it is baked into the rendered markup.

**Upgrade note:** buttons and event layers keep the unsuffixed
`perc-<className>` class alongside the suffixed one, so existing
site/theme CSS targeting the legacy hooks keeps matching and the toggle
sweep keys off the suffixed class. Button *ids* are suffixed only —
duplicate DOM ids were the defect — so scripts that addressed the old
unsuffixed button ids must target the suffixed ids instead.

## Print stylesheet

The widget links `web_resources/widgets/calendarTwo/css/fullcalendar.
print.css` with `media="print"`. As of 8.2 the rules target the v6
renderer: calendar chrome (toolbar buttons, toggle buttons, source-filter
bar) hides, background and highlight layers drop out, scrollers expand to
full height, events flatten to black-on-white with `page-break-inside:
avoid`, and list rows keep one row per page. The month grid and the event
list print as plain markup.

## Mini-calendar links

The **Mini calendar** layout renders a month picker. Picking a day
navigates to the full calendar page configured on the widget:

```text
<fullCalendarPage>?startDate=MM,DD,YYYY
```

For example, picking November 21, 2026 on a widget whose full page is
`/calendar` navigates to `/calendar?startDate=11,21,2026`. Days that have
events render highlighted; in edit/preview mode the picker is inert (no
navigation).

## Layouts

- **Calendar layout** renders the month grid above; event details come from
  the DTS dated query for the visible range (or from the page list in
  preview/edit mode).
- **List layout** renders the upcoming-events list directly (up to **Max
  results**): it fetches an explicit 52-week window through the same DTS
  query instead of rendering a hidden calendar.
- **Calendar/List toggle buttons** (`Show Calendar` / `Show List`) destroy
  and re-create the calendar instance when both layouts are enabled.

## Editor

The calendar editor (language, Google feed setup, MiniColors swatches)
is unchanged by the runtime swap.

## Verifying a site

1. Publish a page with a Calendar 2.0 widget.
2. Open browser devtools → Network, filter `fullcalendar`: expect **zero**
   `2.9.1`-era requests and no script errors.
3. The month grid shows the current month with event titles; switching the
   widget **Language** to e.g. French localizes the month title.
4. With toggle buttons enabled, **List View** shows the upcoming-events
   list (or `Loading events…` then `No upcoming events.` on an empty
   calendar); **Calendar View** switches back instantly.
5. With a Google feed configured, the feed loads over
   `https://www.googleapis.com`; under a restrictive CSP confirm
   `connect-src https://www.googleapis.com` is allowed.
6. Print-preview the page: toolbar buttons, toggle buttons, and the
   source-filter bar hide while the grid and list print black-on-white.
