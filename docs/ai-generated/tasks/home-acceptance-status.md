# Home acceptance status (Wave 1)

| Field | Value |
|-------|--------|
| **Date** | 2026-07-28 |
| **Branch tip** | `development` (post Home PRs #1571–#1574) |
| **Overall** | **Not accepted** — core sections largely functional; Gadgets not product-ready |

## Section status

| Section | Status | Evidence / notes |
|---------|--------|------------------|
| Recent | **Usable** | List + open + bookmark toggle (#1571) |
| Bookmarks | **Usable** | List + remove; add from Recent/Search/Library (#1571) |
| Library | **Usable** | Browse, Up/breadcrumb (#1573) |
| Search | **Usable** | Extended search + counts (#1573); Lucene full-token matching only |
| Create Page | **Usable** | createPage + .html (#1568, #1574) |
| Create Asset | **Wired** | widgetId + editAsset.jsp; creatable-widgets public REST (#1574). **Needs CMS restart** after jar deploy for new REST class |
| Create Blog | **Wired** | allBlogs; empty until site has blog sections (Demo has none) |
| Gadgets | **Not accepted** | React shell/registry exists; most data endpoints are missing or wrong vs real CMS services |

## Gadgets assessment (2026-07-28 live probe)

React gadgets under `WebUI/src/main/ts/dashboard/*` call paths that largely **do not exist** on the server (HTTP 500 wrapping 404). These are not a thin port of classic Shindig gadgets onto verified sitemanage REST.

| Widget | Endpoint used by TS | Live status |
|--------|---------------------|-------------|
| Welcome | (static) | OK shell only |
| Workflow Status | `/services/dashboardmanagement/gadget/workflow-status` | 200 but empty settings |
| Activity | `/services/activity/contentactivity?limit=` | 500 (wrong shape vs classic activity API) |
| Process Monitor | `/services/monitor/all` | 500 |
| Effectiveness | `POST /services/activity/effectiveness` | 500 |
| Assets By Status | `/services/asset/workflow-status` | 500 |
| Bulk Upload | `/services/bulk-upload/jobs` | 500 |
| Reports | `/services/reports/list` | 500 |
| Traffic | `POST /services/activity/contenttraffic` | 500 |
| Blogs | `/services/blogs/list` | 500 |
| Comments | `/services/comments/latest` | 500 |
| Form Tracker | `/services/forms/tracker` | 500 |
| Cookie Consent | `/services/compliance/cookie-consent` | 500 |
| SEO Audit | `/services/seo/audit` | 500 |
| Google Setup | `/services/google/setup` | 500 |
| Membership | `/services/membership/list` | 500 |
| Sitewide Framework | `/services/framework/config` | 500 |
| Siteimprove | `/services/siteimprove/metrics` | 500 |
| External iframe | `/services/embed/iframe` | 500 |
| Global Variables | `/services/admin/variables` | 500 |
| Dashboard Configuration | `/services/dashboard/config` | 500 (persist uses dashboardmanagement separately) |

**Conclusion (product):** Gadgets on Home are a **host shell** (compose, add/remove UI, some layout persist via `dashboardmanagement/dashboard`). Individual gadgets need **real REST contracts + TS rewrite** (or deliberate retirement). Do not mark Home accepted while checklist requires “Gadgets usable for default set.”

**Recommendation:** Defer gadget productization as a **dedicated wave** (or accept Home without full gadgets with explicit product sign-off). Prefer next: FTS residual (#1561 body extract) or Wave 2 Publish.

## Deploy note (native `/opt/Percussion`)

Jars + modern assets copied 2026-07-28:

- `rest-8.2.0-SNAPSHOT.jar`, `sitemanage-8.2.0-SNAPSHOT.jar` → `WEB-INF/lib`
- `cm/modern/assets/*` from WebUI build

**Jetty restart required** for new classes (`CreatableWidget` resource, `isMyPage` Produces). Agent could not `sudo jetty.sh restart` (no password). Operator should restart CMS, then re-smoke:

```bash
curl -s -b cookies ... '/rest/assets/creatable-widgets?filterDisabled=true'
curl -s -H 'Accept: application/json' ... '/services/itemmanagement/item/ismypage/{id}'
```

## Checklist progress

- [x] Section tabs / deep links (infra)
- [x] Recent / Bookmarks / Library / Search / Create (core)
- [ ] Gadgets usable for default set
- [ ] Full smoke after restart (create asset, creatable-widgets)
- [ ] TMX catalog keys for all modern strings (fallbacks work)
- [ ] Home marked **Accepted**
