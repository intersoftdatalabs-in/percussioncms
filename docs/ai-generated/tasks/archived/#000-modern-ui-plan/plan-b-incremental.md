# Incremental UI Modernization Plan (Plan B — Draft)

## Problem / Goal
Modernize the Percussion CMS UI **incrementally**, without a parallel rewrite, by consolidating the existing frontend codebase, introducing a proper build pipeline, and progressively replacing legacy widgets and pages with React + TypeScript components — starting with the highest-risk area (the Shindig-based dashboard).

## Guiding Philosophy
*Incremental improvement over big-bang rewrite.*  Every change ships production-ready code. Legacy pages continue to work during migration; new components are introduced one at a time within the existing WAR.

---

## Current State — All Three UI Layers

Percussion CMS has **three distinct UI layers**, each with different technology and risk profiles:

### Layer 1 — CM1 UI (`WebUI/` + `cui/`)
| Aspect | Detail |
|---|---|
| **Framework** | jQuery 3.6, Backbone 1.3, jQuery UI (WebUI); RequireJS + Knockout.js + Bootstrap 3 (cui) |
| **Entry points** | JSP files under `WebUI/war/app/` (dashboard.jsp, admin.jsp, webmgt.jsp, siteArchitecture.jsp, etc.) |
| **Bundling** | **None.** WebUI uses Google Closure Compiler via `minify-maven-plugin`; cui uses AMD modules loaded at runtime by RequireJS |
| **URL path** | `/cm/app/*` |
| **Build** | Maven WAR (`perc-web-ui`); no npm/Node step |

### Layer 2 — Shindig Dashboard (OpenSocial gadgets)
| Aspect | Detail |
|---|---|
| **Framework** | Apache Shindig 3.0.0-beta4 (OpenSocial 2.x), Guice 2.0, Google Collections 1.0-rc2, Caja r3828, OAuth 20100527 |
| **Gadgets** | 23 XML-defined OpenSocial gadgets under `system/Packages/perc.gadget.*/` |
| **JS controller** | `WebUI/war/views/PercDashboard.js` (1,628 lines) |
| **REST endpoints** | `/services/dashboardmanagement/dashboard/`, `/services/dashboardmanagement/gadget/`, `/services/activity/*` |
| **Security risk** | **HIGH.** Shindig is retired/unmaintained. Shaded dependencies include Guice 2.0, Google Collections 1.0-rc2, and ancient OAuth/Caja. Attack surface includes proxy servlets (`makeRequest`, `concat`) and iframe rendering. |
| **URL paths** | `/cm/gadgets/*`, `/cm/social/*` |

### Layer 3 — Legacy Rhythmyx Admin / Content Editor
| Aspect | Detail |
|---|---|
| **Framework** | JSF via **Apache MyFaces 4.1.2** + **Trinidad 2.2.1** + Tomahawk; content editor uses **XSL→HTML** with embedded **Dojo** (pre-1.0, ~1,477 static JS files) |
| **Entry points** | JSPs under `system/ear/jsps/ui/admin/`, `system/ear/jsps/ui/publishing/`, `system/ear/jsps/ui/content/`; XSL templates under `system/cms/content/applications/` |
| **URL paths** | `/Rhythmyx/ui/admin/console.faces`, `/Rhythmyx/ui/publishing/SiteList.faces`, `/Rhythmyx/*`, `/Designer/*` |
| **Dojo usage** | Loaded from `/sys_resources/dojo/dojo.js`, used in active assembly, relationship editor, search, single-field editor. All AMD `dojo.provide`/`dojo.require` style. |
| **Build** | Maven; Dojo and MyFaces assets are static resources (no JS build step) |
| **Security risk** | **MEDIUM.** MyFaces itself is current (4.1.2), but Dojo is ancient and unmaintained. Trinidad 2.2.1 is niche but still receives patches. |

