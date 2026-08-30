# Erlang review — #3964 REST CD-16 system def field create/delete

| Field | Value |
|-------|--------|
| **Date** | 2026-08-28 |
| **Branch** | `feat/issue-3964-systemdef-field-create-delete` |
| **Scope** | Uncommitted vs `HEAD` / `origin/main` |
| **Recommendation** | **approve** |
| **Gate** | **May commit/push: yes** |
| **Memory patterns hit** | Change-class closure (rest resource + adaptor + Spring stub + sitemanage impl); Admin 403 not a global JAX-RS filter; lock mapped to typed 409 not message-sniffing |

## Summary

Admin POST `/services/systemdef/fields` and DELETE `/services/systemdef/fields/{fieldName}` create and delete persistable TYPE_SYSTEM fields (backend column + `sys_EditBox` display mapping) via `IPSContentDesignWs.loadContentEditorSystemDef(lock=true)` then `saveContentEditorSystemDef(release=true)`, matching CD-15 `#3954` nested fields. PUT remains patch-only. No new SOAP; no SPA; control/stylesheet/flow still `designGaps`.

## Issues

None that block.

## Notes (non-blocking)

- Unknown DELETE field is **400** (singleton catalog; same as PUT unknown field), not 404.
- System-mandatory / system-internal fields cannot be deleted (**400**).
- Duplicate create is **409** (`WebApplicationException`).
- Cross-platform path checklist: N/A (no file I/O). Name validation rejects `/`, `\\`, `..`, NUL as invalid (**400**).

## Tests / companions

- Mockito `SystemDefResourceTest` (POST/DELETE 400/403/409/204/500)
- Spring `TestSystemDefAdaptor` implements `addField` / `deleteField`
- `SystemDefAdaptorTest` success / 403 / lock 409 / duplicate 409 / unknown 400 / mandatory 400 / persistable XML
- product-docs 8.2 Developer REST + admin content-types + gap map CD-16 field create/delete

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 767, Failures: 0 (`SystemDefResourceTest` 21)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1798, Failures: 0, Skipped: 125 (`SystemDefAdaptorTest` 32)
- Downstream: grepped `implements ISystemDefAdaptor` (stub + adaptor); sitemanage standalone install
