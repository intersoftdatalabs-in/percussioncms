# Erlang review: issue #2875 display table/panel/view rawtypes

**Scope:** `PSDisplayFormatTableModel`, `PSMainDisplayPanel`, `PSMainView`, new `PSDisplayFormatTableModelTest`
**Date:** 2026-08-11
**Verdict:** PASS

## Bugs
- None found. Generics-only; `setRoot` / selection / refresh control flow preserved.
- `convertCellValue` extracted with same String casts and NumberFormatException fall-through as historical inline logic.
- `getData()` still uses column 0 for the bulk iterator (historic behavior; row `getData(int)` still uses `m_titleCol`).
- `PSTableSorter.getSortingColumns()` remains raw (ServerUIComponents); call site uses annotated unchecked cast to `List<Integer>` for `PSNode.setLastSortColumns`.

## Behavioral tests
- `PSDisplayFormatTableModelTest` (10): ctor/setRoot null guards, name-column path, display-format number/text conversion + column classes, convertCellValue, getSysTitleIndex without format id, setLocale.
- Swing-heavy `PSMainDisplayPanel` / `PSMainView` not unit-instantiated (headless ActionManager / tree); typing only.

## Cross-platform
- N/A (no path/file I/O changes).

## Change-class companions
- Unit tests for typed model; module standalone clean install green (119 tests).
- Product docs N/A (compiler tech-debt, no operator surface).
- No Playwright (no WebUI product screen change).
- C2: no `final`/`sealed` on public types; method shapes unchanged for callers.

## Residual
- Scope files clean of rawtypes/unchecked under module compile.
- Avoid #2439 PSFolderAclEditorDialog (In Progress).
- Next PR-sized clusters: PSACLNewUserDialog (~20), PSSearchDialog, PSOptionManager / PSDisplayFormatOption, etc.
