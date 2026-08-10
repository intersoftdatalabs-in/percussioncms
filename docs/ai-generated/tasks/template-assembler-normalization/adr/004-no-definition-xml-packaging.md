# ADR-004: No Page / Widget / Gadget XML definition files for product packaging

| Field | Value |
|-------|--------|
| **Status** | Accepted |
| **Date** | 2026-08-09 |

## Decision

When 8.2 product packages **ship**, they are **not** authored or maintained as:

- `rxconfig/Widgets/*.xml` widget definitions
- Page meta/definition XML dialects (region/widget blob as the authoring format)
- Per-gadget OpenSocial-style definition XML files

### Allowed transitional uses

1. **Upgrade input** — compile/convert tools read old XML.
2. **Time-boxed runtime shim** — load customer XML if modern package not present.

Product **source of truth** becomes:

```text
Content types + Templates (assembler + JEXL bindings + source)
  + Slots (incl. layout/styles) + Catalog metadata + Resources
```

## Evidence

- **Widgets:** 48 product XML defs under `perc-packages` (see widget inventory).
- **Gadgets:** registry XML remains; per-gadget definition files already largely absent from tree (see gadget inventory) — finish the job with a proper catalog.

## Consequences

- Compiler/upgrade tooling is mandatory before deleting package XML.
- Widget Builder / Design tools write modern format only.
- Package install/export paths must understand the new manifest shape (prefer extending existing package system).
- **Dual-run runtime shim (time-boxed):** selection prefers modern `component-package.json`, falls back to legacy Widget/Page/Gadget definition XML when modern is absent, and fails clearly when neither exists. Implementation: `com.percussion.packages.shim.PSLegacyDefinitionXmlShim` (#2752). Operator policy, entry points, and exit criteria: [dual-run-legacy-definition-xml-shim.md](../dual-run-legacy-definition-xml-shim.md). Product packages must not depend on the shim long-term; Phase 5 (#2632) removes it when metrics allow.

## Component Package Manifest (schema v1.0)

Ship-format model and docs landed as Phase 3 slice 1 (#2750):

| Artifact | Location |
|----------|----------|
| Schema / field docs | [../component-package-manifest.md](../component-package-manifest.md) |
| Java model + IO + validation | `modules/perc-packages/.../manifest/PSComponentPackageManifest*.java` |
| Minimal fixture | `modules/perc-packages/src/test/resources/manifests/minimal-component-package.json` |

**Upgrade-input XML** remains compiler/shim only; **product ship format** is `component-package.json` plus content types, templates, slots, catalog metadata, and resources.

## Widget XML compiler (slices #2751 / #2772)

Compiler for upgrade-input Widget XML → Component Package Manifest (baseWidgets + high-traffic batch):

| Artifact | Location |
|----------|----------|
| Parser / compiler / package scanner | `modules/perc-packages/.../widgetxml/PSWidgetXml*.java` |
| Golden parity (simple + high-traffic) | `modules/perc-packages/src/test/resources/widgetxml/golden/` |
| Inventory + residuals | [../widget-xml-inventory.md](../widget-xml-inventory.md) |

Remaining long-tail product packages and product Widget XML removal remain residual under #2630.

## Page templateDef compiler (slice #2770)

Compiler for upgrade-input Page / assembly `*.templateDef` → Component Package Manifest (product page layout packages first):

| Artifact | Location |
|----------|----------|
| Parser / compiler / package scanner | `modules/perc-packages/.../pagexml/PSPageXml*.java` |
| Golden parity (`perc.base.plain`) | `modules/perc-packages/src/test/resources/pagexml/golden/` |
| Inventory + dual-run note | [../page-definition-inventory.md](../page-definition-inventory.md) |

**#2786 (landed):** `perc.baseTemplates` and `perc.responsiveTemplates` **author** modern `pages/<id>/component-package.json` + sources. Package build dual-ships install `*.templateDef` via `PSPageXmlDualShip` / `PSPackageBuilder` so deployer `TemplateDef` install parity is preserved. Baseline system templates and native modern install remain residual under #2630.

## Gadget registry compiler (slice #2771)

Compiler for upgrade-input `GadgetRegistry.xml` → aggregate `gadget-catalog.json` + per-gadget `component-package.json` (`catalog.kind = "gadget"`):

| Artifact | Location |
|----------|----------|
| Parser / compiler / catalog IO | `modules/perc-packages/.../gadgetxml/PSGadgetRegistry*.java`, `PSGadgetCatalog*.java` |
| Product modern catalog | `modules/perc-packages/src/main/resources/catalogs/gadgets/gadget-catalog.json` |
| Golden parity (Welcome + full catalog) | `modules/perc-packages/src/test/resources/gadgetxml/golden/` |
| Inventory + residuals | [../gadget-definition-inventory.md](../gadget-definition-inventory.md) |

Gadgets are SPA/dashboard hosts (not assembly templates). Validator allows gadget packages without `contentTypes[]` / `templates[]` when `catalog.kind = "gadget"` and `catalog.title` is set. WebUI dual-load of JSON catalog (retire `GadgetRegistry.xml` at runtime) remains residual.
