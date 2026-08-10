# Plan: Template & Assembler Model Normalization (8.2)

**Status:** Strategic analysis / product architecture plan (not an implementation PR plan)  
**Scope:** Multi-generation template models → unified assembler plugin architecture  
**Primary modules:** `system/services` (assembly), `projects/sitemanage` (CM1 page/widgets), `modules/perc-packages` (widget packages), `modules/utils` (JEXL), Workbench/WebUI Design surfaces  
**Related existing track:** `docs/ai-generated/tasks/design-templates-item-types/` (SPA Design; explicitly defers full AA/page-editor rewrite)

---

## 1. Executive recommendation

Your gut is **directionally correct**, with important refinements:

| Instinct | Verdict | Refinement |
|----------|---------|------------|
| Migrate CM1 Page templates to Velocity templates on upgrade | **Mostly true already; normalize storage/UI, not invent Velocity** | CM1 pages already assemble via `pageAssembler` → `PSVelocityAssembler`. What is “meta” is the **region tree + widget instances + `#region` macros**, not a separate non-Velocity runtime. |
| Repackage widgets as content types + snippet template pairs | **Right long-term model** | Many widgets already *are* a content type (asset) + Velocity fragment + JEXL code block. Normalization means making that the **first-class product model**, not a side-car XML definition under `rxconfig/Widgets`. |
| End state: XSL / Velocity / Markdown / HTML / … assemblers | **Strong fit for existing architecture** | `IPSAssembler` + extension registration already is the plugin seam. Extend it; do not replace with a greenfield pipeline. |
| Ship without Page / Widget / Gadget **XML definition files** | **Primary packaging goal** | Upgrade converts product + customer definition XML into content types, templates, slots, and catalog metadata. Runtime may keep a short-lived read shim; **shipping product packages should not still be authored as those XML dialects**. |
| JEXL for bindings | **Keep JEXL** | No binding-language migration in this plan. Expression language stays JEXL. |
| Layout/style prefs only on widgets | **Promote to slots** | Widget layout properties and styles become **slot-level** `slot_layout` / `slot_styles` (usable for all slot content, not only CM1 widgets). |

**Recommended north star for 8.2+:**

> One **Template** object model (page / snippet / global / binary / resource) + ordered **JEXL bindings** + pluggable **Assemblers** (legacy/XSL, Velocity, Markdown, HTML-first, future). Layout (regions/slots) is a **unified hole model** with optional layout/style metadata. **Page / Widget / Gadget XML definition files are eliminated** as the authoring and packaging format.

**Explicitly out of this plan:** migrating bindings or UDFs off JEXL to JavaScript (or any other expression language).

---

## 2. Current-state inventory (what actually exists)

### 2.1 Assembler plugin layer (Rhythmyx / CM System core)

Registered under `Java/global/percussion/assembly/*` (see `modules/extensions-main/.../Extensions.xml` + Baseline packages):

| Assembler | Role |
|-----------|------|
| `velocityAssembler` | Classic Velocity body + JEXL bindings (`PSVelocityAssembler`) |
| `legacyAssembler` | Proxy to XML app + stylesheet (XSL variants); **skips binding processing** |
| `binaryAssembler` | Pass-through binary fields |
| `dispatchAssembler` | Choose another template from bindings |
| `databaseAssembler` | DB publishing XML |
| `debugAssembler` | Diagnostics |
| `pageAssembler` | CM1 page/template assembly (`PSPageAssembler` **extends** `PSVelocityAssembler`) |
| `pageVariantAssembler` | Page content with an explicitly chosen template |
| `resourceAssembler` | CM1 resource publishing path |
| `pageDatabaseAssembler` | CM1 DB export path |

**Seam that already matches the target:**

```text
Template → assembler extension name → IPSAssembler.assemble(items)
                ↳ preProcessItemBinding(item, PSAssemblyJexlEvaluator)
                ↳ evaluate ordered JEXL bindings
                ↳ render template source (language-specific)
```

Core types: `IPSAssembler`, `IPSAssemblyTemplate` (page/snippet/global/binary/database; shared/local; AA type; publish-when), `PSTemplateBinding` (ordered variable + JEXL expression).

### 2.2 Classic Velocity + slots + Active Assembly

