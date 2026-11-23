# Definition-XML shim removal criteria (Phase 5)

| Field | Value |
|-------|--------|
| **Status** | **RETIRED / MET 2026-11-08** via [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) removal change (this PR): dual-run legacy fallback hard-disabled, selection modern-only. M1 PASS, M3 PASS (owner upgrade-YES 2026-11-08), M2 live-telemetry remainder QA-tracked in [#5808](https://github.com/intersoftdatalabs-in/percussioncms/issues/5808), G1–G6 evidenced on the removal PR (see retirement snapshot below). Gadget WebUI dual-load retired as focused residual [#5810](https://github.com/intersoftdatalabs-in/percussioncms/issues/5810) (modern-only) |
| **Issue** | Criteria lock [#2835](https://github.com/intersoftdatalabs-in/percussioncms/issues/2835); M2 snapshot refresh [#3132](https://github.com/intersoftdatalabs-in/percussioncms/issues/3132); product/H2 zero-legacy-selection evidence [#3583](https://github.com/intersoftdatalabs-in/percussioncms/issues/3583); M2 re-check after perc.Test modern [#3738](https://github.com/intersoftdatalabs-in/percussioncms/issues/3738); M1 refresh after #3736 merge [#3951](https://github.com/intersoftdatalabs-in/percussioncms/issues/3951); removal residual [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) |
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

**Hard ban (removal residual — SPENT 2026-11-08):** [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) has closed, retiring the shim to modern-only with G1–G6 evidenced on the removal PR; the live-telemetry remainder rides QA tracking [#5808](https://github.com/intersoftdatalabs-in/percussioncms/issues/5808). Historical text kept below: do **not** start implementation on #2852 (shim / dual-run deletion) until **every** row in the Status snapshot table below is **PASS** (or explicitly waived with owner + sunset). Partial M2 infrastructure is **not** “ready to remove.”

## Status snapshot (2026-08-28) — `origin/main` after perc.Test modern (#3736)

Re-verify against current `main` before any removal PR. Counts below were taken from the overnight worktree after `git fetch origin main` / hard reset to `origin/main` on **2026-08-28** (#3951 M1 refresh). [#3736](https://github.com/intersoftdatalabs-in/percussioncms/issues/3736) / PR [#3750](https://github.com/intersoftdatalabs-in/percussioncms/pull/3750) (perc.Test modern Widget, drop XML waiver) **merged 2026-08-25**.

| ID | Criterion | Status | Evidence on `main` / links | What still blocks |
|----|-----------|--------|----------------------------|-------------------|
| **M1** | Zero product package definition-XML as ship format (ADR-004) | **PASS** (empty Widget waiver; Pages/Gadgets ship-path 0) | Cluster [#2897](https://github.com/intersoftdatalabs-in/percussioncms/pull/2897) A+B+C then [#3750](https://github.com/intersoftdatalabs-in/percussioncms/pull/3750) #3736: committed Widget def XML under Packages **0** (was **1** `perc.Test`, was **48**). G4 `PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS` **empty** (`PSWidgetDefinitionXmlInventoryTest.waivedPackageSet_isEmptyAfterPercTestShipExit`). **81** `component-package.json` under Packages. Pages/Gadgets ship-path gate #3581; perc.Test page dual-ship exit #3737 (Page/Gadget waive empty) | None for M1 ship-path (retired with #2852) |
| **M2** | Zero (or waived) runtime legacy definition-XML loads | **RETIRED — modern-only selection, no LEGACY_* selectable (#2852)** | **Merged on main (pre-removal):** [#3045](https://github.com/intersoftdatalabs-in/percussioncms/pull/3045) #3024; [#3071](https://github.com/intersoftdatalabs-in/percussioncms/pull/3071) #3025; #3130/#3131/#3132 defaults + counters + snapshot; #3583 product/H2 zero-legacy-selection; #3738 dual-mode waive (empty); **#3736 merged** so perc.Test is modern-first and the waive list is **empty**. **Removal (#2852, this PR):** `PSLegacyDefinitionXmlShim` legacy branches deleted from selection — `selectByPresence` / `selectForPackageRoot` / `selectDefinition` return MODERN or throw a retired-error (never LEGACY_*); `wouldUseLegacyShim` deprecated to always-false; `PSWidgetDao` rewired modern-only with unchanged metric keys for #5808 telemetry; retired-legacy ids resolve to null-kind (harness) / clear error (DAO). Live-telemetry remainder rides with QA tracking [#5808](https://github.com/intersoftdatalabs-in/percussioncms/issues/5808) | Live telemetry in #5808 (parallel, not a start gate); Gadget WebUI dual-load retired via [#5810](https://github.com/intersoftdatalabs-in/percussioncms/issues/5810) |
| **M3** | Customer upgrade window closed or residual waived | **PASS (owner decision 2026-11-08)** | Owner answered **YES — upgrade**: the 8.2 dual-run window is closing; compilers + dual-run checklist exist | Decided by owner (natechadwick-intsof) 2026-11-08; residual-customer list (if any) rides with M2 QA tracking [#5808](https://github.com/intersoftdatalabs-in/percussioncms/issues/5808) |
| **G1** | `modules/perc-packages` clean install green after removal | **PASS (removal PR)** | `cd modules/perc-packages && ../../mvnw clean install` → BUILD SUCCESS, Tests run: 234, Failures: 0, Errors: 0 (JDK 21, no skips). No new warnings (one pre-existing `serialVersionUID` warning on untouched `PSDefinitionSourceNotFoundException`) | None |
| **G2** | Behavioral modern-only selection tests | **PASS (removal PR)** | `PSLegacyDefinitionXmlShimTest` 15/15 modern-only (legacy-only → retired error naming the XML + compiler path; `wouldUseLegacyShim` always false); `PSWidgetDaoTest` 10/10, `PSWidgetDaoSelectionMetricsHarnessTest` 5/5, `PSWidgetDaoProductH2ZeroLegacySelectionTest` 1/1; evidence-harness tests updated to null-kind/unresolvable (never legacy) | None |
| **G3** | No production refs to deleted dual-run APIs after removal | **PASS (hard-disable, signatures stable)** | No API deleted: shim keeps signatures (legacy params retained as retired diagnostics-only probes); only production runtime caller `PSWidgetDao` rewired modern-only. Gadget `GadgetRegistry` never referenced the shim (own load path; retired modern-only via focused residual [#5810](https://github.com/intersoftdatalabs-in/percussioncms/issues/5810)). Compilers (`PSWidgetXml*`, page/gadget) kept as upgrade inputs per scope | Gadget dual-load retired #5810 |
| **G4** | CI fails if product definition XML reappears under Packages ship paths | **PASS (unchanged gate)** | Widget: `PSWidgetDefinitionXmlInventory` + Surefire #3026 / #3736 (`WAIVED_PACKAGE_DIRS` empty); Pages/Gadgets: `PSPageDefinitionXmlInventory` / `PSGadgetDefinitionXmlInventory` / `PSDefinitionXmlShipPathInventory` #3581 — all untouched and green in the removal-PR builds. Selection-harness legacy-kind assertions retired (nothing can select LEGACY_*); reappearance is guarded by these file-presence inventories | None |
| **G5** | Reverse-dep consumers green after removal | **PASS (removal PR)** | `cd projects/sitemanage && ../../mvnw clean install` → BUILD SUCCESS, Tests run: 4150, Failures: 0, Errors: 0, Skipped: 125 (pre-existing skips; JDK 21, no skips). WebUI untouched (no signature change reaches it — shim API stable, `GadgetRegistry` independent); gadget dual-load rides the focused residual | Gadget dual-load retired via #5810 (this run) |
| **G6** | Cross-platform Path/Files only in load paths | **PASS (removal PR)** | Replacement load path is deletion-only (no new I/O): retained probes use existing `Path`/`Files`/`DirectoryStream` helpers; no hardcoded separators added; tests use `@TempDir` + `Path` | None |

### Decision (2026-11-08 — retired by #2852 removal change)

| Question | Answer |
|----------|--------|
| Are removal criteria met? | **Yes — retired/met.** M1 PASS, M3 PASS (owner upgrade-YES), M2 selection retired to modern-only (live telemetry in #5808), G1–G6 evidenced on the removal PR |
| Is the `PSLegacyDefinitionXmlShim` legacy fallback deleted? | **Yes — hard-disabled:** legacy selection branches removed; modern-only `selectByPresence` / `selectForPackageRoot` / `selectDefinition` (throw retired-error otherwise); `wouldUseLegacyShim` deprecated to always-false; signatures kept stable; compilers kept as upgrade inputs |
| Is [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) done? | **Yes for widget/page selection.** Gadget WebUI dual-load (`GadgetRegistry`) retired modern-only via focused residual [#5810](https://github.com/intersoftdatalabs-in/percussioncms/issues/5810) (separate surface) per #2852 scope item 3 |
| Thin proven-dead removal now? | **Done — this change** |
| False “ready to remove” claims allowed? | **No** — PARTIAL M2 infrastructure ≠ M2 PASS (historical note; superseded by retirement) |

**Snapshot 2026-11-08 (#2852 removal — modern-only retirement):** `PSWidgetDao` + package-root loaders rewired to modern `component-package.json` with clear failure when modern is absent (no silent invent); `PSLegacyDefinitionXmlShim` legacy fallback hard-disabled (API stable, compilers kept); modern-only unit/integration tests across `perc-packages` (234 green) + `sitemanage` (4150 green); this doc + dual-run doc + product-docs dual-run page updated to retired. M2 live-telemetry remainder rides QA #5808. Gadget WebUI dual-load retired via focused residual #5810 (this run).

**Snapshot 2026-11-08 (#5810 gadget residual — modern-only retirement):** `GadgetRegistry.loadGadgetTypeMap()` rewired to modern `gadget-catalog.json` only; modern absent/empty/unreadable yields empty map + ERROR (retired `GadgetRegistry.xml` never loaded, kept on classpath unreferenced — that product classpath copy was later deleted by #7094, absence pinned by #7107). Signatures stable (`Source.LEGACY_REGISTRY_XML`, `REGISTRY_RESOURCE`, snapshot/summary keys retained for QA telemetry; legacy reads 0). Pure `parseRegistryXml` parser retained for upgrade/tooling parity with kept compilers. Modern-only unit tests (`GadgetRegistryTest` + `GadgetRegistrySelectionMetricsHarnessTest`); standalone `WebUI` clean install green (count in PR body); this doc + dual-run doc + product-docs dual-run page updated to retired.

**Snapshot 2026-11-16 (slice #6440 — `LEGACY_WIDGET_XML` load path + `GadgetRegistry` dual-load fallback removed):** gate flipped since #2852 closed. `PSDefinitionSourceKind.LEGACY_WIDGET_XML` deleted; `PSLegacyDefinitionXmlShim.selectByPresence` / `selectDefinition` / `legacyProbeHint*` no longer take or probe a Widget XML path (Page / Gadget diagnostic probes retained for sibling slices); `PSWidgetDao` no longer passes a Widgets dir and its legacy counters / `legacyWidgetXml` snapshot key / summary field are removed (snapshot keys now `modern`, `total`); `GadgetRegistry` is single-load only (`Source.LEGACY_REGISTRY_XML`, `REGISTRY_RESOURCE`, two-arg overload, legacy counters / snapshot key removed; `parseRegistryXml` kept as the gadget-XML pure parser for the LEGACY_GADGET_XML residual slice). Regression evidence: `PSLegacyDefinitionXmlShimTest` asserts no `LEGACY_WIDGET_*` kind exists and the retired error names no widget file; `GadgetRegistryTest` asserts `Source` has exactly 2 values. Builds: `cd modules/perc-packages && ../../mvnw clean install` → BUILD SUCCESS, Tests run: 295, Failures: 0; `cd projects/sitemanage && ../../mvnw clean install` → BUILD SUCCESS, Tests run: 4351, Failures: 0; `cd WebUI && ../mvnw clean install` → Java tests 83/83 green, vitest 9021 passed with 3 pre-existing `widgetPayloadNpmParity` failures verified identical on pristine `origin/main` (environmental, #6319 area, untouched by this slice). Out of scope (sibling slices): `LEGACY_PAGE_XML` removal, `LEGACY_GADGET_XML` + shim-removal closeout.

**Snapshot 2026-11-16 (slice #6446 — `LEGACY_PAGE_XML` load path removed):** follow-on to #6440. `PSDefinitionSourceKind.LEGACY_PAGE_XML` deleted (only `MODERN_COMPONENT_PACKAGE` + `LEGACY_GADGET_XML` remain); `PSLegacyDefinitionXmlShim.selectByPresence` drops the page-presence flag and `selectDefinition` drops the `legacyPagesDir` probe parameter (Gadget probe retained for sibling slice #6447); `legacyProbeHintForRoot` no longer scans `rxconfig/Pages` and `legacyProbeHint` no longer has a Page branch; the retired-error base message records both removals. Callers updated: `PSWidgetDao` (2 sites) + `PSProductPackageRootSelectionEvidence` pass the shrunken `selectDefinition` signature. Regression evidence: new `selectDefinition_legacyPageOnlyThrowsRetiredErrorWithoutPageProbe` asserts on-disk Page XML is unnamed and no `LEGACY_PAGE_*` kind exists; `selectByPresence` gadget-only test asserts the hint names `(Gadget)` with no Page branch; `selectDefinition_gadgetOnlyThrowsRetiredError` keeps the remaining probe covered. Builds: `cd modules/perc-packages && ../../mvnw clean install` → BUILD SUCCESS, Tests run: 295, Failures: 0, Errors: 0; `cd projects/sitemanage && ../../mvnw clean install` → BUILD SUCCESS, Tests run: 4355, Failures: 0, Errors: 0 (125 pre-existing skips). Out of scope (sibling slice): `LEGACY_GADGET_XML` + shim-removal closeout (#6447). gate flipped since #2852 closed. `PSDefinitionSourceKind.LEGACY_WIDGET_XML` deleted; `PSLegacyDefinitionXmlShim.selectByPresence` / `selectDefinition` / `legacyProbeHint*` no longer take or probe a Widget XML path (Page / Gadget diagnostic probes retained for sibling slices); `PSWidgetDao` no longer passes a Widgets dir and its legacy counters / `legacyWidgetXml` snapshot key / summary field are removed (snapshot keys now `modern`, `total`); `GadgetRegistry` is single-load only (`Source.LEGACY_REGISTRY_XML`, `REGISTRY_RESOURCE`, two-arg overload, legacy counters / snapshot key removed; `parseRegistryXml` kept as the gadget-XML pure parser for the LEGACY_GADGET_XML residual slice). Regression evidence: `PSLegacyDefinitionXmlShimTest` asserts no `LEGACY_WIDGET_*` kind exists and the retired error names no widget file; `GadgetRegistryTest` asserts `Source` has exactly 2 values. Builds: `cd modules/perc-packages && ../../mvnw clean install` → BUILD SUCCESS, Tests run: 295, Failures: 0; `cd projects/sitemanage && ../../mvnw clean install` → BUILD SUCCESS, Tests run: 4351, Failures: 0; `cd WebUI && ../mvnw clean install` → Java tests 83/83 green, vitest 9021 passed with 3 pre-existing `widgetPayloadNpmParity` failures verified identical on pristine `origin/main` (environmental, #6319 area, untouched by this slice). Out of scope (sibling slices): `LEGACY_PAGE_XML` removal, `LEGACY_GADGET_XML` + shim-removal closeout.

**Snapshot 2026-11-16 (slice #6447 — `LEGACY_GADGET_XML` load path removed + shim-removal closeout):** follow-on to #6440/#6446, closes the vertical slice. `PSDefinitionSourceKind.LEGACY_GADGET_XML` deleted (only `MODERN_COMPONENT_PACKAGE` remains — no `LEGACY_*` kind exists); `PSLegacyDefinitionXmlShim.selectByPresence` drops the gadget-presence flag (now `(definitionId, modernPresent)`), `selectDefinition` drops the `legacyGadgetsDir` probe parameter (now `(definitionId, modernPackageRoots)`) and no longer probes or names retired Gadget XML, `legacyProbeHint*` / `findLegacyXmlFile` / `findFirstRetiredXml` deleted, and the retired always-false `wouldUseLegacyShim` probe deleted (callers now pin `isLegacyXml() == false` / the evidence record field); the retired-error base message records all three removals. Callers updated: `PSWidgetDao` (2 sites) + `PSProductPackageRootSelectionEvidence` pass the shrunken `selectDefinition` signature. `WebUI GadgetRegistry.parseRegistryXml` deleted (production-unreferenced; only its own test referenced it) with its XML-only imports. Regression evidence: new `selectByPresence_modernAbsentThrowsRetiredErrorWithoutLegacyProbe` + `selectDefinition_legacyGadgetOnlyThrowsRetiredErrorWithoutGadgetProbe` assert on-disk Gadget XML is unnamed and no `LEGACY_GADGET_*` kind exists; `PSDefinitionSourceKind.values()` carries no `LEGACY_*`; `GadgetRegistryTest` keeps asserting `Source` has exactly 2 values with no legacy XML path. Builds: `cd modules/perc-packages && ../../mvnw clean install` → BUILD SUCCESS, Tests run: 294, Failures: 0, Errors: 0; `cd projects/sitemanage && ../../mvnw clean install` → BUILD SUCCESS, Tests run: 4365, Failures: 0, Errors: 0 (125 pre-existing skips); `cd WebUI && ../mvnw clean install` → BUILD SUCCESS (Java tests 82/82 green, vitest 941 files / 9092 tests passed). Out of scope: none — epic #2626 residual tracking updated; compilers (`PSWidgetXml*`, page, `PSGadgetRegistryCompiler`) kept as upgrade inputs.

**Snapshot 2026-11-23 (shim-audit §3 steps 1–2 executed — doc refresh #7108):** dead `WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml` classpath resource deleted ([#7094](https://github.com/intersoftdatalabs-in/percussioncms/issues/7094) / PR #7111, merged); Java consumer refs swept + `GadgetRegistryTest.deletedLegacyRegistryXmlIsAbsentFromClasspath` absence guard added ([#7107](https://github.com/intersoftdatalabs-in/percussioncms/issues/7107) / PR #7137, merged); no runtime behavior change (the resource was unreferenced; `PSGadgetRegistryCompiler` keeps reading customer upgrade-input XML of the same name). M1/M2/M3 + G1–G6 posture unchanged (RETIRED / MET via #2852; live-telemetry remainder rides #5808). Out of scope: remaining dead fixtures ride [#7109](https://github.com/intersoftdatalabs-in/percussioncms/issues/7109); inventory-gate deletion rides residuals.

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

**Snapshot 2026-08-28 (#3951 M1 refresh on `origin/main`):** **PASS M1 (empty Widget waiver)** — [#3736](https://github.com/intersoftdatalabs-in/percussioncms/issues/3736) / PR [#3750](https://github.com/intersoftdatalabs-in/percussioncms/pull/3750) merged 2026-08-25. Committed Widget def XML under `Packages/**/sys__UserDependency--rxconfig/Widgets/` = **0**. `PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS` is **empty** (Surefire `waivedPackageSet_isEmptyAfterPercTestShipExit`; product scan `report.all().size() == 0`). **81** `component-package.json` under Packages. Pages/Gadgets ship-path remain 0 (#3581 / #3737). **M2 still PARTIAL. M3 still FAIL. Shim kept. #2852 not ready.**

### M2 — Zero (or waived) runtime legacy definition-XML loads

| Metric | Pass condition | How to measure |
|--------|----------------|----------------|
| Legacy selection rate | kind `MODERN_COMPONENT_PACKAGE` / evidence `wouldUseLegacyShim == false` for all product package roots in install/staging | Unit / integration probe using `PSProductPackageRootSelectionEvidence` (the retired static `PSLegacyDefinitionXmlShim.wouldUseLegacyShim` probe was removed #6447) |
| Runtime selection kind | No production log/metric for any `LEGACY_*` kind over agreed window (see time-box), **or** remaining hits are **explicitly waived** with owner + sunset date (all `LEGACY_*` kinds structurally removed — Widget #6440, Page #6446, Gadget #6447 — no log/metric can name one) | Log field or counter on selection kind when DAO wiring is complete |
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

**Snapshot 2026-08-28 (#3951 — #3736 merged):** **PARTIAL M2 unchanged overall** — waive list is **empty**; perc.Test / `PSWidget_TestProperties` is modern-first; product/H2 still **zero** `LEGACY_WIDGET_XML` on product stems; customer-only still legacy. Empty M1 waiver does **not** close the live time-box or M3. **Do not claim M2 PASS. Shim kept. Do not start #2852.**

**Snapshot 2026-11-08 (#5756 — re-verify on `origin/main` `572d5557ad`):** **PARTIAL M2 unchanged overall** — full evidence in [m2-evidence-snapshot-2026-11-08.md](./m2-evidence-snapshot-2026-11-08.md). M1 companion re-measured: committed Widget def XML **0**, `component-package.json` **81**, Widget waiver still **empty**. CI-assertable probes re-run green on JDK 21 with no skips: `PSProductPackageRootSelectionEvidenceTest` **13/13**, `PSWidgetDaoTest` + `PSWidgetDaoSelectionMetricsHarnessTest` + `PSWidgetDaoProductH2ZeroLegacySelectionTest` **16/16**, `GadgetRegistryTest` + `GadgetRegistrySelectionMetricsHarnessTest` **22/22** (plus WebUI vitest 8116 passed in the same run). Product/H2 still **zero** non-waived `LEGACY_*`; customer-only XML still selects legacy by design. Live time-box legacy rate + support inventory of customer packages still **open** (need a production/support install and support ticket data — owner: support/product human; no waiver recorded, agents must not self-waive). **Do not claim M2 PASS. Shim kept. Do not start #2852.**

**Snapshot 2026-11-08 (owner directive):** **M2 removed as a #2852 start gate.** All CI-assertable evidence stays green (see #5756/#5780 snapshots); the live time-box legacy rate + support inventory remainder is now QA-tracked in [#5808](https://github.com/intersoftdatalabs-in/percussioncms/issues/5808) and runs **in parallel** with removal — it no longer blocks #2852 start. Agents must still not self-waive; the waiver/PASS lands via #5808.

**Snapshot 2026-11-08 (#5780 — re-verify on `origin/main` `e75727d1a9`):** **PARTIAL M2 unchanged overall** — full evidence in [m2-evidence-snapshot-2026-11-08-5780.md](./m2-evidence-snapshot-2026-11-08-5780.md). M1 companion re-measured: committed Widget def XML **0**, `component-package.json` **81**, Widget waiver still **empty** (source-verified). CI-assertable probes re-run green on JDK 21 with no skips: `PSProductPackageRootSelectionEvidenceTest` **13/13**, `PSWidgetDaoTest` + `PSWidgetDaoSelectionMetricsHarnessTest` + `PSWidgetDaoProductH2ZeroLegacySelectionTest` **16/16**, `GadgetRegistryTest` + `GadgetRegistrySelectionMetricsHarnessTest` **22/22**. Product/H2 still **zero** non-waived `LEGACY_*`; customer-only XML still selects legacy by design. Live time-box legacy rate + support inventory still **open** (owner: support/product human; no waiver recorded, agents must not self-waive). **Do not claim M2 PASS. Shim kept. Do not start #2852.**

| Surface | Status in this cluster | Metric visibility | Legacy still required? |
|---------|------------------------|-------------------|------------------------|
| Widget selection wire | #3024 on main | Per-poll INFO + selection kinds | **Yes** until zero-legacy rate |
| Gadget dual-load | #3025 on main, retired #5810 | INFO + last source/entry count | **No** — modern-only since #5810 (legacy counter always 0) |
| Product modern roots default | #3130 (this cluster) | Defaults + tests | Customer-only XML still selects legacy |
| Cumulative counters + CI harness | #3131 (this cluster) | Snapshot/summary APIs + harness tests | Process-lifetime rate still needs zero/waived window |
| Product/H2 zero-legacy-selection evidence | #3583 (this slice) | `wouldUseLegacyShim` / kind on product + H2 materialized roots; DAO H2 defaults | Customer-only + waived `perc.Test` still legacy; shim kept |
| M2 re-check after perc.Test modern | #3738 | Dual-mode waive policy (empty or `perc.Test` only); product/H2 still zero non-waived `LEGACY_*` | Historical (2026-08-22): #3736 unmerged ⇒ `perc.Test` still waived+legacy |
| M1 snapshot after perc.Test modern merge | #3951 | Empty Widget XML waiver; 0 committed Widget def XML; 81 `component-package.json` | M2 PARTIAL / M3 FAIL; shim kept; **not** #2852 |

### How to measure M2 (evidence harness)

Use these **CI-assertable** probes and log fields when collecting Phase 5 M2 evidence. Do **not** treat “no ERROR in chat” as a substitute for counters.

#### Widget DAO (`projects/sitemanage` — `PSWidgetDao`)

| Probe | API / surface | What it proves |
|-------|---------------|----------------|
| Last kind | `getLastSelectionKind()` | Most recent selection (always `MODERN_COMPONENT_PACKAGE` after #6440; no `LEGACY_WIDGET_XML` kind remains) |
| Per-id poll map | `getSelectionKindsById()` | Kind for every id on last repository poll |
| Cumulative counters | `getModernSelectionCount()` / `getTotalSelectionCount()` (legacy counters removed #6440) | Process-lifetime modern selection rate |
| Snapshot map | `getSelectionMetricsSnapshot()` keys `modern`, `total` (`legacyWidgetXml` removed #6440) | Stable map for tests / support dumps |
| Ops one-liner | `formatSelectionMetricsSummary()` | Log-friendly dump without new admin UI |
| Reset (tests/probes) | `resetSelectionMetrics()` | Isolate a measurement window |
| INFO log | `Widget definition single-source selection: modern=…, total=…, cumulativeModern=…` (#6440; was `dual-run … legacyWidgetXml=…`) | Runtime operators |

**CI harness tests (must stay green):**

```text
# from projects/sitemanage (repo-root mvnw)
..\..\mvnw.cmd -Dtest=PSWidgetDaoTest,PSWidgetDaoSelectionMetricsHarnessTest,PSWidgetDaoProductH2ZeroLegacySelectionTest test
# or full module gate:
..\..\mvnw.cmd clean install
```

- `com.percussion.pagemanagement.dao.PSWidgetDaoTest` — modern preferred, retired-legacy throws (names no widget file, counts nothing), neither throws, poll kinds (#3024 / #6440)
- `com.percussion.pagemanagement.dao.PSWidgetDaoSelectionMetricsHarnessTest` — counters, snapshot, summary, reset, mixed select+poll (#3131)
- `com.percussion.pagemanagement.dao.PSWidgetDaoProductH2ZeroLegacySelectionTest` — H2 blank-property defaults + classpath materialize; every non-waived product stem selects `MODERN_COMPONENT_PACKAGE`; customer-only ids are unresolvable (null kind), never legacy (#3583 / #6440)

```text
# from modules/perc-packages (repo-root mvnw)
..\..\mvnw.cmd -Dtest=PSProductPackageRootSelectionEvidenceTest test
# or full module gate:
..\..\mvnw.cmd clean install
```

- `com.percussion.packages.shim.PSProductPackageRootSelectionEvidence` / `PSProductPackageRootSelectionEvidenceTest` — product `Packages/` roots + H2 `Packages/Modern` materialize: evidence `wouldUseLegacyShim == false` / kind MODERN for product widget packages (the retired static `PSLegacyDefinitionXmlShim.wouldUseLegacyShim` probe was removed #6447; selections pin `isLegacyXml() == false`); fails if dummy LEGACY_* is introduced; waive list **empty** after #3736 (was empty-or-`perc.Test` on #3738)

**M2 pass signal (widget path):** structural after #6440 — no `LEGACY_WIDGET_XML` kind exists and no widget-XML probe remains, so the rate is zero by construction; extended by #6446 (Page) and #6447 (Gadget) — no `LEGACY_*` kind of any flavor exists and no legacy probe of any kind remains. Product widgets select `MODERN_COMPONENT_PACKAGE` when modern roots are configured, and retired-legacy ids throw a retired-error. **#3583** makes the product/H2 *selection* half of that signal CI-assertable; it does **not** close the live time-box or M3. (Historical: `legacyWidgetXml` / `getLegacySelectionCount()` reads pre-removal.)

#### Gadget registry (`WebUI` — `GadgetRegistry`)

| Probe | API / surface | What it proves |
|-------|---------------|----------------|
| Last source | `getLastLoadSource()` | `MODERN_CATALOG` / `NONE` (`LEGACY_REGISTRY_XML` removed #6440) |
| Last entries | `getLastLoadEntryCount()` | Size of last load map |
| Cumulative counters | `getModernLoadCount()` / `getNoneLoadCount()` / `getTotalLoadCount()` (legacy counters removed #6440) | Process-lifetime load rate |
| Snapshot map | `getSelectionMetricsSnapshot()` keys `modern`, `none`, `total` (`legacyRegistryXml` removed #6440) | Stable map for tests / support dumps |
| Ops one-liner | `formatSelectionMetricsSummary()` | Log-friendly dump |
| Reset (tests/probes) | `resetSelectionMetrics()` | Isolate a measurement window |
| INFO log | `Gadget registry single-load selection: modern=…, none=…, entries=…, source=…, cumulativeModern=…` (#6440; was `dual-load … legacyRegistryXml=…`) | Runtime operators |

**CI harness tests (must stay green):**

```text
# from WebUI (repo-root mvnw)
..\mvnw.cmd -Dtest=GadgetRegistryTest,GadgetRegistrySelectionMetricsHarnessTest test
# or full module gate:
..\mvnw.cmd clean install
```

- `com.percussion.webui.gadget.servlets.GadgetRegistryTest` — single-load + edges, `Source` enum has no legacy value (#3025 / #5810 / #6440)
- `com.percussion.webui.gadget.servlets.GadgetRegistrySelectionMetricsHarnessTest` — counters, snapshot, summary, reset (#3131 / #6440)

**M2 pass signal (gadget path):** product installs report `lastSource=MODERN_CATALOG`. Structural-zero since #5810, selection remnants removed by #6440 (no legacy source, counter, or snapshot key remains; no waived classpaths needed — the fallback is gone).

#### Support / operator collection recipe

1. Run the module Surefire classes above in CI or a local clean install (both modules).
2. On a live/QA CMS after widget/gadget traffic: capture INFO lines containing `single-source selection` / `single-load selection` (pre-#6440 logs say `dual-run selection` / `dual-load selection`), or call `formatSelectionMetricsSummary()` from a support probe / debugger.
3. Attach counter snapshots (or log excerpts) to the removal residual (#2852) when claiming M2.
4. **Hard ban:** do not delete the remaining Page / Gadget probes or the shim itself based only on product classpath modern preference without the zero-legacy rate evidence above (widget path needs none — it is structurally removed by #6440).

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

**Snapshot 2026-08-28 (#3951):** **FAIL M3** — unchanged. Empty Widget XML waiver (#3736 merged) does **not** close the customer upgrade window. **Do not claim removal-ready. Do not start #2852.**

**Snapshot 2026-11-08 (#5756):** **FAIL M3** — unchanged. Customer upgrade window still **open by design** for 8.2 dual-run; support inventory of customer packages still shipping definition XML not closed (needs support/human data — owner: support/product human). Evidence: [m2-evidence-snapshot-2026-11-08.md](./m2-evidence-snapshot-2026-11-08.md) §5. **Do not claim removal-ready. Do not start #2852.**

**Snapshot 2026-11-08 (#5780):** **FAIL M3** — unchanged on `origin/main` `e75727d1a9`. Evidence: [m2-evidence-snapshot-2026-11-08-5780.md](./m2-evidence-snapshot-2026-11-08-5780.md) §5. **Do not claim removal-ready. Do not start #2852.**

**Snapshot 2026-11-08 (owner directive):** **PASS M3** — owner answered **YES, upgrade**: the 8.2 dual-run window is closing. Decided by owner (natechadwick-intsof) 2026-11-08. Any residual-customer list rides with M2 QA tracking #5808. **#2852 is unblocked.**

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
| Explicit waive | **Empty** after perc.Test ship-exit (#3736) (`WAIVED_PACKAGE_DIRS`) |
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

## 4. Inventory snapshot (grep / tree) — 2026-08-11 (reaffirmed 2026-08-28 on `main`)

**2026-08-28 re-check on `origin/main` (#3951, after #3736 merge):** product Packages Widget def XML count is **0** (`WAIVED_PACKAGE_DIRS` empty; perc.Test ships modern `widgets/` only). `PSWidgetDao` still the sole production caller of `PSLegacyDefinitionXmlShim`; `GadgetRegistry` dual-load still live with metrics. Product/H2 widget-root selection remains CI-assertable (`PSProductPackageRootSelectionEvidence`); waive policy is **empty**. No thin dead-path deletion candidate. **Shim kept. #2852 not ready.**

### 4.1 Selection API package (`modules/perc-packages`)

| Type / file | Role | Dual-run? |
|-------------|------|-----------|
| `…/shim/PSLegacyDefinitionXmlShim.java` | Modern-only selection (`selectByPresence`, `selectForPackageRoot`, `selectDefinition`; retired `wouldUseLegacyShim` probe removed #6447) | **Yes** — canonical policy |
| `…/shim/PSDefinitionSourceKind.java` | `MODERN_COMPONENT_PACKAGE` only (all `LEGACY_*` removed: Widget #6440, Page #6446, Gadget #6447) | Yes |
| `…/shim/PSDefinitionSourceSelection.java` | Selection result + `isLegacyXml()` (always false, retained for API stability) | Yes |
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
| Product Widget package XML | `modules/perc-packages/.../Packages/**/sys__UserDependency--rxconfig/Widgets/*.xml` | **0** remaining after #3736 (was **1** `perc.Test`, was **48**); install materialize for modern-only; **G4 inventory gate** `PSWidgetDefinitionXmlInventory` / #3026 (`WAIVED_PACKAGE_DIRS` empty) | M1 Widget portion PASS (empty waiver); G4 Widget path automated; keep shim until M2/M3 |
| Product Page package XML | `…/Packages/**/{sys__UserDependency--rxconfig,rxconfig}/Pages/*.xml` | **0** on product tree; **G4 inventory gate** `PSPageDefinitionXmlInventory` / #3581 | G4 Page path automated; modern `pages/` authoring is not this path; keep shim until M2/M3 |
| Product Gadget package XML | `…/Packages/**/{sys__UserDependency--rxconfig,rxconfig}/Gadgets/*.xml` | **0** on product tree; **G4 inventory gate** `PSGadgetDefinitionXmlInventory` / #3581 | G4 Gadget path automated; WebUI `GadgetRegistry.xml` is outside Packages ship paths; keep shim until M2/M3 |
| Widget XML compilers | `…/widgetxml/PSWidgetXml*` | Upgrade-input compilers; keep after runtime shim exit | **No** (upgrade input OK) |
| Page dual-ship / native | `…/pagexml/PSPageXmlDualShip`, `PSPageXmlNativeInstall`, `PSPageXmlInstallPolicy` | **Product ship path native-only** (#3951); native is the policy default (#3949); DualShip class still present; dual-ship is explicit opt-in and fail-closed in package-build (#3950) | Separate checklist ([dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md)) — **not** shim deletion |
| Gadget catalog ship | `modules/perc-packages/.../catalogs/gadgets/gadget-catalog.json` | Modern catalog present | Preferred path |
| Gadget WebUI dual-load | `WebUI/.../GadgetRegistry.java` (modern catalog only) | **Removed** single-load (#6440; was live dual-load #3025, retired #5810); INFO metrics + `getLastLoadSource()` / `getLastLoadEntryCount()` use `modern` / `none` only; legacy `GadgetRegistry.xml` parser `parseRegistryXml` deleted (#6447) | Related dual-run; shim-removal closeout complete; product installs already prefer modern |
| Gadget registry compiler | `…/gadgetxml/PSGadgetRegistry*` | Upgrade-input / build | Keep as compiler |
| Component package manifest | `…/manifest/PSComponentPackageManifest*` | Modern ship format | Keep |

### 4.3 Grep recipes (re-run before any removal PR)

From repo root (PowerShell-friendly; adjust path tools as needed):

```text
# Selection API + kinds
rg -n "PSLegacyDefinitionXmlShim|PSDefinitionSourceKind|wouldUseLegacyShim" --glob "*.java"

# Widget XML install surface
rg -n "rxconfig/Widgets|rxconfig\\\\Widgets" --glob "*.java"

# Gadget dual-load (absence check since #7094 deleted the product classpath copy:
# expect only prose history + compiler upgrade-input refs, never a runtime load)
rg -n "gadget-catalog.json|GadgetRegistry.xml" --glob "*.{java,ts,tsx}"

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
| Page dual-ship default for non-native packages | **No** — policy default is native if unset (#3949); product packages native-only (#3951); DualShip class still present | Keep DualShip code (CLI/tests). Package-build fail-closed (#3950). Product ship path retired (#3951) |

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
| Runtime dual-run shim deletion + consumer rewiring when criteria met | **[#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852)** — **CLOSED 2026-11-08** (shim retired to modern-only; M1/M2-retired/M3 + G1–G6 evidenced on the removal PR; live-telemetry remainder rides [#5808](https://github.com/intersoftdatalabs-in/percussioncms/issues/5808)) | Modules: `modules/perc-packages`, `projects/sitemanage` (widget DAO), `WebUI` gadget single-load |
| Product/H2 default modern package roots | Open [#3130](https://github.com/intersoftdatalabs-in/percussioncms/issues/3130) / PR [#3134](https://github.com/intersoftdatalabs-in/percussioncms/pull/3134) | M2 infrastructure; **not** shim deletion |
| Dual-run selection metrics evidence harness | Open [#3131](https://github.com/intersoftdatalabs-in/percussioncms/issues/3131) / PR [#3136](https://github.com/intersoftdatalabs-in/percussioncms/pull/3136) | M2 measurement; **not** shim deletion |
| Criteria M2 snapshot + operator checklist | [#3132](https://github.com/intersoftdatalabs-in/percussioncms/issues/3132) (this refresh) | Docs only; recorded the pre-removal blocked posture (spent — #2852 retired 2026-11-08) |
| Product/H2 runtime zero-legacy-selection evidence | [#3583](https://github.com/intersoftdatalabs-in/percussioncms/issues/3583) | Assertable product/H2 MODERN selection; **not** M2 PASS; **not** shim deletion |
| M2 re-check after perc.Test modern | [#3738](https://github.com/intersoftdatalabs-in/percussioncms/issues/3738) | Dual-mode waive (empty or `perc.Test`); M3 still FAIL; **not** shim deletion |
| M1 snapshot after perc.Test modern merge + dual-ship product-docs | [#3951](https://github.com/intersoftdatalabs-in/percussioncms/issues/3951) | Empty Widget XML waiver; native-only product page help; DualShip **code kept**; **not** #2852 |
| M2 evidence re-verify (harness re-run + inventory + criteria snapshot) | [#5756](https://github.com/intersoftdatalabs-in/percussioncms/issues/5756) | [m2-evidence-snapshot-2026-11-08.md](./m2-evidence-snapshot-2026-11-08.md): probes green (13+16+22), M1 companion 0/81/empty-waiver; M2 PARTIAL / M3 FAIL; live time-box + support inventory need support/product human; **not** #2852 |
| M2 evidence refresh on current main (harness re-run + inventory + criteria snapshot) | [#5780](https://github.com/intersoftdatalabs-in/percussioncms/issues/5780) | [m2-evidence-snapshot-2026-11-08-5780.md](./m2-evidence-snapshot-2026-11-08-5780.md): probes green (13+16+22) on `e75727d1a9`, M1 companion 0/81/empty-waiver; M2 PARTIAL / M3 FAIL; live time-box + support inventory need support/product human; **not** #2852 |
| Template/assembler shim deprecation audit (3/3) + removal plan | [#7051](https://github.com/intersoftdatalabs-in/percussioncms/issues/7051) | [shim-deprecation-audit-7051.md](./shim-deprecation-audit-7051.md): S1–S10 verdicts (shim policy + tests keep; GadgetRegistry single-load keep; DualShip helpers keep; compilers + inventory gates keep permanently); M1 recount on current main (Widget XML 0, waiver empty, `component-package.json` 77 — new baseline vs 81 snapshot); ordered plan, smallest-first Step 1 = delete dead `GadgetRegistry.xml` classpath resource → residual [#7094](https://github.com/intersoftdatalabs-in/percussioncms/issues/7094) (executed — PR #7111 merged), Step 2 Java-ref sweep + absence guard [#7107](https://github.com/intersoftdatalabs-in/percussioncms/issues/7107) (executed — PR #7137 merged), Step 3 dual-run/criteria doc refresh [#7108](https://github.com/intersoftdatalabs-in/percussioncms/issues/7108) (this change); docs-only, no deletion |
| Page dual-ship code retirement | [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md) | Parallel, not identical. Product ship path retired; DualShip call sites still present |

**#2852 gate statement (authoritative for agents — SPENT 2026-11-08, kept as the historical record of the bar):** #2852 closed having met this gate (M1 PASS, M2 retired to modern-only, M3 PASS owner upgrade-YES, G1–G6 evidenced on the removal PR); live-telemetry remainder rides #5808. Original text: implement shim/fallback deletion **only** after this doc’s Status snapshot shows **M1 PASS**, **M2 PASS** (zero or waived legacy rate with attached evidence), **M3 PASS** (or waived residual customers), and **G1–G6 PASS** on the removal PR. Until then leave dual-run code in place.

---

## See also

- [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md) — operator dual-run policy + H2/product checklist
- [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md) — package-build dual-ship
- [widget-xml-inventory.md](./widget-xml-inventory.md) / [page-definition-inventory.md](./page-definition-inventory.md) / [gadget-definition-inventory.md](./gadget-definition-inventory.md)
- [component-package-manifest.md](./component-package-manifest.md) / [adr/004-no-definition-xml-packaging.md](./adr/004-no-definition-xml-packaging.md)
- [plan.md](./plan.md) § Phase 5
- Epic [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) · Phase 5 [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632) · Phase 3 [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) · Removal residual [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) · Criteria refresh [#3132](https://github.com/intersoftdatalabs-in/percussioncms/issues/3132)
