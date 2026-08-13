# Erlang review — issue #3210 services.contentmgr/legacy Xlint batch

**Date:** 2026-08-12  
**Scope:** `com.percussion.services.legacy` + `com.percussion.services.contentmgr` residual rawtypes/unchecked after #3188  
**Reviewer persona:** Erlang (pre-commit gate)

## Change class
Tech-debt generics: typed Hibernate `Query`/`MutationQuery`/`createNamedQuery`/`createNativeQuery` in `PSCmsObjectMgr`, typed `createQuery(..., Class)` in `PSContentMgr` and `PSContentRepository`, focused unit smoke.

## Findings
| Severity | Finding | Resolution |
|----------|---------|------------|
| none | No behavioral logic bugs found in typing refactor | n/a |
| none | Paths/I-O not touched | n/a |
| note | `loadItemEntry` now binds `:id` instead of concatenating Integer into HQL | Safer, same predicate |
| note | `Query<Map>` uses raw `Map` (Hibernate `new map(...)` projection) | Copy via `asStringObjectMap`; no blanket list suppressions |
| note | Residual assembly / schedule / workflow / other services remain | File residual child on #2022 |

## Tests
- `PSServicesLegacyTypedTest` (7): locale HQL builder, update-date mutation HQL, item-entry bind, menu-relation SQL
- `PSServicesContentmgrTypedTest` (5): map copy + folder-id remap
- Module: `system` `mvnw clean install` — BUILD SUCCESS, Tests run: 2008, Failures: 0, Skipped: 241

## Verdict
**Pass** for commit/PR under gates (portable paths N/A; unit smoke present; clean install green).
