# Erlang review - issue #2697 (perc-deployer Xlint batch 3)

**Reviewer persona:** independent (not implementer)  
**Date:** 2026-08-10  
**Module:** `deployer` (perc-deployer)  
**Verdict:** PASS (with residual this-escape / handler / client rawtypes out of batch)

## Scope reviewed

PR-sized Xlint residual after batch 2 (#2417 / PR #2698), slice of #2028 under parent tracker #2200:

- `PSDependencyContext` — full local/`Map`/`List`/`Iterator`/`Entry` parameterization; `checkRemoveLocal` public map typed as `Map<String, List<PSDependency>>`; `getValueLists` stream-based
- `PSDependencyTreeContext` — remove redundant casts once `getDependencies`/`getAncestors` are `Iterator<PSDependency>`
- `PSApplicationIdContext` — `List`/`Iterator` of listeners; empty-statement after if replaced with null-arg check
- Leaf objectstore/catalog classes made `final` (no in-repo subclasses) to clear Element-ctor this-escape without suppressions
- Unit tests: context/tree behavior, listener notify, checkRemoveLocal typed map

## Checklist

| Gate | Result |
|------|--------|
| Bugs in new/changed logic | None found — typing only; remove/add multi paths covered by tests |
| Behavioral unit tests | Present and green (`PSDependencyContextTypedTest` 7, `PSApplicationIdContextListenersTest` 2; suite 192 tests, 0 fail, 19 skip) |
| Cross-platform paths | N/A for production path I/O; no new path construction |
| Suppress-only Xlint | Avoided — real generics + final classes preferred |
| Change-class companions | Call sites for `checkRemoveLocal` already used typed maps from tree context; module suite green |
| Over-scope | No monorepo reformat; no unrelated modules; no product behavior intent |

## Residual (not this PR)

- this-escape on non-leaf / hierarchy types (`PSDependency`, `PSDeployableObject`/`Element`, `PSDbmsInfo`, export/import descriptors, `PSDependencyMap`, policy settings)
- Rawtypes in `PSDeploymentManager`, `PSArchiveDetail`, `IPSDependencyHandler`, `PSFolderContentsDescriptorBuilder`, etc. (fill Maven 100-warn report ceiling after contexts cleared)
- Optional test-source rawtypes / for-removal constructors

## Build evidence

```
cd deployer && ../mvnw.cmd clean install
BUILD SUCCESS
Tests run: 192, Failures: 0, Errors: 0, Skipped: 19
```

Target-file rawtypes/unchecked for PSDependencyContext / PSDependencyTreeContext / PSApplicationIdContext: **cleared**. Leaf final this-escape for ~19 catalog/objectstore types: **cleared**. Capped Maven report remains 100 as previously-suppressed files fill the ceiling.
