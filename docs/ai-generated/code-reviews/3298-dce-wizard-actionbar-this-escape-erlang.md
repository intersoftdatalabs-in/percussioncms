# Erlang review — #3298 DCE wizard + ActionBar this-escape/serial

**Scope:** uncommitted / branch `fix/issue-3298-dce-wizard-actionbar-this-escape` vs `origin/main`  
**Module:** `modules/DesktopContentExplorer`  
**Date:** 2026-08-13  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** final + serialVersionUID + transient for Swing this-escape/serial (PSFolderDialog / #3288 cluster); do not finalize abstract bases that still have product subclasses.

## Summary

PR-sized Xlint cluster: wizard shell (`cx.PSWizardDialog`, `wizard.PSWizardDialog`, `PSWizardCommandPanel`, `PSWizardPanel`, `PSWizardStartFinishPanel`, `PSWizardValidationError`) plus `PSActionBar` and one leftover leaf (`PSCopySiteNamePage`). Leaf Swing types are `final` with `serialVersionUID`; session collaborators and `IPSWizardPanel` maps are `transient`. Abstract `PSWizardPanel` stays open; `initPanel` is `final`. No product behavior / DCE UX change. Standalone `mvnw clean install` **BUILD SUCCESS**, Tests run: 187, Failures: 0. Cluster files have 0 new javac warnings.

## Cross-platform path checklist

N/A — no path or file I/O changes.

## C2 blast radius

Types made `final`: both `PSWizardDialog`s, `PSWizardCommandPanel`, `PSWizardStartFinishPanel`, `PSWizardValidationError`, `PSActionBar`, `PSCopySiteNamePage`. Protected `PSWizardPanel.initPanel` made `final`. Grep monorepo: no `extends` / anonymous subclasses of those types. DCE is the consumer; no extra reverse-dep install.

## Issues

None (hard-gate).

## Suggestions

- Remaining copy-site wizard pages and `PSContentExplorerMenu` still have this-escape/rawtypes; tracked on parent #2045, not this slice.
- `PSRelationshipInfoSet.getComponents()` remains a raw `Iterator`; ActionBar uses a local unchecked cast (system API out of scope).
