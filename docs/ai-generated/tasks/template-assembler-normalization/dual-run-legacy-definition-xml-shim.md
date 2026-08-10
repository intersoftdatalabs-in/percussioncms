# Dual-run: legacy definition-XML runtime shim

| Field | Value |
|-------|--------|
| **Status** | Accepted for Phase 3 slice 3 (#2752) |
| **Parent** | [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) (Phase 3) |
| **Epic** | [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) |
| **ADR** | [ADR-004](./adr/004-no-definition-xml-packaging.md) |
| **Code** | `com.percussion.packages.shim.PSLegacyDefinitionXmlShim` (`modules/perc-packages`) |
| **Related slices** | Manifest model #2750 / PR #2754; Widget XML compiler #2751 / PR #2755 |

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
| Widget definitions (install) | `${rxdeploydir}/rxconfig/Widgets/*.xml` via `PSWidgetDao` (`projects/sitemanage/.../dao/impl/PSWidgetDao`) | Loads legacy Widget XML by file id | Prefer modern package when present; XML remains fallback for unconverted customer defs |
| Package source trees | `modules/perc-packages/src/main/resources/Packages/<pkg>/` | Ships product `.ppkg` content including `sys__UserDependency--rxconfig/Widgets/*.xml` | Product conversion (#2751+) emits modern manifest; product source of truth moves off XML (ADR-004) |
| Package staging Widgets | `sys__UserDependency--rxconfig/Widgets/` or `rxconfig/Widgets/` under a package root | Upgrade-input / deploy layout | Shim package-root API resolves either layout |
| Gadget registry | `WebUI/.../GadgetRegistry.xml` (+ residual definition XML) | Registry; per-gadget XML largely gone | Modern: `gadget-catalog.json` + per-gadget packages (#2771); dual-load residual for WebUI |
| Page meta / definition XML | Site/page storage dialects | Composition authoring legacy | Page conversion is Phase 3 residual; shim recognizes `rxconfig/Pages` if present |

**Selection API (no Spring wiring required for unit use):**

- `PSLegacyDefinitionXmlShim.selectByPresence(...)` — pure flags (tests / metrics)
- `PSLegacyDefinitionXmlShim.selectForPackageRoot(Path)` — package directory
- `PSLegacyDefinitionXmlShim.selectDefinition(id, modernRoots, widgetsDir, pagesDir, gadgetsDir)` — dual-run by id

Deep rewiring of `PSWidgetDao` to call the shim for every load is **incremental** and may land with residual wiring after modern packages appear on disk; this slice ships the **policy + tested selection** and operator docs.

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

1. **Inventory** customer Widget XML under install `rxconfig/Widgets` (and package archives if applicable).
2. **Convert** using the Widget XML → Component Package Manifest compiler when available (#2751).
3. **Deploy** modern packages (`component-package.json` + artifacts) beside or instead of XML.
4. **Verify** selection: modern present ⇒ modern wins even if old XML remains on disk.
5. **Remove** legacy XML after smoke/parity for converted definitions.
6. **Exit dual-run** when no required customer definitions rely on the XML path (Phase 5).

## Exit criteria (shim retirement)

The dual-run window ends when **all** of the following hold:

- [ ] Product packages in repo/install are **not** authored as Page / Widget / Gadget definition XML (ADR-004 ship bar).
- [ ] Customer upgrade path documented and used for remaining XML.
- [ ] Runtime metrics (or support inventory) show no production dependence on legacy definition XML loads — or remaining cases are explicitly waived.
- [ ] Phase 5 (#2632) removes or hard-disables the shim and updates help.

## Non-goals

- Completing full product package conversion (sibling #2750 / #2751 and residuals)
- Multi-RDBMS matrix or host-only install validation
- Phase 4 Design SPA (#2631) or full Phase 5 help rewrite (#2632)

## See also

- [ADR-004](./adr/004-no-definition-xml-packaging.md) — no definition XML for product packaging
- [component-package-manifest.md](./component-package-manifest.md) — modern ship format (when present on branch)
- [plan.md](./plan.md) — Phase 3 goals
- [widget-xml-inventory.md](./widget-xml-inventory.md) — product widget matrix
