# Erlang review — issue #3996 REST CD-10 content type search indexing

**Change class:** New public REST adaptor surface (GET/PUT type-level search indexing).

**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class completeness (rest resource + DTO + IXxxAdaptor + Spring stub + sitemanage impl + Mockito + adaptor tests + product-docs); rest MainTest Spring stub; held design-session lock without steal.

## Summary

Dedicated Admin PUT (and unauthenticated-for-lock GET) at `/services/contenttypes/{idOrName}/searchIndexing` persists the Workbench Properties “Enable searching for this Content Type” flag as the root mapper field-set `isUserSearchable` via existing `IPSContentDesignWs`. Peer is CD-13 PUT `.../enabled`. Distinct from per-field `searchable`. No SPA chrome.

## Issues

None that block.

## Companions

| Artifact | Present |
|----------|---------|
| rest resource + OpenAPI | yes |
| wire DTO `ContentTypeSearchIndexing` | yes |
| `IContentTypesAdaptor` methods | yes |
| Spring `TestContentTypeAdaptor` + `ContentTypesTestAdaptor` | yes |
| sitemanage `ContentTypeAdaptor` | yes |
| Mockito resource tests | yes |
| adaptor success/403/404/409 tests | yes |
| product-docs 8.2 + gap map CD-10 | yes |
| Playwright / SPA | N/A (explicitly out of scope) |

## Cross-platform path checklist

N/A — no filesystem path/file I/O.

## Tests / build

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 767, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1798, Failures: 0, Skipped: 125
- Downstream: sitemanage implements `IContentTypesAdaptor`; grep found three implementors (production + two rest test stubs), all updated.

## Notes

- GET does not require Admin (same as GET itemExits / GET detail). PUT is Admin-only (403). Issue text said “Admin GET/PUT”; 403 is specified with the PUT error set.
- PUT does not acquire or steal the design lock; 409 when unlocked or locked by another user.
- Default-on when the mapper field-set is null (Workbench default).
