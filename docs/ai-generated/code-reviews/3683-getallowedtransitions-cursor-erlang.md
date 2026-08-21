# Erlang review: #3683 getAllowedTransitions first-row skip

**Branch:** `fix/issue-3668-explorer-expire-transition`  
**Scope:** Kilo CRITICAL on `populateFromHibernate` cursor vs `while (tc.moveNext())`  
**Recommendation:** approve  
**Gate:** May commit/push: yes

## Summary

`PSTransitionsContext.populateFromHibernate` positions on row 0 (JDBC constructor parity, #3668). `PSWorkflowService.getAllowedTransitions` walked with `while (tc.moveNext())`, skipping the first transition (empty list for single-row Expire).

Fix: `collectAllowedTransitions` uses `isEmpty` then do-while (peer `PSExitAddPossibleTransitions`). Unit tests: single-row included, two-row both included, empty/null → empty list.

## Issues

None (no bugs, missing behavioral tests, or non-portable path/file I/O).

## Tests

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 2294, Failures: 0
- `PSWorkflowServiceCollectAllowedTransitionsTest` 4 pass
- `PSTransitionsContextPopulateFromHibernateTest` 4 pass
