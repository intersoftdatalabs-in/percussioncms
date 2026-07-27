# Capability Matrix: Publishing Parity

**Feature**: `990-unified-publishing-ui`  
**Purpose**: Normative checklist for feature parity. A cutover milestone MUST mark all rows for that surface **Done** before retiring the classic client.

Status: `Todo` | `In progress` | `Done` | `N/A (deprecated)`

## CG-OPS (Stories US1–US3) — Minuet Publish parity

|   ID   |                  Capability                  | Source | Status |
|--------|----------------------------------------------|--------|--------|
| OPS-01 | Site list card view                          | Minuet | Done   |
| OPS-02 | Site list table view                         | Minuet | Done   |
| OPS-03 | Filter sites                                 | Minuet | Done   |
| OPS-04 | Open site workspace                          | Minuet | Done   |
| OPS-05 | List publish servers                         | Minuet | Done   |
| OPS-06 | Add server                                   | Minuet | Done   |
| OPS-07 | Edit server                                  | Minuet | Done   |
| OPS-08 | Delete server                                | Minuet | Done   |
| OPS-09 | Refresh server list                          | Minuet | Done   |
| OPS-10 | Default Publish Now indicator                | Minuet | Done   |
| OPS-11 | Production / Staging type                    | Minuet | Done   |
| OPS-12 | File Local driver                            | Minuet | Done   |
| OPS-13 | File FTP / FTPS / SFTP                       | Minuet | Done   |
| OPS-14 | File Amazon S3                               | Minuet | Done   |
| OPS-15 | Database MSSQL / MySQL / Oracle fields       | Minuet | Done   |
| OPS-16 | Full publish                                 | Minuet | Done   |
| OPS-17 | Incremental preview queue + related          | Minuet | Done   |
| OPS-18 | Incremental publish (+ approval if required) | Minuet | Done   |
| OPS-19 | Stop job                                     | Minuet | Done   |
| OPS-20 | Status table + progress + sort               | Minuet | Done   |
| OPS-21 | Status auto-refresh                          | Minuet | Done   |
| OPS-22 | Logs filter + list                           | Minuet | Done   |
| OPS-23 | Log details (items)                          | Minuet | Done   |
| OPS-24 | Purge/delete logs with confirm               | Minuet | Done   |
| OPS-25 | EC2/regions/available pub server helpers     | Minuet | Done   |
| OPS-26 | Forbidden / bad config messaging             | Minuet | Done   |

**US9 residual closed (2026-07-19):** OPS-18 approval selection + `publishIncrementalWithApproval`; OPS-20 status column sort; OPS-22 log filters (site/server/days/maxcount); OPS-23 structured `LogDetailsPanel` (SitePublishItem table).

## CG-DESIGN (Story US4) — JSF Design parity

|   ID   |                     Capability                     | Source | Status |
|--------|----------------------------------------------------|--------|--------|
| DES-01 | List/view design sites (+ context vars via DES-02) | Design | Done   |
| DES-02 | Context variables on site                          | Design | Done   |
| DES-03 | List/create/edit/copy/delete editions              | Design | Done   |
| DES-04 | Associate content lists with edition               | Design | Done   |
| DES-05 | Copy edition from other site (+ optional CLs)      | Design | Done   |
| DES-06 | Content lists modern create/edit/copy/delete       | Design | Done   |
| DES-07 | Content lists legacy create/edit                   | Design | Done   |
| DES-08 | Contexts CRUD                                      | Design | Done   |
| DES-09 | Location schemes modern                            | Design | Done   |
| DES-10 | Location schemes legacy + parameters               | Design | Done   |
| DES-11 | Site root / item browser for schemes               | Design | Done   |
| DES-12 | Delivery types CRUD                                | Design | Done   |
| DES-13 | Delete confirmations / dependency warnings         | Design | Done   |

**DES-01 scope note:** List/view of publishable sites + folder/base URL; full CMS site create/copy/delete remains **site administration**, not this Design panel. Context variables are DES-02.

## CG-RUNTIME (Story US5) — JSF Runtime parity

|  ID   |                     Capability                     | Source  | Status |
|-------|----------------------------------------------------|---------|--------|
| RT-01 | Runtime edition list                               | Runtime | Done   |
| RT-02 | Start edition                                      | Runtime | Done   |
| RT-03 | Stop/cancel edition job                            | Runtime | Done   |
| RT-04 | Demand publish                                     | Runtime | Done   |
| RT-05 | Active job status (if not fully covered by OPS-20) | Runtime | Done   |
| RT-06 | Job / item logs navigation                         | Runtime | Done   |
| RT-07 | Advanced log purge/archive behaviors still offered | Runtime | Done   |
| RT-08 | Clear site record                                  | Runtime | Done   |

## CG-ITEM (Story US6)

|   ID   |               Capability                |    Source    | Status |
|--------|-----------------------------------------|--------------|--------|
| ITM-01 | Publish now page/resource               | Item service | Done   |
| ITM-02 | Takedown page/resource (+ linked items) | Item service | Done   |
| ITM-03 | Stage / remove from staging             | Item service | Done   |
| ITM-04 | Get publishing actions                  | Item service | Done   |
| ITM-05 | Schedule dates get/set                  | Item service | Done   |
| ITM-06 | Publishing history dialog               | Item service | Done   |

ITM-01–06: item actions intentionally remain on jQuery `PercItemPublisherService` (no regression); history dialog links to modern logs.

## CG-UX / RETIRE (US7–US8)

|   ID   |                         Capability                         | Status |
|--------|------------------------------------------------------------|--------|
| UX-01  | Ops path without Design                                    | Done   |
| UX-02  | Empty states                                               | Done   |
| UX-03  | Keyboard primary flows                                     | Done   |
| UX-04  | TMX strings                                                | Done   |
| RET-01 | Minuet publish exclusive client removed                    | Done   |
| RET-02 | JSF Design **entry** removed from product path (redirect)  | Done   |
| RET-03 | JSF Runtime **entry** removed from product path (redirect) | Done   |
| RET-04 | Deep links mapped                                          | Done   |
| RET-05 | Removal inventory signed off (entry path)                  | Done   |
| RET-06 | Residual deep faces JSPs + faces-config packaging cleanup  | Todo   |

**RET residual:** Primary entries (`index.jsp`, DCE header, Minuet exclusive clients) are retired. Deep `ui/publishing/*.jsp` / `ui/pubruntime/*.jsp` faces pages may still exist on disk — tracked as **RET-06 / US9 T124**.
