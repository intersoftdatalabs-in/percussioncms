# Erlang review — Virtual Sites Phase 1 (#2679)

| Field | Value |
|-------|--------|
| **Branch** | `feat/virtual-sites-git-docs` |
| **Scope** | Uncommitted Phase 1 Virtual Site package + product-docs + build scripts |
| **Date** | 2026-08-09 |
| **Reviewer** | Erlang (pre-commit, implementer session) |
| **Recommendation** | **approve** |
| **May commit/push** | **yes** |

## Summary

Phase 1 delivers a filesystem Virtual Site SPI, Markdown+frontmatter discovery, static HTML build (Markdown assembler helpers + layout theme), in-memory virtual participants with JSONL flush, link checking, site property helper, `product-docs/` skeleton, and cross-platform build scripts.

Focused tests: **17** (parser, nav, helper, build, link rewriter) green. Module `system` **`mvnw clean install`** **BUILD SUCCESS**. Offline `scripts/build-cms-docs` emits 7 pages with clean link-report.

## Gate checklist

| Gate | Result |
|------|--------|
| Behavioral unit tests for non-trivial logic | Pass — parser, nav, build, helper, link rewrite |
| Cross-platform path/file I/O | Pass — `Path`/`Files`, `resolveHref` segment join, CRLF frontmatter, `.bat`+`.sh` scripts; URL/href paths correctly use `/` |
| Change-class closure | Pass for Phase 1 offline package — no Spring bean scan of new types into shared test contexts; no WebUI/REST surface |
| Secrets / empty catch | Pass |
| New copyright headers | Pass — Intersoft 2026 |

## Issues

None blocking.

### Suggestions (non-blocking)

1. **Site-root absolute hrefs** (`/8.2/...`) require the static site to be served at host root (or a reverse-proxy strip). Fine for help.intsof.com; file:// dogfood of nested pages still depends on absolute CSS. Document in product-docs README if operators host under a subpath later.
2. **Path containment** in `PSGitFilesystemVirtualSiteSource.load` uses `Path.startsWith` after normalize — correct for NIO; keep this pattern if Git/clone adapters add remote fetch later.
3. **JSONL hand-escape** is minimal (quote/backslash only). Adequate for controlled ids/paths; if free-text titles are added to flush later, switch to a real JSON writer.

## Memory patterns hit

- Behavioral tests for new logic (not string presence alone)
- Portable `Path`/`Files` over hardcoded separators
- Both Windows and Unix scripts for required automation
- Module standalone clean install before PR

## Cross-platform path checklist

- [x] No new filesystem path construction with hardcoded `/` or `\\` joins (hrefs intentionally use `/`)
- [x] Path resolve / Files APIs for discovery, write, asset copy
- [x] Tests assert portable Path equality / forward-slash normalized published paths
- [x] Line endings normalized in frontmatter parser
- [x] build-cms-docs has `.bat` and `.sh`

## Verification evidence

```text
cd system && ../mvnw.cmd -Dtest=VirtualFrontmatterParserTest,VirtualNavBuilderTest,PSVirtualSiteHelperTest,PSVirtualSiteBuildServiceTest,VirtualMarkdownLinkRewriterTest test
# Tests run: 17, Failures: 0

cd system && ../mvnw.cmd clean install
# BUILD SUCCESS

scripts\build-cms-docs.bat
# Built 7 page(s); link-report: OK
```
