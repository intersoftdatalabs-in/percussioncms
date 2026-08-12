# Architecture / Navigation — slice checklist (B–G)

**Parent epic:** [#3092](https://github.com/intersoftdatalabs-in/percussioncms/issues/3092)  
**Inventory (Slice A):** [#3093](https://github.com/intersoftdatalabs-in/percussioncms/issues/3093) — see `00-cm1-inventory-api-map.md`  
**Date:** 2026-08-11  

Progress status for overnight runs lives on the **parent GitHub issue body** (`## Agent progress`), not here. This file is a **static implementer checklist** derived from the inventory.

---

## Slice B — SPA shell + routing (#3094)

- [ ] Add `architecture` to `SPA_ENTRIES` / deep-link allowlists  
- [ ] Add `ArchitectureRoute` + lazy `ArchitectureShell` in `routes.tsx` / `registry.ts`  
- [ ] TopNav: replace hard link with `NavLink` to `/architecture`  
- [ ] Role gate: Admin or Designer (match `topNavConfig` / legacy `index.jsp`)  
- [ ] Site picker (or empty state when no sites) inside shell  
- [ ] When ready: `index.jsp` move `arch` from `legacyViews` → SPA redirect; update `spaCutover.test.ts`  
- [ ] Preserve optional `site` query → client path or picker selection  
- [ ] Unit tests for route/gate/nav config peers  

**Module hint:** `WebUI` only.

---

## Slice C — Read-only nav tree (#3095)

- [ ] Typed `sectionApi.ts`: at least `loadTree(siteName)` and/or `loadRoot` + `loadChildSections`  
- [ ] Render accessible tree (role=tree / treeitem, keyboard)  
- [ ] Loading / error / missing-site / bad-site states (map error codes 18001 / 18009 if exposed)  
- [ ] Refresh after site change  
- [ ] Vitest for API client + tree pure helpers  

**REST:** `GET /Rhythmyx/services/sitemanage/section/tree/{siteName}` preferred.

---

## Slice D — Structure editing (#3096)

- [ ] Create regular section (`POST /section/create`)  
- [ ] Edit section properties (`GET properties` + `POST /section/update`)  
- [ ] Move / reorder (`POST /section/move`)  
- [ ] Delete section (`DELETE /section/{id}`)  
- [ ] Convert section → folder (`DELETE /section/convertToFolder/{id}`)  
- [ ] Create from folder (`POST /section/createSectionFromFolder`) if in CM1 parity scope  
- [ ] Optimistic UI or forced refresh; clear client caches  
- [ ] Behavioral tests for mutation mappers / validation  

---

## Slice E — Landing & section-link parity (#3097)

- [ ] Replace landing page (`POST /section/replaceLandingPage`) + page picker pattern  
- [ ] Create / update / delete **section links**  
- [ ] Create / update **external links**  
- [ ] Section tree dialog equivalent for link target / move target  
- [ ] Blog type: document support level (read-only vs full)  

---

## Slice F — a11y + i18n + docs + QA (#3098)

- [ ] TMX keys under `perc.ui.*` (nav, shell, dialogs, errors)  
- [ ] Keyboard tree + focus management + landmarks  
- [ ] `product-docs/8.2/` admin page for Architecture / navigation editor  
- [ ] Playwright smoke under `modules/perc-qa-automation`  
- [ ] Human QA issue when UI is ready (`qa task`, assignee per project rules)  

---

## Slice G — Legacy retirement (#3099)

- [ ] Top-nav and homepage no longer land on `siteArchitecture.jsp` as primary  
- [ ] Redirect or remove JSP + packed architecture bundle when no remaining consumers  
- [ ] Finder/layout coupling to `#perc_site_map` cleaned or guarded  
- [ ] Update SPA cutover tests: `arch` is SPA, not `legacyViews`  
- [ ] Confirm site copy/delete flows still available (Publish / site admin) if removed from Architecture chrome  

---

## API quick reference

Base: `/Rhythmyx/services/sitemanage`

| Method | Path |
|--------|------|
| GET | `/section/root/{siteName}` |
| GET | `/section/tree/{siteName}` |
| POST | `/section/childSections` |
| GET | `/section/{id}` |
| GET | `/section/properties/{id}` |
| POST | `/section/create` |
| POST | `/section/createExternalLinkSection` |
| GET | `/section/createSectionLink/{target}/{parent}` |
| POST | `/section/createSectionFromFolder` |
| POST | `/section/update` |
| POST | `/section/updateSectionLink` |
| POST | `/section/updateExternalLink/{sectionGuid}` |
| POST | `/section/move` |
| POST | `/section/replaceLandingPage` |
| DELETE | `/section/{id}` |
| GET | `/section/deleteSectionLink/{section}/{parent}` |
| DELETE | `/section/convertToFolder/{id}` |
| GET | `/siteArchitecture/{id}` |

> Co-Authored by Grok Build using grok-4.5 with agent main.
