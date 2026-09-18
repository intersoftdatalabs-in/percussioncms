# Template & Assembler Model Normalization (8.2)

| Field | Value |
|-------|--------|
| **Status** | Phase 5 docs complete — Phases 0–4 closed; shim-removal residual [#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852) open (blocked until M1–M3 + G1–G6) |
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

Short note (Phase 5): [rhino-js-extension-note.md](./rhino-js-extension-note.md). **Not** an assembler — do not use Rhino for XSL migration targets ([xsl-migration-cookbook.md](./xsl-migration-cookbook.md)).

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
| **0** | #2627 | Inventory, ADRs, contracts | Done (PR #2625 merged) |
| **1** | #2628 | HTML-first + Markdown assemblers | Done (PR #2634 merged; CLOSED) |
| **2** | #2629 | Unified slots + `slot_layout` / `slot_styles` | Done (PRs #2692 / #2718 / #2719 merged; CLOSED) |
| **3** | #2630 | Widget/Page/Gadget XML → package model | Done (PR #3957 merged; M1 PASS — 0 product Package Widget XML; CLOSED) |
| **4** | #2631 | Design SPA consolidation | Done (PRs #2826 / #3315 / #3601 merged; CLOSED) |
| **5** | #2632 | Deprecation cleanup / help | Docs done (PRs #2845 / #2851 / #2853 / #4437 / #4442 / #4445 merged); code residual #2852 open, blocked until M1–M3 + G1–G6 |

## Documents in this folder

| Doc | Purpose |
|-----|---------|
| **[implementer-guide.md](./implementer-guide.md)** | **Single entry point** — one Template model, JEXL, assemblers, modern packages, dual-run/dual-ship status (Phase 5 #2833 / #2632) |
| [plan.md](./plan.md) | Full strategic plan (canonical) |
| [xsl-migration-cookbook.md](./xsl-migration-cookbook.md) | Phase 5 XSL / `legacyAssembler` support statement + migration cookbook (#2834) |
| [rhino-js-extension-note.md](./rhino-js-extension-note.md) | Phase 5 optional Rhino JS **extension** note — non-assembly (#2834) |
| [component-package-manifest.md](./component-package-manifest.md) | Phase 3 ship-format manifest schema v1.0 + Java model |
| [widget-xml-inventory.md](./widget-xml-inventory.md) | Product widget matrix (48 defs) |
| [widget-xml-inventory.csv](./widget-xml-inventory.csv) | Machine-readable inventory |
| [page-definition-inventory.md](./page-definition-inventory.md) | Product page `*.templateDef` inventory + compiler (#2770) |
| [gadget-definition-inventory.md](./gadget-definition-inventory.md) | Gadget XML / SPA survey |
| [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md) | Phase 3 dual-run operator policy + runtime shim selection (#2752) |
| [definition-xml-shim-removal-criteria.md](./definition-xml-shim-removal-criteria.md) | Phase 5 shim removal metrics, gates, time-box + inventory (#2835 / #2632) |
| [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md) | Dual-ship vs native page templateDef install |
| [adr/](./adr/) | Architecture decision records (001–004) |
| [parity-notes.md](./parity-notes.md) | Region vs slot, pageAssembler vs velocity, etc. |
| [region-slot-mapping.md](./region-slot-mapping.md) | Phase 2 residual: region↔slot composition + CssPref upgrade (#2690) |
| [binding-modules.md](./binding-modules.md) | `$sys` / `$rx` / `$perc` + assembler picker guide |
| Product operator help (Phase 5 #2632 slices #4433–#4435) | `product-docs/8.2/admin/design-templates.md` (assembler picker: HTML-first / Markdown / Velocity), `product-docs/8.2/admin/xsl-legacy-assembler.md` (XSL support statement + migration cookbook), `product-docs/8.2/admin/definition-xml-dual-run.md` (convert → deploy modern cookbook; shim kept) |

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
| Component package manifest | `modules/perc-packages/.../manifest/PSComponentPackageManifest*.java` |
| Widget XML compiler | `modules/perc-packages/.../widgetxml/PSWidgetXmlCompiler.java` (#2751 baseWidgets, #2772 high-traffic) |
| Page templateDef compiler | `modules/perc-packages/.../pagexml/PSPageXmlCompiler.java` (#2770) |
| Page dual-ship / native install | `…/pagexml/PSPageXmlDualShip.java`, `PSPageXmlNativeInstall.java`, `PSPageXmlInstallPolicy.java` (#2786 / #2806) |
| Dual-ship retirement checklist | [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md) |
| Gadget registry compiler | `modules/perc-packages/.../gadgetxml/PSGadgetRegistryCompiler.java` (#2771) |
| Modern gadget catalog (ship) | `modules/perc-packages/src/main/resources/catalogs/gadgets/gadget-catalog.json` |
| Dual-run definition source shim | `modules/perc-packages/.../shim/PSLegacyDefinitionXmlShim.java` |
| Gadget registry (legacy upgrade input) | `WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml` |

## Immediate next work

1. **Start here for implementers:** [implementer-guide.md](./implementer-guide.md) (Phase 5 docs #2833).
2. **Operators:** product help is live — [Design templates](../../../../product-docs/8.2/admin/design-templates.md) (assembler picker), [XSL and legacyAssembler support](../../../../product-docs/8.2/admin/xsl-legacy-assembler.md), [Convert definition XML (dual-run)](../../../../product-docs/8.2/admin/definition-xml-dual-run.md).
3. **Do not start** shim / dual-run deletion ([#2852](https://github.com/intersoftdatalabs-in/percussioncms/issues/2852)) until [definition-xml-shim-removal-criteria.md](./definition-xml-shim-removal-criteria.md) shows M1–M3 + G1–G6 all PASS (or waived with evidence).

Epic: [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) · Phase 3: [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) · Phase 5: [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632).