### `package.json` Inventory
| Path | Status | Action |
|---|---|---|
| `cui/package.json` | Real — defines runtime dependencies but no build scripts | **Keep, upgrade to real build** |
| `WebUI/war/jslib/profiles/3x/package.json` | Vendored upstream metadata for jQuery 3.x | **Remove** (vendored) |
| `modules/perc-tinymce/package.json` | Real — modern esbuild pipeline | **Keep as-is** |
| `WebUI/war/jslib/profiles/3x/libraries/bowser/package.json` | Vendored upstream | **Remove** |
| Various under `WebUI/war/jslib/profiles/3x/libraries/` | Vendored upstream | **Remove** |

---

## Proposed Approach

### Phase 0 — Foundation (Consolidate + Build Pipeline)

**Goal:** Merge `cui/` into `WebUI/`, introduce `frontend-maven-plugin` for the combined module, and establish a TypeScript/React build alongside the existing JS.

#### 0.1 — Merge `cui/` into `WebUI/war/cui/`
- Move `cui/components/`, `cui/pages/`, `cui/widgets/` into `WebUI/war/cui/`.
- Update RequireJS config paths in `WebUI/war/` so existing AMD modules resolve from the new location.
- Remove `cui/package.json` (its dependencies move to the unified `package.json`).
- Delete the top-level `cui/` directory.
- **Validation:** All existing CM1 pages load unchanged.

#### 0.2 — Clean up vendored `package.json` files
These files are metadata that was used to manually track the version of the library in use in the code base.  The hope was that they coould be used to automate updates, but they are not actually used in any build process and are easily confused with real build definitions.  They should be removed to avoid confusion.  We do need to migrate the actual list of dependencies into the new unified `package.json` for the React build, but these vendored files are not the right place for that information.  The 3.x profile is the current one, so we should take the dependencies from there and add them to the new `package.json` as runtime dependencies (e.g., `jquery`, `backbone`, `knockout`, `bootstrap`, etc if there is an npm package available), but the vendored metadata files themselves should be removed. Vendored scripts that still have no npm package available will have to be accounted for if they are actively in use. This includes:
- `WebUI/war/jslib/profiles/3x/package.json`
- `WebUI/war/jslib/profiles/3x/libraries/bowser/package.json`
- Any other `package.json` files under `WebUI/war/jslib/profiles/3x/libraries/`
- Remove `WebUI/war/jslib/profiles/3x/package.json` and any other vendored `package.json` files.
- These are upstream library metadata, not project build definitions.

#### 0.3 — Add `frontend-maven-plugin` to `WebUI/pom.xml`
- Configure `frontend-maven-plugin` (already in parent POM at v1.15.1) in `WebUI/pom.xml`:
  - Install Node 20 LTS + npm
  - Run `npm ci` (install)
  - Run `npm run build` (compile TypeScript/React → `WebUI/war/modern/`)
- Create `WebUI/package.json` with:
  - `react`, `react-dom`, `typescript`
  - `vite` (or `webpack 5`) as bundler
  - `@types/*` packages
  - Dev: `vitest` + `@testing-library/react` for unit tests
  - Dev: `eslint`, `prettier`
- Create `WebUI/tsconfig.json` with strict mode.
- Output bundle to `WebUI/war/modern/` (hashed filenames for cache-busting).
- **Existing JS is untouched** — the new build is additive.

#### 0.4 — Create React mount-point infrastructure
- Add a thin "bridge" layer: a JSP or HTML snippet that renders a `<div id="react-root">` and loads the bundled JS.
- Create a reusable `mountReactComponent(elementId, Component, props)` helper so React components can be embedded into existing jQuery/Backbone pages without a full SPA takeover.
- Implement CSRF token injection: read the OWASPCSRFGuard token from `/JavaScriptServlet` and add it to all API calls via an Axios/fetch interceptor.
- Create a typed API client module (`src/api/`) with auto-generated types from the existing REST DTOs (JAX-RS `@Path` classes in `projects/sitemanage`).

### Phase 1 — Replace Shindig Dashboard (Priority: Security)

