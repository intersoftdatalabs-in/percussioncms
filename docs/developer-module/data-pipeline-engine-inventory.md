# Data Pipeline Engine — Functional Inventory & Modernization Brief

**Source system:** Rhythmyx 7.3.2 XML Applications / E2Designer visual data-pipe engine  
**Codebase roots:**  
- Design UI: `Designer/Src/com/percussion/E2Designer` (~519 Java files)  
- Hosting: `Designer/ui/.../PSXmlApplicationEditor` (Swing canvas embedded in Workbench)  
- Object model: `src/com/percussion/design/objectstore` (`PSApplication`, `PSDataSet`, `PSQueryPipe`, `PSUpdatePipe`, …)  
- Help: `ReleasedDocuments/online/com.percussion.doc.workbench` (XML Application topics)

**Document type:** Tool-agnostic inventory + functional requirements + modernization framing  
**Audience:** `percussioncms` product/engineering evaluating revival of the pipeline engine with modern I/O (JSON, modern datasources, hooks)  
**Related:** [workbench-functional-inventory.md](./workbench-functional-inventory.md) §11 (summary); this document is the full treatment.

---

## 1. Why this still matters

The “XML Application” designer looks dated because its **presentation format** is XML/DTD/XSL and its **authoring UI** is a 1990s visual canvas. Underneath is a **complete request → extract/transform → write → respond data pipeline** with:

|     Engine capability      |            Classic surface             |                    Timeless core                    |
|----------------------------|----------------------------------------|-----------------------------------------------------|
| Multi-resource application | XML Application                        | Deployable service/API package                      |
| Typed request endpoint     | Dataset + Requestor                    | Route/handler definition                            |
| Structured I/O schema      | Page Data Tank (DTD)                   | Request/response contract (JSON Schema, OpenAPI, …) |
| Persistence binding        | Backend Data Tank + joins              | Datasource adapters (SQL, REST, CMS repo, …)        |
| Field mapping + functions  | Mapper + UDFs                          | Transform graph / mapping layer                     |
| Query criteria             | Selector (WHERE / manual SQL)          | Query plan / filter expression                      |
| Write semantics            | Updater properties + keys              | CRUD policy + identity keys                         |
| Transactions               | Transaction Manager                    | Unit-of-work scope                                  |
| Pagination/sort            | Results Pager                          | Page/limit/sort                                     |
| Hooks                      | Pre/Post Java exits, result processors | Middleware / lifecycle hooks                        |
| Presentation               | XSL result pages                       | Optional view layer (or none for API-only)          |
| Runtime control            | Start/stop app, cache, tracing         | Deploy, scale, observe                              |

**Modernization thesis:** Keep the **pipeline semantics**; replace XML-as-the-only-document-model and Swing-as-the-only-authoring-UI. JSON (or any structured payload), pluggable datasources, and first-class hooks make this engine relevant for integrations, headless CMS services, migration tools, and custom APIs again.

---

## 2. Conceptual architecture

### 2.1 Three nested design surfaces

From original E2Designer architecture (`package.html`) and Workbench hosting:

```
Application (deployable unit; start/stop)
└── Dataset / Resource  (one request endpoint; query | update | binary)
    └── Data Pipe       (visual composition of pipeline stages)
        ├── Page Data Tank      (document / payload schema)
        ├── Backend Data Tank   (tables + joins, or modern adapters)
        ├── Mapper              (bidirectional field mappings + conditions + functions)
        ├── Selector            (query only: criteria / SQL)
        ├── Results Pager       (query only: paging/sort)
        ├── Updater Properties  (update only: allow C/U/D + key columns)
        ├── Transaction Manager (update only: row vs set atomicity)
        ├── Encryptor           (optional)
        └── (optional synchronizer / related figures)
```

**Process (Application) view figures** (application canvas):

|           Figure           |                     Role                      |
|----------------------------|-----------------------------------------------|
| Query Dataset              | Resource that reads backend → builds document |
| Update Dataset             | Resource that reads document → writes backend |
| Binary / non-text Resource | Binary payload retrieval                      |
| Result Page / Stylesheet   | Output formatting (XSL historically)          |
| Directed Connection        | Links between app figures                     |
| Application File           | Files owned by application                    |
| External Interface         | External contract surface                     |
| Notifier                   | Notification configuration                    |
| App Security               | Application ACL/security settings             |
| Pre/Post Java Exit         | Request-level hooks attached at app/resource  |
| XSL File                   | Stylesheet artifact                           |

