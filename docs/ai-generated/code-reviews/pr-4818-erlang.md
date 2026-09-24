<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 4818

Independent review of `fix/issue-4807-status-failed-jobs` (`985332f17e`) vs `origin/main`.
Preexisting `paths.hardcoded_sep` on the dummy `/home/section/index.html` in `getJobDetails` is not an in-diff blocker. Cognitive complexity on `buildCurrentJobs` is a suggestion, not a bug. Behavioral tests cover persisted failure, aborted, restart-needed, cancelled/completed exclusion, dedup, and site scope. Playwright companion and product-docs are present.

Gate: PASS. Recommendation: approve. May commit/push: yes.

## Pre-push local code review

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 5 analyzed
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
