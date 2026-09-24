<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->


## Summary

Machine analysis found **5** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 4 analyzed
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

### Issue 4 -- Severity: suggestion

- File: system/src/test/java/com/percussion/services/tx/PSCallerTransactionSuspensionTest.java:49
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: Happy-path unbind/restore is behavioral. The `finally` restore after `work` throws is untested, as is cleanup of an inner leftover resource on a different key than the caller holder.
- Suggestion: Add a test where `work` throws: exception propagates, caller resource and synchronizations are restored. Optionally bind a second key inside work and assert it is gone after return.
- Status: open

### Issue 5 -- Severity: suggestion

- File: system/src/main/java/com/percussion/services/tx/PSCallerTransactionSuspension.java:73
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: Inner `TransactionTemplate` cleanup calls `TransactionSynchronizationManager.clear()`, which drops caller `actualTransactionActive`, transaction name, isolation, and read-only flags. The helper restores resources and synchronizations only. Percussion application code does not read those flags; the outer interceptor still holds `DefaultTransactionStatus` and the rebound `EntityManagerHolder`.
- Suggestion: Snapshot and restore the four TSM characteristics next to the resource map, matching Spring's `SuspendedResourcesHolder`, if later request code after navon save depends on them.
- Status: open

