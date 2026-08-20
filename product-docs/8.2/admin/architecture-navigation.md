---
id: admin-architecture-navigation
title: Navigation & site structure
description: Modern Navigation SPA for browsing and editing site navigation trees (navons / sections)
version: "8.2"
order: 43
tags: [admin, architecture, navigation, ui]
---

# Navigation & site structure

**Navigation** (site navigation / navon tree editor) runs in the modern SPA product
chrome. In Percussion CMS 8.2 the primary entry is the SPA shell. The classic
`siteArchitecture.jsp` host is no longer shipped. Bookmarks to that URL and to
`?view=arch` still open SPA Navigation.

The product name is **Navigation**. The SPA route remains `/architecture` (and
`spa.jsp?entry=architecture`) so existing bookmarks and homepage type
`Architecture` keep working.

## Open Navigation (SPA)

1. Sign in as an **Admin** or **Designer**.
2. Choose **Navigation** in the product top navigation, or open the SPA entry:
   - Query contract: `spa.jsp?entry=architecture`
   - Path route: `/cm/app/architecture` (optional site segment or `?site=` for context)
   - Legacy bookmarks: `/cm/app/?view=arch` and `/cm/app/siteArchitecture.jsp` still redirect here (the JSP file is gone; the server 301s the old URL). Those landings load SPA Navigation console-clean: `GET /Rhythmyx/services/sitemanage/section/tree/{site}` is HTTP 200 (FastForward `rffNavTree` via perc/rff JCR alias), not HTTP 500.
3. The shell loads under the same product top nav as Explorer, Developer, Publish, and Admin.

Default landing can also be set to **Navigation** (stored homepage type
`Architecture`, also accepted as `Navigation`) for a user or role. After sign-in
with no deep-link return URL, the login form posts to `/cm/app/` so the
dispatcher applies that preference and opens the Navigation SPA — not Home.

## Browse a site navigation tree

1. Open **Navigation**.
2. Choose a site from the **Site** list (or open a deep link with `?site=YourSiteName`
   or `/architecture/YourSiteName`).
3. The **Navigation tree** panel loads the site’s sections (navons) from the server.
4. Expand and collapse nodes with the mouse or keyboard (Enter/Space, Arrow Left/Right).
5. Use **Refresh** to reload the tree after external changes.
6. Use **New Site** (Admin or Designer) to open the same Create Site wizard
   used in Content Explorer (Traditional / Page / Virtual type picker). After a
   successful create, the new site is selected in this shell.
7. Use **Copy Site** (Admin or Designer) with a site selected to open the
   existing site-copy wizard (same sitemanage copy service as Explorer). The
   source is prefilled from the current site. A copy already in progress blocks
   a new copy until it finishes. After a successful copy, the site list
   refreshes and the new site is selected.
8. Use **Delete Site** (Admin or Designer) with a site selected. Confirm the
   prompt. Delete is blocked while a site copy is in progress or while the site
   is still importing. After a successful delete, the site picker refreshes and
   selects another remaining site (or the empty state if none remain).

Empty, loading, and error states are shown explicitly when the site list or tree
cannot be loaded, or when a site has no sections. **New Site** remains available
when the site list is empty so operators can create the first site from this
screen. **Copy Site** and **Delete Site** stay disabled until a site is selected.

### Site without a navigation tree

A site can exist without a NavTree item at the site root (for example a newly
created site, a traditional site created with **Include managed navigation**
unchecked, a Rhythmyx site listed in the site picker, or a site whose
navigation was never created). Opening **Navigation** for that site does
**not** fail with HTTP 500.

Sample / demo sites installed with **Install sample sites**
(`Corporate_Investments`, `Enterprise_Investments` on a typical H2 QA or
new-install seed) include FastForward **sample content**: site folders,
section folders, `rffNavTree` / `rffNavon` items, and pages. Opening
**Navigation** for those sites uses that seeded tree. The installer starts
the FastForward navigation editors at type ids **313–315** (`rffNavImage` /
`rffNavon` / `rffNavTree`). The `perc.nav` package installs `percNav*` under
**1015–1017**. Do not create a second NavTree when sample `rffNavTree` items
already exist. If a site has a folder root but no NavTree (or you are on an
older seed that only created empty site rows), the first **Navigation** open
**creates a NavTree** at the site folder root so the tree panel can show
`role="tree"`. Operators do **not** need to create navigation by hand for
those demo sites.

