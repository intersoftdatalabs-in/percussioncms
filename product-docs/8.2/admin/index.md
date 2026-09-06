---
id: admin
title: Administration
description: Operating and administering Percussion CMS 8.2
version: "8.2"
order: 40
tags: [admin]
---

# Administration

Operator and administrator topics for Percussion CMS 8.2 — Sites, security, publishing, and
day-two server operations.

## Product top navigation (SPA)

For roles that can see the full chrome, the application top navigation is:

1. **Home**
2. **Explorer** (immediately after Home)
3. **Navigation**, **Developer**, **Publish** (Admin or Designer)
4. **Admin** (administrators only — one item)

**Editor**, **Design**, and **Widget Builder** are not top-nav items. Open the React Content Editor from Explorer **Edit**, Preview, or **Home → Create** (`spa.jsp?entry=editor` / `/editor`). Bookmarks to leftover `?view=editor` or `editAsset.jsp` redirect to that same host. Open the template library from **Developer → Design** (or the existing `/design` deep link). Open Widget Builder from **Developer** when that feature is active.

**Dashboard** is not a top-nav item. Dashboard gadgets remain on Home
(`/home/gadgets`) and via homepage preference; they are not a separate
primary destination.

## Home → Create

On **Home**, the **Create** tab is the in-product wizard for a new page, blog
post, or shared asset. After you choose the type and destination folder (and
title/file name for pages and blog posts), the CMS creates the item
(`POST /services/itemmanagement/item/create` for assets; page/blog create
for those kinds) and opens the React Content Editor
(`spa.jsp?entry=editor`). If the browser blocks that popup after create,
use **Open** on the wizard to open the new asset. Home Create does **not**
open leftover `editAsset.jsp` or `?view=editor`.

**Create page:** choose a **Site**, then a **Template**. The Template list
loads that site's page templates. If the site catalog is empty, the wizard
falls back to templates allowed for the page content type. You cannot create
the page until a template is selected. Then pick the destination folder,
title, and file name.

The destination folder is the site's **repository folder**, not only the
site display name. Sample FastForward sites such as **Corporate_Investments**
and **Enterprise_Investments** live under `/Sites/CorporateInvestments` and
`/Sites/EnterpriseInvestments` (no underscore). An administrator can create
a page at that site root, including with site-listed page templates such as
**Page - Database Template**. After create, the React Content Editor opens
(`spa.jsp?entry=editor`) — leftover `?view=editor` is not used. If you are
not allowed to create in a folder, the wizard shows that error; a missing
or mismatched folder is not treated as a successful create.

FastForward sample sites (for example **Corporate_Investments**) may list
assembly templates such as **Page - Database Template** (`perc.pageDatabase`).
Create still saves the page under the site's repository folder
(`/Sites/CorporateInvestments`) and opens the React Content Editor
(`spa.jsp?entry=editor`). The finder SITENAME (`Corporate_Investments`) is
not a separate create ACL; the server maps it to the repository folder.

Rich file, image, and TinyMCE widget chrome in that editor is still a later
slice — a new asset can still be created as a stub and opened on the field
form.

## Product Admin navigation (SPA)

The top navigation exposes a **single Admin** item for administrators (not
separate **Administration** and **Admin tools** entries). **Admin** opens the
working **Admin tools** shell (`/admin`) — not a Workflow-only hub. The shell
title is **Admin tools**, with tabs for:

| Tab | Purpose |
|-----|---------|
| Scheduled Tasks | Create and run scheduled CMS tasks |
| Execution Logs | Review task run history |
| Notification Settings | Task email notification templates |
| System Tools | Security audit log, consistency checker |
| Workflow | Workflow definitions and site/folder assignment |
| Roles | Role membership |
| Users | User accounts and default landing |
| Categories | Category tree administration |

Legacy bookmarks and deep links under `/workflow` and `/workflow/:tab` redirect into the
matching Admin tab (for example `/admin/workflow`, `/admin/roles`). There is no separate
Workflow administration shell or sibling cross-link between Admin tools and Administration.

Each tab should show its list or empty state. If a tab cannot load its data, the rest of
the Admin shell (title and tab list) stays usable so you can switch tabs or reload —
you should not see a full-page **Unable to load Admin** / **Unable to load Administration**
panel for a single tab failure.

**System Tools** shows the Security Audit Log (default) and the Consistency Checker
inside the same Admin shell. Those tools must render without replacing Admin with
**Unable to load Admin**. If one tool fails, the Admin tab list and the other tool
remain available.

Non-administrators never see the Admin top-nav item or these configuration surfaces.

## Topics

- [Sites & content structure](id:admin-sites)
- [Content Explorer](id:admin-content-explorer)
- [Navigation & site structure](id:admin-architecture-navigation)
- [Design templates](id:admin-design-templates)
- [Developer Templates](id:admin-developer-templates)
- [Users, roles & security](id:admin-users-roles) (includes Developer Object ACL for Sites and Display Formats)
- [Developer Content Types](id:admin-developer-content-types)
- [Developer Workflows](id:admin-developer-workflows)
- [Developer Locales](id:admin-developer-locales)
- [Developer Relationship Types](id:admin-developer-relationship-types)
- [Developer Shared Fields](id:admin-developer-shared-fields)
- [Developer System Def](id:admin-developer-system-def)
- [Developer Slots](id:admin-developer-slots)
- [Developer Item Filters](id:admin-developer-item-filters)
- [Developer Searches](id:admin-developer-searches)
- [Developer Communities](id:admin-developer-communities)
- [Developer Roles](id:admin-developer-roles)
- [Developer Views](id:admin-developer-views)
- [Developer Display Formats](id:admin-developer-display-formats)
- [Developer Action Menus](id:admin-developer-action-menus)
- [Developer CE Controls](id:admin-developer-ce-controls)
- [Developer Extensions](id:admin-developer-extensions)
- [Developer Server Configs](id:admin-developer-server-configs)
- [Developer File Explorer](id:admin-developer-file-explorer)
- [Developer Database Explorer](id:admin-developer-database-explorer)
- [Developer Pipelines](id:admin-developer-pipelines)
- [Developer Application Files](id:admin-developer-application-files)
- [Object ACL & default template](id:admin-object-acl)
- [Publishing](id:admin-publishing)
- [Server operations](id:admin-server-ops)

## Day-two checklist

| Task | Frequency | Notes |
|------|-----------|--------|
| Review server logs | Daily | Jetty/base logs under the install tree |
| Backup DB + config | Per policy | Before upgrades and major package changes |
| User/role audit | Quarterly | Remove stale accounts; least privilege |
| Publish health | After deploy | Verify last successful pub jobs and delivery targets |
| Certificate expiry | Before TLS renewals | Edge proxy and any embedded keystores |

## Related

- [Getting Started](id:getting-started)
- [Reference](id:reference)
