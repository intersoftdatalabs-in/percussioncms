# Binding modules: `$sys`, `$rx`, `$perc`

**Phase 1 docs** (epic #2626 / Phase 1 #2628). Bindings remain **JEXL**.

## `$sys` (assembly system context)

Populated by the assembly framework before/during template render. Common keys (not exhaustive):

| Path | Role |
|------|------|
| `$sys.template` | Template source text for the current assembler |
| `$sys.mimetype` | Output MIME (default often `text/html`) |
| `$sys.charset` | Charset name |
| `$sys.site` | Site context (path, global template, …) |
| `$sys.assemblyItem` | Current assembly item |
| `$sys.part.*` | Partial assembly (Velocity AA parts) |
| `$sys.currentslot` | Slot context map (Velocity path) |

HTML-first / Markdown assemblers read `$sys.template` (or fall back to the template object source) and `$sys.mimetype` / `$sys.charset` after JEXL bindings run.

## `$rx` (JEXL tool namespaces)

Java classes annotated with `@IPSJexlMethod` registered under namespaces such as:

`asmhelper`, `codec`, `cond`, `db`, `doc`, `ext`, `guid`, `i18n`, `keyword`, `link`, `location`, `nav`, `pagination`, `session`, `string`, …

Use from **JEXL bindings** (and Velocity via the bound objects). Not a fourth template language.

## `$perc` (CM1 page context)

Bound by `PSPageAssembler` / page assembly context factory when assembling CM1 pages and page templates:

- Region results, widget instances, edit-mode flags, theme/head helpers, widget contents utilities

Normalization direction: keep `$perc` as a **documented context module**, not a reason for a separate template species. See `parity-notes.md`.

## Assembler choice (quick guide)

| Need | Assembler extension |
|------|---------------------|
| Simple HTML + variables | `Java/global/percussion/assembly/htmlAssembler` — `${path}` placeholders |
| Markdown content | `Java/global/percussion/assembly/markdownAssembler` |
| Macros, loops, `#parse`, AA macros | `Java/global/percussion/assembly/velocityAssembler` |
| CM1 page with regions | `pageAssembler` (today Velocity + `$perc`; thin context provider) |
| XSL / XML app | `legacyAssembler` (support only) |
