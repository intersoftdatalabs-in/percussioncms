# Home contributor REST inventory (T005)

**Source:** `WebUI/src/main/webapp/cm/plugins/PercContributorUiAdaptor.js`, `perc_path_constants.js`, related services.

**SERVICES_ROOT:** `/Rhythmyx/services`

## Mapped operations (MVP)

| Home section | Capability | REST / notes |
|--------------|------------|--------------|
| Recent | Recent items | `GET /Rhythmyx/services/recentmanagement/recent/{type}` optional `/{site}` for template/site-folder (`RECENT_ROOT`) |
| Library | Sites list | `GET /Rhythmyx/services/sitemanage/site/` (`SITES_ALL`) |
| Library | Folder children | `GET /Rhythmyx/services/pathmanagement/path/folder{path}` (`PATH_FOLDER`) |
| Search | Extended search | `POST /Rhythmyx/services/searchmanagement/search/get/extendedresults` (`FINDER_SEARCH` + `/extendedresults`) |
| Create page | Create page | Via page management create (`PAGE_CREATE` `/Rhythmyx/services/pagemanagement/page`) — payload parity with legacy form fields |
| Create asset | Create asset | Asset management paths (defer details if blocked; note in PR) |
| Create blog | Blogs for site | `.../sitemanage/section/blogs` (`BLOG_LOAD`) |

## Deferred / later slices

- Full asset-type wizard parity
- Bookmark / my content list details
- Access-level pre-checks (folder helper) — server will still enforce

## Modern client location

Typed wrappers: `WebUI/src/main/ts/api/home/`