**Pipe (Resource) view figures** (pipe canvas):

|          Figure          |  Query   |  Update  |          Notes          |
|--------------------------|----------|----------|-------------------------|
| Query Pipe / Update Pipe | ●        | ●        | Spine of the resource   |
| Page Data Tank           | required | required | Document schema side    |
| Backend Data Tank        | required | required | Persistence side        |
| Mapper                   | required | required | Maps document ↔ backend |
| Selector                 | required | —        | Selection criteria      |
| Results Pager            | optional | —        | Incremental results     |
| Updater / Synchronizer   | —        | required | Write policy            |
| Transaction Manager      | —        | optional | Atomicity scope         |
| Encryptor                | optional | optional | Encryption settings     |

### 2.2 Runtime mental model

For a **query resource**:

```
HTTP/request
  → pre-exits (request preprocessors)
  → bind request values (params, cookies, user context, …)
  → Selector builds query against Backend Data Tank
  → fetch rows
  → Mapper projects rows → Page Data Tank document tree
  → Results Pager slices/sorts if configured
  → post-exits (result document processors)
  → optional stylesheet / result page
  → response
```

For an **update resource**:

```
HTTP/request + input document
  → pre-exits
  → Mapper projects document fields → backend columns
  → Updater applies insert/update/delete per key columns + allow flags
  → Transaction Manager scopes commit (row vs all rows)
  → post-exits
  → response
```

Object model (`PSDataSet` javadoc): *each data set maps a single document type to one or more back-end data stores; query/insert/update/delete may be performed; multiple data sets may exist in one application.*

---

## 3. Application lifecycle (FR)

|   ID   |                       Requirement                        |
|--------|----------------------------------------------------------|
| APP-01 | Create application (name, description)                   |
| APP-02 | Open application design (process canvas + resource list) |
| APP-03 | Add/remove/rename resources (query, update, binary)      |
| APP-04 | Organize application files (HTML, XSL, DTD, assets)      |
| APP-05 | Save application definition to server                    |
| APP-06 | Start / stop application (runtime activation)            |
| APP-07 | Export application XML (serialization of design)         |
| APP-08 | Delete application                                       |
| APP-09 | Configure application-level security                     |
| APP-10 | Configure tracing / debugging flags for diagnosis        |
| APP-11 | Configure resource cache settings (query resources)      |
| APP-12 | Import/transfer auxiliary files with application         |

**System vs user applications:** Navigator separates system applications (restricted mutate) from user applications.

---

## 4. Resource creation modes (FR)

Historical “bootstrap” paths for new resources:

|         Mode         |                Intent                 |                 Modern analogue                 |
|----------------------|---------------------------------------|-------------------------------------------------|
| From HTML page       | Infer page schema + forms from HTML   | From OpenAPI example / HTML form / sample JSON  |
| From database table  | Infer backend tank + starter mappings | From table/view introspection or ORM model      |
| From DTD             | Infer page tank from DTD              | From JSON Schema / XML Schema / Avro / Protobuf |
| White page assembler | Blank structured page scaffold        | Empty schema + empty mapping                    |
| From scratch         | Empty pipe; designer wires all stages | Empty pipeline template                         |
| Non-text / binary    | Binary column/file retrieval          | Blob/file endpoint resource                     |

**Resource categories**

1. **Query** — read path
2. **Update** — write path
3. **Non-text** — binary retrieval

---

## 5. Pipeline stage inventory

### 5.1 Page Data Tank (document / payload schema)

**Purpose:** Defines the **structured document** produced (query) or consumed (update).

**Classic:** DTD-backed XML document shape (elements/attributes).

**Functional requirements**

|   ID   |                         Requirement                          |
|--------|--------------------------------------------------------------|
| PDT-01 | Associate a schema/document definition with the resource     |
| PDT-02 | Browse document fields/nodes for mapping and value selection |
| PDT-03 | Support nested structure (tree), not only flat columns       |
| PDT-04 | Edit/replace schema definition over time                     |
| PDT-05 | Catalog available schemas/DTDs from app/server               |

**Modernization mapping**

