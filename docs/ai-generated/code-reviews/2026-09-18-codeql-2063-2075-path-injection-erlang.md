# Erlang review — CodeQL #2063–#2075 path-injection residuals

**Date:** 2026-09-18  
**Branch:** `fix/codeql-alert-2063-path-injection`  
**Base:** `origin/main`  
**Reviewer:** Erlang (independent of implementer)

## Summary

Default-branch CodeQL `java/path-injection` alerts #2063–#2073 (`ApplicationFileAdaptor`) and #2074–#2075 (`PSServerXmlObjectStore`) are residuals after ladder steps 1–4 already on `main` (PR #4571 / #4351). This change does **not** re-edit annotated sinks. It adds ObjectStore store I/O tests under a thread RxDir, documents #2074–#2075 in `suppressions.md`, and extends the existing `PSServerXmlObjectStore` query-filter reason.

## Scope

- `projects/sitemanage/src/main/java/com/percussion/apibridge/ApplicationFileAdaptor.java` (package-visible inner store only)
- `projects/sitemanage/src/test/java/com/percussion/apibridge/ApplicationFileAdaptorTest.java`
- `.github/codeql/codeql-config.yml` (reason text)
- `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md`
- Prior memory: T043 path-injection residuals; do not re-fingerprint annotated sinks
- Cross-platform path review: new tests use `Path.of` / `Files` / `@TempDir`; no hardcoded `/` or `\\` filesystem joins; traversal uses `Path.of("..", "escape")`

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None (no `bug` findings).

## Evidence

- `cd projects/sitemanage && JAVA_HOME=/usr/lib/jvm/java-21-openjdk ../../mvnw clean install` → BUILD SUCCESS
- `ApplicationFileAdaptorTest`: Tests run: 36, Failures: 0
- Module: Tests run: 2767, Failures: 0, Errors: 0, Skipped: 125
