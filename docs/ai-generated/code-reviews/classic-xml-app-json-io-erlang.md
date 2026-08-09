# Erlang review — classic XML Application JSON I/O

| Field | Value |
|-------|-------|
| **Branch** | `feat/classic-xml-app-json-io` |
| **Scope** | Uncommitted classic app JSON request/response support (`system` + docs) |
| **Date** | 2026-08-09 |
| **Persona** | Erlang (strict pre-commit) |

## Summary

Adds bidirectional JSON wire format for classic XML Applications: `.json` response extension, `application/json` request body → input document via `PSJsonContentParser`, shared `PSXmlDocumentJsonCodec`, CE/update response paths.

## Recommendation

**approve**

## Gate

| Item | Result |
|------|--------|
| Bugs | None remaining after pre-commit fixes |
| Behavioral tests | Codec goldens + round-trip + invalid name; parser happy/error paths; page-type extension |
| Change-class closure | Codec + converter + parser registration + UpdateHandler + CE + docs + tests |
| Cross-platform I/O | N/A (in-memory bytes/strings; no filesystem path joins) |
| May commit/push | **yes** |

## Issues found and fixed before approve

| Severity | Finding | Mitigation |
|----------|---------|------------|
| bug | Empty request page extension could `charAt(0)` NPE/SIE | Guard null/empty before prefixing `.` |
| bug | Invalid JSON keys could throw raw DOMException | `requireXmlName` / `requireXmlAttributeName` |
| bug | CE JSON path called `toJson` on possible null result doc | 404 when `resultDoc == null` |
| suggestion | Negative Content-Length / bad charset | Explicit parse exceptions |

## Memory patterns hit

- Missing behavioral unit tests for new non-trivial logic — addressed with goldens + parser tests
- Security rejection path coverage — invalid names + malformed JSON tests

## Residual suggestions (non-blocking)

- Large JSON bodies allocate `byte[length]` in-memory (XML path uses temp file); acceptable for v1; consider temp-file path if product needs multi-GB bodies.
- Optional `Accept: application/json` negotiation deferred by design.
