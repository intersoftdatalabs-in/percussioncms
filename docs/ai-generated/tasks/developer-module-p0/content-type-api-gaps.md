# Content type design API — gap map (P0.2)

|  Field   |                                  Value                                   |
|----------|--------------------------------------------------------------------------|
| **Date** | 2026-08-29                                                               |
| **FR**   | CD-01–CD-19 in `docs/developer-module/workbench-functional-inventory.md` |

## Surfaces available today

|              Surface              |             Capability              |                  Path / type                   |
|-----------------------------------|-------------------------------------|------------------------------------------------|
| Public REST list                  | Catalog name/label/description/guid | `GET /services/contenttypes`                   |
| Public REST detail (**new P0.2**) | Read-only field catalog + meta      | `GET /services/contenttypes/{idOrName}`        |
| Public REST lock/unlock           | Self-only design-session lock       | `POST /services/contenttypes/{idOrName}/lock` / `.../unlock` |
| Public REST PUT save              | Held-lock save (label/description/…) | `PUT /services/contenttypes/{idOrName}`        |
| Public REST icon strategy (CD-11) | GET/PUT none\|specified\|fromFileField | `GET/PUT /services/contenttypes/{idOrName}/icon` |
| Public REST POST create           | Persist new type (Workbench Finish) | `POST /services/contenttypes`                  |
| Public REST shared-field list     | CD-15 catalog (Admin only)          | `GET /services/sharedfields`                   |
| Public REST shared-field detail   | CD-15 group fields (Admin only)     | `GET /services/sharedfields/{idOrName}`        |
| Public REST shared-field write    | CD-15 create/save/delete (Admin)    | `POST /services/sharedfields`, `PUT …/{idOrName}`, `DELETE …/{idOrName}` |
| Public REST shared-field fields   | CD-15 field create/delete (Admin)   | `POST …/{idOrName}/fields`, `DELETE …/{idOrName}/fields/{fieldName}` |
| Public REST include field         | CD-04 include system/shared (Admin) | `POST /services/contenttypes/{idOrName}/fields/include` |
| Public REST system-def catalog    | CD-16 GET (Admin only)              | `GET /services/systemdef`                      |
| Public REST system-def write      | CD-16 field-property save (Admin)   | `PUT /services/systemdef` (request lock, release on save) |
| Public REST system-def fields     | CD-16 field create/delete (Admin)   | `POST /services/systemdef/fields`, `DELETE /services/systemdef/fields/{fieldName}` |
| Design SOAP (Workbench)           | Full load/save/lock/create/delete   | `IPSContentDesignWs` / `ContentDesignSOAPImpl` |
| Item def manager                  | Runtime CE definition cache         | `PSItemDefManager.getItemDef`                  |

## Detail payload includes

