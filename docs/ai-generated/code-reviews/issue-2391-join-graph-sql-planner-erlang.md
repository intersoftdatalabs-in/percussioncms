# Erlang review — issue #2391 join-graph SQL planner

| Field | Value |
|-------|-------|
| Issue | #2391 |
| Change class | Pipeline IR join-graph + multi-table SELECT planner |
| Module | `system` |
| Date | 2026-08-09 |

## Scope

| Artifact | Role |
|----------|------|
| `BackendJoinIr` | IR join edge model |
| `BackendTankStageIr` | `joins[]` + joinCount inventory |
| `PSClassicPipelineImporter` | Classic `PSBackEndJoin` → IR edges |
| `PSPipelineSqlPlanner` | ANSI multi-table FROM/JOIN SELECT |
| `PSPipelineRuntimeServiceTest` | H2 INNER + LEFT OUTER + limit tests |
| `docs/developer-module/pipeline-ir-v1.md` | Product docs |

## Checklist

| Gate | Result |
|------|--------|
| Bugs | No hard bugs found. Join-type flip when introducing left table preserves OUTER semantics. Disconnected graphs / translators / missing edges rejected with stable messages. |
| Behavioral tests | Planner SQL shape + H2 INNER/LEFT execute + translator/edge-limit rejection |
| Cross-platform paths | N/A (SQL/IR only; no filesystem path construction) |
| Companions | IR model + classic import + planner + H2 tests + docs |
| Mutations | Still single-table; join edges rejected on INSERT/UPDATE/DELETE |
| Security | Identifiers validated; values only as JDBC binds; no string-concat user data |

## Residual (not this PR)

- Multi-table mutations
- Join edges with classic extension translators (native SELECT escape hatch)
- Designer UI / graph editor

## Verdict

**Pass** — ready to commit/PR after `system` `mvnw clean install` BUILD SUCCESS.
