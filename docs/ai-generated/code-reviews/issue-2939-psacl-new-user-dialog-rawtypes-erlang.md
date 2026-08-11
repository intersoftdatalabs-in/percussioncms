# Erlang review: issue #2939 PSACLNewUserDialog rawtypes residual

**Scope:** `PSACLNewUserDialog`, `PSDisplayFormatOption`, `PSACLNewUserDialogTest`, `PSDisplayFormatOptionTest`
**Date:** 2026-08-11
**Verdict:** PASS

## Bugs
- None found. Generics-only; dialog load/combo selection control flow preserved.
- `JComboBox#getSelectedItem` remains Object-typed in the JDK; casts retained at three call sites (same as pre-change).
- `ProviderType.equals` still uses `appendSuper(Object.equals)` (historic IDE-style equals). Grouping via `List#indexOf` therefore still does not merge same-type providers — behavior preserved; documented in unit test. Not fixed (no product behavior change for this tech-debt slice).
- `groupProvidersByType` extracted from `loadComboBoxes` with same membership/add order.

## Behavioral tests
- `PSACLNewUserDialogTest` (6): null/empty iterator, grouping adds instance per provider (historic equals), getInstance by name / null reject, instances list.
- `PSDisplayFormatOptionTest` (5): empty/have, add/get/remove, path validation, toXml/fromXml round-trip + equals/hashCode, fromXml null reject.
- Swing dialog not unit-instantiated (live applet resources).

## Cross-platform
- N/A (no path/file I/O changes). DOM test fixtures only.

## Change-class companions
- Unit tests for typed helpers/options; module standalone clean install green (130 tests).
- Product docs N/A (compiler tech-debt, no operator surface change).
- No Playwright (no WebUI product screen).
- C2: `ProviderType` made static nested (was non-static); package-visible. No monorepo subclasses. Constructors now take `Iterator<PSSecurityProviderInstanceSummary>` — call site `PSFolderAclEditorDialog` still uses raw `getProviders()` (unchecked conversion only; file not edited — #2439).

## Residual
- Scope files clear of rawtypes on collections/models.
- Avoid #2439 PSFolderAclEditorDialog (In Progress).
- Next PR-sized clusters under #2326: PSSearchDialog, PSOptionManager, etc.
