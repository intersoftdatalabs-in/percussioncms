# Erlang review — issue #2847 perc-deployer Xlint residual (handlers + hierarchy)

**Status:** PASS  
**Reviewer persona:** Erlang (pre-commit)  
**Scope:** deployer module Xlint residual after batch 5 (#2825 / PR #2846)

## Change class

Compile-time generics typing + hierarchy this-escape mitigation. No product behavior change.

## Checklist

| Gate | Result |
|------|--------|
| Bugs / logic regressions | PASS — private helpers delegate to same bodies; Element XML round-trips covered by tests |
| Behavioral unit tests | PASS — `PSDependencyHierarchyXlintTest`, `PSApplicationDependencyHandlerTypedTest` |
| Cross-platform paths | PASS — no new path I/O |
| Change-class companions | PASS — tests + module suite; product-docs N/A (tech-debt only) |
| C2 final types | PASS — grepped monorepo for `extends PSDeployableElement` / `PSDependencyData` — none; `PSDeployableObject` remains non-final (`PSUserDependency`) |
| Module clean install | PASS — `cd deployer && ../mvnw.cmd clean install` |

## Findings

None blocking. Scoped `@SuppressWarnings("this-escape")` on `PSDependencyHandler` ctor (abstract `getType()`) and `PSDeployableObject` Element ctor (`setParentDependency(this)` child linking) are documented; private/final helpers used where possible.

## Residual (next batch)

Maven still caps Xlint report at 100. Post-batch composition dominated by other dependency handlers (`PSDataObjectDependencyHandler`, `PSCmsObjectDependencyHandler`, `PSAuthTypeDependencyHandler`, community/component handlers) — file residual child under #2028/#2200.