- **Template source:** Velocity text in assembly template rows.
- **Bindings:** ordered **JEXL** expressions → `$var` in Velocity context; system tools under `$sys` / `$rx.*` (asmhelper, codec, link, location, nav, string, …).
- **Composition:** **slots** + content finders (auto, relationship, nav, …).
- **Editor UX:** Active Assembly on **preview** (add/remove/reorder related items) — not true layout design.
- **Legacy XSL variants:** still reachable via `legacyAssembler` + assembly URL into XML applications; “technically deprecated” in product narrative but still in the installer/data path.

### 2.3 CM1 Page / Widget model (“meta” templates)

Layer lives primarily in `projects/sitemanage/.../pagemanagement`:

| Concept | Storage / shape | Runtime |
|---------|-----------------|---------|
| **Page template** | `PSTemplate`: region tree, body markup, theme/CSS/head fragments; backed by assembly template with `pageAssembler` | Region assembly → inject `$perc` → Velocity |
| **Page** | Page item + branch overrides of regions/widgets | Same assembler |
| **Widget definition** | XML under `rxconfig/Widgets` (packaged in `modules/perc-packages`) | `Code` (default **jexl**) + `Content` (default **velocity** / html) |
| **Widget instance** | On region: definition id, id, user/css prefs | Relationships to assets via widget-asset relationship services |
| **Asset content types** | Normal CMS content types (e.g. `percRichTextAsset`) | Often 1:1 with creatable widgets |

Example (`percRichText.xml`): JEXL builds CSS class attribute; Velocity loads related asset and emits field — **exactly** “snippet + bindings + content type,” packaged as a Widget.

Macro surface unique to CM1: `#region(...)`, `#loadRelatedWidgetContents()`, edit-mode sample content helpers, `$perc.widget` / `$perc.widgetContents`.

### 2.4 Dual editor UIs

1. **Classic AA / Workbench template editor** — source, bindings, slots, general meta (`docs/developer-module/workbench-functional-inventory.md` §7).
2. **CM1 visual Page Editor + Design** — region drag/drop, widget palette, semi-WYSIWYG — historically weak fidelity; SPA Design track (`design-templates-item-types`) is the modernization front door but **defers** full layout rewrite.

### 2.5 Binding / UDF / JavaScript reality check

Two different “JavaScript” surfaces get confused historically:

| Surface | Engine / mechanism | Used for | Status in this plan |
|---------|-------------------|----------|---------------------|
| **Template bindings** | Apache Commons **JEXL 3** via `PSScript` / `PSJexlEvaluator` | Ordered variables on assembly templates; widget `Code` blocks | **Keep JEXL. No migration.** |
| **JavaScript extensions / UDFs** | **Rhino** (`org.mozilla:rhino` on the classpath; `PSJavaScriptExtensionHandler`) | Legacy extension handler type `handler="JavaScript"` — exits/UDFs registered like Java extensions (Workbench “JavaScript” category) | **Leave as-is** for this plan; not assembly bindings |
| **WebUI / client JS** | Browser | Editors, gadgets host, SPA | Unrelated to server assembly bindings |

Evidence:

- `PSTemplateBinding` stores `VARIABLE` + `EXPRESSION` only — **no language column**; expressions are JEXL.
- Widget `Code@type` schema enumerates **`jexl` only**.
- Root/`system` POMs still declare **Rhino**; extension handler `PSJavaScriptExtensionHandler` and historic app UDFs (`PSJavaScriptUdfExitHandler`) explain why Rhino is in the stack.
- `PSTypeEnum` even has a comment referring to “Rhino” as the product lineage name in places — not evidence that template bindings are JS.

**Decision for this plan:** stick to **JEXL** for template/widget bindings and `$rx` tool methods (`@IPSJexlMethod`). Do **not** introduce GraalJS, dual-language bindings, or a JEXL deprecation path here.

### 2.6 Velocity versions

Root POM: **Velocity Engine 2.4.1**, Velocity Tools 3.1. Help-site still has Velocity 2.0 upgrade guidance for implementers — syntax “weirdness” is a mix of:

- Velocity macro/`#` directive style vs HTML familiarity
- Historical `$sys` / `$rx` / `$perc` dual namespaces
- CM1 macros that hide slots/relationships
- JEXL `$var = expr` style next to Velocity `$var` reference rules
- Optional strict/silent/debug/lexical JEXL toggles in `PSScript`

