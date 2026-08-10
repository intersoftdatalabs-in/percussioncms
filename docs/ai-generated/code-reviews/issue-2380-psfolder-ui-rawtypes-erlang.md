# Erlang review: issue #2380 PSFolder* UI rawtypes

**Branch:** fix/issue-2380-psfolder-ui-rawtypes
**Module:** modules/DesktopContentExplorer (perc-content-explorer)
**Reviewer persona:** Erlang (independent of implementer)
**Date:** 2026-08-08

## Scope

Parameterize Swing list/combo models and iterators in:
- PSFolderSecurityPanel
- PSFolderGeneralPanel
- PSFolderPropertiesPanel

Residual left: PSFolderAclEditorDialog (~90 diags) — follow-up issue.

## Checklist

- [x] No intentional product behavior change (typing only + extract pure helpers)
- [x] Real generics preferred over class-level suppress
- [x] Cross-platform: no path I/O changes
- [x] Unit tests for pure helpers (ACL sort, display-format comparator, stringCell)
- [x] Module standalone `mvnw clean install` BUILD SUCCESS
- [x] No new production bugs found in typed conversions (instanceof guards on raw upstream iterators)

## Findings

None blocking. Optional residual: PSFolderAclEditorDialog still raw; Security panel still depends on raw getResultAclEntries() until that dialog is parameterized.

## Verdict

**PASS** — ready for commit/PR.
