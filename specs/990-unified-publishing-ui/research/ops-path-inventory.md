# Ops REST path inventory (T006)

**Feature**: `990-unified-publishing-ui`  
**Date**: 2026-07-19  
**Sources**: `WebUI/.../perc_path_constants.js`, `PercPublisherService.js`, `contracts/ops-publish-api.md`, research R4.

Hard gate before US1 API coding. Typed clients under `WebUI/src/main/ts/api/publishing/` MUST match these paths.

## Service roots

| Legacy constant | Root (context-aware) |
|-----------------|----------------------|
| `SERVICES.SITEMGT` | `{SERVICES_ROOT}/sitemanage` |
| `SERVICES.PUBMGT` | `{SERVICES_ROOT}/publishmanagement` |

`SERVICES_ROOT` is `/services` (or `/Rhythmyx/services` when pathname is under `/Rhythmyx`) — see `WebUI/src/main/ts/api/paths.ts`.

## Publish (`sitemanage`)

| Operation | Method | Path suffix |
|-----------|--------|-------------|
| Full site publish | GET | `/publish/{siteName}/{serverName}` |
| Incremental queue | GET | `/publish/incremental/content/{site}/{server}?startIndex=&pageSize=` |
| Incremental related | GET | `/publish/incremental/relatedcontent/{site}/{server}?startIndex=&pageSize=` |
| Incremental publish | GET | `/publish/incremental/publish/{site}/{server}` |
| Incremental + approval | GET | `/publish/incremental/publish/{site}/{server}/{relatedItems}` |
| Item publish actions | GET | `/publish/publishingActions/...` |
| Page / resource publish | GET | `/publish/page/{id}`, `/publish/resource/{id}` |
| Takedown | GET | `/publish/takedown/page|resource/{id}` |
| Publish properties | GET/POST | `/site/publishProperties`, `/site/updatePublishProperties` |

## Status & logs (`sitemanage/pubstatus`)

| Operation | Method | Path |
|-----------|--------|------|
| Current jobs | GET | `/pubstatus/current` |
| Current by site | GET | `/pubstatus/current/{siteId}` |
| Logs list | POST | `/pubstatus/logs` (body: SitePublishLogRequest) |
| Log details | POST | `/pubstatus/details` |
| Purge logs | POST | `/pubstatus/purge` |

## Servers (`publishmanagement/servers`)

| Operation | Method | Path |
|-----------|--------|------|
| List / get | GET | `/servers/{siteId}`, `/servers/{siteId}/{serverId}` |
| Create | POST | `/servers/{siteId}/{serverName}` |
| Update | PUT | `/servers/{siteId}/{serverId}` |
| Delete | DELETE | `/servers/{siteId}/{serverId}` |
| Stop job | GET/POST | `/servers/stopPublishing/{jobId}` |
| Helpers | GET | `availableDrivers`, `availableRegions`, `isEC2Instance`, `defaultFolderLocation/...`, `isDefaultServerModified/{siteId}`, `availablePublishingServer/{type}`, `availableDeliveryServers` |

## Error tokens (preserve)

`FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, license gate messages — map to clear UI messaging (FR-011).

## Client requirements

- Session cookie + CSRF via `api/client.ts`
- Do not invent new DTO shapes; map existing field names
