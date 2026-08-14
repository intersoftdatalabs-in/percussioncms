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
`siteArchitecture.jsp` page and `?view=arch` bookmarks hard-redirect into the SPA.

The product name is **Navigation**. The SPA route remains `/architecture` (and
`spa.jsp?entry=architecture`) so existing bookmarks and homepage type
`Architecture` keep working.

## Open Navigation (SPA)

1. Sign in as an **Admin** or **Designer**.
2. Choose **Navigation** in the product top navigation, or open the SPA entry:
   - Query contract: `spa.jsp?entry=architecture`
   - Path route: `/cm/app/architecture` (optional site segment or `?site=` for context)
   - Legacy bookmarks: `/cm/app/?view=arch` and `/cm/app/siteArchitecture.jsp` redirect here
3. The shell loads under the same product top nav as Explorer, Design, Publish, and Admin.

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
6. Use **New Site** (Admin or Designer) to open the same traditional-site wizard
   used in Content Explorer. After a successful create, the new site is selected
   in this shell.
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
created site, or a site whose navigation was never created). Opening
**Navigation** for that site does **not** fail with HTTP 500.

The server `GET /Rhythmyx/sitemanage/section/tree/{siteName}` call returns
**HTTP 200** with an empty section tree (`childNodes` empty). The SPA shows an
operator **empty state** (“No navigation tree”) in the Navigation tree panel
instead of a route error or a generic 500 banner.

To add navigation later, create a NavTree at the site root in **Explorer**
(or create the site with managed navigation), then use **Refresh**. Other
sites with a tree continue to load normally.

Rhythmyx FastForward sites store the same tree as **`rffNavTree` / `rffNavon`**
(same tables as CM1 **`percNavTree` / `percNavon`**). The product treats those
names as the same Navigation types. A site that already has an `rffNavTree` at
the folder root is **not** empty — do not create a second tree. New sites
created in this product use the `percNav*` names.

## Keyboard and accessibility

The navigation tree follows the ARIA tree pattern:

| Key | Behavior |
|-----|----------|
| **Tab** | Moves focus into the tree (roving tabindex on the selected or root item). |
| **Arrow Up / Down** | Move focus among visible nodes. |
| **Arrow Right** | Expand a collapsed branch, or move into the first child when expanded. |
| **Arrow Left** | Collapse an expanded branch, or move focus to the parent. |
| **Home / End** | Jump to the first or last visible node. |
| **Enter / Space** | Select the focused section (and toggle expand on branches). |

Structure dialogs (create, create from folder, rename, landing page, section link, external link, the
section picker, **New Site**, and **Copy Site**) are modal (`role="dialog"`, `aria-modal`). **Escape**
closes the open dialog when a mutation is not in progress. Closing **New Site** or
**Copy Site** returns keyboard focus to the matching toolbar button. Primary
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
| **Move up / Move down** | Reorders the selected section among its siblings under the same parent. |
| **Convert to folder** | After confirmation, removes the selected regular (non-root) section and its sub-sections from Navigation. The folder and its pages stay in the site. |
| **Delete** | Deletes the selected non-root section after confirmation. Section links use the section-link delete path. |

Server errors from create, convert, create-from-folder, rename, move, delete, landing-page, or link mutations are
shown in the panel (no silent failure). The tree reloads after a successful mutation
and keeps the previously selected section when it still exists.

### Replace a section landing page

1. Select a **regular section** in the Navigation tree (not a section link, external
   link, or blog).
2. Choose **Landing page**. The page picker opens on that site under `//Sites/{site}`.
3. Select a **page** and confirm. The shell posts the replace request and reloads
   the tree. The selected section stays selected and shows **Landing page is now …**.
4. **Cancel** (picker or dialog) closes without changing the section.
5. If no page is selected, **Replace landing page** stays disabled and the dialog
   shows **No page selected** — the server is not called.

### Blog sections

Blog-type sections appear in the navigation tree (type badge). Full blog post
authoring remains outside this Navigation editor; treat blog structure as
visible but limited in this surface. **Convert to folder** applies only to
regular (non-root) sections — not blogs, section links, or external links.

### Still later

Full section security / ACL preferences, blog type editor, and
retirement of the legacy `siteArchitecture.jsp` host ship in follow-on slices.

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
| Convert section to folder / create section from folder | **Available** |
| Landing page / section-link / external-link parity | **Available** |
| Landing page picker + replace (`replaceLandingPage`) | **Available** |
| Keyboard / ARIA tree + Escape dialogs | **Available** |
| `perc.ui.architecture.modern` TMX chrome keys | **Available** (en-us feature keys; other locales via nightly i18n) |
| Playwright surface smokes (shell / tree / mutations / links / a11y) | **Available** |
| Legacy `siteArchitecture.jsp` retirement | **Planned** after SPA parity |
| Legacy `siteArchitecture.jsp` / `?view=arch` | **Redirected** to SPA Navigation (#3099) |

## Related

- [Sites & content structure](id:admin-sites)
- [Content Explorer](id:admin-content-explorer)
- [Users, roles & security](id:admin-users-roles) (default landing options include Navigation)
