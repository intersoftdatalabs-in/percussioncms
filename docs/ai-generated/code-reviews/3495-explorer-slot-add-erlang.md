# Erlang review — #3495 Explorer slot add

**Branch:** `feat/issue-3495-explorer-slot-add`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral unit tests for new logic; WebUI Playwright companion; product-docs for operator-facing Explorer/AA; empty-catch must surface user-facing failure

## Summary

Slice 1 wires `pickSlotDependent` as a real Content Browser waiter in `AssemblyHost` and `ContentExplorerShell`. Dispatch persists via existing `addSlotRelationship`. Folder browse without `slotContextHasSlot` stays needs-slot (not Data Flow navigation).

## Cross-platform path checklist

Not applicable — no filesystem path construction. REST/URL paths correctly use `/`.

## Issues

None that block.

### Suggestion (non-blocking)

`ContentExplorerShell` only receives slot context via props. Live Explorer folder browse will keep showing needs-slot until an AA host injects a slot. That matches the slice: do not invent slot context from a folder.

## Tests

- `slotDependentPick.test.ts` — empty pick, template mapping, type hint, load failure, session cancel
- `actionDispatch.test.ts` — picker cancel does not POST
- `AssemblyHost.slotAdd.test.tsx` — cancel vs successful pick → add
- `ContentExplorerShell.slotAdd.test.tsx` — no slot / cancel / POST
- Playwright `explorer-active-assembly.spec.js` `@aa-slots` add path

## Companions

Vitest + Playwright + `product-docs/8.2/admin/content-explorer.md`. No Java API signature change (C2 none).
