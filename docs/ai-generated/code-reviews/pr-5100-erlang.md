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
- Head: 1428cb3d4ebf0e75ab243b035a9d9af30e9ce514
- Files analyzed: 13

Independent re-review after `1428cb3d` (finished job no longer blocks edition delete). I did not author the change. `runningJobId` returns 0 when `IPSPublisherJobStatus.State.isTerminal()`, the same predicate as `PSPublishingJob.isFinished()`. `getEditionJobId` is unchanged, so the runtime list can still show a completed job. `getPublishingJobStatus` throws `IllegalStateException` for an unknown id, and that path is idle. Any other status-lookup failure stays in use. Tests cover a completed job, a cancelled job, an active `WORKING` job, an unknown id, a failed lookup, and delete through `PSPublishingRuntimeSupport` (completed deletes, working is HTTP 409).

Nit, non-blocking: the `runningJobId` comment says "`null` is idle". A null edition guid is idle. A null status or null state still returns the job id (treated as in use). The code is the safer behavior.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 13 analyzed
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

No blocking bugs. The first-pass bug (a finished job still in `m_jobs` returned HTTP 409 for up to `REAP_TIME`) is fixed on this head.

Nit: `PSPublishingRuntimeSupport.runningJobId` javadoc "`null` is idle" does not match a null job status, which stays in use.

Preexisting suggestion: `listRuntimeEditions` cognitive complexity. Not introduced by this diff. Does not block.

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
