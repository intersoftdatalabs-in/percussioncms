# Erlang review — issue #2693 residual data rawtypes batch 4f

**Date:** 2026-08-10  
**Reviewer persona:** Erlang (independent of implementer)  
**Branch:** `fix/issue-2693-data-rawtypes-residual`  
**Module:** `system` / `perc-system`

## Scope

PR-sized residual rawtypes/unchecked cleanup in `com.percussion.data` (+ `macro`) after #2602 / PR #2694, constrained to ≤~40–50 diagnostics (actual inventory drop ~161→94 raw-collection hits in package grep; ~13 production files typed).

## Files changed (production)

| Area | Files |
| --- | --- |
| User context | `PSUserContextExtractor` |
| Query cache | `PSQueryCacher` (`ConcurrentHashMap<String,PSCachedEntry>`, `SortedSet<PSCachedEntry>`, `Comparable<PSCachedEntry>`, typed `expirationBoundary`) |
| Views / params / extractors | `PSViewEvaluator`, `PSHtmlParameterTree`, `PSFunctionCallExtractor`, `PSUrlRequestExtractor`, `PSFunctionBlock` |
| Validation / transactions | `PSValidationRulesEvaluator`, `PSTransactionSet` |
| Columns / events / redirect / macro | `PSStatementColumn`, `PSTableChangeEvent`, `PSRequestRedirector`, `macro/PSMacroUtils` |

## Tests

| Class | Intent |
| --- | --- |
| `PSTableChangeEventTypedTest` | typed column map ctor, defensive copy, validation |
| `PSFunctionCallExtractorTypedTest` | `formatFunctionBody` null/positional behavior after typed collections |
| `PSHtmlParameterTreeTypedTest` | scalar + multi-value HTML param tree generation |

## Gate checklist

| Gate | Result |
| --- | --- |
| Bugs / behavior change | **PASS** — typing only; `PSQueryCacher` aging uses same expiration-time order via typed boundary entry instead of heterogeneous `headSet(Date)` |
| Behavioral unit tests | **PASS** — 3 new test classes; peers + `PSTableChangeDataTest` green |
| Cross-platform paths | **PASS** — no filesystem path construction changes |
| Module standalone clean install | **PASS** — `cd system && ../mvnw.cmd clean install` path: compile green; **Tests run: 1659, Failures: 0, Errors: 0, Skipped: 240**; install green (javadoc skip on reinstall only after full test run) |
| New warnings attributable | **PASS** — no new warnings from edited production files; residual rawtypes remain in unedited jdbc/meta/SQL builders |
| Change-class companions | **PASS** — typed tests + Erlang report; API surface `PSTableChangeEvent` already consumed as `Map<String,String>` |

## Residual (package not zeroed)

Still raw/unchecked-heavy:

- `jdbc/PSXmlDatabaseMetaData`, `jdbc/PSFileSystemDatabaseMetaData`, `PSDatabasePoolDatabaseMetaData`
- Stylesheet cleanup / XSL merger helpers
- Scattered SQL builders / optimizers (`PSSql*`, `PSOracle*`, `PSQueryOptimizer`)
- Minor leftovers (`PSExtensionRunner` API boundary, etc.)

File residual child under parent #2022 / this #2693 for batch **4g**.

## Verdict

**Ship** — no hard-gate findings.
