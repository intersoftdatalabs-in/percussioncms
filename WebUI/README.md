# WebUI

This module contains the support for the User Interface for CMS.

## Building

```bash
cd WebUI
../mvnw clean install
```

### Frontend-only (Vite / Vitest / ESLint)

Maven uses `WebUI/src/main/frontend` as the npm working directory
(`frontend-maven-plugin`). Prefer that cwd for day-to-day frontend work:

```bash
cd WebUI/src/main/frontend
npm ci
npm run build:modern   # tsc --noEmit + vite
npm run test           # Vitest
npm run lint           # ESLint 10 flat config → product sources in ../ts
```

The WebUI module root also has a twin `package.json` / `eslint.config.mjs`
for developers who run from `WebUI/`:

```bash
cd WebUI
npm run lint           # eslint src/main/ts
```

#### ESLint notes (issue #1593)

* Flat config only (`eslint.config.mjs`). No `.eslintrc*`.
* ESLint **10** — drop obsolete CLI flags such as `--ext`.
* Scope: product React/TypeScript under `src/main/ts` (not legacy `webapp` JS).
* Parser: `@babel/eslint-parser` with TypeScript + JSX plugins.
  **typescript-eslint** currently hard-rejects TypeScript 7.x (this module uses
  `typescript@^7`); switch when typescript-eslint supports TS ≥7.1
  (typescript-eslint#10940).
* Baseline: `react-hooks/rules-of-hooks` is error; noisy Babel-without-types
  rules (`no-unused-vars`, `set-state-in-effect`, etc.) are off/warn so
  `npm run lint` exits 0. Tighten later with type-aware lint.
* Lint is not yet bound in `frontend-maven-plugin` (dev/agent gate via npm).

## Layout

Most ui elements are located under the war folder. When deployed they are placed into the main application war file under the cm folder.

war/

## UI Themes (`src/main/ts/ui-themes/`)

The modern React layer ships a pluggable theme system. The default theme is
**Intersoft Data Labs** (sampled from `https://intsof.com/`). The product
wordmark remains **Percussion CMS**; the theme only aligns the chrome,
colors, and typography with the Intersoft brand.

* Tokens: `ui-themes/intersoft/intersoftTheme.ts` (colors, type, spacing, brand)
* React context: `ui-themes/ThemeProvider.tsx` (injects CSS custom properties)
* Branded chrome: `ui-themes/components/Branding.tsx` (`<BrandBar />`, `<BrandFooter />`)
* Brand assets: `src/main/webapp/cm/themes/intersoft/brand/`
* Per-theme guide + customisation: `ui-themes/README.md`

## Modern Publishing UI (feature 990)

Primary nav `view=publish` redirects (PR-5) to `/cm/app/spa.jsp?entry=publish` (query contract); React `PublishingShell` mounts inside the SPA shell. Product host `publishModern.jsp` was removed in PR-8.

### Query parameters (allowlisted)

|   Param    |                         Purpose                          |
|------------|----------------------------------------------------------|
| `section`  | `sites` (default), `status`, `logs`, `design`, `runtime` |
| `siteId`   | Preselect site (safe charset)                            |
| `serverId` | Preselect server                                         |

Classic Minuet `publish.jsp` and JSF `/ui/publishing`, `/ui/pubruntime` entries redirect to this shell.

Spec/plan/tasks: `specs/990-unified-publishing-ui/`.

## Modern Content Explorer (feature 992)

Modern React replacement for the legacy Miller-column Finder and the Desktop Content Explorer (DCE). New entry points mount `ContentExplorerShell` (US1 core navigate), plus per-feature panels for menus (US3), folder security (US4), search (US5), and advanced CE tools (US7). New reusable `ContentBrowser` dialog (US2) replaces the per-host asset / page / folder pickers.

### Entry points

|            Surface             |                URL                |                                              Component(s)                                              |
|--------------------------------|-----------------------------------|--------------------------------------------------------------------------------------------------------|
| **SPA product explorer**       | `cm/app/spa.jsp?entry=explorer`   | `ContentExplorerShell` (primary product path)                                                          |
| Residual ContentBrowser dialog | `cm/app/assetPickerModern.jsp`    | `ContentBrowser` (asset-only, single-select)                                                           |
| Residual ContentBrowser dialog | `cm/app/pagePickerModern.jsp`     | `ContentBrowser` (page-only, single-select)                                                            |
| Residual ContentBrowser dialog | `cm/app/folderPickerModern.jsp`   | `ContentBrowser` (folder-only, single-select)                                                          |
| Residual action menu pilot     | `cm/app/actionMenuModern.jsp`     | `ContextMenu` + `ActionToolbar`                                                                        |
| Residual folder security       | `cm/app/folderSecurityModern.jsp` | `FolderSecurityPanel`                                                                                  |
| Residual search panel          | `cm/app/searchModern.jsp`         | `SearchPanel`                                                                                          |
| Residual advanced CE pilot     | `cm/app/us7AdvancedModern.jsp`    | `ClipboardPanel` + `SiteCopyWizard` + `SubfolderCopyWizard` + `DependencyViewer` + `RelationshipsView` |

Product shell hosts (`explorerModern.jsp`, `homeModern.jsp`, etc.) were **removed in PR-8**. Residual dialog pilots are mirrored under `cm/pages/app/` (except `assetPickerModern`, app-tree only). Legacy Finder chrome (`.perc-mcol`, `$.perc_finder()` widgets) is **not** loaded on residual modern pilot pages.

### Finder / DCE retirement

The legacy Miller-column Finder is hard-cut in 8.2:

- JSP entry points in `cm/app/` (webmgt, dashboard, admin, editTemplate, adminWorkflow, users) no longer include `finder.jsp` / `finder_js.jsp` and no longer mount `$.Percussion.PercFinderView()`. `siteArchitecture.jsp` is not shipped; bookmarks 301 to SPA Navigation. `editAsset.jsp` is not shipped (#3473); bookmarks 301 to the React Content Editor (`spa.jsp?entry=editor`). `?view=editor` is the same SPA host, not `webmgt.jsp`.
- The Desktop Content Explorer (DCE) is **not required for core admin** in 8.2 (CE-not-required sign-off, SC-007).
- Per-host migration: `host-asset-picker`, `host-page-picker`, `host-folder-picker` are migrated to the modern `ContentBrowser` (PRs #1391 + #1394). `host-aa-contentbrowser-dialog` is deferred to 8.3 (Dojo Track A blocker per the WebUI AGENTS.md Track A "no new Dojo code" rule). `host-home-library` is optional (gated on the 989 widget builder).
- Pre-existing Java finder consumers still remain in the legacy view/plugin layer (e.g. `PercFolderPropertiesDialog`, `PercNavigationManager`, `perc_newsitedialog`, `PercContributorUiAdaptor`); these are the per-host follow-up migration tracked in `specs/992-react-content-explorer/checklists/cutover-inventory.md` §C. They are not on the 8.2 critical path; they remain functional through 8.2.x.

### Modern REACT CHROME — interactive map

|       Component        |         Mount path         |                                      Role                                       |
|------------------------|----------------------------|---------------------------------------------------------------------------------|
| `ContentExplorerShell` | SPA `entry=explorer`       | Tree + detail-list + action toolbar + context-menu compose                      |
| `ExplorerTree`         | inside Shell               | Sites / folders tree; lazy expand                                               |
| `DetailList`           | inside Shell               | Item list with pagination; SC-005 perf regression                               |
| `ReducedActions`       | inside Shell               | Per-item menu (open/preview/createFolder/rename/move/copy/delete)               |
| `ContextMenu`          | inside Shell               | Right-click menu driven by server `actions/...` REST                            |
| `ActionToolbar`        | inside Shell               | Top toolbar driven by the same server action set                                |
| `FolderSecurityPanel`  | Explorer shell + `folderSecurityModern.jsp` | ACL + folder properties (community/locale/DF/workflow); `aclLockout.ts` + bootstrap identities |
| `SearchPanel`          | `searchModern.jsp`         | Extended search with retry / open / reveal                                      |
| `ClipboardPanel`       | `us7AdvancedModern.jsp`    | Copy / cut / paste to a target folder                                           |
| `SiteCopyWizard`       | same                       | 5-step site copy wizard                                                         |
| `SubfolderCopyWizard`  | same                       | 3-step subfolder copy wizard                                                    |
| `DependencyViewer`     | same                       | 6-dimension dependency summary (AA populated, 5 dimensions client-side preview) |
| `RelationshipsView`    | same                       | 4 primary IA rows + AA / reverse details (same partial)                         |

### Spec / artifacts

- Spec/plan/tasks: `specs/992-react-content-explorer/`.
- Capability matrix roll-up: `specs/992-react-content-explorer/contracts/capability-matrix.md` §"T086 status roll-up (2026-07-20)".
- a11y checklist (T082): `specs/992-react-content-explorer/checklists/a11y-spotcheck.md`.
- i18n key inventory (T083): `specs/992-react-content-explorer/checklists/i18n-key-presence.md`.
- Cutover inventory + per-phase sign-off (T085): `specs/992-react-content-explorer/checklists/cutover-inventory.md`.
- 8.2 parity evidence (T089a): `docs/ai-generated/release/992-8.2-parity-evidence.md`.
- Security review (T089): `docs/ai-generated/release/security-review-992.md`.

### Verification

|             Suite              |                         Where                         |                       Pattern                        |
|--------------------------------|-------------------------------------------------------|------------------------------------------------------|
| Vitest (component + a11y gate) | `WebUI/src/test/ts/{contentExplorer,contentBrowser}/` | `cd WebUI/src/main/frontend && npx vitest run`       |
| Playwright (E2E + a11y gate)   | `modules/perc-qa-automation/frontend/tests/`          | `cd modules/perc-qa-automation/frontend && npm test` |

## Modern Unified Workflow & Admin UI (feature 993 / #3088 / #3201)

Modern React replacement for the legacy Workflow, Role, User, Category, and Admin UI screens. **One product shell:** `AdminShell` titled **Admin tools**. Workflow, roles, users, and categories are in-shell tabs — there is no separate Administration sibling (`admin-sibling-workflow-link`, #3340). `WorkflowAdminShell` is a redirect stub into `/admin/:tab`.

### Entry points (SPA — PR-5/PR-8)

|      Page      |               URL               |                                       Component(s)                                        |
|----------------|---------------------------------|-------------------------------------------------------------------------------------------|
| Admin tools    | `cm/app/spa.jsp?entry=admin`    | `AdminShell` (tasks, logs, notifications, tools, workflow, roles, users, categories)      |
| Legacy workflow entry | `cm/app/spa.jsp?entry=workflow` | Redirects into `AdminShell` workflow tab (`/admin/workflow`)                         |

Former product hosts `adminWorkflowModern.jsp` / `adminModern.jsp` were removed in PR-8.

### React Components — interactive map

|          Component          |      Mount path      |                                       Role                                       |
|-----------------------------|----------------------|----------------------------------------------------------------------------------|
| `AdminShell`                | SPA `entry=admin`    | Unified Admin tools (tasks, logs, notifications, tools + workflow/roles/users/categories) |
| `WorkflowAdminShell`        | redirect only        | Legacy name; `Navigate` to `/admin/:tab` (#3088)                                 |
| `WorkflowsSection`          | Admin tab            | Workflow list, creation, editing, state & transition management                  |
| `WorkflowAssignmentSection` | inside Workflow tab  | Site-to-workflow mapping, contentType & publishing default template rules        |
| `RolesSection`              | Admin tab            | Role list, creation, member management with dual list picker                     |
| `UsersSection`              | Admin tab            | User list, user creation, role & group assignment                                |
| `CategoriesSection`         | Admin tab            | Category tree explorer, node creation, lock management                           |
| `InContextTransitionButton` | standalone / editor  | Action button for executing item workflow state transitions                      |
| `TasksSection`              | inside AdminShell    | Scheduled task list, schedule builder, trigger actions                           |
| `TaskLogsSection`           | inside AdminShell    | Task execution logs, status filter, detail viewer                                |
| `TaskNotifications`         | inside AdminShell    | Email notification template manager                                              |
| `ToolsSection`              | inside AdminShell    | System tools layout container                                                    |
| `ConsistencyChecker`        | inside ToolsSection  | Content tree consistency verification & fix launcher                             |

### Spec / artifacts

- Spec/plan/tasks: `specs/993-workflow-admin-react-ui/`.

