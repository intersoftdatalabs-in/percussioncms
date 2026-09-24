<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue 4809 (publish-log day window)

## Scope

- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Status: mkd-code-review 0.1.18, pack percussion, `--gate advisory`, `--machine-only`, `--git-base origin/main`
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/4809
- Parent: https://github.com/intersoftdatalabs-in/percussioncms/issues/4531
- Branch: `fix/issue-4809-logs-day-window` (`e958cdbeed` vs `origin/main` `19279a8254`; working tree clean)
- This re-review: commit `e958cdbeed` only (`test(publish): assert logs day window on the DTO envelope`). Query slice `aa26f751f7` was already approved.
- Files in `e958cdbeed`:
  - `modules/perc-qa-automation/frontend/tests/publishing/logsDayWindow.spec.js` (+2 / −1)
  - `docs/ai-generated/code-reviews/issue-4809-erlang.md` (prior report)
- `--git-base origin/main` analyzed 9 committed files (query slice + this commit); no untracked slice
- Memory: `~/.local/share/mkd/agents/erlang/PATTERNS.md`
- Hard-block this turn: new in-diff bugs, missing behavioral tests, or non-portable path/file I/O in **this** commit only

## This-diff behavior

Playwright `logsDayWindow.spec.js` posted `POST …/sitemanage/pubstatus/logs` with the JAXB/Minuet envelope `{ SitePublishLogRequest: { days, showOnlyFailures, … } }` (`buildLogListRequestBody` / `@XmlRootElement(name = "SitePublishLogRequest")`). The spec now unwraps `envelope.SitePublishLogRequest || envelope` before asserting `days === 3` and `showOnlyFailures === true`.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
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

Gate counts in-diff bugs only. Residual LLM skipped (`--machine-only`).

Preexisting `paths.hardcoded_sep` is dummy `getJobDetails` fixture `setFileLocation("/home/section/index.html")` at line 365. Outside this hunk; not a 4809 path defect.

Independent review of `e958cdbeed`:

Production `fetchPublishingLogs` posts `buildLogListRequestBody(request)` → `{ SitePublishLogRequest: { ...request } }`. Vitest `logRequestBodies.test.ts` already pins that root. Reading `.days` on the raw `postDataJSON()` envelope is undefined; the unwrap is the contract the surface spec must check. Assertions `posted.days === 3` and `posted.showOnlyFailures === true` are behavioral.

### Issue 2 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/publishing/logsDayWindow.spec.js:67
- Description: The route mock still takes `days` from the raw envelope (`bodies[last].days`) for `startDate: "window-" + days`. With the live wrapper that value is undefined (`"window-undefined"`). The later unwrap assertions cover the DTO fields; the mock string is unused.
- Suggestion: Unwrap before the mock read, or drop `days` from the fulfill payload.
- Status: open

`|| envelope` still accepts a flat body. That is dual-shape, not a missing test. No new filesystem path joins.

Query-slice companions from `aa26f751f7` are unchanged: `PSPubStatusLogQuery` + publisher overloads + sitemanage pass-through + unit tests + this Playwright surface + `product-docs/8.2/admin/publishing.md`.

No in-diff bug. Playwright now asserts the DTO envelope. No new non-portable path.

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

> Co-Authored by Grok Build 1.0.41 using grok-4.6 with agent Erlang Shen.
