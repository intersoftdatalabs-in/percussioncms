# Erlang review — issue #2299 process package rawtypes (slice 5g)

**Branch:** `fix/issue-2299-process-rawtypes-5g`  
**Scope:** `com.percussion.process` rawtypes/unchecked cleanup under perc-system  
**Date:** 2026-08-10  
**Reviewer persona:** Erlang (implementer self-review)

## Summary

PR-sized batch types process-framework collections with real generics (prefer
`Map<String,String>` / `Map<String,?>` / `List<byte[]>` / `List<?>` / typed
fields) across interfaces and implementations. Behavioral unit tests cover
manager load, command param resolution, resolvers, request XML round-trip, and
null-contract guards. No product behavior change intended.

## Recommendation

**approve**

## Gate

- Bugs: none found  
- Behavioral tests: present (`PSProcessPackageTypedTest`, 9 tests green)  
- Cross-platform path checklist: N/A for new path I/O (no new separators /
  absolute-path assumptions); existing daemon/local path guards unchanged  
- **May commit/push: yes**

## Issues

None.

## Notes

- Public signature tightening (`Map` → `Map<String,String>` / `Map<String,?>`)
  is source-compatible for typical callers using string maps / null. Grep found
  no production subclasses of `PSSimpleProcess` / alternate `IPSProcess`
  implementors outside the package. Legacy cactus tests under
  `modules/CMLight-Main-cactus-tests` are out of main reactor; they pass null /
  untyped maps which remain assignable with unchecked conversion.
- `Class.newInstance()` → `getDeclaredConstructor().newInstance()` for
  resolver instantiation only; broader catch via `ReflectiveOperationException`.
- Stayed out of open search/objectstore Xlint PRs (#2874, #2872, etc.).

## Build evidence

- `cd system && ../mvnw.cmd clean install` → **BUILD SUCCESS**  
- Tests run: **1697**, Failures: **0**, Errors: **0**, Skipped: **240**
