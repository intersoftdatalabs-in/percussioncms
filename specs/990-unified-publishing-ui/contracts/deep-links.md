# Contract: Publishing Deep Links & Navigation

**Feature**: `990-unified-publishing-ui`

## Primary nav

| Concept | Value |
|---------|--------|
| Navigation manager view key | `publish` (`PercNavigationManager.VIEW_PUBLISH`) |
| Main nav data attribute | `VIEW_PUBLISH` |
| `index.jsp` views map (today) | `views.put("publish", "publish.jsp")` |
| Target after ops cutover | `views.put("publish", "publishModern.jsp")` (final filename at implement) |

## Modern shell props (suggested)

| Prop / query | Maps to section |
|--------------|-----------------|
| (default) | Sites & servers |
| `section=status` | Status |
| `section=logs` | Logs |
| `section=design` | Design |
| `section=runtime` | Runtime / Editions |
| `siteId=…` | Preselect site when listing |
| `serverId=…` | Preselect server when in site workspace |

Allowlist query values in JSP (same XSS discipline as `homeModern.jsp` initialScreen).

## Classic URLs to map after cutover

| Classic | After cutover |
|---------|----------------|
| Web Management Publish view (`view=publish` / publish.jsp) | `publishModern.jsp` |
| `/ui/publishing/*` (Design faces) | Publishing shell `section=design` (+ optional object query) **or** clear moved message if object deep-link unsupported in v1 |
| `/ui/pubruntime/*` | Publishing shell `section=runtime` |
| `/publisher/demandpublishing` | Prefer Runtime demand section; keep servlet if engine requires it but hide as sole UI |

Unknown retired paths: show product-standard unavailable/moved view (`UnavailableView` pattern) rather than 500.

## Dual-tree requirement

Any `index.jsp` / shell change under `cm/app` MUST be mirrored under `cm/pages/app` (and packaging verified).
