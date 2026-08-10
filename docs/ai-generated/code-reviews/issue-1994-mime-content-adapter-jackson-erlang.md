# Erlang review — issue #1994 (PSMimeContentAdapter Jackson)

**Verdict:** PASS (self-review before commit)

**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** approve

**Scope reviewed:** Jackson opt-in annotations + base64 content wire + golden/round-trip
tests for `PSMimeContentAdapter`; deviations doc
`docs/ai-generated/tasks/505-betwixt-jackson/1994-mime-content-adapter-deviations.md`.

## Gates

|          Check          |                                                      Result                                                      |
|-------------------------|------------------------------------------------------------------------------------------------------------------|
| Bugs / RT correctness   | Pass — `PSSystemDataXmlSerializationTest` 20/20 green (incl. 4 mime tests); `PSMimeContentAdapterTest` 1/1 green |
| Behavioral unit tests   | Pass — golden + inline RT + attachment RT + legacy `<null>` root; interface test JUnit 5                         |
| Cross-platform paths    | Pass — no new filesystem path construction (classpath resources + in-memory buffers only)                        |
| Change-class companions | Pass — domain annotations, golden fixture, deviations doc; peer pattern matches #1920/#1993 system data batch    |
| Spotless                | Pass — module `spotless:apply` then `spotless:check` on system; out-of-scope HierarchyNode reformat discarded    |

## Notes

- Wire root `mime-content-adapter`; inline payload is standard Base64 under `content`.
- API `InputStream` surface is `@JsonIgnore`; `getContentBase64` / `setContentBase64` own the wire.
- Content buffered as `byte[]` so BeanUtils property-copy order and re-read after `toXML` are safe.
- `setContent(null)` is null-safe (BeanUtils may clear content before `attachment-id`).
- Suppressed: `label`, `content-attached`, `description`.

## Residuals (not this PR)

- Betwixt POM removal (#1824)
- SOAP webservice DTO / converter (already separate)

## Memory patterns hit

- Jackson domain batch: opt-in `@JsonAutoDetect`, root element, golden/RT/legacy-null triad
- Null-safe setters for BeanUtils XML restore (`setVersion` peer)
- Binary/content fields: buffer + base64 string property, not raw `InputStream` on the wire

