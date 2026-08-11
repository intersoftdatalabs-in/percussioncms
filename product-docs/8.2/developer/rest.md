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

## Keywords (design catalog)

Keyword definitions (Workbench **Keywords** / content design) are exposed under `/services/keywords`.
The REST layer is a thin contract over the content **design** web service (`IPSContentDesignWs`) —
create, update, and delete use the same design locks and session identity classic tools used.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/services/keywords?includeChoices=false\|true` | List keyword definitions; optionally embed choices |
| `GET` | `/services/keywords/{idOrValue}` | Load one keyword by uuid (or numeric id) **or** by value/label (choices included) |
| `POST` | `/services/keywords` | Create a keyword (`label` required, must be unique); optional description, sequence, choices |
| `PUT` | `/services/keywords/{id}` | Update label / description / sequence / choices by uuid |
| `DELETE` | `/services/keywords/{id}` | Delete keyword and its choices by uuid (`204` on success) |

### Request / response shape

JSON objects use the `Keyword` wire type (fields include `guid`, `label`, `value`, `description`,
`sequence`, and `choices[]` with `label` / `value` / `description` / `sequence`). Prefer the
generated OpenAPI schema as the integration source of truth.

Example create body:

```json
{
  "label": "Priority",
  "description": "Item priority",
  "sequence": 1,
  "choices": [
    { "label": "High", "value": "high", "sequence": 1 },
    { "label": "Low", "value": "low", "sequence": 2 }
  ]
}
```

### Status codes and authorization

| Status | Typical meaning |
|--------|-----------------|
| `200` | List / get / create / update success |
| `204` | Delete success |
| `400` | Invalid input (missing label, duplicate label, invalid id) |
| `404` | Keyword not found |
| `500` | Design service or server failure |
| `503` | Keywords adaptor not configured (deployment miswire) |

- Callers must be authenticated; write operations require a request session and user identity for
  the design web service (same pattern as other design catalog writes).
- Design ACL / design-session rules of the underlying content design service still apply — REST does
  not introduce a separate admin-only bypass.

### Integrator notes

- After create/update the server reloads the keyword so the response includes the assigned `guid`
  and normalized choices.
- Prefer uuid (or the `guid` string form) for update/delete; value/label lookup on `GET` is for
  convenience in tooling.
- The Developer SPA Keyword editor uses these endpoints; integrators can call the same surface
  without the UI.

## Testing tips

- Unit-test resources with Mockito and provide Spring test stubs for new adaptor interfaces on the
  rest test classpath.
- Exercise adaptor implementations in sitemanage tests.
- Run **standalone** `mvnw clean install` in each changed module before PR (see root `AGENTS.md`).

## Related

- [Extensions & packages](id:developer-extensions)
- [Build from source](id:developer-build-source)
