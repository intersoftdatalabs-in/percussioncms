# Erlang review — issue #3160 server.webservices rawtypes residual

**Verdict:** APPROVE  
**Scope:** PR-sized residual of #2877 / parent #2022 (grandparent #2200): real generics in `com.percussion.server.webservices` (+ typed `PSWSSearchRequest#getInternalSearchParams`). Avoids open PR #3161 (`server.cache`).  
**Reviewer persona:** Erlang (independent of implementer)  
**Date:** 2026-08-12

## Change class

Typed generics modernization for server webservices handlers and folder/search processors (no new public REST surface, no Spring beans, no installer/UI paths).

## Companions checked

| Companion | Status |
| --- | --- |
| Behavioral unit tests | `PSServerWebServicesTypedTest` (5 tests: init roots, empty reject, internal search params, locator projection, communities set) |
| Module standalone `mvnw clean install` | system / perc-system **BUILD SUCCESS** — Tests run: **1956**, Failures: 0, Errors: 0, Skipped: 241 |
| Path / file I/O | None in this diff |
| Spring / ApplicationContext | N/A |
| Public API shape | `PSWSSearchRequest#getInternalSearchParams` now `Map<String,String>` (field already typed); `PSWebServicesRequestHandler#init` / `getRequestRoots` align to already-typed interfaces |

## Findings

### Bugs

None introduced. Folder copy path now calls `PSServerFolderProcessor#copy(String, List<?>, PSKey)` so `PSLocatorWithName` override names remain available (previously raw `copyChildren(List<PSLocator>)` hid the mixed list).

### Missing tests

None for this change class: typed init roots, search params map immutability, and locator projection for add/move/remove are covered.

### Non-portable paths

None.

### Residual risk

- `PSRelationshipTracker#getItemSources` / `getItemRelationships` still return raw `Iterator` (typed as `Iterator<?>` at call sites).
- Crosssite package javadoc "Enumeration" noise; not rawtypes diagnostics.
- Broader perc-system residual outside `server.webservices` remains (server root non-cache, `services.*`, security residual) — file residual issue under #2022.

## Decision

**Approve** for commit/PR. Pure tech-debt Xlint cleanup; product-docs N/A; C5 UI live proof N/A.
