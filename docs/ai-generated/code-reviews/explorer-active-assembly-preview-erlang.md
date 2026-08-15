# Erlang review — feat/explorer-active-assembly-preview

**Date**: 2026-08-15  
**Scope**: uncommitted vs `origin/main` on `feat/explorer-active-assembly-preview`  
**Reviewer**: Erlang (independent of implementer)  
**Memory patterns hit**: change-class completeness (WebUI screen + Playwright + product-docs + dual `spa.jsp` + Java `SPA_ENTRIES`); behavioral tests for dispatcher; URL `/` paths are URL not filesystem (false-positive guard)

## Summary

Preview-first Active Assembly: Explorer **Active Assembly** (and template children under those parents) opens a named window on `spa.jsp?entry=assembly&contentId=&templateId=`. The host loads `isAA` page/snippet templates, resolves `GET /services/assembly/preview-location`, and iframes `/assembler/render` under a light overlay. Slot add/create/arrange stay unavailable. Contenteditable is deferred to the Content Editor spec.

Chrome is omitted for `/assembly` inside `AppLayout` so the catch-all `*` cannot swallow the route.

## Recommendation

`approve`

## Gate

May commit/push: **yes**

## Change-class closure

| Companion | Status |
|-----------|--------|
| SPA entry allowlist (TS + both `spa.jsp` + `PSWebUiSpaFallbackFilter`) | Present |
| Chrome-less `/assembly` route + host | Present |
| Dispatcher + parent stamping (Preview vs AA same template name) | Present |
| AA template merge under Item_ActiveAssembly | Present |
| Vitest (dispatch, host, URL, catalog, parseEntry, toolbar parentName) | Present |
| Playwright (`explorer-active-assembly.spec.js` + dispatch blocklist) | Present |
| product-docs `8.2/admin/content-explorer.md` + `developer/rest.md` | Present |
| Spec 996 + action-execution update | Present |
| New files Intersoft 2026 Apache headers | Present |

## Issues

None blocking.

### Suggestion

- `AssemblyHost` imports `parseTemplateIdFromAction` from `actionDispatch`, which pulls Explorer dispatch into the assembly chunk. A later slice can move the parser to a tiny shared module. Not a bug.

### Nits

- Overlay copy is TMX-key fallback (`perc.ui.assembly@…`) until a locale pack lands. Same pattern as Explorer dispatcher keys.
- Live Playwright against QA mode was not run in this review (surface spec is present).

## Cross-platform path checklist

- No new filesystem path joins.
- Assembly / preview hrefs are URL/query paths (`/cm/app/spa.jsp`, `/assembler/render`) — `/` is correct.
- Tests do not assert OS file-separator strings.

## Tests run (implementer)

- Focused Vitest: assembly, actionDispatch, menuCatalogLoad, parseEntryQuery, ActionToolbar, ContextMenu, App — pass
- WebUI standalone `cd WebUI && ../mvnw.cmd clean install` — **BUILD SUCCESS**; Tests run: 2487, Failures: 0. Javadoc/dependency-analyze warnings are baseline (`PSDefaultLandingView`, unused declared deps), not from this change.
