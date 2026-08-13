# Erlang review — issue #3172 utils javac residual batch

- **Branch:** `fix/issue-3172-utils-javac-residuals`
- **Scope:** uncommitted + vs `origin/main` in `modules/utils` (plus this report)
- **Date:** 2026-08-13
- **Reviewer persona:** Erlang (independent of implementer)
- **Memory patterns hit:** missing behavioral tests; incomplete change-class closure (not applicable — no Spring/UI/adaptor surface); non-portable paths (none)

## Summary

Safe utils main-source suppression batch after #3015. Does **not** reparameterize `PSWorkflowUtilsBase` public raw List/Map APIs. Changes:

1. `PSXmlSerializationHelper` — drop class-level `@SuppressWarnings({"rawtypes","unchecked"})`; parameterize leftover raw `Class` as `Class<?>` (erasure-identical).
2. `PSCollection(String)` — assign `Class.forName` to existing `Class<?>` field instead of an unchecked `Class<? extends E>` ctor cast.
3. `PSItemIterator` — collapse two method-scoped unchecked casts into one private `asItem` helper (no new blanket suppress).

`PSCopier` nested-map `(V)` and `PSConcurrentList.restoreList` remain inherent deserialization/recursion residuals. `ConfigurationContextAbstract` already has no suppressions after batch 8.

## Recommendation

**approve**

## Gate

- **Bugs:** none found
- **Behavioral tests:** present for className ctor, `readFromXML(String, Class<?>)`, and multi-map non-Collection rejection
- **Cross-platform paths:** N/A (no filesystem path I/O in this diff)
- **May commit/push:** yes

## Issues

None (hard-gate).

### Notes (not blocking)

- `Class` → `Class<?>` is a public static signature widening; bytecode erasure is unchanged. No `extends PSXmlSerializationHelper` / anonymous subclass sites.
- Multi-map detection still uses `!(m_map instanceof HashMap)` (pre-existing). New test uses `TreeMap` because `LinkedHashMap` is a `HashMap`.
- Remaining `PSWorkflowUtilsBase` raw public API stays out of this PR by source-compat policy (issue body).

## Test evidence

- `cd modules/utils && ../../mvnw.cmd clean install`
- `BUILD SUCCESS`
- `Tests run: 387, Failures: 0, Errors: 0, Skipped: 9`