|        Classic         |                         Modern                          |
|------------------------|---------------------------------------------------------|
| DTD / XML elements     | JSON Schema / OpenAPI components / GraphQL types        |
| Attributes vs elements | Object properties / arrays                              |
| Empty XML on no rows   | Empty object `{}`, `null`, or 404 policy (configurable) |

### 5.2 Backend Data Tank (datasources)

**Purpose:** Defines **where data lives** and how tables relate.

**Classic capabilities**

- One or more backend tables (multi-table tank)
- Datasource selection / credentials context
- Column cataloging
- **Join editor** (relationships between tables)
- Index/key awareness (unique index check for updater keys)

**Functional requirements**

|   ID   |                     Requirement                      |
|--------|------------------------------------------------------|
| BDT-01 | Select datasource(s) available to the server         |
| BDT-02 | Add/remove tables (or logical entities)              |
| BDT-03 | Define joins (type, left/right keys)                 |
| BDT-04 | Catalog columns for mapping/selector/value picker    |
| BDT-05 | Support multi-table tanks                            |
| BDT-06 | Validate key columns against unique indexes (update) |

**Modernization: pluggable datasource adapters**

Keep the **tank** abstraction; expand backends:

|   Adapter class   |                    Examples                    |
|-------------------|------------------------------------------------|
| Relational SQL    | Existing JDBC datasources, views, procs        |
| CMS repository    | Content items via JCR/API (read status/fields) |
| HTTP / REST       | External JSON APIs as “tables” of records      |
| Message / queue   | Optional async sink (update side)              |
| Object store / S3 | Binary resources                               |
| In-memory / mock  | Design-time test fixtures                      |

Each adapter still exposes: **entities**, **fields**, **join-like associations** (where meaningful), **query capability**, **write capability**.

### 5.3 Mapper (transform core)

**Purpose:** Bidirectional mapping between backend fields and document fields, with optional conditions and functions.

**UI behaviors (classic)**

- Left browser: tables + columns + UDFs
- Right browser: DTD elements/attributes
- Mapping grid: Backend ↔ XML
- Add / Clear / Remove
- **Guess** auto-map by name similarity
- **Return empty document** if no rows (query)
- Per-mapping **conditional properties**
- **Function properties** (UDF / SQL function usage in mapping)

**Functional requirements**

|   ID   |                      Requirement                      |
|--------|-------------------------------------------------------|
| MAP-01 | Create ordered list of field mappings                 |
| MAP-02 | Map backend field → document field (query direction)  |
| MAP-03 | Map document field → backend field (update direction) |
| MAP-04 | Support function/UDF as mapping source or transform   |
| MAP-05 | Attach conditions to a mapping (conditional apply)    |
| MAP-06 | Auto-suggest mappings by name (“Guess”)               |
| MAP-07 | Configure empty-result document policy                |
| MAP-08 | Validate mappings against current tank/schema         |
| MAP-09 | Support remove/clear/reorder mappings                 |

**Modernization**

- Mapping endpoints become **JSONPath / JMESPath / object paths** instead of only XPath-like XML fields
- Functions become a **function registry** (expression language: JEXL already exists in product, JS, or CEL)
- Conditions become first-class boolean expressions over the Value system
- Bidirectional mapping can split into **read-map** and **write-map** when formats diverge

### 5.4 Selector (query criteria)

**Purpose:** Define which backend rows to retrieve.

**Two modes**

1. **Structured WHERE table**
   - Variable (column or SQL function)
   - Operator
   - Value (via Value system)
   - Boolean linker AND/OR (AND precedence; no nesting in table mode)
   - Omit if null
   - Distinct results flag
2. **Manual SQL**
   - Full SELECT statement
   - Parameter binding: `:"PSXPARAM/Name"` style placeholders
   - Character data quoting rules for LIKE etc.

**Functional requirements**

|   ID   |                        Requirement                        |
|--------|-----------------------------------------------------------|
| SEL-01 | Build multi-clause filters with AND/OR                    |
| SEL-02 | Operators for comparison/match (product SQL op set)       |
| SEL-03 | Bind clause values from Value system (params, context, …) |
| SEL-04 | Omit clause when value null                               |
| SEL-05 | Distinct result option                                    |
| SEL-06 | Escape hatch: full manual query text with param tokens    |
| SEL-07 | Catalog SQL functions usable in variables                 |

**Modernization**

