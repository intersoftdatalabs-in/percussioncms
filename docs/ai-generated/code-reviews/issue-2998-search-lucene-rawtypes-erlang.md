# Erlang review — issue #2998 (search Lucene rawtypes residual)

**Reviewer persona:** Erlang (independent of implementer)  
**Date:** 2026-08-11  
**Branch:** `fix/issue-2998-search-lucene-rawtypes`  
**Scope:** Type `PSSearchIndexer` / `PSSearchIndexerImpl` and `PSSearchQuery` / `PSSearchQueryImpl` Map/Collection/List surfaces after #2873.

## Verdict: PASS

No hard-gate bugs, missing behavioral tests for changed logic, or non-portable path I/O in this diff.

## Change class

**Public API generics typing** on Lucene search indexer/query abstract surfaces + Lucene impl + minimal call-site closure (`PSSearchHandler`). Companion: unit tests for typed contracts/helpers; system module suite.

## Findings

| Severity | Finding | Disposition |
|----------|---------|-------------|
| none | Product behavior preserved: `maxResults` still only removed from props (not applied as a hit cap) matching prior Lucene impl | intentional — no behavior change |
| note | Disabled cactus tests under `modules/CMLight-Main-cactus-tests` still use raw `Map` + `Integer` for control props; not in reactor product path; re-enable would need `"2"` string values | out of scope residual optional |
| none | Paths: no new filesystem path construction; existing `File` + root path usage unchanged | OK |
| none | `stringFieldValues` filters non-String payloads before `getFieldMimeType(Map<String,String>)` — matches historical cast-to-String mime lookup | OK |

## Tests

- `PSSearchIndexerImplTypedTest` — stringFieldValues filter; null arg contracts; null-entry delete skip
- `PSSearchQueryTypedTest` — convenience overload delegates with null control props; typed maps/list return
- `cd system && ../mvnw.cmd clean install` → BUILD SUCCESS; Tests run: 1797, Failures: 0, Errors: 0, Skipped: 241

## Downstream / API blast radius (C2)

Public signature changes on abstract `PSSearchIndexer` / `PSSearchQuery`. Grep:

- No external production modules implement or extend these types outside `system` Lucene package.
- Call sites: `PSSearchIndexEventQueue` (already typed fragments/`Collection<PSSearchKey>`), `PSSearchHandler` (typed maps + empty `ArrayList<>`).
- Reverse-dep cactus tests: disabled; raw maps still compile via unchecked conversion when that module is built separately.

**downstream_checked:** monorepo grep for `extends PSSearchIndexer|extends PSSearchQuery` and call sites; system standalone install only (no reverse-dep module implements the API).

## Product documentation

N/A — pure tech-debt generics / `-Xlint` cleanup; no operator/user/API behavior change.
