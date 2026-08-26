# Erlang review — #3855 Explorer find/types 400 + find/templates 500

Persona: independent of implementer. Date: 2026-08-26.

## Verdict

**Pass** (no bug/hard-gate findings). Residual risk is C5 live proof on H2 QA (Playwright), required before PR.

## Change class

Explorer REST + SPA catalog bind: `POST /actions/find/types` Jackson envelope/GUID tokens, `GET /actions/find/templates/{id}` uncaught helper failures.

## Companions (closure)

| Layer | Artifact |
|-------|----------|
| rest resource | `ActionMenuResource` null-safe empty list, no 500 |
| rest wire | `AllowedContentTypeMenusRequestJsonReader` + `JsonRootName` |
| rest tests | JsonReader, CXF unmarshall, serial/deserial, resource Mockito |
| sitemanage | beans.xml provider ahead of jackson; adaptor catch; convert null PK |
| sitemanage tests | CatalogRestJaxrsRegistrationTest; adaptor/convert unit tests |
| WebUI | WRAP_ROOT envelope + GUID last-segment coerce |
| Vitest | actionMenuApi + menuCatalogLoad |
| Playwright | explorer-console-clean + explorer-preview-view collectors |
| product-docs | N/A — operator Preview/actions steps unchanged |

## Hard gates

- **Bugs:** None remaining. Live 500 was JAXB `ActionMenu nor any of its super class is known to this context` — fixed with `@XmlSeeAlso(ActionMenu.class)` on `ActionMenuList` (peer `SiteList`). SPA posts WRAP_ROOT `{AllowedContentTypeMenusRequest:{contentIds:[551]}}` (GUID last-segment coerced). JsonReader still binds flat/GUID when Jackson is selected (CXF unit tests).
- **Tests:** Behavioral unit tests for production types (`int[]`, GUID string, wrapped envelope). CXF local POST proves HTTP 200.
- **Portable paths:** Java uses `StandardCharsets` / Jackson trees; Playwright URL regexes; no OS path joins.
- **C2:** No `final`/`sealed` type; `ActionMenuList(Collection)` still accepts the same type (null-safe). TS `findAllowedContentTypeMenus` widened `number[]` → `Array<number\|string>`. Downstream: sitemanage clean install against rest SNAPSHOT.

## Notes (not blocking)

- Swallowing adaptor `RuntimeException` as empty 200 can hide a still-broken helper; C5 must prove HTTP 200 with a selected rffHome row.
- `GET /actions/find/templates/{id}` path param remains `int` (SPA sends numeric last-segment).