|      Classic       |                               Modern                               |
|--------------------|--------------------------------------------------------------------|
| WHERE table        | Structured filter AST (and/or/groups — **add nesting**)            |
| Manual SQL         | Adapter-native query (SQL, RSQL, GraphQL query, Elasticsearch DSL) |
| `PSXPARAM` tokens  | Named bind parameters with typed coercion                          |
| SQL-only functions | Adapter function packs + portable expression subset                |

### 5.5 Results Pager (query pagination)

|          Field           |                Meaning                 |
|--------------------------|----------------------------------------|
| Max rows per page        | Page size (`-1` = all)                 |
| Max pages                | Cap on pages processed (`-1` = all)    |
| Max displayed page links | UI link window for multi-page nav      |
| Sort table               | Ordered columns + ascending/descending |

|   ID   |                    Requirement                     |
|--------|----------------------------------------------------|
| PAG-01 | Configure page size and max pages                  |
| PAG-02 | Multi-column sort specification                    |
| PAG-03 | Expose paging metadata to consumers (links/counts) |

**Modernization:** Cursor/offset/limit APIs; `Link` headers; GraphQL connection model — same FR, different wire shape.

### 5.6 Updater properties (write policy)

|             Control              |                    Meaning                    |
|----------------------------------|-----------------------------------------------|
| Allow creates                    | Inserts permitted                             |
| Allow updates                    | Updates permitted                             |
| Allow deletes                    | Deletes permitted                             |
| Key columns table                | Identity columns for matching rows            |
| Check key against unique indexes | Validate keys exist as uniqueness constraints |

|   ID   |                    Requirement                    |
|--------|---------------------------------------------------|
| UPD-01 | Independently enable insert/update/delete         |
| UPD-02 | Define key columns for row identity               |
| UPD-03 | Validate keys against backend uniqueness metadata |
| UPD-04 | Reject/disable operations not allowed             |

### 5.7 Transaction Manager

|       Mode        |                   Meaning                   |
|-------------------|---------------------------------------------|
| One row at a time | Atomicity per row across multi-table writes |
| All rows together | Atomicity for entire batch                  |

|   ID   |                             Requirement                             |
|--------|---------------------------------------------------------------------|
| TXN-01 | Choose transaction scope: per-row vs entire result set              |
| TXN-02 | Multi-table updates participate in same transaction when configured |

### 5.8 Exits / hooks (the extensibility spine)

Classic exit attachment points:

|       Phase       | Interface category (Workbench) |                      Role                       |
|-------------------|--------------------------------|-------------------------------------------------|
| Pre-process       | `IPSRequestPreProcessor`       | Mutate/validate request before pipe             |
| Post-process      | `IPSResultDocumentProcessor`   | Mutate result document after pipe               |
| UDF               | `IPSUdfProcessor`              | Functions usable in mapper/selector value space |
| Java exit figures | Pre/Post Java Exit on canvas   | Ordered extension calls with parameters         |

**Exit Properties:** parameter name/value pairs; values via Value Selector; optional descriptions.

|   ID   |                          Requirement                          |
|--------|---------------------------------------------------------------|
| HOK-01 | Attach ordered pre-hooks to resource/application              |
| HOK-02 | Attach ordered post-hooks to resource/application             |
| HOK-03 | Pass typed/named parameters into hooks from Value system      |
| HOK-04 | Register UDF/functions callable from mappings/filters         |
| HOK-05 | Conditional execution of hooks where supported                |
| HOK-06 | Fail pipeline or continue based on hook error policy (define) |

**Modernization: first-class hook model**

Suggested lifecycle events for a revived engine:

```
onRequestReceived
onBeforeQuery / onBeforeWrite
onAfterQuery / onAfterWrite
onMapField (optional fine-grained)
onError
onResponseReady
```

Hook runtimes: Java extensions (existing), JavaScript, HTTP webhooks, serverless functions, script sandboxes — **same FR, multiple runners**.

### 5.9 Binary / non-text resources

|   ID   |                  Requirement                  |
|--------|-----------------------------------------------|
| BIN-01 | Define resource that returns binary payload   |
| BIN-02 | Bind to backend binary column or file storage |
| BIN-03 | Set content type / disposition behavior       |

### 5.10 Result pages / stylesheets (optional presentation)

