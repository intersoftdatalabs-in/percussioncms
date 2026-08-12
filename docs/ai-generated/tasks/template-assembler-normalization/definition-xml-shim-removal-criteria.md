# Definition-XML shim removal criteria (Phase 5)

| Field | Value |
|-------|--------|
| **Status** | Criteria **not met** (snapshot 2026-08-11) — shim **must remain** |
| **Issue** | [#2835](https://github.com/intersoftdatalabs-in/percussioncms/issues/2835) (slice 3 of [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632)) |
| **Parent** | [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632) Phase 5 · Grandparent [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) |
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

## Decision (this slice)

| Question | Answer (2026-08-11) |
|----------|---------------------|
| Are removal criteria met? | **No** |
| May agents delete `PSLegacyDefinitionXmlShim` / related dual-run loaders now? | **No** — leave shim |
| Thin proven-dead removal in this PR? | **None** — no dead production path safe to drop |
| Residual? | File residual for **shim removal when metrics allow** (see Residual work) |

---

## 1. Metrics (must all pass)

### M1 — Zero product package definition-XML as ship format

| Metric | Pass condition | How to measure |
|--------|----------------|----------------|
| Product Widget definition XML in repo | **0** files under `modules/perc-packages/src/main/resources/Packages/**/sys__UserDependency--rxconfig/Widgets/*.xml` (except explicitly waived test packages such as `perc.Test`) | `Get-ChildItem -Recurse` / CI inventory job against Packages tree |
| Product Page meta definition XML | **0** product-authored `rxconfig/Pages/*.xml` (or package staging equivalent) as package source of truth | Same inventory; see [page-definition-inventory.md](./page-definition-inventory.md) |
| Product Gadget definition XML | **0** product per-gadget OpenSocial-style definition XML required at runtime | [gadget-definition-inventory.md](./gadget-definition-inventory.md) |
| ADR-004 ship bar | Product packages author **only** modern `component-package.json` (+ CT / template / slot / catalog artifacts) | Package tree review + package-compile CI |

**Snapshot 2026-08-11 (cluster #2883+#2884+#2885 A+B+C ship-exit):** **PASS M1 for product non-waived Widget XML** — only **1** Widget definition XML remains under `Packages/**/sys__UserDependency--rxconfig/Widgets/` (`perc.Test`, explicit waiver; was **48**). Batches A/B/C removed **8+20+19** committed install XMLs; install wire format is materialized at package-build via `PSWidgetXmlInstallEmitter`. Modern widget `component-package.json` roots: **47** (full product excl. Test). Overall Phase 5 removal criteria still **not met** (M2/M3 and shim still required).

### M2 — Zero (or waived) runtime legacy definition-XML loads

| Metric | Pass condition | How to measure |
|--------|----------------|----------------|
| Legacy selection rate | `wouldUseLegacyShim(packageRoot) == false` for all product package roots in install/staging | Unit / integration probe using `PSLegacyDefinitionXmlShim.wouldUseLegacyShim` |
| Runtime selection kind | No production log/metric for `LEGACY_WIDGET_XML` / `LEGACY_PAGE_XML` / `LEGACY_GADGET_XML` over agreed window (see time-box), **or** remaining hits are **explicitly waived** with owner + sunset date | Log field or counter on selection kind when DAO wiring is complete |
| Widget DAO path | Production widget load no longer depends solely on `${rxdeploydir}/rxconfig/Widgets/*.xml` for product widgets | Code + install smoke |
| Gadget registry dual-load | WebUI `GadgetRegistry` last load source is modern catalog in product installs (legacy XML only for waived customer cases) | `GadgetRegistry.getLastLoadSource()` / `getLastLoadEntryCount()` / INFO metrics / tests |

**Snapshot 2026-08-11:** **FAIL M2** — selection API existed and was unit-tested, but **no production caller** outside `modules/perc-packages` shim package + javadoc cross-refs wired `PSLegacyDefinitionXmlShim` into live load paths. `PSWidgetDao` still bound only `@Value("${rxdeploydir}/rxconfig/Widgets")`. `GadgetRegistry` dual-load (modern catalog preferred, legacy `GadgetRegistry.xml` fallback) is still active.

**Snapshot 2026-08-11 (slice #3024):** **PARTIAL M2** — `PSWidgetDao` now wires `PSLegacyDefinitionXmlShim.selectDefinition` (modern-first) with test-visible `getLastSelectionKind()` / `getSelectionKindsById()` and INFO metrics `modern=` / `legacyWidgetXml=`. Optional Spring property `widgetDao.modernPackageRoots` (`File.pathSeparator` list). Content still loads install Widget XML (materialized wire format); selection kind reports MODERN when a modern package root is present for the id. **M2 still FAIL overall** until production installs configure modern roots and runtime metrics show zero (or waived) `LEGACY_WIDGET_XML` over the time-box. Shim **must remain** (#2852).

**Snapshot 2026-08-12 (slice #3130):** **PARTIAL M2 (defaults landed)** — product / H2 installs no longer need manual Spring surgery for modern roots. When `widgetDao.modernPackageRoots` is **blank**, `PSModernPackageRootDefaults` discovers package roots under `${rxdeploydir}/Packages/Modern` (layout `Packages/Modern/<pkg>/widgets/<stem>/component-package.json`), materializing from the product `perc-packages` classpath when that tree is empty. Distribution stages `Packages/Modern` at install; upgrade overwrite includes the tree. Explicit path-separator property still overrides. Tests cover modern-present → MODERN and modern-absent / customer-only id → LEGACY. Shim **must remain** (#2852). **M2 still FAIL overall** until runtime metrics show zero (or waived) `LEGACY_WIDGET_XML` over the time-box.

**Snapshot 2026-08-11 (slice #3025):** **PARTIAL M2 (gadget dual-load hardened)** — WebUI `GadgetRegistry` dual-load already preferred `gadget-catalog.json` with `GadgetRegistry.xml` fallback (#2788). Hardening adds INFO selection metrics `modern=` / `legacyRegistryXml=` / `none=` / `entries=` (parity with `PSWidgetDao`), test-visible `getLastLoadEntryCount()`, and edge-case tests (empty/unreadable modern → legacy; blank/null modern resource; successive last-load updates). Product classpath still reports `MODERN_CATALOG`. **Legacy fallback retained** (#2852). **M2 still FAIL overall** until widget modern roots + runtime legacy rate criteria and remaining dual-run exit evidence land.

**Snapshot 2026-08-12 (slice #3131):** **PARTIAL M2 (evidence harness)** — cumulative dual-run / dual-load counters + snapshot/summary APIs + CI harness tests for `PSWidgetDao` and `GadgetRegistry` (see **How to measure M2** below). Still **FAIL overall** until production installs show zero (or waived) legacy rate over the time-box and modern roots are product-defaulted (#3130). Shim **must remain** (#2852).

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
..\..\mvnw.cmd -Dtest=PSWidgetDaoTest,PSWidgetDaoSelectionMetricsHarnessTest test
# or full module gate:
..\..\mvnw.cmd clean install
```

- `com.percussion.pagemanagement.dao.PSWidgetDaoTest` — modern preferred, legacy fallback, neither throws, poll kinds (#3024)
- `com.percussion.pagemanagement.dao.PSWidgetDaoSelectionMetricsHarnessTest` — counters, snapshot, summary, reset, mixed select+poll (#3131)

**M2 pass signal (widget path):** over the agreed window, `legacyWidgetXml` / `getLegacySelectionCount()` is **0** (or waived list with owner + sunset), and product widgets select `MODERN_COMPONENT_PACKAGE` when modern roots are configured.

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

**Snapshot 2026-08-11 (G4 Widget inventory gate #3026):** **PASS G4 for product Widget definition XML** — automated assertion in `modules/perc-packages`:

| Piece | Location |
|-------|----------|
| Inventory API + optional CLI | `com.percussion.packages.widgetxml.PSWidgetDefinitionXmlInventory` |
| Surefire gate | `PSWidgetDefinitionXmlInventoryTest` (product tree zero non-waived; TempDir proves failure when dummy non-waived XML is introduced) |
| Explicit waive | `perc.Test` only (`WAIVED_PACKAGE_DIRS`) |
| Path I/O | `Path.resolve` / `Files` only (G6-aligned) |

Pages/Gadgets ship-path inventory remains residual under the broader G4 wording; Widget path is the Phase 3 residual closed by #3026.

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

## 4. Inventory snapshot (grep / tree) — 2026-08-11

### 4.1 Selection API package (`modules/perc-packages`)

| Type / file | Role | Dual-run? |
|-------------|------|-----------|
| `…/shim/PSLegacyDefinitionXmlShim.java` | Modern-preferred selection (`selectByPresence`, `selectForPackageRoot`, `selectDefinition`, `wouldUseLegacyShim`) | **Yes** — canonical policy |
| `…/shim/PSDefinitionSourceKind.java` | `MODERN_COMPONENT_PACKAGE`, `LEGACY_WIDGET_XML`, `LEGACY_PAGE_XML`, `LEGACY_GADGET_XML` | Yes |
| `…/shim/PSDefinitionSourceSelection.java` | Selection result + `isLegacyXml()` | Yes |
| `…/shim/PSDefinitionSourceNotFoundException.java` | Neither modern nor legacy | Yes |
| `…/shim/PSLegacyDefinitionXmlShimTest.java` | Unit coverage for selection | Test only |

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
| Widget XML compilers | `…/widgetxml/PSWidgetXml*` | Upgrade-input compilers; keep after runtime shim exit | **No** (upgrade input OK) |
| Page dual-ship / native | `…/pagexml/PSPageXmlDualShip`, `PSPageXmlNativeInstall`, `PSPageXmlInstallPolicy` | Native for base/responsive; dual-ship default elsewhere | Separate checklist ([dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md)) |
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
| Runtime dual-run shim deletion + consumer rewiring when criteria met | Residual GitHub issue (p2, unassigned) | Modules: `modules/perc-packages`, likely `projects/sitemanage` (widget DAO), possibly `WebUI` gadget dual-load |
| Product Widget XML deletion from Packages after modern ship | Owned under Phase 3 [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) residuals — **do not** invent mega-delete here | Prerequisite for M1 |
| Page dual-ship code retirement | [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md) | Parallel, not identical |

Residual issue for this slice is filed from the night-issue-prs run of #2835 (link in issue/PR comments and parent Agent progress).

---

## See also

- [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md) — operator dual-run policy
- [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md) — package-build dual-ship
- [widget-xml-inventory.md](./widget-xml-inventory.md) / [page-definition-inventory.md](./page-definition-inventory.md) / [gadget-definition-inventory.md](./gadget-definition-inventory.md)
- [component-package-manifest.md](./component-package-manifest.md) / [adr/004-no-definition-xml-packaging.md](./adr/004-no-definition-xml-packaging.md)
- [plan.md](./plan.md) § Phase 5
- Epic [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) · Phase 5 [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632) · Phase 3 [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630)
