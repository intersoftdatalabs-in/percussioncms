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
| **Sites** | `//Sites` | Traditional site folders and pages. Use the tree disclosure control (not only the row label) to expand **Sites**, then expand a sample site to list FastForward folders such as **AboutEnterpriseInvestments**, **Files**, and **Images**. Sample-site **folder names** can differ from the site name (for example folder `CorporateInvestments` vs site `Corporate_Investments`); Explorer still opens the folder. A path that is not a site or folder (for example a typed `/Sites/Demo/Home` on a cell that only has the FastForward sample site) is treated as **not found** — it does not fail the Explorer with a server error |
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
| **Preview** (and per-template children) | Select a **page** or **asset** row first (folder rows stay Preview-disabled). Listed pages with a content id open the chrome-less editor in **view** mode (`spa.jsp?entry=editor&mode=view`) — a same-origin HTTP 200 host. Pages without an id use the Finder site-path URL (`/Sites/…?percmobilepreview=`). Assets open the asset view URL. Per-template children still use assembly preview (`GET /services/assembly/preview-location`). Applies to listed pages and assets, including customer-defined content types (type names are not a closed list; FastForward names stay stable). New language: **template**, not variant. |
| **New Item** | Select a **site or folder** first, then choose **New Item**. When the catalog lists types under **New**, pick a type from that menu. When **New Item** is a single action (no type children), Explorer opens **Choose a content type** — pick a type and **OK**, or **Cancel** to leave the folder unchanged. Explorer then creates the item in the current folder (`POST /services/itemmanagement/item/create`) and opens the React Content Editor. It does **not** show *Choose a content type from New Item* as an error toast instead of the picker, and it does not open leftover Content Editor HTML. **Pages** (`percPage`) need a page template. Explorer loads allowed templates for the type, then the site's templates when the type has none. One template is used automatically; more than one opens **Choose a page template**. Cancel leaves the folder unchanged. If no template is available, Explorer asks you to pick a site folder or use Home → Create. Home → Create **Asset** uses the same create + React editor host (not leftover `editAsset.jsp`). |
| **Workflow** | Allowed transitions run through itemmanagement (not `wfactionset.html`) |
| **Purge** | Confirm, then permanently purge a **page** or **asset** (`pagemanagement` / `assetmanagement` purge). Other types stay unavailable. Distinct from **Delete** (remove from folder / recycle). |
| **Edit / Quick Edit / View content** | Opens a new Content Editor window (`spa.jsp?entry=editor`) that checkouts the item (Edit) and shows content-type fields. Text, rich text (TinyMCE), keyword, and community controls save through `PUT /services/itemmanagement/item/fields/{id}`. File and image controls upload through `PUT /services/itemmanagement/item/binary/{id}/{field}`. Does not open the CM1 editor (`?view=editor`). |
| **Translate** | Opens the Explorer **Translations** panel for the **selected page or asset** (this item’s locale, related variants, and create-variant). List row ids may be GUID-shaped (`1-101-708`); the panel uses the content-id segment. Folders and sites have no content id — Explorer shows a select-item hint. Does not open the legacy translate XSL wizard. |
| **Impact Analysis** | Opens the Explorer **Dependencies** panel for the selected item. |
| **Copy URL to Clipboard** | Copies the site-path preview URL (or CMS path) for the selected item. |
| **Revisions** | Opens the Revisions panel; restore is available when the selected revision is restorable. **Promote revision** opens the same chrome-less editor host (`mode=promote`) and restores the chosen revision through `GET /services/itemmanagement/item/restoreRevision/{revisionGuid}`. |
| **Audit Trail** | Same Revisions panel, audit-trail tab. |
| **New Copy** | Confirm, then create a copy in the current folder. |
| **Promotable Version** | Confirm, then create a promotable version in the current folder. |
| **Flush Cache** (Refresh Item) | Confirms, then flushes **all** assembler pages (not only the selected item). |
| **Nav Reset** | Same goal as classic Nav Reset. On 8.2 this is typically a no-op once managed navigation is loaded (FastForward 6.0+ variants unused). |
| **Publish Now** | Select a **page** or **asset** row in the list first (clicking only **Sites** or another folder is not enough). The toolbar and the item context menu hide Publish Now until a page or asset is selected. Explorer then confirms and demand-publishes (`GET /services/sitemanage/publish/page/{id}` or `/resource/{id}`). Other types stay unavailable. Does not open the demand-publish servlet page. HTTP 200 with application-level `FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` is a failure (same as classic Finder) — the **Server actions** error region shows the server warning (for example licensing / Publication stopped) and the list does not refresh as if published. Folder-only selection does not publish. |
| **Active Assembly** | Opens a new window that assembles the selected item with its **page** or **snippet template** (`GET /services/assembly/preview-location` in an iframe). A light overlay shows the content id and template. Known **scalar text** fields on the assembled output become contenteditable (classic `PsAaField` wrappers, `data-perc-field` markers, or a unique assembled text value). **Save fields** writes through `PUT /services/itemmanagement/item/fields/{id}` — the same API as the React Content Editor. If the assembled page has no mappable nodes, the overlay lists those fields so you can still edit them. Rich text, file, image, keyword, and community stay on the Content Editor. If the requested template is missing from the item's available list, Explorer shows that mismatch instead of silently using another template. A failed template catalog load is an error, not a hidden preview retry. Does not open leftover Active Assembly HTML (`variantlistwithslots.html` / `itemassembly.html`) or leftover Content Editor HTML. |
| **Slot add / create / arrange** | Available when a **slot** is selected on the Active Assembly canvas (`GET /services/assembly/slot-relationships/canvas`). **Add** (AA **Add**, or Explorer **Slot Add** when that slot is selected) opens Content Browser. Confirming a page or asset adds the snippet through `POST /services/assembly/slot-relationships` (allowed snippet template from the slot). Cancel leaves the slot unchanged and does not post. Folder browse without a selected slot stays **Select a slot in Active Assembly first** — Explorer does not open leftover `variantlistwithslots.html` / `itemassembly.html`. **Create** (AA **Create**, or Explorer **Slot Create** when that slot is selected) opens a type / snippet-template / folder picker from the slot's allowed lists. Apply creates the item (`POST /services/itemmanagement/item/create`) and opens the React Content Editor (`spa.jsp?entry=editor`) — not leftover `editAsset.jsp` or Data Flow create HTML — then adds the new item through `POST /services/assembly/slot-relationships`. Cancel closes the picker and does not create or add. **Arrange** (AA **Move up** / **Move down** / **Change template** / **Remove**, or the matching Explorer **Arrange** / **Change Template** / **Move to Slot** actions) is available only when a **snippet** is selected in that slot. Move reorders through `POST /services/assembly/slot-relationships/{id}/move`. Remove confirms, then deletes the relationship (`DELETE /services/assembly/slot-relationships/{id}`). Change template opens a slot + snippet-template picker; Apply writes `POST /services/assembly/slot-relationships/{id}/template-slot`. Cancel leaves the relationship unchanged. Selecting only a slot (no snippet) shows **Select an item in the slot first**. Folder browse has no slot — Explorer does not invent Arrange actions from a folder and does not open leftover `itemassembly.html` / `variantlistwithslots.html`. Inline field edits use itemmanagement, not leftover Content Editor HTML. |

