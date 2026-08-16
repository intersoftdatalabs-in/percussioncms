# Erlang review — fix/issue-3469-home-create-asset-editor

**Date**: 2026-08-16
**Scope**: uncommitted vs `origin/main` (`d0838a584f`)
**Reviewer**: Erlang
**Memory patterns hit**: change-class completeness (WebUI + Playwright + product-docs); behavioral tests for itemmanagement create + React host opener; no leftover `editAsset.jsp`

## Summary

Home → Create → Asset creates a stub via `POST /services/itemmanagement/item/create` (`contentTypeName` preferred) and opens `spa.jsp?entry=editor`. It no longer assigns `window.location` to leftover `editAsset.jsp` / `?view=editor`. TinyMCE/file/image chrome remains #3470. `editAsset.jsp` is not deleted (#3473). Explorer `selection.ts` untouched (#3477).

## Recommendation

`approve`

## Gate

May commit/push: **yes**

## Cross-platform path review

CMS folder paths stay `/` repository URLs (`normalizeCmsPath` / `joinFolderAndName`). No OS filesystem I/O. Clean.

## Change-class closure

| Companion | Status |
|-----------|--------|
| WebUI AssetWizard + create/open host | yes |
| Vitest (AssetWizard opener + CreateWizard) | yes |
| Playwright (`home-react-editor.spec.js` asset path) | yes |
| product-docs 8.2 Home/Create + getting-started + rest | yes |

## Issues

None blocking.

## Tests run

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests 2588 passed
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` — BUILD SUCCESS
- QA H2 Playwright: `npm run test:surface -- --path tests/home-react-editor.spec.js` — 1 passed (asset create), 1 skipped (no recent row)
