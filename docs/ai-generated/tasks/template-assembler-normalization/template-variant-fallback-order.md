# Template variant fallback order (registry precedence)

| Field | Value |
|-------|-------|
| **Status** | **Evidence locked 2026-11-16** (issue #6476 / parent #2626) |
| **Issue** | [#6476](https://github.com/intersoftdatalabs-in/percussioncms/issues/6476) — slice 1 of the #2626 triage expansion: registry precedence tests + criteria doc + product-docs |
| **Parent** | [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) — Template & assembler model normalization (epic) |
| **Registry** | `PSPageXmlTemplateDefEmitter.REGISTERED_ASSEMBLER_PATHS` (`modules/perc-packages`) — short manifest assembler id → product extension path, registration order |
| **Tests** | `PSAssemblerRegistryPrecedenceTest` (`modules/perc-packages`, 6/6 green) |
| **Operator doc** | `product-docs/8.2/admin/template-normalization-migration.md` § Assembler fallback order |

## Why this exists

Template normalization keeps one assembler model per template (Velocity, HTML-first,
Markdown, legacy XSL), selected through the short `templates[].assembler` id. The
template-variant assembler (`pageVariantAssembler` — renders a page with a specified
template instead of the page-registered one) is a first-class registry row, but until
this slice no single place pinned the **fallback order** across the four surfaces that
touch the registry. This doc is that place; the JUnit class above is the CI-assertable
evidence.

## Fallback order (highest precedence first)

### 1. Compiler normalization — `PSPageXmlCompiler.mapAssembler` (lenient, upgrade input)

| # | Input | Result |
|---|-------|--------|
| 1 | Blank / null | `pageAssembler` (default beats every other rule) |
| 2 | Extension path whose leaf names a known assembler (case-insensitive) | Normalized short id (`pageVariantAssembler`, `legacyAssembler` for `xslAssembler`, …) |
| 3 | Extension path whose leaf ends in `Assembler` but is unknown | Leaf passes through unchanged |
| 4 | Anything else | Leaf + `Assembler` suffix |

### 2. Lenient upgrade emit — `PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath` (dual-ship / customer XML)

| # | Input | Result |
|---|-------|--------|
| 1 | Blank / null | `Java/global/percussion/assembly/pageAssembler` (default) |
| 2 | Value containing `/` (full path) | Passes through untouched, even when unregistered |
| 3 | Registered short id (case-insensitive, `xslAssembler` alias included) | Registered extension path |
| 4 | Unregistered short id | Product extension prefix + id (site-local custom assemblers still compile) |

### 3. Strict modern-only load — `PSPageXmlTemplateDefEmitter.resolveRegisteredAssemblerPath` (no fallback)

Short id, full-path leaf, or the `xslAssembler` alias resolve; **everything else throws
`IllegalArgumentException`** — blank, unregistered short ids, and unregistered full
paths. There is deliberately no step 2 here (issue #5864): an unmigrated assembler
surfaces as a migration blocker instead of a silently defaulted install.

### 4. Operator diagnostics — `PSAssemblerCatalogDiagnostics.diagnoseOne`

| # | Input | Verdict |
|---|-------|---------|
| 1 | Blank | BLOCKED — "blank assembler" + fix hint |
| 2 | Registered id (including `pageVariantAssembler`) | OK — resolves to the registered path |
| 3 | Unregistered full path | BLOCKED — "legacy-upgrade emit path" + short-id fix hint |
| 4 | Unregistered short id | BLOCKED — "unregistered assembler" + registered-id fix hint |

## Evidence

- `PSAssemblerRegistryPrecedenceTest` (6 tests): catalog registration order pins the
  `pageVariantAssembler` row; strict resolution covers short id / full path / any case /
  alias / all four throw branches; lenient emit covers all four fallback steps; compiler
  normalization covers all four fallback steps; diagnostics covers the variant-OK plus all
  three BLOCKED branches; the end-to-end chain walks **every** registered row through
  legacy path → compiler id → strict path → diagnostics OK.
- Peer coverage this slice does not duplicate: `PSPageXmlDualShipTest`
  (`toLegacyAssemblerPath_knownShortNames`, `assemblerCatalog_registersModernPageAssemblers`,
  `resolveRegisteredAssemblerPath_strictModernOnlyResolution`),
  `PSPageXmlCompilerTest.mapAssembler_extensionPathAndShortNames`,
  `PSAssemblerCatalogDiagnosticsTest` (per-surface verdicts).

## Operator pointers

- Author `templates[].assembler` with a short registered catalog id
  (`pageAssembler`, `pageVariantAssembler`, `velocityAssembler`, `htmlAssembler`,
  `markdownAssembler`, `legacyAssembler`, `dispatchAssembler`, `resourceAssembler`,
  `pageDatabaseAssembler`, `binaryAssembler`); `xslAssembler` still counts as
  `legacyAssembler`.
- A blank or mistyped assembler on the modern load path fails fast — run the assembler
  catalog diagnostics (see `PSAssemblerCatalogDiagnostics.Report#toMarkdown`) instead of
  guessing; the report names the exact fix per template.
- Customer legacy XML with site-local custom assemblers still compiles through the
  lenient upgrade path only — modern manifests must name a registered id.