|   ID   |                           Requirement                           |
|--------|-----------------------------------------------------------------|
| PRE-01 | Associate one or more presentation transforms with query output |
| PRE-02 | Edit stylesheet/source artifacts in application files           |
| PRE-03 | Allow raw structured response with **no** presentation layer    |

**Modernization:** Presentation becomes optional. API-first resources return JSON; HTML/PDF/etc. are separate view bindings.

### 5.11 Encryptor / security / notifier

|    Component     |                    FR intent                    |
|------------------|-------------------------------------------------|
| Encryptor figure | Configure encryption requirements for pipe data |
| App security     | Who may design/run application resources        |
| Notifier         | Notification side-effects configuration         |

Retain as capability buckets even if UI consolidates.

### 5.12 Cache settings (query)

|   ID   |                             Requirement                             |
|--------|---------------------------------------------------------------------|
| CCH-01 | Configure caching for query resource results                        |
| CCH-02 | Enable/disable and parameterize cache behavior without code changes |

### 5.13 Tracing / debugging

|   ID   |                      Requirement                       |
|--------|--------------------------------------------------------|
| DBG-01 | Enable tracing on application                          |
| DBG-02 | Adjust tracer types/flags while running                |
| DBG-03 | Start/stop tracing; view/collect trace output          |
| DBG-04 | Correlate traces to resource/pipe stage where possible |

---

## 6. Value system (expression operands)

The **Value Selector** is the shared operand model used in mappings, selector values, exit parameters, conditions, and functions.

### 6.1 Value types (inventory)

From `DT*` classes + help:

|               Type                |                   Description                    |            Modern notes             |
|-----------------------------------|--------------------------------------------------|-------------------------------------|
| Backend Column                    | Column from resource tanks                       | Entity field                        |
| XML Field                         | Field from page tank                             | JSON path / schema field            |
| HTML Parameter                    | Multi-valued request param                       | Query/body param (array)            |
| Single HTML Parameter             | Single-valued request param                      | Scalar param                        |
| CGI Variable                      | Server CGI/environment                           | Request headers / env / server vars |
| Cookie                            | Named cookie                                     | Cookie                              |
| User Context                      | Authenticated user attributes                    | Security principal claims           |
| Content Item Data                 | CMS item field values (may be collection)        | Repository field binding            |
| Content Item Status               | Workflow/status tables (e.g. state name/id)      | CMS status projection               |
| Relationship Property             | Relationship property values                     | Graph edge attributes               |
| Originating Relationship Property | Originating relationship context                 |                                     |
| Macro                             | Named macro expansion                            | Shared named expressions            |
| Text Literal                      | String constant                                  |                                     |
| Numeric Literal                   | Number constant                                  |                                     |
| Date Literal                      | Date constant (SQL-oriented syntax historically) | ISO-8601                            |
| UDF / Function call               | Computed value                                   | Expression/function registry        |

### 6.2 Value system FR

|   ID   |                            Requirement                             |
|--------|--------------------------------------------------------------------|
| VAL-01 | Pick value by type + cataloged instance                            |
| VAL-02 | Enter literals with type coercion                                  |
| VAL-03 | Reference request inputs (params, headers, cookies)                |
| VAL-04 | Reference security/user context                                    |
| VAL-05 | Reference CMS item/status when in CMS context                      |
| VAL-06 | Reference backend and document fields in-scope                     |
| VAL-07 | Invoke registered functions with nested values as args             |
| VAL-08 | Support collections (multi-value) with documented processing rules |

**Parameter binding in manual SQL (classic):**  
`CONTENTSTATUS.CONTENTID=:"PSXPARAM/contentid"`  
Modern equivalent: named binds `:contentid` with type metadata.

---

## 7. Catalog services (design-time intelligence)

The designer is catalog-driven. Inventory of catalogers (non-exhaustive but representative):

|              Cataloger              |            Feeds             |
|-------------------------------------|------------------------------|
| Datasources                         | Backend tank datasource list |
| Tables / owners                     | Table picker                 |
| Backend columns / extended columns  | Mapping + selector           |
| Indices                             | Key validation               |
| Database functions                  | Selector/mapper functions    |
| DTDs / XML fields                   | Page tank + mapping          |
| HTML params                         | Value picker                 |
| CGI variables                       | Value picker                 |
| Cookies                             | Value picker                 |
| User context                        | Value picker                 |
| Macros                              | Value picker                 |
| UDFs / server exits                 | Function + hook pickers      |
| Mime types                          | Binary/result config         |
| Locales                             | i18n-aware design            |
| Content editor fields / item status | CMS-aware values             |
| Workflow content types / workflows  | CMS context                  |

