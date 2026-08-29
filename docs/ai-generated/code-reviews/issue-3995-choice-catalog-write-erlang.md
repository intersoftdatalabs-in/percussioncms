# Erlang review — issue #3995 REST CD-07 remaining choice catalog write

| Field | Value |
|-------|-------|
| **Date** | 2026-08-29 |
| **Branch** | `fix/issue-3995-choice-catalog-write` |
| **Base** | `origin/main` |
| **Recommendation** | approve |
| **Gate** | pass |
| **May commit/push** | yes |

## Summary

Parent #1690 / CD-07 remainder after #3786. Existing GET/PUT
`/services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties` already
replaces property name/value and the choice catalog. This slice writes **choice
filter**, **null-entry**, and **default-selected** on that same catalog object.

Persist path is unchanged: held design-session lock + `IPSContentDesignWs`
`loadContentTypes` / `saveContentTypes`. Omitted `choices` still leaves the
catalog untouched; `type: none` still clears. No SPA Choices tab. Shared-field
control properties remain #3984.

**Memory patterns hit:** change-class closure (rest DTO + resource + Mockito +
Spring stub + sitemanage adaptor tests + product-docs); no new adaptor methods
so existing `TestContentTypeAdaptor` still satisfies the interface.

## Change-class companions

| Companion | Status |
|-----------|--------|
| rest DTOs / OpenAPI | New `filter` / `nullEntry` / `defaultSelected` on `ContentTypeChoiceCatalog` |
| Adaptor mapping | `toChoiceCatalog` / `fromChoiceCatalog` round-trip; validation → 400 |
| Mockito resource | GET extras, PUT extras, invalid filter 400 |
| Spring test stub | Same interface; stub GET now returns extras |
| sitemanage adaptor tests | Persist, omit, type none, 403/409/404 unchanged |
| product-docs 8.2 | `developer/rest.md` + admin content-types note (SPA still omits `choices`) |
| Gap map | CD-07 remaining write marked shipped |

## Cross-platform path checklist

N/A — no filesystem path/file I/O.

## Issues

None blocking.

### Suggestion (not a gate)

`PSChoiceFilter` lookup is mapped as `lookupHref` / `lookupName` only, matching
existing catalog lookup mapping. Converter UDFs and query parameters on the
filter URL are not round-tripped. Out of scope for this slice.

## Tests / builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 759, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1717, Failures: 0, Skipped: 125
- Downstream: `projects/sitemanage` (implements `IContentTypesAdaptor`). Additive DTO fields; no `final`/signature break.

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
