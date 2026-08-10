# Erlang review: issue #2298 data / data.jdbc rawtypes batch

**Reviewer:** Erlang (strict independent)  
**Date:** 2026-08-07  
**Branch:** `fix/issue-2298-data-jdbc-rawtypes`  
**Base:** `origin/main`  
**Recommendation:** **approve**  
**Gate:** **May commit/push: yes**

## Summary

Parameterizes raw collections and maps in the metadata cluster of `com.percussion.data` and a small `data.jdbc` DSN reader. Real generics preferred over `@SuppressWarnings`. Behavioral unit tests cover type-map loading, DSN parsing, and the case-insensitive column index algorithm. Module `system` `mvnw clean install` green.

## Scope

|                                Path                                |                                            Change                                             |
|--------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `system/src/main/java/com/percussion/data/PSDatabaseMetaData.java` | Typed maps/lists/sets; `Map<String,PSDataTypeInfo>` API; `Comparable<ShortTableInfo>`         |
| `system/src/main/java/com/percussion/data/PSTableMetaData.java`    | Typed column/key/type maps; `loadDataTypes` → `Map<String,Integer>`; manual `findColumnIndex` |
| `system/src/main/java/com/percussion/data/PSMetaDataCache.java`    | `Iterator<?>` for table refs                                                                  |
| `system/src/main/java/com/percussion/data/jdbc/PSDsnReader.java`   | `ArrayList<String>` + try-with-resources close                                                |
| Tests                                                              | `PSDatabaseMetaDataTypeMapTest`, `PSDsnReaderTest`, fold-test update                          |
| This report                                                        | Durable Erlang artifact                                                                       |

**Prior / patterns:** Aligns with #2295 / #2296 rawtypes batches (prefer real generics; ≤~40–50 diags; residual child issues).  
**Cross-platform path review:** PSDsnReader uses `java.io.File` + `FileReader` (platform path strings). Tests use `Path` / `@TempDir` / `Files.writeString` — portable. No hardcoded `C:\` or Unix-only absolutes.

## Issues

_None at bug severity._

### suggestion (non-blocking)

1. **`PSTableMetaData` ShortTableInfo-style schema NPE** (`PSDatabaseMetaData.ShortTableInfo#compareTo`) — if both schemas are null, fall-through still calls `m_schema.compareTo` (pre-existing). Out of scope for rawtypes slice; optional follow-up.

2. **Package residual** — large rawtypes inventory remains in `data` / `data.jdbc` (optimizers, SQL builders, file-system JDBC meta, etc.). Track under parent #2022 residual slice; do not expand this PR.

### nit

1. `new String(key)` removed in `loadDataTypes` in favor of reusing the map key — equivalent for immutable `String` keys.

## Verification

|                  Check                   |                                            Result                                             |
|------------------------------------------|-----------------------------------------------------------------------------------------------|
| Focused tests                            | `PSDatabaseMetaDataTypeMapTest`, `PSDsnReaderTest`, `PSTableMetaDataIdentifierFoldTest` green |
| `cd system && ../mvnw.cmd clean install` | BUILD SUCCESS                                                                                 |
| Module surefire                          | failures=0, errors=0                                                                          |
| Touched production files                 | 0 residual rawtypes/unchecked on compile log paths                                            |

## Gate

No bugs, behavioral tests present for changed logic, portable I/O. **approve**.

> Co-Authored by Grok Build using grok-4.5 with agent main.

