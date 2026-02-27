# Percussion CMS — UI Structure Research Report

_Generated 2026-02-21_

---

## 1. `cui/` — Content Contributor UI

### Directory listing

```
cui/
├── index.html            ← SPA entry point
├── package.json          ← real npm package (see below)
├── require.js            ← copy/symlink of RequireJS
├── README.md
├── node_modules/         ← npm install output (gitignored)
├── components/           ← vendored JS libraries
│   ├── dynatree/
│   ├── font-awesome-4.1.0/
│   ├── google-fonts/
│   ├── jquery/
│   ├── jquery-migrate/
│   ├── jquery-ui/          (package.json = upstream jquery-ui 1.13.2 metadata)
│   ├── knockoutjs/         (package.json = upstream knockout 3.1.0 metadata)
│   ├── perc-css/
│   ├── perc-utils/
│   ├── pubsub-js/
│   ├── requirejs/
│   ├── requirejs-text/     (package.json = upstream requirejs-text 2.0.12 metadata)
│   ├── twitter-bootstrap-3.0.0/ (package.json = upstream bootstrap 3.0.0 metadata)
│   └── widgel/             (package.json in widgel/sample/ = example app, not real)
├── pages/
│   ├── _bootstrap.js     ← RequireJS config (path mappings, shims)
│   ├── config.js         ← bootstraps the app using requirejs + jquery
│   ├── cm1adaptor.js
│   └── utils.js
└── widgets/              ← Knockout/jQuery UI widgets
    ├── addwizard/
    ├── app/
    ├── assetwizard/
    ├── basedialog/
    ├── blogpostwizard/
    ├── contentList/
    ├── hello/
    ├── pagewizard/
    └── search/
```

### `cui/package.json` — **REAL** npm package

```json
{
  "name": "cui",
  "version": "0.0.2",
  "private": true,
  "scripts": { "start": "node app" },
  "dependencies": {
    "express": "~5.2.1",
    "extend": "*",
    "jquery-ui": "^1.13.2",
    "lodash": ">=4.17.21",
    "pubsub-js": "^1.9.4",
    "requirejs": "^2.3.7",
    "requirejs-text": "^2.0.16",
    "jquery": "^3.6.0",
    "knockout": "^3.5.1"
  }
}
```

### Frameworks used

| Library | Version | Purpose |
|---------|---------|---------|
| **RequireJS** | 2.3.7 | AMD module loader — the entire cui/ is structured as AMD modules |
| **Knockout.js** | 3.5.1 (npm) / 3.1.0 (vendored) | MVVM data-binding for widgets |
| **jQuery** | 3.6.0 | DOM manipulation, AJAX |
| **jQuery UI** | 1.13.2 | Dialogs, accordions, drag-drop |
| **Bootstrap** | 3.0.0 | Grid / CSS framework |
| **PubSub.js** | 1.9.4 | Inter-widget event bus |
| **Dynatree** | (vendored) | Tree widget |
| **Font Awesome** | 4.1.0 | Icons |
| **Lodash** | ≥4.17.21 | Utility functions |
| **Express** | ~5.2.1 | Dev server only (`node app`) |

### How cui/ is built / packaged

- **No dedicated Maven module.** The `cui/` directory is copied into the WebUI WAR by the `maven-war-plugin` (it lives under `WebUI/war/cui/` in the final artifact — the `_bootstrap.js` paths reference `/cm/cui/…`).
- **No frontend-maven-plugin here** — assets are served as-is (no transpile, no bundling; relied on RequireJS for module loading in the browser).
- The Google Closure Compiler via `minify-maven-plugin` in WebUI does bundle/minify *some* JS, but the cui/ AMD modules are loaded individually at runtime.

---

## 2. `WebUI/` — Main CMS Web Application (WAR)

### Top-level structure

