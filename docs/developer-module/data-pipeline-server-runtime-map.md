# Data Pipeline Engine — Server Runtime Map (`percussioncms`)

| Field | Value |
|-------|--------|
| **Purpose** | Locate where classic XML Application / query-update **pipe** semantics execute in this repo (vs design-time E2Designer / objectstore only) |
| **Audience** | Engineering estimating **reuse vs reimplement** for Slice A (IR + SQL runtime + JSON I/O) |
| **Companion** | [data-pipeline-engine-inventory.md](./data-pipeline-engine-inventory.md) (functional inventory + modernization brief) |
| **SPA surface today** | Developer **Pipelines** catalog is list-only: `GET /services/pipelines` → classic **XML Application** summaries (`PipelinesAdaptor` / `PipelinesResource`) — not execution |

---

## 1. One-line answer

**Runtime lives under `system/src/main/java/com/percussion/{server,data}`**, not under Designer or the REST catalog.

HTTP/app requests hit **`PSApplicationHandler`**, which dispatches per-dataset to **`PSQueryHandler`** or **`PSUpdateHandler`** (both extend **`PSDataHandler`**). Design-time pipe objects (`PSQueryPipe`, `PSUpdatePipe`, mapper, tanks, …) live in **`com.percussion.design.objectstore`** and are **compiled into execution plans** when handlers are constructed — they are not re-interpreted from Swing at request time.

---

## 2. Call chain (classic request)

```
Client HTTP request (app resource URL / internal request)
  → server routing / app lookup
  → com.percussion.server.PSApplicationHandler
       • ACL / encryptor / tracing / start-stop lifecycle
       • owns dataset → handler map for the loaded PSApplication
  → com.percussion.data.PSDataHandler  (abstract)
       • pre-processors (IPSRequestPreProcessor)
       • validation / auth helpers shared by query & update
       • result-document processors (IPSResultDocumentProcessor)
  → either:
       • PSQueryHandler   — read path (Selector → SQL plan → map → document)
       • PSUpdateHandler  — write path (XML/doc → statements → backend)
  → back-end JDBC / statement builders (same package)
  → response converters (XML/HTML/MIME under com.percussion.data)
```

### Entry / dispatch (server)

| Class | Path | Role |
|-------|------|------|
| `PSApplicationHandler` | `system/src/main/java/com/percussion/server/PSApplicationHandler.java` | Per-application runtime: load `PSApplication`, register request handlers, start/stop, stats, ACL, cache hooks. Imports `PSQueryHandler`, `PSUpdateHandler`, `PSDataHandler`, `PSQueryPipe`, `PSUpdatePipe`, `PSDataSet`. |
| `PSApplicationStatistics` | `system/.../server/PSApplicationStatistics.java` | Runtime counters for app activity. |
| App listeners / objectstore server | `com.percussion.design.objectstore.server.*` | Persist/load applications into the running server (design objectstore, not pipe UI). |

### Data handlers (request execution)

| Class | Path | Role |
|-------|------|------|
| `PSDataHandler` | `system/src/main/java/com/percussion/data/PSDataHandler.java` | Abstract base implementing `IPSRequestHandler` + `IPSInternalRequestHandler`. Shared pre/post extension runs, auth errors, logging. |
| `PSQueryHandler` | `system/.../data/PSQueryHandler.java` | Query resource execution: optimizer → execution plan → column mappers → result converter. Constructed from a `PSDataSet` (pipes + page tank + requestor + pager). |
| `PSUpdateHandler` | `system/.../data/PSUpdateHandler.java` | Insert/update/delete resource execution: transaction sets, update statements, statement column mappers, multi-row XML document processing. |

### Supporting runtime (same package, non-exhaustive)

| Area | Representative classes | Notes |
|------|------------------------|--------|
| SQL / joins | `PSQueryOptimizer`, `PSQueryStatement`, `PSQueryJoiner`, `PSJoinTree`, `PSNativeStatement`, `PSOptimizer` | Query plan building and execution |
| Writes | `PSOracleUpdateBuilder`, `PSOracleInsertBuilder`, `PSLockedUpdateStatement`, … | DB-specific update/insert builders |
| Mapping extractors | `PSDataExtractor*`, `PSBackEndColumnExtractor`, `PSHtmlParameterExtractor`, `PSFunctionCallExtractor`, … | Value extraction for map/selector/exit params |
| Conditions / rules | `PSConditionalEvaluator`, `PSRuleEvaluator`, `PSFieldValidationRulesEvaluator` | Conditional mapping / validation |
| Extensions | `PSExtensionRunner`, `PSConditionalExtensionRunner` | Pre/post exits & UDFs at request time |
| Results | `PSResultSet*`, `PSResultSetXmlConverter`, `PSResultSetHtmlConverter`, `PSCssStyleSheetMerger` | Result shaping (classic XML/HTML bias) |
| Content Editor overlap | `PSContentEditorHandler` (cms.handlers), `PSContentEditorPipe` (objectstore) | CE reuses pipe DNA for item save; separate product surface |

