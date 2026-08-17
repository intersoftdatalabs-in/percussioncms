# Erlang review — #3497 Explorer slot create

**Branch:** `feat/issue-3497-explorer-slot-create`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral unit tests for new logic; WebUI Playwright companion; product-docs for operator-facing Explorer/AA; empty-catch must surface user-facing failure

## Summary

Slice 2 wires `pickSlotCreate` as a real type/template/folder waiter in `AssemblyHost` and `ContentExplorerShell`. Dispatch creates via `createEditorItem` (itemmanagement), POSTs `addSlotRelationship`, then opens `spa.jsp?entry=editor`. Cancel resolves the picker as `null` and does not create or add. Folder browse without `slotContextHasSlot` stays needs-slot (not Data Flow / `editAsset.jsp`).

## Cross-platform path checklist

Not applicable — no filesystem path construction. REST/URL paths correctly use `/`.

## Issues

None that block.

### Suggestion (non-blocking)

`ContentExplorerShell` only receives slot context via props. Live Explorer folder browse will keep showing needs-slot until an AA host injects a slot. That matches slice 1 add and this slice: do not invent slot context from a folder.

## Tests

- `slotCreatePick.test.ts` — missing type/folder, valid pick, templateId 0, session cancel
- `SlotCreateDialog.test.tsx` — apply without folder stays; apply emits pick
- `actionDispatch.test.ts` — picker cancel does not create/POST; create-then-add opens editor
- `AssemblyHost.slotCreate.test.tsx` — cancel vs create + add + editor
- `ContentExplorerShell.slotCreate.test.tsx` — no slot / cancel / create-then-add + a11y
- Playwright `explorer-active-assembly.spec.js` `@aa-slots` create path

## Companions

Vitest + Playwright + `product-docs/8.2/admin/content-explorer.md`. No Java API signature change (C2 none).
