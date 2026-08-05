# Erlang review: issue #1693 ContentType list DTO serialization

**Branch:** `fix/issue-1693-contenttype-list-dto`  
**Scope:** `rest` module only (ContentType wire DTO + unit/resource tests)  
**Date:** 2026-08-04  
**Reviewer persona:** Erlang (independent of implementer)

## Summary

`GET /contenttypes` list items were observed live as hideFromMenu-only. Root cause: `ContentType` used `Optional` getters with `@JsonInclude(NON_NULL)`; without reliable Optional unwrapping on the wire path, name/label/guid were dropped. Production mapping (`ApiUtils.convertContentType`) already sets those fields.

**Fix:** Align `ContentType` getters with `ContentTypeDetail` (plain `String` / `Guid` return types). Extend unit and Mockito resource tests to assert list JSON is not hideFromMenu-only. Spring test stub `TestContentTypeAdaptor` returns populated samples for MainTest context scan.

## Recommendation

**approve**

## Gate

|                  Check                  |                                             Result                                              |
|-----------------------------------------|-------------------------------------------------------------------------------------------------|
| Bugs                                    | none found                                                                                      |
| Behavioral unit tests for changed logic | yes (Jackson serialisation + resource list + adaptor stub)                                      |
| Cross-platform path / file I/O          | not touched                                                                                     |
| Change-class companions                 | rest resource/DTO/test stubs; sitemanage adaptor mapping already correct — no sitemanage change |
| New dependencies                        | none                                                                                            |
| May commit/push                         | **yes**                                                                                         |

## Issues

None.

### Nits (non-blocking)

- `TemplateSummary` still uses Optional getters; covered by existing Jackson test and Jackson 3 unwrapping. Out of scope for this slice unless similar live symptoms appear.
- Nested `Guid` still uses Optional for some string fields; ContentTypeDetail already depends on the same Guid type successfully for detail views.

## Tests run

```text
cd rest && ../mvnw clean install
# focused: JacksonContextResolverOptionalTest, ContentTypesTest, ContentTypesResourceDetailTest — pass
```

## Residual (not this PR)

- Playwright developer-catalog-smoke against live CMS after redeploy remains under parent #1690 / #1695 (and redeploy #1692).

