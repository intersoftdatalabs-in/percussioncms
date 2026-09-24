<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #4823

Independent pre-merge review. Author is not this reviewer.

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

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

Gate counts in-diff bugs only. Preexisting `paths.hardcoded_sep` on `PSSitePublishStatusService.getJobDetails` dummy `/home/section/index.html` is outside this hunk (not an in-diff blocker).

Independent read of `5926193d4a` vs `origin/main`:

- `PSPubStatusLogQuery.appendWindow` adds `startDate >= :fromDate` when days != -1 and `endingStatus in (:endingStates)` when failures-only. Entity field is `endingStatus`. Ordinals match `PSSitePublishStatusService.isFailure`.
- `buildLogs(..., showAll)` sets `failuresOnly = !showAll` and calls the new publisher overloads (site+server, site, all sites) with skip and max. Old 4-arg publisher methods delegate with skip 0 and failuresOnly false.
- `PSPubStatusLogQueryTest` and `PSSitePublishLogWindowTest` exercise the HQL fragments, date bound, limits, and which overload is called. Playwright `logsDayWindow.spec.js` posts the day window. `product-docs/8.2/admin/publishing.md` describes the server window.

Suggestion (non-blocking): the Playwright route mock reads `.days` on the raw envelope before unwrap, so the fulfilled `startDate` can be `window-undefined`. Later assertions unwrap `SitePublishLogRequest` and check `days` and `showOnlyFailures`.

No in-diff bug. Recommendation: approve. May commit/push: yes.
