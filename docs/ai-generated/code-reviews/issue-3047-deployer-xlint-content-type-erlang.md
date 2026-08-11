# Erlang-style review — issue #3047 PSContentTypeDependencyHandler Xlint

**Scope:** perc-deployer main-source Xlint residual on `PSContentTypeDependencyHandler` (~69 → 0).

**Change class:** pure generics / rawtypes cleanup (no product behavior).

## Checklist

| Gate | Result |
|------|--------|
| Bugs / behavior change | None — typed iterators, diamond constructors, enhanced for-loops only |
| Whole-class `@SuppressWarnings("unchecked")` | None; one local suppress on `PSWorkflowInfo.getWorkflowIds()` raw return |
| Portable paths | N/A — no path/file I/O changes |
| Behavioral unit tests | `PSContentTypeHandlerTypedTest` locks typed override + package-private returns |
| Product docs | N/A — tech-debt only |
| Module clean install | Required before PR (`cd deployer && ../mvnw clean install`) |

## Peers followed

- #3017 / PR #3046 `PSContentRelationDependencyHandler`
- #3016 batch 10 ContentAssembler/Content/ContentListDef
- `PSApplicationDependencyHandler.getIdTypes` typed `List<PSApplicationIDTypeMapping>` + for-each over `PSCollection`

## Residual (not this PR)

Other perc-deployer main residuals remain (SystemDef / SharedGroup / WorkflowDef / FilterDef / Custom / Context*, etc.) under epic #2028 / monorepo #2200.
