# Erlang review — issue #4600 Explorer copy selected item

Recommendation: **approve** (strict self-review of uncommitted work before PR).

## Scope

Vertical slice: destination picker for Explorer Copy, REST 403/404 mapping for `POST /folders/copy/item`, list/tree refresh (existing shell), Playwright surface + product-docs.

## Bugs

None found. Copy no longer uses `window.prompt` (native dialog) on the product bar. 403/404 are rethrown as `NotAuthorizedException` / `FolderNotFoundException` instead of wrapping as HTTP 500. `FolderNotFoundException` now carries `Status.NOT_FOUND` so `RestExceptionMapper` emits 404.

## Tests

- rest `FoldersTest` copy success / 403 / 404
- sitemanage `FolderAdaptorCopyFolderItemTest` not-admin and missing path
- WebUI picker + `formatCopyItemError` + shell copy-item/folder
- Playwright `explorer-copy-item.spec.js` fills dest picker

## Paths

No new filesystem path concatenation.

## Companions

REST resource + adaptor + WebUI + Playwright + product-docs/8.2/admin/content-explorer.md.

## Nits

Destination picker is a path text field (peer of type picker), not a live tree widget. Acceptable for this slice.

Reviewed by Erlang persona (implementer self-review).
