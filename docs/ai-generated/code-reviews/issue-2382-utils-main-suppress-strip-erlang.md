# Erlang review: fix/issue-2382-utils-main-suppress-strip

## Summary

Batch 5 residual after utils batch 4 (#2362 / PR #2383). Strips **safe** main-source `@SuppressWarnings` with real generics/typing (no new blanket suppressions; does **not** reparameterize `PSWorkflowUtilsBase` public raw List/Map API). Module `mvnw clean install` green; project-Xlint compiler warnings during compile remain **0** (deprecation excluded by parent).

## Scope

- Branch: `fix/issue-2382-utils-main-suppress-strip`
- Module: `modules/utils` only
- Parent module issue: #2016
- Tracker: #2200
- Issue: #2382

## Changes (main)

| Area | Fix |
|------|-----|
| `PSIteratorUtils` | Generic `CountedIterator<T>`; drop unchecked cast suppress |
| `PSMultiMapIterator` | `Predicate<? super Object>` filter; drop cast suppress |
| `PSItemIterator` | `Predicate<Object>` for shared key/value name filter |
| `PSBaseValue` | Pattern-match `equals`; drop class rawtypes suppress |
| `PSNamingContextHelper` | `Map<?, ?>` bindings; drop class rawtypes/unchecked |
| `PSBaseHttpUtils` | Rebuild multi-value `List<String>` without unchecked list cast |
| `PSMultiProperty` | Checked `Object → Collection<?>` cast; drop unchecked suppress |
| `PSAbstractConnector` | Drop class rawtypes/unchecked; pattern-match `HttpsBuilder`; drop unused type param |
| `PSCopier` | Enhanced for-each / pattern match; keep **one** inherent nested-map unchecked cast |

## Tests added/updated

- `PSIteratorUtilsTest` (new)
- `PSBaseHttpUtilsParseQueryTest` (new)
- `PSCopierTest` (new)
- `PSValuesTest.testBaseValueEqualsUsesTypedPatternMatch`

## Recommendation

**approve**

## Gate

May commit/push: **yes**

## Cross-platform path review

N/A for logic paths. Connector builder still uses `Path` / `Paths.get` (portable). No new OS path separators.

## Issues

None (bug).

### residual (intentional / hard)

- `PSWorkflowUtilsBase` method-level rawtypes/unchecked (public raw List/Map source-compat — do not reparameterize without policy)
- `PSCollection`, `PSJexlEvaluator`, `PSXmlSerializationHelper` class-level raw/unchecked
- Brand-code / connector / exception / guid / properties `this-escape` suppressions
- `ConfigurationContextAbstract` CRTP `(T) this` / `BeanUtils.cloneBean` unchecked
- `PSItemIterator` Map value / multi-map collection unchecked casts
- `PSCopier` nested `Map` as `V` unchecked cast (documented, method-scoped)

## Verification

- `cd modules/utils && ..\..\mvnw.cmd clean install` → BUILD SUCCESS
- Tests: **322** run, **0** fail, **9** skip (pre-existing)
- Project-Xlint compiler warnings (rawtypes/unchecked/this-escape noise): **0** new; compile only reports excluded deprecation

> Co-Authored by Grok Build using grok-4.5 with agent main.
