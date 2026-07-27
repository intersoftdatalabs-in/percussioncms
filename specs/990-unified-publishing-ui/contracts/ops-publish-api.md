# Contract: Ops Publish / Status / Server APIs

**Feature**: `990-unified-publishing-ui`  
**Source of truth**: Existing sitemanage REST used by Minuet (`perc_path_constants.js`, `PercPublisherService.js`, `PSPubServerRestService`, `PSSitePublishStatusService`, `IPSSitePublishService`).

Base: site management services root (historically `SERVICES.SITEMGT` under the CMS services path).

## Site publish

|             Operation              |  Method  |               Path pattern                |                   Notes                   |
|------------------------------------|----------|-------------------------------------------|-------------------------------------------|
| Publish site to server             | GET      | `/publish/{siteName}/{serverName}`        | Starts full-style site publish for server |
| Publishing actions (item)          | GET      | `/publish/publishingActions/...`          | Item actions list                         |
| Publish page                       | …        | `/publish/page/{id}`                      | Item                                      |
| Publish resource                   | …        | `/publish/resource/{id}`                  | Item                                      |
| Takedown page/resource             | …        | `/publish/takedown/page|resource/{id}`    | Item                                      |
| Incremental content page           | GET/…    | `/publish/incremental/content/...`        | Queue preview                             |
| Incremental related                | …        | `/publish/incremental/relatedcontent/...` | Related items                             |
| Incremental publish                | …        | `/publish/incremental/publish/...`        | May include approval variant              |
| Site publish properties get/update | GET/POST | `/site/publishProperties`                 | Legacy FTP-oriented props if still used   |

**PubType** (server enum): `FULL`, `FULL_NONBINARY`, `INCREMENTAL`, `STAGING_INCREMENTAL`, `PUBLISH_NOW`, `TAKEDOWN_NOW`, `STAGE_NOW`, `REMOVE_FROM_STAGING_NOW`.

## Status & logs (`@Path("/pubstatus")`)

|      Operation       | Method |             Path              |            Body / params            |
|----------------------|--------|-------------------------------|-------------------------------------|
| Current jobs         | GET    | `/pubstatus/current`          | All                                 |
| Current jobs by site | GET    | `/pubstatus/current/{siteId}` |                                     |
| Logs list            | POST   | `/pubstatus/logs`             | `SitePublishLogRequest`-shaped JSON |
| Log details          | POST   | `/pubstatus/details`          | job id request                      |
| Purge logs           | POST   | `/pubstatus/purge`            | selected job ids                    |

## Publish servers (`@Path("/servers")`)

|          Operation          | Method |                                     Path                                      |
|-----------------------------|--------|-------------------------------------------------------------------------------|
| Get server                  | GET    | `/servers/{siteId}/{serverId}`                                                |
| List servers                | GET    | `/servers/{siteId}`                                                           |
| Create                      | POST   | `/servers/{siteId}/{serverName}`                                              |
| Update                      | PUT    | `/servers/{siteId}/{serverId}`                                                |
| Delete                      | DELETE | `/servers/{siteId}/{serverId}`                                                |
| Stop publishing             | …      | `/servers/stopPublishing/{jobId}`                                             |
| Available drivers           | GET    | `/servers/availableDrivers`                                                   |
| Available regions           | GET    | `/servers/availableRegions`                                                   |
| Available publishing server | GET    | `/servers/availablePublishingServer/{publishServerType}`                      |
| Is EC2                      | GET    | `/servers/isEC2Instance`                                                      |
| Default server modified     | GET    | `/servers/isDefaultServerModified/{siteId}`                                   |
| Default folder location     | GET    | `/servers/defaultFolderLocation/{siteId}/{publishType}/{driver}/{serverType}` |
| Available delivery servers  | GET    | `/servers/availableDeliveryServers`                                           |

Exact verb/path nuances: implementers MUST re-read `PSPubServerRestService.java` at coding time (path regexes use `{serverName:.*}` / `{serverId:.*}`).

## Error semantics (preserve)

|      Signal       |              User meaning              |
|-------------------|----------------------------------------|
| FORBIDDEN         | Not allowed to publish                 |
| BADCONFIG         | Server/site configuration invalid      |
| NOSTAGING_SERVERS | Staging action without staging servers |
| License blocks    | Map existing license gate messages     |

## Client requirements

- Same-origin session cookie
- CSRF header via existing `api/client.ts` / CSRF helpers
- JSON content types as current Minuet `makeJsonRequest`
- Do not change response DTOs casually—UI maps existing field names (`SitePublishJob`, `serverInfo.properties`, etc.)

