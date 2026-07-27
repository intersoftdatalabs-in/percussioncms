# Unified UI Plan: Dojo → jQuery (Tactical) + Consolidated React UI (Strategic)

**Status**: Active
**Created**: 2026-02-27
**Replaces**: [#000-modern-ui-plan](../archived/#000-modern-ui-plan/) (archived)

## Summary

Two parallel tracks. **Track A** is a fast, mechanical Dojo 0.4.3 → jQuery swap on 5 legacy
Rhythmyx screens to eliminate security scan alerts — ships in the next release. **Track B** is a
longer-term effort to design a single React UI that replaces all legacy UI layers.

### Completed Milestones (Prior Work)

- [x] Phase 0 — Build pipeline: Vite, frontend-maven-plugin, React bridge (`PercModernUI.mount()`)
- [x] Phase 1 — React Dashboard: 24 widget components replacing Shindig gadget container
- [x] FancyTree migration: Dynatree → FancyTree across 16 files
- [x] Bootstrap 5 migration

---

## UI Layer Inventory

| # |          Layer           |              Technology               |                                                                 Exclusive Features                                                                 |        Comm Protocol        |  Screens  |
|---|--------------------------|---------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------|-----------|
| 1 | Desktop Content Explorer | Java Swing + JavaFX WebView           | Clipboard, desktop app                                                                                                                             | JAX-WS SOAP + HTTP          | ~10       |
| 2 | Rhythmyx Admin           | MyFaces JSF + Trinidad                | Server console, scheduled tasks, notifications, consistency checker, RxFix, variant migration                                                      | JSF managed beans           | 12        |
| 3 | Rhythmyx Publishing      | MyFaces JSF + Trinidad                | Site/edition/content list/context/delivery type/location scheme CRUD, pub runtime, pub logs                                                        | JSF managed beans           | 28        |
| 4 | Package Manager          | GWT + SmartGWT                        | Package install/uninstall, visibility                                                                                                              | GWT-RPC                     | 3+dialogs |
| 5 | WebUI Legacy             | jQuery 3.6 + jQuery UI + Backbone     | Template design/layout/style, site architecture, user/role/category mgmt, pub servers/reports, widget builder, revision comparison, content finder | REST/JSON                   | ~20       |
| 6 | Contributor UI (CUI)     | RequireJS + Knockout.js + widGEL      | Simplified content browsing, page/asset/blog creation wizards                                                                                      | REST/JSON                   | ~8        |
| 7 | Rhythmyx Dojo Screens    | **Dojo 0.4.3** (→ jQuery via Track A) | Active Assembly, Content Browser, Relationship Editor, field editing, search                                                                       | REST/JSON (`/contentui/aa`) | 5         |
| 8 | Eclipse Workbench        | Eclipse RCP plugin (external repo)    | Content type editor, system design, XML application editor                                                                                         | Custom protocol             | 3 views   |
| — | React Modern (new)       | React 19 + TypeScript 5.8 + Vite 6    | Dashboard (done), future unified UI                                                                                                                | REST/JSON (typed Fetch)     | Growing   |

---

## Track A — Dojo → jQuery (Immediate, Next Release)

### A0. Prune Unused Dojo Files

1. Delete from `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/`:
   `tests/`, `demos/`, `release/dojo/`, `src/crypto/`, and all `src/` subdirectories not
   referenced by `build.txt` or `ps.*` modules.
2. Update `system/ear/install-dojo.xml` and `system/ear/install.xml` (~line 598).
3. Update Spotless exclusions in root `pom.xml` (lines 2337–2343, 2419–2471).
4. Add temporary OWASP suppressions in `owasp-suppressions.xml`.

### A1. jQuery Compatibility Layer for `ps.io`

Rewrite `ps/io/Actions.js` (1,179 lines) — `dojo.io.bind()` → `$.ajax()`. Same endpoints
(`/contentui/aa?action=<Name>`), same parameters, same response parsing. Keep `async: false`
initially to preserve behavior.

~25 action methods: `move`, `addSnippet`, `removeSnippet`, `checkInItem`, `transitionItem`,
`getUrl`, `getActionVisibility`, `getSlotContent`, `getSnippetContent`, `getFieldContent`, etc.

### A2. jQuery Replacements for `ps.widget.*`

|                             Dojo Widget                              |        jQuery Replacement         |
|----------------------------------------------------------------------|-----------------------------------|
| `ps.widget.PSButton`                                                 | Plain `<button>` + CSS            |
| `ps.widget.Tree` / `TreeSelector` / `TreeIcon` / `TreeDndController` | FancyTree                         |
| `ps.widget.PSSplitContainer`                                         | CSS flexbox + jQuery UI Resizable |
| `ps.widget.PopupMenu` / `MenuBar2` / `MenuBarItem2`                  | jQuery UI Menu                    |
| `ps.widget.ContentPaneProgress`                                      | `<div>` + `$.load()` + spinner    |
| `ps.widget.ScrollableNodes` / `Autoscroller`                         | jQuery UI Draggable scroll option |
| `ps.widget.PSImageGallery`                                           | Simple jQuery gallery plugin      |

### A3. Rewrite `ps.aa.*` Controller & Modules

Rewrite `ps/aa/controller.js` (2,475 lines): `dojo.event.connect` → `$.on()`,
`dojo.byId` → `$()`, `dojo.lang.declare` → ES6 class. Same for Page.js, Tree.js,
Menu.js, dnd.js, Field.js, SnippetMove.js.

Replace DnD with jQuery UI Draggable/Droppable/Sortable.

### A4. Update Server-Side HTML Generation

- `PSPageTree.java` (line 389): `dojoType="TreeNodeV3"` → FancyTree-compatible markup.
- `PSActionBar.java` (line 304): `dojoType="MenuItem2"` → jQuery UI Menu `<li>` markup.

### A5. Update XSL, JSP, and HTML Entry Points

- `sys_aaPageHeader.html`: Replace `dojo.js` with jQuery + jQuery UI includes.
- `rceditor.xsl` (line 32): Dojo script → jQuery.
- `ContentBrowserDialog.jsp` (line 21): Replace Dojo includes. Remove `dojo.hostenv.writeIncludes()`.
- `singleFieldEdit.xsl` (lines 159–169): `dojoType="ps:PSButton"` → plain `<button>`.
- `sys_Templates.xsl` (line 3883): Remove `psxctl:FileDescriptor` for `dojo.js`.
- `getQuery.xsl` (lines 133–134): `dojoType="Button"` → plain `<button>`.
- `activeEdit.xsl` (lines 145–155): Update `ps.aa.controller` references.
- All JSPs in `system/ear/jsps/ui/content/` and `system/ear/jsps/ui/activeassembly/`.
- `styles.css`: Replace `.dojoButton`, `.dojoMenuBar2`, etc. with jQuery UI classes.

### A6. Delete Dojo

- Delete entire `dojo/` directory.
- Delete `install-dojo.xml` and its call in `install.xml`.
- Remove OWASP suppressions from A0.
- Remove Spotless exclusions.
- Clean up UnitTestResources copies.

---

## Track B — Unified React UI (Strategic, Multi-Release)

**Active product path (2026):** Pure React SPA (login-first) owns modern Home, Publish, Workflow,
Admin, Widget Builder, and Explorer. Entry is `spa.jsp?entry=…` (query contract); obsolete
product `*Modern.jsp` hosts were deleted in PR-8. Design: [`#000-pure-react-spa/`](../#000-pure-react-spa/).
Residual bridge embeds remain only for legacy full-page exits and dialog pilots.

### B0. Feature Inventory & REST API Gap Analysis

Catalog every feature across all 8 layers, the endpoints each uses, and identify where REST
endpoints are missing:

- **JSF Admin/Publishing** (40 pages): Managed bean logic → needs REST endpoints.
- **Desktop Content Explorer**: SOAP via JAX-WS → some REST equivalents may exist.
- **Package Manager**: GWT-RPC → needs REST endpoints.
- **Eclipse Workbench**: Content type/template/XML app CRUD → needs REST endpoints.

See [ui-layer-inventory.md](ui-layer-inventory.md) for the full feature-to-layer matrix and
API surface catalog.

### B1. React Application Architecture

- **Shell**: Single React SPA at `/cm/modern/` with sidebar navigation, breadcrumbs, tabbed content.
- **Feature modules**: Lazy-loaded React route modules per feature area.
- **Shared component library**: Tree (FancyTree wrapper), DataTable, Dialog, SplitPane, Menu, Forms.
- **API client**: Extend existing typed `api/client.ts`.
- **Auth/session**: Integrate with existing CSRF infrastructure (`window.OWASP_CSRFTOKEN`).
- **Feature flags**: Each module deployable independently with fallback to legacy UI.

### B2. Prioritized Migration Roadmap

Order based on: security risk, maintenance burden, feature overlap, user impact.

1. **GWT Package Manager** — smallest scope (~3 screens), GWT is dead technology
2. **CUI Contributor UI** — small scope (~8 widgets), high overlap with WebUI finder, Knockout unmaintained
3. **JSF Admin screens** — 12 pages, MyFaces+Trinidad ancient, exclusive features
4. **JSF Publishing screens** — 28 pages, largest JSF scope, exclusive features
5. **Rhythmyx jQuery screens** (from Track A) — now on jQuery, lower urgency
6. **WebUI Legacy jQuery/Backbone** — largest scope (~20 views), actively maintained, lowest risk
7. **Desktop Content Explorer** — drop desktop model; web-based replacement
8. **Eclipse Workbench** — web-based content type designer, template assembler, XML app editor

---

## Verification

|          Track           |                                                                                                           How to Verify                                                                                                            |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Track A (per step)**   | Smoke test: AA (tree, menu, DnD, slot add/remove), Content Browser (folder nav, search, select), Relationship Editor, Workflow Actions, Content Editor fields. OWASP + CodeQL scans. `grep -r "dojo\." system/` = 0 hits after A6. |
| **Track B (per module)** | Vitest unit tests per React component. Integration tests against REST API. Manual QA comparing new vs legacy. WCAG 2.1 AA accessibility audit. Performance benchmarks.                                                             |

---

## Key Decisions

- **jQuery for Track A, React for Track B** — jQuery is tactical (eliminate scan alerts now);
  React is the strategic consolidation target. No intermediate framework.
- **No Dojo shim** — even shimmed, Dojo source files still trigger scanner alerts.
- **FancyTree reuse** — bridge widget used in jQuery screens (Track A) and eventually wrapped
  for React (Track B).
- **Synchronous XHR preserved in Track A** — `$.ajax({ async: false })` to minimize behavioral
  changes initially.
- **Eclipse Workbench → web-based** — Content Design, System Design, XML Server become
  admin-role React route modules.
- **REST API creation is the long pole** — Track B gated by REST endpoints for features behind
  JSF beans, SOAP, and GWT-RPC. Start API work early.

