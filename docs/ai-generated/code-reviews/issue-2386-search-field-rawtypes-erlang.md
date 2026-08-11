# Erlang review — issue #2386 search field/executable rawtypes

**Verdict:** PASS (with residual for remaining search package)

## Scope
Typed residual `-Xlint` cleanup in `com.percussion.search` field/filter/executable core after security residual children (#2458–#2461) closed.

## Findings
- No behavioral bugs found in typed filters/operators/folder cleanup.
- `setContentTypeIdList` kept as `Collection<?>` so DCE callers with `String` ids remain source-compatible.
- `getSearchResults` / `executeSearch` params typed `Map<String,String>`; both in-module subclasses updated.
- Portable paths: N/A (no file I/O changes).
- Product docs: N/A (tech-debt typing only).

## Residual
- `PSGenerateSearchQueryExit`, `PSGenerateSearchResultsExit`, `PSSearchIndexEventQueue`, lucene helpers, remaining search rawtypes.

## Tests
- PSSearchFieldFilterTypedTest (5)
- PSSearchFieldOperatorsTypedTest (6)
- PSCleanFolderSearchResultsExitTypedTest (2)
- system clean install: Tests run 1701, Failures 0
