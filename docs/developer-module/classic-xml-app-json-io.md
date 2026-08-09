# Classic XML Application JSON I/O

| Field | Value |
|-------|-------|
| **Status** | Implemented (classic runtime) |
| **Module** | `system` (`com.percussion.data`, `com.percussion.server`) |
| **Related** | [data-pipeline-server-runtime-map.md](./data-pipeline-server-runtime-map.md), [pipeline-ir-v1.md](./pipeline-ir-v1.md) |

## Purpose

Classic XML Applications historically return **XML** (`.xml` / `.txt`) or **HTML** (`.html` / `.htm` via XSL result pages). This feature adds first-class **JSON** as:

1. **Response format** — request page extension `.json` → `Content-Type: application/json`
2. **Request body format** — `Content-Type: application/json` → input document for update pipes (and any consumer of `PSRequest.getInputDocument()`)

The server still builds and consumes the same **XML DOM** used by mappers, exits, and page tanks. JSON is a wire encoding of that document.

## Response: selecting JSON

Same URL model as XML/HTML:

| Extension | MIME type | Notes |
|-----------|-----------|--------|
| *(none)* / `.xml` | `text/xml` | Default result document |
| `.txt` | `text/plain` | Same document as XML |
| `.html` / `.htm` | `text/html` | XSL merge when result pages configured |
| **`.json`** | **`application/json`** | Same document as XML, **no XSL** |

Example:

```http
GET /Rhythmyx/MyApp/products.json HTTP/1.1
```

`PSRequest.PAGE_TYPE_JSON` (`0x08`) is set from the extension. Query resources use `PSResultSetHtmlConverter`; update stats/results use `PSUpdateHandler`; Content Editor query uses `PSQueryCommandHandler`.

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
- Nesting limited by `MAX_DEPTH`.

## Classic apps vs Pipeline IR

| Surface | When to use |
|---------|-------------|
| **Classic app `.json` + JSON body** | Existing deployed XML Applications / CE-style resources; minimal change |
| **Pipeline IR** `POST /services/pipelines/{app}/resources/{resource}/execute` | Native IR runtime, structured JSON request DTOs, new developer pipelines ([pipeline-ir-v1.md](./pipeline-ir-v1.md)) |

These are complementary, not replacements.

## Non-goals (v1)

- Accept header content negotiation without extension  
- XSL that emits JSON  
- JSON Schema validation against page-tank DTDs  
- Designer UI for “JSON page tank”  
- Changing the default when no extension is present (still XML)
