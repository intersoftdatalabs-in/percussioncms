# Release notes draft — Classic XML Application JSON I/O (8.2)

**Status:** Draft for product / release packaging (parent **#2662**, slice **#2664**).  
**Tracking:** [GitHub #2662](https://github.com/intersoftdatalabs-in/percussioncms/issues/2662) · [slice #2664](https://github.com/intersoftdatalabs-in/percussioncms/issues/2664)  
**Feature:** Landed in **#2660** (`system` runtime codec + parsers + developer map).

Use this text as the source for official release notes and operator / integrator upgrade guides. Codec rules and engineering detail live in the developer doc (link below).

---

## Summary

Classic XML Applications can exchange **JSON** on the wire while the server continues to use the same **XML DOM** for mappers, exits, and page tanks. Starting with the 8.2 line that ships this change:

| Area | Behavior |
|------|----------|
| **JSON response** | Request the resource with page extension **`.json`** → HTTP response `Content-Type: application/json` (same result document as `.xml`, **no XSL**) |
| **JSON request body** | Send `Content-Type: application/json` on the request → body becomes the input document (same structural shape as an XML body, via the JSON codec) |
| **Unchanged defaults** | No extension / `.xml` still returns XML; form-urlencoded and XML bodies still work; only `application/json` is accepted for JSON input (not `text/json`) |

---

## Who is affected?

| Estate | Affected? | Action |
|--------|-----------|--------|
| Sites that only use classic apps as **XML/HTML** | **Optional** | No required config change. Adopt `.json` / JSON bodies when integrators need it. |
| Integrators calling classic app URLs over HTTP | **Yes (capability)** | May request `…/resource.json` and/or POST JSON bodies with `Content-Type: application/json`. |
| Pipeline IR `execute` JSON clients | **No change to that API** | Classic app JSON I/O is a **different surface** (see below). |
| Workbench / Designer page-tank tooling | **No new Designer JSON UI** | Design-time JSON schema / OpenAPI generation are **not** part of this release. |

---

## Operator / integrator highlights

### Response: `.json` extension

Same URL model as XML/HTML. Example:

```http
GET /Rhythmyx/MyApp/products.json HTTP/1.1
```

| Extension | MIME type | Notes |
|-----------|-----------|--------|
| *(none)* / `.xml` | `text/xml` | Default result document |
| `.txt` | `text/plain` | Same document as XML |
| `.html` / `.htm` | `text/html` | XSL merge when result pages configured |
| **`.json`** | **`application/json`** | Same document as XML, **no XSL** |

### Request: `application/json` body

```http
POST /Rhythmyx/MyApp/updateResource.xml HTTP/1.1
Content-Type: application/json; charset=UTF-8

{"Order":{"@id":"1","Item":[{"Sku":"A"},{"Sku":"B"}]}}
```

The body is decoded to the same input document shape used for XML updates. Existing update pipes, mappers, and exits consume that document unchanged.

### Mapping rules

Attribute prefix `@`, mixed text under `#text`, repeated siblings as arrays, and related encode/decode rules are documented for implementers in:

**[docs/developer-module/classic-xml-app-json-io.md](../../../developer-module/classic-xml-app-json-io.md)**

---

## Complementarity: classic apps vs Pipeline IR execute JSON

| Surface | When to use |
|---------|-------------|
| **Classic app `.json` + JSON body** (this feature) | Existing deployed XML Applications / Content Editor–style resources; minimal change for current URLs |
| **Pipeline IR** `POST /services/pipelines/{app}/resources/{resource}/execute` | Native IR runtime, structured JSON request DTOs, new developer pipelines ([pipeline-ir-v1.md](../../../developer-module/pipeline-ir-v1.md)) |

These are **complementary**, not replacements. Release packaging should not describe classic app JSON I/O as the Pipeline IR execute API (or vice versa).

---

## Explicit non-claims (do not put in marketing as shipped)

This 8.2 classic-app JSON I/O work **does not** include:

- Workbench / **Designer** JSON schema page tank or design-time JSON UI  
- **OpenAPI** generation from classic XML Applications  
- **JSON Schema** validation against page-tank DTDs  
- XSL templates that emit JSON  
- Accept-header content negotiation without the `.json` extension (tracked separately under residual #2663 if prioritized)  
- Replacing form-urlencoded updates as the only update path  

---

## Related links

| Doc / issue | Purpose |
|-------------|---------|
| [classic-xml-app-json-io.md](../../../developer-module/classic-xml-app-json-io.md) | Codec mapping rules, security notes, request/response detail |
| [data-pipeline-server-runtime-map.md](../../../developer-module/data-pipeline-server-runtime-map.md) | Server runtime map for classic apps |
| [pipeline-ir-v1.md](../../../developer-module/pipeline-ir-v1.md) | Pipeline IR surface (related, not this feature) |
| Feature PR **#2660** | Runtime implementation |
| Parent tracker **#2662** | Residuals (Accept negotiation, live smoke, large-body hardening, this release-notes slice) |

---

## Suggested short blurb (copy for official notes)

> **Classic XML Applications — JSON request and response (8.2)**  
> Classic XML Application resources can return JSON when the page extension is `.json` (`Content-Type: application/json`, same result document as XML, no XSL). Clients may also submit `Content-Type: application/json` request bodies, which are mapped to the same input document used for XML. Mapping rules are documented in `docs/developer-module/classic-xml-app-json-io.md`. This is complementary to the Pipeline IR `execute` JSON API and does not add Designer JSON schema tooling, OpenAPI generation, or JSON Schema validation.
