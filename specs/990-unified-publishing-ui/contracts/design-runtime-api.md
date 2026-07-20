# Contract: Design & Runtime API Façade (to implement)

**Feature**: `990-unified-publishing-ui`  
**Status**: Target contract for US4/US5. **Not all endpoints exist as JSON today**—implement as thin REST wrapping existing Java services. Do not invent engine behavior.

## Design goals

1. Browser-callable JSON for every Design JSF mutation/query required by FR-007.  
2. Runtime edition start/stop, demand publish, advanced log cleanup for FR-008.  
3. AuthZ consistent with current Design/Runtime authentication pages (admin/design roles).  
4. DTOs are serializable views of existing entities (`IPSEdition`, `IPSContentList`, `IPSLocationScheme`, …).

## Base path (frozen for US4)

`/services/sitemanage/publishingdesign`

| Decision | Value |
|----------|--------|
| Package | `com.percussion.publishingdesign` |
| REST class | `PSPublishingDesignRestService` (`@Component publishingDesignRestService`) |
| JAX-RS server | `sitemanage-jax-rs` (address `/sitemanage`) |
| Backend | `IPSPublisherService` via `PSPublisherServiceLocator` (delegate only) |

### Implemented endpoints (US4)

| Method | Path | Notes |
|--------|------|-------|
| GET | `/publishingdesign/editions?siteId=` | `findAllEditionsBySite` |
| GET | `/publishingdesign/editions/{editionId}` | `loadEdition` → 404 |
| POST | `/publishingdesign/editions` | `createEdition` + `saveEdition` |
| PUT | `/publishingdesign/editions/{editionId}` | `loadEditionModifiable` + save |
| DELETE | `/publishingdesign/editions/{editionId}` | `deleteEdition` |
| POST | `/publishingdesign/editions/copy` | copy to site (+ optional CL links) |
| GET | `/publishingdesign/editions/{id}/contentlists` | associations |
| GET/POST | `/publishingdesign/contentlists` | list / create |
| GET/PUT/DELETE | `/publishingdesign/contentlists/{id}` | CRUD |
| GET/POST | `/publishingdesign/deliverytypes` | list / create |
| GET/PUT/DELETE | `/publishingdesign/deliverytypes/{id}` | CRUD |
| GET | `/publishingdesign/contexts` | `IPSSiteManager.findAllContexts` |
| GET | `/publishingdesign/contexts/{id}/schemes` | schemes by context |

| GET | `/publishingdesign/sites` | design sites |
| GET | `/publishingdesign/sites/{siteId}` | site summary |
| GET/PUT/DELETE | `/publishingdesign/sites/{siteId}/properties` | context variables |
| POST/DELETE | `/publishingdesign/editions/{id}/contentlists[/{clId}]` | associate / disassociate |
| GET/POST/PUT/DELETE | `/publishingdesign/contexts[/{id}]` | context CRUD |
| GET/POST | `/publishingdesign/contexts/{id}/schemes` | list/create schemes |
| GET/PUT/DELETE | `/publishingdesign/schemes/{id}` | scheme + parameters |

Site root browser uses existing pathmanagement folder API from the client (`SiteRootBrowser`).


### Runtime endpoints (US5)

| Method | Path | Notes |
|--------|------|-------|
| GET | `/publishingdesign/runtime/editions?siteId=` | Editions + running job id |
| POST | `/publishingdesign/runtime/editions/{id}/start` | `startPublishingJob` |
| POST | `/publishingdesign/runtime/jobs/{jobId}/stop` | `cancelPublishingJob` |
| GET | `/publishingdesign/runtime/jobs/{jobId}` | Job status |
| POST | `/publishingdesign/runtime/editions/{id}/demand` | `queueDemandWork` |
| POST | `/publishingdesign/runtime/logs/purge?jobId=` | `purgeJobLog` |
| POST | `/publishingdesign/runtime/sites/{siteId}/clearItems` | `deleteSiteItems` |

Delegate: `PSPublishingRuntimeSupport` → `IPSRxPublisherService` + `IPSPublishingWs`.



## Design resources (CRUD-shaped)

| Resource | List | Get | Create | Update | Delete | Notes |
|----------|------|-----|--------|--------|--------|-------|
| Sites (design) | ✓ | ✓ | ✓ | ✓ | ✓ | Wrap `IPSPublishingWs` site methods; context variables on site |
| Editions by site | ✓ | ✓ | ✓ | ✓ | ✓ | `findAllEditionsBySite`, `saveEdition` |
| Edition content lists | ✓ | | associate | update | remove | `EditionContentList` |
| Content lists | ✓ | ✓ | ✓ | ✓ | ✓ | Modern + legacy discriminator field |
| Contexts | ✓ | ✓ | ✓ | ✓ | ✓ | `IPSSiteManager` / publishing context |
| Location schemes | ✓ | ✓ | ✓ | ✓ | ✓ | Modern + legacy parameters |
| Delivery types | ✓ | ✓ | ✓ | ✓ | ✓ | `IPSPublisherService` |
| Edition tasks | ✓ | ✓ | ✓ | ✓ | ✓ | `EditionTaskDef` |

**Copy edition from other site**: dedicated POST with source edition id, target site id, `copyContentLists` boolean—behavior matches Design `SelectEditionFromOtherSite`.

**Site root / item browser**: GET endpoints returning folder tree or path validation used by scheme editors (mirror JSF ItemBrowser/SiteRootBrowser outcomes).

## Runtime resources

| Operation | Method | Suggested path | Backend |
|-----------|--------|----------------|---------|
| List runnable editions | GET | `.../runtime/editions?siteId=` | Editions by site |
| Start edition job | POST | `.../runtime/editions/{id}/start` | `startPublishingJob` / Rx publisher |
| Stop job | POST | `.../runtime/jobs/{jobId}/stop` | Cancel / stopPubJob patterns |
| Job status | GET | `.../runtime/jobs/{jobId}` | `getPublishingJobStatus` |
| Demand publish | POST | `.../runtime/editions/{id}/demand` | `queueDemandWork` |
| Purge job log | POST | `.../runtime/logs/purge` | `purgeJobLog` |
| Clear site items / site record | POST | `.../runtime/sites/{siteId}/clearItems` | `deleteSiteItems` / Runtime clear site record |

Where Minuet already covers stop/status/logs, Runtime section may **call existing** `/pubstatus` and `/servers/stopPublishing` instead of duplicating.

## Non-goals

- Replacing assembly content list generation algorithms  
- New delivery drivers  
- Breaking SOAP `IPSPublishingWs` binary compatibility—façade is additive  

## Test contract

For each façade method: unit test with mocked service verifying:

- Happy path DTO mapping  
- Not found → 404  
- Validation error → 400 with message  
- Unauthorized → 403  

## Discovery checklist for implementers

Before coding each Design screen, confirm:

1. [ ] Which `IPSPublishingWs` / `IPSSiteManager` / `IPSPublisherService` method performs the JSF action  
2. [ ] Whether any sitemanage REST already exposes it  
3. [ ] DTO fields needed for the form (read JSF page bindings)  
4. [ ] Whether legacy vs modern content list/scheme needs a type flag  
