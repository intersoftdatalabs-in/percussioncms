# Erlang code review — issue #2311 cms.objectstore rawtypes batch 2b

**Branch:** `fix/issue-2311-objectstore-rawtypes-batch`  
**Base:** `origin/main`  
**Reviewer persona:** Erlang (strict independent)  
**Date:** 2026-08-07  
**Recommendation:** **approve**  
**Gate:** May commit/push: **yes**

## Summary

Continues #2022 / #2296 rawtypes/unchecked cleanup in `com.percussion.cms.objectstore` with the next hottest residual cluster: `PSRemoteAgent`, client/server `PSRelationshipProcessor` (+ proxy), `PSActiveAssemblerProcessor`, and selected `PSSearch` APIs. Real generics preferred (`List<PSEntry>`, `List<PSLocator>`, `Map<String,String>`, `List<?>`, etc.); interface alignment with existing `List<?>` on `IPSRelationshipProcessor`. Behavioral JUnit 5 tests cover `PSSearch.parseParameters`. Module `system` `mvnw clean install` green (1180 tests, 0 failures; 5 new tests green).

Companion compile fix: `PSNavonNodeInvocationHandler` copy from `Map<?,?>` (utils `PSItemIterator.getMap` already returns wildcards) — unblocks perc-system compile after utils generics change.

## Scope

| Area | Change |
| --- | --- |
| Production | `client/PSRemoteAgent`, `client/PSRelationshipProcessor`, `server/PSRelationshipProcessor`, `server/PSActiveAssemblerProcessor`, `PSRelationshipProcessorProxy`, `PSSearch` (partial), `PSNavonNodeInvocationHandler` (compile companion) |
| Tests | `PSSearchParseParametersTest` (5) |
| Out of scope | design.objectstore (#2295/#2309), this-escape/serial (#2297/#2313/#2319), data.jdbc (#2298/#2315), remaining `PSSearch` Iterator fields/APIs, `PSServerFolderProcessor` raw List overrides |

Cross-platform path review: **N/A** — no file I/O or path handling in this diff.

## Issues

None at severity `bug`.

### suggestion (non-blocking)

1. **`PSSearch` residual** — public/private `Iterator`/`setFields`/`getProperties` and property loops remain raw; leave for next #2022 residual slice.
2. **`PSServerFolderProcessor`** still implements relationship methods with raw `List` parameters; compile-compatible with `List<?>` interface but residual rawtypes remain outside this batch.
3. **Runtime casts** on relationship children (`(PSKey)`, `(PSLocator)`) are unchanged pre-existing contracts (callers pass locator/key lists).

### nit

- Approximate raw declaration sites cleared on touched production files: **~45–55** (within overnight ≤~40–50 target band plus proxy/companion).

## Gate checklist

| Check | Result |
| --- | --- |
| Bugs | none found |
| Behavioral tests for changed logic | yes (5 new tests on pure `parseParameters`) |
| Portable paths | N/A |
| Module clean install | BUILD SUCCESS |
| Scope confinement | cms.objectstore batch + one compile companion in services assembly nav |

## Recommendation

**approve** — ready to commit and open PR (Fixes #2311, Refs #2022 #2200).
