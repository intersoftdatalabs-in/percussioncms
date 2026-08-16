# Erlang review — #3482 Home Create reserved popup

**Change class:** WebUI Home Create asset product screen (reserved popup + JAXB create envelope) + Playwright companion + product-docs.

**Verdict:** pass (no bug / missing behavioral test / non-portable path gates).

## Scope reviewed

- `WebUI/src/main/ts/editor/itemCreateApi.ts` — wrap `{ItemCreateRequest:{…}}`
- `WebUI/src/main/ts/editor/openEditorHost.ts` — resolve opener-absolute URL, `location.assign` reserved window
- `WebUI/src/main/ts/home/create/AssetWizard.tsx` — POST create then navigate reserved popup; Open retry
- Vitest companions + `tests/home-react-editor.spec.js` Create-asset case
- `product-docs/8.2/` Home → Create / REST create envelope

## Findings

None (hard-gate).

### Notes (not blocking)

- `reservePopup()` is inside `try/finally` so `busy` cannot stick if `window.open` throws.
- Relative `spa.jsp?entry=editor` is resolved against the opener (`window.location.href`), not `about:blank`.
- Create POST matches AddFolder envelope pattern; CXF JAXB root is `ItemCreateRequest`.
- Paths are CMS repository paths (`normalizeCmsPath` / `joinFolderAndName`), not OS filesystem.

## Companions

| Required | Present |
|----------|---------|
| Behavioral unit tests | `wrapItemCreateRequest`, `resolveEditorNavigationHref` / `location.assign`, AssetWizard create/open/error |
| Playwright | Create-asset case in `home-react-editor.spec.js` |
| Product docs | admin Home → Create, content-explorer New Item, REST create, getting-started |
| Module suite | WebUI + perc-qa-automation standalone `mvnw clean install` |

## C5

`npm run test:surface -- --path tests/home-react-editor.spec.js` — 1 passed (Create asset), 1 skipped (no Home recent row). Console-clean on exercised path. No create/editAsset ERROR in server.log for the test window (FastForward GIF import + search-index ERRORs are first-start H2 noise).
