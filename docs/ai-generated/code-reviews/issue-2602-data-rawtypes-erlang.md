# Erlang review — issue #2602 residual data rawtypes (slice 4e)

**Reviewer persona:** Erlang (independent of implementer)  
**Date:** 2026-08-10  
**Branch:** fix/issue-2602-data-rawtypes-4e  
**Scope:** `com.percussion.data` + residual `data.jdbc` rawtypes/unchecked after #2398 / PR #2603

## Verdict: **APPROVE** (with residual noted for parent #2022)

### Change class
Compile-time rawtypes/unchecked cleanup in data pipeline packages — no SQL/result-set behavioral intent change. Companion: unit tests for typed rule-list / statement-block helpers + existing table-change tests + module clean install.

### Findings

| Severity | Finding | Disposition |
| --- | --- | --- |
| Bug | None introduced in typed maps/lists | OK |
| Behavioral tests | Empty rule list matches; statement block/group empty extractors; PSTableChangeDataTest retained | Present |
| Portability | No path/file I/O changes | N/A |
| Residual | Package not zeroed (user-context extractor, query cacher ConcurrentHashMap/SortedSet, more SQL builders, etc.) | File residual on #2022 |

### Notes
- `PSExtensionRunner.runSearchResultProcessor` intentionally remains raw `List` at the API boundary: callers pass `List<IPSSearchResultRow>` while `IPSSearchResultsProcessor.processRows` declares `List<Object>` (list invariance). Extractors collection and related helpers are fully typed.
- `PSJoinedRowDataBuffer` / joiner column maps typed as `HashMap<String,Integer>` matching `PSResultSet` constructor.
- `PSTableChangeData` fully parameterized (listeners, column maps, events).
- Statement blocks/groups/PSStatement/Oracle LOB update paths match `IPSStatementBlock` typed returns.

### Tests run
- Focused: PSRuleListEvaluatorTypedTest, PSStatementBlockTypedTest, PSTableChangeDataTest (+ prior typed batch peers)
- Module: `cd system && ../mvnw.cmd clean install` (see PR evidence)

> Co-Authored by Grok Build using grok-4.5 with agent main.
