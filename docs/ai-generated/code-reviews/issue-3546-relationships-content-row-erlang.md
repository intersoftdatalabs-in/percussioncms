# Erlang review — issue #3546 Relationships panel for content row

**Branch:** `fix/issue-3546-relationships-content-row`  
**Scope:** uncommitted vs `HEAD` (WebUI list/id bind, Explorer shell mount, Vitest, Playwright, product-docs)  
**Date:** 2026-08-17  
**Reviewer:** Erlang (pre-commit)

## Summary

Explorer Relationships stayed hint-only unless `selection.item.type !== "folder"` and `parseExplorerContentId(selection.item.id)` succeeded. Sample-site rows can omit `id`, use a slug (`ci-home`), or wrap a GUID object. This change binds a parseable content/GUID id onto path items at list unwrap (and path lookup on select), mounts the panel for any non-folder with a parseable id, and covers GUID + slug + omitted-id cases in Vitest plus a Playwright spec that requires a `data-row-kind="item"` row.

Memory patterns hit: behavioral tests for new logic; WebUI Playwright companion; product-docs for operator-facing View → Relationships; CMS URL paths correctly use `/`.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs, missing behavioral tests, or non-portable filesystem I/O. Change-class companions present (Vitest, Playwright, product-docs).

## Cross-platform path checklist

- No new OS filesystem joins (`"/"` / `"\\"` concatenation for files)
- CMS Explorer paths remain URL-style `/` (correct)
- Tests do not assert OS-specific absolute paths
- N/A for installers / temp files

## Issues

None blocking.

### nit

- `handleSelectItem` path lookup is best-effort; a rapid second click is ignored via `sameRow`. Acceptable for this slice.

## Tests / companions

- `pathItemId.test.ts` — parse, unwrap, bind (GUID, slug+sys_contentid, folder slug)
- `ContentExplorerShell.test.tsx` — GUID row → panel; slug bind → panel; omitted id + path lookup → panel; existing folder → hint
- `pathApi.test.ts` — paginatedFolder binds `sys_contentid`
- Playwright `explorer-relationships.spec.js` — content row must mount panel (not hint-only)
- `product-docs/8.2/admin/content-explorer.md` — select page/asset; FastForward section folders
