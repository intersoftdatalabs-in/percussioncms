<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5100

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5100
- Base: origin/main
- Head: 3511651dfc1c6e826ec197133149d6483c6e2e8e
- Files analyzed: 12

Independent of the author. Machine gate is advisory and did not see this bug. Erlang gate: **request-changes**. Do not merge.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingRuntimeSupport.java:91 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `listRuntimeEditions` cognitive=23 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang findings (strict gate)

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1393 (in-diff)
- Also: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingRuntimeSupport.java:145 (in-diff)
- Description: Delete treats any remembered publish job as "in use". `rejectEditionInUse` throws HTTP 409 when `runningJobId` is `> 0`. `runningJobId` returns `IPSRxPublisherService.getEditionJobId` with no terminal-state check. `PSRxPublisherService.getEditionJobId` returns the max job id still in `m_jobs` for that edition, including finished jobs. Finished jobs stay for `REAP_TIME` (one hour after `endTime`; `IPSRxPublisherServiceInternal`). `startPublishingJob` only refuses an edition when `!job.isFinished()`. After a publish completes, delete of that idle edition returns 409 "Edition is in use" for up to an hour. The runtime list uses the same id to *display* status, including Completed; delete must not reuse `jobId > 0` as "still running".
- Tests: `PSPublishingRuntimeSupportTest.runningJobId_*` only stubs `getEditionJobId` to 55 or 0. Nothing asserts that a finished job still in the map allows delete.
- Suggestion: 409 only when the job exists and `!isFinished()` (same predicate as start). Add a behavioral test for a finished job id that must not 409. Do not change `getEditionJobId` itself; the status list still needs the latest job, completed or not.

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: no
