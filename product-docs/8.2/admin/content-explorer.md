---
id: admin-content-explorer
title: Content Explorer
description: Product Content Explorer shell — browse, Views catalog, Inbox, search, display formats, and server actions
version: "8.2"
order: 42
tags: [admin, content explorer, ui, search, inbox]
---

# Content Explorer

The **Content Explorer** is the product web shell for browsing Sites, folders, pages,
and assets without launching Desktop Content Explorer (DCE). Open it from the SPA at
`/cm/app/spa.jsp?entry=explorer` (or the **Explorer** entry in the product navigation).

## What you see

| Chrome | Purpose |
|--------|---------|
| **Menu bar** (Content / View / Help) | Product commands: search, create site, clipboard, site/subfolder copy, view tools |
| **Display format** | Column layout for the folder list (`validForFolder` formats). Always shown next to the menu bar; a short error stays next to the selector if the catalog cannot be loaded |
| **View tools** | Always-visible **Search**, **Folder Security**, and **Refresh** buttons under the reduced actions / Server actions rows (the same commands remain under **View**; Folder Security is one toolbar control, not a pair of identical buttons) |
| **Reduced actions** | Always-available open / preview / create folder / rename / move / copy / delete |
| **Server actions** (labeled toolbar) | Configuration-driven actions from the CMS action catalog (`rest/actions`) for the current selection. Explorer **executes** these in the SPA (REST + React). It does **not** open legacy Data Flow `.html` pages (those URLs 404 from `/cm/app/explorer`). Always shown as a labeled chrome region under the reduced actions row — even when the catalog is empty or temporarily fails to load |
| **Tree + detail list** | Folder navigation and list of children; folder/item type icons plus optional display-format columns |
| **Views catalog** | System **Views** category under the left tree (My / Community / All / Other Content) |
| **Views → My Content → Inbox** | Assignment list (not a top-level Explorer root — see below) |
| **Context menu** | Right-click an item or folder row for the same catalog filtered for the popup surface |

## Left-hand roots (Sites, Folders, Assets, Design, Recycling)

The Explorer **tree** lists the standard top-level containers returned by the CMS path
service (roles may hide Design/Recycling for some users):

| Root | Maps to | Purpose |
|------|---------|---------|
| **Sites** | `//Sites` | Traditional site folders and pages. Use the tree disclosure control (not only the row label) to expand **Sites**, then expand a sample site to list FastForward folders such as **AboutEnterpriseInvestments**, **Files**, and **Images** |
| **Folders** | `//Folders` | Classic Rhythmyx folder tree (including `$System$` and other repository folders) |
| **Assets** | `//Folders/$System$/Assets` | Shared asset library (CM1 convenience root) |
| **Design** | Design file-system area | Templates, themes, web resources (Admin/Designer) |
| **Recycling** | `//Folders/$System$/Recycling` | Recycled items (Admin/Designer) |

**Folders** is the same classic **//Folders** container available in Desktop Content
Explorer. Use it when you need the full repository folder hierarchy rather than only the
Assets or Sites shortcuts. Expanding **Folders** loads children from the server; folder
visibility still respects folder ACLs.

The detail list always shows a **type icon** in its own column (before Name / display-format
columns). Repository folders — including **`$System$`** and user-created folders — use a
folder icon (open when that row is the current selection). Click the folder icon, or
double-click the row, to browse into the folder. Multi-select **checkboxes** remain in a
separate column when you are selecting several items; they do not replace the folder icon.

Below the folder roots, Explorer lists a **Views** system category. Expand **Views**
to see the same four groups as Desktop Content Explorer:

| Group | What it contains |
|-------|------------------|
| **My Content** | Views in parent category 1 (including system views such as Inbox) |
| **Community Content** | Views in parent category 2 |
| **All Content** | Views in parent category 3. Each **logical** view appears once (same internal name or GUID is not listed repeatedly). Distinct views that share the display label **All** show the internal name in parentheses so you can tell them apart. |
| **Other Content** | Views in parent category 4 (and any view without a known category) |

Selecting a **group** only expands or collapses its children. Selecting a **standard**
view (field-criteria) runs it and replaces the folder list with the result rows.
Each row has **Open** (editor) and **Reveal in folder** (select the item’s parent
folder in the tree), matching the Search panel result actions.

