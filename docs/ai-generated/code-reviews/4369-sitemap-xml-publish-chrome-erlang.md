# Erlang review — #4369 sitemap-xml Playwright Publish chrome

**Branch:** `fix/issue-4369-sitemap-xml-publish-chrome`  
**Base:** `origin/main`  
**Date:** 2026-09-06  
**Reviewer:** Erlang (pre-commit)

## Summary

Cycle Verify residual: `#4188` Playwright test `sitemap-xml live rebuild after in-cell sitemap.xml edit without Jetty restart` asserted `[data-testid="developer-site-virtual-publish"]` `toHaveCount(0)` immediately after `selectOption("sitemap-xml")` and again after the second Build. Product `shouldShowVirtualPublishChrome` already returns true for `sitemap-xml` (#4166). Later tests in the same spec expect Publish chrome. This change aligns those two asserts with #4166 (`toBeVisible`) and keeps the post-`repository` `toHaveCount(0)` reset.

## Scope

- Uncommitted + branch vs `origin/main`
- File: `modules/perc-qa-automation/frontend/tests/developer-site-virtual-source.spec.js`
- Memory patterns hit: none specific (stale Playwright chrome vs product helper)
- Cross-platform path review: no new path/file I/O; rebuild helpers (`deploySitemapXmlVirtualFixtureToQaCell`, `copySitemapXmlRebuildIntoQaCell`) unchanged

## Recommendation

approve

## Gate

- Bugs: none
- Missing behavioral tests: n/a (the spec is the behavioral test)
- Non-portable path/file I/O: none in this diff

**May commit/push:** yes

## Issues

None.

## Notes

- Product UI is unchanged; do not hide Publish chrome for sitemap-xml.
- Rebuild acceptance (docker-cp fixture, pagesWritten increase, marker swap) is preserved.
- Product-docs: N/A (Playwright-only; no operator-facing product change).
