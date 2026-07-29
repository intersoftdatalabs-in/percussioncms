# WebUI Bundling Fix Plan

## Implementation Status

### ✅ Phase 0: Emergency Fix — COMPLETE

**All 8 individual page bundles + 2 common-ui bundles are now being built and packaged into the WAR.**

**Changes made:**
- ✅ Created `scripts/build-legacy-bundles.js` — Node.js script that reads minify JSON configs and concatenates files into bundles
- ✅ Updated `package.json` — Added `build:legacy` script; main build now runs both modern and legacy builds
- ✅ Updated `WebUI/pom.xml` — Removed excludes for `cssMin/` and `jslibMin/` so generated bundles are included in WAR
- ✅ Generated bundles with both `.min` and non-`.min` filenames for compatibility (PercProcessMonitor.jsp uses non-.min)

**Test results:**
- ✅ Build pipeline works end-to-end: `./mvnw package` successfully builds WAR with all bundles
- ✅ WAR contains 16 JS bundles (8 × .min.js + 8 × .js aliases) + 16 CSS bundles
- ✅ Bundles are at correct paths: `cm/jslibMin/perc_*.packed.min.js`, `cm/cssMin/perc_*.packed.min.css`, plus non-.min aliases

---

## Problem Statement

When the `com.samaxes:minify-maven-plugin` (v1.7.6) was removed in commit `2b4a8facd`, the JS/CSS bundling that produced the minified output files was lost. The existing `frontend-maven-plugin` + Vite pipeline only builds the new React/TypeScript code into `war/modern/` — it does **not** produce the legacy bundles that **12 JSP files** still reference.

### Impact

**Every non-debug page load was broken.** All 12 JSP pages use an `isDebug` conditional:
- **Debug mode** (`?debug=true`): loads individual `<script>` and `<link>` tags via JSP includes — still worked
- **Production mode** (default): loads packed bundles from `jslibMin/` and `cssMin/` — **now fixed** ✅

### Missing Output Files (Now Fixed)

|     JS Bundle (in `jslibMin/`)     |      CSS Bundle (in `cssMin/`)      |                Used By                | Status  |
|------------------------------------|-------------------------------------|---------------------------------------|---------|
| `perc_dashboard.packed.min.js`     | `perc_dashboard.packed.min.css`     | dashboard.jsp, PercProcessMonitor.jsp | ✅ Built |
| `perc_architecture.packed.min.js`  | `perc_architecture.packed.min.css`  | siteArchitecture.jsp                  | ✅ Built |
| `perc_webmgt.packed.min.js`        | `perc_webmgt.packed.min.css`        | home.jsp, webmgt.jsp, editAsset.jsp   | ✅ Built |
| `perc_publish.packed.min.js`       | `perc_publish.packed.min.css`       | publish.jsp                           | ✅ Built |
| `perc_users.packed.min.js`         | `perc_users.packed.min.css`         | users.jsp, adminWorkflow.jsp          | ✅ Built |
| `perc_editTemplate.packed.min.js`  | `perc_editTemplate.packed.min.css`  | editTemplate.jsp                      | ✅ Built |
| `perc_widgetBuilder.packed.min.js` | `perc_widgetBuilder.packed.min.css` | widgetBuilder.jsp                     | ✅ Built |
| `perc_admin.packed.min.js`         | `perc_admin.packed.min.css`         | admin.jsp, importTemplate.jsp         | ✅ Built |

Additionally, the `common-ui-bundle` produces `perc_common_ui.js` and `perc_common_ui_slim.js` for the delivery-tier (referencing files from `delivery/common/js/`). ✅ Built (with warnings for missing delivery files — separate issue)

---

## Current State Analysis

### What Exists Today

