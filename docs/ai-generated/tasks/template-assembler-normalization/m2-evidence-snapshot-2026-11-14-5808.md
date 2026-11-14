# M2 evidence snapshot — 2026-11-14 (#5808)

| Field | Value |
|-------|-------|
| **Date (UTC)** | 2026-11-14 |
| **Base** | `origin/main` `4b5137102e` |
| **Issue** | [#5808](https://github.com/intersoftdatalabs-in/percussioncms/issues/5808) — QA (M2): production telemetry + customer support inventory (for #2852) |
| **Parent** | [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) Phase 5 [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632) |
| **Verdict** | **Post-removal re-verify: CI-assertable probes green on the retired modern-only selection. Live-install telemetry + support inventory still open — #5808 stays OPEN. No waiver claimed (a waiver needs an owner + sunset date; none recorded; agents must not self-waive).** |

## 0. Post-removal context (what changed since #5780)

- [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) **merged/CLOSED**: `PSLegacyDefinitionXmlShim` legacy branches hard-disabled — `selectByPresence` / `selectForPackageRoot` / `selectDefinition` return MODERN or throw a retired-error (never `LEGACY_*`); `wouldUseLegacyShim` deprecated to always-false; `PSWidgetDao` rewired modern-only with unchanged metric keys for #5808 telemetry.
- [#5810](https://github.com/intersoftdatalabs-in/percussioncms/issues/5810) **merged/CLOSED**: `GadgetRegistry.loadGadgetTypeMap()` rewired to modern `gadget-catalog.json` only (legacy `GadgetRegistry.xml` never loaded; legacy counter always 0).
- Criteria doc status: **RETIRED / MET 2026-11-08** — M1 PASS, M3 PASS (owner upgrade-YES 2026-11-08), M2 live-telemetry remainder QA-tracked here in #5808.
- Consequence for this snapshot: product/H2 code paths **cannot** select `LEGACY_*` anymore (retired-legacy ids resolve to null-kind in the evidence harness / clear retired error via the DAO). The remaining QA ask is live-install confirmation (zero `LEGACY_*` + zero retired-error noise over the agreed window) plus the support inventory of customer packages still shipping definition XML.

## 1. M1 companion inventory (re-measured on this base)

| Metric | #5780 (`e75727d1a9`) | #5808 (this snapshot) |
|--------|----------------------|------------------------|
| Committed Widget def XML under `Packages/**/sys__UserDependency--rxconfig/Widgets/` | 0 | **0** (`find … -path "*sys__UserDependency--rxconfig/Widgets/*.xml" \| wc -l` → 0) |
| `component-package.json` under `Packages/` | 81 | **81** |
| `PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS` | empty | **empty** (source-verified `PSWidgetDefinitionXmlInventory.java:54-55`; asserted green by `PSProductPackageRootSelectionEvidenceTest` — see §2) |

M1 ship-path stays PASS.

## 2. M2 CI-assertable probes (re-run on this base, JDK 21, no skips)

| Probe | Command (from module dir, repo-root `mvnw`, JDK 21 `21.0.13`, offline) | Result |
|-------|------------------------------------------------------------------------|--------|
| Legacy-shim modern-only + product package-root selection evidence (perc-packages) | `cd modules/perc-packages && ../../mvnw test -Dtest=PSProductPackageRootSelectionEvidenceTest,PSLegacyDefinitionXmlShimTest` | **BUILD SUCCESS — Tests run: 28, Failures: 0, Errors: 0, Skipped: 0** (`PSLegacyDefinitionXmlShimTest` 15/15 modern-only + `PSProductPackageRootSelectionEvidenceTest` 13/13; full log: `scratch/m2-5808-perc-packages.log`) |
| Widget DAO modern-only selection + metrics + H2 zero-legacy (sitemanage) | `cd projects/sitemanage && ../../mvnw test -Dtest=PSWidgetDaoTest,PSWidgetDaoSelectionMetricsHarnessTest,PSWidgetDaoProductH2ZeroLegacySelectionTest` | **BUILD SUCCESS — Tests run: 16, Failures: 0, Errors: 0, Skipped: 0** (`PSWidgetDaoTest` 10/10 + harness 5/5 + H2 1/1; full log: `scratch/m2-5808-sitemanage.log`). Required a producer-first refresh: `cd rest && ../mvnw clean install` → BUILD SUCCESS, Tests run: 1840, Failures: 0, Errors: 0 (local `rest` SNAPSHOT predated the #6066 `PipelineDatasourceBackend`; refresh only, no repo change; full log: `scratch/m2-5808-rest-install.log`) |
| Gadget registry modern-only load + metrics (WebUI, Java) | `cd WebUI && ../mvnw test -Dtest=GadgetRegistryTest,GadgetRegistrySelectionMetricsHarnessTest` | **Java probes green — Tests run: 22, Failures: 0, Errors: 0, Skipped: 0** (`GadgetRegistryTest` 15/15 + harness 7/7; full log: `scratch/m2-5808-webui.log`). The same `mvnw test` invocation also runs the full vitest suite, which hit **environmental flakes under full-suite load, unrelated to this change** (see §3) |

## 3. WebUI vitest note (environmental flake, not M2 evidence)

Two consecutive full runs on this base diverged, both outside any file this
task touches (this task changes docs only):

- Run 1 (`scratch/m2-5808-webui.log`): `Tests 8952 passed (8952)`, but a
  vitest worker was SIGKILLed (OOM) while running
  `ItemFilterDetailPanel.test.tsx` → 936/937 files, BUILD FAILURE.
- Run 2 (`scratch/m2-5808-webui-java.log`): 1 timing flake in
  `WorkflowAssociationDialog.test.tsx` ("Escape cancels without associating",
  `onCancel` called 0 times) → 8971/8972 tests, BUILD FAILURE.

The flaked spec passes in isolation (`npx vitest run
src/test/ts/developer/WorkflowAssociationDialog.test.tsx` → 7/7 passed).
Pre-existing load-sensitive flake; no source changed by this task; Java
GadgetRegistry probes (the actual M2 evidence) are 22/22 green in both runs.

## 4. What is still missing for the #5808 PASS (unchanged, human-owned)

1. **Live time-box legacy rate:** on a production or support-representative
   install (NOT H2/product stems — those already read zero), collect
   `PSWidgetDao` modern-first selection counters + `GadgetRegistry` dual-load
   metrics over the agreed time-box; record `LEGACY_WIDGET_XML` /
   `LEGACY_PAGE_XML` / `LEGACY_GADGET_XML` load kinds + counts (expected: zero —
   selection is modern-only since #2852/#5810; any retired-error noise gets
   recorded instead). **Needs a live production/support install — owner:
   support/product human.**
2. **Support inventory of customer packages still shipping definition XML**
   (companion to the M1 ship-path PASS, which covers product packages only).
   **Needs support ticket/sample data — owner: support human.**
3. **Waiver (if pursued instead of zero):** explicit owner + sunset date. None
   recorded. Agents must not self-waive.

## 5. M3 (unchanged)

Customer upgrade window **closed per owner YES-upgrade 2026-11-08** (recorded
on #2852; criteria M3 PASS). Residual-customer list (if any) rides with the M2
QA tracking here. Human-only follow-through (support/product); out of scope
for this evidence snapshot.

## 6. Decision

- CI-assertable M2 evidence: **green on `4b5137102e`** (28 + 16 + 22 Java
  probes; M1 companion 0/81/empty-waiver).
- Live time-box + support inventory: **open** (support/product human owners;
  no waiver recorded, agents must not self-waive).
- #5808: **stays OPEN** — this snapshot is attached here and linked from the
  criteria doc M2 section; the PASS/waived-with-owner verdict lands via #5808
  when live data arrives.
- Criteria doc updated: dated snapshot note in the M2 section + this file
  linked; retirement status **left as-is**.
