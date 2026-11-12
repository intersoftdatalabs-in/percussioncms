# Contract: Publishing Deep Links & Navigation

**Feature**: `990-unified-publishing-ui`

## Primary nav

|            Concept            |                                   Value                                   |
|-------------------------------|---------------------------------------------------------------------------|
| Navigation manager view key   | `publish` (`PercNavigationManager.VIEW_PUBLISH`)                          |
| Main nav data attribute       | `VIEW_PUBLISH`                                                            |
| `index.jsp` views map (today) | `views.put("publish", "publish.jsp")`                                     |
| Target after ops cutover      | `views.put("publish", "publishModern.jsp")` (final filename at implement) |

## Modern shell props (suggested)

|   Prop / query    |             Maps to section             |
|-------------------|-----------------------------------------|
| (default)         | Sites & servers                         |
| `section=status`  | Status                                  |
| `section=logs`    | Logs                                    |
| `section=design`  | Design                                  |
| `section=runtime` | Runtime / Editions                      |
| `siteId=…`        | Preselect site when listing             |
| `serverId=…`      | Preselect server when in site workspace |
| `jobId=…`         | Preselect + open job detail (logs)      |
| `requestId=…`     | Surface demand request as job (status)  |

Allowlist query values in JSP (same XSS discipline as `homeModern.jsp` initialScreen).
`jobId` / `requestId` reuse the `mapIdParam` allowlist (`mapJobIdParam` /
`mapRequestIdParam` wrappers in `deepLinkMap.ts`): alnum/`-`/`_` only, max
128 chars. Unknown ids degrade to the plain section view — never 500.

> Restored in #6215 (decision from #6091): `jobId` (logs section) and
> `requestId` (status section) deep-link filters restore the
> `sys_publishingJobId` / `requestid` fidelity dropped by #1842/#1844.
> Producers: `PSDemandPublishServlet` appends `requestId`;
> `PSRunEdition.getPublishingLogURL()` appends `jobId`.

## Classic URLs to map after cutover

|                          Classic                           |                                                        After cutover                                                         |
|------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| Web Management Publish view (`view=publish` / publish.jsp) | `publishModern.jsp`                                                                                                          |
| `/ui/publishing/*` (Design faces)                          | Publishing shell `section=design` (+ optional object query) **or** clear moved message if object deep-link unsupported in v1 |
| `/ui/pubruntime/*`                                         | Publishing shell `section=runtime`                                                                                           |
| `/publisher/demandpublishing`                              | Prefer Runtime demand section; keep servlet if engine requires it but hide as sole UI                                        |

Unknown retired paths: show product-standard unavailable/moved view (`UnavailableView` pattern) rather than 500.

## Dual-tree requirement

Any `index.jsp` / shell change under `cm/app` MUST be mirrored under `cm/pages/app` (and packaging verified).