The same create-on-first-open applies to any other entitled site that has a
folder root but no NavTree yet (same path as **New Site**).

If the site has no folder, or creating the NavTree fails, the server still
returns **HTTP 200** with an empty section tree (no section `id`, empty
`childNodes`). The SPA shows an operator **empty state** (“No navigation
tree”) instead of a route error or a generic 500 banner. Use **Refresh**
after the folder exists; other sites with a tree continue to load normally.

Rhythmyx FastForward sites store the same tree as **`rffNavTree` / `rffNavon`**
(same tables as CM1 **`percNavTree` / `percNavon`**). The product treats those
names as the same Navigation types. A site that already has an `rffNavTree` at
the folder root is **not** empty — do not create a second tree. New sites
created in this product use the `percNav*` names.

`GET /Rhythmyx/services/sitemanage/section/tree/{site}` for a FastForward sample
site (for example **Corporate_Investments**) returns **HTTP 200** with the seeded
`rffNavTree` nodes — not HTTP 500 and not an empty tree. Sample items stay on
type ids **313–315**. The `perc.nav` package registers `percNav*` under
**1015–1017** and may omit a separate JCR mapping for 313–315 (and may omit the
FastForward editor from the running catalog). The server still loads those
items through the matching perc/rff mapping (shared `RXS_CT_NAV*` tables),
including when the catalog has no name for type 315. A site with no NavTree
item still returns HTTP 200 with an empty tree.

When that tree GET is HTTP 200 with nodes, the Navigation tree shows
`role="treeitem"` rows and **Create section** stays enabled. **Escape** closes
the Create section dialog. Do not create a second NavTree for a sample site
that already has an `rffNavTree`.

## Keyboard and accessibility

The navigation tree follows the ARIA tree pattern. **Tab** moves focus into
the tree (roving tabindex on the last focused item). **Tab** / **Shift+Tab**
are not captured — focus can leave the tree; there is no keyboard trap.

| Key | Behavior |
|-----|----------|
| **Tab** | Moves focus into the tree (roving tabindex on the last focused item). |
| **Shift+Tab** | Moves focus out of the tree to the previous control. |
| **Arrow Up / Down** | Move focus among visible nodes. |
| **Arrow Right** | Expand a collapsed branch, or move into the first child when expanded. |
| **Arrow Left** | Collapse an expanded branch, or move focus to the parent. |
| **Home / End** | Jump to the first or last visible node. |
| **Enter / Space** | Select the focused section (and toggle expand on branches). |

Sections that require login show a **Secure** badge. The badge tooltip
(**Requires login**) comes from the `perc.ui.architecture.modern` catalog
(not a hard-coded-only string).

Structure dialogs (create, create from folder, rename, **section properties**, **Move section**, landing page, section link, external link, the
section picker, **New Site**, and **Copy Site**) are modal (`role="dialog"`, `aria-modal`). **Escape**
closes the open dialog when a mutation is not in progress. Closing a structure
dialog, **New Site**, or **Copy Site** returns keyboard focus to the control that
opened it. **Create section**, **Create section from folder**, **Create section
link**, and **Create external link** are enabled when the selected site has a
navigation tree and a regular section (or the tree root) can be the parent —
including when the root is used because nothing is selected yet. Primary
structure actions live in a toolbar with an accessible name (**Structure actions**).

Chrome strings (shell, tree states, actions, dialogs, validation) use the
`perc.ui.architecture.modern` message catalog so they follow the session locale when
TMX is loaded.

## Edit navigation structure

With a site selected, use the structure action bar above the tree:

