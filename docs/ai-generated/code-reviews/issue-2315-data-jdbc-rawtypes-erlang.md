# Erlang review: issue #2315 data / data.jdbc residual rawtypes batch

**Reviewer:** Erlang (strict independent)
**Date:** 2026-08-07
**Branch:** `fix/issue-2315-data-jdbc-rawtypes`
**Base:** `origin/main`
**Recommendation:** **approve**
**Gate:** **May commit/push: yes**

## Summary

Slice 4b after #2298/#2314. Parameterizes residual rawtypes/unchecked in the JDBC metadata ResultSet construction cluster: `PSResultSet` (typed column map + list API), `PSFileSystemDatabaseMetaData`, and `PSXmlDatabaseMetaData`. Real generics preferred over package-wide `@SuppressWarnings`. Behavioral unit tests cover typed ResultSet construction and FS metadata table-types/primary-keys ResultSets. Module `system` `mvnw clean install` green.

Also unblocks a **pre-existing** main compile break: `PSNavonNodeInvocationHandler` could not construct `HashMap` from `PSItemIterator#getMap()` after utils typing returned `Map<?,?>`. Minimal cast copy only; behavior unchanged.

## Scope

| Path | Change |
| --- | --- |
| `system/.../data/PSResultSet.java` | `HashMap<String,Integer>` name map; `List<?>[]` / `List<Object>[]` column data; typed getters |
| `system/.../data/jdbc/PSFileSystemDatabaseMetaData.java` | Typed ArrayLists/HashMaps for getTables/getCatalogs/getTableTypes/getColumns/getPrimaryKeys; typed Comparator |
| `system/.../data/jdbc/PSXmlDatabaseMetaData.java` | Same pattern for getTables/getTableTypes/getColumns helpers + cgi static lists; typed Comparator |
| `system/.../nav/PSNavonNodeInvocationHandler.java` | Unchecked cast of `getMap()` for typed HashMap copy (build unblocker) |
| Tests | `PSResultSetTypedConstructionTest`, `PSFileSystemDatabaseMetaDataTypedTest` |
| This report | Durable Erlang artifact |

## Issues

_None at bug severity._

### suggestion (non-blocking)

1. **Package residual** — optimizers, SQL builders, `PSXmlDocumentQuery`, `PSErrorCollector`, `PSRequestLinkGenerator`, and remaining drivers still have large rawtypes inventory. Track under #2022 residual after this slice; do not expand this PR into #2299 packages.

2. **PSItemIterator API** — prefer `Map<String,M>` (or dual accessors) in utils so the nav cast can go away in a dedicated utils/system companion PR.

### nit

1. `setResultData` casts `List<?>[]` → `List<Object>[]` for mutators; array erasure makes this safe at runtime for the existing callers. Documented in-code.

## Cross-platform path review

- FS/XML metadata continue to use `java.io.File` / `FileFilter` (platform path strings). No new hardcoded `C:\` or Unix-only absolutes in production code.
- Unit tests for primary keys pass catalog string through as opaque ResultSet cell (no filesystem I/O). Temp paths not required for this batch's new tests.

## Verification

| Check | Result |
| --- | --- |
| Focused tests | `PSResultSetTypedConstructionTest` (2), `PSFileSystemDatabaseMetaDataTypedTest` (2) green |
| `cd system && ../mvnw.cmd clean install` | BUILD SUCCESS |
| Module surefire | failures=0, errors=0 |
| Touched production files | residual rawtypes/unchecked cleared on parameterized sites |

## Gate

No bugs, behavioral tests present for changed construction logic, portable I/O. **approve**.

> Co-Authored by Grok Build using grok-4.5 with agent main.
