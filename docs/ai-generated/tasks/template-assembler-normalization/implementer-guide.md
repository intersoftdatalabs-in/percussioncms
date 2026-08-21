# Assemblers and Templates — implementer guide

| Field | Value |
|-------|--------|
| **Audience** | Implementers, package authors, assembly extension developers |
| **Status** | Living — Phase 5 docs slice 1 (#2833) |
| **Epic** | [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) |
| **Phase 5 parent** | [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632) |
| **Related** | Phase 3 [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) |
| **Not for** | Customer operator help site (`product-docs/`) — engineering track docs |

This guide is the **single entry point** for the 8.2 Template & Assembler Model Normalization program. It consolidates the north star, ADRs 001–004, binding/assembler choices, modern package format, and dual-run / dual-ship status. Deep dives stay in the linked peer docs; do not treat those peers as alternate “sources of truth” for direction—use this guide first, then drill down.

**Explicit non-goal of the program:** migrating template bindings or UDFs off **JEXL** to JavaScript (or any other expression language).

---

## 1. North star (one Template model)

```text
Template (page | snippet | global | binary | resource)
  + ordered JEXL bindings
  + pluggable Assembler (Velocity | HTML-first | Markdown | Legacy/XSL | specialized)
  + Slots (unified holes; optional slot_layout / slot_styles)
```

Product packages ship **without** Page / Widget / Gadget **XML definition files** as the authoring format. Upgrade tools may still **read** those XML dialects; a time-boxed runtime **shim** may load them when modern packages are absent.

| Concept | Meaning |
|---------|---------|
| **Template** | First-class assembly unit: type (page/snippet/global/binary/resource), assembler extension name, source body, ordered bindings |
| **Bindings** | Ordered JEXL `variable` + `expression` rows evaluated before render ([ADR-001](./adr/001-jexl-bindings-stay.md)) |
| **Assembler** | Plugin implementing `IPSAssembler` — language/specialized render path ([ADR-002](./adr/002-assembler-set.md)) |
| **Slot** | Composition hole; layout chrome is first-class on the slot, not only on “widgets” ([ADR-003](./adr/003-slot-layout-styles.md)) |
| **Package** | Content types + templates + slots + catalog + resources via `component-package.json` ([ADR-004](./adr/004-no-definition-xml-packaging.md)) |

### What collapses into this model

| Legacy / CM1 surface | Normalized form |
|----------------------|-----------------|
| Widget `Code` (JEXL) | Template binding rows (still JEXL) |
| Widget `Content` (Velocity/HTML) | Snippet template source + assembler |
| Widget definition XML | Content type + snippet template(s) + catalog in modern package |
| CM1 region | Slot (unified **placement hole** — intentional “hole,” not “whole”; regions collapse into the slot composition model); see [region-slot-mapping.md](./region-slot-mapping.md) |
| Widget `CssPref` / layout-ish prefs | `slot_layout` / `slot_styles` (definition + instance overrides) |
| `pageAssembler` | Page context + text assembler — **not** a separate template language |
| Gadget definition / registry XML | Gadget catalog + per-gadget packages (`catalog.kind = "gadget"`) |

Parity detail: [parity-notes.md](./parity-notes.md). Strategic background: [plan.md](./plan.md). Task index: [README.md](./README.md).

---

## 2. Pipeline (how assembly runs)

```text
Template
  → assembler extension name (e.g. velocityAssembler, htmlAssembler)
  → IPSAssembler.assemble(items)
       ↳ preProcessItemBinding(item, PSAssemblyJexlEvaluator)
       ↳ evaluate ordered JEXL bindings
       ↳ render template source (language-specific)
```

Core types (module `system` / assembly services):

| Type | Role |
|------|------|
| `IPSAssembler` | Assembler SPI |
| `IPSAssemblyTemplate` | Template metadata (type, AA, publish-when, …) |
| `PSTemplateBinding` | Ordered variable + JEXL expression (no language column) |

**Code anchors** (full table on [README.md](./README.md)):

| Area | Path |
|------|------|
| Assembler SPI | `system/services/.../assembly/IPSAssembler.java` |
| Velocity | `.../impl/plugin/PSVelocityAssembler.java` |
| HTML-first | `.../impl/plugin/PSHtmlAssembler.java` (`htmlAssembler`) |
| Markdown | `.../impl/plugin/PSMarkdownAssembler.java` (`markdownAssembler`) |
| Placeholder renderer | `.../impl/plugin/PSBindingPlaceholderRenderer.java` (`${path}`) |
| Legacy/XSL | `.../impl/plugin/PSLegacyAssembler.java` |
| JEXL | `modules/utils/.../jexl/PSScript.java` |
| CM1 page assembler | `projects/sitemanage/.../assembler/PSPageAssembler.java` |

---

## 3. JEXL bindings (ADR-001)

**Decision:** template bindings and widget Code blocks **remain JEXL** for the entire 8.2 normalization program. No `LANGUAGE` column, no GraalJS for assembly, no dual-language bindings workstream.

### Why Rhino is still on the classpath

| Surface | Mechanism | Role in this program |
|---------|-----------|----------------------|
| **Template bindings** | Commons JEXL 3 (`PSScript`, `PSTemplateBinding`) | **Stay JEXL** |
| **JavaScript extensions / UDFs** | Rhino + `PSJavaScriptExtensionHandler` | Legacy `handler="JavaScript"` exits — **leave alone** (not assembly bindings) |
| **WebUI / gadgets host** | Browser JS | Client only |

Do **not** conflate Rhino extension UDFs with template bindings. Product inventory: all **48** product widget definitions use `Code type=jexl` ([widget-xml-inventory.md](./widget-xml-inventory.md)).

### Binding modules

See [binding-modules.md](./binding-modules.md) for the full tables. Summary:

| Module | Role |
|--------|------|
| **`$sys`** | Assembly system context (`template`, `mimetype`, `charset`, `site`, `assemblyItem`, `slot`, `currentslot`, …) |
| **`$rx`** | JEXL tool namespaces (`asmhelper`, `codec`, `link`, `location`, `nav`, `string`, …) via `@IPSJexlMethod` |
| **`$perc`** | CM1 page context (regions/widgets/theme helpers) from `PSPageAssembler` |

**Slot layout/styles (ADR-003)** bind as:

| Binding | Meaning |
|---------|---------|
| `$sys.slot.layout` | Structural layout map for current slot |
| `$sys.slot.styles` | Presentational styles map |
| `$sys.slot.schemaVersion` | Integer schema version |
| `$sys.currentslot.layout` / `.styles` | Mirrored on Velocity AA slot context |
| `$rx.asmhelper.slotAssemblyContext(slot)` | Builds `$sys.slot` from a definition |

Schema helper: `PSSlotLayoutStyles` (`SCHEMA_VERSION = 1`). Persistence: `RXSLOTTYPE.SLOT_LAYOUT` / `SLOT_STYLES` CLOB JSON. REST: `slotLayout` / `slotStyles` on `SlotDetail` (`GET/PUT /slots/{idOrName}`).

---

## 4. Assembler set (ADR-002)

Invest in these **text/render** assemblers; keep specialized ones.

| Assembler | Extension (typical) | When to use |
|-----------|---------------------|-------------|
| **HTML-first** | `Java/global/percussion/assembly/htmlAssembler` | Simple HTML + variables; no Velocity cliff |
| **Markdown** | `Java/global/percussion/assembly/markdownAssembler` | Content-oriented bodies → HTML |
| **Velocity** | `Java/global/percussion/assembly/velocityAssembler` | Macros, loops, `#parse`, AA macros, power packages |
| **page** | `pageAssembler` | CM1 page/template: page context + `$perc`, then text render (today Velocity-backed) |
| **Legacy/XSL** | `legacyAssembler` | Compatibility only — XML app + stylesheet; **skips** JEXL bindings |
| **Binary / Dispatch / Database / Resource** | specialized | Keep; not general text authoring |

`pageAssembler` is **not** a separate language — it is page context + a text assembler ([parity-notes.md](./parity-notes.md) §1).

### HTML-first / Markdown placeholder syntax (locked)

| Rule | Detail |
|------|--------|
| Form | `${title}`, `${sys.mimetype}` — **`${dotted.path}` only** |
| Not supported | Bare `$title`, Mustache `{{ }}`, Velocity directives |
| Lookup | Binding key `title` or `$title`; nested maps via `sys` / `$sys` |
| Missing | Empty string |
| Implementation | `PSBindingPlaceholderRenderer` |

Design SPA assembler picker and slot editor: see [binding-modules.md](./binding-modules.md) (Design SPA section / #2810).

### Choosing an assembler (quick)

1. Need macros / AA Velocity surface → **Velocity**.
2. Simple static HTML + a few bound values → **HTML-first** with `${path}`.
3. Long prose body that should become HTML → **Markdown**.
4. Supporting old XSL variants only → **legacyAssembler** (no new design investment; XSL cookbook is a separate Phase 5 slice).

---

## 5. Slots, layout, and styles (ADR-003)

Promote CM1 widget layout/style preferences to **first-class slot properties**:

- **`slot_layout`** — structural hints (orientation, columns, max items, wrapper policy, empty state, …).
- **`slot_styles`** — presentational tokens/classes (e.g. `rootclass`, `itemclass`).

Stored as versioned JSON maps on:

1. Slot **definition** (defaults)
2. Slot **instance** on page/template composition (sparse overrides; instance keys win)

Upgrade mapping sketch: [region-slot-mapping.md](./region-slot-mapping.md) (`PSRegionToSlotCompositionMapper`, `PSWidgetPrefToSlotMapper` / #2690).

**Do not** invent open-ended CSS-in-DB in v1 — start with CM1-parity property set and version the schema.

---

## 6. Modern package format (ADR-004 + manifest v1.0)

### Ship vs upgrade-input

| Concern | **Ship format (modern)** | **Upgrade input (legacy)** |
|---------|--------------------------|----------------------------|
| Manifest | `component-package.json` (schema `1.0`) | Widget/Page/Gadget definition XML |
| Templates | `templates[]` with `assembler`, `sourceRef`, JEXL `bindings[]` | Widget `Code`/`Content` or `*.templateDef` |
| Content types | `contentTypes[]` + package-relative CT artifacts | CT trees inside `.ppkg` |
| Slots | `slots[]` with `layout` / `styles` maps | Region tree + widget prefs |
| Catalog | `catalog` object (kind, title, category, …) | WidgetPrefs / gadget registry |
| Definition XML | **Not** product authoring format | Compilers + time-boxed shim only |

Canonical schema and field tables: [component-package-manifest.md](./component-package-manifest.md).  
Java model: `com.percussion.packages.manifest.PSComponentPackageManifest` (`modules/perc-packages`).

### Package layout (illustrative)

```text
<package-source>/
  component-package.json
  contentTypes/<typeName>/…
  templates/<templateName>.vm   # or .html / .md per assembler
  resources/…
  package-install.properties    # e.g. page.installMode=native for page layouts
```

Paths inside the manifest are **package-relative**, always with `/` separators (URL / zip entry style). Absolute OS paths and `..` segments are invalid.

### Compilers (upgrade input → modern)

| Domain | Compiler area | Inventory / notes |
|--------|---------------|-------------------|
| Widgets | `.../widgetxml/PSWidgetXml*.java` | [widget-xml-inventory.md](./widget-xml-inventory.md) |
| Pages | `.../pagexml/PSPageXml*.java` | [page-definition-inventory.md](./page-definition-inventory.md) |
| Gadgets | `.../gadgetxml/*`, catalog JSON | [gadget-definition-inventory.md](./gadget-definition-inventory.md) |

**Rules for product engineering (ADR-004):**

1. Do **not** add new product features that **require** definition XML.
2. Widget Builder / Design tools write **modern** format only.
3. Compilers read XML as **upgrade input** only — not as ship format.
4. Prefer metrics / logging of legacy selection so Phase 5 has exit data for shim removal.

---

## 7. Dual-run shim status

**Policy doc:** [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md)  
**Code:** `com.percussion.packages.shim.PSLegacyDefinitionXmlShim` (`modules/perc-packages`)

### Selection order (hard)

1. **Modern preferred** — if `component-package.json` is present, use modern. Do **not** prefer co-located legacy XML.
2. **Legacy XML fallback** — if modern is absent and Widget / Page / Gadget definition XML is present, load as today.
3. **Neither** — fail clearly (definition id + expected locations). No silent default.

```text
modern component-package.json?
  yes → MODERN_COMPONENT_PACKAGE
  no  → legacy Widget/Page/Gadget XML?
          yes → LEGACY_*_XML
          no  → PSDefinitionSourceNotFoundException
```

### Time box / Phase 5 exit (shim)

| Milestone | Expectation |
|-----------|-------------|
| **Now (dual-run)** | Shim available; modern preferred; customer XML still loads when alone |
| **Product converted** | Product packages not authored as definition XML |
| **Customer conversion** | Operators convert remaining XML; measure residual loads |
| **Phase 5 (#2632)** | Remove or hard-disable shim when metrics allow; update help |

**This slice (#2833)** documents the model and points operators/implementers at the policy. **Shim code deletion** is out of scope here (separate residual under #2632 when exit criteria are met).

---

## 8. Dual-ship page templateDef status

**Checklist:** [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md)

Dual-ship is a **package-build** bridge (materialize root `*.templateDef` for deployer), **not** the same as dual-run runtime shim selection.

| Mode | Behavior |
|------|----------|
| **dual-ship** (default) | Materialize root `*.templateDef` → reorganize into archive `TemplateDef-N/` |
| **native** | Skip root dual-ship; stage `TemplateDef-N/` from modern `pages/` (`page.installMode=native`) |

**Already native:** `perc.baseTemplates`, `perc.responsiveTemplates` (#2806), `perc.Baseline` (#3673). Remaining dual-ship is widget leftover binary `*.templateDef` (#3674), not page-layout packages.

| Concept | Layer | Status |
|---------|-------|--------|
| Dual-run **definition XML shim** | Runtime modern vs legacy XML | Time-boxed; Phase 5 #2632 |
| Dual-ship **page templateDef** | Package-build install bridge | Optional; native preferred for converted packages |
| Native **page install** | Build stages TemplateDef from modern pages | Landed for base/responsive/Baseline |

---

## 9. Implementer checklist

### Authoring a new component package

1. Create `component-package.json` with `schemaVersion: "1.0"`, stable `id`, `version`, and required `contentTypes` / `templates` (gadgets: `catalog.kind = "gadget"` rules apply — see manifest doc).
2. Point each template at an **assembler** and package-relative `sourceRef`.
3. Put JEXL bindings on the template (`bindings[]`), not in a parallel Code XML dialect.
4. Declare slots with optional `layout` / `styles` maps when composition needs chrome.
5. Prefer **HTML-first** or **Markdown** for simple snippets; use Velocity when macros/AA surface is required.
6. Do **not** add product `rxconfig/Widgets|Pages|Gadgets` definition XML as the source of truth.

### Migrating an existing widget / page definition

1. Inventory (product matrices already exist — widgets / pages / gadgets docs above).
2. Run the appropriate compiler (or follow golden fixtures under `modules/perc-packages/src/test/resources/`).
3. Deploy modern package; leave legacy XML only until selection verifies modern wins.
4. Smoke assembly (Velocity path, HTML-first placeholders, page `$perc` if applicable).
5. Remove legacy definition XML when parity is accepted.
6. For page layout packages: opt into **native** install when ready ([dual-ship checklist](./dual-ship-page-template-retirement.md)).

### Extending assembly (new assembler)

1. Implement `IPSAssembler` (or extend an existing plugin carefully).
2. Register under `Java/global/percussion/assembly/*` (see `modules/extensions-main` Extensions + Baseline packages).
3. Run JEXL bindings first unless the assembler is intentionally binding-free (legacy XSL).
4. For simple substitution languages, reuse `PSBindingPlaceholderRenderer` / `${path}` rules — do not invent a second placeholder dialect without an ADR.
5. Add unit tests and update this guide + [binding-modules.md](./binding-modules.md) assembler picker table if the Design catalog gains an entry.

### Operator dual-run (customer installs)

Follow the checklist in [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md) § Operator dual-run checklist: inventory → convert → deploy modern → verify selection → remove XML → exit dual-run.

---

## 10. Architecture decisions (index)

| ADR | Title | Status |
|-----|-------|--------|
| [001](./adr/001-jexl-bindings-stay.md) | Keep JEXL for template bindings | Accepted |
| [002](./adr/002-assembler-set.md) | Assembler set; `${path}` locked | Accepted; placeholder syntax locked |
| [003](./adr/003-slot-layout-styles.md) | Slot layout and slot styles | Accepted (direction) |
| [004](./adr/004-no-definition-xml-packaging.md) | No Page/Widget/Gadget XML for product packaging | Accepted |

ADR folder index: [adr/README.md](./adr/README.md).

---

## 11. Related docs (do not duplicate status here)

| Doc | Purpose |
|-----|---------|
| [README.md](./README.md) | Task folder index, code anchors, phase table |
| [plan.md](./plan.md) | Full strategic plan |
| [component-package-manifest.md](./component-package-manifest.md) | Manifest schema v1.0 |
| [binding-modules.md](./binding-modules.md) | `$sys` / `$rx` / `$perc` + assembler picker |
| [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md) | Dual-run operator policy + shim API |
| [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md) | Dual-ship vs native page install |
| [parity-notes.md](./parity-notes.md) | Region/slot, page vs velocity, widget Code/Content |
| [region-slot-mapping.md](./region-slot-mapping.md) | Region↔slot composition + CssPref upgrade |
| Inventories | [widget](./widget-xml-inventory.md) · [page](./page-definition-inventory.md) · [gadget](./gadget-definition-inventory.md) |

### Sibling Phase 5 slices (not this doc)

| Slice | Issue | Scope |
|-------|-------|-------|
| **1 (this)** | #2833 | Implementer guide (you are here) |
| **2** | #2834 | XSL migration cookbook (support statement) |
| **3** | #2835 | Design SPA deprecation / help surfaces |

Do **not** merge those into this PR; keep each PR-sized.

### Product docs note

This file lives under `docs/ai-generated/` (engineering / agent task tree). Customer/operator **product help** under `product-docs/` is a separate Phase 5 deliverable when product-facing help is rewritten. Pure implementer consolidation does not replace `product-docs/` pages for operators.

---

## 12. References

- GitHub epic [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626)
- Phase 5 parent [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632)
- This slice [#2833](https://github.com/intersoftdatalabs-in/percussioncms/issues/2833)
- Phase 3 packaging [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630)
- Design templates SPA track (related, separate): `docs/ai-generated/tasks/design-templates-item-types/`
