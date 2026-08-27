# Erlang review — #3895 Developer Content Type item-level exits chrome (CD-09)

**Date:** 2026-08-27  
**Branch:** `feat/issue-3895-ct-item-exits-chrome`  
**Scope:** WebUI SPA item-exits chrome, REST JSON bind companion, Vitest, Playwright, product-docs  
**Memory patterns hit:** change-class closure (Vitest + Playwright); REST wrap consume (peer CD-12 JsonReader / InputStream bind); omit pipe pre/post on PUT (REST omit = unchanged).

## Summary

Developer Content Type detail shows item-level input/output translations, validations, and pipe pre/post exits after a held design lock. Save uses dedicated `PUT /contenttypes/{id}/itemExits`. Unlocked editors stay disabled; 409 lock is not stolen.

Live H2: CXF UNWRAP_ROOT_VALUE delivered an empty DTO (400 required lists) for the documented wrap. Companion: `ContentTypeItemExitsJsonReader` plus `replaceItemExitsFromJson(InputStream)` so wrap/flat JSON binds. PUT omits unchanged `preExits`/`postExits` (pipe `setInputDataExtensions` is UOE on percPage).

## Recommendation

**approve with residual** — chrome, lock/409, docs, and unit tests are in place. Live `saveContentTypes` after applying reconstructed input translations still returns `PSErrorsException` (HTTP 500) on sample types; persist Playwright remains fail-closed.

## Gate

**May commit/push: yes** (Partial; persist residual)

## Issues

### Residual (not blocking this chrome PR)

Live PUT after lock+add `sys_ToUpperCase` on percPage / percFileAsset / percRawHtmlAsset: `ContentTypeAdaptor.replaceItemExits` → `PSContentDesignWs.saveContentTypes` `PSErrorsException` (SAVE_FAILED). Not a chrome bind bug (body lists are populated). Follow-up: surface `PSErrorsException` errors and fix apply/save of reconstructed extension calls.

### Companions (checked)

| Artifact | Present |
|----------|---------|
| WebUI unwrap/wrap + omit-pipe PUT | yes |
| ContentTypeDetailPanel lock → PUT | yes |
| Vitest API + helpers + panel | yes |
| REST JsonReader + InputStream PUT | yes |
| Playwright 409 / unlocked | yes (H2) |
| Playwright persist GET | fail-closed on REST save 500 |
| product-docs 8.2 admin + REST | yes |

### Notes

- Cross-platform path checklist: N/A (REST URL `/` only).
- Do not steal Object ACL catalog (#3810/#3706).
