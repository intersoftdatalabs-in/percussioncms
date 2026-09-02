# Erlang review — #4164 Developer Sites sitemap-xml Build chrome

**Branch:** `feat/issue-4164-sitemap-xml-build-chrome`

Independent pre-commit review of Developer Sites **Build Virtual Site** chrome for `sourceKind=sitemap-xml` (parent #2678).

## Change class

Developer Sites product chrome: enable **Build Virtual Site** for `sourceKind=sitemap-xml` after save (same last-build REST as cluster REST Build on `main`). No REST persist/factory/preview/publish reimplementation. Preview/Publish chrome stay hidden. Repository still hides Virtual Build chrome.

## Companions

| Artifact | Present |
|----------|---------|
| `shouldShowVirtualBuildChrome` includes `sitemap-xml` | yes |
| Preview/Publish predicates stay false | yes |
| i18n hint (`SITE_VIRT_SITEMAP_XML_HINT`) save-then-Build | yes |
| Vitest `virtualSiteBuild` + panel load/save/Build success | yes |
| Playwright `developer-site-virtual-source.spec.js` intercept + live H2 Build | yes |
| Local sitemap.xml QA fixture + docker `cp` helper | yes |
| product-docs 8.2 admin Sites (+ peer developer/reference) | yes |
| ActionMenu / docker dual-ship / REST adaptor files untouched | yes |

## Cross-platform

- Fixture helper uses `path.join` on the host and POSIX `/opt/Percussion/tmp/...` only as the Linux QA cell path (peer of icalendar/rss-atom).
- No new OS separator concatenation in production TS.

## Hard-gate result

No bug, missing behavioral tests, or non-portable path I/O found. Safe to commit pending module `clean install` + C5 Playwright.