**Goal:** Remove the entire Shindig gadget container and replace it with a React + TypeScript dashboard that reuses the existing REST endpoints.

#### 1.1 — Build new Dashboard component
- Create `WebUI/src/dashboard/` with:
  - `Dashboard.tsx` — main two-column layout (matching current gadget columns)
  - Individual widget components replacing each gadget:
    - `WelcomeWidget.tsx` — static welcome content + links
    - `WorkflowStatusWidget.tsx` — calls `/services/dashboardmanagement/gadget/` + workflow APIs
    - `ActivityWidget.tsx` — calls `/services/activity/contentactivity`
    - `ContentTrafficWidget.tsx` — calls `/services/activity/contenttraffic`
    - `ProcessMonitorWidget.tsx` — calls process monitor APIs
    - `ReportsWidget.tsx` — calls report APIs
  - `DashboardLayout.tsx` — drag-and-drop grid (e.g., `react-grid-layout`)
  - `useDashboardConfig.ts` — hook to load/save user's layout via `/services/dashboardmanagement/dashboard/`
- Port the 6-8 most-used gadgets first; stub remaining gadgets with "Coming soon" placeholders.
- Match existing visual design initially; defer redesign to a later phase.

#### 1.2 — Replace `dashboard.jsp` entry point
- Update `WebUI/war/app/dashboard.jsp` to load the React bundle instead of `PercDashboard.js` + Shindig iframes.
- Old dashboard code (`PercDashboard.js`, Shindig servlet config) remains in the codebase but is no longer loaded.
- Feature flag: server property + URL param (`?legacyDashboard=true`) to fall back during beta.

#### 1.3 — Remove Shindig dependency
- After beta period, remove:
  - `modules/shindig-uber/` module
  - Shindig servlet/filter/listener config from `WebUI/war/WEB-INF/web.xml`
  - Shindig properties from `WebUI/war/WEB-INF/classes/shindig.properties`
  - Guice module references
  - OpenSocial gadget XML files under `system/Packages/perc.gadget.*/`
- **Security payoff:** Removes Shindig 3.0.0-beta4, Guice 2.0, Google Collections 1.0-rc2, Caja, ancient OAuth, and all proxy servlets.

### Phase 2 — Incremental Page Migration (JS → TypeScript/React)

**Goal:** Convert existing CM1 pages one by one from jQuery/Backbone/Knockout to React + TypeScript, reusing the infrastructure from Phase 0.

#### 2.1 — Establish migration pattern
- For each page:
  1. Create a new React component under `WebUI/src/pages/<PageName>/`
  2. Type the REST calls used by the page (from the `src/api/` layer)
  3. Replace the JSP's body content with a React mount-point (`<div id="page-root">`)
  4. Keep the JSP's outer shell (header/nav/footer) intact initially
  5. Test: unit tests (vitest) + manual regression
- Start with **low-traffic, self-contained pages** to build confidence:
  - Admin settings pages
  - User profile / preferences
  - Blog management

#### 2.2 — Migrate shared navigation / chrome
- Once several pages are migrated, extract the header/sidebar/footer into React components.
- Convert the shared navigation layout from jQuery/JSP to a React shell.
- Legacy pages that haven't been converted yet render inside the shell via an `<iframe>` or JSP-include bridge.

#### 2.3 — Migrate core pages
- Site architecture / page editor
- Template editor
- Web management (publishing, preview)
- Content explorer (heaviest page — schedule separately)

### Phase 3 — Legacy Rhythmyx Admin UI Modernization

**Goal:** Reduce the JSF/Dojo attack surface incrementally. This layer is lower-traffic (admin-only) but contains security-sensitive functionality.

#### 3.1 — Inventory and triage Rhythmyx pages
Map all `*.faces` pages and Dojo-dependent features:

| Area | Pages | Framework | Priority |
|---|---|---|---|
| Admin Console | `console.faces`, `ScheduledTask*.faces` | JSF + Trinidad | Medium |
| Publishing Design | `SiteList.faces`, `EditionEditor.faces`, etc. (28 JSPs) | JSF + Trinidad | Medium |
| Content Browser | `ContentBrowserDialog.jsp`, `CreateItem.jsp`, etc. | JSF + Trinidad | Lower (used less directly) |
| Active Assembly | XSL→HTML + Dojo | XSL + Dojo | High (security) |
| Relationship Editor | `rceditor.xsl` + Dojo | XSL + Dojo | High (security) |
| Search | `getQuery.xsl` + Dojo | XSL + Dojo | Medium |

#### 3.2 — Replace Dojo-dependent pages first (security priority)
- Active Assembly and the Relationship Editor are the most Dojo-heavy.
- Create React replacements that call the same XML Application / REST endpoints.
- Serve new pages at the existing `/Rhythmyx/*` paths via a servlet forward or URL rewrite.
- Remove Dojo JS files from `system/cms/content/applications/sys_resources/` once all consumers are migrated.

#### 3.3 — Migrate JSF admin pages
- Publishing Design pages (`SiteList`, `EditionEditor`, etc.) are CRUD forms — straightforward React conversions.
- Admin Console pages (`console.faces`, scheduled tasks) — convert to React views served from the same WAR.
- Approach: Replace each `*.faces` page with a React-rendered page behind a new servlet endpoint; update navigation rules to point to the new endpoint.
- MyFaces, Trinidad, and Tomahawk dependencies can be removed once all `*.faces` pages are eliminated.

#### 3.4 — Timeline consideration
- Rhythmyx admin pages are internal/admin-facing; they can be migrated on a slower cadence than customer-facing CM1 pages.
- Consider gating this phase on completing the majority of Phase 2 first.

### Phase 4 — Polish and Cleanup

#### 4.1 — Unified design system
- Extract shared React components into a design system package (`WebUI/src/design-system/`).
- Ensure consistent theming, accessibility (WCAG 2.1 AA), and responsive behavior across all migrated pages.

#### 4.2 — Remove legacy JS frameworks
- Once all pages are migrated, remove:
  - jQuery, Backbone, jQuery UI (from WebUI)
  - RequireJS, Knockout, Bootstrap 3 (from former cui)
  - Dojo (from system/cms)
  - MyFaces, Trinidad, Tomahawk (from system/ear)
  - Google Closure Compiler minification step (replaced by Vite/webpack)
- Update `WebUI/pom.xml` to remove `minify-maven-plugin`.

#### 4.3 — Full TypeScript coverage
- Enable `strict: true` in `tsconfig.json` from the start.
- Add ESLint rules that warn on remaining `.js` files.
- Target: 100% TypeScript for all new code; progressively convert remaining JS.

---

## Technical Details

### Build Pipeline
```
WebUI/pom.xml
  ├── frontend-maven-plugin (generate-resources phase)
  │   ├── install-node-and-npm  → Node 20 LTS
  │   ├── npm ci                → install deps
  │   └── npm run build         → vite build → WebUI/war/modern/
  ├── minify-maven-plugin       → legacy JS minification (existing, unchanged)
  └── maven-war-plugin          → packages everything into WAR
```

### Directory Structure (after Phase 0)
```
WebUI/
├── package.json           ← NEW: unified dependencies
├── tsconfig.json          ← NEW: strict TypeScript config
├── vite.config.ts         ← NEW: Vite bundler config
├── src/                   ← NEW: all TypeScript/React source
│   ├── api/               ← typed REST client
│   ├── dashboard/         ← Phase 1: dashboard widgets
│   ├── pages/             ← Phase 2: migrated pages
│   ├── design-system/     ← Phase 4: shared components
│   ├── bridge.ts          ← mount-point helper for embedding React in JSPs
│   └── index.ts           ← main entry point
├── war/
│   ├── app/               ← existing JSPs (unchanged)
│   ├── cui/               ← merged from top-level cui/
│   ├── modern/            ← NEW: Vite build output (hashed bundles)
│   ├── jslib/             ← existing vendored JS libs
│   ├── views/             ← existing JS views
│   ├── widgets/           ← existing JS widgets
│   └── WEB-INF/
│       └── web.xml        ← existing (Shindig config removed in Phase 1.3)
└── pom.xml                ← updated with frontend-maven-plugin
```