**Inbox** is always listed under **Views → My Content** (the Desktop Content Explorer
path `//Views//MyContent/Inbox`, not a separate Explorer root). Selecting **Inbox**
runs it with the same view execute service as standard views
(`POST /services/views/{idOrName}/execute`, body wrapped as `ViewExecuteRequest`)
and shows assignment rows, or an empty state when you have no Inbox items. A
Retry button appears only for a real execute failure — not for a request-envelope
mismatch.

Other **custom URL** views (for example Outbox or Recent) stay listed so you can see
them in the catalog, but they cannot be executed from this Explorer release. Those
leaves show an error instead of an empty list. Do not use Developer → Views as a
substitute for this Explorer tree — Developer is the design catalog, not the
operator navigation path.

If the left tree fails to load, Explorer shows an **error** in the tree panel (not a
blank list). Typical causes are a path-service HTTP error or a session timeout.
Refresh after login, then confirm
`/Rhythmyx/services/pathmanagement/path/folder/` returns JSON
`{"PathItem":[…]}` with Sites / Folders / Assets (and Design / Recycling when
your role allows them). A `500` whose message mentions
`IllegalAnnotationExceptions` is a server serialization defect on `PathItem`
(fixed in 8.2 for the `relatedObject` field) — collect that response body for
support.

## Folder mutations dual-run (optional)

By default, Explorer **create / rename / move / delete** folder actions use the same
pathmanagement REST surface as browse. Operators and QA can opt into a dual-run path that
sends those mutations under **Folders** and **Sites** to the Rhythmyx content-explorer
folders REST façade (`/Rhythmyx/rest/content-explorer/folders`) while list/pagination stay
on pathmanagement.

| Setting | Detail |
|---------|--------|
| Name | `perc.explorer.rxFolderMutations` |
| Default | **off** |
| Enable in browser | Append `?rxFolderMutations=1` to the Explorer URL, or set storage key `perc.explorer.rxFolderMutations` to `true` |
| Scope when on | `/Folders` and `/Sites` (and repository `//…` forms) only. The Assets library is **not** in scope even when its repository path is `/Folders/$System$/Assets` or `//Folders/$System$/Assets`. |
| Unchanged | Browse/list, folder ACL (security panel), `/Assets` (including `/Folders/$System$/Assets` and `//Folders/$System$/Assets`), `/Design`, `/Recycling` (including `/Folders/$System$/Recycling`) |
| Copy folder | Public REST `POST /rest/folders/copy/folder` with a `CopyFolderItemRequest` root (`itemPath` + `targetFolderPath`). Not pathmanagement `moveItem` (that DTO is move-only). |

Documented for integrators on [Public REST](id:developer-rest). Leave the flag **off** in
production unless you are validating the RX folder façade with QA.

## Server actions (how they run)

The **Server actions** toolbar and the item **context menu** use the same catalog
(`GET /services/actions/find`, plus type and **template** menus for the selected item).

| Action | What happens |
|--------|----------------|
| **Preview** (and per-template children) | Opens assembly preview (`GET /services/assembly/preview-location`) or the default page/asset preview. New language: **template**, not variant. |
| **New Item** | Type menus list from `POST /services/actions/find/types`. Creating an item still requires the Content Editor (not in this Explorer slice) — choosing a type shows that the editor is not available. |
| **Workflow** | Allowed transitions run through itemmanagement (not `wfactionset.html`) |
| **Purge** | Confirm, then permanently purge a **page** or **asset** (`pagemanagement` / `assetmanagement` purge). Other types stay unavailable. Distinct from **Delete** (remove from folder / recycle). |
| **Edit / Quick Edit / View content** | Not the CM1 editor (`?view=editor`). Explorer shows that the content editor is not available in this slice. |
| **Translate** | Opens the Explorer **Translations** panel (create locale copies). Does not open the legacy translate XSL wizard. |
| **Impact Analysis** | Opens the Explorer **Dependencies** panel for the selected item. |
| **Copy URL to Clipboard** | Copies the site-path preview URL (or CMS path) for the selected item. |
| **Revisions** | Opens the Revisions panel; restore is available when the selected revision is restorable. |
| **Audit Trail** | Same Revisions panel, audit-trail tab. |
| **New Copy** | Confirm, then create a copy in the current folder. |
| **Promotable Version** | Confirm, then create a promotable version in the current folder. |
| **Flush Cache** (Refresh Item) | Confirms, then flushes **all** assembler pages (not only the selected item). |
| **Nav Reset** | Same goal as classic Nav Reset. On 8.2 this is typically a no-op once managed navigation is loaded (FastForward 6.0+ variants unused). |
| **Publish Now** | Confirms, then demand-publishes a **page** or **asset** (`GET /services/sitemanage/publish/page/{id}` or `/resource/{id}`). Other types stay unavailable. Does not open the demand-publish servlet page. HTTP 200 with application-level `FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` is a failure (same as classic Finder) — Explorer shows the server warning and does not refresh as if published. |
| **Active Assembly** / slot arrange | Not available in this Explorer slice (needs a React Active Assembly host). |

