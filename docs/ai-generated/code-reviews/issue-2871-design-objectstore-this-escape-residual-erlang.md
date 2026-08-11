# Erlang review — issue #2871 design.objectstore this-escape residual

**Reviewer persona:** Erlang (strict pre-commit)  
**Change class:** design.objectstore this-escape residual batch (final leaf classes + final parent-list helpers)  
**Module:** `system` / perc-system  
**Parent:** #2022 / #2200 · Prior: #2465 (never landed on main; re-applied leaf finals) · PR open thrash note: #2952

## Summary

PR-sized residual after #2465 inventory: make ~50 monorepo-leaf `design.objectstore` types `final`, and seal `PSComponent` / `PSCollectionComponent` parent-list helpers (`updateParentList` / `resetParentList` / `applyId`) so Element/fromXml constructors no longer trip `-Xlint:this-escape` via overridable bookkeeping.

## Checklist

| Gate | Result |
|------|--------|
| Bugs in new logic | None — class-final + helper-final only; no behavior change |
| Behavioral unit tests | Extended `PSDesignObjectStoreThisEscapeTest` (+ CE/FieldSet/role/directory/auth) |
| Cross-platform paths | N/A — no path I/O |
| Product docs | N/A — compiler hygiene / non-operator-facing |
| Subclass blast radius | Grep `extends <Type>` for all finalized types; zero monorepo subclasses |
| Blanket `@SuppressWarnings("this-escape")` | Not used |
| Thrash vs open PRs | Class-declaration-only finals minimize conflict with #2952 parentComponents retypes on same files; base helpers only sealed as `final` (no signature retype) |

## Cannot-finalize (documented residual)

Left non-final with external subclasses: `PSServerConfiguration` (+ legacy), `PSRelationship` (+ AA / RxFix), `PSRelationshipConfig` / `PSRelationshipConfigSet` (+ install upgrade), `PSPipe`, `PSDataSet`, `PSEntry`, `PSConditional`, `PSUrlRequest`, `PSStylesheet`, deployer `PSDependency`. Need private base-load if further this-escape remains.

## Risk notes

- Making types `final` is a binary-compatible change for callers that do not subclass; monorepo has no subclasses of the batch.
- `copyFrom` now uses `applyId` so id assignment during copy is non-virtual (matches #2465 intent).