- name, label, description, enabled, hideFromMenu, appName, editorUrl, guid
- fields: name, label (display), origin (local/system/shared), dataType, control, searchable, fieldSet, required
- fields **P0.2c rule flags**: readOnly, occurrence, hasValidation, hasVisibilityRules, hasInputTranslation, hasOutputTranslation
- fields **CD-05–07 read-only expressions** (issue #2920): `validationExpression`, `visibilityExpression`, `inputTranslationExpression`, `outputTranslationExpression`, `controlPropertyNames[]` plus `controlProperties[]` name/value (CD-07 GET/PUT #3786; choice filter / null-entry / default-selected write #3995)
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

## Locales

|              Surface               |                     Path                     |
|------------------------------------|----------------------------------------------|
| List locales                       | `GET /services/locales`                      |
| Get one locale                     | `GET /services/locales/{idOrLang}`           |
| Create locale                      | `POST /services/locales`                     |
| Update locale                      | `PUT /services/locales/{idOrLang}`           |
| Delete locale                      | `DELETE /services/locales/{idOrLang}`        |
| Get auto-translation set           | `GET /services/locales/auto-translations`    |
| Replace auto-translation set       | `PUT /services/locales/auto-translations`    |

**CD-18 Locale write shipped** (REST create/update/delete, #3959). Adaptor: `ILocalesAdaptor` →
`LocalesAdaptor` via `IPSContentDesignWs` (`createLocales` / `loadLocales` / `saveLocales` /
`deleteLocales` with a held design lock released on save). Admin 403; unknown 404;
lock/duplicate/dependency 409. Companion tests: `LocalesResourceTest` CRUD, `LocalesAdaptorDesignWsTest`,
Spring `TestLocalesAdaptor` stub.

**CD-18 Auto-translation set shipped** (REST GET/PUT, #3981). Adaptor: `IAutoTranslationsAdaptor` →
`AutoTranslationsAdaptor` via `IPSContentDesignWs` (`loadTranslationSettings` / `saveTranslationSettings`
with a held design lock released on save). Empty list clears. Admin 403; unknown locale/content type
400; lock 409. Companion tests: `AutoTranslationsResourceTest`, `AutoTranslationsAdaptorTest`, Spring
`TestAutoTranslationsAdaptor` stub. SPA locale editor remains a later slice.

## Explicit gaps vs Workbench Content Type editor

|                         Gap                          |    FR IDs    |                       Notes                       |
|------------------------------------------------------|--------------|---------------------------------------------------|
| Full field rule **expressions** / control properties | CD-05–CD-07  | **Read-only expressions** shipped (#2920); **control property values + choice catalogs** GET/PUT (#3786). Rule write/save still open |
| Control property + choice configuration              | CD-07        | **REST GET/PUT** `.../fields/{fieldName}/controlProperties` (#3786, held design lock for PUT). Choice filter / null-entry / default-selected **written** when `choices` is sent (#3995). SPA still omits `choices`. Shared-field control properties are #3984 |
| Item-level pre/post exits & validations              | CD-09        | Properties tab                                    |
| Edit workflow/template associations                  | CD-08, CD-12 | **CD-08 REST PUT .../allowedWorkflows** (#3763, held design lock); **CD-12 REST PUT .../allowedTemplates** (#3775, held design lock; full replace, empty list clears) |
| Enable/disable search indexing at type level         | CD-10        | **REST `GET`/`PUT /contenttypes/{id}/searchIndexing`** (#3996, root field-set `isUserSearchable`; held design lock on PUT; 409 without). Default on. Distinct from per-field `searchable`. SPA Properties checkbox still open |
| Enable/disable as design action                      | CD-13        | **REST `PUT /contenttypes/{id}/enabled`** (#3773, held design lock; 409 without) |
| Content type **icon strategy**                       | CD-11        | **REST `GET/PUT /contenttypes/{id}/icon`** (#3997, held design lock for PUT; `none` clears value; blank non-none value 400; no binary upload; SPA picker still Workbench) |
| Shared field file **write**                          | CD-15        | **Group create/save/delete shipped** (`POST /services/sharedfields`, `PUT …/{idOrName}`, `DELETE …/{idOrName}`, #3944). **Field create/delete shipped** (`POST …/fields`, `DELETE …/fields/{fieldName}`, persistable `PSField` + display mapping, Admin 403, lock 409, #3954). Control/choice write and SPA editor still open |
| System def **write** (existing field properties)     | CD-16        | **PUT `/services/systemdef` shipped** (#3958, Admin 403, lock 409). **Field create/delete shipped** (`POST /services/systemdef/fields`, `DELETE …/fields/{fieldName}`, persistable `PSField` + display mapping, Admin 403, lock 409, #3964). Control/stylesheet/flow and SPA editor still SOAP |
| Create / delete                                      | CD-01, §5.2  | **POST `/services/contenttypes` create shipped** (#3912). Delete still SOAP design only; lock + PUT save via REST |
| Rename                                               | CD-01, §5.2  | **REST `PUT /contenttypes/{id}/name`** (#3914, held design lock; unique, no spaces; bulk PUT does not rename) |
| Import/export CT                                     | CD-14        | Workbench wizards                                 |
| ACL                                                  | CD-19, §5.4  | Existing ACL REST may help later                  |
| ~~Keyword write~~                                    | CD-17        | **Done** — REST + SPA + design WS (#1612/#1701)   |
| Locale **write** (create/update/delete)              | CD-18        | **REST write shipped** (#3959, design WS + Admin 403 / lock 409) |
| Auto-translation **set** (GET/PUT)                   | CD-18        | **REST GET/PUT shipped** (#3981, `loadTranslationSettings` / `saveTranslationSettings`, Admin 403 / lock 409 / unknown locale-type 400). SPA locale editor remains open |
| Include system/shared fields                         | CD-04        | **REST `POST /contenttypes/{id}/fields/include` shipped** (#3985, held design lock, origin stays system/shared). SPA field picker still Workbench |

## Recommended next API work

1. ~~Optional JSON projection of field rules (read-only)~~ **Done P0.2c (flags only)**
2. ~~Read-only field rule expressions + control property names~~ **Done CD-05-07 read path (#2920)**
3. ~~Design-session lock + `PUT` save via thin REST over `IPSContentDesignWs`~~ **Lock** (#3742) + **PUT save requires held lock** (#3743)
4. ~~Keyword create/update/delete~~ **Done CD-17** (REST write + design WS + SPA editor)
5. ~~Control property value/choice catalogs~~ **Done CD-07 REST** (#3786). ~~Choice filter / null-entry / default-selected write~~ **Done** (#3995). Field-rule write/save still open. SPA Choices tab still later. Shared-field control properties are #3984
6. ~~Shared field catalog read~~ **Done CD-15 read** (`GET /services/sharedfields`, Admin 403). ~~Write~~ **Done CD-15 group create/save/delete** (`POST`/`PUT`/`DELETE`, #3944). ~~Field create/delete~~ **Done** (`POST …/fields`, `DELETE …/fields/{fieldName}`, #3954). Control/choice write + SPA editor still open
7. ~~System def field-property write~~ **Done CD-16 PUT** (`PUT /services/systemdef`, Admin 403, request lock + release on save, #3958). ~~Field create/delete~~ **Done** (`POST /services/systemdef/fields`, `DELETE …/fields/{fieldName}`, #3964). Control/stylesheet/flow + SPA still open
8. ~~Include system/shared fields~~ **Done CD-04 REST** (`POST /contenttypes/{id}/fields/include`, Admin 403, held lock 409, origin stays system/shared, #3985). SPA field picker still open
9. Templates/slots design editors
10. CD-18 remainder: SPA locale / auto-translation editor (REST locale CRUD #3959; REST auto-translation set #3981)

## Client behavior

SPA shows gaps list under the field table so implementers do not confuse catalog view with full editor parity.