Do not bookmark or paste `../sys_cxSupport/…html` URLs from older Desktop Content Explorer
menus — they are not Explorer pages.

## Content Editor controls

The chrome-less React Content Editor (`spa.jsp?entry=editor` / `/editor`) maps
content-type **Control** names from `GET /services/contenttypes/{type}` onto widgets:

| Control / field | Widget | How it is saved |
|-----------------|--------|-----------------|
| `sys_EditBox` and other short text | Text input | `PUT /services/itemmanagement/item/fields/{id}` |
| `sys_TextArea` | Multi-line text | Same fields API |
| `sys_tinymce`, `sys_TinyMCE`, `sys_EditLive`, HTML | TinyMCE (textarea fallback if the shipped TinyMCE script is unavailable) | Same fields API — HTML string |
| Keyword-named dropdowns | Keyword picker | Same fields API — selected choice value |
| `sys_communityid` | Community picker | Same fields API |
| `sys_File` / file asset fields | File upload | `PUT /services/itemmanagement/item/binary/{id}/{field}` |
| Image controls (`sys_webImageFX`, `img`) | Image upload with local preview | Same binary API |

Save and **Check In** stay on itemmanagement. The host does not request leftover Content
Editor HTML (`checkoutedit.xml`, `contenteditorurls.html`, `?view=editor`).

