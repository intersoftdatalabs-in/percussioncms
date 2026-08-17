---
id: admin-sites
title: Sites & content structure
description: Sites, folders, and content organization in Percussion CMS 8.2
version: "8.2"
order: 41
tags: [admin, sites]
---

# Sites & content structure

A **Site** is the primary organizational unit for published output and editorial structure in
Percussion CMS. Site properties include hostnames, permissions, publishing configuration, and
(optionally) Virtual Site source settings.

## Traditional Sites

Traditional Sites store pages and assets in the Percussion **content repository**:

- Editors work in the Web UI / Finder against Site folders and pages.
- Workflow, ACL permissions, and revisions apply to repository items.
- Publishing assembles templates/variants and delivers to filesystem, FTP, or other pub locations.

### Browse Sites in Content Explorer

1. Sign in as an administrator (or a role that can open **Explorer**).
2. Open **Content Explorer** (`spa.jsp?entry=explorer` or the **Explorer** product navigation entry).
3. In the tree, expand **Sites** (use the disclosure control next to the **Sites** label — selecting the row only opens the list, it does not expand child site nodes).
4. Select a site folder to browse its pages and folders in the detail list.
5. Expand a sample site (for example **Corporate_Investments** or **Enterprise_Investments**). The tree and detail list show that site's **real** folders (for example **AboutEnterpriseInvestments**, **Files**, **Images**) and pages, not an empty-folder message. Explorer also shows a **Pages** folder as site chrome. Opening **Pages** lists at least one page-type child from the sample site (the homepage or a page under an About section) so the detail list is not empty. Opening **Files** lists the FastForward Files folder items (or file children when that folder is missing).

After a standard install with **sample sites** (installer **Install sample sites** / silent `--demo-sites`), the Sites tree typically includes stock demo sites such as **Corporate Investments** and **Enterprise Investments**. Those FastForward sample sites are traditional **Rhythmyx** sites (not CM1 page-based). The installer loads the FastForward type/template seed, the **ObjectStore** editors for those types (`psx_cerffGeneric.xml`, `psx_cerffPressRelease.xml`, and the other rff editors — CM1 `perc*` types still come from packages), the **sample content** graph (site folders, Files/Images, About sections, navons, and pages), plus hashed sample binaries under `rxconfig/FastForward/importFiles` (imported on first server start). If sample items exist but the server log shows `Invalid content type id (311)` (or other 301–316 ids), those ObjectStore editors were not copied — reinstall with `--demo-sites` from an installer that includes this step. FastForward **navigation** types stay at ids 313–315 (`rffNavImage` / `rffNavon` / `rffNavTree`). The `perc.nav` package installs `percNav*` under separate ids; the sample seed must not rename 313–315 to those package names (that unique-name clash aborts the rest of the type table and drops **Press Release** id 316). Site **names** may use underscores (`Corporate_Investments`) while the repository folder is `//Sites/CorporateInvestments` — Explorer binds expand/list to the site folder root. The Sites list includes **all** sites — Rhythmyx and CM1 page-based, with or without a publishing server or navigation tree. Opening **Navigation** for a demo site that already has an `rffNavTree` / `percNavTree` shows that tree; if a site has a folder root but no NavTree, first open creates one — see [Navigation & site structure](id:admin-architecture-navigation). Fresh evaluation or H2 QA stacks without sample seed may show an empty Sites list until you create a site or reinstall with sample data. A running install that was seeded before this content pass is **not** backfilled — reinstall with `--demo-sites` (or a new H2 `qa-up`) to get FastForward pages and navons.

### Create a Site from Explorer

Use **Content Explorer** (or **Navigation → New Site**) when you need a new Site without leaving the product shell. The first wizard step is **Site type**:

| Type | Managed navigation | Page / base template | Persist |
|------|--------------------|----------------------|---------|
| **Traditional** | Optional (checked by default) | Not prompted — a generated template name and `perc.base.plain` are sent | Repository site |
| **Page** | Required (locked on) | Required — choose a **template name** and **base template** | CM1 page-based site (`pageBased` / `IS_PAGE_BASED` on `POST /sitemanage/site/`) |
| **Virtual** | Not used | Not used | Not available in this wizard yet — configure Virtual source on Developer → Sites |

1. Open **Content Explorer**.
2. Choose **Content → Create Site**.
3. On **Site type**, choose **Traditional** or **Page**. **Virtual** is listed but cannot continue until that path is implemented.
4. On **Details**, enter a unique **Site name** and optional description.
   - **Traditional:** **Include managed navigation** is checked by default. Leave it checked to create a NavTree and homepage. Uncheck it to create the site folder only — no NavTree and no homepage. You can add navigation later from Explorer.
   - **Page:** managed navigation is required and cannot be turned off.
5. **Page** only: on **Base template**, enter a **template name** (defaults from the site name) and pick a base template from the catalog (or accept the default when the catalog is empty). Traditional does not show this step.
6. On **Confirm**, review the type, name, managed-navigation choice, and (Page only) the template fields.
7. Choose **Create** and wait for progress to complete. Explorer opens the new site under `/Sites/<name>`. With managed navigation on, the server seeds a NavTree, a site template, and the homepage (`index.html`) in that folder in one operation. If the folder already has a NavTree or Navon, Create Site returns HTTP 400 with a clear error (not HTTP 500).

