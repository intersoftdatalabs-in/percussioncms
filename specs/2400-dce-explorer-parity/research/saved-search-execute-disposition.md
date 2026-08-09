# Saved search execute disposition (REST inventory)

**Parent epic:** [#2409](https://github.com/intersoftdatalabs-in/percussioncms/issues/2409) (slice of [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400))  
**This slice:** [#2504](https://github.com/intersoftdatalabs-in/percussioncms/issues/2504) — **(A) REST inventory + execute façade disposition**  
**Follow-ups:** [#2505](https://github.com/intersoftdatalabs-in/percussioncms/issues/2505) (B REST execute), [#2506](https://github.com/intersoftdatalabs-in/percussioncms/issues/2506) (C Explorer picker UI), [#2507](https://github.com/intersoftdatalabs-in/percussioncms/issues/2507) (D Playwright)  
**Inventory date:** 2026-08-09  
**Status:** Disposition complete — **no product UI or execute endpoint in this PR**.

## Scope

Map existing public REST + sitemanage + WebUI surfaces for CX **design** searches (`SearchDef`) and Explorer **runtime** search, then decide how Explorer should **execute** a saved/design search:

| Option | Meaning |
|--------|---------|
| **Reuse** | Client maps `SearchDef` fields → existing Explorer runtime criteria (`PSSearchCriteria` / `searchExtended`); **no** new public REST |
| **Façade** | New public execute endpoint under `rest/searches` (slice B implements full companion closure) |
| **Hybrid** | Extend design detail DTO only + client maps to existing runtime execute |

---

## Decision (executive)

| | |
|--|--|
| **Disposition** | **Façade** |
| **Rationale** | DCE runs a full `PSSearch` design object (field operators, display format, max results, case sensitivity, search mode). Public design catalog already exposes operators on `SearchFieldSummary`, but the existing Explorer runtime path (`POST …/searchmanagement/search/get/extendedresults` + `PSSearchCriteria`) only accepts `searchFields: Map<field, value>` and **hard-codes operator `=`** in `PSSearchService`. Client-side reuse would silently lose non-equal operators (`like`, `in`, `between`, null checks, etc.) and force fragile mapping of design metadata that already lives server-side. A dedicated public execute endpoint reuses the design load already in `SearchAdaptor` and returns Explorer-ready paged item properties. |
| **Slice B** | Implement façade per signature sketch below; do **not** implement in this docs PR. |
| **Slice C** | Catalog via existing `GET /rest/searches` + execute via new façade; keep free-text `searchExtended` for ad-hoc queries. |

---

## Inventory — public design catalog (`rest/searches`)

Read-only CX search design catalog for Developer UI-06. **No write** and **no execute** today.

### Endpoints

| Method | Path (servlet context) | Resource | Capability |
|--------|------------------------|----------|------------|
| `GET` | `/rest/searches` (WebUI client: `/services/searches`) | `SearchResource.listSearches` | List design searches (not views) |
| `GET` | `/rest/searches/{idOrName}` | `SearchResource.getSearch` | Detail by name, GUID string, or numeric id; field criteria when present |

**Module paths:**

| Layer | Path |
|-------|------|
| Resource | `rest/src/main/java/com/percussion/rest/searches/SearchResource.java` |
| Adaptor interface | `rest/src/main/java/com/percussion/rest/searches/ISearchAdaptor.java` |
| Wire DTOs | `SearchDef.java`, `SearchFieldSummary.java` (same package) |
| Mockito resource tests | `rest/src/test/java/com/percussion/rest/searches/SearchResourceTest.java` |
| Spring test stub | `rest/src/test/java/com/percussion/rest/test/apibridge/TestSearchAdaptor.java` |
| Production adaptor | `projects/sitemanage/src/main/java/com/percussion/apibridge/SearchAdaptor.java` |
| Adaptor unit tests | `SearchAdaptorMapTest.java`, `SearchAdaptorSafeKeyTest.java` under `projects/sitemanage/src/test/java/com/percussion/apibridge/` |

### `ISearchAdaptor` surface (today)

```text
List<SearchDef> listSearches();
SearchDef findSearchByKey(String idOrName);  // null if missing/unsafe
```

### `SearchDef` / `SearchFieldSummary` (wire)

`SearchDef` meta: guid, id, name, label, description, type, displayFormatId, url, parentCategory, maximumResultSize, userSearch / customSearch / standardSearch, userCustomizable, caseSensitive, fields[], designGaps[].

`SearchFieldSummary` per criterion: fieldName, displayName, **operator**, fieldValue, fieldType, position.

**Design gaps** stamped by adaptor (explicit non-goals of this catalog):

- Search create / update / delete not supported via this API  
- Search field criterion editing not supported via this API  
- Views are a separate catalog (Developer Views / UI-07)

### Production mapping

`SearchAdaptor` → `IPSUiDesignWs.findSearches` + `loadSearches` → `PSSearch` → `toDef` / `mapFields` (`PSSearchField` → `SearchFieldSummary`).  
Key safety: `isSafeSearchKey` rejects blank, `..`, `/`, `\`, NUL (path-injection hygiene on `idOrName`).

HTTP behavior (resource): 200 list/detail; 404 not found; 503 adaptor missing; 500 unexpected.

---

## Inventory — Explorer runtime search (sitemanage, not public `rest`)

Used today by modern Content Explorer free-text search.

| Piece | Location / path | Notes |
|-------|-----------------|-------|
| REST | `PSSearchRestService` `@Path("/search")` under searchmanagement | `POST /get`, `POST /get/extendedresults` |
| Service | `IPSSearchService` / `PSSearchService` | Lucene + field map → `PSWSSearchField` |
| Criteria DTO | `PSSearchCriteria` (`SearchCriteria` root) | query, searchType, startIndex, maxResults, sortColumn, sortOrder, formatId, **searchFields Map&lt;String,String&gt;**, folderPath |
| WebUI client | `WebUI/src/main/ts/api/contentExplorer/searchApi.ts` | `searchExtended` → `PATHS.FINDER_SEARCH_EXTENDED` |
| Paths constant | `WebUI/src/main/ts/api/paths.ts` | `…/searchmanagement/search/get/extendedresults` |
| Panel | `WebUI/src/main/ts/contentExplorer/SearchPanel.tsx` | Free-text only; injectible `search` prop for tests |
| Shell | `ContentExplorerShell.tsx` | Toggles `SearchPanel`; passes optional `folderPath` as initialCriteria |
| Vitest | `SearchPanel.test.tsx`, `searchApi.test.ts` | US5 coverage |
| Playwright | `modules/perc-qa-automation` us5-search (992) | Free-text panel; no saved-search picker |

### Critical runtime constraint (operator loss)

In `PSSearchService.searchForIds` / `getContentIdsForSearchByStatus`, each map entry becomes:

```text
new PSWSSearchField(fieldName, "=", value, CONN_ATTR_AND)
```

Operators on design fields are **not** accepted on this wire. Only equality is applied.  
`validateSearchCriteria` also requires a numeric `formatId` for id-based search paths (throws if blank).

Explorer `SearchPanel` currently only sets `query` (+ optional folder scope, pagination defaults). It never posts `searchFields` or design operators.

---

## Inventory — WebUI design consumers (Developer, read-only)

| Piece | Path | Role |
|-------|------|------|
| Client | `WebUI/src/main/ts/api/developer/searchesApi.ts` | `listSearches`, `getSearchDetail` |
| Types | `WebUI/src/main/ts/api/developer/types.ts` | `SearchDef`, `SearchFieldSummary` |
| Paths | `PATHS.SEARCHES` → `/services/searches` | |
| List UI | `WebUI/src/main/ts/developer/SearchesPanel.tsx` | Catalog table → detail |
| Detail UI | `WebUI/src/main/ts/developer/SearchDetailPanel.tsx` | Meta + fields table (operator/value) |
| Shell | `DeveloperShell.tsx` | Mounts `SearchesPanel` |
| Tests | `SearchesPanel.test.tsx`, `SearchDetailPanel.test.tsx` | Mockito-free Vitest with mocked API |

**No execute** from Developer catalog UI (read-only by design).

---

## Inventory — DCE execute path (parity target)

| Piece | Path | Role |
|-------|------|------|
| Catalog load | `PSSearchViewCatalog.loadSearches` | Loads full `PSSearch` component XML via CMS component processor |
| Run node children | `PSSearchViewActionManager` | Resolves saved/standard search id → `PSSearch`; builds `PSExecutableSearch` |
| Execute | `PSExecutableSearch.executeSearch` | Runs design search with display format, search mode (simple/advanced), case sensitivity, folder inclusion, synonym props; produces result nodes for the CE tree |
| Dialog | `PSSearchDialog` | Edit/run simple vs advanced; can save searches |

DCE does **not** rehydrate criteria from a flattened field-name→value map alone. It holds the **design object** and executes it through the CX search pipeline (`cxSearch` / related params).

Saved search types in CE nodes include `TYPE_SAVE_SRCH`, `TYPE_STANDARD_SRCH`, `TYPE_NEW_SRCH` (new/ad-hoc).

---

## Gap: design `SearchDef` vs Explorer execute

| Concern | Design catalog (`SearchDef`) | Explorer runtime (`PSSearchCriteria` + SearchPanel) | DCE |
|---------|------------------------------|------------------------------------------------------|-----|
| List saved designs | **Present** (`GET /searches`) | Missing picker | Present |
| Field operators | **Present** on detail | Lost (forced `=`) | Full `PSSearchField` ops |
| Display format | `displayFormatId` string | `formatId` Integer; required on service search paths | Bound via DF on execute |
| Max results | `maximumResultSize` | `maxResults` (panel default 25) | Honored on execute |
| Case sensitivity | `caseSensitive` flag | Not on criteria DTO | Applied on `PSSearch` |
| Free-text query | Not the primary design model | **Primary** panel path | Simple mode query + advanced fields |
| Execute by id/name | **Missing** | **Missing** | **Present** |
| Open / reveal results | N/A (design) | **Present** (panel callbacks) | Tree navigation |

---

## Disposition analysis

### Reuse (rejected for parity)

**Idea:** Slice C loads `GET /searches` + `GET /searches/{id}`, maps `fields[]` → `PSSearchCriteria.searchFields`, calls `searchExtended`.

**Fails DCE parity because:**

1. Operators other than equality are dropped (`like`, `in`, `between`, null/not-null, comparisons).  
2. Multi-value / external operators on `PSSearchField` are not modeled on the Map.  
3. Client must invent formatId resolution from `displayFormatId` strings, max-results defaults, case sensitivity — all server knowledge.  
4. Custom / URL-based searches (`SearchDef.url`, non-standard types) cannot be expressed as FTS + equality fields alone.  
5. Duplicates mapping logic that already exists when loading `PSSearch` server-side.

**Acceptable only for a degraded “equals-only demo”** — not the #2409 acceptance target (“operator can pick a saved search and see results” with design fidelity).

### Hybrid — extend detail DTO only + client execute (rejected as sole path)

**Idea:** Enrich `SearchDef` (e.g. pre-baked runnable criteria blob) and still call existing searchmanagement.

**Still blocked** unless `PSSearchCriteria` / `PSSearchService` gain real operators (and related semantics). Extending the **design** DTO alone does not fix the **runtime** contract. That is effectively a multi-module runtime change without a clean public façade for Explorer, and it leaves operators on an internal sitemanage API rather than a versioned public `rest` companion.

Optional later: runtime enhancement of `PSSearchCriteria` could support **advanced free-text / field search** UI, but that is orthogonal to “execute this design search by id.”

### Façade (selected)

**Idea:** Public `rest` execute endpoint that:

1. Resolves design by the same key rules as `findSearchByKey`.  
2. Loads authoritative `PSSearch` (via design WS — same as list/detail).  
3. Executes server-side with correct operators, format, max results, case sensitivity.  
4. Returns paged item properties suitable for Explorer open/reveal (same conceptual shape as extended results: id, title, folderPath, type, …).

**Pros:** Matches rest/sitemanage companion pattern; keeps operator fidelity; thin WebUI; reuses catalog security/key checks; avoids teaching every client the PSSearch→Lucene mapping.

**Cons:** Slice B work (resource + adaptor + wire DTOs + tests + sitemanage impl). Acceptable — already scoped as #2505.

---

## Façade sketch (for slice B — do not implement here)

Peer patterns: catalog resource + adaptor (`SearchResource` / `ISearchAdaptor`); execute sub-resource naming like `PipelinesResource` `POST …/execute` (avoids colliding with `GET /{idOrName}`).

### Recommended contract

```http
POST /rest/searches/{idOrName}/execute
Content-Type: application/json
Accept: application/json

{
  "folderPath": "//Sites/…",   // optional scope override
  "startIndex": 1,             // optional, default 1
  "maxResults": 25,            // optional; default from design maximumResultSize or product default
  "sortColumn": "sys_title",   // optional
  "sortOrder": "asc"           // optional asc|desc
}
```

```http
200 OK
{
  "children": [ /* SearchResultItem… */ ],
  "totalCount": 42,
  "startIndex": 1,
  "searchName": "All Content",
  "displayFormatId": "…"
}
```

Errors: 400 bad body / unsafe paging; 404 unknown idOrName; 503 adaptor missing; 500 execute failure.

### Companion closure (change class: new public REST execute surface)

| Artifact | Module | Notes |
|----------|--------|-------|
| `SearchExecuteRequest` wire DTO | `rest` `…/searches/` | Optional overrides only; no full field rewrite in v1 |
| `SearchExecuteResult` (+ item row if needed) | `rest` | Prefer a rest-owned result DTO (do not force rest → sitemanage DTO dependency). Map from service layer in adaptor. Align field names with Explorer open/reveal needs (id, title/name, folderPath, type). |
| `ISearchAdaptor.executeSearch(String idOrName, SearchExecuteRequest req)` | `rest` | New method |
| `SearchResource` `POST /{idOrName}/execute` | `rest` | JAX-RS + OpenAPI annotations |
| `SearchResourceTest` | `rest` | Mockito: happy path, 404, 503, 500 |
| `TestSearchAdaptor` | `rest` test apibridge | Stub new method for shared Spring contexts |
| `SearchAdaptor.executeSearch` | `projects/sitemanage` | Load `PSSearch` by key; map fields with real operators to search handler / existing search service; apply overrides; page results |
| Adaptor unit tests | `projects/sitemanage` | Mapping + key safety + empty results |
| Full module `mvnw clean install` | `rest`, then `projects/sitemanage` | Pre-PR hard gate |
| WebUI client | **Out of B** → C | Add `executeSearch` next to `searchesApi` or contentExplorer API; wire picker in `SearchPanel` |
| Playwright | **Out of B** → D | Saved-search pick + results |

**Implementation notes for B:**

- Prefer resolving `PSSearch` once server-side rather than re-reading only `SearchDef` (round-trip would re-lose operators if rebuilt from Map).  
- Reuse `isSafeSearchKey` for path param.  
- Default `formatId` from design `displayFormatId` when parseable; document failure if missing/invalid.  
- v1 scope: standard/user field criteria searches. Custom URL searches may return 400 with a clear message and a residual if product still needs them.  
- Do **not** put execute on internal-only `searchmanagement` without a public `rest` façade if Explorer is the consumer (public contract + companion tests).

### Optional secondary path (not primary disposition)

If product later wants ad-hoc multi-operator field search **without** a saved design, extend `PSSearchCriteria` / `PSSearchService` to accept operator-bearing fields. That does **not** replace the façade for “run design search by id.”

---

## Explorer SearchPanel entry points (for slice C planning)

| Entry | Path | Today |
|-------|------|-------|
| Component | `WebUI/src/main/ts/contentExplorer/SearchPanel.tsx` | Free-text form; `runSearch` → `search` prop or `searchExtended` |
| Props | `initialQuery`, `initialCriteria`, `search`, `onOpen`, `onReveal` | No `savedSearchId` / catalog prop |
| Shell | `ContentExplorerShell.tsx` (~search panel region) | Show/hide search; folderPath criteria only |
| Developer catalog (reference UX) | `SearchesPanel` / `SearchDetailPanel` | List + detail only — good peer for picker list styling, not execute |

**C expected delta (after B):** load catalog via existing `listSearches`; on pick, call façade execute; render results with existing open/reveal rows; keep free-text path intact.

---

## Out of scope (confirmed)

- Implementing execute endpoint (→ #2505)  
- Explorer SearchPanel saved-search UI (→ #2506)  
- Playwright (→ #2507)  
- Create/edit/delete of search designs  
- Views catalog execute (`/rest/views` is separate UI-07)

---

## Links

| Doc / issue | Role |
|-------------|------|
| [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400) | Grandparent DCE ↔ Explorer parity |
| [#2409](https://github.com/intersoftdatalabs-in/percussioncms/issues/2409) | Parent: saved searches catalog + execute |
| [#2504](https://github.com/intersoftdatalabs-in/percussioncms/issues/2504) | This inventory / disposition |
| [#2505](https://github.com/intersoftdatalabs-in/percussioncms/issues/2505) | B: façade implement |
| `specs/2400-dce-explorer-parity/contracts/gap-matrix.md` | Search row “Saved searches catalog + run” |
| `specs/2400-dce-explorer-parity/plan.md` | Phase 2 saved search execute note |
| `specs/992-react-content-explorer/` | US5 free-text SearchPanel baseline |

---

## Acceptance checklist (#2504)

- [x] Written inventory of endpoints, adaptor methods, DTOs, and Explorer SearchPanel entry points (paths + capability notes)  
- [x] Explicit **execute disposition: façade** with evidence (operator loss on runtime Map; DCE full `PSSearch` execute)  
- [x] Façade method/path signature sketch + rest/sitemanage companion closure list  
- [x] Links to #2409 / #2400; no product UI or execute code in this PR  

> Co-Authored by Grok Build using grok-4.5 with agent main.
