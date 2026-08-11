# Dual-ship Widget XML exit checklist

| Field | Value |
|-------|--------|
| **Status** | Active — batch A modern authoring roots landed (#2831) |
| **Parent** | [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) · Grandparent [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) |
| **Related** | Compiler #2751 / #2772 / #2789 / #2802 · Shim #2752 · Phase 5 #2632 · Page dual-ship #2786 / native #2806 |
| **Code** | `PSWidgetXmlDualShip`, `PSWidgetXmlCompiler`, `PSLegacyDefinitionXmlShim` |

## Purpose

Product widget packages still ship legacy `sys__UserDependency--rxconfig/Widgets/*.xml` so deployer / `PSWidgetDao` install is unchanged. Batch A **authors** modern `widgets/<stem>/component-package.json` + template sources (ADR-004) so dual-run selection prefers the Component Package Manifest when both exist.

This document is the **operator / engineering checklist** for exiting dual-ship Widget XML package-by-package. **Do not** mass-delete remaining product Widget XML until a native install path exists (or a dual-ship install emitter regenerates install XML from modern).

## Authoring vs install

| Layer | Batch A (after #2831) | Other product widgets |
|-------|----------------------|------------------------|
| **Authoring truth** | `widgets/<stem>/component-package.json` + `templates/*.vm` | Still Widget XML (compile path validated; modern roots residual) |
| **Install wire format** | Still `sys__UserDependency--rxconfig/Widgets/*.xml` dual-ship | Same |
| **Selection** | `PSLegacyDefinitionXmlShim` prefers modern `widgets/` (or root) over legacy XML | Legacy Widget XML until modern roots land |

### Configuration / APIs

| Knob / API | Notes |
|------------|--------|
| `PSWidgetXmlDualShip.materializeModernWidgetSources(packageDir)` | Widget XML → `widgets/` (migration / refresh) |
| `PSWidgetXmlDualShip.materializeModernBatchA(packagesRoot)` | Batch A packages only |
| `PSWidgetXmlDualShip.hasModernWidgetSources` / `compileModernWidgets` | Product parity tests |
| CLI | `PSWidgetXmlDualShip materialize-modern\|materialize-modern-batch-a <path>` |

Policy alignment: modern preferred in `PSLegacyDefinitionXmlShim` (root `component-package.json` **or** `widgets/<stem>/component-package.json`).

## Packages on modern dual-ship authoring (batch A)

| Package | Widgets | Notes |
|---------|---------|-------|
| `perc.baseWidgets` | percSimpleText, percRichText, percRawHtml | Core content widgets; goldens exist |
| `perc.defaultLanguage` | percDefaultLang, percLocalLang | Multi-widget language package |
| `perc.eventWidget` | percEvent | Content CT; golden |
| `perc.openGraphWidget` | percOpenGraph | Social meta |
| `perc.twitterSummaryCards` | percTwitterSummaryCards | Social meta |

**Batch A total:** 5 packages · **8** widgets with modern roots. Product inventory remains **48** Widget XML files (including `perc.Test`); modern dual-ship does **not** delete install XML.

## Retirement checklist (per package)

1. **Confirm compiler goldens / package compile** — `PSWidgetXmlPackageCompiler.compilePackage` green for the package.
2. **Materialize modern** — `widgets/<stem>/component-package.json` + templates committed (or refresh via `materializeModernWidgetSources`).
3. **Parity test** — modern manifest/template equals compile-from-XML (`PSWidgetXmlDualShipTest` pattern).
4. **Shim** — `selectForPackageRoot` / `selectDefinition` prefer modern when XML co-located.
5. **Keep install XML** until native widget install (or reverse emitter) exists.
6. **Later residual** — optional native install mode (page peer: `PSPageXmlNativeInstall` / #2806); then remove dual-ship XML for converted packages only.

## Residual after batch A

| Residual | Scope | Guidance |
|----------|-------|----------|
| High-traffic dual-ship roots | title, lists, nav, file, image (7) | Next coherent batch |
| Long-tail dual-ship roots | blog/calendar/directory/social/forms… | After high-traffic |
| Remaining product dual-ship roots | auto-lists, companions, login variants… | After long-tail |
| Native widget install | Package build stages install Widget XML (or new wire format) from modern | Peer of #2806; required before mass XML delete |
| Global shim removal | #2632 | Metrics + zero required legacy loads |

Approximate residual Widget XML still dual-shipped as **install** source of truth for authoring: **~40** product widgets without modern `widgets/` roots (48 − 8 batch A; still 48 XML files on disk until native exit).

## Dual-run / dual-ship relationship

| Concept | Layer | Status |
|---------|-------|--------|
| Dual-run **definition XML shim** | Runtime selection modern vs Widget XML | Time-boxed; Phase 5 #2632 |
| Dual-ship **widget modern roots** | Package **authoring** under `widgets/` | Batch A (#2831); more batches residual |
| Dual-ship **page templateDef** | Package-build install bridge | Optional; native preferred for base/responsive (#2806) |
| Native **widget install** | Package-build stages install artifacts from modern | **Not landed** — do not delete product Widget XML |

## See also

- [widget-xml-inventory.md](./widget-xml-inventory.md)
- [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md)
- [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md)
- [adr/004-no-definition-xml-packaging.md](./adr/004-no-definition-xml-packaging.md)
