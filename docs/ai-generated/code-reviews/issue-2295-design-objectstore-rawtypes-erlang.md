# Erlang review: issue #2295 design.objectstore rawtypes batch

**Date:** 2026-08-07  
**Branch:** `fix/issue-2295-design-objectstore-rawtypes`  
**Scope:** uncommitted vs `HEAD` (PSReplacementValueFactory + test)

## Summary

Parameterizes rawtypes/unchecked in `PSReplacementValueFactory` (~41 diagnostics) using real generics (`List<?>`, `Class<?>`, `Constructor<?>`, `ConcurrentHashMap<String, Class<?>>`). Adds JUnit 5 behavioral tests for factory construction paths. No behavioral logic changes beyond typing; reflection + map lookup semantics preserved.

## Recommendation

**approve**

## Gate

| Check | Result |
| --- | --- |
| Bugs | none |
| Behavioral unit tests for new/changed logic | yes (12 tests) |
| Portable paths / file I/O | N/A (no path I/O) |
| Scope confined | yes (one production file + one test) |
| May commit/push | **yes** |

## Issues

None.

### Nits (non-blocking)

- `getReplacementValueFromString` still wraps `IllegalArgumentException` for unknown types in a catch-all `RuntimeException` (pre-existing). Tests document this; fixing would be a separate behavior change.

## Memory patterns hit

- Prefer real generics over class-level `@SuppressWarnings({"rawtypes","unchecked"})`
- Match surrounding objectstore style; Intersoft header on new 2026 test file

## Verification

- `cd system` → `../mvnw.cmd clean install` → **BUILD SUCCESS**
- Tests run: **1182**, Failures: **0**, Errors: **0**, Skipped: **240**
- Touched file rawtypes/unchecked: **41 → 0** (`PSReplacementValueFactory.java`)