Virtual Site source settings (Git/filesystem) are configured later on the Site properties / Developer Sites surface — not in this Create Site wizard. See [Virtual Sites (developer)](id:developer-virtual-sites) and the Virtual Sites section below.

## Virtual Sites (8.2)

A **Virtual Site** is a Site whose content originates **outside** the traditional repository
(for example Git/filesystem Markdown under `product-docs/`). Virtual items are discovered and
assembled without ingesting them as ordinary CMS content items.

Operators configure Virtual Sites through **Site properties** (not new database columns in Phase 1).
Integrators can set the same properties over public Site REST
(`GET`/`PUT /sites/{nameOrId}/virtual`) without Workbench. Authors of Virtual content use Git
and Markdown tooling, not the classic page editor.

### Property keys operators set

| Property | Required | Example | Notes |
|----------|----------|---------|-------|
| `virtual.sourceKind` | Yes (for Virtual) | `git-filesystem` | Phase 1 allow-list: **`git-filesystem` only**. Blank or `repository` = traditional Site. |
| `virtual.rootPath` | Yes (when virtual) | absolute path to `product-docs` | Must be non-blank for Virtual Sites. Prefer absolute paths; use portable paths (Windows/Linux/macOS). Paths with `..` after normalize are rejected. |
| `virtual.configFile` | No | `_config.yaml` | Default `_config.yaml`. Simple file name under the root (no `..` or directory separators). |
| `virtual.siteKey` | No | `product-docs` | Optional participant key; defaults to the Site name. |

Invalid combinations (unknown source kind, missing root, unsafe path, config path traversal) are
rejected by server validation with clear error messages.

### Browse Sites in Developer

**Developer → Sites** lists **all** CMS Sites from `GET /services/sites` (traditional repository
Sites, CM1 page-based Sites, and Virtual Sites). The JSON envelope is a Jackson root-wrapped
array (`{"SiteList":[{"name":"…",…}]}`), not an `{empty:false}` bean. Sample / demo Sites
appear when they exist on the server (for example after **Install sample sites**). If that
public list is empty or unreadable, the catalog also consults `GET /sitemanage/site/`
(the same `SiteSummary` list Home and Explorer use) so existing Sites are not hidden.

1. Sign in as an administrator (or a role that can open **Developer**).
2. Open **Developer** → **Sites** (SPA entry `spa.jsp?entry=developer&section=sites`).
3. Confirm the catalog table shows site **name**, **description**, **base URL**, and flags.
4. Choose a row to open **Site detail** (URL defaults, object GUID, Virtual Site source,
   and **Object ACL**).

The **Object ACL** section on Site detail uses the site GUID from the catalog payload
(`guid.stringValue`, or `hostId-type-uuid` when `stringValue` is omitted). When a GUID
is present, the ACL table (or empty create path) is readable and editable — Design
access plus Runtime visibility. When the catalog row has no GUID parts, the section
still mounts with site-specific empty copy and does not crash the page. See
[Users, roles & security](id:admin-users-roles) for the same Object ACL behavior on
Display Formats.

Empty state (**No sites returned**) appears only when **every** list source has **zero**
named Sites. A successful HTTP 200 with Site entries — including `SiteList`, nested
`Site`/`sites`/`item` wraps, or `SiteSummary` — must populate the table (never a silent
blank). Load failures show **Could not load sites** rather than the empty state.

### Configure Virtual Site source in the product UI

1. Sign in as an administrator (or a role that can open **Developer**).
2. Open **Developer** → **Sites** (SPA entry `spa.jsp?entry=developer&section=sites`).
3. Select a Site row to open **Site detail**.
4. In the **Virtual Site source** section:
   - **Source kind** — leave **Repository (traditional)** for ordinary CMS Sites
     (blank/`repository` on the server). Choose **Git filesystem** for Phase 1 Virtual Sites.
   - **Root path** — absolute or install-relative path to the documentation tree
     (required when source kind is Virtual). Do not use `..` path segments.
   - **Config file** (optional) — simple file name under the root (default `_config.yaml`
     when unset). No path separators.
   - **Site key** (optional) — participant registry key; defaults to the Site name when blank.
5. Choose **Save Virtual Site source**. The SPA sends the Jackson/JAXB envelope
   `{ "VirtualSiteProperties": { "sourceKind": "git-filesystem", "rootPath": "…", … } }`
   (not a bare `sourceKind` object). After a successful save the panel reloads
   properties from GET so **Build Virtual Site** appears without a full page reload.
6. To return a Virtual Site to traditional repository mode, set source kind back to
   **Repository (traditional)** and save (clears `virtual.*` properties).

Validation matches the server helper (`PSVirtualSiteHelper`): allow-listed source kinds,
required root path when virtual, and safe path/config names. After root or config changes,
re-run the offline docs build, the in-product **Build Virtual Site** action (below), or the
CMS publish path to verify links.

### Build a Virtual Site from the product UI

