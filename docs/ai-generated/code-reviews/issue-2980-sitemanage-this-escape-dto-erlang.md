# Erlang-style review — issue #2980 sitemanage this-escape DTO batch

## Scope
PR-sized this-escape residual for `projects/sitemanage` after unchecked/rawtypes (#2895 / PR #2979).
Cluster: data/DTO constructors and related leaf helpers (not full ~55 service/listener set).

## Findings
- **Bugs:** none found. Constructor validation inlined where setters were skipped (name blank, asset type constraints).
- **Behavior:** intentional parity with previous setter logic; exception decorator wrap path preserved with justified `@SuppressWarnings("this-escape")` (Throwable stack publish).
- **Tests:** `PSThisEscapeDtoConstructorTest` (12) covers seeds + validation.
- **Cross-platform:** N/A (no path/file I/O in this batch).
- **API:** `PSAnalyticsQueryResult` made `final`; monorepo grep for `extends PSAnalyticsQueryResult` — none.
  Protected field visibility on summary/user/asset-request bases for subclass ctor assignment only; public setters unchanged.

## Verdict
PASS for commit/PR.

> Co-Authored by Grok Build using grok-4.5 with agent main.
