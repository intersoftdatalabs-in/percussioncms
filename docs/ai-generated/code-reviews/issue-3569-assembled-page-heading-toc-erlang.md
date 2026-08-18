# Erlang review — issue #3569 assembled-page heading TOC

- **Branch:** `feat/issue-3569-assembled-page-heading-toc`
- **Scope:** uncommitted vs `HEAD` (Virtual Site heading TOC + product-docs theme)
- **Reviewer:** Erlang (independent of implementer)
- **Date:** 2026-08-18
- **Memory patterns hit:** behavioral unit tests; change-class closure; portable Path/Files; XSS/escape of generated HTML; product-docs companion

## Summary

Adds h2–h3 TOC generation from assembled Markdown HTML (`PSVirtualSiteHeadingToc`), binds `${toc}` in `PSVirtualSiteLayoutRenderer` (theme + built-in default layout), annotates headings with stable fragment ids, and documents the placeholder under `product-docs/`. Tests cover empty/safe, nesting, id stability/collision, unsafe-id replacement, HTML escape of labels, renderer bind, and the sample-docs build fixture.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No hard-gate bugs found. Change-class companions are present (helper + renderer bind + theme + CSS + product-docs + unit/integration tests). No new filesystem path construction. C2 (API shape) does not apply: existing `render(...)` signature is unchanged; new type is additive.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Tests use `Path` / `Files` / `@TempDir`
- [x] No Unix-only absolute path assertions
- [x] N/A: no new scripts, temp roots, or path-separator lists

## Issues

None (hard-gate).

### Low / nits (non-blocking)

- `PSVirtualSiteHeadingToc` calls package-private `PSVirtualSiteLayoutRenderer.htmlEscape`. Same package; acceptable. A shared escape helper would be slightly cleaner if a third caller appears.
- Default-layout empty-TOC path is covered via theme placeholder test + helper empty tests, not a dedicated default-layout assertion. Sufficient.

## Tests exercised (focused)

`PSVirtualSiteHeadingTocTest` (9), `PSVirtualSiteLayoutRendererTest` (4), `PSVirtualSiteBuildServiceTest` (8) — 21/0/0 before module clean install.

## Re-review

Product-docs prose that used literal `${toc}` / `${content}` was emptied by `PSTextAssemblerSupport.renderMarkdown(..., Map.of())`. Tokens in Markdown are now written as `&#36;{name}` so assembled help still shows the placeholder names. Theme HTML still uses real `${toc}`. Not a code bug; docs-only fix. Gate unchanged.
