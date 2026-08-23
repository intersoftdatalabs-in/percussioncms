# Content type design API — gap map (P0.2)

|  Field   |                                  Value                                   |
|----------|--------------------------------------------------------------------------|
| **Date** | 2026-07-28                                                               |
| **FR**   | CD-01–CD-19 in `docs/developer-module/workbench-functional-inventory.md` |

## Surfaces available today

|              Surface              |             Capability              |                  Path / type                   |
|-----------------------------------|-------------------------------------|------------------------------------------------|
| Public REST list                  | Catalog name/label/description/guid | `GET /services/contenttypes`                   |
| Public REST detail (**new P0.2**) | Read-only field catalog + meta      | `GET /services/contenttypes/{idOrName}`        |
| Design SOAP (Workbench)           | Full load/save/lock/create/delete   | `IPSContentDesignWs` / `ContentDesignSOAPImpl` |
| Item def manager                  | Runtime CE definition cache         | `PSItemDefManager.getItemDef`                  |

## Detail payload includes

- name, label, description, enabled, hideFromMenu, appName, editorUrl, guid
- fields: name, label (display), origin (local/system/shared), dataType, control, searchable, fieldSet, required
- fields **P0.2c rule flags**: readOnly, occurrence, hasValidation, hasVisibilityRules, hasInputTranslation, hasOutputTranslation
- fields **CD-05–07 read-only expressions** (issue #2920): `validationExpression`, `visibilityExpression`, `inputTranslationExpression`, `outputTranslationExpression`, `controlPropertyNames[]` (names only; no values)
- child field set names
- **allowedWorkflows[]** + **defaultWorkflow** (P0.2b)
- **allowedTemplates[]** (P0.2b)
- `designGaps[]` strings for client honesty

## Keywords

|              Surface               |                     Path                     |
|------------------------------------|----------------------------------------------|
| List keywords (+ optional choices) | `GET /services/keywords?includeChoices=true` |
| Get one keyword (+ choices)        | `GET /services/keywords/{idOrValue}`         |
| Create keyword                     | `POST /services/keywords`                             |
| Update keyword                     | `PUT /services/keywords/{id}`                         |
| Delete keyword                     | `DELETE /services/keywords/{id}`                      |

**CD-17 Keyword write shipped** (PR #1612 SPA+REST; design-WS path PR #1701; product-docs #2919).
Adaptor: `IKeywordsAdaptor` → `KeywordsAdaptor` via `IPSContentDesignWs` (create/save/delete with
design locks + session user). Companion tests: `KeywordsResourceCrudTest`,
`KeywordsAdaptorDesignWsTest`, Spring `TestKeywordsAdaptor` stub.

## Explicit gaps vs Workbench Content Type editor

|                         Gap                          |    FR IDs    |                       Notes                       |
|------------------------------------------------------|--------------|---------------------------------------------------|
| Full field rule **expressions** / control properties | CD-05–CD-07  | **Read-only expressions + control property names** shipped (#2920); write/save + full catalogs still open |
| Control property + choice configuration              | CD-07        | Control **name** + **property names** (read-only); values/choices not exposed |
| Item-level pre/post exits & validations              | CD-09        | Properties tab                                    |
| Edit workflow/template associations                  | CD-08, CD-12 | **CD-12 template PUT** `PUT /contenttypes/{id}/allowedTemplates` (held lock, #3762). CD-08 workflow PUT still open (#3763). GET lists remain on detail + dedicated GET. |
| Enable/disable as design action                      | CD-13        | Read `enabled` only                               |
| Shared field file editing                            | CD-15        | Separate object                                   |
| System def                                           | CD-16        | Separate object                                   |
| Create / rename / delete / lock                      | CD-01, §5.2  | SOAP design only                                  |
| Import/export CT                                     | CD-14        | Workbench wizards                                 |
| ACL                                                  | CD-19, §5.4  | Existing ACL REST may help later                  |
| ~~Keyword write~~                                    | CD-17        | **Done** — REST + SPA + design WS (#1612/#1701)   |

## Recommended next API work

1. ~~Optional JSON projection of field rules (read-only)~~ **Done P0.2c (flags only)**
2. ~~Read-only field rule expressions + control property names~~ **Done CD-05-07 read path (#2920)**
3. Design-session lock + `PUT` save via thin REST over `IPSContentDesignWs`
4. ~~Keyword create/update/delete~~ **Done CD-17** (REST write + design WS + SPA editor)
5. Control property value/choice catalogs + rule write/save
6. Templates/slots design editors

## Client behavior

SPA shows gaps list under the field table so implementers do not confuse catalog view with full editor parity.