When **Source kind** is **Git filesystem** (Virtual), the Site detail panel shows a
**Build Virtual Site** control. Traditional **Repository** Sites do **not** show this control
(no misleading virtual-build chrome).

1. Sign in as an **Admin** (the build REST operation requires Admin).
2. Open **Developer** → **Sites** and open the Virtual Site detail.
3. Confirm **Virtual Site source** is saved as **Git filesystem** with a valid **Root path**
   that exists on the CMS host. If you just edited properties, choose **Save Virtual Site source**
   first — the build uses the **saved** server properties, not unsaved form fields.
4. Choose **Build Virtual Site**.
5. After a `git pull` or a local Markdown/frontmatter edit on the host, choose **Build Virtual
   Site** again. The build re-reads the current filesystem — **do not restart the CMS** just to
   pick up those edits. There is no file watcher; the next explicit build is the refresh.
6. Wait for the busy indicator, then review:
   - **Success** — pages written, absolute output path (default under
     `{install}/tmp/virtual-sites/{siteKey}` when no custom output is set).
   - **Link problems** — a count appears when internal `id:` or relative links fail.
     Expand **Show link problem details** to read the same lines written to
     `link-report.txt` in the output directory. **Copy link problems** puts those
     lines on the clipboard. The build still completes with HTTP 200
     (`hasLinkProblems=true`); this is not a 500 and is not a failed save.
     A clean build does **not** show a link-problem banner.
   - **Error** — clear message when the Site is not virtual, the root is missing/invalid,
     or the caller lacks Admin (for example 400/403 from REST).

### Preview the assembled Virtual Site

After a successful **Build Virtual Site**, operators can open the assembled documentation
home from the same Site detail panel (no CLI, no `file://` path).

1. Stay on **Developer → Sites → Site detail** for the Virtual Site (Admin).
2. Choose **Preview assembled site**.
3. The CMS opens the last build’s home (typically `8.2/index.html`, or root `index.html`
   when present) in a new tab. Navigation stays on the same-origin preview URL
   (`GET /services/sites/{name}/virtual/preview/{path}`).
4. If no build has been run yet (or the last output is missing), the panel shows a clear
   empty state — **No assembled site to preview. Run Build Virtual Site first.** — and
   does not return HTTP 500.

The preview stream reads the last recorded `outputPath` from the build (default
`{install}/tmp/virtual-sites/{siteKey}`). It is **Admin-only**, path-traversal safe, and
does not invent a second assembler. Traditional **Repository** Sites do not show Preview
chrome.

Integrators can call the same operation over REST:
`POST /sites/{nameOrId}/virtual/build` (optional JSON body `outputRoot`). See
[Site configuration reference](id:reference-site-config) and
[Virtual Sites (developer)](id:developer-virtual-sites).

### Publish a Virtual Site to the Site filesystem target

Build output under `{install}/tmp/virtual-sites/` is **staging**. To deliver a navigable static
site to the Site's configured filesystem publish location:

1. Set the Site **publishing filesystem root** (Site root / `IPSSite.root`) to a dedicated
   directory on the CMS host (not the Markdown `virtual.rootPath`).
2. As **Admin**, open **Developer → Sites → Site detail** for the Virtual Site.
3. Confirm **Source kind** is **Git filesystem** and **Save Virtual Site source** if you
   changed properties. Traditional **Repository** Sites never show Publish chrome.
4. Choose **Publish Virtual Site**. The panel shows a busy state, then success with
   **files copied** and the **destination path**, or a clear error (not Admin, still a
   repository Site on the server, missing or unsafe Site root).
5. Integrators can call the same operation over REST: `POST /sites/{nameOrId}/virtual/publish`.
   The server **builds then copies** HTML/assets to that Site root and returns `publishPath`
   and `filesCopied`. Missing or unsafe Site root, overlap with the source tree, or a
   non-virtual Site returns **400** with a readable message (not HTTP 500 / silent no-op).

See [Publishing](id:admin-publishing) for the operator checklist.

Offline scripts (`scripts/build-cms-docs.*`) remain available for developer workstations
without a running CMS.

See [Virtual Sites (developer)](id:developer-virtual-sites) and
[Site configuration reference](id:reference-site-config) for full contract and offline build steps.

## Folders, pages, and assets

| Concept | Role |
|---------|------|
| **Site root / folders** | Hierarchy in Finder; drives navigation and pub structure |
| **Pages** | Assembled HTML (or other) destinations for site visitors |
| **Assets** | Shared content fragments (images, rich text, shared widgets) referenced by pages |
| **Templates / variants** | Presentation used during assembly and publishing |

## Operational tips

- Prefer clear folder naming that matches the public URL tree when possible.
- Limit deep nesting that confuses editors and slows Finder browsing.
- Document which Sites are production vs staging vs documentation Virtual Sites.
- After property changes that affect Virtual roots, re-run the offline docs build or CMS pub path to verify links.

## Related

- [Content Explorer](id:admin-content-explorer) — product shell for browsing Sites/folders and running server actions
- [Publishing](id:admin-publishing)
- [Users, roles & security](id:admin-users-roles)
