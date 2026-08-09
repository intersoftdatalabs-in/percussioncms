# Template & Assembler Model Normalization (8.2)

| Field | Value |
|-------|--------|
| **Status** | Active — Phase 0 inventory / ADRs |
| **Created** | 2026-08-09 |
| **Type** | Product architecture (multi-phase) |
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

| Phase | Goal | Status |
|-------|------|--------|
| **0** | Inventory, ADRs, contracts | In progress |
| **1** | HTML-first + Markdown assemblers; Velocity docs; golden fixtures | Not started |
| **2** | Unified slots + `slot_layout` / `slot_styles` | Not started |
| **3** | Widget/Page/Gadget XML → package model (product first) | Not started |
| **4** | Design SPA consolidation (depends on Design track) | Not started |
| **5** | Deprecation cleanup, help rewrite | Later |

## Documents in this folder

| Doc | Purpose |
|-----|---------|
| [plan.md](./plan.md) | Full strategic plan (canonical) |
| [widget-xml-inventory.md](./widget-xml-inventory.md) | Product widget matrix (48 defs) |
| [widget-xml-inventory.csv](./widget-xml-inventory.csv) | Machine-readable inventory |
| [gadget-definition-inventory.md](./gadget-definition-inventory.md) | Gadget XML / SPA survey |
| [adr/](./adr/) | Architecture decision records |
| [parity-notes.md](./parity-notes.md) | Region vs slot, pageAssembler vs velocity, etc. |

## Code anchors

| Area | Path |
|------|------|
| Assembler SPI | `system/services/.../assembly/IPSAssembler.java` |
| Velocity assembler | `.../impl/plugin/PSVelocityAssembler.java` |
| Legacy/XSL | `.../impl/plugin/PSLegacyAssembler.java` |
| JEXL | `modules/utils/.../jexl/PSScript.java` |
| CM1 page assembler | `projects/sitemanage/.../assembler/PSPageAssembler.java` |
| Widget model | `projects/sitemanage/.../data/PSWidgetDefinition.java` |
| Widget packages | `modules/perc-packages/.../rxconfig/Widgets/` |
| Gadget registry | `WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml` |

## Immediate next work

1. Complete ADRs (placeholder syntax, slot schema, packaging manifest).
2. Spike HTML-first assembler + one simple widget parity test (no Widget XML).
3. Spike `slot_layout` / `slot_styles` schema sketch.
4. Align Widget Builder / Design SPA so they author the modern package format.
