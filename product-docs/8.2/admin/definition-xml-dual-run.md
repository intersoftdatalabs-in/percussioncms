---
id: admin-definition-xml-dual-run
title: Convert definition XML (dual-run)
description: Convert customer Widget, Page, and Gadget definition XML to modern component packages during the 8.2 dual-run window
version: "8.2"
order: 47
tags: [admin, upgrade, packages, widgets, dual-run]
---

# Convert definition XML (dual-run, retired)

> **Retired 2026-11-08 (#2852):** the 8.2 dual-run window is closed. The CMS loads
> **modern component packages only** and **no longer falls back** to customer
> **Widget / Page definition XML** when no modern package is present — that id now fails
> with a clear error naming the retired XML and the compiler path. Convert any remaining
> customer XML **before** relying on those ids. (Gadget registry XML fallback was a separate
> surface and was retired via #5810: the dashboard gadget map is modern-catalog-only; see below.)
>
> **M1 retirement evidence** (ship-path inventory; #3736 / PR #3750, re-verified #5756 /
> #5780): committed product Widget definition XML **0**, Widget waiver set **empty**,
> **81** `component-package.json` under Packages, Pages/Gadgets ship-path **0**.

Percussion CMS **8.2** loads **modern component packages**. Product Widget Builder, Design
create, and shipped product packages already write **modern** format only.

This page is the operator **convert → deploy modern** path for **customer** definition
XML that remains after upgrade.

## 8.2 support statement

| Posture | What it means in 8.2 |
|---------|----------------------|
| **Modern first** | If `component-package.json` is present for a definition id, the CMS uses the modern package. Co-located legacy XML is **not** preferred. |
| **Legacy XML no longer loads (retired)** | If modern is **absent**, the id fails with a clear retired-legacy error — legacy Widget / Page definition XML on disk is **not** loaded, even when present. Convert it with the compiler path below. |
| **Neither is an error** | Missing both modern and XML fails with a clear error naming the definition id. There is no silent default. |
| **Product packages** | Shipped product widgets and page layouts already author modern packages. Native page install is **not** the same as removing dual-run. See [Product page packages](id:developer-page-packages). |
| **Shim retired** | The runtime dual-run fallback was hard-disabled (#2852). Do **not** reintroduce a legacy fallback to “keep XML working” — convert the XML instead. |
| **Gadget registry** | Gadget `GadgetRegistry.xml` fallback was a **separate surface** and is retired (#5810): the dashboard gadget map loads the modern catalog only, and a missing catalog yields an empty map with a clear error. Do not reintroduce it. |

**Honest product messaging:**

1. 8.2 upgrade does **not** require you to convert every customer XML file on day one —
   but after retirement, **unconverted Widget / Page ids fail clearly** instead of loading
   legacy XML. Convert before you depend on those ids.
2. Convert incrementally: inventory → compile modern → deploy modern → smoke → then remove
   **that** XML.
3. After modern is deployed, old XML may remain on disk; **modern still wins**.
4. Do **not** treat product/QA “zero legacy on product widgets” as proof your **customer**
   XML is converted. Customer XML without a modern package is now an error, not a fallback.

## Dual-run is not dual-ship

| Concept | Layer | Operator takeaway |
|---------|-------|-------------------|
| **Dual-ship** | Package **build** (`*.templateDef` next to modern `pages/`) | Retired as the **product ship** path. Product page packages use native install. |
| **Dual-run** | **Runtime** selection (was: modern package vs definition XML) | **Retired** — selection is modern-only. Do not turn dual-ship back on to “keep XML working”; convert the XML instead. |

Do not turn dual-ship back on to “keep XML working” — dual-run is retired and no
longer loads legacy XML at runtime; convert the XML instead.
Details: [Product page packages](id:developer-page-packages).

This page is also **not** the XSL assembler cookbook. Existing **Legacy / XSL**
templates keep running; convert those separately — see
[XSL and legacyAssembler support](id:admin-xsl-legacy-assembler).

## Where customer XML still applies

Inventory **your** install (staging first). Do **not** copy product engineering
inventories or customer name lists into a shared runbook.

| Kind | Typical leftover location (under the CMS install root) | Modern target |
|------|--------------------------------------------------------|---------------|
| **Widget** | `rxconfig` → `Widgets` → `*.xml` (and the same files inside a customer `.ppkg` / package source) | `widgets/<stem>/component-package.json` plus template sources |
| **Page layout** | `rxconfig` → `Pages` → definition XML, or package `pages/` missing while XML remains | `pages/<id>/component-package.json` plus template sources (prefer native page install) |
| **Gadget** | Classpath / install gadget registry XML, or `rxconfig` → `Gadgets` | Modern gadget catalog (`gadget-catalog.json`) or a gadget `component-package.json` (`catalog.kind` = `gadget`) |

Product widgets may still **materialize** install-wire Widget XML at package-build time.
That wire format is not a reason to author new customer XML.

**Convert XML before relying on the id** when:

- You have not compiled and deployed a matching modern package yet (unconverted Widget /
  Page ids now fail clearly instead of loading legacy XML).
- Preview / page edit / gadget load still depends on that id and you have not smoked
  the modern package.
- There is no waiver path for unconverted ids — the legacy fallback is retired, so
  convert the XML instead of requesting an exception.

## Convert → deploy modern (operator cookbook)

Work on a **clone or lower environment** that mirrors production package shape. Back up
the install tree and database first (see [Upgrade Overview](id:upgrade-overview)).

### 1. Inventory (your host only)

1. List customer Widget XML under the install `rxconfig` / `Widgets` directory (and
   customer package archives if you still ship `.ppkg` sources).
2. Repeat for Pages and Gadgets if those directories exist on the host.
3. Record definition **ids / file stems** you own. Skip product ids that already have
   modern packages under `Packages` / `Modern`.
4. Do **not** paste customer inventories, host paths with secrets, or support dumps
   into product documentation.

### 2. Convert XML to a modern component package

Compilers live in the `perc-packages` module (JDK 21, repo Maven wrapper). They **read**
legacy XML as **upgrade input** and **emit** `component-package.json` plus artifacts.
They are not a new authoring format.

From a checkout of this repository, after the module is compiled, run one package
directory at a time. Use a **staging copy** of the customer package (or a folder that
contains `rxconfig` / `Widgets` XML, which the compiler also accepts).

**Windows** (from `modules/perc-packages`):

```bat
..\..\mvnw.cmd -q exec:java -Dexec.classpathScope=compile ^
  -Dexec.mainClass=com.percussion.packages.widgetxml.PSWidgetXmlDualShip ^
  -Dexec.args="materialize-modern C:\path\to\staging\customer-package"
```

**Linux / macOS** (from `modules/perc-packages`):

```bash
../../mvnw -q exec:java -Dexec.classpathScope=compile \
  -Dexec.mainClass=com.percussion.packages.widgetxml.PSWidgetXmlDualShip \
  -Dexec.args="materialize-modern /path/to/staging/customer-package"
```

That writes `widgets/<stem>/component-package.json` (and template sources) under the
package directory.

For **page** definition / templateDef sources, use
`com.percussion.packages.pagexml.PSPageXmlDualShip` with the same
`materialize-modern <packageDir>` arguments. Prefer
[native page install](id:developer-page-packages) (`page.installMode=native`) when you
rebuild a `.ppkg`.

Review the generated manifest (`schemaVersion` `1.0`, stable `id`, templates / content
types as required). Fix compile errors on the **XML input** or the emitted sources
before deploy. Engineering compiler notes (not product help): repository
`docs/ai-generated/tasks/template-assembler-normalization/` (dual-run shim policy,
shim-removal **criteria**, widget/page/gadget inventories).

### 3. Deploy modern (keep XML until smoke passes)

1. Stage the modern tree on the CMS host under the install root
   `Packages` → `Modern` → `<packageName>` → `widgets` → `<stem>` →
   `component-package.json` (pages use `pages` → `<id>` instead of `widgets`).
2. Leave `widgetDao.modernPackageRoots` **blank** unless you need an explicit override.
   Blank uses product defaults: discover `Packages` / `Modern` under the deploy directory
   (and materialize from the product classpath when that tree is empty).
3. If you must override, set `widgetDao.modernPackageRoots` to a list of package roots
   separated by the **OS path-list separator** (`File.pathSeparator`: `;` on Windows,
   `:` on Unix). Do not mix those list separators with file-path separators.
4. Restart CMS (or wait for the widget repository poll) so selection re-runs.
5. **Do not** delete the matching XML yet.

New widgets and templates created in the product UI already skip Widget definition XML
— see [Design templates](id:admin-design-templates).

### 4. Verify modern wins

| Check | Pass signal |
|-------|-------------|
| Page / widget that uses the converted id | Edit and preview work on a non-production site |
| Widget dual-run log | INFO `Widget definition dual-run selection: modern=…, legacyWidgetXml=…` — converted ids resolve modern; `legacyWidgetXml` stays `0` (retired, never selected) |
| Gadget load | INFO `Gadget registry dual-load selection: … source=…` — product classpath `MODERN_CATALOG`; the gadget XML fallback is retired (#5810), `legacyRegistryXml` always `0` |
| Neither | No mass `PSDefinitionSourceNotFoundException` / “expected modern vs legacy” errors for ids you converted |

Modern present **and** old XML still on disk is expected during conversion: selection
is **modern-only** (retired XML on disk is ignored).

The convert → modern-selection round trip is pinned by automated regression coverage
(`PSCustomerLegacyXmlUpgradePathTest` in `modules/perc-packages`): legacy Widget XML,
Page templateDef, and Gadget registry fixtures compile through the normalized path to
`component-package.json`, the retired shim then selects `MODERN` for each converted id
(even with the legacy XML still on disk), and an unconverted legacy-only id fails with
the retired-legacy error naming the compiler upgrade path.

### 5. Remove XML only for converted ids

After smoke/parity on the converted definitions:

1. Remove **those** customer XML files from the install Widgets / Pages / Gadgets
   directories (and from customer package sources you still build).
2. Keep a backup of the XML until you are confident you will not roll back.
3. Repeat for the next id. Do not mass-delete XML for ids that still have no modern
   package.

### 6. After retirement: convert, do not reintroduce a fallback

Do **not**:

- Reintroduce a legacy Widget / Page XML fallback because an unconverted id errors —
  convert that XML with the compiler path above instead
- Remove gadget `GadgetRegistry.xml` because the modern catalog loaded once
  (it is already retired as a load path via #5810; never-loaded file, not a fallback)
- Claim QA “M2” telemetry alone proves every customer id converted — convert and smoke
  each id you depend on

Retired legacy XML on disk is never loaded; the error names the file and points back
at this convert path.

## Optional: confirm product packages are already modern

You do **not** need to convert shipped product widgets. Optional inventory CLIs
(after compiling `perc-packages`) scan **product** package source trees — they are
CI gates, not a customer dump. Example, from `modules/perc-packages`:

**Windows:** `..\..\mvnw.cmd -q exec:java -Dexec.classpathScope=compile -Dexec.mainClass=com.percussion.packages.widgetxml.PSWidgetDefinitionXmlInventory -Dexec.args="src/main/resources/Packages"`

**Unix:** `../../mvnw -q exec:java -Dexec.classpathScope=compile -Dexec.mainClass=com.percussion.packages.widgetxml.PSWidgetDefinitionXmlInventory -Dexec.args="src/main/resources/Packages"`

The `-Dexec.args` value is a relative path from `modules/perc-packages`. Forward slashes work on Windows with this Java CLI.

Page and gadget siblings: `PSPageDefinitionXmlInventory`, `PSGadgetDefinitionXmlInventory`.
A **zero** product ship-path count does **not** mean your **customer** XML is gone.

## What this page does not do

- Reintroduce the retired dual-run fallback or the retired gadget XML fallback
- Force conversion of every XML file during 8.2 upgrade
- Convert XSL / `legacyAssembler` templates (see
  [XSL and legacyAssembler support](id:admin-xsl-legacy-assembler))
- Publish customer inventories, host secrets, or support dumps
- Change native vs dual-**ship** page package build (see
  [Product page packages](id:developer-page-packages))

## Related

- [Upgrade Overview](id:upgrade-overview)
- [Product page packages](id:developer-page-packages)
- [Design templates](id:admin-design-templates)
- [XSL and legacyAssembler support](id:admin-xsl-legacy-assembler)
- [Extensions & packages](id:developer-extensions)
- [Installation Overview](id:install-overview)
- [Glossary](id:reference-glossary)