| Action | Behavior |
|--------|----------|
| **Create section** | Opens a dialog to add a regular section (title, URL name, template) under the selected section, or under the site root when nothing is selected. Requires a site template. |
| **Create section from folder** | Promotes an existing site folder to a section under the selected parent (or site root). Choose the folder and a landing page that already exists in that folder. The server creates a navon and attaches the folder. |
| **Create section link** | Creates a link under the selected parent that points at another section in the same site tree (browse target in the section picker). |
| **Create external link** | Creates a nav entry that points at an external or relative URL (link text, URL, target window). |
| **Landing page** | Opens the product **page picker** (Content Browser, pages only) so you can assign a different landing page to the selected regular section. Confirming a page calls `POST /section/replaceLandingPage` and **refreshes the tree** while keeping the section selected. The assigned page name is shown on the selected section. **Cancel** or an empty pick does not call the server and does not produce an error 500 — the dialog shows “No page selected” until a page is chosen. Folders and assets cannot be assigned. |
| **Edit link** | Edits the selected section link (new target) or external link (text, URL, target window). |
| **Rename** | Renames the selected regular section (updates section title / landing link title). |
| **Properties** | Opens **Section properties** for the selected regular section (including the site root). Edit **title**, **folder name** (not on the site root), **target window**, **CSS classes**, and **Requires login** / **Allow access to** group names when the site is secure. **Save** posts `GET /section/properties/{id}` then `POST /section/update` (`SiteSectionProperties`). **Cancel** or **Escape** closes without posting. Validation errors (empty title, invalid folder name, invalid CSS class tokens) stay in the dialog — they do not produce an HTTP 500. Folder ACL principals are not edited here; **Save** still sends the current `folderPermission` so the ACL is not dropped. |
| **Folder ACL** | Opens **Folder ACL** for the selected regular section (including the site root). Add or remove users and roles on the section folder (Admin / Write / Read / View lists). **Save** on the ACL list posts `POST /pathmanagement/path/saveFolderProperties` when a folder GUID is available (same Explorer folder-security service). If pathmanagement has no folder GUID (some site-root PathItems), **Save** posts `POST /sitemanage/section/update` with the current `folderPermission` so principals are not dropped. **Close** or **Escape** does not post. Blog, section-link, and external-link nodes stay disabled. |
| **Move section** | Opens a target-parent picker for the selected **non-root** section. Choose a regular section (not the section you are moving or one of its children) as the new parent, optionally a position among that parent's children, then **Move**. The shell posts `POST /sitemanage/section/move` and **refreshes the tree**. **Cancel** (or Escape) does not post. An invalid parent shows a clear message in the dialog — it does not produce an error 500. |
| **Move up / Move down** | Reorders the selected section among its siblings under the same parent (same move API, one step). |
| **Convert to folder** | After confirmation, removes the selected regular (non-root) section and its sub-sections from Navigation. The folder and its pages stay in the site. |
| **Delete** | Deletes the selected non-root section after confirmation. Section links use the section-link delete path. |

Server errors from create, convert, create-from-folder, rename, properties, move, delete, landing-page, or link mutations are
shown in the panel (no silent failure). The tree reloads after a successful mutation
and keeps the previously selected section when it still exists.

### Edit section properties

1. Select a **regular section** (or the site root) in the Navigation tree. Section
   links, external links, and blogs use other editors — **Properties** stays disabled.
2. Choose **Properties**. The dialog loads the current values from
   `GET /sitemanage/section/properties/{id}`.
3. Change **Title** (required), **Folder name** (URL segment; locked on the site
   root), **Target window** (same / new / top / parent), and **CSS classes**
   (optional nav-widget class tokens).
4. When the site is **secure** and no ancestor already requires login, check
   **Requires login** and optionally list group names (comma-separated) in
   **Allow access to**. Otherwise login is inherited or unavailable and those
   fields stay read-only.
5. Choose **Save**. The shell reloads current properties (so a Folder ACL
   save is not overwritten) then posts `POST /sitemanage/section/update` and
   refreshes the tree. Folder ACL (`folderPermission`) from the latest load is
   sent back so the ACL is not dropped.
6. **Cancel** or **Escape** closes the dialog without posting.

### Edit folder ACL principals

1. Select a **regular section** (or the site root) in the Navigation tree.
   Section links, external links, and blogs stay disabled.
2. Choose **Folder ACL**. The shell resolves the section folder from its
   path (`GET /pathmanagement/path/item/…`) and opens the same folder-security
   lists used in Content Explorer.
3. Add a principal: choose **Add** on Admin, Write, Read, or View, type the
   user or role name, then confirm. Remove a principal with **Remove** on
   that row.
4. Choose **Save** on the ACL list to write principals. When the section
   folder has a pathmanagement GUID, that posts
   `POST /pathmanagement/path/saveFolderProperties`. Otherwise the shell
   posts `POST /sitemanage/section/update` with `folderPermission` so a
   later Properties save does not drop the list. A warning appears if the
   change would lock you out of the folder.
5. **Close** or **Escape** closes without posting further changes. Unsaved
   list edits are discarded.
6. **Section properties** **Save** still includes `folderPermission` from
   the latest properties load so an ACL write is not wiped by a later title
   or folder-name save.

