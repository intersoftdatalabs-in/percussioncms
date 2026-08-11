# Erlang review — issue #2873 search exits / index queue rawtypes

**Reviewer persona:** Erlang (strict, independent of implementer)
**Scope:** `com.percussion.search` residual after #2386 / PR #2874
**Files:**
- `PSSearchIndexEventQueue.java` — fragment / bin-field maps typed
- `PSGenerateSearchQueryExit.java` — keyword field maps, `PSValueListIterator`, helpers
- `PSGenerateSearchResultsExit.java` — parseParameters, SearchField, display/row lists
- unit tests: `PSGenerateSearchQueryExitTypedTest`, `PSGenerateSearchResultsExitTypedTest`

## Checklist

| Gate | Result |
|------|--------|
| Bugs in new logic | PASS — typing only; cartesian iterator semantics preserved |
| Behavioral unit tests | PASS — 8 tests for value-list cross product + parseParameters / SearchField |
| Cross-platform paths | N/A — no path/file I/O changes |
| Public API breakage | PASS — no abstract/public signature changes on `PSSearchIndexer`/`PSSearchQuery` |
| Companion closure | PASS for tech-debt typing batch; Lucene helpers deferred to residual |

## Notes

- `PSValueListIterator` and `SearchField` / `parseParameters` package-visible for tests (same package).
- `itemChanges` uses `Map<PSKey, Set<String>>` (`PSLocator` / `PSItemChildLocator` keys).
- Fragments typed as `Map<String, Object>` to match `IPSFieldValueModifier.modifyFields`.
- Product docs: N/A (no operator-facing behavior change).

## Residual

- Lucene `PSSearchIndexerImpl` / `PSSearchQueryImpl` and related abstract `Map`/`List` surfaces on `PSSearchIndexer` / `PSSearchQuery`.
