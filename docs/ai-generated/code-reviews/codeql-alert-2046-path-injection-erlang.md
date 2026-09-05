# Erlang review — fix/codeql-alert-2046-path-injection

**Date:** 2026-09-05  
**Base:** origin/main  
**Recommendation:** approve  
**Gate:** pass  
**May commit/push:** yes

## Summary

Runtime `requireFileUnderRxDir` before save Application File API (CodeQL #2046–#2050). Tests extended. Sink-line + existing path query-filter.

## Scope

Cross-platform path review: `requireUnderBase` + `Path`/`Files` in tests. No hardcoded OS separators.

## Issues

None.