**Product note:** many implementers struggle with Velocity. This plan therefore **adds** an **HTML-first assembler** (and Markdown) so simple templates need little or no Velocity.

---

## 3. Analysis of the proposed migration

### 3.1 “Migrate Page templates to Velocity templates”

**What this should mean in practice:**

| Do | Don’t |
|----|-------|
| Treat **assembler source as the canonical render body** (Velocity, HTML-first, Markdown, …) | Pretend CM1 pages are not already Velocity-backed today |
| Compile/expand region trees into explicit source (or keep region IR that *compiles* for debug/export) | Delete region metadata without a layout story |
| Point page templates at **`velocityAssembler`** / **htmlAssembler** (or a thin `pageAssembler` that is “assembler + page context bindings”) | Fork a third page-only template table forever |
| Preserve theme/head/CSS fields as **template properties or include snippets** | Lose SEO/head/protected-region fields in a naïve dump |
| **Stop shipping Page definition XML** as the package authoring format | Leave a permanent dual storage model |

**Hard problem:** CM1 templates encode **layout + widget placement**. Classic templates encode **slots** (typed holes filled by relationships/finders). These are related but not identical:

- Slot ≈ named region with allowed content types/templates and a finder.
- Widget instance ≈ specific component with definition, prefs, and optional asset relationship.

A good upgrade maps:

```text
Region  →  Slot (unified hole model)
Widget definition  →  Content type (+ field defs) + Snippet template(s) + optional resource templates + catalog metadata
Widget instance on page  →  Relationship / related content in slot OR structured page composition data
Widget Code (JEXL)  →  Template bindings (still JEXL)
Widget Content (Velocity/HTML)  →  Snippet template source
Widget css/layout prefs  →  Slot layout/style properties (see §3.3)
```

### 3.2 “Repackage widgets as content types + snippet templates”

**Strengths**

- Aligns CM1 with Rhythmyx mental model customers already understand for non-page content.
- Enables one Design surface for “templates” instead of Widget Builder vs Workbench Template Editor vs Page Design.
- Publishing, security ACLs, workflow, package export already understand content types + assembly templates.
- Matches how product packages already ship assets (`percRichTextAsset` + `percRichText.xml`).

**Ship criterion (user goal):** when 8.2 product packages ship, they are **not** authored/maintained as Page / Widget / Gadget **XML definition files**. Those files may exist only as:

1. **Input** to an upgrade/compile tool, or  
2. A **temporary runtime shim** for not-yet-converted customer data — not as the ongoing product source of truth.

**Risks / incompleteness if done naïvely**

| Widget concern | Where it lives today | Must land after migration |
|----------------|----------------------|---------------------------|
| Palette metadata (icon, category, thumbnail) | Widget XML prefs | Content type / template catalog metadata or extension registry |
| DnD / drop criteria / allowed asset types | Widget XML + asset services | Slot allowed types + UI policy |
| CSS / user preferences / layout | Widget instance properties | **Slot** `slot_layout` / `slot_styles` (+ optional instance overrides) — see §3.3 |
| Edit-mode sample content | Velocity macros + `$perc.isEditMode()` | Snippet bindings + page-editor contract |
| Multi-asset / auto-list widgets | Complex JEXL + finders | Snippet + slot finders (already classic) or query bindings |
| Responsive / preferred editor size | Widget prefs | UI chrome metadata, not assembly |
| Shared vs local asset | Widget-asset relationship service | Relationship types / AA-style related content |

**Conclusion:** Migration is not “delete Widgets folder on day one of coding.” It is **reify Widget as a packaging profile**, then **stop using the XML dialect**:

> **Component package = Content Type + one or more Templates (snippet/page/resource) + slot definitions (with layout/styles) + catalog metadata + optional CSS/JS resources.**

Widget Builder (React track `specs/989-react-cui-widget-builder`) should eventually **author that package**, not a private XML dialect.

**Gadgets:** treat Gadget definition XML the same way where it is still a separate XML dialect — catalog + config + templates/resources, not a permanent side-car file format. Dashboard gadget *runtime* may remain SPA components; the goal is out of **XML definition files** for product packaging, not “delete all gadgets.”

### 3.3 Slot layout & slot styles (promote from widgets)

Widget layout properties and styles were a useful CM1 idea stuck in the wrong layer. They should become **first-class slot features**:

