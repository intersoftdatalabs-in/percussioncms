# XSL / legacyAssembler migration cookbook (8.2)

| Field | Value |
|-------|--------|
| **Status** | Active — Phase 5 docs (#2834) |
| **Parent** | [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632) Phase 5 · epic [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) |
| **Audience** | Implementers / integrators leaving legacy XSL assembly |
| **Related** | [ADR-002](./adr/002-assembler-set.md), [plan.md](./plan.md) §3.4 / §5 Phase 5, [parity-notes.md](./parity-notes.md) §7, [binding-modules.md](./binding-modules.md), [implementer-guide.md](./implementer-guide.md) (when present — #2833 / PR #2845) |

## Purpose

This cookbook helps sites that still assemble content through **`legacyAssembler`** (XML application + stylesheet / XSL path) move toward the **supported modern text assemblers** without waiting for a forced runtime removal.

It does **not**:

- Remove XSL / `legacyAssembler` from the product in this track
- Provide mass automated content conversion
- Replace template **bindings** with JavaScript (bindings stay **JEXL** — [ADR-001](./adr/001-jexl-bindings-stay.md))

For Rhino **JavaScript extensions** (non-assembly UDFs), see the short companion note: [rhino-js-extension-note.md](./rhino-js-extension-note.md).

---

## 8.2 support statement

| Posture | What it means in 8.2 |
|---------|----------------------|
| **Supported & recommended (new design)** | **Velocity** (power), **HTML-first** (`htmlAssembler`), **Markdown** (`markdownAssembler`), with ordered **JEXL** bindings |
| **Supported legacy (existing installs)** | `legacyAssembler` + classic XML apps / stylesheets that already work; classic Workbench Active Assembly; pre-upgrade definition XML still loadable via dual-run shims until converted |
| **No new investment** | New product packages, new Design SPA defaults, new help examples, or feature work that *requires* XSL / `legacyAssembler` |
| **Not removed in 8.2** | `PSLegacyAssembler`, installer data paths that still register legacy variants, customer XSL that continues to assemble |

**Product messaging (honest):**

1. Existing XSL / `legacyAssembler` content **continues to run** in 8.2.
2. **New** templates and product packaging should **not** choose XSL.
3. There is **no hard “must convert XSL on upgrade” cliff** in 8.2; conversion is operator-driven and incremental.
4. Long-term direction (ADR-002 / plan): XSL is **compatibility only**. Prefer HTML-first / Markdown for simple bodies; Velocity when macros, loops, or existing AA patterns are required.

Specialized non-text assemblers (**Binary**, **Dispatch**, **Database**, **Resource**) are unchanged by this cookbook — they are purpose assemblers, not XSL languages.

---

## How legacy XSL assembly works (inventory surfaces)

### Runtime seam

| Piece | Role |
|-------|------|
| Extension / assembler id | `legacyAssembler` |
| Implementation | `system/.../impl/plugin/PSLegacyAssembler.java` |
| Behavior | Proxies an **internal request** to the template’s **assembly URL** (XML application resource path). Acts as a **marker** so the assembly service **skips JEXL binding processing** for that item. |
| Template fields that matter | Assembler name = `legacyAssembler`; **assembly URL** points at the XML app resource; variant/template id forced into request params (`sys_variantid`, etc.) |

Key implication for migration: **legacy assembly does not run ordered template bindings**. Any “logic” lives in the XML app / XSL / request parameters, not in modern `PSTemplateBinding` rows.

### Surfaces to inventory on a site

Use this checklist to find remaining XSL / legacy usage before planning cutover.

| Surface | What to look for | Where / how |
|---------|------------------|-------------|
| **Assembly templates** | Assembler = `legacyAssembler` (or historic XSL variant names mapped to it) | Design SPA template list / Workbench template editor; REST `GET` templates; DB assembly template rows |
| **Assembly URL** | Paths into classic XML applications (`…/sys_…`, app-specific resources) | Template property `assemblyUrl` / equivalent |
| **XML applications** | Stylesheet resources, query/result pipes feeding XSL | `rxconfig` / installed app trees under the CMS install; Workbench Application Explorer |
| **Publish / site variants** | Publish templates still on legacy assembler | Site publish templates, content type default variants |
| **Slots / related content** | Snippets assembled via legacy variants inside AA slots | Slot allowed templates; preview assembly |
| **Custom packages** | Customer or partner packages shipping XSL bodies | Deployed packages under `Packages` / custom install |
| **Product baseline** | Prefer **not** to add new XSL; inventory is for **customer/custom** debt | Product widgets are already JEXL + Velocity (see [widget-xml-inventory.md](./widget-xml-inventory.md)) |

### What product packages already look like

All **48** product widget definitions use `Code type=jexl` and `Content type=velocity` ([widget-xml-inventory.md](./widget-xml-inventory.md)). Product page templates use `pageAssembler` → Velocity (`PSPageAssembler` extends `PSVelocityAssembler`). So **product out-of-box** is not an XSL migration problem; **customer and historic custom** templates are.

---

## Target assemblers (where to land)

Pick a **text assembler** per template. Bindings remain **JEXL** in all cases except pure static HTML-first with zero dynamics.

| Target | When to choose | Assembler id | Body model |
|--------|----------------|--------------|------------|
| **HTML-first** | Mostly static HTML; few dynamic values; no Velocity macros | `htmlAssembler` | HTML + `${dotted.path}` placeholders ([ADR-002](./adr/002-assembler-set.md)) |
| **Markdown** | Content-oriented prose → HTML | `markdownAssembler` | CommonMark after JEXL bindings |
| **Velocity** | Macros, loops, AA slot macros, existing Velocity snippets | `velocityAssembler` | Velocity 2.x + JEXL-bound `$vars` |
| **Page context** | CM1 page/template with `$perc` / regions | `pageAssembler` today (thin context + Velocity; long-term “context + text assembler”) | Same as Velocity body with page bindings |

**Do not** migrate XSL assembly into Rhino / `handler="JavaScript"` extensions. That path is **not** an assembler (see [rhino-js-extension-note.md](./rhino-js-extension-note.md)).

Picker / module cheat sheet: [binding-modules.md](./binding-modules.md).

---

## Migration steps (per template or small batch)

Work **one template (or one content type’s variants)** at a time. Golden HTML diffs beat big-bang cutovers.

### 1. Inventory and freeze

1. List templates with assembler `legacyAssembler` (or XSL-era variants).
2. Record for each: content types using it, sites/publish contexts, assembly URL, MIME type, AA type, dependencies on request params.
3. Capture **golden outputs**: preview + publish HTML (or binary) for representative items (draft + public if filters differ).
4. Note any XML app resources **shared** by multiple templates (shared stylesheets complicate per-template cutover).

### 2. Classify complexity

| Class | Symptoms | Recommended target |
|-------|----------|--------------------|
| **A — Static shell** | XSL mostly emits fixed markup + a few field values | HTML-first + JEXL bindings + `${path}` |
| **B — Light transform** | Field maps, simple conditionals, string formatting | HTML-first or Markdown; put conditionals in **JEXL bindings** |
| **C — Structural / iterative** | Loops over nodes, complex grouping, slot-like expansion in XSL | Velocity (or restructure as modern slots + snippet templates) |
| **D — Full XML app pipeline** | Multi-resource apps, queries, non-HTML mime, deep request param contracts | Keep on `legacyAssembler` until the **pipeline** is redesigned; do not “paste XSL into Velocity” |

### 3. Rebuild the modern template

1. Create a **new** assembly template (keep the legacy template until parity is signed off).
2. Set assembler to `htmlAssembler`, `markdownAssembler`, `velocityAssembler`, or `pageAssembler` as classified.
3. **Port data access to JEXL bindings** (ordered list), not XSL `select`/`value-of` only:
   - Item fields, `$sys.*`, `$rx.*` tools, related content helpers as documented in [binding-modules.md](./binding-modules.md).
   - Remember: modern path **runs** bindings; legacy path **skipped** them.
4. Port markup:
   - Class A/B → HTML-first body with `${bindingKey}` / `${sys…}` only (no bare `$title`, no Mustache, no Velocity directives in HTML-first).
   - Class C → Velocity body; prefer small macros over one giant stylesheet.
5. Wire the same (or intentional) MIME / charset / AA type / sites as the legacy template.

### 4. Dual-run parity

1. Point a **non-production** content type variant or site publish template at the new template first.
2. Diff golden HTML (normalize whitespace / line endings).
3. Exercise: edit mode vs publish, filters/auth types, empty fields, multi-valued fields, related content in slots.
4. Only then switch default variants / publish templates in production.

### 5. Retire the legacy path (local site policy)

1. Unlink content types / publish configs from the legacy template.
2. Archive or document the old assembly URL + stylesheet for rollback.
3. Leave `legacyAssembler` registered on the server — **site-level** retirement does not require product runtime removal.
4. Optionally track “zero remaining legacy templates on this install” as an operator metric; product-wide XSL removal remains a later residual (not this doc).

### 6. Packaging (when applicable)

If the template ships in a package, follow the modern **component package** model ([component-package-manifest.md](./component-package-manifest.md), ADR-004): content type + template(s) + slots + catalog — **not** new Page/Widget/Gadget definition XML, and **not** new XSL app packaging for greenfield work.

---

## Mapping cheat sheet (XSL ideas → modern)

| Legacy XSL / app idea | Modern approach |
|----------------------|-----------------|
| Stylesheet field copy `value-of` | JEXL binding → `${field}` (HTML-first) or `$field` (Velocity) |
| `xsl:if` / choose | JEXL binding computing a flag or preformatted fragment; or Velocity `#if` |
| `xsl:for-each` over children | Velocity `#foreach`; or model as **slot** + related items + snippet templates |
| Call another XML resource | Snippet template include / slot expansion / internal link helpers — not a second XSL app when avoidable |
| Request parameters as control surface | Prefer bindings + template config; document any remaining HTML params |
| “No bindings” mental model | **Invert**: move pure presentation to body; move data shaping to **ordered JEXL** |
| Binary or non-HTML pipeline | Specialized assembler or keep legacy until redesigned |

---

## Explicit non-goals (this cookbook)

- Deleting `PSLegacyAssembler` or unregistering `legacyAssembler`
- Mass-conversion scripts or overnight bulk rewrites of customer XSL
- Migrating **JEXL bindings** to GraalJS / Rhino / dual language columns
- Forcing conversion during 8.2 upgrade
- Treating gadgets as assembly templates (gadgets are not XSL assembly — see [gadget-definition-inventory.md](./gadget-definition-inventory.md))

---

## Related Phase 5 docs

| Doc | Role |
|-----|------|
| [implementer-guide.md](./implementer-guide.md) | Single Assemblers & Templates implementer guide (#2833) — link when landed |
| [rhino-js-extension-note.md](./rhino-js-extension-note.md) | Optional Rhino JS **extension** note (not an assembler) |
| [plan.md](./plan.md) | Strategic plan; Phase 5 item “XSL support statement + cookbook” |
| [parity-notes.md](./parity-notes.md) §7 | One-line legacy XSL parity note |
| [adr/002-assembler-set.md](./adr/002-assembler-set.md) | Assembler set decision |

---

## Acceptance (issue #2834)

- [x] Cookbook committed under this task folder
- [x] Explicit 8.2 support statement
- [x] Migration steps toward HTML-first / Velocity / Markdown per ADR-002
- [x] Cross-linked from task [README.md](./README.md); reciprocal link to Phase 5 implementer guide path
- [x] Companion Rhino note (non-assembly)
- [x] Docs-only — no XSL runtime removal
