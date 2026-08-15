# Erlang review — PSDateRange.getDaysInRange calendar-day count

**Branch:** `main` (uncommitted vs `HEAD` `23533bc0862e96e4d84ef203f93cce67155c452b`)  
**Scope:** local working tree vs `HEAD` (two files)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests required for changed date math (met); Calendar leftover fields / Interval whole-day undercount (this is the fix); no path I/O.

## Summary

`PSDateRange.getDaysInRange()` stopped using `Days.daysIn(new Interval(start, end.plusDays(1)))`, which counts whole chronology days and undercounts by one whenever start millis-of-day is greater than end millis-of-day. The new implementation is `Days.daysBetween(start.toLocalDate(), end.toLocalDate()).getDays() + 1` — inclusive calendar days, time ignored — which matches the class contract and `getGranularityBreakdown()` day walks.

`PSDateRangeTest.createDate` now `clear()`s the `Calendar` before `set(year, month-1, day)`, so leftover `MILLISECOND` from `getInstance()` cannot re-flake the existing midnight assertions. Two new cases lock the regression (Feb 28 23:59:59.999 → Mar 2 00:00:00.000, and 800ms vs 100ms).

Call site `PSTrafficService` uses `getDaysInRange()` as a DAY duration for the previous window. Date-only `MM/dd/yyyy` parse paths are unchanged; time-bearing ranges that previously undercounted now get the correct inclusive length. That is the intended fix, not a silent contract break.

Implementer evidence: `cd modules/utils && ../../mvnw.cmd clean install` BUILD SUCCESS; Tests run 387, Failures 0, Errors 0, Skipped 9; `PSDateRangeTest` 6/6.

## Cross-platform path checklist

Not applicable — no filesystem path construction, temp files, or path assertions.

## Change-class closure

Internal utility bugfix + existing unit test class. No Spring scan, REST adaptor, WebUI, Playwright, or `product-docs/` companion required. Module `AGENTS.md` absent under `modules/utils/`.

## Issues

None.

## Suggestion (non-blocking)

Optional extra assertion: same calendar day with different clock times (e.g. 00:00:00.800 → 23:59:59.100) expecting `1`. The two added cases already prove the undercount class; this would only document the same-day bound.

## Pattern candidate (do not commit without human rule review)

`Calendar.set(y, m, d, h, min, s)` does not zero `MILLISECOND`. Pairing leftover millis with `Days.daysIn(Interval)` / `daysBetween(DateTime, DateTime)` undercounts when start millis-of-day exceeds end. Prefer `LocalDate` day counts, or `cal.clear()` / explicit `MILLISECOND = 0`.