|   ID   |                        Requirement                        |
|--------|-----------------------------------------------------------|
| CAT-01 | All pickers load live metadata from connected server      |
| CAT-02 | Catalogs refresh after schema/datasource changes          |
| CAT-03 | Adapter-specific catalogs plug into same picker framework |

---

## 8. Conditions and functions

### 8.1 Conditional mappings / rules

- Conditional property dialogs attach predicates to mappings (and related constructs)
- Boolean composition for when a mapping/hook applies

|   ID   |                  Requirement                   |
|--------|------------------------------------------------|
| CND-01 | Define condition expressions over Value system |
| CND-02 | Apply conditions to mappings                   |
| CND-03 | Apply conditions to hooks where supported      |

### 8.2 Functions / UDFs

- SQL functions catalog for selector variables
- User-defined functions for mapper
- Function properties dialog for configuration

|  ID   |               Requirement                |
|-------|------------------------------------------|
| FN-01 | Browse built-in functions per adapter    |
| FN-02 | Register custom functions (UDF/exits)    |
| FN-03 | Configure function args via Value system |

---

## 9. Application process canvas behaviors

|   ID    |                           Requirement                           |
|---------|-----------------------------------------------------------------|
| UI-P-01 | Visual or structured editing of resources within an application |
| UI-P-02 | Open resource editor (pipe design) from resource node           |
| UI-P-03 | Connect related figures (request flow / result pages)           |
| UI-P-04 | Drag-drop create resources from HTML/DTD/table/file sources     |
| UI-P-05 | Edit associated text artifacts (XSL, HTML) in side tabs/editors |
| UI-P-06 | Cut/copy/paste figures                                          |
| UI-P-07 | Validate design before save/start                               |

**Note:** The classic UI is a freeform figure canvas (`UIAppFrame`, `UIPipeFrame`). A modern UI may use **node graph**, **ordered stage list**, or **YAML/JSON pipeline document** — FR is the object graph, not Swing.

---

## 10. Serialization & object model anchors

Core persisted types (`com.percussion.design.objectstore`):

|                          Type                          |           Role            |
|--------------------------------------------------------|---------------------------|
| `PSApplication`                                        | Root application document |
| `PSApplicationFile`                                    | File artifacts in app     |
| `PSDataSet`                                            | Resource definition       |
| `PSPipe` / `PSQueryPipe` / `PSUpdatePipe`              | Pipe specialization       |
| `PSPageDataTank`                                       | Document schema side      |
| `PSBackEndDataTank`                                    | Backend tables            |
| `PSBackEndJoin` / `PSBackEndTable` / `PSBackEndColumn` | Relational structure      |
| `PSDataMapper`                                         | Mappings collection       |
| `PSDataSelector`                                       | Query selection           |
| `PSResultPager`                                        | Paging/sort               |
| `PSRequestor`                                          | Request interface         |
| `PSConditionalExit`                                    | Conditional exit calls    |
| `PSApplicationFlow`                                    | App flow concerns         |

Designer wrappers (`OS*` classes) adapt these for the visual editor without changing the server objectstore semantics.

**Modernization implication:** Introduce a **versioned pipeline IR** (intermediate representation) that can:

1. Import classic `PSApplication` XML for migration
2. Export classic form for compatibility (optional)
3. Native-save a cleaner JSON IR for new apps

---

## 11. End-to-end scenarios (acceptance)

### Scenario P1 — JSON read API over SQL

1. Create application `orders-api`
2. Create query resource `listOrders`
3. Page tank = JSON Schema for `OrderList`
4. Backend tank = `ORDERS` + `ORDER_LINES` with join
5. Selector: `status = :status` AND optional date range
6. Mapper: columns → JSON fields; compute `total` via function
7. Pager: 50 rows, sort by `orderDate` desc
8. Post-hook: redact PII for non-admin roles
9. Deploy/start; `GET /apps/orders-api/listOrders?status=OPEN` returns JSON

### Scenario P2 — JSON write API with transaction

