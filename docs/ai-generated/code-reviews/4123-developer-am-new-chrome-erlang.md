# Erlang review — #4123 Developer Action Menus New chrome / dual-ship

Persona: independent reviewer (not implementer). Date: 2026-09-02.

## Change class

Product UI (Developer Action Menus create/delete chrome) + REST JSON bind for Admin `POST /actions` + QA dual-ship gate so `qa-deploy-webui` refuses a stale SPA missing `developer-am-new`.

## Findings

No blocking bugs. Paths in hot-deploy dest are POSIX inside the Linux cell; host src uses `pathlib.Path`. Playwright names are REST-safe (no spaces). JAXB `ActionMenuJsonReader` is listed on `rest-jax-rs` providers before `jacksonProvider` (sitemanage beans + `CatalogRestJaxrsRegistrationTest`).

## Tests

- Rest: `ActionMenuJsonReaderTest`, `ActionMenuCreateCxfUnmarshallTest`
- WebUI Vitest: `ActionMenusPanel` / `ActionMenuDetailPanel` / `actionMenusApi`
- `docker/scripts/test_hot_deploy_webui_modern.py` requires quoted `developer-am-new`
- Playwright: `tests/developer-action-menu-editor.spec.js` (catalog New, create notice + name read-only, system Edit not removed)

## Product docs

`product-docs/8.2/admin/developer-action-menus.md` plus index/rest links.

## Residual

Catalog GET after POST may still miss the row until Hibernate `RXMENUACTION` persist (#4140 / PR #4146). Not in this slice.