```
WebUI/
├── pom.xml               ← packaging=war, the main deployable
├── war/                   ← web content root
│   ├── index.jsp          ← redirects to app/
│   ├── favicon.ico
│   ├── META-INF/
│   ├── WEB-INF/
│   │   ├── web.xml, Owasp.CsrfGuard.js, tlds/, tmxtags.tld, classes/
│   ├── api/               ← (? REST or swagger-related)
│   ├── app/               ← JSP pages (dashboard, admin, publish, template, etc.)
│   │   ├── dashboard.jsp, home.jsp, publish.jsp, siteArchitecture.jsp, …
│   │   ├── includes/      ← shared JSP includes
│   │   ├── dialogs/
│   │   └── popups/
│   ├── controllers/       ← 11 JS controller files (PercAssetController.js, etc.)
│   ├── css/ , cssMin/
│   ├── cui/               ← mounted CUI content (see section 1)
│   ├── gadgets/           ← OpenSocial gadgets
│   │   ├── container/
│   │   └── repository/common/  ← shared gadget CSS/images/libs
│   ├── images/
│   ├── jslib/             ← JavaScript libraries
│   │   ├── backgridjs/, jquery-ui.js, modernizer/
│   │   └── profiles/3x/  ← jQuery 3.x profile with many libs
│   ├── mock/              ← test mock data
│   ├── models/            ← 4 JS model files (PercPageModel.js, etc.)
│   ├── plugins/           ← ~57 JS plugin files (jQuery widgets, dialogs, utilities)
│   ├── services/          ← ~31 JS service files (PercPageService.js, etc.)
│   ├── skin-win8/
│   ├── testing/
│   ├── themes/
│   ├── views/             ← ~37 JS view files (PercDashboard.js, PercFinderView.js, etc.)
│   ├── web_resources/cm/common/  ← common JS (deployed at runtime)
│   ├── widgetbuilder/
│   └── widgets/           ← ~45 JS widget files (perc_finder.js, perc_site_map.js, etc.)
├── src/
│   ├── main/java/         ← Java source (servlets, filters)
│   ├── main/resources/minify/  ← Closure/minify bundle configs
│   │   ├── common-bundles.json
│   │   ├── common-minuet-bundles.json
│   │   ├── common-ui-bundle.json
│   │   └── static-bundles.json
│   └── test/java/
├── node/                  ← contains node_modules/ (from frontend-maven-plugin in ancestors?)
├── lib/
└── target/
```

### `WebUI/pom.xml` — Build overview

| Aspect | Detail |
|--------|--------|
| **Artifact** | `perc-web-ui` (WAR) |
| **Java source** | `src/main/java` — servlets, tag libs, filters |
| **JS/CSS minification** | **`minify-maven-plugin` 1.7.6** with Google Closure Compiler (WHITESPACE_ONLY level) — 4 execution profiles: `min-common`, `min-common-minuet`, `min-package`, `common-ui` |
| **WAR Assembly** | `maven-war-plugin` → copies from `war/`, `system/ear/jsps`, `system/ear/config`, `system/ear/WEB-INF`, deployer config, etc. |
| **Ant tasks** | `maven-antrun-plugin` — chmod, attrib, dev-install targets |
| **No frontend-maven-plugin** in WebUI itself | Node/npm is NOT used to build WebUI assets |
| **Dependencies** | MyFaces, Trinidad, Tomahawk, Shindig, Jersey, Swagger UI |

### JS Framework in WebUI

The main editor UI (`war/app/*.jsp`, `war/views/`, `war/controllers/`, `war/services/`, `war/plugins/`, `war/widgets/`) is a custom **jQuery + Backbone.js** MVC architecture:

- `views/` — Backbone-style view classes
- `controllers/` — controller JS files
- `models/` — Backbone model files (PercPageModel.js, etc.)
- `services/` — AJAX service wrappers calling REST APIs
- `plugins/` — jQuery UI widget plugins
- `widgets/` — more jQuery widgets (finder, tree, datatable, etc.)

Key libraries (from `war/jslib/profiles/3x/package.json`):

