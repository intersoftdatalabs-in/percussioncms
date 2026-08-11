# Erlang review: fix/issue-2414-utils-javac-residual-batch6

## Summary

Batch 6 residual after utils batch 5 (#2382 / PR #2413). Clears a **coherent this-escape cluster** with real redesigns (final classes/methods, direct field init, Exception cause via `super(cause)` instead of post-construction `initCause`). Does **not** reparameterize `PSWorkflowUtilsBase` public raw List/Map API. Module `mvnw clean install` green; project-Xlint compile shows **0** this-escape warnings on changed sources.

## Scope

- Branch: `fix/issue-2414-utils-javac-residual-batch6`
- Module: `modules/utils` only
- Parent tracker: #2200 / module #2016 / residual #2414
- Prior: batch 5 #2382 / PR #2413

## Recommendation

**approve**

## Gate

May commit/push: **yes**

## Cross-platform path review

- New tests use `Path` / `@TempDir` / `Files` — portable.
- `PSTomcatConnectors` resolves OS via `System.getProperty("os.name")` (not hardcoded path separators).
- No new `".../" +` filesystem joins introduced.

## Change map

| Area | Fix |
|------|-----|
| `PSFileFilter` | `final` class; strip this-escape (setters already final) |
| `PSHtmlParamDocument` | `final` class; strip this-escape |
| `PSDatasourceConfig` | `final` class + final mutators/`fromXml`/`copyFrom`; direct field init in members ctor |
| `PSProperties` | `final` class; `final` `load`/`put` |
| `PSBrandCodeElement` / `List` / `MapVersion` | `final` classes; strip this-escape |
| `Code` | `final` class; strip this-escape |
| `PSAbstractConnectors` | `final` `setConnectors` / `mergeConnectors` / http(s) getters |
| `PSJettyConnectors` / `PSJBossConnectors` | `final` classes; strip this-escape on copy ctors |
| `PSTomcatConnectors` | OS via `System` property; `final` getter; strip this-escape |
| `HttpsBuilder` | direct `scheme` assign instead of overridable `setHttps()` |
| `PSBaseException` / `ConnectorConfigurationException` | `super(cause)` / `super(message, cause)`; drop post-`initCause` this-escape |
| `PSGuid` | `final` `getHostId`/`getType`/`getUUID`; strip class this-escape |

## Behavioral tests

- `PSFileFilterTest` (3)
- `PSHtmlParamDocumentTest` (3)
- `PSPropertiesTest` (3)
- `PSDatasourceConfigTest` (4)
- `PSBaseExceptionCauseTest` (2)
- `ConnectorConfigurationExceptionTest` (2)
- `PSGuidTest` (3)

## Residual (intentional — file child if desired)

| Area | Notes |
|------|-------|
| `PSWorkflowUtilsBase` raw List/Map API | Source-compat; do not reparameterize without policy |
| `PSCollection` raw/this-escape | Legacy collection |
| `PSJexlEvaluator` / `PSXmlSerializationHelper` | Scripting/serialization surfaces |
| `PSException` this-escape | Deep exception hierarchy; many subclasses |
| `PSJBossJndiDatasource` / `PSJettyJndiDatasource` | BeanUtils.copyProperties / props ctor this-escape |
| `ConfigurationContextAbstract` CRTP | `(T) this` / BeanUtils.cloneBean |
| `PSItemIterator` / `PSCopier` | Documented inherent unchecked casts |
| `PSConcurrentList.readObject` | Serialization unchecked |

## Verification

- `cd modules/utils && ..\..\mvnw.cmd clean install` → **BUILD SUCCESS**
- Tests: **346** run, **0** failures, **9** skips
- Main compile: **no** `possible 'this' escape` warnings on changed files
- Grep: no monorepo `extends` of newly-final leaf types

## Issues

None (bug).
