# Erlang review — issue #4435 (product-docs dual-run upgrade path)

| Field | Value |
|-------|--------|
| **Issue** | #4435 (parent #2632, grandparent #2626) |
| **Change class** | Product-docs operator cookbook (docs-only) |
| **Reviewer persona** | Erlang (independent of implementer) |
| **Verdict** | Pass — no bugs, no missing behavioral tests for production logic, no non-portable path I/O in product code |

## Scope

New `product-docs/8.2/admin/definition-xml-dual-run.md` (`id: admin-definition-xml-dual-run`)
plus links from Upgrade Overview, Getting Started, page-packages, glossary, Design
templates, XSL support, and extensions. No Java/UI/Playwright. Did **not** edit
hot-path `admin/index.md`, `developer/index.md`, or `developer/rest.md` (open PR #4444).

## Hard gates

| Gate | Result |
|------|--------|
| Bugs | None. Dual-run vs dual-ship distinguished; modern-first / XML fallback / neither-error match `PSLegacyDefinitionXmlShim` policy. |
| Shim deletion | **Not present.** Page forbids deleting or hard-disabling the shim; does not claim M2/M3 PASS or authorize #2852. |
| Assemblers / XSL cookbook | **Not expanded.** Points at existing `id:admin-xsl-legacy-assembler` only. |
| Secrets / customer inventories | **Not copied.** Instructs operators to inventory their own host; points at engineering compiler/checklist paths without dumping product widget lists. |
| Frontmatter `id` | New unique `admin-definition-xml-dual-run`. Existing ids unchanged. |
| Cross-platform paths | Compiler examples use `Path`-style package dirs; Windows `.bat` and Unix `.sh` wrappers; `File.pathSeparator` explained (`;` vs `:`) without hardcoding install roots like `C:\Percussion` or `/tmp`. |
| Product-docs companions | Upgrade + page-packages links required by the issue; extra Related links are same change class. |
| Tests | N/A — no production logic. Docs smoke is the verification (`scripts/ci-smoke-product-docs.*`). |

## Residual

None for this slice. Shim removal remains #2852 (blocked). Do not queue it from this PR.
