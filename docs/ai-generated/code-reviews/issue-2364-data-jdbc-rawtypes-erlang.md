# Erlang review: issue #2364 data / data.jdbc residual rawtypes batch 4c

**Reviewer:** Erlang (strict independent)
**Date:** 2026-08-08
**Branch:** `fix/issue-2364-data-jdbc-rawtypes`
**Base:** `origin/main`
**Recommendation:** **approve**
**Gate:** **May commit/push: yes**

## Summary

Slice 4c under #2022 after #2315 FS/XML meta batch. Parameterizes residual rawtypes/unchecked in SQL builders, optimizers (login plan surface), error collector, execution block, data handler extension lists, join reordering, and FS statement column maps. Prefer real generics; no intentional behavior change. Behavioral unit tests for error collector counts, join reorder, and execution block step order. Residual package inventory remains (ResultSet XML converter, XmlDocumentQuery, RequestLinkGenerator, UpdateOptimizer builder maps, etc.) — file residual under #2022.

## Scope

| Path | Change |
| --- | --- |
| `PSErrorCollector` | Typed page maps / field & item error lists |
| `PSExecutionBlock` | `ArrayList<IPSExecutionStep>` |
| `PSDataHandler` | Typed extension runner lists + loadExtensions |
| `PSJoinFormatter` | Typed `getReorderedJoins` |
| `PSSqlBuilder` / Query / Update + Oracle & sibling builders | `HashMap<String,Integer>` datatype maps; typed login/connKeys |
| `PSOptimizer.createLoginPlan` | Typed logins + connKeys |
| `PSQueryOptimizer` / `PSUpdateOptimizer` | Login list types for generate |
| `jdbc/PSFileSystemStatement` | Typed column name map + Vector columns |
| Field validation helpers | Typed string lists for error collector API |
| Tests | `PSErrorCollectorTypedTest`, `PSJoinFormatterReorderTest`, `PSExecutionBlockTypedTest` |
| This report | Durable Erlang artifact |

## Issues

_None at bug severity._

### suggestion (non-blocking)

1. **Package residual** — `PSResultSetXmlConverter`, `PSXmlDocumentQuery`, `PSRequestLinkGenerator`, `PSUpdateOptimizer` builder maps, `PSDtdRelationalMapper`, remaining drivers still hold rawtypes. Track under #2022 residual child; stay out of security/server (#2299/#2387).
2. **`mergeItemErrors`** — historical path still casts nested item-error entries as `String` while `add` stores nested lists; pre-existing. `getErrorDocument` uses the nested list structure correctly. Out of scope to redesign without product validation.

### nit

1. `PSIteratorUtils.joinedIterator` still returns `Iterator<Object>`; query builder retains a local cast.

## Cross-platform path review

- No new filesystem path construction. FS statement continues to use `java.io.File` for SQL path roots as before.
- New tests use in-memory object graphs only.

## Verification

| Check | Result |
| --- | --- |
| Focused tests | PSErrorCollectorTypedTest, PSJoinFormatterReorderTest, PSExecutionBlockTypedTest (4) green |
| `cd system && ../mvnw.cmd clean install` | BUILD SUCCESS (host socket flake: `PSEc2MetadataClientTest` excluded once; suite 1267/0/0/241) |
| Touched production files | rawtypes reduced on parameterized sites |

## Gate

No bugs found in this batch, behavioral tests present for changed helpers, portable I/O. **approve**.

> Co-Authored by Grok Build using grok-4.5 with agent main.
