# Sitemanage Spring constructor-injection cycle inventory

**Issue:** #2463 (residual of #2423)  
**Module:** `projects/sitemanage`  
**Date:** 2026-08-08  
**Method:** Static scan of `@Autowired` constructors + field `@Autowired` among high-fan-in sitemanage beans (interfaces mapped to primary impls). Reflection peers: `PSContentItemDaoCycleLazyWiringTest`, `FolderHelperCycleContextTest` (#2436).

This is an analysis note for humans/agents — **not** an agent rule file.

## Known cycle (fixed)

```text
folderHelper
  → recycleService
    → widgetAssetRelationshipService
      → assetDao
        → contentItemDao
          → folderHelper   ← back-edge broken with @Lazy on PSContentItemDao ctor param
```

| Edge | Type | Breaker |
|------|------|---------|
| `PSFolderHelper` → `IPSRecycleService` | ctor | none (forward) |
| `PSRecycleService` → `IPSWidgetAssetRelationshipService` | ctor | none (forward) |
| `PSWidgetAssetRelationshipService` → `IPSAssetDao` | ctor | none (forward) |
| `PSAssetDao` → `IPSContentItemDao` | ctor | none (forward) |
| `PSContentItemDao` → `IPSFolderHelper` | ctor | **`@Lazy`** (#2435) |

Class-level `@Lazy` is present on `folderHelper` and `recycleService` but is **not** sufficient alone: an eager consumer (e.g. field inject on `pSRedirectService`) still forces full construction and re-enters the cycle without a parameter-level `@Lazy` proxy.

## Inventory result: other constructor cycles

Among the mapped interest set (folder/recycle/asset/content/page/item/template/workflow/site hubs), the **only constructor cycle of length 2–6** is the known folderHelper chain above.

No second closed constructor cycle was found in the current tree.

Field injection adds a few edges (e.g. `PSItemService` → `IPSRecycleService` field; `PSSiteDataService` → `IPSPageService` / path service field) but does not form an additional closed cycle in the combo graph under the same mapping.

## High-risk hubs (near-miss / future cycle fuel)

These beans have high constructor fan-in and sit on or next to the known cycle peers. They are **not** currently cyclic, but a single reverse ctor edge would re-create `BeanCurrentlyInCreationException`.

| Rank | Bean | Approx. ctor out / in (interest graph) | Notes |
|------|------|----------------------------------------|-------|
| 1 | `PSPageService` | out ~11 / in ~28 | Injects `contentItemDao`, `folderHelper`, `recycleService`, `widgetAsset`, `itemWorkflow`. Class `@Lazy`. |
| 2 | `PSItemWorkflowService` | out ~7 / in ~23 | Injects `assetDao`, `folderHelper`, `recycleService`, `widgetAsset`. Class `@Lazy`. |
| 3 | `PSTemplateService` | out ~6 / in ~20 | **Not** class `@Lazy`. Injects `widgetAsset`, page/template DAOs. |
| 4 | `PSWidgetAssetRelationshipService` | out ~4 / in ~18 | **Not** class `@Lazy`. On known cycle path. |
| 5 | `PSAssetService` | out ~8 / in ~14 | Ctor takes `IPSPageService` with param `@Lazy` (#2476) (and folder/asset/widget/item). Class `@Lazy`. |
| 6 | `PSSiteDataService` | out ~15 / in ~12 | Wide hub; class `@Lazy`; field injects page/path. |

### Next hottest near-cycle edge (protected — #2476)

**`PSAssetService` → `IPSPageService` (constructor, param `@Lazy` as of #2476)** with **no reverse** `PSPageService` → `IPSAssetService`.

| Aspect | Disposition |
|--------|-------------|
| Class-level `@Lazy` on both beans | Present but **not** a cycle breaker when an eager consumer forces construction |
| Param `@Lazy` on forward edge | **Added (#2476)** — Spring injects a pageService proxy; safe because `PSAssetService` only calls `pageService` post-construction (`load` / `notifyPageChange` / `isPageItem` / etc.), never during the ctor body beyond a non-null check (proxy is non-null) |
| Reverse edge ban | Kept — `PSPageService` must not construct-require `IPSAssetService` |
| Why not skip param `@Lazy` | Inventory residual after #2463: without it, a future reverse edge or multi-hop eager path would form a second `BeanCurrentlyInCreationException` independent of the folderHelper fix |

Protection test: `PSAssetServicePageServiceNearCycleWiringTest` (asserts one-way ctor edge + param `@Lazy` + no reverse).

### Other one-way edges to keep one-way (known cycle intermediate)

These must not gain reverse constructor dependencies:

| From | To | Reverse would mean |
|------|----|--------------------|
| `PSAssetDao` | `IPSContentItemDao` | contentItemDao → assetDao closes dao sub-cycle |
| `PSWidgetAssetRelationshipService` | `IPSAssetDao` | assetDao → widgetAsset skips contentItemDao break |
| `PSRecycleService` | `IPSWidgetAssetRelationshipService` | widgetAsset → recycle closes mid-chain |
| `PSFolderHelper` | `IPSRecycleService` | recycle → folderHelper bypasses contentItemDao `@Lazy` |

`FolderHelperCycleContextTest` (#2436) + `PSContentItemDaoCycleLazyWiringTest` (#2435) cover the `@Lazy` break; intermediate reverse edges are residual hardening candidates.

## Constructor `@Lazy` parameter edges found (sitemanage)

Constructor-parameter `@Lazy` remains rare in this module. Scan snapshot (after #2476):

- `PSContentItemDao` → `IPSFolderHelper` (`@Lazy`) — known cycle break (#2435)
- `PSAssetService` → `IPSPageService` (`@Lazy`) — near-cycle belt-and-braces (#2476)
- `PSRecentRestService` → `IPSRecentService` (`@Lazy`) — rest convenience, not cycle-related

Class-level `@Lazy` is common on REST facades and many services; treat it as lazy *init*, not a cycle breaker.

## Who constructs `IPSFolderHelper` (no param `@Lazy`)

Many path/item services inject `IPSFolderHelper` without parameter `@Lazy` (e.g. `PSPageService`, `PSItemService`, `PSItemWorkflowService`, `PSSiteDataService`, path item services). That is OK **only while** the back-edge `contentItemDao → folderHelper` remains `@Lazy` (or another edge on the same cycle is broken).

## Disposition for #2463

| Acceptance item | Status |
|-----------------|--------|
| Inventory of high-risk edges | This note + issue comment |
| Additional regression/protection test for next hottest edge | `PSAssetServicePageServiceNearCycleWiringTest` |
| Explicit disposition if no second full cycle | **No second closed constructor cycle found**; residual work is reverse-edge hardening + Docker smoke (#2437) |
| Module clean install | Required on PR |

## Residual recommendations (file as GitHub issues under #2423)

1. Optional: protect intermediate known-cycle reverse edges with reflection tests (assetDao/widgetAsset/recycle one-way) — **covered in** `PSAssetServicePageServiceNearCycleWiringTest.knownCycleIntermediateBeansHaveNoReverseConstructorEdges` (#2463).
2. ~~Optional: add `@Lazy` on `PSAssetService`'s `IPSPageService` ctor param~~ — **done (#2476)**.
3. Keep Docker `qa-up` / Rhythmyx health smoke (#2437) as the production-level gate.
4. When adding new `@Autowired` constructors on cycle peers, re-run this inventory method (or extend the reflection tests).
5. Hub hardening still open as separate residuals: templateService (#2477), itemWorkflow reverse-edge tests (#2478).

## Related

- Parent #2423 — startup cycle epic  
- #2435 — `@Lazy` break (merged)  
- #2436 — `FolderHelperCycleContextTest` graph witness  
- #2437 — Docker qa-up health/login smoke  
- #2463 — this residual inventory  
- #2476 — param `@Lazy` on assetService→pageService  
- #2457 / PR #2469 — JDK 21 lambda compile fix often needed to build sitemanage tests