### Design objectstore (compiled inputs, not the runtime)

| Class | Path | Role |
|-------|------|------|
| `PSApplication` | `system/.../design/objectstore/PSApplication.java` | Deployable unit |
| `PSDataSet` | `.../PSDataSet.java` | One request resource (query or update) |
| `PSPipe` / `PSQueryPipe` / `PSUpdatePipe` | `.../PS*Pipe.java` | Pipe spine definitions |
| Mapper / tanks / selector / pager / updater | sibling `PS*` types in objectstore | Design IR for E2Designer + what handlers compile |

---

## 3. How this maps to inventory concepts

From [data-pipeline-engine-inventory.md](./data-pipeline-engine-inventory.md) §2:

| Design concept | Runtime home (this repo) |
|----------------|--------------------------|
| Application start/stop | `PSApplicationHandler` lifecycle + server app registry |
| Dataset / requestor | `PSDataSet` + handler registration in `PSApplicationHandler` |
| Query pipe | `PSQueryHandler` + optimizer/statement stack |
| Update pipe | `PSUpdateHandler` + transaction/statement stack |
| Mapper | Compiled into extractors / column mappers at handler build time |
| Selector | Query plan / criteria in query optimizer path |
| Pre/post exits | `PSDataHandler` extension runners (`IPSRequestPreProcessor`, `IPSResultDocumentProcessor`) |
| UDFs | Extension manager + function extractors (`PSFunctionCallExtractor`, …) |
| Result pages / XSL | Result converters + stylesheet merge under `com.percussion.data` |
| Content Editor pipes | `PSContentEditorHandler` / `PSContentEditorPipe` (parallel, item-centric) |

---

## 4. What the Developer SPA “Pipelines” tab is (and is not)

| Layer | Location | Capability today |
|-------|----------|------------------|
| REST list | `rest/.../pipelines/PipelinesResource.java` | List classic applications (optional filter/page) |
| Adaptor | `projects/sitemanage/.../PipelinesAdaptor.java` | Summaries only |
| SPA | `WebUI/.../developer/PipelinesPanel.tsx` | Catalog table |
| **Runtime** | This map | **Not exposed** to SPA; no start/stop, invoke, or IR editor |

Implication for Slice A: **catalog + runtime are separate workstreams**. Reuse candidates are primarily `com.percussion.data.*` handlers and objectstore models; the SPA catalog is a thin discovery UI.

---

## 5. Reuse vs reimplement (engineering notes)

### Likely reusable (with care)

- **Objectstore model** (`PSApplication`, `PSDataSet`, pipes, mapper definitions) as a **source IR** for import.  
- **SQL builder / join / statement** machinery under `com.percussion.data` if Slice A stays SQL-first.  
- **Extension hook points** (pre/post processors, UDF interfaces) if modern IR keeps the same lifecycle.  
- **Application load/start/stop** patterns from `PSApplicationHandler` (even if HTTP surface changes).

### Hard coupling / modernization friction

- Request/response is **classic server `PSRequest` / XML document** oriented — not first-class JSON.  
- Result converters favor **XML/HTML/XSL**.  
- Handlers are **internal** (`processRequest`) and tightly bound to app ACL, session, and CMS request plumbing.  
- Oracle-named builders and historical DB forks suggest **adapter cleanup** before multi-DB product claims.  
- Manual SQL escape hatches (inventory risk §17) remain security-sensitive.

### Practical Slice A recommendation

1. **Import path:** classic application XML/objectstore → new IR (document), not “call PSQueryHandler from REST” as the long-term API.  
2. **Execution path (v1):** either  
   - **wrap** a subset of query/update handler entry points behind a new service boundary (faster demo, high coupling), or  
   - **rehost** optimizer/mapper ideas against a clean SQL adapter + JSON document tank (slower, healthier).  
3. Do **not** plan on E2Designer/Swing for runtime; design UI is Slice B.  
4. Keep CE (`PSContentEditorHandler`) out of v1 scope unless item pipelines are an explicit goal.

---

## 6. Suggested next probes (not done in this doc)

These are the highest-value code-reading follow-ups before locking Slice A estimates:

1. `PSApplicationHandler` method that **constructs** `PSQueryHandler` / `PSUpdateHandler` from each `PSDataSet` (handler map population).  
2. `PSQueryHandler` constructor → `PSQueryOptimizer` plan object graph.  
3. `PSUpdateHandler` transaction set build vs multi-table XML example in class javadoc.  
4. Internal request path: `IPSInternalRequest` / `IPSInternalRequestHandler` used by CMS features that **call apps as services**.  
5. Where applications are **loaded from disk/DB** into the running server (objectstore server handlers).

---

## 7. Document history

| Version | Date | Notes |
|---------|------|-------|
| v1 | 2026-07-29 | Initial server runtime map from `percussioncms` tree: `PSApplicationHandler` → `PSDataHandler` → `PSQueryHandler` / `PSUpdateHandler` + package inventory |
