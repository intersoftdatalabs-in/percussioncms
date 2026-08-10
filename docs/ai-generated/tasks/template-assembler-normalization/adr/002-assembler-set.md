# ADR-002: Assembler set for 8.2+

|   Field    |        Value         |
|------------|----------------------|
| **Status** | Accepted (direction) |
| **Date**   | 2026-08-09           |

## Decision

Invest in these **text/render assemblers**:

|                  Assembler                  |                     Role                     |
|---------------------------------------------|----------------------------------------------|
| **Velocity**                                | Power users, existing packages, macros       |
| **HTML-first** (new)                        | Simple snippets/pages without Velocity cliff |
| **Markdown** (new)                          | Content-oriented bodies → HTML               |
| **Legacy/XSL**                              | Compatibility only (`legacyAssembler`)       |
| **Binary / Dispatch / Database / Resource** | Specialized; keep                            |

`pageAssembler` is **not** a separate language — it is page context + a text assembler.

## HTML-first placeholder syntax

**Open (must lock before Phase 1 code):** choose one substitution style, e.g.:

1. `${name}` only (from JEXL binding map)
2. Mustache-like `{{name}}`
3. Velocity-compatible `$name` / `${name}` without directives

Prefer the smallest surface that is hard to confuse with customer HTML/JS.

## Consequences

- New extension registrations + tests for HTML-first and Markdown.
- Help and Design “assembler” picker gains entries.
- Simple product widgets prefer HTML-first/Markdown after XML elimination.

