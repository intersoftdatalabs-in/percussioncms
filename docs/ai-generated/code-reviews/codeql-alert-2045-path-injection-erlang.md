# Erlang review — fix/codeql-alert-2045-path-injection

**Date:** 2026-09-05  
**Base:** origin/main  
**Recommendation:** approve  
**Gate:** pass  
**May commit/push:** yes

## Summary

Runtime + tests for `IOTools.getFileContent` (CodeQL #2045): NUL reject, canonical FileInputStream, `getFileContentUnderBase` via `requireUnderBase`, sink-line, query-filter, model pack.

## Scope

Cross-platform path review: tests use `Path` / `Files`; no hardcoded OS separators.

## Issues

None.
