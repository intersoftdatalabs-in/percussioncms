# Erlang review: issue #2323 extensions-sfp javac batch 2

## Summary

Batch 2 strips residual class-level `@SuppressWarnings({"rawtypes","unchecked",…})` from the site-folder / relationship cluster in `modules/extensions-sfp`, replacing them with real `List`/`Map`/`Set`/`Iterator` parameterization. Shared `Set<String>` param chain is closed through exits → builders → link generator → content list items.

## Scope

- Branch: `fix/issue-2323-sfp-rawtypes-batch2`
- Module: `modules/extensions-sfp`
- Prior: batch 1 PR #2322 / issue #2035 residual #2323
- Memory: calendar MultiMap method-level unchecked retained (batch 1 structural)

## Recommendation

**approve**

## Gate

|                  Check                  |                    Result                     |
|-----------------------------------------|-----------------------------------------------|
| Bugs                                    | none found                                    |
| Behavioral unit tests for changed logic | yes (`PSSqlInListTest`, `PSSitePathListTest`) |
| Portable paths / file I/O               | N/A (no path I/O changes)                     |
| May commit/push                         | **yes**                                       |

Cross-platform path review: no path/file I/O in this diff.

## Issues

None (severity bug/suggestion/nit empty for hard gate).

### Notes (non-blocking)

- `PSCalendarMonthModel.getEvents` still has method-level `@SuppressWarnings("unchecked")` for untyped Apache MultiMap — intentional structural retain from batch 1; not a class-level rawtypes/unchecked blanket.
- `this-escape` / `serial` / `deprecation` suppressions remain where legitimate.
- `PSSiteFolderCListBulk` ID batching loop preserves original flush-when-full iterator semantics (does not advance iterator on flush branch).

## Verification

- `cd modules/extensions-sfp` → `../../mvnw.cmd clean install` → **BUILD SUCCESS**
- Tests run: **22**, Failures: **0**, Errors: **0**, Skipped: **4** (pre-existing calendar model skips)
- Zero class-level rawtypes/unchecked suppressions remain in module

## Conclusion

Safe to commit and open PR with `Fixes #2323` and `Fixes #2035` (parent residual fully cleared of class-level rawtypes/unchecked suppressions).
