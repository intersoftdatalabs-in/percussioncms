# Erlang review — #3432 Action request + PublishResponse / Status wire getters

**Branch:** `fix/issue-3432-action-publish-status-wire-getters`  
**Base:** `origin/main`  
**Date:** 2026-08-15  
**Reviewer:** Erlang (independent of implementer)

## Summary

Convert Explorer action-menu request DTOs plus rest `PublishResponse` / `Status` from `Optional` wire getters to plain nullable getters so toolbar find/types bodies and publish/status JSON emit arrays/scalars, not Optional beans. Callers that used `.orElse()` on converted getters were updated. Production-mapper family tests appended to `JacksonContextResolverOptionalTest`.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No bugs, no missing behavioral tests for changed logic, no non-portable path/file I/O.

## Scope

Uncommitted rest DTO/resource/test changes vs `HEAD` / `origin/main`. `rest/AGENTS.md` not in the change set (human rule review). `Guid` and sitemanage-internal `PSSitePublishResponse` unchanged.

## Change-class closure

Change class: REST wire-getter conversion (peer of User/Role/Acl / #3388 slices).

- Production DTOs: `@JsonInclude(NON_NULL)` + plain getters
- Resource caller: `ActionMenuResource.getAllowedContentTypeMenus` null-safe `int[]`
- Rest unit tests: `FoldersTest`, `PagesTest`, `ActionMenuResourceTest`, `JacksonContextResolverOptionalTest`
- Reverse-dep: sitemanage compiles against new signatures (EditionAdaptor / Status setters only)
- Product-docs: N/A (no documented Optional-bean JSON examples)
- Playwright: N/A (no WebUI screen change)
- `rest/AGENTS.md`: not committed

## Cross-platform path checklist

N/A — no filesystem path/file I/O.

## Issues

None.

## Memory patterns hit

- Missing behavioral unit tests for new/changed non-trivial logic — covered (resource null `contentIds` + mapper round-trip).
- Incomplete change-class closure — companions from prior #3388 slices present.
- Agent rule files not committed without human review.

## Build evidence

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 467, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1238, Failures: 0, Skipped: 125
