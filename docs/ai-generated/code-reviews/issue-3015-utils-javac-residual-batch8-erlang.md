# Erlang review: fix/issue-3015-utils-javac-batch8-real-generics

## Summary

Batch 8 residual after utils batch 7 (#2969 / PR #3014). Clears **class-level** suppressions on `PSJexlEvaluator` and `PSCollection` with real generics; hardens `PSConcurrentList` deserialization; consolidates CRTP unchecked sites on `ConfigurationContextAbstract`. Does **not** reparameterize `PSWorkflowUtilsBase` public raw List/Map API. Module `mvnw clean install` green.

## Scope

- Branch: `fix/issue-3015-utils-javac-batch8-real-generics`
- Module: `modules/utils` only
- Parent tracker: #2200 / module #2016 / residual #3015
- Prior: batch 7 #2969 / PR #3014

## Recommendation

**approve**

## Gate

May commit/push: **yes**

## Cross-platform path review

- No new filesystem path construction or path string handling.
- Serialization and binder tests use in-memory streams / maps only.

## Change map

| Area | Fix |
|------|-----|
| `PSJexlEvaluator` | Real `Map<String,Object>` / `List<Object>` binder path; strip class-level suppress; scoped helpers `asStringObjectMap` / `asMutableObjectList` |
| `PSCollection` | `extends PSConcurrentList<Object>`; `Class<?>` / `Collection<?>` / `Iterator<?>`; strip class-level rawtypes/unchecked/this-escape suppress |
| `PSConcurrentList` | `restoreList` with `instanceof List` + `InvalidObjectException`; single scoped unchecked residual |
| `ConfigurationContextAbstract` | CRTP `self()` + `cloneConfig()` helpers; no multi-site method suppress scatter |

## Behavioral tests

- `PSCollectionTypedTest` (4) — member class, addAll type reject, iterator ctor, capacity ctor
- `PSConcurrentListSerializationTest` (+2) — restoreList rejects non-list; null → empty
- Existing `PSJexlEvaluatorTest` binder/eval suite still green

## Residual (intentional — file child)

| Area | Notes |
|------|-------|
| `PSWorkflowUtilsBase` raw List/Map API | Source-compat; do **not** reparameterize without explicit policy |
| `PSXmlSerializationHelper` class rawtypes/unchecked | Serialization helpers; larger redesign |
| `PSItemIterator` method unchecked | Map value / multi-map Collection casts inherent to `Map<?,?>` |
| `PSCopier` nested Map-as-V unchecked | Documented method-scoped residual |
| `PSConcurrentList.restoreList` | Serialization cast residual (scoped) |
| `ConfigurationContextAbstract` CRTP helpers | `(T) this` / BeanUtils clone residual (scoped) |

## Verification

- `cd modules/utils && ..\..\mvnw.cmd clean install` → **BUILD SUCCESS**
- Tests: **380** run, **0** failures, **9** skips
- Grep: monorepo `extends PSCollection` remains product subclasses (TableFactory etc.); no API reparameterization of WorkflowUtilsBase

## Issues

None (bug).