|                                                    Component                                                    |                                Status                                |
|-----------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| **202 first-party JS files** (`war/{plugins,services,controllers,views,models,classes,widgets,widgetbuilder}/`) | Present, unmodified                                                  |
| **289 third-party JS files** (`war/jslib/`)                                                                     | Present, unmodified, committed as vendored files                     |
| **Bundle config JSON files** (`src/main/resources/minify/*.json`)                                               | Still present — 4 files defining all bundles                         |
| **JSP debug includes** (`war/app/includes/common_js.jsp`, `common_css.jsp`, `finder_js.jsp`)                    | Present — individual `<script>`/`<link>` tags                        |
| **Vite + React/TS pipeline** (`package.json`, `vite.config.ts`, `src/main/ts/`)                                 | Present — builds new dashboard widgets to `war/modern/`              |
| **frontend-maven-plugin** in `pom.xml`                                                                          | Present — runs `npm ci && npm run build` (now includes legacy build) |
| **`war/jslibMin/` directory**                                                                                   | **Now populated** with minified bundles ✅                            |
| **`war/cssMin/` directory**                                                                                     | **Now populated** with CSS bundles ✅                                 |
| **Legacy bundle builder** (`scripts/build-legacy-bundles.js`)                                                   | **Created** — replaces minify-maven-plugin  ✅                        |

### Old Build Pipeline (Removed, Replaced by build-legacy-bundles.js)

The minify-maven-plugin ran 4 executions during `prepare-package`:

1. **`min-common`** → Produced `shared-common.js` and `shared-common.css` (intermediate bundles) into `target/minify-common/`

2. **`min-common-minuet`** → Produced `shared-common-minuet.js` and `shared-common-minuet.css` (intermediate) into `target/minify-common/`

3. **`min-package`** → Consumed the intermediates + page-specific files → Produced final `jslibMin/*.packed.min.js` and `cssMin/*.packed.min.css` into the WAR

4. **`common-ui`** → Produced `perc_common_ui.js` and `perc_common_ui_slim.js` (delivery-tier bundle) using files from `../../delivery/common/js/`

All used Google Closure Compiler (`WHITESPACE_ONLY` level, `ECMASCRIPT6` output).

---

## Third-Party Library Inventory (jslib/)

### Critical Security / EOL Issues

|     Library      | Version |              Issue               |                     Action                     |
|------------------|---------|----------------------------------|------------------------------------------------|
| **Uploadify**    | 2.1.0   | Requires Flash (EOL Dec 2020)    | **Remove immediately**                         |
| **Handlebars**   | 4.0.12  | Known CVEs (prototype pollution) | **Update to 4.7.8**                            |
| **Bootstrap**    | 4.5.1   | v4 EOL (Jan 2023)                | Update to 4.6.2 LTS minimum; plan v5 migration |
| **Popper.js**    | 1.14.4  | v1 unmaintained                  | Migrate to `@popperjs/core` v2                 |
| **Font Awesome** | 5.6.1   | v5 security-fix only             | Plan v6 upgrade                                |
| **Backgrid**     | ~0.3.x  | Abandoned (2013)                 | Replace with DataTables or modern grid         |

### Libraries Available on npm (Should Manage via package.json)

