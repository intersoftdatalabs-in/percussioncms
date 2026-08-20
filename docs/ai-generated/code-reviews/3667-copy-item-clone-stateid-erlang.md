# Erlang review — #3667 copy/item NewCopy clone stateId>0

**Branch:** `fix/issue-3667-copy-item-clone-stateid`  
**Base:** `origin/main`  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

## Summary

Explorer Copy of a non-folder POSTs `/rest/folders/copy/item` →
`FolderAdaptor.copyFolderItem` → `contentService.newCopies(..., "NewCopy", false)`.
Clone insert can leave `CONTENTSTATUS.CONTENTSTATEID` 0/null; Hibernate
`PSContentStatusContext.loadFromHibernate` then check-in `commit()` called
`updateContentStatusState` with stateId 0 (`IllegalArgumentException`:
`stateId must be > 0`).

Fix: coerce unset clone state to the workflow **initial** state
(`PSCloneInitialWorkflowState`) on load/commit; null-safe
`PSComponentSummary.getContentStateId()` / `getWorkflowAppId()`; skip
clone auto-checkin (`sys_wfPerformTransition` on state 0); restore
explicit `TYPE_NEW_COPY`; FolderAdaptor treats Spring
`UnexpectedRollbackException` after a persisted clone insert as HTTP 200.
Folder Copy (#3647) and #3656 routing are unchanged. C5: explorer-copy-item
2 passed / 0 skipped on H2 QA.

## Issues

None.

## Tests

- `PSCloneInitialWorkflowStateTest` — positive state left alone; 0/−1 uses
  real `PSWorkflow.setInitialStateId`; null service / missing workflow /
  non-positive initial id keep 0.
- `PSComponentSummaryTest.testNullWorkflowFieldsDoNotUnbox` — null Integer
  columns do not NPE (production `Integer` fields).
- `FolderAdaptorCopyFolderItemTest` — still `copy/item`; relationship type
  is `NewCopy`.

## Cross-platform path checklist

N/A for filesystem I/O. REST/URL paths correctly use `/`. No OS temp or
separator concatenation.

## Memory patterns hit

- Null Hibernate `Integer` columns must not unbox in primitive getters
  (peer: `getNextAgingTransition` / NavTree check-in).
- Test fakes use production types (`PSWorkflow`, not a wrong field type).
- Change-class companions: adaptor test + system unit tests + product-docs
  Copy item cell (operator-facing Explorer Copy).