| Property | Intent |
|----------|--------|
| **`slot_layout`** | Structural/layout hints for the hole: e.g. orientation, columns, max items, wrapper class policy, responsive breakpoints, empty-state policy |
| **`slot_styles`** | Presentational style map/tokens applied to the slot wrapper or items (CSS class roots, inline-safe tokens, theme hooks) |

**Why on slots, not only on “widgets”:**

- Classic AA/slots and CM1 regions become one composition model.
- Any content type/template placed in a slot can inherit the same layout/style chrome.
- Page Design no longer needs a parallel “widget cssPref / userPref” silo for layout.
- Snippet templates can read `$sys.slot.layout` / `$sys.slot.styles` (exact names TBD) via existing **JEXL** binding conventions.

**Model sketch:**

```text
Slot definition
  id, label, allowed content types/templates, finder, cardinality
  slot_layout  : structured map (JSON) — defaults from catalog, overridable per template
  slot_styles  : structured map (JSON) — same
Slot instance (on page/template composition)
  optional overrides of layout/styles
  ordered related items
```

Upgrade: map CM1 widget `CssPref` / layout-ish prefs → slot defaults on the slots created for those components; instance values → slot instance overrides.

### 3.4 Target assembler set

| Assembler | Status | 8.2 posture |
|-----------|--------|-------------|
| **XSL / Legacy** | Exists (`legacyAssembler` + XML apps) | **Support** for existing installs; mark unsupported for *new* design; no feature investment |
| **Velocity** | Primary modern path | **Keep** for power users and existing packages; improve docs/macros |
| **HTML-first** | Does not exist | **Add** — primary answer to “Velocity is hard”: HTML (or HTML + limited placeholders) with JEXL bindings supplying variables; optional small include mechanism |
| **Markdown** | Does not exist | **Add** — Markdown → HTML (CommonMark), bindings apply first |
| **Binary / Dispatch / Database / Resource** | Exist | Keep as specialized assemblers (not “languages”) |
| **Future (FreeMarker, Handlebars, …)** | N/A | Only if customer demand; **assembler plugin contract** is the investment |
| **pageAssembler** | CM1 specialization of Velocity | Fold into **chosen text assembler + standard page context bindings** (`$perc` becomes documented context module) |

**Design principle:**  
**Language assembler** (how body is interpreted) vs **output/purpose assembler** (binary, dispatch, database) stay distinct.

#### HTML-first assembler (new — in scope)

**Problem:** Velocity’s `#` directives, silent refs, and macro soup are a steep cliff for simple content snippets and page shells.

**Direction:**

1. **HTML-first body** is mostly static HTML.
2. Dynamic values come from **JEXL bindings** into a simple substitution context (e.g. `${var}` or a deliberately small placeholder syntax — **decide in ADR**; prefer one clear style, document it).
3. Optional: allow **includes** of other snippets by name (implemented in Java, not full Velocity).
4. When power users need loops/macros, they choose **Velocity** or **Markdown** assemblers explicitly.
5. CM1 simple widgets (Rich Text, Simple Text, Raw HTML) should migrate to HTML-first or Markdown snippets where possible.

This is **not** “React SSR” and **not** a client-side template engine.

### 3.5 Bindings: stay on JEXL

- Template bindings remain **JEXL** (`commons-jexl3`, `PSScript`, `PSTemplateBinding`).
- Widget `Code` remains JEXL (until widgets are templates; then the same binding rows).
- Java UDF tools remain `@IPSJexlMethod` / `$rx.*`.
- Rhino **JavaScript extensions** remain a separate legacy extension handler; no work in this plan to replace or expand them for assembly bindings.
- **No** language column, GraalJS, dual evaluators, or JEXL deprecation workstream.

---

## 4. Target architecture (normalized model)

```text
┌─────────────────────────────────────────────────────────────────┐
│ Template (unified)                                              │
│  id, name, label, type (local/shared), output (page/snippet/…)  │
│  assemblerId, mime, charset, publishWhen, aaType, sites, …      │
│  sourceText  OR  sourceRef (file)                               │
│  bindings[] { order, name, expression }   ← JEXL only           │
│  composition: slots[] (unified hole model; was region/slot)     │
│    each slot: finder, allowed types, slot_layout, slot_styles   │
│  optional layout IR (for visual editor; compiles to source)     │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ Assembly runtime                                                │
│  1. Load item + template                                        │
│  2. Build BindingContext (sys, rx tools, perc page, item, …)    │
│  3. Run JEXL bindings in order                                  │
│  4. IPSAssembler.render(body, context) → bytes + mime           │
│  5. Optional global template wrap                               │
└─────────────────────────────────────────────────────────────────┘
```

