# Erlang review: issue #2132 C7 relationshiptypes catalog 503

**Date:** 2026-08-06  
**Branch:** `fix/issue-2132-relationshiptypes-catalog-503`  
**Scope:** `rest` only — `RelationshipTypeResource` + `RelationshipTypeResourceTest`  
**Peers:** C6 `ServerConfigsResource` / Slots / Keywords `requireAdaptor` → 503 ladder  
**Parent:** #2117 / epic #1694

## Summary

Align `RelationshipTypeResource.requireAdaptor()` with catalog peers: missing adaptor is **503 Service Unavailable** (`WebApplicationException`), not an `IllegalStateException` re-wrapped as **500**. OpenAPI documents 503. Unit tests match peer ladder (delegate, null-safe list, 404, 500 wrap, WAE rethrow, bare 503 list/get).

## Scope

|                                     Path                                     |                          Change                          |
|------------------------------------------------------------------------------|----------------------------------------------------------|
| `rest/src/main/java/.../relationshiptypes/RelationshipTypeResource.java`     | requireAdaptor → 503; OpenAPI 503; preserve WAE comments |
| `rest/src/test/java/.../relationshiptypes/RelationshipTypeResourceTest.java` | peer ladder tests                                        |

No file I/O / path handling in this diff.  
Cross-platform path review: N/A (no path/file I/O).

Memory patterns: REST resource null-adaptor must map to 503 not 500; catch blocks must rethrow `WebApplicationException` before generic Exception→500 wrap.

## Recommendation

**approve**

## Gate

- Bugs: none
- Behavioral unit tests for changed logic: present (9 tests, peer ladder)
- Portable paths: N/A
- **May commit/push: yes**

## Issues

None.

## Verification

```text
cd rest && ../mvnw clean install
BUILD SUCCESS
RelationshipTypeResourceTest: Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

