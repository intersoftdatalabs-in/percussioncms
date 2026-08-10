# Erlang review — issue #2398 residual data/data.jdbc rawtypes (slice 4d)

**Reviewer persona:** Erlang (independent of implementer)  
**Date:** 2026-08-09  
**Branch:** fix/issue-2398-data-jdbc-rawtypes  
**Scope:** `com.percussion.data` + `data.jdbc` residual rawtypes/unchecked after #2364 / PR #2399

## Verdict: **APPROVE** (with residual noted for parent #2022)

### Change class

Compile-time rawtypes/unchecked cleanup in data pipeline packages — no SQL/result-set behavioral intent change. Companion: unit tests for typed maps/helpers + module clean install.

### Findings

|     Severity     |                                                         Finding                                                          |           Disposition            |
|------------------|--------------------------------------------------------------------------------------------------------------------------|----------------------------------|
| Bug              | None introduced in typed maps                                                                                            | OK                               |
| Behavioral tests | Typed tests for DTD mapper initResultSet name map, UpdateOptimizer dependency helper, link type strings, join plan order | Present                          |
| Portability      | No path/file I/O changes                                                                                                 | N/A                              |
| Residual         | Package not zeroed (extensions, more optimizers, converters residual, etc.)                                              | File residual on #2022 if needed |

### Notes

- `PSUpdateOptimizer.builderMaps` remains intentionally heterogeneous (`Map<Object,Object>`) because keys are both `String` and `PSBackEndTable` and values are both builders and lists — documented; casts localized with `@SuppressWarnings("unchecked")` where necessary.
- `PSDtdRelationalMapper.TableDef.initResultSet` now builds a proper `HashMap<String,Integer>` name-to-index map matching `PSResultSet.setResultData` (previous raw clone of column-def map was type-incorrect; `setMetaData` still rebuilds the map from metadata afterward).
- `PSQueryOptimizer.validateJoins` collapsed to a single `List<?>` overload to avoid ambiguity with `PSCollection` (which implements List).

### Tests run

- Focused: PSDtdRelationalMapperTypedTest, PSUpdateOptimizerTypedTest, PSRequestLinkGeneratorTypedTest (+ prior typed batch tests) — green
- Module: `cd system && ../mvnw.cmd clean install` (see PR evidence)

> Co-Authored by Grok Build using grok-4.5 with agent main.

