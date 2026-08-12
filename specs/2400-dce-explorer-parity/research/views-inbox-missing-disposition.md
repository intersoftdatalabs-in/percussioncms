# Views + Inbox: unplanned Missing (operator reality-check)

**Parent epic:** [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400)  
**Operator reality-check:** [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102)  
**This docs slice:** [#3108](https://github.com/intersoftdatalabs-in/percussioncms/issues/3108) — gap-matrix rows + this note  
**Sibling slices:** [#3109](https://github.com/intersoftdatalabs-in/percussioncms/issues/3109) (false Present vs Failed QA) · [#3110](https://github.com/intersoftdatalabs-in/percussioncms/issues/3110) (Views/Inbox implement backlog + IA map after product IN)  
**Related inventory:** [saved-search-execute-disposition.md](./saved-search-execute-disposition.md) (Views ≠ saved searches)  
**Disposition date:** 2026-08-11  
**Status:** Matrix rows added as **Missing**. Product has **not** signed IN / OUT / REDESIGN. Do **not** invent OUT without product sign-off.

---

## Executive summary

| Capability | Gap-matrix status (2026-08-11) | Product decision needed |
|------------|--------------------------------|-------------------------|
| **Views (DCE navigation category)** | **Missing** | IN (SPA tree/run design Views) · OUT (Developer-only / other path) · REDESIGN (different IA) |
| **Inbox** | **Missing** | IN (CE Inbox parity under Explorer) · OUT with reason · REDESIGN |

Silent omission is banned under #2400 rules. Until product signs, keep **Missing** and do not claim Present via View-menu chrome or Developer-only catalogs.

---

## Operator symptoms → matrix mapping

Source: [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102) operator reality-check (live SPA Explorer vs DCE day-to-day management).

| # | Operator symptom | Matrix row (status) | Related open QA / bugs (do not re-implement blindly) |
|---|------------------|---------------------|------------------------------------------------------|
| 1 | Left/right panes feel like partial design-system layout, not CE Sites/Assets/folders model | Sites/folders tree + detail list (**Present** on matrix; product may still fail QA) | Hierarchy: [#2989](https://github.com/intersoftdatalabs-in/percussioncms/issues/2989), [#3044](https://github.com/intersoftdatalabs-in/percussioncms/issues/3044), [#3074](https://github.com/intersoftdatalabs-in/percussioncms/issues/3074), QA [#3101](https://github.com/intersoftdatalabs-in/percussioncms/issues/3101) |
| 2 | Folder hierarchy unreliable; Sites/sample content missing or unusable | Same tree/list rows | Same as #1 |
| 3 | Action bar looks like flat label buttons, not nested DCE action manager | Server-driven toolbar (**Present**) | QA [#2783](https://github.com/intersoftdatalabs-in/percussioncms/issues/2783), [#2856](https://github.com/intersoftdatalabs-in/percussioncms/issues/2856), [#2988](https://github.com/intersoftdatalabs-in/percussioncms/issues/2988) · implement #2730 closed |
| 4 | Saved searches not usable as operator feature | Saved searches catalog + run (**Present**) | QA [#2607](https://github.com/intersoftdatalabs-in/percussioncms/issues/2607), [#2645](https://github.com/intersoftdatalabs-in/percussioncms/issues/2645), [#2729](https://github.com/intersoftdatalabs-in/percussioncms/issues/2729) |
| 5 | **Views** system category not visible in SPA Explorer | **Views (DCE navigation category)** → **Missing** (this note) | Not covered by View-menu QA [#2741](https://github.com/intersoftdatalabs-in/percussioncms/issues/2741)/[#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745); Developer Views under broader [#1690](https://github.com/intersoftdatalabs-in/percussioncms/issues/1690) is **not** CE nav parity |
| 6 | No operator-visible **Inbox** | **Inbox** → **Missing** (this note) | No prior matrix row; no implement child until product IN |

**Rule restated from #3102:** Present without closed human QA is not release-ready. Flip or keep Partial until QA passes; do not treat agent merge alone as CE parity. Sibling [#3109](https://github.com/intersoftdatalabs-in/percussioncms/issues/3109) handles false Present reconciliation for rows that already claim Present.

---

## A — Views (DCE navigation category) → **Missing**

| | |
|--|--|
| **DCE source** | `ContentExplorer.xml` system category **Views**; children loaded via `sys_cxSupport/Views.html` (categories 1–4: My / Community / … design views) |
| **Explorer today** | No product-route left-tree (or equivalent) listing/running design **Views**. `ExplorerMenuBar` **View** menu = chrome (Refresh, panel toggles), not the Views catalog. Display formats = column layout only. |
| **Not the same as** | Saved searches (`GET /rest/searches` + execute façade) — design inventory already states *Views are a separate catalog (Developer Views / UI-07)* in [saved-search-execute-disposition.md](./saved-search-execute-disposition.md). Developer module catalogs outside Explorer do **not** satisfy DCE navigation parity. |
| **Matrix status** | **Missing** until product IN / OUT / REDESIGN |
| **Why not invent OUT** | Operators expect CE Views under Explorer for day-to-day work (#3102). OUT requires explicit product sign-off and rationale (e.g. “use Developer only”), same discipline as P-Trans OUT ([p-trans-out-disposition.md](./p-trans-out-disposition.md)). |
| **If product chooses IN** | File PR-sized children under #3110 / #2400: catalog REST if needed, tree node, execute/list results, a11y/i18n, Playwright + human QA. |
| **If product chooses OUT** | Document under gap-matrix **Explicit OUT** with sign-off issue cite; do not leave Missing forever as silent debt. |
| **If product chooses REDESIGN** | Write acceptance for alternate IA (e.g. Views only under Search) before implement. |
| **Do not** | Mark Present because View menu or display formats exist; conflate with saved-search Present; implement SPA Views catalog without product IN. |

### Product decision options (from #3102)

| Option | Meaning |
|--------|---------|
| **IN** | SPA Explorer left tree (or equivalent) lists/runs design Views with DCE-class parity; REST + UI slices; a11y/i18n |
| **OUT** | Explicit product OUT (operators use Developer / other path only) with sign-off note |
| **REDESIGN** | Different IA (e.g. Views only under Search) with acceptance written down |

---

## B — Inbox → **Missing**

| | |
|--|--|
| **DCE source** | Operator **Inbox** in Desktop Content Explorer navigation (assignments / inbox workflow surface) |
| **Explorer today** | No operator-visible Inbox equivalent on `ContentExplorerShell` product route |
| **Matrix status** | **Missing** (row was previously absent — silent omission) |
| **Why not invent OUT** | #2400 requires disposition for DCE capabilities; silent omission is not allowed. Product may still choose OUT with reason, but must sign. |
| **If product chooses IN** | File PR-sized children under #3110 / #2400 (REST/list surface, shell entry, a11y/i18n, Playwright + human QA). |
| **If product chooses OUT / REDESIGN** | Explicit matrix OUT or redesign acceptance; same re-open discipline as other signed OUT rows. |
| **Do not** | Assume workflow transition menus or notifications substitute for Inbox without product mapping; implement without IN. |

### Product decision options (from #3102)

| Option | Meaning |
|--------|---------|
| **IN** | Implement CE Inbox parity under Explorer |
| **OUT** | Explicit OUT with reason and sign-off |
| **REDESIGN** | Different assignment/inbox IA with written acceptance |

---

## Matrix doc updates (#3108)

| Doc | Update |
|-----|--------|
| [gap-matrix.md](../contracts/gap-matrix.md) | Navigation & chrome: **Views (DCE navigation category)** + **Inbox** rows → **Missing**; implementation note 2026-08-11 |
| This research note | Symptoms → matrix → QA/bugs → decision options |
| #3102 / #2400 Agent progress | Slice #3108 status when PR opens |

---

## What this slice does **not** do

- Product IN/OUT/REDESIGN sign-off (human)
- SPA implement of Views catalog or Inbox
- Re-running full Playwright suite
- Flipping other Present rows for Failed QA (see #3109)

---

## Linked trackers (quick index)

| Role | Issue |
|------|-------|
| Grandparent parity program | [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400) |
| Operator reality-check parent | [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102) |
| This matrix docs slice | [#3108](https://github.com/intersoftdatalabs-in/percussioncms/issues/3108) |
| False Present vs Failed QA | [#3109](https://github.com/intersoftdatalabs-in/percussioncms/issues/3109) |
| Views/Inbox implement backlog after product IN | [#3110](https://github.com/intersoftdatalabs-in/percussioncms/issues/3110) |
| Developer post-P0 (Views catalog outside CE — not CE nav) | [#1690](https://github.com/intersoftdatalabs-in/percussioncms/issues/1690) |

---

## Change log

| Date | Note |
|------|------|
| 2026-08-11 | #3108: document Missing for Views + Inbox; map #3102 symptoms to matrix; link open QA without inventing product OUT. |