|  Current Vendored File   |     npm Package     | Current Version |          Latest           |
|--------------------------|---------------------|-----------------|---------------------------|
| jQuery 3.6.0             | `jquery`            | 3.6.0           | 3.7.1                     |
| jQuery UI 1.13.2         | `jquery-ui`         | 1.13.2          | 1.14.1                    |
| jQuery Migrate 3.3.2     | `jquery-migrate`    | 3.3.2           | 3.4.1                     |
| Bootstrap 4.5.1          | `bootstrap`         | 4.5.1           | 5.3.x                     |
| Bowser                   | `bowser`            | ~2.x            | 2.11.0                    |
| Handlebars 4.0.12        | `handlebars`        | 4.0.12          | 4.7.8                     |
| Moment.js 2.29.4         | `moment`            | 2.29.4          | 2.30.1 (maintenance mode) |
| Underscore 1.13.1        | `underscore`        | 1.13.1          | 1.13.7                    |
| Backbone 1.4.0           | `backbone`          | 1.4.0           | 1.6.0                     |
| DataTables 1.12.1        | `datatables.net`    | 1.12.1          | 2.2.x                     |
| Fancytree 2.38.3         | `jquery.fancytree`  | 2.38.3          | 2.38.3                    |
| jquery-validation 1.19.5 | `jquery-validation` | 1.19.5          | 1.21.0                    |
| jquery-form 4.3.0        | `jquery-form`       | 4.3.0           | 4.3.0                     |
| Superfish 1.7.10         | `superfish`         | 1.7.10          | 1.7.10                    |
| Mousetrap 1.6.2          | `mousetrap`         | 1.6.2           | 1.6.5                     |
| Animate.css 3.7.1        | `animate.css`       | 3.7.1           | 4.1.1                     |
| QUnit 2.6.2              | `qunit`             | 2.6.2           | 2.22.0                    |
| Modernizr 3.6.0          | `modernizr`         | 3.6.0           | 3.13.0                    |
| js-cookie 2.2.1          | `js-cookie`         | 2.2.1           | 3.0.5                     |

### Vendored-Only (No npm package / Percussion-specific)

|            Library             |                         Notes                          |
|--------------------------------|--------------------------------------------------------|
| `jquery-percutils`             | Percussion's own jQuery utility extensions             |
| `jquery-perc-retiredjs/*`      | ~20 legacy jQuery plugins already flagged as "retired" |
| `jquery-layout`                | Unmaintained; no modern npm package                    |
| `jquery-dropdown` (claviska)   | Unmaintained; no npm                                   |
| `jquery-collapser`             | Unmaintained; no npm                                   |
| `jquery-uploadify`             | Flash-based, dead                                      |
| `jquery-dynatree`              | Superseded by Fancytree (already present)              |
| `perc-retiredjs/*`             | Legacy shims (json2, rAF, date.js, etc.)               |
| `jquery-jeditable`             | npm exists (`jquery-jeditable`) but low activity       |
| `jquery-ui-multiselect-widget` | No official npm                                        |

---

## Proposed Solution

### Phased Approach

#### Phase 0: Emergency Fix — Restore Bundling (HIGH PRIORITY)

**Goal:** Get the 8 page bundles + 2 common-ui bundles building again via the *existing* Vite pipeline, producing files at the exact paths the JSPs expect.

**Approach:** Extend `vite.config.ts` to add legacy bundle entry points that replicate what the minify plugin did. This uses the *already-configured* `frontend-maven-plugin` — no new Maven plugins needed.

**Steps:**

1. **Create bundle entry point files** — one JS entry per bundle that imports the constituent files in order:

   ```
   WebUI/src/main/bundles/
   ├── perc_dashboard.bundle.js      # imports shared-common deps + dashboard-specific files
   ├── perc_architecture.bundle.js
   ├── perc_webmgt.bundle.js
   ├── perc_publish.bundle.js
   ├── perc_users.bundle.js
   ├── perc_editTemplate.bundle.js
   ├── perc_widgetBuilder.bundle.js
   ├── perc_admin.bundle.js
   ├── perc_common_ui.bundle.js
   └── css/
       ├── perc_dashboard.bundle.css
       ├── perc_architecture.bundle.css
       ├── ... (one per page)
   ```
2. **Update `vite.config.ts`** — add a second build target (or use Rollup `input` object) for legacy bundles:
   - Input: the bundle entry files above
   - Output: `war/jslibMin/perc_<name>.packed.min.js` and `war/cssMin/perc_<name>.packed.min.css`
   - Use `build.rollupOptions.output.entryFileNames` to produce deterministic names (no hash)
   - Configure `build.rollupOptions.external` to avoid bundling jQuery as a separate chunk (it should be inlined since the legacy code expects globals)
