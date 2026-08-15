# Erlang review — #3427 merge conflict (CopyFolderItemRequest)

**Branch:** `cluster/night-issue-20260815-rest-wire-getters`  
**PR:** [#3427](https://github.com/intersoftdatalabs-in/percussioncms/pull/3427)  
**Scope:** merge `origin/main` into cluster; authored resolution vs incoming #3409 wrap-root  
**Date:** 2026-08-15  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** REST DTO `Optional` getters serialize as empty/present beans; wrap-root / JAXB root name is a lockstep wire contract (Explorer Copy #3362).

## Summary

`main` landed #3408 / #3409 / #3414 after the cluster opened. Only
`rest/src/main/java/com/percussion/rest/folders/CopyFolderItemRequest.java`
was **changed in both**.

Union keeps:

- `#3409` `@XmlRootElement(name = "CopyFolderItemRequest")` + `@JsonRootName("CopyFolderItemRequest")` (WRAP_ROOT_VALUE)
- cluster `@JsonInclude(NON_NULL)` + plain `String` getters (no `Optional`)

`CopyFolderItemRequestSerialDeserialTest` (new on `main`) was updated to call
plain getters (`orElse` / `Optional.isEmpty` would not compile).

## Issues

None (gate-blocking).

## Notes (non-blocking)

- `JacksonContextResolverOptionalTest.copyFolderItemRequest_serializesPathsNotOptionalBeans` still only `contains` path keys (passes with wrap-root). Wrap-root itself is locked by `CopyFolderItemRequestSerialDeserialTest`.
- Cross-platform path checklist: **N/A** (no filesystem I/O).
- Incoming `sitemanage` / `WebUI` / `system` files from merged #3408/#3409/#3414 were not re-authored.

## Builds

- `cd rest && ../mvnw.cmd clean install` → **BUILD SUCCESS**; Tests run: 459, Failures: 0, Errors: 0, Skipped: 0
  (`CopyFolderItemRequestSerialDeserialTest`: 5; `JacksonContextResolverOptionalTest`: 36)
