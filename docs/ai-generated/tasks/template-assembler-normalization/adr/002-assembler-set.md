# ADR-002: Assembler set for 8.2+

| Field | Value |
|-------|--------|
| **Status** | Accepted (direction) |
| **Date** | 2026-08-09 |

## Decision

Invest in these **text/render assemblers**:

| Assembler | Role |
|-----------|------|
| **Velocity** | Power users, existing packages, macros |
| **HTML-first** (new) | Simple snippets/pages without Velocity cliff |
| **Markdown** (new) | Content-oriented bodies → HTML |
| **Legacy/XSL** | Compatibility only (`legacyAssembler`) |
| **Binary / Dispatch / Database / Resource** | Specialized; keep |

`pageAssembler` is **not** a separate language — it is page context + a text assembler.

## HTML-first / Markdown placeholder syntax — **LOCKED (Phase 1)**

**Choice: `${dotted.path}` only** (implemented by `PSBindingPlaceholderRenderer`).

| Rule | Detail |
|------|--------|
| Form | `${title}`, `${sys.mimetype}` |
| Not supported | Bare `$title` (avoids HTML/JS false positives), Mustache `{{ }}`, Velocity directives |
| Lookup | Binding key `title` or `$title`; nested maps via `sys` / `$sys` then child keys |
| Missing | Empty string |
| Implementation | `com.percussion.services.assembly.impl.plugin.PSBindingPlaceholderRenderer` |

Rationale: smallest surface, hard to confuse with customer scripts, easy to document.

## Consequences

- New extension registrations + tests for HTML-first and Markdown.
- Help and Design “assembler” picker gains entries.
- Simple product widgets prefer HTML-first/Markdown after XML elimination.
