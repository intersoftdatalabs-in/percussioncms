# Erlang review: issue #2035 extensions-sfp javac batch 1

## Summary

Replace class-level `@SuppressWarnings({"rawtypes","unchecked"})` on a PR-sized
slice of `modules/extensions-sfp` with real generics / `@Deprecated` annotations.
Calendar + content-list helpers + dep-ann fixes. Residual site-folder hierarchy
and large exits keep suppressions for follow-on slices.

## Scope

- Branch: `fix/issue-2035-extensions-sfp-javac-warnings-batch1`
- Base: `origin/main`
- Module: `modules/extensions-sfp` only
- Cross-platform path review: N/A (no path/file I/O changes)

## Recommendation

**approve**

## Gate

May commit/push: **yes**

## Issues

None (bug / missing tests / non-portable paths).

### Notes

- Narrow residual suppressions retained where intentional:
  - `this-escape` on `PSHolidays` / `PSRecurringEvent` constructors (call
    overridable setters used by production XML ctor path).
  - Method-level `unchecked` on `PSCalendarMonthModel.getEvents` (untyped
    commons-collections `MultiMap`).
- `PSRecurrenceIterator` historically starts at recurrence index 1 (skips index
  0); tests document that behavior without changing it.
- Call sites still on residual files pass raw `Set` into
  `PSContentListItem(…, Set<String>, …)`; those call sites remain under class
  suppressions until residual slices.

## Verification

- `cd modules/extensions-sfp && ../../mvnw.cmd clean install` → **BUILD SUCCESS**
- Tests run: **13**, Failures: **0**, Errors: **0**, Skipped: **4** (pre-existing
  disabled `PSCalendarMonthModelTest`)
- Zero javac `warning:` lines under project `-Xlint` (`-Xlint:-deprecation`)
- ~50 rawtypes/unchecked/dep-ann diagnostics fixed; residual remains under
  suppressions on site-folder / large exit classes

> Co-Authored by Grok Build using grok-4.5 with agent main.