| Library | Version |
|---------|---------|
| jQuery | 3.6.0 |
| jQuery UI | 1.13.2 |
| jQuery Migrate | 3.3.2 |
| Backbone.js | 1.3.3 |
| Backgrid | 0.3.8 |
| DataTables | 1.12.1 |
| FancyTree | 2.30.0 |
| Mousetrap | 1.6.2 |
| Moment.js | 2.29.4 |
| Superfish | 1.7.10 |
| jQuery Validation | 1.20.0 |
| QUnit | 2.6.2 (dev) |

---

## 3. `modules/shindig-uber/` — Shindig/OpenSocial Gadget Container

### Purpose

An **uber-JAR** that packages all Apache Shindig runtime components for the dashboard gadget framework. Percussion CMS uses OpenSocial gadgets for the dashboard (e.g., activity gadget, site summary gadget, content list gadgets, etc.).

### Key details

| Aspect | Detail |
|--------|--------|
| **Artifact** | `shindig-uber` (JAR, shaded) |
| **Shindig version** | `${shindig.version}` (managed in parent POM) |
| **Dependencies** | shindig-common, shindig-social-api, shindig-gadgets, shindig-features, Google Guice, Ehcache, Apache Shiro, Caja (HTML sanitizer), Rome (RSS), JUEL |
| **Build** | `maven-shade-plugin` creates a fat JAR |
| **Gadget content** | `WebUI/war/gadgets/` has the actual XML gadget specs and JS |

### Gadgets in WebUI

```
WebUI/war/gadgets/
├── container/       ← Shindig container config/JS
└── repository/
    └── common/      ← shared CSS, images, lib/ for gadgets
```

---

## 4. Legacy Rhythmyx UI — MyFaces / JSF / Trinidad / Dojo / XSL

### MyFaces (JSF)

**Versions (parent POM):**
- `myfaces.version` = **4.1.2** (MyFaces Core 4.x — Jakarta Faces)
- `trinidad.version` = **2.2.1** (Apache MyFaces Trinidad)

**POM dependencies referencing MyFaces:**

| Module | Dependencies |
|--------|-------------|
| Parent POM (`pom.xml`) | myfaces-api, myfaces-impl, trinidad-api, trinidad-impl, tomahawk |
| `system/pom.xml` | myfaces-api, myfaces-impl, trinidad-api, jakarta.faces-api |
| `WebUI/pom.xml` | trinidad-api, trinidad-impl, tomahawk |

**JSF Faces Config files:**

| File | Purpose |
|------|---------|
| `system/ear/WEB-INF/faces-config.xml` | Main faces-config (DTD 1.1 style, migrated to jakarta) |
| `system/ear/WEB-INF/admin-faces-config.xml` | Admin UI managed beans (PSAdminNavigation, etc.) |
| `system/ear/WEB-INF/publishing-faces-config.xml` | Publishing design/runtime managed beans (PSDesignNavigation, PSRuntimeNavigation) |
| `system/ear/WEB-INF/trinidad-config.xml` | Trinidad render kit config |
| `system/ear/WEB-INF/trinidad-skins.xml` | Trinidad skin config |
| `system/ear/config/user/faces/faces-config.xml` | User/tenant customizable faces-config |
| `system/ear/WEB-INF/web.xml` | References all faces-configs: `publishing-faces-config.xml, admin-faces-config.xml, config/user/faces/faces-config.xml` |

**JSF Java packages:**

| Package | Location | Description |
|---------|----------|-------------|
| `com.percussion.rx.admin.jsf.beans` | system/business/ | Admin JSF managed beans |
| `com.percussion.rx.admin.jsf.nodes` | system/business/ | Admin JSF tree nodes |
| `com.percussion.rx.publisher.jsf.beans` | system/business/ | Publishing JSF beans |
| `com.percussion.rx.publisher.jsf.data` | system/business/ | Publishing JSF data beans |
| `com.percussion.rx.ui.jsf.beans` | system/business/ | Common UI JSF beans (PSTopNavigation, PSHelpTopicMapping, PSUserStatus) |
| `com.percussion.servlets.taglib` | system/src/main/java/ | Custom JSF tag components: PSUIProgressBar, PSUISpanId, PSUIMenuBar, PSUIMenuItem, PSMenuBarTag, PSMenuItemTag, PSCascadeMenuTag, PSTabsContainerTag |

