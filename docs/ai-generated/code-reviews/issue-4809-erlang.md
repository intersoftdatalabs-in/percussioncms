<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue 4809 (publish-log day window)

## Scope

- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Status: mkd-code-review 0.1.18, pack percussion, `--gate advisory`, `--git-base origin/main`
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/4809
- Parent: https://github.com/intersoftdatalabs-in/percussioncms/issues/4531
- Branch: `fix/issue-4809-logs-day-window` (`aa26f751f7` vs `origin/main` `19279a8254`; working tree clean)
- Files reviewed (`git diff origin/main...HEAD`, 8 files, +496 / −69):
  - `system/services/src/com/percussion/services/publisher/impl/PSPubStatusLogQuery.java` (new)
  - `system/src/test/java/com/percussion/services/publisher/impl/PSPubStatusLogQueryTest.java` (new)
  - `system/services/src/com/percussion/services/publisher/impl/PSPublisherService.java`
  - `system/services/src/com/percussion/services/publisher/IPSPublisherService.java`
  - `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishStatusService.java`
  - `projects/sitemanage/src/test/java/com/percussion/sitemanage/service/impl/PSSitePublishLogWindowTest.java` (new)
  - `modules/perc-qa-automation/frontend/tests/publishing/logsDayWindow.spec.js` (new)
  - `product-docs/8.2/admin/publishing.md`
- `--git-base origin/main` analyzed all 8 committed files; no untracked slice
- Memory: `~/.local/share/mkd/agents/erlang/PATTERNS.md`

## This-diff behavior

Issue 4809: Publishing Logs for N days must be a server query window, not a full historical load trimmed in `buildLogs`. Failures-only must still apply. Playwright + `product-docs/8.2` are required companions.

`PSPubStatusLogQuery` owns the window: `fromDate(days, now)` is null only for `days == -1`, otherwise `now` minus `days` via `Calendar.DAY_OF_YEAR`; `appendWindow` adds `alias.startDate >= :fromDate` and, when `failuresOnly`, `alias.endingStatus in (:endingStates)` with ABORTED / CANCELED_BY_USER / COMPLETED_W_FAILURE / RESTARTNEEDED ordinals; `limits` maps skip>0 to `firstResult` and `maxCount != -1` to `maxResults`.

`PSPublisherService` 4-arg `*WithFilters` overloads delegate to 6-arg (`skip=0`, `failuresOnly=false`). Site+server and edition-list HQL both call `appendWindow` + bind + `applyLogLimits`. Site+server already used `>=`; the edition-list path moved from exclusive `startDate > :fromDate` to the same inclusive bound. `PSSitePublishStatusService.buildLogs` forwards `days`, `maxCount`, `skip`, and `failuresOnly = !showAll` and maps every returned row; it no longer skips or `isFailure`-filters in the JVM. `getLogs` still passes `!request.isShowOnlyFailures()`. Current-job lookback (`FAILED_JOB_LOOKBACK_DAYS` / `FAILED_JOB_SCAN_MAX`) stays on the 4-arg overloads.

`PSPubStatus.endingStatus` is the HQL field (`ENDING_STATUS`). Failure ordinals match `isFailure` for every `EndingState`. UI day options remain 3 / 5 / 10.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishStatusService.java:365 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 365)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

## Erlang interpretation

Gate counts in-diff bugs only. Residual LLM added no extras (no `llm.error`).

Preexisting `paths.hardcoded_sep` is dummy `getJobDetails` fixture `setFileLocation("/home/section/index.html")` at line 365. Outside this hunk; not a 4809 path defect.

Independent review of the query slice:

Query construction is covered behaviorally:

- `PSPubStatusLogQueryTest.dayWindowIsInclusiveStartDatePredicate` — HQL `startDate >= :fromDate`, `fromDate(1, now)` equals minus one day, `fromDate(-1, now)` is null
- `unlimitedDaysOmitsDatePredicate` — `days == -1` leaves the WHERE clause unchanged
- `failuresOnlyAddsEndingStatesMatchingLogFailures` — date + `endingStatus in (:endingStates)`, four failure ordinals present, COMPLETED/STARTED absent
- `limitsMapSkipAndMaxOntoTheQuery` — skip 4 / max 20 bind; skip 0 and max −1 omit both
- `PSSitePublishLogWindowTest` — site+server / site-only / all-sites call the 6-arg methods with days, max, skip, and `failuresOnly = !showAll`; COMPLETED rows from that query are kept; 4-arg overloads are not used; `isFailure` matches `failureEndingOrdinals` for every enum value
- Playwright `logsDayWindow.spec.js` — Logs apply posts `days: 3` and `showOnlyFailures: true` to `POST …/sitemanage/pubstatus/logs`

Change-class companions are present: query helper + publisher overloads + sitemanage pass-through + unit tests + Playwright surface + `product-docs/8.2/admin/publishing.md`. `IPSPublisherService` new methods have one implementor (`PSPublisherService`). No new filesystem path joins. New sources use Intersoft 2026 headers.

### Issue 2 -- Severity: suggestion

- File: system/services/src/com/percussion/services/publisher/impl/PSPublisherService.java:3704
- Description: Edition-list HQL previously used exclusive `p.startDate > :fromDate`. Shared `appendWindow` now uses inclusive `>=`, matching the site+server query and the helper test. Jobs whose `startDate` equals the bound instant are now included. That is the documented window.
- Suggestion: None required for this slice. Keep inclusive `>=` as the single contract.
- Status: open

No in-diff bug. Query logic has behavioral tests. No new non-portable path.

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

> Co-Authored by Grok Build 1.0.41 using grok-4.6 with agent Erlang Shen.
