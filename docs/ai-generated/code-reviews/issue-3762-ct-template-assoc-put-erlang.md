# Erlang review — #3762 Content Type template associations PUT REST (CD-12)

**Date:** 2026-08-23  
**Branch:** `feat/issue-3762-ct-template-assoc-put`  
**Scope:** uncommitted vs `origin/main` (rest, sitemanage, product-docs)  
**Memory patterns hit:** change-class closure (rest resource + adaptor interface + DTO + Mockito + Spring stub + sitemanage impl/tests); rest `MainTest` stub for new interface methods; no path I/O.

## Summary

Dedicated `GET`/`PUT /services/contenttypes/{idOrName}/allowedTemplates` replaces allowed-template associations under a **held** design lock (`IPSSystemDesignWs.isLocked` + `IPSContentDesignWs.saveAssociatedTemplates(..., release=false)`). Invalid template refs are 400; missing/foreign lock is 409. GET of the same path (and existing detail GET) lists the set.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Issues

None blocking.

### Companions (checked)

| Artifact | Present |
|----------|---------|
| rest resource + OpenAPI | yes |
| DTO (`NamedObjectRefList`) + `ContentTypeDesignLockException` | yes |
| `IContentTypesAdaptor` methods | yes |
| Mockito `ContentTypesResourceDetailTest` (200/400/404/409) | yes (17 tests, Failures: 0) |
| Spring `TestContentTypeAdaptor` + `ContentTypesTestAdaptor` | yes |
| sitemanage `ContentTypeAdaptor` + `ContentTypeAdaptorAllowedTemplatesTest` | yes (11 tests, Failures: 0) |
| product-docs `8.2/developer/rest.md` | yes |
| Standalone `rest` + `sitemanage` clean install | BUILD SUCCESS |

### Notes (non-blocking)

- Existing `PUT /contenttypes/{idOrName}` still auto lock-save-unlock including optional `allowedTemplates`. This slice is the held-lock sub-resource; peer #3743 will change whole-object PUT lock semantics. Two write paths until that merges — documented.
- Lock check is user-based (`isLockedBy`); session mismatch is deferred to `saveAssociatedTemplates` and mapped via “not locked” error text to 409.
- Cross-platform path checklist: N/A (no filesystem I/O).
