# Erlang review: issue #3181 services.security Xlint batch

**Change class:** Generics/rawtypes cleanup (tech-debt) under `com.percussion.services.security` (+ small utils/legacy helpers).

## Scope reviewed
- `system/services/src/com/percussion/services/security/**` typed collections
- `PSAssemblyServiceUtils`, `PSCriteriaQueryRepeater`
- `PSHibernateEvictionTableUpdateHandler` / `PSCmsObjectMgr.handleDataEviction` Class<?> align
- Unit tests: `PSJaasUtilsTest`, `PSServicesSecurityTypedTest`

## Hard gates
| Gate | Result |
|------|--------|
| Bugs | None found — behavior preserved; loops replace FilterIterator without changing match rules |
| Behavioral tests | Yes — null guards + create/find + permissions enum |
| Portable paths | N/A (no I/O) |
| API blast radius | `findOrCreateGroup(Collection<? super Principal>)`, `findFirstPSPrincipal(Collection<?>)`, `Map<String,?>` assembly params, `Class<?>[]` eviction ctor — callers pass compatible collections |

## Notes
- Prefer real generics over suppressions; removed obsolete `@SuppressWarnings("unchecked")` where typed.
- Out of scope residual: publisher/assembly/contentmgr/legacy bulk Xlint (~200+ diags).

## Verdict
**PASS** — ready for commit/PR after system clean install.

> Co-Authored by Grok Build using grok-4.5 with agent main.