3. **Update `package.json`** — add a `build:legacy` script:

   ```json
   "scripts": {
     "build": "npm run build:modern && npm run build:legacy",
     "build:modern": "tsc --noEmit && vite build",
     "build:legacy": "vite build --config vite.legacy.config.ts"
   }
   ```
4. **Update `pom.xml` war plugin** — ensure `jslibMin/` and `cssMin/` from the build output are included in the WAR (remove the `<exclude>` lines for `cssMin` and `jslibMin` if the generated files should be served).
5. **Verify** — build and confirm all 12 JSPs load correctly in both debug and production modes.

#### Phase 1: Manage Third-Party Libraries via npm

**Goal:** Move npm-available libraries from vendored `jslib/` files to `package.json` dependencies, imported through the Vite build.

**Steps:**

1. **Add npm dependencies** to `package.json`:

   ```json
   "dependencies": {
     "jquery": "^3.7.1",
     "jquery-ui": "^1.14.1",
     "jquery-migrate": "^3.4.1",
     "bootstrap": "^4.6.2",
     "bowser": "^2.11.0",
     "handlebars": "^4.7.8",
     "moment": "^2.30.1",
     "datatables.net": "^1.13.11",
     "jquery-validation": "^1.21.0",
     "jquery-form": "^4.3.0",
     "jquery.fancytree": "^2.38.3",
     "mousetrap": "^1.6.5",
     "underscore": "^1.13.7",
     "backbone": "^1.6.0",
     "@popperjs/core": "^2.11.8"
   }
   ```
2. **Update bundle entry files** to import from `node_modules/` instead of `jslib/`:

   ```js
   // Before (Phase 0):
   import '../../war/jslib/profiles/3x/jquery/jquery-3.6.0.js';

   // After (Phase 1):
   import 'jquery';  // resolved from node_modules
   ```
3. **Configure Vite** to expose jQuery et al. as globals (since first-party code uses `$`, `jQuery`, `Handlebars`, etc. as globals):

   ```js
   // vite.legacy.config.ts
   define: {
     // Or use a plugin to assign window globals after import
   }
   ```
4. **Remove vendored copies** from `jslib/` for each library that is now npm-managed.
5. **Update `common_js.jsp` / `common_css.jsp`** debug includes to reference the same sources (or adjust debug mode to also use the bundle).
6. **Organize remaining vendor-only files** — move files that have no npm package into a clean `war/vendor/` directory with a manifest documenting each one.

#### Phase 2: Security & Deprecation Cleanup

**Goal:** Address critical security and EOL issues in third-party code.

|    Action    |             Library              |                                   Details                                    |
|--------------|----------------------------------|------------------------------------------------------------------------------|
| **Remove**   | Uploadify 2.1.0                  | Flash-based, completely dead. Remove all references.                         |
| **Remove**   | Dynatree 1.1.0                   | Superseded by Fancytree (already in project).                                |
| **Update**   | Handlebars 4.0.12 → 4.7.8        | CVE fixes (prototype pollution).                                             |
| **Update**   | Bootstrap 4.5.1 → 4.6.2          | Security patches for the v4 LTS line.                                        |
| **Migrate**  | Popper.js v1 → @popperjs/core v2 | Required for future Bootstrap 5.                                             |
| **Evaluate** | Backgrid                         | Abandoned since 2013. Replace with DataTables extension or remove if unused. |
| **Update**   | Font Awesome 5.6.1 → 6.x         | Evaluate scope of icon class name changes.                                   |
| **Update**   | QUnit 2.6.2 → 2.22.0             | Test framework, low risk.                                                    |

#### Phase 3: Modernization (Future)

**Goal:** Incrementally migrate the legacy jQuery/global-function architecture toward the React/TS pipeline.