**Composition model (unify slots and regions):**

- One **Slot** abstraction (regions are slots): id, allowed content types, allowed templates, finder, cardinality, **`slot_layout`**, **`slot_styles`**.
- **Instances** on a page: ordered related content with optional layout/style overrides.
- Visual editor edits **composition graph**; **Source** view shows expanded assembler source for power users — two views of one truth, with clear ownership rules (visual may own layout blocks; freeform source may opt out of visual).

**Packaging model (no Page/Widget/Gadget XML):**

```text
Product/customer package
  ├── Content types (schemas, editors)
  ├── Templates (source + JEXL bindings + assembler id)
  ├── Slots (incl. layout/styles defaults)
  ├── Catalog metadata (palette icons, categories, editor chrome)
  └── Resources (CSS/JS/images)
```

Upgrade tools convert historical:

- `rxconfig/Widgets/*.xml`
- Page template meta/XML blobs (region trees as side-car shapes)
- Gadget definition XML (where applicable)

into the package model above.

**Deprecation ladder (honest product messaging):**

1. **Supported & recommended:** Velocity / HTML-first / Markdown templates, JEXL bindings, unified Design SPA, slot layout/styles.
2. **Supported legacy:** classic Workbench AA, existing XSL/legacyAssembler content, pre-upgrade Widget/Page XML still loadable via shim until converted.
3. **Not for new product packaging:** Page / Widget / Gadget XML definition files.
4. **No new investment:** dual page-editor UIs once SPA Design owns accepted flows.

---

## 5. Phased roadmap (8.2–oriented)

This is multi-quarter work even if labeled “8.2.” Split so 8.2 can ship **foundations + product package migration off XML definitions**, not a forced customer cliff on day one.

### Phase 0 — Truth inventory & contracts (2–4 weeks)

**Deliverables**

1. **Catalog matrix** of all product widgets (packages under `modules/perc-packages`) → content type, Code/Content types, finders used, page-editor dependencies, css/layout prefs.
2. **Gadget definition inventory** — which gadgets are XML-defined vs pure SPA; conversion targets.
3. **Customer-impact survey** (internal): who still uses XSL variants, pure AA velocity, CM1 only, mixed.
4. **Parity document:** region vs slot; pageAssembler vs velocityAssembler; widget Code vs template binding; widget cssPref → slot_styles.
5. **Public REST/OpenAPI** gaps for unified template + slot CRUD (partially exists in `rest` templates resources — extend, don’t fork).
6. Help-site map for implementer docs to rewrite.
7. **ADRs:** unified template entity; slot_layout/slot_styles schema; HTML-first placeholder syntax; “no Page/Widget/Gadget XML for new product packages.”

**Exit:** written ADRs approved; inventory complete.

### Phase 1 — Assembler platform (core, low UI risk)

**Goals:** make the *target* assemblers real without forcing full XML elimination yet.

1. **HTML-first assembler** MVP + tests + docs (placeholder syntax locked by ADR).
2. **Markdown assembler** MVP (CommonMark → HTML; JEXL bindings apply first).
3. Stabilize **Velocity** as power-user path; document `$sys` / `$rx` / `$perc` modules.
4. Ensure `pageAssembler` is clearly “context + text assembler” (refactor toward thin wrapper).
5. Golden assembly fixtures for product widgets (current behavior baseline).

**Exit:** new templates can choose Velocity, HTML-first, or Markdown; bindings remain JEXL.

### Phase 2 — Unified slots + layout/styles

**Goals:** one composition model.

1. Extend slot model with **`slot_layout`** and **`slot_styles`** (schema, persistence, REST, Workbench/SPA fields as available).
2. Map CM1 region tree ↔ slot composition (runtime and/or upgrade).
3. Expose layout/styles to assembly context for templates (JEXL-readable).
4. Tests: defaults, instance overrides, publish output classes/structure.

**Exit:** slots carry layout/style; new design does not need widget-only cssPref for layout.

