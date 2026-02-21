# Modern UI Replacement Plan (Draft)

## Problem / Goal
Replace the legacy JSP/JS UI (WebUI + cui) with a modern TypeScript UI (React or Angular), while running both side-by-side for a beta period before deprecating the old UI.

## Current State (from codebase)

### Layer 1 — CM1 UI (`WebUI/` + `cui/`)

- **WebUI**: Maven WAR (`perc-web-ui`) with many JSP entry points under `WebUI/war/app` and supporting assets under `WebUI/war`. Uses jQuery 3.6, Backbone 1.3, jQuery UI. Minified by Google Closure Compiler (`minify-maven-plugin`). No npm/Node build.
- **cui**: Legacy UI assets using RequireJS/Knockout/jQuery/Bootstrap 3; `cui/package.json` exists but has no build scripts.
- **projects/sitemanage**: Backend REST services and DTOs; referenced by WebUI in its WAR.
- **URL path**: `/cm/app/*`

### Layer 2 — Shindig Dashboard (OpenSocial gadgets)

- **Apache Shindig 3.0.0-beta4** (retired/unmaintained) with ancient dependencies (Guice 2.0, Google Collections 1.0-rc2, Caja, OAuth 20100527).
- 23 XML-defined OpenSocial gadgets under `system/Packages/perc.gadget.*/`.
- Main JS controller: `WebUI/war/views/PercDashboard.js` (1,628 lines).
- REST endpoints: `/services/dashboardmanagement/dashboard/`, `/services/activity/*`.
- **Security risk: HIGH** — proxy servlets, iframe rendering, all unmaintained.
- **URL paths**: `/cm/gadgets/*`, `/cm/social/*`

### Layer 3 — Legacy Rhythmyx Admin / Content Editor

- **JSF** via Apache MyFaces 4.1.2 + Trinidad 2.2.1 + Tomahawk for admin and publishing design pages.
- **XSL→HTML** content editor with embedded **Dojo** (pre-1.0, ~1,477 static JS files) for active assembly, relationship editor, search.
- JSPs under `system/ear/jsps/ui/admin/`, `system/ear/jsps/ui/publishing/`, `system/ear/jsps/ui/content/`.
- Three `faces-config.xml` files govern navigation rules.
- **URL paths**: `/Rhythmyx/ui/admin/console.faces`, `/Rhythmyx/ui/publishing/SiteList.faces`, `/Rhythmyx/*`, `/Designer/*`
- **Security risk: MEDIUM** — MyFaces is current, but Dojo is ancient and unmaintained.

## Proposed Approach (high level)

Create a new **TypeScript SPA** under a new module (recommended: `modules/firehorse`), build it with a modern bundler (Angular CLI), and publish compiled assets into the main webapp under a **new context path** (e.g., `/cm/fh`). Keep the legacy UI intact and introduce a **feature-flagged routing switch** so users can opt into the new UI during beta. The SPA must eventually cover all three UI layers, including the Rhythmyx admin/content editor.

## Workplan

- [ ] **Confirm scope/choices**: We pick Angular, choose beta target areas (Home + Site Manage + Dashboard), /cm/fh, and decide on feature-flagging method (config via flag in server properties / url path).
- [ ] **Inventory legacy UI**: map JSP entry points, RequireJS modules, and key REST endpoints used by WebUI/cui; identify the first few pages to port.
- [ ] **Create new module**: scaffold `modules/modern-ui` (or `WebUI/war/modern` if preferred) with TypeScript, routing, state management, and test setup (unit + e2e). Use existing UI conventions where possible.
- [ ] **Build + packaging integration**: add a Maven build step (e.g., `frontend-maven-plugin` or `maven-exec`) to run `npm ci && npm run build`, then copy the compiled assets to the WAR (e.g., `WebUI/war/modern/`). Ensure cache-busting asset names.
- [ ] **Auth/CSRF integration**: reuse existing session cookies and CSRFGuard token (`/JavaScriptServlet`) from `WebUI/WEB-INF/web.xml`; document and implement token injection for API calls.
- [ ] **Typed API layer**: create a typed client for `rest` + `sitemanage` endpoints; add a compatibility layer or new endpoints only if gaps are found.
- [ ] **Side-by-side routing**: add a feature flag + entry-point routing so `/cm/app` (legacy) and `/cm/app-next` (modern) can run in parallel; add a UI toggle to switch back.
- [ ] **Incremental migration**: port selected pages/modules iteratively, keeping functional parity and fallbacks to legacy UI.
- [ ] **Beta + deprecation**: add telemetry/error reporting, run regression tests, release as beta, then schedule legacy UI deprecation and cleanup.
- [ ] **Shindig dashboard replacement**: Replace the retired Apache Shindig 3.0.0-beta4 gadget container with native SPA dashboard widgets; remove Shindig module and all proxy servlets to eliminate high-security-risk dependencies.
- [ ] **Rhythmyx admin UI migration**: Port JSF/MyFaces/Trinidad admin and publishing design pages (`/Rhythmyx/ui/admin/`, `/Rhythmyx/ui/publishing/`) to the new SPA. Remove MyFaces, Trinidad, and Tomahawk dependencies once all `.faces` pages are eliminated.
- [ ] **Dojo content editor replacement**: Replace XSL→HTML + Dojo content editor (active assembly, relationship editor, search) with SPA equivalents. Remove ~1,477 static Dojo JS files from `system/cms/content/applications/sys_resources/`.

## Notes / Considerations

- Keep all changes **JDK 21 compatible** and Maven-driven; do not introduce Spring Boot.
- Maintain OWASP protections (CSRF, security headers) and reuse existing filters.
- Consider creating a shared design system package for consistent styling between old and new UIs.
- The legacy Rhythmyx admin UI (MyFaces + Trinidad + Dojo) must also be addressed — it is admin-facing but contains security-sensitive publishing and content editing workflows.
- Shindig (OpenSocial gadget container) is a top security priority — it is retired and bundles ancient dependencies (Guice 2.0, Google Collections 1.0-rc2, Caja, OAuth 20100527).
- Dojo (pre-1.0) used in the content editor is unmaintained and should be replaced, but the active assembly / relationship editor pages are complex and may need a dedicated design spike.

## Assumptions to Confirm

1. “csui” refers to `cui` (not present as a separate module).
2. Preferred framework (React vs Angular) and the initial beta scope.
3. Desired **new UI URL path** and feature-flag mechanism.
4. Whether the new UI should be a **single SPA** or multiple independently deployed apps.
5. Priority order for Rhythmyx admin pages — which faces pages are most critical to migrate first?
6. Whether the Dojo-based content editor (active assembly) should be migrated within the SPA or rebuilt as a separate focused effort.

## See Also

- [Plan B — Incremental UI Modernization](plan-b-incremental.md): Alternate approach that merges cui into WebUI, adds a build pipeline, and replaces components incrementally without a parallel SPA rewrite.
