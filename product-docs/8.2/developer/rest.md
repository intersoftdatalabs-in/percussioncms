---
id: developer-rest
title: REST API
description: Public REST API orientation for Percussion CMS 8.2
version: "8.2"
order: 51
tags: [developer, rest]
---

# REST API

Percussion CMS exposes a public **REST** surface for product integrations and modern UI clients.

## Module layout

| Module | Responsibility |
|--------|----------------|
| **`rest`** | JAX-RS resources, wire DTOs, `IXxxAdaptor` interfaces, OpenAPI generation inputs |
| **`projects/sitemanage`** | Thin `com.percussion.apibridge` implementations of those interfaces |
| **`system`** | Core services, objectstore, assembly, design backends |

Do **not** reverse the dependency: `rest` never depends on `sitemanage`.

## OpenAPI / exploration

- OpenAPI artifacts are generated from JAX-RS annotations (see `modules/perc-openapi-generator-plugin`).
- A Swagger UI webapp module packages interactive exploration for supported deployments.

Prefer the generated contract as the integration source of truth rather than reverse-engineering
UI traffic alone.

## Auth and clients

- REST calls require the authentication mode configured for the server (session/cookie or token
  patterns depending on surface and deployment).
- Treat credentials and tokens as secrets; never commit them to Git or product-docs examples.

## Workbench-replacement APIs

Developer-module (Workbench replacement) endpoints should map design operations through clean REST
+ adaptors to the same design/system capabilities classic tools used — not ad-hoc sitemanage
endpoints chosen only because they “look REST.” See repository
`docs/developer-module/workbench-rest-and-qa-modes.md` for the engineering contract.

## Design capability gaps (`designGaps`)

Some Developer detail payloads include a **`designGaps`** array so clients know what the REST
surface does **not** yet match full Workbench / design-WS capability.

### Structured shape (Content Type, Template, Slot detail)

On **content type**, **template**, and **slot** detail responses, each gap is a structured object:

```json
{
  "designGaps": [
    {
      "code": "CT_ITEM_EXITS",
      "message": "Item-level pre/post exits not exposed"
    }
  ]
}
```

| Field | Role |
|-------|------|
| **`code`** | Stable machine-readable id for SPA grouping, docs links, and future i18n keys |
| **`message`** | English human-readable text for operators (this release) |

Do **not** treat these entries as free-text strings on those three detail paths. Other Developer
catalog detail resources may still return string arrays until migrated.

Clients should render **`message`** when present and fall back to **`code`** (or a legacy string)
when needed.

## Testing tips

- Unit-test resources with Mockito and provide Spring test stubs for new adaptor interfaces on the
  rest test classpath.
- Exercise adaptor implementations in sitemanage tests.
- Run **standalone** `mvnw clean install` in each changed module before PR (see root `AGENTS.md`).

## Related

- [Extensions & packages](id:developer-extensions)
- [Build from source](id:developer-build-source)
