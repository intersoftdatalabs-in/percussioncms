# Template & Assembler Model Normalization (8.2)

| Field | Value |
|-------|--------|
| **Status** | Active — Phase 1 assemblers in progress |
| **Created** | 2026-08-09 |
| **Type** | Product architecture (multi-phase) |
| **GitHub epic** | [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) (all children **p2**) |
| **Phase issues** | #2627 → #2628 → #2629 → #2630 → #2631 / #2632 |
| **Related** | [design-templates-item-types](../design-templates-item-types/README.md); Workbench inventory §7 assembly; `specs/989-react-cui-widget-builder` |

## North star

One **Template** object model (page / snippet / global / binary / resource) + ordered **JEXL bindings** + pluggable **Assemblers** (legacy/XSL, Velocity, Markdown, HTML-first, future). Layout (regions/slots) is a **unified hole model** with optional **`slot_layout` / `slot_styles`**. Product packages ship **without** Page / Widget / Gadget **XML definition files**.

**Explicit non-goal:** migrating bindings or UDFs off JEXL to JavaScript.

## Why Rhino is on the classpath (not template bindings)

| Surface | Mechanism | Role |
|---------|-----------|------|
| Template bindings | Commons JEXL 3 (`PSScript`, `PSTemplateBinding`) | Assembly variables — **stay JEXL** |
| JavaScript extensions / UDFs | Rhino + `PSJavaScriptExtensionHandler` | Legacy extension handler `handler="JavaScript"` — leave alone this track |
| WebUI / gadgets host | Browser JS | Client only |

All **48** product widget definitions use `Code type=jexl` and `Content type=velocity` (see [widget-xml-inventory.md](./widget-xml-inventory.md)).

## Decision summary (approved)

1. **Assemblers:** Velocity (power), **HTML-first** (simple default path), **Markdown** (new), Legacy/XSL (compat), Binary/Dispatch/Database (specialized).
2. **Bindings:** JEXL only — no language migration workstream.
3. **Packaging:** Content type + template(s) + slots (with layout/styles) + catalog metadata.
4. **Ship bar:** Product **out of Page / Widget / Gadget XML definition files**; customer upgrade converts; optional time-boxed runtime shim.
5. **Widget layout/styles** → first-class **slot** properties (`slot_layout`, `slot_styles`).

## Phases

| Phase | Issue | Goal | Status |
|-------|-------|------|--------|
| **0** | #2627 | Inventory, ADRs, contracts | PR #2625 |
| **1** | #2628 | HTML-first + Markdown assemblers | In progress |
| **2** | #2629 | Unified slots + `slot_layout` / `slot_styles` | Blocked by #2628 |
| **3** | #2630 | Widget/Page/Gadget XML → package model | Blocked by #2629 |
| **4** | #2631 | Design SPA consolidation | Blocked by #2630 |
| **5** | #2632 | Deprecation cleanup / help | Blocked by #2630 |

## Documents in this folder

| Doc | Purpose |
|-----|---------|
| [plan.md](./plan.md) | Full strategic plan (canonical) |
| [widget-xml-inventory.md](./widget-xml-inventory.md) | Product widget matrix (48 defs) |
| [widget-xml-inventory.csv](./widget-xml-inventory.csv) | Machine-readable inventory |
| [gadget-definition-inventory.md](./gadget-definition-inventory.md) | Gadget XML / SPA survey |
| [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md) | Phase 3 dual-run operator policy + runtime shim selection (#2752) |
| [adr/](./adr/) | Architecture decision records |
| [parity-notes.md](./parity-notes.md) | Region vs slot, pageAssembler vs velocity, etc. |
| [region-slot-mapping.md](./region-slot-mapping.md) | Phase 2 residual: region↔slot composition + CssPref upgrade (#2690) |
| [binding-modules.md](./binding-modules.md) | `$sys` / `$rx` / `$perc` + assembler picker guide |

## Code anchors

| Area | Path |
|------|------|
| Assembler SPI | `system/services/.../assembly/IPSAssembler.java` |
| Velocity assembler | `.../impl/plugin/PSVelocityAssembler.java` |
| HTML-first assembler | `.../impl/plugin/PSHtmlAssembler.java` (`htmlAssembler`) |
| Markdown assembler | `.../impl/plugin/PSMarkdownAssembler.java` (`markdownAssembler`) |
| Placeholder renderer | `.../impl/plugin/PSBindingPlaceholderRenderer.java` (`${path}`) |
| Legacy/XSL | `.../impl/plugin/PSLegacyAssembler.java` |
| JEXL | `modules/utils/.../jexl/PSScript.java` |
| CM1 page assembler | `projects/sitemanage/.../assembler/PSPageAssembler.java` |
| Widget model | `projects/sitemanage/.../data/PSWidgetDefinition.java` |
| Widget DAO (legacy XML load) | `projects/sitemanage/.../dao/impl/PSWidgetDao.java` (`rxconfig/Widgets`) |
| Widget packages | `modules/perc-packages/.../rxconfig/Widgets/` |
| Dual-run definition source shim | `modules/perc-packages/.../shim/PSLegacyDefinitionXmlShim.java` |
| Gadget registry | `WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml` |

## Immediate next work

1. Land Phase 1 (#2628): HTML-first + Markdown assemblers + tests.
2. Phase 2 (#2629): `slot_layout` / `slot_styles` schema.
3. Align Widget Builder / Design SPA with modern package format (Phase 3+).
