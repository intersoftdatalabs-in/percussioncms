# Erlang review — issue #3179 SharedGroup + WorkflowDef Xlint

**Scope:** `PSSharedGroupDependencyHandler`, `PSWorkflowDefDependencyHandler`, typed signature test  
**Change class:** perc-deployer Xlint residual (real generics, no behavior change)

## Checklist

| Gate | Result |
|------|--------|
| Bugs / logic regressions | Pass — casts removed only where APIs already return typed iterators/lists; raw system-module APIs (`getFieldGroups`) use `Iterator<?>` + documented casts |
| Behavioral unit tests | Pass — `PSSharedGroupWorkflowDefHandlersTypedTest` locks typed public API shapes (3 tests) |
| Portable paths / I/O | N/A — no path/file I/O changes |
| Companions | Pass — mirrors SystemDef #3178 / ContentType #3047 pattern |
| Suppressions | None |

## Notes

- Private install helpers in WorkflowDef now use `Iterator<PSJdbcRowData>`, `Iterator<PSJdbcColumnData>`, `Map<String,String>` id maps.
- Unused `PSBackEndRole` assignment dropped (`createRole` call retained).
- Module: `cd deployer && ../mvnw.cmd clean install` → BUILD SUCCESS; Tests run: 271.

> Co-Authored by Grok Build using grok-4.5 with agent main.