1. Update resource `submitOrder`
2. Page tank = `Order` JSON Schema
3. Backend multi-table tank
4. Mapper document → columns
5. Updater: allow create+update; keys = `orderId`
6. Transaction: all rows together
7. Pre-hook: validate payload; stamp `createdBy` from user context
8. `POST` JSON creates header+lines atomically

### Scenario P3 — CMS-aware integration query

1. Query resource joins backend table with Content Item Status values
2. Selector uses HTML/JSON param `contentid`
3. Mapper includes workflow state name
4. Used by external system to poll publishability

### Scenario P4 — External REST datasource (modern)

1. Backend tank adapter type = HTTP
2. Entity = remote collection `https://erp.example/api/items`
3. Mapper ERP JSON → response schema
4. Cache settings enabled
5. Same exit/hook model as SQL resources

### Scenario P5 — Migration of legacy XML app

1. Import existing application XML
2. IR shows pipes/resources
3. Designer switches page tank from DTD to JSON Schema (mapping assist)
4. Dual-run or cutover; export still possible for rollback

---

## 12. Functional requirements summary matrix

|               Area                |   Classic completeness   |        Revive priority         |
|-----------------------------------|--------------------------|--------------------------------|
| App lifecycle start/stop/export   | Full                     | P0                             |
| Query pipe (tank/map/select/page) | Full                     | P0                             |
| Update pipe (map/updater/txn)     | Full                     | P0                             |
| Value system                      | Full                     | P0                             |
| Hooks/exits + UDF                 | Full                     | P0                             |
| Binary resources                  | Full                     | P1                             |
| Join editor                       | Full                     | P1                             |
| Guess mapping                     | Full                     | P1                             |
| Cache + tracing                   | Full                     | P1                             |
| XSL presentation                  | Full                     | P2 (optional path)             |
| Freeform Swing canvas             | Full                     | **Replace** (not parity-bound) |
| JSON schema tanks                 | Absent                   | **P0 modernization**           |
| Nested boolean filter groups      | Weak (manual SQL escape) | **P0 modernization**           |
| Non-SQL datasources               | Absent/limited           | **P0–P1 modernization**        |
| OpenAPI publish                   | Absent                   | **P1 modernization**           |
| Webhook hooks                     | Absent                   | **P1 modernization**           |

---

## 13. Modernization blueprint (normative intent, tool-agnostic)

### 13.1 Preserve (engine semantics)

1. Application as deployable unit of many resources
2. Resource as single endpoint with query or update orientation
3. Explicit schema tank + datasource tank + mapper
4. Declarative filters with expression values
5. Write policies + keys + transactions
6. Ordered hooks around the pipe
7. Design-time catalogs
8. Runtime activate/deactivate + diagnostics

### 13.2 Replace (incidental legacy)

|         Legacy choice          |                  Replacement direction                   |
|--------------------------------|----------------------------------------------------------|
| XML/DTD as only document model | Multi-schema: JSON Schema first; XML optional            |
| XSL as primary presentation    | Optional views; default raw structured I/O               |
| HTML parameters only           | Unified request binding: query, path, headers, JSON body |
| SQL as only query language     | Adapter-native queries + portable filter AST             |
| Swing figure canvas            | Graph or form-based pipeline editor in Developer module  |
| CGI variable naming            | HTTP semantics (headers, method, path)                   |
| Application XML as sole IR     | Versioned JSON IR + importers                            |

### 13.3 Target IR sketch (illustrative, not final schema)

```yaml
application:
  name: orders-api
  resources:
    - name: listOrders
      kind: query
      request:
        method: GET
        path: /orders
        params: [status, fromDate]
      pageTank:
        type: jsonSchema
        ref: schemas/OrderList.json
      backendTank:
        adapter: sql
        datasource: default
        entities: [ORDERS, ORDER_LINES]
        joins: [{left: ORDERS.ID, right: ORDER_LINES.ORDER_ID, type: inner}]
      selector:
        mode: structured
        where:
          - {field: ORDERS.STATUS, op: eq, value: {type: param, name: status}}
      mapper:
        - {from: ORDERS.ID, to: $.orders[*].id}
        - {from: ORDERS.STATUS, to: $.orders[*].status}
      page:
        size: 50
        sort: [{field: ORDERS.ORDER_DATE, dir: desc}]
      hooks:
        post: [redactPii]
```

### 13.4 Suggested product naming

Avoid leading with “XML Application” in the new world:

