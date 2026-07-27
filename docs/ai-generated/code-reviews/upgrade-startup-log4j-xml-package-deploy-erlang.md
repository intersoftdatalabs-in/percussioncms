# Erlang-style review: fix/upgrade-startup-log4j-xml-package-deploy

**Date:** 2026-07-27
**Scope:** Jetty Log4j webapp visibility; XML catalog/secure factory hygiene; Betwixt package-deploy (keyword null root, slot association element names).

## Hard gates

| Gate | Result |
|------|--------|
| Behavioral bugs in new logic | Pass after install retest (server starts; packages install) |
| Unit tests for non-trivial logic | Present: PSXmlSerializationHelperTest, PSCatalogResolverTest, PSSecureXMLUtilsCallSiteOptionsTest, PSTemplateSlotXmlRestoreTest, StartupWarnHygieneTest |
| Cross-platform paths | Pass — no new OS-hardcoded path construction; rewrite uses string element names only |

## Findings

- **None blocking.** Legacy Betwixt/package contracts are intentionally preserved (root `<null>`, unhyphenated `contenttypeid`).
- Slot deploy skips zero-id associations with warn (defense if deserialization still fails).
- Catalog DOCTYPE removal avoids TR9401 fallback AIOOBE under secured SAX.
- Log4j: `addProtectedClasses` (not hidden) so WEB-INF-excluded log4j-iostreams remains visible (`IoBuilder`).

## Residual risk

- Full Betwixt→Jackson remains #505; this PR is tactical compat only.
- Other design-object XML using unhyphenated names outside slots may need similar normalizers if found.
