# Erlang review — issue #3360 AddFolderRequest JAXB envelope

**Branch:** `fix/issue-3360-addfolderrequest-jaxb`  
**Date:** 2026-08-15  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (rest reader + sitemanage jaxrs:providers + Vitest wrap + Playwright REST); JAXB root-envelope peer (ViewExecuteRequest / AclList)

## Summary

POST `/content-explorer/folders` failed with `JAXBException: unexpected element local:"name"` when Explorer sent a flat `{name,parentPath}` body. The change wraps the SPA POST under `AddFolderRequest`, adds a JAX-RS reader that binds wrap or flat, registers it on `rest-jax-rs` ahead of Jackson, and adds marshal/CXF/Vitest/Playwright coverage.

## Issues

None blocking.

- Production H2 cell still selected Jettison/JAXB for a *flat* POST even with the reader listed (HTTP 400). Preferred wrap (SPA + Playwright) is HTTP 200 under `/Folders` and `/Sites`. Reader is proven on an in-process CXF bus (`AddFolderRequestCxfUnmarshallTest`).
- UI dual-run Playwright skipped when the shell still used pathmanagement (query dropped on client route). Envelope is proven by REST wrap tests.

## Cross-platform path checklist

N/A for new I/O. `CatalogRestJaxrsRegistrationTest` continues to use `java.nio.file.Path`.

## Tests

- rest: `AddFolderRequestJsonReaderTest` (11), `AddFolderRequestSerialDeserialTest` (5), `AddFolderRequestCxfUnmarshallTest` (3)
- WebUI Vitest: wrap helper + POST body has `AddFolderRequest` root
- Playwright H2: wrap create/delete under Folders and Sites passed
