# Research: Migrate Home/CUI and Widget Builder to React

**Feature**: 989-react-cui-widget-builder  
**Date**: 2026-07-17

## R1 — UI framework and mount pattern

**Decision**: Use existing Track B stack—React 19 + TypeScript + Vite, `window.PercModernUI.mount(containerId, componentName, props)` from `WebUI/src/main/ts/bridge.ts`, components registered in `registry.ts`. Build output base `/cm/modern/` (`vite.config.ts` → `target/generated-webui/cm/modern`).

**Rationale**: Already production-proven for Dashboard widgets; WebUI AGENTS migration approach; CSRF-aware `api/client.ts` exists; avoids introducing a second SPA framework (spec assumption).

**Alternatives considered**:
- Full React Router SPA replacing `index.jsp` dispatcher — rejected for this feature (scope creep; remaining ~20 jQuery screens still need dispatcher).
- Rewrite classic `home.jsp` in place — rejected by clarify FR-017 hard cut.
- Keep CUI iframe and only restyle — rejected (does not remove Knockout).

## R2 — Home information architecture

**Decision**: Single React `HomeShell` with section routes: **Recent**, **Library**, **Search**, **Create**. Map legacy `initialScreen` values `library | list | search | newitem` to those sections (exact mapping table in `contracts/home-deep-links.md`).

**Rationale**: Spec FR-002 / clarify session; eliminates dual library-mode vs contributor-iframe shells.

**Alternatives considered**: Preserve dual-mode toggle — rejected in clarify.

## R3 — Home data / service access

**Decision**: Call the **same backend REST** used today by `PercContributorUiAdaptor` and CUI widgets, via typed TypeScript API modules (not `window.parent.jQuery` after cutover). Use existing session cookies + CSRF headers from `api/csrf.ts` / `api/client.ts`.

**Rationale**: Spec requires parity without embedded SPA; adaptor documents operations (recent, sites, folders, create page/asset, search, bookmarks, templates). Server remains system of record.

**Alternatives considered**:
- Keep parent-page jQuery adaptor bridge permanently — fragile, blocks US3 CUI/iframe removal.
- New REST aggregation BFF — out of scope unless gaps found during implementation.

**Open implementation note** (not a product ambiguity): Implementers must inventory exact REST URLs from `PercContributorUiAdaptor.js` / `perc_path_constants.js` when coding each section; contracts describe capability groups, not every path string.

## R4 — Widget Builder server contract

**Decision**: Reuse `com.percussion.widgetbuilder.service.PSWidgetBuilderService` (`@Path("/widgetbuilder")` under widget management). Client paths (from `perc_path_constants.js`):

| Operation | Method | Path constant |
|-----------|--------|---------------|
| Summaries | GET | `{SERVICES_ROOT}/widgetmanagement/widgetbuilder/summaries` |
| Full definition | GET | `.../widgetbuilder/definition/{id}` |
| Save | POST | `.../widgetbuilder/definition/` |
| Validate | POST | `.../widgetbuilder/validate/` |
| Deploy | POST | `.../widgetbuilder/deploy/{id}` |
| Delete | DELETE | `.../widgetbuilder/definition/{id}` |
| Active flag | GET | `.../widgetbuilder/active` |

**Rationale**: Spec FR-006; server validation and package generation stay authoritative; last-write-wins matches typical POST save without ETag (FR-015).

**Alternatives considered**: GraphQL or new versioned API — unnecessary risk.

## R5 — Entry points and hard cut

**Decision**:
- Keep view keys `home` and `widgetbuilder` in `index.jsp` for nav stability (`PercNavigationManager.VIEW_HOME` / `VIEW_WIDGET_BUILDER`).
- Point them at **new** thin shell JSP filenames (e.g. `homeModern.jsp`, `widgetBuilderModern.jsp`—final names chosen at implement time).
- **Delete** classic `home.jsp`, `widgetBuilder.jsp` (and `cm/pages/app` mirrors) in US3; do not leave redirect-only stubs.
- Deep links: implement mapping in dispatcher/query handling / modern props so known URLs work without classic files (FR-013).

**Rationale**: Clarify option C hard cut + deep-link policy already locked.

**Alternatives considered**: In-place rewrite of classic JSPs — rejected.

## R6 — Dual webapp trees and war/

**Decision**: Treat `WebUI/src/main/webapp/cm/app` and `cm/pages/app` (and `cui` / `pages/cui`) as **both** requiring rewire/delete per inventory. Treat `WebUI/war/**` as build/sync output—ensure packaging does not ship deleted trees; do not invent a third source of truth.

**Rationale**: Repo currently mirrors paths; incomplete cleanup would leave classic UI in some deployments.

## R7 — Removal scope and orphan vendors

**Decision**: Exclusive delete CUI SPA + WB client + classic entry JSPs + CUI-local vendors. Run **manual** inventory for Backbone, Underscore, Backgrid (and similar). Remove only with zero remaining product consumers. **Keep** platform jQuery and `perc_widget_library` (referenced by `webmgt.jsp` / `editAsset.jsp`).

**Rationale**: Clarify B + C + inventory A; out-of-scope full jQuery retirement.

**Alternatives considered**: CI hard absence scan — rejected (manual inventory only).

## R8 — Testing strategy

**Decision**: Vitest + Testing Library under `WebUI/src/test/ts/{home,widgetbuilder}/`. Delete `percWidgetBuilderDefinitionView.test.js` / `percWidgetFieldsViews.test.js` when clients die; replace with modern coverage same release (FR-018). Service-layer Java tests in sitemanage remain valid for server.

**Rationale**: Constitution test discipline + clarify A.

## R9 — Delivery sequencing

**Decision**: Prefer constitution story checkpoint—**US1 PR → US2 PR → US3 PR** on feature branch; release only when all land and removal inventory signed. No production dual-path toggle.

**Rationale**: Big-bang release semantics without one mega-PR if team prefers smaller reviews.

## R10 — i18n and a11y

**Decision**:
1. **TMX tables are mandatory** for user-visible Home/WB chrome (FR-021)—not English-only React hardcoding and not “optional where practical.”
2. **Reuse** existing `perc.ui.*` keys when semantics match; **add** net-new units to product UI TMX (typically `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx`) with **structural locale parity** (same `xml:lang` set as neighbors, e.g. en-us/es/hi); non-en may temporarily copy en-us (FR-022).
3. **Runtime**: thin modern shell JSPs load `tmx.jsp?mode=js&prefix=perc.ui.&sys_lang=…`; React uses `I18N.message(...)` or a thin TS wrapper over that global (FR-023). No second i18n framework; no build-time-only catalog as sole runtime source.
4. **Proof**: SC-008 / FR-024 **manual key-presence checklist** (`checklists/i18n-key-checklist.md`); Vitest multi-locale / `I18N` mock assertions are **not** required for i18n acceptance.
5. **a11y**: keyboard-operable primary actions per FR-012 (same bar as modern CM UI).

**Rationale**: Clarify session (i18n); constitution VIII; existing WebUI `I18N.message` + TMX pipeline.

**Alternatives considered**: Match current React Dashboard English hardcoding — rejected by clarify; new React i18n library — rejected; full multi-locale CI — rejected (manual SC-008).
