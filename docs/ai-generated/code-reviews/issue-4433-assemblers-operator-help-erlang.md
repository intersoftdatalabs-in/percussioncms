# Erlang review — #4433 product-docs assemblers operator help

| Field | Value |
|-------|--------|
| **Date** | 2026-09-09 |
| **Branch** | `feat/issue-4433-assemblers-operator-help` |
| **Base** | `origin/main` |
| **Reviewer** | Erlang (independent of implementer) |
| **Recommendation** | **approve** |
| **May commit/push** | **yes** |
| **Gate** | pass |

## Summary

Docs-only operator help for Design / Developer Templates assembler choice
(HTML-first default, Markdown, Velocity; Legacy/XSL compatibility only). Stable
frontmatter `id: admin-design-templates` is unchanged. No pipeline hot-path
files. `scripts/ci-smoke-product-docs.sh` built 48 pages and emitted
`tmp/product-docs-site/8.2/index.html`. Dollar-brace examples use HTML entities
so Virtual Site HTML-first layout does not eat them.

## Scope

- `product-docs/8.2/admin/design-templates.md`
- `product-docs/8.2/admin/developer-templates.md`
- `product-docs/8.2/getting-started/index.md`
- `product-docs/8.2/reference/glossary.md`
- Uncommitted vs HEAD; no Maven / WebUI / REST / Playwright in this slice
- Memory patterns: product-docs companion; no file I/O in product code
- Cross-platform path review: **N/A** (Markdown content only; smoke uses existing `scripts/ci-smoke-product-docs.sh` + `.bat`)

## Issues

None (bug / missing tests / non-portable I/O).

### nit

- Markdown pipe tables remain as CommonMark source in assembled HTML (pre-existing
  Virtual Site Markdown table rendering). Not introduced uniquely by this slice.

## Product-docs / tests / C5

- Product documentation **is** the change.
- Unit tests N/A (no production logic).
- Playwright / C5 N/A (no WebUI).