**JSF JSP pages (legacy admin/publishing UIs):**

```
system/ear/jsps/ui/
├── admin/           ← Admin console JSPs (ScheduledTask, TaskLogs, console, etc.)
├── publishing/      ← Publishing design JSPs (SiteEditor, EditionList, ContentlistView, etc.)
├── pubruntime/      ← Publishing runtime JSPs
├── actionpage/
├── activeassembly/
├── assembly/
├── content/
└── banner.jsp, header.jsp, userstatus.jsp, error.jsp
```

### Dojo Toolkit (Legacy)

**Location:** `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/`

This is a **very old Dojo version** (pre-1.0, includes `flash6_gateway.swf`, `storage_dialog.swf`). The search returned **1,477 files** in this directory. It is used by the legacy Rhythmyx content editor and workflow editor, which renders HTML via XSL transformations. Referenced in parent POM `spotless` exclusions (Prettier can't parse it).

### XSL Stylesheets

**445 `.xsl` files found.** Major locations:

| Location | Purpose |
|----------|---------|
| `system/workflow/applications/sys_wfEditor/ApplicationFiles/` | Workflow editor XSL (workflowedit, stateedit, transitionedit, roleedit, etc.) |
| `system/workflow/applications/sys_wfLookups/ApplicationFiles/` | Workflow lookup XSL (states, roles, transitions lists) |
| `system/cms/content/applications/` | Content editor and system app XSL |
| `system/src/main/resources/com/percussion/` | Java-resource XSL for upgrades, table-building, etc. |

These are used by the **Rhythmyx XML Application** framework — Java code generates XML result documents, which are transformed by XSLT into HTML for the browser.

---

## 5. All `package.json` Files — Analysis

| # | Path | Real or Placeholder? | Notes |
|---|------|---------------------|-------|
| 1 | **`cui/package.json`** | **REAL** — active npm project | Dependencies: jquery, knockout, requirejs, express (dev server), lodash, pubsub-js, jquery-ui |
| 2 | `cui/components/knockoutjs/package.json` | **Vendored upstream** — knockout 3.1.0 | Upstream package.json shipped with the library; not a project file |
| 3 | `cui/components/jquery-ui/package.json` | **Vendored upstream** — jquery-ui 1.13.2 | Same — upstream metadata |
| 4 | `cui/components/requirejs-text/package.json` | **Vendored upstream** — requirejs-text 2.0.12 | Same |
| 5 | `cui/components/twitter-bootstrap-3.0.0/package.json` | **Vendored upstream** — bootstrap 3.0.0 | Same |
| 6 | `cui/components/widgel/sample/package.json` | **Placeholder/example** | Widgel sample app; references jade, grunt, bower — not used in production |
| 7 | **`WebUI/war/jslib/profiles/3x/package.json`** | **REAL** — dependency manifest | Declares jQuery 3.x profile dependencies: jquery, backbone, datatables, fancytree, moment, etc. Appears to drive `npm install` for populating jslib. Version `8.1.6-SNAPSHOT`. |
| 8 | `WebUI/war/jslib/profiles/3x/jquery/libraries/jquery-ui/package.json` | **Vendored upstream** — jquery-ui 1.13.2 | Same as #3 |
| 9 | **`modules/perc-tinymce/package.json`** | **REAL** — build tooling | `perc-tinymce-build` — uses esbuild 0.24.2 for minifying TinyMCE plugin files |

**Summary:** 3 real/active package.json files (#1, #7, #9), 5 vendored upstream metadata, 1 placeholder sample.

---

## 6. Frontend Build Integration

### `frontend-maven-plugin` Usage

| Location | Version | Node Version | Purpose |
|----------|---------|--------------|---------|
| **Parent POM** (pluginManagement) | **1.15.1** | — | Declares plugin version for child modules |
| **`modules/perc-tinymce/pom.xml`** | (inherits 1.15.1) | **v20.18.0** | Installs Node.js + npm, runs `npm install`, then `npm run minify` (esbuild-based minification of TinyMCE plugin JS) |
| **WebUI — NOT USED** | — | — | WebUI does NOT use frontend-maven-plugin |

### How each UI module is currently built

| Module | Build mechanism | Details |
|--------|----------------|---------|
| **`cui/`** | **Not built** — served as raw AMD modules | `npm install` populates `node_modules/` but there is no compile/transpile/bundle step. RequireJS loads modules at runtime from the browser. Assets are copied into the WAR by `maven-war-plugin`. |
| **`WebUI/`** | **`minify-maven-plugin`** (Google Closure Compiler + YUI Compressor) | 4 minification execution profiles defined in `WebUI/pom.xml` using `common-bundles.json`, `common-minuet-bundles.json`, `static-bundles.json`, `common-ui-bundle.json` configs. Produces concatenated/minified JS bundles. No transpilation, no npm involvement. |
| **`WebUI/war/jslib/profiles/3x/`** | **npm install** (manual or CI) | `package.json` drives `npm install` to pull jQuery, Backbone, DataTables, etc. into `node_modules/`. These get copied/referenced in the WAR. No build step beyond install. |
| **`modules/perc-tinymce/`** | **`frontend-maven-plugin`** → Node 20 → `npm run minify` (esbuild) | The only module with a proper modern JS build pipeline. Downloads Node, installs deps, runs esbuild minification at `prepare-package` phase. |
| **`modules/shindig-uber/`** | **`maven-shade-plugin`** (Java only) | Pure Java uber-JAR; no JS build. Gadget JS is in `WebUI/war/gadgets/`. |
| **Legacy Rhythmyx (system/)** | **XSL transformations** at runtime | XML → XSLT → HTML. Dojo is served statically from `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/`. No build step. |
| **Legacy JSF/MyFaces (system/)** | **Maven compile** (Java) + **JSP compilation** at deploy time | JSF managed beans compiled with Maven. JSPs compiled by the servlet container (Jetty). Trinidad provides component rendering. |

---

## 7. Summary of UI Technology Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                    Percussion CMS UI Layers                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. CM1 Editor UI (WebUI/war/)                                  │
│     ├─ jQuery 3.6 + Backbone 1.3 + jQuery UI 1.13              │
│     ├─ Custom MVC: views/ controllers/ models/ services/        │
│     ├─ JSP server-side rendering (dashboard.jsp, etc.)          │
│     ├─ Minified via Closure Compiler (minify-maven-plugin)      │
│     └─ OpenSocial Gadgets (Shindig) for dashboard               │
│                                                                 │
│  2. Content Contributor UI (cui/)                               │
│     ├─ RequireJS + Knockout.js + jQuery + Bootstrap 3           │
│     ├─ AMD modules loaded at runtime (no bundling)              │
│     └─ Served as static files within the WAR                    │
│                                                                 │
│  3. Legacy Rhythmyx Admin/Publishing UI (system/ear/jsps/ui/)   │
│     ├─ JSF 4.x (MyFaces) + Trinidad 2.2 + Tomahawk             │
│     ├─ faces-config.xml managed beans                           │
│     ├─ Custom JSF tag components                                │
│     └─ JSP + JSTL + EL                                         │
│                                                                 │
│  4. Legacy Content Editor / Workflow Editor (system/cms/)       │
│     ├─ XSL transformations → HTML                               │
│     ├─ Dojo Toolkit (ancient pre-1.0 version)                   │
│     └─ XML Application framework                                │
│                                                                 │
│  5. Rich Text Editor (modules/perc-tinymce/)                    │
│     ├─ TinyMCE (webjars) + CodeMirror                          │
│     ├─ Custom plugins minified via esbuild                      │
│     └─ Built with frontend-maven-plugin (Node 20)               │
│                                                                 │
│  6. Package Manager UI (PCM-PkgMgtUI/)                          │
│     ├─ SWT-based desktop application                            │
│     └─ Built with Ant (build.xml)                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```
