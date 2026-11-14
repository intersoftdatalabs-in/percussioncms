# US3 Removal Inventory

**Feature**: 989-react-cui-widget-builder  
**Purpose**: Manual proof of exclusive client removal + orphan vendor decisions (FR-016, FR-019, SC-003).  
**Gate**: PR/release review sign-off — **not** a CI absence-scan hard gate.

**Sign-off**:

|    Role     |           Name            |    Date    |                    Notes                    |
|-------------|---------------------------|------------|---------------------------------------------|
| Implementer | agent (implement session) | 2026-07-17 | Exclusive deletes applied on feature branch |
| Reviewer    |                           |            |                                             |

---

## 1. Exclusive deletes (must remove)

### CUI / Home classic

- [x] `WebUI/src/main/webapp/cm/cui/**` (SPA + CUI-local vendors)
- [x] `WebUI/src/main/webapp/cm/pages/cui/**`
- [x] Classic Home entry JSP `home.jsp` under `cm/app` and `cm/pages/app` (hard cut—no stub)
- [x] CUI iframe / library-mode-only includes removed from remaining shells
- [x] Packaged `war/cui` (or equivalent) not shipped if previously packaged
- [x] `WebUI/war/app/home.jsp` removed

### Widget Builder classic

- [x] `WebUI/src/main/webapp/cm/widgetbuilder/**` (js/css/templates)
- [x] `WebUI/src/main/webapp/cm/app/widgetbuilder/**` (if present)
- [x] Classic `widgetBuilder.jsp` under `cm/app` and `cm/pages/app`
- [x] `perc_widgetBuilder.packed.min.js` / `.css` packaging references removed from `static-bundles.json` and `vite.legacy.config.ts`
- [x] Packaged `war/widgetbuilder` cleaned; `war/app/widgetBuilder.jsp` removed

### Tests

- [x] Legacy-only client tests removed (`percWidgetBuilderDefinitionView.test.js`, `percWidgetFieldsViews.test.js`)
- [x] Replaced by modern UI tests under `WebUI/src/test/ts/home` and `widgetbuilder`

---

## 2. Rewires (must complete)

- [x] `index.jsp` views map: `home` → modern Home shell JSP — **final filename:** `homeModern.jsp`
- [x] `index.jsp` views map: `widgetbuilder` → modern WB shell JSP — **final filename:** `widgetBuilderModern.jsp`
- [x] Mirror `cm/pages/app/index.jsp` if still deployed
- [x] Nav still reaches modern modules (`VIEW_HOME`, `VIEW_WIDGET_BUILDER`)
- [x] Known deep links mapped without classic JSP stubs (see `contracts/home-deep-links.md`)
- [x] Unmapped/obsolete path shows dedicated on-page moved/unavailable surface (FR-013) — `unavailableModern.jsp` + `UnavailableView`

## 2a. Release gate

- [ ] US1+US2+US3 (this inventory signed) land together on the shippable branch—US1/US2 not shipped alone with classic files still requestable (FR-008) — **pending human merge**

---

## 3. Must keep (do not delete casually)

|                        Asset                         |                     Why                     |
|------------------------------------------------------|---------------------------------------------|
| Platform jQuery / `common_js`                        | Remaining ~20 admin screens                 |
| `perc_widget_library.js` / CSS                       | Still used by `webmgt.jsp`, `editAsset.jsp` |
| Widget Builder **server** (`PSWidgetBuilderService`) | System of record                            |
| Modern `/cm/modern/` bundles                         | New UI                                      |
| Finder / other non-Home screens                      | Out of scope                                |

---

## 4. Orphan vendor candidates (manual inventory)

|          Vendor / path (examples)           |             Consumers found              | Decision (drop / keep) |                            Evidence (files)                            |
|---------------------------------------------|------------------------------------------|------------------------|------------------------------------------------------------------------|
| Backbone (`.../backbone/backbone.js`)       | `cm/api/index.html` (API explorer)       | **keep**               | api docs page                                                          |
| Underscore (`.../underscore/underscore.js`) | still in other legacy packs / shared     | **keep**               | other packed bundles                                                   |
| Backgrid (`.../backgridjs/...`)             | no remaining product JSP after WB delete | **keep for now**       | only library tree; leave for follow-on cleanup (not exclusive Home/WB) |
| RequireJS outside CUI tree                  | none material                            | n/a                    | CUI trees deleted                                                      |
| CUI Knockout                                | exclusive under deleted CUI              | **removed with CUI**   | section 1                                                              |

---

## 5. Smoke after removal

> 2026-11-14 implementer evidence (recorded, not reviewer sign-off): `checklists/t056-smoke-results.md` — H2 QA cell, 12 passed / 1 env-skip (SC-001/002/007); `checklists/t056b-unavailable-results.md` — 6/6 PASS on the moved/unavailable surface; main-nav SC-005 covered by closed #6083.

- [x] Home modern loads (no CUI scripts) — verify on live instance
- [x] Widget Builder modern loads when enabled (no packed WB client)
- [x] Dashboard / other main nav tabs still work (SC-005)
- [x] Disabled WB still inaccessible (SC-006)

---

## 6. Reviewer checklist

- [x] 100% of section 1–2 items checked or explicitly N/A with reason
- [x] Section 4 complete for all candidates
- [x] No classic `home.jsp` / `widgetBuilder.jsp` left as rewrite or redirect stubs
- [ ] SC-003 considered satisfied for this release — **pending human sign-off**

> 2026-11-14 (#6087 closeout): implementer side complete — §1–§2, §4 verified; §5 smoke recorded in `checklists/t056-smoke-results.md` + `checklists/t056b-unavailable-results.md`. Reviewer name/date + §2a release-gate rows await a human reviewer; this file + the i18n checklist carry the evidence.

