# Erlang review: perc-system PSCollection compile restore

## Summary

PR #3173 parameterized `PSCollection` as `PSConcurrentList<Object>` (`List<Object>`).
That is not source-compatible with perc-system: `Iterator` is invariant, so
`PSRelationshipSet.iterator()` returning `Iterator<PSRelationship>` cannot implement
`List<Object>.iterator()`, and `Iterator<Object>` cannot be assigned to
`Iterator<PSDisplayMapping>` (and the same pattern across objectstore/cms/security).

This change parameterizes `PSCollection<E> extends PSConcurrentList<E>`. Raw
subclasses (`PSCollectionComponent`, TableFactory collections) remain raw lists,
so covariant `iterator()` overrides and `Iterable<Specific>` assignments compile
again. Typed `PSCollection<String>` is a real `List<String>`. Iterator-from-
constructor uses a snapshot + private ctor so it does not call overridable `add`
during construction.

## Scope

- Uncommitted vs `HEAD` on `fix/perc-system-pscollection-compile` (from `main`)
- Files:
  - `modules/utils/src/main/java/com/percussion/util/PSCollection.java`
  - `modules/utils/src/test/java/com/percussion/util/PSCollectionTypedTest.java`
  - `modules/utils/src/test/java/com/percussion/util/PSConcurrentListSerializationTest.java`
- Out of scope: perc-system / TableFactory sources (consumers only)
- Memory patterns hit: missing behavioral tests (addressed); change-class closure
  (utils type + consumer compile); do not lock historically raw collections to
  `List<Object>`; no path I/O
- Cross-platform path review: N/A (no file I/O / path construction)

## Recommendation

**approve**

## Gate

- May commit/push: **yes**
- Bugs: none
- Missing behavioral tests: no — typed list API, runtime `checkType`, covariant
  raw-subclass `iterator()` / `Iterable<E>`
- Change-class closure: yes — producer (`utils`) plus verified perc-system and
  TableFactory standalone clean installs
- Agent rule files: none in this diff

## Issues

None.

### Suggestion (non-blocking)

Later batches can parameterize `PSCollectionComponent<E>` and product subclasses
so raw-type warnings at `extends PSCollection` go to zero. Do **not** “fix” those
by locking the base type to `List<Object>` again.

## Verification noted

```text
cd modules/utils
..\..\mvnw.cmd clean install
# BUILD SUCCESS — Tests run: 382, Failures: 0, Errors: 0, Skipped: 9
# PSCollectionTypedTest: 6 tests, 0 fail

cd system
..\mvnw.cmd clean install
# BUILD SUCCESS — Tests run: 1996, Failures: 0, Errors: 0, Skipped: 241

cd modules/TableFactory
..\..\mvnw.cmd clean install
# BUILD SUCCESS (consumer still compiles after #3216 + this change)
```
