---
id: admin-template-normalization-migration
title: Template normalization migration guide
description: Migrate legacy Widget, Page, and Gadget definition XML to normalized modern component package manifests, with minimal before/after samples
version: "8.2"
order: 48
tags: [admin, upgrade, packages, widgets, templates, assembler, migration]
---

# Template normalization migration guide

Percussion CMS **8.2** loads **modern component packages only**. Legacy Widget, Page,
and Gadget definition XML is **never loaded at runtime** (dual-run retired, #2852;
gadget registry fallback retired separately, #5810). The supported upgrade path is
**template normalization**: the `perc-packages` compilers read your legacy definition
XML as upgrade input and emit normalized `component-package.json` manifests plus
template sources. This page shows what normalization does to each definition kind
with minimal before/after samples.

This page is the **what-changes** companion. The step-by-step operator cookbook
(inventory → compile → deploy → smoke → remove XML) lives at
[Convert definition XML (dual-run)](id:admin-definition-xml-dual-run).

## What normalization means

| Legacy definition XML | Normalized modern manifest |
|---|---|
| Widget `<WidgetPrefs title, contenttype_name, category, description, author, thumbnail, preferred_editor_*>` | `catalog.*` (`title`, `category`, `description`, `author`, `thumbnail`/`icon`, `preferredEditorWidth/Height`, `responsive`, `paletteVisible`) |
| Widget `<UserPref>` / `<CssPref>` | `userPreferences[]` / `cssPreferences[]` |
| Widget `<Code type="jexl">` | `templates[].bindings[]` (per-variable expressions plus a `__widgetCode` entry carrying the script) |
| Widget `<Content type="velocity">` | `templates[]` with `assembler: velocityAssembler` and `sourceRef: templates/<name>.vm` |
| Page `<assembly-template>` `<name>` / `<label>` | Manifest `id` / `name` |
| Page `<assembler>` Java extension path | Short manifest assembler id (see mapping below) |
| Page `<template-slot-ids>` / `#region("…")` slots | `slots[]` with layout orientation and styles |
| Gadget registry `<gadget name baseuri file>` | One manifest per gadget (`catalog.kind: gadget`, id from the `baseuri` leaf) plus a `gadget-catalog.json` entry |

`schemaVersion` is `1.0` on every emitted manifest. Review the generated manifest and
fix compile errors on the **XML input** or the emitted sources before deploy.

## Assembler normalization

The compilers map legacy assembler spellings (often full Java extension paths) to
short manifest assembler ids:

| Legacy value | Normalized `assembler` |
|---|---|
| `Java/global/percussion/assembly/pageAssembler` (or blank) | `pageAssembler` |
| `…/pageVariantAssembler` | `pageVariantAssembler` |
| `…/velocityAssembler` | `velocityAssembler` |
| `…/htmlAssembler` | `htmlAssembler` |
| `…/markdownAssembler` | `markdownAssembler` |
| `…/legacyAssembler` or `…/xslAssembler` | `legacyAssembler` |
| `…/dispatchAssembler` | `dispatchAssembler` |
| Widget `<Content type="velocity">` / `"vm"` | `velocityAssembler` |
| Widget `<Content type="html*">` | `htmlAssembler` |
| Widget `<Content type="markdown">` / `"md"` | `markdownAssembler` |

Normalization only renames the assembler id in the manifest. Existing **Legacy / XSL**
templates keep running — see [XSL and legacyAssembler support](id:admin-xsl-legacy-assembler).

## Sample 1 — widget definition XML to component package

Before (legacy `rxconfig/Widgets/percSimpleText.xml`, trimmed):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Widget>
	<WidgetPrefs title="Simple Text"
		contenttype_name="percSimpleTextAsset" category="content"
		description="Widget used for entering plain text"
		author="Percussion Software Inc"
		thumbnail="/rx_resources/widgets/PSWidget_SimpleText/images/widgetIconSimpleText.png"
		preferred_editor_width="640" preferred_editor_height="500"
		is_responsive="true" />
	<CssPref name="rootclass" display_name="CSS Root Class"
		datatype="string" />
	<Code type="jexl">
        <![CDATA[
    $rootclass = $perc.widget.item.cssProperties.get('rootclass');
    …
    ]]>
	</Code>
	<Content type="velocity"><![CDATA[<div$!{classAttribute}>
…
</div>]]></Content>
</Widget>
```

After (normalized `widgets/percSimpleText/component-package.json`, trimmed):

```json
{
  "schemaVersion": "1.0",
  "id": "percSimpleText",
  "name": "Simple Text",
  "description": "Widget used for entering plain text",
  "catalog": {
    "kind": "component",
    "title": "Simple Text",
    "category": "content",
    "preferredEditorWidth": 640,
    "preferredEditorHeight": 500,
    "responsive": true,
    "paletteVisible": true
  },
  "templates": [
    {
      "name": "percSimpleTextSnippet",
      "type": "snippet",
      "assembler": "velocityAssembler",
      "sourceRef": "templates/percSimpleTextSnippet.vm",
      "contentType": "percSimpleTextAsset",
      "bindings": [
        {
          "variable": "rootclass",
          "expression": "$perc.widget.item.cssProperties.get('rootclass')"
        }
      ]
    }
  ],
  "cssPreferences": [
    {
      "name": "rootclass",
      "displayName": "CSS Root Class",
      "datatype": "string"
    }
  ]
}
```

The compiler also emits the snippet source (`templates/percSimpleTextSnippet.vm`)
and relocates the thumbnail under `resources/`. Compile one staging package at a
time with `PSWidgetXmlDualShip materialize-modern <packageDir>` — see the
[convert cookbook](id:admin-definition-xml-dual-run).

## Sample 2 — page templateDef to component package

Before (legacy `perc.base.plain.templateDef`, trimmed):

```xml
<?xml version="1.0" encoding="utf-8"?>  <assembly-template id="1">
    <bindings/>
    <template-slot-ids/>
    …
    <assembler>Java/global/percussion/assembly/pageAssembler</assembler>
    …
    <label>Plain</label>
    …
    <name>perc.base.plain</name>
    <output-format>Page</output-format>
    …
    <template>#perc_templateHeader()##
  &lt;div id="perc-container" class="perc-region perc-vertical"&gt;
      …
                        #region("perc-content" '' '' '' '')##
      …
#perc_templateFooter()</template>
    …
</assembly-template>
```

After (normalized `pages/perc.base.plain/component-package.json`, trimmed):

```json
{
  "schemaVersion": "1.0",
  "id": "perc.base.plain",
  "name": "Plain",
  "description": "Percussion base layout templates.",
  "catalog": {
    "kind": "page",
    "title": "Plain",
    "category": "page",
    "paletteVisible": true
  },
  "templates": [
    {
      "name": "perc.base.plain",
      "type": "page",
      "assembler": "pageAssembler",
      "sourceRef": "templates/perc.base.plain.vm",
      "bindings": []
    }
  ],
  "slots": [
    {
      "name": "perc-content",
      "allowedContentTypes": [],
      "layout": {
        "orientation": "vertical"
      },
      "styles": {
        "rootclass": "perc-region perc-vertical"
      }
    }
  ]
}
```

Note the assembler line: the Java extension path
`Java/global/percussion/assembly/pageAssembler` normalizes to the short id
`pageAssembler`, and the `#region("perc-content" …)` call becomes the `slots[]`
entry. Compile with `PSPageXmlDualShip materialize-modern <packageDir>` (prefer
[native page install](id:developer-page-packages) when you rebuild a `.ppkg`).

## Sample 3 — gadget registry entry to gadget package

Before (legacy `GadgetRegistry.xml`, one entry):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gadgets>
	<group name="Percussion">
		…
		<gadget name="Welcome"
			baseuri="/cm/gadgets/repository/cm1_welcome_gadget"
			file="perc_welcome_gadget.xml" />
		…
	</group>
	…
</gadgets>
```

After (normalized `cm1_welcome_gadget/component-package.json`, trimmed):

```json
{
  "schemaVersion": "1.0",
  "id": "cm1_welcome_gadget",
  "name": "Welcome",
  "version": "8.2.0",
  "description": "Dashboard gadget package for 'Welcome' (group=Percussion)",
  "catalog": {
    "kind": "gadget",
    "title": "Welcome",
    "category": "Percussion",
    "paletteVisible": true
  },
  "resources": [
    {
      "path": "resources/host-ref.txt",
      "target": "cm/gadgets/repository/cm1_welcome_gadget",
      "type": "file"
    }
  ]
}
```

The gadget id comes from the `baseuri` leaf (`cm1_welcome_gadget`), the group
(`Percussion`) becomes the catalog `category`, and the host path becomes a
`resources[]` target. The registry compiles to one manifest per gadget plus a
`gadget-catalog.json` index. The old registry XML is never loaded (#5810) — the
dashboard gadget map is modern-catalog-only.

## Typical emitted layout

After compiling a staging customer package, expect this shape (widget example —
pages use `pages/<id>/`, gadgets one directory per gadget):

```text
customer-package/
  widgets/percSimpleText/component-package.json
  widgets/percSimpleText/templates/percSimpleTextSnippet.vm
  widgets/percSimpleText/resources/widgetIconSimpleText.png
  rxconfig/Widgets/percSimpleText.xml   ← legacy leftover: keep until smoke passes
```

Deploy the modern tree under the install `Packages` / `Modern` root, leave the
XML in place, smoke the converted ids (edit + preview on a non-production site),
then remove **only** the XML for converted ids. Full sequence:
[Convert definition XML (dual-run)](id:admin-definition-xml-dual-run).

Before converting, you can archive a parse → compile → manifest-readback dry run
(`PSPageXmlMigrationDryRun`, `Report#writeTo`) that persists a per-package status
panel as `migration-dry-run.md` plus `migration-dry-run.txt` — READY rows are the
convert list, BLOCKED rows name what to fix first.

## What this page does not do

- Replace the operator convert → deploy cookbook — see
  [Convert definition XML (dual-run)](id:admin-definition-xml-dual-run)
- Convert XSL / `legacyAssembler` templates (see
  [XSL and legacyAssembler support](id:admin-xsl-legacy-assembler))
- Change native vs dual-**ship** page package build (see
  [Product page packages](id:developer-page-packages))
- Force conversion of every XML file during 8.2 upgrade — convert incrementally,
  but unconverted Widget / Page ids now fail clearly instead of loading legacy XML

## Related

- [Convert definition XML (dual-run)](id:admin-definition-xml-dual-run)
- [Product page packages](id:developer-page-packages)
- [Design templates](id:admin-design-templates)
- [XSL and legacyAssembler support](id:admin-xsl-legacy-assembler)
- [Extensions & packages](id:developer-extensions)
- [Upgrade Overview](id:upgrade-overview)
- [Glossary](id:reference-glossary)
