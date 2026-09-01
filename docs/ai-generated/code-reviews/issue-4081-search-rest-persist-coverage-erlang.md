# Erlang review — issue #4081 REST UI-06 search persist coverage

**Branch:** `fix/issue-4081-search-rest-persist-coverage`  
**Scope:** uncommitted vs `origin/main` (rest tests only)  
**Date:** 2026-08-31  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for write contract; exact `ISearchAdaptor` Spring stub (not a supertype); no second production persist implementation (PR #4088 owns that)

## Summary

#4084 / PR #4088 already implements H2 durable `POST /services/searches`. This slice adds rest-module coverage that was missing on that PR: Mockito mapping plus a persist-contract test against an **exact** `ISearchAdaptor` (create then GET/list, duplicate 409, blank/whitespace/spaces 400, DELETE 204 then GET 404). `TestSearchAdaptor` remains the Spring `MainTest` stub of type `ISearchAdaptor`.

No production persist code in this diff (do not land a second `ensureSearchRowPersisted`).

## Cross-platform path review

N/A — no filesystem I/O. `InMemorySearchAdaptor.requireValidName` rejects `/` and `\` as **name characters** (same product rule as `SearchAdaptor.isSafeSearchKey`), not path joins.

## Issues

None (hard-gate).

Behavioral tests:

- `SearchResourcePersistContractTest` (4) — exact `ISearchAdaptor` in-memory catalog
- `SearchResourceTest` — spaces → 400; Mockito durable create/list/delete; missing adaptor on DELETE → 503

Standalone `cd rest && ../mvnw.cmd clean install` BUILD SUCCESS; Tests run: 948, Failures: 0 (`SearchResourcePersistContractTest` 4; `SearchResourceTest` 30).

## Suggestion (non-blocking)

Mockito `createThenGetAndListAreDurable` overlaps the persist-contract class. Keep both: one proves HTTP mapping with a mock, the other proves the same verbs against the real interface type Spring injects.
