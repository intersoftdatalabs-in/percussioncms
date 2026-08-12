# Erlang review: #3196 PathItem IllegalAnnotationExceptions

**Branch:** `fix/issue-3196-pathitem-illegal-annotation`  
**Scope:** uncommitted vs HEAD (sitemanage PathItem JAXB, WebUI error unwrap, Playwright, product-docs)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** JAXB vs Jackson dual annotations; Explorer tree must not blank-fail; change-class companions (unit + Playwright + product-docs)

## Summary

Explorer left-panel `GET /pathmanagement/path/folder/` returned HTTP 500 with
`1 counts of IllegalAnnotationExceptions`. Reproduced via
`JAXBContext.newInstance(PSPathItem.class)`:

> Transient field "relatedObject" cannot have any JAXB annotations.

Glassfish JAXB forbids `@XmlTransient` on a Java `transient` field. The field
must remain `transient` (runtime `File` / non-serializable association). Removing
the JAXB annotation and `@JsonIgnore` on accessors is the correct pairing.

No new public method signatures, no `final`/`sealed` on the DTO. Path I/O
checklist N/A (no filesystem path construction).

## Issues

None (hard gate).

## Notes

- `PSPathItemListJacksonTest` now exercises production `JacksonContextResolver`
  ObjectMapper and JAXBContext (not annotation reflection only).
- UI: `extractRestErrorMessage` unwraps `Errors.globalError` and
  `PathItem:[{Errors}]`; `findChildren` throws on error-shaped PathItem so the
  tree shows `explorer-tree-error` instead of empty.
- Playwright: `tests/bugs/bug-3196-pathitem-left-panel.spec.js`.
- Product docs: left-tree troubleshooting on `admin-content-explorer`.
