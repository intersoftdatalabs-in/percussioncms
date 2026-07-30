# Erlang Review: fix/fts-index-search-rows

**Date:** 2026-07-28  
**Scope:** Uncommitted FTS table-stakes fixes vs origin/development  
**Recommendation:** approve  
**Gate:** May commit/push: yes

## Summary

FTS/Home search table-stakes recovery after #1568:

1. **PSFolderHelper** — call `IPSPublisherService.findLastPublishedItemStatus` without casting JDK proxy to concrete `PSPublisherService` (fixed empty search/recent rows).
2. **PSExtractHtmlContent** — soft-fail extract so FTS queue still indexes title/metadata when assembly fails.
3. **PSRenderService.renderPageForSearchIndex** — programmatic REQUIRES_NEW TX; detect rollback-only instead of UnexpectedRollbackException.
4. **PSSaxParserFactoryImpl** — never cache null factory (Digester/Velocity tools hardening).
5. **PSRecentService** — clearer isolation/logging for failed property lookups.

## Issues

None blocking.

### Nits / follow-ups

- Full HTML body extract during FTS still fails under dual JDBC/ORM (connection null on assembly) — tracked as #1561 residual; soft-fail preserves title indexing.
- tools.xml Digester still logs null-config then programmatic fallback; acceptable.

## Cross-platform path checklist

N/A — no new filesystem path I/O in production code. SAX factory clears/restores a system property only.

## Tests

- `PSSaxParserFactoryImplTest` (4)
- `PSExtractHtmlContentTest` (5)
- `PSRenderServiceSearchIndexTest` (4)
- modules/utils clean install: Tests run 237, Failures 0
- projects/sitemanage clean install: BUILD SUCCESS (new tests included)

## Memory patterns hit

- Spring JDK proxy cast to concrete class (search/recent rows empty)
- Soft-fail non-critical extractors so parent pipeline continues

