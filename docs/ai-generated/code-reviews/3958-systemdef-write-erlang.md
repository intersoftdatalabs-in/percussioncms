# Erlang review — #3958 REST CD-16 system def write

| Field | Value |
|-------|--------|
| **Date** | 2026-08-28 |
| **Branch** | `feat/issue-3958-systemdef-write` |
| **Scope** | Uncommitted vs `HEAD` / `origin/main` |
| **Recommendation** | **approve** |
| **Gate** | **May commit/push: yes** |
| **Memory patterns hit** | Change-class closure (rest resource + adaptor + Spring stub + sitemanage impl); Admin 403 not a global JAX-RS filter; lock mapped to typed 409 not message-sniffing |

## Summary

Admin PUT `/services/systemdef` patches existing system-field properties (`searchable`, occurrence/required) via `IPSContentDesignWs.loadContentEditorSystemDef(lock=true)` then `saveContentEditorSystemDef(release=true)`, matching CD-15 `#3944`. GET remains. No new SOAP; no field create/delete; no SPA.

## Issues

None that block.

## Notes (non-blocking)

- GET now requires Admin (same tightening CD-15 applied to `/sharedfields`). Documented in product-docs.
- `dataType` / `readOnly` / `cacheTimeoutMinutes` are not written (no objectstore setters used).
- Cross-platform path checklist: N/A (no file I/O).

## Tests / companions

- Mockito `SystemDefResourceTest` (GET/PUT 400/403/409/500)
- Spring `TestSystemDefAdaptor` implements new `updateSystemDef`
- `SystemDefAdaptorTest` success / 403 / load-lock 409 / save-lock 409 / unknown field / occurrence conflict
- product-docs 8.2 Developer REST + admin content-types + gap map CD-16 write

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 714, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1661, Failures: 0
- Downstream: grepped `implements ISystemDefAdaptor` (stub + adaptor); sitemanage standalone install
