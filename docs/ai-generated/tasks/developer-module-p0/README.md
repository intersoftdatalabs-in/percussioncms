# Developer module — P0 (CMS design tools)

| Field | Value |
|-------|--------|
| **Status** | **In progress** — shell + CT list + read-only field detail |
| **FR source** | [`docs/developer-module/workbench-functional-inventory.md`](../../developer-module/workbench-functional-inventory.md) §15 P0 |
| **Pipeline track** | [`docs/developer-module/data-pipeline-engine-inventory.md`](../../developer-module/data-pipeline-engine-inventory.md) (parallel; not blocking P0) |
| **Related** | [design-templates-item-types](../design-templates-item-types/README.md) (CM1 template library — complementary, not a substitute) |

## P0 scope (from FR)

| Capability | FR IDs (sample) | SPA slice |
|------------|-----------------|-----------|
| Connection / design session | §5.1 | Use SPA login session (no multi-server profiles in v1) |
| Content types list/open | CD-01 | **P0.1** list via REST |
| Content type fields editor | CD-03–CD-11 | P0.2+ (API survey first) |
| Shared fields / system def | CD-15–CD-16 | P0.3 |
| Keywords | CD-17 | P0.3 |
| Templates + source/bindings/slots | AS-03–AS-06 | P0.4 |
| Slots | AS-01–AS-02 | P0.4 |
| Communities + object ACL | SE-01, SE-04, §5.4 | P0.5 |
| Locking / save / problems | §5.2–5.3 | Cross-cutting as editors land |

## Done in this branch

- [x] SPA route `/developer` (+ section) for Admin or Designer  
- [x] Top nav **Developer** (replaces legacy Design full-page exit for designers)  
- [x] Deep-link entry `developer` (query + path fallback filter)  
- [x] Module shell with sections: Content Types, Templates, Slots, Keywords, Communities, Pipelines (placeholder)  
- [x] **Content Types** panel: list from `GET /services/contenttypes`  
- [x] **P0.2** `GET /services/contenttypes/{idOrName}` field catalog + SPA detail view  
- [x] **P0.2b** Allowed workflows + default workflow + allowed templates on CT detail  
- [x] **P0.3** Keywords list `GET /services/keywords` + SPA Keywords panel (read-only)  
- [x] **P0.4** Templates `GET /services/templates` + Slots `GET /services/slots` + SPA panels  
- [x] **P0.5** Communities list via `GET /services/communities/find?name=*` + SPA panel  
- [x] Gap map: [content-type-api-gaps.md](./content-type-api-gaps.md)  
- [x] Vitest coverage for shell + catalogs  

## Next slices

1. Template/slot/CT detail editors (write + field rules)  
2. Keyword create/edit; community roles + ACL dialogs  
3. Pipelines track (Slice A IR)  
4. Server runtime map for data pipelines

## Product locks

- React + TypeScript only under `WebUI/src/main/ts`  
- No jQuery in SPA  
- Prefer `rest` module OpenAPI; document gaps when Workbench design objects lack public REST  
- Shell ≠ done: list is a gate, not acceptance of full CT editor  