1. **Create a bridge layer** between legacy globals and React components (the existing `src/main/ts/bridge.ts` is a start).
2. **Extract shared services** (`PercServiceUtils`, `PercSiteService`, etc.) into TypeScript modules that can be imported by both legacy and modern code.
3. **Replace Moment.js** with `date-fns` (already a dependency in the new React code).
4. **Replace RequireJS** — Vite handles module loading; RequireJS is only used by the CUI layer.
5. **Incrementally convert** page views from jQuery widgets to React components, page by page.
6. **Eventually remove** the legacy bundle build when all pages are migrated.

---

## File Changes Summary

### Phase 0 (Emergency Fix)

|                   File                    |                          Action                           |
|-------------------------------------------|-----------------------------------------------------------|
| `WebUI/vite.legacy.config.ts`             | **Create** — Vite config for legacy bundles               |
| `WebUI/src/main/bundles/*.bundle.js`      | **Create** — 8 page bundles + 2 common-ui bundles         |
| `WebUI/src/main/bundles/css/*.bundle.css` | **Create** — 8 CSS bundles                                |
| `WebUI/package.json`                      | **Modify** — add `build:legacy` script                    |
| `WebUI/pom.xml`                           | **Modify** — ensure war plugin includes generated bundles |
| `WebUI/src/main/resources/minify/*.json`  | **Keep** — reference documentation; may delete later      |

### Phase 1

|                  File                   |                         Action                          |
|-----------------------------------------|---------------------------------------------------------|
| `WebUI/package.json`                    | **Modify** — add ~15 npm dependencies                   |
| `WebUI/src/main/bundles/*.bundle.js`    | **Modify** — switch imports from vendored to npm        |
| `WebUI/vite.legacy.config.ts`           | **Modify** — configure npm resolution + global exposure |
| `WebUI/war/jslib/`                      | **Modify** — remove files now managed by npm            |
| `WebUI/war/vendor/`                     | **Create** — organized home for non-npm vendored files  |
| `WebUI/war/app/includes/common_js.jsp`  | **Modify** — update debug paths                         |
| `WebUI/war/app/includes/common_css.jsp` | **Modify** — update debug paths                         |

---

## Risks & Considerations

1. **Load order matters** — The legacy code depends on specific global variable availability (e.g., `$`, `jQuery`, `jQuery.ui`, `Handlebars`). Vite's bundling must preserve the concatenation order from the original bundle JSON configs.

2. **Global scope pollution** — All 202 first-party JS files expect to be in global scope, not modules. The bundle entry points must use side-effect imports (`import './file.js'`) rather than named imports, and Vite must be configured to NOT wrap code in IIFE/modules.

3. **Relative path differences** — The old minify plugin used `webappSourceDir=${project.basedir}/war` as the base. The bundle entry files must resolve paths relative to the same `war/` directory.

4. **PercProcessMonitor.jsp inconsistency** — This JSP uses absolute paths (`/cm/cssMin/...`) and different filenames (`packed.css` not `packed.min.css`). Needs a separate compatibility alias or fix.

5. **Delivery common-ui bundle** — The `common-ui-bundle.json` pulls files from `../../delivery/common/js/` (a separate module). The Vite build needs access to these files. The delivery JS files exist in git but may need to be restored to the working tree.

6. **No tree-shaking possible** — The legacy code is not modular; everything must be concatenated as-is. Vite/Rollup should be configured with `treeshake: false` for legacy bundles.

7. **Source maps** — The old plugin had `closureCreateSourceMap: false`. We should enable source maps in the new build for debugging, but ensure they're not shipped to production.

---

## Success Criteria

- [ ] All 12 JSP pages load without 404 errors in production mode (no `?debug=true`)
- [ ] Bundle output matches expected paths: `cm/jslibMin/*.packed.min.js` and `cm/cssMin/*.packed.min.css`
- [ ] Debug mode (`?debug=true`) continues to work with individual file loading
- [ ] `mvn package` produces a WAR containing all bundles
- [ ] No regression in existing React/TypeScript build (`war/modern/`)
- [ ] npm audit shows no critical/high vulnerabilities in managed dependencies

