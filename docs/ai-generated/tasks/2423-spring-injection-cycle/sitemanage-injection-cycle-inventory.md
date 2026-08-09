# Sitemanage Spring constructor-injection cycle inventory

**Issue:** #2463 (residual of #2423); reverse-edge inventory refresh **#2485**; hub freezes **#2477** / **#2514** / **#2519**; itemWorkflow param `@Lazy` **#2515**; templateService param `@Lazy` **#2520**; siteData reverse-edge freeze **#2516**; widgetAsset managedLink path-C **#2527**  
**Module:** `projects/sitemanage`  
**Date:** 2026-08-08 (updated 2026-08-09 for #2485 / #2515 / #2520 / #2521 / #2525 / #2514 / #2516 / #2519 / #2527)  
**Method:** Static scan of `@Autowired` constructors + field `@Autowired` among high-fan-in sitemanage beans (interfaces mapped to primary impls). Reflection peers: `PSContentItemDaoCycleLazyWiringTest`, `PSPageDaoHelperCycleLazyWiringTest`, `PSFolderHelperRecycleLazyWiringTest`, `PSFolderHelperReverseEdgeInventoryWiringTest` (#2485), `PSFolderHelperFieldInjectionInventoryWiringTest` (#2525), `PSTemplateServiceCycleWiringTest` (#2477), `PSTemplateServiceParamLazyWiringTest` (#2520), `PSPageServiceCycleWiringTest` (#2514 — ctor + field reverse-edge freeze, mirrors #2478), `PSItemWorkflowServiceHubReverseEdgeWiringTest` (#2478), `PSItemWorkflowServiceCycleLazyWiringTest` (#2515), `PSSiteDataServiceHubReverseEdgeWiringTest` (#2516), `PSWidgetAssetRelationshipServiceHubReverseEdgeWiringTest` (#2519 + path-C #2527), `PSAssetServicePageServiceNearCycleWiringTest`, `PSAssetServiceTemplateServiceNearCycleWiringTest` (#2521), `FolderHelperCycleContextTest` (#2436).

This is an analysis note for humans/agents ΓÇö **not** an agent rule file.

## Known cycles (fixed)

```text
Path A (slice A #2435):
folderHelper
  ΓåÆ recycleService
    ΓåÆ widgetAssetRelationshipService
      ΓåÆ assetDao
        ΓåÆ contentItemDao
          ΓåÆ folderHelper   ΓåÉ @Lazy on PSContentItemDao ctor param

Path B (slice C #2437 / PR #2483):
folderHelper
  ΓåÆ recycleService
    ΓåÆ widgetAssetRelationshipService
      ΓåÆ pageIndexService
        ΓåÆ pageDaoHelper
          ΓåÆ folderHelper   ΓåÉ @Lazy on PSPageDaoHelper ctor param

Forward deferral (also #2437):
folderHelper ΓåÆ recycleService   ΓåÉ @Lazy on PSFolderHelper ctor param

Path C (latent / banned — #2527; not a live ctor cycle):
widgetAsset ↛ managedLinkService   ΓåÉ application-context lookup only
managedLinkService ΓåÆ pageService ΓåÆ folderHelper
  (would re-enter folderHelper while paths A/B may still be constructing)
```

| Edge | Type | Breaker |
|------|------|---------|
| `PSFolderHelper` ΓåÆ `IPSRecycleService` | ctor | **`@Lazy`** (#2437) |
| `PSRecycleService` ΓåÆ `IPSWidgetAssetRelationshipService` | ctor | none (forward) |
| `PSWidgetAssetRelationshipService` ΓåÆ `IPSAssetDao` | ctor | none (forward) |
| `PSAssetDao` ΓåÆ `IPSContentItemDao` | ctor | none (forward) |
| `PSContentItemDao` ΓåÆ `IPSFolderHelper` | ctor | **`@Lazy`** (#2435) |
| `PSWidgetAssetRelationshipService` ΓåÆ `IPSPageIndexService` | ctor | none (forward) |
| `PSPageIndexService` ΓåÆ `IPSPageDaoHelper` | ctor | none (forward) |
| `PSPageDaoHelper` ΓåÆ `IPSFolderHelper` | ctor | **`@Lazy`** (#2437) |
| `PSWidgetAssetRelationshipService` ΓåÆ `IPSManagedLinkService` | **banned** (context lookup) | **Ban** (#2527 path C) |
| `PSManagedLinkService` ΓåÆ `IPSPageService` | ctor | none (forward consumer; hazard only if widgetAsset ctor-pulls managedLink) |

Class-level `@Lazy` is present on `folderHelper` and `recycleService` but is **not** sufficient alone: an eager consumer (e.g. field inject on `pSRedirectService`) still forces full construction and re-enters the cycle without a parameter-level `@Lazy` proxy.

## folderHelper reverse-edge inventory (#2485)

**Definition ΓÇö reverse edge:** a constructor (or field) dependency **into** `IPSFolderHelper` from a bean that is itself on `folderHelper`'s construction subgraph (or can be forced while `folderHelper` is still creating). Only reverse edges need param `@Lazy` (or equivalent). Downstream product consumers of `folderHelper` are **not** reverse edges.

### Live reverse edges (must keep param `@Lazy`)

| From bean | Edge | Disposition | Regression test |
|-----------|------|-------------|-----------------|
| `PSContentItemDao` | ctor ΓåÆ `IPSFolderHelper` | **param `@Lazy`** (#2435) | `PSContentItemDaoCycleLazyWiringTest` |
| `PSPageDaoHelper` | ctor ΓåÆ `IPSFolderHelper` | **param `@Lazy`** (#2437) | `PSPageDaoHelperCycleLazyWiringTest` |
| `PSFolderHelper` | ctor ΓåÆ `IPSRecycleService` | **param `@Lazy`** (forward deferral, #2437) | `PSFolderHelperRecycleLazyWiringTest` |

**#2485 result:** no additional live reverse ctor edges into `folderHelper` were found. No further production `@Lazy` changes required on this slice.

### Cycle-subgraph intermediates that must NOT construct-require `folderHelper`

These sit on path A/B. Adding a ctor edge to `IPSFolderHelper` would short-circuit the known breaks:

| Bean | Current ctor deps of interest | Disposition |
|------|------------------------------|-------------|
| `PSRecycleService` | widgetAsset (+ system peers) | **Ban** ctor ΓåÆ folderHelper (would reverse `folderHelperΓåÆrecycle`) |
| `PSWidgetAssetRelationshipService` | assetDao, pageIndexService, ΓÇª | **Ban** ctor ΓåÆ folderHelper |
| `PSAssetDao` | contentItemDao only | **Ban** ctor ΓåÆ folderHelper |
| `PSPageIndexService` | pageDao, pageDaoHelper, ΓÇª | **Ban** ctor ΓåÆ folderHelper |
| `PSPageDao` | contentItemDao, ΓÇª | **Ban** ctor ΓåÆ folderHelper |

`managedLinkService` is resolved by `PSWidgetAssetRelationshipService` via application-context lookup (not ctor) — **hardened #2527** (keep it that way; freezes in hub reverse-edge test). A ctor inject of `IPSManagedLinkService` would pull `pageService` → `folderHelper` and form path C.

### Consumer-only injectors of `IPSFolderHelper` (no param `@Lazy` required)

Class-level `@Lazy` on these beans is **lazy init only** ΓÇö it is **false safety** for constructor cycles when an eager peer forces creation. OK **only while** live reverse edges above keep param `@Lazy`.

| Class | Class `@Lazy`? | Disposition |
|-------|----------------|-------------|
| `PSPageService` | yes | consumer / hub ΓÇö reverse-edge freeze done (#2514 / `PSPageServiceCycleWiringTest`) |
| `PSItemWorkflowService` | yes | consumer / hub ΓÇö reverse freeze #2478; cycle-peer **param `@Lazy`** #2515 |
| `PSSiteDataService` | yes | consumer / hub ΓÇö #2516 |
| `PSAssetService` | yes | consumer; folderHelper param **not** `@Lazy` (pageService param **is** `@Lazy` #2476) |
| `PSItemService` | no | consumer |
| `PSSiteContentDao` | yes | consumer (also takes pageDaoHelper ΓÇö not a reverse edge into folderHelper creation) |
| `PSTemplateDao` | yes | consumer |
| `PSEmptyRecycleService` | yes | consumer; not on folderHelper ctor path (depends on pathService) |
| `PSPathService` / path item services (`PSSitePathItemService`, `PSAssetPathItemService`, `PSRecyclePathItemService`, `PSSearchPathItemService`, `PSFileSystemPathItemService`, `PSDesignPathItemService`, `PSWebResourcesPathItemService`, `PSPathItemService`, `PSDispatchingPathService`) | mixed | path consumers |
| `PSFolderService`, `PSPageCatalogService`, `PSPageRestService`, `PSAssetRestService`, `PSCommentsService`, `PSCm1ListViewHelper`, `PSSiteSectionMetaDataService`, `PSSearchService`, `PSSearchIndexFieldValueModifier`, `PSTrafficService`, `PSCloudService`, `PSPageOptimizerService`, `PSLinkExtractionHelper`, `PSLivePublishChangeHandler`, `PSAssetUploadFolderPathMap` | mixed | product consumers / REST facades |

### False-safety note (class-level `@Lazy` only)

Spring class-level `@Lazy` defers bean *creation until first request*. It does **not** stop constructor dependency resolution once creation starts. Documented failure mode (#2437 Docker): class `@Lazy` on `PSFolderHelper` / `PSPageDaoHelper` still produced `BeanCurrentlyInCreationException` until **parameter** `@Lazy` was added on reverse edges.

## folderHelper field / setter injection inventory (#2525)

**Definition — field/setter edge:** a `@Autowired` / `@Resource` / `@Inject` annotation on a **field** declaration, or on a `setXxx` setter method, where the injected type is one of the cycle interfaces. Field/setter injection bypasses constructor `@Lazy` breaks because Spring resolves the dependency after construction has started — the field-injected dependency must already exist (or a proxy) when the bean is being instantiated.

### Cycle-subgraph classes (force-creatable while `folderHelper` is constructing)

These seven classes sit on the recycle subgraph and can be forced to construct while `folderHelper` is still being created (paths A/B above). Any field/setter inject of a target interface on these classes would be a **live reverse field edge**.

| Class | Injection mechanism for target interface | Disposition |
|-------|-------------------------------------------|-------------|
| `PSFolderHelper` → `IPSRecycleService` | ctor only (field `recycleService` is plain; only assigned in ctor; no setter; no field `@Autowired`) | OK — no field edge |
| `PSRecycleService` → `IPSWidgetAssetRelationshipService` | ctor only (field `widgetAssetRelationshipService` is plain; no setter; no field `@Autowired`) | OK — no field edge |
| `PSWidgetAssetRelationshipService` → `IPSAssetDao`, `IPSPageIndexService` | ctor only (fields `assetDao`, `pageIndexService` are plain; assigned in ctor; no setter; no field `@Autowired`) | OK — no field edge |
| `PSAssetDao` → `IPSContentItemDao` | ctor only (field `contentItemDao` is `final`, assigned in ctor; no setter; no field `@Autowired`) | OK — no field edge |
| `PSContentItemDao` → `IPSFolderHelper` | ctor only (`@Lazy`; field `folderHelper` is plain; assigned in ctor; no setter; no field `@Autowired`) | OK — no field edge |
| `PSPageIndexService` → `IPSPageDaoHelper` | ctor only (field `pageDaoHelper` is `final`, assigned in ctor; no setter; no field `@Autowired`) | OK — no field edge |
| `PSPageDaoHelper` → `IPSFolderHelper` | ctor only (`@Lazy`; field `folderHelper` is plain; assigned in ctor; no setter; no field `@Autowired`) | OK — no field edge |

**Result:** **No live reverse field/setter edges** were found in the cycle subgraph. The #2485 ctor work fully converted the recycle subgraph to pure constructor injection (no surviving setter or field `@Autowired` for any of the target interfaces), so the `@Lazy` parameter breaks remain the only protection needed.

**No production code change is required for #2525.** The peer reflection test `PSFolderHelperFieldInjectionInventoryWiringTest` (#2525) freezes this state by asserting that none of the target fields on the cycle subgraph carry a `@Autowired` annotation (Spring would otherwise be free to re-introduce field injection).

### Downstream consumer field injectors (informational, NOT reverse edges)

These classes have field `@Autowired` of a target interface but are **downstream consumers** of `folderHelper` (not on its construction subgraph), so forcing their construction does **not** force `folderHelper` creation. They are listed here to document the full scan and to clarify why no `@Lazy` is required on their fields.

| Class | Field-injected target type | Disposition |
|-------|----------------------------|-------------|
| `FolderAdaptor` (REST adaptor) | `IPSFolderHelper`, `IPSPageDaoHelper`, `IPSPageDao` | downstream consumer (also ctor-injected; field annotations are redundant with ctor). Class `@Lazy`. Not on folderHelper ctor path. |
| `AssetAdaptor` (REST adaptor) | `IPSAssetDao` (final field) | downstream consumer (also ctor-injected). Class `@Lazy`. Not on folderHelper ctor path. |
| `PageAdaptor` (REST adaptor) | `IPSFolderHelper`, `IPSContentItemDao`, `IPSWidgetAssetRelationshipService`, `IPSAssetService` | downstream consumer (ctor-injected `final` fields; field `@Autowired` annotations redundant with ctor). Class `@Lazy`. Not on folderHelper ctor path. |
| `PSAssetRestService` | `IPSAssetService`, `IPSWidgetAssetRelationshipService`, `IPSRecycleService`, `IPSFolderHelper` | downstream REST facade (ctor-injected). Class `@Lazy`. Not on folderHelper ctor path. |
| `PSPageRestService` | `IPSRecycleService`, `IPSFolderHelper` | downstream REST facade (ctor-injected). Class `@Lazy`. Not on folderHelper ctor path. |
| `PSPageService` | `IPSPageDaoHelper`, `IPSFolderHelper`, `IPSWidgetAssetRelationshipService`, `IPSContentItemDao`, `IPSRecycleService` | downstream consumer hub (#2514 freeze). Class `@Lazy`. Not on folderHelper ctor path. |
| `PSItemService` | `IPSRecycleService` (field `@Autowired`) | downstream consumer. Not on folderHelper ctor path. |
| `PSAbstractWorkflowExtension` | `IPSWidgetAssetRelationshipService`, `IPSFolderHelper` | downstream extension. Not on folderHelper ctor path. |
| `PSLivePublishChangeHandler` | `IPSFolderHelper`, `IPSWidgetAssetRelationshipService` | downstream handler. Not on folderHelper ctor path. |
| `PSBulkApprovalJob` | `IPSFolderHelper` | downstream async job. Not on folderHelper ctor path. |
| `PSFolderService` | `IPSFolderHelper` | downstream REST facade. Not on folderHelper ctor path. |
| `PSTemplateService` | `IPSWidgetAssetRelationshipService`, `IPSPageDaoHelper` | downstream hub (#2477 freeze). Class `@Lazy`. Not on folderHelper ctor path. |
| `PSPageListViewProcessor` | `IPSPageDaoHelper` | downstream extension. Not on folderHelper ctor path. |
| `PSSiteTemplateService` | `IPSFolderHelper`, `IPSAssetService`, `IPSWidgetAssetRelationshipService` | downstream service. Not on folderHelper ctor path. |
| `PSSiteSectionService` | `IPSPageDaoHelper`, `IPSFolderHelper` | downstream service. Not on folderHelper ctor path. |
| `PSSiteSectionMetaDataService` | `IPSFolderHelper` | downstream service. Not on folderHelper ctor path. |
| `PSPageChangeHandler` | `IPSContentItemDao` | downstream handler. Not on folderHelper ctor path. |
| `PSSitePublishServiceWebAdapter` | `IPSFolderHelper` | downstream adapter. Not on folderHelper ctor path. |
| `PSPageCatalogService` | `IPSFolderHelper`, `IPSPageDaoHelper` | downstream REST facade. Not on folderHelper ctor path. |
| `PSSitePublishServiceHelper` | `IPSAssetService` | downstream helper. Not on folderHelper ctor path. |
| `PSSitePublishService` | `IPSWidgetAssetRelationshipService` | downstream service. Not on folderHelper ctor path. |
| `PSSiteDataService` | `IPSWidgetAssetRelationshipService`, `IPSAssetDao`, `IPSFolderHelper`, `IPSItemWorkflowService` (+ field page/path) | downstream hub (#2516 reverse freeze). Class `@Lazy`. Not on folderHelper ctor path. |
| `PSAssetUploadFolderPathMap` | `IPSFolderHelper` | downstream helper. Not on folderHelper ctor path. |
| `PSAssemblyItemBridge` | `IPSFolderHelper`, `IPSAssetService` | downstream assembler. Not on folderHelper ctor path. |
| `PSResourceAssemblyLocation` | `IPSFolderHelper`, `IPSAssetService` | downstream assembler. Not on folderHelper ctor path. |
| `PSResourceInstanceHelper` | `IPSAssetService` | downstream assembler. Not on folderHelper ctor path. |
| `PSRecyclePathItemService` | `IPSRecycleService` | downstream path item service. Not on folderHelper ctor path. |
| `PSDesignPathItemService` | `IPSFolderHelper` | downstream path item service. Not on folderHelper ctor path. |
| `PSPageUtils` | `IPSRecycleService`, `IPSContentItemDao` | downstream utility (static `@Autowired` fields). Not on folderHelper ctor path. |
| `PSPathService` | `IPSFolderHelper`, `IPSRecycleService` | downstream service. Class `@Lazy`. Not on folderHelper ctor path. |
| `PSDispatchingPathService` | `IPSRecycleService`, `IPSFolderHelper` | downstream service. Not on folderHelper ctor path. |
| `PSRedirectService` | `IPSFolderHelper` | downstream service. Not on folderHelper ctor path. |
| `PSTemplateDao` | `IPSContentItemDao`, `IPSFolderHelper` | downstream DAO. Not on folderHelper ctor path. |
| `PSEmptyRecycleService` | `IPSFolderHelper` | downstream empty-recycle service. Class `@Lazy`. Not on folderHelper ctor path. |
| `PSSharedRelationshipDeleteListener` | `IPSPageIndexService` | downstream listener. Not on folderHelper ctor path. |
| `PSSearchService` | `IPSFolderHelper`, `IPSRecycleService` | downstream search service. Not on folderHelper ctor path. |
| `PSSearchIndexFieldValueModifier` | `IPSFolderHelper` | downstream modifier. Not on folderHelper ctor path. |
| `PSAssetChangeListener` | `IPSWidgetAssetRelationshipService`, `IPSPageIndexService` | downstream listener. Not on folderHelper ctor path. |
| `PSSiteContentDao` | `IPSFolderHelper`, `IPSContentItemDao`, `IPSPageDaoHelper`, `IPSRecycleService` | downstream DAO. Class `@Lazy`. Not on folderHelper ctor path. |
| `PSRelationshipSummaryService` | `IPSWidgetAssetRelationshipService` | downstream service. Not on folderHelper ctor path. |
| `PSLinkExtractionHelper` | `IPSFolderHelper` | downstream importer helper. Not on folderHelper ctor path. |
| `PSPageExtractorHelper` | `IPSAssetService` | downstream importer helper. Not on folderHelper ctor path. |
| `PSCommentsService` | `IPSFolderHelper` | downstream service. Not on folderHelper ctor path. |
| `PSIntegrityCheckerService` | `IPSAssetService` | downstream service. Not on folderHelper ctor path. |
| `PSRecentService` | `IPSAssetService` | downstream service. Not on folderHelper ctor path. |
| `PSSiteImportLogViewer` | `IPSFolderHelper` (static field) | downstream importer. Not on folderHelper ctor path. |
| `PSTrafficService` | `IPSFolderHelper` (`final`) | downstream service (ctor-injected). Not on folderHelper ctor path. |

**Conclusion:** Every observed field-injected target type lives on a downstream consumer bean. None of the seven cycle-subgraph beans (`PSFolderHelper`, `PSRecycleService`, `PSWidgetAssetRelationshipService`, `PSAssetDao`, `PSContentItemDao`, `PSPageIndexService`, `PSPageDaoHelper`) carry any `@Autowired` / `@Resource` / `@Inject` field annotation on the target interfaces, nor any public setter that takes a target interface. The cycle subgraph is fully converted to constructor injection, so the existing `@Lazy` parameter breaks remain the only required protection.

### Cycle-subgraph intermediates that must NOT construct-require `folderHelper`

These sit on path A/B. Adding a ctor edge to `IPSFolderHelper` would short-circuit the known breaks:

| Bean | Current ctor deps of interest | Disposition |
|------|------------------------------|-------------|
| `PSRecycleService` | widgetAsset (+ system peers) | **Ban** ctor ΓåÆ folderHelper (would reverse `folderHelperΓåÆrecycle`) |
| `PSWidgetAssetRelationshipService` | assetDao, pageIndexService, ΓÇª | **Ban** ctor ΓåÆ folderHelper |
| `PSAssetDao` | contentItemDao only | **Ban** ctor ΓåÆ folderHelper |
| `PSPageIndexService` | pageDao, pageDaoHelper, ΓÇª | **Ban** ctor ΓåÆ folderHelper |
| `PSPageDao` | contentItemDao, ΓÇª | **Ban** ctor ΓåÆ folderHelper |

`managedLinkService` is resolved by `PSWidgetAssetRelationshipService` via application-context lookup (not ctor) — **hardened #2527** (keep it that way; freezes in hub reverse-edge test). A ctor inject of `IPSManagedLinkService` would pull `pageService` → `folderHelper` and form path C.

### Consumer-only injectors of `IPSFolderHelper` (no param `@Lazy` required)

Class-level `@Lazy` on these beans is **lazy init only** ΓÇö it is **false safety** for constructor cycles when an eager peer forces creation. OK **only while** live reverse edges above keep param `@Lazy`.

| Class | Class `@Lazy`? | Disposition |
|-------|----------------|-------------|
| `PSPageService` | yes | consumer / hub ΓÇö reverse-edge freeze done (#2514 / `PSPageServiceCycleWiringTest`) |
| `PSItemWorkflowService` | yes | consumer / hub ΓÇö reverse freeze #2478; cycle-peer **param `@Lazy`** #2515 |
| `PSSiteDataService` | yes | consumer / hub — reverse freeze #2516 / `PSSiteDataServiceHubReverseEdgeWiringTest` |
| `PSAssetService` | yes | consumer; folderHelper param **not** `@Lazy` (pageService param **is** `@Lazy` #2476) |
| `PSItemService` | no | consumer |
| `PSSiteContentDao` | yes | consumer (also takes pageDaoHelper ΓÇö not a reverse edge into folderHelper creation) |
| `PSTemplateDao` | yes | consumer |
| `PSEmptyRecycleService` | yes | consumer; not on folderHelper ctor path (depends on pathService) |
| `PSPathService` / path item services (`PSSitePathItemService`, `PSAssetPathItemService`, `PSRecyclePathItemService`, `PSSearchPathItemService`, `PSFileSystemPathItemService`, `PSDesignPathItemService`, `PSWebResourcesPathItemService`, `PSPathItemService`, `PSDispatchingPathService`) | mixed | path consumers |
| `PSFolderService`, `PSPageCatalogService`, `PSPageRestService`, `PSAssetRestService`, `PSCommentsService`, `PSCm1ListViewHelper`, `PSSiteSectionMetaDataService`, `PSSearchService`, `PSSearchIndexFieldValueModifier`, `PSTrafficService`, `PSCloudService`, `PSPageOptimizerService`, `PSLinkExtractionHelper`, `PSLivePublishChangeHandler`, `PSAssetUploadFolderPathMap` | mixed | product consumers / REST facades |

### False-safety note (class-level `@Lazy` only)

Spring class-level `@Lazy` defers bean *creation until first request*. It does **not** stop constructor dependency resolution once creation starts. Documented failure mode (#2437 Docker): class `@Lazy` on `PSFolderHelper` / `PSPageDaoHelper` still produced `BeanCurrentlyInCreationException` until **parameter** `@Lazy` was added on reverse edges.

## Inventory result: other constructor cycles

Among the mapped interest set (folder/recycle/asset/content/page/item/template/workflow/site hubs), the **only constructor cycle of length 2ΓÇô6** is the known folderHelper chain above.

No second closed constructor cycle was found in the current tree.

Field injection adds a few edges (e.g. `PSItemService` ΓåÆ `IPSRecycleService` field; `PSSiteDataService` ΓåÆ `IPSPageService` / path service field) but does not form an additional closed cycle in the combo graph under the same mapping.

## High-risk hubs (near-miss / future cycle fuel)

These beans have high constructor fan-in and sit on or next to the known cycle peers. They are **not** currently cyclic, but a single reverse ctor edge would re-create `BeanCurrentlyInCreationException`.

| Rank | Bean | Approx. ctor out / in (interest graph) | Notes |
|------|------|----------------------------------------|-------|
| 1 | `PSPageService` | out ~11 / in ~28 | Injects `contentItemDao`, `folderHelper`, `recycleService`, `widgetAsset`, `itemWorkflow`. Class `@Lazy`. Reverse-edge freeze: `PSPageServiceCycleWiringTest` (#2514 — ctor + non-`@Lazy` field bans on cycle peers; intentional `@Lazy` partner: `PSAssetService` #2476). |
| 2 | `PSItemWorkflowService` | out ~7 / in ~23 | Injects `assetDao`, `folderHelper`, `recycleService`, `widgetAsset` with **param `@Lazy`** (#2515). Class `@Lazy`. Reverse-edge freeze: `PSItemWorkflowServiceHubReverseEdgeWiringTest` (#2478). |
| 3 | `PSTemplateService` | out ~6 / in ~20 | Class `@Lazy` (2026-08-08, #2477). Forward ctor edges to `widgetAsset` / `pageDao` / `pageDaoHelper` / `templateDao` carry **param `@Lazy`** (2026-08-08, #2520). Belt-and-braces reverse-edge ban covered by `PSTemplateServiceCycleWiringTest`. |
| 4 | `PSWidgetAssetRelationshipService` | out ~4 / in ~18 | Class `@Lazy` (2026-08-09, #2519). On known cycle path. Reverse-edge freeze: `PSWidgetAssetRelationshipServiceHubReverseEdgeWiringTest`. |
| 5 | `PSAssetService` | out ~8 / in ~14 | Ctor takes `IPSPageService` with param `@Lazy` (#2476) (and folder/asset/widget/item). Class `@Lazy`. |
| 6 | `PSSiteDataService` | out ~15 / in ~12 | Wide hub; class `@Lazy`; field injects page/path. Reverse-edge freeze: `PSSiteDataServiceHubReverseEdgeWiringTest` (#2516). |

### Next hottest near-cycle edge (protected ΓÇö #2476)

**`PSAssetService` ΓåÆ `IPSPageService` (constructor, param `@Lazy` as of #2476)** with **no reverse** `PSPageService` ΓåÆ `IPSAssetService`.

| Aspect | Disposition |
|--------|-------------|
| Class-level `@Lazy` on both beans | Present but **not** a cycle breaker when an eager consumer forces construction |
| Param `@Lazy` on forward edge | **Added (#2476)** ΓÇö Spring injects a pageService proxy; safe because `PSAssetService` only calls `pageService` post-construction (`load` / `notifyPageChange` / `isPageItem` / etc.), never during the ctor body beyond a non-null check (proxy is non-null) |
| Reverse edge ban | Kept ΓÇö `PSPageService` must not construct-require `IPSAssetService` |
| Why not skip param `@Lazy` | Inventory residual after #2463: without it, a future reverse edge or multi-hop eager path would form a second `BeanCurrentlyInCreationException` independent of the folderHelper fix |

Protection test: `PSAssetServicePageServiceNearCycleWiringTest` (asserts one-way ctor edge + param `@Lazy` + no reverse).

### pageService hub reverse-edge freeze (#2514)

**`PSPageService`** is rank-1 (ctor out ~11 / in ~28). Forward construct-requires cycle peers; reverse edges into `IPSPageService` from those peers (or eager field inject) are frozen.

#### Cycle peers scanned (ctor + field)

| Peer class | Ctor → `IPSPageService` | Field → `IPSPageService` | Disposition |
|------------|-------------------------|--------------------------|-------------|
| `PSFolderHelper` | none | none | **Ban** reverse |
| `PSContentItemDao` | none | none | **Ban** reverse (cycle-break peer) |
| `PSRecycleService` | none | none | **Ban** reverse |
| `PSWidgetAssetRelationshipService` | none | none | **Ban** reverse |
| `PSItemWorkflowService` | none | none (removed unused field 2026-08-09) | **Ban** reverse; dead `pageService` field removed |
| `PSAssetDao` | none | none | **Ban** reverse (cycle intermediate) |

#### Intentional `@Lazy` reverse partner (not banned)

| From bean | Edge | Disposition | Regression test |
|-----------|------|-------------|-----------------|
| `PSAssetService` | ctor → `IPSPageService` **param `@Lazy`** | **Intentional** consumer edge (#2476); keep `@Lazy` | `PSPageServiceCycleWiringTest.assetServiceIntentionalPageServiceEdgeIsLazy` + `PSAssetServicePageServiceNearCycleWiringTest` |

#### Freeze coverage (`PSPageServiceCycleWiringTest`)

- Class `@Lazy` on `PSPageService`
- Forward edges still present (folderHelper, contentItemDao, recycleService, widgetAsset, itemWorkflow)
- Cycle peers: no reverse ctor without `@Lazy`; no eager field inject without `@Lazy`
- Explicit `contentItemDao` no reverse edge
- Documented intentional `PSAssetService` `@Lazy` partner

**No additional production `@Lazy` required** for #2514 beyond the dead-field cleanup on `PSItemWorkflowService`.

### ItemWorkflow hub cycle-peer param `@Lazy` (#2515)

**`PSItemWorkflowService` ΓåÆ known-cycle peers (constructor, param `@Lazy` as of #2515)** with **no reverse** peer ΓåÆ `IPSItemWorkflowService` (frozen by #2478).

Behavior review of call sites (safe for lazy proxy):

| Peer param | Ctor body | Post-construction use | Param `@Lazy`? |
|------------|-----------|----------------------|----------------|
| `IPSAssetDao` | field assign only | **unused** in production methods today | **yes** |
| `IPSFolderHelper` | field assign only | folder path / access / workflow id on transition paths | **yes** |
| `IPSRecycleService` | field assign only | `isInRecycler` checks | **yes** |
| `IPSWidgetAssetRelationshipService` | field assign only | shared/linked/local asset relations on checkout/transition | **yes** |

| Aspect | Disposition |
|--------|-------------|
| Class-level `@Lazy` on hub | Present but **not** a cycle breaker when an eager consumer forces construction |
| Param `@Lazy` on four forward edges | **Added (#2515)** ΓÇö Spring injects proxies; safe because ctor never method-calls peers |
| Reverse edge ban | Kept ΓÇö #2478 / `PSItemWorkflowServiceHubReverseEdgeWiringTest` |
| Why apply now | Residual after #2478 reverse-edge freezes: belt-and-braces peer of #2476 so multi-hop eager paths through cycle peers cannot form a second `BeanCurrentlyInCreationException` independent of the folderHelper fix |

Protection test: `PSItemWorkflowServiceCycleLazyWiringTest` (asserts construct-require + param `@Lazy` on each chosen peer).

### TemplateService hub forward-edge param `@Lazy` (#2520)

**`PSTemplateService` → forward ctor edges to known-cycle peers (constructor, param `@Lazy` as of #2520)** with **no reverse** peer → `IPSTemplateService` (frozen by #2477 / `PSTemplateServiceCycleWiringTest`).

Behavior review of call sites (safe for lazy proxy):

| Peer param | Ctor body | Post-construction use | Param `@Lazy`? |
|------------|-----------|----------------------|----------------|
| `IPSTemplateDao` | field assign only | save / load / find / generateTemplate | **yes** |
| `IPSWidgetAssetRelationshipService` | field assign only | shared/linked/local asset relations on save / import / create | **yes** |
| `IPSPageDao` | field assign only | `isValidPageId` on save | **yes** |
| `IPSPageDaoHelper` | field assign only | `findPageIdsByTemplateInRecentRevision` / `replaceTemplateForPageInOlderRevisions` on delete | **yes** |
| `IPSWidgetService` | **used in ctor** (`new RegionWidgetValidator(widgetService)`) | n/a | **no** — lazy proxy not safe here |

| Aspect | Disposition |
|--------|-------------|
| Class-level `@Lazy` on hub | Present (#2477) but **not** a cycle breaker when an eager consumer forces construction |
| Param `@Lazy` on four forward edges | **Added (#2520)** — Spring injects proxies; safe because ctor never method-calls those peers (widgetService is intentionally excluded — it is consumed in the ctor body) |
| Reverse edge ban | Kept — #2477 / `PSTemplateServiceCycleWiringTest` |
| Why apply now | Residual after #2477 class-level + reverse-edge freeze: belt-and-braces peer of #2476 / #2515 so multi-hop eager paths through cycle peers cannot form a second `BeanCurrentlyInCreationException` independent of the folderHelper fix |

Protection test: `PSTemplateServiceParamLazyWiringTest` (asserts construct-require + param `@Lazy` on each chosen peer; widgetService intentionally excluded).
### siteDataService hub reverse-edge freeze (protected — #2516)

**`PSSiteDataService` → cycle peers / page-item hubs (constructor + page/path field inject, one-way)** with **no reverse** peer → `IPSSiteDataService` without param/field `@Lazy`.

Forward edges frozen by the UnitTest:

| Direction | Edge | Notes |
|-----------|------|-------|
| Ctor forward | siteData → `IPSFolderHelper`, `IPSWidgetAssetRelationshipService`, `IPSAssetDao`, `IPSItemWorkflowService` | Hub model (#2463 rank 6) |
| Field forward | siteData → `PSPageService` / `PSPathService` (`@Autowired` fields) | Documented; not reverse-cycle fuel |
| Reverse ban (ctor + non-`@Lazy` field) | cycle peers + page/item hubs must not inject `IPSSiteDataService` | Peers: folderHelper, contentItemDao, widgetAsset, recycle, assetDao, pageDaoHelper, pageService, itemWorkflow, assetService, templateService |

**Scan snapshot (#2516):** none of the reverse-ban peers currently inject `IPSSiteDataService` (ctor or field). **Intentional reverse `@Lazy` exceptions:** none. Downstream consumers (path item services, REST adaptors, publish handlers, traffic/category services) remain allowed — they are not cycle fuel.

| Aspect | Disposition |
|--------|-------------|
| Class-level `@Lazy` on hub | Present but **not** a cycle breaker when an eager consumer forces construction |
| Param `@Lazy` on reverse edges | **Not needed today** — no live reverse edges; freeze forbids adding them without `@Lazy` |
| Reverse edge ban | **Hard** — #2516 / `PSSiteDataServiceHubReverseEdgeWiringTest` |
| Why apply now | Residual after #2463 rank-6 inventory + #2478 pattern: freeze so multi-hop eager paths through cycle peers cannot form a second `BeanCurrentlyInCreationException` via siteData |

Protection test: `PSSiteDataServiceHubReverseEdgeWiringTest` (forward freeze + reverse ctor/field ban; intentional exceptions documented above).

### WidgetAsset hub class `@Lazy` + reverse-edge freezes (#2519)

**`PSWidgetAssetRelationshipService`** sits **on** the known folderHelper creation path (paths A/B above) with high fan-in from recycle / page / template / itemWorkflow / assetService.

| Aspect | Disposition |
|--------|-------------|
| Class-level `@Lazy` on hub | **Added (#2519)** — hub alignment with page/template/itemWorkflow; defers init until first use. **Not** a constructor-edge cycle breaker when an eager consumer forces construction |
| Forward edges kept | widgetAsset → `IPSAssetDao` / `IPSPageIndexService` (path A/B); recycle / page / template still construct-require the hub (intentional fan-in) |
| Reverse edge ban | **Hard** — cycle-path peers (`assetDao`, `contentItemDao`, `folderHelper`, `pageIndexService`, `pageDaoHelper`) must not construct-require or eagerly field-inject `IPSWidgetAssetRelationshipService` without param/field `@Lazy`. Hub must not reverse-require `IPSRecycleService` / `IPSPageService` / `IPSTemplateService` (would reverse known fan-in / mid-chain edges) |
| Param `@Lazy` on forward cycle edges | **Not** added here — contentItemDao / pageDaoHelper already carry the cycle breaks; optional belt-and-braces on consumers remains separate residual territory |
| managedLink path C | **Hardened (#2527)** — see section below |

Protection test: `PSWidgetAssetRelationshipServiceHubReverseEdgeWiringTest` (class `@Lazy` + forward freezes + reverse ctor/field bans + named assetDao/recycle/page/template assertions + path-C managedLink freezes).

### WidgetAsset managedLink path C (#2527)

**Hazard:** `PSManagedLinkService` construct-requires `IPSPageService`, which construct-requires `folderHelper`. If `PSWidgetAssetRelationshipService` (on paths A/B) gained a ctor or eager field inject of `IPSManagedLinkService`, Spring would force `managedLink → pageService → folderHelper` while folderHelper may still be under construction → third reverse path (**path C**).

| Aspect | Disposition |
|--------|-------------|
| Production wiring | **Keep application-context lookup** (`getManagedLinkService()`); field is post-lookup cache only — not `@Autowired` |
| Class `@Lazy` on hub | Already present (#2519); **not** a substitute for keeping managedLink off the ctor |
| Ctor inject of managedLink | **Hard ban** — do not add `IPSManagedLinkService` to widgetAsset ctor (even "cleanup"); if DI is preferred later, use param `@Lazy` + inventory update |
| Eager field `@Autowired` of managedLink | **Hard ban** without field `@Lazy` |
| managedLink reverse freezes | managedLink must not construct-require `folderHelper` / `widgetAsset` / `recycleService` |
| Force-chain witness | managedLink still construct-requires `pageService` (documents why path C is dangerous) |
| Seed ban | Also covered by `PSFolderHelperReverseEdgeInventoryWiringTest` (#2485) |

Protection test: `PSWidgetAssetRelationshipServiceHubReverseEdgeWiringTest` path-C methods (`widgetAssetMustNotConstructRequireManagedLinkService`, field non-eager autowire, managedLink pageService force-chain + reverse bans).


### assetServiceΓåötemplateService near-cycle (protected ΓÇö #2521)

**`PSAssetService` ΓåÆ `IPSTemplateService` (constructor, one-way)** with **no reverse** `PSTemplateService` ΓåÆ `IPSAssetService` (ctor or non-`@Lazy` field).

| Aspect | Disposition |
|--------|-------------|
| Forward edge | **Frozen** ΓÇö `PSAssetService` still construct-requires `@Qualifier("sys_templateService") IPSTemplateService` |
| Param `@Lazy` on forward edge | **Optional / not required** ΓÇö `templateService` is only used post-construction (`load` / `find`); ctor body does a non-null check only (proxy-safe if `@Lazy` were added later). Reverse-edge ban is the hard gate; do not treat class `@Lazy` on assetService as a cycle breaker |
| Reverse edge ban | **Hard** ΓÇö `PSTemplateService` must not construct-require or eagerly field-inject `IPSAssetService` without param/field `@Lazy` |
| Relation to #2477 | #2477 hardens templateService as a hub vs cycle peers (class `@Lazy`, peer reverse bans). This slice freezes the **assetServiceΓåötemplateService pair** specifically; do not absorb into pageService/template/folderHelper cluster PRs without reconciling the forward freeze |

Protection test: `PSAssetServiceTemplateServiceNearCycleWiringTest` (one-way freeze + reverse ctor/field ban; param `@Lazy` disposition documented, not required).

### Other one-way edges to keep one-way (known cycle intermediate)

These must not gain reverse constructor dependencies:

| From | To | Reverse would mean |
|------|----|--------------------|
| `PSAssetDao` | `IPSContentItemDao` | contentItemDao ΓåÆ assetDao closes dao sub-cycle |
| `PSWidgetAssetRelationshipService` | `IPSAssetDao` | assetDao ΓåÆ widgetAsset skips contentItemDao break |
| `PSRecycleService` | `IPSWidgetAssetRelationshipService` | widgetAsset ΓåÆ recycle closes mid-chain |
| `PSFolderHelper` | `IPSRecycleService` | recycle ΓåÆ folderHelper bypasses contentItemDao `@Lazy` |

`FolderHelperCycleContextTest` (#2436) + `PSContentItemDaoCycleLazyWiringTest` (#2435) cover the `@Lazy` break; intermediate reverse edges are residual hardening candidates.

## Constructor `@Lazy` parameter edges found (sitemanage)

Constructor-parameter `@Lazy` remains rare in this module. Scan snapshot (after #2485):

- `PSContentItemDao` → `IPSFolderHelper` (`@Lazy`) — known reverse edge path A (#2435)
- `PSPageDaoHelper` → `IPSFolderHelper` (`@Lazy`) — known reverse edge path B (#2437)
- `PSFolderHelper` → `IPSRecycleService` (`@Lazy`) — forward deferral of recycle subgraph (#2437)
- `PSAssetService` → `IPSPageService` (`@Lazy`) — near-cycle belt-and-braces (#2476)
- `PSItemWorkflowService` → `IPSAssetDao` / `IPSFolderHelper` / `IPSRecycleService` / `IPSWidgetAssetRelationshipService` (`@Lazy`) — hub cycle-peer belt-and-braces (#2515)
- `PSTemplateService` → `IPSTemplateDao` / `IPSWidgetAssetRelationshipService` / `IPSPageDao` / `IPSPageDaoHelper` (`@Lazy`) — hub forward-edge belt-and-braces (#2520)
- `PSRecentRestService` → `IPSRecentService` (`@Lazy`) — rest convenience, not cycle-related

Class-level `@Lazy` is common on REST facades and many services; treat it as lazy *init*, not a cycle breaker.

## Who constructs `IPSFolderHelper` (no param `@Lazy`)

Many path/item services inject `IPSFolderHelper` without parameter `@Lazy` (e.g. `PSPageService`, `PSItemService`, `PSSiteDataService`, path item services). `PSItemWorkflowService` now **does** use param `@Lazy` on `folderHelper` and other cycle peers (#2515). Remaining consumer-only injectors are OK **only while** the live reverse edges above keep param `@Lazy` (or another edge on the same cycle is broken). Full table: **folderHelper reverse-edge inventory (#2485)** above.

## Disposition for #2463

| Acceptance item | Status |
|-----------------|--------|
| Inventory of high-risk edges | This note + issue comment |
| Additional regression/protection test for next hottest edge | `PSAssetServicePageServiceNearCycleWiringTest` |
| Explicit disposition if no second full cycle | **No second closed constructor cycle found**; residual work is reverse-edge hardening + Docker smoke (#2437) |
| Module clean install | Required on PR |

## Disposition for #2485

| Acceptance item | Status |
|-----------------|--------|
| Inventory table of folderHelper reverse edges + disposition | **folderHelper reverse-edge inventory (#2485)** section above |
| Any remaining live cycle edges fixed + unit tests | **None remaining** ΓÇö path A/B already `@Lazy`; freeze tests added |
| Link #2463 / #2476+ without duplicating hubs | Hub residuals stay #2476ΓÇô#2478 / #2514ΓÇô#2521; this slice freezes folderHelper reverse edges only |
| Regression peer | `PSFolderHelperReverseEdgeInventoryWiringTest` |

## Disposition for #2525

| Acceptance item | Status |
|-----------------|--------|
| Field/setter inject inventory table (edge + disposition) linked from parent progress | **folderHelper field / setter injection inventory (#2525)** section above |
| Any live reverse field edges fixed with param/field `@Lazy` + unit tests | **None** — cycle subgraph (7 classes) uses pure ctor injection; no field/setter target-type injection found; no production `@Lazy` change required |
| No duplication of hub ctor work (#2476–#2478 / #2514–#2521) — field-only scope | Respected — only field-only scan + freeze test; no ctor changes; no peer-rebuild of hub tests |
| `projects/sitemanage` standalone `mvnw clean install` green | Required on PR |
| Regression peer | `PSFolderHelperFieldInjectionInventoryWiringTest` |
| Residual | Item 10 of the residual recommendations below is closed by this slice |

## Disposition for #2526

| Acceptance item | Status |
|-----------------|--------|
| emptyRecycle / pathService edges inventoried with disposition | **emptyRecycle / pathService near-cycle inventory (#2526)** section below |
| Param `@Lazy` and/or reverse-edge freeze tests as needed | **Both** — `PSEmptyRecycleService` → `IPSFolderHelper` ctor param now `@Lazy`; freeze test bans `recycleService` → `emptyRecycle`/`pathService` and `pathService` → `emptyRecycle` reverse ctor edges (with `@Lazy` exception path) |
| Peers of `PSFolderHelperReverseEdgeInventoryWiringTest` | `PSEmptyRecycleServiceCycleLazyWiringTest` (new) |
| sitemanage clean install green | Required on PR |

## emptyRecycle / pathService near-cycle inventory (#2526)

**Definition — near-cycle:** a constructor edge into a known cycle subgraph from a consumer
bean that is **not** itself on the cycle path today but would close a new cycle path under a
single additional reverse edge. Belt-and-braces protection uses parameter {@code @Lazy} on the
edge into the cycle subgraph (so the consumer does not force cycle-subgraph construction
synchronously) plus a reflection test that bans a future reverse ctor edge from the cycle peers.

### Live belt-and-braces param `@Lazy` (added #2526)

| From bean | Edge | Disposition | Regression test |
|-----------|------|-------------|-----------------|
| `PSEmptyRecycleService` | ctor → `IPSFolderHelper` | **param `@Lazy`** (#2526) — ctor body only field-assigns `folderHelper`; field is only used in `purgeLeaf` (post-construction). Spring injects a proxy. | `PSEmptyRecycleServiceCycleLazyWiringTest` |

Class-level `@Lazy` on `PSEmptyRecycleService` is **not** a cycle breaker when an eager peer
forces creation; parameter `@Lazy` is required to inject a proxy and break the eager
construction edge. The consumer-edge justification from #2485 still holds: `emptyRecycle` is
not on `folderHelper`'s construction path, so this `@Lazy` is **belt-and-braces** rather than
a cycle-break fix.

### freeze test bans (#2526)

These ctor edges would close a new mid-cycle and must not be added without parameter
`@Lazy` (and an inventory note documenting the intentional edge):

| From bean | Banned edge | Why banned |
|-----------|-------------|------------|
| `PSRecycleService` | ctor → `IPSEmptyRecycleService` | would pull `pathService` + `folderHelper` mid-cycle (already on `folderHelper`'s construction path A/B) |
| `PSRecycleService` | ctor → `IPSPathService` | would form a path/recycle cross-wire cycle (pathService → folderHelper; recycleService → folderHelper) |
| `PSPathService` | ctor → `IPSEmptyRecycleService` | would re-enter `folderHelper` via emptyRecycle's ctor param and couple two near-cycle hubs |

Enforced by `PSEmptyRecycleServiceCycleLazyWiringTest` (peer of
`PSFolderHelperReverseEdgeInventoryWiringTest`, `PSContentItemDaoCycleLazyWiringTest`,
`PSAssetServicePageServiceNearCycleWiringTest`).

### Other path item services that inject `folderHelper` and recycle peers (#2526)

The path item services (`PSSitePathItemService`, `PSAssetPathItemService`,
`PSRecyclePathItemService`, `PSSearchPathItemService`, `PSFileSystemPathItemService`,
`PSDesignPathItemService`, `PSWebResourcesPathItemService`, `PSPathItemService`,
`PSDispatchingPathService`) inject `IPSFolderHelper` + `IPSRecycleService` + cycle peers via
`PSPathItemService`'s ctor. They are **consumer-only** of the cycle subgraph (none are on the
known cycle path A/B), and class-level `@Lazy` is present on the named beans above. They do
**not** need parameter `@Lazy` on `folderHelper`/`recycle` ctor params today because they are
not in the cycle construction path; the freeze tests above guard the most likely future
reverse edges. Field `@Autowired` of these cycle peers on the path services is the residual
listed in the original inventory and is out of scope for #2526.

## Residual recommendations (file as GitHub issues under #2423)

1. Optional: protect intermediate known-cycle reverse edges with reflection tests (assetDao/widgetAsset/recycle one-way) ΓÇö **covered in** `PSAssetServicePageServiceNearCycleWiringTest` (#2463) + `PSFolderHelperReverseEdgeInventoryWiringTest` (#2485).
2. ~~Optional: add `@Lazy` on `PSAssetService`'s `IPSPageService` ctor param~~ ΓÇö **done (#2476)**.
3. ~~ItemWorkflow hub reverse-edge tests.~~ Done: `PSItemWorkflowServiceHubReverseEdgeWiringTest` (#2478).
4. ~~Hub hardening: `PSTemplateService` class `@Lazy` + reverse-edge tests.~~ **Done (#2477)** ΓÇö `PSTemplateServiceCycleWiringTest`.
5. ~~`PSPageService` reverse-edge freeze.~~ **Done (#2514)** ΓÇö `PSPageServiceCycleWiringTest`.
6. ~~Optional: param `@Lazy` on itemWorkflow ΓåÆ cycle peers.~~ **Done (#2515)** ΓÇö `PSItemWorkflowServiceCycleLazyWiringTest`.
7. ~~Optional: param `@Lazy` on templateService → forward-cycle peers.~~ **Done (#2520)** — `PSTemplateServiceParamLazyWiringTest`. widgetService intentionally excluded (consumed in ctor body).
8. ~~assetService↔templateService one-way freeze.~~ **Done (#2521)** — `PSAssetServiceTemplateServiceNearCycleWiringTest`.
9. Keep Docker `qa-up` / Rhythmyx health smoke (#2437) as the production-level gate.
10. When adding new `@Autowired` constructors on cycle peers, re-run this inventory method (or extend the reflection tests).
11. ~~Optional next hubs: siteData reverse-edge freeze.~~ **Done (#2516)** — `PSSiteDataServiceHubReverseEdgeWiringTest`. ~~widgetAsset class `@Lazy`.~~ **Done (#2519)** — class `@Lazy` + `PSWidgetAssetRelationshipServiceHubReverseEdgeWiringTest`.
12. ~~Optional residual: field-injection inventory for `IPSFolderHelper` / recycle subgraph (ctor inventory complete under #2485).~~ **Done (#2525)** — `PSFolderHelperFieldInjectionInventoryWiringTest`; cycle subgraph is fully ctor-injected, no live reverse field edges.
13. Optional residual: belt-and-braces param `@Lazy` on other high-fan-in consumer hubs (e.g. pageService / siteData) that inject cycle peers ΓÇö only if product risk warrants; do not treat class `@Lazy` as the fix.
14. ~~widgetAsset managedLink path-C reverse-edge hardening.~~ **Done (#2527)** — keep context lookup; freezes in `PSWidgetAssetRelationshipServiceHubReverseEdgeWiringTest` + inventory note.

## Related

- Parent #2423 ΓÇö startup cycle epic  
- #2435 ΓÇö `@Lazy` break contentItemDao (merged)  
- #2436 ΓÇö `FolderHelperCycleContextTest` graph witness  
- #2437 ΓÇö Docker qa-up health/login smoke + pageDaoHelper / recycle `@Lazy` (PR #2483)  
- #2463 ΓÇö high-fan-in inventory beyond folderHelper  
- #2476 ΓÇö param `@Lazy` on assetServiceΓåÆpageService  
- #2477 ΓÇö templateService hub hardening (`PSTemplateServiceCycleWiringTest`)  
- #2478 ΓÇö itemWorkflow hub reverse-edge protection (`PSItemWorkflowServiceHubReverseEdgeWiringTest`)  
- #2485 ΓÇö folderHelper reverse-edge inventory  
- #2514 ΓÇö pageService hub reverse-edge freeze (`PSPageServiceCycleWiringTest`)  
- #2515 ΓÇö itemWorkflow cycle-peer param `@Lazy` (`PSItemWorkflowServiceCycleLazyWiringTest`)  
- #2516 — siteDataService hub reverse-edge freeze (`PSSiteDataServiceHubReverseEdgeWiringTest`)
- #2519 — widgetAsset hub class `@Lazy` + reverse-edge freeze (`PSWidgetAssetRelationshipServiceHubReverseEdgeWiringTest`)
- #2520 — templateService forward-edge param `@Lazy` (`PSTemplateServiceParamLazyWiringTest`)
- #2521 — assetService↔templateService one-way freeze + reverse ban  
- #2526 — emptyRecycle / pathService near-cycle belt-and-braces (`PSEmptyRecycleServiceCycleLazyWiringTest`)
- #2527 — widgetAsset managedLink path-C reverse-edge hardening (context lookup freeze + managedLink reverse bans)

- #2457 / PR #2469 ΓÇö JDK 21 lambda compile fix often needed to build sitemanage tests
