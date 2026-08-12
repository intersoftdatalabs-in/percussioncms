# Dual-run: legacy definition-XML runtime shim

| Field | Value |
|-------|--------|
| **Status** | Accepted for Phase 3 slice 3 (#2752); **shim still required** (criteria not met — snapshot **2026-08-12**); [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) **blocked** until M1–M3 + G1–G6 |
| **Parent** | [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) (Phase 3) |
| **Epic** | [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) |
| **Phase 5 criteria** | [definition-xml-shim-removal-criteria.md](./definition-xml-shim-removal-criteria.md) (#2835 / #2632 / criteria refresh #3132) |
| **ADR** | [ADR-004](./adr/004-no-definition-xml-packaging.md) |
| **Code** | `com.percussion.packages.shim.PSLegacyDefinitionXmlShim` (`modules/perc-packages`) |
| **Related slices** | Manifest model #2750; Widget XML compiler #2751; modern-first wire #3024 / PR #3045; gadget metrics #3025 / PR #3071; G4 inventory #3026 / PR #3051; defaults open #3130 / PR #3134; harness open #3131 / PR #3136 |

## Purpose

During the 8.2 dual-run window, **customer** installs may still have Widget / Page / Gadget **definition XML** while product moves to **Component Package Manifest** (`component-package.json`) + CT / template / slot / catalog artifacts.

This document is the **operator policy** for that window. Selection logic is implemented and unit-tested in `PSLegacyDefinitionXmlShim`. Product packages must **not** treat the shim as a permanent authoring path.

## Selection policy (runtime)

Hard order for a package root or definition id:

1. **Modern preferred** — if `component-package.json` is present (Component Package Manifest, schema v1.0), use the modern package. Do **not** prefer co-located legacy XML when modern exists.
2. **Legacy XML fallback** — if modern is absent and legacy Widget / Page / Gadget definition XML is present, load as today.
3. **Neither** — fail with a clear error naming the definition id and expected locations (modern manifest vs legacy `rxconfig/Widgets|Pages|Gadgets` / package staging). Do not invent a silent default.

```text
                    ┌─────────────────────────┐
                    │ Resolve definition id / │
                    │ package root            │
                    └───────────┬─────────────┘
                                │
              modern manifest?  │
                    ┌───────────┴───────────┐
                    │ yes                   │ no
                    ▼                       ▼
         MODERN_COMPONENT_PACKAGE    legacy Widget/Page/Gadget XML?
                                            │
                              ┌─────────────┴─────────────┐
                              │ yes                       │ no
                              ▼                           ▼
                     LEGACY_*_XML          PSDefinitionSourceNotFoundException
```

## Runtime entry points (document)

| Surface | Path / class | Role today | Dual-run note |
|---------|--------------|------------|---------------|
| Widget definitions (install) | `${rxdeploydir}/rxconfig/Widgets/*.xml` via `PSWidgetDao` (`projects/sitemanage/.../dao/impl/PSWidgetDao`) | Loads install Widget XML by file id; dual-run selection via `PSLegacyDefinitionXmlShim` (#3024) | Prefer modern when `widgetDao.modernPackageRoots` has a matching `component-package.json`; selection kind test-visible; XML content remains install wire / customer fallback |
| Package source trees | `modules/perc-packages/src/main/resources/Packages/<pkg>/` | Ships product `.ppkg` content including `sys__UserDependency--rxconfig/Widgets/*.xml` | Product conversion (#2751+) emits modern manifest; batch A dual-ships modern `widgets/<stem>/` (#2831) while install XML remains; product source of truth moves off XML (ADR-004) |
| Package staging Widgets | `sys__UserDependency--rxconfig/Widgets/` or `rxconfig/Widgets/` under a package root | Upgrade-input / deploy layout | Shim package-root API resolves either layout |
| Gadget registry | `WebUI/.../GadgetRegistry` dual-load (`gadget-catalog.json` preferred, `GadgetRegistry.xml` fallback) | Product ships modern catalog; legacy XML kept for dual-run | Prefer modern (#2788); INFO metrics + `getLastLoadSource()` / `getLastLoadEntryCount()` (#3025); do not delete fallback (#2852) |
| Page meta / definition XML | Site/page storage dialects | Composition authoring legacy | Product layout packages (`perc.baseTemplates`, `perc.responsiveTemplates`) author modern `pages/` (#2786); native package install stages archive `TemplateDef-N/` without dual-ship roots (#2806). Shim recognizes `rxconfig/Pages` if present for customer defs |

**Selection API (no Spring wiring required for unit use):**

- `PSLegacyDefinitionXmlShim.selectByPresence(...)` — pure flags (tests / metrics)
- `PSLegacyDefinitionXmlShim.selectForPackageRoot(Path)` — package directory
- `PSLegacyDefinitionXmlShim.selectDefinition(id, modernRoots, widgetsDir, pagesDir, gadgetsDir)` — dual-run by id
- **Production wire (#3024):** `PSWidgetDao.selectDefinitionSource(id)` and poll-time `recordSelectionKinds` call `selectDefinition`; optional Spring `widgetDao.modernPackageRoots` (`File.pathSeparator` list); `getLastSelectionKind()` / `getSelectionKindsById()` for tests and support.

## Time box / deprecation

| Milestone | Expectation |
|-----------|-------------|
| **Now (Phase 3 dual-run)** | Shim available; modern preferred; legacy customer XML still loads |
| **Product packages converted** | Product source trees no longer authored as Widget/Page/Gadget definition XML (#2751 baseWidgets, high-traffic residuals) |
| **Customer conversion** | Operators run Widget XML compiler / upgrade path; measure remaining XML loads |
| **Phase 5 (#2632)** | Remove shim when metrics show zero (or accepted residual) legacy definition XML loads; help rewrite |

**Rules for product engineering:**

1. Do **not** add new product features that **require** definition XML.
2. Widget Builder / Design tools write **modern** format only (ADR-004).
3. Compilers (#2751) read XML as **upgrade input** only — not as ship format.
4. Log or metric `wouldUseLegacyShim` / selection kind when wiring is complete so Phase 5 has exit data.

## Operator dual-run checklist

High-level convert path (all environments):

1. **Inventory** customer Widget XML under install `rxconfig/Widgets` (and package archives if applicable).
2. **Convert** using the Widget XML → Component Package Manifest compiler when available (#2751).
3. **Deploy** modern packages (`component-package.json` + artifacts) beside or instead of XML.
4. **Verify** selection: modern present ⇒ modern wins even if old XML remains on disk.
5. **Remove** legacy XML after smoke/parity for converted definitions.
6. **Exit dual-run** when no required customer definitions rely on the XML path (Phase 5) — only after [criteria](./definition-xml-shim-removal-criteria.md) M1–M3 + G1–G6.

### When the shim **must stay**

Keep `PSLegacyDefinitionXmlShim` and gadget legacy fallback when **any** of the following is true:

| Condition | Why |
|-----------|-----|
| Criteria Status snapshot shows M2 **FAIL** or **PARTIAL** | No zero-legacy runtime evidence yet |
| Criteria M3 **FAIL** | Customer upgrade window still open |
| Install has Widget XML without a matching modern package root | Customer / residual defs still load via legacy |
| `widgetDao.modernPackageRoots` is blank **and** product defaults PR (#3130) is not on the build | On current `main`, blank property ⇒ legacy-only selection kinds |
| Gadget classpath missing `gadget-catalog.json` | Product prefers modern; fallback is the safety net |
| Agent or human is tempted to “finish Phase 5 early” | **#2852 is blocked** — deletion is not the dual-run checklist |

### How to read metrics (on `main` today)

| Surface | Log / API | How to interpret |
|---------|-----------|------------------|
| Widgets | INFO `Widget definition dual-run selection: modern=N, legacyWidgetXml=M, total=T` | After a repository poll: `M > 0` means at least some loaded ids classified as legacy (expected when modern roots empty or customer-only XML). `N` rises when modern package roots match ids. |
| Widgets | `PSWidgetDao.getLastSelectionKind()` / `getSelectionKindsById()` | Last single selection / per-id map — use from tests or support probes |
| Widgets | `PSWidgetDao.getModernPackageRoots()` | Empty ⇒ expect legacy kinds for install Widgets XML on disk |
| Gadgets | INFO `Gadget registry dual-load selection: modern=…, legacyRegistryXml=…, none=…, entries=…, source=…` | Product classpath should show modern preferred; `legacyRegistryXml=1` means fallback fired |
| Gadgets | `GadgetRegistry.getLastLoadSource()` / `getLastLoadEntryCount()` | Diagnostics / unit tests |

**After open PRs merge (not yet on main):**

- #3130 / PR #3134 — blank `widgetDao.modernPackageRoots` discovers `${rxdeploydir}/Packages/Modern` (+ classpath materialize).
- #3131 / PR #3136 — cumulative counters, `formatSelectionMetricsSummary()`, harness tests; see criteria § **How to measure M2**.

### What evidence **closes M2**

M2 becomes **PASS** only when **all** hold (document on #2852 when claiming):

1. Product / QA installs have modern roots available (explicit property **or** product defaults after #3130 merges).
2. Over the agreed time-box, widget `legacyWidgetXml` rate is **0** (or an explicit waiver list with owner + sunset date).
3. Gadget product path reports modern catalog with **no** required production dependence on `LEGACY_REGISTRY_XML` (or waived classpaths only).
4. Evidence is attached (log excerpts and/or counter snapshots) — not chat assertion alone.

Until then: **shim stays; #2852 stays blocked.**

### H2 `qa-up` (docker / local QA)

| Step | Action | Pass signal |
|------|--------|-------------|
| 1 | Bring up CMS via project H2 `qa-up` / docker compose path used by the team | Server starts; widgets load |
| 2 | Confirm install Widgets dir exists (`${rxdeploydir}/rxconfig/Widgets`) | Product widgets present as install wire XML (materialized) |
| 3 | **On current main:** set `widgetDao.modernPackageRoots` to a `File.pathSeparator` list of package roots that contain `component-package.json` **or** accept legacy-only kinds until #3130 merges. Prefer roots under staged modern package trees when available | `getModernPackageRoots()` non-empty when testing modern path |
| 4 | Exercise widget list / page edit that loads definitions (poll triggers `recordSelectionKinds`) | INFO line appears with `modern=` / `legacyWidgetXml=` |
| 5 | Open dashboard (gadget type map load) | INFO gadget dual-load line; product classpath typically `source=MODERN_CATALOG` |
| 6 | Capture log excerpts for any M2 discussion | Attach to #2852 only when claiming zero-legacy — do **not** open deletion work from PARTIAL metrics |

### Product install (full installer / upgrade)

| Step | Action | Pass signal |
|------|--------|-------------|
| 1 | Install or upgrade using product distribution | `rxconfig/Widgets` populated for install wire format |
| 2 | Inventory customer residual Widget XML (support path) | List of non-product ids retained under Widgets |
| 3 | Configure modern roots (property list of package roots with `component-package.json`) until #3130 product defaults ship | Selection kinds show MODERN for converted product ids |
| 4 | Convert remaining customer XML via Widget XML compiler → deploy modern packages | Customer defs selectable as modern |
| 5 | Smoke pages/widgets; read dual-run INFO metrics | No unexpected `none` / mass legacy for product ids after modern roots configured |
| 6 | Only after criteria M1–M3 + G1–G6: allow #2852 deletion work | Criteria Status snapshot all PASS |

### Spring / property knobs (portable)

| Knob | Behavior on `main` (2026-08-12) |
|------|----------------------------------|
| `widgetDao.modernPackageRoots` | `File.pathSeparator`-separated absolute or resolvable paths. **Blank default** → no modern roots (legacy-only selection kinds). |
| Widgets repository | `${rxdeploydir}/rxconfig/Widgets` (existing DAO repository directory) |
| Gadget modern resource | Classpath `com/percussion/webui/gadget/servlets/gadget-catalog.json` |
| Gadget legacy resource | Classpath `…/GadgetRegistry.xml` (fallback; **do not delete** until #2852 criteria met) |

## Exit criteria (shim retirement)

The dual-run window ends when **all** of the following hold. **Authoritative metrics, test gates, time-box, and inventory:** [definition-xml-shim-removal-criteria.md](./definition-xml-shim-removal-criteria.md) (Status snapshot **2026-08-12**, #2835 / #3132).

- [x] Product **page layout** packages `perc.baseTemplates` / `perc.responsiveTemplates` are **not** authored as `*.templateDef` (#2786; native install mode #2806 — dual-ship roots off).
- [x] Product **Widget** packages (non-waived) no longer ship definition XML as source of truth under Packages — cluster [#2897](https://github.com/intersoftdatalabs-in/percussioncms/pull/2897) (A+B+C); only `perc.Test` waived residual; G4 gate [#3051](https://github.com/intersoftdatalabs-in/percussioncms/pull/3051). Install wire XML may still be **materialized** at package-build for runtime load — that is not ADR-004 authoring regression. **Broader M1 (Pages/Gadgets wording):** see criteria M1 row.
- [ ] Customer upgrade path documented and used for remaining XML (window still open for 8.2 dual-run) — **M3 FAIL**.
- [ ] Runtime metrics (or support inventory) show no production dependence on legacy definition XML loads — or remaining cases are explicitly waived. **Snapshot 2026-08-12:** #3024 / #3025 on main (PARTIAL M2); #3130 / #3131 open for defaults + harness; overall **M2 FAIL**. See criteria § How to measure M2.
- [ ] Phase 5 (#2632) removes or hard-disables the shim and updates help — **blocked** until criteria doc **M1–M3 + G1–G6** pass; residual **[#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852)** tracks the deletion PR and must **not** start early.

**Do not** mass-delete the customer-facing shim while any box above is open. **Do not** treat PARTIAL M2 as “ready to remove.”

## Non-goals

- Completing full product package conversion (sibling #2750 / #2751 and residuals)
- Multi-RDBMS matrix or host-only install validation
- Phase 4 Design SPA (#2631) or full Phase 5 help rewrite (#2632)

## See also

- [definition-xml-shim-removal-criteria.md](./definition-xml-shim-removal-criteria.md) — Phase 5 metrics, gates, time-box, inventory (#2835)
- [ADR-004](./adr/004-no-definition-xml-packaging.md) — no definition XML for product packaging
- [component-package-manifest.md](./component-package-manifest.md) — modern ship format (when present on branch)
- [plan.md](./plan.md) — Phase 3 / Phase 5 goals
- [widget-xml-inventory.md](./widget-xml-inventory.md) — product widget matrix
- [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md) — package-build dual-ship (related, not identical)
