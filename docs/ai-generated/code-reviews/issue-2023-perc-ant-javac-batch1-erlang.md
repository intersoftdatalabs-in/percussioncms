# Erlang self-review — issue #2023 perc-ant javac batch 1

**Date:** 2026-08-07  
**Branch:** `fix/issue-2023-perc-ant-javac-warnings-batch1`  
**Module:** `modules/perc-ant`  
**Verdict:** **Approve** (commit-ready)

## Scope

PR-sized batch 1: real-fix raw types, redundant casts, and unchecked calls under project `-Xlint` / `-Xlint:-deprecation` (~50 diagnostics). No behavior change intended beyond type parameters and removal of redundant casts.

**In batch:** top-level ant helpers, help-hint / helptopic mapping tasks, install tasks (copy, extract jar, war update, table/data/rxfix/secure property/page tags/derby, etc.).

**Out of batch (residual on #2023):** `PSJunitFileSelector` (~15), `PSRxBuildInput` (~19), constructor `this-escape` sites (~6).

## Checklist

| Gate | Result |
|------|--------|
| Bugs in typed refactors | None found — collections already held the typed elements; only declarations/casts updated |
| Portable paths / file I/O | N/A for this batch (no path-string changes) |
| Behavioral unit tests | Existing module suite green (39 tests); no new non-trivial logic |
| Prefer real fix over suppress | Yes — no new `@SuppressWarnings` |
| Standalone `mvnw clean install` | BUILD SUCCESS |
| Scope confined to perc-ant | Yes |

## Notes

- Jericho HTML 2.1 APIs return raw `List` / `Iterator`; callers use enhanced-for + local cast to `StartTag`/`Tag` (no unchecked conversion to `List<StartTag>`).
- `PSProperties.getProperty` already returns `String` — redundant casts removed.
- `PSJdbcTableSchema.getKeyColumns()` / `PSJdbcTableData.getRows()` / `PSRxFixCmd.getResults()` already parameterized upstream.
- Residual `this-escape` and large GUI/JUnit selector files deferred deliberately for PR size.

## Residual issue

Follow-up residual issue to be filed for remaining ~40 main-source diagnostics (batch 2+).
