<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue 4807 (uncommitted)

## Scope

- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Status: mkd-code-review 0.1.18, pack percussion, `--gate advisory`, `--diff` of uncommitted 4807 files vs HEAD
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/4807
- Branch: `fix/issue-4807-status-failed-jobs` (HEAD `e4cf724992` == `origin/main`; all work is uncommitted)
- Files reviewed:
  - `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishStatusService.java` (modified)
  - `projects/sitemanage/src/test/java/com/percussion/sitemanage/service/impl/PSSitePublishStatusServiceJobDetailTest.java` (modified)
  - `product-docs/8.2/admin/publishing.md` (modified)
  - `modules/perc-qa-automation/frontend/tests/publishing/statusFailedJobs.spec.js` (untracked)
- `--git-base origin/main` is empty here (no commits vs main) and would omit the untracked Playwright spec; analysis used a unified `git diff HEAD` plus `--no-index` for the spec, with `a/` `b/` prefixes (no `c/w` or `1/2` rewrite needed)
- Memory: `~/.agents/skills/erlang/PATTERNS.md`
- Hard-block this turn: in-diff bugs, missing behavioral tests, or non-portable path/file I/O in **this** diff only
- Out of scope: preexisting dummy path `/home/section/index.html` in `getJobDetails`

## This-diff behavior

`GET …/sitemanage/pubstatus/current` listed a failed job only while the publisher still returned its id. Those ids are reaped about an hour after the job ends, so Status went empty for **Completed with failures** / **Failed** before operators could open detail.

`buildCurrentJobs` now keeps live active/failed jobs, then merges persisted `IPSPubStatus` from the last day (`FAILED_JOB_LOOKBACK_DAYS = 1`, `FAILED_JOB_SCAN_MAX = -1` voids the SQL row cap — same `-1` convention as `PSPublisherService.findPubStatusByEditionListWithFilters`). Ending states that stay listed: `ABORTED`, `COMPLETED_W_FAILURE`, `RESTARTNEEDED`. Clean **Completed** and user-cancelled jobs stay off Status (Logs still has them). Site-scoped current uses `findPubStatusBySiteWithFilters`; the all-sites path uses `findAllPubStatusWithFilters`. Dedup is by job id (`LinkedHashMap`).

`PSSitePublishStatusServiceJobDetailTest` exercises: persisted failure after empty active ids; aborted + restart-needed; running + dropped failure together; live aborted not duplicated by the persisted row; site-scoped scan does not call the all-sites finder; cancelled/completed rows in the scan are dropped.

Playwright `statusFailedJobs.spec.js` (peer of `statusJobDetail.spec.js`) fulfills current-jobs JSON with a completed-with-failures row and a running row, asserts no Stop on 4807, Stop on 12, and detail status/error/edition. Product-docs Status filter + job-detail paragraphs match the one-day persisted window and live publisher message.

Change-class companions: sitemanage current-jobs merge, behavioral unit tests, PublishingShell Playwright, `product-docs/8.2/admin/publishing.md`. No WebUI source change (shell already renders whatever current returns). Cross-platform path I/O: none in the new merge; URL `/` in Playwright is correct.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 4 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishStatusService.java:366 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 366)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishStatusService.java:388 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `buildCurrentJobs` cognitive=17 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang interpretation

Gate counts in-diff bugs only. Residual LLM added no extras (no `llm.error`).

Issue 1 is the dummy `setFileLocation("/home/section/index.html")` in `getJobDetails` when `dummyData` is true. It sits on a context line next to the new lookback constants. **Out of scope** for this review (preexisting dummy fixture, not filesystem join on a product path). Do not hard-block on it.

In-diff machine hit is cognitive 17 on `buildCurrentJobs` (pack max 15). Maintainability only. Cyclomatic 11 is under the pack cap.

Independent review of the merge:

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishStatusService.java:400
- Description: Every Status poll now loads one day of all pub-status rows (`maxCount = -1`) and filters failures in memory. That matches the publisher API’s documented “void max” value and is covered by the unit tests, but a busy site with Status auto-refresh will pay the full scan on each `GET /current`.
- Suggestion: If operators see lag, add a modest SQL cap or a failure-only backend filter rather than widening lookback.
- Status: open

### Issue 4 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishStatusService.java:419
- Description: Persisted `buildJob(status)` still goes through `getSiteName` / `loadEdition`. One missing edition in the last-day window throws `PSNotFoundException` and 500s the whole current list. Logs already used that `buildJob`; current jobs did not until this merge.
- Suggestion: Catch not-found on the persisted loop, log, and skip that row so Status still lists the rest.
- Status: open

Behavioral coverage for the new merge is present (empty active ids, mixed live+persisted, dedup, site vs all, cancelled/completed excluded). Playwright covers the Status chrome for a finished failure without Stop. Product-docs describe the one-day window and live-only publisher message.

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

Gate: PASS

May commit/push: yes

> Co-Authored by Grok Build using grok-4.6 with agent Erlang Shen.
