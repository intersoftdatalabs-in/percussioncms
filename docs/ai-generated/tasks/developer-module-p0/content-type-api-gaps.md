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
| Public REST lock/unlock           | Self-only design-session lock       | `POST /services/contenttypes/{idOrName}/lock` / `.../unlock` |
| Public REST PUT save              | Held-lock save (label/description/…) | `PUT /services/contenttypes/{idOrName}`        |
| Design SOAP (Workbench)           | Full load/save/lock/create/delete   | `IPSContentDesignWs` / `ContentDesignSOAPImpl` |
| Item def manager                  | Runtime CE definition cache         | `PSItemDefManager.getItemDef`                  |

## Detail payload includes

- name, label, description, enabled, hideFromMenu, appName, editorUrl, guid
- fields: name, label (display), origin (local/system/shared), dataType, control, searchable, fieldSet, required
- fields **P0.2c rule flags**: readOnly, occurrence, hasValidation, hasVisibilityRules, hasInputTranslation, hasOutputTranslation
- fields **CD-05–07 read-only expressions** (issue #2920): `validationExpression`, `visibilityExpression`, `inputTranslationExpression`, `outputTranslationExpression`, `controlPropertyNames[]` plus `controlProperties[]` name/value (CD-07 GET/PUT #3786)
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
| Full field rule **expressions** / control properties | CD-05–CD-07  | **Read-only expressions** shipped (#2920); **control property values + choice catalogs** GET/PUT (#3786). Rule write/save still open |
| Control property + choice configuration              | CD-07        | **REST GET/PUT** `.../fields/{fieldName}/controlProperties` (#3786, held design lock for PUT). Choice filter / null-entry / default-selected not written |
| Item-level pre/post exits & validations              | CD-09        | Properties tab                                    |
| Edit workflow/template associations                  | CD-08, CD-12 | **CD-08 REST PUT .../allowedWorkflows** (#3763, held design lock); **CD-12 REST PUT .../allowedTemplates** (#3775, held design lock; full replace, empty list clears) |
| Enable/disable as design action                      | CD-13        | **REST `PUT /contenttypes/{id}/enabled`** (#3773, held design lock; 409 without) |
| Shared field file editing                            | CD-15        | Separate object                                   |
| System def                                           | CD-16        | Separate object                                   |
| Create / rename / delete                             | CD-01, §5.2  | SOAP design only; lock + PUT save via REST        |
| Import/export CT                                     | CD-14        | Workbench wizards                                 |
| ACL                                                  | CD-19, §5.4  | Existing ACL REST may help later                  |
| ~~Keyword write~~                                    | CD-17        | **Done** — REST + SPA + design WS (#1612/#1701)   |

## Recommended next API work

1. ~~Optional JSON projection of field rules (read-only)~~ **Done P0.2c (flags only)**
2. ~~Read-only field rule expressions + control property names~~ **Done CD-05-07 read path (#2920)**
3. ~~Design-session lock + `PUT` save via thin REST over `IPSContentDesignWs`~~ **Lock** (#3742) + **PUT save requires held lock** (#3743)
4. ~~Keyword create/update/delete~~ **Done CD-17** (REST write + design WS + SPA editor)
5. ~~Control property value/choice catalogs~~ **Done CD-07 REST** (#3786). Field-rule write/save still open
6. Templates/slots design editors

## Client behavior

SPA shows gaps list under the field table so implementers do not confuse catalog view with full editor parity.
