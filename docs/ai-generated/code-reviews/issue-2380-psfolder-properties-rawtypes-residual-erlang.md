# Erlang self-review — issue #2380 residual (PSFolderPropertiesPanel rawtypes)

**Date:** 2026-08-10  
**Branch:** fix/issue-2380-psfolder-panels-rawtypes  
**Module:** modules/DesktopContentExplorer  
**Verdict:** PASS

## Scope
Clear last residual rawtypes/unchecked in `PSFolderPropertiesPanel` after merged PR #2438 left Security/General at 0 Xlint diags and Properties with one `Vector` rawtypes from `DefaultTableModel#getDataVector()`.

**Out of scope:** `PSFolderAclEditorDialog` (owned by open #2439).

## Findings
| Severity | Finding | Disposition |
|----------|---------|-------------|
| — | none | — |

## Checklist
- [x] No product behavior change (same name/value/desc read path via TableModel#getValueAt)
- [x] No non-portable path/file I/O
- [x] Unit test for `stringValue` helper + existing `stringCell`
- [x] Module standalone `mvnw clean install` BUILD SUCCESS — Tests run: 101, Failures: 0
- [x] Direct javac `-Xlint:rawtypes,unchecked` on Properties panel: 0 warnings
- [x] Security/General panels already 0 diags (verified); not re-churned

## Companions
- Change class: Swing rawtypes residual polish (typed table row access + helper)
- Peers: prior #2438 helpers on Security/General panels