## Sites list and Create Site

Under the tree root **Sites** you see traditional site folders available to your community
(sample sites after a demo-sites install, plus any sites you create). Expand **Sites** and
select a sample site to browse FastForward folders and pages (About…, Files, Images,
and the site NavTree).

To create a new Site from Explorer:

1. Choose **Content → Create Site** (available without selecting an existing site).
2. On **Site type**, choose **Traditional**, **Page**, or **Virtual**.
3. Complete the remaining steps (name/description; managed navigation only for Traditional/Page;
   page template only for Page; optional Git root on Virtual confirm).
4. On success, Explorer navigates to `/Sites/<new-site-name>`.

**Virtual** create does not show managed navigation or a page template. If you supply a Git
root path, the wizard PUTs the existing `VirtualSiteProperties` envelope after create.
Otherwise finish source settings on **Developer → Sites**. See
[Sites & content structure](id:admin-sites) and [Virtual Sites](id:developer-virtual-sites).

Related Content menu commands:

- **Create Site** — new Traditional, Page, or Virtual Site (no site context required)
- **Site Copy** / **Subfolder Copy** — copy workflows when a site or folder is in context
- **Search** — same Search panel as **View → Search**

### Subfolder Copy

**Content → Subfolder Copy** opens a wizard overlay when a folder is in context
(the current tree folder, or a selected folder row). Source path is prefilled from
that folder.

| Control | Behavior |
|---------|----------|
| **Next** | Advance to the next step (source → target → confirm → run). Next does not copy and does not require the typed source path to exist; a missing fixture path such as `/Sites/Demo/Home` is not a server error |
| **Back** | Return to the previous step. The overlay stays open |
| **Cancel** | Close the wizard without copying. Cancel is **not** Back — it does not reset to step 1 while leaving the overlay up |
| **Escape** | Same as Cancel: dismiss without submitting |
| Tree or list item | Selecting another folder or item closes the wizard without submitting |
| Click outside the wizard | Also dismisses without submitting |

After Cancel, Escape, item-click, or click-away, focus returns to the Explorer
**Content** menu so you can continue working in the shell. The wizard does not
POST a folder copy until you reach the last step and choose **Submit**.

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
| Views category or Inbox leaf missing | The Views catalog on this server does not include Inbox; use Desktop Content Explorer **Views → My Content → Inbox** or check that the Inbox design view is installed |
| Error instead of a list | Custom-URL execute failed (unsupported URL, missing `sys_cxViews`, or a server error). An empty list is success, not an error |

Do **not** look for a top-level **Inbox** tree root. Do **not** treat **Developer → Views**
(design catalog) as the operator Inbox. Outbox / Recent / Session peers live in the same
My Content group when the Views catalog returns them; this page documents **Inbox** as the
assignment surface.

Automated QA rerun (after `perc-devctl qa-up`, from `modules/perc-qa-automation/frontend`):

```text
npm run test:surface -- --path tests/explorer-inbox.spec.js
```

The spec soft-skips only when `GET /services/views` has no Inbox design view. If Inbox is
in the catalog, the Explorer leaf must run execute (`200`) and show rows or an empty
state — a missing results region is a product defect, not a skip.

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
2. Submit the search from this Explorer page (`spa.jsp?entry=explorer` — the
   `searchModern.jsp` pilot is not required). The server answers **200** with
   matching items **or** an empty results page. Open a hit or reveal it in its
   parent folder.
