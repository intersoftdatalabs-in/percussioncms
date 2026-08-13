# Erlang review: fix/issue-3172-utils-safe-residual-generics

## Summary

Conservative utils residual after batch 8 (#3015 / PR #3173). Removes the two remaining **product-safe** CRTP unchecked sites on `ConfigurationContextAbstract` with real typing: abstract `self()` (`return this` in the only subclass) and `cloneConfig` via `ctor.get()` + `PropertyUtils.copyProperties` (no `(U)` cast). Does **not** reparameterize `PSWorkflowUtilsBase` public raw List/Map APIs. Does **not** redesign `PSXmlSerializationHelper`. Leaves `PSItemIterator` / `PSCopier` / `PSConcurrentList.restoreList` scoped casts (inherent to `Map<?,?>` / `V`/generic deserialization). Module `mvnw clean install` green.

## Scope

- Branch: `fix/issue-3172-utils-safe-residual-generics`
- Module: `modules/utils` only
- Parent tracker: #2200 / module #2016 / residual #3172
- Prior: batch 8 #3015 / PR #3173

## Recommendation

**approve**

## Gate

May commit/push: **yes**

## Cross-platform path review

- New test uses JUnit `@TempDir Path` only. No filesystem path concatenation or OS-specific roots.

## Change map

| Area | Fix |
|------|-----|
| `ConfigurationContextAbstract` | `protected abstract T self()`; `cloneConfig` uses supplier + `PropertyUtils.copyProperties` |
| `DefaultConfigurationContextImpl` | `self()` returns `this` |
| Tests | `ConfigurationContextAbstractCrtpTest` — self identity; copyFrom clones and does not alias |

## Behavioral tests

- `ConfigurationContextAbstractCrtpTest` (2) — `load`/`save` via typed `self()`; `copyFrom` clones flags without aliasing the source config
- Existing adapter `copyFrom` suite still green

## Residual (intentional — remain on #3172 / #2016)

| Area | Notes |
|------|-------|
| `PSWorkflowUtilsBase` raw List/Map API | Source-compat; do **not** reparameterize without explicit policy |
| `PSXmlSerializationHelper` class rawtypes/unchecked | Serialization helpers; larger redesign |
| `PSItemIterator` method unchecked | Map value / multi-map Collection casts inherent to `Map<?,?>` + `setMap` |
| `PSCopier` nested Map-as-V unchecked | Narrowing to `Map<K,Object>` would break `Map<String,String[]>` parameters |
| `PSConcurrentList.restoreList` | Serialization cast residual (scoped); no `Class<E>` on the stream |

## C2 API shape

- Added `protected abstract T self()`. Grep monorepo `extends ConfigurationContextAbstract` / `new ConfigurationContextAbstract` → only `DefaultConfigurationContextImpl`. No anonymous subclasses. Reverse-deps (`system`, `perc-ant`) construct the concrete type only.

## Verification

- `cd modules/utils && ..\..\mvnw.cmd clean install` → **BUILD SUCCESS**
- Tests: **384** run, **0** failures, **9** skips
- Memory patterns hit: behavioral tests for changed logic; no path I/O; no new blanket suppressions

## Issues

None (bug).
