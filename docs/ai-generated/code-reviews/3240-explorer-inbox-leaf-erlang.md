# Erlang review: #3240 Explorer Inbox leaf

**Branch:** `feat/issue-3240-explorer-inbox-leaf` (stacked on #3116 / PR #3243)  
**Date:** 2026-08-12  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** no blocking bugs; behavioral Vitest + compact Playwright smoke present.

## Summary

Product Explorer Views tree already listed Inbox as a custom-URL leaf and refused execute (#3116). This slice runs Inbox via `POST /services/views/{idOrName}/execute`, keeps the leaf under **Views → My Content** (`//Views//MyContent/Inbox`), adds Inbox icon/testids, injects a stub when the catalog omits Inbox, and leaves other custom-URL views unsupported. Matrix **Present** is not claimed.

## Cross-platform path checklist

- No new filesystem path construction.
- URL/path constants use `/` only (`//Views//MyContent/Inbox`).
- `normalizeInboxToken` treats `\` as `/` for comparison only (not OS I/O).

## Issues

None blocking.

## Tests

- `viewCatalog.test.ts` — Inbox identity, My Content placement, stub inject, `canExecuteView`.
- `ViewsCatalogTree.test.tsx` — icon/testid/path + empty-catalog Inbox + `renderA11yGate`.
- `ContentExplorerShell.test.tsx` — Inbox execute + empty + Outbox still unsupported + `renderA11yGate`.
- Playwright compact: `explorer-views-catalog.spec.js` Inbox click (not slice 3 matrix Present).

## Memory patterns hit

- Change-class companions: product-docs + Vitest + compact Playwright; REST C1 left to #3239.
- Do not invent a second Views tree; compose on V2 groups.
