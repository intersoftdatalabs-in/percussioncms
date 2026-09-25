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
| **Menu bar** (Content / View / Help) | Product commands: search, create site, clipboard, **Copy selected to folder**, **Move selected to folder**, **Recycle selected**, site/subfolder copy, **Rename Site**, view tools. **Rename Site** is enabled when the selection is under a site (`/Sites/<name>`). It asks for a new name and posts `POST /services/sites/{name}/rename` (`RenameSiteRequest` with `name`). After HTTP 200 the tree reloads on the new site name. Blank or illegal names are HTTP **400**; non-admin callers receive **403**; a name already used by a site or site folder is **409**. Cancel closes the panel and does not change the name. Rename Site is not folder **Rename**, not Site Copy, and does not copy content. |
| **Display format** | Column layout for the folder list (`validForFolder` formats). Always shown next to the menu bar; a short error stays next to the selector if the catalog cannot be loaded |
| **View tools** | Always-visible **Search**, **Folder Security**, **Item properties**, and **Refresh** buttons under the reduced actions / Server actions rows (the same commands remain under **View**; Folder Security is one toolbar control, not a pair of identical buttons). **Item properties** opens a side panel for a selected **page, file, or asset** to edit **Name** (`sys_title`) and **Display title** (`displaytitle`). Save posts public REST `POST /rest/folders/item-properties` (`ItemPropertiesRequest` with `itemPath`, `name`, optional `displayTitle`). Blank name is HTTP **400**; missing item is **404**; non-admin API callers receive **403**; folder selected, name-in-use, or locked is **409**. View-only users see the fields read-only. Folder ACL remains **Folder Security**. |
| **Reduced actions** | Always-available open / preview / **Create Folder** / **Rename** / **Move** / **Copy** / **Delete** / **Restore**. **Create Folder** uses the selected Sites or Assets (or other writable) folder as the parent. The name must be a single folder segment (no `/` or `\`). On the product Explorer URL (`spa.jsp?entry=explorer`, without `?rxFolderMutations=1`) it posts public REST `POST /rest/folders/create` (`CreateFolderRequest` with `parentPath` + `name`). After HTTP 200 the detail list and the tree children of that parent refresh so the new name appears without a manual **Refresh**. Missing parent is HTTP **404**; non-admin API callers receive **403**; name-in-use or parent-not-a-folder is **409** — Explorer surfaces those as errors and does not treat them as a successful create. Create Folder is not rename, clipboard paste, or the site wizard. **Create Page** (same toolbar, writable folder selected) asks for a page name and a page content type, then posts `POST /services/itemmanagement/item/create` (`ItemCreateRequest` with `folderPath`, `name`, `contentType`, and the type’s first allowed template when the type is a page). After HTTP 200 the new page appears in the open folder list. Cancel closes the dialog and does not create. A blank name, a name that contains `/` or `\`, a missing content type, no page type in the catalog, no template on a page type, or HTTP **400** / **403** / **404** / **409** stays in the dialog as an error and is not success. Create Page is not Create Folder and not New Site. **Rename** prompts for a new name. A selected **folder** posts pathmanagement `POST …/path/renameFolder` (`RenameFolderItem`). A selected **page, file, or asset** posts public REST `POST /rest/folders/rename/item` (`RenameFolderItemRequest` with `itemPath` + `newName`). After HTTP 200 the new name appears in the detail list (and the folder tree for folders) without a manual **Refresh**. Missing item is HTTP **404**; non-admin API callers receive **403**; name-in-use, locked, or folder-selected-on-item-endpoint is **409** — Explorer surfaces those as errors and does not treat them as a successful rename. **Move** opens a destination folder picker (path field). A selected **folder** posts public REST `POST /rest/folders/move/folder`; a selected **page, file, or asset** posts `POST /rest/folders/move/item`. Both use a `MoveFolderItem` body (`itemPath` + `targetFolderPath`) and do **not** require `?rxFolderMutations=1`. Finder paths such as `/Assets/…` are accepted (the server maps them to repository `//Folders/$System$/Assets/…`). After HTTP 200 Explorer opens the destination so the item leaves the source list/tree and appears under the destination without a manual **Refresh**. Missing source or destination is HTTP **404**; non-admin API callers receive **403**; destination conflict is **409** — Explorer surfaces those as errors and does not treat them as a successful move. Move is not a clipboard paste.  **Copy** opens a destination folder picker (path field). A selected **folder** posts public REST `POST /rest/folders/copy/folder`; a selected **page, file, or asset** posts `POST /rest/folders/copy/item`. Both use a `CopyFolderItemRequest` body (`itemPath` + `targetFolderPath`) and do **not** require `?rxFolderMutations=1`. After a successful copy, Explorer opens the destination so the copy appears in the destination list without a manual **Refresh**. Folder copies also appear under the destination in the tree. Copy is not the Content → Subfolder Copy wizard. **Delete** is enabled for a selected **folder** or a writable **page, file, or asset** (view-only users cannot delete). Confirm, then Explorer **recycles** the selection: folders use pathmanagement `POST …/path/deleteFolder` (`DeleteFolderCriteria`, `shouldPurge=false`); pages/files/assets use public REST `DELETE /rest/folders/item/{path}` (403/404/409 mapped). After HTTP 200 the name leaves the detail list (and the tree for folders) without a manual **Refresh**. Delete does not require `?rxFolderMutations=1` and does not call `/path/delete/{path}` (that resource does not exist). **Restore** is enabled when the selection is under **Recycling**. It posts public REST `PUT /rest/folders/recycle/restore/{guid}` and returns the item to its original folder. Missing recycled GUID is HTTP **404**; non-admin API callers receive **403**; destination name-in-use is **409** — Explorer surfaces those as errors and does not treat them as a successful restore. **Empty recycle bin** is enabled while the open folder (or selection) is under **Recycling**. It asks for an explicit confirm, then posts public REST `POST /rest/folders/recycle/empty` and reloads the bin so a successful empty shows no children. A missing Recycling root is HTTP **404**; non-admin callers receive **403**; items that cannot be purged are **409**. Explorer shows those as errors and does not treat them as a successful empty. **Purge** appears only for a writable selection under **Recycling** (it is not shown on Sites or Assets). Confirm, then Explorer permanently deletes that one item with `DELETE /rest/folders/recycle/{guid}` and refreshes the list so the name stays gone after reload. Missing GUID is HTTP **404**; non-admin callers receive **403**; in-use or locked is **409**. Purge is not Restore and is not Empty recycle bin. |
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
| **Sites** | `//Sites` | Traditional site folders and pages. Use the tree disclosure control (not only the row label) to expand **Sites**, then select a sample site. Selecting a site opens its **Pages** listing so pages such as **Corporate Investments Home** are previewable (FastForward sample sites inject a Pages chrome folder when the site has no physical Pages folder). Expand the site node to browse other FastForward folders such as **AboutEnterpriseInvestments**, **Files**, and **Images**. Sample-site **folder names** can differ from the site name (for example folder `CorporateInvestments` vs site `Corporate_Investments`); Explorer still opens the folder. A path that is not a site or folder (for example a typed `/Sites/Demo/Home` on a cell that only has the FastForward sample site) is treated as **not found** — it does not fail the Explorer with a server error |
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
view (field-criteria) or a **packaged CX view** (Inbox, Outbox, Recent, Session,
Checked Out By Me, Duplicate Folder Paths) runs it
(`POST /services/views/{idOrName}/execute`) and replaces the folder list with the
result rows, or an empty state when the view returns no items. Each row has
**Open** (editor) and **Reveal in folder** (select the item’s parent folder in the
tree), matching the Search panel result actions. A view the catalog no longer
has returns **HTTP 404** in the results panel. A user-created custom URL view
(not one of the packaged names above) runs only for an Admin session; other
roles see **HTTP 403** in that same panel, with Retry.

**Inbox** is always listed under **Views → My Content** (the Desktop Content Explorer
path `//Views//MyContent/Inbox`, not a separate Explorer root). Selecting **Inbox**
runs it with the same view execute service as standard views
(`POST /services/views/{idOrName}/execute`, body wrapped as `ViewExecuteRequest`)
and shows assignment rows, or an empty state when you have no Inbox items. A
Retry button appears only for a real execute failure — not for a request-envelope
mismatch.

**Outbox**, **Recent**, and the other packaged CX views use the same execute path
as Inbox. Selecting one of those leaves (or any other non-Inbox custom URL view
the catalog lists) on `spa.jsp?entry=explorer` replaces the folder list with
result rows, an empty state, or the results error that includes the HTTP status.
Explorer does not keep the folder list and show a message that custom URL views
cannot be run. Do not use Developer → Views as a substitute for this Explorer
tree — Developer is the design catalog, not the operator navigation path.

If the left tree fails to load, Explorer shows an **error** in the tree panel (not a
blank list). Typical causes are a path-service HTTP error or a session timeout.
Refresh after login, then confirm
`/Rhythmyx/services/pathmanagement/path/folder/` returns JSON
`{"PathItem":[…]}` with Sites / Folders / Assets (and Design / Recycling when
your role allows them). A `500` whose message mentions
`IllegalAnnotationExceptions` is a server serialization defect on `PathItem`
(fixed in 8.2 for the `relatedObject` field) — collect that response body for
support.

## Folder mutations dual-run (diagnostic)

By default, Explorer **create / rename / move / delete** folder actions use the same
pathmanagement REST surface as browse. **Operators use this flag-off product URL**
(`spa.jsp?entry=explorer` without `?rxFolderMutations=1`). The dual-run switch is a
**diagnostic leftover** for QA and integrators validating the Rhythmyx content-explorer
folders REST façade. **Do not enable `perc.explorer.rxFolderMutations` in production.**

When QA appends `?rxFolderMutations=1`, Create Folder / Rename / Delete under **Folders**
and **Sites** go through `/Rhythmyx/rest/content-explorer/folders` (HTTP 200) while
list/pagination stay on pathmanagement. The product default stays **off**.

| Setting | Detail |
|---------|--------|
| Name | `perc.explorer.rxFolderMutations` |
| Default | **off** — leave it off for operators |
| Diagnostic enable (QA only) | Append `?rxFolderMutations=1` to the Explorer URL, or set storage key `perc.explorer.rxFolderMutations` to `true` |
| Scope when on | `/Folders` and `/Sites` (and repository `//…` forms) only. The Assets library is **not** in scope even when its repository path is `/Folders/$System$/Assets` or `//Folders/$System$/Assets`. |
| Unchanged | Browse/list, folder ACL (security panel), `/Assets` (including `/Folders/$System$/Assets` and `//Folders/$System$/Assets`), `/Design`, `/Recycling` (including `/Folders/$System$/Recycling`) |
| Copy folder | Public REST `POST /rest/folders/copy/folder` with a `CopyFolderItemRequest` root (`itemPath` + `targetFolderPath`). Not pathmanagement `moveItem` (that DTO is move-only). Copy does not require the dual-run flag. |
| Copy item (page/file/asset) | Public REST `POST /rest/folders/copy/item` with the same `CopyFolderItemRequest` root. Not `copy/folder` (that endpoint 500s for non-folders). The copy is a NewCopy of the item in the destination folder and starts in that item's workflow **initial state**. Missing source or destination is HTTP **404**; callers without API permission receive **403**. Explorer shows those errors and does not treat them as a successful copy. |
| Move folder | Public REST `POST /rest/folders/move/folder` with a `MoveFolderItem` root (`itemPath` + `targetFolderPath`). Pathmanagement `POST …/path/moveItem` is no longer the Explorer default (slice 8 / #4601). Finder `/Assets` and `/Sites` paths are mapped to repository `//` form so the move does not fail with "Path must start with '//'". |
| Move item (page/file/asset) | Public REST `POST /rest/folders/move/item` with the same `MoveFolderItem` root. Not `move/folder` (that endpoint is for folders). Missing source or destination is HTTP **404**; non-admin API callers receive **403**; destination conflict (locked, name in use) is **409**. Explorer shows those errors and does not treat them as a successful move. The Explorer destination picker writes the move into the destination detail-list and tree without a manual **Refresh**. |

Documented for integrators on [Public REST](id:developer-rest). Production and operator
day-to-day Explorer stay on pathmanagement (flag **off**).

## Server actions (how they run)

The **Server actions** toolbar and the item **context menu** use the same catalog
(`GET /services/actions/find`, plus type and **template** menus for the selected item).

| Action | What happens |
|--------|----------------|
| **Preview** (and per-template children) | Select a **page** or **asset** row first (folder rows stay Preview-disabled). Listed pages with a content id open Page Management render (`GET /services/pagemanagement/render/page/{id}`). Pages under **Sites** with no id open the assembled Finder site-path preview (`/Sites/…?percmobilepreview=`, path segments percent-encoded). That site-path (and Page Management render) **assembles** the item: FastForward sample pages such as **Corporate Investments Home** (`rffHome`, content id 551) use the site’s default page template, not the CM1 `percPage` dispatcher. The popup is assembled HTML (HTTP 200), not a JAX-RS `PSErrors` / “No message body writer” failure, not a null-pointer error body, and not the React Content Editor (`spa.jsp?entry=editor` / `/cm/app/editor`). If assembly still fails, the product error page (`ui/assembly/error.jsp`) or an HTML error document renders instead of a blank 500. Assets open the asset view URL. Per-template children still use assembly preview (`GET /services/assembly/preview-location`). Applies to listed pages and assets, including customer-defined content types (type names are not a closed list; FastForward names stay stable). New language: **template**, not variant. |
| **New Item** | Select a **site or folder** first, then choose **New Item**. When the catalog lists types under **New**, pick a type from that menu. When **New Item** is a single action (no type children), Explorer opens **Choose a content type** — pick a type and **OK**, or **Cancel** to leave the folder unchanged. Explorer then creates the item in the current folder (`POST /services/itemmanagement/item/create`) and opens the React Content Editor. It does **not** show *Choose a content type from New Item* as an error toast instead of the picker, and it does not open leftover Content Editor HTML. **Pages** (`percPage`) need a page template. Explorer loads allowed templates for the type, then the site's templates when the type has none. One template is used automatically; more than one opens **Choose a page template**. Cancel leaves the folder unchanged. If no template is available, Explorer asks you to pick a site folder or use Home → Create. Home → Create **Asset** uses the same create + React editor host (not leftover `editAsset.jsp`). |
| **Workflow** | Select a content item, then use the toolbar or context **Workflow** menu. Allowed transitions run through itemmanagement (`GET …/workflow/getTransitions/{id}` then `GET …/workflow/transitionWithComments/{id}/{trigger}`, not `wfactionset.html`). Clicking a listed trigger — including **Expire** when it is listed — returns HTTP 200 and does not show an error toast. Triggers the server marks as comment-required ask for a comment first; a blank comment is not sent and is shown as an error. HTTP **403** (transition not allowed) and **409** (comment required, or the item state rejects the transition) are errors in the server-actions region — they are not treated as success and the list does not refresh. **Multi-select:** check two or more rows. The toolbar **Workflow** group lists only triggers every selected page or asset allows (the intersection of `getTransitions`). Triggers that only some items allow are hidden. **Folders** are not workflow items and do not add or remove triggers. One confirm (**Apply {trigger} to N selected items…**) runs `transitionWithComments` for each selected page or asset. **Cancel** changes nothing. Folders in the selection are named in the **Server actions** error region (**Folders are not transitioned**). A **403** or **409** on one item does not report the whole selection as transitioned — the error region names that item (for example **About (HTTP 403)**) and the list refreshes only for items that did transition. A comment-required trigger asks once; that comment is sent with every item. A blank comment transitions nothing. |
| **Purge** | Confirm, then permanently purge a **page** or **asset** (`pagemanagement` / `assetmanagement` purge). Other types stay unavailable. Distinct from **Delete** (remove from folder / recycle). |
| **Edit / Quick Edit / View content** | Select a **page** or **asset** row first. Opens a new Content Editor window (`spa.jsp?entry=editor`) that checkouts the item (Edit) and shows content-type fields. From that host, **New item** creates a content item of the selected type in the folder path you enter (`POST /services/itemmanagement/item/create`) and then opens the new item. HTTP **403** / **400** / **404** are errors, not success. Opening the editor with no `contentId` shows the same create form (plus Home → Create). Keyword drop-downs coerce catalog choice values (including JSON numbers) so the field form stays on screen instead of flashing to a blank page. If checkout or load fails (for example a workflow lock), the editor host shows an error — not a white screen. Explorer **Open** on that same selected row lands the same React editor host (HTTP 200). **Folders** stay non-editable: Edit / Quick Edit / View content stay hidden, and Open on a folder browses into it instead of opening the editor. Text, rich text (TinyMCE HTML body), keyword, and community controls save through `PUT /services/itemmanagement/item/fields/{id}`. HTML that contains script tags, inline event handlers, or `javascript:` URLs is blocked on the client; HTTP **400** from that PUT is mapped onto the HTML field (not treated as success). **View content** keeps the HTML widget read-only. File and image controls upload through `PUT /services/itemmanagement/item/binary/{id}/{field}`. In **Edit**, the host also lists **allowed workflow transitions** for the open item (`GET /services/itemmanagement/workflow/getTransitions/{id}`) and runs a chosen trigger with an optional comment (`GET …/workflow/transitionWithComments/{id}/{trigger}?comment=`). Triggers that are not in that allowlist do not run. Reject-style transitions (for example **Reject**) require a comment before the request is sent. **View content** stays read-only (no save, check-in, or workflow buttons). With an item open, the same host lists translation locales (`GET /rest/content-explorer/translations/{contentId}`). **Open locale copy** switches the editor to that variant. Creating one target locale (`POST /rest/content-explorer/translations`) opens the new copy. HTTP **403** (not allowed) and **409** (locale already exists) stay in the translations panel — they do not blank the editor host. Does not open leftover Content Editor HTML (`?view=editor`, `editAsset.jsp`, `rx_ce`, `checkoutedit.xml`). |
| **Translate** | Opens the Explorer **Translations** panel for the **selected page or asset** (this item’s locale, related variants, and create-variant). List row ids may be GUID-shaped (`16777215-101-551`); the panel requests variants with that full GUID (`GET /rest/content-explorer/translations/{itemId}`) and uses the content-id segment only for create-variant. Folders and sites have no content id — Explorer shows a select-item hint. Does not open the legacy translate XSL wizard. |
| **Impact Analysis** | Opens the Explorer **Dependencies** panel for the **selected page or asset**. List row ids may be GUID-shaped (`1-101-708`); the viewer uses the content-id segment. Folders and sites have no content id — Explorer shows a select-item hint. |
| **Copy URL to Clipboard** | Copies the site-path preview URL (or CMS path) for the selected item. |
| **Revisions** | Opens the Revisions panel; restore is available when the selected revision is restorable. Pick two revision numbers and **Compare** to see a field-level text compare (`GET /services/itemmanagement/item/compare/{id}/{rev1}/{rev2}`). HTTP **404** (unknown item or revision) and **403** (no read assignment) are errors in the panel — they are not treated as success or as an empty compare. Explorer does **not** open leftover Data Flow `sys_Compare/compare.html`. **Promote revision** opens the same chrome-less editor host (`mode=promote`) and restores the chosen revision through `GET /services/itemmanagement/item/restoreRevision/{revisionGuid}`. |
| **Audit Trail** | Same Revisions panel, audit-trail tab. |
| **New Copy** | Confirm, then create a copy in the current folder. |
| **Promotable Version** | Confirm, then create a promotable version in the current folder. |
| **Restore Prior Revision** (inside the editor host) | Open the open item in the React editor (`spa.jsp?entry=editor`) and click **Show revisions** in the header. Pick a revision and **Restore prior revision** to confirm; the host calls `GET /services/itemmanagement/item/restoreRevision/{revisionGuid}` and refreshes fields from `GET /services/itemmanagement/item/fields/{id}`. HTTP **403** (not allowed to restore) and **404** (item or revision not found) are errors in the panel — they are not treated as success. In the same panel, pick **From revision** and **To revision** and **Compare revisions** to see a field-level compare (`GET /services/itemmanagement/item/compare/{id}/{rev1}/{rev2}`). The compare view does not edit fields and does not restore. Choosing the same revision, or opening an item with fewer than two revisions, does not show a diff. An empty field list is shown as no differences. HTTP **403** and **404** on compare, and a failure to load revision history, are errors in the panel. Edit mode only. View / Promote modes hide the toggle. |
| **Flush Cache** (Refresh Item) | Confirms, then flushes **all** assembler pages (not only the selected item). |
| **Nav Reset** | Same goal as classic Nav Reset. On 8.2 this is typically a no-op once managed navigation is loaded (FastForward 6.0+ variants unused). |
| **Publish Now** | Select a **page** or **asset** row in the list first (clicking only **Sites** or another folder is not enough). The toolbar and the item context menu hide Publish Now until a page or asset is selected. Explorer then confirms and demand-publishes (`GET /services/sitemanage/publish/page/{id}` or `/resource/{id}`). Other types stay unavailable. Does not open the demand-publish servlet page. HTTP 200 with application-level `FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` is a failure (same as classic Finder) — the **Server actions** error region shows the server warning (for example licensing / Publication stopped) and the list does not refresh as if published. Folder-only selection does not publish. **Multi-select:** check two or more rows, then **Publish Now**. One confirm (**Publish N selected items now…**) demand-publishes each selected page and asset. **Cancel** publishes nothing. Folders in the selection are not published; the **Server actions** error region names them (**Folders are not published**). A **403**, **404**, or **409** (or the same application-level failure) on one item does not report the whole selection as published — the error region names the failed item and the list refreshes only for items that did publish. |
| **Take Down** | Select a **page** or **asset** row first (same kind rules as **Publish Now**). Folders, templates, and other non-publishable types stay hidden. Explorer confirms **Take down (unpublish) this item from its site?** If other pages link to the item (`GET /services/itemmanagement/item/findLinkedItems/{id}`), the confirm lists those paths (up to ten). Confirm then takes the item down (`GET /services/sitemanage/publish/takedown/page/{id}` or `/resource/{id}`; `PUT` of the linked-page list when that list is non-empty). A failed linked-item lookup still proceeds with the confirm (classic Finder). HTTP 200 with application-level `FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` is a failure — the **Server actions** error region shows the server warning and the list does not refresh as if unpublished. Does not open leftover demand-publish servlet pages. Schedule dates are a separate Explorer action. **Multi-select:** check two or more rows, then **Take Down**. One confirm (**Take down N selected items…**) unpublishes each selected page and asset, using the same linked-page lookup and GET/PUT per item. **Cancel** takes down nothing. Folders in the selection are not taken down; the **Server actions** error region names them (**Folders are not taken down**). A **403**, **404**, or **409** (or the same application-level failure) on one item does not report the whole selection as taken down — the error region names the failed item and the list refreshes only for items that were unpublished. |
| **Stage** | Select a **page** or **asset** row first (same kind rules as **Publish Now** / **Take Down**). Folders, templates, and other non-publishable types stay hidden for a single row. Explorer confirms **Stage this item to the staging server?** Confirm then stages the item (`GET /services/sitemanage/publish/page/staging/{id}` or `/resource/staging/{id}` — same URLs as classic Finder `publishToStaging`). HTTP 200 with application-level `FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` is a failure — the **Server actions** error region shows the server warning (for example no staging servers configured) and the list does not refresh as if staged. **Multi-select:** check two or more rows, then **Stage**. One confirm (**Stage N selected items…**) stages each selected page and asset. **Cancel** stages nothing. Folders in the selection are not staged; the **Server actions** error region names them (**Folders are not staged**). A **403**, **404**, or **409** (or the same application-level failure) on one item does not report the whole selection as staged — the error region names the failed item and the list refreshes only for items that did stage. |
| **Remove from Staging** | Select a **page** or **asset** row first (same kind rules as **Stage**). Folders stay hidden for a single row. Explorer confirms **Remove this item from staging?** Confirm then removes the item from staging (`GET /services/sitemanage/publish/takedown/page/staging/{id}` or `/takedown/resource/staging/{id}` — same URLs as classic Finder `removeFromStaging`). HTTP 200 with application-level `FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` is a failure — the **Server actions** error region shows the server warning and the list does not refresh as if unstaged. **Multi-select:** check two or more rows, then **Remove from Staging**. One confirm (**Remove N selected items…**) removes each selected page and asset from staging. **Cancel** changes nothing. Folders in the selection are not removed; the **Server actions** error region names them (**Folders are not removed from staging**). A **403**, **404**, or **409** (or the same application-level failure) on one item does not report the whole selection as removed — the error region names the failed item and the list refreshes only for items that were removed. |
| **Schedule** | Select a **page** or **asset** row first (same kind rules as **Publish Now** / **Take Down**). Folders, templates, and other non-publishable types stay hidden. Explorer loads the current dates (`GET /services/itemmanagement/item/getitemdates/{id}`) and opens **Schedule**. Set or **Clear dates** for publish (start) and removal (end), optional comments (500 characters), then confirm **Save schedule publish dates for this item?** Confirm posts `{ ItemDates: { itemId, startDate, endDate, comments } }` to `POST /services/itemmanagement/item/setitemdates` (classic Finder `PercScheduleDialog`). Empty dates clear the schedule. Publish and removal cannot be the same instant, and removal must be after publish. HTTP 200 with application-level `FORBIDDEN`, `BADCONFIG`, or `INVALID` is a failure — the **Server actions** error region shows the server warning and the list does not refresh as if saved. **Multi-select:** check two or more rows, then **Schedule** once. One dialog and one confirm write the same start, end, and comments to every selected page or asset. Folders and other non-publishable rows are skipped and are not a failure. Cancel on the dialog or the confirm writes nothing. If some rows save and others return `FORBIDDEN`, `BADCONFIG`, or `INVALID`, the **Server actions** error region lists the failures — that is not full success. Rows that did save still refresh the list. A single highlighted row (fewer than two checkboxes) still schedules only that row. Does not open leftover jQuery `PercScheduleDialog`. |
| **Publishing History** | Select a **page** or **asset** row first (same kind rules as **Publish Now**). Folders, templates, and other non-publishable types stay hidden. Explorer opens **Publishing History** and loads `GET /services/itemmanagement/item/pubhistory/{id}` (the same item-management API as **Publish → Status / Logs**). Rows show server, location, revision, date, operation, and status (newest first). An item that has never been published shows an empty-history message. HTTP **404** (unknown id) and **403** (forbidden) are errors in the history dialog — they are not treated as success or as empty history. Does not open leftover jQuery `PercPublishingHistoryDialog`. See [Publishing](id:admin-publishing) for the Publish-shell panel and deep links. |
| **Check Out** | Select a **page** or **asset** row first (same kind rules as **Publish Now**). Folders stay hidden — the action is not applicable. Explorer then checks the item out (`GET /services/itemmanagement/workflow/checkOut/{id}`). Success refreshes the folder list. HTTP **403** (not allowed) and **409** (checked out to another user) are errors in the **Server actions** region — they are not silent success. Does not require opening the Content Editor. **Multi-select:** check two or more rows, then **Check Out**. One confirm (**Check out N selected items…**) checks out each selected page and asset. **Cancel** checks out nothing. Folders in the selection are not checked out; the **Server actions** error region names them (**Folders are not checked out**). A **403** or **409** on one item does not fail the rest of the selection silently — the error region names that item (for example **About (HTTP 409)**) and the list refreshes only for items that did check out. |
| **Check In** | Select a **page** or **asset** row first. Folders stay hidden. Explorer then checks the item in (`GET /services/itemmanagement/workflow/checkIn/{id}`). Success refreshes the folder list. HTTP **403** (not allowed) and **409** (not checked out to you) are errors in the **Server actions** region. Distinct from **Force Check-in** (Admin only). Does not require opening the Content Editor. **Multi-select:** check two or more rows, then **Check In**. One confirm (**Check in N selected items…**) checks in each selected page and asset. **Cancel** checks in nothing. Folders in the selection are not checked in; the **Server actions** error region names them (**Folders are not checked in**). A **403** or **409** on one item does not report the whole selection as checked in — the error region names that item (for example **About (HTTP 409)**) and the list refreshes only for items that did check in. |
| **Force Check-in** | **Admin** sessions only. Select a **page** or **asset** that is checked out (including by another user). The toolbar and item context menu inject **Force Check-in**. Confirm **Force check-in this item? Other users will lose their checkout.** Confirm then calls `GET /services/itemmanagement/workflow/forceCheckIn/{id}` (not the editor check-in used after a save). Success refreshes the folder list. HTTP **403** (not Admin assignment), **404** (unknown id), and **409** (item is not checked out) are errors in the Server actions region — they are not silent success. Non-Admin sessions do not see the action. Does not require opening the Content Editor. **Multi-select:** check two or more rows, then **Force Check-in**. One confirm (**Force check-in N selected items…**) force-checks-in each selected page and asset. **Cancel** force-checks-in nothing. Folders in the selection are not force checked in; the **Server actions** error region names them (**Folders are not force checked in**). A **403**, **404**, or **409** on one item does not report the whole selection as force checked in — the error region names that item (for example **About (HTTP 409)**) and the list refreshes only for items that did force check in. |
| **Active Assembly** | Opens a new window that assembles the selected item with its **page** or **snippet template** (`GET /services/assembly/preview-location` in an iframe). A light overlay shows the content id and template. Known **scalar text** fields on the assembled output become contenteditable (classic `PsAaField` wrappers, `data-perc-field` markers, or a unique assembled text value). **Save fields** writes through `PUT /services/itemmanagement/item/fields/{id}` — the same API as the React Content Editor. If the assembled page has no mappable nodes, the overlay lists those fields so you can still edit them. Rich text, file, image, keyword, and community stay on the Content Editor. If the requested template is missing from the item's available list, Explorer shows that mismatch instead of silently using another template. A failed template catalog load is an error, not a hidden preview retry. Does not open leftover Active Assembly HTML (`variantlistwithslots.html` / `itemassembly.html`) or leftover Content Editor HTML. |
| **Slot add / create / arrange** | Available when a **slot** is selected on the Active Assembly canvas (`GET /services/assembly/slot-relationships/canvas`). **Add** (AA **Add**, or Explorer **Slot Add** when that slot is selected) opens Content Browser. Confirming a page or asset adds the snippet through `POST /services/assembly/slot-relationships` (allowed snippet template from the slot). Cancel leaves the slot unchanged and does not post. Folder browse without a selected slot stays **Select a slot in Active Assembly first** — Explorer does not open leftover `variantlistwithslots.html` / `itemassembly.html`. **Create** (AA **Create**, or Explorer **Slot Create** when that slot is selected) opens a type / snippet-template / folder picker from the slot's allowed lists. Apply creates the item (`POST /services/itemmanagement/item/create`) and opens the React Content Editor (`spa.jsp?entry=editor`) — not leftover `editAsset.jsp` or Data Flow create HTML — then adds the new item through `POST /services/assembly/slot-relationships`. Cancel closes the picker and does not create or add. **Arrange** (AA **Move up** / **Move down** / **Change template** / **Remove**, or the matching Explorer **Arrange** / **Change Template** / **Move to Slot** actions) is available only when a **snippet** is selected in that slot. Move reorders through `POST /services/assembly/slot-relationships/{id}/move`. Remove confirms, then deletes the relationship (`DELETE /services/assembly/slot-relationships/{id}`). Change template opens a slot + snippet-template picker; Apply writes `POST /services/assembly/slot-relationships/{id}/template-slot`. Cancel leaves the relationship unchanged. Selecting only a slot (no snippet) shows **Select an item in the slot first**. Folder browse has no slot — Explorer does not invent Arrange actions from a folder and does not open leftover `itemassembly.html` / `variantlistwithslots.html`. Inline field edits use itemmanagement, not leftover Content Editor HTML. |

Do not bookmark or paste `../sys_cxSupport/…html` URLs from older Desktop Content Explorer
menus — they are not Explorer pages.

## Leftover Content Editor URLs (retired)

The React Content Editor is the only product editor (`spa.jsp?entry=editor` /
`/editor`). These leftover CM1 / Data Flow **UI** requestors are retired:

| Retired bookmark | What happens now |
|------------------|------------------|
| `editAsset.jsp` | Permanent redirect to the React editor host |
| `?view=editor` (including login return / `perc_linkback_id`) | Redirects to `spa.jsp?entry=editor` (with `contentId` when known) |
| `sys_action/checkoutedit.xml`, `sys_cxSupport/contenteditorurls.html` | Not used by Home, Explorer, TopNav, or login return |

The **Data Flow Server** remains platform I/O (lookups, stored actions, content-type
system definition). Do not treat those XML applications as a product editor UI. Desktop
Content Explorer is not an 8.2 client.

## Content Editor controls

The chrome-less React Content Editor (`spa.jsp?entry=editor` / `/editor`) maps
content-type **Control** names from `GET /services/contenttypes/{type}` onto widgets:

| Control / field | Widget | How it is saved |
|-----------------|--------|-----------------|
| `sys_EditBox` and other short text (`dataType` `text`) | Text input | `PUT /services/itemmanagement/item/fields/{id}` |
| `sys_Table` (not a related-content table) | Editable grid: **Add row** / **Remove row**, one input per cell | Same fields API. An empty default grid saves as a blank string and does not drop other fields. A grid with columns or cell text saves as JSON `{"columns":["…"],"rows":[["…"]]}`. A value that is not that JSON stays one cell so older text is not discarded. **View** mode makes every cell read-only, hides Add row and Remove row, and hides Save. |
| `sys_TextArea`, or `sys_EditBox` when `dataType` is `maxtext` | Multi-line text | Same fields API. Line breaks are kept. A value that contains a NUL character is rejected before save and shown on that row. HTTP **400** from the fields PUT is mapped onto the long-text field (when the error names it, or when it is the only long-text field). **View** mode makes the box read-only and hides Save. |
| `sys_Number`, or `dataType` `integer`, `number`, or `float` | Number input | Same fields API. A valid number is stored. Blank clears the field. Non-numeric text is rejected before save and shown on that row. Inclusive `minimum` / `maximum` control properties (also `min` / `max`) reject an out-of-range value the same way. An unbounded integer outside the signed 64-bit range is out of range. The PUT includes `dataType` plus any bounds; the server returns HTTP **400** and does not save. HTTP **400** is mapped onto the number field (when the error names it, or when it is the only number field). **View** mode makes the input read-only and hides Save. |
| Page link / managed link (`sys_PageLink`, `sys_ManagedLink`, `sys_link`, `sys_url`, `rxhyperlink`, or `dataType` `link`) | Link input with **Clear link** | Same fields API. The PUT sends `dataType` `link`. Blank clears the field and does not look up a target. A value must be a content id, a hyphenated content GUID, or a site folder path (`//Sites/...` or `/Sites/...`). Anything else (including `javascript:` and `http` URLs) is rejected before save and shown on that row. The server returns HTTP **400** for a bad shape, **404** when the target item does not exist, and **403** when the caller may not use that target. Those statuses are not a successful save. This control does not insert, remove, or reorder slot relationships. **View** mode makes the input read-only, hides Clear link, and hides Save. |
| `sys_tinymce`, `sys_TinyMCE`, `sys_EditLive`, HTML | TinyMCE (textarea fallback if the shipped TinyMCE script is unavailable) | Same fields API — HTML string |
| Keyword-named dropdowns | Keyword picker | Same fields API — selected choice value |
| `sys_communityid` | Community picker | Same fields API. The list is `GET /services/communities/find`. Saving writes the selected community id; reopening the item shows that id. **View** mode, a checkout held by someone else, and a read-only community control disable the picker and hide Save. |
| `sys_File` / file asset fields | File upload | `PUT /services/itemmanagement/item/binary/{id}/{field}` (multipart `file`). The item must be checked out to you. **View** mode disables the file input. HTTP **403** (not allowed / checked out to someone else), **400** (missing or invalid file), and **413** (file too large, 50 MiB) are errors — they are not treated as a successful save. |
| Image controls (`sys_webImageFX`, `img`, names containing `image`) | Image upload with local preview | Same binary PUT. The payload must be an image (`image/*` MIME or a known image extension). Non-image files return HTTP **400**. **View** mode disables the file input. HTTP **403** / **400** / **413** are errors — they are not treated as a successful save. |
| `sys_CalendarSimple` and other date controls | Date picker (`type=date`) | Same fields API — `yyyy-MM-dd` |
| Datetime / timestamp controls (`dataType` datetime) | Date-time picker (`type=datetime-local`) | Same fields API — `yyyy-MM-dd HH:mm:ss` |

Date and datetime widgets are **not** free-text boxes. Empty optional dates save as
blank. A value the host cannot parse is blocked before PUT and shown on that row
(`Enter a valid date.`). HTTP **400** from
`PUT /services/itemmanagement/item/fields/{id}` for an invalid date is mapped onto
the named field. In **View** mode the pickers are read-only (disabled); Save is hidden.

Save stays on itemmanagement (`PUT /services/itemmanagement/item/fields/{id}`).
The GET/PUT field payload includes a **revision** (CMS tip). Saving from the
React Content Editor sends that revision. If the item was saved again in the
meantime, the PUT returns HTTP **409** and the host shows **This item was
saved with a newer revision. Reload and try again.** — 409 is not treated as
success. Validation errors (empty required fields, HTTP **400**) stay on the
form as before. Active Assembly field saves that omit `revision` (or send `0`)
are not stale-checked.
**Check Out** and **Check In** on the editor host call public REST
`POST /rest/editor/items/{id}/checkout` and `POST /rest/editor/items/{id}/checkin`.
**Check In** asks for an optional revision comment first. **Cancel** leaves the
item checked out. Leave the comment blank to check in with no comment, or enter
one to send `?comment=` on that POST.
When the item is **not** checked out to you, the host is **view-only** (fields
read-only; Save and Check In hidden). Use **Check Out** to take the lock. HTTP
**403** (not allowed) and **409** (checked out to someone else) are shown as
errors — they are not treated as success. Use **Show revisions** to open the
restore-prior-revision panel: the host lists revisions plus workflow comments
(`GET /services/itemmanagement/item/revisions/{id}`). Pick a revision and
**Restore prior revision** to call
`GET /services/itemmanagement/item/restoreRevision/{revisionGuid}`; the editor
**reloads fields** from `GET /services/itemmanagement/item/fields/{id}` after
a successful restore. HTTP **403** (not allowed to restore) and **404** (item
or revision not found) are errors in the panel — they are not treated as
success. In that same panel, **Compare revisions** reads
`GET /services/itemmanagement/item/compare/{id}/{rev1}/{rev2}` and lists each
field’s older value, newer value, and whether it changed. It does not edit
fields and does not restore a revision. The same revision, or a history with
fewer than two revisions, does not show a diff. A compare with no field rows
says the revisions have no field differences. HTTP **403** and **404** on
compare are errors, as is a failure to load the revision list. The restore
panel is shown in **Edit** mode only.  **Required** fields (content-type
`required` flag or occurrence `required` / `oneOrMore` from
`GET /services/contenttypes/{type}`) show a marker on the field row. Saving with
an empty required field keeps the form on screen and shows an inline error on
that row — the host does not PUT a partial save. HTTP **400** from
`PUT /services/itemmanagement/item/fields/{id}` is mapped onto the named field
when the error body includes that field (`errorData`, `field`, `fieldName`, or a
quoted name in the message). **Check In** is blocked while required fields are
still empty. In **Edit** mode the host also shows a
**Workflow** region: the current state name and buttons for each allowed transition
from `GET /services/itemmanagement/workflow/getTransitions/{id}`. Enter an optional
**Transition comment**, then choose a trigger. The host calls
`GET /services/itemmanagement/workflow/transitionWithComments/{id}/{trigger}`
(with `?comment=` when the comment is non-empty). Triggers that are not in the
loaded allowlist are not shown and cannot run. **Reject**, **Return**, **Disapprove**,
**Decline**, and **Send Back** require a comment — the host blocks the request until
the comment field is filled. **View** mode does not load or run transitions.

In **Edit** mode the host also shows **Publish now** for the already-open **page** or
**asset** so authors do not need to bounce to Explorer solely to publish. Confirm
**Publish this item now?** then demand-publish (`GET /services/sitemanage/publish/page/{id}`
or `/resource/{id}` — same URLs as Explorer **Publish Now**). HTTP 200 with
application-level `FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` is a
failure: the host shows the server warning and does not treat the job as started.
**View** and **Promote** stay read-only (no Publish now). Templates and other
non-page/non-asset types stay unavailable. Does not open the demand-publish servlet.

The host also lists **Related content** for the open item. Slot relationships
come from `GET /services/assembly/slot-relationships/canvas?ownerId=` and inline
/ local dependents from
`GET /Rhythmyx/rest/content-explorer/relationships/{itemId}/local`. An empty
list is shown as **No related content for this item.** HTTP **403** on the list
is an explicit **not allowed** message — not a blank panel.

In **Edit** mode, when the canvas has at least one slot, the host shows
**Insert existing item**. Choose the slot, enter the id of an item that already
exists, and insert. The host calls
`POST /services/assembly/slot-relationships` with the owner, dependent, slot,
and the snippet template from the canvas (or from an item already in that slot).
HTTP **400** (bad id or slot), **403** (not allowed), and **404** (item or slot
not found) stay on the panel as errors — they are not treated as a successful
insert and the list is not cleared. **View** and **Promote** do not show the
insert form.

In **Edit** mode each slot row that has an Active Assembly relationship id shows
**Remove**. The host calls
`DELETE /services/assembly/slot-relationships/{relationshipId}` and reloads the
list. HTTP **403** (not allowed) and **404** (relationship not found) stay on
the panel — the row is not dropped and the call is not treated as success.
Inline local links that have no relationship id are listed but have no Remove
control. **View** and **Promote** do not show Remove.

In **Edit** mode, when a slot has two or more Active Assembly relationships,
each row that is not already first shows **Move up** and each row that is not
already last shows **Move down**. The host calls
`POST /services/assembly/slot-relationships/{relationshipId}/move` with
`direction` `UP` or `DOWN`, then reloads the canvas. The new order is the order
returned by that reload. HTTP **403** (not allowed), **404** (relationship not
found), and **409** (the item is not checked out to you) stay on the panel —
the list is not reordered and the call is not treated as success. A single
item in a slot has no move controls. Inline local links are not reordered.
**View** and **Promote** do not show Move up or Move down. This does not
insert or remove associations and does not save numeric or link fields.

Each related row that has a content id shows **Open**. That opens
`spa.jsp?entry=editor` for that id in a reserved editor window (the same
editor window helper as other editor launches). **Edit** mode opens the
related item in edit. **View** and **Promote** open it in view, not edit.
A row with no content id has no **Open** control and cannot be opened.
Open does not insert, remove, or reorder the association.

**Preview** is available in **View** and **Edit** for the already-open **page**
or **asset** so authors do not need to bounce to Explorer solely to preview.
It uses the same assembled preview as Explorer **Preview**: pages open Page
Management render (`GET /services/pagemanagement/render/page/{id}`); assets
open the asset view URL (`GET /services/assetmanagement/asset/assetViewUrl/{id}`)
and then that URL. Preview is always the **last saved revision**. Unsaved field
edits are not assembled — if the form is dirty, the host confirms **Preview the
last saved revision? Unsaved edits are not included.** Cancel leaves the editor
unchanged and does not open a window. HTTP **403** (forbidden) and **404**
(unknown id) are failures: the host shows the error and does not treat preview
as opened. **Promote** does not show Preview. Does not open leftover Content
Editor HTML or an Active Assembly overlay.

The same host shows a **Preview template** list when Preview is available.
**Current template** is the default and keeps the assembled preview above
(Page Management render for a page). Choosing another allowed template
reloads that preview from `GET /services/assembly/preview-location`
(`contentId` and `templateId`) into the preview frame. That choice does
**not** change the saved page template (the separate page-template control
still does that on save). A location that is not an assembler render URL is
an error on the panel. Unsaved edits are still not assembled.

In **Edit** mode the host also shows **New copy** and **Promotable version** for the
already-open item (same REST as Explorer **New Copy** / **Promotable Version**).
Confirm, then `POST /services/itemmanagement/item/newCopy/{id}` or
`…/promotableVersion/{id}`. After a successful `ItemCopyResult` the host opens the
new `itemId` in this editor (edit mode). HTTP **403** (forbidden) and **404**
(unknown id) are errors on the host — they are not treated as success and do not
change the open item. Cancel confirm does not POST. **View** and **Promote** do
not show these actions.

In **Edit** mode the host also shows **Copy to folder** for the already-open item.
This is not **New copy** (same-folder itemmanagement `newCopy`). The author picks
a destination folder. Cancel closes the picker and does not create a copy.
Confirm posts public REST `POST /rest/folders/copy/item` (`CopyFolderItemRequest`:
the item's current path and `targetFolderPath`). The copy is created in the
chosen folder. The public response is a status, not a new content id, so the
host stays on the **original** item and shows that the copy landed in that
folder. HTTP **400** (blank or invalid destination), **403** (not allowed), and
**404** (item or destination missing) are errors on the host — they are not
treated as success. **View** and **Promote** do not show **Copy to folder**.

In **Edit** mode the host also shows **Rename** and a **Listing name** field for
the already-open item. The name starts as the item's `sys_title`. Submit posts
public REST `POST /rest/folders/rename/item` (`RenameFolderItemRequest`: the
item's current path and `newName`). The item stays in the same folder. After a
successful rename the host reloads the item and shows the new listing name.
A blank name, a name that contains `/`, HTTP **400** (invalid request), **403**
(not allowed), and **404** (item not found) are errors on the host — they are
not treated as success and the previous name stays. **View** and **Promote** do
not show Rename. This does not move the item or change folder membership.

In **Edit** mode the host also shows **Move to folder** for the already-open
item. The author picks a destination folder (cancel closes the picker and does
not move). Confirm posts public REST `POST /rest/folders/move/item`
(`MoveFolderItem`: the item's current path and `targetFolderPath`). The editor
stays on the **same content id**. The item is listed in the destination folder
and is no longer in the source folder. Choosing the folder the item is already
in does not post. HTTP **403** (not allowed), **404** (item or destination
missing), and **409** (name already in the destination, or the target is not a
folder) are errors on the host — they are not treated as a successful move.
**View** and **Promote** do not show **Move to folder**. This is not a folder-node
move, recycle, or copy.

In **Edit** mode the host also shows **Recycle** for the already-open **item**
(not a folder). Confirm, then the host looks up the item
(`GET /services/pathmanagement/path/item/id/{id}`) and recycles that path with
`DELETE /rest/folders/item/{path}` — the same public REST call Explorer uses to
recycle a page, file, or asset. After success the host **leaves edit mode** and
no longer shows that content id as an open edit. Cancel does not call delete.
A folder path is refused and is not deleted. HTTP **403** (not allowed),
**404** (item or path not found), and **409** (in use or checked out) stay on
the host as errors — they are not treated as success and the item stays open.
**View** and **Promote** do not show Recycle. This is not Explorer **Empty
recycle bin** or a permanent purge.

The host does not request leftover Content Editor HTML (`checkoutedit.xml`,
`contenteditorurls.html`, `?view=editor`).

## Sites list and Create Site

Under the tree root **Sites** you see traditional site folders available to your community
(sample sites after a demo-sites install, plus any sites you create). The tree label and
the list **Name** column show the **site name** (for example `Corporate_Investments`
or `Corporate Investments`), not an internal identifier. Expand **Sites** and
select a sample site to browse FastForward folders and pages (About…, Files, Images,
and the site NavTree).

To create a new Site from Explorer:

1. Choose **Content → Create Site** (available without selecting an existing site).
2. On **Site type**, choose **Traditional**, **Page**, or **Virtual**.
3. Complete the remaining steps (name/description; managed navigation only for Traditional/Page;
   page template only for Page; optional Git root on Virtual confirm).
4. On success, Explorer navigates to `/Sites/<new-site-name>`.
   HTTP 400 (invalid name / existing NavTree) and HTTP 403 (no permission) stay on the wizard with mapped error chrome.

**Virtual** create does not show managed navigation or a page template. If you supply a Git
root path, the wizard PUTs the existing `VirtualSiteProperties` envelope after create.
Otherwise finish source settings on **Developer → Sites**. See
[Sites & content structure](id:admin-sites) and [Virtual Sites](id:developer-virtual-sites).

Related Content menu commands:

- **Create Site** — new Traditional, Page, or Virtual Site (no site context required)
- **Site Copy** / **Subfolder Copy** — copy workflows when a site or folder is in context

### Site Copy

**Content → Site Copy** opens when a site folder is in context (you are inside a site
under **Sites**). It does not create an empty site (**Create Site** does that) and it
does not copy a single subfolder (**Subfolder Copy** does that).

| Step | What you enter |
|------|----------------|
| Source site | Prefilled from the open site. You can change the name before continuing |
| Target site | New site name. An optional asset-folder path is sent only when it is not blank or `/` |
| Options | Workflow and template filters (use `*` to copy all) |
| Confirm | Review source, target, workflows, and templates |
| Run | Posts `POST /services/sitemanage/site/copy` with a `SiteCopyRequest` body (`srcSite`, `copySite`, optional `assetFolder`) |

| Result | What Explorer shows |
|--------|---------------------|
| HTTP 200 | **Site copy completed** and the new site name. The folder list refreshes |
| HTTP 400 | The request was rejected (missing source, unknown source site, or an invalid target name). The wizard stays on the progress step; this is not success |
| HTTP 403 | You are not allowed to copy a site |
| HTTP 409 | The target name already exists, or another site copy is already in progress |

Cancel resets the wizard. A copy that is still running blocks a second copy of the same server until it finishes.
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

**Submit** copies the selected folder subtree into the destination
(`POST /Rhythmyx/rest/folders/copy/folder`). It does not copy a whole Site and
it does not delete the source folder. On success, Explorer opens the
destination so the copied folder is listed there. Failures stay on the wizard:

| HTTP | Meaning |
|------|---------|
| 400 | Source or destination path is missing |
| 403 | You do not have permission to copy the folder |
| 404 | Source or destination folder was not found. The source folder is not deleted |
| 409 | Destination conflict (for example, copying a folder into itself). The source folder is not deleted |

### Copy selected pages and assets to one folder

**Content → Copy selected to folder** is enabled after you check one or more rows
in the detail list. One confirm uses the same destination path field as reduced-actions
**Copy**. Each checked **page, file, or asset** is copied with
`POST /rest/folders/copy/item` (`CopyFolderItemRequest`). Checked **folders** are
not copied; Explorer names them in the result (for example `Skipped folders: News`).

Copies that already succeeded stay in the destination when a later item fails.
The result is full success only when every page or asset copied. A partial HTTP
failure (for example **404** on one item) stays on the result as an alert and is
not reported as full success. A selection that is only folders copies nothing.
This is not **Move**, not **Site Copy**, not **Subfolder Copy**, and not clipboard paste.

### Move selected pages and assets to one folder

**Content → Move selected to folder** is enabled after you check one or more rows
in the detail list. One confirm uses the same destination path field as reduced-actions
**Move**. Each checked **page, file, or asset** is moved with
`POST /rest/folders/move/item` (`MoveFolderItem`). Checked **folders** are
not moved; Explorer names them in the result (for example `Skipped folders: News`).

Moves that already succeeded stay in the destination when a later item fails.
The result is full success only when every page or asset moved. A partial HTTP
failure (for example **404** on one item) stays on the result as an alert and is
not reported as full success. A selection that is only folders moves nothing.
This is not single-item **Move** of a folder, not **Copy selected to folder**,
and not clipboard cut-and-paste.

### Recycle selected pages and assets

**Content → Recycle selected** is enabled after you check one or more rows in
the detail list. One confirm recycles each checked **page, file, or asset** with
public REST `DELETE /rest/folders/item/{path}` (the same call as single-item
**Delete** for non-folders). Checked **folders** are not recycled; Explorer names
them in the result (for example `Skipped folders: News`). Folder recycle stays
on reduced-actions **Delete** (`POST …/path/deleteFolder`).

Items that already recycled stay in the bin when a later item fails. The result
is full success only when every page or asset recycled. A partial HTTP failure
(for example **404** on one item) stays on the result as an alert and is not
reported as full success. A selection that is only folders recycles nothing.
This is not **Empty recycle bin**, not **Purge selected**, and not multi-select copy or move.

### Purge selected pages and assets in Recycling

**Content → Purge selected** (and the reduced-actions **Purge** button when two or
more Recycling rows are checked) asks once, then permanently purges each checked
**page or asset** with `DELETE /rest/folders/recycle/{guid}` — the same call as
single-item **Purge**. **Cancel** purges nothing. Checked **folders** are not
purged; Explorer names them in the result (for example `Skipped folders: News`).

Items that already purged stay gone when a later item fails. The result is full
success only when every checked page or asset was purged and no folder was
selected. HTTP **403**, **404**, or **409** on one item stays on the result as
an alert and is not reported as if the whole selection was purged. A selection
that is only folders purges nothing. This is not **Empty recycle bin** and not
**Recycle selected**.

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

## Content Browser pickers

Slot **Add**, the modern **asset picker**, and the **page picker** open **Content Browser**
(tree, list, and the same Search panel). Browse to a page or asset, or search, then
**Open** a result and **Confirm**.

Search hits use CMS content-type names (for example **Image**, **File**, **percPage**,
or FastForward types such as **rffImage**). Those names match the picker's page/asset
filter — **Open** selects the hit, the selection summary updates, and **Confirm**
enables. Folders and navigation nodes are not selectable in the asset or page picker.
A type that the picker does not allow still shows *Selected item type is not allowed*
and leaves Confirm disabled.

## Display format

Use the **display format** selector next to the menu bar to choose list columns for the
current folder. Explorer prefers folder-valid formats (`validForFolder`). If that filtered
catalog is empty, it lists the full display-format catalog so you can still switch columns.
Changing the format reloads the detail list using that format's numeric id
(`displayFormatId`) so headings and cell values match the selected columns. The selector
stays available if the catalog fails to load (a short error appears next to it).

**Columns** (next to the menu bar) changes which fields the current folder list shows
without editing the shared display format. Title (`sys_title`) stays selected. Apply
saves the list for this user and folder for the server session
(`PUT /Rhythmyx/services/explorer/list-columns`). Opening the same folder again in that
session restores the columns. A missing folder, unknown field, or duplicate field is
**HTTP 400**. A request with no signed-in user is **HTTP 403**. The list shows that
status next to the Columns control. Restarting the server clears the overlay.

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
  Select a **page or other workflowed content item** — **folders** (including Sites
  and folder rows in the list) do not show Workflow transitions. Invoking a listed
  trigger (for example **Expire** on a Public FastForward page) performs the
  transition; it does not fail with HTTP 500 *Failed to perform a transition*.
  With two or more rows checked, the toolbar group shows only transitions shared by
  every selected page or asset. One confirm applies that trigger to each of them
  and skips folders. A failure on one item names that item; it is not full success.
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
principal lists and **Save** ACL rows for the selected folder. Save uses
`POST /Rhythmyx/services/pathmanagement/path/saveFolderProperties`. A missing
folder is HTTP **404** and a caller without ADMIN on that folder is HTTP **403** —
neither is treated as a successful save; Explorer shows the error on the panel.
Other users see a read-only view.

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
  content row. Under **Assets**, select a library asset (including auto-named
  items such as `New-percSimpleTextAsset-…`) the same way. **View → IA
  Relationships** or **View → Dependencies** then mounts the matching panel
  (loading, results, or empty). **Dependencies** calls
  `GET /Rhythmyx/rest/content-explorer/relationships/{contentId}/summary`
  (read-only; this view does not create or delete relationships). When the
  item has relationship buckets, taxonomy nodes, local links, or Active
  Assembly links, those edges are listed. When every count is zero the panel
  says **No known dependencies for this item** — that is not a successful
  edit and not a blank panel. HTTP **403** is *You do not have permission to
  perform this action*. HTTP **404** (unknown content id) is **This item was
  not found.** Neither status is shown as an empty graph. An **Admin** session
  does not see the permission message for a selected asset; that message
  is reserved for a true authorization failure. List row ids may be
  GUID-shaped (`1-101-708`); both panels use the last segment as the content
  id, not a trailing timestamp on the asset title. A folder-only or empty
  selection keeps the select-item hint. See **Translations** below for locale
  variants.
- **Clipboard** — copy/cut staging panel. **View → Clipboard** always opens
  the panel (even when empty) and shows a check mark while it is visible.
  Use **Content → Add to clipboard** after multi-select to put items on the
  clipboard and open the panel (including **Sites** rows). Do not click
  **View → Clipboard** again after Add — that hides the already-open panel.
  Select the destination folder in the tree or list, then **Paste** on the
  clipboard panel. Paste copies (or cuts) each clipboard item **into that
  destination** using public REST `POST /rest/folders/copy/item` (pages and
  assets), `POST /rest/folders/copy/folder` (folders), or the matching
  `move/item` / `move/folder` endpoints for Cut. Missing destination is HTTP
  **404**; non-admin callers receive **403**; pasting a folder into itself is
  **409**. Paste is not the reduced-actions Copy destination picker.

## Translations

Open **View → Translations** after selecting a **page or asset** in the list (or use
**Translate** on Server actions / the item context menu).

The panel shows **this item’s current locale** and **related locale variants**, and lets
an authorized user **Create variants** for catalog locales the item does not already
have. Each variant row includes **Open locale copy**, which opens that locale’s
content item in the editor. Create-variant maps HTTP **403** (not allowed), **404**
(source item missing), and **409** (that locale copy already exists). Explorer list rows identify items with a Percussion content id. Page and asset
rows expose that id as a GUID (`host-type-uuid`, for example `16777215-101-551`) on
the row — the same key Translations GET accepts. The panel sends that **full GUID** on
`GET /rest/content-explorer/translations/{itemId}` (the REST façade also accepts a
bare numeric content id such as `551`). Create-variant still posts the numeric
content id. Stripping a GUID to its last segment for the GET (`…/translations/551`)
fails with **Item not found**. That GUID form must not fail with “Selected item does
not have a numeric content id.”

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
