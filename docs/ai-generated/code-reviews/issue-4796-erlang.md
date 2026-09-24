<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue 4796 (uncommitted)

## Scope

- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Status: mkd-code-review 0.1.18, pack percussion, `--gate advisory`, `--diff` of uncommitted 4796 files
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/4796
- Branch: `fix/issue-4796-explorer-rename-navon-tx` (HEAD == `origin/main` `33ac3f61b7`; all work is uncommitted)
- Files reviewed:
  - `system/src/main/java/com/percussion/fastforward/managednav/PSManagedNavService.java` (modified)
  - `system/src/main/java/com/percussion/services/tx/PSCallerTransactionSuspension.java` (untracked)
  - `system/src/test/java/com/percussion/services/tx/PSCallerTransactionSuspensionTest.java` (untracked)
- `--git-base origin/main` is empty here (no commits vs main) and would omit the two untracked files; analysis used a unified diff with `a/` `b/` prefixes so the machine pass saw all three files
- Memory: `~/.agents/skills/erlang/PATTERNS.md` and repo `erlang-review/patterns.md`

## This-diff behavior

Explorer site rename still returns HTTP 500 after the publish-server CHAR(1) flag work. `setNavonProperties` wraps `UnexpectedRollbackException` from `contentWs.loadItems`. `JpaTransactionManager` `REQUIRES_NEW` suspends only the resources it bound; site rename also binds a JDBC connection holder on the same datasource, so the nested transaction enlists that holder and sees `isGlobalRollbackOnly`.

This slice extracts `PSCallerTransactionSuspension.executeRequiresNew`: unbind every `TransactionSynchronizationManager` resource, drop caller synchronizations, run `TransactionTemplate` `PROPAGATION_REQUIRES_NEW`, then in `finally` drop leftover inner-only resources and restore caller bindings. `PSManagedNavService.runWithoutJoiningCallerTx` delegates to that helper. Null manager still runs `work` on the caller thread.

`PSCallerTransactionSuspensionTest` binds a caller resource plus synchronizations, asserts they are invisible at `getTransaction` / inside work, asserts `REQUIRES_NEW`, then asserts restore. Null manager and null-work rejection are covered.

Change-class companions: static helper (not a scanned Spring bean), helper unit test, nav-service call site. Existing `PSManagedNavServiceSetNavonPropertiesTest` still covers save / retry / sample-workflow prepare. Playwright `modules/perc-qa-automation/frontend/tests/explorer-site-rename.spec.js` already exists for the Explorer surface; this diff does not change UI. Product-docs N/A (internal tx isolation restoring the intended 200). Cross-platform path I/O: none in the new helper.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 3 analyzed
- In-diff: 1 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: system/src/main/java/com/percussion/fastforward/managednav/PSManagedNavService.java:461 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 461)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: system/src/main/java/com/percussion/fastforward/managednav/PSManagedNavService.java:1329 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1329)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: suggestion

- File: system/src/main/java/com/percussion/services/tx/PSCallerTransactionSuspension.java:52 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `executeRequiresNew` cognitive=16 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang interpretation

Gate counts in-diff bugs only. Residual LLM added no extras (no `llm.error`).

Preexisting `paths.hardcoded_sep` on `PSManagedNavService`: line 461 concatenates a CMS item path (`findItemPaths` + `"/" + name`); line 1329 is a log string around a slot name. Both sit outside this hunk.

In-diff machine hit is cognitive 16 on `executeRequiresNew` (pack max 15). Maintainability only.

Independent review of the helper:

### Issue 4 -- Severity: suggestion

- File: system/src/test/java/com/percussion/services/tx/PSCallerTransactionSuspensionTest.java:49
- Description: Happy-path unbind/restore is behavioral. The `finally` restore after `work` throws is untested, as is cleanup of an inner leftover resource on a different key than the caller holder.
- Suggestion: Add a test where `work` throws: exception propagates, caller resource and synchronizations are restored. Optionally bind a second key inside work and assert it is gone after return.
- Status: open

### Issue 5 -- Severity: suggestion

- File: system/src/main/java/com/percussion/services/tx/PSCallerTransactionSuspension.java:73
- Description: Inner `TransactionTemplate` cleanup calls `TransactionSynchronizationManager.clear()`, which drops caller `actualTransactionActive`, transaction name, isolation, and read-only flags. The helper restores resources and synchronizations only. Percussion application code does not read those flags; the outer interceptor still holds `DefaultTransactionStatus` and the rebound `EntityManagerHolder`.
- Suggestion: Snapshot and restore the four TSM characteristics next to the resource map, matching Spring's `SuspendedResourcesHolder`, if later request code after navon save depends on them.
- Status: open

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

> Co-Authored by Grok Build 1.0.40 using grok-4.6 with agent Erlang Shen.
