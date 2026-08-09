# ADR-001: Keep JEXL for template bindings

| Field | Value |
|-------|--------|
| **Status** | Accepted |
| **Date** | 2026-08-09 |
| **Context** | Template/assembler normalization plan |

## Decision

**Template bindings and widget Code blocks remain JEXL** for the 8.2 normalization program.

## Rationale

- Entire product inventory uses JEXL (48/48 widgets; assembly templates via `PSTemplateBinding`).
- `$rx.*` tools and `@IPSJexlMethod` are JEXL-oriented.
- Binding-language migration (GraalJS, dual language column, etc.) is high risk / low urgency relative to packaging and assembler language problems.
- Rhino on the classpath serves **JavaScript extension UDFs** (`PSJavaScriptExtensionHandler`), not assembly bindings — do not conflate the two.

## Consequences

- No `LANGUAGE` column workstream in this program.
- No GraalJS dependency for assembly.
- Docs and Design UI continue to say “JEXL bindings.”
- Optional future revisit is out of scope here.

## Non-decision

Health of Rhino **JavaScript extensions** is a separate topic.
