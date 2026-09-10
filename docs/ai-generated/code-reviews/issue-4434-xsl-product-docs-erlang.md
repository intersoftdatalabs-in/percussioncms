# Erlang review — #4434 product-docs XSL support + migration cookbook

| Field | Value |
|-------|--------|
| **Date** | 2026-09-09 |
| **Branch** | `fix/issue-4434-xsl-product-docs` |
| **Base** | `origin/main` |
| **Reviewer** | Erlang (independent of implementer) |
| **Recommendation** | **approve** |
| **May commit/push** | **yes** |
| **Gate** | pass |

## Summary

Docs-only operator increment for parent #2632 slice 5: 8.2 XSL / `legacyAssembler`
support statement plus a short migration cookbook. New stable frontmatter
`id: admin-xsl-legacy-assembler`. Existing `id: admin-design-templates` is
unchanged. `id:` links from Design templates, Developer Templates, admin index,
getting-started, upgrade, developer index, and glossary. Engineering cookbook
facts are ported for operators; `docs/ai-generated` tree is not dumped.
`scripts/ci-smoke-product-docs.bat` built **49** pages and emitted
`tmp/product-docs-site/8.2/index.html`. Dollar-brace examples use HTML entities
so Virtual Site HTML-first layout does not eat them.

## Scope

- `product-docs/8.2/admin/xsl-legacy-assembler.md` (new)
- `product-docs/8.2/admin/design-templates.md`
- `product-docs/8.2/admin/developer-templates.md`
- `product-docs/8.2/admin/index.md`
- `product-docs/8.2/getting-started/index.md`
- `product-docs/8.2/getting-started/upgrade.md`
- `product-docs/8.2/developer/index.md`
- `product-docs/8.2/reference/glossary.md`
- Uncommitted vs HEAD; no Maven / WebUI / REST / Playwright in this slice
- Hot-path files (`rest.md`, WebUI messages/paths/DeveloperShell, `sitemanage-beans.xml`,
  `developer-smoke-set.js`) **not** edited
- Memory patterns: product-docs companion; no file I/O in product code
- Cross-platform path review: **N/A** (Markdown content only; smoke uses existing
  `scripts/ci-smoke-product-docs.bat` + `.sh`)

## Issues

None (bug / missing tests / non-portable I/O).

### nit

- Markdown pipe tables remain as CommonMark source in assembled HTML (pre-existing
  Virtual Site Markdown table rendering). Not introduced uniquely by this slice.

## Product-docs / tests / C5

- Product documentation **is** the change.
- Unit tests N/A (no production logic).
- Playwright / C5 N/A (no WebUI).
- Maven C1 N/A (docs-only; no Maven module sources changed).
