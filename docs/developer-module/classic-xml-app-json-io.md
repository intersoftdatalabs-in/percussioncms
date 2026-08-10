# Classic XML Application JSON I/O

| Field | Value |
|-------|-------|
| **Status** | Implemented (classic runtime) |
| **Module** | `system` (`com.percussion.data`, `com.percussion.server`) |
| **Related** | [data-pipeline-server-runtime-map.md](./data-pipeline-server-runtime-map.md), [pipeline-ir-v1.md](./pipeline-ir-v1.md) |

## Purpose

Classic XML Applications historically return **XML** (`.xml` / `.txt`) or **HTML** (`.html` / `.htm` via XSL result pages). This feature adds first-class **JSON** as:

1. **Response format** — request page extension `.json`, or extensionless URL with `Accept` preferring `application/json` (see [precedence](#precedence-extension--accept--default-xml)) → `Content-Type: application/json`
2. **Request body format** — `Content-Type: application/json` → input document for update pipes (and any consumer of `PSRequest.getInputDocument()`)

The server still builds and consumes the same **XML DOM** used by mappers, exits, and page tanks. JSON is a wire encoding of that document.

## Response: selecting JSON

### Precedence (extension > Accept > default XML)

| Priority | Rule | Result |
|----------|------|--------|
| **1** | Known page extension (`.xml` / `.html` / `.htm` / `.txt` / `.json`) | That extension's page type **always wins** |
| **2** | **No** extension + `Accept` prefers JSON | `PAGE_TYPE_JSON` |
| **3** | Otherwise | Product default **XML** |

JSON is preferred from `Accept` only when:

- Media type is `application/json`, or a structured JSON type `application/*+json` (e.g. `application/ld+json`), and
- Its quality (`q`) is **strictly greater** than any competing XML/HTML type (`application/xml`, `text/xml`, `text/html`, `application/xhtml+xml`, `*+xml`).

Missing `Accept`, `*/*` alone, Accept that prefers XML/HTML, or equal `q` for JSON and XML → still **XML**. Unknown extensions (e.g. `.bin`) stay `PAGE_TYPE_UNKNOWN` and are **not** negotiated via Accept.

Same URL model as XML/HTML:

| Extension | MIME type | Notes |
|-----------|-----------|--------|
| *(none)* (no Accept / Accept XML) | `text/xml` | Default result document |
| *(none)* + `Accept: application/json` | **`application/json`** | Same document as XML, **no XSL** |
| `.xml` | `text/xml` | Extension wins even if Accept asks for JSON |
| `.txt` | `text/plain` | Same document as XML |
| `.html` / `.htm` | `text/html` | XSL merge when result pages configured |
| **`.json`** | **`application/json`** | Extension wins; same document as XML, **no XSL** |

Examples:

```http
GET /Rhythmyx/MyApp/products.json HTTP/1.1
```

```http
GET /Rhythmyx/MyApp/products HTTP/1.1
Accept: application/json
```

`PSRequest.PAGE_TYPE_JSON` (`0x08`) is set from the extension or from Accept negotiation when there is no extension. Query resources use `PSResultSetHtmlConverter`; update stats/results use `PSUpdateHandler`; Content Editor query uses `PSQueryCommandHandler`.

## Request: JSON input document

Register parser: `PSJsonContentParser` for `application/json` (alongside XML and form parsers in `PSRequest`).

```http
POST /Rhythmyx/MyApp/updateResource.xml HTTP/1.1
Content-Type: application/json; charset=UTF-8

{"Order":{"@id":"1","Item":[{"Sku":"A"},{"Sku":"B"}]}}
```

Flow:

```
parseBody → PSJsonContentParser → PSXmlDocumentJsonCodec.fromJson
  → request.setInputDocument(Document)
  → existing PSUpdateHandler / mappers / exits
```

Form-urlencoded and XML bodies are unchanged. Only `application/json` is accepted (not `text/json`).

## Mapping rules (`PSXmlDocumentJsonCodec`)

Bidirectional rules (encode/decode round-trip for supported shapes):

| XML | JSON |
|-----|------|
| Document root element | Single top-level object key = root name |
| Attribute `id="x"` | Property `"@id": "x"` |
| Text-only element | String |
| Empty element | `null` |
| Element with attrs and/or children | Object; text mixed with structure under `"#text"` |
| Repeated same-name siblings | Array |
| Number/boolean in JSON input | Stringified leaf text in DOM |

Constants: attribute prefix `@`, text key `#text`, max depth 64.

Implementation: `system/src/main/java/com/percussion/data/PSXmlDocumentJsonCodec.java`  
Goldens: `system/src/test/resources/com/percussion/data/json-codec/`

## Security

- Same trust boundary as XML input.
- Embedded file URL attributes (`PSXUrlReferenceAttribute`) are rejected (document cleared), matching `PSXmlContentParser`.
- No script execution; pure structural transform.
- Nesting limited by `MAX_DEPTH` (`PSXmlDocumentJsonCodec.MAX_DEPTH` = 64).
- **Body intake / size:** `PSJsonContentParser` streams the request body into a purgable temp file in fixed-size chunks (same helper as `PSXmlContentParser`), then decodes with a charset-aware `Reader`. This avoids allocating a single `byte[Content-Length]` for the entire body on the happy path. There is **no additional hard max body size** beyond the Content-Length `int` already used by the classic request parser (same policy as the XML content path). Content-Length mismatches log a server warning and parse what was actually read.

## Classic apps vs Pipeline IR

| Surface | When to use |
|---------|-------------|
| **Classic app `.json` + JSON body** | Existing deployed XML Applications / CE-style resources; minimal change |
| **Pipeline IR** `POST /services/pipelines/{app}/resources/{resource}/execute` | Native IR runtime, structured JSON request DTOs, new developer pipelines ([pipeline-ir-v1.md](./pipeline-ir-v1.md)) |

These are complementary, not replacements.

## Non-goals (v1)

- Changing a **present** `.xml` / `.html` / `.txt` URL to JSON via Accept alone (extension remains authoritative)  
- Accept negotiation for **request** bodies (already Content-Type driven)  
- XSL that emits JSON  
- JSON Schema validation against page-tank DTDs  
- Designer UI for “JSON page tank”  
- Changing the default when Accept is missing or prefers XML/HTML (still XML)
