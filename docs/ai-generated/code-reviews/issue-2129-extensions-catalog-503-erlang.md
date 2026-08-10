# Erlang review: fix/issue-2129-extensions-catalog-503

## Summary

Align `ExtensionsResource` with View/Slots peers: constructor-inject `IExtensionAdaptor`, `requireAdaptor()` returns **503 SERVICE_UNAVAILABLE** (not `IllegalStateException` → 500) when uninjected, catalog list/detail rethrow `WebApplicationException`, unexpected failures stay 500, null list → empty. Harden `ExtensionsResourceTest` to the peer ladder (success, null-safe, list 500 wrap, missing-adaptor 503 on list+get, WAE rethrow, 404, get 500 wrap). OpenAPI documents 503. `getExtensions` (POST /list) also uses `requireAdaptor` + same catch ladder for consistency.

## Scope

- Branch: `fix/issue-2129-extensions-catalog-503` vs `origin/main`
- Files: `rest/.../extensions/ExtensionsResource.java`, `rest/.../extensions/ExtensionsResourceTest.java`
- Module: rest only (standalone `mvnw clean install` BUILD SUCCESS)
- Cross-platform path review: N/A (no file I/O / paths)
- Prior peer: issue #2128 ViewResource 503 align

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None (bug/suggestion/nit).

### Checklist

- [x] Behavioral unit tests for new/changed logic (503 ladder + rethrow + 500 wrap + null-safe)
- [x] Peer pattern match (ViewResource / SlotsResource)
- [x] No portable-path concerns
- [x] No multi-module sprawl / ExtensionAdaptor rewrite without live stack

