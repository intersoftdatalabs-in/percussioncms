# Data Model: Unified Publishing UI

**Feature**: `990-unified-publishing-ui`  
**Date**: 2026-07-18  
**Note**: Logical model for UI/API contracts. Persistence remains existing Hibernate entities in publisher/sitemgr services—**no new tables** for this feature unless a façade requires DTOs only (in-memory/JSON).

## Entities

### Site (ops summary)

| Field | Type | Notes |
|-------|------|-------|
| siteId | string/guid | Identity for APIs |
| name | string | Display |
| folderPath | string | Optional path display |

**Source**: Site list services already used by Minuet (`PercSiteService.getSites` / site summaries).

### PublishServer

| Field | Type | Notes |
|-------|------|-------|
| serverId | string | |
| serverName | string | |
| siteId | string | Parent site |
| serverType | enum | `PRODUCTION` \| `STAGING` |
| type | enum | `File` \| `Database` (product strings) |
| isDefault | boolean | Publish Now default |
| properties | list of {key, value} | Driver-specific (driver, serverip, password, privateKey, region, …) |

**Validation**: Required fields depend on `type` + `driver` (see Minuet templates / PSPubServerValidator). Passwords optional on update when unchanged if product already supports that pattern—match server behavior.

### PublishingJob (status)

| Field | Type | Notes |
|-------|------|-------|
| jobId | string/long | |
| siteName | string | |
| status | string/enum | Running, completed, failed, stopping, … |
| startTime / startDate | string/datetime | |
| elapsedTime | number | |
| completedItems | number | |
| totalItems | number | |
| isStopping | boolean | Disables stop button |

**Transitions**: Running → Stopping → Stopped/Cancelled; Running → Completed / Failed / Abnormal. UI must not invent states—map server status strings.

### PublishingLog / LogDetails

| Field | Type | Notes |
|-------|------|-------|
| jobId | string/long | |
| site / server filters | request fields | Match `PSSitePublishLogRequest` |
| items | list of SitePublishItem | Details endpoint |

**Purge**: batch of job IDs → `/pubstatus/purge`.

### Edition (design/runtime)

| Field | Type | Notes |
|-------|------|-------|
| editionId / guid | identity | |
| name | string | |
| siteId | guid | |
| contentListAssociations | list | EditionContentList links |
| tasks | list | EditionTaskDef optional |

**Operations**: create, save, copy (including from other site), delete, start job, stop job.

### ContentList

| Field | Type | Notes |
|-------|------|-------|
| id / name | | |
| generator type | modern vs legacy | Product-supported legacy remains editable |
| parameters | map/list | Generator-specific |

### Context

| Field | Type | Notes |
|-------|------|-------|
| id / name | | |
| defaultScheme | LocationScheme ref | |
| schemes | list | |

### LocationScheme

| Field | Type | Notes |
|-------|------|-------|
| id / name | | |
| contextId | | |
| modern expression vs legacy parameters | | Legacy: ordered parameters (name, type, value, sequence) |

### DeliveryType

| Field | Type | Notes |
|-------|------|-------|
| id / name | | |
| bean/implementation registration fields | as today | |

### IncrementalQueuePage

| Field | Type | Notes |
|-------|------|-------|
| childrenInPage | list | Paged items |
| columns | sys_title, modified, modifier, category | Match preview template |

### ItemPublishAction

| Field | Type | Notes |
|-------|------|-------|
| action id/label | | From `getPublishingActions` |
| PubType | FULL, INCREMENTAL, PUBLISH_NOW, TAKEDOWN_NOW, STAGE_*, … | Server enum |

## Relationships

```text
Site 1──* PublishServer
Site 1──* Edition
Edition *──* ContentList (via EditionContentList)
Site 1──* PublishingJob / PubStatus logs
Context 1──* LocationScheme
Edition 1──* EditionTaskDef
Item ──* ItemPublishAction (computed)
```

## Validation rules (UI)

1. Cannot save File server without driver + required path/host fields for that driver.  
2. Cannot save Database server without server, port, user, database (and owner/schema/sid per driver).  
3. Cannot start full publish without selected site + server.  
4. Delete design objects: confirm; honor server dependency errors.  
5. Purge logs: require ≥1 selection + confirm.  
6. Secrets: never echo in client-side error toasts beyond product norms.

## State machines

### Job

`IDLE → START_REQUESTED → RUNNING → (STOP_REQUESTED → STOPPED) | COMPLETED | FAILED`

### Server editor

`VIEW → EDIT (dirty) → SAVE (validate) → VIEW | CANCEL (confirm if dirty)`

Dirty-guard pattern already exists in classic `PublishView` / dirty controller—replicate in React (prompt on section change).
