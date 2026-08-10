# Parity notes: models that must collapse

Living document for Phase 0–3 implementers. Not a customer-facing guide.

## 1. pageAssembler vs velocityAssembler

| | `velocityAssembler` | `pageAssembler` |
|--|---------------------|-----------------|
| Class | `PSVelocityAssembler` | `PSPageAssembler` extends `PSVelocityAssembler` |
| Extra work | JEXL bindings + Velocity render | Loads page/template bridge, builds `PSPageAssemblyContext`, binds `$perc`, then super.assemble |
| Template source | Assembly template body | Same (`template.getTemplate()`) |
| Typical use | Classic page/snippet/global templates | CM1 page + page template items |

**Normalization target:** `pageAssembler` becomes a thin “page context” preprocessor (or standard binding pack), not a separate template *species*. Source remains a text assembler (Velocity / HTML-first / Markdown).

## 2. Region vs slot

|    Concept    |                  CM1                   |                  Classic                  |
|---------------|----------------------------------------|-------------------------------------------|
| Hole          | Region (`regionId` in region tree)     | Slot (`PSTemplateSlot`, slotid)           |
| Fill          | Widget instances + asset relationships | Related content via finders / AA          |
| Allowed types | Widget definition + DnD prefs          | Allowed content types / templates on slot |
| Layout chrome | Widget `CssPref` / `UserPref`          | Mostly absent (HTML in template)          |

**Target:** one **Slot** abstraction. Regions rename/map to slots. Layout chrome moves to **`slot_layout` / `slot_styles`**.

## 3. Widget Code vs template bindings

| | Widget `Code` | Template bindings |
|--|---------------|-------------------|
| Language | JEXL only (schema) | JEXL only |
| Shape | Single CDATA block in XML | Ordered `VARIABLE` + `EXPRESSION` rows |
| When run | Widget assembly context | Before template body render |

**Target:** Widget Code becomes **binding rows** on the snippet template. Order preserved. Still JEXL.

## 4. Widget Content vs snippet template source

| | Widget `Content` | Assembly template source |
|--|------------------|--------------------------|
| Markup | velocity (all 48 product widgets) | Velocity (typical) |
| Macros | `#loadRelatedWidgetContents`, edit-mode helpers | `$rx.*`, `#parse`, global macros |

**Target:** Content CDATA becomes **snippet template source**. HTML-first/Markdown assemblers for simple widgets after conversion.

## 5. Global template vs theme/head fields

CM1 templates carry `htmlHeader`, `cssRegion`, `additionalHeadContent`, protected regions, theme. Classic uses **global** templates + `#inner()`.

**Target:** keep both patterns documented; do not drop CM1 head fields on upgrade—map to template properties or global includes.

## 6. Active Assembly vs visual Page Editor

| | AA | CM1 Page Editor |
|--|----|-----------------|
| Metaphor | Edit related content on preview | Drag widgets onto regions |
| Strength | Relationship editing | Layout placement |
| Weakness | No true layout design | Semi-WYSIWYG fidelity |

**Target long-term:** SPA Design owns layout; related-content editing may remain AA-like or fold into SPA. Not a Phase 1 requirement.

## 7. Legacy XSL variants

`legacyAssembler` proxies XML app + stylesheet. No JEXL bindings. Support-only; no new design investment. Migration cookbook later (outside forced 8.2 cut).