### CSRF / Auth Integration
- The existing OWASP CSRFGuard setup injects a token via `/JavaScriptServlet`.
- The React API layer will:
  1. Read the CSRF token from the `<script>` tag injected by CSRFGuard.
  2. Attach it as a header (`OWASP-CSRFTOKEN`) on every API request via an Axios/fetch interceptor.
  3. Reuse the existing session cookie for authentication (no changes to auth flow).

### Feature Flags
- Server property: `percussion.ui.modernDashboard=true|false` (default `true` after beta).
- URL fallback: `?legacyDashboard=true` on `dashboard.jsp` forces old Shindig dashboard.
- Same pattern extends to other pages as they are migrated: `?legacy=true` falls back.

---

## Risk Analysis

| Risk | Mitigation |
|---|---|
| Merged cui breaks existing RequireJS paths | Automated integration tests on all CM1 pages before/after merge |
| React bundle bloats page load time | Code-split by page; lazy-load dashboard widgets; Vite tree-shaking |
| Shindig removal breaks gadget customizations | Document migration path for each gadget; provide config migration script |
| Dojo removal breaks Active Assembly | Thorough QA on content editing workflow; feature-flag rollback |
| MyFaces removal breaks admin workflows | Parallel run: new React pages at same URL, fallback to `.faces` via param |
| Build pipeline slows CI | Cache Node/npm in CI; Vite builds are fast (~5s typical) |

## Comparison with Plan A (Full SPA Rewrite)

| Dimension | Plan A (Full SPA) | Plan B (Incremental) |
|---|---|---|
| **First deliverable** | Months (full SPA scaffold + auth + first pages) | Weeks (build pipeline + dashboard replacement) |
| **Risk** | High (big-bang cutover, dual maintenance) | Low (each phase ships independently) |
| **Shindig removal** | Blocked until SPA is ready | Phase 1 priority — can ship within first sprint |
| **Legacy Rhythmyx UI** | Not addressed | Explicitly phased (Phase 3) |
| **Code duplication** | Two codebases during beta | Single codebase; React components coexist with legacy |
| **Team ramp-up** | Must learn full SPA architecture upfront | Learn React incrementally, one component at a time |
| **Final state** | Clean SPA | Same clean state, reached incrementally |

---

## Rough Phasing / Timeline Estimates

| Phase | Scope | Estimated Effort |
|---|---|---|
| **Phase 0** | Build pipeline + cui merge + React infrastructure | 2–3 sprints |
| **Phase 1** | Shindig dashboard replacement | 2–3 sprints |
| **Phase 2** | CM1 page migration (iterative) | 6–12 sprints (ongoing) |
| **Phase 3** | Rhythmyx admin UI migration | 4–8 sprints (can overlap Phase 2) |
| **Phase 4** | Cleanup + design system polish | 2–3 sprints |

Total: ~16–29 sprints, but **production value delivered from Phase 0 onward** — no waiting for a big-bang release.

---

## Open Questions
1. **React vs. Vue:** This plan uses React; Vue is a viable alternative with smaller bundle size and gentler learning curve. Decision should factor team familiarity.
2. **Vite vs. Webpack:** Vite recommended for speed, but Webpack 5 has broader ecosystem if needed.
3. **Dashboard widget priority:** Which of the 23 gadgets are actually used in production? Usage telemetry would help prioritize Phase 1.1.
4. **Active Assembly Dojo replacement:** This is the most complex Dojo page — may need its own dedicated design spike.
5. **MyFaces removal timeline:** Is there organizational appetite to remove JSF, or should Phase 3 be deferred?
