# #3054 Spike: Explorer folder ops — REST facade over Rhythmyx web-service folder API

**Status:** design complete (spike only — **no production Explorer switch**)  
**Issue:** [#3054](https://github.com/intersoftdatalabs-in/percussioncms/issues/3054)  
**Related:** [#3044](https://github.com/intersoftdatalabs-in/percussioncms/issues/3044) (Explorer `//Folders` root — closed / PR #3052), [#3053](https://github.com/intersoftdatalabs-in/percussioncms/issues/3053) (QA for #3044)  
**Date:** 2026-08-11  
**Operator:** night-issue-prs / Grok (model grok-4.5)

---

## 1. Problem statement

Modern **Content Explorer** (React WebUI) performs folder tree and folder mutation work primarily through the **CM1 sitemanage pathmanagement REST** surface (`…/services/pathmanagement/path/*`). Classic **Rhythmyx Content Explorer** performed folder operations through the **content web-service folder API** (`IPSContentWs` folder methods / SOAP content service).

Those stacks are **not equivalent**:

| Concern | Classic web-service folder API | CM1 pathmanagement / existing rest `Folders` |
|--------|--------------------------------|-----------------------------------------------|
| Path model | Fully qualified RX paths (`//Folders`, `//Sites/…`, `/` for roots) | Finder paths (`/Sites`, `/Assets`, `/Folders`, `/Recycling`, …) mapped to internal `//…` |
| Roots | `loadFolders` / `findFolderChildren("/")` → Folders + Sites roots | Dispatching registry of path item services |
| DTOs | `PSFolder`, `PSItemSummary`, folder ACL properties | `PSPathItem`, `PSFolderProperties`, CM1 access levels |
| Ops shape | multi-id child add/remove/move, purge flags, folder-tree create | single path create/rename/move/delete, CM1 site-copy guards |
| Audience | Integrators + classic CX | Finder + modern Explorer |

Using only CM1 Finder semantics for Explorer risks permanent parity gaps (roots, ACL, multi-child ops, purge vs recycle, non-site folder trees).

**Spike goal:** inventory call sites, map APIs, recommend a REST facade shape, migration plan, risks, and phased implement issues. **Out of scope for this PR:** implementing the facade or rewiring Explorer end-to-end.

---

## 2. Inventory — current call sites

### 2.1 Modern Explorer (WebUI React)

Primary client: `WebUI/src/main/ts/api/contentExplorer/pathApi.ts`  
Contracts note: `specs/992-react-content-explorer/contracts/path-api.md`  
Server: `projects/sitemanage` `PSPathService` (`@Path("/path")` under pathmanagement).

| Client function | Wire (via `PATHS` in `WebUI/src/main/ts/api/paths.ts`) | Server method |
|-----------------|--------------------------------------------------------|---------------|
| `findChildren` | `GET …/pathmanagement/path/folder/{path}` | `findChildren` |
| `paginatedFolder` | `GET …/pathmanagement/path/paginatedFolder/{path}` | paginated children |
| `findItemByPath` | `GET …/pathmanagement/path/item/{path}` | `find` |
| `findItemById` | `GET …/pathmanagement/path/item/id/{id}` | `findById` |
| `addNewFolder` | `GET …/pathmanagement/path/addNewFolder/{path}?name=` | `addNewFolder` |
| `renameFolder` | `POST …/pathmanagement/path/renameFolder` | `renameFolder` |
| `moveItem` | `POST …/pathmanagement/path/moveItem` | `moveItem` |
| `deleteItem` | `POST …/pathmanagement/path/delete/{path}` | delete item |
| `folderProperties` / `saveFolderProperties` | `GET/POST …/folderProperties` / `saveFolderProperties` | folder ACL/props |
| `validatePath` / `lastExisting` | `GET …/validate` / `lastExisting` | path helpers |

Explorer shell/tree/detail modules that depend on this surface:

- `WebUI/src/main/ts/contentExplorer/ContentExplorerShell.tsx` — `findItemByPath`
- `ExplorerTree.tsx`, `DetailList.tsx`, `folderPath.ts`, `sitePath.ts`
- `FolderSecurityPanel.tsx` — folderproperties REST
- Clipboard / wizards (`clipboardApi.ts`, `SubfolderCopyWizard.tsx`) — `moveItem` and related pathmanagement ops
- Unit: `WebUI/src/test/ts/contentExplorer/ExplorerTree.test.tsx` (mocks `…/pathmanagement/path/folder/…`)

**Note:** paths use the **services** root (`/services/pathmanagement/…` or `/Rhythmyx/services/…` via `detectServicesRoot()`), **not** the public `rest` module base (`/Rhythmyx/rest/…`).

### 2.2 Legacy Finder WebUI (jQuery)

Still heavy pathmanagement / PercPathService usage (same CM1 stack):

- `WebUI/war/widgets/perc_finder.js` — open path, move, get by id
- `perc_new_folder_button.js`, `perc_delete_page_button.js`, `perc_folderproperties_button.js`
- `perc_site_map.js` — section trees / folderPath
- `$.perc_pathmanager` / `$.PercPathService` clients

These should **remain** on pathmanagement unless product later unifies Finder with Explorer.

### 2.3 Public `rest` module folders surface (existing)

| Artifact | Path / role |
|----------|-------------|
| `rest/.../folders/FoldersResource` | `@Path("/folders")` — by-path get/put/delete, move/copy item|folder, rename, delete item |
| `rest/.../folders/IFolderAdaptor` | adaptor contract |
| `rest/.../folders/Folder` DTO | siteName + path + name; pages/assets/subfolders/sectionInfo |
| `projects/sitemanage/.../apibridge/FolderAdaptor` | **impl** — uses `IPSPathService`, `IPSFolderHelper`, `IPSContentWs`, site section services |

This surface is **site/asset/section-centric** (examples: `by-path/MySite/…`, `by-path/Assets/uploads`). It is **not** what modern Explorer uses for the left tree. QA automation sometimes hits it (`modules/perc-qa-automation/.../contentExplorer.spec.js` references `rest/folders/by-path/Assets`).

### 2.4 Sitemanage foldermanagement REST (workflow assignment)

- `PSFolderRestService` `@Path("/folders")` under **foldermanagement** — workflow assignment jobs, not Explorer tree ops.
- Orthogonal; leave alone.

### 2.5 `//Folders` root (#3044)

Already landed on **pathmanagement**, not a new rest facade:

- `PSFoldersPathItemService` — maps finder `/Folders` → repository `//Folders`
- Registered in `sitemanage-beans.xml`: `<entry key="/Folders/" value-ref="foldersPathItemService" />`
- Human QA: **#3053**

So root **visibility** is a path-dispatch concern; this spike still addresses **ops parity** and a cleaner RX REST contract for Explorer mutations and integrators.

### 2.6 Classic web-service folder API

Java API: `system/webservices/.../content/IPSContentWs` (folder section).  
SOAP: content WSDL operations (see `ContentTestCase` / sample WSDL under `system/webservices`).

Representative operations (folder domain):

| Op | Purpose |
|----|---------|
| `loadFolder` / `loadFolders(ids\|paths)` | Load full `PSFolder` (paths may be `//Folders/...`; `/` for root folders) |
| `saveFolder` / `saveFolders` | Persist existing folder defs (ACL, props) |
| `addFolder` / `addFolderTree` | Create single folder or missing path segments |
| `deleteFolders(ids, purgeItems[, checkFolderPermission])` | Recursive delete; optional purge |
| `findFolderChildren` (id or path) | Direct children (items + folders); `/` → roots |
| `findChildFolders` | Folders only |
| `findFolderParents` | Parent folders |
| `findDescendantFolders` | Recursive folder set |
| `addFolderChildren` / `removeFolderChildren` | Multi-child attach/detach (+ purge flag) |
| `moveFolderChildren` | Multi-child move source → target |
| `findItemPaths` | Paths for an item id |

Object model: `com.percussion.cms.objectstore.PSFolder` (+ WS wire types under `modules/webservices` / content stubs). Security: folder ACLs on folder objects; WS inherits parent ACL on create.

Classic Content Explorer also uses CX/server folder processor paths (`PSServerFolderProcessor`, `//Folders` constant in `PSContentExplorerConstants.PARAM_PATH_FOLDERS`).

---

## 3. Diff — CM1 pathmanagement / rest Folders vs web-service folder API

### 3.1 Paths and roots

| | Web service | Pathmanagement | rest `FoldersResource` |
|--|-------------|----------------|------------------------|
| Repository form | `//Folders`, `//Sites/Name`, `//Folders/$System$/Assets` | Internal full path via each `PSPathItemService` | Built as `/Sites|Assets/...` style site-first paths |
| Finder form | N/A (uses RX paths) | `/Sites`, `/Assets`, `/Folders`, `/Design`, `/Recycling` | Not finder-oriented |
| Root listing | `findFolderChildren("/", …)` / `loadFolders("/")` | `findChildren("")` → registered path services | No true RX root API |

### 3.2 Identifiers

| | Web service | Pathmanagement | rest Folders |
|--|-------------|----------------|--------------|
| Primary id | Folder/content GUID (`IPSGuid` / legacy guid string) | String id on `PSPathItem` (id mapper space) | Guid string on `Folder.id` |
| Path as key | Fully qualified RX path | Finder path segments on URL | `site` + relative path + name |

### 3.3 Operations coverage (Explorer-relevant)

| Capability | WS | Pathmanagement | rest Folders |
|------------|----|----------------|--------------|
| List children | Yes (path/id) | Yes (+ pagination, display format) | Via subfolders/pages/assets on get |
| Create folder | `addFolder` / tree | `addNewFolder` / `addFolder` | `PUT by-path` update/create |
| Rename | save / path ops | `renameFolder` | `POST rename` |
| Move | `moveFolderChildren` multi | `moveItem` single | `POST move/item\|folder` |
| Copy | (item copy elsewhere + folder clone variants) | Limited / site-specific | `POST copy/item\|folder` |
| Delete | `deleteFolders` + purge | `deleteFolder` / delete item (+ recycle paths) | `DELETE by-path` + includeSubFolders |
| Folder ACL/props | save folder object | `folderProperties` / save | partial on `Folder` DTO |
| Site sections / nav | No | Partial via other CM1 services | **Yes** (sectionInfo) — CM1-only |

### 3.4 Errors and ACL

- **WS:** `PSErrorException` / `PSErrorResultsException` / `PSErrorsException` with multi-result fault shapes (batch ops).
- **Pathmanagement:** `PSPathNotFoundServiceException`, validation, WebApplicationException messages; CM1 site-copy locks.
- **rest Folders:** `FolderNotFoundException` → 404; `BackendException` → 500; location mismatch on PUT.
- **ACL:** WS folder security entries vs CM1 `PSFolderPermission` access levels (`ADMIN`/`READ`/`WRITE`/`VIEW`) — overlapping but not 1:1 wire models.

### 3.5 Implications

1. Pathmanagement is the right **browse/pagination** stack for Explorer today (and already hosts `/Folders` via #3044).
2. Pathmanagement + rest `Folders` are **not** a clean RX parity API for integrators or for ops that need multi-child/purge/`//Folders` semantics without CM1 site guards.
3. Existing `FolderAdaptor` already **depends on** `IPSContentWs` but wraps it in **CM1 site/section** product semantics — reusing/extending it for RX parity would further conflate two products.

---

## 4. Options

### Option A — New `rest` facade over web-service folder API (**recommended**)

Add a **new** public REST surface under the `rest` module, following existing Content Explorer peers:

- Peers: `@Path("/content-explorer/relationships")`, `@Path("/content-explorer/translations")`
- Proposed: `@Path("/content-explorer/folders")` (name TBD; avoid colliding with sitemanage foldermanagement `/folders` and public `/folders`)

**Layering (mandatory rest/sitemanage split):**

```text
WebUI Explorer / integrators
    → rest  ContentExplorerFoldersResource + wire DTOs + IContentExplorerFolderAdaptor
        → sitemanage apibridge ContentExplorerFolderAdaptor (@PSSiteManageBean)
            → IPSContentWs folder methods (behavioral reference = classic CX / SOAP)
            → optional thin helpers (id mapper, folder helper) only where WS needs them
```

- **No** `rest` → `sitemanage` Maven dependency.
- **Do not** implement domain logic inside `rest`.
- Keep **CM1 Finder** and **pathmanagement** as-is for Finder and for Explorer **list/paginate** until a deliberate switch.

### Option B — Extend CM1 pathmanagement only

Continue adding RX semantics into `PSPathService` / path item services.

| Pros | Cons |
|------|------|
| Explorer already wired | Permanent CM1/RX conflation |
| #3044 already here | Harder for integrators; SOAP parity undocumented |
| No new URL family | Site-copy guards, recycle, display-format concerns leak into RX ops |

**Reject as sole strategy** for long-term Explorer parity (may still host browse).

### Option C — Extend existing `rest` `FoldersResource` / `IFolderAdaptor`

Grow site-centric `Folder` DTO and paths to accept `//Folders`.

| Pros | Cons |
|------|------|
| Less surface area | DTO already overloaded with sections/pages/assets |
| FolderAdaptor already has ContentWs | Breaks OpenAPI semantics; high regression risk for site REST consumers |

**Reject** as primary path; leave CM1 site/asset folder REST stable.

### Option D — Dual: pathmanagement browse + new facade mutations

Phased hybrid (recommended **migration shape** even under Option A):

1. Keep Explorer **reads** on pathmanagement (pagination, display formats, roots registry).
2. Introduce content-explorer folders REST for **mutations** and **RX-path identity** ops.
3. Optionally later add read/list methods on the facade for integrators (not required for first Explorer cut).

---

## 5. Recommendation

**Recommend Option A + Option D migration:**

1. **New** REST facade in `rest` under `/content-explorer/folders` (or `/content-explorer/rx-folders` if naming collision concerns win) that is a **thin façade over `IPSContentWs` folder operations**, not over pathmanagement.
2. **Do not** repurpose `FoldersResource` or foldermanagement REST.
3. **Do not** hard-cut Explorer off pathmanagement in the first implement PR.
4. **Keep** pathmanagement for:
   - Left-nav roots including `/Folders` (#3044 / QA #3053)
   - Paginated listing + display formats
   - Finder / CM1 product flows
5. **Switch Explorer mutations** (create/rename/move/delete/ACL where parity fails) onto the new facade only after:
   - Human design approval of this note
   - Resource + adaptor + unit/Spring tests green
   - Agent-safe H2 QA evidence for RX path cases

Rationale aligns with `rest/AGENTS.md` **Workbench / classic API replacement** rule: clean REST contract in `rest`, behavioral reference = classic web services, impl in sitemanage apibridge — avoid lazily stretching partial sitemanage REST.

---

## 6. Proposed REST facade shape (implement sketch — not in this PR)

### 6.1 Packages / types

| Layer | Module | Suggested names |
|-------|--------|-----------------|
| Resource | `rest` | `com.percussion.rest.contentexplorer.folders.ContentExplorerFoldersResource` |
| Adaptor iface | `rest` | `IContentExplorerFolderAdaptor` |
| DTOs | `rest` | `RxFolder`, `RxFolderSummary`, `RxFolderChildList`, request bodies for move/remove/add-children |
| Errors | `rest` | map to existing `RestError` / 404/403/409; avoid leaking WS stack traces |
| Impl | `sitemanage` | `com.percussion.apibridge.ContentExplorerFolderAdaptor` |

### 6.2 Suggested endpoints (v1 — mutation + path load)

All under `@Path("/content-explorer/folders")`, JSON, OpenAPI tags `Content Explorer Folders`.

| Method | Path | Maps to IPSContentWs |
|--------|------|----------------------|
| GET | `/by-path/{path:.+}` | `loadFolders(String[] paths)` (single) |
| GET | `/by-id/{id}` | `loadFolder(IPSGuid)` |
| GET | `/{id\|path}/children` | `findFolderChildren` |
| GET | `/{id\|path}/child-folders` | `findChildFolders` |
| POST | `/` or `/add` | `addFolder` |
| POST | `/tree` | `addFolderTree` |
| PUT | `/by-id/{id}` | `saveFolder` |
| POST | `/move-children` | `moveFolderChildren` |
| POST | `/add-children` | `addFolderChildren` |
| POST | `/remove-children` | `removeFolderChildren` |
| DELETE | `/by-id/{id}` | `deleteFolders` (+ `purge` query) |

Path encoding: accept **both** repository form (`//Folders/...`) and a documented single-slash form; normalize in adaptor (mirror `folderPath.ts` rules). Never invent a third path dialect.

### 6.3 Companions (change-class gate)

From `rest/AGENTS.md` + root change-class table:

- [ ] Resource + DTOs + `IAdaptor` in `rest`
- [ ] apibridge impl in sitemanage `@PSSiteManageBean`
- [ ] Mockito resource test in `rest`
- [ ] **Spring test stub** for adaptor on rest test classpath (`MainTest` peers)
- [ ] Adaptor unit tests in sitemanage (mock `IPSContentWs`)
- [ ] OpenAPI annotations; perc-openapi if required by module practice
- [ ] Standalone `mvnw clean install` for **rest** and **projects/sitemanage**
- [ ] product-docs only when operator/integrator public surface ships (implement phase)
- [ ] Playwright / perc-qa-automation when Explorer **UI** is switched (later phase)

### 6.4 What stays on CM1 REST

| Surface | Keep on pathmanagement / sitemanage |
|---------|-------------------------------------|
| Finder UI | Yes |
| Explorer tree list + pagination + display formats | Yes (initially forever-or-until proven) |
| Sites / Assets convenience roots | Yes |
| Site section create / nav tree | Yes (section services — not WS folders) |
| Recycle bin empty / restore | recycle pathmanagement APIs |
| Workflow folder assignment | foldermanagement REST |

---

## 7. Migration plan (Explorer)

| Phase | Work | Production switch? |
|-------|------|--------------------|
| **0 — Spike (this issue)** | Design note + recommendation + follow-on issues | No |
| **1 — Facade skeleton** | Resource + DTOs + adaptor iface + Spring stub + no-op or read-only load by path | No |
| **2 — WS-backed ops** | Implement load/children/add/rename/move/delete/ACL via `IPSContentWs` + tests | No |
| **3 — Dual-run / feature flag** | Optional server or UI flag: Explorer mutations call new REST; reads stay pathmanagement | Soft / flag default off |
| **4 — Explorer client switch** | `pathApi` (or new `rxFolderApi.ts`) for selected mutations; keep list on pathmanagement | Yes (flag or hard cut after QA) |
| **5 — Parity hardening** | Playwright Explorer folder ops under `//Folders` + Sites; ACL matrix; purge vs recycle | After human QA |

**Hard rule from issue:** no production switch until human reviews this design.

### Flag sketch (phase 3)

- Prefer existing product patterns (server.properties or UI feature flag used by Explorer).
- Default **off**. When off, behavior unchanged (pathmanagement only).
- When on, mutations for folder create/rename/move/delete under RX-capable roots go to `/content-explorer/folders`.

---

## 8. Risks

| Risk | Mitigation |
|------|------------|
| Dual trees / dual path dialects confuse UI | Single path normalizer shared by WebUI (`folderPath.ts`) and adaptor; document accepted forms |
| Dual-ship packaging (services vs rest base URL) | Explorer already mixes services pathmanagement and `/Rhythmyx/rest` content-explorer APIs — follow content-explorer peers for base URL in `paths.ts` |
| ACL mismatch CM1 vs RX folder security | Map carefully in adaptor tests; do not silently drop ACL entries |
| Performance (load full `PSFolder` trees) | Keep pagination on pathmanagement; facade children calls stay **direct children** only |
| rest→sitemanage cycle | Interfaces only in rest; impl in apibridge |
| Site-copy / CM1 locks bypassed | For site paths, either delegate to pathmanagement or re-apply `PSSiteCopyUtils` guards in adaptor |
| Breaking Finder | Never change Finder clients in early phases |
| Agent-safe test gaps | See §9 |

---

## 9. Agent-safe H2 QA / test strategy

| Layer | Where | What |
|-------|-------|------|
| Unit resource | `rest` | Mock adaptor; status codes, path decode |
| Unit adaptor | `sitemanage` | Mock `IPSContentWs`; path normalize; error mapping |
| Spring wiring | `rest` MainTest stubs | New adaptor bean on test classpath |
| Integration (optional) | sitemanage/H2 | Existing pathmanagement tests for `/Folders` remain; add WS-backed tests only if H2 harness can host content WS |
| Playwright | `modules/perc-qa-automation` | **After** UI switch: create/rename/move/delete under Folders root; ACL deny cases if fixtures allow |
| Manual QA | QA H2 docker / install | Assign `@vijaya-boddipudi` “qa task” when UI or install-visible behavior changes |

Prefer **unit + module clean install** for facade PRs; do not require full host install for pure API slices.

---

## 10. Phased implement issues (checklist)

Use these as follow-on GitHub issues (PR-sized). Parent tracker: **#3054**.

| # | Slice title | Modules | Acceptance (short) |
|---|-------------|---------|-------------------|
| 1 | Content-explorer folders REST skeleton (DTOs + IAdaptor + resource stubs + Spring test stub) | `rest`, `projects/sitemanage` (stub bean) | OpenAPI paths exist; MainTest green; no Explorer switch |
| 2 | apibridge impl: load + children via `IPSContentWs` | `projects/sitemanage`, `rest` tests | GET by path/id + children for `//Folders` and `//Sites`; unit tests |
| 3 | apibridge impl: add / rename / save folder (ACL props) | same | Mutations match WS semantics; permission failures mapped |
| 4 | apibridge impl: move/add/remove children + delete (purge flag) | same | Multi-child ops; purge vs non-purge tested |
| 5 | Explorer dual-run flag + client API module | `WebUI`, optional server flag | Default off; when on, mutations use new REST |
| 6 | Explorer switch + Playwright folder ops under `/Folders` | `WebUI`, `modules/perc-qa-automation` | Human QA issue; no regressions on Sites/Assets |

Cross-links:

- Depends on / informed by **#3044** / QA **#3053** for root presence in pathmanagement.
- Does **not** replace #3044; complements it (root registration vs ops facade).

---

## 11. Explicit recommendation (acceptance checkbox)

| Question | Answer |
|----------|--------|
| New `rest` facade over web-service folder API **vs** extend CM1 folder REST? | **New rest facade** (`/content-explorer/folders`) over `IPSContentWs`. |
| Extend existing `FoldersResource`? | **No** (keep CM1 site/asset/section API stable). |
| Extend pathmanagement only? | **No as sole strategy**; keep for browse/Finder/`//Folders` root (#3044). |
| Production Explorer switch in first implement PR? | **No** — flag/default-off dual-run first. |
| Spike delivers production code? | **No** — this document only. |

---

## 12. Source map (absolute-ish repo paths)

| Area | Path |
|------|------|
| Explorer path client | `WebUI/src/main/ts/api/contentExplorer/pathApi.ts` |
| Path constants | `WebUI/src/main/ts/api/paths.ts` |
| Path REST | `projects/sitemanage/.../pathmanagement/service/impl/PSPathService.java` |
| Folders root service | `.../PSFoldersPathItemService.java` |
| Spring registry | `projects/sitemanage/src/main/resources/.../sitemanage-beans.xml` (`/Folders/`) |
| rest folders | `rest/src/main/java/com/percussion/rest/folders/*` |
| Folder adaptor | `projects/sitemanage/.../apibridge/FolderAdaptor.java` |
| WS folder API | `system/webservices/.../content/IPSContentWs.java` |
| rest agent rules | `rest/AGENTS.md` |

---

## 13. Decision log

| Date | Decision |
|------|----------|
| 2026-08-11 | Spike research complete; recommend new content-explorer folders REST façade over `IPSContentWs`; no production switch; follow-on implement slices listed in §10. |

---

*Engineering design note under `docs/ai-generated/` — not product-docs. Product-docs companion required only when the public REST/UI surface ships (implement phases).*
