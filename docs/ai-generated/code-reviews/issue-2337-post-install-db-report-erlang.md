# Erlang review: issue #2337 post-install DB verification report

**Branch:** `fix/issue-2337-post-install-db-report`  
**Date:** 2026-08-07  
**Reviewer persona:** Erlang (strict pre-commit gate)  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**

## Summary

Adds a durable, operator-readable post-install verification section for the
selected RDBMS backend after successful silent and interactive installs (parent

# 934 AC-5). Pure formatting from `ResolvedDbConfig` + install path; wired once

in `Main` after ANT success. Unit tests cover embedded H2, external MySQL,
source labels, and password redaction.

## Scope

- `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/PostInstallVerificationReport.java` (new)
- `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java` (emit after success)
- `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/PostInstallVerificationReportTest.java` (new)
- Diff vs `origin/main` limited to the above.

**Cross-platform path review:** `formatEmbeddedPath` uses `Path.toAbsolutePath().normalize().resolve("Repository/CMDB")` — portable; no hardcoded `\`/`C:\` joins. Report uses `System.lineSeparator()`; `emit` splits on `\R`.

**Memory patterns:** secret redaction (never dump full property maps with PWD/cmdb.password); post-success only; non-fatal report failures.

## Issues

None (bug / missing behavioral tests / non-portable I/O).

### Suggestions (non-blocking)

1. **nit** — `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/PostInstallVerificationReport.java`: empty CLI resolve still labels source `structured` (product `DbInstallConfigResolver` behavior). Report surfaces the label faithfully; no change required for AC-5.

## Gates evidence

- `cd modules/perc-distribution-tree && ../../mvnw clean install` → BUILD SUCCESS (242 tests, 0 failures; `PostInstallVerificationReportTest` 7/7).
- Secrets: tests assert operator password and structured MySQL password never appear in report text.
- Silent + interactive share `Main` success path → single emit covers both.

## Recommendation

**approve** — ship as PR Fixes #2337; update parent #934 Agent progress AC-5 row to `pr_opened`.