Do not bookmark or paste `../sys_cxSupport/…html` URLs from older Desktop Content Explorer
menus — they are not Explorer pages.

## Sites list and Create Site

Under the tree root **Sites** you see traditional site folders available to your community
(sample sites after a demo-sites install, plus any sites you create). Expand **Sites** and
select a sample site to browse FastForward folders and pages (About…, Files, Images,
and the site NavTree).

To create a new **traditional** repository Site from Explorer:

1. Choose **Content → Create Site** (available without selecting an existing site).
2. Complete the wizard: site name → base template → confirm → create.
3. On success, Explorer navigates to `/Sites/<new-site-name>`.

This wizard creates repository Sites only. Configure **Virtual Site** source properties
(Git/filesystem) from **Developer → Sites** / Site detail — see
[Sites & content structure](id:admin-sites) and [Virtual Sites](id:developer-virtual-sites).

Related Content menu commands:

- **Create Site** — new traditional Site (no site context required)
- **Site Copy** / **Subfolder Copy** — copy workflows when a site or folder is in context
- **Search** — same Search panel as **View → Search**

## Views → My Content → Inbox

Desktop Content Explorer **Inbox** is **not** a separate Content Explorer root and is **not**
the workflow transition toolbar. It is a **system view** under **Views → My Content**
(repository path `//Views//MyContent/Inbox`, classic resource `sys_cxViews/inbox`).
Workflow **ShowInInbox** flags feed that view’s data.

On the product web Explorer (`/cm/app/spa.jsp?entry=explorer`):

1. Open **Explorer**.
2. In the left tree, expand **Views** (below the Sites / Folders / Assets roots when that
   catalog is deployed).
3. Expand **My Content**.
4. Select **Inbox**.

Selecting **Inbox** runs the Inbox custom-URL view
(`POST /services/views/Inbox/execute` — see [Public REST](id:developer-rest)) and lists
items assigned to your workflow roles. **Open** and **Reveal in folder** behave like other
Explorer result rows when the leaf is wired.

| Result | Meaning |
|--------|---------|
| Rows in the Inbox results list | You have current assignments |
| Empty list (`children: []`) | No current assignments — this is success, not an error |
| Views category or Inbox leaf missing | This build does not yet show the operator Inbox leaf; use DCE **Views → My Content → Inbox**, or wait for the Inbox leaf on the Explorer route |
| Error instead of a list | Custom-URL execute is not available on this server, or the view is not in the Inbox family allow-list |

Do **not** look for a top-level **Inbox** tree root. Do **not** treat **Developer → Views**
(design catalog) as the operator Inbox. Outbox / Recent / Session peers live in the same
My Content group when the Views catalog returns them; this page documents **Inbox** as the
assignment surface.

Automated QA rerun (after `perc-devctl qa-up`, from `modules/perc-qa-automation/frontend`):

```text
npm run test:surface -- --path tests/explorer-inbox.spec.js
```

The spec soft-skips with a clear reason when the Inbox leaf or assignment execute path is
missing on a minimal H2 cell.

## Search panel

Use **Search** from any of:

- the always-visible **Search** button on the Explorer view-tools row
- **View → Search**
- **Content → Search**

All three commands toggle the same **Search panel**. The panel opens in a
full-width region **directly under the header chrome** (menu bar, reduced
actions, Server actions, view tools) so it is visible without scrolling past
the folder tree and detail list.