- **Integration Pipelines**
- **Data Services**
- **Request Pipelines**
- **Application Pipelines**

Keep “XML Application” as **legacy compatibility mode** label.

---

## 14. Relationship to CMS Developer module

|   Concern    |                 Content/Assembly design                  |                            Pipeline engine                            |
|--------------|----------------------------------------------------------|-----------------------------------------------------------------------|
| Primary user | CMS implementer modeling content                         | Integrator/API builder                                                |
| Artifact     | Content types, templates, slots                          | Applications/resources/pipes                                          |
| Runtime      | Editors, assembly, CX                                    | HTTP endpoints + hooks                                                |
| Overlap      | Content Item Data/Status values; extensions; datasources | Can call CMS data; CE historically also used pipe concepts internally |

**Recommendation:** Implement as a **first-class Developer module section** (e.g. “Pipelines” / “Data Services”) alongside Content/Assembly — not a buried legacy XML Server tab. Shared platform: connection session, extension registry, ACL, locking, problems/validation.

Content Editor historically shares DNA (pipes/mappers/exits for item transforms). A modern pipeline IR may eventually unify “item save pipeline” and “integration pipeline” conceptually — out of scope for v1 inventory, but architecturally attractive.

---

## 15. Source anchors

|              Topic               |                         Location                          |
|----------------------------------|-----------------------------------------------------------|
| Visual designer package overview | `Designer/Src/com/percussion/E2Designer/package.html`     |
| App figures                      | `AppFigureFactory.java`                                   |
| Pipe figures                     | `PipeFigureFactory.java`                                  |
| Query/Update pipe wrappers       | `OSQueryPipe.java`, `OSUpdatePipe.java`                   |
| Mapper UI                        | `MapperPropertyDialog.java`, help `13296.htm`             |
| Selector UI                      | `SelectorPropertyDialog.java`, help `13297.htm`           |
| Value types                      | `DT*.java`, help `Using_the_Value_Selector.htm`           |
| Workbench host                   | `PSXmlApplicationEditor.java`                             |
| Objectstore                      | `src/com/percussion/design/objectstore/PS*.java`          |
| Help TOC branch                  | “Maintaining XML Applications” in workbench `toc.xml`     |
| Resource create modes            | help `13373.htm` et al.                                   |
| Lifecycle                        | help `13411.htm` (save/start/stop), export/tracing topics |

---

## 16. Prioritized delivery slices (for `percussioncms`)

### Slice A — IR + runtime (no fancy designer)

- Load/save pipeline IR
- Execute query/update against SQL adapter
- JSON request/response
- Pre/post hooks (Java)
- Import subset of classic applications

**Server code map (this repo):** see [data-pipeline-server-runtime-map.md](./data-pipeline-server-runtime-map.md) for where query/update handlers live today (`PSApplicationHandler` → `PSQueryHandler` / `PSUpdateHandler`).

### Slice B — Developer UI (structured editor)

- Form/graph editor for tanks, mapper grid, selector builder
- Catalog pickers
- Deploy start/stop
- Problems/validation

### Slice C — Modern adapters & DX

- REST datasource adapter
- OpenAPI generation from resources
- Webhook hooks
- Nested filter groups
- Test-invoke from UI with sample payloads

### Slice D — Legacy depth

- Full binary resources
- XSL/result page compatibility
- Advanced tracing UI parity
- 100% classic visual metaphor (only if users demand)

---

## 17. Risks and open questions

1. **Runtime coupling:** How much execution engine lives only in classic Rhythmyx request handlers vs reusable services? (Needs server-side follow-up in `src` request handler stack.)
2. **Security:** Manual SQL escape hatch is powerful and dangerous — modern product needs parameterization defaults and least-privilege datasources.
3. **Parity vs progress:** Pixel-parity with E2Designer is the wrong goal; behavioral parity of pipe semantics is the right goal.
4. **CE overlap:** Decide whether content-type item pipelines remain separate forever or converge on same IR.
5. **Multi-doc types:** Classic dataset = one document type; modern APIs may want multiple response shapes per resource (content negotiation).

---

## Document history

| Version |    Date    |                                                       Notes                                                        |
|---------|------------|--------------------------------------------------------------------------------------------------------------------|
| v1      | 2026-07-28 | Full reverse engineering of E2Designer/XML Application pipeline + modernization framing for JSON/datasources/hooks |

