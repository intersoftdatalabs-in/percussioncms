# Erlang review — issue 3362 (Copy Folder sourcePath HTTP 400)

## Summary

Explorer Copy posted a bare `{sourcePath, targetPath, copy}` object to
`POST /pathmanagement/path/moveItem`. CXF JAXB expects root `MoveFolderItem`
with `itemPath` + `targetFolderPath`. `PSMoveFolderItem` has no `copy` field.

The SPA now wraps move bodies like legacy `PercPathService` and routes Copy
to public REST `POST /rest/folders/copy/folder` (`CopyFolderItemRequest`).

## Scope

- Uncommitted work on `fix/issue-3362-copy-folder-sourcepath` vs `origin/main`
- Modules: `WebUI`, `rest` (CopyFolderItemRequest root name), `projects/sitemanage` (marshal tests), `modules/perc-qa-automation`, `product-docs/8.2`
- Memory patterns: JAXB WRAP_ROOT_VALUE / unexpected element (peer #3360/#3361)
- Cross-platform path review: no new filesystem path joins; URL paths use `/` (correct for REST)

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None at bug severity.

- suggestion: clipboard paste for pages still always hits page-copy even on cut (pre-existing). Out of scope for #3362.
- nit: `PSMoveFolderItem.copy` remains a client-only router flag; documented not to serialize.

## Evidence

- rest `mvnw clean install`: BUILD SUCCESS, Tests run: 423 (CopyFolderItemRequestSerialDeserialTest 5)
- sitemanage `mvnw clean install`: BUILD SUCCESS, Tests run: 1156, Failures: 0 (PSMoveFolderItemJacksonTest 5)
- WebUI `mvnw clean install`: BUILD SUCCESS, Vitest 2418 passed
- Playwright C5: 4 passed (`bug-3362-copy-folder-envelope` + `explorer-subfolder-copy`)
