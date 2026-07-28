# Home acceptance status (Wave 1)

| Field | Value |
|-------|--------|
| **Date** | 2026-07-28 (updated after #1576 / #1577) |
| **Branch tip** | `development` (post Home PRs #1571–#1574; Gadgets host #1577) |
| **Overall** | **Not accepted** — core sections largely usable; Gadgets host + Blogs recovering (#1577); Design/template gaps tracked separately |

## Section status

| Section | Status | Evidence / notes |
|---------|--------|------------------|
| Recent | **Usable** | List + open + bookmark toggle (#1571) |
| Bookmarks | **Usable** | List + remove; add from Recent/Search/Library (#1571) |
| Library | **Usable** | Browse, Up/breadcrumb (#1573) |
| Search | **Usable** | Extended search + counts (#1573); Lucene full-token matching only |
| Create Page | **Usable** | createPage + .html (#1568, #1574) |
| Create Asset | **Wired** | widgetId + editAsset.jsp; creatable-widgets public REST (#1574). **Needs CMS restart** after jar deploy for new REST class |
| Create Blog Post | **Blocked** | Requires existing blog **section** + eligible list/post **templates** (Design track) |
| Create Blog Section | **Wired** | Blogs gadget (#1577); disabled until site has Blog List / Blog Post templates |
| Gadgets | **Partial** | Host load + Blogs fixed in #1577; most other widgets still wrong/missing APIs (#1580 wires Activity + Pages By Status) |

## Design / templates (out of Home PR scope)

Home will keep hitting Design walls (blog list/post templates, non-page items).  
**Do not expand Home PRs into full Design.** Track here:

→ **[`design-templates-item-types/`](./design-templates-item-types/README.md)**  
→ **[gaps-from-home.md](./design-templates-item-types/gaps-from-home.md)**

Includes: Page templates vs Non-Page templates; Assets vs Non-Asset items; Rhythmyx-style items; blog widget eligibility (`percBlogIndexPage` / `percBlogPost`).

## Gadgets assessment (2026-07-28 live probe; updated after #1577)

React gadgets under `WebUI/src/main/ts/dashboard/*` historically called paths that largely **did not exist** on the server (HTTP 500 wrapping 404). These were not a thin port of classic Shindig gadgets onto verified sitemanage REST.

| Widget | Status after #1577 / #1580 |
|--------|----------------------------|
| Welcome | OK shell |
| Blogs | **Fixed** (#1577) — `allBlogs` + section create + template filter |
| Dashboard host | **Fixed** (#1577) — `GET /services/dashboardmanagement/dashboard` (session user) |
| Pages By Status | **In progress** (#1580) — real `path/item/wfState` |
| Activity | **In progress** (#1580) — real `activitymanagement/.../contentactivity` POST |
| Assets By Status / others | Still invented endpoints unless rewritten |

**Conclusion (product):** Gadgets host is recoverable; individual gadgets need **real REST contracts + TS rewrite** (or deliberate retirement). Do not mark Home accepted while checklist requires “Gadgets usable for default set.”

**Recommendation:** Continue Gadgets wave (#1580+), keep Design track separate, FTS residual ([issue #1561](https://github.com/intersoftdatalabs-in/percussioncms/issues/1561) body extract) in parallel when capacity allows.

## Deploy note (native `/opt/Percussion`)

Jars + modern assets:

- `rest-8.2.0-SNAPSHOT.jar`, `sitemanage-8.2.0-SNAPSHOT.jar` → `WEB-INF/lib`
- `cm/modern/assets/*` from WebUI build

**Jetty restart required** for new Java classes (`CreatableWidget` resource, `isMyPage` Produces). Hard-refresh browser after WebUI deploy.

## Checklist progress

- [x] Section tabs / deep links (infra)
- [x] Recent / Bookmarks / Library / Search / Create (core)
- [x] Gadgets host + Blogs section create (#1577)
- [ ] Gadgets usable for default set (Activity / Pages By Status / gate rest)
- [ ] Full smoke after restart (create asset, creatable-widgets)
- [ ] TMX catalog keys for all modern strings (fallbacks work)
- [ ] Home marked **Accepted**
