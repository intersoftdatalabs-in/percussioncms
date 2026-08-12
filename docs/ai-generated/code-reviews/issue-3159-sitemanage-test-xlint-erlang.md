# Erlang review — issue #3159 sitemanage test-source Xlint residual

**Date:** 2026-08-12  
**Branch:** `fix/issue-3159-sitemanage-test-xlint`  
**Scope:** `projects/sitemanage` **test-source only** project `-Xlint` residual after main-source misc zero (#3107 / open PR #3158)  
**Reviewer persona:** Erlang (independent of implementer)

## Summary

Clears **all 60** test-source project `-Xlint` diagnostics for `projects/sitemanage` with real fixes preferred over blanket suppress. Main sources untouched (open #3158 owns remaining main-source residual on this base). Path-injection security assertions preserved after dropping `sun.misc.Unsafe`.

| Metric | Before | After |
|--------|--------|-------|
| test-source project Xlint | **60** | **0** |
| main-source project Xlint on this base | 40 (owned by #3158) | 40 (untouched) |

## Clusters cleared

| Cluster | Approach |
|---------|----------|
| **this-escape** (REST clients) | Final/safe setters; protected `requestHeaders` / `postContentType` field seeds; `PSRestClient(String)` ctor chain; remove redundant `fillInStackTrace`; justified `@SuppressWarnings("this-escape")` only on intentional Throwable parse ctors |
| **serialVersionUID** | Nested concurrency/exception stubs + dummy validation exceptions |
| **serial-field** | `transient` on parsed `PSErrors` / `PSValidationErrors` exception payloads |
| **unchecked** | Typed `TypeReference` JSON; `Class.cast` / `@SuppressWarnings` on ArgumentCaptor generics; chained `thenReturn` (no generic varargs arrays); typed `IPSGenericDao` mock |
| **heap-pollution** | `@SafeVarargs` + `@SuppressWarnings("varargs")` on `PSTestDataCleaner` |
| **sun.misc.Unsafe** | Real ctors + Mockito for path-injection tests (`PSCloudService` public API; `PSCSSParser` public ctor; `PSSiteDataService` mock shell + private Method.invoke) |
| **static** | `PSThumbnailProcessMonitor.incrementCount/decrementCount` qualified by type name |

## Findings

- **Bugs:** none. Security tests still assert `IllegalArgumentException` for traversal/NUL/separator payloads before File I/O.
- **Behavior:** test-only; no production main-source change; REST client header seeding equivalent.
- **Cross-platform:** path-injection tests keep portable path construction; no hardcoded separators introduced.
- **C2 API shape:** no production types made `final`/sealed; no public production signature changes. Test REST helper visibility changes (`protected` fields, `final` helpers) are test-classpath only.

## Build evidence

```text
cd projects/sitemanage && ../../mvnw.cmd clean install
BUILD SUCCESS
Tests run: 1090, Failures: 0, Errors: 0, Skipped: 125

cd projects/sitemanage && ../../mvnw.cmd test-compile -Dmaven.compiler.showWarnings=true
test-source [WARNING] *.java: 0
```

## Verdict

**PASS** for commit/PR. Residual: none for test-source Xlint on sitemanage; main-source remains #3158 / #3107.

> Co-Authored by Grok Build using grok-4.5 with agent main.