### Phase 3 — Widget / Page / Gadget XML → package model (product first)

**Goals:** **ship product out of XML definition files.**

1. Define **Component Package Manifest** (reuse package system where possible) listing content types, templates, slots, catalog metadata, resources.  
   **Done (slice 1 / #2750):** schema docs + Java model + parse/validate/round-trip tests — see [component-package-manifest.md](./component-package-manifest.md) and `com.percussion.packages.manifest` in `modules/perc-packages`.
2. **Compiler/upgrade tool:** Widget XML → manifest + templates + slots (incl. layout/styles). *(#2751)*
3. **Page meta upgrade:** region/widget instances → composition + templates; assembler source canonical.  
   **Done (slice 4 / #2770 — product page templateDefs):** inventory + compiler `PSPageXml*` for `*.templateDef` → Component Package Manifest + golden `perc.base.plain` (see [page-definition-inventory.md](./page-definition-inventory.md)). Dual-run: product may still ship templateDefs until residual removal. **Still residual:** site-storage page item composition IR, Baseline system templates matrix, thumbnail resources, delete package templateDefs after install path.
4. **Gadget XML** conversion path for remaining XML-defined gadgets.
5. Convert **baseline / baseWidgets** first; then high-traffic packages (nav, blog, lists, rich text).
6. Runtime **compatibility shim:** if old XML still present and no modern package, load as today; product **source tree** no longer maintains those XML files as the authoring format. *(#2752)*
7. Widget Builder / Design tools write modern package format only.

**Exit criterion for “ship”:** product packages in repo/install are **not** defined by Page/Widget/Gadget XML files; upgrade converts customer XML; shim optional and time-boxed.

### Phase 4 — Design UX consolidation (depends on SPA Design track)

Align with `design-templates-item-types` phases D0–D4:

1. Template library SPA (all template kinds, not only page).
2. Source + JEXL Bindings editor; assembler picker (Velocity / HTML-first / Markdown / …).
3. Visual layout editor (slots/regions) that **round-trips** to composition IR + slot_layout/styles.
4. Retire classic Design/Page Editor for accepted flows only when parity is signed off.
5. Active Assembly remains for **related content** editing on preview where composition is relationship-based — or fold into SPA page editor.

**Do not** block platform phases 1–3 on full visual parity.

### Phase 5 — Deprecation & cleanup (post-8.2 acceptable if product XML is already gone)

1. Docs: single “Assemblers & Templates” guide; archive dual-model tutorials; Velocity as advanced track; HTML-first/Markdown for simple cases.
2. Remove product shims when metrics show zero XML definition loads.
3. XSL: support statement only; migration cookbook to Velocity/HTML/Markdown.
4. Optional later (not this plan): revisit Rhino JS extension handler health independently of assembly.

---

## 6. Velocity ergonomics + HTML-first (parallel)

| Issue | Direction |
|-------|-----------|
| Velocity hard for simple templates | **HTML-first assembler** (and Markdown) as default recommendation for simple snippets |
| Dual `$sys` / `$rx` / `$perc` | Document as modules; stabilize; avoid adding a fourth |
| Macro-heavy CM1 templates | Prefer explicit includes + short standard library; keep macros as sugar on Velocity path |
| Stringly `#region('id' '' '' '' '')` | Replace with structured IR + generated calls / slot markers |
| Global template `#inner()` pattern | Keep; document as layout shell pattern |
| Velocity 2.x migration leftovers | Re-audit product packages against 2.4.1; help-site update |

---

## 7. Risks & non-goals

### Risks

| Risk | Mitigation |
|------|------------|
| Customer template breakage on upgrade | Dual-run shims; golden HTML diffs; keep JEXL and Velocity behavior stable |
| Visual ↔ source round-trip lossy | Own layout in IR; source generation one-way for freeform templates |
| Scope explosion (full page editor rewrite) | Split: platform vs SPA Design; 8.2 ships platform + product XML elimination |
| Mixed CM1/classic sites | Explicit support matrix; no “must convert XSL in 8.2” |
| Incomplete gadget inventory | Phase 0 gadget survey; convert XML-defined only |
| Slot layout/styles scope creep | Start with CM1-parity property set; version schema |

### Non-goals for this plan

- **Any migration of bindings or expression language off JEXL** (no GraalJS, no dual binding languages, no JEXL deprecation).
- Replacing or expanding Rhino JavaScript **extensions** as assembly bindings.
- Removing XSL/`legacyAssembler` in 8.2.
- Forcing all customers off CM1 Page Editor UI in one release (XML packaging can die while editor shim remains briefly).
- Replacing publish assembly with React SSR or client-only rendering.
- Inventing a composition model unrelated to slots/regions.

---

## 8. Suggested decision record (for human approval)

1. **Canonical render assemblers:** Velocity (power), **HTML-first** (simple default path), **Markdown** (new), Legacy/XSL (compat), Binary/Dispatch/Database (specialized).
2. **Canonical binding language:** **JEXL only** for this plan; no language migration workstream.
3. **Canonical component packaging:** Content type + template(s) + slots (with **slot_layout** / **slot_styles**) + catalog metadata.
4. **Page templates:** Composition IR + assembler source; `pageAssembler` becomes context provider, not a separate template species.
5. **Ship bar:** Product **out of Page / Widget / Gadget XML definition files**; customer upgrade converts them; optional time-boxed runtime shim.
6. **Editors:** One Design SPA long-term; Workbench/AA retained until SPA acceptance; no third new editor.
7. **8.2 success bar:** HTML-first + Markdown assemblers; unified slot layout/styles; product packages migrated off definition XML; Demo/site golden assembly parity — **not** forced XSL removal or JEXL replacement.

---

## 9. Immediate next steps (when leaving plan mode)

1. Open a durable design doc under `docs/ai-generated/tasks/template-assembler-normalization/` (or promote this plan) with ADRs.
2. Inventory script: list all `rxconfig/Widgets/*.xml` → extract Code type, Content type, contenttype_name, css/user prefs; same for Gadget XML if present.
3. Spike: Rich Text (or Simple Text) as pure content type + **HTML-first** snippet template + JEXL binding rows (no Widget XML) and assembly parity test.
4. Spike: slot model extension for `slot_layout` / `slot_styles` schema + assembly context exposure.
5. Spike: HTML-first assembler placeholder syntax options (document pros/cons; pick one).
6. Align with Design SPA / Widget Builder so they author the modern package format, not private XML.

---

## 10. Code anchors (for implementers)

| Area | Path |
|------|------|
| Assembler SPI | `system/services/.../assembly/IPSAssembler.java` |
| Velocity assembler | `.../impl/plugin/PSVelocityAssembler.java` |
| Legacy/XSL assembler | `.../impl/plugin/PSLegacyAssembler.java` |
| Template + bindings | `.../data/PSAssemblyTemplate.java`, `PSTemplateBinding.java` |
| JEXL engine | `modules/utils/.../jexl/PSScript.java`, `PSJexlEvaluator.java` |
| JS **extension** handler (not bindings) | `PSJavaScriptExtensionHandler` (+ Rhino dependency in `pom.xml`) |
| CM1 page assembler | `projects/sitemanage/.../assembler/PSPageAssembler.java` |
| CM1 page context | `.../PSPageAssemblyContext.java` |
| Widget model | `.../data/PSWidgetDefinition.java` |
| Sample widget | `modules/perc-packages/.../Widgets/percRichText.xml` |
| Extension registration | `modules/extensions-main/.../Extensions.xml` |
| Workbench inventory | `docs/developer-module/workbench-functional-inventory.md` §7 |
| Design SPA placeholder | `docs/ai-generated/tasks/design-templates-item-types/` |

---

## 11. Bottom line

You already have the **right seam** (`IPSAssembler` + templates + ordered **JEXL** bindings). The product pain is **three authoring/storage dialects** (XSL apps, classic Velocity+slots, CM1 region+widget/page/gadget XML) and **Velocity as the only friendly-ish text path**, not the binding language.

**Best 8.2 strategy:**

1. Keep **JEXL** for bindings (and leave Rhino JS **extensions** alone for now).  
2. Add **HTML-first** and **Markdown** assemblers so people are not forced through Velocity.  
3. Unify **slots/regions** and put **layout/styles on slots**.  
4. Repackage widgets as **content type + snippet templates**.  
5. **Ship product packages free of Page / Widget / Gadget XML definition files**, with upgrade conversion for existing systems.

Rhino in the stack is explained by the **JavaScript extension/UDF handler**, not by template bindings. Bindings have been JEXL; this plan keeps them that way.