### Move or reorder a section

1. Select a **non-root** section in the Navigation tree.
2. Choose **Move section**. The picker lists the site tree with the selected
   section (and its children) omitted so you cannot create a cycle.
3. Select the **new parent** (a regular section or the site root) and confirm.
   Optionally set **Position** among that parent's children, or leave **At the
   end** (append).
4. Choose **Move**. The tree reloads under the new parent / order.
5. **Cancel** closes without calling the server.
6. If the chosen parent is not valid (for example an external link), the dialog
   shows a message and does not post.

To change order only under the **same** parent, use **Move up** / **Move down**,
or **Move section** and pick the current parent with a new position.

### Replace a section landing page

1. Select a **regular section** in the Navigation tree (not a section link, external
   link, or blog).
2. Choose **Landing page**. The page picker opens on that site under `//Sites/{site}`.
3. Select a **page** and confirm. The shell posts the replace request and reloads
   the tree. The selected section stays selected and shows **Landing page is now …**.
4. **Cancel** (picker or dialog) closes without changing the section.
5. If no page is selected, **Replace landing page** stays disabled and the dialog
   shows **No page selected** — the server is not called.

### Blog sections (signed support — #3351)

Blog-type navons are **recognizable** in the Navigation tree (a **Blog** type
badge). Navigation is **read-only** for blog type: it is not a blog editor.

| Capability | Support |
|------------|---------|
| See blog sections in the tree (**Blog** badge) | **IN** |
| Use a blog as the parent when creating a regular section, section link, or external link | **IN** |
| Delete or move a blog navon (same structure APIs as other non-root nodes) | **IN** |
| Create or edit a blog section in Navigation | **Explicit OUT** |
| Replace landing page / section properties / rename / convert to folder on a blog | **Explicit OUT** |
| Full blog post authoring in Navigation | **Explicit OUT** |

**Operator alternative (create / edit blogs):**

1. Create or list blog sections from the Home dashboard **Blogs** gadget. That
   gadget already calls the existing site-section REST
   (`POST /sitemanage/section` with `sectionType=blog`, plus a blog-list
   template and a blog-post template).
2. Write posts from **Home → Create → Blog**.
3. After a blog exists, open **Navigation** for that site to see the **Blog**
   badge and to add regular child sections or links under it.

**Convert to folder** still applies only to regular (non-root) sections — not
blogs, section links, or external links.

### Still later

Blog authoring stays on Home / the Blogs gadget — Navigation will not grow a
second blog editor. The classic `siteArchitecture.jsp` host is retired from the
shipped WebUI app; bookmarks still land here via `?view=arch` and the former JSP URL.

## Current status (migration)

| Capability | Status |
|------------|--------|
| SPA route + top-nav entry under product chrome | **Available** |
| Role gate (Admin / Designer) | **Available** |
| Site picker | **Available** |
| New Site (Explorer create-site wizard) | **Available** |
| Copy Site (existing sitemanage copy wizard) | **Available** |
| Delete Site (confirm + picker refresh) | **Available** |
| Site navigation tree browse (navons / sections) | **Available** |
| Structure editing (create / rename / reorder / delete) | **Available** |
| Section properties (title / folder / target / CSS / login) | **Available** |
| Folder ACL user-list write (add/remove principals) | **Available** |
| Move section (target-parent picker + optional position) | **Available** |
| Convert section to folder / create section from folder | **Available** |
| Landing page / section-link / external-link parity | **Available** |
| Landing page picker + replace (`replaceLandingPage`) | **Available** |
| Blog navon type (badge + read-only structure) | **Available** (signed #3351; create/edit blogs on Home Blogs gadget) |
| Keyboard / ARIA tree + Escape dialogs | **Available** |
| `perc.ui.architecture.modern` TMX chrome keys | **Available** (en-us feature keys; other locales via nightly i18n) |
| Playwright surface smokes (shell / tree / mutations / links / a11y) | **Available** |
| Legacy `siteArchitecture.jsp` host retirement | **Available** (JSP removed from the shipped WebUI app; bookmarks still work) |
| Legacy `siteArchitecture.jsp` / `?view=arch` | **Redirected** to SPA Navigation (filter 301; #3099 / #3587) |

## Related

- [Sites & content structure](id:admin-sites)
- [Content Explorer](id:admin-content-explorer)
- [Users, roles & security](id:admin-users-roles) (default landing options include Navigation)
