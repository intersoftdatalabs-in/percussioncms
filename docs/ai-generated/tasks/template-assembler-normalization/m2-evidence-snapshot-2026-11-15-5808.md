# M2 evidence snapshot — 2026-11-15 (#5808)

| Field | Value |
|-------|-------|
| **Date (UTC)** | 2026-11-15 |
| **Base** | `origin/main` `e040e2039b` |
| **Issue** | [#5808](https://github.com/intersoftdatalabs-in/percussioncms/issues/5808) — production telemetry: legacy definition-XML load rate + customer support inventory (for [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852)) |
| **Parent** | [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) Phase 5 [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632) |
| **Verdict** | **Selection retired modern-only (#2852 merged). CI-assertable probes green on this base (13+16+22). Live time-box legacy rate + customer support inventory still open — owner: support/product human. No waiver claimed (agents must not self-waive).** |

## 1. What this snapshot re-verifies

Prior snapshot: 2026-11-08 (#5780, base `e75727d1a9`) — M2 PARTIAL/FAIL,
M3 FAIL, shim kept, #2852 blocked. Since then the #2852 removal change merged:
`PSLegacyDefinitionXmlShim` legacy fallback hard-disabled (modern-only
`selectByPresence` / `selectForPackageRoot` / `selectDefinition`; legacy-only
resolves to retired error / null-kind, never `LEGACY_*`), and the criteria doc
records M2 as **RETIRED — modern-only selection** with the live-telemetry
remainder QA-tracked in #5808. This snapshot re-runs the CI-assertable M2
probes on current `main` and records the measured state. It does **not** close
the live time-box or the support inventory — those need production/support
data no agent H2/QA cell can produce (see §4).

## 2. M1 companion inventory (re-measured on this base)

| Metric | #5780 (`e75727d1a9`) | #5808 (this snapshot) |
|--------|----------------------|------------------------|
| Committed Widget def XML under `Packages/**/sys__UserDependency--rxconfig/Widgets/` | 0 | **0** (`find … -path "*sys__UserDependency--rxconfig/Widgets/*.xml" \| wc -l` → 0) |
| `component-package.json` under `Packages/` | 81 | **81** |
| `PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS` | empty | **empty** (source-verified `PSWidgetDefinitionXmlInventory.java:54-55`; asserted green by `PSProductPackageRootSelectionEvidenceTest` — see §3) |

M1 ship-path stays PASS. **M1 PASS does not close #5808** (live time-box +
support inventory still open — see §4).

## 3. M2 CI-assertable probes (re-run on this base, JDK 21)

Post-#2852 selection is modern-only; the probes now assert retired semantics:

- Widget: `PSLegacyDefinitionXmlShim.wouldUseLegacyShim` always false;
  retired legacy-only XML resolves to null-kind / retired-error (never
  `LEGACY_*`); `PSWidgetDao` modern-only with unchanged metric keys for #5808
  telemetry (`getLastSelectionKind()` / `getSelectionKindsById()` + cumulative
  modern/legacy/total counters + snapshot/summary/reset + INFO
  `Widget definition dual-run selection: modern=…, legacyWidgetXml=…`).
- Gadget: `GadgetRegistry.loadGadgetTypeMap()` modern `gadget-catalog.json`
  only (#5810); retired `GadgetRegistry.xml` never loaded; legacy counter
  always 0; source/summary keys retained for QA telemetry.

Results (focused Surefire runs, no skips, JDK 21 `java-21-openjdk` 21.0.13):

| Probe | Command (from module dir, repo-root `mvnw`, JDK 21) | Result |
|-------|------------------------------------------------------|--------|
| Product package-root selection evidence (perc-packages) | `cd modules/perc-packages && ../../mvnw test -Dtest=PSProductPackageRootSelectionEvidenceTest` | **BUILD SUCCESS — Tests run: 13, Failures: 0, Errors: 0** (full log: `scratch/m2-5808-perc-packages.log`). Product/H2 roots `wouldUseLegacyShim == false`, zero non-waived `LEGACY_WIDGET_XML`; retired customer-only XML resolves to null-kind / retired error (never selected). |
| Widget DAO selection + metrics + H2 zero-legacy (sitemanage) | `cd projects/sitemanage && ../../mvnw test -Dtest=PSWidgetDaoTest,PSWidgetDaoSelectionMetricsHarnessTest,PSWidgetDaoProductH2ZeroLegacySelectionTest` | **BUILD SUCCESS — Tests run: 16, Failures: 0, Errors: 0** (`PSWidgetDaoTest` 10/10 + Harness 5/5 + H2 1/1; full log: `scratch/m2-5808-sitemanage.log`). Modern-only selection on product stems; customer-only XML surfaces the modern-only retired error (shim hard-disabled). |
| Gadget registry dual-load + metrics (WebUI) | `cd WebUI && ../mvnw test -Dtest=GadgetRegistryTest,GadgetRegistrySelectionMetricsHarnessTest` | **BUILD SUCCESS — Tests run: 22, Failures: 0, Errors: 0** (`GadgetRegistryTest` 15/15 + Harness 7/7; full log: `scratch/m2-5808-webui.log`). Modern-catalog-only loads; legacy counter always 0; product classpath reports modern. |

No new warnings from this docs-only change (focused `test` runs only; no
production sources touched).

## 4. What is still missing for #5808 PASS (unchanged — human-owned)

1. **Live time-box legacy rate:** process-lifetime `legacyWidgetXml` /
   `getLegacySelectionCount()` = 0 (or explicitly waived) over an agreed window
   on a prod-like/support dataset, captured via the INFO `dual-run selection` /
   `dual-load selection` lines or `formatSelectionMetricsSummary()` from a
   support probe. No agent H2/QA cell carries production traffic; **needs a
   live production/support install — owner: support/product human.**
2. **Support inventory of customer packages still shipping definition XML**
   (companion to the M1 ship-path PASS, which covers product packages only).
   **Needs support ticket/sample data — owner: support human.**
3. **Waiver (if pursued instead of zero):** explicit owner + sunset date. None
   recorded. Agents must not self-waive.

## 5. M3 (unchanged)

Customer upgrade window **closed per owner decision 2026-11-08 (upgrade-YES,
recorded on #2852)**. Residual-customer list (if any) rides with this #5808
tracking.

## 6. Decision

- M2 selection: **RETIRED modern-only** (#2852 merged); CI probes green on
  this base (13+16+22); live time-box + support inventory **open** (#5808).
- May agents delete further dual-run loaders now? **Out of scope** — gadget
  dual-load retired via #5810; this slice is evidence only.
- Does this snapshot close #5808? **No** — PASS needs the §4 live/support
  evidence attached here by a support/product human (or an explicit
  owner+sunset waiver). This snapshot only refreshes the CI-assertable half.
- Criteria doc updated: dated snapshot note in the M2 section + link to this
  file (see `definition-xml-shim-removal-criteria.md`).

> Co-Authored by OpenCode 2.3.0 using muse-spark-1.3-contributor with agent night-issue-prs.
