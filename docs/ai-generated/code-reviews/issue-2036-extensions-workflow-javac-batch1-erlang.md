# Erlang review — issue #2036 extensions-workflow javac batch 1

**Date:** 2026-08-07  
**Branch:** `fix/issue-2036-extensions-workflow-javac-batch1`  
**Reviewer persona:** Erlang (strict pre-commit)

## Summary

Clears **all 54** module javac diagnostics (main+test under `-Xlint`) in
`modules/extensions-workflow` with real fixes (generics/typed helpers, `final`
for this-escape, `serialVersionUID`, `Calendar` static qualification, typed
test fixtures). No blanket `@SuppressWarnings` added for rawtypes/unchecked.

## Scope

- Module: `modules/extensions-workflow` only (no `system` / monorepo-wide churn)
- New: `PSTypedWorkflowLists` + `PSTypedWorkflowListsTest` (7 tests)
- Touched: exceptions (serial), context classes (`final`), `PSWorkflowRoleInfoStatic`, tests
- Prior memory: issue comment on #2036 — replace suppressions with real generics
- Cross-platform path review: **N/A** (no file I/O or path changes)

## Recommendation

**approve**

## Gate

|             Check              |                                      Result                                      |
|--------------------------------|----------------------------------------------------------------------------------|
| Bugs                           | none found                                                                       |
| Behavioral tests for new logic | yes — `PSTypedWorkflowListsTest` (7)                                             |
| Non-portable paths             | none                                                                             |
| Module clean install           | BUILD SUCCESS; Tests run: 67, Failures: 0, Errors: 0, Skipped: 41 (pre-existing) |
| Javac warnings (main+test)     | **0** (baseline was 54)                                                          |
| May commit/push                | **yes**                                                                          |

## Issues

None (bug / suggestion / nit).

### Notes (informational)

- `PSInvalidNumberOfParametersException.getParams()` return type tightened from
  `Object[]` to `String[]` (field was always assigned from `String[]` ctor).
  No in-repo callers of this getter found.
- Typed list helpers intentionally mirror `PSWorkFlowUtils` raw helpers so this
  module is warning-clean without a large `system` install surface. Upstream
  generics on `PSWorkFlowUtils` remain a separate tech-debt opportunity.
- Pre-existing javadoc plugin noise on `PSContentStatusHistoryEntityBuilder`
  and a bad `@link` in `PSContentTypesContext` was already present; not in this
  javac batch scope. Issue acceptance also targets javadoc-zero eventually —
  residual if any javadoc still fails under a dedicated javadoc run should be
  tracked, but this PR zeroes **javac** diagnostics as measured by compiler
  warning lines.

## Build evidence

```
cd modules/extensions-workflow
../../mvnw.cmd clean install   # from repo root: path is ../../mvnw
# BUILD SUCCESS
# Tests run: 67, Failures: 0, Errors: 0, Skipped: 41
# javac [WARNING] *.java lines: 0
```

