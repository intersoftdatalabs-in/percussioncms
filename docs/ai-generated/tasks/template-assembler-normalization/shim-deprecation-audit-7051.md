# Legacy template-model shim deprecation audit + removal plan (#7051)

| Field | Value |
|-------|-------|
| **Issue** | [#7051](https://github.com/intersoftdatalabs-in/percussioncms/issues/7051) (slice 3/3; sibling slices: [#7050](https://github.com/intersoftdatalabs-in/percussioncms/issues/7050) shared-snippet dedupe) |
| **Parent** | [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) (epic) · Phase 5 [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632) |
| **Date / branch** | 2026-11-22 · `origin/main` (`863903181e`) · docs-only, **no production deletion** |
| **Criteria** | [definition-xml-shim-removal-criteria.md](./definition-xml-shim-removal-criteria.md) (Status **RETIRED / MET** 2026-11-08) |
| **Operator policy** | [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md) (Status **RETIRED** 2026-11-08) |
| **Product help** | `product-docs/8.2/admin/definition-xml-dual-run.md` (`id: admin-definition-xml-dual-run`, already retired-noted) |

## Scope

Audit the remaining legacy template-model shim surface now that the
`LEGACY_WIDGET_XML` (#6440), `LEGACY_PAGE_XML` (#6446), and
`LEGACY_GADGET_XML` (#6447) product load paths are removed:
`PSLegacyDefinitionXmlShim` policy + tests, `GadgetRegistry` dual-load
fallback, `PSPageXmlDualShip` code kept for the legacy-upgrade emit path,
and the dual-run operator docs.

**Not a removal.** Shim/fallback deletion stays gated on the criteria doc
Status snapshot (M1 PASS, M2 retired-selection PASS with live-telemetry
remainder QA-tracked in
[#5808](https://github.com/intersoftdatalabs-in/percussioncms/issues/5808),
M3 PASS, G1–G6 PASS). Per-slice rule: produce verdicts, not deletions.

## 1. M1 recount on current main (2026-11-22)

Re-measured from the worktree on `origin/main` (`863903181e`):

| Metric | Command | Result | Criteria snapshot | Verdict |
|--------|---------|--------|-------------------|---------|
| Committed Widget definition XML under `Packages/**/sys__UserDependency--rxconfig/Widgets/` | `find modules/perc-packages/src/main/resources/Packages -path "*sys__UserDependency--rxconfig/Widgets/*.xml" \| wc -l` | **0** | 0 (all snapshots since #3951) | M1 PASS holds |
| `WAIVED_PACKAGE_DIRS` | `PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS` (source) | **empty** | empty | M1 PASS holds |
| `component-package.json` under Packages | `find modules/perc-packages/src/main/resources/Packages -name component-package.json \| wc -l` | **77** | 81 (2026-11-08 snapshots) | Drift only (§1.1) |
| Remaining `.xml` under `Packages/perc.Test` / `perc.baseWidgets` | `find … -path "*Widgets*" -name "*.xml"` | only `psx_archiveInfo.xml` / `psx_archiveManifest.xml` (package archive manifests, **not** widget definition XML) | n/a | Not a regression |

### 1.1 `component-package.json` 81 → 77 drift

The four-package delta post-dates the 2026-11-08 snapshots and tracks
intervening product-package churn on `Packages/` (recent history is
calendarTwo v6 cleanup/polish: `b6e8dce874` #6994, `cd78a6adbe` #6977,
`07d21084f8` #7029, `55b5eca22d` #7062). Direction is consolidation, not
reintroduction of definition XML (Widget def XML stays **0**, waiver stays
**empty**). No criteria action required; next M1 snapshot should record 77
as the new baseline.

## 2. Audit table

Verdict vocabulary: **keep** (load-bearing or CI-gate surface, no removal),
**retire (gated)** (dead or retired-shape surface with a concrete ordered
step in §3 — executed only via its own residual + gates), **keep (doc-only
note)** (deprecation is recorded in prose, no code marker applied).

| # | Surface | Path / class | Status on `main` | Verdict | Rationale + gate |
|---|---------|--------------|------------------|---------|------------------|
| S1 | Legacy selection policy | `modules/perc-packages/.../shim/PSLegacyDefinitionXmlShim.java` (+ `PSDefinitionSourceKind` — `MODERN_COMPONENT_PACKAGE` only; `PSDefinitionSourceSelection`; `PSDefinitionSourceNotFoundException`) | RETIRED javadoc; modern-only `selectByPresence` / `selectForPackageRoot` / `selectDefinition`; no `LEGACY_*` kind exists; retired-error names the retired XML + compiler path | **keep** | Still the production selection policy: `PSWidgetDao.selectDefinitionSource` + poll-time `recordSelectionKinds` call it (S3), and the M2 evidence harness asserts through it (S2). Deletion = inlining modern resolution into callers — behavior-neutral but wide, gated on the §3 step order, **not** this slice. Gate for any future deletion: G1 (perc-packages) + G5 (sitemanage) clean installs, G2 modern-only tests stay green |
| S2 | M2 measurement + defaults infra | `PSModernPackageRootDefaults`, `PSProductPackageRootSelectionEvidence` (+ `PSLegacyDefinitionXmlShimTest`, `PSModernPackageRootDefaultsTest`, `PSProductPackageRootSelectionEvidenceTest`, `PSCustomerLegacyXmlUpgradePathTest`) | Modern defaults (`Packages/Modern` + classpath materialize); evidence harness pins `wouldUseLegacyShim == false` / MODERN kind; customer-legacy upgrade-path test documents the compiler route | **keep** | CI-assertable M2 evidence + product/H2 defaults. Deleting it removes the measurement that guards the gate. Gate: none (keep permanently while #5808 QA tracking is open) |
| S3 | Widget DAO shim wiring + metrics | `projects/sitemanage/.../dao/impl/PSWidgetDao.java` (imports + calls `PSLegacyDefinitionXmlShim.selectDefinition`; `getLastSelectionKind` / `getSelectionKindsById` / snapshot / summary; + `PSWidgetDaoTest`, `PSWidgetDaoSelectionMetricsHarnessTest`, `PSWidgetDaoProductH2ZeroLegacySelectionTest`) | Modern-only selection, INFO `single-source selection` line, snapshot keys `modern`/`total` (legacy keys removed #6440) | **keep** | Production widget load path; metrics feed the #5808 live-telemetry remainder. Gate: none (keep) |
| S4 | Gadget registry single-load | `WebUI/src/main/java/com/percussion/webui/gadget/servlets/GadgetRegistry.java` (`Source` = `MODERN_CATALOG` / `NONE` only; + `GadgetRegistryTest`, `GadgetRegistrySelectionMetricsHarnessTest`) | Single-load modern catalog only (dual-load retired #5810, selection remnants removed #6440, legacy parser deleted #6447) | **keep** | Production dashboard gadget path. Gate: none (keep) |
| S5 | Dead classpath resource | `WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml` | Present on classpath, **unreferenced**: no Java/test/resource/config loads it (sweep §2.1 — only prose mentions: javadoc comments, catalog-JSON provenance strings, compiler upgrade-input path params) | **retire (gated)** | Smallest safe first step (§3 step 1). Gate: WebUI `mvnw clean install` green (C1) + `GadgetRegistryTest` green (G2-adjacent). Filed as residual — **not** deleted in this slice |
| S6 | Page / Widget dual-ship helpers | `modules/perc-packages/.../pagexml/PSPageXmlDualShip.java`, `.../widgetxml/PSWidgetXmlDualShip.java` (+ `PSDualShipPageTemplateDefInventory`, tests) | Dual-ship retired as the **product ship path**; code remains as the package-build modern materialize + native-install helper layer — still called by `PSPackageBuilder` (hasModern*/materialize gates), `PSPageXmlPackageCompiler`, `PSPageXmlNativeInstall`, `PSWidgetXmlPackageCompiler`, `PSWidgetXmlInstallEmitter` | **keep** | Load-bearing build/install helpers despite the legacy names. Renaming is churn with no behavior gain (rejected as a step — see §3 non-goals). Gate for any future removal: G1 + full package-compile round-trip evidence |
| S7 | Upgrade-input compilers + parsers | `PSWidgetXml*` compiler/parser/model, `PSPageXml*` compiler/parser/model, `PSGadgetRegistry*` compiler/parser/model, `PSPageXmlTemplateDefEmitter`, `PSWidgetXmlInstallEmitter` | Read legacy XML as **upgrade input only**, emit modern manifests | **keep (permanent)** | Criteria time-box rule 3: compilers may keep reading definition XML as upgrade input forever. Never a removal candidate |
| S8 | Ship-path inventory gates | `PSWidgetDefinitionXmlInventory`, `PSPageDefinitionXmlInventory`, `PSGadgetDefinitionXmlInventory`, `PSDefinitionXmlShipPathInventory`, `PSWidgetArchiveManifestInventory` (+ Surefire gates; `WAIVED_PACKAGE_DIRS` empty) | Green; product tree 0 XML on all three ship paths | **keep (permanent)** | G4 CI gates that fail the build if definition XML reappears. Deleting them un-guards M1 |
| S9 | `@Deprecated` code markers | None applied (no `@Deprecated` on any S1–S8 type) | Deprecation recorded in `RETIRED` javadoc + criteria/dual-run docs + product-docs retired banner | **keep (doc-only note)** | Annotations were considered and **rejected** for this slice: usage sites (`PSWidgetDao`, evidence harness, defaults) would emit new deprecation warnings, violating the no-new-warnings gate unless paired with suppression churn across three modules. The prose record is the deprecation. Revisit only as part of a deletion PR that removes the usages anyway |
| S10 | `PSTypeEnum.LEGACY_CONTENT` + `LEGACY_*` strings elsewhere | `projects/sitemanage/...` apibridge/services (GUID content-type enum, inline-type constants, category titles) | Unrelated content-GUID / inline-type domain — **not** definition-XML load paths (verified: no `LEGACY_WIDGET/PAGE/GADGET` or `LEGACY_REGISTRY` kind remains anywhere in main sources) | **out of scope** | Name collision only. No action; recorded here so future greps for `LEGACY_` do not re-open this audit |

### 2.1 S5 no-reference sweep (evidence)

From repo root on `origin/main`:

```text
# No code, test, config, or resource loads the classpath GadgetRegistry.xml:
grep -rn "GadgetRegistry\.xml" --include="*.java" --include="*.xml" \
  --include="*.properties" --include="*.json" --include="*.ts" WebUI/src/main/ \
  modules/perc-packages/src/main/ projects/sitemanage/src/main/
# → only prose: GadgetRegistry.java javadoc (2×), GadgetRegistryTest comments (2×),
#   gadget-catalog.json "compiled from GadgetRegistry.xml" provenance strings (2×),
#   PSGadgetRegistryCompiler/Parser upgrade-input @params + test fixture path
# (fixture /gadgetxml/GadgetRegistry.xml is a test resource for the compiler,
#  unrelated to the WebUI classpath resource)

# No Spring / webapp / packaging reference:
grep -rn "GadgetRegistry" WebUI/src/main/resources/ WebUI/src/main/webapp/WEB-INF/
# → only the gadget-catalog.json provenance line above

# Only catalog resource is asserted present by tests:
WebUI/src/test/.../GadgetRegistryTest.java:200:
  GadgetRegistry.class.getClassLoader().getResource(GadgetRegistry.CATALOG_RESOURCE) != null
# (CATALOG_RESOURCE = gadget-catalog.json — not the legacy XML)
```

## 3. Ordered removal plan (not executed here)

Smallest safe step first. Each step is its own residual + gates; stop at
any step whose gate fails.

| Order | Step | Touches | Evidence gate | Residual |
|-------|------|---------|---------------|----------|
| **Step 1 (smallest, first)** | Delete the dead WebUI classpath resource `WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml` (S5). Single-file deletion; no code change; sweep §2.1 is the pre-evidence | `WebUI` (resource only) | `cd WebUI && ../mvnw clean install` → BUILD SUCCESS, `GadgetRegistryTest` + harness green, no new warnings (C1) | **Filed this run** (see §5) |
| Step 2 (optional) | `@Deprecated(forRemoval = …)` marker pass over S1 retired-error surface **only together with** its call-site rewiring — never as a standalone annotation sprinkle (see S9) | `modules/perc-packages`, `projects/sitemanage` | G1 both modules + G5 reverse-dep installs, zero new warnings | Future residual, only if a deletion PR wants the marker as a pre-step |
| Step 3 (gated, later) | Inline/delete `PSLegacyDefinitionXmlShim` modern-only selection into `PSWidgetDao` + evidence harness (S1) once the #5808 live-telemetry remainder closes | `modules/perc-packages`, `projects/sitemanage` | M2-closed evidence from #5808 + G1/G2/G5 | Criteria-gated; **do not file until #5808 closes** |
| Non-goals (never) | Delete compilers/parsers (S7) or inventory gates (S8); rename DualShip helpers (S6) for aesthetics | — | — | — |

## 4. Doc updates in this slice

- This audit (`shim-deprecation-audit-7051.md`) — audit table (§2), plan (§3).
- Criteria doc §6 residual row for #7051 (audit filed, S5 residual filed, M1 recount 77 baseline).
- Dual-run operator doc: pointer note to this audit (docs-only, no policy change — dual-run stays RETIRED).
- `product-docs`: **N/A** — no operator-facing behavior change; the dual-run product page already carries the retired banner + M1 evidence and needs no edit for an engineering-internal audit.

## 5. Residuals filed

| Residual | Scope | Labels |
|----------|-------|--------|
| Step-1 deletion of the dead `GadgetRegistry.xml` classpath resource (WebUI, C1-gated) | [#7094](https://github.com/intersoftdatalabs-in/percussioncms/issues/7094) | `operator:opencode`, `operator:night-issue-prs`, `model:muse-spark-1.3-contributor` |

## Acceptance mapping (issue #7051)

- [x] Audit table: each shim/dual-load/dual-ship surface → verdict + rationale + gate — §2 (S1–S10, gates per row).
- [x] Ordered removal plan with smallest safe first step identified — §3 (Step 1: dead `GadgetRegistry.xml` resource deletion).
- [x] Criteria doc residual row + operator dual-run doc notes updated as docs-only — §4 (criteria §6 row + dual-run pointer note, this PR).
- [x] No production-code deletion; docs-only PR acceptable — this PR touches docs only (`docs/ai-generated/tasks/template-assembler-normalization/`).
