# M2 evidence snapshot — 2026-11-08 (#5780)

| Field | Value |
|-------|-------|
| **Date (UTC)** | 2026-11-08 |
| **Base** | `origin/main` `e75727d1a9` |
| **Issue** | [#5780](https://github.com/intersoftdatalabs-in/percussioncms/issues/5780) — M2 refresh for [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) |
| **Parent** | [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) Phase 5 [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632) |
| **Verdict** | **M2 stays PARTIAL / FAIL overall. M3 stays FAIL. Shim kept. #2852 stays blocked.** No waiver claimed (a waiver needs an owner + sunset date; none recorded). |

## 1. What this snapshot re-verifies

Prior snapshot: 2026-11-08 (#5756, base `572d5557ad`) — M1 PASS (empty Widget
waiver), M2 PARTIAL/FAIL, M3 FAIL. This snapshot re-runs the CI-assertable M2
probes on current `main` (after #5781, #5782, #5785, #5789-branch work) and
records the measured state. It does **not** close the live time-box or the
support inventory — those need production/support data no agent H2/QA cell can
produce (see §4).

## 2. M1 companion inventory (re-measured on this base)

| Metric | #5756 (`572d5557ad`) | #5780 (this snapshot) |
|--------|----------------------|------------------------|
| Committed Widget def XML under `Packages/**/sys__UserDependency--rxconfig/Widgets/` | 0 | **0** (`find … -path "*sys__UserDependency--rxconfig/Widgets/*.xml" \| wc -l` → 0) |
| `component-package.json` under `Packages/` | 81 | **81** |
| `PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS` | empty | **empty** (source-verified `PSWidgetDefinitionXmlInventory.java:54-55`; asserted green by `PSProductPackageRootSelectionEvidenceTest` — see §3) |

M1 ship-path stays PASS. **M1 PASS does not unlock #2852** (M2/M3 still block).

## 3. M2 CI-assertable probes (re-run on this base, JDK 21)

M2 instrumentation is present on `main` (unchanged since #3024/#3025/#3130/#3131):

- Widget: `PSWidgetDao` modern-first `selectDefinition` + `getLastSelectionKind()` /
  `getSelectionKindsById()` + cumulative `getModernSelectionCount()` /
  `getLegacySelectionCount()` / `getTotalSelectionCount()` + `getSelectionMetricsSnapshot()`
  + `formatSelectionMetricsSummary()` + `resetSelectionMetrics()` + INFO
  `Widget definition dual-run selection: modern=…, legacyWidgetXml=…`.
- Gadget: `GadgetRegistry` dual-load (modern `gadget-catalog.json` preferred, legacy
  `GadgetRegistry.xml` fallback) + `getLastLoadSource()` / `getLastLoadEntryCount()` +
  cumulative `getModernLoadCount()` / `getLegacyLoadCount()` / `getNoneLoadCount()` +
  snapshot/summary/reset + INFO `Gadget registry dual-load selection: …`.

Results (focused Surefire runs, no skips, JDK 21 `java-21-openjdk` 21.0.13):

| Probe | Command (from module dir, repo-root `mvnw`, JDK 21) | Result |
|-------|------------------------------------------------------|--------|
| Product package-root selection evidence (perc-packages) | `cd modules/perc-packages && ../../mvnw test -Dtest=PSProductPackageRootSelectionEvidenceTest` | **BUILD SUCCESS — Tests run: 13, Failures: 0, Errors: 0** (full log: `scratch/m2-5780-perc-packages.log`). Product/H2 roots `wouldUseLegacyShim == false`, zero non-waived `LEGACY_WIDGET_XML`; customer-only XML still selects legacy (shim kept). |
| Widget DAO selection + metrics + H2 zero-legacy (sitemanage) | `cd projects/sitemanage && ../../mvnw test -Dtest=PSWidgetDaoTest,PSWidgetDaoSelectionMetricsHarnessTest,PSWidgetDaoProductH2ZeroLegacySelectionTest` | **BUILD SUCCESS — Tests run: 16, Failures: 0, Errors: 0** (`PSWidgetDaoTest` 10/10 + Harness 5/5 + H2 1/1; full log: `scratch/m2-5780-sitemanage.log`). INFO `Widget definition dual-run selection` lines show modern-first on product stems; customer-only ids still select `LEGACY_WIDGET_XML` by design (shim kept). Pre-existing raw-type `PSCollection` unchecked warnings only (untouched files, no new warnings from this docs-only change). |
| Gadget registry dual-load + metrics (WebUI) | `cd WebUI && ../mvnw test -Dtest=GadgetRegistryTest,GadgetRegistrySelectionMetricsHarnessTest` | **BUILD SUCCESS — Tests run: 22, Failures: 0, Errors: 0** (`GadgetRegistryTest` 15/15 + Harness 7/7; full log: `scratch/m2-5780-webui.log`). INFO `Gadget registry dual-load selection` lines confirm modern-preferred with legacy fallback retained; product classpath reports `MODERN_CATALOG`. |

## 4. What is still missing for M2 PASS (unchanged)

1. **Live time-box legacy rate:** process-lifetime `legacyWidgetXml` /
   `getLegacySelectionCount()` = 0 (or explicitly waived) over an agreed window on a
   prod-like/support dataset, captured via the INFO `dual-run selection` /
   `dual-load selection` lines or `formatSelectionMetricsSummary()` from a support
   probe. No agent H2/QA cell carries production traffic; **needs a live
   production/support install — owner: support/product human.**
2. **Support inventory of customer packages still shipping definition XML**
   (companion to the M1 ship-path PASS, which covers product packages only).
   **Needs support ticket/sample data — owner: support human.**
3. **Waiver (if pursued instead of zero):** explicit owner + sunset date. None
   recorded. Agents must not self-waive.

## 5. M3 (unchanged)

Customer upgrade window still **open by design** for 8.2 dual-run. No support
residual list closed. **FAIL.** Human-only decision (support/product); out of
scope for #5780.

## 6. Decision

- M2: **PARTIAL / FAIL overall** (product/H2 selection evidence green; live
  time-box + support inventory open).
- M3: **FAIL.**
- May agents delete `PSLegacyDefinitionXmlShim` / related dual-run loaders now? **No.**
- Is #2852 ready to implement? **No — still blocked** until M1–M3 + G1–G6 all
  PASS (or waived with owner + sunset and evidence).
- Criteria doc updated: dated snapshot notes in M2/M3 sections + this file
  linked; Status snapshot table **left as-is** (still authoritative: not met).
