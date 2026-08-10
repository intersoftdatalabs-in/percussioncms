# Erlang review: issue #2130 CE controls catalog 503

**Branch:** `fix/issue-2130-cecontrols-catalog-503`  
**Scope:** `rest/src/main/java/com/percussion/rest/cecontrols/ControlsResource.java`,  
`rest/src/test/java/com/percussion/rest/cecontrols/ControlsResourceTest.java`  
**Peers:** `ExtensionsResource` / `KeywordsResource` (`requireAdaptor` → 503)  
**Parent:** #2117 / #1694 slice C5

## Summary

Align `ControlsResource.requireAdaptor()` misconfiguration with catalog peers: throw
`WebApplicationException` **503 SERVICE_UNAVAILABLE** instead of `IllegalStateException`
(previously re-wrapped as **500**). Document 503 in OpenAPI; rethrow comments on both
handlers. Harden mocked unit tests for list/get success, null-safe list, 404, unexpected
→500, WAE rethrow, bare no-arg ctor →503 on list and get.

## Recommendation

**approve**

## Gate

|                  Check                  |                                                   Result                                                    |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------|
| Bugs                                    | none                                                                                                        |
| Behavioral unit tests for changed logic | yes (9 tests, all green)                                                                                    |
| Non-portable path/file I/O              | N/A (no path I/O)                                                                                           |
| Change-class companions                 | rest resource + Mockito resource test only; no multi-module ControlAdaptor invent without stack (per issue) |
| Standalone `rest` clean install         | pass                                                                                                        |

**May commit/push: yes**

## Issues

None.

## Cross-platform path review

Not applicable — HTTP status / adaptor wiring only.

## Test evidence

- `cd rest && ../mvnw clean install` — BUILD SUCCESS
- `ControlsResourceTest`: tests=9, failures=0, errors=0

