# Erlang review — #2359 where-clause IR + join product limit

**Date:** 2026-08-08  
**Branch:** `fix/issue-2359-where-clause-ir-join-limit`  
**Module:** `system` (perc-system)  
**Verdict:** **APPROVE**

## Change class

Pipeline IR / SQL planner residual: executable where-clause IR for generated SELECT; documented multi-table join product limit with rejection tests. Companions: classic importer mapping, IR model, planner, H2 runtime tests, `pipeline-ir-v1.md`.

## Scope reviewed

| Artifact | Role |
|----------|------|
| `WhereClauseIr` | New IR predicate model |
| `SelectorStageIr` | `whereClauses` list + count sync |
| `PSClassicPipelineImporter` | Map `PSWhereClause` → IR |
| `PSPipelineSqlPlanner` | Executable WHERE; join product limit message |
| `PSPipelineIrServiceTest` / `PSPipelineRuntimeServiceTest` | Import + H2 + planner tests |
| `docs/developer-module/pipeline-ir-v1.md` | Product limit + IR shape |

## Checklist

| Gate | Result |
|------|--------|
| Bugs / logic | No defects found. Boolean connectors use previous clause op (classic semantics). Literals always JDBC-bound. omitWhenNull skips predicate without false-positive SQL. |
| SQL injection | Identifiers via `SAFE_IDENT` + quote; values always `?` binds. Native path unchanged. |
| Cross-platform paths | No filesystem path construction; N/A. |
| Tests | IR import asserts `sys_key` clause; H2 PARAM + LIKE; joinCount>0 + multi-table rejection. 31 focused tests green; module clean install green. |
| Companions | Residual join-graph planner filed as child (product limit documented, not full multi-table generation). |
| Copyright | New files: Intersoft 2026. |

## Residual (intentional)

Full multi-table join SQL generation deferred; `joinCount > 0` remains a hard product limit with stable message constant `JOIN_PRODUCT_LIMIT_MESSAGE`.

## Build evidence

```
cd system && ../mvnw.cmd clean install
BUILD SUCCESS
```
