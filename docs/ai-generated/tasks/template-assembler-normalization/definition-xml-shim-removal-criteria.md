# Definition-XML shim removal criteria (Phase 5)

| Field | Value |
|-------|--------|
| **Status** | Criteria **not met** (snapshot **2026-08-22**) — shim **must remain**; [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) **blocked** until M1–M3 + G1–G6 |
| **Issue** | Criteria lock [#2835](https://github.com/intersoftdatalabs-in/percussioncms/issues/2835); M2 snapshot refresh [#3132](https://github.com/intersoftdatalabs-in/percussioncms/issues/3132); product/H2 zero-legacy-selection evidence [#3583](https://github.com/intersoftdatalabs-in/percussioncms/issues/3583); M2 re-check after perc.Test modern [#3738](https://github.com/intersoftdatalabs-in/percussioncms/issues/3738); removal residual [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) |
| **Parent** | [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632) Phase 5 · Grandparent [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) · Phase 3 tracker [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) |
| **Depends on** | Phase 3 [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) product off definition XML; dual-run policy [#2752](https://github.com/intersoftdatalabs-in/percussioncms/issues/2752) |
| **Policy / selection** | [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md) |
| **ADR** | [ADR-004](./adr/004-no-definition-xml-packaging.md) |
| **Code (selection API)** | `com.percussion.packages.shim.PSLegacyDefinitionXmlShim` (`modules/perc-packages`) |

## Purpose

This document is the **hard gate** for deleting or hard-disabling the legacy definition-XML dual-run path. It expands the high-level exit checklist in the dual-run operator doc into:

1. **Metrics** (what to measure and pass thresholds)
2. **Test / CI gates** (what must be green before code deletion)
3. **Time-box** (when removal work is allowed to start)
4. **Inventory** (grep snapshot of dual-run surfaces as of this slice)

**Hard ban:** unattended mass deletion of the customer-facing shim without evidence that every criterion below is met. Thin removal is allowed **only** for paths proven dead with tests (none found in this snapshot).

**Hard ban (removal residual):** do **not** start implementation on [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) (shim / dual-run deletion) until **every** row in the Status snapshot table below is **PASS** (or explicitly waived with owner + sunset). Partial M2 infrastructure is **not** “ready to remove.”

## Status snapshot (2026-08-22) — `main` + open perc.Test modern PRs

Re-verify against current `main` before any removal PR. Counts below were taken from this worktree after `git fetch origin main` / hard reset to `origin/main` on **2026-08-22** (#3738 M2 re-check). Sibling [#3736](https://github.com/intersoftdatalabs-in/percussioncms/issues/3736) / PR [#3750](https://github.com/intersoftdatalabs-in/percussioncms/pull/3750) (perc.Test modern Widget, drop XML waiver) was **open, not merged**.

| ID | Criterion | Status | Evidence on `main` / links | What still blocks |
|----|-----------|--------|----------------------------|-------------------|
| **M1** | Zero product package definition-XML as ship format (ADR-004) | **PASS** (Widget non-waived; Pages/Gadgets ship-path 0) | Cluster [#2897](https://github.com/intersoftdatalabs-in/percussioncms/pull/2897) (batches A+B+C #2883/#2884/#2885): **1** remaining Widget def XML under Packages (`perc.Test` waiver only; was **48**). **77** `component-package.json` under Packages. G4 Widget gate [#3051](https://github.com/intersoftdatalabs-in/percussioncms/pull/3051) / #3026; Pages/Gadgets ship-path gate #3581. Open #3736 would drop the Widget waiver to **empty** when merged | Waived Test package only (until #3736); Page `*.templateDef` dual-ship/native is a separate surface (#3737) |
| **M2** | Zero (or waived) runtime legacy definition-XML loads | **PARTIAL / FAIL overall** | **Merged on main:** [#3045](https://github.com/intersoftdatalabs-in/percussioncms/pull/3045) #3024 (`PSWidgetDao` modern-first + INFO + selection kinds); [#3071](https://github.com/intersoftdatalabs-in/percussioncms/pull/3071) #3025 (`GadgetRegistry` dual-load metrics); #3130/#3131/#3132 defaults + counters + snapshot; #3583 product/H2 zero-legacy-selection (`PSProductPackageRootSelectionEvidence` + `PSWidgetDaoProductH2ZeroLegacySelectionTest`). **This slice (#3738):** re-ran/extended that harness — waive list must be **empty or `perc.Test` only**; product/H2 still **zero non-waived** `LEGACY_WIDGET_XML`; waived `perc.Test` + customer-only still legacy while #3736 is unmerged | Product/H2 **selection evidence green**; live time-box / support inventory still open; customer XML remains valid fallback; **do not claim M2 PASS**; **#2852 blocked** |
| **M3** | Customer upgrade window closed or residual waived | **FAIL** | Compilers + dual-run checklist exist; 8.2 dual-run window **open by design** | Need support inventory / waived residual list + closed upgrade window (or documented residual customers) |
| **G1** | `modules/perc-packages` clean install green after removal | **N/A (pre-removal)** | Module green on current dual-run code; deletion not started | Required only on the future removal PR |
| **G2** | Behavioral modern-only selection tests | **PARTIAL** | `PSLegacyDefinitionXmlShimTest` + `PSWidgetDaoTest` modern/legacy/neither on main | Modern-only (no legacy fallback) tests belong on removal PR |
| **G3** | No production refs to deleted dual-run APIs after removal | **N/A (pre-removal)** | Callers still **required**: `PSWidgetDao`, `GadgetRegistry` fallback | Pass only after deletion rewires consumers |
| **G4** | CI fails if product definition XML reappears under Packages ship paths | **PASS** (Widget + Pages + Gadgets) | Widget: `PSWidgetDefinitionXmlInventory` + Surefire [#3051](https://github.com/intersoftdatalabs-in/percussioncms/pull/3051) #3026 (waive `perc.Test` until #3736); Pages/Gadgets: `PSPageDefinitionXmlInventory` / `PSGadgetDefinitionXmlInventory` / `PSDefinitionXmlShipPathInventory` #3581; Page/Gadget waive **empty** after #3737 | None for ship-path inventory; keep shim (#2852) |
| **G5** | Reverse-dep consumers green after removal | **N/A (pre-removal)** | sitemanage / WebUI dual-run tests green with shim present | Re-run sitemanage + WebUI on removal PR |
| **G6** | Cross-platform Path/Files only in load paths | **PASS (current surfaces)** | Widget inventory + shim use `Path`/`Files`; DAO roots use `Path.of` | Re-check any replacement load path on removal PR |

### Decision (2026-08-22)

| Question | Answer |
|----------|--------|
| Are removal criteria met? | **No** |
| May agents delete `PSLegacyDefinitionXmlShim` / related dual-run loaders now? | **No** — leave shim |
| Is [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) ready to implement? | **No — blocked** until **M1–M3 + G1–G6** all PASS (or waived with evidence) |
| Thin proven-dead removal now? | **None** |
| False “ready to remove” claims allowed? | **No** — PARTIAL M2 infrastructure ≠ M2 PASS |

---

## 1. Metrics (must all pass)

### M1 — Zero product package definition-XML as ship format

| Metric | Pass condition | How to measure |
|--------|----------------|----------------|
| Product Widget definition XML in repo | **0** files under `modules/perc-packages/src/main/resources/Packages/**/sys__UserDependency--rxconfig/Widgets/*.xml` (except explicitly waived test packages such as `perc.Test`) | `Get-ChildItem -Recurse` / CI inventory job against Packages tree |
| Product Page meta definition XML | **0** product-authored `rxconfig/Pages/*.xml` (or package staging equivalent) as package source of truth | Same inventory; see [page-definition-inventory.md](./page-definition-inventory.md) |
| Product Gadget definition XML | **0** product per-gadget OpenSocial-style definition XML required at runtime | [gadget-definition-inventory.md](./gadget-definition-inventory.md) |
| ADR-004 ship bar | Product packages author **only** modern `component-package.json` (+ CT / template / slot / catalog artifacts) | Package tree review + package-compile CI |

**Snapshot 2026-08-11 (cluster #2883+#2884+#2885 A+B+C ship-exit / merged cluster PR [#2897](https://github.com/intersoftdatalabs-in/percussioncms/pull/2897)):** **PASS M1 for product non-waived Widget XML** — only **1** Widget definition XML remains under `Packages/**/sys__UserDependency--rxconfig/Widgets/` (`perc.Test`, explicit waiver; was **48**). Batches A/B/C removed **8+20+19** committed install XMLs; install wire format is materialized at package-build via `PSWidgetXmlInstallEmitter`. Modern widget `component-package.json` roots: **47** (full product excl. Test). Overall Phase 5 removal criteria still **not met** (M2/M3 and shim still required).

**Snapshot 2026-08-12 (#3132 re-check on `origin/main`):** **PASS M1 (Widget non-waived)** reaffirmed — Widget def XML count still **1** (`perc.Test` only); **77** `component-package.json` under Packages tree (includes non-widget modern packages). G4 Widget inventory gate remains on main (#3026 / PR #3051).

### M2 — Zero (or waived) runtime legacy definition-XML loads

| Metric | Pass condition | How to measure |
|--------|----------------|----------------|
| Legacy selection rate | `wouldUseLegacyShim(packageRoot) == false` for all product package roots in install/staging | Unit / integration probe using `PSLegacyDefinitionXmlShim.wouldUseLegacyShim` |
| Runtime selection kind | No production log/metric for `LEGACY_WIDGET_XML` / `LEGACY_PAGE_XML` / `LEGACY_GADGET_XML` over agreed window (see time-box), **or** remaining hits are **explicitly waived** with owner + sunset date | Log field or counter on selection kind when DAO wiring is complete |
| Widget DAO path | Production widget load no longer depends solely on `${rxdeploydir}/rxconfig/Widgets/*.xml` for product widgets | Code + install smoke |
| Gadget registry dual-load | WebUI `GadgetRegistry` last load source is modern catalog in product installs (legacy XML only for waived customer cases) | `GadgetRegistry.getLastLoadSource()` / `getLastLoadEntryCount()` / INFO metrics / tests |

**Snapshot 2026-08-11:** **FAIL M2** — selection API existed and was unit-tested, but **no production caller** outside `modules/perc-packages` shim package + javadoc cross-refs wired `PSLegacyDefinitionXmlShim` into live load paths. `PSWidgetDao` still bound only `@Value("${rxdeploydir}/rxconfig/Widgets")`. `GadgetRegistry` dual-load (modern catalog preferred, legacy `GadgetRegistry.xml` fallback) is still active.

**Snapshot 2026-08-11 (slice #3024 / PR [#3045](https://github.com/intersoftdatalabs-in/percussioncms/pull/3045)):** **PARTIAL M2** — `PSWidgetDao` now wires `PSLegacyDefinitionXmlShim.selectDefinition` (modern-first) with test-visible `getLastSelectionKind()` / `getSelectionKindsById()` and INFO metrics `modern=` / `legacyWidgetXml=`. Optional Spring property `widgetDao.modernPackageRoots` (`File.pathSeparator` list). Content still loads install Widget XML (materialized wire format); selection kind reports MODERN when a modern package root is present for the id. **M2 still FAIL overall** until production installs configure modern roots and runtime metrics show zero (or waived) `LEGACY_WIDGET_XML` over the time-box. Shim **must remain** (#2852).

**Snapshot 2026-08-11 (slice #3025 / PR [#3071](https://github.com/intersoftdatalabs-in/percussioncms/pull/3071)):** **PARTIAL M2 (gadget dual-load hardened)** — WebUI `GadgetRegistry` dual-load already preferred `gadget-catalog.json` with `GadgetRegistry.xml` fallback (#2788). Hardening adds INFO selection metrics `modern=` / `legacyRegistryXml=` / `none=` / `entries=` (parity with `PSWidgetDao`), test-visible `getLastLoadEntryCount()`, and edge-case tests (empty/unreadable modern → legacy; blank/null modern resource; successive last-load updates). Product classpath still reports `MODERN_CATALOG`. **Legacy fallback retained** (#2852). **M2 still FAIL overall** until widget modern roots + runtime legacy rate criteria and remaining dual-run exit evidence land.

**Snapshot 2026-08-12 (slice #3130):** **PARTIAL M2 (defaults landed)** — product / H2 installs no longer need manual Spring surgery for modern roots. When `widgetDao.modernPackageRoots` is **blank**, `PSModernPackageRootDefaults` discovers package roots under `${rxdeploydir}/Packages/Modern` (layout `Packages/Modern/<pkg>/widgets/<stem>/component-package.json`), materializing from the product `perc-packages` classpath when that tree is empty. Distribution stages `Packages/Modern` at install; upgrade overwrite includes the tree. Explicit path-separator property still overrides. Tests cover modern-present → MODERN and modern-absent / customer-only id → LEGACY. Shim **must remain** (#2852). **M2 still FAIL overall** until runtime metrics show zero (or waived) `LEGACY_WIDGET_XML` over the time-box.

**Snapshot 2026-08-12 (slice #3131):** **PARTIAL M2 (evidence harness)** — cumulative dual-run / dual-load counters + snapshot/summary APIs + CI harness tests for `PSWidgetDao` and `GadgetRegistry` (see **How to measure M2** below). Still **FAIL overall** until production installs show zero (or waived) legacy rate over the time-box. Shim **must remain** (#2852).

**Snapshot 2026-08-12 (criteria refresh #3132):** **PARTIAL M2 / FAIL overall** — Status snapshot table at top of this doc reaffirms M1 PASS (Widget non-waived), M2 PARTIAL, M3 FAIL, G4 Widget PASS. **Do not claim M2 PASS** from wiring/defaults/harness alone. **#2852 remains blocked** until M1–M3 + G1–G6.

**Snapshot 2026-08-18 (#3581):** G4 Pages/Gadgets ship-path inventory landed (see G4 row). **M2/M3 unchanged.** **#2852 remains blocked.**

**Snapshot 2026-08-18 (slice #3583):** **PARTIAL M2 (product/H2 selection evidence)** — Surefire now **fails** if a non-waived product/H2 widget package root would use the legacy shim or if `PSWidgetDao` H2 defaults select `LEGACY_WIDGET_XML` for a known product widget stem. Probe: `PSProductPackageRootSelectionEvidence` (perc-packages) + `PSWidgetDaoProductH2ZeroLegacySelectionTest` (sitemanage). Waiver: `perc.Test` / `PSWidget_TestProperties` only. Customer-only XML still selects `LEGACY_WIDGET_XML` (shim **kept**). **Do not claim M2 PASS** or removal-ready — live time-box / support inventory and **M3 FAIL** still block #2852.

**Snapshot 2026-08-22 (slice #3738):** **PARTIAL M2 re-check after perc.Test modern work** — #3736 (drop Widget XML waiver) was **not merged** on `main`. Harness now encodes the allowed waive lists (**empty** or **`perc.Test` only**) via `assertWidgetWaiverPolicy()` / `assertPercTestSelectionMatchesWaiver()`. On this snapshot: waive list is still `{perc.Test}`; product/H2 **zero non-waived** `LEGACY_WIDGET_XML`; customer-only still legacy. If/when #3736 merges, the same tests require `perc.Test` / `PSWidget_TestProperties` modern-first and **zero** waived-legacy roots. **M3 still FAIL. Shim kept. Do not start #2852.**

| Surface | Status in this cluster | Metric visibility | Legacy still required? |
|---------|------------------------|-------------------|------------------------|
| Widget selection wire | #3024 on main | Per-poll INFO + selection kinds | **Yes** until zero-legacy rate |
| Gadget dual-load | #3025 on main | INFO + last source/entry count | **Yes** — classpath fallback retained |
| Product modern roots default | #3130 (this cluster) | Defaults + tests | Customer-only XML still selects legacy |
| Cumulative counters + CI harness | #3131 (this cluster) | Snapshot/summary APIs + harness tests | Process-lifetime rate still needs zero/waived window |
| Product/H2 zero-legacy-selection evidence | #3583 (this slice) | `wouldUseLegacyShim` / kind on product + H2 materialized roots; DAO H2 defaults | Customer-only + waived `perc.Test` still legacy; shim kept |
| M2 re-check after perc.Test modern | #3738 | Dual-mode waive policy (empty or `perc.Test` only); product/H2 still zero non-waived `LEGACY_*` | #3736 unmerged ⇒ `perc.Test` still waived+legacy; M3 FAIL; shim kept |

### How to measure M2 (evidence harness)

Use these **CI-assertable** probes and log fields when collecting Phase 5 M2 evidence. Do **not** treat “no ERROR in chat” as a substitute for counters.

#### Widget DAO (`projects/sitemanage` — `PSWidgetDao`)

| Probe | API / surface | What it proves |
|-------|---------------|----------------|
| Last kind | `getLastSelectionKind()` | Most recent modern vs `LEGACY_WIDGET_XML` selection |
| Per-id poll map | `getSelectionKindsById()` | Kind for every id on last repository poll |
| Cumulative counters | `getModernSelectionCount()` / `getLegacySelectionCount()` / `getTotalSelectionCount()` | Process-lifetime modern vs legacy rate |
| Snapshot map | `getSelectionMetricsSnapshot()` keys `modern`, `legacyWidgetXml`, `total` | Stable map for tests / support dumps |
| Ops one-liner | `formatSelectionMetricsSummary()` | Log-friendly dump without new admin UI |
| Reset (tests/probes) | `resetSelectionMetrics()` | Isolate a measurement window |
| INFO log | `Widget definition dual-run selection: modern=…, legacyWidgetXml=…, …, cumulativeModern=…, cumulativeLegacy=…` | Runtime operators |

**CI harness tests (must stay green):**

```text
# from projects/sitemanage (repo-root mvnw)
..\..\mvnw.cmd -Dtest=PSWidgetDaoTest,PSWidgetDaoSelectionMetricsHarnessTest,PSWidgetDaoProductH2ZeroLegacySelectionTest test
# or full module gate:
..\..\mvnw.cmd clean install
```

- `com.percussion.pagemanagement.dao.PSWidgetDaoTest` — modern preferred, legacy fallback, neither throws, poll kinds (#3024)
- `com.percussion.pagemanagement.dao.PSWidgetDaoSelectionMetricsHarnessTest` — counters, snapshot, summary, reset, mixed select+poll (#3131)
- `com.percussion.pagemanagement.dao.PSWidgetDaoProductH2ZeroLegacySelectionTest` — H2 blank-property defaults + classpath materialize; zero unexpected `LEGACY_WIDGET_XML` on non-waived product stems; customer-only stays legacy (#3583); `perc.Test` dual-mode (#3738)

```text
# from modules/perc-packages (repo-root mvnw)
..\..\mvnw.cmd -Dtest=PSProductPackageRootSelectionEvidenceTest test
# or full module gate:
..\..\mvnw.cmd clean install
```

- `com.percussion.packages.shim.PSProductPackageRootSelectionEvidence` / `PSProductPackageRootSelectionEvidenceTest` — product `Packages/` roots + H2 `Packages/Modern` materialize: `wouldUseLegacyShim == false` / kind MODERN for non-waived widget packages; fails if dummy non-waived LEGACY_* is introduced; waive list empty **or** `perc.Test` only (#3738); waived `perc.Test` may remain legacy until #3736

**M2 pass signal (widget path):** over the agreed window, `legacyWidgetXml` / `getLegacySelectionCount()` is **0** (or waived list with owner + sunset), and product widgets select `MODERN_COMPONENT_PACKAGE` when modern roots are configured. **#3583** makes the product/H2 *selection* half of that signal CI-assertable; it does **not** close the live time-box or M3.

#### Gadget registry (`WebUI` — `GadgetRegistry`)

| Probe | API / surface | What it proves |
|-------|---------------|----------------|
| Last source | `getLastLoadSource()` | `MODERN_CATALOG` / `LEGACY_REGISTRY_XML` / `NONE` |
| Last entries | `getLastLoadEntryCount()` | Size of last dual-load map |
| Cumulative counters | `getModernLoadCount()` / `getLegacyLoadCount()` / `getNoneLoadCount()` / `getTotalLoadCount()` | Process-lifetime dual-load rate |
| Snapshot map | `getSelectionMetricsSnapshot()` keys `modern`, `legacyRegistryXml`, `none`, `total` | Stable map for tests / support dumps |
| Ops one-liner | `formatSelectionMetricsSummary()` | Log-friendly dump |
| Reset (tests/probes) | `resetSelectionMetrics()` | Isolate a measurement window |
| INFO log | `Gadget registry dual-load selection: modern=…, legacyRegistryXml=…, none=…, entries=…, source=…, cumulativeModern=…, …` | Runtime operators |

**CI harness tests (must stay green):**

```text
# from WebUI (repo-root mvnw)
..\mvnw.cmd -Dtest=GadgetRegistryTest,GadgetRegistrySelectionMetricsHarnessTest test
# or full module gate:
..\mvnw.cmd clean install
```

- `com.percussion.webui.gadget.servlets.GadgetRegistryTest` — dual-load preference + edges (#3025)
- `com.percussion.webui.gadget.servlets.GadgetRegistrySelectionMetricsHarnessTest` — counters, snapshot, summary, reset (#3131)

**M2 pass signal (gadget path):** product installs report `lastSource=MODERN_CATALOG` and `legacyRegistryXml` cumulative **0** (or waived customer classpaths only).

#### Support / operator collection recipe

1. Run the module Surefire classes above in CI or a local clean install (both modules).
2. On a live/QA CMS after widget/gadget traffic: capture INFO lines containing `dual-run selection` / `dual-load selection`, or call `formatSelectionMetricsSummary()` from a support probe / debugger.
3. Attach counter snapshots (or log excerpts) to the removal residual (#2852) when claiming M2.
4. **Hard ban:** do not delete the shim or legacy fallback based only on product classpath modern preference without the zero-legacy rate evidence above.

### M3 — Customer upgrade window closed (or accepted residual)

| Metric | Pass condition | How to measure |
|--------|----------------|----------------|
| Conversion tooling | Widget / Page / Gadget compilers documented and available for customer XML → modern package | [widget-xml-inventory.md](./widget-xml-inventory.md), [component-package-manifest.md](./component-package-manifest.md) |
| Support inventory | Support / field inventory shows no **required** customer dependence on definition XML, **or** residual customers are listed with waiver + migration plan | Support ticket sample or customer list (operator process; not committed secrets) |
| Upgrade docs | Operators have a documented convert → deploy modern → remove XML path | Dual-run checklist in [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md) + product-docs when operator-facing steps freeze |

**Snapshot 2026-08-11:** **FAIL M3** — compilers and dual-run operator checklist exist; customer upgrade window is **still open** by design for 8.2 dual-run. No production metric stream yet to prove “zero loads.”

**Snapshot 2026-08-12 (#3132):** **FAIL M3** — unchanged. Compilers, dual-run operator checklist (expanded for H2 `qa-up` + product install), and M2 measurement recipes exist; customer upgrade window remains **open**. Support residual list not closed. **#2852 still blocked** on M3 (and remaining M2 evidence) even if M1 stays PASS.

**Snapshot 2026-08-18 (#3583):** **FAIL M3** — unchanged. Product/H2 selection evidence does **not** close the customer upgrade window. **Do not claim removal-ready.**

**Snapshot 2026-08-22 (#3738):** **FAIL M3** — unchanged. Re-checking product/H2 selection after perc.Test modern work does **not** close the customer upgrade window. **Do not claim removal-ready. Do not delete `PSLegacyDefinitionXmlShim`.**

---

## 2. Test / CI gates (must all pass before deletion PR)

| Gate | Requirement |
|------|-------------|
| **G1** | `modules/perc-packages` standalone `mvnw clean install` green after removal (or modern-only replacement) |
| **G2** | Behavioral tests prove modern-only selection: modern present → modern; **neither** → clear `PSDefinitionSourceNotFoundException` (no silent invent) |
| **G3** | No remaining production references to deleted APIs (`PSLegacyDefinitionXmlShim` legacy kinds, dual-load fallbacks) except upgrade-input **compilers** (XML as input only is OK) |
| **G4** | Product package inventory job (or scripted assertion) fails CI if new definition XML reappears under Packages Widgets/Pages/Gadgets ship paths |
| **G5** | Reverse-dep / known runtime consumers still green: at minimum `projects/sitemanage` widget load tests and WebUI `GadgetRegistry` tests if those surfaces change |
| **G6** | Cross-platform: no Unix-only path assumptions in any replacement load path (`Path` / `Files` only) |

**Snapshot 2026-08-11:** G2 partially satisfied for the **selection API** (`PSLegacyDefinitionXmlShimTest`). G1/G3–G6 for **deletion** are N/A until M1–M3 pass.

**Snapshot 2026-08-11 (G4 Widget inventory gate #3026 / PR [#3051](https://github.com/intersoftdatalabs-in/percussioncms/pull/3051)):** **PASS G4 for product Widget definition XML** — automated assertion in `modules/perc-packages`:

| Piece | Location |
|-------|----------|
| Inventory API + optional CLI | `com.percussion.packages.widgetxml.PSWidgetDefinitionXmlInventory` |
| Surefire gate | `PSWidgetDefinitionXmlInventoryTest` (product tree zero non-waived; TempDir proves failure when dummy non-waived XML is introduced) |
| Explicit waive | `perc.Test` only (`WAIVED_PACKAGE_DIRS`) |
| Path I/O | `Path.resolve` / `Files` only (G6-aligned) |

**Snapshot 2026-08-18 (G4 Pages/Gadgets inventory gate #3581):** **PASS G4 for Pages and Gadgets ship paths** — automated assertion in `modules/perc-packages` (peer of Widget #3026):

| Piece | Location |
|-------|----------|
| Shared scanner + optional CLI | `com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory` (`PAGE` / `GADGET` / `ALL`) |
| Page inventory API + CLI | `com.percussion.packages.pagexml.PSPageDefinitionXmlInventory` |
| Gadget inventory API + CLI | `com.percussion.packages.gadgetxml.PSGadgetDefinitionXmlInventory` |
| Surefire gates | `PSPageDefinitionXmlInventoryTest`, `PSGadgetDefinitionXmlInventoryTest`, `PSDefinitionXmlShipPathInventoryTest` (product tree zero non-waived; TempDir proves failure when dummy non-waived XML is introduced) |
| Explicit waive | Empty after perc.Test page dual-ship exit (#3737) (`WAIVED_PACKAGE_DIRS`) |
| Path I/O | `Path.resolve` / `Files` only (G6-aligned) |
| Ship paths | `sys__UserDependency--rxconfig/Pages\|Gadgets` and `rxconfig/Pages\|Gadgets` (shim-recognized layouts). Modern `pages/` / catalog JSON is not definition XML. |

Widget path remains PASS (#3026). Broader G4 wording (Widgets/Pages/Gadgets) is now automated. **Does not unlock #2852** (M2/M3 still fail; shim stays).

**Snapshot 2026-08-12 (#3132):** G-status summary (superseded for G4 by #3581): G4 Widget **PASS** on main; G2 **PARTIAL** (dual-run selection tests, not modern-only deletion tests); G1/G3/G5 **N/A** until removal PR; G6 **PASS** for current Path/Files surfaces. **None of this unlocks #2852** without M2/M3.

---

## 3. Time-box

| Phase | Window | Allowed work |
|-------|--------|--------------|
| **Dual-run (now)** | Through Phase 3 product conversion + customer upgrade window | Ship modern preferred; keep legacy fallback; **no** mass shim delete |
| **Criteria lock** | This doc (#2835) | Metrics + inventory + residual filed; re-evaluate when M1–M3 evidence exists |
| **Removal PR** | Only after M1–M3 + G1–G6 | Thin, reviewed deletion of proven-dead dual-run code + tests; update help |
| **Post-removal** | Next minor after removal merges | Treat reintroduction of product definition XML as regression (G4) |

**Rules for agents / humans:**

1. Do **not** open a “delete all shim” PR because Phase 5 is open.
2. Do **not** remove customer-facing fallback until M2 has evidence (logs/metrics or waived residual list).
3. Compilers may keep reading definition XML as **upgrade input** forever if useful — that is **not** the runtime dual-run shim.
4. Page **dual-ship** (`PSPageXmlDualShip`) is a related but separate package-build bridge — see [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md). Do not conflate dual-ship retirement with runtime shim removal.

---

## 4. Inventory snapshot (grep / tree) — 2026-08-11 (reaffirmed 2026-08-22 on `main`)

**2026-08-22 re-check on `origin/main` + #3738:** product Packages Widget def XML count still **1** (`perc.Test` / `PSWidget_TestProperties.xml` only); `PSWidgetDao` still the sole production caller of `PSLegacyDefinitionXmlShim`; `GadgetRegistry` dual-load still live with metrics. Product/H2 widget-root selection remains CI-assertable (`PSProductPackageRootSelectionEvidence`); waive policy is empty-or-`perc.Test`. Open #3736 would drop that residual XML. No thin dead-path deletion candidate. **Shim kept.**

### 4.1 Selection API package (`modules/perc-packages`)

| Type / file | Role | Dual-run? |
|-------------|------|-----------|
| `…/shim/PSLegacyDefinitionXmlShim.java` | Modern-preferred selection (`selectByPresence`, `selectForPackageRoot`, `selectDefinition`, `wouldUseLegacyShim`) | **Yes** — canonical policy |
| `…/shim/PSDefinitionSourceKind.java` | `MODERN_COMPONENT_PACKAGE`, `LEGACY_WIDGET_XML`, `LEGACY_PAGE_XML`, `LEGACY_GADGET_XML` | Yes |
| `…/shim/PSDefinitionSourceSelection.java` | Selection result + `isLegacyXml()` | Yes |
| `…/shim/PSDefinitionSourceNotFoundException.java` | Neither modern nor legacy | Yes |
| `…/shim/PSLegacyDefinitionXmlShimTest.java` | Unit coverage for selection | Test only |
| `…/shim/PSProductPackageRootSelectionEvidence.java` | Product/H2 zero-legacy-selection probe (#3583 / #3738) | Evidence only — does **not** delete the shim |
| `…/shim/PSProductPackageRootSelectionEvidenceTest.java` | Product tree + H2 materialize + fail-on-dummy-legacy + dual-mode perc.Test waiver | Test only |

**Production callers of `PSLegacyDefinitionXmlShim` (excluding self + tests):**

| Caller | Module | Notes |
|--------|--------|-------|
| `PSWidgetDao` (`selectDefinitionSource` + poll `recordSelectionKinds`) | `projects/sitemanage` | #3024 modern-first wire; keeps legacy Widgets load |

Javadoc-only alignment references:

- `com.percussion.packages.pagexml.PSPageXmlInstallPolicy`
- `com.percussion.packages.pagexml.PSPageXmlNativeInstall`

### 4.2 Related dual-run / dual-ship surfaces (not all deletable with the shim)

| Surface | Path / class | Status in snapshot | Removal coupled to shim? |
|---------|--------------|--------------------|---------------------------|
| Widget install load | `projects/sitemanage/.../dao/impl/PSWidgetDao` → `${rxdeploydir}/rxconfig/Widgets` + default/optional `widgetDao.modernPackageRoots` | **Live** dual-run selection (#3024); **product defaults** to `${rxdeploydir}/Packages/Modern` (+ classpath materialize) (#3130); content still from Widgets XML wire | **Yes** for product modern-only content path; selection wired + defaults; keep shim until M2 metrics pass |
| Product Widget package XML | `modules/perc-packages/.../Packages/**/sys__UserDependency--rxconfig/Widgets/*.xml` | **1** remaining (`perc.Test` waiver only; was **48**); install materialize for modern-only; **G4 inventory gate** `PSWidgetDefinitionXmlInventory` / #3026 | M1 Widget portion PASS; G4 Widget path automated; keep shim until M2/M3 |
| Product Page package XML | `…/Packages/**/{sys__UserDependency--rxconfig,rxconfig}/Pages/*.xml` | **0** on product tree; **G4 inventory gate** `PSPageDefinitionXmlInventory` / #3581 | G4 Page path automated; modern `pages/` authoring is not this path; keep shim until M2/M3 |
| Product Gadget package XML | `…/Packages/**/{sys__UserDependency--rxconfig,rxconfig}/Gadgets/*.xml` | **0** on product tree; **G4 inventory gate** `PSGadgetDefinitionXmlInventory` / #3581 | G4 Gadget path automated; WebUI `GadgetRegistry.xml` is outside Packages ship paths; keep shim until M2/M3 |
| Widget XML compilers | `…/widgetxml/PSWidgetXml*` | Upgrade-input compilers; keep after runtime shim exit | **No** (upgrade input OK) |
| Page dual-ship / native | `…/pagexml/PSPageXmlDualShip`, `PSPageXmlNativeInstall`, `PSPageXmlInstallPolicy` | Native is the policy default (#3949); dual-ship is explicit opt-in | Separate checklist ([dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md)) |
| Gadget catalog ship | `modules/perc-packages/.../catalogs/gadgets/gadget-catalog.json` | Modern catalog present | Preferred path |
| Gadget WebUI dual-load | `WebUI/.../GadgetRegistry.java` (modern catalog → legacy `GadgetRegistry.xml`) | **Live** dual-load; INFO metrics + `getLastLoadSource()` / `getLastLoadEntryCount()` (#3025) | Related dual-run; keep legacy fallback until M2/M3 + #2852 removal criteria; product installs already prefer modern |
| Gadget registry compiler | `…/gadgetxml/PSGadgetRegistry*` | Upgrade-input / build | Keep as compiler |
| Component package manifest | `…/manifest/PSComponentPackageManifest*` | Modern ship format | Keep |

### 4.3 Grep recipes (re-run before any removal PR)

From repo root (PowerShell-friendly; adjust path tools as needed):

```text
# Selection API + kinds
rg -n "PSLegacyDefinitionXmlShim|PSDefinitionSourceKind|wouldUseLegacyShim|LEGACY_WIDGET_XML|LEGACY_PAGE_XML|LEGACY_GADGET_XML" --glob "*.java"

# Widget XML install surface
rg -n "rxconfig/Widgets|rxconfig\\\\Widgets" --glob "*.java"

# Gadget dual-load
rg -n "LEGACY_REGISTRY_XML|gadget-catalog.json|GadgetRegistry.xml" --glob "*.{java,ts,tsx}"

# Product package Widget XML residual count
# (PowerShell) Get-ChildItem modules\perc-packages\src\main\resources\Packages -Recurse -Filter *.xml |
#   Where-Object FullName -match 'sys__UserDependency--rxconfig\\Widgets'
```

### 4.4 Proven-dead thin removal?

| Candidate | Dead? | Action this slice |
|-----------|-------|-------------------|
| Entire `PSLegacyDefinitionXmlShim` package | **No** — policy + tests; future wiring target | Keep |
| `PSWidgetDao` Widgets path | **No** — production load path | Keep |
| `GadgetRegistry` legacy fallback | **No** — dual-load hardened (#3025) but fallback still required for customer/legacy classpath until M2/M3 + #2852 | Keep |
| Page dual-ship default for non-native packages | **No** — still default for non-opted packages | Keep (separate doc) |

**Conclusion:** no thin proven-dead removal in #2835.

---

## 5. Checklist for a future removal PR

Copy into the residual issue / PR when M1–M3 evidence exists:

- [ ] M1 product definition XML count = 0 (or waived list attached)
- [ ] M2 runtime legacy load metrics / support inventory = zero or waived
- [ ] M3 customer upgrade window closed or residual customers documented
- [ ] G1–G6 test/CI gates green with evidence in PR body
- [ ] Dual-run operator doc marked **retired**; this criteria doc status → **Met**
- [ ] Help / product-docs updated only if operator-facing behavior changes
- [ ] **No** mass-delete of upgrade-input compilers without a separate decision
- [ ] Parent #2632 / epic #2626 Agent progress updated

---

## 6. Residual work

| Work | Form | Notes |
|------|------|-------|
| Runtime dual-run shim deletion + consumer rewiring when criteria met | **[#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852)** (p2, unassigned) — **BLOCKED** until M1–M3 + G1–G6 | Modules: `modules/perc-packages`, likely `projects/sitemanage` (widget DAO), possibly `WebUI` gadget dual-load |
| Product/H2 default modern package roots | Open [#3130](https://github.com/intersoftdatalabs-in/percussioncms/issues/3130) / PR [#3134](https://github.com/intersoftdatalabs-in/percussioncms/pull/3134) | M2 infrastructure; **not** shim deletion |
| Dual-run selection metrics evidence harness | Open [#3131](https://github.com/intersoftdatalabs-in/percussioncms/issues/3131) / PR [#3136](https://github.com/intersoftdatalabs-in/percussioncms/pull/3136) | M2 measurement; **not** shim deletion |
| Criteria M2 snapshot + operator checklist | [#3132](https://github.com/intersoftdatalabs-in/percussioncms/issues/3132) (this refresh) | Docs only; keeps #2852 correctly blocked |
| Product/H2 runtime zero-legacy-selection evidence | [#3583](https://github.com/intersoftdatalabs-in/percussioncms/issues/3583) | Assertable product/H2 MODERN selection; **not** M2 PASS; **not** shim deletion |
| M2 re-check after perc.Test modern | [#3738](https://github.com/intersoftdatalabs-in/percussioncms/issues/3738) | Dual-mode waive (empty or `perc.Test`); M3 still FAIL; **not** shim deletion |
| Page dual-ship code retirement | [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md) | Parallel, not identical |

**#2852 gate statement (authoritative for agents):** implement shim/fallback deletion **only** after this doc’s Status snapshot shows **M1 PASS**, **M2 PASS** (zero or waived legacy rate with attached evidence), **M3 PASS** (or waived residual customers), and **G1–G6 PASS** on the removal PR. Until then leave dual-run code in place.

---

## See also

- [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md) — operator dual-run policy + H2/product checklist
- [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md) — package-build dual-ship
- [widget-xml-inventory.md](./widget-xml-inventory.md) / [page-definition-inventory.md](./page-definition-inventory.md) / [gadget-definition-inventory.md](./gadget-definition-inventory.md)
- [component-package-manifest.md](./component-package-manifest.md) / [adr/004-no-definition-xml-packaging.md](./adr/004-no-definition-xml-packaging.md)
- [plan.md](./plan.md) § Phase 5
- Epic [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) · Phase 5 [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632) · Phase 3 [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) · Removal residual [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) · Criteria refresh [#3132](https://github.com/intersoftdatalabs-in/percussioncms/issues/3132)