3. Pick a **saved / design search** from the catalog (when the server exposes one) and run it.
   The picker includes CX **searches and views**. The default **All** view (`View_All`) is
   listed when that design object exists on the server. **Run saved search** on **All**
   (or any other standard/user search) returns matching items or an empty results page —
   not a generic I/O error. At Explorer **root** (`/`) the run is unscoped (all content the
   search allows). When a real folder such as `/Sites` is selected, the run is scoped to
   that folder. Custom URL searches stay listed but cannot be run from Explorer.

Closing **Search** again (view-tools button, **View → Search**, or **Content → Search**)
hides the panel. Revealing a result in its folder also closes the panel so the
tree/list can show the destination.

Extended search uses the same sitemanage search services as other product hosts.
Submit on the product Explorer route returns matching items or an **empty-success**
page (including a quiet or missing search index). A 500 error panel after Submit
is a product defect — the input, Submit button, and optional saved-search picker
stay available either way.

## Display format

Use the **display format** selector next to the menu bar to choose list columns for the
current folder. Explorer prefers folder-valid formats (`validForFolder`). If that filtered
catalog is empty, it lists the full display-format catalog so you can still switch columns.
Changing the format reloads the detail list using that format's numeric id
(`displayFormatId`) so headings and cell values match the selected columns. The selector
stays available if the catalog fails to load (a short error appears next to it).

## Server actions and context menu

The **Server actions** toolbar is the labeled product chrome under Open / Preview / Create Folder
(and related reduced actions). It is always present on the Explorer page so you can tell the
catalog region apart from the Content / View / Help menu bar and the display-format selector.

Menus and toolbar buttons come from the server action catalog used by Content Explorer:

- Cascading **MENU** parents render as **one toolbar control** with a dropdown (`▾`).
  On a typical catalog that includes **Paste**, **Arrange**, **View**, and **Create**,
  each of those names is a single control (`aria-haspopup=menu`). Open the parent to
  choose a child command. Child items (for example **Move** under Paste, or
  **View Properties** under View) are **not** shown as extra top-level buttons while
  the menu is closed. **Workflow** transitions stay a labeled one-click button group.
- **Right-click a selected list row** (Sites, Assets, or a folder with children)
  opens the same catalog as a **nested context menu** — MENU parents stay
  expandable, not a flat list of every child label. Desktop-only actions stay hidden.
- When you select a content item, the shell keeps that cascading tree and may add
  content-type **New** commands under an existing menu. It does not replace the tree with
  a flat list of every allowed command.
- When only a folder is active, the shell loads the same cascading action tree.
  **New Item** without type children opens **Choose a content type** (same create
  path as a type under **New**). It does not fail with a toast that says to
  choose a type from a menu that is not there.
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
  **page or asset** is selected (not a folder or site). On sample FastForward
  sites, expand a site in the tree, open a section folder (for example
  **AboutEnterpriseInvestments** — not only a `Pages` folder), and select a
  content row. **View → Relationships** then mounts the relationships panel
  (loading, results, empty, or an error). A folder-only or empty selection
  keeps the select-item hint. See **Translations** below for locale variants.
- **Clipboard** — copy/cut staging panel. **View → Clipboard** always opens
  the panel (even when empty) and shows a check mark while it is visible.
  Use **Content → Add to clipboard** after multi-select to put items on the
  clipboard and open the panel (including **Sites** rows). Do not click
  **View → Clipboard** again after Add — that hides the already-open panel.
  Paste from the panel when a destination folder is selected.

## Translations

Open **View → Translations** after selecting a **page or asset** in the list (or use
**Translate** on Server actions / the item context menu).

The panel shows **this item’s current locale** and **related locale variants**, and lets
an authorized user **Create variants** for catalog locales the item does not already
have. Explorer list rows identify items with a Percussion content id. The id is often
GUID-shaped (for example `1-101-708`); the panel uses the last segment (`708`) for
both the variants request and create-variant. That GUID form must not fail with
“Selected item does not have a numeric content id.”

**Folders and sites** have no content id. With Translations open and only a folder or
site selected, Explorer shows a select-item hint instead of the live panel.

In-flight translation queue status is not available (product disposition).

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
