# Bug Fix #795 – Calendar 2.0 Widget: "hideCalendarSource is not defined" JS Error on Source Buttons

## Problem

Clicking the "Percussion Calendar" (or Google Calendar source) buttons in the Calendar 2.0 widget on a published page throws:

```
Uncaught ReferenceError: hideCalendarSource is not defined
    at HTMLDivElement.onclick
```

The buttons are the ones that toggle visibility of specific calendar event sources (Google calendars or the Percussion/local events).

Repro:
- Configure Google Calendar or enable Perc events button in Calendar 2.0 widget
- Publish page
- Click the source button (e.g. "Percussion Calendar")
- Error in console; button does not toggle the sources.

Affected: 8.1.7 (and prior versions with the widget)

## Root Cause

In [percCalendarTwo.xml](/home/nate/projects/java8/percussioncms/system/Packages/perc.widget.calendar/sys__UserDependency--rxconfig/Widgets/percCalendarTwo.xml) (the Velocity template that generates the widget's client-side HTML/JS):

```html
<script>
window.addEventListener('DOMContentLoaded', function() {
    function hideCalendarSource(calendarClass) { ... }  // local to this callback, NOT global
});
</script>

<div ... onClick="hideCalendarSource('perc-...')">...</div>
```

The inline `onClick` expects a global `hideCalendarSource`, but the definition was intentionally (or mistakenly) placed inside the DOMContentLoaded IIFE-style callback, so it was never attached to `window` (or global).

The main widget initialization script (also in a DOMContentLoaded) was after in the output but did not define or expose the function either.

## Fix

- Deleted the early `<script>` block that (incorrectly) scoped `hideCalendarSource`.
- Removed the `onClick="..."` attributes from the generated `<div class="perc-calendar-button ...">` elements (for both Google sources via foreach and the Percussion events button).
- Added proper jQuery event binding inside the main widget's `$(function(){ ... })` block (which already runs under DOMContentLoaded):

```js
$('#perc-button-container-${calendarContentId}-${widgetId} .perc-calendar-button').on('click', function() {
    var $btn = $(this);
    var btnId = $btn.attr('id');
    var calendarClass = btnId ? btnId.replace(/-button$/, '') : null;
    if (calendarClass) {
        $('#' + calendarClass + '-button').toggleClass("perc-disabled-calendar-button");
        $('.' + calendarClass).toggleClass("perc-hide-calendar");
    }
});
```

This:
- Uses the already-unique widget-scoped container ID for safe selection (supports multiple calendar widgets on one page).
- Attaches after DOM ready, using jQuery which is guaranteed present.
- Preserves exact original toggle behavior.
- Eliminates reliance on global namespace and inline event attributes (better practice).

## Verification Performed

- Manual review of before/after generated structure (matching the example in the issue report).
- XML well-formed validation (ElementTree).
- `./mvn-env.sh spotless:apply` + `spotless:check` (clean for system module).
- `./mvn-env.sh compile -pl system -am -DskipTests` (success, ~3 min).
- No other copies of the template logic found in tree (only this source file).
- Checked old `percCalendar.xml` widget does not have analogous code.
- No Java changes; purely presentation/widget assembly template.
- Followed AGENTS.md: pulled base, branched with issue# in name (`bugfix/795-calendar-widget-hidecalendarsource`), did not commit to development-8.1.x, no remote push.

## Files Changed

- `system/Packages/perc.widget.calendar/sys__UserDependency--rxconfig/Widgets/percCalendarTwo.xml`

## References

- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/795
- Branch: `bugfix/795-calendar-widget-hidecalendarsource`
- Commit: `9e5d5b01c`