When the panel is open you can:

1. Enter free-text criteria (scoped to the current folder path when a folder is active).
2. Submit the search and open a hit or reveal it in its parent folder.
3. Pick a **saved / design search** from the catalog (when the server exposes one) and run it.
   The picker includes CX **searches and views**. The default **All** view (`View_All`) is
   listed when that design object exists on the server. Custom URL searches stay listed
   but cannot be run from Explorer.

Closing **Search** again (view-tools button, **View → Search**, or **Content → Search**)
hides the panel. Revealing a result in its folder also closes the panel so the
tree/list can show the destination.

Extended search uses the same sitemanage search services as other product hosts; on
minimal fixtures without a search index, free-text may show an error state while the
panel chrome (input, submit, optional saved-search picker) remains available.

## Display format

Use the **display format** selector next to the menu bar to choose list columns for the
current folder (`validForFolder` formats). Changing the format reloads the detail list with
the selected columns.

## Server actions and context menu

The **Server actions** toolbar is the labeled product chrome under Open / Preview / Create Folder
(and related reduced actions). It is always present on the Explorer page so you can tell the
catalog region apart from the Content / View / Help menu bar and the display-format selector.

Menus and toolbar buttons come from the server action catalog used by Content Explorer:

- Cascading **MENU** parents render as **one toolbar control** with a dropdown (`▾`). Open
  the parent to choose a child command. Child items are **not** shown as extra top-level
  buttons while the menu is closed.
- When you select a content item, the shell keeps that cascading tree and may add
  content-type **New** commands under an existing menu. It does not replace the tree with
  a flat list of every allowed command.
- When only a folder is active, the shell loads the same cascading action tree.
- **Desktop-only** actions (for example custom application protocols that only DCE can run)
  are **hidden** in the web shell so operators are not offered controls that cannot succeed
  in the browser.
- Actions of type **context menu** appear on right-click, not as permanent toolbar buttons.
- Workflow transition triggers (when available for the selected item) appear as a labeled
  **one-click button group** on the toolbar and in the context menu (not a dropdown).
- If the catalog cannot be loaded, the **Server actions** region stays visible with a short
  error message (and an empty-action placeholder) rather than disappearing from the page.

Selecting a server action either navigates to a product-safe same-origin URL or refreshes the
list after a client-handled command (for example a workflow transition).

## Folder security

Open folder ACL and properties from either:

- The **Security** button on the Explorer view-tools toolbar (always visible next to
  **Refresh**), or
- **View → Folder Security**

Both commands toggle the same **Folder security** panel. When a folder is selected
(or the tree folder id can be resolved), the panel loads that folder’s permissions.
When no folder is in context, Explorer shows a short hint to select a folder first.

The panel is product React chrome on `spa.jsp?entry=explorer`. It does not open the
legacy miller-column Finder. Site admins with ADMIN access on the folder can edit
principal lists; other users see a read-only view.

## Other View tools

From the **View** menu you can also toggle:

- **Folder security** — same panel as the view-tools **Security** button (see
  **Folder security** above). ACL and folder properties for the **selected folder**
  (tree or list): community, community id, locale, display format, and workflow id,
  plus named **user and role** identities on the Admin / Write / Read / View lists
  (seed folders typically list the **Admin** and **Designer** roles). Administrators can
  add or remove principals and edit locale (and other persistable fields), then **Save**.
  Removing your own user name or a role you hold from a list prompts a self-lockout
  confirmation before save. Without a selected folder the shell shows a select-folder
  hint instead of a blank panel.
- **Translations**, **Relationships**, and **Dependencies** — advanced item tools when a
  suitable item is selected
- **Clipboard** — multi-select copy/cut staging when items are selected

## If Content Explorer cannot start

If the Explorer area shows a short error that the **application session is not
available** instead of the tree and list, the page did not receive the usual
authenticated SPA session. Reload the page, or sign out and sign in again, then
open **Explorer** from the product navigation. Do not use a bookmarked editor
URL that embeds Explorer without the SPA shell.

## Related

- [Sites & content structure](id:admin-sites)
- [Users, roles & security](id:admin-users-roles)
- [Publishing](id:admin-publishing)
