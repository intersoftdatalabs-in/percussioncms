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
