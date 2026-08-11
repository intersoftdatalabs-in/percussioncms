# Erlang review: fix/issue-2969-utils-javac-residual-batch7

## Summary

Batch 7 residual after utils batch 6 (#2414 / PR #2970). Clears the **JNDI datasource this-escape cluster** and **PSException** class-level this-escape with real redesigns (no BeanUtils in copy ctor, final Jetty class + field assigns, direct `m_code` assign). Does **not** reparameterize `PSWorkflowUtilsBase` public raw List/Map API. Module `mvnw clean install` green; main compile shows **0** this-escape warnings on changed sources.

## Scope

- Branch: `fix/issue-2969-utils-javac-residual-batch7`
- Module: `modules/utils` only
- Parent tracker: #2200 / module #2016 / residual #2969
- Prior: batch 6 #2414 / PR #2970

## Recommendation

**approve**

## Gate

May commit/push: **yes**

## Cross-platform path review

- No new filesystem path construction.
- Jetty Properties ctor still uses existing `PathUtils.getRxDir()` / `getRxPath()` APIs (pre-existing portable Path usage).
- Tests use plain Properties / in-memory objects only.

## Change map

| Area | Fix |
|------|-----|
| `PSJBossJndiDatasource` copy ctor | Multi-arg super + protected field overlay; no BeanUtils; strip this-escape |
| `PSJndiDatasourceImpl` | `id` protected; `setId` final |
| `PSJettyJndiDatasource` | `final` class; Properties ctor direct field assign; strip this-escape |
| `PSException` | Direct `m_code = 0` in message+cause ctor; `setErrorCode` final; strip class this-escape |

## Behavioral tests

- `PSJBossJndiDatasourceCopyTest` (3)
- `PSJettyJndiDatasourcePropsTest` (1)
- `PSExceptionCauseCtorTest` (2)

## Residual (intentional — file child)

| Area | Notes |
|------|-------|
| `PSWorkflowUtilsBase` raw List/Map API | Source-compat; do not reparameterize without policy |
| `PSCollection` raw/this-escape | Legacy collection; many product subclasses |
| `PSJexlEvaluator` / `PSXmlSerializationHelper` | Scripting/serialization surfaces |
| `ConfigurationContextAbstract` CRTP | `(T) this` / BeanUtils.cloneBean |
| `PSItemIterator` / `PSCopier` | Documented inherent unchecked casts |
| `PSConcurrentList.readObject` | Serialization unchecked |

## Verification

- `cd modules/utils && ..\..\mvnw.cmd clean install` → **BUILD SUCCESS**
- Tests: **352** run, **0** failures, **9** skips
- Main compile: **no** `possible 'this' escape` warnings on changed files
- Grep: no monorepo `extends PSJettyJndiDatasource` (safe to finalize)

## Issues

None (bug).
