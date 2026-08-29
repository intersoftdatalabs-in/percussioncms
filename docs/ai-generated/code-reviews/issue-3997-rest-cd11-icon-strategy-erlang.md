# Erlang review — issue #3997 REST CD-11 content type icon strategy

| Field | Value |
|-------|-------|
| **Date** | 2026-08-29 |
| **Branch** | `fix/issue-3997-content-type-icon-strategy` |
| **Issue** | #3997 (parent #1690, FR CD-11) |
| **Recommendation** | approve |
| **Gate** | May commit/push: yes |
| **Memory patterns hit** | Incomplete change-class closure (rest↔sitemanage + Spring stub); behavioral tests for validation/403/409/404; C2 interface implementors |

## Summary

Admin REST GET/PUT `/services/contenttypes/{idOrName}/icon` for Workbench Properties icon strategy (`none` / `specified` / `fromFileField`). PUT requires a held design-session lock (does not steal). `none` clears value; non-none blank value is 400. Persist via existing `IPSContentDesignWs.saveContentTypes` + `PSContentEditor.setContentTypeIcon`. No new SOAP, no SPA, no binary upload.

Change-class companions from CD-13 / CD-09 peers are present: wire DTO, resource, adaptor interface, Mockito resource tests, Spring `TestContentTypeAdaptor` stub + `ContentTypesTestAdaptor`, sitemanage apibridge impl + unit tests, product-docs 8.2 Developer REST + admin REST table, gap map CD-11.

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None that block.

## Cross-platform path checklist

N/A for filesystem I/O. Icon `value` is a persisted object-store string (file path/name or field name). Tests assert those strings; they do not join OS paths.

## C2 / change-class

- `IContentTypesAdaptor` gained methods (public API). Grep `implements IContentTypesAdaptor`: 3 types (`ContentTypeAdaptor`, `TestContentTypeAdaptor`, `ContentTypesTestAdaptor`); all updated. No anonymous subclasses.
- Downstream: standalone `projects/sitemanage` clean install after `rest` install.
- Rest `MainTest` Spring stub implemented.

## Tests / builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 769, Failures: 0 (`ContentTypesResourceDetailTest` 104)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1799, Failures: 0, Skipped: 125 (`ContentTypeAdaptorIconTest` 15)

## Product documentation

Updated `product-docs/8.2/developer/rest.md` (table + CD-11 section) and `product-docs/8.2/admin/developer-content-types.md` REST table. Gap map `docs/ai-generated/tasks/developer-module-p0/content-type-api-gaps.md` marks CD-11 REST shipped.

C5 Playwright N/A (no WebUI / SPA).

## Re-review (PR #4007 Kilo lock-prefix)

Kilo thread: no-arg `requireHeldLock(ctGuid)` used generic `"Could not save content type"` for CD-11 PUT. Fix passes `"Could not set content type icon"` so 409 text matches other `lockConflict` calls. Tests assert the prefix on lock-not-held and locked-by-other. Standalone `projects/sitemanage` clean install after this-branch rest SNAPSHOT: BUILD SUCCESS, Tests run: 1799, Failures: 0, Skipped: 125 (`ContentTypeAdaptorIconTest` 15). Recommendation: approve. May commit/push: **yes**.
