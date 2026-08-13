# Erlang review — #3287 DCE display table + search unchecked

**Scope:** uncommitted `fix/issue-3287-dce-display-search-unchecked` vs `origin/main`  
**Module:** `modules/DesktopContentExplorer`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for new helpers; no path I/O; change-class is local typed-copy helpers (peers: `PSSearchDialog` static helpers + unit tests)

## Summary

Removes leftover `@SuppressWarnings("unchecked")` / `rawtypes` on three Explorer display/search helpers by:

- Reading `PSDisplayFormatTableModel.getData()` via `getValueAt` instead of casting `getDataVector()` rows
- Copying `PSTableSorter.getSortingColumns()` through a typed `List<Integer>` helper
- Copying the raw `getContentIdList()` through a typed helper; dropping obsolete suppressions on already-typed `ms_cxPropSet` / `ms_cxRCPropSet`

No public/protected signatures changed. No UX change. C5/product-docs N/A.

## Cross-platform path checklist

Not applicable — no filesystem path construction or assertions.

## Issues

None.

## Tests

- `PSDisplayFormatTableModelTest.getDataOnEmptyModelReturnsEmptyIterator` plus existing `getData()` populated-row coverage
- `PSMainDisplayPanelTest` for null/empty/skip-non-Integer sort indexes
- `PSExecutableSearchTest` for null-preserving content-id copy and CX vs RC prop-set copies

Standalone `modules/DesktopContentExplorer` `mvnw.cmd clean install`: **BUILD SUCCESS**, Surefire Failures: 0 (including new tests).
